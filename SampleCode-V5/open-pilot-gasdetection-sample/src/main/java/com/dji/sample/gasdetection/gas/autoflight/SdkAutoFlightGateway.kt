package com.dji.sample.gasdetection.gas.autoflight

import android.os.Handler
import android.os.Looper
import android.util.Log
import dji.sdk.keyvalue.key.DJIKeyInfo
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.common.LocationCoordinate3D
import dji.sdk.keyvalue.value.flightcontroller.FlightControlAuthority
import dji.sdk.keyvalue.value.flightcontroller.FlightControlAuthorityChangeReason
import dji.sdk.keyvalue.value.flightcontroller.FlightMode
import dji.sdk.keyvalue.value.flightcontroller.FlightCoordinateSystem
import dji.sdk.keyvalue.value.flightcontroller.FlyToMissionState
import dji.sdk.keyvalue.value.flightcontroller.FlyToMode
import dji.sdk.keyvalue.value.flightcontroller.GPSSignalLevel
import dji.sdk.keyvalue.value.flightcontroller.RollPitchControlMode
import dji.sdk.keyvalue.value.flightcontroller.VerticalControlMode
import dji.sdk.keyvalue.value.flightcontroller.VirtualStickFlightControlParam
import dji.sdk.keyvalue.value.flightcontroller.YawControlMode
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.virtualstick.VirtualStickManager
import dji.v5.manager.aircraft.virtualstick.VirtualStickState
import dji.v5.manager.aircraft.virtualstick.VirtualStickStateListener
import dji.v5.manager.intelligent.IMissionInfoListener
import dji.v5.manager.intelligent.IntelligentFlightInfo
import dji.v5.manager.intelligent.IntelligentFlightInfoListener
import dji.v5.manager.intelligent.IntelligentFlightManager
import dji.v5.manager.intelligent.MissionType
import dji.v5.manager.intelligent.flyto.FlyToInfo
import dji.v5.manager.intelligent.flyto.FlyToParam
import dji.v5.manager.intelligent.flyto.FlyToTarget
import dji.v5.manager.interfaces.IIntelligentFlightManager
import dji.v5.manager.interfaces.IVirtualStickManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 基于 MSDK 的自动飞行网关实现。
 *
 * 设计意图：集中管理智能飞行、飞控 Key 与虚拟摇杆三类订阅的生命周期，并把 SDK 回调统一切回
 * 主线程后再更新快照、发出事件；业务层只读 [snapshot] 与 [events]，不直接持有 SDK 对象。
 *
 * 线程约束：`startFlyTo` / `stopFlyTo` / `startMonitoring` / `stopMonitoring` 在主线程调用；
 * SDK 回调来源不固定，内部统一 `mainHandler.post`。
 */
