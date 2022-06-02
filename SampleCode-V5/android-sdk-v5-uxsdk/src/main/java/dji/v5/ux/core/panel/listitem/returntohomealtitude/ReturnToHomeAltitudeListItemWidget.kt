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

package dji.v5.ux.core.panel.listitem.returntohomealtitude

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
import dji.v5.ux.core.base.panel.listitem.ListItemEditTextButtonWidget.UIState.DialogDismissed
import dji.v5.ux.core.base.panel.listitem.ListItemEditTextButtonWidget.UIState.DialogDisplayed
import dji.v5.ux.core.communication.GlobalPreferencesManager
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.*
import dji.v5.ux.core.panel.listitem.returntohomealtitude.ReturnToHomeAltitudeListItemWidget.DialogType.MaxAltitudeExceeded
import dji.v5.ux.core.panel.listitem.returntohomealtitude.ReturnToHomeAltitudeListItemWidget.ModelState
import dji.v5.ux.core.panel.listitem.returntohomealtitude.ReturnToHomeAltitudeListItemWidget.ModelState.ProductConnected
import dji.v5.ux.core.panel.listitem.returntohomealtitude.ReturnToHomeAltitudeListItemWidget.ModelState.ReturnToHomeAltitudeStateUpdated
import dji.v5.ux.core.panel.listitem.returntohomealtitude.ReturnToHomeAltitudeListItemWidgetModel.ReturnToHomeAltitudeState
import dji.v5.ux.core.panel.listitem.returntohomealtitude.ReturnToHomeAltitudeListItemWidgetModel.ReturnToHomeAltitudeState.ReturnToHomeAltitudeValue
import dji.v5.ux.core.util.UnitConversionUtil.UnitType

private const val TAG = "RTHAltitudeListItem"

/**
 * Widget shows the current return to home altitude.
 * The widget enables the user to modify this value. Tap on the limit and enter
 * the new value to modify the return home altitude.
 * The return to home altitude cannot exceed the maximum flight altitude.
 */
open class ReturnToHomeAltitudeListItemWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleArr: Int = 0
) : ListItemEditTextButtonWidget<ModelState>(
    context,
    attrs,
    defStyleArr,
    WidgetType.EDIT,
    R.style.UXSDKReturnToHomeAltitudeListItem
) {

    //region Fields
    /**
     * Enable/Disable toast messages in the widget
     */
    var toastMessagesEnabled: Boolean = true

    /**
     * Theme for the dialogs shown in the widget
     */
    @StyleRes
    var dialogTheme: Int = R.style.UXSDKDialogTheme

    /**
     * Icon for error dialog
     */
    var errorDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_alert_error)

    /**
     * Icon for success dialog
     */
    var successDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_alert_good)

    private val widgetModel: ReturnToHomeAltitudeListItemWidgetModel by lazy {
        ReturnToHomeAltitudeListItemWidgetModel(
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
        addReaction(widgetModel.returnToHomeAltitudeState
            .observeOn(SchedulerProvider.ui())
            .subscribe {
                widgetStateDataProcessor.onNext(ReturnToHomeAltitudeStateUpdated(it))
                this.updateUI(it)
            })
        addReaction(widgetModel.productConnection
            .observeOn(SchedulerProvider.ui())
            .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
    }

    override fun onButtonClick() {
        // Do nothing
    }

    override fun onKeyboardDoneAction() {
        val currentValue = listItemEditTextValue?.toIntOrNull()
        if (currentValue != null
            && widgetModel.isInputInRange(currentValue)) {
            addDisposable(
                widgetModel.returnToHomeAltitudeState.firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe({
                        if (it is ReturnToHomeAltitudeValue) {
                            if (it.maxFlightAltitude < currentValue) {
                                val dialogDismissListener = DialogInterface.OnDismissListener {
                                    uiUpdateStateProcessor.onNext(DialogDismissed(MaxAltitudeExceeded))
                                }
                                showAlertDialog(
                                    dialogTheme = dialogTheme,
                                    icon = errorDialogIcon,
                                    title = getString(R.string.uxsdk_list_rth_dialog_title),
                                    message = getString(R.string.uxsdk_rth_error_dialog_message, it.maxFlightAltitude),
                                    dialogDismissListener = dialogDismissListener
                                )
                                uiUpdateStateProcessor.onNext(DialogDisplayed(MaxAltitudeExceeded))
                                resetToDefaultValue()
                            } else {
                                setReturnToHomeAltitude(currentValue)
                            }
                        }
                    }, {
                        resetToDefaultValue()
                        LogUtils.d(TAG, it.message)
                    })
            )

        } else {
            resetToDefaultValue()
            showToast(getString(R.string.uxsdk_list_item_value_out_of_range))
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
    private fun updateUI(returnToHomeAltitudeState: ReturnToHomeAltitudeState) {
        when (returnToHomeAltitudeState) {
            ReturnToHomeAltitudeState.ProductDisconnected -> updateProductDisconnectedState()
            is ReturnToHomeAltitudeState.NoviceMode -> updateNoviceMode(returnToHomeAltitudeState.unitType)
            is ReturnToHomeAltitudeValue -> updateReturnToHomeValue(returnToHomeAltitudeState)
        }
    }

    private fun updateReturnToHomeValue(returnToHomeAltitudeListItemState: ReturnToHomeAltitudeValue) {
        listItemEditTextVisibility = true
        listItemHintVisibility = true
        listItemHint = if (returnToHomeAltitudeListItemState.unitType == UnitType.METRIC) {
            getString(R.string.uxsdk_altitude_range_meters, returnToHomeAltitudeListItemState.minLimit, returnToHomeAltitudeListItemState.maxLimit)
        } else {
            getString(R.string.uxsdk_altitude_range_feet, returnToHomeAltitudeListItemState.minLimit, returnToHomeAltitudeListItemState.maxLimit)
        }
        listItemEditTextValue = returnToHomeAltitudeListItemState.returnToHomeAltitude.toString()
        isEnabled = true
        listItemEditTextColor = editTextNormalColor
    }

    private fun updateProductDisconnectedState() {
        listItemEditTextVisibility = true
        listItemHintVisibility = false
        listItemEditTextValue = getString(R.string.uxsdk_string_default_value)
        isEnabled = false
        listItemEditTextColor = disconnectedValueColor
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
    //endregion

    //region Helpers
    private fun showToast(message: String?) {
        if (toastMessagesEnabled) {
            showShortToast(message)
        }
    }

    private fun setReturnToHomeAltitude(currentValue: Int) {
        addDisposable(
            widgetModel.setReturnToHomeAltitude(currentValue)
                .observeOn(SchedulerProvider.ui())
                .subscribe({
                    val dialogDismissListener = DialogInterface.OnDismissListener {
                        uiUpdateStateProcessor.onNext(DialogDismissed(DialogType.ReturnHomeAltitudeChangeConfirmation))
                    }
                    showAlertDialog(
                        dialogTheme = dialogTheme,
                        icon = successDialogIcon,
                        title = getString(R.string.uxsdk_list_rth_dialog_title),
                        message = getString(R.string.uxsdk_rth_success_dialog_message),
                        dialogDismissListener = dialogDismissListener
                    )
                    uiUpdateStateProcessor.onNext(DialogDisplayed(DialogType.ReturnHomeAltitudeChangeConfirmation))
                    widgetStateDataProcessor.onNext(ModelState.SetReturnToHomeAltitudeSucceeded)
                }, { error ->
                    if (error is UXSDKError) {
                        showToast(error.djiError.description())
                        widgetStateDataProcessor.onNext(ModelState.SetReturnToHomeAltitudeFailed(error))
                        LogUtils.e(TAG, error.djiError.description())
                    }
                })
        )
    }

    private fun resetToDefaultValue() {
        addDisposable(
            widgetModel.returnToHomeAltitudeState.firstOrError()
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
    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.ReturnToHomeAltitudeListItemWidget, 0, defaultStyle).use { typedArray ->
            toastMessagesEnabled = typedArray.getBoolean(R.styleable.ReturnToHomeAltitudeListItemWidget_uxsdk_toast_messages_enabled, toastMessagesEnabled)
            typedArray.getResourceIdAndUse(R.styleable.ReturnToHomeAltitudeListItemWidget_uxsdk_list_item_dialog_theme) {
                dialogTheme = it
            }
            typedArray.getDrawableAndUse(R.styleable.ReturnToHomeAltitudeListItemWidget_uxsdk_list_item_error_dialog_icon) {
                errorDialogIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.ReturnToHomeAltitudeListItemWidget_uxsdk_list_item_success_dialog_icon) {
                successDialogIcon = it
            }
        }
    }

    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    override val widgetSizeDescription: WidgetSizeDescription =
        WidgetSizeDescription(
            WidgetSizeDescription.SizeType.OTHER,
            widthDimension = WidgetSizeDescription.Dimension.EXPAND,
            heightDimension = WidgetSizeDescription.Dimension.WRAP
        )
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
     * Return to home list item dialog identifiers
     */
    sealed class DialogType {
        /**
         * Dialog shown when return to home altitude
         * exceeds max altitude limit
         */
        object MaxAltitudeExceeded : DialogType()

        /**
         * Dialog shown to warn user when return to home altitude is
         * updated successfully
         */
        object ReturnHomeAltitudeChangeConfirmation : DialogType()

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
         * Return to home altitude set action successful
         */
        object SetReturnToHomeAltitudeSucceeded : ModelState()

        /**
         * Return to home altitude set action failed
         */
        data class SetReturnToHomeAltitudeFailed(val error: UXSDKError) : ModelState()

        /**
         * Return to home altitude state updated
         */
        data class ReturnToHomeAltitudeStateUpdated(val maxAltitudeState: ReturnToHomeAltitudeState) :
            ModelState()
    }
    //endregion

}