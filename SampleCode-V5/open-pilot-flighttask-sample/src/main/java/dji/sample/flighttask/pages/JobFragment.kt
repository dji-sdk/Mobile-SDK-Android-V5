package dji.sample.flighttask.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import dji.sample.flighttask.R
import dji.sample.flighttask.data.Job
import dji.sample.flighttask.data.JobStatus
import dji.sample.flighttask.models.TaskVM
import dji.sample.flighttask.models.WaylineTaskVM
import dji.sample.flighttask.util.globalViewModels

/** 任务列表-详情页。所有数据来自 TaskVM/WaylineTaskVM，不直接访问 Manager。 */
class JobFragment : Fragment() {

    private val taskVM: TaskVM by globalViewModels()
    private val waylineTaskVM: WaylineTaskVM by globalViewModels()

    private lateinit var taskList: LinearLayout
    private lateinit var taskName: TextView
    private lateinit var taskStatus: TextView
    private lateinit var taskInfo: TextView
    private lateinit var routeStatus: TextView
    private lateinit var syncStatus: TextView

    private var jobs: List<Job> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        return inflater.inflate(R.layout.fragment_tasks, container, false)
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        taskList = view.findViewById(R.id.ll_task_list)
        taskName = view.findViewById(R.id.tv_task_name)
        taskStatus = view.findViewById(R.id.tv_task_status)
        taskInfo = view.findViewById(R.id.tv_task_info)
        routeStatus = view.findViewById(R.id.tv_route_status)
        syncStatus = view.findViewById(R.id.tv_sync_status)

        taskVM.jobs.observe(viewLifecycleOwner) { values ->
            jobs = values
            renderList()
            renderDetail()
        }
        taskVM.selectedJobId.observe(viewLifecycleOwner) {
            renderList()
            renderDetail()
        }
        waylineTaskVM.waylineState.observe(viewLifecycleOwner) { renderDetail() }
        waylineTaskVM.waylineProgress.observe(viewLifecycleOwner) { renderDetail() }
        waylineTaskVM.uploadProgress.observe(viewLifecycleOwner) { renderDetail() }
    }

    private fun renderList() {
        taskList.removeAllViews()
        addGroup(R.string.task_group_current, jobs.filter { it.status == JobStatus.IN_PROGRESS || it.status == JobStatus.FINALIZING })
        addGroup(R.string.task_group_pending, jobs.filter { it.status == JobStatus.DISPATCHED || it.status == JobStatus.ACCEPTED })
        addGroup(R.string.task_group_completed, jobs.filter { it.status == JobStatus.COMPLETED || it.status == JobStatus.CANCELLED })
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
                background = requireContext().getDrawable(R.drawable.bg_ft_item)
                isSelected = job.jobId == taskVM.selectedJobId.value
                setOnClickListener {
                    taskVM.selectJob(job.jobId)
                }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(7)
            })
        }
    }

    private fun renderDetail() {
        val job = jobs.firstOrNull { it.jobId == taskVM.selectedJobId.value }
        if (job == null) {
            taskName.setText(R.string.empty_tasks)
            taskStatus.text = ""
            taskInfo.text = ""
            routeStatus.text = ""
            syncStatus.text = ""
            return
        }
        taskName.text = job.title
        taskStatus.text = "● ${jobStatus(job.status)}  ${job.extras["missionType"].orEmpty()}"
        taskInfo.text = buildString {
            appendLine("${getString(R.string.task_requirement)}  ${job.extras["requirements"].orEmpty()}")
            appendLine("${getString(R.string.task_deadline)}  ${job.extras["deadline"].orEmpty()}")
            append("${getString(R.string.task_id)}  ${job.jobId}")
        }
        val waylineState = waylineTaskVM.waylineState.value
        routeStatus.text = "${getString(R.string.route_cloud_status)}  ${job.routeStatus}\n" +
            "${getString(R.string.route_execute_status)}  ${waylineState ?: "-"}\n" +
            (waylineTaskVM.waylineProgress.value?.description.orEmpty())
        val progress = waylineTaskVM.uploadProgress.value?.get(job.jobId)
        syncStatus.text = if (progress == null) {
            getString(R.string.no_transfer)
        } else {
            "${getString(R.string.file_progress)}  ${progress.uploaded}/${progress.total}\n" +
                "${formatBytes(progress.uploadedBytes)} / ${formatBytes(progress.totalBytes)}"
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

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
