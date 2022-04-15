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
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.*
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.use
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.processors.PublishProcessor
import dji.v5.ux.R
import dji.v5.ux.core.base.panel.listitem.ListItemEditTextButtonWidget.WidgetType
import dji.v5.ux.core.extension.*
import dji.v5.ux.core.util.ViewIDGenerator


/**
 * This is the base class to be used for list item
 * The class represents the item with icon, item name and editable fields
 * @property widgetType - The [WidgetType] of the current widget.
 */
abstract class ListItemEditTextButtonWidget<T> @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        val widgetType: WidgetType,
        @StyleRes defaultStyle: Int
) : ListItemTitleWidget<T>(context, attrs, defStyleAttr, defaultStyle), View.OnClickListener {

    //region Fields
    private val listItemButton: TextView = TextView(context)

    private val listItemHintTextView: TextView = TextView(context)

    private val listItemEditTextView: EditText = EditText(context)

    protected val uiUpdateStateProcessor: PublishProcessor<UIState> = PublishProcessor.create()

    //endregion
    /**
     * Get the [UIState] updates
     */
    open fun getUIStateUpdates(): Flowable<UIState> {
        return uiUpdateStateProcessor.onBackpressureBuffer()
    }

    /**
     * Default color of the edit text value
     */
    @ColorInt
    var editTextNormalColor: Int = getColor(R.color.uxsdk_status_edit_text_color)

    //region hint customizations
    /**
     * The list item hint string
     */
    var listItemHint: String?
        @Nullable get() = listItemHintTextView.text.toString()
        set(@Nullable value) {
            listItemHintTextView.text = value
        }

    /**
     * The size of the list item hint text
     */
    var listItemHintTextSize: Float
        @Dimension get() = listItemHintTextView.textSize
        set(@Dimension value) {
            listItemHintTextView.textSize = value
        }

    /**
     * The color of the list item hint text
     */
    var listItemHintTextColor: Int
        @ColorInt get() = listItemHintTextView.textColor
        set(@ColorInt value) {
            listItemHintTextView.textColor = value
        }

    /**
     * The color state list of the list item hint text
     */
    var listItemHintTextColors: ColorStateList?
        get() = listItemHintTextView.textColorStateList
        set(value) {
            listItemHintTextView.textColorStateList = value
        }

    /**
     * The background of the list item hint text
     */
    var listItemHintBackground: Drawable?
        get() = listItemHintTextView.background
        set(value) {
            listItemHintTextView.background = value
        }

    /**
     * The visibility of list item hint
     */
    var listItemHintVisibility: Boolean
        get() = listItemHintTextView.visibility == View.VISIBLE
        set(value) {
            if (value) {
                listItemHintTextView.show()
            } else {
                listItemHintTextView.hide()
            }
        }
    //endregion

    //region button customizations
    /**
     * The list item hint string
     */
    var listItemButtonText: String?
        @Nullable get() = listItemButton.text.toString()
        set(@Nullable hint) {
            listItemButton.text = hint
        }

    /**
     * The size of the list item hint text
     */
    var listItemButtonTextSize: Float
        @Dimension get() = listItemButton.textSize
        set(@Dimension hint) {
            listItemButton.textSize = hint
        }

    /**
     * The color of the list item hint text
     */
    var listItemButtonTextColor: Int
        @ColorInt get() = listItemButton.textColor
        set(@ColorInt value) {
            listItemButton.textColor = value
        }

    /**
     * The color state list of the list item hint text
     */
    var listItemButtonTextColors: ColorStateList?
        get() = listItemButton.textColorStateList
        set(value) {
            listItemButton.textColorStateList = value
        }

    /**
     * The background of the list item hint text
     */
    var listItemButtonBackground: Drawable?
        get() = listItemButton.background
        set(value) {
            listItemButton.background = value
        }

    /**
     * Button enabled/disabled
     */
    var listItemButtonEnabled: Boolean
        get() = listItemButton.isEnabled
        set(value) {
            listItemButton.isEnabled = value
        }

    /**
     * Button visibility
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

    //region Edit text

    /**
     * Background of edit text
     */
    var listItemEditTextBackground: Drawable?
        get() = listItemEditTextView.background
        set(value) {
            listItemEditTextView.background = value
        }

    /**
     * Text color of edit text
     */
    var listItemEditTextColor: Int
        @ColorInt get() = listItemEditTextView.textColor
        set(@ColorInt value) {
            listItemEditTextView.textColor = value
        }

    /**
     * Text ColorStateList for edit text
     */
    var listItemEditTextColors: ColorStateList?
        get() = listItemEditTextView.textColorStateList
        set(value) {
            listItemEditTextView.textColorStateList = value
        }

    /**
     * Width of edit text
     */
    var listItemEditTextWidth: Int
        @Dimension get() = listItemEditTextView.width
        set(@Dimension value) {
            listItemEditTextView.width = value
        }

    /**
     * Input type of edit text
     */
    var listItemEditTextInputType: Int
        get() = listItemEditTextView.inputType
        set(value) {
            listItemEditTextView.inputType = value
        }

    /**
     * Visibility of the edit text
     */
    var listItemEditTextVisibility: Boolean
        get() = listItemEditTextView.visibility == View.VISIBLE
        set(value) {
            if (value) {
                listItemEditTextView.show()
            } else {
                listItemEditTextView.hide()
            }
        }

    /**
     * Edit text value
     */
    var listItemEditTextValue: String?
        get() = listItemEditTextView.text.toString()
        set(value) = listItemEditTextView.setText(value)

    /**
     * Text size of edit text
     */
    var listItemEditTextSize: Float
        get() = listItemEditTextView.textSize
        set(value) {
            listItemEditTextView.textSize = value
        }
    //endregion

    init {
        if (widgetType == WidgetType.EDIT) {
            configureEditTextWidget()
        } else {
            configureEditTextButtonWidget()
        }
        val paddingValue = resources.getDimension(R.dimen.uxsdk_pre_flight_checklist_item_padding).toInt()
        setContentPadding(0, paddingValue, 0, paddingValue)
        initAttributes(context, attrs)
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.ListItemEditTextButtonWidget, 0, defaultStyle).use { typedArray ->
            typedArray.getColorAndUse(R.styleable.ListItemEditTextButtonWidget_uxsdk_list_item_edit_text_normal_color) {
                editTextNormalColor = it
            }
            typedArray.getResourceIdAndUse(R.styleable.ListItemEditTextButtonWidget_uxsdk_list_item_hint_appearance) {
                setListItemHintTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.ListItemEditTextButtonWidget_uxsdk_list_item_hint_text_size) {
                listItemHintTextSize = it
            }
            typedArray.getColorAndUse(R.styleable.ListItemEditTextButtonWidget_uxsdk_list_item_hint_text_color) {
                listItemHintTextColor = it
            }
            typedArray.getColorStateListAndUse(R.styleable.ListItemEditTextButtonWidget_uxsdk_list_item_hint_text_color) {
                listItemHintTextColors = it
            }
            typedArray.getDrawableAndUse(R.styleable.ListItemEditTextButtonWidget_uxsdk_list_item_hint_background) {
                listItemHintBackground = it
            }
            listItemHintVisibility = typedArray.getBoolean(R.styleable.ListItemEditTextButtonWidget_uxsdk_list_item_hint_visibility,
                    listItemHintVisibility)
            listItemHint =
                    typedArray.getString(R.styleable.ListItemEditTextButtonWidget_uxsdk_list_item_hint,
                            getString(R.string.uxsdk_string_default_value))

            typedArray.getResourceIdAndUse(R.styleable.ListItemEditTextButtonWidget_uxsdk_list_item_button_appearance) {
                setListItemButtonTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.ListItemEditTextButtonWidget_uxsdk_list_item_button_text_size) {
                listItemButtonTextSize = it
            }
            typedArray.getColorAndUse(R.styleable.ListItemEditTextButtonWidget_uxsdk_list_item_button_text_color) {
                listItemButtonTextColor = it
            }
            typedArray.getColorStateListAndUse(R.styleable.ListItemEditTextButtonWidget_uxsdk_list_item_button_text_color) {
                listItemButtonTextColors = it
            }
            typedArray.getDrawableAndUse(R.styleable.ListItemEditTextButtonWidget_uxsdk_list_item_button_background) {
                listItemButtonBackground = it
            }
            listItemButtonVisibility = typedArray.getBoolean(R.styleable.ListItemEditTextButtonWidget_uxsdk_list_item_button_visibility,
                    listItemButtonVisibility)
            listItemButtonEnabled = typedArray.getBoolean(R.styleable.ListItemEditTextButtonWidget_uxsdk_list_item_button_enabled,
                    listItemButtonEnabled)
            listItemButtonText =
                    typedArray.getString(R.styleable.ListItemEditTextButtonWidget_uxsdk_list_item_button_text,
                            getString(R.string.uxsdk_string_default_value))

            typedArray.getResourceIdAndUse(R.styleable.ListItemEditTextButtonWidget_uxsdk_list_item_edit_appearance) {
                setListItemButtonTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.ListItemEditTextButtonWidget_uxsdk_list_item_edit_text_size) {
                listItemEditTextSize = it
            }
            typedArray.getColorAndUse(R.styleable.ListItemEditTextButtonWidget_uxsdk_list_item_edit_text_color) {
                listItemEditTextColor = it
            }
            typedArray.getColorStateListAndUse(R.styleable.ListItemEditTextButtonWidget_uxsdk_list_item_edit_text_color) {
                listItemEditTextColors = it
            }
            typedArray.getDrawableAndUse(R.styleable.ListItemEditTextButtonWidget_uxsdk_list_item_edit_background) {
                listItemEditTextBackground = it
            }
            listItemEditTextVisibility = typedArray.getBoolean(R.styleable.ListItemEditTextButtonWidget_uxsdk_list_item_edit_visibility,
                    listItemEditTextVisibility)
            listItemEditTextValue =
                    typedArray.getString(R.styleable.ListItemEditTextButtonWidget_uxsdk_list_item_edit_text,
                            getString(R.string.uxsdk_string_default_value))

        }
    }


    private fun initEditText() {
        listItemEditTextView.id = ViewIDGenerator.generateViewId()
        listItemEditTextView.gravity = Gravity.CENTER
        listItemEditTextView.width = getDimension(R.dimen.uxsdk_list_item_edit_min_width).toInt()
        listItemEditTextView.inputType = InputType.TYPE_CLASS_PHONE
        listItemEditTextView.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
        listItemEditTextSize = getDimension(R.dimen.uxsdk_list_item_edit_text_size)
        val verticalPadding = getDimension(R.dimen.uxsdk_list_item_button_padding_vertical).toInt()
        listItemEditTextView.setPadding(0, verticalPadding, 0, verticalPadding)
        listItemEditTextView.background = getDrawable(R.drawable.uxsdk_system_status_edit_background_selector)
        listItemEditTextView.textColorStateList = resources.getColorStateList(R.color.uxsdk_selector_edit_text_color)
        listItemEditTextView.setOnClickListener(this)
        listItemEditTextView.setOnEditorActionListener { p0, p1, p2 ->
            if (isDoneActionClicked(p1, p2)) {
                uiUpdateStateProcessor.onNext(UIState.EditFinished)
                listItemEditTextView.isCursorVisible = false
                hideKeyboardFrom()
                onKeyboardDoneAction()
            }
            true
        }

        listItemEditTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                onEditorTextChanged(s?.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Do nothing
            }
        })

    }

    private fun initHint() {
        listItemHintTextView.id = ViewIDGenerator.generateViewId()
        listItemHintTextSize = getDimension(R.dimen.uxsdk_list_item_hint_text_size)
        listItemHintTextView.gravity = Gravity.CENTER_VERTICAL or Gravity.END
        listItemHintTextColor = getColor(R.color.uxsdk_white_50_percent)
        listItemHint = getString(R.string.uxsdk_string_default_value)
    }

    private fun initButton() {
        listItemButton.id = ViewIDGenerator.generateViewId()
        listItemButtonBackground = getDrawable(R.drawable.uxsdk_system_status_button_background_selector)
        listItemButtonTextColors = resources.getColorStateList(R.color.uxsdk_selector_text_color)
        listItemButtonText = getString(R.string.uxsdk_string_default_value)
        listItemButtonTextSize = getDimension(R.dimen.uxsdk_list_item_button_text_size)
        listItemButton.setOnClickListener(this)
        listItemButton.gravity = Gravity.CENTER
        listItemButton.minWidth = getDimension(R.dimen.uxsdk_list_item_button_min_width).toInt()
        val buttonPaddingHorizontal: Int = getDimension(R.dimen.uxsdk_list_item_button_padding_horizontal).toInt()
        val buttonPaddingVertical: Int = getDimension(R.dimen.uxsdk_list_item_button_padding_vertical).toInt()
        listItemButton.setPadding(buttonPaddingHorizontal, buttonPaddingVertical, buttonPaddingHorizontal, buttonPaddingVertical)
    }

    private fun configureEditTextWidget() {
        initEditText()
        initHint()
        val layoutParams = LayoutParams(getDimension(R.dimen.uxsdk_list_item_edit_min_width).toInt(), ConstraintSet.WRAP_CONTENT)
        layoutParams.leftMargin = getDimension(R.dimen.uxsdk_list_item_button_padding_horizontal).toInt()
        layoutParams.rightToLeft = clickIndicatorId
        layoutParams.topToTop = guidelineTop.id
        layoutParams.bottomToBottom = guidelineBottom.id
        layoutParams.leftToRight = listItemHintTextView.id
        listItemEditTextView.layoutParams = layoutParams
        val hintLayoutParams = LayoutParams(0, ConstraintSet.WRAP_CONTENT)
        hintLayoutParams.leftToRight = guidelineCenter.id
        hintLayoutParams.rightToLeft = listItemEditTextView.id
        hintLayoutParams.topToTop = guidelineTop.id
        hintLayoutParams.bottomToBottom = guidelineBottom.id
        hintLayoutParams.horizontalBias = 1.0f
        hintLayoutParams.horizontalChainStyle = ConstraintSet.CHAIN_PACKED
        listItemHintTextView.layoutParams = hintLayoutParams
        addView(listItemEditTextView)
        addView(listItemHintTextView)
    }


    private fun configureEditTextButtonWidget() {
        initEditText()
        initHint()
        initButton()
        val layoutParams = LayoutParams(getDimension(R.dimen.uxsdk_list_item_edit_min_width).toInt(), ConstraintSet.WRAP_CONTENT)
        layoutParams.leftMargin = getDimension(R.dimen.uxsdk_list_item_button_padding_horizontal).toInt()
        layoutParams.rightToLeft = clickIndicatorId
        layoutParams.topToTop = guidelineTop.id
        layoutParams.bottomToBottom = guidelineBottom.id
        layoutParams.leftToRight = listItemHintTextView.id
        listItemEditTextView.layoutParams = layoutParams
        val hintLayoutParams = LayoutParams(0, ConstraintSet.WRAP_CONTENT)
        hintLayoutParams.leftMargin = getDimension(R.dimen.uxsdk_list_item_button_padding_horizontal).toInt()
        hintLayoutParams.leftToRight = listItemButton.id
        hintLayoutParams.rightToLeft = listItemEditTextView.id
        hintLayoutParams.topToTop = guidelineTop.id
        hintLayoutParams.bottomToBottom = guidelineBottom.id
        listItemHintTextView.layoutParams = hintLayoutParams
        val buttonLayoutParams = LayoutParams(ConstraintSet.WRAP_CONTENT, ConstraintSet.WRAP_CONTENT)
        buttonLayoutParams.rightToLeft = listItemHintTextView.id
        buttonLayoutParams.topToTop = guidelineTop.id
        buttonLayoutParams.bottomToBottom = guidelineBottom.id
        buttonLayoutParams.leftToRight = guidelineCenter.id
        buttonLayoutParams.horizontalBias = 1.0f
        buttonLayoutParams.horizontalChainStyle = ConstraintSet.CHAIN_PACKED
        listItemButton.layoutParams = buttonLayoutParams
        addView(listItemEditTextView)
        addView(listItemHintTextView)
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
     * Set the background of the item hint
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setListItemHintBackground(@DrawableRes resourceId: Int) {
        listItemHintBackground = getDrawable(resourceId)
    }

    /**
     * Set the background of the item edit text
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setListItemEditTextBackground(@DrawableRes resourceId: Int) {
        listItemEditTextBackground = getDrawable(resourceId)
    }

    /**
     * Set the text appearance of the item hint
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    fun setListItemHintTextAppearance(@StyleRes textAppearanceResId: Int) {
        listItemHintTextView.setTextAppearance(context, textAppearanceResId)
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
     * Set the text appearance of the item edit text
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    fun setListItemEditTextTextAppearance(@StyleRes textAppearanceResId: Int) {
        listItemEditTextView.setTextAppearance(context, textAppearanceResId)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        listItemButton.isEnabled = enabled
        listItemHintTextView.isEnabled = enabled
        listItemEditTextView.isEnabled = enabled
    }

    @CallSuper
    override fun onClick(view: View?) {
        super.onClick(view)
        if (view == listItemButton) {
            uiUpdateStateProcessor.onNext(UIState.ButtonClicked)
            onButtonClick()
        } else if (view == listItemEditTextView) {
            uiUpdateStateProcessor.onNext(UIState.EditStarted)
            listItemEditTextView.isCursorVisible = true
        }
    }

    @CallSuper
    override fun onListItemClick() {
        uiUpdateStateProcessor.onNext(UIState.ListItemClicked)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Android 11 is calling onAttached and onDetached when keyboard is shown.
        // This disposes the disposables mid action. To avoid misbehavior this is a
        // temporary fix.
        if (context is Activity && Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            val window: Window = (context as Activity).window
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        }
    }

    private fun isDoneActionClicked(actionId: Int, keyEvent: KeyEvent?): Boolean {
        return actionId == EditorInfo.IME_ACTION_DONE
                || actionId == EditorInfo.IME_FLAG_NO_EXTRACT_UI
                || actionId == EditorInfo.IME_ACTION_NEXT
                || actionId == EditorInfo.IME_FLAG_NAVIGATE_NEXT
                || actionId == EditorInfo.IME_ACTION_GO
                || (keyEvent != null
                && keyEvent.action == KeyEvent.ACTION_DOWN
                && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER)
    }

    private fun hideKeyboardFrom() {
        val imm: InputMethodManager = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    /**
     * Called when button click event occurs
     */
    abstract fun onButtonClick()

    /**
     * Called when keyboard is hidden
     */
    abstract fun onKeyboardDoneAction()

    /**
     * Called when user changes text
     */
    abstract fun onEditorTextChanged(currentText: String?)


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
         * Update when user clicks edit text to modify
         * value
         */
        object EditStarted : UIState()

        /**
         * Update when user finishes edit action
         */
        object EditFinished : UIState()

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
         *  item name, item hint and editable value
         */
        EDIT,

        /**
         *  The type represents the item with icon,
         *  item name, button and item hint and editable value
         */
        EDIT_BUTTON
    }

}