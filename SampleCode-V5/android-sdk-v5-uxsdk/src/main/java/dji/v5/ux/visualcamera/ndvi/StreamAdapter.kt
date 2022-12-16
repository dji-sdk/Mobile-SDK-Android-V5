package dji.v5.ux.visualcamera.ndvi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dji.v5.utils.common.JsonUtil
import dji.v5.utils.common.LogUtils
import dji.v5.ux.R

class StreamAdapter<T>(val onClickCallback: (T) -> Unit) :
    RecyclerView.Adapter<StreamAdapter.ViewHolder>() {

    var tag = LogUtils.getTag(this)

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var ivPreview: ImageView = itemView.findViewById(R.id.iv_preview)
        var tvName: TextView = itemView.findViewById(R.id.tv_palette_name)
    }

    var models: MutableList<T> = mutableListOf()

    // 目前选中的位置
    var currentPosition: T? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.uxsdk_palette_selection_panel_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemOrigin = models[position]
        LogUtils.i(tag, "onBindViewHolder", JsonUtil.toJson(itemOrigin))
        if (itemOrigin is StreamPanelUtil.NarrowBandModel) {
            val item = itemOrigin as StreamPanelUtil.NarrowBandModel
            if (currentPosition == null) {
                holder.ivPreview.setImageResource(item.imageRes)
                holder.tvName.text = item.nameRes
                holder.itemView.isClickable = true
                holder.ivPreview.isSelected = false
                holder.tvName.isSelected = false
                holder.itemView.setOnClickListener {
                    onClickCallback(item as T)
                }
                return
            }
            val currentItem = currentPosition as StreamPanelUtil.NarrowBandModel
            holder.ivPreview.setImageResource(item.imageRes)
            holder.tvName.text = item.nameRes
            currentItem.let {
                val isSelected = item.sourceType == it.sourceType
                holder.ivPreview.isSelected = isSelected
                holder.tvName.isSelected = isSelected
            }
            holder.itemView.isClickable = true

            holder.itemView.setOnClickListener {
                onClickCallback(item as T)
            }
        } else if (itemOrigin is StreamPanelUtil.VegetationModel) {
            val item = itemOrigin as StreamPanelUtil.VegetationModel
            if (currentPosition == null) {
                holder.ivPreview.setImageResource(item.imageRes)
                holder.tvName.text = item.nameRes
                holder.itemView.isClickable = true
                holder.ivPreview.isSelected = false
                holder.tvName.isSelected = false
                holder.itemView.setOnClickListener {
                    onClickCallback(item as T)
                }
                return
            }
            val currentItem = currentPosition as StreamPanelUtil.VegetationModel
            holder.ivPreview.setImageResource(item.imageRes)
            holder.tvName.text = item.nameRes
            currentItem.let {
                val isSelected = item.sourceType == it.sourceType
                holder.ivPreview.isSelected = isSelected
                holder.tvName.isSelected = isSelected
            }
            holder.itemView.isClickable = true

            holder.itemView.setOnClickListener {
                onClickCallback(item as T)
            }
        }
    }

    override fun getItemCount(): Int {
        return models.size
    }
}