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

package dji.v5.ux.core.panel.listitem.rcstickmode

import android.content.Context
import android.util.AttributeSet
import dji.sdk.keyvalue.value.remotecontroller.ControlMode
import dji.v5.utils.common.LogUtils
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.UXSDKError
import dji.v5.ux.core.base.WidgetSizeDescription
import dji.v5.ux.core.base.panel.listitem.ListItemRadioButtonWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.getString
import dji.v5.ux.core.panel.listitem.rcstickmode.RCStickModeListItemWidget.ModelState
import dji.v5.ux.core.panel.listitem.rcstickmode.RCStickModeListItemWidget.ModelState.*
import dji.v5.ux.core.panel.listitem.rcstickmode.RCStickModeListItemWidgetModel.RCStickModeState

private const val TAG = "RCStickModeListItemWidget"

/**
 * Widget shows the various options for RC Stick mode.
 * The current mode is shown selected. Tapping on another mode will
 * change the mode.
 */
open class RCStickModeListItemWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ListItemRadioButtonWidget<ModelState>(
        context,
        attrs,
        defStyleAttr,
        R.style.UXSDKRCStickModeListItem
) {

    //region Fields
    private val widgetModel by lazy {
        RCStickModeListItemWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
    }

    private var mode1ItemIndex: Int = INVALID_OPTION_INDEX
    private var mode2ItemIndex: Int = INVALID_OPTION_INDEX
    private var mode3ItemIndex: Int = INVALID_OPTION_INDEX
    private var modeCustomItemIndex: Int = INVALID_OPTION_INDEX
    //endregion

    //region Constructor
    init {
        mode1ItemIndex = addOptionToGroup(getString(R.string.uxsdk_rc_stick_mode_jp))
        mode2ItemIndex = addOptionToGroup(getString(R.string.uxsdk_rc_stick_mode_usa))
        mode3ItemIndex = addOptionToGroup(getString(R.string.uxsdk_rc_stick_mode_ch))
    }
    //endregion

    //region Lifecycle
    override fun reactToModelChanges() {
        addReaction(widgetModel.rcStickModeState
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

    override fun onOptionTapped(optionIndex: Int, optionLabel: String) {
        val rcStickModeState = when (optionIndex) {
            mode1ItemIndex -> {
                ControlMode.JP
            }
            mode2ItemIndex -> {
                ControlMode.USA
            }
            mode3ItemIndex -> {
                ControlMode.CH
            }
            modeCustomItemIndex -> {
                ControlMode.CUSTOM
            }
            else -> return
        }
        addDisposable(widgetModel.setControlStickMode(rcStickModeState)
                .observeOn(SchedulerProvider.ui())
                .subscribe({
                    widgetStateDataProcessor.onNext(SetRCStickModeSucceeded)
                }) {
                    widgetStateDataProcessor.onNext(SetRCStickModeFailed(it as UXSDKError))
                    LogUtils.d(TAG, "failed " + it.djiError.description())
                })
    }
    //endregion

    //region Reaction to model
    private fun updateUI(rcStickModeState: RCStickModeState) {
        widgetStateDataProcessor.onNext(RCStickModeUpdated(rcStickModeState))
        when (rcStickModeState) {
            RCStickModeState.ProductDisconnected -> {
                isEnabled = false
            }
            RCStickModeState.JP -> {
                isEnabled = true
                setSelected(mode1ItemIndex)
            }
            RCStickModeState.USA -> {
                isEnabled = true
                setSelected(mode2ItemIndex)
            }
            RCStickModeState.CH -> {
                isEnabled = true
                setSelected(mode3ItemIndex)
            }
            is RCStickModeState.Custom -> {
                isEnabled = true
                setSelected(modeCustomItemIndex)
            }
        }
    }
    //endregion

    //region Customization
    override fun getIdealDimensionRatioString(): String? {
        return null
    }

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
        data class ProductConnected(val isConnected: Boolean) : ModelState()

        /**
         * Set RC stick mode success
         */
        object SetRCStickModeSucceeded : ModelState()

        /**
         * Set RC stick mode failed
         */
        data class SetRCStickModeFailed(val error: UXSDKError) : ModelState()

        /**
         * RC stick mode state updated
         */
        data class RCStickModeUpdated(val rcStickModeState: RCStickModeState) : ModelState()

    }
    //endregion
}