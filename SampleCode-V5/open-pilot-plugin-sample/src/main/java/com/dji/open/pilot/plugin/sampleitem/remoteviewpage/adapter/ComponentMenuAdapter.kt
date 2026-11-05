package com.dji.open.pilot.plugin.sampleitem.remoteviewpage.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.databinding.ItemComponentMenuBinding
import com.dji.open.pilot.plugin.sampleitem.remoteviewpage.ComponentType

class ComponentMenuAdapter(
    private val items: List<ComponentType>,
    private val onItemClick: (ComponentType) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed class Row {
        data class Header(val titleRes: Int) : Row()
        data class Item(val type: ComponentType) : Row()
    }

    private val rows: List<Row> = buildList {
        add(Row.Header(R.string.comp_group_basic))
        add(Row.Item(ComponentType.LAYOUT))
        add(Row.Item(ComponentType.TEXT_VIEW))
        add(Row.Item(ComponentType.BUTTON))
        add(Row.Header(R.string.comp_group_input))
        add(Row.Item(ComponentType.EDIT_TEXT))
        add(Row.Item(ComponentType.CHECK_BOX))
        add(Row.Item(ComponentType.SWITCH))
        add(Row.Item(ComponentType.SPINNER))
        add(Row.Header(R.string.comp_group_display))
        add(Row.Item(ComponentType.IMAGE_VIEW))
        add(Row.Item(ComponentType.WEB_VIEW))
        add(Row.Item(ComponentType.LIST_VIEW))
        add(Row.Item(ComponentType.PROGRESS_BAR))
    }

    /** 当前选中的行索引（-1 表示未选中） */
    private var selectedPosition: Int = -1

    /** 更新选中状态，局部刷新旧行和新行 */
    fun updateSelection(type: ComponentType) {
        val newPos = rows.indexOfFirst { it is Row.Item && it.type == type }
        if (newPos == selectedPosition) return
        val oldPos = selectedPosition
        selectedPosition = newPos
        if (oldPos >= 0) notifyItemChanged(oldPos, PAYLOAD_SELECTION)
        if (newPos >= 0) notifyItemChanged(newPos, PAYLOAD_SELECTION)
    }

    companion object {
        private const val VT_HEADER        = 0
        private const val VT_ITEM          = 1
        private const val PAYLOAD_SELECTION = "selection_changed"
    }

    override fun getItemViewType(position: Int) =
        if (rows[position] is Row.Header) VT_HEADER else VT_ITEM

    inner class HeaderViewHolder(val tv: TextView) : RecyclerView.ViewHolder(tv)

    inner class ItemViewHolder(val binding: ItemComponentMenuBinding) :
        RecyclerView.ViewHolder(binding.root)

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

    /** 带 payload 的增量刷新：只更新选中状态，跳过完整绑定和默认动画 */
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: List<Any>
    ) {
        if (payloads.isNotEmpty() && holder is ItemViewHolder) {
            val isSelected = (position == selectedPosition)
            holder.binding.root.isSelected = isSelected
            holder.binding.root.setBackgroundResource(
                if (isSelected) R.drawable.bg_menu_item_selected else android.R.color.white
            )
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Header -> (holder as HeaderViewHolder).tv.setText(row.titleRes)
            is Row.Item -> {
                val vh = holder as ItemViewHolder
                val type = row.type
                val isSelected = (position == selectedPosition)

                // selected 状态驱动 color selector（图标/文字/箭头 tint 自动切换）
                vh.binding.root.isSelected = isSelected

                // 条目文字：英文组件名 + 中文名，如 "Button 按钮"
                val name = vh.binding.root.context.getString(type.titleRes)
                vh.binding.tvName.text = name

                // 背景高亮
                vh.binding.root.setBackgroundResource(
                    if (isSelected) R.drawable.bg_menu_item_selected else android.R.color.white
                )

                // 最后一个 Item 之后无需分割线（分组末尾紧接 Header，视觉上 Header 的 paddingTop 已充当间距）
                val isLastInGroup = position == rows.size - 1 ||
                        rows.getOrNull(position + 1) is Row.Header
                vh.binding.divider.visibility =
                    if (isLastInGroup) android.view.View.INVISIBLE else android.view.View.VISIBLE

                vh.binding.root.setOnClickListener { onItemClick(type) }
            }
        }
    }

    override fun getItemCount() = rows.size
}
