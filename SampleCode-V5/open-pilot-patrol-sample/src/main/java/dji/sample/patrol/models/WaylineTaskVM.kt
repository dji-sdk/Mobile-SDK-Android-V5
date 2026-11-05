package dji.sample.patrol.models

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import dji.sample.patrol.data.Asset
import dji.sample.patrol.data.Job
import dji.sample.patrol.data.JobStatus
import dji.sample.patrol.data.ResultCallback
import dji.sample.patrol.data.RouteState
import dji.sample.patrol.manager.CapturePointArManager
import dji.sample.patrol.manager.MediaManager
import dji.sample.patrol.manager.PointReporter
import dji.sample.patrol.manager.AssetUploadManager
import dji.sample.patrol.manager.PatrolAssetReporter
import dji.sample.patrol.manager.RouteController
import dji.sample.patrol.manager.TaskManager
import dji.sample.patrol.manager.WaylineManager
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.v5.common.callback.CommonCallbacks
import dji.v5.manager.KeyManager

/**
    * 航线任务 VM（全局级，挂 globalViewModelStore）。
    *
    * 管理航线、资源和传输相关 UI，并把页面操作转交给对应 Manager；
    * 不包含 MSDK、HTTP 或文件上传实现。
    */
class WaylineTaskVM : ViewModel() {

    val routeAsset = WaylineManager.routeAsset

    /** 页面唯一航线快照（状态 + 进度 + 错误 + 归属工单），与浮窗同源。 */
    val routeSnapshot = RouteController.snapshot
    val jobs = TaskManager.jobs
    val mediaReferences = MediaManager.mediaReferences

    // 航线编排权威流（同一 RouteController 驱动），不各自维护副本。
    val localAssets = MediaManager.localAssets
    val manifestAssets = MediaManager.manifestAssets
    val thumbnailPaths = MediaManager.thumbnailPaths
    val downloadStates = MediaManager.downloadStates
    val uploadQueue = AssetUploadManager.queue
    val uploadProgress = AssetUploadManager.progress
    val uploadDetails = AssetUploadManager.details
    val mediaError = MutableLiveData<String?>(null)

    private val mainHandler = Handler(Looper.getMainLooper())
    private var initialized = false

    /** 飞行器连接监听：connection 变 true 时触发一次「加载被拒后的自动重试」。 */
    private val connectionKey = KeyTools.createKey(FlightControllerKey.KeyConnection)
    private var connectionListening = false
    private val connectionListener = CommonCallbacks.KeyListener<Boolean> { _, connected ->
        Log.i(TAG, "aircraft connection changed: $connected")
        if (connected == true) RouteController.onAircraftConnected()
    }
    private var currentJob: Job? = null
    private var mediaSyncRunning = false
    private var mediaSyncAgain = false
    private var finalSyncRequested = false
    private var finalSyncFinished = false
    private var finalSyncScheduled = false
    private var finalizingFinished = false
    private var completionInFlight = false
    private var completionFinished = false
    private var mediaRetryCount = 0
    private var manualRetryRequired = false
    private var lastRouteState = RouteState.ROUTE_PENDING

    /**
     * 取消任务期间的收尾抑制标记。
     *
     * 设计意图：取消任务 = 停止航线 + 取消工单，停止成功同样会发布 `STOPPED`；若不抑制，
     * 收尾触发会先于取消把工单置为 `FINALIZING`，与「取消后工单为 `CANCELLED`」冲突。
     * 只在本次取消调用期间为 `true`，停止回调内立即复位。
     */
    private var suppressStopFinalization = false

    /**
     * 收尾链路代次。
     *
     * 设计意图：`beginFinalization` 失败会定时重试；飞手在「已停止」窗口重新加载航线或断连后，
     * 旧的重试必须作废，避免同一工单同时处于「重新加载航线」与「收尾同步」。每次加载/断连递增。
     */
    private var finalizationEpoch = 0

