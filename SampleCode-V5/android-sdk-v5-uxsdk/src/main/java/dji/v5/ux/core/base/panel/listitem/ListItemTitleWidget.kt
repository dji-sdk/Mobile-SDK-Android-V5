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
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.*
import androidx.constraintlayout.widget.Guideline
import androidx.core.content.res.use
import dji.v5.ux.R
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.extension.*
import dji.v5.ux.core.util.ViewIDGenerator
import kotlin.math.roundToInt


/**
 * This is the base class used for list item. The class represents
 * the item title and item icon.
 * @property defaultStyle - Resource id for tyle used for defining the default setup
 * of the widget.
 */
abstract class ListItemTitleWidget<T> @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        @StyleRes protected val defaultStyle: Int
) : ConstraintLayoutWidget<T>(context, attrs, defStyleAttr), View.OnClickListener {

    private val listItemTitleTextView = findViewById<TextView>(R.id.text_view_list_item_title)
    private val listItemTitleImageView = findViewById<ImageView>(R.id.image_view_title_icon)
    protected val guidelineLeft: Guideline = findViewById(R.id.guideline_left)
    protected val guidelineTop: Guideline = findViewById(R.id.guideline_top)
    protected val guidelineRight: Guideline = findViewById(R.id.guideline_right)
    protected val guidelineBottom: Guideline = findViewById(R.id.guideline_bottom)
    protected val guidelineCenter: Guideline = findViewById(R.id.guideline_column)
    private val clickIndicatorImageView: ImageView = findViewById(R.id.image_view_chevron)

    /**
     * ID of the click indicator view
     */
    val clickIndicatorId = clickIndicatorImageView.id

    /**
     * Icon for showing the click indicator
     */
    var clickIndicatorIcon: Drawable?
        get() = clickIndicatorImageView.imageDrawable
        set(@Nullable value) {
            clickIndicatorImageView.imageDrawable = value
        }

    /**
     * Toggle clickable functionality of the list item
     */
    var listItemClickable: Boolean = false
        set(value) {
            field = value
            if (value) {
                clickIndicatorImageView.visibility = View.VISIBLE
            } else {
                clickIndicatorImageView.visibility = View.INVISIBLE
            }
        }

    /**
     * Color of label text when disconnected
     */
    @ColorInt
    var disconnectedValueColor: Int = getColor(R.color.uxsdk_white_60_percent)

    /**
     * Color of the label text when state is normal
     */
    @ColorInt
    var normalValueColor: Int = getColor(R.color.uxsdk_status_normal)

    /**
     * Color of the label text when state is warning
     */
    @ColorInt
    var warningValueColor: Int = getColor(R.color.uxsdk_status_warning)

    /**
     * Color of the label text when state is error
     */
    @ColorInt
    var errorValueColor: Int = getColor(R.color.uxsdk_status_error)

    /**
     * The list item title string
     */
    var listItemTitle: String?
        get() = listItemTitleTextView.text.toString()
        set(@Nullable value) {
            listItemTitleTextView.text = value
        }

    /**
     * The list item title icon
     */
    var listItemTitleIcon: Drawable?
        get() = listItemTitleImageView.imageDrawable
        set(@Nullable value) {
            listItemTitleImageView.imageDrawable = value
        }

    /**
     * The color of the icon
     */
    @get:ColorInt
    var listItemTitleIconColor: Int = getColor(R.color.uxsdk_white)
        set(@ColorInt value) {
            field = value
            listItemTitleImageView.setColorFilter(value, PorterDuff.Mode.SRC_IN)
        }

    /**
     * The size of the list title text
     */
    var listItemTitleTextSize: Float
        @Dimension get() = listItemTitleTextView.textSize
        set(@Dimension value) {
            listItemTitleTextView.textSize = value
        }

    /**
     * The color of the list item title text
     */
    var listItemTitleTextColor: Int
        @ColorInt get() = listItemTitleTextView.textColor
        set(@ColorInt value) {
            listItemTitleTextView.textColor = value
        }

    /**
     * The color state list of the list item title
     */
    var listItemTitleTextColors: ColorStateList?
        get() = listItemTitleTextView.textColorStateList
        set(value) {
            listItemTitleTextView.textColorStateList = value
        }

    /**
     * The background of the list item title
     */
    var listItemTitleBackground: Drawable?
        get() = listItemTitleTextView.background
        set(value) {
            listItemTitleTextView.background = value
        }

    /**
     * Left padding of the list item content
     */
    var contentPaddingLeft: Int = getDimension(R.dimen.uxsdk_pre_flight_checklist_item_padding).toInt()
        set(value) {
            field = value
            guidelineLeft.setGuidelineBegin(value)
        }

    /**
     * Top padding of the list item content
     */
    var contentPaddingTop: Int = getDimension(R.dimen.uxsdk_pre_flight_checklist_item_padding).toInt()
        set(value) {
            field = value
            guidelineTop.setGuidelineBegin(value)
        }

    /**
     * Right padding of the list item content
     */
    var contentPaddingRight: Int = getDimension(R.dimen.uxsdk_pre_flight_checklist_item_padding).toInt()
        set(value) {
            field = value
            guidelineRight.setGuidelineEnd(value)
        }

    /**
     * Bottom padding of the list item content
     */
    var contentPaddingBottom: Int = getDimension(R.dimen.uxsdk_pre_flight_checklist_item_padding).toInt()
        set(value) {
            field = value
            guidelineBottom.setGuidelineEnd(value)
        }


    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        View.inflate(context, R.layout.uxsdk_list_item_title_widget, this)
    }

    init {
        if (id == View.NO_ID) {
            id = ViewIDGenerator.generateViewId()
        }
        isClickable = true
        setOnClickListener(this)
        val padding = (getDimension(R.dimen.uxsdk_pre_flight_checklist_item_padding)).roundToInt()
        setPadding(padding, 0, padding, 0)
        initAttributes(context, attrs)
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.ListItemTitleWidget,0, defaultStyle).use { typedArray ->
            typedArray.getResourceIdAndUse(R.styleable.ListItemTitleWidget_uxsdk_list_item_title_appearance) {
                setListItemTitleTextAppearance(it)
            }
            typedArray.getDrawableAndUse(R.styleable.ListItemTitleWidget_uxsdk_list_item_icon) {
                listItemTitleIcon = it
            }
            typedArray.getColorAndUse(R.styleable.ListItemTitleWidget_uxsdk_list_item_icon_color) {
                listItemTitleIconColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.ListItemTitleWidget_uxsdk_list_item_title_text_background) {
                listItemTitleBackground = it
            }
            typedArray.getDimensionAndUse(R.styleable.ListItemTitleWidget_uxsdk_list_item_title_text_size) {
                listItemTitleTextSize = it
            }
            typedArray.getColorAndUse(R.styleable.ListItemTitleWidget_uxsdk_list_item_title_text_color) {
                listItemTitleTextColor = it
            }
            typedArray.getColorStateListAndUse(R.styleable.ListItemTitleWidget_uxsdk_list_item_title_text_color) {
                listItemTitleTextColors = it
            }
            typedArray.getDimensionAndUse(R.styleable.ListItemTitleWidget_uxsdk_list_item_padding_left) {
                contentPaddingLeft = it.toInt()
            }
            typedArray.getDimensionAndUse(R.styleable.ListItemTitleWidget_uxsdk_list_item_padding_top) {
                contentPaddingTop = it.toInt()
            }
            typedArray.getDimensionAndUse(R.styleable.ListItemTitleWidget_uxsdk_list_item_padding_right) {
                contentPaddingRight = it.toInt()
            }
            typedArray.getDimensionAndUse(R.styleable.ListItemTitleWidget_uxsdk_list_item_padding_bottom) {
                contentPaddingBottom = it.toInt()
            }
            typedArray.getColorAndUse(R.styleable.ListItemTitleWidget_uxsdk_list_item_disconnected_color) {
                disconnectedValueColor = it
            }
            typedArray.getColorAndUse(R.styleable.ListItemTitleWidget_uxsdk_list_item_normal_color) {
                normalValueColor = it
            }
            typedArray.getColorAndUse(R.styleable.ListItemTitleWidget_uxsdk_list_item_warning_color) {
                warningValueColor = it
            }
            typedArray.getColorAndUse(R.styleable.ListItemTitleWidget_uxsdk_list_item_error_color) {
                errorValueColor = it
            }
            listItemTitle =
                    typedArray.getString(R.styleable.ListItemTitleWidget_uxsdk_list_item_title,
                            getString(R.string.uxsdk_string_default_value))
            typedArray.getDrawableAndUse(R.styleable.ListItemTitleWidget_uxsdk_list_item_click_indicator_icon) {
                clickIndicatorIcon = it
            }
            typedArray.getBooleanAndUse(R.styleable.ListItemTitleWidget_uxsdk_list_item_clickable, false) {
                listItemClickable = it
            }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        clickIndicatorImageView.isEnabled = enabled
    }

    override fun onClick(v: View?) {
        if (v?.id == id && listItemClickable) {
            onListItemClick()
        }
    }

    abstract fun onListItemClick()

    /**
     * Set the background of the list item title
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setListItemTitleBackground(@DrawableRes resourceId: Int) {
        listItemTitleBackground = getDrawable(resourceId)
    }

    /**
     * Set the text appearance of the title
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    fun setListItemTitleTextAppearance(@StyleRes textAppearanceResId: Int) {
        listItemTitleTextView.setTextAppearance(context, textAppearanceResId)
    }

    /**
     * Set padding to the content of the list item
     */
    fun setContentPadding(left: Int, top: Int, right: Int, bottom: Int) {
        contentPaddingLeft = left
        contentPaddingTop = top
        contentPaddingRight = right
        contentPaddingBottom = bottom
    }


}