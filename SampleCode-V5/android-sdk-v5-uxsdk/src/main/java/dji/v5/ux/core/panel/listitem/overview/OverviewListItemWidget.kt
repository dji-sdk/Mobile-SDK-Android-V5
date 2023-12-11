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

package dji.v5.ux.core.panel.listitem.overview

import android.content.Context
import android.util.AttributeSet
import dji.v5.manager.diagnostic.WarningLevel
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.WidgetSizeDescription
import dji.v5.ux.core.base.panel.listitem.ListItemLabelButtonWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.getString
import dji.v5.ux.core.panel.listitem.overview.OverviewListItemWidget.ModelState
import dji.v5.ux.core.panel.listitem.overview.OverviewListItemWidget.ModelState.OverviewStateUpdated
import dji.v5.ux.core.panel.listitem.overview.OverviewListItemWidget.ModelState.ProductConnected
import dji.v5.ux.core.panel.listitem.overview.OverviewListItemWidgetModel.OverviewState

/**
 * Widget displays the overall status of the aircraft
 */
open class OverviewListItemWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ListItemLabelButtonWidget<ModelState>(
        context,
        attrs,
        defStyleAttr,
        WidgetType.LABEL,
        R.style.UXSDKOverviewListItem
) {

    //region Fields
    private val widgetModel by lazy {
        OverviewListItemWidgetModel(DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
    }
    //endregion

    //region Lifecycle
    override fun reactToModelChanges() {
        addReaction(widgetModel.productConnection
                .observeOn(SchedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
        addReaction(widgetModel.overviewStatus
                .observeOn(SchedulerProvider.ui())
                .subscribe { updateUI(it) })
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
        // No code needed
    }
    //endregion

    //region Reactions to model
    private fun updateUI(overviewState: OverviewState) {
        widgetStateDataProcessor.onNext(OverviewStateUpdated(overviewState))
        if (overviewState is OverviewState.CurrentStatus) {
            listItemLabelTextColor = when (overviewState.warningStatusItem.warningLevel()) {
                WarningLevel.NORMAL -> normalValueColor
                WarningLevel.NOTICE -> normalValueColor
                WarningLevel.CAUTION -> normalValueColor
                WarningLevel.WARNING -> warningValueColor
                WarningLevel.SERIOUS_WARNING -> errorValueColor
                else -> normalValueColor
            }
            listItemLabel = overviewState.warningStatusItem.description()
            isEnabled = true
        } else {
            listItemLabel = getString(R.string.uxsdk_string_default_value)
            listItemLabelTextColor = disconnectedValueColor
            isEnabled = false
        }
    }
    //endregion

    //region Customization
    override fun getIdealDimensionRatioString(): String? = null

    override val widgetSizeDescription: WidgetSizeDescription =
            WidgetSizeDescription(WidgetSizeDescription.SizeType.OTHER,
                    widthDimension = WidgetSizeDescription.Dimension.EXPAND,
                    heightDimension = WidgetSizeDescription.Dimension.WRAP)

    // endregion

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
         * Overview status update
         */
        data class OverviewStateUpdated(val overviewState: OverviewState) : ModelState()
    }
    //endregion
}