    /**
     * 收尾触发只认航线权威状态：`FINISHED`（自然结束）与 `STOPPED`（用户停止）同等视为航线结束，
     * 与页面/浮窗展示同源；判定逻辑由纯函数 [StopFinalizationPolicy] 提供。
     */
    private val routeStateObserver = Observer<RouteState> { state ->
        val value = state ?: RouteState.ROUTE_PENDING
        if (value == RouteState.FINISHED || value == RouteState.STOPPED || value == RouteState.FAILED) {
            // 任务结束/停止/失败即清理本次 AR 标识（design D8）；PAUSED/EXECUTING 不清。
            CapturePointArManager.clear()
        }
        if (value == RouteState.EXECUTING) {
            // 执行入口兜底：无论是加载后起飞、重连恢复还是任务列表异步恢复，
            // 进入执行态时必须存在本轮窗口，后续拍照才不会被上一轮已关闭窗口排除。
            currentJob?.let { job ->
                MediaManager.ensureCurrentExecutionWindow(job.jobId, "route-executing")
            }
        }
        if (StopFinalizationPolicy.shouldStartFinalization(
                previous = lastRouteState,
                next = value,
                finalSyncRequested = finalSyncRequested,
                suppressed = suppressStopFinalization,
            )
        ) {
            val job = currentJob
            val owner = RouteController.snapshot.value?.jobId
            when {
                job == null ->
                    Log.w(TAG, "stop finalization skipped: no current job, routeState=$value")
                owner != null && owner != job.jobId ->
                    Log.w(TAG, "stop finalization skipped: owner mismatch, jobId=${job.jobId}, owner=$owner")
                job.status == JobStatus.FINALIZING || job.status == JobStatus.COMPLETED ->
                    Log.i(TAG, "stop finalization skipped: job already ${job.status}, jobId=${job.jobId}")
                else -> {
                    Log.i(
                        TAG,
                        "stop finalization trigger: jobId=${job.jobId}, routeState=$value, " +
                            "epoch=$finalizationEpoch",
                    )
                    beginFinalization(job)
                }
            }
        } else if (value in StopFinalizationPolicy.ENDED_ROUTE_STATES) {
            Log.i(
                TAG,
                "stop finalization ignored: from=$lastRouteState, to=$value, " +
                    "requested=$finalSyncRequested, suppressed=$suppressStopFinalization",
            )
        }
        lastRouteState = value
    }

    private val jobsObserver = Observer<List<Job>> { jobs ->
        MediaManager.updateJobStatuses(jobs.associate { it.jobId to it.status })
        val job = jobs.firstOrNull { it.status == JobStatus.FINALIZING }
        if (job != null && currentJob == null) {
            // APP 冷启动恢复时等待用户从浮窗或任务分组显式继续。
            currentJob = job
            manualRetryRequired = true
        } else if (job != null && !manualRetryRequired &&
            (!finalSyncRequested || (!finalSyncFinished && !mediaSyncRunning && !finalSyncScheduled))
        ) {
            resumeFinalization(job)
        } else {
            jobs.firstOrNull { it.status == JobStatus.IN_PROGRESS }?.let { running ->
                if (currentJob?.jobId != running.jobId) {
                    currentJob = running
                    // 任务列表晚于连接返回时，同样要保证本轮窗口可用。
                    MediaManager.ensureCurrentExecutionWindow(running.jobId, "jobs-adopted")
                    // 航线未结束的恢复不拉媒体，由用户重新执行航线。
                    manualRetryRequired = true
                }
            }
        }
        val current = currentJob ?: return@Observer
        if (jobs.firstOrNull { it.jobId == current.jobId }?.status in setOf(JobStatus.COMPLETED, JobStatus.CANCELLED)) {
            completionFinished = true
            MediaManager.endJob()
        }
    }

    private val uploadObserver = Observer<List<Asset>> {
        finishTaskIfReady()
    }

