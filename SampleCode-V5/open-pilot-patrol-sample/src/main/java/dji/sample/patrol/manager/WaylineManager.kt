package dji.sample.patrol.manager

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dji.sample.patrol.MainApplication
import dji.sample.patrol.data.Asset
import dji.sample.patrol.data.AssetState
import dji.sample.patrol.data.GeoPoint3D
import dji.sample.patrol.data.Job
import dji.sample.patrol.data.RequestHandle
import dji.sample.patrol.data.ResultCallback
import dji.sample.patrol.data.RouteProgressStage
import dji.sample.patrol.data.WaylineProgress
import dji.sample.patrol.data.WaylineState
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.flightcontroller.FlightMode
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.waypoint3.WaylineExecutingInfoListener
import dji.v5.manager.aircraft.waypoint3.WaypointMissionExecuteStateListener
import dji.v5.manager.aircraft.waypoint3.WaypointMissionManager
import dji.v5.manager.aircraft.waypoint3.WPMZParserManager
import dji.v5.manager.aircraft.waypoint3.model.BreakPointInfo
import dji.v5.manager.aircraft.waypoint3.model.WaylineExecutingInfo
import dji.v5.manager.aircraft.waypoint3.model.WaypointMissionExecuteState
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.Executors

/**
    * 航线文件准备、导入和飞行器端执行的统一入口。
    *
    * 命令结果通过 [ResultCallback] 返回；[state] 和 [progress] 只描述持续的航线状态。
    */
object WaylineManager {

    private const val TAG = "WaylineManager"

    /**
     * 「不宜中断航线」的飞行阶段集合。
     *
     * 设计意图：飞控只在飞机已离地并进入航线飞行后才接受航线中断。真机实测：任务被接受后约
     * 1.5 s 飞机仍在地面（`MOTOR_START`）时调用暂停，飞控返回 `CANNOT_BREAK_WAYLINE_IN_CUR_STATE`；
     * 飞机进入航线飞行（`WAYPOINT`）后同一命令立即被接受。悬浮窗据此决定是否给出可点的「暂停」入口。
     */
    private val pauseBlockedFlightModes = setOf(
        FlightMode.MOTOR_START,
        FlightMode.TAKE_OFF_READY,
        FlightMode.AUTO_TAKE_OFF,
        FlightMode.AUTO_LANDING,
        FlightMode.FORCE_LANDING,
        FlightMode.ATTI_LANDING,
        FlightMode.GO_HOME,
    )

    private val mutablePauseBlockedByFlightPhase = MutableLiveData(false)

    /**
     * 飞机是否处于「不宜中断航线」的飞行阶段（地面 / 起飞 / 降落 / 返航）。
     *
     * 能力契约：语义为**否定式**——仅在明确收到 [pauseBlockedFlightModes] 内的飞行模式时为 `true`；
     * Key 未推送、取值为 null 或未知时保持 `false`。这样新信号不达时不会把悬浮窗的暂停能力整体
     * 移除（最多退回改动前的行为）。线程安全：由 Key 回调线程写入，经 `postValue` 投递到主线程。
     */
    val pauseBlockedByFlightPhase: LiveData<Boolean> = mutablePauseBlockedByFlightPhase

    private val mutableIsFlying = MutableLiveData(false)

    /**
     * 飞行器是否在空中（`KeyIsFlying` 权威值）。
     *
     * 设计意图：停止态需要区分“地面停止”与“空中悬停”，并据此决定是否提供返航入口；
     * 与 [pauseBlockedByFlightPhase] 共用 `WaylineManager` holder，`release()` 时一并取消。
     */
    val isFlying: LiveData<Boolean> = mutableIsFlying

    /** 飞行模式 Key；订阅 holder 为 `WaylineManager` 自身（该 holder 名下仅此一个订阅）。 */
    private val flightModeKey = KeyTools.createKey(FlightControllerKey.KeyFlightMode)

