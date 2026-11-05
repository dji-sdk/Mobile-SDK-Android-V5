package com.dji.sample.gasdetection.gas.task

/**
 * 任务阶段迁移。
 *
 * 设计意图：把「什么事件能推进到哪个阶段」收敛成纯函数，UI 与网络层只发事件、不直接赋阶段，
 * 非法迁移返回原阶段和原因，避免网络成功但界面跳错阶段。`FLYTO` 只能由飞手确认后的
 * [Event.StartAutoFlight] 进入，`SEARCH` 只能由飞手在到达后显式选择 [Event.StartSearch] 进入，
 * 接管统一回 `MANUAL`；`FOLLOW` 仍是 FR-9 的占位。
 */
object TaskStageMachine {

    /** 驱动阶段变化的事件。 */
    sealed class Event {
        data object NewOrder : Event()
        data object OpenDetail : Event()
        data object Accept : Event()
        data object Reject : Event()
        data object PreflightPassed : Event()
        data object PreflightFailed : Event()
        data object StartManual : Event()
        data object StartAutoFlight : Event()
        data object StartSearch : Event()
        data object FollowStarted : Event()
        data object AutoFlightHandover : Event()
        data object Finish : Event()
        data object Reset : Event()
    }

    /** 迁移结果；[rejected] 为 true 时表示非法迁移，[stage] 保持原值。 */
    data class Transition(
        val stage: TaskStage,
        val rejected: Boolean = false,
    )

    fun next(current: TaskStage, event: Event): Transition = when (current) {
        TaskStage.IDLE -> when (event) {
            Event.NewOrder -> Transition(TaskStage.NEW_ORDER)
            Event.Reset -> Transition(TaskStage.IDLE)
            else -> rejected(current)
        }

        TaskStage.NEW_ORDER -> when (event) {
            Event.OpenDetail -> Transition(TaskStage.TASK_DETAIL)
            Event.Reject -> Transition(TaskStage.IDLE)
            Event.Reset -> Transition(TaskStage.IDLE)
            else -> rejected(current)
        }

        TaskStage.TASK_DETAIL -> when (event) {
            Event.Accept -> Transition(TaskStage.PREP)
            Event.Reject -> Transition(TaskStage.IDLE)
            Event.Reset -> Transition(TaskStage.IDLE)
            else -> rejected(current)
        }

        TaskStage.PREP -> when (event) {
            Event.PreflightPassed -> Transition(TaskStage.PREP_READY)
            Event.PreflightFailed -> Transition(TaskStage.NOT_READY)
            Event.StartManual -> Transition(TaskStage.MANUAL)
            Event.Reset -> Transition(TaskStage.IDLE)
            else -> rejected(current)
        }

        TaskStage.NOT_READY -> when (event) {
            Event.PreflightPassed -> Transition(TaskStage.PREP_READY)
            Event.StartManual -> Transition(TaskStage.MANUAL)
            Event.Reset -> Transition(TaskStage.IDLE)
            else -> rejected(current)
        }

        TaskStage.PREP_READY -> when (event) {
            Event.PreflightFailed -> Transition(TaskStage.NOT_READY)
            Event.StartManual -> Transition(TaskStage.MANUAL)
            Event.StartAutoFlight -> Transition(TaskStage.FLYTO)
            Event.Finish -> Transition(TaskStage.FINALIZING)
            Event.Reset -> Transition(TaskStage.IDLE)
            else -> rejected(current)
        }

        TaskStage.MANUAL -> when (event) {
            Event.Finish -> Transition(TaskStage.FINALIZING)
            Event.PreflightPassed -> Transition(TaskStage.PREP_READY)
            Event.Reset -> Transition(TaskStage.IDLE)
            else -> rejected(current)
        }

        TaskStage.FINALIZING -> when (event) {
            Event.Reset -> Transition(TaskStage.IDLE)
            else -> rejected(current)
        }

        // 自动作业阶段：进入 FLYTO 后，可结束、接管回手动、到达后显式开始搜索，或整体重置。
        TaskStage.FLYTO -> when (event) {
            Event.StartSearch -> Transition(TaskStage.SEARCH)
            Event.AutoFlightHandover -> Transition(TaskStage.MANUAL)
            Event.StartManual -> Transition(TaskStage.MANUAL)
            Event.Finish -> Transition(TaskStage.FINALIZING)
            Event.Reset -> Transition(TaskStage.IDLE)
            else -> rejected(current)
        }

        // 搜索阶段：只承载原生识别与目标锁定，不涉及飞行控制；出口与自动阶段一致。
        TaskStage.SEARCH -> when (event) {
            Event.FollowStarted -> Transition(TaskStage.FOLLOW)
            Event.AutoFlightHandover -> Transition(TaskStage.MANUAL)
            Event.StartManual -> Transition(TaskStage.MANUAL)
            Event.Finish -> Transition(TaskStage.FINALIZING)
            Event.Reset -> Transition(TaskStage.IDLE)
            else -> rejected(current)
        }

        // 跟随阶段：只允许经统一释放后接管、转手动、结束或整体重置，不能回到搜索。
        TaskStage.FOLLOW -> when (event) {
            Event.AutoFlightHandover -> Transition(TaskStage.MANUAL)
            Event.StartManual -> Transition(TaskStage.MANUAL)
            Event.Finish -> Transition(TaskStage.FINALIZING)
            Event.Reset -> Transition(TaskStage.IDLE)
            else -> rejected(current)
        }
    }

    /** 是否允许在同一遥控器上接受新任务（只有一个进行中任务）。 */
    fun canAcceptNewTask(current: TaskStage): Boolean = current == TaskStage.IDLE

    /** 是否允许飞手在锁定目标后开始跟随：必须处于搜索阶段。 */
    fun canStartFollow(current: TaskStage): Boolean = current == TaskStage.SEARCH

    /** 是否允许飞手启动自动飞行：必须已就绪且最新预检全部通过。 */
    fun canStartAutoFlight(current: TaskStage, preflightAllPassed: Boolean): Boolean =
        current == TaskStage.PREP_READY && preflightAllPassed

    private fun rejected(current: TaskStage) = Transition(current, rejected = true)
}