class SdkAutoFlightGateway(
    private val intelligentFlightManager: IIntelligentFlightManager =
        IntelligentFlightManager.getInstance(),
    private val virtualStickManager: IVirtualStickManager =
        VirtualStickManager.getInstance(),
    private val keyManager: KeyManager = KeyManager.getInstance(),
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
) : AutoFlightGateway {

    private val mutableEvents = MutableSharedFlow<AutoFlightEvent>(extraBufferCapacity = 16)
    override val events: SharedFlow<AutoFlightEvent> = mutableEvents.asSharedFlow()

    private var monitoring = false
    private val keyHolder = Any()

    /** 由 FR-9 的启用路径置位；FR-7 恒为 false，因此退出时不会触碰虚拟摇杆。 */
    internal var virtualStickOwned = false

    @Volatile
    private var flyToSupported = false

    @Volatile
    private var runningMission: MissionType = MissionType.NONE

    @Volatile
    private var connected = false

    @Volatile
    private var flying = false

    @Volatile
    private var gpsReady = false

    @Volatile
    private var lowBattery = false

    @Volatile
    private var criticalLowBattery = false

    @Volatile
    private var fcTakeover = false

    @Volatile
    private var authority = FlightAuthority.UNKNOWN

    @Volatile
    private var virtualStickEnabled = false

    @Volatile
    private var virtualStickAdvanced = false

    private val intelligentInfoListener = object : IntelligentFlightInfoListener {
        override fun onIntelligentFlightInfoUpdate(info: IntelligentFlightInfo) {
            val supported = info.supportedMissions.orEmpty().contains(MissionType.FLY_TO)
            val running = info.runningMission ?: MissionType.NONE
            post {
                flyToSupported = supported
                runningMission = running
                emitSafetyChanged()
            }
        }

        override fun onIntelligentFlightErrorUpdate(error: IDJIError) {
            Log.e(TAG, "intelligent flight error: $error")
        }
    }

    private val missionInfoListener = object : IMissionInfoListener<FlyToInfo, FlyToTarget> {
        override fun onMissionInfoUpdate(info: FlyToInfo) {
            val stage = info.state.toDomain()
            val exitReason = info.exitReason?.name
            post {
                Log.d(TAG, "flyto state=$stage exit=$exitReason")
                mutableEvents.tryEmit(AutoFlightEvent.FlyToStageChanged(stage, exitReason))
            }
        }

        override fun onMissionTargetUpdate(target: FlyToTarget) {
            // 目标回传不改变业务状态，本次不消费。
        }
    }

    private val virtualStickStateListener = object : VirtualStickStateListener {
        override fun onVirtualStickStateUpdate(stickState: VirtualStickState) {
            val mapped = stickState.currentFlightControlAuthorityOwner.toDomain()
            val enabled = stickState.isVirtualStickEnable
            val advanced = stickState.isVirtualStickAdvancedModeEnabled
            post {
                authority = mapped
                virtualStickEnabled = enabled
                virtualStickAdvanced = advanced
                mutableEvents.tryEmit(
                    AutoFlightEvent.AuthorityChanged(mapped, enabled, advanced)
                )
            }
        }

        override fun onChangeReasonUpdate(reason: FlightControlAuthorityChangeReason) {
            Log.d(TAG, "authority change reason=$reason")
        }
    }

    override fun startMonitoring() {
        if (monitoring) return
        monitoring = true
        runCatching {
            intelligentFlightManager.addIntelligentFlightInfoListener(intelligentInfoListener)
        }.onFailure { Log.e(TAG, "register intelligent listener failed: ${it.message}") }
        runCatching {
            intelligentFlightManager.getFlyToMissionManager()
                .addMissionInfoListener(missionInfoListener)
        }.onFailure { Log.e(TAG, "register flyto listener failed: ${it.message}") }
        runCatching {
            virtualStickManager.setVirtualStickStateListener(virtualStickStateListener)
        }.onFailure { Log.e(TAG, "register virtual stick listener failed: ${it.message}") }

        listen(FlightControllerKey.KeyConnection) { connected = it == true; emitSafetyChanged() }
        listen(FlightControllerKey.KeyIsFlying) { flying = it == true; emitSafetyChanged() }
        listen(FlightControllerKey.KeyGPSSignalLevel) {
            gpsReady = it.isUsableGps()
            emitSafetyChanged()
        }
        listen(FlightControllerKey.KeyIsLowBatteryWarning) {
            lowBattery = it == true
            emitSafetyChanged()
        }
        listen(FlightControllerKey.KeyIsSeriousLowBatteryWarning) {
            criticalLowBattery = it == true
            emitSafetyChanged()
        }
        listen(FlightControllerKey.KeyFlightMode) {
            fcTakeover = it.isFcTakeover()
            emitSafetyChanged()
        }
        Log.d(TAG, "listeners registered")
        emitSafetyChanged()
    }

    override fun stopMonitoring() {
        if (!monitoring) return
        monitoring = false
        // 本会话开启过虚拟摇杆时，断开前先尽力归零并关闭，避免留下无主控制状态。
        if (virtualStickOwned) {
            runCatching {
                sendVirtualStickCommand(VirtualStickCommand.NEUTRAL)
                virtualStickManager.setVirtualStickAdvancedModeEnabled(false)
                virtualStickManager.disableVirtualStick(object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() = Unit

                    override fun onFailure(error: IDJIError) {
                        Log.e(TAG, "disconnect release failed code=${error.errorCode()} desc=${error.description()}")
                    }
                })
            }.onFailure { Log.e(TAG, "disconnect release failed: ${it.message}") }
        }
        runCatching {
            intelligentFlightManager.removeIntelligentFlightInfoListener(intelligentInfoListener)
        }
        runCatching {
            intelligentFlightManager.getFlyToMissionManager()
                .removeMissionInfoListener(missionInfoListener)
        }
        runCatching {
            virtualStickManager.removeVirtualStickStateListener(virtualStickStateListener)
        }
        runCatching { keyManager.cancelListen(keyHolder) }
        connected = false
        flying = false
        gpsReady = false
        lowBattery = false
        criticalLowBattery = false
        fcTakeover = false
        flyToSupported = false
        runningMission = MissionType.NONE
        authority = FlightAuthority.UNKNOWN
        virtualStickEnabled = false
        virtualStickAdvanced = false
        virtualStickOwned = false
        Log.d(TAG, "listeners released")
    }

    override fun snapshot(): AutoFlightSafetySnapshot = AutoFlightSafetySnapshot(
        aircraftConnected = connected,
        flying = flying,
        gpsReady = gpsReady,
        lowBattery = lowBattery,
        criticalLowBattery = criticalLowBattery,
        flyToSupported = flyToSupported,
        missionIdle = runningMission == MissionType.NONE,
        authority = authority,
        fcTakeover = fcTakeover,
        virtualStickEnabled = virtualStickEnabled,
        virtualStickAdvancedMode = virtualStickAdvanced,
    )

    override fun startFlyTo(
        target: AutoFlightTarget,
        heightM: Int,
        onResult: (StartOutcome) -> Unit,
    ) {
        val altitude = target.altitudeM
        if (!target.isComplete || altitude == null) {
            Log.e(TAG, "start rejected: incomplete target")
            onResult(StartOutcome.Failed("TARGET_INCOMPLETE", null))
            return
        }
        val flyToTarget = FlyToTarget().apply {
            setTargetLocation(LocationCoordinate3D(target.latitude, target.longitude, altitude))
            setSecurityTakeoffHeight(heightM)
            // SDK 的 doOnFlyToTarget 要求 maxSpeed > 0，否则直接以参数非法拒绝启动。
            setMaxSpeed(DEFAULT_MAX_SPEED_MPS)
        }
        Log.d(TAG, "start flyto lat=${target.latitude} lon=${target.longitude} height=$heightM")
        val manager = intelligentFlightManager.getFlyToMissionManager()
        // SDK 的 updateMissionParam 用 if/else 判断 mode 与 height：同时传时高度分支不执行。
        // 因此先只下发模式、再只下发高度，最后以空参数启动，避免目标高度被忽略。
        manager.updateMissionParam(
            FlyToParam().apply { setFlyToMode(FlyToMode.SET_HEIGHT) },
            null,
        )
        manager.updateMissionParam(
            FlyToParam().apply { setHeight(heightM) },
            null,
        )
        manager.startMission(
            flyToTarget,
            null,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    post {
                        Log.d(TAG, "start accepted")
                        onResult(StartOutcome.Accepted)
                    }
                }

                override fun onFailure(error: IDJIError) {
                    post {
                        Log.e(TAG, "start failed code=${error.errorCode()} desc=${error.description()}")
                        onResult(StartOutcome.Failed(error.errorCode(), error.description()))
                    }
                }
            },
        )
    }

    override fun stopFlyTo(onResult: (StopOutcome) -> Unit) {
        Log.d(TAG, "stop flyto requested")
        intelligentFlightManager.getFlyToMissionManager().stopMission(
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    post {
                        Log.d(TAG, "stop accepted")
                        onResult(StopOutcome.Accepted)
                    }
                }

                override fun onFailure(error: IDJIError) {
                    post {
                        Log.e(TAG, "stop failed code=${error.errorCode()} desc=${error.description()}")
                        onResult(StopOutcome.Failed(error.errorCode(), error.description()))
                    }
                }
            },
        )
    }

    override fun enableVirtualStick(onResult: (StartOutcome) -> Unit) {
        Log.d(TAG, "enable virtual stick requested")
        virtualStickManager.enableVirtualStick(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                post {
                    virtualStickOwned = true
                    Log.d(TAG, "enable virtual stick accepted")
                    onResult(StartOutcome.Accepted)
                }
            }

            override fun onFailure(error: IDJIError) {
                post {
                    Log.e(TAG, "enable virtual stick failed code=${error.errorCode()} desc=${error.description()}")
                    onResult(StartOutcome.Failed(error.errorCode(), error.description()))
                }
            }
        })
    }

    override fun setVirtualStickAdvancedMode(enabled: Boolean) {
        Log.d(TAG, "set virtual stick advanced=$enabled")
        runCatching { virtualStickManager.setVirtualStickAdvancedModeEnabled(enabled) }
            .onFailure { Log.e(TAG, "set advanced mode failed: ${it.message}") }
    }

    override fun sendVirtualStickCommand(command: VirtualStickCommand) {
        val param = VirtualStickFlightControlParam().apply {
            setRollPitchControlMode(RollPitchControlMode.VELOCITY)
            setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY)
            setYawControlMode(YawControlMode.ANGULAR_VELOCITY)
            setVerticalControlMode(VerticalControlMode.VELOCITY)
            setRoll(command.roll)
            setPitch(command.pitch)
            setYaw(command.yaw)
            setVerticalThrottle(command.verticalThrottle)
        }
        virtualStickManager.sendVirtualStickAdvancedParam(param)
    }

    override fun disableVirtualStick(onResult: (StopOutcome) -> Unit) {
        Log.d(TAG, "disable virtual stick requested")
        virtualStickManager.disableVirtualStick(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                post {
                    virtualStickOwned = false
                    Log.d(TAG, "disable virtual stick accepted")
                    onResult(StopOutcome.Accepted)
                }
            }

            override fun onFailure(error: IDJIError) {
                post {
                    Log.e(TAG, "disable virtual stick failed code=${error.errorCode()} desc=${error.description()}")
                    onResult(StopOutcome.Failed(error.errorCode(), error.description()))
                }
            }
        })
    }

    override fun releaseVirtualStickIfOwned(onDone: () -> Unit) {
        if (!virtualStickOwned) {
            onDone()
            return
        }
        runCatching {
            // 高级模式下普通杆量无效，必须发送中立高级参数；普通杆量归零作为兜底。
            if (virtualStickAdvanced) {
                sendVirtualStickCommand(VirtualStickCommand.NEUTRAL)
            }
            virtualStickManager.getLeftStick().apply {
                setHorizontalPosition(0)
                setVerticalPosition(0)
            }
            virtualStickManager.getRightStick().apply {
                setHorizontalPosition(0)
                setVerticalPosition(0)
            }
            virtualStickManager.setVirtualStickAdvancedModeEnabled(false)
        }
        Log.d(TAG, "virtual stick release requested")
        virtualStickManager.disableVirtualStick(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                post {
                    virtualStickOwned = false
                    Log.d(TAG, "virtual stick released")
                    onDone()
                }
            }

            override fun onFailure(error: IDJIError) {
                post {
                    Log.e(TAG, "virtual stick release failed code=${error.errorCode()} desc=${error.description()}")
                    onDone()
                }
            }
        })
    }

    private fun <T> listen(
        keyInfo: DJIKeyInfo<T>,
        onValue: (T?) -> Unit,
    ) {
        runCatching {
            keyManager.listen(
                KeyTools.createKey(keyInfo),
                keyHolder,
            ) { _, newValue ->
                if (newValue != null) post { onValue(newValue) }
            }
        }.onFailure { Log.e(TAG, "listen failed: ${it.message}") }
    }

    private fun post(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private fun emitSafetyChanged() {
        mutableEvents.tryEmit(AutoFlightEvent.SafetyChanged)
    }

    private companion object {
        const val TAG = "GasAutoFlight"

        /** 巡航速度上限（m/s）；SDK 要求 > 0，取保守值。 */
        const val DEFAULT_MAX_SPEED_MPS = 10
    }
}

