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

package dji.v5.ux.core.ui

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.annotation.*
import androidx.constraintlayout.widget.ConstraintLayout
import dji.v5.ux.R
import dji.v5.ux.core.extension.textColor
import dji.v5.ux.core.extension.textColorStateList
import dji.v5.ux.core.util.ViewUtil

private const val SLIDE_MAX_THRESHOLD = 100
private const val SLIDE_COMPLETE_THRESHOLD = 95
private const val SLIDE_START_THRESHOLD = 0

/**
 * A dialog where the positive action is represented by a [SlideAndFillSeekBar]. The negative
 * action is represented by a button at the bottom of the dialog. The dialog contains a title,
 * a message, and a checkbox.
 */
class SlidingDialog @JvmOverloads constructor(
        context: Context,
        @StyleRes styleId: Int,
        @param:DimenRes private val widthId: Int = R.dimen.uxsdk_sliding_dialog_width
) : Dialog(context, styleId), View.OnClickListener, SeekBar.OnSeekBarChangeListener,
        CompoundButton.OnCheckedChangeListener {
    //region Fields
    private var container: ConstraintLayout
    private var iconImageView: ImageView
    private var cancelTextView: TextView
    private var actionTextView: TextView
    private var titleTextView: TextView
    private var messageTextView: TextView
    private var checkbox: CheckBox
    private var seekBar: SlideAndFillSeekBar
    private var onEventListener: OnEventListener? = null

    /**
     * The size of the dialog title text
     */
    var dialogTitleTextSize: Float
        @Dimension
        get() = titleTextView.textSize
        set(@Dimension textSize) {
            titleTextView.textSize = textSize
        }

    /**
     * The color of the dialog title text
     */
    var dialogTitleTextColor: Int
        @ColorInt
        get() = titleTextView.textColor
        set(@ColorInt color) {
            titleTextView.textColor = color
        }

    /**
     * The background of the dialog title
     */
    var dialogTitleBackground: Drawable?
        get() = titleTextView.background
        set(value) {
            titleTextView.background = value
        }

    /**
     * The size of the dialog message text
     */
    var dialogMessageTextSize: Float
        @Dimension
        get() = messageTextView.textSize
        set(@Dimension textSize) {
            messageTextView.textSize = textSize
        }

    /**
     * The color of the dialog message text
     */
    var dialogMessageTextColor: Int
        @ColorInt
        get() = messageTextView.textColor
        set(@ColorInt color) {
            messageTextView.textColor = color
        }

    /**
     * The background of the dialog message
     */
    var dialogMessageBackground: Drawable?
        get() = messageTextView.background
        set(value) {
            messageTextView.background = value
        }

    /**
     * The size of the check box message text
     */
    var checkBoxMessageTextSize: Float
        @Dimension
        get() = checkbox.textSize
        set(@Dimension textSize) {
            checkbox.textSize = textSize
        }

    /**
     * The color of the check box message text
     */
    var checkBoxMessageTextColor: Int
        @ColorInt
        get() = checkbox.textColor
        set(@ColorInt color) {
            checkbox.textColor = color
        }

    /**
     * The background of the check box message
     */
    var checkBoxMessageBackground: Drawable?
        get() = checkbox.background
        set(value) {
            checkbox.background = value
        }

    /**
     * Whether the check box is checked
     */
    var checkBoxChecked: Boolean
        get() = checkbox.isChecked
        set(value) {
            checkbox.isChecked = value
        }

    /**
     * The size of the cancel button text
     */
    var cancelTextSize: Float
        @Dimension
        get() = cancelTextView.textSize
        set(@Dimension textSize) {
            cancelTextView.textSize = textSize
        }

    /**
     * The color of the cancel button text
     */
    var cancelTextColor: Int
        @ColorInt
        get() = cancelTextView.textColor
        set(@ColorInt color) {
            cancelTextView.textColor = color
        }

    /**
     * The colors of the cancel button text
     */
    var cancelTextColors: ColorStateList?
        get() = cancelTextView.textColorStateList
        set(colors) {
            cancelTextView.textColorStateList = colors
        }

    /**
     * The background of the cancel button
     */
    var cancelBackground: Drawable?
        get() = cancelTextView.background
        set(value) {
            cancelTextView.background = value
        }

    /**
     * The size of the action message text
     */
    var actionMessageTextSize: Float
        @Dimension
        get() = actionTextView.textSize
        set(@Dimension textSize) {
            actionTextView.textSize = textSize
        }

    /**
     * The color of the action message text
     */
    var actionMessageTextColor: Int
        @ColorInt
        get() = actionTextView.textColor
        set(@ColorInt color) {
            actionTextView.textColor = color
        }

    /**
     * The background of the action message
     */
    var actionMessageBackground: Drawable?
        get() = actionTextView.background
        set(value) {
            actionTextView.background = value
        }

    /**
     * The icon to the right of the action message
     */
    var actionIcon: Drawable?
        get() = actionTextView.compoundDrawables[2]
        set(icon) {
            actionTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, icon, null)
        }

    /**
     * The color of the slider thumb
     */
    var actionSliderThumbColor: Int
        @ColorInt
        get() = seekBar.thumbNormalColor
        set(@ColorInt color) {
            seekBar.thumbNormalColor = color
        }

    /**
     * The color of the slider thumb when selected
     */
    var actionSliderThumbSelectedColor: Int
        @ColorInt
        get() = seekBar.thumbSelectedColor
        set(@ColorInt color) {
            seekBar.thumbSelectedColor = color
        }

    /**
     * The fill color of the slider
     */
    var actionSliderFillColor: Int
        @ColorInt
        get() = seekBar.progressColor
        set(@ColorInt color) {
            seekBar.progressColor = color
        }

    /**
     * The background of the dialog
     */
    var background: Drawable?
        get() = container.background
        set(value) {
            container.background = value
        }
    //endregion

    /**
     * The listener that handles slider, button, and check box interaction
     */
    interface OnEventListener {
        /**
         * Event when the cancel button is clicked
         *
         * @param dialog The dialog that was interacted with
         */
        fun onCancelClick(dialog: DialogInterface?)

        /**
         * Event when the [SlideAndFillSeekBar] has been filled
         *
         * @param dialog The dialog that was interacted with
         * @param checked `true` if the [SlideAndFillSeekBar] is filled
         */
        fun onSlideChecked(dialog: DialogInterface?, checked: Boolean)

        /**
         * Event when the check box is checked
         *
         * @param dialog The dialog that was interacted with
         * @param checked `true` if the check box is checked
         */
        fun onCheckBoxChecked(dialog: DialogInterface?, checked: Boolean)
    }

    //region Lifecycle
    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.uxsdk_dialog_sliding_action)
        container = findViewById(R.id.container)
        iconImageView = findViewById(R.id.image_view_dialog_icon)
        cancelTextView = findViewById(R.id.text_view_cancel)
        actionTextView = findViewById(R.id.text_view_action)
        titleTextView = findViewById(R.id.text_view_dialog_title)
        messageTextView = findViewById(R.id.text_view_dialog_description)
        checkbox = findViewById(R.id.checkbox)
        checkbox.setOnCheckedChangeListener(this)
        seekBar = findViewById(R.id.slide_seek_bar)
        seekBar.setOnSeekBarChangeListener(this)
        cancelTextView.setOnClickListener(this)
        seekBar.setPadding(0, 0, 0, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        adjustAttrs(context.resources.getDimension(widthId).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT, 0,
                Gravity.CENTER,
                cancelable = true,
                cancelTouchOutside = false)
    }

    override fun onClick(v: View) {
        val id = v.id
        if (id == R.id.text_view_cancel) {
            onEventListener?.onCancelClick(this)
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        onEventListener?.onCheckBoxChecked(this, isChecked)
    }
    //endregion

    //region private methods
    private fun handleSbStopTrack() {
        val progress = seekBar.progress
        if (progress >= SLIDE_COMPLETE_THRESHOLD) {
            seekBar.progress = SLIDE_MAX_THRESHOLD
            setSlideChecked(true)
        } else {
            seekBar.progress = SLIDE_START_THRESHOLD
            setSlideChecked(false)
        }
    }

    private fun setSlideChecked(checked: Boolean) {
        onEventListener?.onSlideChecked(this, checked)
    }
    //endregion

    //region public methods
    /**
     * Set the listener for the slider, button, and check box interaction
     *
     * @param listener The listener for this dialog
     */
    fun setOnEventListener(listener: OnEventListener?) {
        onEventListener = listener
    }

    /**
     * Configure the dialog
     *
     * @param width The width of the dialog
     * @param height The height of the dialog
     * @param yOffset The y offset of the dialog
     * @param gravity The gravity of the dialog
     * @param cancelable Whether the dialog can be canceled
     * @param cancelTouchOutside Whether the dialog is canceled when a touch occurs outside
     */
    fun adjustAttrs(width: Int,
                    height: Int,
                    yOffset: Int,
                    gravity: Int,
                    cancelable: Boolean,
                    cancelTouchOutside: Boolean) {
        val attrs = window?.attributes
        attrs?.width = width
        attrs?.height = height
        attrs?.y = yOffset
        attrs?.flags = attrs?.flags?.and(WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv())
        attrs?.gravity = gravity
        window?.attributes = attrs
        window?.setWindowAnimations(R.style.UXSDKDialogWindowAnim)
        setCancelable(cancelable)
        setCanceledOnTouchOutside(cancelTouchOutside)
    }

    override fun show() {
        window?.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        seekBar.progress = SLIDE_START_THRESHOLD
        super.show()
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        //方法暂时未实现
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        //方法暂时未实现
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        handleSbStopTrack()
    }

    /**
     * Set the dialog title
     *
     * @param titleId The resource ID of the dialog title
     */
    fun setDialogTitleRes(@StringRes titleId: Int): SlidingDialog {
        titleTextView.setText(titleId)
        return this
    }

    /**
     * Set the dialog title
     *
     * @param title The dialog title
     */
    fun setDialogTitle(title: String): SlidingDialog {
        titleTextView.text = title
        return this
    }

    /**
     * Set the dialog message
     *
     * @param messageId The resource ID of the dialog message
     */
    fun setDialogMessageRes(@StringRes messageId: Int): SlidingDialog {
        messageTextView.setText(messageId)
        return this
    }

    /**
     * Set the dialog message
     *
     * @param message The dialog message
     */
    fun setDialogMessage(message: String): SlidingDialog {
        messageTextView.text = message
        return this
    }

    /**
     * Set the color of the dialog title
     *
     * @param color The color of the dialog title
     */
    fun setDialogTitleTextColor(@ColorInt color: Int): SlidingDialog {
        titleTextView.setTextColor(color)
        return this
    }

    /**
     * Set the color of the dialog message
     *
     * @param color The color of the dialog message
     */
    fun setDialogMessageTextColor(@ColorInt color: Int): SlidingDialog {
        messageTextView.setTextColor(color)
        return this
    }

    /**
     * Set the size of the dialog title
     *
     * @param textSize The size of the dialog title
     */
    fun setDialogTitleTextSize(@Dimension textSize: Float): SlidingDialog {
        titleTextView.textSize = textSize
        return this
    }

    /**
     * Set the size of the dialog message
     *
     * @param textSize The size of the dialog message
     */
    fun setDialogMessageTextSize(@Dimension textSize: Float): SlidingDialog {
        messageTextView.textSize = textSize
        return this
    }

    /**
     * Set the background of the dialog title
     *
     * @param drawable The background of the dialog title
     */
    fun setDialogTitleBackground(drawable: Drawable?): SlidingDialog {
        titleTextView.background = drawable
        return this
    }

    /**
     * Set the background of the dialog message
     *
     * @param drawable The background of the dialog message
     */
    fun setDialogMessageBackground(drawable: Drawable?): SlidingDialog {
        messageTextView.background = drawable
        return this
    }

    /**
     * Set the text appearance of the dialog title
     *
     * @param textAppearance The text appearance of the dialog title
     */
    fun setDialogTitleTextAppearance(@StyleRes textAppearance: Int): SlidingDialog {
        titleTextView.setTextAppearance(context, textAppearance)
        return this
    }

    /**
     * Set the text appearance of the dialog message
     *
     * @param textAppearance The text appearance of the dialog message
     */
    fun setDialogMessageTextAppearance(@StyleRes textAppearance: Int): SlidingDialog {
        messageTextView.setTextAppearance(context, textAppearance)
        return this
    }

    /**
     * Set the visibility of the check box
     *
     * @param visible Whether the check box is visible
     */
    fun setCheckBoxVisibility(visible: Boolean): SlidingDialog {
        checkbox.visibility = if (visible) View.VISIBLE else View.GONE
        return this
    }

    /**
     * Set the check box message
     *
     * @param messageId The resource ID of the check box message
     */
    fun setCheckBoxMessageRes(@StringRes messageId: Int): SlidingDialog {
        checkbox.setText(messageId)
        return this
    }

    /**
     * Set the check box message
     *
     * @param message The check box message
     */
    fun setCheckBoxMessage(message: String): SlidingDialog {
        checkbox.text = message
        return this
    }

    /**
     * Set whether the check box is checked
     *
     * @param isChecked Whether the check box is checked
     */
    fun setCheckBoxChecked(isChecked: Boolean): SlidingDialog {
        checkbox.isChecked = isChecked
        return this
    }

    /**
     * Set the color of the check box message
     *
     * @param color The color of the check box message
     */
    fun setCheckBoxMessageTextColor(@ColorInt color: Int): SlidingDialog {
        checkbox.setTextColor(color)
        return this
    }

    /**
     * Set the size of the check box message
     *
     * @param textSize The size of the check box message
     */
    fun setCheckBoxMessageTextSize(@Dimension textSize: Float): SlidingDialog {
        checkbox.textSize = textSize
        return this
    }

    /**
     * Set the background of the check box message
     *
     * @param drawable The background of the check box message
     */
    fun setCheckBoxMessageBackground(drawable: Drawable?): SlidingDialog {
        checkbox.background = drawable
        return this
    }

    /**
     * Set the text appearance of the check box message
     *
     * @param textAppearance The text appearance of the check box message
     */
    fun setCheckBoxMessageTextAppearance(@StyleRes textAppearance: Int): SlidingDialog {
        checkbox.setTextAppearance(context, textAppearance)
        return this
    }

    /**
     * Set the action message
     *
     * @param actionId The resource ID of the action message
     */
    fun setActionMessageRes(@StringRes actionId: Int): SlidingDialog {
        actionTextView.setText(actionId)
        return this
    }

    /**
     * Set the action message
     *
     * @param action The action message
     */
    fun setActionMessage(action: String): SlidingDialog {
        actionTextView.text = action
        return this
    }

    /**
     * Set the color of the action message
     *
     * @param color The color of the action message
     */
    fun setActionMessageTextColor(@ColorInt color: Int): SlidingDialog {
        actionTextView.setTextColor(color)
        return this
    }

    /**
     * Set the size of the action message
     *
     * @param textSize The size of the action message
     */
    fun setActionMessageTextSize(@Dimension textSize: Float): SlidingDialog {
        actionTextView.textSize = textSize
        return this
    }

    /**
     * Set the background of the action message
     *
     * @param drawable The background of the action message
     */
    fun setActionMessageBackground(drawable: Drawable?): SlidingDialog {
        actionTextView.background = drawable
        return this
    }

    /**
     * Set the text appearance of the action message
     *
     * @param textAppearance The text appearance of the action message
     */
    fun setActionMessageTextAppearance(@StyleRes textAppearance: Int): SlidingDialog {
        actionTextView.setTextAppearance(context, textAppearance)
        return this
    }

    /**
     * Set the icon next to the dialog title
     *
     * @param drawable The icon next to the dialog title
     */
    fun setDialogIcon(drawable: Drawable?): SlidingDialog {
        iconImageView.setImageDrawable(drawable)
        return this
    }

    /**
     * Set the tint of the icon next to the dialog title
     *
     * @param color The tint of the icon
     */
    fun setDialogIconTint(@ColorInt color: Int): SlidingDialog {
        ViewUtil.tintImage(iconImageView, color)
        return this
    }

    /**
     * Set the color of the cancel button text
     *
     * @param color The color of the cancel text
     */
    fun setCancelTextColor(@ColorInt color: Int): SlidingDialog {
        cancelTextView.setTextColor(color)
        return this
    }

    /**
     * Set the size of the cancel button text
     *
     * @param textSize The size of the cancel text
     */
    fun setCancelTextSize(@Dimension textSize: Float): SlidingDialog {
        cancelTextView.textSize = textSize
        return this
    }

    /**
     * Set the background of the cancel button
     *
     * @param drawable The background of the cancel button
     */
    fun setCancelBackground(drawable: Drawable?): SlidingDialog {
        cancelTextView.background = drawable
        return this
    }

    /**
     * Set the text appearance of the cancel button
     *
     * @param textAppearance The text appearance of the cancel button
     */
    fun setCancelTextAppearance(@StyleRes textAppearance: Int): SlidingDialog {
        cancelTextView.setTextAppearance(context, textAppearance)
        return this
    }
    //endregion
}