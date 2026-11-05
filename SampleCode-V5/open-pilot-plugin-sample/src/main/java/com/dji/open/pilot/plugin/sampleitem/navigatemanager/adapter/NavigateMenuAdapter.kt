package com.dji.open.pilot.plugin.sampleitem.navigatemanager.adapter

import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.recyclerview.widget.RecyclerView
import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.databinding.ItemComponentMenuBinding
import com.dji.open.pilot.plugin.sampleitem.navigatemanager.NavigateType

/**
 * 页面跳转 Demo 页左侧菜单适配器。
 * 平铺 8 项（覆盖 NavigateAction.Value 全部有效跳转目标），无分组 Header。
 * 选中效果与 DialogMenuAdapter 一致：selector 驱动文字变色 + 浅蓝底蓝边框背景。
 */
class NavigateMenuAdapter(
    private val onItemClick: (NavigateType) -> Unit
) : RecyclerView.Adapter<NavigateMenuAdapter.ItemViewHolder>() {

    private val items: List<NavigateType> = NavigateType.values().toList()

    /** 当前选中的行索引（-1 表示未选中） */
    private var selectedPosition: Int = -1

    companion object {
        private const val PAYLOAD_SELECTION = "selection_changed"
    }

    /** 更新选中状态，局部刷新旧行和新行（带 payload，跳过完整绑定与默认动画） */
    fun updateSelection(type: NavigateType) {
        val newPos = items.indexOf(type)
        if (newPos == selectedPosition) return
        val oldPos = selectedPosition
        selectedPosition = newPos
        if (oldPos >= 0) notifyItemChanged(oldPos, PAYLOAD_SELECTION)
        if (newPos >= 0) notifyItemChanged(newPos, PAYLOAD_SELECTION)
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemComponentMenuBinding.inflate(inflater, parent, false)
        return ItemViewHolder(binding)
    }

    /** 带 payload 的增量刷新：只更新选中状态，跳过完整绑定和默认动画 */
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isNotEmpty()) {
            applySelection(holder, position)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val type = items[position]
        holder.binding.tvName.setText(type.titleRes)
        applySelection(holder, position)
        holder.binding.root.setOnClickListener { onItemClick(type) }
    }

    private fun applySelection(holder: ItemViewHolder, position: Int) {
        val isSelected = position == selectedPosition
        holder.binding.root.isSelected = isSelected
        holder.binding.root.setBackgroundResource(
            if (isSelected) R.drawable.bg_menu_item_selected else android.R.color.white
        )
    }

    inner class ItemViewHolder(val binding: ItemComponentMenuBinding) :
        RecyclerView.ViewHolder(binding.root)
}
