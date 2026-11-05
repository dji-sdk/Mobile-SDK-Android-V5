package com.dji.open.pilot.plugin.sampleitem.mapmanager.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.databinding.ItemComponentMenuBinding
import com.dji.open.pilot.plugin.sampleitem.mapmanager.MapCaseType

/**
 * 地图 Demo 页左侧菜单适配器，带分组 Header。
 * - 地图控件：地图控件 / 批量控件 / 图层管理
 * - 地图元素：地图元素
 */
class MapMenuAdapter(
    private val onItemClick: (MapCaseType) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed class Row {
        data class Header(val titleRes: Int) : Row()
        data class Item(val type: MapCaseType) : Row()
    }

    private val rows: List<Row> = buildList {
        MapCaseType.VISIBLE_CASES.groupBy { type ->
            if (type == MapCaseType.ELEMENT) R.string.map_group_element else R.string.map_group_widget
        }.forEach { (titleRes, types) ->
            add(Row.Header(titleRes))
            types.forEach { add(Row.Item(it)) }
        }
    }

    private var selectedPosition: Int = -1

    fun updateSelection(type: MapCaseType) {
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
        holder.binding.root.isSelected = isSelected
        holder.binding.root.setBackgroundResource(
            if (isSelected) R.drawable.bg_menu_item_selected else android.R.color.white
        )
    }

    inner class HeaderViewHolder(val tv: TextView) : RecyclerView.ViewHolder(tv)
    inner class ItemViewHolder(val binding: ItemComponentMenuBinding) : RecyclerView.ViewHolder(binding.root)
}
