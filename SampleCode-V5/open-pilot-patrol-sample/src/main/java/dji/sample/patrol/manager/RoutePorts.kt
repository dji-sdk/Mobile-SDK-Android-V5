package dji.sample.patrol.manager

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dji.sample.patrol.data.Job
import dji.sample.patrol.data.ResultCallback
import dji.sample.patrol.data.WaylineProgress
import dji.sample.patrol.data.WaylineState
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.common.EmptyMsg
import dji.sdk.keyvalue.value.flightcontroller.FlightMode as SdkFlightMode
import dji.sdk.keyvalue.value.flightcontroller.GoHomeState
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.KeyManager

/**
 * SDK 集成边界（design D8）：航线动作与飞行状态均以接口声明，生产实现委托既有
 * `WaylineManager` / 读取 `KeyManager`+`FlightControllerKey`；测试以手写 fake 驱动状态机，
 * 不加载 `dji.open.pilot.*`，也不触碰真实 SDK 调用。
 */

/** 经纬度快照（不携带 `dji.*` 类型，便于 JVM 单测）。 */
data class GeoPoint(val latitude: Double, val longitude: Double)

/** 飞行模式投影（仅保留起飞门控关心的取值）。 */
enum class FlightMode { NORMAL, GO_HOME, AUTO_LANDING, UNKNOWN }

interface RouteActionPort {
    val state: LiveData<WaylineState>
    val progress: LiveData<WaylineProgress>
    val error: LiveData<String?>
    val interruptReason: LiveData<Throwable?>

    /**
     * 飞行器上是否已有未结束的航线任务（由 SDK 原始执行状态驱动，不受本工单加载流程写入）。
     *
     * 能力契约：工程自有布尔，不携带 `dji.*` 类型，JVM 单测可用 `MutableLiveData` fake；
     * 加载起点取样用于判断「飞机上是否已被别的任务占用」。
     */
    val aircraftBusy: LiveData<Boolean>
    val waylineIds: List<Int>

    fun load(job: Job, callback: ResultCallback<Unit>?)
    fun takeOff(callback: ResultCallback<Unit>?)
    fun pause(callback: ResultCallback<Unit>?)
    fun resume(callback: ResultCallback<Unit>?)
    fun stop(callback: ResultCallback<Unit>?)
    fun release()
}

interface FlightStateProbe {
    val connected: Boolean
    val location: GeoPoint?
    val homeLocation: GeoPoint?
    val flightMode: FlightMode
    val isFlying: Boolean
}

/** 生产实现：纯委托 [WaylineManager]，不改其任何 SDK 调用。 */
object WaylineRouteActionPort : RouteActionPort {
    override val state: LiveData<WaylineState> = WaylineManager.state
    override val progress: LiveData<WaylineProgress> = WaylineManager.progress
    override val error: LiveData<String?> = WaylineManager.error
    override val interruptReason: LiveData<Throwable?> = WaylineManager.interruptReason
    override val aircraftBusy: LiveData<Boolean> = WaylineManager.aircraftBusy
    override val waylineIds: List<Int> get() = WaylineManager.waylineIds

    override fun load(job: Job, callback: ResultCallback<Unit>?) = WaylineManager.prepare(job, callback)
    override fun takeOff(callback: ResultCallback<Unit>?) = WaylineManager.start(null, callback)
    override fun pause(callback: ResultCallback<Unit>?) = WaylineManager.pause(callback)
    override fun resume(callback: ResultCallback<Unit>?) = WaylineManager.resume(callback)
    override fun stop(callback: ResultCallback<Unit>?) = WaylineManager.stop(callback)
    override fun release() = WaylineManager.release()
}

/**
 * 返航能力端口（design D4）：把“启动返航动作”与“返航权威状态”隔离在 SDK 边界内。
 *
 * 能力契约：
 * - [state] 由 `KeyGoHomeStatus` 订阅驱动，代表飞控权威返航阶段；
 * - [start] 的 `onSuccess` 只代表动作被接受，不代表已进入返航；
 * - [init]/[release] 必须成对，`release` 以自身为 holder 精确 `cancelListen`。
 */
