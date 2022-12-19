package dji.v5.ux.core.widget.gpssignal

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Switch
import dji.v5.manager.aircraft.rtk.RTKCenter
import dji.v5.utils.common.LogUtils
import dji.v5.ux.R
import dji.v5.ux.accessory.RTKEnabledWidgetModel

import dji.v5.ux.accessory.item.TitleValueCell
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.widget.FrameLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.getString
import dji.v5.ux.core.extension.showLongToast
import dji.v5.utils.common.AndUtil
import dji.v5.ux.core.util.RxUtil

class GpsSignalPopoverView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : FrameLayoutWidget<Boolean>(context, attrs, defStyleAttr), CompoundButton.OnCheckedChangeListener {

    private var rtkTitleWrapper: FrameLayout = findViewById(R.id.rtk_title_wrapper)
    private var rtkContentWrapper: LinearLayout = findViewById(R.id.rtk_content_wrapper)
    private var gpsTitleWrapper: FrameLayout = findViewById(R.id.gps_title_wrapper)
    private var gpsContentWrapper: LinearLayout = findViewById(R.id.gps_content_wrapper)

    private var tvGpsSignal: TitleValueCell = findViewById(R.id.tv_gps_signal)
    private var tvGpsSatelliteCount: TitleValueCell = findViewById(R.id.tv_gps_satellite_count)

    private var rtkEnabledSwitch: Switch = findViewById(R.id.switch_rtk_enable)
    private var tvRtkState: TitleValueCell = findViewById(R.id.tv_rtk_state)
    private var tvRtkSatelliteCount: TitleValueCell = findViewById(R.id.tv_rtk_satellite_count)
    private var tvRtkSignal: TitleValueCell = findViewById(R.id.tv_rtk_signal)
    private var maxWidth = 0
    private var rtkOverview: GpsSignalWidgetModel.RtkOverview = GpsSignalWidgetModel.RtkOverview()

    init {

        rtkEnabledSwitch.setOnCheckedChangeListener(this)

        rtkContentWrapper.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        gpsContentWrapper.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        maxWidth = rtkContentWrapper.measuredWidth.coerceAtLeast(gpsContentWrapper.measuredWidth)


    }

    private val rtkEnabledWidgetModel by lazy {
        RTKEnabledWidgetModel(
            DJISDKModel.getInstance(),
            ObservableInMemoryKeyedStore.getInstance())
    }

