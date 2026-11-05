package com.dji.open.pilot.plugin.main

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.databinding.ItemModuleCardBinding
import com.dji.open.pilot.plugin.main.DemoStatus
import com.dji.open.pilot.plugin.main.ModuleItem

class ModuleAdapter(
    private val items: List<ModuleItem>,
    private val onItemClick: (ModuleItem) -> Unit
) : RecyclerView.Adapter<ModuleAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemModuleCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ModuleItem) {
            binding.ivIcon.setImageResource(item.iconResId)
            binding.tvTitle.setText(item.titleResId)
            binding.tvInterfaceName.text = item.interfaceName
            binding.tvDesc.setText(item.descResId)

            // 截图区：有图显示图，无图显示占位框
            if (item.thumbResId != 0) {
                binding.ivThumb.setImageResource(item.thumbResId)
                binding.ivThumb.isVisible = true
                binding.tvThumbPlaceholder.isVisible = false
            } else {
                binding.ivThumb.isVisible = false
                binding.tvThumbPlaceholder.isVisible = true
            }

            // Demo 状态
            val (statusTextResId, statusColor) = when (item.status) {
                DemoStatus.DONE -> Pair(R.string.demo_status_done, Color.parseColor("#28A856"))
                DemoStatus.WIP  -> Pair(R.string.demo_status_wip,  Color.parseColor("#E08000"))
                DemoStatus.TODO -> Pair(R.string.demo_status_todo, Color.parseColor("#B0B0C8"))
            }
            binding.tvStatus.setText(statusTextResId)
            binding.tvStatus.setTextColor(statusColor)
            binding.viewStatusDot.background.setTint(statusColor)

            // 点击：仅 demoClass != null 时响应
            if (item.demoClass != null) {
                binding.root.setOnClickListener { onItemClick(item) }
                binding.root.alpha = 1.0f
            } else {
                binding.root.setOnClickListener(null)
                binding.root.alpha = 0.7f
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemModuleCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}
