package dji.sample.patrol.pages

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import dji.sample.patrol.R
import dji.sample.patrol.data.Asset
import dji.sample.patrol.data.AssetState
import dji.sample.patrol.data.Job
import dji.sample.patrol.data.JobStatus
import dji.sample.patrol.data.ResultCallback
import dji.sample.patrol.manager.MediaManager
import dji.sample.patrol.manager.isCapturedInRound
import dji.sample.patrol.models.TaskVM
import dji.sample.patrol.models.WaylineTaskVM
import dji.sample.patrol.util.globalViewModels
import java.io.File
import java.util.concurrent.Executors

/** 本地资源网格-预览页。 */
class AssetFragment : Fragment() {

    private val taskVM: TaskVM by globalViewModels()
    private val vm: WaylineTaskVM by globalViewModels()

    private lateinit var groupsContainer: LinearLayout
    private lateinit var preview: ImageView
    private lateinit var assetName: TextView
    private lateinit var assetDetail: TextView
    private lateinit var chips: List<TextView>

    private var jobs: List<Job> = emptyList()
    private var mediaAssets: List<Asset> = emptyList()
    private var manifestAssets: List<Asset> = emptyList()
    private var uploadAssets: List<Asset> = emptyList()
    private var thumbnailPaths: Map<String, String> = emptyMap()
    private var routeAsset: Asset? = null
    private var filter = FILTER_ALL
    private var selectedKey: String? = null
    private var structureKey = ""
    private var previewAssetKey: String? = null
    private var previewSource: String? = null
    private val pendingRetries = mutableSetOf<String>()
    private val cardBindings = mutableMapOf<String, CardBinding>()
    private val groupBindings = mutableMapOf<String, GroupBinding>()
    private val imageExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val bitmapCache = object : LruCache<String, Bitmap>(12 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = (value.byteCount / 1024).coerceAtLeast(1)
    }
    private var previewGeneration = 0L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        return inflater.inflate(R.layout.fragment_assets, container, false)
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        groupsContainer = view.findViewById(R.id.grid_assets)
        preview = view.findViewById(R.id.iv_asset_preview)
        assetName = view.findViewById(R.id.tv_asset_name)
        assetDetail = view.findViewById(R.id.tv_asset_detail)
        chips = listOf(
            view.findViewById(R.id.chip_all),
            view.findViewById(R.id.chip_photo),
            view.findViewById(R.id.chip_route),
            view.findViewById(R.id.chip_point_event),
        )
        chips.forEachIndexed { index, chip -> chip.setOnClickListener { selectFilter(index) } }

