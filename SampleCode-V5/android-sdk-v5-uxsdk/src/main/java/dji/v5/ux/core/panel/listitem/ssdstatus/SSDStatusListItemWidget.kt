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

package dji.v5.ux.core.panel.listitem.ssdstatus

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.StyleRes
import androidx.core.content.res.use
import dji.sdk.keyvalue.value.camera.SSDOperationState
import dji.v5.utils.common.JsonUtil
import dji.v5.utils.common.LogUtils
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.UXSDKError
import dji.v5.ux.core.base.WidgetSizeDescription
import dji.v5.ux.core.base.panel.listitem.ListItemLabelButtonWidget
import dji.v5.ux.core.base.panel.listitem.ListItemLabelButtonWidget.UIState.*
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.*
import dji.v5.ux.core.panel.listitem.ssdstatus.SSDStatusListItemWidget.DialogType.*
import dji.v5.ux.core.panel.listitem.ssdstatus.SSDStatusListItemWidget.ModelState
import dji.v5.ux.core.panel.listitem.ssdstatus.SSDStatusListItemWidget.ModelState.ProductConnected
import dji.v5.ux.core.panel.listitem.ssdstatus.SSDStatusListItemWidget.ModelState.SSDStateUpdated
import dji.v5.ux.core.panel.listitem.ssdstatus.SSDStatusListItemWidgetModel.SSDState
import dji.v5.ux.core.util.UnitConversionUtil
import io.reactivex.rxjava3.core.Flowable

/**
 *  SSD status list item
 *
 *  It displays the remaining capacity of the SSD along with
 *  any warnings / errors related to the SSD.
 *  It provides a button to format SSD.
 */
