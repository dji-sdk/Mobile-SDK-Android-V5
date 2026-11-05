package com.dji.sample.gasdetection.gas.recognition

import com.dji.sample.gasdetection.gas.model.VesselReference

/**
 * 原生识别目标的类别投影。
 *
 * 设计意图：领域层不依赖 SDK 枚举，SDK 的 `AutoSensingTargetType` 在适配层映射到本枚举；
 * 只有 [BOAT] 会进入船舶锁定流程，其它取值只记录日志。
 */
enum class TargetKind {
    PERSON,
    CAR,
    BOAT,
    CUSTOM,
    UNKNOWN,
}

/**
 * 目标区域快照（原生识别矩形）。
 *
 * 设计意图：官方文档未说明坐标单位与原点，本轮只原样保存供后续跟随/叠加使用，
 * 不参与锁定判定，也不对外承诺具体坐标语义。
 */
data class TargetRect(
    val centerX: Double,
    val centerY: Double,
    val width: Double,
    val height: Double,
)

/** 单个原生识别目标的投影，不含任何 `dji.*` 类型。 */
data class SensingTargetInfo(
    /** 原生目标索引；只作为本次会话内的引用，不是永久船舶标识。 */
    val index: Int,
    val kind: TargetKind,
    /** 原生标签；可能为空。 */
    val label: String? = null,
    /** 目标区域；原生未提供时为 `null`。 */
    val rect: TargetRect? = null,
)

/** 一帧识别结果，对应 SDK `AutoSensingInfo` 的一次回调。 */
data class SensingFrame(
    /** 原生开启态（`isAutoSensingEnable`），是「识别已开启」的唯一权威。 */
    val enabled: Boolean,
    val targets: List<SensingTargetInfo> = emptyList(),
)

/**
 * 识别会话阶段。
 *
 * 设计意图：只表达插件这一侧推进到哪一步；识别是否真的开启由 [RecognitionState] 中的权威
 * 开启态驱动，命令回调成功不会直接进入 [SENSING]。
 */
enum class RecognitionPhase {
    /** 未搜索或已复位。 */
    IDLE,

    /** 开启命令已下发，等待权威开启态。 */
    STARTING,

    /** 搜索中，尚未锁定船舶。 */
    SENSING,

    /** 已锁定船舶。 */
    LOCKED,

    /** 识别不可用；任务停留在搜索阶段，等待飞手重试或退出。 */
    DEGRADED,

    /** 已释放监听。 */
    STOPPED,
}

/** 识别不可用的原因类别；具体错误码只进日志，不在这里承载。 */
enum class RecognitionFailure {
    /** `startAutoSensing` 命令失败。 */
    START_FAILED,

    /** 命令被接受后 5 秒内未收到权威开启态。 */
    ENABLE_TIMEOUT,

    /** 已开启后权威开启态变为 false。 */
    ENABLE_LOST,
}

/** 开始搜索被拒绝的原因；入口页据此展示可读原因。 */
enum class VesselSearchBlock {
    /** 自动阶段尚未到达搜索点。 */
    NOT_ARRIVED,

    /** 飞机未连接。 */
    AIRCRAFT_DISCONNECTED,

    /** 飞机未起飞。 */
    NOT_FLYING,

    /** 其它智能飞行任务占用中。 */
    MISSION_RUNNING,

    /** 尚未收到智能飞行状态，不能确认是否空闲。 */
    MISSION_UNKNOWN,

    /** 当前任务阶段不允许进入搜索。 */
    STAGE_NOT_READY,
}

/** 前置条件快照；[missionIdle] 为 `null` 表示尚未收到智能飞行状态。 */
data class RecognitionPreconditions(
    val connected: Boolean,
    val flying: Boolean,
    val missionIdle: Boolean?,
)

/**
 * 识别状态快照，供入口页与浮窗共同渲染。
 *
 * [vessel] 为当前锁定对象（业务标识与确认状态），[nativeIndex] 只用于界面展示目标索引；
 * [heldMs] 为连续有效时长，由控制器按整秒发布。
 */
data class RecognitionState(
    val phase: RecognitionPhase = RecognitionPhase.IDLE,
    val taskId: String? = null,
    /** 当前达到连续性计数（未锁定）或已锁定目标的原生索引。 */
    val nativeIndex: Int? = null,
    /** 当前目标的连续有效次数。 */
    val hits: Int = 0,
    val requiredHits: Int = VesselTracker.REQUIRED_HITS,
    /** 当前锁定对象；未锁定时为 `null`。 */
    val vessel: VesselReference? = null,
    /** 连续有效时长（毫秒，按整秒发布）。 */
    val heldMs: Long = 0,
    /** 是否刚刚失去已锁定目标（用于展示「已失锁」，重新出现候选或再次锁定时清除）。 */
    val lost: Boolean = false,
    /** 当前锁定目标的最新矩形；未锁定或原生未提供时为 `null`。 */
    val targetRect: TargetRect? = null,
    /** 当前锁定目标最近一次观测时间（毫秒）。 */
    val targetSeenAtMs: Long? = null,
    /** 识别不可用原因；其余阶段为 `null`。 */
    val failure: RecognitionFailure? = null,
    /** 最近一次开始搜索被拒绝的原因；成功进入搜索后清空。 */
    val block: VesselSearchBlock? = null,
) {
    /** 是否处于搜索相关状态（用于浮窗是否展示船舶状态行）。 */
    val isSearching: Boolean
        get() = phase == RecognitionPhase.STARTING ||
            phase == RecognitionPhase.SENSING ||
            phase == RecognitionPhase.LOCKED ||
            phase == RecognitionPhase.DEGRADED
}
