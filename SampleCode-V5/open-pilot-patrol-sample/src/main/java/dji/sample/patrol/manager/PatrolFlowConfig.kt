package dji.sample.patrol.manager

import dji.sample.patrol.R
import dji.sample.patrol.data.FlowConfig
import dji.sample.patrol.data.FlowControl
import dji.sample.patrol.data.FlowStage
import dji.sample.patrol.data.PatrolStage

/**
 * 巡逻浮窗流程配置（design D5）：向框架浮窗声明巡逻阶段与各阶段最小控制，
 * 由业务侧注入 [TaskVM.flowConfig]；未注入时框架退回默认八状态流程。
 */
object PatrolFlowConfig : FlowConfig {

    const val KIND = "DATA_COLLECTION"

    const val STAGE_JOB_LIST = "JOB_LIST"
    const val STAGE_ROUTE_PENDING = "ROUTE_PENDING"
    const val STAGE_ROUTE_PREPARING = "ROUTE_PREPARING"
    const val STAGE_ROUTE_READY = "ROUTE_READY"
    const val STAGE_EXECUTING = "EXECUTING"
    const val STAGE_MATCHED = "MATCHED"
    const val STAGE_PAUSED = "PAUSED"

    /** 用户主动停止：航线终止但飞行器可能仍在空中悬停。 */
    const val STAGE_STOPPED = "STOPPED"
    const val STAGE_FINISHED = "FINISHED"
    const val STAGE_FAILED = "FAILED"
    const val STAGE_FINALIZING = "FINALIZING"
    const val STAGE_DONE = "DONE"
    const val STAGE_DESTINATION = "DESTINATION"

    const val ACTION_LOAD_ROUTE = "LOAD_ROUTE"
    const val ACTION_TAKE_OFF = "TAKE_OFF"
    const val ACTION_PAUSE = "PAUSE"
    const val ACTION_RESUME = "RESUME"
    const val ACTION_STOP = "STOP"

    /** 已停止态的安全出口：经飞控返航动作返回返航点。 */
    const val ACTION_RETURN_HOME = "RETURN_HOME"
    const val ACTION_RETRY = "RETRY"
    const val ACTION_RETRY_SYNC = "RETRY_SYNC"
    const val ACTION_PAUSE_SYNC = "PAUSE_SYNC"
    const val ACTION_BACK = "BACK"
    const val ACTION_CANCEL_FLYTO = "CANCEL_FLYTO"
    const val ACTION_MARK_DESTINATION = "MARK_DESTINATION"

    override val kind: String = KIND

