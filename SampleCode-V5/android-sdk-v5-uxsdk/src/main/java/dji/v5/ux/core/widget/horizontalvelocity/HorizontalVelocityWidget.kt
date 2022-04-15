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

package dji.v5.ux.core.widget.horizontalvelocity

import android.content.Context
import android.util.AttributeSet
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.WidgetSizeDescription
import dji.v5.ux.core.base.widget.BaseTelemetryWidget
import dji.v5.ux.core.communication.GlobalPreferencesManager
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.getString
import dji.v5.ux.core.extension.getVelocityString
import dji.v5.ux.core.widget.horizontalvelocity.HorizontalVelocityWidget.ModelState
import dji.v5.ux.core.widget.horizontalvelocity.HorizontalVelocityWidget.ModelState.HorizontalVelocityStateUpdated
import dji.v5.ux.core.widget.horizontalvelocity.HorizontalVelocityWidget.ModelState.ProductConnected
import dji.v5.ux.core.widget.horizontalvelocity.HorizontalVelocityWidgetModel.HorizontalVelocityState
import java.text.DecimalFormat

/**
 * Widget displays the horizontal velocity of the aircraft.
 *
 */
open class HorizontalVelocityWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        widgetTheme: Int = 0
) : BaseTelemetryWidget<ModelState>(
        context,
        attrs,
        defStyleAttr,
        WidgetType.TEXT,
        widgetTheme,
        R.style.UXSDKHorizontalVelocityWidget
) {

    //region Fields
    override val metricDecimalFormat: DecimalFormat = DecimalFormat("###0.0")

    override val imperialDecimalFormat: DecimalFormat = DecimalFormat("###0.0")

    private val widgetModel: HorizontalVelocityWidgetModel by lazy {
        HorizontalVelocityWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance(),
                GlobalPreferencesManager.getInstance())
    }
    //endregion 

    //region Constructor
    init {
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
                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
        addReaction(widgetModel.horizontalVelocityState
                .observeOn(SchedulerProvider.ui())
                .subscribe { updateUI(it) })
    }

    //endregion

    //region Reactions to model
    private fun updateUI(horizontalVelocityState: HorizontalVelocityState) {
        widgetStateDataProcessor.onNext(HorizontalVelocityStateUpdated(horizontalVelocityState))
        if (horizontalVelocityState is HorizontalVelocityState.CurrentVelocity) {
            valueString = getDecimalFormat(horizontalVelocityState.unitType)
                    .format(horizontalVelocityState.velocity).toString()
            unitString = getVelocityString(horizontalVelocityState.unitType)
        } else {
            valueString = getString(R.string.uxsdk_string_default_value)
            unitString = null
        }
    }

    //endregion

    //region customizations
    override fun getIdealDimensionRatioString(): String? = null


    override val widgetSizeDescription: WidgetSizeDescription =
            WidgetSizeDescription(WidgetSizeDescription.SizeType.OTHER,
                    widthDimension = WidgetSizeDescription.Dimension.EXPAND,
                    heightDimension = WidgetSizeDescription.Dimension.WRAP)

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
         * Horizontal velocity state update
         */
        data class HorizontalVelocityStateUpdated(val horizontalVelocityState: HorizontalVelocityState) : ModelState()
    }

    //endregion
}