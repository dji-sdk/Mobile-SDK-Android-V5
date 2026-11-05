package com.dji.sample.gasdetection.gas.follow

import com.dji.sample.gasdetection.gas.autoflight.FlightAuthority

/**
 * 跟随阶段。
 *
 * 设计意图：只表达插件这一侧的虚拟摇杆会话推进到哪一步。`ARMING` 表示 enable 已下发但尚未
 * 取得权威控制权，此时绝不能发送控制量；`ACTIVE` 才允许控制环运行。
 */
enum class FollowPhase {
    /** 未跟随或已复位。 */
    IDLE,

    /** enable 已下发，等待控制权与高级模式确认。 */
    ARMING,

    /** 控制权与高级模式均确认，控制环运行中。 */
    ACTIVE,

    /** 已停止控制，正在执行释放序列并等待状态确认。 */
    RELEASING,

    /** 已正常退出。 */
    STOPPED,

    /** 建立或运行失败，已完成（或尝试完成）清理。 */
    ABORTED,
}

/** 跟随退出/中止原因；文案由界面层映射，具体 SDK 错误只进日志。 */
enum class FollowAbortReason {
    /** 识别不再处于锁定状态。 */
    TARGET_LOST,

    /** 已锁定目标观测超过新鲜窗口。 */
    TARGET_STALE,

    /** 原生目标矩形不是归一化坐标，无法计算控制量。 */
    INVALID_RECT,

    /** 控制权不再归属 MSDK 或被其他端占据。 */
    CONTROL_AUTHORITY_LOST,

    /** 低电或严重低电。 */
    LOW_BATTERY,

    /** 定位不满足跟随要求。 */
    GPS_WEAK,

    /** 飞控进入返航或自动降落。 */
    FC_TAKEOVER,

    /** 飞机断连或 Pilot 断开。 */
    DISCONNECTED,

    /** `enableVirtualStick` 命令失败。 */
    ENABLE_FAILED,

    /** 5 秒内未确认控制权。 */
    AUTHORITY_TIMEOUT,

    /** 3 秒内未确认高级模式。 */
    ADVANCED_TIMEOUT,

    /** 释放超时，未确认控制权交还。 */
    RELEASE_TIMEOUT,

    /** 飞手主动退出。 */
    USER_EXIT,

    /** 任务结束。 */
    TASK_FINISHED,

    /** 任务阶段机拒绝进入 FOLLOW。 */
    STAGE_REJECTED,
}

/** 开始跟随被拒绝的原因。 */
enum class FollowBlock {
    /** 当前任务阶段不是搜索。 */
    STAGE_NOT_READY,

    /** 尚未锁定船舶。 */
    NOT_LOCKED,

    /** 目标观测过期。 */
    TARGET_STALE,

    /** 目标区域不是归一化坐标。 */
    INVALID_RECT,

    /** 飞行安全条件不满足（连接/起飞/定位/电量/飞控接管/智能任务占用）。 */
    SAFETY,

    /** 控制权被其它端占用。 */
    AUTHORITY_OCCUPIED,

    /** 已有跟随会话在进行。 */
    ALREADY_ACTIVE,
}

/** 一次受限控制量；横滚与垂直在本次设计中恒为 0。 */
data class FollowCommand(
    val roll: Double = 0.0,
    val pitch: Double = 0.0,
    val yaw: Double = 0.0,
    val verticalThrottle: Double = 0.0,
) {
    /** 四轴是否全部为 0。 */
    val isNeutral: Boolean
        get() = roll == 0.0 && pitch == 0.0 && yaw == 0.0 && verticalThrottle == 0.0
}

/** 跟随状态快照，供入口页与浮窗共同渲染。 */
data class FollowState(
    val phase: FollowPhase = FollowPhase.IDLE,
    val taskId: String? = null,
    /** 最近一次状态回调的控制权归属。 */
    val authority: FlightAuthority = FlightAuthority.UNKNOWN,
    /** 退出/中止原因；运行中为 `null`。 */
    val reason: FollowAbortReason? = null,
    /** 是否已通过状态回调确认虚拟摇杆关闭。 */
    val releaseConfirmed: Boolean = false,
) {
    /** 会话是否占用中（用于浮窗按钮与阶段展示）。 */
    val isBusy: Boolean
        get() = phase == FollowPhase.ARMING || phase == FollowPhase.ACTIVE || phase == FollowPhase.RELEASING
}

/** 控制律常量；全部为显式常量，便于真机标定时统一调整。 */
object FollowLimits {
    /** 控制环周期（10 Hz）。 */
    const val TICK_MS = 100L

    /** 水平速度上限（m/s）。 */
    const val MAX_HORIZONTAL_MPS = 1.0

    /** 偏航角速度上限（°/s）。 */
    const val MAX_YAW_DPS = 10.0

    /** 偏航比例增益（°/s 每单位横向误差）。 */
    const val YAW_GAIN_DPS = 30.0

    /** 横向死区（归一化横坐标）。 */
    const val YAW_DEADBAND = 0.05

    /** 期望目标框宽度下限（占画面比例）。 */
    const val WIDTH_MIN = 0.15

    /** 期望目标框宽度上限（占画面比例）。 */
    const val WIDTH_MAX = 0.30

    /** 距离误差到速度的增益（m/s 每单位宽度误差）。 */
    const val WIDTH_GAIN = 5.0

    /** 目标观测新鲜窗口（毫秒）。 */
    const val FRESHNESS_MS = 1_000L
}
