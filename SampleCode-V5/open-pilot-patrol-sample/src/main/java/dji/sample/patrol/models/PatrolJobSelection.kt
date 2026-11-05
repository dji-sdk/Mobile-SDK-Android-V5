package dji.sample.patrol.models

import dji.sample.patrol.data.Job
import dji.sample.patrol.data.JobStatus

/**
 * 一次工单列表刷新后的选择解析结果。
 *
 * - [activeJobId]：当前在途工单（已接受 / 进行中 / 收尾同步中），没有则为 `null`；
 * - [selectedJobId]：浮窗当前关注的工单，没有则为 `null`（浮窗进入空态）；
 * - [pendingJobId]：工单列表容器里展示的待执行工单。
 */
data class JobSelection(
    val activeJobId: String?,
    val selectedJobId: String?,
    val pendingJobId: String?,
)

/**
 * 工单选择的纯解析逻辑。
 *
 * 设计意图（design D2）：把「刷新后该选中谁」从 `TaskVM` 的回调里抽出来，避免在观察者内部
 * 写兜底链——历史缺陷正是「选中为空 → 回退到列表首项 → 已完成的工单被重新选回」。
 *
 * 关键约束：浏览态（用户主动「切换任务」后）只认待执行工单，绝不回退到 `COMPLETED` /
 * `CANCELLED` 工单；需要继续显示已完成工单时，走非浏览态的既有规则（用户显式选中该工单）。
 */
object PatrolJobSelection {

    private val activeStatuses = setOf(
        JobStatus.ACCEPTED,
        JobStatus.IN_PROGRESS,
        JobStatus.FINALIZING,
    )

    /** 列表里是否出现了在途工单：出现即代表浏览态应当结束。 */
    fun hasActiveJob(jobs: List<Job>): Boolean = jobs.any { it.status in activeStatuses }

    /**
     * 解析本次刷新后的工单选择。
     *
     * @param jobs 最新的工单列表（只读取 `jobId` 与 `status`）。
     * @param browsing 用户是否处于「切换任务」后的浏览态。
     * @param cachedActiveJobId 上一次解析出的在途工单；仍有效时优先沿用，避免选中项来回跳。
     * @param cachedPendingJobId 工单列表容器的上一次选中项；仍为待执行时优先沿用。
     * @param currentSelectedJobId 当前选中工单。
     */
    fun resolve(
        jobs: List<Job>,
        browsing: Boolean,
        cachedActiveJobId: String?,
        cachedPendingJobId: String?,
        currentSelectedJobId: String?,
    ): JobSelection {
        val dispatched = jobs.filter { it.status == JobStatus.DISPATCHED }
        if (browsing) {
            val selected = dispatched.firstOrNull { it.jobId == currentSelectedJobId }?.jobId
                ?: dispatched.firstOrNull()?.jobId
            return JobSelection(
                activeJobId = null,
                selectedJobId = selected,
                pendingJobId = selected,
            )
        }

        val active = cachedActiveJobId?.let { id ->
            jobs.firstOrNull { it.jobId == id && it.status in activeStatuses }
        } ?: jobs.firstOrNull { it.status in activeStatuses }

        val selectionStillExists = jobs.any { it.jobId == currentSelectedJobId }
        val selected = if (selectionStillExists) {
            currentSelectedJobId
        } else {
            active?.jobId ?: dispatched.firstOrNull()?.jobId ?: jobs.firstOrNull()?.jobId
        }

        val pendingJobId = dispatched.firstOrNull { it.jobId == selected }?.jobId
            ?: cachedPendingJobId?.takeIf { id -> dispatched.any { it.jobId == id } }
            ?: dispatched.firstOrNull()?.jobId

        return JobSelection(
            activeJobId = active?.jobId,
            selectedJobId = selected,
            pendingJobId = pendingJobId,
        )
    }
}
