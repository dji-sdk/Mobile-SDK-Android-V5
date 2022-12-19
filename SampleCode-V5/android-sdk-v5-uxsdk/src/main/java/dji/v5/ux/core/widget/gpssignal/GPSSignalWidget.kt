package dji.v5.ux.core.widget.gpssignal

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import dji.v5.manager.aircraft.rtk.RTKCenter
import dji.v5.utils.common.LogUtils
import dji.v5.ux.R
import dji.v5.ux.accessory.RTKEnabledWidgetModel
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.base.widget.FrameLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.getString
import dji.v5.ux.core.popover.PopoverHelper

/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2022/9/12
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class GpsSignalWidget @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : ConstraintLayoutWidget<Boolean>(context, attrs, defStyleAttr) {
    private val ivRtkIcon: ImageView = findViewById(R.id.iv_rtk_icon)
    private val ivSatelliteIcon: ImageView = findViewById(R.id.iv_satellite_icon)
    private val tvSatelliteCount: TextView = findViewById(R.id.tv_satellite_count)
    private var rtkOverView: GpsSignalWidgetModel.RtkOverview = GpsSignalWidgetModel.RtkOverview()
    private val rootView: ConstraintLayout = findViewById(R.id.root_view)

    private val rtkEnabledWidgetModel by lazy {
        RTKEnabledWidgetModel(
            DJISDKModel.getInstance(),
            ObservableInMemoryKeyedStore.getInstance()
        )
    }

    private val popover by lazy {
        PopoverHelper.showPopover(rootView, GpsSignalPopoverView(context))
    }

    private val gpsSignalWidgetModel by lazy {
        GpsSignalWidgetModel(
            DJISDKModel.getInstance(),
            ObservableInMemoryKeyedStore.getInstance(), RTKCenter.getInstance()
        )
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_fpv_top_bar_widget_gps_signal, this)
    }

    init {
        setOnClickListener {
            if (!popover.isShowing()) {
                popover.show()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            gpsSignalWidgetModel.setup()
            rtkEnabledWidgetModel.setup()
        }
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            gpsSignalWidgetModel.cleanup()
            rtkEnabledWidgetModel.cleanup()
        }
        super.onDetachedFromWindow()
    }

    override fun reactToModelChanges() {

        addReaction(gpsSignalWidgetModel.rtkOverview.observeOn(SchedulerProvider.ui()).subscribe {
            rtkOverView = it
            updateRtkIcon(it)
        })

        addReaction(gpsSignalWidgetModel.gpsSatelliteCount.observeOn(SchedulerProvider.ui()).subscribe {
            if (!rtkOverView.rtkHealthy) {
                LogUtils.d(logTag, "rtk is  not healthy,use gpsSatelliteCount")
                tvSatelliteCount.text = it.toString()
            }
        })

        addReaction(gpsSignalWidgetModel.rtkSatelliteCount.observeOn(SchedulerProvider.ui()).subscribe {
            if (rtkOverView.rtkHealthy) {
                LogUtils.d(logTag, "rtk is healthy,use rtkSatelliteCount")
                tvSatelliteCount.text = it.toString()
            }
        })
        addReaction(gpsSignalWidgetModel.gpsSignalLevel.observeOn(SchedulerProvider.ui()).subscribe {
            ivSatelliteIcon.setColorFilter(getTintColor(it))
            tvSatelliteCount.setTextColor(getTintColor(it))
        })
    }

    private fun updateRtkIcon(overview: GpsSignalWidgetModel.RtkOverview) {
        ivRtkIcon.visibility = if (overview.connected) VISIBLE else GONE

        if (overview.enabled) {
            when {
                overview.rtkKeepingStatus -> {
                    ivRtkIcon.setImageResource(R.drawable.uxsdk_ic_fpv_topbar_rtk_caution)
                }
                overview.rtkHealthy -> {
                    ivRtkIcon.setImageResource(R.drawable.uxsdk_ic_fpv_topbar_rtk_normal)
                }
                else -> {
                    ivRtkIcon.setImageResource(R.drawable.uxsdk_ic_fpv_topbar_rtk_danger)
                }
            }
        } else {
            ivRtkIcon.setImageResource(R.drawable.uxsdk_ic_fpv_topbar_rtk_no_open)
        }
    }

    private fun getTintColor(level: GpsSignalWidgetModel.SignalLevel): Int {
        return when (level) {
            GpsSignalWidgetModel.SignalLevel.LEVEL_1 ->
                resources.getColor(R.color.uxsdk_tips_danger_in_dark)
            GpsSignalWidgetModel.SignalLevel.LEVEL_2 ->
                resources.getColor(R.color.uxsdk_tips_caution_in_dark)
            GpsSignalWidgetModel.SignalLevel.LEVEL_3 ->
                resources.getColor(R.color.uxsdk_tips_normal_in_dark)
        }
    }
}