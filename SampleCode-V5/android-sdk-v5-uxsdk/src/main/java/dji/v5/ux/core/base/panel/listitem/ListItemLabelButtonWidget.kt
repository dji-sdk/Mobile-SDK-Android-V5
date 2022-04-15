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

package dji.v5.ux.core.base.panel.listitem

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.annotation.*
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.use
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.processors.PublishProcessor
import dji.v5.ux.R
import dji.v5.ux.core.extension.*
import dji.v5.ux.core.util.ViewIDGenerator

/**
 * This is the base class to be used for display
 * type of list item
 * @property widgetType - The [WidgetType] of the current widget.
 */
abstract class ListItemLabelButtonWidget<T> @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        val widgetType: WidgetType,
        defaultStyle: Int
) : ListItemTitleWidget<T>(context, attrs, defStyleAttr, defaultStyle), View.OnClickListener {

    //region Fields
    private val listItemButton: TextView = TextView(context)

    private val listItemLabelTextView: TextView = TextView(context)

    protected val uiUpdateStateProcessor: PublishProcessor<UIState> = PublishProcessor.create()
    //endregion

    //region label customizations
    /**
     * Get the [UIState] updates
     */
    open fun getUIStateUpdates(): Flowable<UIState> {
        return uiUpdateStateProcessor.onBackpressureBuffer()
    }

    /**
     * The list item label string
     */
    var listItemLabel: String?
        @Nullable get() = listItemLabelTextView.text.toString()
        set(@Nullable value) {
            listItemLabelTextView.text = value
        }

    /**
     * The size of the list item label text
     */
    var listItemLabelTextSize: Float
        @Dimension get() = listItemLabelTextView.textSize
        set(@Dimension value) {
            listItemLabelTextView.textSize = value
        }

    /**
     * The color of the list item label text
     */
    var listItemLabelTextColor: Int
        @ColorInt get() = listItemLabelTextView.textColor
        set(@ColorInt value) {
            listItemLabelTextView.textColor = value
        }

    /**
     * The color state list of the list item label text
     */
    var listItemLabelTextColors: ColorStateList?
        get() = listItemLabelTextView.textColorStateList
        set(value) {
            listItemLabelTextView.textColorStateList = value
        }

    /**
     * The background of the list item label text
     */
    var listItemLabelBackground: Drawable?
        get() = listItemLabelTextView.background
        set(value) {
            listItemLabelTextView.background = value
        }

    /**
     * The visibility of the list item button
     */
    var listItemLabelVisibility: Boolean
        get() = listItemLabelTextView.visibility == View.VISIBLE
        set(value) {
            if (value) {
                listItemLabelTextView.show()
            } else {
                listItemLabelTextView.hide()
            }
        }
    //endregion

    //region button customizations
    /**
     * The list item label string
     */
    var listItemButtonText: String?
        @Nullable get() = listItemButton.text.toString()
        set(@Nullable value) {
            listItemButton.text = value
        }

    /**
     * The size of the list item label text
     */
    var listItemButtonTextSize: Float
        @Dimension get() = listItemButton.textSize
        set(@Dimension value) {
            listItemButton.textSize = value
        }

    /**
     * The color of the list item label text
     */
    var listItemButtonTextColor: Int
        @ColorInt get() = listItemButton.textColor
        set(@ColorInt value) {
            listItemButton.textColor = value
        }

    /**
     * The color state list of the list item label text
     */
    var listItemButtonTextColors: ColorStateList?
        get() = listItemButton.textColorStateList
        set(value) {
            listItemButton.textColorStateList = value
        }

    /**
     * The background of the list item label text
     */
    var listItemButtonBackground: Drawable?
        get() = listItemButton.background
        set(value) {
            listItemButton.background = value
        }

    /**
     * Enable/Disable button
     */
    var listItemButtonEnabled: Boolean
        get() = listItemButton.isEnabled
        set(value) {
            listItemButton.isEnabled = value
        }

    /**
     * The visibility of the list item button
     */
    var listItemButtonVisibility: Boolean
        get() = listItemButton.visibility == View.VISIBLE
        set(value) {
            if (value) {
                listItemButton.show()
            } else {
                listItemButton.hide()
            }
        }

    //endregion

    init {
        when (widgetType) {
            WidgetType.LABEL -> {
                configureLabelWidget()
            }
            WidgetType.BUTTON -> {
                configureButtonWidget()
            }
            WidgetType.LABEL_BUTTON -> {
                configureLabelButtonWidget()
            }
        }
        val paddingValue = resources.getDimension(R.dimen.uxsdk_pre_flight_checklist_item_padding).toInt()
        setContentPadding(0, paddingValue, 0, paddingValue)
        attrs?.let { initAttributes(context, it) }
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.ListItemLabelButtonWidget, 0, defaultStyle).use { typedArray ->
            typedArray.getResourceIdAndUse(R.styleable.ListItemLabelButtonWidget_uxsdk_list_item_label_appearance) {
                setListItemLabelTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.ListItemLabelButtonWidget_uxsdk_list_item_label_text_size) {
                listItemLabelTextSize = it
            }
            typedArray.getColorAndUse(R.styleable.ListItemLabelButtonWidget_uxsdk_list_item_label_text_color) {
                listItemLabelTextColor = it
            }
            typedArray.getColorStateListAndUse(R.styleable.ListItemLabelButtonWidget_uxsdk_list_item_label_text_color) {
                listItemLabelTextColors = it
            }
            typedArray.getDrawableAndUse(R.styleable.ListItemLabelButtonWidget_uxsdk_list_item_label_background) {
                listItemLabelBackground = it
            }
            listItemLabelVisibility = typedArray.getBoolean(R.styleable.ListItemLabelButtonWidget_uxsdk_list_item_label_visibility,
                    listItemLabelVisibility)
            listItemLabel =
                    typedArray.getString(R.styleable.ListItemLabelButtonWidget_uxsdk_list_item_label,
                            getString(R.string.uxsdk_string_default_value))

            typedArray.getResourceIdAndUse(R.styleable.ListItemLabelButtonWidget_uxsdk_list_item_button_appearance) {
                setListItemButtonTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.ListItemLabelButtonWidget_uxsdk_list_item_button_text_size) {
                listItemButtonTextSize = it
            }
            typedArray.getColorAndUse(R.styleable.ListItemLabelButtonWidget_uxsdk_list_item_button_text_color) {
                listItemButtonTextColor = it
            }
            typedArray.getColorStateListAndUse(R.styleable.ListItemLabelButtonWidget_uxsdk_list_item_button_text_color) {
                listItemButtonTextColors = it
            }
            typedArray.getDrawableAndUse(R.styleable.ListItemLabelButtonWidget_uxsdk_list_item_button_background) {
                listItemButtonBackground = it
            }
            listItemButtonVisibility = typedArray.getBoolean(R.styleable.ListItemLabelButtonWidget_uxsdk_list_item_button_visibility,
                    listItemButtonVisibility)
            listItemButtonEnabled = typedArray.getBoolean(R.styleable.ListItemLabelButtonWidget_uxsdk_list_item_button_enabled,
                    listItemButtonEnabled)

            listItemButtonText =
                    typedArray.getString(R.styleable.ListItemLabelButtonWidget_uxsdk_list_item_button_text,
                            getString(R.string.uxsdk_string_default_value))

        }
    }


    private fun initLabel() {
        listItemLabelTextView.id = ViewIDGenerator.generateViewId()
        listItemLabelTextView.gravity = Gravity.CENTER_VERTICAL or Gravity.END
        listItemLabelTextColors = resources.getColorStateList(R.color.uxsdk_selector_text_color)
        listItemLabelTextSize = getDimension(R.dimen.uxsdk_list_item_label_text_size)
        listItemLabel = getString(R.string.uxsdk_string_default_value)
    }

    private fun initButton() {
        listItemButton.id = ViewIDGenerator.generateViewId()
        listItemButtonBackground = getDrawable(R.drawable.uxsdk_system_status_button_background_selector)
        listItemButtonTextColors = resources.getColorStateList(R.color.uxsdk_selector_text_color)
        listItemButtonText = getString(R.string.uxsdk_string_default_value)
        listItemButton.setOnClickListener(this)
        listItemButton.gravity = Gravity.CENTER
        listItemButton.minWidth = getDimension(R.dimen.uxsdk_list_item_button_min_width).toInt()
        listItemButtonTextSize = getDimension(R.dimen.uxsdk_list_item_button_text_size)
        val buttonPaddingHorizontal: Int = getDimension(R.dimen.uxsdk_list_item_button_padding_horizontal).toInt()
        val buttonPaddingVertical: Int = getDimension(R.dimen.uxsdk_list_item_button_padding_vertical).toInt()
        listItemButton.setPadding(buttonPaddingHorizontal, buttonPaddingVertical, buttonPaddingHorizontal, buttonPaddingVertical)
    }

    private fun configureLabelWidget() {
        initLabel()
        val layoutParams = LayoutParams(0, ConstraintSet.WRAP_CONTENT)
        layoutParams.rightToLeft = clickIndicatorId
        layoutParams.topToTop = guidelineTop.id
        layoutParams.bottomToBottom = guidelineBottom.id
        layoutParams.leftToRight = guidelineCenter.id
        listItemLabelTextView.layoutParams = layoutParams
        addView(listItemLabelTextView)
    }

    private fun configureButtonWidget() {
        initButton()
        val layoutParams = LayoutParams(ConstraintSet.WRAP_CONTENT, ConstraintSet.WRAP_CONTENT)
        layoutParams.rightToLeft = clickIndicatorId
        layoutParams.topToTop = guidelineTop.id
        layoutParams.bottomToBottom = guidelineBottom.id
        listItemButton.layoutParams = layoutParams
        addView(listItemButton)
    }

    private fun configureLabelButtonWidget() {
        initLabel()
        initButton()
        val labelLayoutParams = LayoutParams(0, ConstraintSet.WRAP_CONTENT)
        labelLayoutParams.leftToRight = listItemButton.id
        labelLayoutParams.rightToLeft = clickIndicatorId
        labelLayoutParams.topToTop = guidelineTop.id
        labelLayoutParams.bottomToBottom = guidelineBottom.id
        listItemLabelTextView.layoutParams = labelLayoutParams
        val buttonLayoutParams = LayoutParams(ConstraintSet.WRAP_CONTENT, ConstraintSet.WRAP_CONTENT)
        buttonLayoutParams.rightMargin = getDimension(R.dimen.uxsdk_list_item_button_padding_horizontal).toInt()
        buttonLayoutParams.rightToLeft = listItemLabelTextView.id
        buttonLayoutParams.topToTop = guidelineTop.id
        buttonLayoutParams.bottomToBottom = guidelineBottom.id
        buttonLayoutParams.leftToRight = guidelineCenter.id
        buttonLayoutParams.horizontalChainStyle = ConstraintSet.CHAIN_PACKED
        buttonLayoutParams.horizontalBias = 1.0f
        listItemButton.layoutParams = buttonLayoutParams
        addView(listItemLabelTextView)
        addView(listItemButton)
    }

    /**
     * Set the background of the item button
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setListItemButtonBackground(@DrawableRes resourceId: Int) {
        listItemButtonBackground = getDrawable(resourceId)
    }

    /**
     * Set the text appearance of the item button
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    fun setListItemButtonTextAppearance(@StyleRes textAppearanceResId: Int) {
        listItemButton.setTextAppearance(context, textAppearanceResId)
    }

    /**
     * Set the background of the item label
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setListItemLabelBackground(@DrawableRes resourceId: Int) {
        listItemLabelBackground = getDrawable(resourceId)
    }

    /**
     * Set the text appearance of the item label
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    fun setListItemLabelTextAppearance(@StyleRes textAppearanceResId: Int) {
        listItemLabelTextView.setTextAppearance(context, textAppearanceResId)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        listItemButton.isEnabled = enabled
        listItemLabelTextView.isEnabled = enabled
    }

    @CallSuper
    override fun onClick(view: View?) {
        super.onClick(view)
        if (view == listItemButton) {
            uiUpdateStateProcessor.onNext(UIState.ButtonClicked)
            onButtonClick()
        }
    }

    @CallSuper
    override fun onListItemClick() {
      uiUpdateStateProcessor.onNext(UIState.ListItemClicked)
    }

    abstract fun onButtonClick()

    /**
     * Widget UI update State
     */
    sealed class UIState {
        /**
         * List Item click update
         */
        object ListItemClicked : UIState()

        /**
         * Button click update
         */
        object ButtonClicked : UIState()

        /**
         *  Dialog shown update
         */
        data class DialogDisplayed(val info: Any?) : UIState()

        /**
         *  Dialog action confirm
         */
        data class DialogActionConfirmed(val info: Any?) : UIState()

        /**
         *  Dialog action cancel
         */
        data class DialogActionCanceled(val info: Any?) : UIState()

        /**
         *  Dialog action dismiss
         */
        data class DialogDismissed(val info: Any?) : UIState()
    }

    /**
     * Defines the type of widget
     */
    enum class WidgetType {

        /**
         *  The type represents the item with icon,
         *  item name and item string label
         */
        LABEL,

        /**
         * The type represents the item with icon,
         * item name and button for action
         */
        BUTTON,

        /**
         * The class represents the item with icon,
         * item name, item string label
         * and button for action
         */
        LABEL_BUTTON
    }

}