package com.dji.sample.gasdetection

import android.app.Application
import android.content.Context
import android.provider.Settings
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.dji.sample.gasdetection.gas.alert.AlertCenter
import com.dji.sample.gasdetection.gas.alert.AlertUiState
import com.dji.sample.gasdetection.gas.alert.severity
import com.dji.sample.gasdetection.gas.autoflight.AutoFlightController
import com.dji.sample.gasdetection.gas.autoflight.AutoFlightPhase
import com.dji.sample.gasdetection.gas.autoflight.AutoFlightSession
import com.dji.sample.gasdetection.gas.autoflight.AutoFlightState
import com.dji.sample.gasdetection.gas.autoflight.AutoFlightTarget
import com.dji.sample.gasdetection.gas.autoflight.SdkAutoFlightGateway
import com.dji.sample.gasdetection.gas.catalog.CatalogInfo
import com.dji.sample.gasdetection.gas.catalog.GasConfig
import com.dji.sample.gasdetection.gas.catalog.GasConfigLoader
import com.dji.sample.gasdetection.gas.follow.FollowAbortReason
import com.dji.sample.gasdetection.gas.follow.FollowBlock
import com.dji.sample.gasdetection.gas.follow.FollowControlLaw
import com.dji.sample.gasdetection.gas.follow.FollowController
import com.dji.sample.gasdetection.gas.follow.FollowLimits
import com.dji.sample.gasdetection.gas.follow.FollowStageSink
import com.dji.sample.gasdetection.gas.follow.FollowState
import com.dji.sample.gasdetection.gas.model.GasMeasurement
import com.dji.sample.gasdetection.gas.monitor.MonitorState
import com.dji.sample.gasdetection.gas.monitor.MonitorStore
import com.dji.sample.gasdetection.gas.payload.GasDemoFrameAdapter
import com.dji.sample.gasdetection.gas.payload.GasPayloadIngest
import com.dji.sample.gasdetection.gas.payload.IngestStartResult
import com.dji.sample.gasdetection.gas.payload.IngestState
import com.dji.sample.gasdetection.gas.payload.PayloadAdapterRegistry
import com.dji.sample.gasdetection.gas.payload.PayloadDiagnostics
import com.dji.sample.gasdetection.gas.payload.PayloadPort
import com.dji.sample.gasdetection.gas.payload.PayloadSnapshot
import com.dji.sample.gasdetection.gas.record.AtomicInspectionQueueFile
import com.dji.sample.gasdetection.gas.record.ExecutorInspectionScheduler
import com.dji.sample.gasdetection.gas.record.HttpInspectionTransport
import com.dji.sample.gasdetection.gas.record.InspectionAggregator
import com.dji.sample.gasdetection.gas.record.InspectionQueue
import com.dji.sample.gasdetection.gas.record.InspectionRecord
import com.dji.sample.gasdetection.gas.record.InspectionRecordBuilder
import com.dji.sample.gasdetection.gas.record.RecordEndReason
import com.dji.sample.gasdetection.gas.remote.AutoFlightCapability
import com.dji.sample.gasdetection.gas.remote.AutoFlightCapabilityProbe
import com.dji.sample.gasdetection.gas.remote.ChainStatus
import com.dji.sample.gasdetection.gas.remote.FloatingChainNode
import com.dji.sample.gasdetection.gas.remote.FloatingChannelLine
import com.dji.sample.gasdetection.gas.remote.FloatingJobAcceptModel
import com.dji.sample.gasdetection.gas.remote.FloatingViewModel
import com.dji.sample.gasdetection.gas.remote.FloatingWidgetController
import com.dji.sample.gasdetection.gas.autoflight.FlightAuthority
import com.dji.sample.gasdetection.gas.recognition.RecognitionController
import com.dji.sample.gasdetection.gas.recognition.RecognitionPhase
import com.dji.sample.gasdetection.gas.recognition.RecognitionPort
import com.dji.sample.gasdetection.gas.recognition.RecognitionPreconditions
import com.dji.sample.gasdetection.gas.recognition.RecognitionState
import com.dji.sample.gasdetection.gas.recognition.SdkRecognitionPort
import com.dji.sample.gasdetection.gas.recognition.VesselSearchBlock
import com.dji.sample.gasdetection.gas.service.ServiceHealthMonitor
import com.dji.sample.gasdetection.gas.task.GasJob
import com.dji.sample.gasdetection.gas.task.JobApiClient
import com.dji.sample.gasdetection.gas.task.JobStatus
import com.dji.sample.gasdetection.gas.task.PreflightEvaluator
import com.dji.sample.gasdetection.gas.task.TaskStage
import com.dji.sample.gasdetection.gas.task.TaskStore
import com.dji.sample.gasdetection.gas.task.TaskSyncState
import com.dji.sample.gasdetection.gas.task.TaskTexts
import com.dji.sample.gasdetection.gas.task.TaskUiState
import com.dji.sample.gasdetection.ui.AlertTexts
import com.dji.sample.gasdetection.ui.AutoFlightTexts
import com.dji.sample.gasdetection.ui.FollowTexts
import com.dji.sample.gasdetection.ui.VesselTexts
import dji.open.pilot.base.manager.ui.config.DialogButtonPosition
import dji.open.pilot.base.manager.ui.config.DialogConfig
import dji.open.pilot.base.manager.ui.config.DialogElementConfig
import dji.open.pilot.base.ui.NavigateAction
import dji.open.pilot.base.ui.model.OperationResult
import dji.open.pilot.client.manager.ui.NavigateManager
import dji.open.pilot.client.manager.ui.RemoteDialogManager
import dji.open.pilot.psdk.BridgeStage
import dji.open.pilot.psdk.PsdkBridge
import dji.open.pilot.psdk.PsdkConnections
import dji.sdk.keyvalue.key.DJIRemoteControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.payload.PayloadCenter
import dji.v5.manager.aircraft.payload.PayloadIndexType
import dji.v5.manager.intelligent.MissionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File
import kotlinx.coroutines.launch

/**
 * 插件进程的唯一 SDK 生命周期持有者。
 *
 * 设计意图：SDK 初始化/注册是进程级资源，集中在 Application 完成；Activity 订阅
 * SDK、连接与业务状态，并通过本类触发业务操作，不各自重复初始化。
 * 状态文案全部来自字符串资源，语言随系统/遥控器语言切换。
 */
