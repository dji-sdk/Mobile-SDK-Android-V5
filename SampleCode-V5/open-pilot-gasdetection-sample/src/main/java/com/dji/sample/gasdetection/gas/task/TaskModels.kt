package com.dji.sample.gasdetection.gas.task

/**
 * 演示后端任务状态，与 FR-3 的 `status` 字段一一对应。
 *
 * 设计意图：本地只镜像服务端状态，不自行扩展；未知字符串按 [DISPATCHED] 之前的非法值处理，
 * 由解析层丢弃整个任务而不是猜测。
 */
enum class JobStatus {
    DISPATCHED,
    ACCEPTED,
    IN_PROGRESS,
    FINALIZING,
    COMPLETED,
    CANCELLED,
    REJECTED;

    companion object {
        /** 解析服务端状态；无法识别返回 `null`，调用方按契约错误处理。 */
        fun parse(value: String?): JobStatus? =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}

/**
 * 本地任务阶段。
 *
 * `IDLE` 到 `PREP_READY` 是任务接单与预检；`PREP_READY` 经飞手确认后进入 `FLYTO`（自动到点），
 * 到达后由飞手显式选择进入 `SEARCH`（原生识别与目标锁定）；接管或结束分别回 `MANUAL` /
 * `FINALIZING`。`FOLLOW` 是 FR-9 的占位，任何事件都不会进入。
 */
enum class TaskStage {
    IDLE,
    NEW_ORDER,
    TASK_DETAIL,
    PREP,
    NOT_READY,
    PREP_READY,
    MANUAL,
    FINALIZING,
    FLYTO,
    SEARCH,
    FOLLOW,
}

/**
 * 任务的单个气体通道配置。
 *
 * 设计意图：只承载通道身份与阈值；阈值属于任务配置，不在插件内写死。规则标识由告警侧统一
 * 使用固定常量（`AlertRules`），任务配置不提供规则字段。
 */
data class JobChannel(
    val channelId: String,
    val displayName: String,
    val unit: String,
    val threshold: Double? = null,
)

/** 目标点；后端未提供时保持 `null`，界面显示「未提供」。 */
data class GasTarget(
    val latitude: Double,
    val longitude: Double,
    val altitudeM: Double? = null,
)

/** 一条检测任务。 */
data class GasJob(
    val jobId: String,
    val title: String,
    val status: JobStatus,
    val target: GasTarget? = null,
    val channels: List<JobChannel> = emptyList(),
    val safetyBoundaryM: Double? = null,
    val requirements: String? = null,
    val deadline: String? = null,
)

/** 预检单项结论；未通过时用 [failure] 表达原因，由界面层映射文案。 */
data class PreflightItem(
    val id: PreflightId,
    val passed: Boolean,
    val failure: PreflightFailure? = null,
)

enum class PreflightId {
    PAYLOAD_ONLINE,
    CHANNEL_CONFIG,
    AUTO_FLIGHT_CAPABILITY,
    AIRCRAFT_CONNECTED,
    AIRCRAFT_FLYING,
    GPS_READY,
    BATTERY_OK,
    MISSION_IDLE,
    CONTROL_AUTHORITY,
    TARGET_READY,
}

/** 预检失败原因。 */
enum class PreflightFailure {
    PAYLOAD_OFFLINE,
    CONFIG_INVALID,
    AUTO_FLIGHT_UNSUPPORTED,
    AUTO_FLIGHT_UNKNOWN,
    AIRCRAFT_DISCONNECTED,
    AIRCRAFT_NOT_FLYING,
    GPS_WEAK,
    BATTERY_LOW,
    MISSION_RUNNING,
    AUTHORITY_OCCUPIED,
    TARGET_MISSING,
}

/** 预检汇总；[allPassed] 为 false 时必须逐项展示未通过原因。 */
data class PreflightResult(
    val items: List<PreflightItem>,
) {
    val allPassed: Boolean get() = items.all { it.passed }
}

/** 任务同步状态；失败时保留上一次成功拉取的任务。 */
sealed class TaskSyncState {
    data object Idle : TaskSyncState()
    data object Loading : TaskSyncState()
    data object Ready : TaskSyncState()

    /** 保留原始错误类型，由界面层映射成可读文案。 */
    data class Failed(val error: ApiError) : TaskSyncState()

    /** 已有进行中任务时的冲突提示，不覆盖当前任务。 */
    data object Conflict : TaskSyncState()
}

/** 任务界面状态快照；浮窗与入口页共用同一份。 */
data class TaskUiState(
    /** 当前选中任务；列表为空或未选中时为 `null`。 */
    val job: GasJob? = null,
    /** 后端返回的全部任务，按结果顺序保存，分组由界面层决定。 */
    val jobs: List<GasJob> = emptyList(),
    /** 当前选中的任务标识。 */
    val selectedJobId: String? = null,
    /** 是否同时存在多个进行中任务；只做提示，不阻止查看。 */
    val multipleActive: Boolean = false,
    val stage: TaskStage = TaskStage.IDLE,
    val sync: TaskSyncState = TaskSyncState.Idle,
    val preflight: PreflightResult? = null,
) {
    /** 是否存在进行中的任务上下文（用于浮窗显示条件）。 */
    val hasActiveTask: Boolean
        get() = job != null && stage in ACTIVE_STAGES

    private companion object {
        val ACTIVE_STAGES = setOf(
            TaskStage.NEW_ORDER,
            TaskStage.TASK_DETAIL,
            TaskStage.PREP,
            TaskStage.NOT_READY,
            TaskStage.PREP_READY,
            TaskStage.MANUAL,
            TaskStage.FLYTO,
            TaskStage.SEARCH,
            TaskStage.FOLLOW,
        )
    }
}