    private val isFlyingKey = KeyTools.createKey(FlightControllerKey.KeyIsFlying)

    @Volatile
    private var lastLoggedFlightMode: FlightMode? = null

    @Volatile
    private var lastLoggedMissionState: WaypointMissionExecuteState? = null

    /**
     * 飞行阶段监听器（字段持有便于 [release] 精确取消；禁止匿名对象注册）。
     *
     * 只在取值变化时打日志，避免刷屏；不涉及敏感信息。
     */
    private val flightModeListener = CommonCallbacks.KeyListener<FlightMode> { _, value ->
        if (value != lastLoggedFlightMode) {
            lastLoggedFlightMode = value
            Log.i(TAG, "flight mode changed: $value")
        }
        val blocked = value != null && value in pauseBlockedFlightModes
        if (mutablePauseBlockedByFlightPhase.value != blocked) {
            mutablePauseBlockedByFlightPhase.postValue(blocked)
        }
    }

    private val isFlyingListener = CommonCallbacks.KeyListener<Boolean> { _, value ->
        val flying = value == true
        if (mutableIsFlying.value != flying) {
            Log.i(TAG, "isFlying changed: $flying")
            mutableIsFlying.postValue(flying)
        }
    }

    private val mutableState = MutableLiveData(WaylineState.IDLE)
    val state: LiveData<WaylineState> = mutableState

    private val mutableAircraftBusy = MutableLiveData(false)

    /**
     * 飞行器上航线任务是否处于「占用」态（上传 / 准备 / 进入航线 / 执行 / 恢复 / 返回起点）。
     *
     * 设计意图：`WaylineState` 会把 `UPLOADING`/`PREPARING` 塌缩成同一个 `PREPARING`，并把
     * 「本工单加载」与「飞行器上残留的其他任务」混在一起；`RouteController` 判断「能否点亮起飞入口」
     * 需要一个**只由 SDK 原始执行状态驱动、不被本工单加载流程写入**的信号。
     *
     * 能力契约：由 [missionStateListener] 写入，仅在取值变化时 `postValue`；`release()` 复位为 `false`。
     * 断连时 SDK 不再推送状态，保留最后一次已知值（起飞前置另要求飞行器已连接）。
     */
    val aircraftBusy: LiveData<Boolean> = mutableAircraftBusy

    private val mutableProgress = MutableLiveData(WaylineProgress())
    val progress: LiveData<WaylineProgress> = mutableProgress

    private val mutableError = MutableLiveData<String?>(null)
    val error: LiveData<String?> = mutableError

    /**
     * 航线执行中断原因（`onWaylineExecutingInterruptReasonUpdate` 携带的完整 `IDJIError`，只推一次）。
     * 以 [Throwable] 形态保留 errorCode/description，供上层 `RouteController` 不吞掉中断原因。
     */
    private val mutableInterruptReason = MutableLiveData<Throwable?>(null)
    val interruptReason: LiveData<Throwable?> = mutableInterruptReason

    private val mutableRouteAsset = MutableLiveData<Asset?>(null)
    val routeAsset: LiveData<Asset?> = mutableRouteAsset
    var preparedRouteAsset: Asset? = null
        private set

    private val missionManager = WaypointMissionManager.getInstance()
    private val fileExecutor = Executors.newSingleThreadExecutor()

    @Volatile
    private var listenersRegistered = false

    @Volatile
    private var prepareGeneration = 0L

    var missionName: String? = null
        private set

    var waylineIds: List<Int> = emptyList()
        private set
    private var waypointCounts: Map<Int, Int> = emptyMap()

    /** 既有 KMZ 解析出的航点坐标（route 轨迹图层用）；不新增 SDK 调用。 */
    var routePoints: List<GeoPoint3D> = emptyList()
        private set

    var localFile: File? = null
        private set

    private var currentJob: Job? = null
    var preparedJobId: String? = null
        private set
    private var downloadRequest: RequestHandle? = null