class PsdkApplication : Application() {
    /** SDK 初始化、注册与连接状态文案，供入口页面直接展示。 */
    val sdkStatus = MutableLiveData<String>()

    /** Pilot 宿主是否已连接（`ServerState.CONNECTED`），是各 Manager 能力可用的前置条件。 */
    val pilotConnected = MutableLiveData(false)

    private val sdkRegisteredFlow = MutableStateFlow(false)
    private val pilotConnectedFlow = MutableStateFlow(false)
    private val aircraftConnectedFlow = MutableStateFlow(false)

    @Volatile
    private var bridge: PsdkBridge? = null

    /** 气体载荷接入服务；仅在 Pilot 连接期间存在。 */
    private var gasIngest: GasPayloadIngest? = null

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** 演示后端健康探测；进程级生命周期，入口页只订阅其状态、不参与调度。 */
    val serviceHealth = ServiceHealthMonitor()

    /** 接入状态的只读代理，供入口页与浮窗订阅；接入服务重建时保持同一对象。 */
    private val ingestStateProxy = MutableStateFlow(IngestState())
    private val payloadsProxy = MutableStateFlow<List<PayloadSnapshot>>(emptyList())
    private val diagnosticsProxy = MutableStateFlow(PayloadDiagnostics())
    private val measurementProxy = MutableSharedFlow<GasMeasurement>(extraBufferCapacity = 32)
    private val latestMeasurements = mutableMapOf<String, GasMeasurement>()

    /** 通道配置只加载一次；预检、载荷接入与监测/传输页共用，不依赖 Pilot 是否已连接。 */
    private val gasConfigResult: Result<GasConfig> by lazy {
        runCatching { GasConfigLoader(assets).load() }
            .onFailure { Log.e(TAG, "气体检测配置加载失败", it) }
    }

    private val gasConfig: GasConfig? get() = gasConfigResult.getOrNull()

    /** 配置加载失败原因；配置正常时为 `null`，供传输页显示「配置未加载」。 */
    val configError: String?
        get() = gasConfigResult.exceptionOrNull()?.let { it.message ?: it.javaClass.simpleName }

    /** 监测页聚合状态；无测量时为带目录通道的空快照。 */
    private val monitorStore by lazy {
        MonitorStore(configProvider = { gasConfig })
    }

    val monitorState: StateFlow<MonitorState> = monitorStore.state

    private var ingestCollectJob: Job? = null
    private var taskRenderJob: Job? = null
    private var taskPollJob: Job? = null
    private var alertTickerJob: Job? = null

    /** 任务级检测累计；任务切换时由聚合器自行复位。 */
    private val inspectionAggregator = InspectionAggregator()

    /** 记录上传队列状态；传输页订阅。 */
    private val recordStateProxy = MutableStateFlow<List<InspectionRecord>>(emptyList())
    val inspectionRecords: StateFlow<List<InspectionRecord>> = recordStateProxy.asStateFlow()

    private val inspectionScheduler = ExecutorInspectionScheduler()

    /** 检测记录队列：移植巡逻 Demo 的 AssetUploadManager 模型，文件与传输层按本模块适配。 */
    private val inspectionQueue: InspectionQueue by lazy {
        InspectionQueue(
            file = AtomicInspectionQueueFile(File(filesDir, "inspection-records/upload-queue.json")),
            transport = HttpInspectionTransport(rcSnProvider = ::currentRcSn),
            scheduler = inspectionScheduler,
            onStateChanged = { records -> recordStateProxy.value = records },
        )
    }

    /** 手动重试一条未上传记录。 */
    fun retryRecord(recordId: String) = inspectionQueue.retry(recordId)

    val gasIngestState: StateFlow<IngestState> = ingestStateProxy.asStateFlow()
    val gasPayloads: StateFlow<List<PayloadSnapshot>> = payloadsProxy.asStateFlow()
    val gasDiagnostics: StateFlow<PayloadDiagnostics> = diagnosticsProxy.asStateFlow()
    val gasMeasurements: Flow<GasMeasurement> = measurementProxy

    /** 通道目录摘要；配置未加载或解析失败时为 `null`，调用方不得猜测映射。 */
    fun catalogInfo(): CatalogInfo? = gasConfig?.catalog?.let { catalog ->
        CatalogInfo(version = catalog.version, channels = catalog.descriptors())
    }

    /** 数据过期阈值（毫秒）；配置未加载时为 `null`，界面不得用猜测值代替。 */
    val staleAfterMs: Long? get() = gasConfig?.staleAfterMs

    /** 查看并确认告警；非法状态返回 `false`，界面不做乐观更新。 */
    fun acknowledgeAlert(alertId: String): Boolean = alertCenter.acknowledge(alertId)

    /** 标记告警为异常处理中。 */
    fun markAlertUnderReview(alertId: String): Boolean = alertCenter.markUnderReview(alertId)

    /** 忽略告警提醒；同窗口内不会重复弹出。 */
    fun dismissAlert(alertId: String): Boolean = alertCenter.dismiss(alertId)

    private val capabilityProbe by lazy { AutoFlightCapabilityProbe() }

    /** 自动飞行网关；控制器与跟随控制器共用。 */
    private val autoFlightGateway by lazy { SdkAutoFlightGateway() }

    /** 自动飞行会话控制器：网关事件、看门狗与任务阶段在此汇合。 */
    private val autoFlightController: AutoFlightController by lazy {
        AutoFlightController(
            gateway = autoFlightGateway,
            session = AutoFlightSession(),
            stageSink = taskStore,
            scope = appScope,
        )
    }

    /** 自动飞行状态快照；入口页与浮窗共用。 */
    val autoFlightState: StateFlow<AutoFlightState> get() = autoFlightController.state

    /** 原生识别会话；负责开启看门狗、连续锁定、失锁与船名确认。 */
    private val recognitionController: RecognitionController by lazy {
        RecognitionController(port = SdkRecognitionPort(), scope = appScope)
    }

    /** 船舶识别状态快照；入口页与浮窗共用。 */
    val vesselRecognitionState: StateFlow<RecognitionState>
        get() = recognitionController.state

