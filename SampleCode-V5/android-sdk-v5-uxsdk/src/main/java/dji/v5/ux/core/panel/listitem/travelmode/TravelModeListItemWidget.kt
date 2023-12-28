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

package dji.v5.ux.core.panel.listitem.travelmode

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.StyleRes
import androidx.core.content.res.use
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.UXSDKError
import dji.v5.ux.core.base.WidgetSizeDescription
import dji.v5.ux.core.base.panel.listitem.ListItemLabelButtonWidget
import dji.v5.ux.core.base.panel.listitem.ListItemLabelButtonWidget.UIState.*
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.*
import dji.v5.ux.core.panel.listitem.travelmode.TravelModeListItemWidget.DialogType.*
import dji.v5.ux.core.panel.listitem.travelmode.TravelModeListItemWidget.ModelState
import dji.v5.ux.core.panel.listitem.travelmode.TravelModeListItemWidget.ModelState.ProductConnected
import dji.v5.ux.core.panel.listitem.travelmode.TravelModeListItemWidget.ModelState.TravelModeStateUpdated
import dji.v5.ux.core.panel.listitem.travelmode.TravelModeListItemWidgetModel.TravelModeState

/**
 * Travel Mode List Item
 *
 */
open class TravelModeListItemWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ListItemLabelButtonWidget<ModelState>(
        context,
        attrs,
        defStyleAttr,
        WidgetType.BUTTON,
        R.style.UXSDKTravelModeListItem
) {

    //region Fields
    /**
     * Icon when landing gear is not in travel mode
     */
    var travelModeActiveIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_travel_mode_active)

    /**
     * Icon when landing gear is not in travel mode
     */
    var travelModeInactiveIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_travel_mode_inactive)

    /**
     * Label for button when landing gear is in travel mode
     */
    var exitTravelModeButtonString: String = getString(R.string.uxsdk_travel_mode_exit)

    /**
     * Label for button when landing gear is not in travel mode
     */
    var enterTravelModeButtonString: String = getString(R.string.uxsdk_travel_mode_enter)

    /**
     * Icon for confirmation dialog
     */
    var confirmationDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_alert_yellow)

    /**
     * Icon for error dialog
     */
    var errorDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_alert_error)

    /**
     * Icon for success dialog
     */
    var successDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_alert_good)

    /**
     * Theme for the dialogs shown
     */
    @StyleRes
    var dialogTheme: Int = R.style.UXSDKDialogTheme

    private val widgetModel: TravelModeListItemWidgetModel by lazy {
        TravelModeListItemWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
    }
    //endregion

    //region Constructor
    init {
        initAttributes(context, attrs)
        listItemTitleIcon = travelModeInactiveIcon
    }
    //endregion

    //region Lifecycle
    override fun reactToModelChanges() {
        addReaction(widgetModel.travelModeState
                .observeOn(SchedulerProvider.ui())
                .subscribe { this.updateUI(it) })
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

    override fun onButtonClick() {
        checkAndToggleTravelMode()
    }
    //endregion

    //region Reactions to model
    private fun updateUI(travelModeState: TravelModeState) {
        widgetStateDataProcessor.onNext(TravelModeStateUpdated(travelModeState))
        when (travelModeState) {
            TravelModeState.ProductDisconnected,
            TravelModeState.NotSupported -> {
                isEnabled = false
                listItemTitleIcon = travelModeInactiveIcon
                listItemButtonText = getString(R.string.uxsdk_string_default_value)
            }
            TravelModeState.Active -> {
                isEnabled = true
                listItemTitleIcon = travelModeActiveIcon
                listItemButtonText = exitTravelModeButtonString
            }
            TravelModeState.Inactive -> {
                isEnabled = true
                listItemTitleIcon = travelModeInactiveIcon
                listItemButtonText = enterTravelModeButtonString
            }
        }

    }

    //endregion

    //region Helpers
    private fun checkAndToggleTravelMode() {
        addDisposable(widgetModel.travelModeState.firstOrError()
                .observeOn(SchedulerProvider.ui())
                .subscribe({
                    when (it) {
                        TravelModeState.Inactive -> {
                            showEnterTravelModeConfirmationDialog()
                        }
                        TravelModeState.Active -> {
                            exitTravelMode()
                        }
                        else -> {
                            //do something
                        }
                    }
                }, { }))
    }

    private fun showEnterTravelModeConfirmationDialog() {
        val dialogListener = DialogInterface.OnClickListener { dialogInterface, buttonId: Int ->
            if (buttonId == DialogInterface.BUTTON_POSITIVE) {
                enterTravelMode()
                uiUpdateStateProcessor.onNext(DialogActionConfirmed(EnterTravelModeConfirmation))
            } else {
                uiUpdateStateProcessor.onNext(DialogActionCanceled(EnterTravelModeConfirmation))
            }
            dialogInterface.dismiss()
        }
        val dialogDismissListener = DialogInterface.OnDismissListener {
            uiUpdateStateProcessor.onNext(DialogDismissed(EnterTravelModeConfirmation))
        }
        showConfirmationDialog(title = getString(R.string.uxsdk_list_item_travel_mode),
                icon = confirmationDialogIcon,
                dialogTheme = dialogTheme,
                message = getString(R.string.uxsdk_travel_mode_enter_confirmation),
                dialogClickListener = dialogListener,
                dialogDismissListener = dialogDismissListener)
        uiUpdateStateProcessor.onNext(DialogDisplayed(EnterTravelModeConfirmation))
    }

    private fun exitTravelMode() {
        addDisposable(widgetModel.exitTravelMode()
                .observeOn(SchedulerProvider.ui())
                .subscribe(
                        { },
                        { error ->
                            if (error is UXSDKError) {
                                val dialogDismissListener = DialogInterface.OnDismissListener {
                                    uiUpdateStateProcessor.onNext(DialogDismissed(ExitTravelModeError))
                                }
                                showAlertDialog(title = getString(R.string.uxsdk_list_item_travel_mode),
                                        icon = errorDialogIcon,
                                        dialogTheme = dialogTheme,
                                        message = getString(R.string.uxsdk_exit_travel_mode_failed, error.djiError.description()),
                                        dialogDismissListener = dialogDismissListener)
                                uiUpdateStateProcessor.onNext(DialogDisplayed(ExitTravelModeError))
                            }
                        }
                ))

    }

    private fun enterTravelMode() {
        var dialogType: DialogType? = null
        val dialogDismissListener = DialogInterface.OnDismissListener {
            uiUpdateStateProcessor.onNext(DialogDismissed(dialogType))
        }
        addDisposable(widgetModel.enterTravelMode()
                .observeOn(SchedulerProvider.ui())
                .subscribe(
                        {
                            dialogType = EnterTravelModeSuccess
                            showAlertDialog(title = getString(R.string.uxsdk_list_item_travel_mode),
                                    icon = successDialogIcon,
                                    dialogTheme = dialogTheme,
                                    message = getString(R.string.uxsdk_enter_travel_mode_success),
                                    dialogDismissListener = dialogDismissListener)
                            uiUpdateStateProcessor.onNext(DialogDisplayed(dialogType))
                        },
                        { error ->
                            if (error is UXSDKError) {
                                dialogType = EnterTravelModeError
                                showAlertDialog(title = getString(R.string.uxsdk_list_item_travel_mode),
                                        icon = errorDialogIcon,
                                        dialogTheme = dialogTheme,
                                        message = getString(R.string.uxsdk_enter_travel_mode_failed, error.djiError.description()),
                                        dialogDismissListener = dialogDismissListener)
                                uiUpdateStateProcessor.onNext(DialogDisplayed(dialogType))
                            }
                        }
                ))

    }


    //endregion

    //region Customization
    override fun getIdealDimensionRatioString(): String? = null


    override val widgetSizeDescription: WidgetSizeDescription =
            WidgetSizeDescription(WidgetSizeDescription.SizeType.OTHER,
                    widthDimension = WidgetSizeDescription.Dimension.EXPAND,
                    heightDimension = WidgetSizeDescription.Dimension.WRAP)


    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.TravelModeListItemWidget, 0, defaultStyle).use { typedArray ->
            typedArray.getDrawableAndUse(R.styleable.TravelModeListItemWidget_uxsdk_travel_mode_active_icon) {
                travelModeActiveIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.TravelModeListItemWidget_uxsdk_travel_mode_inactive_icon) {
                travelModeInactiveIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.TravelModeListItemWidget_uxsdk_list_item_confirmation_dialog_icon) {
                confirmationDialogIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.TravelModeListItemWidget_uxsdk_list_item_error_dialog_icon) {
                errorDialogIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.TravelModeListItemWidget_uxsdk_list_item_success_dialog_icon) {
                successDialogIcon = it
            }

            enterTravelModeButtonString = typedArray.getString(R.styleable.TravelModeListItemWidget_uxsdk_enter_travel_mode_button_string, enterTravelModeButtonString)
            exitTravelModeButtonString = typedArray.getString(R.styleable.TravelModeListItemWidget_uxsdk_exit_travel_mode_button_string, exitTravelModeButtonString)
            typedArray.getResourceIdAndUse(R.styleable.TravelModeListItemWidget_uxsdk_list_item_dialog_theme) {
                dialogTheme = it
            }

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
     * Get the [ListItemLabelButtonWidget.UIState] updates
     * The info parameter is instance of [DialogType]
     */
    override fun getUIStateUpdates(): Flowable<UIState> {
        return uiUpdateStateProcessor.onBackpressureBuffer()
    }

    /**
     * Travel mode List Item Dialog Identifiers
     */
    sealed class DialogType {
        /**
         * Dialog shown for confirmation to enter travel mode
         */
        object EnterTravelModeConfirmation : DialogType()

        /**
         * Dialog shown when entering travel mode success
         */
        object EnterTravelModeSuccess : DialogType()

        /**
         * Dialog shown when entering travel mode fails
         */
        object EnterTravelModeError : DialogType()

        /**
         * Dialog shown when exiting travel mode fails
         */
        object ExitTravelModeError : DialogType()

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
         * Travel mode state updated
         */
        data class TravelModeStateUpdated(val travelModeState: TravelModeState) : ModelState()

    }
    //endregion
}
