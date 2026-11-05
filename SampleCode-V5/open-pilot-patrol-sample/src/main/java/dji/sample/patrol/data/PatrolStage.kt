package dji.sample.patrol.data

/**
 * 巡逻流程阶段：插件页面与浮窗共用的展示语义。
 *
 * 设计意图：把「工单状态 + 航线权威状态」派生为唯一阶段，页面与浮窗都消费同一结果，
 * 避免两侧各自推断导致「页面待加载、浮窗执行失败」并存。
 */
enum class PatrolStage {
    /** 没有在途工单。 */
    IDLE,

    /** 有待执行工单，等待接单。 */
    JOB_LIST,

    /** 当前工单从未加载过航线。 */
    ROUTE_PENDING,

    /** 航线加载/上传/校验中。 */
    ROUTE_PREPARING,

    /** 航线已就绪，等待飞手确认起飞或继续执行。 */
    ROUTE_READY,

    /** 航线执行中。 */
    EXECUTING,

    /** 航线被暂停。 */
    PAUSED,

    /** 用户主动停止：中性态，飞行器可能仍在空中。 */
    STOPPED,

    /** 航线业务终态已完成（短暂过渡，随后进入收尾同步）。 */
    FINISHED,

    /** 非用户发起的航线失败。 */
    FAILED,

    /** 收尾同步/补传。 */
    FINALIZING,

    /** 工单已完成。 */
    DONE,

    /** 工单已取消。 */
    CANCELLED,
}

/**
 * 由工单与航线权威状态派生展示阶段（纯函数）。
 *
 * 优先级（与 design D3 一致）：
 * 1. 无工单 → [PatrolStage.IDLE]
 * 2. 工单终态优先：`COMPLETED` → [PatrolStage.DONE]，`CANCELLED` → [PatrolStage.CANCELLED]
 * 3. 待接单 → [PatrolStage.JOB_LIST]；收尾中 → [PatrolStage.FINALIZING]
 * 4. 航线归属与当前工单不一致 → [PatrolStage.ROUTE_PENDING]（中性，不串用他人快照）
 * 5. 之后按航线状态映射；`IN_PROGRESS + ROUTE_PENDING`（非起飞中）视为非用户发起中断 → [PatrolStage.FAILED]
 *
 * @param job 当前展示/执行的工单；`null` 表示没有工单上下文
 * @param routeState 航线权威状态
 * @param routeJobId 航线快照的归属工单
 * @param executionStarting 起飞命令是否正在下发（用于避免把启动瞬间误判为中断）
 */
fun resolvePatrolStage(
    job: Job?,
    routeState: RouteState,
    routeJobId: String?,
    executionStarting: Boolean,
): PatrolStage {
    if (job == null) return PatrolStage.IDLE
    when (job.status) {
        JobStatus.COMPLETED -> return PatrolStage.DONE
        JobStatus.CANCELLED -> return PatrolStage.CANCELLED
        JobStatus.DISPATCHED -> return PatrolStage.JOB_LIST
        JobStatus.FINALIZING -> return PatrolStage.FINALIZING
        else -> Unit
    }
    if (routeJobId != job.jobId) return PatrolStage.ROUTE_PENDING
    // 停止态只属于「本次执行中」的工单：同一 jobId 重新下发/接受时，上一轮残留的 STOPPED
    // 是本地污染，必须回到待加载，不能与页面的工单状态（如「待执行」）矛盾。
    if (routeState == RouteState.STOPPED && job.status != JobStatus.IN_PROGRESS) {
        return PatrolStage.ROUTE_PENDING
    }
    return when (routeState) {
        RouteState.ROUTE_PENDING ->
            if (job.status == JobStatus.IN_PROGRESS && !executionStarting) {
                // 工单已执行但航线既未加载也未执行：非用户发起的中断。
                PatrolStage.FAILED
            } else {
                PatrolStage.ROUTE_PENDING
            }
        RouteState.ROUTE_PREPARING -> PatrolStage.ROUTE_PREPARING
        RouteState.ROUTE_READY -> PatrolStage.ROUTE_READY
        RouteState.EXECUTING -> PatrolStage.EXECUTING
        RouteState.PAUSED -> PatrolStage.PAUSED
        RouteState.STOPPED -> PatrolStage.STOPPED
        RouteState.FINISHED -> PatrolStage.FINALIZING
        RouteState.FAILED -> PatrolStage.FAILED
    }
}
