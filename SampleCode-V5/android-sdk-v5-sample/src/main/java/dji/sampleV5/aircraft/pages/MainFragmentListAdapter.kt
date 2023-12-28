package dji.sampleV5.aircraft.pages

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.data.FragmentPageItem
import dji.v5.utils.common.StringUtils


/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/5/10
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
class MainFragmentListAdapter(private val onClick: (FragmentPageItem) -> Unit) :
    ListAdapter<FragmentPageItem, MainFragmentListAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.frag_main_item, parent, false)
        return ViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class ViewHolder(itemView: View, val onClick: (FragmentPageItem) -> Unit) : RecyclerView.ViewHolder(itemView) {

        private val titleTextView: TextView = itemView.findViewById(R.id.item_title)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.item_description)
        private var currentPageInfoItem: FragmentPageItem? = null

        init {
            itemView.setOnClickListener {
                currentPageInfoItem?.let {
                    onClick(it)
                }
            }
        }

        fun bind(pageInfo: FragmentPageItem) {
            currentPageInfoItem = pageInfo
            titleTextView.text = StringUtils.getResStr(itemView.context, pageInfo.title)
            descriptionTextView.text = StringUtils.getResStr(itemView.context, pageInfo.description)
            if (pageInfo.isStrike) {
                titleTextView.paintFlags = titleTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                descriptionTextView.paintFlags = descriptionTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                titleTextView.paintFlags = titleTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                descriptionTextView.paintFlags = descriptionTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<FragmentPageItem>() {
        override fun areItemsTheSame(oldItem: FragmentPageItem, newItem: FragmentPageItem): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: FragmentPageItem, newItem: FragmentPageItem): Boolean {
            return oldItem.title == newItem.title
        }
    }
}