interface ReturnHomePort {
    val state: LiveData<GoHomeState>
    fun init()
    fun start(callback: ResultCallback<Unit>?)
    fun release()
}

/** 生产实现：`KeyGoHomeStatus` 监听 + `KeyStartGoHome` 动作。 */
object KeyManagerReturnHomePort : ReturnHomePort {

    private const val TAG = "ReturnHomePort"

    private val goHomeStatusKey = KeyTools.createKey(FlightControllerKey.KeyGoHomeStatus)
    private val startGoHomeKey = KeyTools.createKey(FlightControllerKey.KeyStartGoHome)
    private val mutableState = MutableLiveData(GoHomeState.UNKNOWN)
    override val state: LiveData<GoHomeState> = mutableState

    @Volatile
    private var listening = false

    private val listener = CommonCallbacks.KeyListener<GoHomeState> { _, value ->
        Log.i(TAG, "goHomeState changed: $value")
        mutableState.postValue(value ?: GoHomeState.UNKNOWN)
    }

    @Synchronized
    override fun init() {
        if (listening) return
        KeyManager.getInstance().listen(goHomeStatusKey, this, true, listener)
        listening = true
        Log.i(TAG, "return home port attached")
    }

    override fun start(callback: ResultCallback<Unit>?) {
        init()
        KeyManager.getInstance().performAction(
            startGoHomeKey,
            object : CommonCallbacks.CompletionCallbackWithParam<EmptyMsg> {
                override fun onSuccess(result: EmptyMsg?) {
                    Log.i(TAG, "startGoHome accepted")
                    callback?.onSuccess(Unit)
                }

                override fun onFailure(error: IDJIError) {
                    Log.w(TAG, "startGoHome failed: $error")
                    callback?.onFailure(
                        ReturnHomeFailure(
                            error.errorCode(),
                            error,
                            "${error.errorCode()}: ${error.description()}",
                        ),
                    )
                }
            },
        )
    }

    @Synchronized
    override fun release() {
        if (!listening) return
        KeyManager.getInstance().cancelListen(this)
        listening = false
        Log.i(TAG, "return home port released")
    }
}

/** 生产实现：同步读取 [KeyManager] 缓存快照（design D6）。 */
object KeyManagerFlightStateProbe : FlightStateProbe {

    private val connectionKey = KeyTools.createKey(FlightControllerKey.KeyConnection)
    private val locationKey = KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D)
    private val homeLocationKey = KeyTools.createKey(FlightControllerKey.KeyHomeLocation)
    private val flightModeKey = KeyTools.createKey(FlightControllerKey.KeyFlightMode)
    private val isFlyingKey = KeyTools.createKey(FlightControllerKey.KeyIsFlying)

    override val connected: Boolean
        get() = KeyManager.getInstance().getValue(connectionKey) == true

    override val location: GeoPoint?
        get() = KeyManager.getInstance().getValue(locationKey)?.let { coordinate ->
            val latitude = coordinate.latitude
            val longitude = coordinate.longitude
            if (latitude != null && longitude != null && isValid(latitude, longitude)) {
                GeoPoint(latitude, longitude)
            } else {
                null
            }
        }

    override val homeLocation: GeoPoint?
        get() = KeyManager.getInstance().getValue(homeLocationKey)?.let { coordinate ->
            val latitude = coordinate.latitude
            val longitude = coordinate.longitude
            if (latitude != null && longitude != null) GeoPoint(latitude, longitude) else null
        }

    override val flightMode: FlightMode
        get() = when (KeyManager.getInstance().getValue(flightModeKey)) {
            SdkFlightMode.GO_HOME -> FlightMode.GO_HOME
            SdkFlightMode.AUTO_LANDING -> FlightMode.AUTO_LANDING
            null -> FlightMode.UNKNOWN
            else -> FlightMode.NORMAL
        }

    override val isFlying: Boolean
        get() = KeyManager.getInstance().getValue(isFlyingKey) == true

    private fun isValid(latitude: Double, longitude: Double): Boolean =
        latitude in -90.0..90.0 &&
            longitude in -180.0..180.0 &&
            !(latitude == 0.0 && longitude == 0.0)
}
