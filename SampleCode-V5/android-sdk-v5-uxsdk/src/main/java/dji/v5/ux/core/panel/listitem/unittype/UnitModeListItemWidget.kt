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

package dji.v5.ux.core.panel.listitem.unittype

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.text.Layout
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.AlignmentSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.res.use
import dji.v5.utils.common.DisplayUtil
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.UXSDKError
import dji.v5.ux.core.base.WidgetSizeDescription
import dji.v5.ux.core.base.panel.listitem.ListItemRadioButtonWidget
import dji.v5.ux.core.communication.GlobalPreferencesManager
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.*
import dji.v5.ux.core.panel.listitem.unittype.UnitModeListItemWidget.ModelState
import dji.v5.ux.core.panel.listitem.unittype.UnitModeListItemWidget.ModelState.*
import dji.v5.ux.core.panel.listitem.unittype.UnitModeListItemWidgetModel.UnitTypeState
import dji.v5.ux.core.util.UnitConversionUtil.UnitType

/**
 * Widget shows the current [UnitType] being used.
 * It also provides an option to switch between them.
 */
open class UnitModeListItemWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ListItemRadioButtonWidget<ModelState>(
        context,
        attrs,
        defStyleAttr,
        R.style.UXSDKUnitModeListItem
) {

    //region Fields
    private val widgetModel: UnitModeListItemWidgetModel by lazy {
        UnitModeListItemWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance(),
                GlobalPreferencesManager.getInstance())
    }

    private var imperialItemIndex: Int = INVALID_OPTION_INDEX
    private var metricItemIndex: Int = INVALID_OPTION_INDEX

    /**
     * Theme for the dialogs shown for format
     */
    @StyleRes
    var dialogTheme: Int = R.style.UXSDKDialogTheme

    /**
     * The text appearance of the check box label
     */
    @get:StyleRes
    @setparam:StyleRes
    var checkBoxTextAppearance = 0

    /**
     * The text color state list of the check box label
     */
    var checkBoxTextColor: ColorStateList? = null

    /**
     * The background of the check box
     */
    var checkBoxTextBackground: Drawable? = null

    /**
     * The text size of the check box label
     */
    @get:Dimension
    @setparam:Dimension
    var checkBoxTextSize: Float = 0f

    //endregion

    //region Constructor
    init {
        imperialItemIndex = addOptionToGroup(getString(R.string.uxsdk_list_item_unit_mode_imperial))
        metricItemIndex = addOptionToGroup(getString(R.string.uxsdk_list_item_unit_mode_metric))
        initAttributes(context, attrs)
    }
    //endregion

    //region Lifecycle
    override fun reactToModelChanges() {
        addReaction(widgetModel.productConnection
                .observeOn(SchedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
        addReaction(widgetModel.unitTypeState
                .observeOn(SchedulerProvider.ui())
                .subscribe {
                    widgetStateDataProcessor.onNext(UnitTypeUpdated(it))
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

    override fun onOptionTapped(optionIndex: Int, optionLabel: String) {
        val newUnitType: UnitType = if (optionIndex == imperialItemIndex) {
            UnitType.IMPERIAL
        } else {
            UnitType.METRIC
        }
        addDisposable(widgetModel.setUnitType(newUnitType)
                .observeOn(SchedulerProvider.ui())
                .subscribe({
                    widgetStateDataProcessor.onNext(SetUnitTypeSucceeded)
                    if (newUnitType == UnitType.IMPERIAL
                            && !GlobalPreferencesManager.getInstance().isUnitModeDialogNeverShown) {
                        showWarningDialog()
                    }
                }, {
                    widgetStateDataProcessor.onNext(SetUnitTypeFailed(it as UXSDKError))
                    resetUI()
                }))

    }

    //endregion

    //region Reactions to model

    private fun updateUI(unitTypeState: UnitTypeState) {
        isEnabled = if (unitTypeState is UnitTypeState.CurrentUnitType) {
            if (unitTypeState.unitType == UnitType.IMPERIAL) {
                setSelected(imperialItemIndex)
            } else {
                setSelected(metricItemIndex)
            }
            true
        } else {
            false
        }
    }

    //endregion

    //region Helpers
    @SuppressLint("InflateParams")
    private fun showWarningDialog() {
        val dialogListener = DialogInterface.OnClickListener { dialogInterface, _: Int ->
            dialogInterface.dismiss()
        }
        val dialogDismissListener = DialogInterface.OnDismissListener {
            uiUpdateStateProcessor.onNext(UIState.DialogDismissed(null))
        }
        val ctw = ContextThemeWrapper(context, dialogTheme)
        val inflater: LayoutInflater = ctw.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.uxsdk_layout_dialog_checkbox, null)
        val builder = AlertDialog.Builder(context, dialogTheme)
        builder.setPositiveButton(getString(R.string.uxsdk_app_ok), dialogListener)
        builder.setOnDismissListener(dialogDismissListener)
        builder.setCancelable(true)
        builder.setTitle(getString(R.string.uxsdk_list_item_unit_mode))
        val neverShowAgainCheckBox = view.findViewById<CheckBox>(R.id.checkbox_dont_show_again)
        neverShowAgainCheckBox.setTextColor(getColor(R.color.uxsdk_white))
        if (checkBoxTextAppearance != INVALID_RESOURCE) {
            neverShowAgainCheckBox.setTextAppearance(context, checkBoxTextAppearance)
        }
        if (checkBoxTextColor != null) {
            neverShowAgainCheckBox.setTextColor(checkBoxTextColor)
        }
        if (checkBoxTextBackground != null) {
            neverShowAgainCheckBox.background = checkBoxTextBackground
        }
        if (checkBoxTextSize != INVALID_DIMENSION) {
            neverShowAgainCheckBox.textSize = checkBoxTextSize
        }
        val textView = view.findViewById<TextView>(R.id.textview_dialog_content)
        textView.setText(getSpannableString(), TextView.BufferType.SPANNABLE)
        neverShowAgainCheckBox.setOnCheckedChangeListener { _: CompoundButton?, checked: Boolean ->
            GlobalPreferencesManager.getInstance().isUnitModeDialogNeverShown = checked
            uiUpdateStateProcessor.onNext(UIState.NeverShowAgainCheckChanged(checked))
        }
        builder.setView(view)
        builder.create().show()
        uiUpdateStateProcessor.onNext(UIState.DialogDisplayed(null))
    }

    private fun resetUI() {
        addDisposable(widgetModel.unitTypeState
                .observeOn(SchedulerProvider.ui())
                .subscribe { updateUI(it) })
    }

    private fun getSpannableString(): SpannableStringBuilder {
        val builder = SpannableStringBuilder()

        val str1 = SpannableString(getString(R.string.uxsdk_dialog_unit_change_notice))
        str1.setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL), 0, str1.length, 0)
        builder.appendLine(str1).appendLine()

        val str2 = SpannableString(resources.getString(R.string.uxsdk_dialog_unit_change_example))
        str2.setSpan(AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, str2.length, 0)
        builder.append(str2)
        return builder
    }
    //endregion

    //region Customizations

    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    override val widgetSizeDescription: WidgetSizeDescription =
            WidgetSizeDescription(WidgetSizeDescription.SizeType.OTHER,
                    widthDimension = WidgetSizeDescription.Dimension.EXPAND,
                    heightDimension = WidgetSizeDescription.Dimension.WRAP)

    /**
     * Set the text color for the check box label
     *
     * @param color color integer resource
     */
    fun setCheckBoxTextColor(@ColorInt color: Int) {
        if (color != INVALID_COLOR) {
            checkBoxTextColor = ColorStateList.valueOf(color)
        }
    }

    /**
     * Set the resource ID for the background of the check box
     *
     * @param resourceId Integer ID of the text view's background resource
     */
    fun setCheckBoxBackground(@DrawableRes resourceId: Int) {
        checkBoxTextBackground = getDrawable(resourceId)
    }

        @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.UnitModeListItemWidget, 0, defaultStyle).use { typedArray ->
            typedArray.getResourceIdAndUse(R.styleable.UnitModeListItemWidget_uxsdk_list_item_dialog_theme) {
                dialogTheme = it
            }
            typedArray.getResourceIdAndUse(R.styleable.UnitModeListItemWidget_uxsdk_checkBoxTextAppearance) {
                checkBoxTextAppearance = it
            }
            typedArray.getColorStateListAndUse(R.styleable.UnitModeListItemWidget_uxsdk_checkBoxTextColor) {
                checkBoxTextColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.UnitModeListItemWidget_uxsdk_checkBoxTextBackground) {
                checkBoxTextBackground = it
            }
            typedArray.getDimensionAndUse(R.styleable.UnitModeListItemWidget_uxsdk_checkBoxTextSize) {
                checkBoxTextSize = DisplayUtil.pxToSp(context, it)
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
     * Class defines widget state updates
     */
    sealed class ModelState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : ModelState()

        /**
         * Set unit type success
         */
        object SetUnitTypeSucceeded : ModelState()

        /**
         * Set unit type failed
         */
        data class SetUnitTypeFailed(val error: UXSDKError) : ModelState()

        /**
         * Unit type updated
         */
        data class UnitTypeUpdated(val unitTypeState: UnitTypeState) : ModelState()
    }
    //endregion

}