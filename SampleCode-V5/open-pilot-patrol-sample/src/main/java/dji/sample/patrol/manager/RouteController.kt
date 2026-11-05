package dji.sample.patrol.manager

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import dji.sample.patrol.data.CategorizedException
import dji.sample.patrol.data.DetectionTarget
import dji.sample.patrol.data.Job
import dji.sample.patrol.data.JobStatus
import dji.sample.patrol.data.RecognitionFailureCategory
import dji.sample.patrol.data.ResultCallback
import dji.sample.patrol.data.RouteFailureCategory
import dji.sample.patrol.data.RouteSnapshot
import dji.sample.patrol.data.RouteState
import dji.sample.patrol.data.WaylineProgress
import dji.sample.patrol.data.WaylineState
import dji.sample.patrol.data.classifyRecognitionError
import dji.sdk.keyvalue.value.flightcontroller.GoHomeState
import dji.v5.common.error.DJIWaypointMissionError

private val NO_MATCH_PIPELINE: (DetectionTarget, ResultCallback<Unit>) -> Unit = { _, callback ->
    callback.onFailure(
        CategorizedException(RecognitionFailureCategory.NOT_READY, "Match pipeline is not connected"),
    )
}

/** 命中前置门禁结果：Ready 直接执行；Wait 先暂停并等待媒体任务结束；Reject 不暂停。 */
sealed class MatchGate {
    object Ready : MatchGate()
    data class Wait(val reason: String) : MatchGate()
    data class Reject(val reason: String) : MatchGate()
}

/** 命中过程中需要向 UI 反馈的问题类型。 */
/** 命中等待类提示（由 UI 层映射为中英文案）。 */
enum class MatchNotice { WAITING_MEDIA }

enum class MatchIssue {
    CAMERA_NOT_READY,
    MEDIA_WAIT_TIMEOUT,
    CAPTURE_NOT_ISSUED,
    CAPTURE_FAILED,
    RESUME_FAILED,
}

/**
 * 巡逻航线编排与状态订阅（design D4/D6）：
 * - 依赖框架 [WaylineManager] 的权威 Listener 驱动状态（经 [RouteActionPort]），不自定义发令时序；
 * - `startMission().onSuccess` 仅代表命令被接受，唯一业务终态为 `WaypointMissionExecuteState.FINISHED`
 *   （由 [WaylineManager] 映射为 [WaylineState.FINISHED] 后经 [RouteState.FINISHED] 判定）；
 * - 起飞必须显式确认并通过前置门控后才发令，前置不满足不发令；
 * - 精确 `remove*Listener`（由 [WaylineManager.release] 负责），禁止 `clearAll*Listener`。
 *
 * 端口 [actions]/[probe] 可注入，使状态机可脱离真机与 `dji.open.pilot.*` 单测。
 */
object RouteController {

    private const val TAG = "RouteController"

    /** 就绪门禁失败原因：区分“飞行器未连接”与“云端未配置”，避免归因错误。UI 层据常量映射本地化文案。 */
    const val REASON_AIRCRAFT_NOT_CONNECTED = "Aircraft is not connected"
    const val REASON_CLOUD_NOT_CONFIGURED = "Cloud is not configured"
    const val REASON_ROUTE_NOT_READY = "Route is not ready for takeoff"
    const val REASON_NOT_AIRBORNE = "Aircraft is not airborne"
    const val REASON_ALREADY_RETURNING = "Aircraft is already returning or landing"
    const val REASON_NOT_STOPPED = "Return home is only available after the route is stopped"

    /**
     * 加载/起飞被拒原因：飞行器上仍有未结束的航线任务（上一趟任务残留）。
     *
     * UI 层据该常量映射双语文案「飞行器上仍有未结束的航线任务，请先停止后再加载或起飞」；
     * 原始 SDK 错误码只保留在日志。
     */
    const val REASON_MISSION_RUNNING = "Aircraft has a running wayline mission"

    /**
     * 定位相关拒绝原因：飞机无有效定位、返航点未记录。
     *
     * UI 层据常量映射双语文案（等待定位 / 等待返航点刷新），原始原因串只保留在日志。
     */
    const val REASON_GPS_INVALID = "GPS_INVALID: aircraft location unavailable"
    const val REASON_HOME_POINT_NOT_RECORDED = "HOME_POINT_NOT_RECORDED"

    /** 飞机权威执行态：只有到达这些状态时，SDK 上报才能改变对外航线状态。 */
    private val EXECUTION_STATES = setOf(WaylineState.EXECUTING, WaylineState.PAUSED, WaylineState.FINISHED)

    /** 对外航线状态中的「已进入执行」集合：命令结果与加载就绪都不得覆盖这些状态。 */
    private val ROUTE_EXECUTION_STATES =
        setOf(RouteState.EXECUTING, RouteState.PAUSED, RouteState.FINISHED)

    /** 待自动重试的工单：仅用于「因飞行器未连接被拒」的一次性恢复。 */
    @Volatile
    private var aircraftRetryJob: Job? = null

    @Volatile
    private var aircraftRetryUsed = false

    /**
     * 本次加载开始时飞行器是否已被航线任务占用。
     *
     * 在 `actions.load(...)` 之前取样：此刻本工单还没开始上传，占用只可能来自上一趟残留任务。
     * 用「加载起点快照」而不是实时信号，避免本工单自己的 `UPLOADING`/`PREPARING` 把正常加载误判为残留。
     */
    private var residualMissionBusy = false

