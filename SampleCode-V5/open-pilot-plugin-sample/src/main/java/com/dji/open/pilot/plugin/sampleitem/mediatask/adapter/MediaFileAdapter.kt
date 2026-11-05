package com.dji.open.pilot.plugin.sampleitem.mediatask.adapter

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.databinding.ItemMediaFileBinding
import com.dji.open.pilot.plugin.sampleitem.mediatask.formatFileSize
import dji.v5.manager.datacenter.media.MediaFile

/**
 * 媒体文件三列网格适配器。
 *
 * 约定：
 * - 稳定键为 [MediaFile.fileIndex]，异步缩略图回调必须核对当前绑定的 fileIndex，避免快速滚动串图。
 * - 选择态变化使用 payload 局部刷新，不使用全量 `notifyDataSetChanged()`。
 */
class MediaFileAdapter(
    private val onFileClick: (Int) -> Unit,
    private val onThumbnailRequired: (MediaFile, (Result<Bitmap>) -> Unit) -> Unit
) : ListAdapter<MediaFile, MediaFileAdapter.ViewHolder>(DIFF_CALLBACK) {

    private companion object {
        const val PAYLOAD_SELECTION = "payload_selection"

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MediaFile>() {
            override fun areItemsTheSame(oldItem: MediaFile, newItem: MediaFile): Boolean =
                oldItem.fileIndex == newItem.fileIndex

            override fun areContentsTheSame(oldItem: MediaFile, newItem: MediaFile): Boolean =
                oldItem.fileIndex == newItem.fileIndex &&
                    oldItem.fileName == newItem.fileName &&
                    oldItem.fileSize == newItem.fileSize
        }
    }

    private var selectionMode = false
    private var selectedIndices: Set<Int> = emptySet()

    /** 提交选择态；仅刷新发生变化或模式切换的条目。 */
    fun submitSelection(mode: Boolean, indices: Set<Int>) {
        val modeChanged = selectionMode != mode
        val previousIndices = selectedIndices
        selectionMode = mode
        selectedIndices = indices

        if (modeChanged) {
            notifyItemRangeChanged(0, itemCount, PAYLOAD_SELECTION)
            return
        }
        val changed = (previousIndices - indices) + (indices - previousIndices)
        changed.forEach { fileIndex ->
            val position = positionOf(fileIndex)
            if (position >= 0) {
                notifyItemChanged(position, PAYLOAD_SELECTION)
            }
        }
    }

    private fun positionOf(fileIndex: Int): Int {
        val list = currentList
        for (position in list.indices) {
            if (list[position].fileIndex == fileIndex) {
                return position
            }
        }
        return -1
    }

    inner class ViewHolder(private val binding: ItemMediaFileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var boundFileIndex = -1

        fun bind(file: MediaFile) {
            boundFileIndex = file.fileIndex
            binding.tvFileName.text = file.fileName
            binding.tvFileMeta.text = metaText(binding.root.context, file)
            binding.root.setOnClickListener { onFileClick(file.fileIndex) }
            bindSelection(file.fileIndex)
            loadThumbnail(file)
        }

        fun bindSelection(fileIndex: Int) {
            val selected = selectionMode && selectedIndices.contains(fileIndex)
            binding.viewSelectionOverlay.isVisible = selected
            binding.tvSelectionBadge.isVisible = selected
        }

        private fun loadThumbnail(file: MediaFile) {
            val cached = file.thumbNail
            if (cached != null && !cached.isRecycled) {
                showThumbnail(cached)
                return
            }
            val requestIndex = file.fileIndex
            binding.progressThumb.isVisible = true
            binding.tvThumbPlaceholder.isVisible = false
            onThumbnailRequired(file) { result ->
                // 回调可能发生在滚动之后，必须核对当前绑定项。
                if (boundFileIndex != requestIndex) {
                    return@onThumbnailRequired
                }
                binding.progressThumb.isVisible = false
                val bitmap = result.getOrNull()
                if (bitmap == null || bitmap.isRecycled) {
                    binding.tvThumbPlaceholder.isVisible = true
                } else {
                    showThumbnail(bitmap)
                }
            }
        }

        private fun showThumbnail(bitmap: Bitmap) {
            binding.ivThumb.setImageBitmap(bitmap)
            binding.ivThumb.isVisible = true
            binding.tvThumbPlaceholder.isVisible = false
        }

        private fun metaText(context: Context, file: MediaFile): String {
            val type = file.fileType.name
            val size = formatFileSize(file.fileSize)
            return context.getString(R.string.media_file_meta, type, size)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            ItemMediaFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_SELECTION)) {
            holder.bindSelection(getItem(position).fileIndex)
            return
        }
        super.onBindViewHolder(holder, position, payloads)
    }
}