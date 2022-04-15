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

package dji.v5.ux.core.widget.gpssignal

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.core.content.res.use
import dji.sdk.keyvalue.value.flightcontroller.GPSSignalLevel
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.processors.PublishProcessor
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.communication.OnStateChangeCallback
import dji.v5.ux.core.extension.*
import dji.v5.ux.core.util.DisplayUtil
import dji.v5.ux.core.util.RxUtil
import dji.v5.ux.core.widget.gpssignal.GPSSignalWidget.ModelState
import dji.v5.ux.core.widget.gpssignal.GPSSignalWidget.ModelState.*
import java.util.*

/**
 * This widget displays the current GPS Signal strength along with the number of satellites
 * available and whether RTK is enabled on supported aircraft.
 */
open class GPSSignalWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayoutWidget<ModelState>(context, attrs, defStyleAttr), View.OnClickListener {

    //region Fields
    private val gpsIconImageView: ImageView = findViewById(R.id.imageview_gps_icon)
    private val gpsSignalImageView: ImageView = findViewById(R.id.imageview_gps_signal)
    private val satelliteCountTextView: TextView = findViewById(R.id.textview_satellite_count)
    private val rtkEnabledTextView: TextView = findViewById(R.id.textview_rtk_enabled)
    private var stateChangeResourceId: Int = 0
    private val uiUpdateStateProcessor: PublishProcessor<UIState> = PublishProcessor.create()

    private val widgetModel by lazy {
        GPSSignalWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
    }

    /**
     * The color of the GPS display icon when the product is connected
     */
    @get:ColorInt
    var connectedStateIconColor: Int = getColor(R.color.uxsdk_white)
        set(@ColorInt value) {
            field = value
            checkAndUpdateIconColor()
        }

    /**
     * The color of the GPS display icon when the product is disconnected
     */
    @get:ColorInt
    var disconnectedStateIconColor: Int = getColor(R.color.uxsdk_gray_58)
        set(@ColorInt value) {
            field = value
            checkAndUpdateIconColor()
        }

    /**
     * The text color of the RTK enabled text view when the RTK is accurate
     */
    @get:ColorInt
    @get:JvmName("getRTKAccurateTextColor")
    var rtkAccurateTextColor = getColor(R.color.uxsdk_white)
        @JvmName("setRTKAccurateTextColor")
        set(@ColorInt value) {
            field = value
            checkAndUpdateRTKColor()
        }

    /**
     * The text color of the RTK enabled text view when the RTK is inaccurate
     */
    @get:ColorInt
    @get:JvmName("getRTKInaccurateTextColor")
    var rtkInaccurateTextColor = getColor(R.color.uxsdk_red)
        @JvmName("setRTKInaccurateTextColor")
        set(@ColorInt value) {
            field = value
            checkAndUpdateRTKColor()
        }