    /** 已把哪个工单置为「执行中」，避免命令回调重复置位。 */
    private var progressMarkedJobId: String? = null

    private val mutableRouteState = MutableLiveData(RouteState.ROUTE_PENDING)
    val routeState: LiveData<RouteState> = mutableRouteState

    /**
     * 航线展示唯一快照（状态 + 进度 + 错误 + 归属工单）。
     *
     * 设计意图：页面与浮窗只读该快照，避免分别观察多个 LiveData 时读到「新工单 + 旧进度」的中间态。
     * 旧的分项 LiveData 仍同步发布，仅用于兼容既有消费者与单测。
     */
    private val mutableSnapshot = MutableLiveData(RouteSnapshot())
    val snapshot: LiveData<RouteSnapshot> = mutableSnapshot

    /** 快照的线程安全组合基线：所有更新先在调用线程合并，再投递到主线程。 */
    @Volatile
    private var snapshotState = RouteSnapshot()

    private val mutableProgress = MutableLiveData(WaylineProgress())
    val progress: LiveData<WaylineProgress> = mutableProgress

    private val mutableError = MutableLiveData<String?>(null)
    val error: LiveData<String?> = mutableError

    private val mutableFailureCategory = MutableLiveData<RouteFailureCategory?>(null)
    val failureCategory: LiveData<RouteFailureCategory?> = mutableFailureCategory

    private val mutableInterruptReason = MutableLiveData<Throwable?>(null)
    val interruptReason: LiveData<Throwable?> = mutableInterruptReason

    private val mutableMatchFailure = MutableLiveData<RecognitionFailureCategory?>(null)
    val matchFailure: LiveData<RecognitionFailureCategory?> = mutableMatchFailure

    // 端口默认值惰性解析：避免 JVM 单测在对象初始化时就触碰生产 SDK 实现。
    @Volatile
    private var actionsOverride: RouteActionPort? = null
    var actions: RouteActionPort
        get() = actionsOverride ?: WaylineRouteActionPort
        set(value) {
            actionsOverride = value
        }

    @Volatile
    private var probeOverride: FlightStateProbe? = null
    var probe: FlightStateProbe
        get() = probeOverride ?: KeyManagerFlightStateProbe
        set(value) {
            probeOverride = value
        }

    /**
     * 返航端口：默认生产实现（惰性创建，避免 JVM 单测在对象初始化时触碰 SDK）；
     * JVM 单测通过 [configure] 替换为 fake。
     */
    private val defaultReturnHome: ReturnHomePort by lazy { KeyManagerReturnHomePort }

    @Volatile
    private var returnHomeOverride: ReturnHomePort? = null

    /** 返航端口是否已订阅；用于 release 时只在真正初始化后取消，避免 JVM 单测触碰 SDK。 */
    @Volatile
    private var returnHomeInitialized = false

    var returnHome: ReturnHomePort
        get() = returnHomeOverride ?: defaultReturnHome
        set(value) {
            returnHomeOverride = value
        }

    /** 返航权威状态（`KeyGoHomeStatus`）；仅用于展示与前置门禁，不改变航线状态。 */
    val goHomeState: LiveData<GoHomeState>
        get() = returnHome.state

    @Volatile
    var cloudConfigured: () -> Boolean = { CloudManager.isConfigured() }
    @Volatile
    var markInProgress: (Job) -> Unit = { TaskManager.start(it.jobId) }

    /**
     * 命中流水线（生产 = 拍照 + 取点 + 落点，design D3）；默认失败以免在未装配时误触发。
     * 成功表示「拍照落盘 + 取点有效」；地图落点失败由 D6 裁定不阻塞，不在此回调失败。
     */
    @Volatile
    var matchPipeline: (DetectionTarget, ResultCallback<Unit>) -> Unit = NO_MATCH_PIPELINE

    /** 命中失败裁定回调（保持暂停待人工处置，不自动 resume）。 */
    @Volatile
    var onMatchFailure: (RecognitionFailureCategory, String) -> Unit = { _, _ -> }

    /** 命中前置门禁；生产由 [MainApplication] 注入相机与媒体状态。 */
    @Volatile
    var matchGate: (DetectionTarget) -> MatchGate = { MatchGate.Ready }

    /** 媒体任务占用状态；生产接 [MediaManager.busy]。 */
    @Volatile
    var mediaBusy: LiveData<Boolean> = MutableLiveData(false)

    /** 等待媒体任务结束的上限（毫秒），仅作兜底，不替代状态判定。 */
    @Volatile
    var mediaWaitTimeoutMs: Long = 8_000L

    /** 等待超时的调度端口（JVM 单测可注入同步执行）。 */
    @Volatile
    var postDelayed: (Long, Runnable) -> Unit = { delay, action -> mainHandler.postDelayed(action, delay) }

    /** 取消等待超时的端口。 */
    @Volatile
    var removeCallbacks: (Runnable) -> Unit = { action -> mainHandler.removeCallbacks(action) }

    /** 命中问题反馈（供 UI 提示；原始错误另经 [onMatchFailure] 保留）。 */
    @Volatile
    var onMatchIssue: (MatchIssue, String) -> Unit = { _, _ -> }

    /** 等待类提示（null 表示清除），由 UI 层映射为本地化文案。 */
    @Volatile
    var onMatchNotice: (MatchNotice?) -> Unit = { }

