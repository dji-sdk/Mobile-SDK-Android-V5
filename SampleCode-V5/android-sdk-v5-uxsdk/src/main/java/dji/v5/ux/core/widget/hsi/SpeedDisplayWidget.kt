package dji.v5.ux.core.widget.hsi

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import dji.sdk.keyvalue.value.flightcontroller.WindDirection
import dji.sdk.keyvalue.value.flightcontroller.WindWarning
import dji.v5.utils.common.LogUtils
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.UnitUtils
import kotlinx.android.synthetic.main.uxsdk_liveview_pfd_speed_display_widget.view.*
import java.util.*

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/11/25
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class SpeedDisplayWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayoutWidget<SpeedDisplayWidget.ModelState>(context, attrs, defStyleAttr) {
    private val tag = LogUtils.getTag("SpeedDisplayWidget")
    private val widgetModel by lazy {
        SpeedDisplayModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance())
    }

    private var mAnimation: Animation? = null

    private var mIsAnimating = false

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {

        loadLayout()

        mAnimation = AlphaAnimation(1f, 0f)
        mAnimation?.apply {
            repeatMode = Animation.REVERSE
            repeatCount = -1
            interpolator = AccelerateInterpolator()
            duration = 1000
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {
                    mIsAnimating = true
                }

                override fun onAnimationEnd(animation: Animation) {
                    mIsAnimating = false
                }

                override fun onAnimationRepeat(animation: Animation) {
//                    LogUtils.d(tag,"TODO Method not implemented yet")
                }
            })
        }
    }

    open fun loadLayout() {
        View.inflate(context, R.layout.uxsdk_liveview_pfd_speed_display_widget, this)
    }

    override fun reactToModelChanges() {
        addDisposable(widgetModel.windSpeedProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {
            updateWindStatus(it.toFloat() / 10, widgetModel.windDirectionProcessor.value, widgetModel.windWarningProcessor.value, getAircraftDegree())
        })
        addDisposable(widgetModel.windDirectionProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {
            updateWindStatus(widgetModel.windSpeedProcessor.value.toFloat() / 10, it, widgetModel.windWarningProcessor.value, getAircraftDegree())
        })
        addDisposable(widgetModel.windWarningProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {
            updateWindStatus(widgetModel.windSpeedProcessor.value.toFloat() / 10, widgetModel.windDirectionProcessor.value, it, getAircraftDegree())
        })
        addDisposable(widgetModel.aircraftAttitudeProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {
            updateWindStatus(widgetModel.windSpeedProcessor.value.toFloat() / 10, widgetModel.windDirectionProcessor.value, widgetModel.windWarningProcessor.value, getAircraftDegree())
        })
    }

    private fun getAircraftDegree(): Float {
        return widgetModel.aircraftAttitudeProcessor.value.yaw.toFloat() + if (widgetModel.aircraftAttitudeProcessor.value.yaw.toFloat() < 0) 359F else 0F
    }

    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            pfd_speed_dash_board.setModel(widgetModel)
            widgetModel.setup()
        }
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
        super.onDetachedFromWindow()
    }

    private fun updateWindStatus(windSpeed: Float, windDirection: WindDirection, windWarning: WindWarning, aircraftDegree: Float) {
        val value: Float = UnitUtils.transFormSpeedIntoDifferentUnit(windSpeed)
        val textStr = String.format(Locale.ENGLISH, "WS %04.1f %s", value, getWindDirectionText(windDirection, aircraftDegree))
        if (textStr != pfd_ws_value.text.toString()) {
            pfd_ws_value.text = textStr
        }

        when (windWarning) {
            WindWarning.LEVEL_1 -> pfd_ws_value.setTextColor(resources.getColor(R.color.uxsdk_pfd_avoidance_color))
            WindWarning.LEVEL_2 -> pfd_ws_value.setTextColor(resources.getColor(R.color.uxsdk_pfd_barrier_color))
            else -> pfd_ws_value.setTextColor(resources.getColor(R.color.uxsdk_pfd_main_color))
        }

        val shouldBlink = windWarning === WindWarning.LEVEL_2
        // 红色需要持续闪烁
        if (shouldBlink && !mIsAnimating) {
            pfd_ws_value.startAnimation(mAnimation)
        } else if (!shouldBlink && mIsAnimating) {
            pfd_ws_value.clearAnimation()
        }
    }

    private fun getWindDirectionText(windDirection: WindDirection, aircraftDegree: Float): String? {
        if (windDirection === WindDirection.WINDLESS) {
            return " "
        }
        var toAircraft: WindDirection? = null
        var delta = getWindDegree(windDirection) - aircraftDegree
        delta += if (delta >= 0) 0F else 360F
        var start = 22.5F
        val offset = 45F
        for (i in 2..8) {
            if (delta >= start && delta < start + offset) {
                toAircraft = WindDirection.find(i)
                break
            }
            start += offset
        }
        if (toAircraft == null) {
            toAircraft = WindDirection.NORTH
        }
        return getWindDirectionText(toAircraft)
    }

    private fun getWindDirectionText(windDirection: WindDirection?): String {
        return when (windDirection) {
            WindDirection.EAST -> "←"
            WindDirection.WEST -> "→"
            WindDirection.NORTH -> "↓"
            WindDirection.SOUTH -> "↑"
            WindDirection.NORTH_EAST -> "↙"
            WindDirection.NORTH_WEST -> "↘"
            WindDirection.SOUTH_EAST -> "↖"
            WindDirection.SOUTH_WEST -> "↗"
            WindDirection.WINDLESS -> " "
            else -> " "
        }
    }

    private fun getWindDegree(fcWindDirectionStatus: WindDirection?): Int {
        return when (fcWindDirectionStatus) {
            WindDirection.EAST -> 90
            WindDirection.WEST -> 270
            WindDirection.SOUTH -> 180
            WindDirection.NORTH_EAST -> 45
            WindDirection.NORTH_WEST -> 315
            WindDirection.SOUTH_EAST -> 135
            WindDirection.SOUTH_WEST -> 225
            WindDirection.NORTH -> 0
            else -> 0
        }
    }


    sealed class ModelState
}