    /**
     * The drawable resource for the GPS display icon when the GPS is internal
     */
    @get:JvmName("getGPSIcon")
    var gpsIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_topbar_gps)
        @JvmName("setGPSIcon")
        set(value) {
            field = value
            checkAndUpdateIcon()
        }

    /**
     * The drawable resource for the GPS display icon when the GPS is external
     */
    var externalGPSIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_topbar_external_gps)
        set(value) {
            field = value
            checkAndUpdateIcon()
        }

    /**
     * Icon for the GPS signal strength
     */
    var gpsSignalIcon: Drawable?
        @JvmName("getGPSSignalIcon")
        get() = gpsSignalImageView.imageDrawable
        @JvmName("setGPSSignalIcon")
        set(value) {
            gpsSignalImageView.imageDrawable = value
        }

    /**
     * Background drawable resource for the GPS icon
     */
    var gpsIconBackground: Drawable?
        @JvmName("getGPSIconBackground")
        get() = gpsIconImageView.background
        @JvmName("setGPSIconBackground")
        set(value) {
            gpsIconImageView.background = value
        }

    /**
     * Background of the GPS signal icon
     */
    var gpsSignalIconBackground: Drawable?
        @JvmName("getGPSSignalIconBackground")
        get() = gpsSignalImageView.background
        @JvmName("setGPSSignalIconBackground")
        set(value) {
            gpsSignalImageView.background = value
        }

    /**
     * ColorStateList for the satellite count text
     */
    var satelliteCountTextColors: ColorStateList?
        get() = satelliteCountTextView.textColorStateList
        set(value) {
            satelliteCountTextView.textColorStateList = value
        }

    /**
     * Color for the satellite count text
     */
    var satelliteCountTextColor: Int
        @ColorInt
        get() = satelliteCountTextView.textColor
        set(@ColorInt value) {
            satelliteCountTextView.textColor = value
        }

    /**
     * Size for the satellite count text
     */
    var satelliteCountTextSize: Float
        @Dimension
        get() = satelliteCountTextView.textSize
        set(@Dimension textSize) {
            satelliteCountTextView.textSize = textSize
        }

    /**
     * Background for the satellite count text
     */
    var satelliteCountTextBackground: Drawable?
        get() = satelliteCountTextView.background
        set(drawable) {
            satelliteCountTextView.background = drawable
        }

    /**
     * Size for the satellite count text
     */
    var rtkEnabledTextSize: Float
        @Dimension
        get() = rtkEnabledTextView.textSize
        set(@Dimension textSize) {
            rtkEnabledTextView.textSize = textSize
        }

    /**
     * Background for the satellite count text
     */
    var rtkEnabledTextBackground: Drawable?
        get() = rtkEnabledTextView.background
        set(drawable) {
            rtkEnabledTextView.background = drawable
        }

    /**
     * Call back for when the widget is tapped.
     * This can be used to link the widget to RTKWidget
     */
    var stateChangeCallback: OnStateChangeCallback<Any>? = null
    //endregion

    //region Constructor
    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_widget_gps_signal, this)
    }

    init {
        setOnClickListener(this)
        attrs?.let { initAttributes(context, it) }
    }
    //endregion

    //region Lifecycle
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
        }
        initializeListener()
    }

    override fun onDetachedFromWindow() {
        destroyListener()
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
        super.onDetachedFromWindow()
    }

    override fun onClick(v: View?) {
        uiUpdateStateProcessor.onNext(UIState.WidgetClicked)
        stateChangeCallback?.onStateChange(null)
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.gpsSignalQuality
                .observeOn(SchedulerProvider.ui())
                .subscribe { updateGPSSignal(it) })
        addReaction(widgetModel.satelliteNumber
                .observeOn(SchedulerProvider.ui())
                .subscribe { updateSatelliteCount(it) })
        addReaction(widgetModel.rtkEnabled
                .observeOn(SchedulerProvider.ui())
                .subscribe { updateRtk(it) })
        addReaction(widgetModel.isRTKAccurate
                .observeOn(SchedulerProvider.ui())
                .subscribe { updateRTKColor(it) })
        addReaction(widgetModel.productConnection
                .observeOn(SchedulerProvider.ui())
                .subscribe { updateIconColor(it) })
        addReaction(widgetModel.isExternalGPSUsed
                .observeOn(SchedulerProvider.ui())
                .subscribe { updateIcon(it) })
    }
    //endregion

    //region Reactions to model
    private fun updateGPSSignal(gpsSignalLevel: GPSSignalLevel?) {
        val gpsLevel = if (gpsSignalLevel != null && gpsSignalLevel != GPSSignalLevel.LEVEL_NONE) {
            gpsSignalLevel.value()
        } else {
            0
        }
        widgetStateDataProcessor.onNext(GPSSignalQualityUpdated(gpsLevel))
        gpsSignalImageView.setImageLevel(gpsLevel)
    }

    private fun updateSatelliteCount(satelliteCount: Int) {
        widgetStateDataProcessor.onNext(SatelliteCountUpdated(satelliteCount))
        satelliteCountTextView.text = String.format(Locale.getDefault(), "%d", satelliteCount)
    }

    private fun updateRtk(rtkEnabled: Boolean) {
        widgetStateDataProcessor.onNext(RTKEnabledUpdated(rtkEnabled))
        rtkEnabledTextView.text = if (rtkEnabled) getString(R.string.uxsdk_gps_rtk_enabled) else ""
    }

    private fun updateRTKColor(isRTKAccurate: Boolean) {
        widgetStateDataProcessor.onNext(RTKAccurateUpdated(isRTKAccurate))
        rtkEnabledTextView.textColor = if (isRTKAccurate) rtkAccurateTextColor else rtkInaccurateTextColor
    }

    private fun updateIconColor(isConnected: Boolean) {
        widgetStateDataProcessor.onNext(ProductConnected(isConnected))
        if (isConnected) {
            gpsIconImageView.setColorFilter(connectedStateIconColor, PorterDuff.Mode.SRC_IN)
            satelliteCountTextView.visibility = View.VISIBLE
        } else {
            gpsIconImageView.setColorFilter(disconnectedStateIconColor, PorterDuff.Mode.SRC_IN)
            satelliteCountTextView.visibility = View.GONE
        }
    }

    private fun updateIcon(isExternalGPSUsed: Boolean) {
        widgetStateDataProcessor.onNext(ExternalGPSUsedUpdated(isExternalGPSUsed))
        gpsIconImageView.imageDrawable = if (isExternalGPSUsed) externalGPSIcon else gpsIcon
    }

    private fun checkAndUpdateIconColor() {
        if (!isInEditMode) {
            addDisposable(widgetModel.productConnection.firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(Consumer { this.updateIconColor(it) }, RxUtil.logErrorConsumer(TAG, "Update Icon Color ")))
        }
    }

    private fun checkAndUpdateRTKColor() {
        if (!isInEditMode) {
            addDisposable(widgetModel.isRTKAccurate.firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(Consumer { this.updateRTKColor(it) }, RxUtil.logErrorConsumer(TAG, "Update RTK Color ")))
        }
    }

    private fun checkAndUpdateIcon() {
        if (!isInEditMode) {
            addDisposable(widgetModel.isExternalGPSUsed.firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(Consumer { this.updateIcon(it) }, RxUtil.logErrorConsumer(TAG, "Update Icon ")))
        }
    }
    //endregion

    //region Helpers
    private fun initializeListener() {
        if (stateChangeResourceId != INVALID_RESOURCE && this.rootView != null) {
            val widgetView = this.rootView.findViewById<View>(stateChangeResourceId)
            if (widgetView is OnStateChangeCallback<*>) {
                stateChangeCallback = widgetView as OnStateChangeCallback<Any>
            }
        }
    }

    private fun destroyListener() {
        stateChangeCallback = null
    }
    //endregion

    //region Customization
    override fun getIdealDimensionRatioString(): String {
        return getString(R.string.uxsdk_widget_gps_signal_ratio)
    }

    /**
     * Set the resource ID for the GPS display icon when the GPS is internal
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setGPSIcon(@DrawableRes resourceId: Int) {
        gpsIcon = getDrawable(resourceId)
    }

    /**
     * Set the resource ID for the GPS display icon when the GPS is external
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setExternalGPSIcon(@DrawableRes resourceId: Int) {
        externalGPSIcon = getDrawable(resourceId)
    }

    /**
     * Set the resource ID for the GPS signal icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setGPSSignalIcon(@DrawableRes resourceId: Int) {
        gpsSignalIcon = getDrawable(resourceId)
    }

    /**
     * Set the resource ID for the GPS display icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setGPSIconBackground(@DrawableRes resourceId: Int) {
        gpsIconBackground = getDrawable(resourceId)
    }

    /**
     * Set the resource ID for the GPS signal icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setGPSSignalIconBackground(@DrawableRes resourceId: Int) {
        gpsSignalIconBackground = getDrawable(resourceId)
    }

    /**
     * Set text appearance of the satellite count text view
     *
     * @param textAppearance Style resource for text appearance
     */
    fun setSatelliteCountTextAppearance(@StyleRes textAppearance: Int) {
        satelliteCountTextView.setTextAppearance(context, textAppearance)
    }

    /**
     * Set text appearance of the RTK enabled text view
     *
     * @param textAppearance Style resource for text appearance
     */
    fun setRTKEnabledTextAppearance(@StyleRes textAppearance: Int) {
        rtkEnabledTextView.setTextAppearance(context, textAppearance)
    }

    //Initialize all customizable attributes
    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.GPSSignalWidget).use { typedArray ->
            stateChangeResourceId = typedArray.getResourceId(R.styleable.GPSSignalWidget_uxsdk_onStateChange, INVALID_RESOURCE)
            typedArray.getDrawableAndUse(R.styleable.GPSSignalWidget_uxsdk_gpsIcon) {
                gpsIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.GPSSignalWidget_uxsdk_externalGPSIcon) {
                externalGPSIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.GPSSignalWidget_uxsdk_gpsIconBackgroundDrawable) {
                gpsIconBackground = it
            }
            typedArray.getDrawableAndUse(R.styleable.GPSSignalWidget_uxsdk_gpsSignalIcon) {
                gpsSignalIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.GPSSignalWidget_uxsdk_gpsSignalIconBackgroundDrawable) {
                gpsSignalIconBackground = it
            }
            typedArray.getResourceIdAndUse(R.styleable.GPSSignalWidget_uxsdk_satelliteCountTextAppearance) {
                setSatelliteCountTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.GPSSignalWidget_uxsdk_satelliteCountTextSize) {
                satelliteCountTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getColorAndUse(R.styleable.GPSSignalWidget_uxsdk_satelliteCountTextColor) {
                satelliteCountTextColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.GPSSignalWidget_uxsdk_satelliteCountBackgroundDrawable) {
                satelliteCountTextBackground = it
            }
            typedArray.getResourceIdAndUse(R.styleable.GPSSignalWidget_uxsdk_rtkEnabledTextAppearance) {
                setRTKEnabledTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.GPSSignalWidget_uxsdk_rtkEnabledTextSize) {
                rtkEnabledTextSize = DisplayUtil.pxToSp(context, it)
            }
            rtkAccurateTextColor = typedArray.getColor(R.styleable.GPSSignalWidget_uxsdk_rtkAccurateTextColor,
                    rtkAccurateTextColor)
            rtkInaccurateTextColor = typedArray.getColor(R.styleable.GPSSignalWidget_uxsdk_rtkInaccurateTextColor,
                    rtkInaccurateTextColor)

            typedArray.getDrawableAndUse(R.styleable.GPSSignalWidget_uxsdk_rtkEnabledBackgroundDrawable) {
                rtkEnabledTextBackground = it
            }
            connectedStateIconColor = typedArray.getColor(R.styleable.GPSSignalWidget_uxsdk_connectedStateIconColor,
                    connectedStateIconColor)
            disconnectedStateIconColor = typedArray.getColor(R.styleable.GPSSignalWidget_uxsdk_disconnectedStateIconColor,
                    disconnectedStateIconColor)
        }
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
     * Get the [UIState] updates
     */
    fun getUIStateUpdates(): Flowable<UIState> {
        return uiUpdateStateProcessor.onBackpressureBuffer()
    }

    /**
     * Widget UI update State
     */
    sealed class UIState {
        /**
         * Widget click update
         */
        object WidgetClicked : UIState()
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
         * GPS signal quality / strength update
         */
        data class GPSSignalQualityUpdated(val signalQuality: Int) : ModelState()

        /**
         * Satellite count update
         */
        data class SatelliteCountUpdated(val satelliteCount: Int) : ModelState()

        /**
         * RTK enabled state update
         */
        data class RTKEnabledUpdated(val isRTKEnabled: Boolean) : ModelState()

        /**
         * RTK accuracy update
         */
        data class RTKAccurateUpdated(val isRTKAccurate: Boolean) : ModelState()

        /**
         * External GPS usage update
         */
        data class ExternalGPSUsedUpdated(val isExternalGPSUsed: Boolean) : ModelState()
    }
    //endregion

    companion object {
        private const val TAG = "GPSSignalWidget"
    }
}