    /** 命中被前置门禁拒绝（未暂停），供识别状态机复位 MATCHED。 */
    @Volatile
    var onMatchAborted: (String) -> Unit = { }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var observing = false
    private var currentJob: Job? = null

    /**
     * 唯一真相源入口：SDK 状态只作为输入。
     *
     * - `EXECUTING`/`PAUSED`/`FINISHED`：飞机是执行阶段的权威，直接同步；
     * - `READY`：不采信。就绪只能由本工单「下载 → 校验 → 推送成功」产生，避免残留任务点亮可起飞入口；
     * - `FAILED`（由 `NOT_SUPPORTED` 映射而来）及其余加载中状态：只在日志留痕，由本工单加载流程裁定。
     */
    private val stateObserver = Observer<WaylineState> { sdkState ->
        // 已停止是用户主动动作：迟到的 IDLE/INTERRUPTED/PAUSED 不得把中性态改回“可恢复”；
        // 只有外部重新发起执行（EXECUTING）才视为新的权威输入。重新加载会先置 ROUTE_PREPARING 解除门闩。
        if (mutableRouteState.value == RouteState.STOPPED && sdkState != WaylineState.EXECUTING) {
            Log.i(TAG, "ignore SDK state $sdkState while STOPPED")
            return@Observer
        }
        if (sdkState in EXECUTION_STATES) {
            Log.i(TAG, "adopt SDK execution state: $sdkState")
            update { it.copy(state = sdkState.toRouteState()) }
        } else {
            Log.i(TAG, "ignore SDK state $sdkState (route=${mutableRouteState.value})")
        }
    }

    private val progressObserver = Observer<WaylineProgress> { value ->
        update { it.copy(progress = value) }
    }

    private val errorObserver = Observer<String?> { message ->
        if (message != null) {
            update {
                it.copy(
                    error = message,
                    failureCategory = it.failureCategory ?: RouteFailureCategory.ROUTE_ERROR,
                )
            }
        }
    }

    private val interruptObserver = Observer<Throwable?> { reason ->
        if (reason != null) {
            update {
                it.copy(
                    error = reason.message,
                    failureCategory = RouteFailureCategory.ROUTE_ERROR,
                )
            }
            publish(mutableInterruptReason, reason)
        }
    }

    /** 在 Pilot 连接建立时调用：注册唯一状态出口的观察者与返航订阅（幂等）。 */
    @Synchronized
    fun attach() {
        ensureObserving()
        returnHome.init()
        returnHomeInitialized = true
    }

    /**
     * 新一轮执行开始时清除上一轮遗留的对外航线状态（纯本地复位，不发任何飞行指令）。
     *
     * 设计意图：同一 jobId 被重新下发/接受时，归属工单标识与上一轮相同，仅靠归属无法区分执行轮次；
     * 若不清除，上一轮的 `STOPPED`/失败会被当成当前状态，导致浮窗与页面矛盾、失败态回退错误。
     *
     * 能力契约：仅当「不是同一执行内的重新加载」时复位；同一执行（工单仍为 `IN_PROGRESS` 且归属相同）
     * 保持既有 `STOPPED` 语义不变。
     */
    @Synchronized
    fun startNewExecution(job: Job) {
        val sameExecution = snapshotState.jobId == job.jobId && job.status == JobStatus.IN_PROGRESS
        if (sameExecution) return
        Log.i(
            TAG,
            "reset for new execution: jobId=${job.jobId}, jobStatus=${job.status}, " +
                "previous=${mutableRouteState.value}",
        )
        aircraftRetryJob = null
        aircraftRetryUsed = false
        progressMarkedJobId = null
        residualMissionBusy = false
        update { RouteSnapshot(jobId = job.jobId) }
        publish(mutableInterruptReason, null)
        publish(mutableMatchFailure, null)
    }

    fun load(job: Job, callback: ResultCallback<Unit>? = null) {
        // 显式加载开启新的自动重试窗口（飞手点[加载航线]/[重试]后允许再次自动恢复）。
        aircraftRetryJob = null
        aircraftRetryUsed = false
        doLoad(job, callback, autoRetry = false)
    }

