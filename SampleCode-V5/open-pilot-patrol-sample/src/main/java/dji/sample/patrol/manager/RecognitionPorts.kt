package dji.sample.patrol.manager

import android.util.Log
import dji.sample.patrol.data.ResultCallback
import dji.sample.patrol.data.TargetKind
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.utils.ProductUtil
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.KeyManager
import dji.v5.manager.intelligent.IntelligentFlightManager
import dji.v5.manager.intelligent.IntelligentFlightInfo
import dji.v5.manager.intelligent.IntelligentFlightInfoListener
import dji.v5.manager.intelligent.MissionType
import dji.v5.manager.intelligent.AutoSensingInfoListener as SdkAutoSensingInfoListener
import dji.v5.manager.intelligent.AutoSensingInfo as SdkAutoSensingInfo
import dji.v5.manager.intelligent.AutoSensingTargetType
import dji.v5.manager.interfaces.IIntelligentFlightManager
import java.util.IdentityHashMap

/**
 * 识别 SDK 集成边界（design D2/D9）：识别开启/订阅与智能飞行前置状态均以工程自有接口声明，
 * 生产实现委托 MSDK `IIntelligentFlightManager` / `KeyManager`；`dji.*` import 仅落本文件。
 */

/** 工程内识别订阅事件（不携带 `dji.*` 类型）。 */
data class AutoSensingStatus(
    val enabled: Boolean,
    val targets: List<AutoSensingTargetInfo> = emptyList(),
)

/** 工程内识别目标投影。 */
data class AutoSensingTargetInfo(
    val targetIndex: Int,
    val targetType: TargetKind,
    val label: String = "",
)

/** 识别订阅回调（对应 SDK `AutoSensingInfoListener.onAutoSensingInfoUpdate`）。 */
fun interface AutoSensingListener {
    fun onAutoSensingInfoUpdate(info: AutoSensingStatus)
}

/** 智能飞行功能类型投影（对应 SDK `MissionType`，`NONE` 为可用态）。 */
enum class IntelligentMission { NONE, POI, SPOTLIGHT, TRACK, FLY_TO }

/**
 * 识别端口：可 JVM fake，隔离 MSDK AutoSensing。
 *
 * 调用纪律（spec「识别开启与订阅」）：必须先 [addAutoSensingInfoListener] 再 [startAutoSensing]，
 * 释放时用 [removeAutoSensingInfoListener] 精确移除，禁止 `clearAll*`。
 */
interface RecognitionPort {
    fun startAutoSensing(callback: ResultCallback<Unit>)
    fun stopAutoSensing(callback: ResultCallback<Unit>)
    fun addAutoSensingInfoListener(listener: AutoSensingListener)
    fun removeAutoSensingInfoListener(listener: AutoSensingListener)
}

/** 智能飞行前置状态探针（spec「前置条件不满足不开启」）。 */
interface IntelligentStatusProbe {
    val connected: Boolean
    val isFlying: Boolean
    val isIndustry: Boolean
    val runningMission: IntelligentMission

    /** 连接机型支持的智能功能类型；至少收到一次更新后非空。 */
    val supportedMissions: Set<IntelligentMission>

    /** 释放内部订阅（默认无操作，生产实现精确移除监听）。 */
    fun release() {}
}

/** 生产实现：委托 MSDK `IIntelligentFlightManager`，并按 listener 引用精确配对注册/移除。 */
object IntelligentFlightRecognitionPort : RecognitionPort {

    private const val TAG = "PatrolRecognition"

    private val manager: IIntelligentFlightManager get() = IntelligentFlightManager.getInstance()
    private val registered = IdentityHashMap<AutoSensingListener, SdkAutoSensingInfoListener>()

    override fun addAutoSensingInfoListener(listener: AutoSensingListener) {
        val sdkListener = object : SdkAutoSensingInfoListener {
            override fun onAutoSensingInfoUpdate(info: SdkAutoSensingInfo) {
                listener.onAutoSensingInfoUpdate(
                    AutoSensingStatus(
                        enabled = info.isAutoSensingEnable,
                        targets = info.targets.orEmpty().map { target ->
                            AutoSensingTargetInfo(
                                targetIndex = target.targetIndex,
                                targetType = target.targetType.toTargetKind(),
                                label = target.label.orEmpty(),
                            )
                        },
                    ),
                )
            }
        }
        synchronized(registered) { registered[listener] = sdkListener }
        manager.addAutoSensingInfoListener(sdkListener)
        Log.i(TAG, "addAutoSensingInfoListener registered")
    }

