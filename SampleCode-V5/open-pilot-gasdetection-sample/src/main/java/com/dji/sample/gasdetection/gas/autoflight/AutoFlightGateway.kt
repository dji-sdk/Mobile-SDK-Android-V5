package com.dji.sample.gasdetection.gas.autoflight

import kotlinx.coroutines.flow.SharedFlow

/**
 * 自动飞行网关事件。
 *
 * 设计意图：领域层只消费「发生了什么」，不接触 SDK 枚举与线程细节；连接、安全项变化用统一
 * [SafetyChanged] 通知，具体判定统一读 [AutoFlightGateway.snapshot]。
 */
sealed class AutoFlightEvent {
    /** FlyTo 权威状态变化；[exitReason] 只在 SDK 提供时非空。 */
    data class FlyToStageChanged(
        val stage: FlyToStage,
        val exitReason: String?,
    ) : AutoFlightEvent()

    /** 控制权与虚拟摇杆状态变化（只读）。 */
    data class AuthorityChanged(
        val authority: FlightAuthority,
        val virtualStickEnabled: Boolean,
        val virtualStickAdvancedMode: Boolean,
    ) : AutoFlightEvent()

    /** 连接、飞行状态、GPS、电量、飞控模式等安全相关快照发生变化。 */
    data object SafetyChanged : AutoFlightEvent()
}

/**
 * 自动飞行 SDK 交互端口。
 *
 * 设计意图：把 `IIntelligentFlightManager`、`KeyManager`、`IVirtualStickManager` 的调用收敛到
 * 一个可替换实现后面，使状态机、门控与接管逻辑能在无真机时用假实现验证。实现方负责线程切换，
 * 回调与事件必须回到主线程。
 */
interface AutoFlightGateway {

    /** 网关事件流；订阅者应常驻会话生命周期。 */
    val events: SharedFlow<AutoFlightEvent>

    /** 注册全部监听；重复调用幂等。 */
    fun startMonitoring()

    /** 精确移除全部监听并清空快照；重复调用幂等。 */
    fun stopMonitoring()

    /** 当前只读快照；目标点由调用方补齐。 */
    fun snapshot(): AutoFlightSafetySnapshot

    /** 下发一次 FlyTo；[onResult] 在命令回调后触发，`Accepted` 不代表已开始飞行。 */
    fun startFlyTo(
        target: AutoFlightTarget,
        heightM: Int,
        onResult: (StartOutcome) -> Unit,
    )

    /** 下发停止命令；只用于用户显式确认的紧急停止。 */
    fun stopFlyTo(onResult: (StopOutcome) -> Unit)

    /** 开启虚拟摇杆；[onResult] 只代表命令结果，不代表已获得控制权。 */
    fun enableVirtualStick(onResult: (StartOutcome) -> Unit)

    /** 设置高级模式开关；状态以 [snapshot] 与事件回调为准。 */
    fun setVirtualStickAdvancedMode(enabled: Boolean)

    /** 发送一帧高级控制参数；仅在高级模式确认后调用。 */
    fun sendVirtualStickCommand(command: VirtualStickCommand)

    /** 关闭虚拟摇杆；[onResult] 只代表命令结果，交还以状态回调为准。 */
    fun disableVirtualStick(onResult: (StopOutcome) -> Unit)

    /**
     * 释放本次会话可能开启的虚拟摇杆。
     *
     * FR-7 不会 enable，正常情况下直接回调；保留该入口给 FR-9 的安全退出序列。
     */
    fun releaseVirtualStickIfOwned(onDone: () -> Unit)
}