    private fun doLoad(job: Job, callback: ResultCallback<Unit>?, autoRetry: Boolean) {
        // 只有「同一执行内从已停止重新加载」才沿用 STOPPED 语义；新一轮执行不得继承上一轮状态。
        val fromStopped = mutableRouteState.value == RouteState.STOPPED && job.status == JobStatus.IN_PROGRESS
        val staleStopped = mutableRouteState.value == RouteState.STOPPED && !fromStopped
        val previousJobId = snapshotState.jobId
        currentJob = job
        clearFailure()
        // 归属切换必须一次性清掉上一工单的进度与错误，避免页面/浮窗读到「新工单 + 旧进度」。
        if (previousJobId != job.jobId) {
            update {
                RouteSnapshot(
                    jobId = job.jobId,
                    state = if (fromStopped) RouteState.STOPPED else RouteState.ROUTE_PENDING,
                )
            }
        } else {
            update {
                it.copy(
                    jobId = job.jobId,
                    progress = WaylineProgress(),
                    state = if (staleStopped) RouteState.ROUTE_PENDING else it.state,
                )
            }
        }
        ensureObserving()
        // 加载起点取样：本工单尚未上传，占用只可能来自飞行器上的残留任务（design D2）。
        residualMissionBusy = actions.aircraftBusy.value == true
        progressMarkedJobId = null
        Log.i(
            TAG,
            "route load start: jobId=${job.jobId}, autoRetry=$autoRetry, fromStopped=$fromStopped, " +
                "residualMissionBusy=$residualMissionBusy",
        )
        val loadFailureState = if (fromStopped) RouteState.STOPPED else RouteState.FAILED
        when (val source = RouteLoader.resolve(job)) {
            is RouteLoader.RouteSource.NotRequired -> {
                Log.i(TAG, "route load skipped: jobId=${job.jobId} has no required route")
                update { it.copy(state = RouteState.ROUTE_PENDING) }
                callback?.onSuccess(Unit)
            }
            is RouteLoader.RouteSource.Invalid ->
                fail(RouteFailureCategory.ROUTE_ERROR, source.reason, callback, loadFailureState)
            is RouteLoader.RouteSource.CloudProvided -> {
                if (!probe.connected) {
                    if (!autoRetry && !aircraftRetryUsed) {
                        aircraftRetryJob = job
                        Log.i(
                            TAG,
                            "aircraft not connected; schedule one auto retry on connection: jobId=${job.jobId}",
                        )
                    } else {
                        Log.w(TAG, "aircraft not connected; auto retry unavailable: jobId=${job.jobId}")
                    }
                    fail(RouteFailureCategory.NOT_READY, REASON_AIRCRAFT_NOT_CONNECTED, callback, loadFailureState)
                    return
                }
                if (!cloudConfigured()) {
                    fail(RouteFailureCategory.NOT_READY, REASON_CLOUD_NOT_CONFIGURED, callback, loadFailureState)
                    return
                }
                update { it.copy(state = RouteState.ROUTE_PREPARING) }
                actions.load(
                    RouteLoader.effectiveJob(job, source),
                    loadCallback(job, callback, loadFailureState),
                )
            }
        }
    }

    /**
     * 工单级加载结果：成功是 `ROUTE_READY` 的唯一来源；失败按调用来源收敛到 `FAILED`
     * 或回到 `STOPPED`（停止后重新加载），并保留原始错误。
     */
    private fun loadCallback(
        job: Job,
        callback: ResultCallback<Unit>?,
        loadFailureState: RouteState,
    ) = object : ResultCallback<Unit> {
        override fun onSuccess(data: Unit) {
            // 就绪门禁（design D2）：飞行器上已有未结束的航线任务时不得点亮起飞入口。
            // 1) 权威执行态优先：不把 EXECUTING/PAUSED/FINISHED 覆盖成 ROUTE_READY，也不改写成 FAILED；
            // 2) 加载起点已被占用（残留 PREPARING 等）：本次加载按失败收敛。
            val executing = mutableRouteState.value in ROUTE_EXECUTION_STATES
            if (residualMissionBusy || executing) {
                Log.w(
                    TAG,
                    "route ready blocked: jobId=${job.jobId}, residual=$residualMissionBusy, " +
                        "routeState=${mutableRouteState.value}",
                )
                if (executing) {
                    reportFailure(RouteFailureCategory.NOT_READY, REASON_MISSION_RUNNING, callback)
                } else {
                    fail(
                        RouteFailureCategory.NOT_READY,
                        REASON_MISSION_RUNNING,
                        callback,
                        loadFailureState,
                    )
                }
                return
            }
            Log.i(TAG, "route ready: jobId=${job.jobId}, waylines=${actions.waylineIds}")
            aircraftRetryJob = null
            update { it.copy(state = RouteState.ROUTE_READY) }
            callback?.onSuccess(Unit)
        }

        override fun onFailure(error: Throwable) {
            Log.w(TAG, "route load failed: jobId=${job.jobId}, error=${error.message}")
            fail(
                RouteFailureCategory.ROUTE_ERROR,
                error.message ?: "route load failed",
                callback,
                loadFailureState,
            )
        }
    }

    /** 飞行器连接恢复：对「因未连接被拒」的工单自动重试一次完整加载；不触发任何起飞。 */
    @Synchronized
    fun onAircraftConnected() {
        val job = aircraftRetryJob ?: return
        if (aircraftRetryUsed) return
        aircraftRetryUsed = true
        aircraftRetryJob = null
        Log.i(TAG, "aircraft connected; retry route load once: jobId=${job.jobId}")
        doLoad(job, null, autoRetry = true)
    }