    private val missionStateListener = WaypointMissionExecuteStateListener { sdkState ->
        // 原始状态是本工程诊断链路的关键证据：塌缩后的 WaylineState 无法表达飞控真实阶段，
        // 缺这行日志时只能靠外部 logcat 反推（本次故障即如此）。
        if (sdkState != lastLoggedMissionState) {
            lastLoggedMissionState = sdkState
            Log.i(TAG, "waypoint mission state changed: $sdkState")
        }
        val busy = sdkState.isAircraftBusy()
        if (mutableAircraftBusy.value != busy) {
            Log.i(TAG, "aircraft busy=$busy state=$sdkState")
            mutableAircraftBusy.postValue(busy)
        }
        mutableState.postValue(sdkState.toAppState())
    }

    private val executingInfoListener = object : WaylineExecutingInfoListener {
        override fun onWaylineExecutingInfoUpdate(info: WaylineExecutingInfo) {
            val offset = waylineIds.takeWhile { it != info.waylineID }.sumOf { waypointCounts[it] ?: 0 }
            val total = waylineIds.sumOf { waypointCounts[it] ?: 0 }
            val current = if (total > 0) {
                (offset + info.currentWaypointIndex + 1).coerceAtMost(total)
            } else {
                info.currentWaypointIndex
            }
            mutableProgress.postValue(
                WaylineProgress(
                    stage = RouteProgressStage.EXECUTING,
                    currentWaypoint = current,
                    totalWaypoints = total,
                    waylineIndex = waylineIds.indexOf(info.waylineID) + 1,
                    waylineCount = waylineIds.size,
                ),
            )
        }

        override fun onWaylineExecutingInterruptReasonUpdate(error: IDJIError?) {
            if (error != null) {
                mutableInterruptReason.postValue(error.asThrowable())
                mutableProgress.postValue(
                    WaylineProgress(
                        stage = RouteProgressStage.INTERRUPTED,
                        interruptCode = error.errorCode(),
                    ),
                )
            }
        }
    }

    @Synchronized
    fun init() {
        // 每次初始化/重连（以及每次准备航线前）清空「占用」基线：断连期间 SDK 不再推送状态，
        // 若保留最后已知值，重连后可能把后续加载永久拦下；随后的上传/执行推送会重新置位。
        mutableAircraftBusy.postValue(false)
        if (listenersRegistered) return
        missionManager.init()
        missionManager.addWaypointMissionExecuteStateListener(missionStateListener)
        missionManager.addWaylineExecutingInfoListener(executingInfoListener)
        // 飞行阶段/空中状态订阅与航线监听器同点注册；release() 内用同一 holder 成对取消。
        KeyManager.getInstance().listen(flightModeKey, this, false, flightModeListener)
        KeyManager.getInstance().listen(isFlyingKey, this, true, isFlyingListener)
        listenersRegistered = true
    }

    @Synchronized
    fun prepare(job: Job, callback: ResultCallback<Unit>? = null) {
        init()
        currentJob = job
        preparedJobId = job.jobId
        downloadRequest?.cancel()
        downloadRequest = null
        missionName = null
        waylineIds = emptyList()
        waypointCounts = emptyMap()
        routePoints = emptyList()
        localFile = null
        preparedRouteAsset = null
        mutableRouteAsset.postValue(null)
        val generation = ++prepareGeneration
        mutableState.postValue(WaylineState.PREPARING)
        mutableError.postValue(null)
        mutableInterruptReason.postValue(null)

        val route = job.routeFileUrl
        if (route.isNullOrBlank()) {
            failPreparation(IllegalArgumentException("Job ${job.jobId} has no wayline file"), callback)
            return
        }

        val routePath = route.substringBefore('?').substringBefore('#')
        val fileName = routePath.substringAfterLast('/')
        if (!fileName.endsWith(".kmz") || fileName.length == 4) {
            failPreparation(
                IllegalArgumentException("Wayline file must use the lowercase .kmz extension: $fileName"),
                callback,
            )
            return
        }

        mutableProgress.postValue(WaylineProgress(stage = RouteProgressStage.PREPARING))

        val directory = File(MainApplication.instance.filesDir, "waylines")
        if (!directory.exists() && !directory.mkdirs()) {
            failPreparation(IllegalStateException("Cannot create ${directory.absolutePath}"), callback)
            return
        }
        val targetFile = File(directory, fileName)

        downloadRequest = CloudManager.downloadWayline(
            route,
            targetFile,
            { percent ->
                if (generation == prepareGeneration) {
                    mutableProgress.postValue(
                        WaylineProgress(
                            stage = RouteProgressStage.DOWNLOADING,
                            percent = percent,
                        ),
                    )
                }
            },
            object : ResultCallback<File> {
                override fun onSuccess(data: File) {
                    downloadRequest = null
                    fileExecutor.execute { parseAndPush(targetFile, generation, callback) }
                }

                override fun onFailure(error: Throwable) {
                    downloadRequest = null
                    if (generation == prepareGeneration) failPreparation(error, callback)
                }
            },
        )
    }

