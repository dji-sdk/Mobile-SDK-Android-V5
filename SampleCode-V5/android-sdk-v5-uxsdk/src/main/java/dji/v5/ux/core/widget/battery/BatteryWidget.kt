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

package dji.v5.ux.core.widget.battery

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
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.use
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.core.base.SchedulerProvider
import io.reactivex.rxjava3.functions.Consumer
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetSizeDescription
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.*
import dji.v5.ux.core.util.RxUtil
import dji.v5.ux.core.widget.battery.BatteryWidget.ModelState
import dji.v5.ux.core.widget.battery.BatteryWidget.ModelState.BatteryStateUpdated
import dji.v5.ux.core.widget.battery.BatteryWidget.ModelState.ProductConnected
import dji.v5.ux.core.widget.battery.BatteryWidgetModel.BatteryState
import dji.v5.ux.core.widget.battery.BatteryWidgetModel.BatteryStatus

/**
 * Battery Widget
 * Widget represents the current state of the aircraft battery.
 * Defaults to showing the aggregate battery percentage for the aircraft unless a dual battery
 * is detected or the user overrides this configuration.
 */
open class BatteryWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayoutWidget<ModelState>(context, attrs, defStyleAttr) {

    //region Fields
    private val widgetModel by lazy {
        BatteryWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
    }

    private var singleIconDimensionRatio = getString(R.string.uxsdk_icon_single_battery_ratio)
    private var dualIconDimensionRatio = getString(R.string.uxsdk_icon_dual_battery_ratio)


    private val batteryIconImageView: ImageView = findViewById(R.id.imageview_battery_icon)
    private val singleBatteryValueTextView: TextView = findViewById(R.id.textview_battery_value)
    private val dualBattery1ValueTextView: TextView = findViewById(R.id.textview_battery1_value)
    private val dualBattery2ValueTextView: TextView = findViewById(R.id.textview_battery2_value)
    private val dualBattery1VoltageTextView: TextView = findViewById(R.id.textview_battery1_voltage)
    private val dualBattery2VoltageTextView: TextView = findViewById(R.id.textview_battery2_voltage)
    private var percentColorStates: MutableMap<BatteryStatus, ColorStateList> = mutableMapOf(
            BatteryStatus.NORMAL to ColorStateList.valueOf(getColor(R.color.uxsdk_battery_healthy)),
            BatteryStatus.UNKNOWN to ColorStateList.valueOf(getColor(R.color.uxsdk_battery_healthy)),
            BatteryStatus.OVERHEATING to ColorStateList.valueOf(getColor(R.color.uxsdk_battery_overheating)),
            BatteryStatus.WARNING_LEVEL_1 to ColorStateList.valueOf(getColor(R.color.uxsdk_battery_danger)),
            BatteryStatus.WARNING_LEVEL_2 to ColorStateList.valueOf(getColor(R.color.uxsdk_battery_danger)),
            BatteryStatus.ERROR to ColorStateList.valueOf(getColor(R.color.uxsdk_battery_danger))
    )
    private var voltageColorStates: MutableMap<BatteryStatus, ColorStateList> = mutableMapOf(
            BatteryStatus.NORMAL to ColorStateList.valueOf(getColor(R.color.uxsdk_battery_healthy)),
            BatteryStatus.UNKNOWN to ColorStateList.valueOf(getColor(R.color.uxsdk_battery_healthy)),
            BatteryStatus.OVERHEATING to ColorStateList.valueOf(getColor(R.color.uxsdk_battery_overheating)),
            BatteryStatus.WARNING_LEVEL_1 to ColorStateList.valueOf(getColor(R.color.uxsdk_battery_danger)),
            BatteryStatus.WARNING_LEVEL_2 to ColorStateList.valueOf(getColor(R.color.uxsdk_battery_danger)),
            BatteryStatus.ERROR to ColorStateList.valueOf(getColor(R.color.uxsdk_battery_danger))
    )