    /** 虚拟摇杆跟随会话；控制权确认、控制环与释放都收敛在控制器里。 */
    private val followController: FollowController by lazy {
        FollowController(
            gateway = autoFlightGateway,
            scope = appScope,
            recognition = { recognitionController.state.value },
            safety = { autoFlightGateway.snapshot() },
            stageSink = object : FollowStageSink {
                override fun beginFollow(): Boolean = taskStore.beginFollow()

                override fun handoverFollow() = taskStore.handoverFollow()
            },
            onStopped = { taskStore.handoverFollow() },
        )
    }

    /** 跟随状态快照；入口页与浮窗共用。 */
    val followState: StateFlow<FollowState> get() = followController.state

    private val preflightEvaluator = PreflightEvaluator(
        ingestState = { ingestStateProxy.value },
        channelConfigReady = { gasConfig != null },
        capability = { capabilityProbe.state.value },
        aircraftConnected = { aircraftConnectedFlow.value },
        safety = { autoFlightController.safetySnapshot(taskStore.state.value.job) },
    )

    /** 飞手确认后的自动飞行启动入口；返回 `false` 表示门控或阶段不允许。 */
    fun startAutoFlight(): Boolean {
        val job = taskStore.state.value.job ?: return false
        val target = job.target ?: return false
        val altitude = target.altitudeM?.toInt() ?: return false
        return autoFlightController.requestStart(
            AutoFlightTarget(
                latitude = target.latitude,
                longitude = target.longitude,
                altitudeM = target.altitudeM,
            ),
            altitude,
        )
    }

    /** 飞手主动退出自动飞行；跟随中先走虚拟摇杆释放。 */
    fun exitAutoFlight() {
        if (followState.value.isBusy) {
            followController.exit(FollowAbortReason.USER_EXIT)
        } else {
            autoFlightController.exitAuto()
        }
    }

    /**
     * 飞手在锁定目标后确认开始跟随。
     *
     * @return `null` 表示已开始建立控制；否则返回拒绝原因。
     */
    fun startFollow(): FollowBlock? {
        val task = taskStore.state.value
        if (task.stage != TaskStage.SEARCH || autoFlightState.value.phase != AutoFlightPhase.ARRIVED) {
            return FollowBlock.STAGE_NOT_READY
        }
        if (followState.value.isBusy) return FollowBlock.ALREADY_ACTIVE
        val recognition = recognitionController.state.value
        if (recognition.phase != RecognitionPhase.LOCKED || recognition.vessel == null) {
            return FollowBlock.NOT_LOCKED
        }
        val rect = recognition.targetRect ?: return FollowBlock.INVALID_RECT
        if (!FollowControlLaw.isNormalized(rect)) return FollowBlock.INVALID_RECT
        val seenAt = recognition.targetSeenAtMs ?: return FollowBlock.TARGET_STALE
        if (System.currentTimeMillis() - seenAt > FollowLimits.FRESHNESS_MS) {
            return FollowBlock.TARGET_STALE
        }
        val snapshot = autoFlightGateway.snapshot()
        if (!snapshot.aircraftConnected ||
            !snapshot.flying ||
            !snapshot.gpsReady ||
            snapshot.lowBattery ||
            snapshot.criticalLowBattery ||
            snapshot.fcTakeover ||
            !snapshot.missionIdle
        ) {
            return FollowBlock.SAFETY
        }
        if (snapshot.authority == FlightAuthority.OSDK ||
            snapshot.authority == FlightAuthority.AIRPORT ||
            snapshot.authority == FlightAuthority.AUTO_TEST
        ) {
            return FollowBlock.AUTHORITY_OCCUPIED
        }
        val job = task.job ?: return FollowBlock.STAGE_NOT_READY
        return if (followController.arm(job.jobId)) null else FollowBlock.ALREADY_ACTIVE
    }

    /** 飞手确认紧急停止。 */
    fun emergencyStopAutoFlight() = autoFlightController.requestEmergencyStop()

    /** 飞手确认已接管。 */
    fun confirmAutoFlightHandover() = autoFlightController.confirmHandover()

    /**
     * 飞手在到达后开始搜索船舶。
     *
     * @return `null` 表示已进入搜索；否则返回拒绝原因，供界面展示。
     */
    fun startVesselSearch(): VesselSearchBlock? {
        val job = taskStore.state.value.job ?: return VesselSearchBlock.STAGE_NOT_READY
        // 重复点击/重入时不再下发第二次开启命令。
        if (vesselRecognitionState.value.isSearching) return null
        if (autoFlightState.value.phase != AutoFlightPhase.ARRIVED) {
            recognitionController.reportBlock(VesselSearchBlock.NOT_ARRIVED)
            return VesselSearchBlock.NOT_ARRIVED
        }
        val block = recognitionController.canStart(currentRecognitionPreconditions())
        if (block != null) {
            recognitionController.reportBlock(block)
            return block
        }
        if (!taskStore.beginSearch()) {
            recognitionController.reportBlock(VesselSearchBlock.STAGE_NOT_READY)
            return VesselSearchBlock.STAGE_NOT_READY
        }
        recognitionController.start(job.jobId)
        return null
    }

    /** 识别不可用时由飞手重试。 */
    fun retryVesselSearch() = recognitionController.retry()

    /** 飞手确认当前锁定对象的船名或编号。 */
    fun confirmVesselName(nameOrNumber: String): Boolean =
        recognitionController.confirmVessel(nameOrNumber)

    /** 识别前置条件：连接、起飞与智能任务占用。 */
    private fun currentRecognitionPreconditions(): RecognitionPreconditions {
        val safety = autoFlightController.safetySnapshot(taskStore.state.value.job)
        val mission = capabilityProbe.runningMission.value
        return RecognitionPreconditions(
            connected = safety.aircraftConnected,
            flying = safety.flying,
            missionIdle = mission?.let { it == MissionType.NONE },
        )
    }

    /**
     * 统一的任务结束入口：先生成检测记录，再按阶段执行既有收尾。
     *
     * @return 生成的记录；没有任务或生成失败时为 `null`。
     */
    fun finishTask(): InspectionRecord? {
        val state = taskStore.state.value
        val job = state.job
        if (state.stage != TaskStage.PREP_READY &&
            state.stage != TaskStage.MANUAL &&
            state.stage != TaskStage.FLYTO &&
            state.stage != TaskStage.SEARCH &&
            state.stage != TaskStage.FOLLOW
        ) {
            return null
        }
        val record = job?.let { generateRecord(it, state.stage) }
        if (state.stage == TaskStage.FLYTO ||
            state.stage == TaskStage.SEARCH ||
            state.stage == TaskStage.FOLLOW
        ) {
            finishAutoFlightTask()
        } else {
            taskStore.finish()
        }
        return record
    }