/** FlyTo 状态映射；未知取值不猜测为成功。 */
internal fun FlyToMissionState?.toDomain(): FlyToStage = when (this) {
    FlyToMissionState.PREPARE -> FlyToStage.PREPARE
    FlyToMissionState.LAUNCH_PRE -> FlyToStage.LAUNCH_PRE
    FlyToMissionState.LAUNCH -> FlyToStage.LAUNCH
    FlyToMissionState.MARCHON -> FlyToStage.MARCHON
    FlyToMissionState.FINISHED -> FlyToStage.FINISHED
    FlyToMissionState.INTERRUPTED -> FlyToStage.INTERRUPTED
    else -> FlyToStage.UNKNOWN
}

/** 控制权枚举映射。 */
internal fun FlightControlAuthority?.toDomain(): FlightAuthority = when (this) {
    FlightControlAuthority.RC -> FlightAuthority.RC
    FlightControlAuthority.MSDK -> FlightAuthority.MSDK
    FlightControlAuthority.OSDK -> FlightAuthority.OSDK
    FlightControlAuthority.AIRPORT -> FlightAuthority.AIRPORT
    FlightControlAuthority.AUTO_TEST -> FlightAuthority.AUTO_TEST
    else -> FlightAuthority.UNKNOWN
}

/** GPS 等级是否达到可悬停水平。 */
internal fun GPSSignalLevel?.isUsableGps(): Boolean = when (this) {
    GPSSignalLevel.LEVEL_3,
    GPSSignalLevel.LEVEL_4,
    GPSSignalLevel.LEVEL_5,
    GPSSignalLevel.LEVEL_10,
    -> true

    else -> false
}

/** 飞控是否已进入返航/自动降落。 */
internal fun FlightMode?.isFcTakeover(): Boolean = when (this) {
    FlightMode.GO_HOME,
    FlightMode.AUTO_LANDING,
    FlightMode.FORCE_LANDING,
    -> true

    else -> false
}