    private var voltageBackgroundStates: MutableMap<BatteryStatus, Drawable?> = mutableMapOf(
            BatteryStatus.NORMAL to getDrawable(R.drawable.uxsdk_background_battery_voltage_bg_normal),
            BatteryStatus.UNKNOWN to getDrawable(R.drawable.uxsdk_background_battery_voltage_bg_normal),
            BatteryStatus.OVERHEATING to getDrawable(R.drawable.uxsdk_background_battery_voltage_bg_overheating),
            BatteryStatus.WARNING_LEVEL_1 to getDrawable(R.drawable.uxsdk_background_battery_voltage_bg_danger),
            BatteryStatus.WARNING_LEVEL_2 to getDrawable(R.drawable.uxsdk_background_battery_voltage_bg_danger),
            BatteryStatus.ERROR to getDrawable(R.drawable.uxsdk_background_battery_voltage_bg_danger)
    )
    private var singleIconStates: MutableMap<BatteryStatus, Drawable> = mutableMapOf(
            BatteryStatus.NORMAL to getDrawable(R.drawable.uxsdk_ic_topbar_battery_single_nor),
            BatteryStatus.UNKNOWN to getDrawable(R.drawable.uxsdk_ic_topbar_battery_single_nor),
            BatteryStatus.OVERHEATING to getDrawable(R.drawable.uxsdk_ic_topbar_battery_single_overheating),
            BatteryStatus.WARNING_LEVEL_1 to getDrawable(R.drawable.uxsdk_ic_topbar_battery_single_warning),
            BatteryStatus.WARNING_LEVEL_2 to getDrawable(R.drawable.uxsdk_ic_topbar_battery_single_land_immediately),
            BatteryStatus.ERROR to getDrawable(R.drawable.uxsdk_ic_topbar_battery_single_error)
    )

    private var dualIconStates: MutableMap<BatteryStatus, Drawable> = mutableMapOf(
            BatteryStatus.NORMAL to getDrawable(R.drawable.uxsdk_ic_topbar_battery_double_nor),
            BatteryStatus.UNKNOWN to getDrawable(R.drawable.uxsdk_ic_topbar_battery_double_nor),
            BatteryStatus.OVERHEATING to getDrawable(R.drawable.uxsdk_ic_topbar_battery_double_overheating),
            BatteryStatus.WARNING_LEVEL_1 to getDrawable(R.drawable.uxsdk_ic_topbar_battery_double_warning),
            BatteryStatus.WARNING_LEVEL_2 to getDrawable(R.drawable.uxsdk_ic_topbar_battery_double_land_immediately),
            BatteryStatus.ERROR to getDrawable(R.drawable.uxsdk_ic_topbar_battery_double_warning)
    )

    /**
     * Represents visibility of voltage text for dual battery
     */
    var voltageVisibility: Boolean = true
        set(value) {
            field = value
            checkAndUpdateUI()
        }

    /**
     * Represents text size of single battery percentage
     */
    var singleBatteryPercentageTextSize: Float
        get() = singleBatteryValueTextView.textSize
        set(@Dimension value) {
            singleBatteryValueTextView.textSize = value
        }

    /**
     * Represents background of single battery percentage
     */
    var singleBatteryPercentageBackground: Drawable?
        get() = singleBatteryValueTextView.background
        set(value) {
            singleBatteryValueTextView.background = value
        }

    /**
     * Represents text size of dual battery percentage
     */
    var dualBatteryPercentageTextSize: Float
        get() = dualBattery1ValueTextView.textSize
        set(@Dimension value) {
            dualBattery1ValueTextView.textSize = value
            dualBattery2ValueTextView.textSize = value
        }

    /**
     * Represents text size of dual battery voltage
     */
    var dualBatteryVoltageTextSize: Float
        get() = dualBattery1VoltageTextView.textSize
        set(@Dimension value) {
            dualBattery1VoltageTextView.textSize = value
            dualBattery2VoltageTextView.textSize = value
        }


    /**
     * Represents background of dual battery percentage
     */
    var dualBatteryPercentageBackground: Drawable?
        get() = dualBattery1ValueTextView.background
        set(value) {
            dualBattery1ValueTextView.background = value
            dualBattery2ValueTextView.background = value
        }

    //endregion

