/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dji.v5.ux.core.widget.remainingflighttime

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.use
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.BiFunction
import io.reactivex.rxjava3.functions.Consumer
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.widget.FrameLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.getColorAndUse
import dji.v5.ux.core.extension.getString
import dji.v5.ux.core.util.RxUtil
import dji.v5.ux.core.widget.remainingflighttime.RemainingFlightTimeWidget.ModelState
import dji.v5.ux.core.widget.remainingflighttime.RemainingFlightTimeWidget.ModelState.*
import dji.v5.ux.core.widget.remainingflighttime.RemainingFlightTimeWidgetModel.RemainingFlightTimeData

/**
 * Remaining Flight Time Widget
 *
 * Widget shows the remaining flight time graphically. Data displayed includes
 *
 * 1. Battery charge remaining in percentage
 * 2. Battery required for the aircraft to return home
 * 3. Battery required for the aircraft to land
 * 4. Remaining flight time
 * 5. Serious low battery threshold level
 * 6. Low battery threshold level
 */
open class RemainingFlightTimeWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayoutWidget<ModelState>(
        context,
        attrs,
        defStyleAttr
) {

    private val widgetModel by lazy {
        RemainingFlightTimeWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
    }
    private val batteryRequiredToLandPaint: Paint = Paint()
    private val batteryChargeRemainingPaint: Paint = Paint()
    private val batteryRequiredToGoHomePaint: Paint = Paint()
    private val flightTimeRoundedBackgroundPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val flightTimeTextPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lowBatteryThresholdDotPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val seriousLowBatteryThresholdDotPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val homePointBackgroundPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val homeLetterPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var flightTimeText: String = DISCONNECTED_STRING
    private val flightTimeTextBounds: Rect = Rect()
    private val homeLetterBounds: Rect = Rect()
    private var batteryRequiredToLandPercentage = 0f
    private var remainingBatteryChargePercentage = 0f
    private var batteryRequiredToGoHomePercentage = 0f
    private var seriousLowBatteryThresholdPercentage = 0f
    private var lowBatteryThresholdPercentage = 0f
    private var viewHeight = 0f
    private var usableViewWidth = 0f
    private var homeLetterWidth = 0f

    //region customizable fields

    /**
     * Color representing flight time available in battery
     */
    var batteryChargeRemainingColor: Int
        get() = batteryChargeRemainingPaint.color
        set(color) {
            batteryChargeRemainingPaint.color = color
            invalidate()
        }

    /**
     * Color representing flight time available in battery
     * for the aircraft to return home
     */
    var batteryToReturnHomeColor: Int
        get() = batteryRequiredToGoHomePaint.color
        set(color) {
            batteryRequiredToGoHomePaint.color = color
            invalidate()
        }

    /**
     * Color representing flight time available in battery
     * for the aircraft to land
     */
    var batteryRequiredToLandColor: Int
        get() = batteryRequiredToLandPaint.color
        set(color) {
            batteryRequiredToLandPaint.color = color
            invalidate()
        }

    /**
     * Color for the serious low battery level threshold indicator dot
     */
    var seriousLowBatteryThresholdDotColor: Int
        get() = seriousLowBatteryThresholdDotPaint.color
        set(color) {
            seriousLowBatteryThresholdDotPaint.color = color
            invalidate()
        }

    /**
     * Color for the low battery level threshold indicator dot
     */
    var lowBatteryThresholdDotColor: Int
        get() = lowBatteryThresholdDotPaint.color
        set(color) {
            lowBatteryThresholdDotPaint.color = color
            invalidate()
        }

    /**
     * Color for the flight time text
     */
    var flightTimeTextColor: Int
        get() = flightTimeTextPaint.color
        set(color) {
            flightTimeTextPaint.color = color
            invalidate()
        }

    /**
     * Background color for flight time text
     */
    var flightTimeBackgroundColor: Int
        get() = flightTimeRoundedBackgroundPaint.color
        set(color) {
            flightTimeRoundedBackgroundPaint.color = color
            invalidate()
        }

    /**
     * Color for home letter
     */
    var homeLetterColor: Int
        get() = homeLetterPaint.color
        set(color) {
            homeLetterPaint.color = color
            invalidate()
        }

    /**
     * Background color for home letter
     */
    var homeLetterBackgroundColor: Int
        get() = homePointBackgroundPaint.color
        set(color) {
            homePointBackgroundPaint.color = color
            invalidate()
        }

    //endregion

    //region Lifecycle

    init {
        setWillNotDraw(false)
        initDefaults()
        if (attrs != null) {
            initAttributes(context, attrs)
        }
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        // No Layout to init
    }

    override fun onDraw(canvas: Canvas) {
        val homeLetter = getString(R.string.uxsdk_home_location_letter)
        if (viewHeight == 0f) { // Initialize stuff based on View's dimension
            viewHeight = height.toFloat()
            // Not getting the whole width so it would not touch the edge of the screen
            // This is to leave some room in the end for the rounded white background of FlightTime
            usableViewWidth = width.toFloat()
            batteryRequiredToLandPaint.strokeWidth = viewHeight / 6f
            batteryChargeRemainingPaint.strokeWidth = viewHeight / 6f
            batteryRequiredToGoHomePaint.strokeWidth = viewHeight / 6f
            flightTimeRoundedBackgroundPaint.strokeWidth = viewHeight / 1.1f
            seriousLowBatteryThresholdDotPaint.strokeWidth = viewHeight / 2.4f
            lowBatteryThresholdDotPaint.strokeWidth = viewHeight / 2.4f
            homePointBackgroundPaint.strokeWidth = viewHeight / 1.6f
            flightTimeTextPaint.textSize = viewHeight / 1.5f
            homeLetterPaint.textSize = viewHeight / 2.5f
            homeLetterWidth = homeLetterPaint.measureText(homeLetter)
            homeLetterPaint.getTextBounds(homeLetter, 0, 1, homeLetterBounds)
            flightTimeTextPaint.getTextBounds(flightTimeText, 0, 1, flightTimeTextBounds)
        } else {
            val textWidth = flightTimeTextPaint.measureText(flightTimeText)
            val roundedBgWidth = textWidth * 1.55f
            // Draw remaining flight time based on battery charge
            canvas.drawLine(0f,
                    viewHeight / 2f,
                    usableViewWidth * remainingBatteryChargePercentage / 100f - roundedBgWidth / 2,
                    viewHeight / 2f,
                    batteryChargeRemainingPaint)
            // Draw flight time to go home
            if (batteryRequiredToGoHomePercentage <= remainingBatteryChargePercentage) {
                canvas.drawLine(0f,
                        viewHeight / 2f,
                        usableViewWidth * batteryRequiredToGoHomePercentage / 100f - roundedBgWidth / 2,
                        viewHeight / 2f, batteryRequiredToGoHomePaint)
            } else {
                canvas.drawLine(0f,
                        viewHeight / 2f,
                        usableViewWidth * remainingBatteryChargePercentage / 100f - roundedBgWidth / 2,
                        viewHeight / 2f, batteryRequiredToGoHomePaint)
            }

            // Draw flight time to land immediately
            if (batteryRequiredToLandPercentage <= remainingBatteryChargePercentage) {
                canvas.drawLine(0f,
                        viewHeight / 2f,
                        usableViewWidth * batteryRequiredToLandPercentage / 100f - roundedBgWidth / 2,
                        viewHeight / 2f, batteryRequiredToLandPaint)
            } else {
                canvas.drawLine(0f,
                        viewHeight / 2f,
                        usableViewWidth * remainingBatteryChargePercentage / 100f - roundedBgWidth / 2,
                        viewHeight / 2f, batteryRequiredToLandPaint)
            }
            // Draw serious low battery level indicator
            if (seriousLowBatteryThresholdPercentage <= remainingBatteryChargePercentage) {
                canvas.drawPoint(usableViewWidth * seriousLowBatteryThresholdPercentage / 100f - roundedBgWidth / 2,
                        viewHeight / 2f, seriousLowBatteryThresholdDotPaint)
            }
            // Draw low battery level indicator
            if (lowBatteryThresholdPercentage <= remainingBatteryChargePercentage) {
                canvas.drawPoint(usableViewWidth * lowBatteryThresholdPercentage / 100f - roundedBgWidth / 2,
                        viewHeight / 2f, lowBatteryThresholdDotPaint)
            }

            // Draw battery left for time to home point indicator
            if (batteryRequiredToGoHomePercentage <= remainingBatteryChargePercentage) {
                // Draw H letter background
                canvas.drawPoint(usableViewWidth * batteryRequiredToGoHomePercentage / 100f - roundedBgWidth / 2,
                        viewHeight / 2f, homePointBackgroundPaint)
                // Draw H letter
                canvas.drawText(homeLetter,
                        usableViewWidth * batteryRequiredToGoHomePercentage / 100f - homeLetterWidth / 2f - roundedBgWidth / 2,
                        viewHeight / 2f + homeLetterBounds.height() / 2f,
                        homeLetterPaint)
            } else {
                // Draw H letter background

                canvas.drawPoint(usableViewWidth * remainingBatteryChargePercentage / 100f - roundedBgWidth / 2,
                        viewHeight / 2f, homePointBackgroundPaint)
                // Draw H letter
                canvas.drawText(homeLetter,
                        usableViewWidth * remainingBatteryChargePercentage / 100f - homeLetterWidth / 2f - roundedBgWidth / 2,
                        viewHeight / 2f + homeLetterBounds.height() / 2f,
                        homeLetterPaint)
            }
            drawFlightText(canvas, roundedBgWidth, textWidth)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
        }
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
        super.onDetachedFromWindow()
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.productConnection
                .observeOn(SchedulerProvider.ui())
                .subscribe { isProductConnected: Boolean -> updateVisibility(isProductConnected) })
        addReaction(reactToRemainingFlightTimeChange())
        addReaction(widgetModel.isAircraftFlying
                .observeOn(SchedulerProvider.io())
                .subscribe {
                    widgetStateDataProcessor.onNext(AircraftFlyingUpdated(it))
                })
        addReaction(widgetModel.remainingFlightTimeData
                .observeOn(SchedulerProvider.io())
                .subscribe {
                    widgetStateDataProcessor.onNext(
                            FlightTimeDataUpdated(it))
                })
    }

    override fun getIdealDimensionRatioString(): String {
        return getString(R.string.uxsdk_widget_remaining_flight_time_ratio)
    }

    //endregion

    //region private methods

    private fun updateVisibility(isProductConnected: Boolean) {
        widgetStateDataProcessor.onNext(ProductConnected(isProductConnected))
        visibility = if (isProductConnected) View.VISIBLE else View.GONE
    }

    private fun drawFlightText(canvas: Canvas, roundedBgWidth: Float, textWidth: Float) {
        flightTimeRoundedBackgroundPaint.strokeCap = Paint.Cap.ROUND
        val start = if (usableViewWidth * remainingBatteryChargePercentage / 100f - roundedBgWidth > 0) usableViewWidth * remainingBatteryChargePercentage / 100f - roundedBgWidth / 2 - textWidth / 2.5f else flightTimeRoundedBackgroundPaint.strokeWidth / 2.0f
        val end = if (usableViewWidth * remainingBatteryChargePercentage / 100f - roundedBgWidth > 0) usableViewWidth * remainingBatteryChargePercentage / 100f - roundedBgWidth / 2 + textWidth / 2.5f else roundedBgWidth / 2 + textWidth / 2.5f
        val textStart = if (usableViewWidth * remainingBatteryChargePercentage / 100f - roundedBgWidth > 0) usableViewWidth * remainingBatteryChargePercentage / 100f - roundedBgWidth / 2 - textWidth / 2 else flightTimeRoundedBackgroundPaint.strokeWidth / 2.5f
        canvas.drawPoint(start,
                viewHeight / 2f, flightTimeRoundedBackgroundPaint)
        canvas.drawPoint(end, viewHeight / 2f, flightTimeRoundedBackgroundPaint)
        canvas.drawLine(start, viewHeight / 2f, end, viewHeight / 2f,
                flightTimeRoundedBackgroundPaint)
        canvas.drawText(flightTimeText,
                textStart,
                viewHeight / 2f + if (flightTimeTextBounds.height() > homeLetterBounds.height()) flightTimeTextBounds.height() / 2.5f else homeLetterBounds.height() / 1.5f,
                flightTimeTextPaint)
    }


    private fun reactToRemainingFlightTimeChange(): Disposable {
        return Flowable.combineLatest(widgetModel.isAircraftFlying,
                widgetModel.remainingFlightTimeData,
                BiFunction { first: Boolean, second: RemainingFlightTimeData -> Pair(first, second) })
                .observeOn(SchedulerProvider.ui())
                .subscribe(Consumer { values: Pair<Boolean, RemainingFlightTimeData> -> onRemainingFlightTimeChange(values.first, values.second) },
                        RxUtil.logErrorConsumer(TAG, "react to flight time update: "))
    }

    private fun onRemainingFlightTimeChange(isAircraftFlying: Boolean,
                                            remainingFlightTimeData: RemainingFlightTimeData) {
        flightTimeText = if (isAircraftFlying) {
            getFormattedString(remainingFlightTimeData.flightTime)
        } else {
            DISCONNECTED_STRING
        }

        batteryRequiredToLandPercentage = remainingFlightTimeData.batteryNeededToLand.toFloat()
        batteryRequiredToGoHomePercentage = remainingFlightTimeData.batteryNeededToGoHome.toFloat()
        remainingBatteryChargePercentage = remainingFlightTimeData.remainingCharge.toFloat()
        seriousLowBatteryThresholdPercentage = remainingFlightTimeData.seriousLowBatteryThreshold.toFloat()
        lowBatteryThresholdPercentage = remainingFlightTimeData.lowBatteryThreshold.toFloat()
        invalidate()
    }

    private fun getFormattedString(flightTime: Int): String {
        return if (flightTime / MINUTE_CONVERSION_CONSTANT > 59) {
            String.format(HOUR_FLIGHT_TIME_FORMAT_STRING,
                    flightTime / HOUR_CONVERSION_CONSTANT,
                    flightTime / HOUR_CONVERSION_CONSTANT % MINUTE_CONVERSION_CONSTANT,
                    flightTime % HOUR_CONVERSION_CONSTANT % MINUTE_CONVERSION_CONSTANT)
        } else {
            String.format(MINUTE_FLIGHT_TIME_FORMAT_STRING,
                    flightTime / MINUTE_CONVERSION_CONSTANT,
                    flightTime % MINUTE_CONVERSION_CONSTANT)
        }
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.RemainingFlightTimeWidget).use { typedArray ->
            typedArray.getColorAndUse(R.styleable.RemainingFlightTimeWidget_uxsdk_homeLetterBackgroundColor) {
                homeLetterBackgroundColor = it
            }
            typedArray.getColorAndUse(R.styleable.RemainingFlightTimeWidget_uxsdk_homeLetterColor) {
                homeLetterColor = it
            }
            typedArray.getColorAndUse(R.styleable.RemainingFlightTimeWidget_uxsdk_flightTimeBackgroundColor) {
                flightTimeBackgroundColor = it
            }
            typedArray.getColorAndUse(R.styleable.RemainingFlightTimeWidget_uxsdk_flightTimeTextColor) {
                flightTimeTextColor = it
            }
            typedArray.getColorAndUse(R.styleable.RemainingFlightTimeWidget_uxsdk_lowBatteryThresholdColor) {
                lowBatteryThresholdDotColor = it
            }
            typedArray.getColorAndUse(R.styleable.RemainingFlightTimeWidget_uxsdk_seriousLowBatteryThresholdColor) {
                seriousLowBatteryThresholdDotColor = it
            }
            typedArray.getColorAndUse(R.styleable.RemainingFlightTimeWidget_uxsdk_batteryRequiredToLandColor) {
                batteryRequiredToLandColor = it
            }
            typedArray.getColorAndUse(R.styleable.RemainingFlightTimeWidget_uxsdk_batteryRequiredToGoHomeColor) {
                batteryToReturnHomeColor = it
            }
            typedArray.getColorAndUse(R.styleable.RemainingFlightTimeWidget_uxsdk_batteryChargeRemainingColor) {
                batteryChargeRemainingColor = it
            }
        }
    }

    private fun initDefaults() {
        batteryRequiredToLandPaint.color = Color.RED
        batteryRequiredToLandPaint.style = Paint.Style.STROKE
        batteryRequiredToLandPaint.strokeCap = Paint.Cap.SQUARE

        batteryChargeRemainingPaint.color = Color.GREEN
        batteryChargeRemainingPaint.style = Paint.Style.STROKE
        batteryChargeRemainingPaint.strokeCap = Paint.Cap.SQUARE

        batteryRequiredToGoHomePaint.color = Color.YELLOW
        batteryRequiredToGoHomePaint.style = Paint.Style.STROKE
        batteryRequiredToGoHomePaint.strokeCap = Paint.Cap.SQUARE

        flightTimeRoundedBackgroundPaint.color = Color.WHITE
        flightTimeRoundedBackgroundPaint.style = Paint.Style.STROKE
        flightTimeRoundedBackgroundPaint.strokeCap = Paint.Cap.ROUND

        flightTimeTextPaint.color = Color.BLACK
        flightTimeTextPaint.style = Paint.Style.FILL

        seriousLowBatteryThresholdDotPaint.color = Color.WHITE
        seriousLowBatteryThresholdDotPaint.style = Paint.Style.STROKE
        seriousLowBatteryThresholdDotPaint.strokeCap = Paint.Cap.ROUND

        lowBatteryThresholdDotPaint.color = Color.WHITE
        lowBatteryThresholdDotPaint.style = Paint.Style.STROKE
        lowBatteryThresholdDotPaint.strokeCap = Paint.Cap.ROUND

        homeLetterPaint.color = Color.BLACK
        homeLetterPaint.style = Paint.Style.FILL
        homeLetterPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        homePointBackgroundPaint.color = Color.YELLOW
        homePointBackgroundPaint.style = Paint.Style.STROKE
        homePointBackgroundPaint.strokeCap = Paint.Cap.ROUND
    }

    //endregion

    //region Hooks
    /**
     * Get the [ModelState] updates
     */
    @SuppressWarnings
    override fun getWidgetStateUpdate(): Flowable<ModelState> {
        return super.getWidgetStateUpdate()
    }

    /**
     * Class defines the widget state updates
     */
    sealed class ModelState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : ModelState()

        /**
         * Aircraft flying status change update
         */
        data class AircraftFlyingUpdated(val isAircraftFlying: Boolean) : ModelState()

        /**
         * Remaining flight time data update
         */
        data class FlightTimeDataUpdated(val remainingFlightTimeData: RemainingFlightTimeData) : ModelState()

    }
    //endregion

    companion object {
        private const val TAG = "FlightTimeWidget"
        private const val DISCONNECTED_STRING = "--:--"
        private const val MINUTE_FLIGHT_TIME_FORMAT_STRING = "%02d:%02d"
        private const val HOUR_FLIGHT_TIME_FORMAT_STRING = "%01d:%02d:%02d"
        private const val MINUTE_CONVERSION_CONSTANT = 60
        private const val HOUR_CONVERSION_CONSTANT = 3600
    }
}