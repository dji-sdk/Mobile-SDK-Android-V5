package dji.sample.patrol.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import dji.sample.patrol.R
import dji.sample.patrol.data.Asset
import dji.sample.patrol.data.Job
import dji.sample.patrol.data.JobStatus
import dji.sample.patrol.data.UploadQueueItem
import dji.sample.patrol.data.UploadStatus
import dji.sample.patrol.manager.MediaManager
import dji.sample.patrol.manager.isCapturedInRound
import dji.sample.patrol.models.WaylineTaskVM
import dji.sample.patrol.util.globalViewModels

/** 上传队列-会话详情页。 */
class TransferFragment : Fragment() {

    private val vm: WaylineTaskVM by globalViewModels()

    private lateinit var list: LinearLayout
    private lateinit var name: TextView
    private lateinit var percent: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var metrics: TextView
    private lateinit var session: TextView
    private lateinit var pauseResume: Button
    private lateinit var retry: Button

    private var assets: List<Asset> = emptyList()
    private var allAssets: List<Asset> = emptyList()
    private var jobs: List<Job> = emptyList()
    private var details: List<UploadQueueItem> = emptyList()
    private var selectedKey: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        return inflater.inflate(R.layout.fragment_transfers, container, false)
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        list = view.findViewById(R.id.ll_transfer_list)
        name = view.findViewById(R.id.tv_transfer_name)
        percent = view.findViewById(R.id.tv_transfer_percent)
        progressBar = view.findViewById(R.id.pb_transfer)
        metrics = view.findViewById(R.id.tv_transfer_metrics)
        session = view.findViewById(R.id.tv_session_info)
        pauseResume = view.findViewById(R.id.btn_pause_resume)
        retry = view.findViewById(R.id.btn_retry)

        vm.jobs.observe(viewLifecycleOwner) {
            jobs = it.orEmpty()
            applyVisibleAssets()
        }
        vm.uploadQueue.observe(viewLifecycleOwner) {
            allAssets = it.filterNot { asset -> asset.typeId == "route" }
            applyVisibleAssets()
        }
        vm.uploadDetails.observe(viewLifecycleOwner) {
            details = it.filterNot { item -> item.asset.typeId == "route" }
            render()
        }
        pauseResume.setOnClickListener {
            selectedItem()?.let { item ->
                if (item.status == UploadStatus.PAUSED) vm.resumeUpload(item.asset.jobId)
                else vm.pauseUpload(item.asset.jobId)
            }
        }
        retry.setOnClickListener { selectedItem()?.let { vm.retryUpload(it.asset.jobId, it.asset.assetId) } }
    }

    private fun applyVisibleAssets() {
        assets = visibleTransfers(allAssets)
        if (selectedKey !in assets.map(::assetKey)) selectedKey = assets.firstOrNull()?.let(::assetKey)
        render()
    }

    /**
     * 执行中/收尾期间只显示本轮上传项；已完成工单保持历史展示。
     *
     * 设计意图：上传队列会持久化历史项，直接展示会让新一轮页面像在「拉旧照片」；
     * 这里只按工单状态与本轮窗口过滤，不删除队列记录。
     */
    private fun visibleTransfers(source: List<Asset>): List<Asset> {
        val activeJobIds = jobs
            .filter { it.status == JobStatus.IN_PROGRESS || it.status == JobStatus.FINALIZING }
            .map { it.jobId }
            .toSet()
        if (activeJobIds.isEmpty()) return source
        return source.filter { asset ->
            if (asset.jobId !in activeJobIds) return@filter true
            val start = MediaManager.currentRoundStart(asset.jobId) ?: return@filter true
            val capturedAt = asset.meta["capturedAtMillis"]?.toLongOrNull() ?: 0L
            isCapturedInRound(capturedAt, start)
        }
    }

    private fun render() {
        if (!::list.isInitialized) return
        list.removeAllViews()
        if (assets.isEmpty()) {
            list.addView(TextView(requireContext()).apply {
                setText(R.string.empty_transfers)
                setTextColor(0xFF78838E.toInt())
                setPadding(dp(8), dp(16), 0, 0)
            })
        }
        assets.forEach { asset ->
            list.addView(TextView(requireContext()).apply {
                text = "${asset.meta["fileName"] ?: asset.assetId}\n" +
                    "${formatBytes(asset.sizeBytes)} · ${uploadStatus(selectedDetail(asset)?.status)}"
                textSize = 13f
                setTextColor(0xFF25313D.toInt())
                setPadding(dp(12), dp(12), dp(10), dp(12))
                background = requireContext().getDrawable(R.drawable.bg_patrol_item)
                isSelected = assetKey(asset) == selectedKey
                setOnClickListener {
                    selectedKey = assetKey(asset)
                    render()
                }
            }, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dp(7)
            })
        }
        renderDetail()
    }

    private fun renderDetail() {
        if (!::name.isInitialized) return
        val item = selectedItem()
        if (item == null) {
            name.setText(R.string.empty_transfers)
            percent.text = "0%"
            progressBar.progress = 0
            metrics.text = ""
            session.text = ""
            pauseResume.isEnabled = false
            retry.isEnabled = false
            return
        }
        val asset = item.asset
        val uploadedBytes = item.uploadedBytes()
        val value = if (asset.sizeBytes > 0) (uploadedBytes * 100 / asset.sizeBytes).toInt() else 0
        name.text = asset.meta["fileName"] ?: asset.assetId
        percent.text = "$value%"
        progressBar.progress = value
        metrics.text = "${getString(R.string.uploaded)}  ${formatBytes(uploadedBytes)}\n" +
            "${getString(R.string.remaining)}  ${formatBytes((asset.sizeBytes - uploadedBytes).coerceAtLeast(0))}\n" +
            "${getString(R.string.asset_status)}  ${uploadStatus(item.status)}"
        session.text = "${getString(R.string.session_id)}  ${item.sessionId ?: "-"}\n" +
            "${getString(R.string.parts)}  ${item.completedParts.size}\n" +
            "MD5  ${asset.md5.orEmpty()}"
        pauseResume.isEnabled = item.status != UploadStatus.UPLOADED && item.status != UploadStatus.FAILED
        pauseResume.setText(if (item.status == UploadStatus.PAUSED) R.string.resume else R.string.pause)
        retry.isEnabled = item.status == UploadStatus.FAILED
    }

    private fun selectedItem(): UploadQueueItem? = details.firstOrNull { assetKey(it.asset) == selectedKey }
    private fun selectedDetail(asset: Asset): UploadQueueItem? = details.firstOrNull {
        assetKey(it.asset) == assetKey(asset)
    }
    private fun assetKey(asset: Asset) = "${asset.jobId}/${asset.assetId}"
    private fun uploadStatus(status: UploadStatus?): String = if (status == null) "-" else getString(when (status) {
        UploadStatus.QUEUED -> R.string.status_queued
        UploadStatus.UPLOADING -> R.string.status_uploading
        UploadStatus.PAUSED -> R.string.status_paused
        UploadStatus.UPLOADED -> R.string.status_uploaded
        UploadStatus.FAILED -> R.string.status_failed
    })
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / 1024f / 1024f)
        bytes >= 1024 -> "%.1f KB".format(bytes / 1024f)
        else -> "$bytes B"
    }
}