    override fun removeAutoSensingInfoListener(listener: AutoSensingListener) {
        val sdkListener = synchronized(registered) { registered.remove(listener) }
        if (sdkListener == null) {
            Log.w(TAG, "removeAutoSensingInfoListener ignored: listener was not registered")
            return
        }
        manager.removeAutoSensingInfoListener(sdkListener)
        Log.i(TAG, "removeAutoSensingInfoListener removed")
    }

    override fun startAutoSensing(callback: ResultCallback<Unit>) {
        manager.startAutoSensing(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                Log.i(TAG, "startAutoSensing onSuccess (command accepted)")
                callback.onSuccess(Unit)
            }

            override fun onFailure(error: IDJIError) {
                Log.e(TAG, "startAutoSensing onFailure ${error.errorCode()} ${error.description()}")
                callback.onFailure(IllegalStateException("${error.errorCode()}: ${error.description()}"))
            }
        })
    }

    override fun stopAutoSensing(callback: ResultCallback<Unit>) {
        manager.stopAutoSensing(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                Log.i(TAG, "stopAutoSensing onSuccess")
                callback.onSuccess(Unit)
            }

            override fun onFailure(error: IDJIError) {
                Log.e(TAG, "stopAutoSensing onFailure ${error.errorCode()} ${error.description()}")
                callback.onFailure(IllegalStateException("${error.errorCode()}: ${error.description()}"))
            }
        })
    }
}

private fun AutoSensingTargetType?.toTargetKind(): TargetKind = when (this) {
    AutoSensingTargetType.PERSON -> TargetKind.PERSON
    AutoSensingTargetType.CAR -> TargetKind.CAR
    AutoSensingTargetType.BOAT -> TargetKind.BOAT
    AutoSensingTargetType.CUSTOM -> TargetKind.CUSTOM
    else -> TargetKind.UNKNOWN
}

/**
 * 生产实现：同步读取 [KeyManager] 连接/飞行状态，并经 [IntelligentFlightInfoListener]
 * 缓存 `supportedMissions`/`runningMission`；`release()` 精确移除监听。
 */
object KeyManagerIntelligentStatusProbe : IntelligentStatusProbe {

    private const val TAG = "PatrolRecognition"

    private val connectionKey = KeyTools.createKey(FlightControllerKey.KeyConnection)
    private val isFlyingKey = KeyTools.createKey(FlightControllerKey.KeyIsFlying)

    @Volatile
    private var supported: Set<IntelligentMission> = emptySet()

    @Volatile
    private var running: IntelligentMission = IntelligentMission.NONE

    @Volatile
    private var listening = false

    private val infoListener = object : IntelligentFlightInfoListener {
        override fun onIntelligentFlightInfoUpdate(info: IntelligentFlightInfo) {
            supported = info.supportedMissions.orEmpty().map { it.toIntelligentMission() }.toSet()
            running = info.runningMission.toIntelligentMission()
            Log.i(TAG, "IntelligentFlightInfo runningMission=$running supported=$supported")
        }

        override fun onIntelligentFlightErrorUpdate(error: IDJIError) {
            Log.e(TAG, "IntelligentFlightInfo error ${error.errorCode()} ${error.description()}")
        }
    }

    override val connected: Boolean
        get() = KeyManager.getInstance().getValue(connectionKey) == true

    override val isFlying: Boolean
        get() = KeyManager.getInstance().getValue(isFlyingKey) == true

    override val isIndustry: Boolean
        get() = ProductUtil.isIndustryV3()

    override val runningMission: IntelligentMission
        get() {
            ensureListening()
            return running
        }

    override val supportedMissions: Set<IntelligentMission>
        get() {
            ensureListening()
            return supported
        }

    override fun release() {
        if (!listening) return
        listening = false
        IntelligentFlightManager.getInstance().removeIntelligentFlightInfoListener(infoListener)
        Log.i(TAG, "removeIntelligentFlightInfoListener removed")
    }

    @Synchronized
    private fun ensureListening() {
        if (listening) return
        IntelligentFlightManager.getInstance().addIntelligentFlightInfoListener(infoListener)
        listening = true
        Log.i(TAG, "addIntelligentFlightInfoListener registered")
    }
}

private fun MissionType.toIntelligentMission(): IntelligentMission = when (this) {
    MissionType.POI -> IntelligentMission.POI
    MissionType.SPOTLIGHT -> IntelligentMission.SPOTLIGHT
    MissionType.TRACK -> IntelligentMission.TRACK
    MissionType.FLY_TO -> IntelligentMission.FLY_TO
    MissionType.NONE -> IntelligentMission.NONE
}