    private val localAssetObserver = Observer<List<Asset>> { assets ->
        val job = currentJob ?: return@Observer
        // 只把本轮照片加入上传队列：本地历史照片保留，但不再被新一轮重复上传。
        val jobAssets = assets.filter { it.jobId == job.jobId && isCurrentRoundAsset(job.jobId, it) }
        if (jobAssets.isNotEmpty()) {
            AssetUploadManager.enqueue(job.jobId, jobAssets)
        }
    }

    private fun isCurrentRoundAsset(jobId: String, asset: Asset): Boolean {
        val capturedAt = asset.meta["capturedAtMillis"]?.toLongOrNull() ?: 0L
        return MediaManager.isCurrentRound(jobId, capturedAt)
    }

    fun onPilotConnected(context: Context) {
        if (!initialized) {
            initialized = true
            MediaManager.init(context)
            AssetUploadManager.init(context)
            RouteController.routeState.observeForever(routeStateObserver)
            TaskManager.jobs.observeForever(jobsObserver)
            MediaManager.localAssets.observeForever(localAssetObserver)
            AssetUploadManager.queue.observeForever(uploadObserver)
        }
        // 唯一真相源：Pilot 连接建立即开始订阅 RouteController，界面不再直接读 SDK 原始航线状态。
        RouteController.attach()
        startConnectionRetryListener()
        WaylineManager.init()
        TaskManager.jobs.value?.let { jobsObserver.onChanged(it) }
        currentJob?.let { job ->
            if (job.status != JobStatus.FINALIZING) {
                val startMedia = { MediaManager.beginJob(job.jobId) { requestMediaSync(job) } }
                if (job.status == JobStatus.IN_PROGRESS) {
                    // 冷启动/重连恢复执行中工单：先保证执行窗口可用，再注册媒体事件，
                    // 避免恢复后拍摄的照片被上一轮已关闭的窗口排除。
                    MediaManager.ensureExecutionWindowForResume(job.jobId) { startMedia() }
                } else {
                    startMedia()
                }
            }
        }
    }

    fun loadWayline(job: Job, callback: ResultCallback<Unit>? = null) {
        if (!initialized) {
            callback?.onFailure(IllegalStateException("Pilot is not connected"))
            return
        }
        currentJob = job
        PatrolAssetReporter.resetForJob(job.jobId)
        // 新一次执行从 0 组开始：点位按拍照组计数，不得沿用上一轮记录。
        PointReporter.clear()
        // 新一轮执行清空已投射的 AR 标识（保留类别配置以便复用），与地图落点同一边界。
        CapturePointArManager.clear()
        // 重新加载开启新一代收尾窗口：作废上一轮（停止后）尚未完成的重试与延迟任务。
        finalizationEpoch++
        suppressStopFinalization = false
        finalSyncRequested = false
        finalSyncFinished = false
        finalSyncScheduled = false
        finalizingFinished = false
        completionInFlight = false
        completionFinished = false
        mediaRetryCount = 0
        manualRetryRequired = false
        lastRouteState = RouteState.ROUTE_PENDING
        mediaError.value = null
        // 新一轮执行：清除上一轮遗留的航线状态（同 jobId 重新下发时尤其重要）。
        // 同一执行内的「停止后重新加载」由 startNewExecution 内部判定为同一轮，保持 STOPPED 语义。
        RouteController.startNewExecution(job)
        if (job.status == JobStatus.IN_PROGRESS) {
            MediaManager.restartJob(job.jobId) { prepareWaylineJob(job, callback) }
        } else {
            prepareWaylineJob(job, callback)
        }
    }

