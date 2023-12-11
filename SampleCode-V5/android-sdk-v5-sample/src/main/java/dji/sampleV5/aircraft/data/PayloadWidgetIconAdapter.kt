package dji.sampleV5.aircraft.data

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import dji.sampleV5.aircraft.R


/**
 * Description :PayloadWidgetIcon数据处理类
 *
 * @author: Byte.Cai
 *  date : 2022/12/1
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class PayloadWidgetIconAdapter :
    ListAdapter<PayloadWidgetItem, PayloadWidgetIconAdapter.ViewHolder>(DiffCallback) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_payload_wdiget_item, parent, false)
        return ViewHolder(view, parent.context)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class ViewHolder(itemView: View, val context: Context) : RecyclerView.ViewHolder(itemView) {
        private val iconImage: ImageView = itemView.findViewById(R.id.iv_widget_icon)
        private val iconDesc: TextView = itemView.findViewById(R.id.tv_widget_desc)

        fun bind(item: PayloadWidgetItem) {
            Glide.with(context).load(item.imgPath).into(iconImage)
            iconDesc.text = item.des
        }
    }


    object DiffCallback : DiffUtil.ItemCallback<PayloadWidgetItem>() {
        override fun areItemsTheSame(oldItem: PayloadWidgetItem, newItem: PayloadWidgetItem): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: PayloadWidgetItem, newItem: PayloadWidgetItem): Boolean {
            return oldItem.des == newItem.des && oldItem.imgPath == newItem.imgPath
        }

    }
}