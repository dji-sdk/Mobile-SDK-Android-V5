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

package dji.v5.ux.core.panel.listitem.emmcstatus

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.StyleRes
import androidx.core.content.res.use
import dji.sdk.keyvalue.value.camera.CameraSDCardState
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
import dji.v5.ux.core.panel.listitem.emmcstatus.EMMCStatusListItemWidget.DialogType.*
import dji.v5.ux.core.panel.listitem.emmcstatus.EMMCStatusListItemWidget.ModelState
import dji.v5.ux.core.panel.listitem.emmcstatus.EMMCStatusListItemWidget.ModelState.ProductConnected
import dji.v5.ux.core.util.UnitConversionUtil.getSpaceWithUnit


/**
 *  eMMC status list item
 *
 *  It displays the remaining capacity of the eMMC (internal storage) along with
 *  any warnings / errors related to the eMMC.
 *  It provides a button to format eMMC.
 */
open class EMMCStatusListItemWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ListItemLabelButtonWidget<ModelState>(
        context,
        attrs,
        defStyleAttr,
        WidgetType.LABEL_BUTTON,
        R.style.UXSDKEMMCStatusListItem
) {

    //region Fields
    /**
     * Theme for the dialogs shown for format
     */
    @StyleRes
    var dialogTheme: Int = R.style.UXSDKDialogTheme

    /**
     * Icon for the dialog which shows format confirmation message
     */
    var formatConfirmationDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_alert_yellow)

    /**
     * Icon for the dialog which shows format success message
     */
    var formatSuccessDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_alert_good)

    /**
     * Icon for the dialog which shows format error message
     */
    var formatErrorDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_alert_error)

    private val widgetModel: EMMCStatusListItemWidgetModel by lazy {
        EMMCStatusListItemWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
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
        addReaction(widgetModel.eMMCState
                .observeOn(SchedulerProvider.ui())
                .subscribe { this.updateUI(it) })
        addReaction(widgetModel.productConnection
                .observeOn(SchedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
    }

    override fun onButtonClick() {
        val dialogListener = DialogInterface.OnClickListener { dialogInterface, buttonId: Int ->
            if (buttonId == DialogInterface.BUTTON_POSITIVE) {
                uiUpdateStateProcessor.onNext(DialogActionConfirmed(FormatConfirmation))
                formatEMMC()
            } else if (buttonId == DialogInterface.BUTTON_NEGATIVE) {
                uiUpdateStateProcessor.onNext(DialogActionCanceled(FormatConfirmation))
            }
            dialogInterface.dismiss()

        }
        val dialogDismissListener = DialogInterface.OnDismissListener {
            uiUpdateStateProcessor.onNext(DialogDismissed(FormatConfirmation))
        }

        showConfirmationDialog(title = getString(R.string.uxsdk_emmc_dialog_title),
                icon = formatConfirmationDialogIcon,
                dialogTheme = dialogTheme,
                message = getString(R.string.uxsdk_emmc_format_confirmation),
                dialogClickListener = dialogListener,
                dialogDismissListener = dialogDismissListener)
        uiUpdateStateProcessor.onNext(DialogDisplayed(FormatConfirmation))
    }
    //endregion

    //region Reaction to model
    private fun updateUI(eMMCState: EMMCStatusListItemWidgetModel.EMMCState) {
        widgetStateDataProcessor.onNext(ModelState.EMMCStateUpdated(eMMCState))
        when (eMMCState) {
            EMMCStatusListItemWidgetModel.EMMCState.ProductDisconnected,
            EMMCStatusListItemWidgetModel.EMMCState.NotSupported -> updateDisabledState(eMMCState)
            is EMMCStatusListItemWidgetModel.EMMCState.CurrentEMMCState -> {
                isEnabled = true
                listItemLabel = getEMMCMessage(eMMCState.eMMCOperationState,
                        eMMCState.remainingSpace)
                listItemLabelTextColor = getEMMCMessageColor(eMMCState.eMMCOperationState)
                listItemButtonEnabled = getFormatButtonVisibility(eMMCState.eMMCOperationState)
            }
        }
    }

    private fun updateDisabledState(eMMCState: EMMCStatusListItemWidgetModel.EMMCState) {
        listItemLabel = if (eMMCState is EMMCStatusListItemWidgetModel.EMMCState.ProductDisconnected) {
            getString(R.string.uxsdk_string_default_value)
        } else {
            getString(R.string.uxsdk_storage_status_not_supported)
        }
        listItemLabelTextColor = if (eMMCState is EMMCStatusListItemWidgetModel.EMMCState.ProductDisconnected) {
            disconnectedValueColor
        } else {
            errorValueColor
        }
        isEnabled = false
    }
    //endregion

    //region private helpers
    private fun formatEMMC() {
        var dialogType: DialogType? = null
        val dialogDismissListener = DialogInterface.OnDismissListener {
            uiUpdateStateProcessor.onNext(DialogDismissed(dialogType))
        }
        addDisposable(widgetModel.formatEMMC()
                .observeOn(SchedulerProvider.ui())
                .subscribe({
                    dialogType = FormatSuccess
                    showAlertDialog(title = getString(R.string.uxsdk_emmc_dialog_title),
                            icon = formatSuccessDialogIcon,
                            dialogTheme = dialogTheme,
                            message = getString(R.string.uxsdk_emmc_format_complete),
                            dialogDismissListener = dialogDismissListener)
                    uiUpdateStateProcessor.onNext(DialogDisplayed(dialogType))
                }) { error ->
                    if (error is UXSDKError) {
                        dialogType = FormatError
                        showAlertDialog(
                            title = getString(R.string.uxsdk_emmc_dialog_title),
                            icon = formatErrorDialogIcon,
                            dialogTheme = dialogTheme,
                            message = getString(R.string.uxsdk_emmc_format_error, error.djiError.description()),
                            dialogDismissListener = dialogDismissListener
                        )
                        uiUpdateStateProcessor.onNext(DialogDisplayed(dialogType))
                    }
                })

    }

    private fun getFormatButtonVisibility(eMMCOperationState: CameraSDCardState): Boolean {
        return when (eMMCOperationState) {
            CameraSDCardState.NORMAL,
            CameraSDCardState.FORMAT_RECOMMENDED,
            CameraSDCardState.FULL,
            CameraSDCardState.SLOW,
            CameraSDCardState.WRITING_SLOWLY,
            CameraSDCardState.INVALID_FILE_SYSTEM,
            CameraSDCardState.FORMAT_NEEDED -> true
            else -> false
        }
    }

    private fun getEMMCMessageColor(eMMCOperationState: CameraSDCardState): Int {
        return when (eMMCOperationState) {
            CameraSDCardState.NORMAL -> normalValueColor
            else -> warningValueColor
        }
    }

    private fun getEMMCMessage(eMMCOperationState: CameraSDCardState,
                               space: Int): String? {
        return when (eMMCOperationState) {
            CameraSDCardState.NORMAL -> getSpaceWithUnit(context, space)
            CameraSDCardState.NOT_INSERTED -> getString(R.string.uxsdk_storage_status_missing)
            CameraSDCardState.INVALID -> getString(R.string.uxsdk_storage_status_invalid)
            CameraSDCardState.READ_ONLY -> getString(R.string.uxsdk_storage_status_write_protect)
            CameraSDCardState.INVALID_FILE_SYSTEM,
            CameraSDCardState.FORMAT_NEEDED -> getString(R.string.uxsdk_storage_status_needs_formatting)
            CameraSDCardState.FORMATTING -> getString(R.string.uxsdk_storage_status_formatting)
            CameraSDCardState.BUSY -> getString(R.string.uxsdk_storage_status_busy)
            CameraSDCardState.FULL -> getString(R.string.uxsdk_storage_status_full)
            CameraSDCardState.SLOW -> getString(R.string.uxsdk_storage_status_slow, getSpaceWithUnit(context, space))
            CameraSDCardState.NO_REMAINING_FILE_INDICES -> getString(R.string.uxsdk_storage_status_file_indices)
            CameraSDCardState.INITIALIZING -> getString(R.string.uxsdk_storage_status_initial)
            CameraSDCardState.FORMAT_RECOMMENDED -> getString(R.string.uxsdk_storage_status_formatting_recommended)
            CameraSDCardState.RECOVERING_FILES -> getString(R.string.uxsdk_storage_status_recover_file)
            CameraSDCardState.WRITING_SLOWLY -> getString(R.string.uxsdk_storage_status_write_slow)
            CameraSDCardState.USB_CONNECTED -> getString(R.string.uxsdk_storage_status_usb_connected)
            CameraSDCardState.UNKNOWN_ERROR -> getString(R.string.uxsdk_storage_status_unknown_error)
            CameraSDCardState.UNKNOWN -> getString(R.string.uxsdk_string_default_value)
        }

    }
    //endregion

    //region Customization
    override val widgetSizeDescription: WidgetSizeDescription =
            WidgetSizeDescription(WidgetSizeDescription.SizeType.OTHER,
                    widthDimension = WidgetSizeDescription.Dimension.EXPAND,
                    heightDimension = WidgetSizeDescription.Dimension.WRAP)

    override fun getIdealDimensionRatioString(): String? {
        return null
    }


    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.EMMCStatusListItemWidget, 0, defaultStyle).use { typedArray ->
            typedArray.getDrawableAndUse(R.styleable.EMMCStatusListItemWidget_uxsdk_list_item_confirmation_dialog_icon) {
                formatConfirmationDialogIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.EMMCStatusListItemWidget_uxsdk_list_item_success_dialog_icon) {
                formatSuccessDialogIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.EMMCStatusListItemWidget_uxsdk_list_item_error_dialog_icon) {
                formatErrorDialogIcon = it
            }
            typedArray.getResourceIdAndUse(R.styleable.EMMCStatusListItemWidget_uxsdk_list_item_dialog_theme) {
                dialogTheme = it
            }

        }
    }
    //endregion

    //region Hooks
    /**
     * Get the [ListItemLabelButtonWidget.UIState] updates
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
     * eMMC List Item Dialog Identifiers
     */
    sealed class DialogType {
        /**
         * Dialog shown for format confirmation
         */
        object FormatConfirmation : DialogType()

        /**
         * Dialog shown for format success
         */
        object FormatSuccess : DialogType()

        /**
         * Dialog shown for format fail
         */
        object FormatError : DialogType()
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
         * Current eMMC List Item State
         */
        data class EMMCStateUpdated(val eMMCState: EMMCStatusListItemWidgetModel.EMMCState) : ModelState()
    }
    //endregion
}