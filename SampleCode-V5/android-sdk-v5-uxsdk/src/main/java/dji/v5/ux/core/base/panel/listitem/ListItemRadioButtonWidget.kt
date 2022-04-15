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
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.annotation.StyleRes
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.use
import androidx.core.view.get
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.processors.PublishProcessor
import dji.v5.ux.R
import dji.v5.ux.core.extension.*
import dji.v5.ux.core.util.ViewIDGenerator

/**
 * This is the base class to be used for radio button type list item
 * The class represents the item with icon, item name and radio group options
 */
abstract class ListItemRadioButtonWidget<T> @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defaultStyle: Int
) : ListItemTitleWidget<T>(context, attrs, defStyleAttr, defaultStyle),
        RadioGroup.OnCheckedChangeListener {

    protected val uiUpdateStateProcessor: PublishProcessor<UIState> = PublishProcessor.create()

    private val radioGroup: RadioGroup = RadioGroup(context)

    /**
     * Background selector of radio button in center
     *
     * Recommended states in selector
     * 1. android_state_enabled = false
     * 2. android_state_checked = false
     * 3. android_state_checked = true
     */
    var centerOptionBackgroundSelector: Drawable? = getDrawable(R.drawable.uxsdk_selector_radio_button_middle)

    /**
     * Background selector of radio button on the extreme left
     * i.e. the first item
     *
     * Recommended states in selector
     * 1. android_state_enabled = false
     * 2. android_state_checked = false
     * 3. android_state_checked = true
     */
    var firstOptionBackgroundSelector: Drawable? = getDrawable(R.drawable.uxsdk_selector_radio_button_first)

    /**
     * Background selector of radio button on the extreme right
     * i.e. the last item
     *
     * Recommended states in selector
     * 1. android_state_enabled = false
     * 2. android_state_checked = false
     * 3. android_state_checked = true
     */
    var lastOptionBackgroundSelector: Drawable? = getDrawable(R.drawable.uxsdk_selector_radio_button_last)


    /**
     * Text color state list for radio button text
     *
     * Recommended states in selector
     * 1. android_state_enabled = false
     * 2. android_state_checked = false
     * 3. android_state_checked = true
     */
    var optionColorStateList: ColorStateList? = resources.getColorStateList(R.color.uxsdk_selector_radio_button_colors)
        set(value) {
            field = value
            for (i in 0 until radioGroup.childCount) {
                val radioButton: RadioButton? = radioGroup.getChildAt(i) as RadioButton
                radioButton?.textColorStateList = field
            }
        }

    /**
     * Text size for the radio button text
     */
    var optionTextSize: Float = getDimension(R.dimen.uxsdk_list_item_radio_button_text_size)
        set(value) {
            field = value
            for (i in 0 until radioGroup.childCount) {
                val radioButton: RadioButton? = radioGroup.getChildAt(i) as RadioButton
                radioButton?.textSize = field
            }
        }

    /**
     * The count of option items
     */
    val optionCount: Int = radioGroup.childCount

    /**
     * Get the [UIState] updates
     */
    fun getUIStateUpdates(): Flowable<UIState> {
        return uiUpdateStateProcessor.onBackpressureBuffer()
    }

    init {
        radioGroup.id = ViewIDGenerator.generateViewId()
        radioGroup.orientation = LinearLayout.HORIZONTAL
        radioGroup.setOnCheckedChangeListener(this)
        radioGroup.gravity = Gravity.CENTER_VERTICAL or Gravity.END
        val layoutParams = LayoutParams(0, ConstraintSet.WRAP_CONTENT)
        layoutParams.rightToLeft = clickIndicatorId
        layoutParams.topToTop = guidelineTop.id
        layoutParams.bottomToBottom = guidelineBottom.id
        layoutParams.leftToRight = guidelineCenter.id
        radioGroup.layoutParams = layoutParams
        addView(radioGroup)
        val paddingValue = resources.getDimension(R.dimen.uxsdk_pre_flight_checklist_item_padding).toInt()
        setContentPadding(0, paddingValue, 0, paddingValue)
        initAttributes(context, attrs)
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.ListItemRadioButtonWidget, 0, defaultStyle).use { typedArray ->
            typedArray.getResourceIdAndUse(R.styleable.ListItemRadioButtonWidget_uxsdk_option_text_appearance) {
                setOptionTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.ListItemRadioButtonWidget_uxsdk_option_text_size) {
                optionTextSize = it
            }
            typedArray.getDrawableAndUse(R.styleable.ListItemRadioButtonWidget_uxsdk_center_option_background_selector) {
                centerOptionBackgroundSelector = it
            }
            typedArray.getDrawableAndUse(R.styleable.ListItemRadioButtonWidget_uxsdk_first_option_background_selector) {
                firstOptionBackgroundSelector = it
            }
            typedArray.getDrawableAndUse(R.styleable.ListItemRadioButtonWidget_uxsdk_last_option_background_selector) {
                lastOptionBackgroundSelector = it
            }
            typedArray.getColorStateListAndUse(R.styleable.ListItemRadioButtonWidget_uxsdk_option_color_state_list) {
                optionColorStateList = it
            }
        }
    }

    protected fun addOptionToGroup(label: String): Int {
        val radioButton = RadioButton(context)
        radioButton.id = ViewIDGenerator.generateViewId()
        radioButton.text = label
        radioButton.textColorStateList = optionColorStateList
        radioButton.isChecked = true
        radioButton.buttonDrawable = null
        radioButton.textSize = optionTextSize
        val paddingValue = resources.getDimension(R.dimen.uxsdk_pre_flight_checklist_item_padding).toInt()
        radioButton.setPadding(paddingValue, 0, paddingValue, 0)
        radioGroup.addView(radioButton, radioGroup.childCount)
        restyleRadioGroup()
        return radioGroup.childCount - 1
    }

    protected fun removeOptionFromGroup(index: Int) {
        radioGroup.removeViewAt(index)
        restyleRadioGroup()
    }

    protected fun setSelected(index: Int) {
        radioGroup.setOnCheckedChangeListener(null)
        val radioButton: RadioButton? = radioGroup.getChildAt(index) as RadioButton
        radioButton?.isChecked = true
        radioGroup.setOnCheckedChangeListener(this)
    }

    private fun restyleRadioGroup() {
        when (val childCount = radioGroup.childCount) {
            1 -> radioGroup[0].background = centerOptionBackgroundSelector
            2 -> {
                radioGroup[0].background = firstOptionBackgroundSelector
                radioGroup[childCount - 1].background = lastOptionBackgroundSelector
            }
            else -> {
                radioGroup[0].background = firstOptionBackgroundSelector
                radioGroup[childCount - 1].background = lastOptionBackgroundSelector
                for (i in 1 until childCount - 1) {
                    radioGroup[i].background = centerOptionBackgroundSelector
                }
            }
        }


    }

    override fun onCheckedChanged(group: RadioGroup?, radioButtonId: Int) {
        for (i in 0 until radioGroup.childCount) {
            if (radioGroup[i].id == radioButtonId) {
                val radioButton: RadioButton? = findViewById(radioButtonId)
                if (radioButton != null) {
                    uiUpdateStateProcessor.onNext(UIState.OptionSelected(i,
                            radioButton.text.toString()))
                    onOptionTapped(i, radioButton.text.toString())
                }
                break
            }

        }
    }

    override fun onListItemClick() {
        uiUpdateStateProcessor.onNext(UIState.ListItemClicked)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        for (i in 0 until radioGroup.childCount) {
            radioGroup[i].isEnabled = enabled
        }
    }

    abstract fun onOptionTapped(optionIndex: Int, optionLabel: String)

    /**
     * Set the text of the radio button by index
     */
    protected fun setOptionTextByIndex(index: Int, text: String) {
        val radioButton = radioGroup.getChildAt(index) as RadioButton?
        radioButton?.text = text
    }

    /**
     * Set the text of the radio button by index
     */
    fun setOptionTextAppearance(@StyleRes textAppearance: Int) {
        for (i in 0 until radioGroup.childCount) {
            val radioButton: RadioButton? = radioGroup.getChildAt(i) as RadioButton
            radioButton?.setTextAppearance(context, textAppearance)
        }

    }

    /**
     * Widget UI update State
     */
    sealed class UIState {
        /**
         * List Item click update
         */
        object ListItemClicked : UIState()

        /**
         * Option click update
         */
        data class OptionSelected(val optionIndex: Int, val optionLabel: String) : UIState()

        /**
         *  Dialog shown update
         */
        data class DialogDisplayed(val info: Any?) : UIState()

        /**
         *  Dialog action dismiss
         */
        data class DialogDismissed(val info: Any?) : UIState()

        /**
         *  Never show again checkbox checked
         */
        data class NeverShowAgainCheckChanged(val isChecked: Boolean) : UIState()

    }

    protected companion object {
        const val INVALID_OPTION_INDEX = -1
    }

}