    private val gpsSignalWidgetModel by lazy {
        GpsSignalWidgetModel(
            DJISDKModel.getInstance(),
            ObservableInMemoryKeyedStore.getInstance(), RTKCenter.getInstance())
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


    private fun updateRtkState(overview: GpsSignalWidgetModel.RtkOverview) {
        if (overview.connected) {
            rtkTitleWrapper.visibility = VISIBLE
            rtkContentWrapper.visibility = VISIBLE
            gpsTitleWrapper.visibility = VISIBLE
            gpsContentWrapper.setBackgroundResource(R.color.uxsdk_fpv_popover_content_background_color)
            gpsContentWrapper.layoutParams.width = maxWidth
        } else {
            rtkTitleWrapper.visibility = GONE
            rtkContentWrapper.visibility = GONE
            gpsTitleWrapper.visibility = GONE
            gpsContentWrapper.setBackgroundResource(R.color.uxsdk_fpv_popover_title_background_color)
            gpsContentWrapper.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
        }
        setRTKSwitch(overview.enabled)
        if (!overview.enabled) {
            tvRtkSatelliteCount.value = "- -"
            tvRtkSignal.value = "- -"

            tvRtkSignal.setValueTextColor(AndUtil.getResColor(R.color.uxsdk_white))
            tvRtkSatelliteCount.setValueTextColor(AndUtil.getResColor(R.color.uxsdk_white))
        }

        if (overview.rtkKeepingStatus) {
            //黄
            tvRtkState.value = AndUtil.getResString(R.string.uxsdk_rtk_keep_status_mode)
            tvRtkState.setValueTextColor(AndUtil.getResColor(R.color.uxsdk_zone_warning_enhanced))
        } else {

            when (overview.rtkState) {
                GpsSignalWidgetModel.RtkState.NOT_OPEN -> {
                    //黄
                    tvRtkState.value =
                        AndUtil.getResString(R.string.uxsdk_checklist_manual_rtk_not_open)
                    tvRtkState.setValueTextColor(AndUtil.getResColor(R.color.uxsdk_zone_warning_enhanced))
                }

                GpsSignalWidgetModel.RtkState.NOT_CONNECT -> {
                    //黄
                    tvRtkState.value =
                        AndUtil.getResString(R.string.uxsdk_setting_menu_rtk_state_disconnect)
                    tvRtkState.setValueTextColor(AndUtil.getResColor(R.color.uxsdk_red_in_dark))
                }

                GpsSignalWidgetModel.RtkState.CONVERGING -> {
                    //黄
                    tvRtkState.value =
                        AndUtil.getResString(R.string.uxsdk_checklist_rtk_status_converging)
                    tvRtkState.setValueTextColor(AndUtil.getResColor(R.color.uxsdk_zone_warning_enhanced))
                }

                GpsSignalWidgetModel.RtkState.CONNECTED -> {
                    //绿
                    tvRtkState.value =
                        AndUtil.getResString(R.string.uxsdk_checklist_rtk_status_connected)
                    tvRtkState.setValueTextColor(AndUtil.getResColor(R.color.uxsdk_green_in_dark))
                }

                GpsSignalWidgetModel.RtkState.ERROR -> {
                    //红
                    tvRtkState.value = AndUtil.getResString(R.string.uxsdk_abnormal)
                    tvRtkState.setValueTextColor(AndUtil.getResColor(R.color.uxsdk_red_in_dark))
                }
            }
        }
    }

    private fun updateRtKSatelliteCount(count: Int) {
        rtkOverview.let {
            if (it.enabled) {
                LogUtils.i(tag,"updateRtKSatelliteCount,it.enabled=${it.enabled},count=$count")
                tvRtkSatelliteCount.value = count.toString()
            }
        }

    }

    private fun setRTKSwitch(isChecked: Boolean) {
        rtkEnabledSwitch.setOnCheckedChangeListener(null)
        rtkEnabledSwitch.isChecked = isChecked
        rtkEnabledSwitch.setOnCheckedChangeListener(this)
    }

    private fun updateGpsSatelliteCount(count: Int) {
        tvGpsSatelliteCount.value = count.toString()
    }

    private fun updateSignalLevel(level: GpsSignalWidgetModel.SignalLevel) {
        tvGpsSignal.setValueTextColor(AndUtil.getResColor(mapSignalLevelToColorRes(level)))
        tvGpsSignal.value = AndUtil.getResString(mapSignalLevelToStringRes(level))
        tvGpsSatelliteCount.setValueTextColor(AndUtil.getResColor(mapSignalLevelToColorRes(level)))

        if (rtkOverview.enabled) {
            tvRtkSignal.setValueTextColor(AndUtil.getResColor(mapSignalLevelToColorRes(level)))
            tvRtkSignal.value = AndUtil.getResString(mapSignalLevelToStringRes(level))
            tvRtkSatelliteCount.setValueTextColor(AndUtil.getResColor(mapSignalLevelToColorRes(level)))
        }
    }


    private fun mapSignalLevelToStringRes(level: GpsSignalWidgetModel.SignalLevel): Int {
        return when (level) {
            GpsSignalWidgetModel.SignalLevel.LEVEL_3 -> {
                R.string.uxsdk_fpv_top_bar_gps_signal_state_strong
            }
            GpsSignalWidgetModel.SignalLevel.LEVEL_2 -> {
                R.string.uxsdk_fpv_top_bar_gps_signal_state_normal
            }
            GpsSignalWidgetModel.SignalLevel.LEVEL_1 -> {
                R.string.uxsdk_fpv_top_bar_gps_signal_state_weak
            }
        }
    }

    private fun mapSignalLevelToColorRes(level: GpsSignalWidgetModel.SignalLevel): Int {
        return when (level) {
            GpsSignalWidgetModel.SignalLevel.LEVEL_3 -> {
                R.color.uxsdk_tips_normal_in_dark
            }
            GpsSignalWidgetModel.SignalLevel.LEVEL_2 -> {
                R.color.uxsdk_tips_caution_in_dark
            }
            GpsSignalWidgetModel.SignalLevel.LEVEL_1 -> {
                R.color.uxsdk_tips_danger_in_dark
            }
        }
    }


    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_fpv_top_bar_widget_gps_signal_popover_rtk, this)
    }

    override fun reactToModelChanges() {
        addReaction(gpsSignalWidgetModel.rtkOverview.observeOn(SchedulerProvider.ui()).subscribe {
            rtkOverview = it
            updateRtkState(it)
        })
        addReaction(gpsSignalWidgetModel.gpsSatelliteCount.observeOn(SchedulerProvider.ui()).subscribe {
            updateGpsSatelliteCount(it)
        })
        addReaction(gpsSignalWidgetModel.rtkSatelliteCount.observeOn(SchedulerProvider.ui()).subscribe {
            updateRtKSatelliteCount(it)
        })
        addReaction(gpsSignalWidgetModel.gpsSignalLevel.observeOn(SchedulerProvider.ui()).subscribe {
            updateSignalLevel(it)
        })
    }

    override fun getIdealDimensionRatioString(): String? {
        return getString(R.string.uxsdk_widget_rtk_enabled_ratio)
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        addDisposable(rtkEnabledWidgetModel.canEnableRTK.firstOrError()
            .observeOn(SchedulerProvider.ui())
            .subscribe({ canEnableRTK: Boolean ->
                if (!canEnableRTK) {
                    setRTKSwitch(!isChecked)
                    showLongToast(R.string.uxsdk_rtk_enabled_motors_running)
                } else {
                    gpsSignalWidgetModel.setRTKEnable(isChecked)
                }
            }, RxUtil.logErrorConsumer(tag, "canEnableRTK:")))
    }


}