    fun importToPilot(file: File, callback: ResultCallback<Unit>? = null) {
        init()
        val error = validateKmz(file)
        if (error != null) {
            callback?.onFailure(error)
            return
        }
        missionManager.pushKMZFileToRemote(file.absolutePath, progressCallback(callback))
    }

    fun start(waylineIDs: List<Int>? = null, callback: ResultCallback<Unit>? = null) {
        val name = missionName
        if (name == null) {
            callback?.onFailure(IllegalStateException("Prepare a wayline before starting"))
            return
        }
        val selectedWaylineIds = waylineIDs ?: this.waylineIds
        if (selectedWaylineIds.isEmpty()) {
            callback?.onFailure(IllegalStateException("No available wayline IDs"))
            return
        }
        // 与 WayPointV3 Sample 一致：启动时显式传入 KMZ 中解析出的 wayline ID。
        missionManager.startMission(name, selectedWaylineIds, completionCallback("startMission", callback))
    }

    fun pause(callback: ResultCallback<Unit>? = null) {
        missionManager.pauseMission(completionCallback("pauseMission", callback))
    }

    fun resume(callback: ResultCallback<Unit>? = null) {
        missionManager.resumeMission(completionCallback("resumeMission", callback))
    }

    fun stop(callback: ResultCallback<Unit>? = null) {
        val name = missionName
        if (name == null) {
            callback?.onFailure(IllegalStateException("No prepared wayline to stop"))
            return
        }
        missionManager.stopMission(name, completionCallback("stopMission", callback))
    }

    fun queryBreakPoint(callback: ResultCallback<BreakPointInfo>) {
        val name = missionName
        if (name == null) {
            callback.onFailure(IllegalStateException("No prepared wayline to query"))
            return
        }
        missionManager.queryBreakPointInfoFromAircraft(
            name,
            object : CommonCallbacks.CompletionCallbackWithParam<BreakPointInfo> {
                override fun onSuccess(data: BreakPointInfo?) {
                    if (data == null) {
                        callback.onFailure(IllegalStateException("Aircraft returned no breakpoint"))
                    } else {
                        callback.onSuccess(data)
                    }
                }

                override fun onFailure(error: IDJIError) {
                    callback.onFailure(error.asThrowable())
                }
            },
        )
    }

    /** Starts the prepared mission from a persisted aircraft breakpoint. */
    fun resumeFrom(breakPointInfo: BreakPointInfo, callback: ResultCallback<Unit>? = null) {
        val name = missionName
        if (name == null) {
            callback?.onFailure(IllegalStateException("No prepared wayline to resume"))
            return
        }
        missionManager.startMission(name, breakPointInfo, completionCallback("resumeMission(breakPoint)", callback))
    }

