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

package dji.v5.ux.core.panel.listitem.maxaltitude

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.StyleRes
import androidx.core.content.res.use
import dji.v5.utils.common.LogUtils
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.UXSDKError
import dji.v5.ux.core.base.WidgetSizeDescription
import dji.v5.ux.core.base.panel.listitem.ListItemEditTextButtonWidget
import dji.v5.ux.core.base.panel.listitem.ListItemEditTextButtonWidget.UIState.*
import dji.v5.ux.core.communication.GlobalPreferencesManager
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.*
import dji.v5.ux.core.panel.listitem.maxaltitude.MaxAltitudeListItemWidget.DialogType.*
import dji.v5.ux.core.panel.listitem.maxaltitude.MaxAltitudeListItemWidget.ModelState
import dji.v5.ux.core.panel.listitem.maxaltitude.MaxAltitudeListItemWidget.ModelState.MaxAltitudeStateUpdated
import dji.v5.ux.core.panel.listitem.maxaltitude.MaxAltitudeListItemWidget.ModelState.ProductConnected
import dji.v5.ux.core.panel.listitem.maxaltitude.MaxAltitudeListItemWidgetModel.MaxAltitudeState
import dji.v5.ux.core.util.UnitConversionUtil.*
import kotlin.math.roundToInt

private const val TAG = "MaxAltitudeListItemWidget"
private const val ALARM_LIMIT_METRIC = 120
private const val ALARM_LIMIT_IMPERIAL = 400

/**
 * Widget shows the current flight height limit.
 * Based on the product connected and the mode that it currently is in,
 * the widget will allow the user to update the flight height limit.
 * Tap on the limit and enter the new value to modify the flight height limit.
 */