    override val stages: List<FlowStage> = listOf(
        FlowStage(
            STAGE_ROUTE_PENDING,
            R.string.fw_patrol_route_pending,
            listOf(FlowControl(ACTION_LOAD_ROUTE, R.string.fw_btn_load_route)),
        ),
        FlowStage(STAGE_ROUTE_PREPARING, R.string.fw_patrol_route_preparing),
        FlowStage(
            STAGE_ROUTE_READY,
            R.string.fw_patrol_route_ready,
            listOf(FlowControl(ACTION_TAKE_OFF, R.string.fw_btn_take_off, primary = true)),
        ),
        FlowStage(
            STAGE_EXECUTING,
            R.string.fw_patrol_executing,
            listOf(
                FlowControl(ACTION_PAUSE, R.string.fw_btn_pause),
                FlowControl(ACTION_STOP, R.string.fw_btn_stop),
            ),
        ),
        // 命中阶段：拍照失败时保持暂停，飞手可二次确认后恢复，或停止航线。
        FlowStage(
            STAGE_MATCHED,
            R.string.fw_patrol_matched,
            listOf(
                FlowControl(ACTION_RESUME, R.string.fw_btn_resume_route, primary = true),
                FlowControl(ACTION_STOP, R.string.fw_btn_stop),
            ),
        ),
        FlowStage(
            STAGE_PAUSED,
            R.string.fw_patrol_paused,
            listOf(
                FlowControl(ACTION_RESUME, R.string.fw_btn_resume, primary = true),
                FlowControl(ACTION_STOP, R.string.fw_btn_stop),
            ),
        ),
        // 主动停止是中性态：空中提供返航安全出口，同时允许重新加载后受控继续。
        FlowStage(
            STAGE_STOPPED,
            R.string.fw_patrol_stopped,
            listOf(
                FlowControl(ACTION_RETURN_HOME, R.string.fw_btn_return_home, primary = true),
                FlowControl(ACTION_LOAD_ROUTE, R.string.fw_btn_reload_route),
            ),
        ),
        FlowStage(STAGE_FINISHED, R.string.fw_patrol_finished),
        FlowStage(
            STAGE_FAILED,
            R.string.fw_patrol_failed,
            listOf(
                FlowControl(ACTION_RETRY, R.string.fw_btn_retry, primary = true),
                FlowControl(ACTION_STOP, R.string.fw_btn_stop),
            ),
        ),
        // 收尾同步·补传：展示上传进度与失败数，提供重试/暂停同步（最小控制）。
        // 停止后立即进入收尾，飞机可能仍在空中：保留[返航]安全出口，按返航前置显隐。
        FlowStage(
            STAGE_FINALIZING,
            R.string.fw_patrol_finalizing,
            listOf(
                FlowControl(ACTION_RETRY_SYNC, R.string.fw_btn_retry_sync, primary = true),
                FlowControl(ACTION_PAUSE_SYNC, R.string.fw_btn_pause_sync),
                FlowControl(ACTION_RETURN_HOME, R.string.fw_btn_return_home),
            ),
        ),
        // 完成：汇总已上传文件数/大小，可返回任务列表；空中时同样保留[返航]。
        FlowStage(
            STAGE_DONE,
            R.string.fw_patrol_done,
            listOf(
                FlowControl(ACTION_RETURN_HOME, R.string.fw_btn_return_home, primary = true),
                FlowControl(ACTION_BACK, R.string.fw_btn_back_jobs),
            ),
        ),
        // 目的地旁路：FlyTo 飞行中，最小控制[取消]。
        FlowStage(
            STAGE_DESTINATION,
            R.string.fw_patrol_destination,
            listOf(FlowControl(ACTION_CANCEL_FLYTO, R.string.fw_btn_cancel_flyto)),
        ),
    )

    /**
     * 唯一阶段入口：只做「共享派生阶段 → 浮窗 stage id」的纯映射。
     *
     * `IDLE` / `CANCELLED` 返回 `null`，由 [flowStageFor] 交回既有 legacy 流程。
     */
    override fun resolveStage(stage: PatrolStage): String? = when (stage) {
        PatrolStage.IDLE, PatrolStage.CANCELLED -> null
        PatrolStage.JOB_LIST -> STAGE_JOB_LIST
        PatrolStage.ROUTE_PENDING -> STAGE_ROUTE_PENDING
        PatrolStage.ROUTE_PREPARING -> STAGE_ROUTE_PREPARING
        PatrolStage.ROUTE_READY -> STAGE_ROUTE_READY
        PatrolStage.EXECUTING -> STAGE_EXECUTING
        PatrolStage.PAUSED -> STAGE_PAUSED
        PatrolStage.STOPPED -> STAGE_STOPPED
        PatrolStage.FINISHED -> STAGE_FINISHED
        PatrolStage.FAILED -> STAGE_FAILED
        PatrolStage.FINALIZING -> STAGE_FINALIZING
        PatrolStage.DONE -> STAGE_DONE
    }
}

/**
 * 浮窗目标阶段解析：仅在注入了配置且阶段需要浮窗承载时返回阶段 id；
 * 否则返回 `null`，由 [dji.sample.patrol.models.TaskVM] 走既有八状态逻辑（回退）。
 */
internal fun flowStageFor(config: FlowConfig?, stage: PatrolStage?): String? {
    if (config == null || stage == null) return null
    return config.resolveStage(stage)
}
