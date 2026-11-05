package com.dji.sample.gasdetection.gas.task

/** 任务列表分组。 */
enum class TaskGroup {
    /** 进行中：巡检/收尾。 */
    ACTIVE,

    /** 待处理：已下发或已接受。 */
    PENDING,

    /** 已完成：完成、取消或拒绝。 */
    DONE,
}

/**
 * 任务列表的纯判定逻辑。
 *
 * 设计意图：把「选中哪一条、切换后阶段怎么重建、什么时候禁止切换、归到哪个分组」从
 * [TaskStore] 与界面里剥离，避免多处重复判断，并让这些规则可以在纯 JVM 单测里覆盖。
 */
object TaskSelection {

    /** 按后端状态重建本地阶段；未知/终态回到空闲。 */
    fun stageFor(status: JobStatus): TaskStage = when (status) {
        JobStatus.DISPATCHED -> TaskStage.NEW_ORDER
        JobStatus.ACCEPTED -> TaskStage.PREP
        JobStatus.IN_PROGRESS -> TaskStage.MANUAL
        JobStatus.FINALIZING -> TaskStage.FINALIZING
        else -> TaskStage.IDLE
    }

    /**
     * 选择刷新后的选中项。
     *
     * 优先保留仍然存在的当前选中项；否则落到第一个进行中任务；再否则落到列表第一项；
     * 空列表返回 `null`。
     */
    fun chooseSelection(jobs: List<GasJob>, currentId: String?): String? {
        if (currentId != null && jobs.any { it.jobId == currentId }) return currentId
        return jobs.firstOrNull { groupOf(it.status) == TaskGroup.ACTIVE }?.jobId
            ?: jobs.firstOrNull()?.jobId
    }

    /** 本地处于这些阶段时禁止切换任务，避免丢失自动作业、搜索或预检上下文。 */
    fun canSwitch(stage: TaskStage): Boolean = when (stage) {
        TaskStage.PREP,
        TaskStage.PREP_READY,
        TaskStage.FLYTO,
        TaskStage.SEARCH,
        TaskStage.FOLLOW,
        -> false

        else -> true
    }

    /** 状态归组：进行中 / 待处理 / 已完成。 */
    fun groupOf(status: JobStatus): TaskGroup = when (status) {
        JobStatus.IN_PROGRESS,
        JobStatus.FINALIZING,
        -> TaskGroup.ACTIVE

        JobStatus.DISPATCHED,
        JobStatus.ACCEPTED,
        -> TaskGroup.PENDING

        JobStatus.COMPLETED,
        JobStatus.CANCELLED,
        JobStatus.REJECTED,
        -> TaskGroup.DONE
    }
}
