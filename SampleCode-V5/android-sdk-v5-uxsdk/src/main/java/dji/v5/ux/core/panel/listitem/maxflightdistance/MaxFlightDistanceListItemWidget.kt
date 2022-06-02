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

package dji.v5.ux.core.panel.listitem.maxflightdistance

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.use
import dji.v5.utils.common.LogUtils
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.UXSDKError
import dji.v5.ux.core.base.WidgetSizeDescription
import dji.v5.ux.core.base.panel.listitem.ListItemEditTextButtonWidget
import dji.v5.ux.core.communication.GlobalPreferencesManager
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.getString
import dji.v5.ux.core.extension.getStringAndUse
import dji.v5.ux.core.extension.showShortToast
import dji.v5.ux.core.panel.listitem.maxflightdistance.MaxFlightDistanceListItemWidget.ModelState
import dji.v5.ux.core.panel.listitem.maxflightdistance.MaxFlightDistanceListItemWidget.ModelState.MaxFlightDistanceStateUpdated
import dji.v5.ux.core.panel.listitem.maxflightdistance.MaxFlightDistanceListItemWidget.ModelState.ProductConnected
import dji.v5.ux.core.panel.listitem.maxflightdistance.MaxFlightDistanceListItemWidgetModel.MaxFlightDistanceState
import dji.v5.ux.core.panel.listitem.maxflightdistance.MaxFlightDistanceListItemWidgetModel.MaxFlightDistanceState.MaxFlightDistanceValue
import dji.v5.ux.core.util.UnitConversionUtil.UnitType

private const val TAG = "MaxFlightDistanceItem"

/**
 * Widget shows the current flight distance limit.
 * Based on the product connected and the mode that it currently is in,
 * the widget will allow the user to update the flight distance limit.
 * Tap on the limit and enter the new value to modify the flight distance  limit.
 */
open class MaxFlightDistanceListItemWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleArr: Int = 0
) : ListItemEditTextButtonWidget<ModelState>(
        context,
        attrs,
        defStyleArr,
        WidgetType.EDIT_BUTTON,
        R.style.UXSDKMaxFlightDistanceListItem
) {

    //region Fields
    private val widgetModel: MaxFlightDistanceListItemWidgetModel by lazy {
        MaxFlightDistanceListItemWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance(),
                GlobalPreferencesManager.getInstance())
    }

    /**
     * String for enable action button
     */
    var enableActionButtonString: String = getString(R.string.uxsdk_enable)

    /**
     * String for disable action button
     */
    var disableActionButtonString: String = getString(R.string.uxsdk_disable)

    /**
     * Enable/Disable toast messages in the widget
     */
    var toastMessagesEnabled: Boolean = true

    //endregion

    //region Constructor
    init {
        initAttributes(context, attrs)
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

    override fun onButtonClick() {
        addDisposable(widgetModel.toggleFlightDistanceAvailability()
                .observeOn(SchedulerProvider.ui())
                .subscribe({
                }, { error ->
                    if (error is UXSDKError) {
                        showToast(error.djiError.description())
                    }
                }))
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.maxFlightDistanceState
                .observeOn(SchedulerProvider.ui())
                .subscribe {
                    widgetStateDataProcessor.onNext(MaxFlightDistanceStateUpdated(it))
                    updateUI(it)
                })
        addReaction(widgetModel.productConnection
                .observeOn(SchedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
    }

    override fun onKeyboardDoneAction() {
        val currentValue = listItemEditTextValue?.toIntOrNull()
        if (currentValue == null || !widgetModel.isInputInRange(currentValue)) {
            showToast(getString(R.string.uxsdk_list_item_value_out_of_range))
            resetToDefaultValue()
        } else {
            setMaxFlightDistance(currentValue)
        }
    }

    override fun onEditorTextChanged(currentText: String?) {
        listItemEditTextColor = if (!currentText.isNullOrBlank()
                && currentText.toIntOrNull() != null
                && widgetModel.isInputInRange(currentText.toInt())) {
            editTextNormalColor
        } else {
            errorValueColor
        }
    }
    //endregion

    //region Reactions to model
    private fun updateUI(maxFlightDistanceState: MaxFlightDistanceState) {
        when (maxFlightDistanceState) {
            MaxFlightDistanceState.ProductDisconnected -> updateProductDisconnectedState()
            MaxFlightDistanceState.Disabled -> updateDisabledState()
            is MaxFlightDistanceState.NoviceMode -> updateNoviceMode(maxFlightDistanceState.unitType)
            is MaxFlightDistanceValue -> updateMaxFlightDistance(maxFlightDistanceState)
        }
    }

    private fun updateMaxFlightDistance(maxFlightDistanceState: MaxFlightDistanceValue) {
        isEnabled = true
        listItemHintVisibility = true
        listItemEditTextVisibility = true
        listItemButtonVisibility = true
        listItemHint = if (maxFlightDistanceState.unitType == UnitType.METRIC) {
            getString(R.string.uxsdk_altitude_range_meters, maxFlightDistanceState.minDistanceLimit, maxFlightDistanceState.maxDistanceLimit)
        } else {
            getString(R.string.uxsdk_altitude_range_feet, maxFlightDistanceState.minDistanceLimit, maxFlightDistanceState.maxDistanceLimit)
        }
        listItemEditTextValue = maxFlightDistanceState.flightDistanceLimit.toString()
        listItemEditTextColor = editTextNormalColor
        listItemButtonText = disableActionButtonString
    }

    private fun updateNoviceMode(unitType: UnitType) {
        listItemEditTextVisibility = true
        listItemHintVisibility = false
        listItemButtonVisibility = false
        listItemEditTextValue = if (unitType == UnitType.METRIC) {
            getString(R.string.uxsdk_novice_mode_distance_meters)
        } else {
            getString(R.string.uxsdk_novice_mode_distance_feet)
        }
        isEnabled = false
        listItemEditTextColor = disconnectedValueColor
    }

    private fun updateDisabledState() {
        isEnabled = true
        listItemHintVisibility = false
        listItemEditTextVisibility = false
        listItemButtonVisibility = true
        listItemButtonText = enableActionButtonString
    }

    private fun updateProductDisconnectedState() {
        listItemEditTextVisibility = true
        listItemHintVisibility = false
        listItemButtonVisibility = false
        listItemEditTextValue = getString(R.string.uxsdk_string_default_value)
        listItemEditTextColor = disconnectedValueColor
        listItemButtonText = getString(R.string.uxsdk_string_default_value)
        isEnabled = false

    }
    //endregion

    //region Helpers
    private fun showToast(message: String?) {
        if (toastMessagesEnabled) {
            showShortToast(message)
        }
    }

    private fun resetToDefaultValue() {
        addDisposable(widgetModel.maxFlightDistanceState.firstOrError()
                .observeOn(SchedulerProvider.ui())
                .subscribe({
                    updateUI(it)
                }, {
                    LogUtils.e(TAG, it.message)
                }))
    }

    private fun setMaxFlightDistance(maxFlightDistance: Int) {
        addDisposable(widgetModel.setMaxFlightDistance(maxFlightDistance)
                .observeOn(SchedulerProvider.ui())
                .subscribe({
                    showToast(getString(R.string.uxsdk_success))
                    widgetStateDataProcessor.onNext(ModelState.SetMaxFlightDistanceSucceeded)
                }, {
                    if (it is UXSDKError) {
                        showToast(it.djiError.description())
                        widgetStateDataProcessor.onNext(ModelState.SetMaxFlightDistanceFailed(it))
                    }
                    resetToDefaultValue()
                }))
    }
    //endregion

    //region Customization
    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.MaxFlightDistanceListItemWidget, 0, defaultStyle).use { typedArray ->
            toastMessagesEnabled = typedArray.getBoolean(R.styleable.MaxFlightDistanceListItemWidget_uxsdk_toast_messages_enabled, toastMessagesEnabled)
            typedArray.getStringAndUse(R.styleable.MaxFlightDistanceListItemWidget_uxsdk_enable_action_button_string) {
                enableActionButtonString = it
            }
            typedArray.getStringAndUse(R.styleable.MaxFlightDistanceListItemWidget_uxsdk_disable_action_button_string) {
                disableActionButtonString = it
            }
        }
    }

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
         * Max flight distance set action successful
         */
        object SetMaxFlightDistanceSucceeded : ModelState()

        /**
         * Max flight distance set action failed
         */
        data class SetMaxFlightDistanceFailed(val error: UXSDKError) : ModelState()

        /**
         * Max flight distance state updated
         */
        data class MaxFlightDistanceStateUpdated(val maxFlightDistanceState: MaxFlightDistanceState) : ModelState()
    }
    //endregion
}