package dji.sample.patrol.manager

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dji.sample.patrol.data.DestinationFailureCategory
import dji.sample.patrol.data.DestinationPoint
import dji.sample.patrol.data.FlyToModeKind
import dji.sample.patrol.data.FlyToStage
import dji.sample.patrol.data.ResultCallback

/**
 * FlyTo 目的地飞行编排（design D4，高风险·飞行安全）。
 *
 * 设计意图：把「选定目的地 → 二次确认 → 参数设置 → 启动 → 权威状态推进 → 到达/取消回航线」
 * 的状态机与 SDK 解耦（经 [port]）；`startMission().onSuccess` 仅置 `REQUESTED`，`FLYING`/`ARRIVED`
 * 只能由 `FlyToInfo.getState()`（`FlyToMissionState`）驱动；先注册 Listener 再发令；释放精确
 * `removeInfoListener`。可 JVM fake 单测。
 */
object FlyToController {

    private const val TAG = "PatrolFlyToCtrl"

    /** 可注入的 FlyTo 端口（生产为 [PilotFlyToPort]）。 */
    @Volatile
    var port: FlyToPort? = null

    /** 失败回调（`flyto-error`/`not-ready`）。 */
    @Volatile
    var onError: ((DestinationFailureCategory, String) -> Unit)? = null

    /** FlyTo 前暂停航线（由 MainApplication 注入 `RouteController.pause`）。 */
    @Volatile
    var requestPauseRoute: (() -> Unit)? = null

    /** 到达/取消后回航线（不自动恢复，仅回调以收敛浮窗）。 */
    @Volatile
    var onReturnToRoute: (() -> Unit)? = null

    /** 智能飞行互斥查询：`runningMission == NONE` 返回 `true`（未注入时视为可执行）。 */
    @Volatile
    var runningMissionFree: (() -> Boolean)? = null

    /** 默认飞行高度（米）。 */
    @Volatile
    var defaultHeightMeters: Int = 30

    private val mutableStage = MutableLiveData(FlyToStage.IDLE)

    /** FlyTo 编排态：供浮窗只读订阅。 */
    val stage: LiveData<FlyToStage> = mutableStage

    /** 编排态快照（JVM 单测用，避免依赖主线程）。 */
    @Volatile
    var currentStage: FlyToStage = FlyToStage.IDLE
        private set

    /** 当前目的地点位（选定后非空）。 */
    @Volatile
    var destination: DestinationPoint? = null
        private set

    private var listenerRegistered = false
    private val stateListener = FlyToStateListener { onState(it) }

    /** 选定目的地（未确认），进入 `MARKED`；坐标无效返回 `false`。 */
    fun mark(point: DestinationPoint): Boolean {
        if (!point.isValid) {
            fail(DestinationFailureCategory.NOT_READY, "invalid destination coordinate")
            return false
        }
        destination = point
        setStage(FlyToStage.MARKED)
        Log.i(TAG, "destination marked ${point.latitude},${point.longitude}")
        return true
    }

    /** 飞手二次确认后启动 FlyTo。 */
    fun confirm() {
        val point = destination ?: return
        val p = port ?: run { fail(DestinationFailureCategory.FLYTO_ERROR, "flyto port not injected"); return }
        if (currentStage != FlyToStage.MARKED) return
        p.isSupported(object : ResultCallback<Boolean> {
            override fun onSuccess(supported: Boolean) {
                if (!supported) {
                    fail(DestinationFailureCategory.NOT_READY, "flyto not supported on this aircraft")
                    return
                }
                if (runningMissionFree?.invoke() == false) {
                    fail(DestinationFailureCategory.NOT_READY, "another intelligent mission is running")
                    return
                }
                requestPauseRoute?.invoke()
                ensureListener(p)
                p.updateParam(FlyToModeKind.SMART_HEIGHT, defaultHeightMeters, object : ResultCallback<Unit> {
                    override fun onSuccess(data: Unit) = start(p, point)
                    override fun onFailure(error: Throwable) =
                        fail(DestinationFailureCategory.FLYTO_ERROR, "updateMissionParam failed: ${error.message}")
                })
            }

            override fun onFailure(error: Throwable) {
                fail(DestinationFailureCategory.FLYTO_ERROR, "capability query failed: ${error.message}")
            }
        })
    }

    /** 取消 FlyTo（走 `stopMission()`，底层急停）。 */
    fun cancel() {
        val p = port ?: run { reset(); return }
        p.stop(object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) = settle(FlyToStage.CANCELLED)

            override fun onFailure(error: Throwable) {
                report(DestinationFailureCategory.FLYTO_ERROR, "stopMission failed: ${error.message}")
                settle(FlyToStage.CANCELLED)
            }
        })
    }

    /** 断连/离开任务清理：精确移除 Listener 并复位。 */
    fun onDisconnected() {
        val p = port
        if (p != null) cleanupListener(p)
        reset()
    }

    private fun start(p: FlyToPort, point: DestinationPoint) {
        p.start(point, object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) {
                // 仅命令被接受：进入 REQUESTED，等待 FlyToMissionState 驱动。
                if (currentStage == FlyToStage.MARKED || currentStage == FlyToStage.REQUESTED) {
                    setStage(FlyToStage.REQUESTED)
                }
                Log.i(TAG, "startMission accepted; awaiting FlyToMissionState")
            }

            override fun onFailure(error: Throwable) {
                cleanupListener(p)
                fail(DestinationFailureCategory.FLYTO_ERROR, "startMission failed: ${error.message}")
            }
        })
    }

    private fun onState(state: FlyToState) {
        when (state) {
            FlyToState.MARCHON -> setStage(FlyToStage.FLYING)
            FlyToState.FINISHED -> settle(FlyToStage.ARRIVED)
            FlyToState.INTERRUPTED -> settle(FlyToStage.CANCELLED)
            else -> Unit
        }
    }

    /** 到达/取消统一收敛：先置终态，再清理、回航线、复位。 */
    private fun settle(stage: FlyToStage) {
        setStage(stage)
        val p = port
        if (p != null) cleanupListener(p)
        destination = null
        onReturnToRoute?.invoke()
        setStage(FlyToStage.IDLE)
    }

    private fun ensureListener(p: FlyToPort) {
        if (listenerRegistered) return
        listenerRegistered = true
        p.addInfoListener(stateListener)
    }

    private fun cleanupListener(p: FlyToPort) {
        if (!listenerRegistered) return
        listenerRegistered = false
        p.removeInfoListener(stateListener)
    }

    private fun reset() {
        destination = null
        setStage(FlyToStage.IDLE)
    }

    private fun setStage(next: FlyToStage) {
        if (currentStage == next) return
        Log.i(TAG, "flyto stage ${currentStage} -> $next")
        currentStage = next
        runCatching { mutableStage.postValue(next) }
    }

    private fun fail(category: DestinationFailureCategory, message: String) {
        report(category, message)
        setStage(FlyToStage.FAILED)
    }

    private fun report(category: DestinationFailureCategory, message: String) {
        Log.w(TAG, "${category.id}: $message")
        onError?.invoke(category, message)
    }

    /** 测试辅助：重置。 */
    fun resetForTest() {
        port = null
        onError = null
        requestPauseRoute = null
        onReturnToRoute = null
        runningMissionFree = null
        defaultHeightMeters = 30
        destination = null
        listenerRegistered = false
        currentStage = FlyToStage.IDLE
        runCatching { mutableStage.postValue(FlyToStage.IDLE) }
    }
}
