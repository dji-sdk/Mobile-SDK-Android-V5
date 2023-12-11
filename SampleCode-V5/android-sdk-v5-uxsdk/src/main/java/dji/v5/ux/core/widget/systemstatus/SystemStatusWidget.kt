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

package dji.v5.ux.core.widget.systemstatus

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Pair
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.StyleRes
import androidx.core.content.res.use
import dji.v5.manager.diagnostic.DJIDeviceStatus
import dji.v5.manager.diagnostic.WarningLevel
import dji.v5.utils.common.DisplayUtil
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.processors.PublishProcessor
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.WidgetSizeDescription
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.communication.GlobalPreferencesManager
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.*
import dji.v5.ux.core.util.RxUtil
import dji.v5.ux.core.util.UnitConversionUtil
import dji.v5.ux.core.widget.systemstatus.SystemStatusWidget.ModelState
import dji.v5.ux.core.widget.systemstatus.SystemStatusWidget.ModelState.ProductConnected
import dji.v5.ux.core.widget.systemstatus.SystemStatusWidget.ModelState.SystemStatusUpdated
import java.util.*

private const val TAG = "SystemStatusWidget"

/**
 * This widget shows the system status of the aircraft.
 *
 * The WarningStatusItem received by this widget contains the message to be
 * displayed, the warning level and the urgency of the message.
 *
 * The color of the background changes depending on the severity of the
 * status as determined by the WarningLevel. The UI also reacts
 * to the urgency of the message by causing the background to blink.
 */
open class SystemStatusWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayoutWidget<ModelState>(context, attrs, defStyleAttr){
    //region Fields
    private val systemStatusTextView: TextView = findViewById(R.id.textview_system_status)
    private val systemStatusBackgroundImageView: ImageView = findViewById(R.id.imageview_system_status_background)
    private val blinkAnimation: Animation = AnimationUtils.loadAnimation(context, R.anim.uxsdk_anim_blink)
    protected val uiUpdateStateProcessor: PublishProcessor<UIState> = PublishProcessor.create()

    private val widgetModel by lazy {
        SystemStatusWidgetModel(
            DJISDKModel.getInstance(),
            ObservableInMemoryKeyedStore.getInstance(),
            GlobalPreferencesManager.getInstance()
        )
    }

    private val textColorMap: MutableMap<WarningLevel, Int> by lazy {
        if (isInEditMode) {
            mutableMapOf()
        } else {
            mutableMapOf(
                WarningLevel.SERIOUS_WARNING to getColor(R.color.uxsdk_status_error),
                WarningLevel.WARNING to getColor(R.color.uxsdk_status_warning),
                WarningLevel.NORMAL to getColor(R.color.uxsdk_status_good),
                WarningLevel.UNKNOWN to getColor(R.color.uxsdk_status_offline)
            )
        }
    }

    private val backgroundDrawableMap: MutableMap<WarningLevel, Drawable?> = mutableMapOf()

    /**
     * The text size of the system status message text view
     */
    var systemStatusMessageTextSize: Float
        @Dimension
        get() = systemStatusTextView.textSize
        set(@Dimension textSize) {
            systemStatusTextView.textSize = textSize
        }

    /**
     * The default mode determines the default text color and image background settings.
     */
    var defaultMode = DefaultMode.COLOR
        set(value) {
            field = value
            if (value == DefaultMode.COLOR) {
                setSystemStatusMessageTextColor(WarningLevel.SERIOUS_WARNING, getColor(R.color.uxsdk_status_error))
                setSystemStatusMessageTextColor(WarningLevel.WARNING, getColor(R.color.uxsdk_status_warning))
                setSystemStatusMessageTextColor(WarningLevel.NORMAL, getColor(R.color.uxsdk_status_good))
                setSystemStatusMessageTextColor(WarningLevel.UNKNOWN, getColor(R.color.uxsdk_status_offline))

                setSystemStatusBackgroundDrawable(null)
            } else {
                setSystemStatusMessageTextColor(getColor(R.color.uxsdk_white))
                setSystemStatusBackgroundDrawable(WarningLevel.SERIOUS_WARNING, getDrawable(R.drawable.uxsdk_gradient_error))
                setSystemStatusBackgroundDrawable(WarningLevel.WARNING, getDrawable(R.drawable.uxsdk_gradient_warning))
                setSystemStatusBackgroundDrawable(WarningLevel.NORMAL, getDrawable(R.drawable.uxsdk_gradient_good))
                setSystemStatusBackgroundDrawable(WarningLevel.UNKNOWN, getDrawable(R.drawable.uxsdk_gradient_offline))
            }
        }

    private var stateChangeResourceId: Int = 0

    //endregion

