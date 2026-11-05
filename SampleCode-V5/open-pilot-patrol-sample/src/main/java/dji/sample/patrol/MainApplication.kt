package dji.sample.patrol

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Observer
import dji.sample.patrol.data.Asset
import dji.sample.patrol.data.CapturePoint
import dji.sample.patrol.data.CloudConfig
import dji.sample.patrol.data.Job
import dji.sample.patrol.data.JobStatus
import dji.sample.patrol.data.PatrolEvent
import dji.sample.patrol.data.RecognitionFailureCategory
import dji.sample.patrol.data.RecognitionStage
import dji.sample.patrol.data.ResultCallback
import dji.sample.patrol.data.RouteState
import dji.sample.patrol.data.TargetKind
import dji.sample.patrol.data.UploadProgress
import dji.sample.patrol.data.UploadStage
import dji.sample.patrol.manager.AssetUploadManager
import dji.sample.patrol.manager.CaptureController
import dji.sample.patrol.manager.CapturePointArManager
import dji.sample.patrol.manager.CloudManager
import dji.sample.patrol.manager.DestinationArManager
import dji.sample.patrol.manager.DestinationMapManager
import dji.sample.patrol.manager.FlyToController
import dji.sample.patrol.manager.IntelligentFlightRecognitionPort
import dji.sample.patrol.manager.KeyManagerCameraCommandPort
import dji.sample.patrol.manager.KeyManagerGeoProbe3D
import dji.sample.patrol.manager.KeyManagerIntelligentStatusProbe
import dji.sample.patrol.manager.MatchGate
import dji.sample.patrol.manager.MatchIssue
import dji.sample.patrol.manager.MatchNotice
import dji.sample.patrol.manager.MediaManager
import dji.sample.patrol.manager.PatrolAssetReporter
import dji.sample.patrol.manager.PatrolFlowConfig
import dji.sample.patrol.manager.PatrolMapManager
import dji.sample.patrol.manager.PilotCapturePointArPort
import dji.sample.patrol.manager.PilotDestinationArPort
import dji.sample.patrol.manager.PilotFlyToPort
import dji.sample.patrol.manager.PilotMapPort
import dji.sample.patrol.manager.PointReporter
import dji.sample.patrol.manager.RecognitionManager
import dji.sample.patrol.manager.RemoteFilePusher
import dji.sample.patrol.manager.RouteController
import dji.sample.patrol.manager.TaskManager
import dji.sample.patrol.manager.UploadNotifier
import dji.sample.patrol.manager.WaylineManager
import dji.sample.patrol.models.ConnectionVM
import dji.sample.patrol.models.TaskVM
import dji.sample.patrol.models.WaylineTaskVM
import dji.sample.patrol.pilot.RemoteControllerSerialNumberMonitor
import dji.sample.patrol.pilot.SdkBootstrapper
import dji.sample.patrol.util.globalViewModel
import java.io.File

/**
    * 插件 App 入口：MSDK 初始化（REMOTE_PILOT）交给 [SdkBootstrapper]，本类负责框架组装。
    *
    * 组装职责：把悬浮窗按钮动作接到业务 VM，业务初始化统一收敛到 CONNECTED 后触发（连接门控）。
    * SDK 的 init/registerApp 生命周期由 [SdkBootstrapper] 唯一负责，[ConnectionVM] 只消费其状态。
    */
class MainApplication : Application() {

    companion object {
        private const val DISCONNECT_GRACE_MS = 3_000L
        private const val PATROL_ICON_DIR = "patrol-icons"
        private const val PATROL_AR_ICON_DIR = "patrol-ar"

        lateinit var instance: MainApplication
            private set
    }

    val sdkBootstrapper: SdkBootstrapper by lazy { SdkBootstrapper(this) }

    private val rcSerialNumberMonitor by lazy { RemoteControllerSerialNumberMonitor() }
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pilotConnected = false
    private var disconnectCleanup: Runnable? = null

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        com.cySdkyc.clx.Helper.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        val connectionVM = globalViewModel<ConnectionVM>()
        val taskVM = globalViewModel<TaskVM>()
        val waylineTaskVM = globalViewModel<WaylineTaskVM>()

        CloudManager.startHealthChecks(BuildConfig.PATROL_SERVER_URL)