    /** 生成并入队检测记录；重复结束按 jobId 复用已有记录。 */
    private fun generateRecord(
        job: com.dji.sample.gasdetection.gas.task.GasJob,
        stage: TaskStage,
    ): InspectionRecord? = runCatching {
        val snapshot = inspectionAggregator.snapshot(job.jobId)
        val alerts = alertState.value.alerts.filter { it.taskId == null || it.taskId == job.jobId }
        val existing = inspectionQueue.snapshot().firstOrNull { it.jobId == job.jobId }
        InspectionRecordBuilder.build(
            job = job,
            snapshot = snapshot,
            alerts = alerts,
            vessel = recognitionController.state.value.vessel,
            endedAtMs = System.currentTimeMillis(),
            endReason = if (stage == TaskStage.FOLLOW) {
                RecordEndReason.FOLLOW_EXIT_THEN_FINISH
            } else {
                RecordEndReason.PILOT_FINISH
            },
            existing = existing,
        ).also { record ->
            inspectionQueue.enqueue(record)
            Log.i(
                TAG,
                "record generated id=${record.recordId} job=${record.jobId} " +
                    "samples=${record.totalSamples} alerts=${record.alertIds.size}",
            )
        }
    }.onFailure { Log.e(TAG, "record build failed: ${it.message}") }.getOrNull()

    /** 到达或接管后结束任务：跟随先释放，再收掉自动会话并进入收尾。 */
    fun finishAutoFlightTask() {
        if (followState.value.isBusy) {
            followController.exit(FollowAbortReason.TASK_FINISHED) { taskStore.finish() }
        } else {
            autoFlightController.closeSession()
            taskStore.finish()
        }
    }

    /** 到达或接管后转为手动监测：跟随先释放，再收掉自动会话并进入手动。 */
    fun enterManualFromAutoFlight() {
        if (followState.value.isBusy) {
            followController.exit(FollowAbortReason.USER_EXIT) { taskStore.enterManual() }
        } else {
            autoFlightController.closeSession()
            taskStore.enterManual()
        }
    }

    /** 任务状态持有者；浮窗与入口页共用同一份。 */
    val taskStore: TaskStore by lazy {
        TaskStore(
            api = JobApiClient(rcSnProvider = ::currentRcSn),
            scope = appScope,
            preflightEvaluator = { preflightEvaluator.evaluate() },
        )
    }

    /** 告警中心：判定结果去重、状态流转与任务生命周期管理。 */
    private val alertCenter = AlertCenter()

    /** 告警快照；监测页与浮窗共用。 */
    val alertState: StateFlow<AlertUiState> = alertCenter.state

    /** 浮窗临时提示；导航失败等跨进程动作的结果在这里短暂展示。 */
    private val floatingNotice = MutableStateFlow<String?>(null)