    @Synchronized
    fun release() {
        prepareGeneration++
        downloadRequest?.cancel()
        downloadRequest = null
        if (listenersRegistered) {
            missionManager.removeWaypointMissionExecuteStateListener(missionStateListener)
            missionManager.removeWaylineExecutingInfoListener(executingInfoListener)
            // 精确取消本 holder 名下的飞行模式订阅（不使用任何 clearAll*）。
            KeyManager.getInstance().cancelListen(this)
            listenersRegistered = false
        }
        currentJob = null
        preparedJobId = null
        missionName = null
        waylineIds = emptyList()
        waypointCounts = emptyMap()
        routePoints = emptyList()
        localFile = null
        preparedRouteAsset = null
        mutableRouteAsset.postValue(null)
        mutableState.postValue(WaylineState.IDLE)
        mutableProgress.postValue(WaylineProgress())
        mutableError.postValue(null)
        mutableInterruptReason.postValue(null)
        // 飞行阶段信号与日志去重基线随监听器一起废弃：断连后旧值不得残留。
        lastLoggedFlightMode = null
        lastLoggedMissionState = null
        mutablePauseBlockedByFlightPhase.postValue(false)
        mutableIsFlying.postValue(false)
        mutableAircraftBusy.postValue(false)
    }

    /**
     * 原始执行状态是否代表「飞行器上已有未结束的航线任务」。
     *
     * 只覆盖会占用飞行器的阶段：上传 / 准备 / 进入航线 / 执行 / 恢复 / 返回起点。
     * `READY`（本工单自己上传完成后必然出现）与 `INTERRUPTED`（已由 [WaylineState.PAUSED] 采纳）
     * 不计入，避免把本工单的正常加载误判为残留任务。
     */
    private fun WaypointMissionExecuteState.isAircraftBusy(): Boolean = when (this) {
        WaypointMissionExecuteState.UPLOADING,
        WaypointMissionExecuteState.PREPARING,
        WaypointMissionExecuteState.ENTER_WAYLINE,
        WaypointMissionExecuteState.EXECUTING,
        WaypointMissionExecuteState.RECOVERING,
        WaypointMissionExecuteState.RETURN_TO_START_POINT,
        -> true
        else -> false
    }

    private fun parseAndPush(file: File, generation: Long, callback: ResultCallback<Unit>?) {
        if (generation != prepareGeneration) return
        val validationError = validateKmz(file)
        if (validationError != null) {
            failPreparation(validationError, callback)
            return
        }

        try {
            val ids = missionManager.getAvailableWaylineIDs(file.absolutePath)
            if (ids.isEmpty()) {
                failPreparation(IllegalArgumentException("KMZ contains no available waylines"), callback)
                return
            }
            if (generation != prepareGeneration) return
            missionName = file.name.removeSuffix(".kmz")
            waylineIds = ids.toList()
            val parsedWaylines = WPMZParserManager.getWaylines(file.absolutePath)
            waypointCounts = parsedWaylines.associate { wayline ->
                wayline.waylineId to wayline.waypoints.size
            }
            routePoints = parsedWaylines.flatMap { it.waypoints }.mapNotNull { waypoint ->
                val location = waypoint.location ?: return@mapNotNull null
                val latitude = location.latitude ?: return@mapNotNull null
                val longitude = location.longitude ?: return@mapNotNull null
                // 轨迹线仅用于地图对照；高度在 payload 中并非总有值，缺省取 0。
                GeoPoint3D(latitude, longitude, 0.0)
            }
            localFile = file
            missionManager.pushKMZFileToAircraft(
                file.absolutePath,
                progressCallback(
                    {
                        if (generation == prepareGeneration) {
                            val jobId = currentJob?.jobId.orEmpty()
                            preparedRouteAsset = Asset(
                                    assetId = "route-${file.nameWithoutExtension}",
                                    jobId = jobId,
                                    typeId = "route",
                                    localUri = file.absolutePath,
                                    sizeBytes = file.length(),
                                    md5 = md5(file),
                                    state = AssetState.DETECTED,
                                    meta = mapOf("fileName" to file.name, "format" to "KMZ"),
                                )
                            mutableRouteAsset.postValue(preparedRouteAsset)
                            mutableState.postValue(WaylineState.READY)
                            callback?.onSuccess(Unit)
                        }
                    },
                    { error ->
                        if (generation == prepareGeneration) failPreparation(error, callback)
                    },
                ),
            )
        } catch (error: Throwable) {
            if (generation == prepareGeneration) failPreparation(error, callback)
        }
    }