        // 巡逻航线编排统一委托 RouteController：接受任务后自动加载、显式确认起飞、暂停/恢复/停止。
        taskVM.onLoadRoute = { job, callback -> RouteController.load(job, callback) }
        taskVM.onTakeOff = { job, callback -> RouteController.takeOff(job, callback) }
        taskVM.onPauseWayline = { callback -> waylineTaskVM.pauseMission(callback) }
        taskVM.onResumeWayline = { callback -> waylineTaskVM.resumeMission(callback) }
        // 浮窗[停止]只终止航线（工单保持进行中，需重新加载航线并起飞）。
        taskVM.onStopMission = { callback -> waylineTaskVM.stopMission(callback) }
        taskVM.onReturnHome = { callback -> RouteController.requestReturnHome(callback) }
        // 旧八状态容器的[取消任务]：先停止航线，成功后再取消服务端工单。
        taskVM.onCancelExecution = { job, callback -> waylineTaskVM.cancelMission(job, callback) }
        taskVM.onRetryFinalization = { job, callback -> waylineTaskVM.retryFinalization(job, callback) }
        taskVM.onPauseFinalization = { job -> waylineTaskVM.pauseFinalization(job) }

        wireRecognitionCapture(taskVM)
        preparePatrolMapIcons()
        wireRealtimeUpload()
        wireDestinationFlyto(taskVM)

        // Pilot 连接后继续等待 RC SN，避免用错误身份初始化任何云端业务。
        connectionVM.onConnected {
            pilotConnected = true
            disconnectCleanup?.let(mainHandler::removeCallbacks)
            disconnectCleanup = null
            // 连接就绪即渲染巡逻空闲态「暂无任务」，不等待 RC SN（design D2；spec「连接就绪即呈现空闲态」）
            taskVM.flowConfig = PatrolFlowConfig
            taskVM.render()
            rcSerialNumberMonitor.await { rcSerialNumber ->
                CloudManager.init(
                    CloudConfig(
                        baseUrl = BuildConfig.PATROL_SERVER_URL,
                        rcSerialNumber = rcSerialNumber,
                    )
                )
                taskVM.start(this, rcSerialNumber)
                waylineTaskVM.onPilotConnected(this)
                PatrolAssetReporter.start(this, WaylineManager.preparedJobId)
                UploadNotifier.init(this)
                DestinationMapManager.start()
                TaskManager.startPolling()
                PatrolMapManager.start()
                // 拍照点 AR 投射（design D6）：连接就绪即预热图标与类别配置，拍照时只上传点位。
                CapturePointArManager.start(preparePatrolArIcons())
                taskVM.render()
            }
        }
        connectionVM.onDisconnected {
            pilotConnected = false
            rcSerialNumberMonitor.cancel()
            TaskManager.stopPolling()
            RecognitionManager.stop("Pilot disconnected")
            PatrolAssetReporter.stop()
            DestinationMapManager.cleanup()
            DestinationArManager.reset()
            CapturePointArManager.reset()
            RemoteFilePusher.reset()
            FlyToController.onDisconnected()
            runCatching { PatrolMapManager.cleanup() }
            CaptureController.cancel()
            taskVM.onPilotDisconnected()
            disconnectCleanup?.let(mainHandler::removeCallbacks)
            disconnectCleanup = Runnable {
                if (!pilotConnected) {
                    TaskManager.deactivateIdentity()
                    CloudManager.clear()
                    waylineTaskVM.onPilotDisconnected()
                }
            }.also { mainHandler.postDelayed(it, DISCONNECT_GRACE_MS) }
        }
        // 先挂好状态回调再订阅 SdkBootstrapper 连接状态，避免快速连接时漏掉 CONNECTED。
        connectionVM.initSDK(this)

