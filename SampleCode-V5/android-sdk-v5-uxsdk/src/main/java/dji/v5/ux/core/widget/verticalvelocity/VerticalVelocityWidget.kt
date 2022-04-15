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

package dji.v5.ux.core.widget.verticalvelocity

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.core.content.res.use
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.WidgetSizeDescription
import dji.v5.ux.core.base.widget.BaseTelemetryWidget
import dji.v5.ux.core.communication.GlobalPreferencesManager
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.getDrawable
import dji.v5.ux.core.extension.getDrawableAndUse
import dji.v5.ux.core.extension.getString
import dji.v5.ux.core.extension.getVelocityString
import dji.v5.ux.core.util.UnitConversionUtil.UnitType
import dji.v5.ux.core.widget.verticalvelocity.VerticalVelocityWidget.ModelState
import dji.v5.ux.core.widget.verticalvelocity.VerticalVelocityWidgetModel.VerticalVelocityState
import java.text.DecimalFormat

/**
 * Widget displays the vertical velocity of the aircraft.
 *
 */
open class VerticalVelocityWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        widgetTheme: Int = 0
) : BaseTelemetryWidget<ModelState>(
        context,
        attrs,
        defStyleAttr,
        WidgetType.TEXT_IMAGE_RIGHT,
        widgetTheme,
        R.style.UXSDKVerticalVelocityWidget
) {


    //region Fields
    /**
     * Icon for upward velocity
     */
    var upwardVelocityIcon: Drawable = getDrawable(R.drawable.uxsdk_ic_arrow_up)

    /**
     * Icon for downward velocity
     */
    var downwardVelocityIcon: Drawable = getDrawable(R.drawable.uxsdk_ic_arrow_down)

    private val widgetModel: VerticalVelocityWidgetModel by lazy {
        VerticalVelocityWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance(),
                GlobalPreferencesManager.getInstance())
    }

    override val metricDecimalFormat: DecimalFormat = DecimalFormat("###0.0")

    override val imperialDecimalFormat: DecimalFormat = DecimalFormat("###0.0")
    //endregion

    //region Constructor
    init {
        initThemeAttributes(context, widgetTheme)
        initAttributes(context, attrs)
        setValueTextViewMinWidthByText("88.8")
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
        addReaction(widgetModel.productConnection
                .observeOn(SchedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ModelState.ProductConnected(it)) })
        addReaction(widgetModel.verticalVelocityState
                .observeOn(SchedulerProvider.ui())
                .subscribe { updateUI(it) })
    }
    //endregion

    //region Customization
    override fun getIdealDimensionRatioString(): String? = null


    override val widgetSizeDescription: WidgetSizeDescription =
            WidgetSizeDescription(WidgetSizeDescription.SizeType.OTHER,
                    widthDimension = WidgetSizeDescription.Dimension.EXPAND,
                    heightDimension = WidgetSizeDescription.Dimension.WRAP)

    /**
     * Set the icon for upward velocity
     *
     * @param resourceId Integer ID of the icon
     */
    fun setUpwardVelocityIcon(@DrawableRes resourceId: Int) {
        upwardVelocityIcon = getDrawable(resourceId)
    }

    /**
     * Set the icon for downward velocity
     *
     * @param resourceId Integer ID of the icon
     */
    fun setDownwardVelocityIcon(@DrawableRes resourceId: Int) {
        downwardVelocityIcon = getDrawable(resourceId)
    }

    @SuppressLint("Recycle")
    private fun initThemeAttributes(context: Context, widgetTheme: Int) {
        val verticalVelocityAttributeArray: IntArray = R.styleable.VerticalVelocityWidget
        context.obtainStyledAttributes(widgetTheme, verticalVelocityAttributeArray).use {
            initAttributesByTypedArray(it)
        }
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.VerticalVelocityWidget, 0, defaultStyle).use {
            initAttributesByTypedArray(it)
        }
    }

    private fun initAttributesByTypedArray(typedArray: TypedArray) {
        typedArray.getDrawableAndUse(R.styleable.VerticalVelocityWidget_uxsdk_upward_velocity_icon) {
            upwardVelocityIcon = it
        }
        typedArray.getDrawableAndUse(R.styleable.VerticalVelocityWidget_uxsdk_downward_velocity_icon) {
            downwardVelocityIcon = it
        }
    }
    //endregion

    //region Reactions to model
    private fun updateUI(verticalVelocityState: VerticalVelocityState) {
        widgetStateDataProcessor.onNext(ModelState.VerticalVelocityStateUpdated(verticalVelocityState))
        when (verticalVelocityState) {
            VerticalVelocityState.ProductDisconnected -> updateToDisconnectedState()
            is VerticalVelocityState.Idle -> updateVelocityState(0.0, verticalVelocityState.unitType, null)
            is VerticalVelocityState.UpwardVelocity -> updateVelocityState(verticalVelocityState.velocity, verticalVelocityState.unitType, upwardVelocityIcon)
            is VerticalVelocityState.DownwardVelocity -> updateVelocityState(verticalVelocityState.velocity, verticalVelocityState.unitType, downwardVelocityIcon)
        }

    }

    private fun updateVelocityState(velocity: Double, unitType: UnitType, icon: Drawable?) {
        widgetIcon = icon
        valueString = getDecimalFormat(unitType).format(velocity).toString()
        unitString = getVelocityString(unitType)
    }

    private fun updateToDisconnectedState() {
        unitString = null
        widgetIcon = null
        valueString = getString(R.string.uxsdk_string_default_value)
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
     * Class defines widget state updates
     */
    sealed class ModelState {
        /**
         * Product connection update
         */
        data class ProductConnected(val boolean: Boolean) : ModelState()

        /**
         * Vertical velocity state update
         */
        data class VerticalVelocityStateUpdated(val verticalVelocityState: VerticalVelocityState) : ModelState()
    }
    //endregion

}