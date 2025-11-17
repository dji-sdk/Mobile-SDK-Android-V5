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
package dji.v5.ux.core.widget.compass

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.hardware.SensorManager
import android.util.AttributeSet
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.core.content.res.use
import dji.sdk.keyvalue.value.common.Attitude
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.*
import dji.v5.ux.core.widget.compass.CompassWidget.ModelState
import dji.v5.ux.core.widget.compass.CompassWidget.ModelState.CompassStateUpdated
import dji.v5.ux.core.widget.compass.CompassWidget.ModelState.ProductConnected
import dji.v5.ux.core.widget.compass.CompassWidgetModel.*
import io.reactivex.rxjava3.core.Flowable
import kotlin.math.cos
import kotlin.math.sin

private const val TAG = "CompassWidget"
private const val MAX_DISTANCE = 400
private const val MAX_SCALE_DISTANCE = 2000
private const val MIN_SCALE = 0.6f
private const val MAX_PROGRESS = 100
private const val MIN_PROGRESS = 0
private const val FULL_TURN = 360
private const val HALF_TURN = 180
private const val QUARTER_TURN = 90

/**
 * This widget aggregates the attitude and location data of the aircraft
 * into one widget. This includes -
 * - Position of the aircraft relative to the pilot
 * - Distance of the aircraft from the pilot
 * - Heading of the aircraft relative to the pilot
 * - True north relative to the pilot and the aircraft
 * - The aircraft's last recorded home location
 * - Attitude of the aircraft
 * - Yaw of the gimbal
 */
open class CompassWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayoutWidget<ModelState>(context, attrs, defStyleAttr) {

    //region Fields
    private var halfNorthIconWidth = 0f
    private var halfAttitudeBallWidth = 0f
    private var paddingWidth = 0f
    private var paddingHeight = 0f

    private val homeImageView: ImageView = findViewById(R.id.imageview_compass_home)
    private val rcImageView: ImageView = findViewById(R.id.imageview_compass_rc)
    private val aircraftImageView: ImageView = findViewById(R.id.imageview_compass_aircraft)
    private val gimbalYawImageView: ImageView = findViewById(R.id.imageview_gimbal_heading)
    private val innerCirclesImageView: ImageView = findViewById(R.id.imageview_inner_circles)
    private val northImageView: ImageView = findViewById(R.id.imageview_north)
    private val compassBackgroundImageView: ImageView = findViewById(R.id.imageview_compass_background)
    private val aircraftAttitudeProgressBar: ProgressBar = findViewById(R.id.progressbar_compass_attitude)
    private val visualCompassView: VisualCompassView = findViewById(R.id.visual_compass_view)
    private val gimbalYawView: GimbalYawView = findViewById(R.id.gimbal_yaw_view)

    private val widgetModel: CompassWidgetModel by lazy {
        CompassWidgetModel(
            DJISDKModel.getInstance(),
            ObservableInMemoryKeyedStore.getInstance(),
            context.getSystemService(Context.SENSOR_SERVICE) as SensorManager,
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        )
    }

    /**
     * The drawable resource for the home icon
     */
    var homeIcon: Drawable
        get() = homeImageView.drawable
        set(icon) {
            homeImageView.setImageDrawable(icon)
        }

    /**
     * The drawable resource for the home icon's background
     */
    var homeIconBackground: Drawable
        get() = homeImageView.background
        set(background) {
            homeImageView.background = background
        }

    /**
     * The drawable resource for the RC location icon
     */
    var rcLocationIcon: Drawable
        @JvmName("getRCLocationIcon")
        get() = rcImageView.drawable
        @JvmName("setRCLocationIcon")
        set(icon) {
            rcImageView.setImageDrawable(icon)
        }

    /**
     * The drawable resource for the RC location icon's background
     */
    var rcLocationIconBackground: Drawable
        @JvmName("getRCLocationIconBackground")
        get() = rcImageView.background
        @JvmName("setRCLocationIconBackground")
        set(background) {
            rcImageView.background = background
        }

    /**
     * The drawable resource for the aircraft icon
     */
    var aircraftIcon: Drawable
        get() = aircraftImageView.drawable
        set(icon) {
            aircraftImageView.setImageDrawable(icon)
        }