    private fun progressCallback(
        callback: ResultCallback<Unit>?,
    ): CommonCallbacks.CompletionCallbackWithProgress<Double> = progressCallback(
        { callback?.onSuccess(Unit) },
        { callback?.onFailure(it) },
    )

    private fun progressCallback(
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit,
    ) = object : CommonCallbacks.CompletionCallbackWithProgress<Double> {
        override fun onProgressUpdate(progress: Double) {
            mutableProgress.postValue(
                WaylineProgress(
                    stage = RouteProgressStage.UPLOADING,
                    percent = progress.toInt(),
                ),
            )
        }

        override fun onSuccess() = onSuccess.invoke()

        override fun onFailure(error: IDJIError) = onFailure.invoke(error.asThrowable())
    }

    /**
     * 命令回调包装：打印命令结果后转发。
     *
     * 设计意图：`onSuccess` 只代表命令被**接受**，日志因此用 accepted 表述，避免与业务终态混淆；
     * 失败时打印完整 `IDJIError`（errorCode / innerCode / description / hint），供现场归因。
     */
    private fun completionCallback(operation: String, callback: ResultCallback<Unit>?) =
        object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                Log.i(TAG, "$operation accepted")
                callback?.onSuccess(Unit)
            }

            override fun onFailure(error: IDJIError) {
                Log.w(TAG, "$operation failed: $error")
                callback?.onFailure(error.asThrowable())
            }
        }

    private fun validateKmz(file: File): Throwable? = when {
        !file.name.endsWith(".kmz") ->
            IllegalArgumentException("Wayline file must use the lowercase .kmz extension: ${file.name}")
        !file.isFile -> IllegalArgumentException("Wayline file does not exist: ${file.absolutePath}")
        else -> null
    }

    private fun failPreparation(error: Throwable, callback: ResultCallback<Unit>?) {
        mutableState.postValue(WaylineState.FAILED)
        mutableError.postValue(error.message)
        mutableProgress.postValue(WaylineProgress())
        callback?.onFailure(error)
    }

    private fun WaypointMissionExecuteState.toAppState(): WaylineState = when (this) {
        WaypointMissionExecuteState.READY -> WaylineState.READY
        WaypointMissionExecuteState.UPLOADING,
        WaypointMissionExecuteState.PREPARING,
        -> WaylineState.PREPARING
        WaypointMissionExecuteState.ENTER_WAYLINE,
        WaypointMissionExecuteState.EXECUTING,
        WaypointMissionExecuteState.RECOVERING,
        WaypointMissionExecuteState.RETURN_TO_START_POINT,
        -> WaylineState.EXECUTING
        WaypointMissionExecuteState.INTERRUPTED -> WaylineState.PAUSED
        WaypointMissionExecuteState.FINISHED -> WaylineState.FINISHED
        WaypointMissionExecuteState.NOT_SUPPORTED -> WaylineState.FAILED
        WaypointMissionExecuteState.DISCONNECTED,
        WaypointMissionExecuteState.IDLE,
        WaypointMissionExecuteState.UNKNOWN,
        -> WaylineState.IDLE
    }

    /**
     * 把 SDK 错误转成带类型的航线失败异常：保留完整 `IDJIError` 与错误码，
     * 供上层做可读文案映射；消息文本与类型均保持与既有行为兼容。
     */
    private fun IDJIError.asThrowable() =
        WaylineFailure(errorCode(), this, "${errorCode()}: ${description()}")

    private fun md5(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
