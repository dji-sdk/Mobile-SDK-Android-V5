package dji.sample.flighttask.manager

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dji.sample.flighttask.FlightTaskApplication
import dji.sample.flighttask.data.Asset
import dji.sample.flighttask.data.AssetState
import dji.sample.flighttask.data.Job
import dji.sample.flighttask.data.RequestHandle
import dji.sample.flighttask.data.ResultCallback
import dji.sample.flighttask.data.WaylineProgress
import dji.sample.flighttask.data.WaylineState
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
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

    private val mutableState = MutableLiveData(WaylineState.IDLE)
    val state: LiveData<WaylineState> = mutableState

    private val mutableProgress = MutableLiveData(WaylineProgress())
    val progress: LiveData<WaylineProgress> = mutableProgress

    private val mutableError = MutableLiveData<String?>(null)
    val error: LiveData<String?> = mutableError

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

    var localFile: File? = null
        private set

    private var currentJob: Job? = null
    var preparedJobId: String? = null
        private set
    private var downloadRequest: RequestHandle? = null

    private val missionStateListener = WaypointMissionExecuteStateListener { sdkState ->
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
                    currentWaypoint = current,
                    totalWaypoints = total,
                    description = "wayline=${info.waylineID}, mission=${info.missionFileName}",
                ),
            )
        }

        override fun onWaylineExecutingInterruptReasonUpdate(error: IDJIError?) {
            if (error != null) {
                mutableProgress.postValue(
                    WaylineProgress(description = "interrupted: ${error.errorCode()} ${error.description()}"),
                )
            }
        }
    }

    @Synchronized
    fun init() {
        if (listenersRegistered) return
        missionManager.init()
        missionManager.addWaypointMissionExecuteStateListener(missionStateListener)
        missionManager.addWaylineExecutingInfoListener(executingInfoListener)
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
        localFile = null
        preparedRouteAsset = null
        mutableRouteAsset.postValue(null)
        val generation = ++prepareGeneration
        mutableState.postValue(WaylineState.PREPARING)
        mutableError.postValue(null)

        val route = job.routeFileUrl
        if (route.isNullOrBlank()) {
            failPreparation(IllegalArgumentException("Job ${job.jobId} has no wayline file"), callback)
            return
        }

        val routePath = route.substringBefore('?').substringBefore('#')
        val fileName = routePath.substringAfterLast('/')
        if (!fileName.endsWith(".kmz") || fileName.length == 4) {
            failPreparation(IllegalArgumentException("Wayline file must use the lowercase .kmz extension: $fileName"), callback)
            return
        }

        mutableProgress.postValue(WaylineProgress(description = "Preparing $fileName"))

        val directory = File(FlightTaskApplication.instance.filesDir, "waylines")
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
                    mutableProgress.postValue(WaylineProgress(description = "Downloading $percent%"))
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
        missionManager.pushKMZFileToRemote(file.absolutePath, progressCallback("Importing", callback))
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
        missionManager.startMission(name, selectedWaylineIds, completionCallback(callback))
    }

    fun pause(callback: ResultCallback<Unit>? = null) {
        missionManager.pauseMission(completionCallback(callback))
    }

    fun resume(callback: ResultCallback<Unit>? = null) {
        missionManager.resumeMission(completionCallback(callback))
    }

    fun stop(callback: ResultCallback<Unit>? = null) {
        val name = missionName
        if (name == null) {
            callback?.onFailure(IllegalStateException("No prepared wayline to stop"))
            return
        }
        missionManager.stopMission(name, completionCallback(callback))
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
        missionManager.startMission(name, breakPointInfo, completionCallback(callback))
    }

    @Synchronized
    fun release() {
        prepareGeneration++
        downloadRequest?.cancel()
        downloadRequest = null
        if (listenersRegistered) {
            missionManager.removeWaypointMissionExecuteStateListener(missionStateListener)
            missionManager.removeWaylineExecutingInfoListener(executingInfoListener)
            listenersRegistered = false
        }
        currentJob = null
        preparedJobId = null
        missionName = null
        waylineIds = emptyList()
        waypointCounts = emptyMap()
        localFile = null
        preparedRouteAsset = null
        mutableRouteAsset.postValue(null)
        mutableState.postValue(WaylineState.IDLE)
        mutableProgress.postValue(WaylineProgress())
        mutableError.postValue(null)
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
            waypointCounts = WPMZParserManager.getWaylines(file.absolutePath).associate { wayline ->
                wayline.waylineId to wayline.waypoints.size
            }
            localFile = file
            missionManager.pushKMZFileToAircraft(
                file.absolutePath,
                progressCallback(
                    "Uploading",
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
        operation: String,
        callback: ResultCallback<Unit>?,
    ): CommonCallbacks.CompletionCallbackWithProgress<Double> = progressCallback(
        operation,
        { callback?.onSuccess(Unit) },
        { callback?.onFailure(it) },
    )

    private fun progressCallback(
        operation: String,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit,
    ) = object : CommonCallbacks.CompletionCallbackWithProgress<Double> {
        override fun onProgressUpdate(progress: Double) {
            mutableProgress.postValue(
                WaylineProgress(description = "$operation ${progress.toInt()}%"),
            )
        }

        override fun onSuccess() = onSuccess.invoke()

        override fun onFailure(error: IDJIError) = onFailure.invoke(error.asThrowable())
    }

    private fun completionCallback(callback: ResultCallback<Unit>?) =
        object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                callback?.onSuccess(Unit)
            }

            override fun onFailure(error: IDJIError) {
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
        mutableProgress.postValue(WaylineProgress(description = error.message ?: "Wayline preparation failed"))
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

    private fun IDJIError.asThrowable() =
        IllegalStateException("${errorCode()}: ${description()}")

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