    //region Constructors
    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        View.inflate(context, R.layout.uxsdk_widget_battery, this)
    }

    init {
        attrs?.let { initAttributes(context, it) }
    }
    //endregion

    //region Lifecycle
    override fun reactToModelChanges() {
        addReaction(widgetModel.batteryState
                .observeOn(SchedulerProvider.ui())
                .subscribe { updateUI(it) })

        addReaction(widgetModel.productConnection
                .observeOn(SchedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })

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

    //endregion

    //region Reactions to model
    private fun updateUI(batteryState: BatteryState) {
        widgetStateDataProcessor.onNext(BatteryStateUpdated(batteryState))
        when (batteryState) {
            is BatteryState.DualBatteryState -> {
                setDualBatteryUI()
                updateDualBatteryValues(batteryState)
            }
            is BatteryState.SingleBatteryState -> {
                setSingleBatteryUI()
                updateSingleBatteryValues(batteryState)
            }
            is BatteryState.AggregateBatteryState -> {
                setSingleBatteryUI()
                updateAggregateState(batteryState)
            }
            is BatteryState.DisconnectedState -> {
                setSingleBatteryUI()
                updateDisconnectedUI()
            }
        }
    }

    private fun updateDisconnectedUI() {
        setSingleBatteryUI()
        singleBatteryValueTextView.text = getString(R.string.uxsdk_string_default_value)
        singleBatteryValueTextView.textColor = getColor(R.color.uxsdk_white_60_percent)
        batteryIconImageView.imageDrawable = singleIconStates[BatteryStatus.NORMAL]
        batteryIconImageView.setColorFilter(getColor(R.color.uxsdk_white_60_percent), PorterDuff.Mode.SRC_IN)


    }

    private fun updateAggregateState(batteryState: BatteryState.AggregateBatteryState) {
        singleBatteryValueTextView.text = resources.getString(R.string.uxsdk_battery_percent, batteryState.aggregatePercentage)
        setPercentageTextColorByState(singleBatteryValueTextView, batteryState.aggregateBatteryStatus)
        singleIconStates[batteryState.aggregateBatteryStatus]?.let {
            batteryIconImageView.imageDrawable = it
        }
    }

    private fun updateSingleBatteryValues(batteryState: BatteryState.SingleBatteryState) {
        singleBatteryValueTextView.text = resources.getString(R.string.uxsdk_battery_percent, batteryState.percentageRemaining)
        setPercentageTextColorByState(singleBatteryValueTextView, batteryState.batteryStatus)
        singleIconStates[batteryState.batteryStatus]?.let {
            batteryIconImageView.imageDrawable = it
        }
    }

    private fun updateDualBatteryValues(batteryState: BatteryState.DualBatteryState) {
        dualBattery1ValueTextView.text = resources.getString(R.string.uxsdk_battery_percent, batteryState.percentageRemaining1)
        dualBattery2ValueTextView.text = resources.getString(R.string.uxsdk_battery_percent, batteryState.percentageRemaining2)
        dualBattery1VoltageTextView.text = resources.getString(R.string.uxsdk_battery_voltage_unit, batteryState.voltageLevel1)
        dualBattery2VoltageTextView.text = resources.getString(R.string.uxsdk_battery_voltage_unit, batteryState.voltageLevel2)
        setPercentageTextColorByState(dualBattery1ValueTextView, batteryState.batteryStatus1)
        setPercentageTextColorByState(dualBattery2ValueTextView, batteryState.batteryStatus2)
        setVoltageTextColorByState(dualBattery1VoltageTextView, batteryState.batteryStatus1)
        setVoltageTextColorByState(dualBattery2VoltageTextView, batteryState.batteryStatus2)
        dualBattery1VoltageTextView.background = voltageBackgroundStates[batteryState.batteryStatus1]
        dualBattery2VoltageTextView.background = voltageBackgroundStates[batteryState.batteryStatus2]
        val priorityStatus: BatteryStatus =
                if (batteryState.batteryStatus1.index > batteryState.batteryStatus2.index)
                    batteryState.batteryStatus1
                else
                    batteryState.batteryStatus2
        dualIconStates[priorityStatus]?.let {
            batteryIconImageView.imageDrawable = it
        }


    }
    //endregion

    //region private helpers
    private fun setPercentageTextColorByState(textView: TextView, batteryStatus: BatteryStatus) {
        percentColorStates[batteryStatus]?.let {
            textView.textColorStateList = it
        }
    }

    private fun setVoltageTextColorByState(textView: TextView, batteryStatus: BatteryStatus) {
        voltageColorStates[batteryStatus]?.let {
            textView.textColorStateList = it
        }
    }


    private fun setSingleBatteryUI() {
        batteryIconImageView.clearColorFilter()
        singleBatteryValueTextView.visibility = View.VISIBLE
        dualBattery1ValueTextView.visibility = View.GONE
        dualBattery2ValueTextView.visibility = View.GONE
        dualBattery1VoltageTextView.visibility = View.GONE
        dualBattery2VoltageTextView.visibility = View.GONE
    }

    private fun setDualBatteryUI() {
        batteryIconImageView.clearColorFilter()
        singleBatteryValueTextView.visibility = View.GONE
        dualBattery1ValueTextView.visibility = View.VISIBLE
        dualBattery2ValueTextView.visibility = View.VISIBLE
        dualBattery1VoltageTextView.visibility = if (voltageVisibility) View.VISIBLE else View.GONE
        dualBattery2VoltageTextView.visibility = if (voltageVisibility) View.VISIBLE else View.GONE
    }

    private fun checkAndUpdateUI() {
        if (!isInEditMode) {
            addDisposable(widgetModel.batteryState.firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(Consumer { this.updateUI(it) }, RxUtil.logErrorConsumer(TAG, "Update UI ")))
        }
    }

    private fun checkAndUpdateIconDimensionRatio() {
        if (!isInEditMode) {
            addDisposable(widgetModel.batteryState.firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(Consumer { this.updateIconRatio(it) }, RxUtil.logErrorConsumer(TAG, "Update icon dimension ratio ")))
        }
    }


    private fun updateIconRatio(batteryState: BatteryState) {
        val set = ConstraintSet()
        set.clone(this)
        set.setDimensionRatio(batteryIconImageView.id,
                if (batteryState is BatteryState.DualBatteryState) dualIconDimensionRatio else singleIconDimensionRatio)
        set.applyTo(this)
    }
    //endregion

    //region customizations
    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    override val widgetSizeDescription: WidgetSizeDescription =
            WidgetSizeDescription(WidgetSizeDescription.SizeType.OTHER,
                    widthDimension = WidgetSizeDescription.Dimension.WRAP,
                    heightDimension = WidgetSizeDescription.Dimension.EXPAND)

    /**
     * Set the single battery icon drawable for the given batteryStatus
     *
     * @param batteryStatus    batteryStatus to apply the change to
     * @param resource drawable resource for the icon
     */
    fun setSingleIcon(batteryStatus: BatteryStatus, @DrawableRes resource: Int) {
        setSingleIcon(batteryStatus, getDrawable(resource))
    }

    /**
     * Set the single battery icon drawable for the given batteryStatus with the given dimension ratio
     *
     * @param batteryStatus    batteryStatus to apply the change to
     * @param resource drawable resource for the icon
     * @param ratio    A String containing the dimension ratio of the single battery icon.
     */
    fun setSingleIcon(batteryStatus: BatteryStatus, @DrawableRes resource: Int, ratio: String) {
        setSingleIcon(batteryStatus, getDrawable(resource), ratio)
    }

    /**
     * Set the single battery icon drawable for the given batteryStatus
     *
     * @param batteryStatus    batteryStatus to apply the change to
     * @param drawable drawable for the icon
     */
    fun setSingleIcon(batteryStatus: BatteryStatus, drawable: Drawable) {
        singleIconStates[batteryStatus] = drawable
        checkAndUpdateUI()
    }

    /**
     * Set the single battery icon drawable for the given batteryStatus with the given dimension ratio
     *
     * @param batteryStatus    batteryStatus to apply the change to
     * @param drawable drawable for the icon
     * @param ratio    A String containing the dimension ratio of the single battery icon.
     */
    fun setSingleIcon(batteryStatus: BatteryStatus, drawable: Drawable, ratio: String) {
        setSingleIcon(batteryStatus, drawable)
        singleIconDimensionRatio = ratio
        checkAndUpdateIconDimensionRatio()
    }

    /**
     * Set the dual battery icon drawable for the given batteryStatus
     *
     * @param batteryStatus    batteryStatus to apply the change to
     * @param resource drawable resource for the icon
     */
    fun setDualIcon(batteryStatus: BatteryStatus, @DrawableRes resource: Int) {
        setDualIcon(batteryStatus, getDrawable(resource))
    }

    /**
     * Set the dual battery icon drawable for the given batteryStatus with the given dimension ratio
     *
     * @param batteryStatus    batteryStatus to apply the change to
     * @param resource drawable resource for the icon
     * @param ratio    A String containing the dimension ratio of the dual battery icon.
     */
    fun setDualIcon(batteryStatus: BatteryStatus, @DrawableRes resource: Int, ratio: String) {
        setDualIcon(batteryStatus, getDrawable(resource), ratio)
    }

    /**
     * Set the dual battery icon drawable for the given batteryStatus
     *
     * @param batteryStatus    batteryStatus to apply the change to
     * @param drawable drawable for the icon
     */
    fun setDualIcon(batteryStatus: BatteryStatus, drawable: Drawable) {
        dualIconStates[batteryStatus] = drawable
        checkAndUpdateUI()
    }

    /**
     * Set the dual battery icon drawable for the given batteryStatus with the given dimension ratio
     *
     * @param batteryStatus    batteryStatus to apply the change to
     * @param drawable drawable for the icon
     * @param ratio    A String containing the dimension ratio of the dual battery icon.
     */
    fun setDualIcon(batteryStatus: BatteryStatus, drawable: Drawable, ratio: String) {
        setDualIcon(batteryStatus, drawable)
        dualIconDimensionRatio = ratio
        checkAndUpdateIconDimensionRatio()
    }

    /**
     * Set appearance of the percent text in single battery mode
     *
     * @param textAppearance appearance to apply
     */
    fun setSinglePercentTextAppearance(@StyleRes textAppearance: Int) {
        singleBatteryValueTextView.setTextAppearance(context, textAppearance)
    }

    /**
     * Set appearance of the percent text in dual battery mode
     *
     * @param textAppearance appearance to apply
     */
    fun setDualPercentTextAppearance(@StyleRes textAppearance: Int) {
        dualBattery1ValueTextView.setTextAppearance(context, textAppearance)
        dualBattery2ValueTextView.setTextAppearance(context, textAppearance)
    }

    /**
     * Set appearance of the voltage text in dual battery mode
     *
     * @param textAppearance appearance to apply
     */
    fun setDualVoltageTextAppearance(@StyleRes textAppearance: Int) {
        dualBattery1VoltageTextView.setTextAppearance(context, textAppearance)
        dualBattery2VoltageTextView.setTextAppearance(context, textAppearance)
    }

    /**
     * Set the current color of the percent text view for the given state
     *
     * @param batteryStatus to apply the color to
     * @param color color integer resource to apply
     */
    fun setPercentTextColor(batteryStatus: BatteryStatus, @ColorInt color: Int) {
        setPercentTextColor(batteryStatus, ColorStateList.valueOf(color))
    }

    /**
     * Set current color state list of the percent text view for the given state
     *
     * @param batteryStatus         state to apply the color to
     * @param colorStateList ColorStateList to apply
     */
    fun setPercentTextColor(batteryStatus: BatteryStatus, colorStateList: ColorStateList) {
        percentColorStates[batteryStatus] = colorStateList
        checkAndUpdateUI()
    }

    /**
     * Set current color of the voltage text view for the given state
     *
     * @param batteryStatus to apply the color to
     * @param color color integer resource to apply
     */
    fun setVoltageTextColor(batteryStatus: BatteryStatus, @ColorInt color: Int) {
        setVoltageTextColor(batteryStatus, ColorStateList.valueOf(color))
    }

    /**
     * Set current color state list of the voltage text view for the given state
     *
     * @param batteryStatus         state to apply the color to
     * @param colorStateList ColorStateList to apply
     */
    fun setVoltageTextColor(batteryStatus: BatteryStatus, colorStateList: ColorStateList) {
        voltageColorStates[batteryStatus] = colorStateList
        checkAndUpdateUI()
    }

    /**
     * Set the background of the dual battery percentage text
     *
     * @param resourceId integer id of the resource
     */
    fun setDualBatteryPercentageBackground(@DrawableRes resourceId: Int) {
        dualBatteryPercentageBackground = getDrawable(resourceId)
    }

    /**
     * Set the background of the dual battery voltage text
     *
     * @param resourceId integer id of the resource
     */
    fun setDualBatteryVoltageBackground(batteryStatus: BatteryStatus, @DrawableRes resourceId: Int) {
        setDualBatteryVoltageBackground(batteryStatus, getDrawable(resourceId))
    }

    fun setDualBatteryVoltageBackground(batteryStatus: BatteryStatus, drawable: Drawable?) {
        voltageBackgroundStates[batteryStatus] = drawable
        checkAndUpdateUI()
    }

    fun getDualBatteryVoltageBackground(batteryStatus: BatteryStatus): Drawable? {
        return voltageBackgroundStates[batteryStatus]
    }

    /**
     * Set the background of the single battery percentage text
     *
     * @param resourceId integer id of the resource
     */
    fun setSingleBatteryPercentageBackground(@DrawableRes resourceId: Int) {
        singleBatteryPercentageBackground = getDrawable(resourceId)
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.BatteryWidget).use { typedArray ->

            voltageVisibility = typedArray.getBoolean(R.styleable.BatteryWidget_uxsdk_voltageVisibility, true)

            typedArray.getResourceIdAndUse(R.styleable.BatteryWidget_uxsdk_singlePercentAppearance) {
                setSinglePercentTextAppearance(it)
            }

            typedArray.getDimensionAndUse(R.styleable.BatteryWidget_uxsdk_singlePercentTextSize) {
                singleBatteryPercentageTextSize = it
            }

            typedArray.getResourceIdAndUse(R.styleable.BatteryWidget_uxsdk_singleIconUnknown) {
                setSingleIcon(BatteryStatus.UNKNOWN, it)
            }
            typedArray.getResourceIdAndUse(R.styleable.BatteryWidget_uxsdk_singleIconNormal) {
                setSingleIcon(BatteryStatus.NORMAL, it)
            }
            typedArray.getResourceIdAndUse(R.styleable.BatteryWidget_uxsdk_singleIconOverheating) {
                setSingleIcon(BatteryStatus.OVERHEATING, it)
            }
            typedArray.getResourceIdAndUse(R.styleable.BatteryWidget_uxsdk_singleIconWarningLevel1) {
                setSingleIcon(BatteryStatus.WARNING_LEVEL_1, it)
            }
            typedArray.getResourceIdAndUse(R.styleable.BatteryWidget_uxsdk_singleIconWarningLevel2) {
                setSingleIcon(BatteryStatus.WARNING_LEVEL_2, it)
            }
            typedArray.getResourceIdAndUse(R.styleable.BatteryWidget_uxsdk_singleIconError) {
                setSingleIcon(BatteryStatus.ERROR, it)
            }

            typedArray.getResourceIdAndUse(R.styleable.BatteryWidget_uxsdk_dualPercentAppearance) {
                setDualPercentTextAppearance(it)
            }

            typedArray.getDimensionAndUse(R.styleable.BatteryWidget_uxsdk_dualPercentTextSize) {
                dualBatteryPercentageTextSize = it
            }

            typedArray.getColorAndUse(R.styleable.BatteryWidget_uxsdk_percentTextColorUnknown) {
                setPercentTextColor(BatteryStatus.UNKNOWN, it)
            }
            typedArray.getColorAndUse(R.styleable.BatteryWidget_uxsdk_percentTextColorNormal) {
                setPercentTextColor(BatteryStatus.NORMAL, it)
            }
            typedArray.getColorAndUse(R.styleable.BatteryWidget_uxsdk_percentTextColorOverheating) {
                setPercentTextColor(BatteryStatus.OVERHEATING, it)
            }
            typedArray.getColorAndUse(R.styleable.BatteryWidget_uxsdk_percentTextColorWarningLevel1) {
                setPercentTextColor(BatteryStatus.WARNING_LEVEL_1, it)
            }
            typedArray.getColorAndUse(R.styleable.BatteryWidget_uxsdk_percentTextColorWarningLevel2) {
                setPercentTextColor(BatteryStatus.WARNING_LEVEL_2, it)
            }
            typedArray.getColorAndUse(R.styleable.BatteryWidget_uxsdk_percentTextColorError) {
                setPercentTextColor(BatteryStatus.ERROR, it)
            }

            typedArray.getResourceIdAndUse(R.styleable.BatteryWidget_uxsdk_dualVoltageAppearance) {
                setDualVoltageTextAppearance(it)
            }

            typedArray.getDimensionAndUse(R.styleable.BatteryWidget_uxsdk_dualPercentTextSize) {
                dualBatteryVoltageTextSize = it
            }

            typedArray.getColorAndUse(R.styleable.BatteryWidget_uxsdk_voltageTextColorUnknown) {
                setVoltageTextColor(BatteryStatus.UNKNOWN, it)
            }
            typedArray.getColorAndUse(R.styleable.BatteryWidget_uxsdk_voltageTextColorNormal) {
                setVoltageTextColor(BatteryStatus.NORMAL, it)
            }
            typedArray.getColorAndUse(R.styleable.BatteryWidget_uxsdk_voltageTextColorOverheating) {
                setVoltageTextColor(BatteryStatus.OVERHEATING, it)
            }
            typedArray.getColorAndUse(R.styleable.BatteryWidget_uxsdk_voltageTextColorWarningLevel1) {
                setVoltageTextColor(BatteryStatus.WARNING_LEVEL_1, it)
            }
            typedArray.getColorAndUse(R.styleable.BatteryWidget_uxsdk_voltageTextColorWarningLevel2) {
                setVoltageTextColor(BatteryStatus.WARNING_LEVEL_2, it)
            }
            typedArray.getColorAndUse(R.styleable.BatteryWidget_uxsdk_voltageTextColorError) {
                setVoltageTextColor(BatteryStatus.ERROR, it)
            }

            typedArray.getResourceIdAndUse(R.styleable.BatteryWidget_uxsdk_voltageBackgroundUnknown) {
                setDualBatteryVoltageBackground(BatteryStatus.UNKNOWN, it)
            }
            typedArray.getResourceIdAndUse(R.styleable.BatteryWidget_uxsdk_voltageBackgroundNormal) {
                setDualBatteryVoltageBackground(BatteryStatus.NORMAL, it)
            }
            typedArray.getResourceIdAndUse(R.styleable.BatteryWidget_uxsdk_voltageBackgroundOverheating) {
                setDualBatteryVoltageBackground(BatteryStatus.OVERHEATING, it)
            }
            typedArray.getResourceIdAndUse(R.styleable.BatteryWidget_uxsdk_voltageBackgroundWarningLevel1) {
                setDualBatteryVoltageBackground(BatteryStatus.WARNING_LEVEL_1, it)
            }
            typedArray.getResourceIdAndUse(R.styleable.BatteryWidget_uxsdk_voltageBackgroundWarningLevel2) {
                setDualBatteryVoltageBackground(BatteryStatus.WARNING_LEVEL_2, it)
            }
            typedArray.getResourceIdAndUse(R.styleable.BatteryWidget_uxsdk_voltageBackgroundError) {
                setDualBatteryVoltageBackground(BatteryStatus.ERROR, it)
            }

            typedArray.getResourceIdAndUse(R.styleable.BatteryWidget_uxsdk_dualIconUnknown) {
                setDualIcon(BatteryStatus.UNKNOWN, it)
            }
            typedArray.getResourceIdAndUse(R.styleable.BatteryWidget_uxsdk_dualIconNormal) {
                setDualIcon(BatteryStatus.NORMAL, it)
            }
            typedArray.getResourceIdAndUse(R.styleable.BatteryWidget_uxsdk_dualIconOverheating) {
                setDualIcon(BatteryStatus.OVERHEATING, it)
            }
            typedArray.getResourceIdAndUse(R.styleable.BatteryWidget_uxsdk_dualIconWarningLevel1) {
                setDualIcon(BatteryStatus.WARNING_LEVEL_1, it)
            }
            typedArray.getResourceIdAndUse(R.styleable.BatteryWidget_uxsdk_dualIconWarningLevel2) {
                setDualIcon(BatteryStatus.WARNING_LEVEL_2, it)
            }
            typedArray.getResourceIdAndUse(R.styleable.BatteryWidget_uxsdk_dualIconError) {
                setDualIcon(BatteryStatus.ERROR, it)
            }

            singleIconDimensionRatio =
                    typedArray.getString(R.styleable.BatteryWidget_uxsdk_singleIconDimensionRatio, singleIconDimensionRatio)
            dualIconDimensionRatio =
                    typedArray.getString(R.styleable.BatteryWidget_uxsdk_dualIconDimensionRatio, dualIconDimensionRatio)
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
     *
     * Class defines widget state updates
     */
    sealed class ModelState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : ModelState()

        /**
         * Battery state update
         */
        data class BatteryStateUpdated(val batteryState: BatteryState) : ModelState()
    }
    //endregion

    companion object {
        private const val TAG = "BatteryWidget"
    }

}