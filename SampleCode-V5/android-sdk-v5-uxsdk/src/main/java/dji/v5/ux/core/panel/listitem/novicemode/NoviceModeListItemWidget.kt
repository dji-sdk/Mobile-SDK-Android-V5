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

package dji.v5.ux.core.panel.listitem.novicemode

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
import dji.v5.ux.core.base.panel.listitem.ListItemSwitchWidget
import dji.v5.ux.core.base.panel.listitem.ListItemSwitchWidget.UIState.*
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.*
import dji.v5.ux.core.panel.listitem.novicemode.NoviceModeListItemWidget.DialogType.NoviceModeDisableConfirmation
import dji.v5.ux.core.panel.listitem.novicemode.NoviceModeListItemWidget.DialogType.NoviceModeEnabled
import dji.v5.ux.core.panel.listitem.novicemode.NoviceModeListItemWidget.ModelState
import dji.v5.ux.core.panel.listitem.novicemode.NoviceModeListItemWidget.ModelState.NoviceModeStateUpdated
import dji.v5.ux.core.panel.listitem.novicemode.NoviceModeListItemWidget.ModelState.ProductConnected
import dji.v5.ux.core.panel.listitem.novicemode.NoviceModeListItemWidgetModel.NoviceModeState

private const val TAG = "NoviceModeListItemW"

/**
 * Widget shows the current status of the Novice Mode, also known
 * as Beginner Mode.
 * It also provides an option to switch between ON/OFF state
 */
open class NoviceModeListItemWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleArr: Int = 0
) : ListItemSwitchWidget<ModelState>(
        context,
        attrs,
        defStyleArr,
        R.style.UXSDKNoviceModeListItem
) {

    //region Fields
    private val widgetModel: NoviceModeListItemWidgetModel by lazy {
        NoviceModeListItemWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
    }

    /**
     * Icon for confirmation dialog
     */
    var confirmationDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_alert_yellow)


    /**
     * Icon for success dialog
     */
    var successDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_alert_good)

    /**
     * Theme for the dialogs shown in the widget
     */
    @StyleRes
    var dialogTheme: Int = R.style.UXSDKDialogTheme
    //endregion

    //region Constructor
    init {
        initAttributes(context, attrs)
    }
    //endregion

    //region Lifecycle
    override fun reactToModelChanges() {
        addReaction(widgetModel.productConnection
                .observeOn(SchedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
        addReaction(widgetModel.noviceModeState
                .observeOn(SchedulerProvider.ui())
                .subscribe {
                    widgetStateDataProcessor.onNext(NoviceModeStateUpdated(it))
                    updateUI(it)
                })
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

    override fun onSwitchToggle(isChecked: Boolean) {
        if (isChecked) {
            toggleNoviceMode(isChecked)
        } else {
            showNoviceModeConfirmationDialog()
        }
    }
    //endregion

    //region Reactions to Model
    private fun updateUI(noviceModeState: NoviceModeState) {
        when (noviceModeState) {
            NoviceModeState.ProductDisconnected -> isEnabled = false
            NoviceModeState.Enabled -> {
                updateState(true)
            }
            NoviceModeState.Disabled -> {
                updateState(false)
            }
        }
    }

    private fun updateState(noviceModeEnabled: Boolean) {
        isEnabled = true
        setChecked(noviceModeEnabled)
    }
    //endregion

    //region Helpers
    private fun toggleNoviceMode(checked: Boolean) {
        addDisposable(widgetModel.toggleNoviceMode()
                .observeOn(SchedulerProvider.ui())
                .subscribe({
                    if (checked) {
                        showEnabledDialog()
                    }
                    LogUtils.d(TAG, " toggle success ")
                }, {
                    if (it is UXSDKError) {
                        LogUtils.e(TAG, it.djiError.description())
                    }
                    resetSwitchState()
                }))
    }

    private fun showNoviceModeConfirmationDialog() {
        val dialogListener = DialogInterface.OnClickListener { dialogInterface, buttonId: Int ->
            if (buttonId == DialogInterface.BUTTON_POSITIVE) {
                toggleNoviceMode(false)
                uiUpdateStateProcessor.onNext(DialogActionConfirmed(NoviceModeDisableConfirmation))
            } else {
                uiUpdateStateProcessor.onNext(DialogActionCanceled(NoviceModeDisableConfirmation))
                resetSwitchState()
            }
            dialogInterface.dismiss()
        }
        val dialogDismissListener = DialogInterface.OnDismissListener {
            uiUpdateStateProcessor.onNext(DialogDismissed(NoviceModeDisableConfirmation))
        }
        showConfirmationDialog(dialogTheme = dialogTheme,
                icon = confirmationDialogIcon,
                title = getString(R.string.uxsdk_list_item_novice_mode),
                message = getString(R.string.uxsdk_novice_mode_disabled_message),
                dialogClickListener = dialogListener,
                dialogDismissListener = dialogDismissListener)
        uiUpdateStateProcessor.onNext(DialogDisplayed(NoviceModeDisableConfirmation))
    }

    private fun showEnabledDialog() {
        val dialogDismissListener = DialogInterface.OnDismissListener {
            uiUpdateStateProcessor.onNext(DialogDismissed(NoviceModeEnabled))
        }
        showAlertDialog(dialogTheme = dialogTheme,
                icon = successDialogIcon,
                title = getString(R.string.uxsdk_list_item_novice_mode),
                message = getString(R.string.uxsdk_novice_mode_enabled_message),
                dialogDismissListener = dialogDismissListener)
        uiUpdateStateProcessor.onNext(DialogDisplayed(NoviceModeEnabled))
    }

    private fun resetSwitchState() {
        addDisposable(widgetModel.noviceModeState.firstOrError()
                .observeOn(SchedulerProvider.ui())
                .subscribe({
                    updateUI(it)
                }, {
                    LogUtils.e(TAG, it.message)
                }))
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

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.NoviceModeListItemWidget, 0, defaultStyle).use { typedArray ->
            typedArray.getDrawableAndUse(R.styleable.NoviceModeListItemWidget_uxsdk_list_item_confirmation_dialog_icon) {
                confirmationDialogIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.NoviceModeListItemWidget_uxsdk_list_item_success_dialog_icon) {
                successDialogIcon = it
            }
            typedArray.getResourceIdAndUse(R.styleable.NoviceModeListItemWidget_uxsdk_list_item_dialog_theme) {
                dialogTheme = it
            }

        }
    }
    //endregion

    //region Hooks

    /**
     * Get the [ListItemSwitchWidget.UIState] updates
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
     * Novice mode dialog identifiers
     */
    sealed class DialogType {
        /**
         * Dialog shown when novice mode is enabled successfully
         */
        object NoviceModeEnabled : DialogType()

        /**
         * Dialog shown when switching from enabled to disabled
         */
        object NoviceModeDisableConfirmation : DialogType()
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
         * Current novice mode state
         */
        data class NoviceModeStateUpdated(val noviceModeState: NoviceModeState) : ModelState()

    }
    //endregion
}