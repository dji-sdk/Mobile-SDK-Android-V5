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

package dji.v5.ux.core.widget.altitude

import android.content.Context
import android.util.AttributeSet
import dji.v5.ux.R
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.WidgetSizeDescription
import dji.v5.ux.core.base.widget.BaseTelemetryWidget
import dji.v5.ux.core.communication.GlobalPreferencesManager
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.getDistanceString
import dji.v5.ux.core.extension.getString
import dji.v5.ux.core.util.UnitConversionUtil
import dji.v5.ux.core.widget.altitude.AMSLAltitudeWidget.ModelState
import java.text.DecimalFormat

/**
 * Widget displays the above mean sea level altitude
 */
open class AMSLAltitudeWidget @JvmOverloads constructor(
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
        R.style.UXSDKAMSLAltitudeWidget
) {

    //region Fields
    override val metricDecimalFormat: DecimalFormat = DecimalFormat("###0.0")

    override val imperialDecimalFormat: DecimalFormat = DecimalFormat("###0")

    private val widgetModel: AltitudeWidgetModel by lazy {
        AltitudeWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance(),
                GlobalPreferencesManager.getInstance())
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
        addReaction(widgetModel.altitudeState
                .observeOn(SchedulerProvider.ui())
                .subscribe { updateUI(it) })
    }
    //endregion

    //region Reactions to model
    private fun updateUI(altitudeState: AltitudeWidgetModel.AltitudeState) {
        widgetStateDataProcessor.onNext(ModelState.AltitudeStateUpdated(altitudeState))
        if (altitudeState is AltitudeWidgetModel.AltitudeState.CurrentAltitude) {
            if(altitudeState.unitType == UnitConversionUtil.UnitType.IMPERIAL) {
                setValueTextViewMinWidthByText("8888")
            } else {
                setValueTextViewMinWidthByText("888.8")
            }
            unitString = getDistanceString(altitudeState.unitType)
            valueString = getDecimalFormat(altitudeState.unitType)
                    .format(altitudeState.altitudeAMSL).toString()
        } else {
            unitString = null
            valueString = getString(R.string.uxsdk_string_default_value)
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
         * Altitude widget state updated
         */
        data class AltitudeStateUpdated(val altitudeState: AltitudeWidgetModel.AltitudeState) : ModelState()
    }

    //endregion
}