    fun takeOff(job: Job, callback: ResultCallback<Unit>? = null) {
        clearFailure()
        val owner = snapshotState.jobId
        if (owner == null) {
            // 防御直接起飞路径：归属缺失时补齐为本次工单，保证快照与展示端可判定归属。
            update { it.copy(jobId = job.jobId) }
        } else if (owner != job.jobId) {
            Log.w(TAG, "takeoff rejected: jobId=${job.jobId}, owner=$owner")
            reject(RouteFailureCategory.NOT_READY, REASON_ROUTE_NOT_READY, callback)
            return
        }
        if (mutableRouteState.value != RouteState.ROUTE_READY) {
            Log.w(TAG, "takeoff rejected: jobId=${job.jobId}, routeState=${mutableRouteState.value}")
            reject(RouteFailureCategory.NOT_READY, REASON_ROUTE_NOT_READY, callback)
            return
        }
        preflightFailure()?.let { (category, message) ->
            Log.w(TAG, "takeoff preflight rejected: jobId=${job.jobId}, reason=$message")
            reject(category, message, callback)
            return
        }
        Log.i(TAG, "takeoff preflight passed: jobId=${job.jobId}; sending startMission")
        actions.takeOff(object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) = runOnMain {
                // 命令被接受：保持 EXECUTING，由 WaypointMissionExecuteStateListener 驱动直至 FINISHED。
                // 工单只在**本次起飞被接受**后才置为执行中（design D3）。
                Log.i(TAG, "startMission accepted: jobId=${job.jobId}")
                markInProgressOnce(job)
                update { it.copy(state = RouteState.EXECUTING) }
                callback?.onSuccess(Unit)
            }

            override fun onFailure(error: Throwable) = runOnMain {
                // 命令结果服从权威状态（design D1）：失败回调不得覆盖执行态，也不得伪造失败展示。
                val state = mutableRouteState.value
                val waylineRunning = (error as? WaylineFailure)?.sdkError
                    ?.isError(DJIWaypointMissionError.CANNOT_START_WAYLINE_WHEN_WAYLINE_RUNNING) == true
                Log.w(
                    TAG,
                    "startMission failed: jobId=${job.jobId}, routeState=$state, " +
                        "waylineRunning=$waylineRunning, error=$error",
                )
                when {
                    // 1) 权威 Listener 已推进到执行态：不改状态、不置工单执行中、不展示为业务失败。
                    state in ROUTE_EXECUTION_STATES -> callback?.onSuccess(Unit)
                    // 2) 飞行器上已有航线任务在执行：幂等，保持原状态，不重发、不自动 stopMission。
                    waylineRunning -> reportFailure(
                        RouteFailureCategory.NOT_READY,
                        REASON_MISSION_RUNNING,
                        callback,
                    )
                    // 3) 真实失败（飞行器未进入执行态）：按失败收敛，工单保持「已受理」。
                    else -> fail(RouteFailureCategory.ROUTE_ERROR, error.message ?: "startMission failed", callback)
                }
            }
        })
    }

    fun pause(callback: ResultCallback<Unit>? = null) = actions.pause(callback)

    fun resume(callback: ResultCallback<Unit>? = null) = actions.resume(callback)

    fun stop(callback: ResultCallback<Unit>? = null) {
        actions.stop(object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) = runOnMain {
                Log.i(TAG, "route stopped: jobId=${currentJob?.jobId}, from=${mutableRouteState.value}")
                update {
                    it.copy(
                        state = RouteState.STOPPED,
                        failureCategory = RouteFailureCategory.CANCELLED,
                    )
                }
                callback?.onSuccess(Unit)
            }

            override fun onFailure(error: Throwable) = runOnMain {
                // 航线本来就没在运行：停止是幂等的，按已停止处理。
                // 错误码可能带模块前缀，必须用 SDK 的后缀匹配语义判定，不做字符串等值/包含。
                val notRunning = (error as? WaylineFailure)?.sdkError
                    ?.isError(DJIWaypointMissionError.CANNOT_STOP_WAYLINE_WHEN_WAYLINE_NOT_RUNNING) == true
                if (notRunning) {
                    Log.i(TAG, "route already not running; treat stop as idempotent success")
                    update {
                        it.copy(
                            state = RouteState.STOPPED,
                            failureCategory = RouteFailureCategory.CANCELLED,
                        )
                    }
                    callback?.onSuccess(Unit)
                } else {
                    // 停止失败不改状态：飞手仍需以原执行/暂停态处置，绝不伪造已停止。
                    reportFailure(
                        RouteFailureCategory.ROUTE_ERROR,
                        error.message ?: "stopMission failed",
                        callback,
                    )
                }
            }
        })
    }

    /** 当前是否具备返航条件；浮窗据此显隐[返航]（不可用时用 GONE，不渲染必失败入口）。 */
    fun canReturnHome(): Boolean =
        mutableRouteState.value == RouteState.STOPPED && returnHomePreflightFailure() == null

    /**
     * 请求返航（高风险·飞行安全）。
     *
     * 设计意图：只在用户主动停止后的 `STOPPED` 态提供；UI 必须先弹 Pilot 原生确认框，
     * 确认后才调用本方法。动作接受不代表已返航，权威终态由 `KeyGoHomeStatus` 决定；
     * 失败只记录错误并保持 `STOPPED`，不自动重试、不自动降落。
     */
    fun requestReturnHome(callback: ResultCallback<Unit>? = null) {
        if (mutableRouteState.value != RouteState.STOPPED) {
            Log.w(TAG, "return home rejected: state=${mutableRouteState.value}")
            reject(RouteFailureCategory.NOT_READY, REASON_NOT_STOPPED, callback)
            return
        }
        returnHomePreflightFailure()?.let { (category, message) ->
            Log.w(TAG, "return home preflight rejected: $message")
            reject(category, message, callback)
            return
        }
        Log.i(TAG, "return home requested: jobId=${currentJob?.jobId}, goHome=${goHomeState.value}")
        returnHomeInitialized = true
        returnHome.start(object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) = runOnMain {
                Log.i(TAG, "return home accepted")
                callback?.onSuccess(Unit)
            }

            override fun onFailure(error: Throwable) = runOnMain {
                Log.w(TAG, "return home failed: ${error.message}")
                reportFailure(
                    RouteFailureCategory.ROUTE_ERROR,
                    error.message ?: "startGoHome failed",
                    callback,
                )
            }
        })
    }

    /** 返航前置门禁：全部条件满足才允许下发动作。 */
    private fun returnHomePreflightFailure(): Pair<RouteFailureCategory, String>? {
        if (!probe.connected) return RouteFailureCategory.NOT_READY to REASON_AIRCRAFT_NOT_CONNECTED
        if (!probe.isFlying) return RouteFailureCategory.NOT_READY to REASON_NOT_AIRBORNE
        val goHome = goHomeState.value
        val alreadyReturning = probe.flightMode == FlightMode.GO_HOME ||
            probe.flightMode == FlightMode.AUTO_LANDING ||
            goHome == GoHomeState.RETURNING_TO_HOME ||
            goHome == GoHomeState.LANDING
        if (alreadyReturning) return RouteFailureCategory.NOT_READY to REASON_ALREADY_RETURNING
        if (probe.location == null) {
            return RouteFailureCategory.NOT_READY to REASON_GPS_INVALID
        }
        if (probe.homeLocation == null) {
            return RouteFailureCategory.NOT_READY to REASON_HOME_POINT_NOT_RECORDED
        }
        return null
    }

    /**
     * 命中编排（design D1-D3）：仅 `EXECUTING` 时执行。
     *
     * `Ready` → 暂停 → 拍照 → 快门接受即恢复；`Wait` → 暂停 → 等媒体任务结束 → 再拍照；
     * `Reject` → 不暂停。拍照命令未下发即失败时自动恢复，快门已下发后失败保持暂停待处置。
     */
    fun handleMatch(target: DetectionTarget, callback: ResultCallback<Unit>? = null) {
        if (mutableRouteState.value != RouteState.EXECUTING) {
            reject(RouteFailureCategory.NOT_READY, "Route is not executing", callback)
            return
        }
        when (val gate = matchGate(target)) {
            is MatchGate.Reject -> {
                Log.w(TAG, "match gate rejected: ${gate.reason}")
                onMatchIssue(MatchIssue.CAMERA_NOT_READY, gate.reason)
                onMatchAborted(gate.reason)
                recordMatchFailure(RecognitionFailureCategory.NOT_READY, gate.reason)
                callback?.onFailure(IllegalStateException(gate.reason))
                return
            }
            is MatchGate.Wait -> Log.i(TAG, "match gate wait: ${gate.reason}")
            MatchGate.Ready -> Log.i(TAG, "match gate passed")
        }
        clearFailure()
        onMatchNotice(null)
        actions.pause(object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) = runOnMain {
                Log.i(TAG, "match pause accepted")
                // 暂停后再评估一次，覆盖「门禁通过到暂停之间」媒体开始占用的情况。
                when (val gate = matchGate(target)) {
                    is MatchGate.Reject -> {
                        Log.w(TAG, "match gate rejected after pause: ${gate.reason}")
                        onMatchIssue(MatchIssue.CAMERA_NOT_READY, gate.reason)
                        resumeAfterUnissuedCapture(gate.reason, callback)
                    }
                    is MatchGate.Wait -> awaitMediaIdle(target, callback)
                    MatchGate.Ready -> runMatchPipeline(target, callback)
                }
            }

            override fun onFailure(error: Throwable) = runOnMain {
                onMatchIssue(MatchIssue.CAMERA_NOT_READY, error.message ?: "pause failed")
                onMatchFailure(RecognitionFailureCategory.NOT_READY, error.message ?: "pause failed")
                recordMatchFailure(RecognitionFailureCategory.NOT_READY, error.message ?: "pause failed")
                callback?.onFailure(IllegalStateException(error.message ?: "pause failed"))
            }
        })
    }

    /** 等待媒体任务结束；事件驱动，超时兜底后按「拍照未下发」恢复。 */
    private fun awaitMediaIdle(target: DetectionTarget, callback: ResultCallback<Unit>?) {
        val source = mediaBusy
        if (source.value != true) {
            afterMediaWait(target, callback)
            return
        }
        val startedAt = System.currentTimeMillis()
        Log.i(TAG, "media wait start: timeout=${mediaWaitTimeoutMs}ms")
        onMatchNotice(MatchNotice.WAITING_MEDIA)
        var finished = false
        var observer: Observer<Boolean>? = null
        val timeout = Runnable {
            if (finished) return@Runnable
            finished = true
            observer?.let(source::removeObserver)
            Log.w(TAG, "media wait timeout: elapsed=${System.currentTimeMillis() - startedAt}ms")
            onMatchIssue(MatchIssue.MEDIA_WAIT_TIMEOUT, "媒体同步等待超时")
            resumeAfterUnissuedCapture("媒体同步等待超时", callback)
        }
        val obs = Observer<Boolean> { busy ->
            if (busy == true || finished) return@Observer
            finished = true
            observer?.let(source::removeObserver)
            removeCallbacks(timeout)
            Log.i(TAG, "media wait end: elapsed=${System.currentTimeMillis() - startedAt}ms")
            afterMediaWait(target, callback)
        }
        observer = obs
        source.observeForever(obs)
        postDelayed(mediaWaitTimeoutMs, timeout)
    }

    /** 媒体任务结束后重新评估门禁，再决定拍照或恢复。 */
    private fun afterMediaWait(target: DetectionTarget, callback: ResultCallback<Unit>?) {
        onMatchNotice(null)
        when (val gate = matchGate(target)) {
            is MatchGate.Reject -> {
                Log.w(TAG, "match gate rejected after media wait: ${gate.reason}")
                onMatchIssue(MatchIssue.CAMERA_NOT_READY, gate.reason)
                resumeAfterUnissuedCapture(gate.reason, callback)
            }
            is MatchGate.Wait -> {
                Log.w(TAG, "media still busy after wait: ${gate.reason}")
                onMatchIssue(MatchIssue.MEDIA_WAIT_TIMEOUT, gate.reason)
                resumeAfterUnissuedCapture(gate.reason, callback)
            }
            MatchGate.Ready -> {
                Log.i(TAG, "match gate passed after media wait")
                runMatchPipeline(target, callback)
            }
        }
    }

    private fun runMatchPipeline(target: DetectionTarget, callback: ResultCallback<Unit>?) {
        matchPipeline(target, object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) = runOnMain { resumeAfterCapture(callback) }

            override fun onFailure(error: Throwable) = runOnMain {
                if ((error as? CaptureFailure)?.phase == CapturePhase.PRE_FLIGHT) {
                    Log.w(TAG, "capture not issued: ${error.message}")
                    onMatchIssue(MatchIssue.CAPTURE_NOT_ISSUED, error.message ?: "capture not issued")
                    resumeAfterUnissuedCapture(error.message ?: "capture not issued", callback)
                } else {
                    val category = classifyRecognitionError(error)
                    Log.w(TAG, "capture failed after shutter: ${error.message}")
                    onMatchIssue(MatchIssue.CAPTURE_FAILED, error.message ?: "capture failed")
                    onMatchFailure(category, error.message ?: "match pipeline failed")
                    recordMatchFailure(category, error.message ?: "match pipeline failed")
                    callback?.onFailure(error)
                }
            }
        })
    }

    private fun resumeAfterCapture(callback: ResultCallback<Unit>?) {
        actions.resume(object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) = runOnMain {
                Log.i(TAG, "resume accepted after capture")
                callback?.onSuccess(Unit)
            }

            override fun onFailure(error: Throwable) = runOnMain {
                Log.w(TAG, "resume failed after capture: ${error.message}")
                onMatchIssue(MatchIssue.RESUME_FAILED, error.message ?: "resume failed")
                onMatchFailure(RecognitionFailureCategory.NOT_READY, error.message ?: "resume failed")
                recordMatchFailure(RecognitionFailureCategory.NOT_READY, error.message ?: "resume failed")
                callback?.onFailure(IllegalStateException(error.message ?: "resume failed"))
            }
        })
    }

    /** 拍照未下发（或等待超时）时恢复航线；恢复失败仍保持暂停待处置。 */
    private fun resumeAfterUnissuedCapture(reason: String, callback: ResultCallback<Unit>?) {
        Log.i(TAG, "resume after unissued capture: $reason")
        onMatchNotice(null)
        actions.resume(object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) = runOnMain {
                Log.i(TAG, "resume accepted after unissued capture")
                callback?.onSuccess(Unit)
            }

            override fun onFailure(error: Throwable) = runOnMain {
                Log.w(TAG, "resume failed after unissued capture: ${error.message}")
                onMatchIssue(MatchIssue.RESUME_FAILED, error.message ?: "resume failed")
                onMatchFailure(RecognitionFailureCategory.NOT_READY, error.message ?: "resume failed")
                recordMatchFailure(RecognitionFailureCategory.NOT_READY, error.message ?: "resume failed")
                callback?.onFailure(IllegalStateException(error.message ?: "resume failed"))
            }
        })
    }

    fun release() {
        stopObserving()
        if (returnHomeInitialized) {
            returnHome.release()
            returnHomeInitialized = false
        }
        actions.release()
        currentJob = null
        aircraftRetryJob = null
        aircraftRetryUsed = false
        reset()
    }

    /** 测试注入点：替换端口并复位状态（生产由 [MainApplication] 组装默认实现）。 */
    fun configure(
        actionPort: RouteActionPort,
        stateProbe: FlightStateProbe,
        configured: () -> Boolean = { true },
        inProgress: (Job) -> Unit = {},
        returnHomePort: ReturnHomePort? = null,
    ) {
        stopObserving()
        actions = actionPort
        probe = stateProbe
        cloudConfigured = configured
        markInProgress = inProgress
        // 仅释放测试注入的端口；生产默认端口由 release() 负责，避免 JVM 单测触碰 SDK。
        returnHomeOverride?.release()
        returnHomeOverride = returnHomePort
        returnHomeInitialized = false
        matchPipeline = NO_MATCH_PIPELINE
        onMatchFailure = { _, _ -> }
        currentJob = null
        aircraftRetryJob = null
        aircraftRetryUsed = false
        reset()
    }

    private fun preflightFailure(): Pair<RouteFailureCategory, String>? = when {
        !probe.connected -> RouteFailureCategory.NOT_READY to REASON_AIRCRAFT_NOT_CONNECTED
        !cloudConfigured() -> RouteFailureCategory.NOT_READY to REASON_CLOUD_NOT_CONFIGURED
        probe.location == null -> RouteFailureCategory.NOT_READY to REASON_GPS_INVALID
        probe.homeLocation == null -> RouteFailureCategory.NOT_READY to REASON_HOME_POINT_NOT_RECORDED
        probe.flightMode == FlightMode.GO_HOME || probe.flightMode == FlightMode.AUTO_LANDING ->
            RouteFailureCategory.NOT_READY to "Flight mode blocks takeoff"
        actions.waylineIds.none { it >= 0 } ->
            RouteFailureCategory.ROUTE_ERROR to "No available wayline id >= 0"
        else -> null
    }

    private fun ensureObserving() {
        if (observing) return
        actions.state.observeForever(stateObserver)
        actions.progress.observeForever(progressObserver)
        actions.error.observeForever(errorObserver)
        actions.interruptReason.observeForever(interruptObserver)
        observing = true
    }

    private fun stopObserving() {
        if (!observing) return
        actions.state.removeObserver(stateObserver)
        actions.progress.removeObserver(progressObserver)
        actions.error.removeObserver(errorObserver)
        actions.interruptReason.removeObserver(interruptObserver)
        observing = false
    }

    /**
     * 记录航线失败，并按 [fallbackState] 收敛对外状态。
     *
     * @param fallbackState `null` 表示保持当前状态（停止失败等场景）；默认收敛到 `FAILED`；
     * “已停止态下重新加载失败”传 `STOPPED`，保证中性态不被失败态覆盖。
     */
    private fun fail(
        category: RouteFailureCategory,
        message: String,
        callback: ResultCallback<Unit>?,
        fallbackState: RouteState? = RouteState.FAILED,
    ) {
        reportFailure(category, message, callback)
        if (fallbackState != null) update { it.copy(state = fallbackState) }
    }

    /** 只发布错误与分类，不改变航线状态；供“命令失败但需保持原状态”的路径复用。 */
    private fun reportFailure(
        category: RouteFailureCategory,
        message: String,
        callback: ResultCallback<Unit>?,
    ) {
        Log.w(TAG, "route failure reported: category=$category, message=$message, state=${mutableRouteState.value}")
        update { it.copy(failureCategory = category, error = message) }
        callback?.onFailure(IllegalStateException(message))
    }

    private fun reject(category: RouteFailureCategory, message: String, callback: ResultCallback<Unit>?) {
        Log.w(TAG, "route rejected: category=$category, state=${mutableRouteState.value}, message=$message")
        update {
            it.copy(
                failureCategory = category,
                error = message,
                state = if (category == RouteFailureCategory.ROUTE_ERROR) RouteState.FAILED else it.state,
            )
        }
        callback?.onFailure(IllegalStateException(message))
    }

    private fun clearFailure() {
        update { it.copy(error = null, failureCategory = null) }
        publish(mutableMatchFailure, null)
    }

    /**
     * 把工单置为「执行中」，同一工单只置位一次。
     *
     * 设计意图（design D3）：工单状态只由**本次起飞指令被接受**驱动；命令失败时不得因为飞行器
     * 上残留任务处于执行态就把本工单标成执行中。去重避免重复回调或重入导致重复请求云端。
     */
    private fun markInProgressOnce(job: Job) {
        if (progressMarkedJobId == job.jobId) return
        progressMarkedJobId = job.jobId
        Log.i(TAG, "job marked in progress: jobId=${job.jobId}")
        markInProgress(job)
    }

    /** 命中失败/恢复失败：保留错误与分类，**不**改变航线状态（保持暂停待处置）。 */
    private fun recordMatchFailure(category: RecognitionFailureCategory, message: String) {
        publish(mutableMatchFailure, category)
        update { it.copy(failureCategory = RouteFailureCategory.ROUTE_ERROR, error = message) }
    }

    private fun reset() {
        residualMissionBusy = false
        progressMarkedJobId = null
        update { RouteSnapshot() }
        publish(mutableInterruptReason, null)
        publish(mutableMatchFailure, null)
    }

    private fun WaylineState.toRouteState(): RouteState = when (this) {
        WaylineState.IDLE -> RouteState.ROUTE_PENDING
        WaylineState.PREPARING -> RouteState.ROUTE_PREPARING
        WaylineState.READY -> RouteState.ROUTE_READY
        WaylineState.EXECUTING -> RouteState.EXECUTING
        WaylineState.PAUSED -> RouteState.PAUSED
        WaylineState.FINISHED -> RouteState.FINISHED
        WaylineState.FAILED -> RouteState.FAILED
        WaylineState.STOPPED -> RouteState.STOPPED
    }

    /**
     * 组合更新航线快照：状态、进度、错误与归属在同一份不可变数据中切换。
     *
     * 设计意图：调用方可能来自 Binder/SDK 线程，先在当前线程用 [snapshotState] 合并，
     * 再经 [publish] 投递；页面与浮窗观察 [snapshot] 时不会看到半更新状态。
     */
    private fun update(transform: (RouteSnapshot) -> RouteSnapshot) {
        val next = synchronized(this) {
            transform(snapshotState).also { snapshotState = it }
        }
        publish(mutableSnapshot, next)
        if (mutableRouteState.value != next.state) publish(mutableRouteState, next.state)
        if (mutableProgress.value != next.progress) publish(mutableProgress, next.progress)
        if (mutableError.value != next.error) publish(mutableError, next.error)
        if (mutableFailureCategory.value != next.failureCategory) {
            publish(mutableFailureCategory, next.failureCategory)
        }
    }

    private fun <T> publish(liveData: MutableLiveData<T>, value: T) {
        if (Looper.myLooper() == Looper.getMainLooper()) liveData.value = value else liveData.postValue(value)
    }

    private fun runOnMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action() else mainHandler.post(action)
    }
}