    /**
     * The drawable resource for the aircraft icon's background
     */
    var aircraftIconBackground: Drawable
        get() = aircraftImageView.background
        set(background) {
            aircraftImageView.background = background
        }

    /**
     * The drawable resource for the gimbal yaw icon
     */
    var gimbalYawIcon: Drawable
        get() = gimbalYawImageView.drawable
        set(icon) {
            gimbalYawImageView.setImageDrawable(icon)
        }

    /**
     * The drawable resource for the gimbal yaw icon's background
     */
    var gimbalYawIconBackground: Drawable
        get() = gimbalYawImageView.background
        set(background) {
            gimbalYawImageView.background = background
        }

    /**
     * The drawable resource for the north icon
     */
    var northIcon: Drawable
        get() = northImageView.drawable
        set(icon) {
            northImageView.setImageDrawable(icon)
        }

    /**
     * The drawable resource for the north icon's background
     */
    var northIconBackground: Drawable
        get() = northImageView.background
        set(background) {
            northImageView.background = background
        }

    /**
     * The drawable resource for the inner circles icon
     */
    var innerCirclesIcon: Drawable
        get() = innerCirclesImageView.drawable
        set(icon) {
            innerCirclesImageView.setImageDrawable(icon)
        }

    /**
     * The drawable resource for the inner circles icon's background
     */
    var innerCirclesIconBackground: Drawable
        get() = innerCirclesImageView.background
        set(background) {
            innerCirclesImageView.background = background
        }

    /**
     * The drawable resource for the compass background icon
     */
    var compassBackgroundIcon: Drawable
        get() = compassBackgroundImageView.drawable
        set(icon) {
            compassBackgroundImageView.setImageDrawable(icon)
        }

    /**
     * The drawable resource for the compass background icon's background
     */
    var compassBackgroundIconBackground: Drawable
        get() = compassBackgroundImageView.background
        set(background) {
            compassBackgroundImageView.background = background
        }

    /**
     * The drawable resource for the aircraft attitude icon
     */
    var aircraftAttitudeIcon: Drawable?
        get() = aircraftAttitudeProgressBar.progressDrawable
        set(icon) {
            aircraftAttitudeProgressBar.progressDrawable = icon
        }

    /**
     * The drawable resource for the aircraft attitude icon's background
     */
    var aircraftAttitudeIconBackground: Drawable
        get() = aircraftAttitudeProgressBar.background
        set(background) {
            aircraftAttitudeProgressBar.background = background
        }

    /**
     * The stroke width in px for the lines in the visual compass view
     */
    var visualCompassViewStrokeWidth: Float
        @FloatRange(from = 1.0, to = VisualCompassView.MAX_LINE_WIDTH.toDouble())
        get() = visualCompassView.strokeWidth
        set(@FloatRange(from = 1.0, to = VisualCompassView.MAX_LINE_WIDTH.toDouble()) strokeWidth) {
            visualCompassView.strokeWidth = strokeWidth
        }

    /**
     * The color for the lines in the visual compass view
     */
    var visualCompassViewLineColor: Int
        @ColorInt
        get() = visualCompassView.lineColor
        set(@ColorInt color) {
            visualCompassView.lineColor = color
        }

    /**
     * The interval between the lines in the visual compass view
     */
    var visualCompassViewLineInterval: Int
        get() = visualCompassView.lineInterval
        set(@IntRange(from = 1) interval) {
            visualCompassView.lineInterval = interval
        }

    /**
     * The number of lines to be drawn in the visual compass view
     */
    var visualCompassViewNumberOfLines: Int
        get() = visualCompassView.numberOfLines
        set(@IntRange(from = 3) numberOfLines) {
            visualCompassView.numberOfLines = numberOfLines
        }

    /**
     * The stroke width in px for the lines in the gimbal yaw view
     */
    var gimbalYawViewStrokeWidth: Float
        get() = gimbalYawView.strokeWidth
        set(@FloatRange(from = 1.0, to = GimbalYawView.MAX_LINE_WIDTH.toDouble()) strokeWidth) {
            gimbalYawView.strokeWidth = strokeWidth
        }

    /**
     * The yaw color in the gimbal yaw view
     */
    var gimbalYawViewYawColor: Int
        @ColorInt
        get() = gimbalYawView.yawColor
        set(@ColorInt color) {
            gimbalYawView.yawColor = color
        }

