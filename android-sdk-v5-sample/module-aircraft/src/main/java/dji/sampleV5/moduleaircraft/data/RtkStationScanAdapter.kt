package dji.sampleV5.moduleaircraft.data

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import dji.sampleV5.modulecommon.util.ToastUtils
import dji.sampleV5.moduleaircraft.R
import dji.sdk.keyvalue.value.rtkbasestation.RTKStationConnetState
import dji.v5.utils.common.LogUtils

/**
 * Description :基站RTK的Adapter，展示扫码到的基站情况
 *
 * @author: Byte.Cai
 *  date : 2022/3/6
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class RtkStationScanAdapter(val context:Context,list: List<DJIRTKBaseStationConnectInfo>?) :
    RecyclerView.Adapter<RtkStationScanAdapter.RtkViewHolder>() {

   private val LEVEL_0 = 0
   private val LEVEL_1 = 1
   private val LEVEL_2 = 2
   private val LEVEL_3 = 3
   private val LEVEL_4 = 4
   private  var baseStationInfoList: List<DJIRTKBaseStationConnectInfo>? = list
    private val TAG="RtkStationScanAdapter"



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RtkViewHolder {
        val view: View = LayoutInflater.from(context)
            .inflate(R.layout.adapter_rtk_connect_status_item, parent, false)
        return RtkViewHolder(view)
    }

    override fun onBindViewHolder(holder: RtkViewHolder, position: Int) {
        baseStationInfoList?.get(position)?.let { info ->
            holder.mRtkStationNameTv.text = info.rtkStationName
            holder.mConnectSignalIv.setBackgroundResource(getSignalLevelDrawable(info.signalLevel))
            holder.itemView.setOnClickListener {
                if (!checkConnecting()) {//上一笔连接还没结束的话则不响应新的连接请求
                    val pos = holder.layoutPosition
                    mOnItemClickListener?.onItemClick(holder.itemView, pos)
                } else {
                    ToastUtils.showToast("The base station is currently connecting, please try to connect later！")
                }
            }
            when (info.connectStatus) {
                RTKStationConnetState.IDLE -> {
                    holder.mConnectStatusIv.gone()
                }
                RTKStationConnetState.CONNECTING -> {
                    holder.mConnectStatusIv.visible()
                    holder.mConnectStatusIv.setImageResource(R.drawable.ic_rotate_progress_circle)
                }
                RTKStationConnetState.CONNECTED -> {
                    holder.mConnectStatusIv.visible()
                    holder.mConnectStatusIv.setImageResource(R.drawable.ic_confirm)
                }
                else -> {
                    holder.mConnectStatusIv.gone()
                }
            }

        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        return if (baseStationInfoList == null) 0 else baseStationInfoList!!.size
    }

    interface OnItemClickListener {
        fun onItemClick(view: View?, position: Int)
    }

    private var mOnItemClickListener: OnItemClickListener? = null

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        mOnItemClickListener = onItemClickListener
    }

    @DrawableRes
    fun getSignalLevelDrawable(signalLevel: Int): Int {
        LogUtils.d(TAG,"getSignalLevelDrawable,signalLevel=$signalLevel")
        return when (signalLevel) {
            LEVEL_0 -> R.drawable.ic_topbar_signal_level_0
            LEVEL_1 -> R.drawable.ic_topbar_signal_level_1
            LEVEL_2 -> R.drawable.ic_topbar_signal_level_2
            LEVEL_3 -> R.drawable.ic_topbar_signal_level_3
            LEVEL_4 -> R.drawable.ic_topbar_signal_level_4
            else -> R.drawable.ic_topbar_signal_level_5
        }
    }


    class RtkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var mConnectStatusIv: ImageView
        var mRtkStationNameTv: TextView
        var mConnectSignalIv: ImageView

        init {
            mConnectStatusIv = itemView.findViewById(R.id.connect_status_iv)
            mRtkStationNameTv = itemView.findViewById(R.id.station_name_tv)
            mConnectSignalIv = itemView.findViewById(R.id.connect_signal_iv)
            mConnectStatusIv.visibility = View.GONE
        }
    }

    fun View.visible() {
        this.visibility = View.VISIBLE
    }

    fun View.gone() {
        this.visibility = View.GONE
    }

    private fun checkConnecting(): Boolean {
        baseStationInfoList?.let {
            for (station in it) {
                if (station.connectStatus == RTKStationConnetState.CONNECTING) {
                    return@let true
                }
            }
            return@let false
        }
        return false
    }


}