    //region Constructor
    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_widget_system_status, this)
    }

    init {
        systemStatusTextView.isSelected = true //Required for horizontal scrolling in textView
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

    override fun reactToModelChanges() {
        addReaction(widgetModel.systemStatus
            .observeOn(SchedulerProvider.ui())
            .subscribe { updateUI(it) })
        addReaction(reactToCompassError())
        addReaction(widgetModel.warningStatusMessageData
            .observeOn(SchedulerProvider.ui())
            .subscribe { updateMessage(it) })
        addReaction(widgetModel.productConnection
            .observeOn(SchedulerProvider.ui())
            .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
    }

    //endregion

    //region Reactions to model
    private fun updateUI(status: DJIDeviceStatus) {
        systemStatusTextView.textColor = getSystemStatusMessageTextColor(status.warningLevel())
        systemStatusBackgroundImageView.imageDrawable = getSystemStatusBackgroundDrawable(status.warningLevel())
        blinkBackground(status.warningLevel() == WarningLevel.SERIOUS_WARNING)
        widgetStateDataProcessor.onNext(SystemStatusUpdated(status))
    }

    private fun updateMessage(messageData: SystemStatusWidgetModel.WarningStatusMessageData) {
        systemStatusTextView.text =
            if (isMaxHeightMessage(messageData.message)) {
                messageData.message + " - " + formatMaxHeight(messageData.maxHeight, messageData.unitType)
            } else {
                messageData.message
            }
    }

    private fun isMaxHeightMessage(text: String?): Boolean {
        return DJIDeviceStatus.IN_NFZ_MAX_HEIGHT.statusCode().equals(text)
    }

    private fun formatMaxHeight(maxHeight: Float, unitType: UnitConversionUtil.UnitType): String? {
        val maxHeightStr: String =
            if (unitType == UnitConversionUtil.UnitType.IMPERIAL) {
                resources.getString(R.string.uxsdk_value_feet, String.format(Locale.US, "%.0f", maxHeight))
            } else {
                resources.getString(R.string.uxsdk_value_meters, String.format(Locale.US, "%.0f", maxHeight))
            }
        return getString(R.string.uxsdk_max_flight_height_limit, maxHeightStr)
    }

    private fun blinkBackground(isUrgentMessage: Boolean) {
        if (isUrgentMessage) {
            systemStatusBackgroundImageView.startAnimation(blinkAnimation)
        } else {
            systemStatusBackgroundImageView.clearAnimation()
        }
    }

    private fun reactToCompassError(): Disposable {
        return Flowable.combineLatest(widgetModel.systemStatus, widgetModel.isMotorOn
        ) { first: DJIDeviceStatus, second: Boolean -> Pair(first, second) }
            .observeOn(SchedulerProvider.ui())
            .subscribe(
                Consumer { values: Pair<DJIDeviceStatus, Boolean> -> updateVoiceNotification(values.first, values.second) },
                RxUtil.logErrorConsumer(TAG, "react to Compass Error: ")
            )
    }

    private fun updateVoiceNotification(statusItem: DJIDeviceStatus, isMotorOn: Boolean) {
        if (isMotorOn && statusItem == DJIDeviceStatus.COMPASS_ERROR) {
            addDisposable(widgetModel.sendVoiceNotification().subscribe())
        }
    }

    private fun checkAndUpdateUI() {
        if (!isInEditMode) {
            addDisposable(widgetModel.systemStatus.firstOrError()
                .observeOn(SchedulerProvider.ui())
                .subscribe(Consumer { this.updateUI(it) }, RxUtil.logErrorConsumer(TAG, "Update UI "))
            )
        }
    }

    //endregion

    //region Customization
    override fun getIdealDimensionRatioString(): String? = null

    override val widgetSizeDescription: WidgetSizeDescription = WidgetSizeDescription(
        WidgetSizeDescription.SizeType.OTHER,
        widthDimension = WidgetSizeDescription.Dimension.EXPAND,
        heightDimension = WidgetSizeDescription.Dimension.EXPAND
    )
    //endregion

    //region Customization Helpers
    /**
     * Set text appearance of the system status message text view
     *
     * @param textAppearance Style resource for text appearance
     */
    fun setSystemStatusMessageTextAppearance(@StyleRes textAppearance: Int) {
        systemStatusTextView.setTextAppearance(context, textAppearance)
    }

    /**
     * Set the text color of the system status message for the given warning level.
     *
     * @param level The level for which to set the system status message text color.
     * @param color The color of the system status message text.
     */
    fun setSystemStatusMessageTextColor(level: WarningLevel, @ColorInt color: Int) {
        textColorMap[level] = color
        checkAndUpdateUI()
    }

    /**
     * Set the text color of the system status message for all warning levels.
     *
     * @param color The color of the system status message text.
     */
    fun setSystemStatusMessageTextColor(@ColorInt color: Int) {
        textColorMap[WarningLevel.SERIOUS_WARNING] = color
        textColorMap[WarningLevel.WARNING] = color
        textColorMap[WarningLevel.NORMAL] = color
        textColorMap[WarningLevel.UNKNOWN] = color
        checkAndUpdateUI()
    }

    /**
     * Get the text color of the system status message for the given warning level.
     *
     * @param level The level for which to get the system status message text color.
     * @return The color of the system status message text.
     */
    @ColorInt
    fun getSystemStatusMessageTextColor(level: WarningLevel): Int {
        return (textColorMap[level]?.let { it } ?: getColor(R.color.uxsdk_status_offline))
    }

    /**
     * Set the background drawable of the system status message for the given warning level.
     *
     * @param level The level for which to set the system status message background drawable.
     * @param background The background of the system status message.
     */
    fun setSystemStatusBackgroundDrawable(level: WarningLevel, background: Drawable?) {
        backgroundDrawableMap[level] = background
        checkAndUpdateUI()
    }

    /**
     * Set the background drawable of the system status message for all warning levels.
     *
     * @param background The background of the system status message.
     */
    fun setSystemStatusBackgroundDrawable(background: Drawable?) {
        backgroundDrawableMap[WarningLevel.SERIOUS_WARNING] = background
        backgroundDrawableMap[WarningLevel.WARNING] = background
        backgroundDrawableMap[WarningLevel.NORMAL] = background
        backgroundDrawableMap[WarningLevel.UNKNOWN] = background
        checkAndUpdateUI()
    }

    /**
     * Get the background drawable of the system status message for the given warning level.
     *
     * @param level The level for which to get the system status message background drawable.
     * @return The background drawable of the system status message.
     */
    fun getSystemStatusBackgroundDrawable(level: WarningLevel): Drawable? {
        return backgroundDrawableMap[level]
    }

    //Initialize all customizable attributes
    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.SystemStatusWidget).use { typedArray ->
            typedArray.getIntegerAndUse(R.styleable.SystemStatusWidget_uxsdk_defaultMode) {
                defaultMode = DefaultMode.find(it)
            }
            typedArray.getResourceIdAndUse(R.styleable.SystemStatusWidget_uxsdk_systemStatusMessageTextAppearance) {
                setSystemStatusMessageTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.SystemStatusWidget_uxsdk_systemStatusMessageTextSize) {
                systemStatusMessageTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getColorAndUse(R.styleable.SystemStatusWidget_uxsdk_systemStatusMessageErrorTextColor) {
                setSystemStatusMessageTextColor(WarningLevel.SERIOUS_WARNING, it)
            }
            typedArray.getColorAndUse(R.styleable.SystemStatusWidget_uxsdk_systemStatusMessageWarningTextColor) {
                setSystemStatusMessageTextColor(WarningLevel.WARNING, it)
            }
            typedArray.getColorAndUse(R.styleable.SystemStatusWidget_uxsdk_systemStatusMessageGoodTextColor) {
                setSystemStatusMessageTextColor(WarningLevel.NORMAL, it)
            }
            typedArray.getColorAndUse(R.styleable.SystemStatusWidget_uxsdk_systemStatusMessageOfflineTextColor) {
                setSystemStatusMessageTextColor(WarningLevel.UNKNOWN, it)
            }
            typedArray.getDrawableAndUse(R.styleable.SystemStatusWidget_uxsdk_systemStatusErrorBackgroundDrawable) {
                setSystemStatusBackgroundDrawable(WarningLevel.SERIOUS_WARNING, it)
            }
            typedArray.getDrawableAndUse(R.styleable.SystemStatusWidget_uxsdk_systemStatusWarningBackgroundDrawable) {
                setSystemStatusBackgroundDrawable(WarningLevel.WARNING, it)
            }
            typedArray.getDrawableAndUse(R.styleable.SystemStatusWidget_uxsdk_systemStatusGoodBackgroundDrawable) {
                setSystemStatusBackgroundDrawable(WarningLevel.NORMAL, it)
            }
            typedArray.getDrawableAndUse(R.styleable.SystemStatusWidget_uxsdk_systemStatusOfflineBackgroundDrawable) {
                setSystemStatusBackgroundDrawable(WarningLevel.UNKNOWN, it)
            }
            typedArray.getResourceIdAndUse(R.styleable.SystemStatusWidget_uxsdk_onStateChange) {
                stateChangeResourceId = it
            }
        }
    }

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
     * Class defines widget UI states
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
         * System status update
         */
        data class SystemStatusUpdated(val status: DJIDeviceStatus) : ModelState()
    }

    /**
     * Sets the mode for the default image backgrounds and text colors.
     */
    enum class DefaultMode(@get:JvmName("value") val value: Int) {
        /**
         * The text color updates to match the [WarningLevel] and there is no image background.
         */
        COLOR(0),

        /**
         * The text is white and the background image is a gradient which updates to match the
         * [WarningLevel].
         */
        GRADIENT(1);

        companion object {
            @JvmStatic
            val values = values()

            @JvmStatic
            fun find(value: Int): DefaultMode {
                return values.find { it.value == value } ?: COLOR
            }
        }
    }
    //endregion
}