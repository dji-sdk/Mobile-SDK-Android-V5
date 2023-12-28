package dji.v5.ux.core.widget.hsi

import android.content.Context

import dji.v5.ux.core.ui.hsi.FlashTimer.removeListener

import dji.v5.ux.core.ui.hsi.FlashTimer.addListener
import kotlin.jvm.JvmOverloads
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import android.view.ViewDebug.ExportedProperty
import android.widget.TextView
import dji.v5.ux.core.ui.hsi.dashboard.SpeedDashBoard
import io.reactivex.rxjava3.disposables.CompositeDisposable
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.R
import dji.v5.ux.core.ui.hsi.FlashTimer
import io.reactivex.rxjava3.core.Flowable
import dji.v5.common.utils.UnitUtils
import android.graphics.Color
import android.util.AttributeSet
import dji.sdk.keyvalue.value.common.Attitude
import dji.sdk.keyvalue.value.flightcontroller.WindDirection
import dji.sdk.keyvalue.value.flightcontroller.WindWarning
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import java.util.*

open class SpeedDisplayWidget @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ConstraintLayoutWidget<Boolean?>(context, attrs, defStyleAttr) {
    @ExportedProperty(category = "dji", formatToHexString = true)
    private val mWindTextColor: Int
    private var mTvWsValue: TextView? = null
    private var mIsAnimating = false
    var mSpeedDashBoard: SpeedDashBoard? = null
    private val mCompositeDisposable = CompositeDisposable()
    private var mListener: FlashTimer.Listener? = null
    private val widgetModel = SpeedDisplayModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance())
    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        loadLayout(context)
        mSpeedDashBoard = findViewById(R.id.pfd_speed_dash_board)
        mTvWsValue = findViewById(R.id.pfd_ws_value)
    }

    protected open fun loadLayout(context: Context) {
        inflate(context, R.layout.uxsdk_liveview_pfd_speed_display_widget, this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            mSpeedDashBoard?.setModel(widgetModel)
            widgetModel.setup()
        }
        mListener = FlashTimer.Listener { show: Boolean ->
            val visible: Int = if (mIsAnimating && !show) {
                INVISIBLE
            } else {
                VISIBLE
            }
            if (mTvWsValue?.visibility != visible) {
                postTvWsVisibility(visible)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mCompositeDisposable.dispose()
        // 当 View 在闪烁时，需要移除掉回调
        removeListener(mListener)
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
    }

    override fun reactToModelChanges() {
        mCompositeDisposable.add(
            Flowable.combineLatest(
                widgetModel.windSpeedProcessor.toFlowable(),
                widgetModel.windDirectionProcessor.toFlowable(),
                widgetModel.windWarningProcessor.toFlowable(),
                widgetModel.aircraftAttitudeProcessor.toFlowable(),
                { windSpeed: Int, fcWindDirectionStatus: WindDirection, fcWindWarning: WindWarning, attitude: Attitude ->
                    val yaw = attitude.yaw.toFloat()
                    val aircraftDegree: Float = yaw + if (yaw < 0) 359f else 0f
                    AndroidSchedulers.mainThread().scheduleDirect {
                        updateWindStatus(windSpeed.toFloat() / 10, fcWindDirectionStatus, fcWindWarning, aircraftDegree)
                    }
                    true
                }
            ).subscribe()
        )
    }

    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    private fun postTvWsVisibility(visible: Int) {
        mTvWsValue?.post { mTvWsValue?.visibility = visible }
    }

    private fun updateWindStatus(windSpeed: Float, fcWindDirectionStatus: WindDirection, fcWindWarning: WindWarning, aircraftDegree: Float) {
        val value = UnitUtils.transFormSpeedIntoDifferentUnit(windSpeed)
        val textStr = String.format(Locale.ENGLISH, "WS %04.1f %s", value, getWindDirectionText(fcWindDirectionStatus, aircraftDegree))
        if (textStr != mTvWsValue?.text.toString()) {
            mTvWsValue!!.text = textStr
        }
        if (fcWindWarning == WindWarning.LEVEL_2) {
            mTvWsValue!!.setTextColor(resources.getColor(R.color.uxsdk_pfd_barrier_color))
        } else if (fcWindWarning == WindWarning.LEVEL_1) {
            mTvWsValue?.setTextColor(resources.getColor(R.color.uxsdk_pfd_avoidance_color))
        } else {
            mTvWsValue?.setTextColor(mWindTextColor)
        }
        val shouldBlink = fcWindWarning == WindWarning.LEVEL_2
        // 红色需要持续闪烁
        if (shouldBlink && !mIsAnimating) {
            mIsAnimating = true
            addListener(mListener)
        } else if (!shouldBlink && mIsAnimating) {
            mIsAnimating = false
            removeListener(mListener)
            // 使用 post，避免消息队列还存在隐藏的消息
            postTvWsVisibility(VISIBLE)
        }
    }

    private fun getWindDirectionText(fcWindDirectionStatus: WindDirection, aircraftDegree: Float): String {
        if (fcWindDirectionStatus == WindDirection.WINDLESS) {
            return " "
        }
        var toAircraft: WindDirection? = null
        var delta = getWindDegree(fcWindDirectionStatus) - aircraftDegree
        delta += if (delta >= 0) 0f else 360.toFloat()
        var start = 22.5f
        val offset = 45f
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

    private fun getWindDirectionText(windDirection: WindDirection): String {
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
        return if (fcWindDirectionStatus == null) {
            0
        } else when (fcWindDirectionStatus) {
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

    companion object {
        private val TAG = SpeedDisplayWidget::class.java.simpleName
    }

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SpeedDisplayWidget)
        mWindTextColor = typedArray.getColor(R.styleable.SpeedDisplayWidget_android_textColor, Color.WHITE)
        typedArray.recycle()
    }
}