open class MaxAltitudeListItemWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleArr: Int = 0
) : ListItemEditTextButtonWidget<ModelState>(
    context,
    attrs,
    defStyleArr,
    WidgetType.EDIT,
    R.style.UXSDKMaxAltitudeListItem
) {

    //region Fields
    /**
     * Icon for confirmation dialog
     */
    var confirmationDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_alert_yellow)

    /**
     * Icon for error dialog
     */
    var errorDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_alert_error)

    /**
     * Enable/Disable toast messages in the widget
     */
    var toastMessagesEnabled: Boolean = true

    /**
     * Theme for the dialogs shown in the widget
     */
    @StyleRes
    var dialogTheme: Int = R.style.UXSDKDialogTheme


    private val widgetModel: MaxAltitudeListItemWidgetModel by lazy {
        MaxAltitudeListItemWidgetModel(
            DJISDKModel.getInstance(),
            ObservableInMemoryKeyedStore.getInstance(),
            GlobalPreferencesManager.getInstance()
        )
    }
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


    override fun reactToModelChanges() {
        addReaction(widgetModel.maxAltitudeState
            .observeOn(SchedulerProvider.ui())
            .subscribe {
                widgetStateDataProcessor.onNext(MaxAltitudeStateUpdated(it))
                updateUI(it)
            })
        addReaction(widgetModel.productConnection
            .observeOn(SchedulerProvider.ui())
            .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
    }

    override fun onButtonClick() {
        // No code required
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

    override fun onKeyboardDoneAction() {
        val currentValue = listItemEditTextValue?.toIntOrNull()
        if (currentValue != null
            && widgetModel.isInputInRange(currentValue)) {
            addDisposable(
                widgetModel.maxAltitudeState.firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe({
                        if (it is MaxAltitudeState.MaxAltitudeValue) {
                            when {
                                isOverAlarmLimit(currentValue, it.unitType) -> {
                                    showOverAlarmLimitDialog(currentValue, it.returnToHomeHeight, it.unitType)
                                }
                                else -> {
                                    verifyReturnHomeAltitudeValue(currentValue, it.returnToHomeHeight, it.unitType)
                                }
                            }
                        }
                    }, {
                        LogUtils.d(TAG, it.message)
                    })
            )

        } else {
            showToast(getString(R.string.uxsdk_list_item_value_out_of_range))
            resetToDefaultValue()
        }

    }
    //endregion

    //region Reactions to model
    private fun updateUI(maxAltitudeState: MaxAltitudeState) {
        when (maxAltitudeState) {
            MaxAltitudeState.ProductDisconnected -> updateProductDisconnectedState()
            is MaxAltitudeState.NoviceMode -> updateNoviceMode(maxAltitudeState.unitType)
            is MaxAltitudeState.MaxAltitudeValue -> updateMaxAltitudeValue(maxAltitudeState)
        }
    }

    private fun updateMaxAltitudeValue(maxAltitudeListItemState: MaxAltitudeState.MaxAltitudeValue) {
        listItemEditTextVisibility = true
        listItemHintVisibility = true
        listItemHint = if (maxAltitudeListItemState.unitType == UnitType.METRIC) {
            getString(R.string.uxsdk_altitude_range_meters, maxAltitudeListItemState.minAltitudeLimit, maxAltitudeListItemState.maxAltitudeLimit)
        } else {
            getString(R.string.uxsdk_altitude_range_feet, maxAltitudeListItemState.minAltitudeLimit, maxAltitudeListItemState.maxAltitudeLimit)
        }
        listItemEditTextValue = maxAltitudeListItemState.altitudeLimit.toString()
        isEnabled = true
        listItemEditTextColor = editTextNormalColor
    }


    private fun updateNoviceMode(unitType: UnitType) {
        listItemEditTextVisibility = true
        listItemHintVisibility = false
        listItemEditTextValue = if (unitType == UnitType.METRIC) {
            getString(R.string.uxsdk_novice_mode_altitude_meters)
        } else {
            getString(R.string.uxsdk_novice_mode_altitude_feet)
        }
        isEnabled = false
        listItemEditTextColor = disconnectedValueColor
    }

    private fun updateProductDisconnectedState() {
        listItemEditTextVisibility = true
        listItemHintVisibility = false
        listItemEditTextValue = getString(R.string.uxsdk_string_default_value)
        isEnabled = false
        listItemEditTextColor = disconnectedValueColor
    }
    //endregion

    //region Helpers
    private fun showToast(message: String?) {
        if (toastMessagesEnabled) {
            showShortToast(message)
        }
    }

    private fun showOverAlarmLimitDialog(currentValue: Int, currentReturnToHomeValue: Int, unitType: UnitType) {
        val dialogListener = DialogInterface.OnClickListener { dialogInterface, buttonId: Int ->
            if (buttonId == DialogInterface.BUTTON_POSITIVE) {
                verifyReturnHomeAltitudeValue(currentValue, currentReturnToHomeValue, unitType)
                uiUpdateStateProcessor.onNext(DialogActionConfirmed(MaxAltitudeOverAlarmConfirmation))
            } else {
                uiUpdateStateProcessor.onNext(DialogActionCanceled(MaxAltitudeOverAlarmConfirmation))
                resetToDefaultValue()
            }
            dialogInterface.dismiss()

        }
        val dialogDismissListener = DialogInterface.OnDismissListener {
            uiUpdateStateProcessor.onNext(DialogDismissed(MaxAltitudeOverAlarmConfirmation))
        }
        showConfirmationDialog(
            dialogTheme = dialogTheme,
            title = getString(R.string.uxsdk_list_item_max_flight_altitude),
            icon = confirmationDialogIcon,
            message = getString(R.string.uxsdk_limit_high_notice),
            dialogClickListener = dialogListener,
            dialogDismissListener = dialogDismissListener
        )
        uiUpdateStateProcessor.onNext(DialogDisplayed(MaxAltitudeOverAlarmConfirmation))

    }

    private fun isOverAlarmLimit(currentVal: Int, unitType: UnitType): Boolean {
        return if (unitType == UnitType.METRIC) {
            currentVal > ALARM_LIMIT_METRIC
        } else {
            currentVal > ALARM_LIMIT_IMPERIAL
        }
    }

    private fun verifyReturnHomeAltitudeValue(currentValue: Int, currentReturnToHomeValue: Int, unitType: UnitType) {
        if (currentValue < currentReturnToHomeValue) {
            val metricHeight: Int = if (unitType == UnitType.METRIC) {
                currentValue
            } else {
                convertFeetToMeters(currentValue.toFloat()).roundToInt()
            }
            val imperialHeight: Int = if (unitType == UnitType.METRIC) {
                convertMetersToFeet(currentValue.toFloat()).roundToInt()
            } else {
                currentValue
            }

            val dialogListener = DialogInterface.OnClickListener { dialogInterface, buttonId: Int ->
                if (buttonId == DialogInterface.BUTTON_POSITIVE) {
                    setMaxAltitudeValue(currentValue)
                    uiUpdateStateProcessor.onNext(DialogActionConfirmed(ReturnHomeAltitudeUpdate))
                } else {
                    uiUpdateStateProcessor.onNext(DialogActionCanceled(ReturnHomeAltitudeUpdate))
                    resetToDefaultValue()
                }
                dialogInterface.dismiss()
            }

            val dialogDismissListener = DialogInterface.OnDismissListener {
                uiUpdateStateProcessor.onNext(DialogDismissed(ReturnHomeAltitudeUpdate))
            }
            showConfirmationDialog(
                dialogTheme = dialogTheme,
                icon = confirmationDialogIcon,
                title = getString(R.string.uxsdk_list_item_max_flight_altitude),
                message = getString(R.string.uxsdk_limit_return_home_warning, imperialHeight, metricHeight),
                dialogClickListener = dialogListener,
                dialogDismissListener = dialogDismissListener
            )
            uiUpdateStateProcessor.onNext(DialogDisplayed(ReturnHomeAltitudeUpdate))
        } else {
            setMaxAltitudeValue(currentValue)
        }

    }

    private fun setMaxAltitudeValue(currentValue: Int) {
        addDisposable(
            widgetModel.setFlightMaxAltitude(currentValue)
                .observeOn(SchedulerProvider.ui())
                .subscribe({
                    showToast(getString(R.string.uxsdk_success))
                    widgetStateDataProcessor.onNext(ModelState.SetMaxAltitudeSucceeded)
                }, { error ->
                    resetToDefaultValue()
                    if (error is UXSDKError) {
                        showToast(error.djiError.description())
                        widgetStateDataProcessor.onNext(ModelState.SetMaxAltitudeFailed(error))
                    }
                })
        )
    }

    private fun resetToDefaultValue() {
        addDisposable(
            widgetModel.maxAltitudeState.firstOrError()
                .observeOn(SchedulerProvider.ui())
                .subscribe({
                    updateUI(it)
                }, {
                    LogUtils.e(TAG, it.message)
                })
        )
    }
    //endregion

    //region Customization
    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    override val widgetSizeDescription: WidgetSizeDescription =
        WidgetSizeDescription(
            WidgetSizeDescription.SizeType.OTHER,
            widthDimension = WidgetSizeDescription.Dimension.EXPAND,
            heightDimension = WidgetSizeDescription.Dimension.WRAP
        )

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.MaxAltitudeListItemWidget, 0, defaultStyle).use { typedArray ->
            toastMessagesEnabled = typedArray.getBoolean(R.styleable.MaxAltitudeListItemWidget_uxsdk_toast_messages_enabled, toastMessagesEnabled)
            typedArray.getResourceIdAndUse(R.styleable.MaxAltitudeListItemWidget_uxsdk_list_item_dialog_theme) {
                dialogTheme = it
            }
            typedArray.getDrawableAndUse(R.styleable.SDCardStatusListItemWidget_uxsdk_list_item_confirmation_dialog_icon) {
                confirmationDialogIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.SDCardStatusListItemWidget_uxsdk_list_item_error_dialog_icon) {
                errorDialogIcon = it
            }
        }
    }
    //endregion

    //region Hooks
    /**
     * Get the [ListItemEditTextButtonWidget.UIState] updates
     * The info parameter is instance of [DialogType]
     */
    override fun getUIStateUpdates(): Flowable<UIState> {
        return uiUpdateStateProcessor.onBackpressureBuffer()
    }

    /**
     * Get the [ModelState] updates
     */
    @SuppressWarnings
    override fun getWidgetStateUpdate(): Flowable<ModelState> {
        return super.getWidgetStateUpdate()
    }

    /**
     * Max altitude List Item Dialog Identifiers
     */
    sealed class DialogType {
        /**
         * Dialog shown when max altitude is over alarm
         * levels.
         */
        object MaxAltitudeOverAlarmConfirmation : DialogType()

        /**
         * Dialog shown when flight limit is restricted and the user
         * tries to set a higher value
         */
        object FlightLimitNeededError : DialogType()

        /**
         * Dialog shown to confirm that the user will have to update return home
         * altitude along with max flight limit
         */
        object ReturnHomeAltitudeUpdate : DialogType()

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
         * Max altitude set action successful
         */
        object SetMaxAltitudeSucceeded : ModelState()

        /**
         * Max altitude set action failed
         */
        data class SetMaxAltitudeFailed(val error: UXSDKError) : ModelState()

        /**
         * Max altitude state update
         */
        data class MaxAltitudeStateUpdated(val maxAltitudeState: MaxAltitudeState) : ModelState()
    }

    //endregion

}