        // MSDK init/registerApp 的唯一入口：初始化异常记录为失败，避免 Application 启动流程直接终止。
        runCatching { sdkBootstrapper.start() }
            .onFailure { Log.e("MainApplication", "SdkBootstrapper start failed", it) }
    }

    /**
     * 组装识别拍照环节（design D9）：注入生产端口，接线「命中 → 暂停 → 拍照 → 取点 → 落点 → 恢复」，
     * 并把识别阶段/点位暴露给浮窗与点位 Tab。保持既有 [WaylineManager]/[MediaManager] SDK 调用不变。
     */
    private fun wireRecognitionCapture(taskVM: TaskVM) {
        RecognitionManager.recognitionPort = IntelligentFlightRecognitionPort
        RecognitionManager.statusProbe = KeyManagerIntelligentStatusProbe
        CaptureController.cameraPort = KeyManagerCameraCommandPort
        PointReporter.geoProbe = KeyManagerGeoProbe3D
        PatrolMapManager.mapPort = PilotMapPort
        PatrolMapManager.titleProvider = { kind -> getString(targetTitleRes(kind)) }
        PatrolMapManager.onError = { category, message -> onRecognitionError(category, message) }
        CapturePointArManager.arPort = PilotCapturePointArPort
        CapturePointArManager.labelProvider = { point ->
            getString(
                R.string.fw_ar_capture_label,
                getString(targetTitleRes(point.kind)),
                point.targetIndex,
            )
        }
        CapturePointArManager.onError = { category, message -> onRecognitionError(category, message) }

        RecognitionManager.isTargetRecorded = { PointReporter.isRecorded(it) }
        RecognitionManager.onCancelled = { PointReporter.cancelInFlight() }
        RecognitionManager.onMatch = { target ->
            showToast(R.string.fw_recognition_matched_toast)
            PatrolAssetReporter.recordEvent(
                PatrolEvent(
                    id = "match-${target.dedupKey}-${System.currentTimeMillis()}",
                    kind = target.targetType,
                    targetIndex = target.targetIndex,
                    at = System.currentTimeMillis(),
                    description = "matched",
                ),
            )
            RouteController.handleMatch(target)
        }

        // 命中流水线：快门成功即取点并恢复航线；媒体下载/落点在后台继续，失败不阻塞（design D2/D3）。
        RouteController.matchPipeline = { target, callback ->
            PointReporter.activeJobId = WaylineManager.preparedJobId
            // 本次拍照对应的点位；快门成功时创建，媒体下载完成后整组回绑（同一次拍照按顺序处理）。
            var capturedPoint: CapturePoint? = null
            CaptureController.capture(
                onShutter = object : ResultCallback<Unit> {
                    override fun onSuccess(data: Unit) {
                        // 照片尚未下载，先记录本次拍照（一条记录 = 一组）；资产到达后整组回绑。
                        val point = PointReporter.record(target).also { capturedPoint = it }
                        if (point == null) {
                            // 坐标无效：不写点、不落点，但按用户要求不阻塞航线恢复。
                            onRecognitionError(
                                RecognitionFailureCategory.ANCHOR_ERROR,
                                "Invalid aircraft location for capture point",
                            )
                        } else {
                            PatrolMapManager.drop(point)
                            // AR 与地图同源同刻投射；失败只降级，不阻塞拍照与航线恢复（design D6/D9）。
                            CapturePointArManager.project(point)
                            showToast(R.string.fw_recognition_captured_toast)
                        }
                        callback.onSuccess(Unit)
                    }

                    override fun onFailure(error: Throwable) = callback.onFailure(error)
                },
                onMedia = object : ResultCallback<List<Asset>> {
                    override fun onSuccess(data: List<Asset>) {
                        // 一次快门的多镜头文件整组绑定到该次拍照点位。
                        capturedPoint?.let { PointReporter.attach(it.id, data) }
                    }

                    override fun onFailure(error: Throwable) {
                        // 媒体下载失败只影响照片同步时效，不改变航线与已拍照判定。
                        onRecognitionError(
                            RecognitionFailureCategory.CAPTURE_ERROR,
                            "Photo download failed: ${error.message}",
                        )
                        showToast(R.string.fw_match_media_failed_toast)
                    }
                },
            )
        }
        RouteController.onMatchFailure = { category, message -> onRecognitionError(category, message) }
        RouteController.matchGate = {
            if (MediaManager.busy.value == true) {
                MatchGate.Wait("媒体同步进行中")
            } else {
                CaptureController.readinessFailure()?.let { reason -> MatchGate.Reject(reason) }
                    ?: MatchGate.Ready
            }
        }
        RouteController.mediaBusy = MediaManager.busy
        RouteController.onMatchNotice = { notice ->
            RecognitionManager.reportMatchNotice(
                notice?.let { getString(R.string.fw_match_waiting_notice) },
            )
        }
        RouteController.onMatchAborted = { reason -> RecognitionManager.abortMatch(reason) }
        RouteController.onMatchIssue = { issue, reason -> onMatchIssue(issue, reason) }

        taskVM.recognitionStageSource = RecognitionManager.stage
        taskVM.recognitionErrorSource = RecognitionManager.error
        taskVM.recognitionNoticeSource = RecognitionManager.matchNotice
        taskVM.capturePointsSource = PointReporter.points

        PointReporter.observeAssets(MediaManager.localAssets)
        RecognitionManager.stage.observeForever(recognitionStageObserver)
        RecognitionManager.start()
    }

    private val recognitionStageObserver = Observer<RecognitionStage> { stage ->
        if (stage == RecognitionStage.DEGRADED) showToast(R.string.fw_recognition_degraded_toast)
    }

    /**
     * 把分类图标从 assets 物化到私有目录并注入地图落点。
     *
     * assets 只读、且文件传输接口需要真实文件路径，所以启动时复制一份；失败时保持空映射，
     * 落点回退为文字标签（仍可见），不影响任务流程。
     */
    private fun preparePatrolMapIcons() {
        runCatching {
            val dir = File(filesDir, PATROL_ICON_DIR).apply { mkdirs() }
            val icons = TargetKind.entries.associateWith { kind ->
                val name = patrolIconAssetName(kind)
                val target = File(dir, name)
                if (!target.isFile) {
                    assets.open("$PATROL_ICON_DIR/$name").use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                target.absolutePath
            }
            PatrolMapManager.iconLocalPaths = icons
            Log.i("MainApplication", "patrol map icons ready: ${icons.size}")
        }.onFailure {
            Log.w("MainApplication", "patrol map icons unavailable, falling back to text labels: ${it.message}")
        }
    }

    private fun patrolIconAssetName(kind: TargetKind): String = when (kind) {
        TargetKind.PERSON -> "person.png"
        TargetKind.CAR -> "car.png"
        TargetKind.BOAT -> "boat.png"
        TargetKind.CUSTOM -> "custom.png"
        TargetKind.UNKNOWN -> "unknown.png"
    }

    /**
     * 把 AR 类别矢量图标从 assets 物化到私有目录（design D4/D6）。
     *
     * AR 只接受 SVG 且推送需要真实文件路径；失败时返回空映射，AR 按类别跳过并上报
     * `AR_ICON_UNAVAILABLE`，不影响地图落点与拍照。
     */
    private fun preparePatrolArIcons(): Map<TargetKind, String> = runCatching {
        val dir = File(filesDir, PATROL_AR_ICON_DIR).apply { mkdirs() }
        TargetKind.entries.associateWith { kind ->
            val name = patrolArIconAssetName(kind)
            val target = File(dir, name)
            if (!target.isFile) {
                assets.open("$PATROL_AR_ICON_DIR/$name").use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }
            target.absolutePath
        }
    }.onFailure {
        Log.w("MainApplication", "patrol AR icons unavailable, AR projection skipped: ${it.message}")
    }.getOrDefault(emptyMap())

    private fun patrolArIconAssetName(kind: TargetKind): String = when (kind) {
        TargetKind.PERSON -> "person.svg"
        TargetKind.CAR -> "car.svg"
        TargetKind.BOAT -> "boat.svg"
        TargetKind.CUSTOM -> "custom.svg"
        TargetKind.UNKNOWN -> "unknown.svg"
    }

    // ---- M3 边飞边上传装配：点位/事件 → PatrolAssetReporter → 上传引擎 + 通知栏 ----

    private val uploadPointsObserver = Observer<List<CapturePoint>> { points ->
        PatrolAssetReporter.updatePoints(points.orEmpty())
    }

    private val uploadQueueObserver = Observer<List<Asset>> { queue ->
        PatrolAssetReporter.onQueueChanged(queue.orEmpty())
    }

    private val uploadProgressObserver = Observer<Map<String, UploadProgress>> { progress ->
        val stage = PatrolAssetReporter.stage.value ?: UploadStage.IDLE
        if (stage == UploadStage.IDLE) return@Observer
        val jobId = PatrolAssetReporter.activeJobId ?: return@Observer
        val value = progress.orEmpty()[jobId] ?: return@Observer
        UploadNotifier.onProgress(jobId, value, stage)
    }

    private val uploadWaylineObserver = Observer<RouteState> { state ->
        PatrolAssetReporter.onRouteExecuting(state == RouteState.EXECUTING)
    }

    private val uploadJobsObserver = Observer<List<Job>> { jobs ->
        val list = jobs.orEmpty()
        val finalizing = list.firstOrNull { it.status == JobStatus.FINALIZING }
        PatrolAssetReporter.onJobFinalizing(finalizing?.jobId)
        val active = list.firstOrNull {
            it.status == JobStatus.IN_PROGRESS || it.status == JobStatus.FINALIZING
        }
        PatrolAssetReporter.onJobActive(active?.jobId)
    }

    /**
     * 装配 M3 边飞边上传（design D2/D4/D5）：把点位/事件源、上传队列/进度、航线与工单状态
     * 接到 [PatrolAssetReporter] 与 [UploadNotifier]；不新增任何 SDK 调用。
     */
    private fun wireRealtimeUpload() {
        PointReporter.points.observeForever(uploadPointsObserver)
        AssetUploadManager.queue.observeForever(uploadQueueObserver)
        AssetUploadManager.progress.observeForever(uploadProgressObserver)
        RouteController.routeState.observeForever(uploadWaylineObserver)
        TaskManager.jobs.observeForever(uploadJobsObserver)
    }

    /**
     * 装配 M4 目的地/FlyTo（design D2/D3/D4/D9）：注入生产端口并接线「标注目的地 → 二次确认 →
     * FlyTo → 到达/取消回航线」；不新增 SDK 调用，复用既有 RouteController.pause。
     */
    private fun wireDestinationFlyto(taskVM: TaskVM) {
        DestinationMapManager.mapPort = PilotMapPort
        DestinationMapManager.onError = { category, message ->
            Log.w("MainApplication", "destination map ${category.id}: $message")
        }
        DestinationArManager.arPort = PilotDestinationArPort
        DestinationArManager.onError = { category, message ->
            Log.w("MainApplication", "destination ar ${category.id}: $message")
        }
        FlyToController.port = PilotFlyToPort
        FlyToController.onError = { category, message ->
            Log.w("MainApplication", "flyto ${category.id}: $message")
        }
        FlyToController.requestPauseRoute = { RouteController.pause() }
        FlyToController.runningMissionFree = { true }
        taskVM.flyToStageSource = FlyToController.stage
        taskVM.onCancelFlyTo = { FlyToController.cancel() }
        taskVM.onMarkDestination = { openDestinationPicker() }
    }

    /** 从浮窗进入目的地选取：拉起插件全屏页并请求展示目的地对话框。 */
    private fun openDestinationPicker() {
        val intent = android.content.Intent(this, PluginActivity::class.java).apply {
            putExtra(PluginActivity.EXTRA_MARK_DESTINATION, true)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    /** 命中失败的 UI 提示：浮窗状态行由 RecognitionManager 承载，这里负责 Toast。 */
    private fun onMatchIssue(issue: MatchIssue, reason: String) {
        Log.w("MainApplication", "match issue: $issue reason=$reason")
        val messageRes = when (issue) {
            MatchIssue.CAMERA_NOT_READY -> R.string.fw_match_camera_not_ready_toast
            MatchIssue.MEDIA_WAIT_TIMEOUT -> R.string.fw_match_wait_timeout_toast
            MatchIssue.CAPTURE_NOT_ISSUED -> R.string.fw_match_capture_failed_toast
            MatchIssue.CAPTURE_FAILED -> R.string.fw_match_capture_failed_toast
            MatchIssue.RESUME_FAILED -> R.string.fw_match_resume_auto_failed_toast
        }
        showToast(messageRes)
        // 仍停留在 MATCHED 的场景：把本地化原因写进浮窗状态行。
        val statusRes = when (issue) {
            MatchIssue.CAPTURE_FAILED -> R.string.fw_match_status_capture_failed
            MatchIssue.RESUME_FAILED -> R.string.fw_match_status_resume_failed
            else -> null
        }
        statusRes?.let {
            RecognitionManager.reportExternalFailure(RecognitionFailureCategory.NOT_READY, getString(it))
        }
    }

    private fun onRecognitionError(category: RecognitionFailureCategory, message: String) {
        Log.w("MainApplication", "recognition ${category.id}: $message")
        PatrolAssetReporter.recordEvent(
            PatrolEvent(
                id = "error-${category.id}-${System.currentTimeMillis()}",
                kind = TargetKind.UNKNOWN,
                targetIndex = -1,
                at = System.currentTimeMillis(),
                description = "${category.id}: $message",
            ),
        )
    }

    private fun targetTitleRes(kind: TargetKind): Int = when (kind) {
        TargetKind.PERSON -> R.string.fw_target_person
        TargetKind.CAR -> R.string.fw_target_car
        TargetKind.BOAT -> R.string.fw_target_boat
        TargetKind.CUSTOM -> R.string.fw_target_custom
        TargetKind.UNKNOWN -> R.string.fw_target_unknown
    }

    private fun showToast(resId: Int) {
        mainHandler.post { Toast.makeText(this, resId, Toast.LENGTH_SHORT).show() }
    }
}
