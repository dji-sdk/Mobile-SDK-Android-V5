package com.dji.sample.gasdetection.gas.autoflight

/**
 * 自动飞行本地阶段。
 *
 * 设计意图：只表达「插件这一侧推进到哪一步」，不代表 SDK 的业务终态；真正的飞行终态由
 * [FlyToStage] 回调决定。`CHECKING` 不在这里，它复用任务阶段的 `PREP` / `PREP_READY`。
 */
enum class AutoFlightPhase {
    /** 未启动或已收尾。 */
    IDLE,

    /** 启动命令已下发，尚未收到权威状态。 */
    STARTING,

    /** 飞行中（含起飞前准备、起飞、巡航）。 */
    FLYING,

    /** 权威状态为 FINISHED。 */
    ARRIVED,

    /** 接管态；只能由飞手确认后回到任务阶段 MANUAL。 */
    HANDOVER,
}

/** FlyTo 权威状态的自有映射，避免领域层依赖 SDK 枚举。 */
enum class FlyToStage {
    PREPARE,
    LAUNCH_PRE,
    LAUNCH,
    MARCHON,
    FINISHED,
    INTERRUPTED,
    UNKNOWN,
    ;

    /** 是否表示已经进入实际飞行阶段。 */
    val isProgressing: Boolean
        get() = this == PREPARE || this == LAUNCH_PRE || this == LAUNCH || this == MARCHON
}

/** 飞行控制权归属（只读订阅结果）。 */
enum class FlightAuthority {
    RC,
    MSDK,
    OSDK,
    AIRPORT,
    AUTO_TEST,
    UNKNOWN,
}

/** 进入接管态的原因；展示层按此映射文案，不在状态机里写死字符串。 */
enum class HandoverReason {
    /** 飞手主动退出自动。 */
    USER_EXIT,

    /** SDK 报告任务中断。 */
    TASK_INTERRUPTED,

    /** 启动命令没有被确认。 */
    START_NOT_CONFIRMED,

    /** 状态在规定时间内没有推进。 */
    STATE_NOT_ADVANCED,

    /** 飞机连接中断。 */
    DISCONNECTED,

    /** 低电或严重低电告警。 */
    BATTERY_LOW,

    /** 定位信号不足。 */
    GPS_LOST,

    /** 飞控进入返航/自动降落等安全接管。 */
    FC_TAKEOVER,

    /** 紧急停止命令失败。 */
    STOP_FAILED,
}

/** 看门狗判定的超时类型。 */
enum class AutoFlightTimeout {
    /** 下发启动命令后 10 秒没有任何确认。 */
    START_NOT_CONFIRMED,

    /** 命令被接受后 5 秒状态没有推进。 */
    STATE_NOT_ADVANCED,
}

/** 启动命令结果；`Accepted` 只代表命令被接受。 */
sealed class StartOutcome {
    data object Accepted : StartOutcome()
    data class Failed(val errorCode: String?, val description: String?) : StartOutcome()
}

/** 停止命令结果；只代表命令是否下发成功。 */
sealed class StopOutcome {
    data object Accepted : StopOutcome()
    data class Failed(val errorCode: String?, val description: String?) : StopOutcome()
}

/** 自动飞行对外快照，供入口页与浮窗共同渲染。 */
data class AutoFlightState(
    val phase: AutoFlightPhase = AutoFlightPhase.IDLE,
    /** 最近一次 FlyTo 权威状态；`null` 表示尚未收到。 */
    val flyToStage: FlyToStage? = null,
    /** 控制权归属；未收到订阅回调时为 UNKNOWN。 */
    val authority: FlightAuthority = FlightAuthority.UNKNOWN,
    /** 接管原因；只在 HANDOVER 阶段有值，保留首个原因。 */
    val handoverReason: HandoverReason? = null,
    /** SDK 返回的退出原因（仅诊断展示，不解析语义）。 */
    val exitReason: String? = null,
    /** 启动/停止失败的错误码与描述；用于界面与日志。 */
    val errorCode: String? = null,
    val errorDescription: String? = null,
    /** 虚拟摇杆当前是否开启（只读）。 */
    val virtualStickEnabled: Boolean = false,
    /** 虚拟摇杆高级模式是否开启（只读）。 */
    val virtualStickAdvancedMode: Boolean = false,
) {
    /** 是否处于需要用户关注自动阶段的状态。 */
    val isAutoActive: Boolean
        get() = phase == AutoFlightPhase.STARTING ||
            phase == AutoFlightPhase.FLYING ||
            phase == AutoFlightPhase.ARRIVED ||
            phase == AutoFlightPhase.HANDOVER
}

/** 任务目标点的领域表示，避免把 SDK 的坐标类型带进状态机。 */
data class AutoFlightTarget(
    val latitude: Double,
    val longitude: Double,
    val altitudeM: Double?,
) {
    /** 目标信息是否完整到可以下发 FlyTo。 */
    val isComplete: Boolean
        get() = latitude.isFinite() &&
            longitude.isFinite() &&
            altitudeM != null &&
            altitudeM!!.isFinite() &&
            altitudeM > 0.0
}

/**
 * 一帧虚拟摇杆高级参数。
 *
 * 设计意图：领域层只描述「四轴控制量」，由网关负责组装 SDK 参数与控制模式，避免把 `dji.*`
 * 类型带进跟随控制律。
 */
data class VirtualStickCommand(
    val roll: Double,
    val pitch: Double,
    val yaw: Double,
    val verticalThrottle: Double,
) {
    /** 四轴是否全部为 0。 */
    val isNeutral: Boolean
        get() = roll == 0.0 && pitch == 0.0 && yaw == 0.0 && verticalThrottle == 0.0

    companion object {
        /** 中立参数：用于退出前归零。 */
        val NEUTRAL = VirtualStickCommand(0.0, 0.0, 0.0, 0.0)
    }
}

/** 门控与展示需要的只读飞行快照。 */
data class AutoFlightSafetySnapshot(
    val aircraftConnected: Boolean = false,
    val flying: Boolean = false,
    /** GPS 等级是否达到可悬停水平（LEVEL_3 及以上，含 RTK）。 */
    val gpsReady: Boolean = false,
    val lowBattery: Boolean = false,
    val criticalLowBattery: Boolean = false,
    val flyToSupported: Boolean = false,
    val missionIdle: Boolean = false,
    val authority: FlightAuthority = FlightAuthority.UNKNOWN,
    /** 飞控是否已进入返航/自动降落/强制降落。 */
    val fcTakeover: Boolean = false,
    /** 虚拟摇杆是否已开启（权威状态回调）。 */
    val virtualStickEnabled: Boolean = false,
    /** 虚拟摇杆高级模式是否已开启（权威状态回调）。 */
    val virtualStickAdvancedMode: Boolean = false,
    val target: AutoFlightTarget? = null,
)
