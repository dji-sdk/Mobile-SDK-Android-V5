package dji.sample.patrol.pages

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import dji.sample.patrol.R
import dji.sample.patrol.data.Job
import dji.sample.patrol.data.JobStatus
import dji.sample.patrol.data.PatrolStage
import dji.sample.patrol.data.RouteState
import dji.sample.patrol.data.RouteStatus
import dji.sample.patrol.data.resolvePatrolStage
import dji.sample.patrol.data.syncSummary
import dji.sample.patrol.models.TaskVM
import dji.sample.patrol.models.WaylineTaskVM
import dji.sample.patrol.util.JobTimeFormatter
import dji.sample.patrol.util.globalViewModels
import dji.sample.patrol.util.routeProgressText
import java.util.TimeZone

/**
 * 任务列表-详情页。
 *
 * 左侧为分组任务列表，右侧为只读详情区：任务信息（字段化）、航线状态（字段 + 时间线节点链）、
 * 同步状态。所有数据来自 [TaskVM] / [WaylineTaskVM]，不直接访问 Manager 与 SDK；页面不提供任何
 * 飞行 / 航线 / 上传控制入口（控制仍在浮窗）。
 */
class JobFragment : Fragment() {

    /** 时间线节点链的呈现上限；超出时以「更早 N 条未显示」交代省略。 */
    private val timelineMaxNodes = 5

    private val taskVM: TaskVM by globalViewModels()
    private val waylineTaskVM: WaylineTaskVM by globalViewModels()

    /** 时间线节点状态：颜色语义 DONE=成功、CURRENT=主色、FAILED=错误、PENDING=未开始。 */
    private enum class NodeStatus { DONE, CURRENT, FAILED, PENDING }

    private lateinit var taskList: LinearLayout
    private lateinit var emptyDetail: TextView
    private lateinit var detailContainer: LinearLayout
    private lateinit var taskName: TextView
    private lateinit var taskStatus: TextView
    private lateinit var taskRequirement: TextView
    private lateinit var taskDeadline: TextView
    private lateinit var taskId: TextView
    private lateinit var routeCloud: TextView
    private lateinit var routeState: TextView
    private lateinit var routeProgressLabel: TextView
    private lateinit var routeProgress: TextView
    private lateinit var timelineEarlier: TextView
    private lateinit var timelineContainer: LinearLayout
    private lateinit var timelineEmpty: TextView
    private lateinit var syncFiles: TextView
    private lateinit var syncByteLabel: TextView
    private lateinit var syncBytes: TextView

    private var jobs: List<Job> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        return inflater.inflate(R.layout.fragment_tasks, container, false)
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        taskList = view.findViewById(R.id.ll_task_list)
        emptyDetail = view.findViewById(R.id.tv_empty_detail)
        detailContainer = view.findViewById(R.id.ll_detail_container)
        taskName = view.findViewById(R.id.tv_task_name)
        taskStatus = view.findViewById(R.id.tv_task_status)
        taskRequirement = view.findViewById(R.id.tv_task_requirement)
        taskDeadline = view.findViewById(R.id.tv_task_deadline)
        taskId = view.findViewById(R.id.tv_task_id)
        routeCloud = view.findViewById(R.id.tv_route_cloud)
        routeState = view.findViewById(R.id.tv_route_state)
        routeProgressLabel = view.findViewById(R.id.tv_route_progress_label)
        routeProgress = view.findViewById(R.id.tv_route_progress)
        timelineEarlier = view.findViewById(R.id.tv_timeline_earlier)
        timelineContainer = view.findViewById(R.id.ll_timeline)
        timelineEmpty = view.findViewById(R.id.tv_timeline_empty)
        syncFiles = view.findViewById(R.id.tv_sync_files)
        syncByteLabel = view.findViewById(R.id.tv_sync_byte_label)
        syncBytes = view.findViewById(R.id.tv_sync_bytes)

