package dji.sample.patrol.manager

import android.util.Log
import dji.sample.patrol.data.DestinationPoint
import dji.sample.patrol.data.FlyToModeKind
import dji.sample.patrol.data.ResultCallback
import dji.sdk.keyvalue.value.common.LocationCoordinate3D
import dji.sdk.keyvalue.value.flightcontroller.FlyToMissionState
import dji.sdk.keyvalue.value.flightcontroller.FlyToMode
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.intelligent.IMissionInfoListener
import dji.v5.manager.intelligent.IntelligentFlightManager
import dji.v5.manager.intelligent.flyto.FlyToInfo
import dji.v5.manager.intelligent.flyto.FlyToParam
import dji.v5.manager.intelligent.flyto.FlyToTarget
import java.util.IdentityHashMap

/** FlyTo 权威状态的工程内投影（不携带 `dji.*`）。 */
enum class FlyToState { UNKNOWN, IDLE, PREPARE, LAUNCH_PRE, LAUNCH, MARCHON, FINISHED, INTERRUPTED }

/** FlyTo 状态订阅（工程内投影）。 */
fun interface FlyToStateListener {
    fun onState(state: FlyToState)
}

/**
 * FlyTo SDK 集成边界（design D4/D9）：能力查询/参数设置/启动/停止/状态订阅均可 JVM fake。
 */
interface FlyToPort {
    fun isSupported(callback: ResultCallback<Boolean>)
    fun updateParam(mode: FlyToModeKind, heightMeters: Int, callback: ResultCallback<Unit>)
    fun start(point: DestinationPoint, callback: ResultCallback<Unit>)
    fun stop(callback: ResultCallback<Unit>)
    fun addInfoListener(listener: FlyToStateListener)
    fun removeInfoListener(listener: FlyToStateListener)
}

/** 生产实现：`IntelligentFlightManager.getInstance().getFlyToMissionManager()`；`dji.*` import 仅落本文件。 */
object PilotFlyToPort : FlyToPort {

    private const val TAG = "PatrolFlyTo"
    private const val DEFAULT_HEIGHT_M = 30

    private val manager get() = IntelligentFlightManager.getInstance().getFlyToMissionManager()
    private val sdkListeners = IdentityHashMap<FlyToStateListener, IMissionInfoListener<FlyToInfo, FlyToTarget>>()

    override fun isSupported(callback: ResultCallback<Boolean>) {
        // Demo：管理器可用即视为支持；机型不支持由 `startMission` 的 UNSUPPORTED 失败兜底降级。
        callback.onSuccess(IntelligentFlightManager.getInstance().getFlyToMissionManager() != null)
    }

    override fun updateParam(mode: FlyToModeKind, heightMeters: Int, callback: ResultCallback<Unit>) {
        val param = FlyToParam().apply {
            setFlyToMode(if (mode == FlyToModeKind.SMART_HEIGHT) FlyToMode.SMART_HEIGHT else FlyToMode.SET_HEIGHT)
            setHeight(heightMeters)
        }
        manager.updateMissionParam(param, completion("updateMissionParam", callback))
    }

    override fun start(point: DestinationPoint, callback: ResultCallback<Unit>) {
        val target = FlyToTarget().apply {
            setTargetLocation(LocationCoordinate3D(point.latitude, point.longitude, point.altitude))
            setSecurityTakeoffHeight(DEFAULT_HEIGHT_M)
        }
        val param = FlyToParam().apply {
            setFlyToMode(FlyToMode.SMART_HEIGHT)
            setHeight(DEFAULT_HEIGHT_M)
        }
        manager.startMission(target, param, completion("startMission", callback))
    }

    override fun stop(callback: ResultCallback<Unit>) {
        manager.stopMission(completion("stopMission", callback))
    }

    override fun addInfoListener(listener: FlyToStateListener) {
        val sdkListener = object : IMissionInfoListener<FlyToInfo, FlyToTarget> {
            override fun onMissionInfoUpdate(info: FlyToInfo) {
                Log.i(TAG, "onMissionInfoUpdate state=${info.state}")
                listener.onState(info.state.toAppState())
            }

            override fun onMissionTargetUpdate(target: FlyToTarget) = Unit
        }
        sdkListeners[listener] = sdkListener
        manager.addMissionInfoListener(sdkListener)
    }

    override fun removeInfoListener(listener: FlyToStateListener) {
        val sdkListener = sdkListeners.remove(listener) ?: return
        manager.removeMissionInfoListener(sdkListener)
    }

    private fun completion(operation: String, callback: ResultCallback<Unit>) =
        object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                Log.i(TAG, "$operation onSuccess")
                callback.onSuccess(Unit)
            }

            override fun onFailure(error: IDJIError) {
                Log.e(TAG, "$operation onFailure ${error.errorCode()} ${error.description()}")
                callback.onFailure(IllegalStateException("${error.errorCode()}: ${error.description()}"))
            }
        }

    private fun FlyToMissionState.toAppState(): FlyToState = when (this) {
        FlyToMissionState.IDLE -> FlyToState.IDLE
        FlyToMissionState.PREPARE -> FlyToState.PREPARE
        FlyToMissionState.LAUNCH_PRE -> FlyToState.LAUNCH_PRE
        FlyToMissionState.LAUNCH -> FlyToState.LAUNCH
        FlyToMissionState.MARCHON -> FlyToState.MARCHON
        FlyToMissionState.FINISHED -> FlyToState.FINISHED
        FlyToMissionState.INTERRUPTED -> FlyToState.INTERRUPTED
        else -> FlyToState.UNKNOWN
    }
}