        taskVM.jobs.observe(viewLifecycleOwner) {
            jobs = it
            render()
        }
        taskVM.selectedJobId.observe(viewLifecycleOwner) { render() }
        vm.localAssets.observe(viewLifecycleOwner) {
            mediaAssets = it
            render()
        }
        vm.manifestAssets.observe(viewLifecycleOwner) {
            manifestAssets = it
            render()
        }
        vm.uploadQueue.observe(viewLifecycleOwner) {
            uploadAssets = it
            updateBindings()
        }
        vm.thumbnailPaths.observe(viewLifecycleOwner) { paths ->
            val changedKeys = (thumbnailPaths.keys + paths.keys).filter { thumbnailPaths[it] != paths[it] }
            thumbnailPaths = paths
            changedKeys.forEach { key -> cardBindings[key]?.let(::updateCardImage) }
            selectedAsset()?.takeIf { mediaKeyOrNull(it) in changedKeys }?.let(::renderDetail)
        }
        vm.routeAsset.observe(viewLifecycleOwner) {
            routeAsset = it
            render()
        }
        groupsContainer.post { render() }
        selectFilter(FILTER_ALL)
    }

    private fun selectFilter(value: Int) {
        filter = value
        chips.forEachIndexed { index, chip ->
            chip.isSelected = index == value
            chip.setTextColor(if (index == value) 0xFFFFFFFF.toInt() else 0xFF4D5965.toInt())
        }
        render()
    }

    /** 执行中/收尾期间只展示本轮照片；其他状态展示历史，避免旧照片混入本轮。 */
    private fun visibleInCurrentRound(job: Job, asset: Asset): Boolean {
        if (job.status != JobStatus.IN_PROGRESS && job.status != JobStatus.FINALIZING) return true
        val start = MediaManager.currentRoundStart(job.jobId) ?: return true
        val capturedAt = asset.meta["capturedAtMillis"]?.toLongOrNull() ?: 0L
        return isCapturedInRound(capturedAt, start)
    }

    private fun photosByKey(): LinkedHashMap<String, Asset> = linkedMapOf<String, Asset>().apply {
        manifestAssets.forEach { put(mediaKey(it), it) }
        mediaAssets.forEach { put(mediaKey(it), it) }
    }

    private fun visibleAssets(job: Job, photos: Map<String, Asset>): List<Asset> = buildList {
        if (filter != FILTER_ROUTE && filter != FILTER_POINT_EVENT) {
            addAll(photos.values.filter {
                it.jobId == job.jobId &&
                    (it.localUri != null || job.status == JobStatus.FINALIZING) &&
                    visibleInCurrentRound(job, it)
            })
        }
        if (filter != FILTER_PHOTO && filter != FILTER_POINT_EVENT) {
            routeAsset?.takeIf { it.jobId == job.jobId }?.let(::add)
        }
        // 点位/事件 JSON 资源（M3）：来自上传队列，按工单归组。
        if (filter != FILTER_PHOTO && filter != FILTER_ROUTE) {
            addAll(uploadAssets.filter {
                it.jobId == job.jobId && (it.typeId == "point" || it.typeId == "event")
            })
        }
    }

    private fun render() {
        if (!::groupsContainer.isInitialized) return
        val photos = photosByKey()
        val grouped = jobs.map { it to visibleAssets(it, photos) }
            .filter { (job, assets) -> assets.isNotEmpty() || job.status == JobStatus.FINALIZING }
        val newStructureKey = buildString {
            append(filter)
            grouped.forEach { (job, assets) ->
                append('|').append(job.jobId).append(':').append(job.status == JobStatus.FINALIZING)
                assets.forEach { append(',').append(assetKey(it)) }
            }
        }
        if (newStructureKey != structureKey) {
            rebuild(grouped)
            structureKey = newStructureKey
        }
        updateBindings()
    }

    private fun rebuild(grouped: List<Pair<Job, List<Asset>>>) {
        val visible = grouped.flatMap { it.second }
        if (selectedKey !in visible.map(::assetKey)) {
            val selectedJobId = taskVM.selectedJobId.value
            selectedKey = visible.firstOrNull { it.jobId == selectedJobId }?.let(::assetKey)
                ?: visible.firstOrNull()?.let(::assetKey)
        }
        groupsContainer.removeAllViews()
        cardBindings.clear()
        groupBindings.clear()
        grouped.forEachIndexed { index, (job, assets) ->
            val group = createGroup(job, assets)
            groupsContainer.addView(group.root, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { if (index > 0) topMargin = dp(12) })
        }
    }

    private fun createGroup(job: Job, assets: List<Asset>): GroupBinding {
        val root = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        val header = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(9), dp(10), dp(9))
            background = requireContext().getDrawable(R.drawable.bg_patrol_panel)
        }
        val labels = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        val title = TextView(requireContext()).apply {
            setTextColor(0xFF25313D.toInt())
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val counts = TextView(requireContext()).apply {
            setTextColor(0xFF65717D.toInt())
            textSize = 11f
            setPadding(0, dp(3), 0, 0)
        }
        labels.addView(title)
        labels.addView(counts)
        header.addView(labels, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val retry = TextView(requireContext()).apply {
            setText(R.string.asset_continue_finalization)
            gravity = Gravity.CENTER
            textSize = 12f
            setTextColor(0xFF25313D.toInt())
            setPadding(dp(12), 0, dp(12), 0)
            background = requireContext().getDrawable(R.drawable.bg_patrol_secondary_button)
            visibility = if (job.status == JobStatus.FINALIZING) View.VISIBLE else View.GONE
            setOnClickListener { retryFinalization(job.jobId) }
        }
        header.addView(retry, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34)).apply {
            marginStart = dp(10)
        })
        root.addView(
            header,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        val assetGrid = GridLayout(requireContext()).apply {
            columnCount = 3
            orientation = GridLayout.HORIZONTAL
        }
        assets.forEach { asset -> assetGrid.addView(createCard(asset)) }
        root.addView(assetGrid, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = dp(4)
        })
        return GroupBinding(root, title, counts, retry).also { groupBindings[job.jobId] = it }
    }

    private fun createCard(asset: Asset): View {
        val key = assetKey(asset)
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = requireContext().getDrawable(R.drawable.bg_patrol_item)
            isSelected = key == selectedKey
            setOnClickListener { selectAsset(key) }
        }
        val image = ImageView(requireContext()).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(if (asset.typeId == "photo") 0xFFDDF3FF.toInt() else 0xFFFFF0B8.toInt())
            setImageResource(
                if (asset.typeId == "photo") {
                    android.R.drawable.ic_menu_gallery
                } else {
                    android.R.drawable.ic_menu_save
                },
            )
        }
        val name = TextView(requireContext()).apply {
            setTextColor(0xFF25313D.toInt())
            textSize = 12f
            maxLines = 1
            setPadding(0, dp(7), 0, 0)
        }
        val status = TextView(requireContext()).apply {
            setTextColor(0xFF78838E.toInt())
            textSize = 10f
        }
        card.addView(image, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(78)))
        card.addView(name)
        card.addView(status)
        cardBindings[key] = CardBinding(card, image, name, status, asset)
        return card.apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = cardWidth()
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                setMargins(dp(4), dp(4), dp(4), dp(4))
            }
        }
    }

    private fun selectAsset(key: String) {
        if (key == selectedKey) return
        selectedKey?.let { cardBindings[it]?.card?.isSelected = false }
        selectedKey = key
        cardBindings[key]?.card?.isSelected = true
        renderDetail(cardBindings[key]?.asset)
    }

    private fun updateBindings() {
        if (!::groupsContainer.isInitialized) return
        val photos = photosByKey()
        jobs.forEach { job ->
            groupBindings[job.jobId]?.let { binding ->
                val jobPhotos = photos.values.filter { it.jobId == job.jobId }
                val localCount = jobPhotos.count { it.localUri?.let(::isFile) == true }
                val uploadedCount = uploadAssets.asSequence()
                    .filter { it.jobId == job.jobId && it.typeId == "photo" && it.state == AssetState.UPLOADED }
                    .map(::mediaKey)
                    .distinct()
                    .count()
                binding.title.text = "${job.title} · ${jobStatus(job.status)}"
                binding.counts.text = getString(R.string.asset_group_counts, jobPhotos.size, localCount, uploadedCount)
                binding.retry.isEnabled = job.jobId !in pendingRetries
                binding.retry.alpha = if (binding.retry.isEnabled) 1f else 0.5f
            }
        }
        val assetsByKey = jobs.flatMap { visibleAssets(it, photos) }.associateBy(::assetKey)
        cardBindings.forEach { (key, binding) ->
            assetsByKey[key]?.let { asset ->
                binding.asset = asset
                binding.name.text = asset.meta["fileName"] ?: asset.assetId
                binding.status.text = "${formatBytes(asset.sizeBytes)} · ${displayState(asset)}"
                updateCardImage(binding)
            }
        }
        renderDetail(selectedAsset())
    }

    private fun retryFinalization(jobId: String) {
        val job = jobs.firstOrNull { it.jobId == jobId && it.status == JobStatus.FINALIZING } ?: return
        if (!pendingRetries.add(jobId)) return
        updateBindings()
        Toast.makeText(requireContext(), R.string.asset_retry_started, Toast.LENGTH_SHORT).show()
        vm.retryFinalization(job, object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) = finishRetry(jobId, null)
            override fun onFailure(error: Throwable) = finishRetry(jobId, error)
        })
    }

    private fun finishRetry(jobId: String, error: Throwable?) {
        mainHandler.post {
            pendingRetries.remove(jobId)
            if (!isAdded || view == null) return@post
            updateBindings()
            val message = if (error == null) {
                getString(R.string.asset_retry_success)
            } else {
                getString(R.string.asset_retry_failed, error.message.orEmpty())
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectedAsset() = selectedKey?.let { cardBindings[it]?.asset }

    private fun renderDetail(asset: Asset?) {
        if (asset == null) {
            previewGeneration++
            previewAssetKey = null
            previewSource = null
            preview.setImageDrawable(null)
            assetName.setText(R.string.empty_assets)
            assetDetail.text = ""
            return
        }
        assetName.text = asset.meta["fileName"] ?: asset.assetId
        assetDetail.text = buildString {
            appendLine("${getString(R.string.asset_type)}  ${asset.typeId.uppercase()}")
            appendLine("${getString(R.string.asset_size)}  ${formatBytes(asset.sizeBytes)}")
            appendLine("${getString(R.string.asset_status)}  ${displayState(asset)}")
            appendLine("${getString(R.string.task_id)}  ${asset.jobId}")
            append("MD5  ${asset.md5.orEmpty()}")
        }
        loadPreview(asset)
    }

    private fun loadPreview(asset: Asset) {
        val key = assetKey(asset)
        val thumbnail = mediaKeyOrNull(asset)?.let(thumbnailPaths::get)?.takeIf(::isFile)
        val original = asset.localUri?.takeIf(::isFile)
        val source = original ?: thumbnail
        val sourceToken = "${original.orEmpty()}|${thumbnail.orEmpty()}"
        if (previewAssetKey == key && previewSource == sourceToken) return
        previewAssetKey = key
        previewSource = sourceToken
        val generation = ++previewGeneration
        preview.setBackgroundColor(0xFF111A2B.toInt())
        if (asset.typeId != "photo" || source == null) {
            preview.setImageResource(
                if (asset.typeId == "photo") {
                    android.R.drawable.ic_menu_gallery
                } else {
                    android.R.drawable.ic_menu_save
                },
            )
            return
        }

        cachedBitmap(source, PREVIEW_WIDTH, PREVIEW_HEIGHT)?.let {
            preview.setImageBitmap(it)
            return
        }
        if (thumbnail != null && thumbnail != source) {
            cachedBitmap(thumbnail, PREVIEW_WIDTH, PREVIEW_HEIGHT)?.let(preview::setImageBitmap)
        }
        imageExecutor.execute {
            if (thumbnail != null && thumbnail != source &&
                cachedBitmap(thumbnail, PREVIEW_WIDTH, PREVIEW_HEIGHT) == null
            ) {
                decodeBitmap(thumbnail, PREVIEW_WIDTH, PREVIEW_HEIGHT)?.let { bitmap ->
                    cacheBitmap(thumbnail, PREVIEW_WIDTH, PREVIEW_HEIGHT, bitmap)
                    mainHandler.post {
                        if (generation == previewGeneration && key == selectedKey &&
                            cachedBitmap(source, PREVIEW_WIDTH, PREVIEW_HEIGHT) == null
                        ) {
                            preview.setImageBitmap(bitmap)
                        }
                    }
                }
            }
            val bitmap = cachedBitmap(source, PREVIEW_WIDTH, PREVIEW_HEIGHT)
                ?: decodeBitmap(source, PREVIEW_WIDTH, PREVIEW_HEIGHT)?.also {
                    cacheBitmap(source, PREVIEW_WIDTH, PREVIEW_HEIGHT, it)
                }
            mainHandler.post {
                if (generation == previewGeneration && key == selectedKey && bitmap != null) {
                    preview.setImageBitmap(bitmap)
                }
            }
        }
    }

    private fun updateCardImage(binding: CardBinding) {
        if (binding.asset.typeId != "photo") return
        val source = thumbnailPaths[mediaKey(binding.asset)]?.takeIf(::isFile)
            ?: binding.asset.localUri?.takeIf(::isFile)
        if (binding.imageSource == source) return
        binding.imageSource = source
        val requestTag = "${assetKey(binding.asset)}|${source.orEmpty()}"
        binding.image.tag = requestTag
        if (source == null) {
            binding.image.setImageResource(android.R.drawable.ic_menu_gallery)
            return
        }
        cachedBitmap(source, CARD_WIDTH, CARD_HEIGHT)?.let {
            binding.image.setImageBitmap(it)
            return
        }
        imageExecutor.execute {
            val bitmap = decodeBitmap(source, CARD_WIDTH, CARD_HEIGHT)?.also {
                cacheBitmap(source, CARD_WIDTH, CARD_HEIGHT, it)
            }
            mainHandler.post {
                if (binding.image.tag == requestTag && binding.image.isAttachedToWindow && bitmap != null) {
                    binding.image.setImageBitmap(bitmap)
                }
            }
        }
    }

    private fun decodeBitmap(path: String, maxWidth: Int, maxHeight: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxWidth || bounds.outHeight / sampleSize > maxHeight) sampleSize *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sampleSize })
    }

    @Synchronized
    private fun cachedBitmap(path: String, width: Int, height: Int) = bitmapCache.get("$path@$width:$height")

    @Synchronized
    private fun cacheBitmap(path: String, width: Int, height: Int, bitmap: Bitmap) {
        bitmapCache.put("$path@$width:$height", bitmap)
    }

    private fun isFile(path: String) = File(path).isFile
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun cardWidth(): Int {
        val available = groupsContainer.width.takeIf { it > 0 } ?: dp(480)
        return ((available - dp(24)) / 3).coerceAtLeast(dp(110))
    }

    private fun assetKey(asset: Asset) = if (asset.typeId == "photo") {
        mediaKey(asset)
    } else {
        "${asset.jobId}/${asset.assetId}"
    }

    private fun mediaKey(asset: Asset): String =
        "${asset.jobId}/${asset.meta["dirIndex"].orEmpty()}/${asset.meta["fileIndex"].orEmpty()}"

    private fun mediaKeyOrNull(asset: Asset) = if (asset.typeId == "photo") mediaKey(asset) else null

    private fun displayState(asset: Asset): String {
        val upload = uploadAssets.firstOrNull { assetKey(it) == assetKey(asset) }
        if (upload != null) return assetState(upload.state)
        if (asset.typeId == "photo" && asset.localUri == null) return getString(R.string.status_pending_download)
        if (asset.typeId == "photo") return getString(R.string.status_downloaded)
        return assetState(asset.state)
    }

    private fun assetState(state: AssetState) = getString(when (state) {
        AssetState.DETECTED -> R.string.status_downloaded
        AssetState.QUEUED -> R.string.status_queued
        AssetState.UPLOADING -> R.string.status_uploading
        AssetState.UPLOADED -> R.string.status_uploaded
        AssetState.FAILED -> R.string.status_failed
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

    private data class CardBinding(
        val card: View,
        val image: ImageView,
        val name: TextView,
        val status: TextView,
        var asset: Asset,
        var imageSource: String? = null,
    )

    private data class GroupBinding(
        val root: View,
        val title: TextView,
        val counts: TextView,
        val retry: TextView,
    )

    companion object {
        private const val FILTER_ALL = 0
        private const val FILTER_PHOTO = 1
        private const val FILTER_ROUTE = 2
        private const val FILTER_POINT_EVENT = 3
        private const val CARD_WIDTH = 320
        private const val CARD_HEIGHT = 240
        private const val PREVIEW_WIDTH = 800
        private const val PREVIEW_HEIGHT = 600
    }

    override fun onDestroyView() {
        previewGeneration++
        previewAssetKey = null
        previewSource = null
        structureKey = ""
        cardBindings.clear()
        groupBindings.clear()
        super.onDestroyView()
    }

    override fun onDestroy() {
        previewGeneration++
        imageExecutor.shutdownNow()
        bitmapCache.evictAll()
        super.onDestroy()
    }
}
