package com.dji.open.pilot.plugin.sampleitem.cameradrawing.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.databinding.ItemComponentMenuBinding
import com.dji.open.pilot.plugin.sampleitem.cameradrawing.CameraDrawingType

/**
 * 图传绘制 Demo 页左侧菜单适配器，带分组 Header。
 * - 图传绘制：矩形框、多边形、文字
 */
class CameraDrawingMenuAdapter(
    private val onItemClick: (CameraDrawingType) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed class Row {
        data class Header(val titleRes: Int) : Row()
        data class Item(val type: CameraDrawingType) : Row()
    }

    private val rows: List<Row> = buildList {
        add(Row.Header(R.string.cvd_group_drawing))
        add(Row.Item(CameraDrawingType.BOX))
        add(Row.Item(CameraDrawingType.POLYGON))
        add(Row.Item(CameraDrawingType.TEXT))
    }

    private var selectedPosition: Int = -1

    fun updateSelection(type: CameraDrawingType) {
        val newPos = rows.indexOfFirst { it is Row.Item && it.type == type }
        if (newPos == selectedPosition) return
        val oldPos = selectedPosition
        selectedPosition = newPos
        if (oldPos >= 0) notifyItemChanged(oldPos, PAYLOAD_SELECTION)
        if (newPos >= 0) notifyItemChanged(newPos, PAYLOAD_SELECTION)
    }

    companion object {
        private const val VT_HEADER = 0
        private const val VT_ITEM = 1
        private const val PAYLOAD_SELECTION = "selection_changed"
    }

    override fun getItemViewType(position: Int) =
        if (rows[position] is Row.Header) VT_HEADER else VT_ITEM

    override fun getItemCount() = rows.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VT_HEADER) {
            val tv = inflater.inflate(R.layout.item_section_header, parent, false) as TextView
            HeaderViewHolder(tv)
        } else {
            val binding = ItemComponentMenuBinding.inflate(inflater, parent, false)
            ItemViewHolder(binding)
        }
    }

    /** 带 payload 的增量刷新：只更新 Item 的选中状态，跳过完整绑定和默认动画 */
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: List<Any>
    ) {
        if (payloads.isNotEmpty() && holder is ItemViewHolder) {
            applySelection(holder, position)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Header -> {
                (holder as HeaderViewHolder).tv.setText(row.titleRes)
            }
            is Row.Item -> {
                val binding = (holder as ItemViewHolder).binding
                binding.tvName.setText(row.type.titleRes)
                applySelection(holder, position)
                binding.root.setOnClickListener { onItemClick(row.type) }
            }
        }
    }

    private fun applySelection(holder: ItemViewHolder, position: Int) {
        val isSelected = position == selectedPosition
        // selected 状态驱动 color selector（文字/箭头 tint 自动切换）
        holder.binding.root.isSelected = isSelected
        holder.binding.root.setBackgroundResource(
            if (isSelected) R.drawable.bg_menu_item_selected else android.R.color.white
        )
    }

    inner class HeaderViewHolder(val tv: TextView) : RecyclerView.ViewHolder(tv)
    inner class ItemViewHolder(val binding: ItemComponentMenuBinding) : RecyclerView.ViewHolder(binding.root)
}