    private fun prepareWaylineJob(job: Job, callback: ResultCallback<Unit>?) {
        MediaManager.beginJob(job.jobId) {
            requestMediaSync(job)
        }
        MediaManager.prepareJobFolder(job.jobId, object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) {
                // 航线装配/下载/校验/导入统一交给 RouteController（内部复用 WaylineManager）。
                RouteController.load(job, callback)
            }

            override fun onFailure(error: Throwable) {
                callback?.onFailure(error)
            }
        })
    }

    fun startMission(callback: ResultCallback<Unit>? = null) {
        val job = currentJob
        if (job == null) {
            callback?.onFailure(IllegalStateException("Load a task before starting the mission"))
            return
        }
        RouteController.takeOff(job, callback)
    }

    /**
     * 暂停当前航线任务。
     *
     * 设计意图：把悬浮窗的「暂停」动作经 [RouteController] 透传到航线管理，并允许调用方拿到命令结果。
     * 回调只表示命令被接受，航线是否真的暂停必须以 [RouteController.routeState] 上报的权威状态为准。
     */
    fun pauseMission(callback: ResultCallback<Unit>? = null) {
        RouteController.pause(callback)
    }

    /**
     * 恢复被暂停的航线任务。
     *
     * 回调语义同 [pauseMission]：命令接受 ≠ 航线已恢复执行。
     */
    fun resumeMission(callback: ResultCallback<Unit>? = null) {
        RouteController.resume(callback)
    }

    /**
     * 停止当前航线任务（取消任务的第一个环节）。
     *
     * 回调语义同 [pauseMission]：命令接受 ≠ 航线已停止。仅终止航线；停止成功后航线视为结束，
     * 由 [routeStateObserver] 自动进入收尾同步，飞手无需再手动触发。
     */
    fun stopMission(callback: ResultCallback<Unit>? = null) {
        RouteController.stop(callback)
    }

    /**
     * 取消任务：先停止航线，成功后再取消服务端工单。
     *
     * 设计意图：把「停止航线 → 取消工单」两段不可逆动作收敛在一个入口，失败即整体失败。
     * 航线本来就没在运行时的停止失败由 [RouteController.stop] 视为幂等成功，继续取消工单。
     *
     * 取消不是「结束并收尾」：停止成功会发布 `STOPPED`，因此本次调用期间抑制收尾触发，
     * 保证工单最终收敛到 `CANCELLED` 而不是 `FINALIZING`/`COMPLETED`。
     */
    fun cancelMission(job: Job, callback: ResultCallback<Unit>) {
        suppressStopFinalization = true
        RouteController.stop(object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) {
                suppressStopFinalization = false
                cancelJobAfterStop(job, callback)
            }

            override fun onFailure(error: Throwable) {
                suppressStopFinalization = false
                callback.onFailure(error)
            }
        })
    }

    private fun cancelJobAfterStop(job: Job, callback: ResultCallback<Unit>) {
        TaskManager.cancel(job.jobId, object : ResultCallback<Job> {
            override fun onSuccess(data: Job) = callback.onSuccess(Unit)

            override fun onFailure(error: Throwable) = callback.onFailure(error)
        })
    }

    fun downloadPhotos(allowPlayback: Boolean = false) {
        currentJob?.let { requestMediaSync(it, finalAttempt = allowPlayback) }
    }

    fun uploadAssets(jobId: String, assets: List<Asset>) {
        AssetUploadManager.enqueue(jobId, assets)
    }

    fun pauseUpload(jobId: String) {
        AssetUploadManager.pause(jobId)
    }

    fun resumeUpload(jobId: String) {
        AssetUploadManager.resume(jobId)
    }

    fun retryUpload(jobId: String, assetId: String) {
        AssetUploadManager.retry(jobId, assetId)
    }

    fun retryFinalization(job: Job, callback: ResultCallback<Unit>) {
        if (mediaSyncRunning || finalSyncScheduled) {
            callback.onFailure(IllegalStateException("Photo sync is already running"))
            return
        }
        manualRetryRequired = false
        mediaSyncRunning = false
        mediaSyncAgain = false
        finalSyncScheduled = false
        MediaManager.cancelDownload()
        AssetUploadManager.resume(job.jobId)
        resumeFinalization(job, callback)
    }

    fun pauseFinalization(job: Job) {
        manualRetryRequired = true
        MediaManager.cancelDownload()
        mediaSyncRunning = false
        mediaSyncAgain = false
        finalSyncScheduled = false
        AssetUploadManager.pause(job.jobId)
        mediaError.postValue("Sync paused")
    }

    fun onPilotDisconnected() {
        // 断连即作废本轮收尾：重连后不得沿用旧代次的重试继续请求。
        finalizationEpoch++
        suppressStopFinalization = false
        MediaManager.cancelBoundaryCapture()
        MediaManager.cancelDownload()
        MediaManager.endJob()
        stopConnectionRetryListener()
        RouteController.release()
        mediaSyncRunning = false
        mediaSyncAgain = false
        finalSyncScheduled = false
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun requestMediaSync(
        job: Job,
        finalAttempt: Boolean = false,
        attemptCallback: ResultCallback<Unit>? = null,
    ) {
        if (mediaSyncRunning) {
            mediaSyncAgain = true
            if (finalAttempt) finalSyncRequested = true
            return
        }
        mediaSyncRunning = true
        // 文件列表和原图拉取均不进入回放，保持边飞边拍边拉能力。
        MediaManager.downloadNewPhotos(job.jobId, allowPlayback = false, object : ResultCallback<List<Asset>> {
            override fun onSuccess(data: List<Asset>) {
                mediaRetryCount = 0
                if (data.isEmpty()) {
                    onMediaSyncFinished(job, finalAttempt)
                    attemptCallback?.onSuccess(Unit)
                } else {
                    AssetUploadManager.enqueue(job.jobId, data) {
                        mainHandler.post {
                            onMediaSyncFinished(job, finalAttempt)
                            attemptCallback?.onSuccess(Unit)
                        }
                    }
                }
            }

            override fun onFailure(error: Throwable) {
                // 飞行中的直拉失败保留 pending，航线结束后按冻结清单补拉。
                mediaSyncRunning = false
                if (finalAttempt) {
                    mediaError.postValue(error.message ?: "Photo download failed")
                    finalSyncFinished = false
                    manualRetryRequired = true
                    attemptCallback?.onFailure(error)
                } else if (mediaSyncAgain) {
                    mediaSyncAgain = false
                    requestMediaSync(job, finalAttempt = finalSyncRequested)
                } else if (mediaRetryCount < MAX_IN_FLIGHT_MEDIA_RETRIES) {
                    mediaRetryCount++
                    mainHandler.postDelayed({
                        if (currentJob?.jobId == job.jobId && !finalSyncRequested) {
                            requestMediaSync(job)
                        }
                    }, IN_FLIGHT_MEDIA_RETRY_DELAY_MS)
                }
            }
        })
    }

    private fun onMediaSyncFinished(job: Job, finalAttempt: Boolean) {
        mediaSyncRunning = false
        if (finalAttempt) finalSyncFinished = true
        if (mediaSyncAgain) {
            mediaSyncAgain = false
            requestMediaSync(job, finalAttempt = finalSyncRequested)
        } else {
            finishTaskIfReady()
        }
    }

    private fun finishTaskIfReady() {
        val job = currentJob ?: return
        if (!finalSyncFinished || !finalizingFinished || mediaSyncRunning || completionInFlight ||
            completionFinished
        ) return
        val jobAssets = AssetUploadManager.queue.value.orEmpty().filter { it.jobId == job.jobId }
        if (jobAssets.any { it.state != dji.sample.patrol.data.AssetState.UPLOADED }) return
        completionInFlight = true
        TaskManager.complete(job.jobId, object : ResultCallback<Job> {
            override fun onSuccess(data: Job) {
                completionInFlight = false
                completionFinished = true
            }

            override fun onFailure(error: Throwable) {
                completionInFlight = false
                mediaError.postValue(error.message ?: "Task completion failed")
                mainHandler.postDelayed(::finishTaskIfReady, PIPELINE_RETRY_DELAY_MS)
            }
        })
    }

    private fun beginFinalization(job: Job) {
        val epoch = finalizationEpoch
        MediaManager.markJobFinished(job.jobId)
        finalSyncRequested = true
        Log.i(TAG, "finalizing request: jobId=${job.jobId}, epoch=$epoch")
        TaskManager.finalize(job.jobId, object : ResultCallback<Job> {
            override fun onSuccess(data: Job) {
                Log.i(TAG, "finalizing accepted: jobId=${job.jobId}")
                currentJob = data
                finalizingFinished = true
                scheduleFinalSync(data, FINAL_MEDIA_SETTLE_MS)
            }

            override fun onFailure(error: Throwable) {
                Log.w(TAG, "finalize request failed: jobId=${job.jobId}, epoch=$epoch, error=${error.message}")
                finalSyncRequested = false
                mediaError.postValue(error.message ?: "Cannot enter finalizing state")
                mainHandler.postDelayed({
                    // 过期判定：重新加载/断连后不得再为上一轮收尾重试。
                    if (epoch != finalizationEpoch || !routeEnded()) {
                        Log.i(
                            TAG,
                            "finalization retry aborted: jobId=${job.jobId}, epoch=$epoch, " +
                                "current=$finalizationEpoch, route=${RouteController.routeState.value}",
                        )
                        return@postDelayed
                    }
                    if (currentJob?.jobId == job.jobId && !finalSyncRequested) beginFinalization(job)
                }, PIPELINE_RETRY_DELAY_MS)
            }
        })
    }

    /**
     * 航线是否仍处于「已结束」权威状态（`STOPPED`/`FINISHED`）。
     *
     * 设计意图：收尾重试与延迟任务只在这一前提下有效；飞手重新加载航线后状态回到
     * `ROUTE_PREPARING`/`ROUTE_READY`，重试必须终止，不能把「重新加载」与「收尾同步」叠在一起。
     */
    private fun routeEnded(): Boolean =
        RouteController.routeState.value in StopFinalizationPolicy.ENDED_ROUTE_STATES

    private fun resumeFinalization(job: Job, callback: ResultCallback<Unit>? = null) {
        currentJob = job
        finalSyncRequested = true
        finalizingFinished = true
        completionFinished = false
        manualRetryRequired = false
        val local = MediaManager.localAssets.value.orEmpty()
            .filter { it.jobId == job.jobId && isCurrentRoundAsset(job.jobId, it) }
        if (local.isNotEmpty()) AssetUploadManager.enqueue(job.jobId, local)
        AssetUploadManager.retry(job.jobId)
        scheduleFinalSync(job, 0, callback)
    }

    /**
     * 收尾同步入口：清单冻结后以「已同步照片数 == 总数」统一判定（0 张时 0 == 0 同样成立）。
     * 已全部同步则直接视为同步完成并尝试结束工单；否则走拉取流程补齐。
     */
    private fun syncFinalMedia(job: Job, callback: ResultCallback<Unit>?) {
        MediaManager.photoSyncCounts(job.jobId) { total, downloaded ->
            if (currentJob?.jobId != job.jobId) return@photoSyncCounts
            if (downloaded < total || mediaSyncRunning) {
                requestMediaSync(job, finalAttempt = true, attemptCallback = callback)
                return@photoSyncCounts
            }
            mediaRetryCount = 0
            finalSyncFinished = true
            callback?.onSuccess(Unit)
            finishTaskIfReady()
        }
    }

    private fun scheduleFinalSync(job: Job, delayMillis: Long, callback: ResultCallback<Unit>? = null) {
        if (mediaSyncRunning || finalSyncScheduled) return
        finalSyncScheduled = true
        val epoch = finalizationEpoch
        mainHandler.postDelayed({
            finalSyncScheduled = false
            if (currentJob?.jobId != job.jobId) {
                return@postDelayed
            }
            if (epoch != finalizationEpoch) {
                Log.i(
                    TAG,
                    "final sync aborted: jobId=${job.jobId}, epoch=$epoch, current=$finalizationEpoch",
                )
                return@postDelayed
            }
            finalSyncFinished = false
            MediaManager.endJob()
            MediaManager.freezeJobManifest(job.jobId, object : ResultCallback<Unit> {
                override fun onSuccess(data: Unit) {
                    // 收尾补扫点位/事件（增量），与照片同队列按 jobId 归并；失败保留 FINALIZING 可重试。
                    runCatching {
                        val pendingReports = PatrolAssetReporter.finalScan(job.jobId)
                        if (pendingReports.isNotEmpty()) {
                            AssetUploadManager.enqueue(job.jobId, pendingReports)
                        }
                    }.onFailure { error ->
                        mediaError.postValue(error.message ?: "Patrol report final scan failed")
                    }
                    syncFinalMedia(job, callback)
                }

                override fun onFailure(error: Throwable) {
                    mediaError.postValue(error.message ?: "Cannot freeze task media list")
                    manualRetryRequired = true
                    callback?.onFailure(error)
                }
            })
        }, delayMillis)
    }

    override fun onCleared() {
        super.onCleared()
        finalizationEpoch++
        suppressStopFinalization = false
        RouteController.routeState.removeObserver(routeStateObserver)
        TaskManager.jobs.removeObserver(jobsObserver)
        MediaManager.localAssets.removeObserver(localAssetObserver)
        AssetUploadManager.queue.removeObserver(uploadObserver)
        MediaManager.release()
        AssetUploadManager.release()
        stopConnectionRetryListener()
        RouteController.release()
    }

    private fun startConnectionRetryListener() {
        if (connectionListening) return
        KeyManager.getInstance().listen(connectionKey, this, true, connectionListener)
        connectionListening = true
    }

    private fun stopConnectionRetryListener() {
        if (!connectionListening) return
        KeyManager.getInstance().cancelListen(this)
        connectionListening = false
    }

    companion object {
        private const val TAG = "WaylineTaskVM"
        private const val FINAL_MEDIA_SETTLE_MS = 1_200L
        private const val IN_FLIGHT_MEDIA_RETRY_DELAY_MS = 1_500L
        private const val MAX_IN_FLIGHT_MEDIA_RETRIES = 3
        private const val PIPELINE_RETRY_DELAY_MS = 3_000L
    }
}