    private val floatingController: FloatingWidgetController by lazy {
        FloatingWidgetController(
            context = this,
            onOpenDetail = { navigateToPluginApp() },
            onExitAuto = { quitAutoFlightFromFloating() },
            // 浮窗回调运行在 Binder 线程，任务操作统一切回主线程。
            onAcceptJob = { mainHandler.post { taskStore.accept() } },
            onRejectJob = { mainHandler.post { taskStore.reject() } },
            onSwitchPending = { offset -> mainHandler.post { selectAdjacentPendingJob(offset) } },
            onStartAuto = { showStartAutoConfirm() },
            onConfirmVessel = { showVesselConfirm() },
            onStartSearch = { startSearchFromFloating() },
            onRetrySearch = { mainHandler.post { retryVesselSearch() } },
            onFinish = { showFinishConfirm() },
        )
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    val sdkRegistered: StateFlow<Boolean> = sdkRegisteredFlow.asStateFlow()
    val pilotConnectedState: StateFlow<Boolean> = pilotConnectedFlow.asStateFlow()
    val aircraftConnected: StateFlow<Boolean> = aircraftConnectedFlow.asStateFlow()

    private val connections: PsdkConnections
        get() = PsdkConnections(
            sdkRegistered = sdkRegisteredFlow,
            pilotConnected = pilotConnectedFlow,
            aircraftConnected = aircraftConnectedFlow,
        )

    /** Opens (or reuses) the application-level protocol bridge for the selected port. */
    @Synchronized
    fun openBridge(port: PayloadIndexType, maxPacketBytes: Int): PsdkBridge {
        bridge?.let { existing ->
            if (existing.state.value.stage != BridgeStage.CLOSED &&
                existing.port == port && existing.maxPacketBytes == maxPacketBytes
            ) return existing
            existing.close()
            bridge = null
        }
        return PsdkBridge.getOrCreate(
            port = port,
            maxPacketBytes = maxPacketBytes,
            connections = connections,
        ).also { bridge = it }
    }

    @Synchronized
    fun closeBridge() {
        bridge?.close()
        bridge = null
    }

    /**
     * 启动气体载荷接入。
     *
     * 配置加载失败时不启动接入并记录原因，避免带着缺失通道目录去解码；重复调用复用同一服务。
     * 只在主线程调用（连接回调会投递到主线程）。
     */
    private fun startGasIngest() {
        gasIngest?.let {
            if (it.start() is IngestStartResult.Started) return
        }
        val config = gasConfig ?: run {
            Log.e(TAG, "气体检测配置加载失败，载荷接入未启动")
            return
        }
        val managers = PayloadCenter.getInstance().payloadManager
        val registry = PayloadAdapterRegistry().apply {
            managers.keys
                .filter { it != PayloadIndexType.EXTERNAL }
                .forEach { index ->
                    register(
                        PayloadPort(index.name, index.value()),
                        GasDemoFrameAdapter(config.catalog),
                    )
                }
        }
        val ingest = GasPayloadIngest(managers, registry)
        when (val result = ingest.start()) {
            is IngestStartResult.Started -> {
                gasIngest = ingest
                startIngestCollectors(ingest)
            }

            is IngestStartResult.Failed -> Log.e(TAG, "载荷接入启动失败：${result.reason}")
        }
    }

    /** 把接入服务的流转发到应用级代理，并在测量到达时刷新浮窗。 */
    private fun startIngestCollectors(ingest: GasPayloadIngest) {
        ingestCollectJob?.cancel()
        // 先放行聚合层再启动收集，避免首批测量因「尚未监听」被丢弃。
        monitorStore.setLink(true, ingest.state.value.onlinePayloads)
        ingestCollectJob = appScope.launch {
            launch {
                ingest.state.collect {
                    ingestStateProxy.value = it
                    monitorStore.setLink(it.started, it.onlinePayloads)
                    taskStore.onCapabilitiesChanged()
                }
            }
            launch { ingest.payloads.collect { payloadsProxy.value = it } }
            launch { ingest.diagnostics.collect { diagnosticsProxy.value = it } }
            launch { ingest.measurements.collect(::handleMeasurement) }
        }
    }

    /**
     * 测量统一入口。
     *
     * 锁定期间为样本补业务船舶标识；未锁定/已失锁时保持原样。
     */
    private fun handleMeasurement(measurement: GasMeasurement) {
        val enriched = recognitionController.attachTarget(measurement)
        val currentJobId = taskStore.state.value.job?.jobId
        currentJobId?.let { inspectionAggregator.onMeasurement(enriched, it) }
        recognitionController.state.value.vessel?.let(inspectionAggregator::onVessel)
        enriched.channels.forEach { latestMeasurements[it.channelId] = enriched }
        monitorStore.record(enriched)
        alertCenter.onMeasurement(enriched)
        renderFloating(taskStore.state.value, ingestStateProxy.value)
    }

    /** 停止气体载荷接入；只移除本服务注册的监听。 */
    private fun stopGasIngest() {
        ingestCollectJob?.cancel()
        ingestCollectJob = null
        gasIngest?.stop()
        gasIngest = null
        ingestStateProxy.value = IngestState()
        payloadsProxy.value = emptyList()
        diagnosticsProxy.value = PayloadDiagnostics()
        latestMeasurements.clear()
        monitorStore.setLink(false, 0)
    }

    /** Pilot 连接期间启动任务刷新、能力探测与浮窗渲染。 */
    private fun startTaskSide() {
        Log.d(TAG, "startTaskSide sn=${currentRcSn()}")
        capabilityProbe.start()
        autoFlightController.startMonitoring()
        followController.start()
        taskStore.refresh()
        // 周期拉取：首次拉取已在上方完成，循环首轮延迟一个间隔，避免重复请求。
        // 拉取失败由 TaskStore 保留任务并更新同步原因，不在这里中断循环。
        if (taskPollJob == null) {
            Log.i(TAG, "task polling started interval=${TASK_POLL_INTERVAL_MS}ms")
            taskPollJob = appScope.launch {
                while (isActive) {
                    delay(TASK_POLL_INTERVAL_MS)
                    taskStore.refresh()
                }
            }
        }
        if (alertTickerJob == null) {
            alertTickerJob = appScope.launch {
                while (isActive) {
                    delay(ALERT_TICK_MS)
                    alertCenter.onTick(staleAfterMs)
                }
            }
        }
        if (taskRenderJob == null) {
            taskRenderJob = appScope.launch {
                launch { capabilityProbe.state.collect { taskStore.onCapabilitiesChanged() } }
                launch {
                    ingestStateProxy.collect {
                        if (taskStore.state.value.stage == TaskStage.PREP ||
                            taskStore.state.value.stage == TaskStage.NOT_READY
                        ) {
                            taskStore.onCapabilitiesChanged()
                        }
                    }
                }
                launch {
                    alertState.collect { renderFloating(taskStore.state.value, ingestStateProxy.value) }
                }
                launch {
                    autoFlightState.collect {
                        renderFloating(taskStore.state.value, ingestStateProxy.value)
                    }
                }
                launch {
                    vesselRecognitionState.collect {
                        renderFloating(taskStore.state.value, ingestStateProxy.value)
                    }
                }
                launch {
                    combine(taskStore.state, autoFlightState) { task, auto ->
                        task.stage to auto.phase
                    }.distinctUntilChanged().collect { (stage, phase) ->
                        val searching = stage == TaskStage.SEARCH && phase != AutoFlightPhase.HANDOVER
                        if (!searching && vesselRecognitionState.value.isSearching) {
                            recognitionController.stop()
                        }
                    }
                }
                combine(taskStore.state, ingestStateProxy, payloadsProxy) { task, ingest, _ ->
                    task to ingest
                }.collect { (task, ingest) ->
                    alertCenter.onTask(task.job, task.stage)
                    renderFloating(task, ingest)
                }
            }
        }
    }

    /** Pilot 断开时停止探测、隐藏并释放浮窗，不保留旧能力结论。 */
    private fun stopTaskSide() {
        taskRenderJob?.cancel()
        taskRenderJob = null
        if (taskPollJob != null) {
            Log.i(TAG, "task polling stopped")
            taskPollJob?.cancel()
            taskPollJob = null
        }
        alertTickerJob?.cancel()
        alertTickerJob = null
        followController.reset()
        autoFlightController.stop()
        recognitionController.reset()
        // Pilot 断开时无法继续自动阶段，任务回到手动，避免重连后停留在无人管理的 FLYTO。
        taskStore.handoverAutoFlight()
        floatingController.hide()
        floatingController.release()
        capabilityProbe.stop()
    }

    /** 浮窗「退出自动」：浮窗在 Pilot 进程，回调线程不固定，统一切回主线程。 */
    private fun quitAutoFlightFromFloating() {
        mainHandler.post { autoFlightController.exitAuto() }
    }

    /** 显示一条浮窗临时提示，[durationMs] 后自动恢复；浮窗回调线程不固定，统一切回主线程。 */
    private fun showFloatingNotice(text: String, durationMs: Long = NOTICE_DURATION_MS) {
        mainHandler.post {
            floatingNotice.value = text
            renderFloating(taskStore.state.value, ingestStateProxy.value)
            mainHandler.postDelayed({
                if (floatingNotice.value == text) {
                    floatingNotice.value = null
                    renderFloating(taskStore.state.value, ingestStateProxy.value)
                }
            }, durationMs)
        }
    }

    /**
     * 渲染浮窗：未连接时隐藏；待接收任务展示接单态；无进行中任务展示入口态；其余展示摘要态。
     */
    private fun renderFloating(task: TaskUiState, ingest: IngestState) {
        if (!pilotConnectedFlow.value) {
            floatingController.hide()
            return
        }
        val job = task.job
        // 待接收：对齐 patrol 的工单列表态，优先于常规摘要（NEW_ORDER 也属于 hasActiveTask）。
        if (job != null && task.stage == TaskStage.NEW_ORDER) {
            renderFloatingJobAccept(task, job)
            return
        }
        // 没有进行中的任务时展示入口卡片，而不是整块隐藏：连接后浮窗常驻，任务结束回到入口态。
        if (!task.hasActiveTask || job == null) {
            floatingController.renderEntry(floatingNotice.value)
            return
        }
        val offline = ingest.onlinePayloads == 0
        val channels = job.channels.take(3).map { channel ->
            val latest = latestMeasurements[channel.channelId]
                ?.channels
                ?.firstOrNull { it.channelId == channel.channelId }
            FloatingChannelLine(
                name = channel.displayName.ifBlank { channel.channelId },
                value = latest?.value?.toString() ?: getString(R.string.gas_task_value_missing),
                unit = channel.unit,
            )
        }
        val topAlert = alertState.value.highestPriority()
        val alertChannelName = topAlert?.let { alert ->
            job.channels.firstOrNull { it.channelId == alert.channelId }
                ?.displayName
                ?.takeIf { it.isNotBlank() }
                ?: alert.channelId
                ?: getString(R.string.gas_channel_not_provided)
        }
        val alertLine = topAlert?.let {
            getString(
                R.string.gas_floating_alert,
                getString(AlertTexts.typeRes(it.type)),
                alertChannelName.orEmpty(),
            )
        } ?: getString(R.string.gas_floating_alert_none)
        val auto = autoFlightState.value
        val follow = followState.value
        val notice = floatingNotice.value
        val stageLine = notice ?: when {
            auto.phase == AutoFlightPhase.HANDOVER -> getString(
                R.string.gas_auto_status_with_reason,
                getString(AutoFlightTexts.phaseRes(auto.phase)),
                getString(AutoFlightTexts.authorityRes(auto.authority)),
                getString(AutoFlightTexts.handoverRes(auto.handoverReason)),
            )

            // 跟随阶段展示跟随状态与控制权，退出过程也同屏可见。
            task.stage == TaskStage.FOLLOW -> FollowTexts.statusLine(this, follow)
                ?: getString(TaskTexts.stageRes(task.stage))

            // 搜索阶段不展示 FlyTo 阶段的「已到达」，改展示任务阶段本身。
            task.stage == TaskStage.SEARCH -> getString(TaskTexts.stageRes(task.stage))
            !auto.isAutoActive -> getString(TaskTexts.stageRes(task.stage))
            else -> getString(
                R.string.gas_auto_status,
                getString(AutoFlightTexts.flyToStageRes(auto.flyToStage)),
                getString(AutoFlightTexts.authorityRes(auto.authority)),
            )
        }
        val vesselStatus = VesselTexts.floatingStatus(this, vesselRecognitionState.value)
        floatingController.renderSummary(
            FloatingViewModel(
                title = job.title,
                stage = stageLine,
                alert = alertLine,
                alertSeverity = topAlert?.type?.severity(),
                channels = channels,
                freshness = getString(
                    if (offline) R.string.gas_floating_offline else R.string.gas_floating_fresh
                ),
                offline = offline,
                autoActive = auto.isAutoActive || follow.isBusy,
                vesselStatus = vesselStatus,
                chain = buildChainNodes(task.stage, stageLine),
                autoStartReady = task.stage == TaskStage.PREP_READY &&
                    task.preflight?.allPassed == true,
                vesselConfirmable = task.stage == TaskStage.SEARCH &&
                    vesselRecognitionState.value.phase == RecognitionPhase.LOCKED,
                vesselLabel = vesselRecognitionState.value.vessel?.nameOrNumber,
                vesselSearchable = task.stage == TaskStage.FLYTO &&
                    auto.phase == AutoFlightPhase.ARRIVED,
                vesselRetryable = task.stage == TaskStage.SEARCH &&
                    vesselRecognitionState.value.phase == RecognitionPhase.DEGRADED,
                finishable = canFinish(task.stage),
            )
        )
    }

    /**
     * 把任务阶段映射为浮窗任务链的 5 个节点。
     *
     * 只读现有阶段，不引入阶段历史：`MANUAL` 无法区分接管来源，统一按「搜索采样」当前处理；
     * 状态短语只挂在焦点（当前/失败）节点上，其余节点不占副文案高度。
     */
    private fun buildChainNodes(stage: TaskStage, statusLine: String): List<FloatingChainNode> {
        val done = ChainStatus.DONE
        val current = ChainStatus.CURRENT
        val failed = ChainStatus.FAILED
        val pending = ChainStatus.PENDING
        val statuses = when (stage) {
            TaskStage.PREP, TaskStage.PREP_READY ->
                listOf(done, current, pending, pending, pending)

            TaskStage.NOT_READY ->
                listOf(done, failed, pending, pending, pending)

            TaskStage.FLYTO ->
                listOf(done, done, current, pending, pending)

            TaskStage.SEARCH, TaskStage.FOLLOW, TaskStage.MANUAL ->
                listOf(done, done, done, current, pending)

            TaskStage.FINALIZING ->
                listOf(done, done, done, done, current)

            // 待接收与空闲不渲染摘要态；返回空列表作为兜底。
            else -> return emptyList()
        }
        val labels = listOf(
            R.string.gas_chain_receive,
            R.string.gas_chain_prepare,
            R.string.gas_chain_flyto,
            R.string.gas_chain_sampling,
            R.string.gas_chain_finish,
        )
        return labels.mapIndexed { index, labelRes ->
            val status = statuses[index]
            FloatingChainNode(
                label = getString(labelRes),
                status = status,
                sub = if (status == ChainStatus.CURRENT || status == ChainStatus.FAILED) statusLine else null,
            )
        }
    }

    /**
     * 浮窗「确认船舶信息」：弹 Pilot 确认框，确认后调用既有确认函数。
     *
     * 浮窗只确认当前锁定的名称；名称编辑仍在 App 任务页。
     */
    private fun showVesselConfirm() {
        mainHandler.post {
            val state = vesselRecognitionState.value
            if (state.phase != RecognitionPhase.LOCKED) {
                showFloatingNotice(getString(R.string.gas_vessel_confirm_rejected))
                return@post
            }
            val config = DialogConfig.Builder()
                .setTitle(getString(R.string.gas_vessel_dialog_title))
                .setElementConfigs(
                    listOf(
                        DialogElementConfig().apply {
                            setInputHint(getString(R.string.gas_vessel_name_hint))
                            setInputPrefill(state.vessel?.nameOrNumber.orEmpty())
                        }
                    )
                )
                .setStartButtonText(getString(R.string.gas_auto_confirm_cancel))
                .setEndButtonText(getString(R.string.gas_vessel_dialog_save))
                .setOnMessage { message ->
                    if (message.buttonType == DialogButtonPosition.END) {
                        val name = message.inputTexts.firstOrNull().orEmpty()
                        mainHandler.post {
                            if (name.isBlank()) {
                                showFloatingNotice(getString(R.string.gas_vessel_name_required))
                            } else if (!confirmVesselName(name)) {
                                showFloatingNotice(getString(R.string.gas_vessel_confirm_rejected))
                            }
                        }
                    }
                }
                .build()
            val result = RemoteDialogManager.getInstance().showPromptDialog(config)
            if (result != OperationResult.SUCCESS) {
                Log.w(TAG, "vessel confirm dialog not shown: $result")
            }
        }
    }

    /** 浮窗开始搜索：复用既有门控，被拒绝时在浮窗给出原因。 */
    private fun startSearchFromFloating() {
        mainHandler.post {
            val block = startVesselSearch()
            if (block != null) {
                showFloatingNotice(VesselTexts.blockReason(this, block))
            }
        }
    }

    /** 浮窗结束任务：二次确认后复用既有结束流程。 */
    private fun showFinishConfirm() {
        mainHandler.post {
            if (!canFinish(taskStore.state.value.stage)) {
                showFloatingNotice(getString(R.string.gas_floating_finish_rejected))
                return@post
            }
            val config = DialogConfig.Builder()
                .setTitle(getString(R.string.gas_task_finish))
                .setMessage(getString(R.string.gas_floating_finish_confirm_message))
                .setStartButtonText(getString(R.string.gas_auto_confirm_cancel))
                .setEndButtonText(getString(R.string.gas_task_finish))
                .setOnStartButtonClick {
                    Log.d(TAG, "finish cancelled from floating window")
                }
                .setOnEndButtonClick {
                    mainHandler.post {
                        if (!canFinish(taskStore.state.value.stage)) {
                            showFloatingNotice(getString(R.string.gas_floating_finish_rejected))
                        } else {
                            finishTask()
                        }
                    }
                }
                .build()
            val result = RemoteDialogManager.getInstance().showConfirmDialog(config)
            if (result != OperationResult.SUCCESS) {
                Log.w(TAG, "finish confirm dialog not shown: $result")
            }
        }
    }

    /** 可结束阶段与 App 任务页保持一致。 */
    private fun canFinish(stage: TaskStage): Boolean = when (stage) {
        TaskStage.PREP_READY,
        TaskStage.MANUAL,
        TaskStage.FLYTO,
        TaskStage.SEARCH,
        TaskStage.FOLLOW -> true

        else -> false
    }

    /**
     * 浮窗「启动自动飞行」：先弹 Pilot 标准确认框，确认后调用既有启动门控。
     *
     * 确认框六个字段（标题/内容/两个按钮文案/两个回调）必须齐全，否则按钮空白且不报错；
     * 弹窗失败（如已有其它弹窗）不启动、不重试。
     */
    private fun showStartAutoConfirm() {
        mainHandler.post {
            val job = taskStore.state.value.job ?: return@post
            val target = job.target ?: return@post
            val height = target.altitudeM?.toInt() ?: return@post
            val config = DialogConfig.Builder()
                .setTitle(getString(R.string.gas_auto_confirm_title))
                .setMessage(
                    getString(
                        R.string.gas_auto_confirm_message,
                        target.latitude,
                        target.longitude,
                        height,
                    )
                )
                .setStartButtonText(getString(R.string.gas_auto_confirm_cancel))
                .setEndButtonText(getString(R.string.gas_auto_confirm_start))
                .setOnStartButtonClick {
                    Log.d(TAG, "auto start cancelled from floating window")
                }
                .setOnEndButtonClick {
                    mainHandler.post {
                        if (!startAutoFlight()) {
                            showFloatingNotice(getString(R.string.gas_auto_start_rejected))
                        }
                    }
                }
                .build()
            val result = RemoteDialogManager.getInstance().showConfirmDialog(config)
            if (result != OperationResult.SUCCESS) {
                Log.w(TAG, "auto start confirm dialog not shown: $result")
            }
        }
    }

    /**
     * 渲染待接收态：位置、阶段、任务标题、截止时间，以及操作失败提示。
     *
     * 待接收列表只取后端已下发的任务；位置按当前选中任务在其中的序号计算。
     */
    private fun renderFloatingJobAccept(task: TaskUiState, job: GasJob) {
        val pending = task.jobs.filter { it.status == JobStatus.DISPATCHED }
        val index = pending.indexOfFirst { it.jobId == job.jobId }.coerceAtLeast(0)
        // 接受/拒绝失败会落到 TaskSyncState.Failed；与导航失败提示共用同一行展示。
        val syncError = (task.sync as? TaskSyncState.Failed)?.let {
            TaskTexts.syncError(this, it.error)
        }
        floatingController.renderJobAccept(
            FloatingJobAcceptModel(
                position = index + 1,
                total = pending.size.coerceAtLeast(1),
                stage = getString(R.string.gas_floating_job_stage),
                title = job.title,
                deadlineLine = getString(
                    R.string.gas_task_deadline,
                    job.deadline ?: getString(R.string.gas_floating_job_deadline_none),
                ),
            ),
            notice = floatingNotice.value ?: syncError,
        )
    }

    /**
     * 待接收浮窗切换任务：在已下发任务中环形取相邻一项。
     *
     * 选中动作复用 [TaskStore.selectJob]，任务列表为空或目标与当前相同时不动作；切换被阶段
     * 规则拒绝时由 TaskStore 保持原任务，浮窗自然不变化。
     */
    private fun selectAdjacentPendingJob(offset: Int) {
        val state = taskStore.state.value
        val pending = state.jobs.filter { it.status == JobStatus.DISPATCHED }
        if (pending.isEmpty()) return
        val current = pending.indexOfFirst { it.jobId == state.job?.jobId }
            .let { if (it < 0) 0 else it }
        val target = pending[(current + offset + pending.size) % pending.size]
        if (target.jobId != state.job?.jobId) {
            taskStore.selectJob(target.jobId)
        }
    }

    /**
     * 遥控器 SN。
     *
     * 正常路径从 MSDK 读取；只有 Pilot 链路就绪时该 KeyValue 才可用。独立启动（演示/无宿主）
     * 时读取为空，此时回退到带 `DEMO-` 前缀的设备标识，便于本地演示后端隔离任务，
     * 日志中会标注回退来源，不冒充真实 SN。
     */
    private fun currentRcSn(): String? {
        val real = runCatching {
            KeyManager.getInstance().getValue(KeyTools.createKey(DJIRemoteControllerKey.KeySerialNumber))
        }.getOrNull()
        if (!real.isNullOrBlank()) return real
        val androidId = runCatching {
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()
        return if (androidId.isNullOrBlank()) null else "DEMO-${androidId.take(8)}"
    }

    /**
     * 浮窗打开插件入口页：跨进程用页面跳转，不在回调里启动 Activity。
     *
     * 使用 `SELF_DEFINE` 直接指向入口 Activity：部分 Pilot 版本对 `PLUGIN_APP` 返回
     * `EXECUTION_FAILED`，而入口页已声明 `exported=true` + `singleTask`，可稳定拉起。
     */
    private fun navigateToPluginApp() {
        mainHandler.post {
            NavigateManager.getInstance().navigateToPage(
                NavigateAction(NavigateAction.Value.SELF_DEFINE, MainActivity::class.java.name),
                object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        Log.d(TAG, "navigateToPluginApp success")
                    }

                    override fun onFailure(error: IDJIError) {
                        Log.e(TAG, "navigateToPluginApp failed: $error")
                        // 浮窗在 Pilot 进程，导航失败时在浮窗内给出可见提示，避免用户以为点击无效。
                        showFloatingNotice(getString(R.string.gas_floating_notice_nav_failed))
                    }
                },
            )
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        stopTaskSide()
        stopGasIngest()
        inspectionScheduler.shutdown()
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        com.cySdkyc.clx.Helper.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        sdkStatus.value = getString(R.string.gas_status_initializing)
        inspectionQueue.start()
        // 服务健康探测独立于 SDK 注册与 Pilot 连接：入口页标题栏需要随时表达服务是否可达。
        serviceHealth.start(appScope)
        SdkBootstrap.register(this, MainActivity::class.java.name, sdkHost)
    }

    /**
     * SDK 注册过程回调。只使用基本类型，避免 Application 类引用加固 AAR 中的类型，
     * 详见 [SdkBootstrap] 的类注释。
     */
    private val sdkHost = object : SdkBootstrap.Host {
        override fun onInitProgress(eventName: String, totalProcess: Int) {
            sdkStatus.postValue(getString(R.string.gas_status_init_progress, eventName, totalProcess))
        }

        override fun onRegisterSucceeded() {
            sdkRegisteredFlow.value = true
            sdkStatus.postValue(getString(R.string.gas_status_registered))
            // 任务拉取只依赖 SDK 注册与遥控器 SN，不要求 Pilot 已连接；
            // 浮窗仍由 Pilot 连接门控（见 renderFloating）。
            mainHandler.post { startTaskSide() }
        }

        override fun onRegisterFailed(reason: String) {
            sdkRegisteredFlow.value = false
            sdkStatus.postValue(getString(R.string.gas_status_register_failed, reason))
        }

        override fun onProductConnected(productId: Int) {
            aircraftConnectedFlow.value = true
            sdkStatus.postValue(getString(R.string.gas_status_aircraft_connected, productId))
        }

        override fun onProductDisconnected(productId: Int) {
            aircraftConnectedFlow.value = false
            sdkStatus.postValue(getString(R.string.gas_status_aircraft_disconnected, productId))
        }

        override fun onProductChanged(productId: Int) {
            sdkStatus.postValue(getString(R.string.gas_status_aircraft_changed, productId))
        }

        override fun onPilotStateChanged(connected: Boolean, stateName: String) {
            pilotConnectedFlow.value = connected
            pilotConnected.postValue(connected)
            sdkStatus.postValue(getString(R.string.gas_status_pilot, stateName))
            mainHandler.post {
                if (connected) {
                    startGasIngest()
                    // 浮窗常驻：连接后立即渲染（无任务=入口态），不依赖 ingest 流是否发射。
                    renderFloating(taskStore.state.value, ingestStateProxy.value)
                } else {
                    stopGasIngest()
                    // Pilot 断开后识别会话无法继续：释放订阅并把搜索阶段交还手动，
                    // 重连后必须由飞手重新点击「开始搜索船舶」。
                    followController.reset()
                    recognitionController.reset()
                    taskStore.handoverAutoFlight()
                }
            }
        }

        override fun onPilotIncompatible(reason: String) {
            sdkStatus.postValue(getString(R.string.gas_status_pilot_incompatible, reason))
        }
    }

    private companion object {
        private const val TAG = "GasPayloadIngest"

        /** 任务轮询间隔；与巡检/飞行任务 Demo 的 10 秒轮询对齐。 */
        private const val TASK_POLL_INTERVAL_MS = 10_000L

        /** 数据过期检查周期；5 秒粒度对秒级以上的过期阈值足够，且避免频繁唤醒。 */
        private const val ALERT_TICK_MS = 5_000L

        /** 浮窗临时提示展示时长。 */
        private const val NOTICE_DURATION_MS = 3_000L

        /** 无真实任务时识别模拟使用的演示任务标识（仅作 vesselId 前缀）。 */
        private const val SIMULATED_TASK_ID = "SIM-TASK"
    }
}