    /**
     * The invalid color in the gimbal yaw view
     */
    var gimbalYawViewInvalidColor: Int
        @ColorInt
        get() = gimbalYawView.invalidColor
        set(@ColorInt color) {
            gimbalYawView.invalidColor = color
        }

    /**
     * Set the blink color in the gimbal yaw view
     */
    var gimbalYawViewBlinkColor: Int
        @ColorInt
        get() = gimbalYawView.blinkColor
        set(@ColorInt color) {
            gimbalYawView.blinkColor = color
        }

    //endregion

    //region Constructor
    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        View.inflate(context, R.layout.uxsdk_widget_compass, this)
    }

    init {
        attrs?.let { initAttributes(context, it) }
    }
    //endregion

    //region Lifecycle
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

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (!isInEditMode) {
            synchronized(this) {
                gimbalYawImageView.pivotX = gimbalYawImageView.measuredWidth / 2f
                gimbalYawImageView.pivotY = gimbalYawImageView.measuredHeight.toFloat()
            }
            halfNorthIconWidth = northImageView.width.toFloat() / 2
            halfAttitudeBallWidth = compassBackgroundImageView.width.toFloat() / 2
            paddingWidth = width.toFloat() - compassBackgroundImageView.width
            paddingHeight = height.toFloat() - compassBackgroundImageView.height
        }
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.compassWidgetState
            .observeOn(SchedulerProvider.ui())
            .subscribe { compassWidgetState -> onCompassStateUpdated(compassWidgetState) })
        addReaction(widgetModel.productConnection
            .observeOn(SchedulerProvider.ui())
            .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
    }
    //endregion

    //region Reaction Helpers
    private fun onCompassStateUpdated(compassWidgetState: CompassWidgetState) {
        widgetStateDataProcessor.onNext(CompassStateUpdated(compassWidgetState))
        updateAircraftAttitudeUI(compassWidgetState.aircraftAttitude)
        updateNorthHeadingUI(compassWidgetState.phoneAzimuth)
        updateAircraftHeadingUI(compassWidgetState.phoneAzimuth, compassWidgetState.aircraftAttitude)

        val viewCoordinates = getAircraftLocationCoordinates(compassWidgetState.phoneAzimuth, compassWidgetState.aircraftState, compassWidgetState.currentLocationState)
        updateAircraftLocationUI(
            getMaxDistance(compassWidgetState.aircraftState, compassWidgetState.currentLocationState),
            calculateScale(compassWidgetState.aircraftState.distance),
            viewCoordinates
        )

        updateGimbalHeadingUI(compassWidgetState.gimbalHeading, compassWidgetState.aircraftAttitude.yaw.toFloat() - compassWidgetState.phoneAzimuth)

        val secondViewCoordinates = getSecondGPSLocationCoordinates(compassWidgetState.phoneAzimuth, compassWidgetState.currentLocationState, compassWidgetState.aircraftState)
        updateSecondGPSLocationUI(compassWidgetState.centerType, secondViewCoordinates)
    }
    //endregion

    //region Calculations
    private fun getSecondGPSLocationCoordinates(
        phoneAzimuth: Float,
        state: CurrentLocationState,
        aircraftState: AircraftState
    ): ViewCoordinates {
        val radians = Math.toRadians(state.angle + phoneAzimuth.toDouble())
        val maxDistance = getMaxDistance(aircraftState, state)
        val rcHomeDistance = state.distance
        val x: Float
        val y: Float
        if (rcHomeDistance == maxDistance) {
            x = cos(radians).toFloat()
            y = sin(radians).toFloat()
        } else {
            x = (rcHomeDistance * cos(radians) / maxDistance).toFloat()
            y = (rcHomeDistance * sin(radians) / maxDistance).toFloat()
        }
        return ViewCoordinates(x, y)
    }

    private fun getMaxDistance(
        aircraftState: AircraftState,
        state: CurrentLocationState
    ): Float {
        var maxDistance = aircraftState.distance
        if (maxDistance < state.distance) {
            maxDistance = state.distance
        }
        if (maxDistance < MAX_DISTANCE) {
            maxDistance = MAX_DISTANCE.toFloat()
        }
        return maxDistance
    }

    private fun getAircraftLocationCoordinates(
        phoneAzimuth: Float,
        aircraftState: AircraftState,
        state: CurrentLocationState
    ): ViewCoordinates {
        val maxDistance = getMaxDistance(aircraftState, state)
        val radians = Math.toRadians(aircraftState.angle + phoneAzimuth.toDouble())
        val aircraftDistance = aircraftState.distance
        val x: Float
        val y: Float
        if (aircraftDistance >= maxDistance) {
            x = cos(radians).toFloat()
            y = sin(radians).toFloat()
        } else {
            x = (aircraftDistance * cos(radians) / maxDistance).toFloat()
            y = (aircraftDistance * sin(radians) / maxDistance).toFloat()
        }
        return ViewCoordinates(x, y)
    }

    private fun calculateScale(distance: Float): Float {
        var scale = 1.0f
        if (distance >= MAX_SCALE_DISTANCE) {
            scale = MIN_SCALE
        } else if (distance > MAX_DISTANCE) {
            scale = 1 - MIN_SCALE + (MAX_SCALE_DISTANCE - distance) / (MAX_SCALE_DISTANCE - MAX_DISTANCE) * MIN_SCALE
        }
        return scale
    }

    //endregion
    //region update UI
    private fun updateNorthHeadingUI(phoneAzimuth: Float) {
        // update north image
        val northRadian = Math.toRadians((FULL_TURN - phoneAzimuth) % FULL_TURN.toDouble())
        val moveX = (halfAttitudeBallWidth + paddingWidth / 2 + halfAttitudeBallWidth * sin(northRadian)).toFloat()
        val moveY = (halfAttitudeBallWidth + paddingHeight / 2 - halfAttitudeBallWidth * cos(northRadian)).toFloat()
        northImageView.x = moveX - halfNorthIconWidth
        northImageView.y = moveY - halfNorthIconWidth
    }

    private fun updateAircraftAttitudeUI(aircraftAttitude: Attitude) {
        //Update aircraft roll
        aircraftAttitudeProgressBar.rotation = aircraftAttitude.roll.toFloat()

        //Update aircraft pitch
        val tempPitch = (-aircraftAttitude.pitch).toFloat() + QUARTER_TURN
        var progress = (tempPitch * 100 / HALF_TURN).toInt()
        if (progress < MIN_PROGRESS) {
            progress = MIN_PROGRESS
        } else if (progress > MAX_PROGRESS) {
            progress = MAX_PROGRESS
        }
        if (aircraftAttitudeProgressBar.progress != progress) {
            aircraftAttitudeProgressBar.progress = progress
        }
    }

    private fun updateAircraftHeadingUI(phoneAzimuth: Float, aircraftAttitude: Attitude) {
        aircraftImageView.rotation = aircraftAttitude.yaw.toFloat() - phoneAzimuth
    }

    private fun updateAircraftLocationUI(maxDistance: Float, scale: Float, viewCoordinates: ViewCoordinates) {
        val wRadius = (measuredWidth - paddingWidth - aircraftImageView.width) / 2.0f
        val hRadius = (measuredHeight - paddingHeight - aircraftImageView.height) / 2.0f

        //update the size and heading of the aircraft
        aircraftImageView.x = paddingWidth / 2.0f + wRadius + viewCoordinates.x * wRadius
        aircraftImageView.y = paddingHeight / 2.0f + hRadius - viewCoordinates.y * hRadius
        aircraftImageView.scaleX = scale
        aircraftImageView.scaleY = scale

        // Update the size and heading of the gimbal
        gimbalYawImageView.x = (aircraftImageView.x + aircraftImageView.width / 2f
                - gimbalYawImageView.width / 2f)
        gimbalYawImageView.y = (aircraftImageView.y + aircraftImageView.height / 2f
                - gimbalYawImageView.height)
        gimbalYawImageView.scaleX = scale
        gimbalYawImageView.scaleY = scale

        //update the compass view
        visualCompassView.visibility = View.VISIBLE
        innerCirclesImageView.visibility = View.GONE
        visualCompassView.setDistance(maxDistance)
    }

    private fun updateGimbalHeadingUI(gimbalHeading: Float, rotationOffset: Float) {
        gimbalYawView.setYaw(gimbalHeading)
        gimbalYawImageView.rotation = gimbalHeading + rotationOffset
    }

    private fun updateSecondGPSLocationUI(
        type: CenterType,
        viewCoordinates: ViewCoordinates
    ) {
        // Calculate the second GPS image's parameters using the center point's parameters
        val centerGPSImage: ImageView?
        val secondGPSImage: ImageView?
        if (type == CenterType.HOME_GPS) {
            centerGPSImage = homeImageView
            secondGPSImage = rcImageView
        } else {
            centerGPSImage = rcImageView
            secondGPSImage = homeImageView
        }
        centerGPSImage.visibility = View.VISIBLE
        val centerParam = centerGPSImage.layoutParams as LayoutParams
        centerParam.leftMargin = 0
        centerParam.topMargin = 0
        centerGPSImage.layoutParams = centerParam

        //Updating second GPS location and show the second GPS image if both exist
        if (type != CenterType.HOME_GPS) {
            secondGPSImage.visibility = View.VISIBLE
            val wRadius = (measuredWidth - paddingWidth - secondGPSImage.width) / 2.0f
            val hRadius = (measuredHeight - paddingHeight - secondGPSImage.height) / 2.0f
            secondGPSImage.x = paddingWidth / 2.0f + wRadius + viewCoordinates.x * wRadius
            secondGPSImage.y = paddingHeight / 2.0f + hRadius - viewCoordinates.y * hRadius
        } else {
            secondGPSImage.visibility = View.GONE
        }
    }
    //endregion

    //region Customization
    override fun getIdealDimensionRatioString(): String {
        return getString(R.string.uxsdk_widget_compass_ratio)
    }

    /**
     * Get the index of the gimbal to which the widget is reacting
     *
     * @return [GimbalIndex]
     */
    fun getGimbalIndex(): ComponentIndexType {
        return widgetModel.getGimbalIndex()
    }

    /**
     * Set the index of gimbal to which the widget should react
     *
     * @param gimbalIndex index of the gimbal.
     */
    fun setGimbalIndex(gimbalIndex: ComponentIndexType?) {
        if (!isInEditMode) {
            widgetModel.setGimbalIndex(gimbalIndex)
        }
    }

    /**
     * Set the resource ID for the home icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setHomeIcon(@DrawableRes resourceId: Int) {
        homeImageView.setImageResource(resourceId)
    }

    /**
     * Set the resource ID for the home icon's background
     *
     * @param resourceId Integer ID of the icon's background resource
     */
    fun setHomeIconBackground(@DrawableRes resourceId: Int) {
        homeImageView.setBackgroundResource(resourceId)
    }

    /**
     * Set the resource ID for the RC location icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setRCLocationIcon(@DrawableRes resourceId: Int) {
        rcImageView.setImageResource(resourceId)
    }

    /**
     * Set the resource ID for the RC location icon's background
     *
     * @param resourceId Integer ID of the icon's background resource
     */
    fun setRCLocationIconBackground(@DrawableRes resourceId: Int) {
        rcImageView.setBackgroundResource(resourceId)
    }

    /**
     * Set the resource ID for the aircraft icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setAircraftIcon(@DrawableRes resourceId: Int) {
        aircraftImageView.setImageResource(resourceId)
    }

    /**
     * Set the resource ID for the aircraft icon's background
     *
     * @param resourceId Integer ID of the icon's background resource
     */
    fun setAircraftIconBackground(@DrawableRes resourceId: Int) {
        aircraftImageView.setBackgroundResource(resourceId)
    }

    /**
     * Set the resource ID for the gimbal yaw icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setGimbalYawIcon(@DrawableRes resourceId: Int) {
        gimbalYawImageView.setImageResource(resourceId)
    }

    /**
     * Set the resource ID for the gimbal yaw icon's background
     *
     * @param resourceId Integer ID of the icon's background resource
     */
    fun setGimbalYawIconBackground(@DrawableRes resourceId: Int) {
        gimbalYawImageView.setBackgroundResource(resourceId)
    }

    /**
     * Set the resource ID for the north icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setNorthIcon(@DrawableRes resourceId: Int) {
        northImageView.setImageResource(resourceId)
    }

    /**
     * Set the resource ID for the north icon's background
     *
     * @param resourceId Integer ID of the icon's background resource
     */
    fun setNorthIconBackground(@DrawableRes resourceId: Int) {
        northImageView.setBackgroundResource(resourceId)
    }

    /**
     * Set the resource ID for the inner circles icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setInnerCirclesIcon(@DrawableRes resourceId: Int) {
        innerCirclesImageView.setImageResource(resourceId)
    }

    /**
     * Set the resource ID for the inner circles icon's background
     *
     * @param resourceId Integer ID of the icon's background resource
     */
    fun setInnerCirclesIconBackground(@DrawableRes resourceId: Int) {
        innerCirclesImageView.setBackgroundResource(resourceId)
    }

    /**
     * Set the resource ID for the compass background icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setCompassBackgroundIcon(@DrawableRes resourceId: Int) {
        compassBackgroundImageView.setImageResource(resourceId)
    }

    /**
     * Set the resource ID for the compass background icon's background
     *
     * @param resourceId Integer ID of the icon's background resource
     */
    fun setCompassBackgroundIconBackground(@DrawableRes resourceId: Int) {
        compassBackgroundImageView.setBackgroundResource(resourceId)
    }

    /**
     * Set the resource ID for the aircraft attitude icon's background
     *
     * @param resourceId Integer ID of the icon's background resource
     */
    fun setAircraftAttitudeIconBackground(@DrawableRes resourceId: Int) {
        aircraftAttitudeProgressBar.setBackgroundResource(resourceId)
    }
    //endregion

    //region Customization Helpers
    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.CompassWidget).use { typedArray ->
            typedArray.getIntAndUse(R.styleable.CompassWidget_uxsdk_cameraIndex) {
                setGimbalIndex(ComponentIndexType.find(it))
            }
            typedArray.getDrawableAndUse(R.styleable.CompassWidget_uxsdk_homeIcon) {
                homeIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.CompassWidget_uxsdk_rcLocationIcon) {
                rcLocationIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.CompassWidget_uxsdk_aircraftIcon) {
                aircraftIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.CompassWidget_uxsdk_gimbalYawIcon) {
                gimbalYawIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.CompassWidget_uxsdk_northIcon) {
                northIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.CompassWidget_uxsdk_innerCirclesIcon) {
                innerCirclesIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.CompassWidget_uxsdk_compassBackgroundIcon) {
                compassBackgroundIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.CompassWidget_uxsdk_aircraftAttitudeIcon) {
                aircraftAttitudeIcon = it
            }
            typedArray.getDimensionAndUse(R.styleable.CompassWidget_uxsdk_visualCompassViewStrokeWidth) {
                visualCompassViewStrokeWidth = it
            }
            typedArray.getColorAndUse(R.styleable.CompassWidget_uxsdk_visualCompassViewLineColor) {
                visualCompassViewLineColor = it
            }
            typedArray.getIntegerAndUse(R.styleable.CompassWidget_uxsdk_visualCompassViewLineInterval) {
                visualCompassViewLineInterval = it
            }
            typedArray.getIntegerAndUse(R.styleable.CompassWidget_uxsdk_visualCompassViewNumberOfLines) {
                visualCompassViewNumberOfLines = it
            }
            typedArray.getDimensionAndUse(R.styleable.CompassWidget_uxsdk_gimbalYawViewStrokeWidth) {
                gimbalYawViewStrokeWidth = it
            }
            typedArray.getColorAndUse(R.styleable.CompassWidget_uxsdk_gimbalYawViewYawColor) {
                gimbalYawViewYawColor = it
            }
            typedArray.getColorAndUse(R.styleable.CompassWidget_uxsdk_gimbalYawViewInvalidColor) {
                gimbalYawViewInvalidColor = it
            }
            typedArray.getColorAndUse(R.styleable.CompassWidget_uxsdk_gimbalYawViewBlinkColor) {
                gimbalYawViewBlinkColor = it
            }
        }
    }
    //endregion

    //region Classes
    /**
     * Wrapper that holds the x and y values of the view coordinates
     */
    private inner class ViewCoordinates internal constructor(val x: Float, val y: Float)
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
         * Compass state update
         */
        data class CompassStateUpdated(val compassWidgetState: CompassWidgetState) : ModelState()
    }
    //endregion
}