/**
 * 「航线结束 → 收尾同步」的触发策略（纯函数，无状态）。
 *
 * 设计意图：收尾只能有一个触发口径。`FINISHED`（航线自然结束）与 `STOPPED`（飞手在浮窗确认停止）
 * 同等视为「航线结束」，都进入既有收尾流水线；其余状态（执行、暂停、失败、加载中）不触发。
 * 抽成纯函数是因为 [WaylineTaskVM] 构造即持有 `Handler(Looper.getMainLooper())`，JVM 单测无法实例化，
 * 而触发条件本身属于可独立验证的业务规则。
 *
 * 能力契约：同一状态不重复触发；已请求过收尾（`finalSyncRequested`）不重复触发；
 * 取消任务期间（`suppressed`）不触发。线程安全：纯函数，任意线程可调用。
 */
internal object StopFinalizationPolicy {

    /** 视为「航线结束」的权威航线状态。 */
    val ENDED_ROUTE_STATES = setOf(RouteState.FINISHED, RouteState.STOPPED)

    /**
     * 是否应在本状态变更时启动收尾同步。
     *
     * @param previous 变更前的航线状态（用于去重，避免同一状态重复触发）
     * @param next 变更后的航线状态
     * @param finalSyncRequested 本工单是否已经请求过收尾
     * @param suppressed 是否为取消任务等需要抑制收尾的流程
     */
    fun shouldStartFinalization(
        previous: RouteState,
        next: RouteState,
        finalSyncRequested: Boolean,
        suppressed: Boolean,
    ): Boolean = next in ENDED_ROUTE_STATES && previous != next && !finalSyncRequested && !suppressed
}
