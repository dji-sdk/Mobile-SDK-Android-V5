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

package dji.v5.ux.core.panel.listitem.rcbattery

import android.content.Context
import android.util.AttributeSet
import dji.v5.utils.common.JsonUtil
import dji.v5.utils.common.LogUtils
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.WidgetSizeDescription
import dji.v5.ux.core.base.panel.listitem.ListItemLabelButtonWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.getString
import dji.v5.ux.core.panel.listitem.rcbattery.RCBatteryListItemWidget.ModelState
import dji.v5.ux.core.panel.listitem.rcbattery.RCBatteryListItemWidget.ModelState.ProductConnected
import dji.v5.ux.core.panel.listitem.rcbattery.RCBatteryListItemWidget.ModelState.RCBatteryStateUpdated
import dji.v5.ux.core.panel.listitem.rcbattery.RCBatteryListItemWidgetModel.RCBatteryState

/**
 * Remote controller battery list item
 */
open class RCBatteryListItemWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ListItemLabelButtonWidget<ModelState>(
    context,
    attrs,
    defStyleAttr,
    WidgetType.LABEL,
    R.style.UXSDKRCBatteryListItem
) {

    //region Fields
    private val widgetModel by lazy {
        RCBatteryListItemWidgetModel(
            DJISDKModel.getInstance(),
            ObservableInMemoryKeyedStore.getInstance()
        )
    }
    //endregion

    //region Lifecycle
    override fun reactToModelChanges() {
        addReaction(widgetModel.productConnection
            .observeOn(SchedulerProvider.ui())
            .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
        addReaction(widgetModel.rcBatteryState
            .observeOn(SchedulerProvider.ui())
            .subscribe { this.updateUI(it) })
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

    override fun onButtonClick() {
        // No implementation needed
    }
    //endregion

    //region Reactions to model
    private fun updateUI(rcBatteryState: RCBatteryState) {
        widgetStateDataProcessor.onNext(RCBatteryStateUpdated(rcBatteryState))
        when (rcBatteryState) {
            RCBatteryState.RCDisconnected -> {
                listItemLabelTextColor = disconnectedValueColor
                listItemLabel = getString(R.string.uxsdk_string_default_value)
            }
            is RCBatteryState.Normal -> {
                listItemLabelTextColor = normalValueColor
                listItemLabel = getString(R.string.uxsdk_rc_battery_percent, rcBatteryState.remainingChargePercent)
            }
            is RCBatteryState.Low -> {
                listItemLabelTextColor = errorValueColor
                listItemLabel = getString(R.string.uxsdk_rc_battery_percent, rcBatteryState.remainingChargePercent)
            }
        }
    }
    //endregion

    //region Customization
    override fun getIdealDimensionRatioString(): String? = null


    override val widgetSizeDescription: WidgetSizeDescription =
        WidgetSizeDescription(
            WidgetSizeDescription.SizeType.OTHER,
            widthDimension = WidgetSizeDescription.Dimension.EXPAND,
            heightDimension = WidgetSizeDescription.Dimension.WRAP
        )

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
         * RC battery State update
         */
        data class RCBatteryStateUpdated(val rcBatteryState: RCBatteryState) : ModelState()
    }
    //endregion

}