open class SSDStatusListItemWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ListItemLabelButtonWidget<ModelState>(
        context,
        attrs,
        defStyleAttr,
        WidgetType.LABEL_BUTTON,
        R.style.UXSDKSSDStatusListItem
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

    private val widgetModel: SSDStatusListItemWidgetModel by lazy {
        SSDStatusListItemWidgetModel(
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

    override fun onButtonClick() {
        val dialogListener = DialogInterface.OnClickListener { dialogInterface, buttonId: Int ->
            if (buttonId == DialogInterface.BUTTON_POSITIVE) {
                uiUpdateStateProcessor.onNext(DialogActionConfirmed(FormatConfirmation))
                formatSSD()
            } else {
                uiUpdateStateProcessor.onNext(DialogActionCanceled(FormatConfirmation))
            }
            dialogInterface.dismiss()

        }
        val dialogDismissListener = DialogInterface.OnDismissListener {
            uiUpdateStateProcessor.onNext(DialogDismissed(FormatConfirmation))
        }
        showConfirmationDialog(title = getString(R.string.uxsdk_ssd_dialog_title),
                icon = formatConfirmationDialogIcon,
                dialogTheme = dialogTheme,
                message = getString(R.string.uxsdk_ssd_format_confirmation),
                dialogClickListener = dialogListener,
                dialogDismissListener = dialogDismissListener)
        uiUpdateStateProcessor.onNext(DialogDisplayed(FormatConfirmation))
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.productConnection
                .observeOn(SchedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
        addReaction(widgetModel.ssdState
                .observeOn(SchedulerProvider.ui())
                .subscribe { this.updateUI(it) })

    }

    //endregion

    //region Reactions to models

    private fun updateUI(ssdState: SSDState) {
        LogUtils.i(logTag,JsonUtil.toJson(ssdState))
        widgetStateDataProcessor.onNext(SSDStateUpdated(ssdState))
        when (ssdState) {
            SSDState.ProductDisconnected,
            SSDState.NotSupported -> updateDisabledState(ssdState)

            is SSDState.CurrentSSDState -> {
                isEnabled = true
                listItemLabel = getSSDMessage(ssdState.ssdOperationState, ssdState.remainingSpace)
                listItemLabelTextColor = getSSDMessageColor(ssdState.ssdOperationState)
                listItemButtonEnabled = getFormatButtonVisibility(ssdState.ssdOperationState)
            }
        }
    }
    //endregion

    //region Helpers
    private fun getSSDMessage(ssdState: SSDOperationState, remainingSpace: Int): String {
        return when (ssdState) {
            SSDOperationState.NOT_FOUND -> getString(R.string.uxsdk_ssd_not_found)
            SSDOperationState.IDLE -> UnitConversionUtil.getSpaceWithUnit(context, remainingSpace)
            SSDOperationState.SAVING -> getString(R.string.uxsdk_ssd_saving)
            SSDOperationState.FORMATTING -> getString(R.string.uxsdk_ssd_formatting)
            SSDOperationState.INITIALIZING -> getString(R.string.uxsdk_ssd_initializing)
            SSDOperationState.STATE_ERROR -> getString(R.string.uxsdk_ssd_error)
            SSDOperationState.FULL -> getString(R.string.uxsdk_ssd_full)
            SSDOperationState.POOR_CONNECTION -> getString(R.string.uxsdk_ssd_poor_connection)
            SSDOperationState.SWITCHING_LICENSE -> getString(R.string.uxsdk_ssd_switching_license)
            SSDOperationState.FORMATTING_REQUIRED -> getString(R.string.uxsdk_ssd_formatting_required)
            SSDOperationState.NOT_INITIALIZED -> getString(R.string.uxsdk_ssd_not_initialized)
            SSDOperationState.INVALID_FILE_SYSTEM -> getString(R.string.uxsdk_ssd_formatting_required)
            SSDOperationState.UNKNOWN -> getString(R.string.uxsdk_string_default_value)
        }
    }

    private fun getSSDMessageColor(ssdState: SSDOperationState): Int {
        return when (ssdState) {
            SSDOperationState.IDLE -> normalValueColor
            SSDOperationState.STATE_ERROR -> errorValueColor
            else -> warningValueColor
        }
    }

    private fun getFormatButtonVisibility(ssdState: SSDOperationState): Boolean {
        return when (ssdState) {
            SSDOperationState.FORMATTING_REQUIRED,
            SSDOperationState.INVALID_FILE_SYSTEM,
            SSDOperationState.FULL,
            SSDOperationState.IDLE -> true
            else -> false
        }
    }

    private fun updateDisabledState(ssdState: SSDState) {
        listItemLabel = if (ssdState is SSDState.ProductDisconnected) {
            getString(R.string.uxsdk_string_default_value)
        } else {
            getString(R.string.uxsdk_storage_status_not_supported)
        }
        listItemLabelTextColor = if (ssdState is SSDState.ProductDisconnected) {
            disconnectedValueColor
        } else {
            errorValueColor
        }
        isEnabled = false
    }

    private fun formatSSD() {
        var dialogType: DialogType? = null
        val dialogDismissListener = DialogInterface.OnDismissListener {
            uiUpdateStateProcessor.onNext(DialogDismissed(dialogType))
        }
        addDisposable(widgetModel.formatSSD()
                .observeOn(SchedulerProvider.ui())
                .subscribe({
                    dialogType = FormatSuccess
                    showAlertDialog(title = getString(R.string.uxsdk_ssd_dialog_title),
                            icon = formatSuccessDialogIcon,
                            dialogTheme = dialogTheme,
                            message = getString(R.string.uxsdk_ssd_format_complete),
                            dialogDismissListener = dialogDismissListener)
                    uiUpdateStateProcessor.onNext(DialogDisplayed(dialogType))
                }, { error ->
                    if (error is UXSDKError) {
                        dialogType = FormatError
                        showAlertDialog(title = getString(R.string.uxsdk_ssd_dialog_title),
                                icon = formatErrorDialogIcon,
                                dialogTheme = dialogTheme,
                                message = getString(R.string.uxsdk_ssd_format_error, error.djiError.description()),
                                dialogDismissListener = dialogDismissListener)
                        uiUpdateStateProcessor.onNext(DialogDisplayed(dialogType))
                    }
                }))
    }
    //endregion

    //region Customizations
    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.SSDStatusListItemWidget, 0, defaultStyle).use { typedArray ->
            typedArray.getDrawableAndUse(R.styleable.SSDStatusListItemWidget_uxsdk_list_item_confirmation_dialog_icon) {
                formatConfirmationDialogIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.SSDStatusListItemWidget_uxsdk_list_item_success_dialog_icon) {
                formatSuccessDialogIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.SSDStatusListItemWidget_uxsdk_list_item_error_dialog_icon) {
                formatErrorDialogIcon = it
            }
            typedArray.getResourceIdAndUse(R.styleable.SSDStatusListItemWidget_uxsdk_list_item_dialog_theme) {
                dialogTheme = it
            }

        }
    }

    override val widgetSizeDescription: WidgetSizeDescription =
            WidgetSizeDescription(WidgetSizeDescription.SizeType.OTHER,
                    widthDimension = WidgetSizeDescription.Dimension.EXPAND,
                    heightDimension = WidgetSizeDescription.Dimension.WRAP)

    override fun getIdealDimensionRatioString(): String? {
        return null
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
     * SSD List Item Dialog Identifiers
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
         * Current SSD List Item State
         */
        data class SSDStateUpdated(val ssdState: SSDState) : ModelState()
    }
    //endregion


}