        taskVM.jobs.observe(viewLifecycleOwner) { values ->
            jobs = values
            renderList()
            renderDetail()
        }
        taskVM.selectedJobId.observe(viewLifecycleOwner) {
            renderList()
            renderDetail()
        }
        // 航线状态/进度/错误只读 RouteController 的单一快照，与浮窗同源。
        waylineTaskVM.routeSnapshot.observe(viewLifecycleOwner) { renderDetail() }
        waylineTaskVM.uploadProgress.observe(viewLifecycleOwner) { renderDetail() }
    }

    private fun renderList() {
        taskList.removeAllViews()
        addGroup(
            R.string.task_group_current,
            jobs.filter { it.status == JobStatus.IN_PROGRESS || it.status == JobStatus.FINALIZING },
        )
        addGroup(
            R.string.task_group_pending,
            jobs.filter { it.status == JobStatus.DISPATCHED || it.status == JobStatus.ACCEPTED },
        )
        addGroup(
            R.string.task_group_completed,
            jobs.filter { it.status == JobStatus.COMPLETED || it.status == JobStatus.CANCELLED },
        )
    }

    private fun addGroup(titleRes: Int, group: List<Job>) {
        if (group.isEmpty()) return
        taskList.addView(TextView(requireContext()).apply {
            setText(titleRes)
            setTextColor(0xFF65717D.toInt())
            textSize = 12f
            setPadding(dp(4), dp(10), 0, dp(6))
        })
        group.forEach { job ->
            taskList.addView(TextView(requireContext()).apply {
                text = "${job.title}\n${job.extras["missionType"].orEmpty()} · ${jobStatus(job.status)}"
                textSize = 13f
                setTextColor(0xFF25313D.toInt())
                setPadding(dp(12), dp(12), dp(10), dp(12))
                background = requireContext().getDrawable(R.drawable.bg_patrol_item)
                isSelected = job.jobId == taskVM.selectedJobId.value
                setOnClickListener {
                    taskVM.selectJob(job.jobId)
                }
            }, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dp(7)
            })
        }
    }

    /**
     * 渲染右侧详情区。
     *
     * 空态：未选中任务时隐藏整个详情容器（不显示空白卡片），只呈现空态提示，并清空上一次的时间线
     * 节点，避免残留上一任务的内容。
     */
    private fun renderDetail() {
        val job = jobs.firstOrNull { it.jobId == taskVM.selectedJobId.value }
        if (job == null) {
            detailContainer.visibility = View.GONE
            emptyDetail.visibility = View.VISIBLE
            timelineContainer.removeAllViews()
            return
        }
        detailContainer.visibility = View.VISIBLE
        emptyDetail.visibility = View.GONE

        taskName.text = job.title
        taskStatus.text = "● ${jobStatus(job.status)}  ${job.extras["missionType"].orEmpty()}"

        renderTaskInfo(job)
        renderRouteStatus(job)
        renderTimeline(job)
        renderSyncStatus(job)
    }

    /** 任务信息：每个字段「标签（静态）+ 取值」，取值缺失时落占位文案。 */
    private fun renderTaskInfo(job: Job) {
        taskRequirement.text = job.extras["requirements"].orEmpty().ifBlank { noneLabel() }
        taskDeadline.text =
            JobTimeFormatter.formatDeadline(job.extras["deadline"], TimeZone.getDefault()) ?: noneLabel()
        taskId.text = job.jobId.ifBlank { noneLabel() }
    }

    /** 航线状态：与浮窗共用同一阶段派生；非归属工单显示中性「待加载」，不展示他人航线进度。 */
    private fun renderRouteStatus(job: Job) {
        routeCloud.text = cloudRouteLabel(job.routeStatus)
        routeState.text = patrolStageLabel(stageFor(job))
        val snapshot = waylineTaskVM.routeSnapshot.value
        val text = if (snapshot?.jobId == job.jobId) {
            snapshot?.progress?.let { routeProgressText(requireContext(), it) }
        } else {
            null
        }
        setFieldVisible(routeProgressLabel, routeProgress, text != null)
        routeProgress.text = text.orEmpty()
    }

    /**
     * 同步状态：文件计数与数据量分行，数值语义与浮窗/通知栏共用 [syncSummary] 的上传队列口径。
     *
     * 无上传队列时只保留「文件进度 = 未开始传输」一行，隐藏数据量行，避免出现 0/0 的伪进度。
     */
    private fun renderSyncStatus(job: Job) {
        val summary = syncSummary(
            jobId = job.jobId,
            uploadProgress = waylineTaskVM.uploadProgress.value?.get(job.jobId),
            references = waylineTaskVM.mediaReferences.value.orEmpty(),
            assets = waylineTaskVM.localAssets.value.orEmpty(),
        )
        if (!summary.hasUpload) {
            syncFiles.text = getString(R.string.no_transfer)
            setFieldVisible(syncByteLabel, syncBytes, false)
            return
        }
        syncFiles.text = "${summary.uploadedFiles}/${summary.totalFiles}"
        syncBytes.text = "${formatBytes(summary.uploadedBytes)} / ${formatBytes(summary.totalBytes)}"
        setFieldVisible(syncByteLabel, syncBytes, true)
    }

    /**
     * 渲染航线状态时间线：每个事件一个「圆点 + 连接线 + 文案 + 时间」节点，自上而下按时间推进。
     *
     * 只渲染最近 [timelineMaxNodes] 条，超出时在链条顶部交代「更早 N 条未显示」；`timeline` 为空时
     * 呈现空态文案而不渲染节点骨架。节点状态取色见 [nodeColorRes]，当前节点附带与航线状态一致的
     * 文案后缀（对齐浮窗任务链对当前节点的表达方式）。
     */
    private fun renderTimeline(job: Job) {
        timelineContainer.removeAllViews()
        val events = job.timeline
        if (events.isEmpty()) {
            timelineEmpty.visibility = View.VISIBLE
            timelineEarlier.visibility = View.GONE
            return
        }
        timelineEmpty.visibility = View.GONE

        val recent = events.takeLast(timelineMaxNodes)
        val omitted = events.size - recent.size
        timelineEarlier.visibility = if (omitted > 0) View.VISIBLE else View.GONE
        if (omitted > 0) timelineEarlier.text = getString(R.string.detail_timeline_earlier, omitted)

        val statuses = timelineStatuses(job, recent.size)
        recent.forEachIndexed { index, event ->
            val node = layoutInflater.inflate(R.layout.item_timeline_node, timelineContainer, false)
            val status = statuses[index]
            val color = ContextCompat.getColor(requireContext(), nodeColorRes(status))

            node.findViewById<View>(R.id.timeline_dot).backgroundTintList = ColorStateList.valueOf(color)
            // 连接线取本节点状态色；最后一个节点没有「下一段」，直接隐藏，避免链条悬空。
            val rail = node.findViewById<View>(R.id.timeline_rail)
            if (index == recent.lastIndex) {
                rail.visibility = View.GONE
            } else {
                rail.setBackgroundColor(color)
            }

            node.findViewById<TextView>(R.id.timeline_title).apply {
                text = timelineNodeText(job, event.description, status)
                setTextColor(color)
                if (status == NodeStatus.CURRENT) {
                    setTypeface(typeface, Typeface.BOLD)
                }
            }
            node.findViewById<TextView>(R.id.timeline_time).text =
                JobTimeFormatter.formatEventTime(event.timestamp, TimeZone.getDefault())

            timelineContainer.addView(node)
        }
    }

    /**
     * 计算时间线各节点的状态。
     *
     * 事件链形态下不存在「尚未发生」的节点，因此只使用 DONE / CURRENT / FAILED 三档；其中
     * FAILED 由权威航线状态或任务取消状态决定，任务已完成时全部标记 DONE（不再显示「当前」）。
     */
    private fun timelineStatuses(job: Job, count: Int): List<NodeStatus> {
        if (count <= 0) return emptyList()
        val stage = stageFor(job)
        val failed = stage == PatrolStage.FAILED || job.status == JobStatus.CANCELLED
        val allDone = stage == PatrolStage.DONE
        val last = when {
            failed -> NodeStatus.FAILED
            allDone -> NodeStatus.DONE
            else -> NodeStatus.CURRENT
        }
        return List(count) { index -> if (index == count - 1) last else NodeStatus.DONE }
    }

    /** 当前节点附上权威航线状态后缀，使颜色语义与文案一致。 */
    private fun timelineNodeText(job: Job, description: String, status: NodeStatus): String {
        if (status == NodeStatus.CURRENT || status == NodeStatus.FAILED) {
            return "$description · ${patrolStageLabel(stageFor(job))}"
        }
        return description
    }

    private fun nodeColorRes(status: NodeStatus): Int = when (status) {
        NodeStatus.DONE -> R.color.timeline_node_done
        NodeStatus.CURRENT -> R.color.timeline_node_current
        NodeStatus.FAILED -> R.color.timeline_node_failed
        NodeStatus.PENDING -> R.color.timeline_node_pending
    }

    private fun setFieldVisible(label: TextView, value: TextView, visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        label.visibility = visibility
        value.visibility = visibility
    }

    private fun noneLabel() = getString(R.string.detail_value_none)

    private fun cloudRouteLabel(status: RouteStatus): String = getString(when (status) {
        RouteStatus.CLOUD_PROVIDED -> R.string.route_cloud_provided
        RouteStatus.NOT_REQUIRED -> R.string.route_cloud_not_required
        RouteStatus.INVALID -> R.string.route_cloud_invalid
    })

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    /** 选中/当前工单的共享阶段；页面不参与起飞瞬态，`executionStarting` 固定为 false。 */
    private fun stageFor(job: Job): PatrolStage {
        val snapshot = waylineTaskVM.routeSnapshot.value
        return resolvePatrolStage(
            job = job,
            routeState = snapshot?.state ?: RouteState.ROUTE_PENDING,
            routeJobId = snapshot?.jobId,
            executionStarting = false,
        )
    }

    private fun patrolStageLabel(stage: PatrolStage): String = getString(when (stage) {
        PatrolStage.IDLE, PatrolStage.JOB_LIST, PatrolStage.ROUTE_PENDING ->
            R.string.fw_patrol_route_pending
        PatrolStage.ROUTE_PREPARING -> R.string.fw_patrol_route_preparing
        PatrolStage.ROUTE_READY -> R.string.fw_patrol_route_ready
        PatrolStage.EXECUTING -> R.string.fw_patrol_executing
        PatrolStage.PAUSED -> R.string.fw_patrol_paused
        PatrolStage.STOPPED -> R.string.fw_patrol_stopped
        PatrolStage.FINISHED, PatrolStage.FINALIZING -> R.string.fw_patrol_finalizing
        PatrolStage.FAILED -> R.string.fw_patrol_failed
        PatrolStage.DONE -> R.string.fw_patrol_done
        PatrolStage.CANCELLED -> R.string.status_cancelled
    })

    private fun jobStatus(status: JobStatus) = getString(when (status) {
        JobStatus.DISPATCHED -> R.string.status_dispatched
        JobStatus.ACCEPTED -> R.string.status_accepted
        JobStatus.IN_PROGRESS -> R.string.status_in_progress
        JobStatus.FINALIZING -> R.string.status_finalizing
        JobStatus.COMPLETED -> R.string.status_completed
        JobStatus.CANCELLED -> R.string.status_cancelled
    })

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / 1024f / 1024f)
        bytes >= 1024 -> "%.1f KB".format(bytes / 1024f)
        else -> "$bytes B"
    }
}
