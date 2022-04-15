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

package dji.v5.ux.core.base.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.*
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Guideline
import androidx.core.content.res.use
import dji.v5.ux.R
import dji.v5.ux.core.base.widget.BaseTelemetryWidget.WidgetType
import dji.v5.ux.core.extension.*
import dji.v5.ux.core.util.UnitConversionUtil
import dji.v5.ux.core.util.ViewIDGenerator
import java.text.DecimalFormat

/**
 * Base class for telemetry widgets
 * @property widgetType - The [WidgetType] for the widget.
 * @property widgetTheme - Resource id for styling the widget.
 * @property defaultStyle - Resource id for style used for defining the default setup
 * of the widget.
 */
abstract class BaseTelemetryWidget<T> @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        val widgetType: WidgetType,
        protected val widgetTheme: Int = 0,
        @StyleRes protected val defaultStyle: Int
) : ConstraintLayoutWidget<T>(context, attrs, defStyleAttr) {

    private val guidelineLeft: Guideline = findViewById(R.id.guideline_left)
    private val guidelineTop: Guideline = findViewById(R.id.guideline_top)
    private val guidelineRight: Guideline = findViewById(R.id.guideline_right)
    private val guidelineBottom: Guideline = findViewById(R.id.guideline_bottom)
    private val labelTextView: TextView = findViewById(R.id.text_view_label)
    private val valueTextView: TextView = findViewById(R.id.text_view_value)
    private val unitTextView: TextView = findViewById(R.id.text_view_unit)
    private val imageView: ImageView = ImageView(context)
    protected abstract val metricDecimalFormat: DecimalFormat
    protected abstract val imperialDecimalFormat: DecimalFormat

    //region color customizations
    /**
     * Color of the value is in error state
     */
    @ColorInt
    var errorValueColor: Int = getColor(R.color.uxsdk_red)

    /**
     * Color of the value is in normal state
     */
    @ColorInt
    var normalValueColor: Int = getColor(R.color.uxsdk_white)

    //endregion


    //region widget customizations
    /**
     * Left padding of the list item content
     */
    var contentPaddingLeft: Int = getDimension(R.dimen.uxsdk_telemetry_item_padding).toInt()
        set(value) {
            field = value
            guidelineLeft.setGuidelineBegin(value)
        }

    /**
     * Top padding of the list item content
     */
    var contentPaddingTop: Int = getDimension(R.dimen.uxsdk_telemetry_item_padding).toInt()
        set(value) {
            field = value
            guidelineTop.setGuidelineBegin(value)
        }

    /**
     * Right padding of the list item content
     */
    var contentPaddingRight: Int = getDimension(R.dimen.uxsdk_telemetry_item_padding).toInt()
        set(value) {
            field = value
            guidelineRight.setGuidelineEnd(value)
        }

    /**
     * Bottom padding of the list item content
     */
    var contentPaddingBottom: Int = getDimension(R.dimen.uxsdk_telemetry_item_padding).toInt()
        set(value) {
            field = value
            guidelineBottom.setGuidelineEnd(value)
        }
    //endregion

    //region image customizations
    /**
     * The icon for the widget
     */
    var widgetIcon: Drawable?
        get() = imageView.imageDrawable
        set(@Nullable value) {
            imageView.imageDrawable = value
        }

    /**
     * The color of the icon
     */
    @get:ColorInt
    var widgetIconColor: Int = getColor(R.color.uxsdk_white)
        set(@ColorInt value) {
            field = value
            imageView.setColorFilter(value, PorterDuff.Mode.SRC_IN)
        }

    /**
     * The background of the widget icon
     */
    var widgetIconBackground: Drawable?
        get() = imageView.background
        set(@Nullable value) {
            imageView.background = value
        }

    /**
     * Visibility of the icon
     */
    var widgetIconVisibility: Boolean
        get() = imageView.visibility == View.VISIBLE
        set(value) {
            if (value) {
                imageView.show()
            } else {
                imageView.hide()
            }
        }
    //endregion

    //region label customizations
    /**
     * String value of label
     */
    var labelString: String?
        @Nullable get() = labelTextView.text.toString()
        set(value) {
            labelTextView.text = value
        }

    /**
     * Float text size of the label
     */
    var labelTextSize: Float
        @Dimension get() = labelTextView.textSize
        set(@Dimension value) {
            labelTextView.textSize = value
        }

    /**
     * Integer color for label
     */
    var labelTextColor: Int
        @ColorInt get() = labelTextView.textColor
        set(@ColorInt value) {
            labelTextView.textColor = value
        }

    /**
     * Color state list of the label
     */
    var labelTextColors: ColorStateList?
        get() = labelTextView.textColorStateList
        set(value) {
            labelTextView.textColorStateList = value
        }

    /**
     * Background of the label
     */
    var labelBackground: Drawable?
        get() = labelTextView.background
        set(value) {
            labelTextView.background = value
        }

    /**
     * Visibility of the label
     */
    var labelVisibility: Boolean
        get() = labelTextView.visibility == View.VISIBLE
        set(value) {
            if (value) {
                labelTextView.show()
            } else {
                labelTextView.hide()
            }
        }

    //endregion

    //region value customizations
    /**
     * String for value text view
     */
    var valueString: String?
        @Nullable get() = valueTextView.text.toString()
        set(value) {
            valueTextView.text = value
        }

    /**
     * Float text size of the value
     */
    var valueTextSize: Float
        @Dimension get() = valueTextView.textSize
        set(@Dimension value) {
            valueTextView.textSize = value
        }

    /**
     * Integer color for value
     */
    var valueTextColor: Int
        @ColorInt get() = valueTextView.textColor
        set(@ColorInt value) {
            valueTextView.textColor = value
        }

    /**
     * Color state list of the value
     */
    var valueTextColors: ColorStateList?
        get() = valueTextView.textColorStateList
        set(value) {
            valueTextView.textColorStateList = value
        }

    /**
     * Background of the value
     */
    var valueBackground: Drawable?
        get() = valueTextView.background
        set(value) {
            valueTextView.background = value
        }

    /**
     * Visibility of the value
     */
    var valueVisibility: Boolean
        get() = valueTextView.visibility == View.VISIBLE
        set(value) {
            if (value) {
                valueTextView.show()
            } else {
                valueTextView.hide()
            }
        }

    /**
     * Text position of the value
     */
    var valueTextGravity: Int
        get() = valueTextView.gravity
        set(value) {
            valueTextView.gravity = value
        }

    //endregion

    //region value customizations
    /**
     * String for unit text view
     */
    var unitString: String?
        @Nullable get() = unitTextView.text.toString()
        set(value) {
            unitTextView.text = value
        }

    /**
     * Float text size of the unit
     */
    var unitTextSize: Float
        @Dimension get() = unitTextView.textSize
        set(@Dimension value) {
            unitTextView.textSize = value
        }

    /**
     * Integer color for unit
     */
    var unitTextColor: Int
        @ColorInt get() = unitTextView.textColor
        set(@ColorInt value) {
            unitTextView.textColor = value
        }

    /**
     * Color state list of the unit
     */
    var unitTextColors: ColorStateList?
        get() = unitTextView.textColorStateList
        set(value) {
            unitTextView.textColorStateList = value
        }

    /**
     * Background of the unit
     */
    var unitBackground: Drawable?
        get() = unitTextView.background
        set(value) {
            unitTextView.background = value
        }

    /**
     * Visibility of the unit
     */
    var unitVisibility: Boolean
        get() = unitTextView.visibility == View.VISIBLE
        set(value) {
            if (value) {
                unitTextView.show()
            } else {
                unitTextView.hide()
            }
        }

    //endregion
    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        View.inflate(context, R.layout.uxsdk_widget_base_telemetry, this)
    }

    init {
        if (widgetType == WidgetType.TEXT_IMAGE_RIGHT) {
            configureRightImageTypeWidget()
        } else if (widgetType == WidgetType.TEXT_IMAGE_LEFT) {
            configureLeftImageTypeWidget()
        }
        initBaseTelemetryAttributes(context)
        setValueTextViewMinWidthByText("888.8")
        initAttributes(context, attrs)
    }

    @SuppressLint("Recycle")
    private fun initBaseTelemetryAttributes(context: Context) {
        val baseTelemetryAttributeArray: IntArray = R.styleable.BaseTelemetryWidget
        context.obtainStyledAttributes(widgetTheme, baseTelemetryAttributeArray).use {
            initAttributesByTypedArray(it)
        }
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.BaseTelemetryWidget, 0, defaultStyle).use { typedArray ->
            initAttributesByTypedArray(typedArray)
        }
    }

    private fun initAttributesByTypedArray(typedArray: TypedArray) {
        typedArray.getResourceIdAndUse(R.styleable.BaseTelemetryWidget_uxsdk_label_text_appearance) {
            setLabelTextAppearance(it)
        }
        typedArray.getDimensionAndUse(R.styleable.BaseTelemetryWidget_uxsdk_label_text_size) {
            labelTextSize = it
        }
        typedArray.getColorAndUse(R.styleable.BaseTelemetryWidget_uxsdk_label_text_color) {
            labelTextColor = it
        }
        typedArray.getColorStateListAndUse(R.styleable.BaseTelemetryWidget_uxsdk_label_text_color) {
            labelTextColors = it
        }
        typedArray.getDrawableAndUse(R.styleable.BaseTelemetryWidget_uxsdk_label_background) {
            labelBackground = it
        }
        labelVisibility = typedArray.getBoolean(R.styleable.BaseTelemetryWidget_uxsdk_label_visibility,
                labelVisibility)
        labelString =
                typedArray.getString(R.styleable.BaseTelemetryWidget_uxsdk_label_string,
                        getString(R.string.uxsdk_string_default_value))

        typedArray.getResourceIdAndUse(R.styleable.BaseTelemetryWidget_uxsdk_value_text_appearance) {
            setValueTextAppearance(it)
        }
        typedArray.getDimensionAndUse(R.styleable.BaseTelemetryWidget_uxsdk_value_text_size) {
            valueTextSize = it
        }
        typedArray.getColorAndUse(R.styleable.BaseTelemetryWidget_uxsdk_value_text_color) {
            valueTextColor = it
        }
        typedArray.getColorStateListAndUse(R.styleable.BaseTelemetryWidget_uxsdk_value_text_color) {
            valueTextColors = it
        }
        typedArray.getDrawableAndUse(R.styleable.BaseTelemetryWidget_uxsdk_value_background) {
            valueBackground = it
        }
        valueVisibility = typedArray.getBoolean(R.styleable.BaseTelemetryWidget_uxsdk_value_visibility,
                valueVisibility)
        typedArray.getIntegerAndUse(R.styleable.BaseTelemetryWidget_uxsdk_value_gravity) {
            valueTextGravity = it
        }
        valueString =
                typedArray.getString(R.styleable.BaseTelemetryWidget_uxsdk_value_string,
                        getString(R.string.uxsdk_string_default_value))

        typedArray.getResourceIdAndUse(R.styleable.BaseTelemetryWidget_uxsdk_unit_text_appearance) {
            setUnitTextAppearance(it)
        }
        typedArray.getDimensionAndUse(R.styleable.BaseTelemetryWidget_uxsdk_unit_text_size) {
            unitTextSize = it
        }
        typedArray.getColorAndUse(R.styleable.BaseTelemetryWidget_uxsdk_unit_text_color) {
            unitTextColor = it
        }
        typedArray.getColorStateListAndUse(R.styleable.BaseTelemetryWidget_uxsdk_unit_text_color) {
            unitTextColors = it
        }
        typedArray.getDrawableAndUse(R.styleable.BaseTelemetryWidget_uxsdk_unit_background) {
            unitBackground = it
        }
        unitVisibility = typedArray.getBoolean(R.styleable.BaseTelemetryWidget_uxsdk_unit_visibility,
                unitVisibility)
        unitString =
                typedArray.getString(R.styleable.BaseTelemetryWidget_uxsdk_unit_string,
                        getString(R.string.uxsdk_string_default_value))

        typedArray.getDrawableAndUse(R.styleable.BaseTelemetryWidget_uxsdk_widget_icon) {
            widgetIcon = it
        }
        typedArray.getColorAndUse(R.styleable.BaseTelemetryWidget_uxsdk_widget_icon_color) {
            widgetIconColor = it
        }
        typedArray.getDrawableAndUse(R.styleable.BaseTelemetryWidget_uxsdk_widget_icon_background) {
            widgetIconBackground = it
        }
        widgetIconVisibility = typedArray.getBoolean(R.styleable.BaseTelemetryWidget_uxsdk_widget_icon_visibility,
                widgetIconVisibility)
        typedArray.getDimensionAndUse(R.styleable.BaseTelemetryWidget_uxsdk_widget_padding_left) {
            contentPaddingLeft = it.toInt()
        }
        typedArray.getDimensionAndUse(R.styleable.BaseTelemetryWidget_uxsdk_widget_padding_top) {
            contentPaddingTop = it.toInt()
        }
        typedArray.getDimensionAndUse(R.styleable.BaseTelemetryWidget_uxsdk_widget_padding_right) {
            contentPaddingRight = it.toInt()
        }
        typedArray.getDimensionAndUse(R.styleable.BaseTelemetryWidget_uxsdk_widget_padding_bottom) {
            contentPaddingBottom = it.toInt()
        }
        typedArray.getColorAndUse(R.styleable.BaseTelemetryWidget_uxsdk_normal_text_color) {
            normalValueColor = it
        }
        typedArray.getColorAndUse(R.styleable.BaseTelemetryWidget_uxsdk_error_text_color) {
            errorValueColor = it
        }
    }

    private fun configureLeftImageTypeWidget() {
        imageView.id = ViewIDGenerator.generateViewId()
        val set = ConstraintSet()
        set.clone(this)
        set.clear(imageView.id)
        imageView.adjustViewBounds = true
        set.setMargin(imageView.id, ConstraintSet.RIGHT, getDimension(R.dimen.uxsdk_telemetry_view_margin).toInt())
        set.clear(labelTextView.id, ConstraintSet.LEFT)
        set.constrainHeight(imageView.id, 0)
        set.setDimensionRatio(imageView.id, "1:1")
        set.setHorizontalChainStyle(imageView.id, ConstraintSet.CHAIN_SPREAD)
        set.connect(imageView.id, ConstraintSet.TOP, guidelineTop.id, ConstraintSet.TOP)
        set.connect(imageView.id, ConstraintSet.BOTTOM, guidelineBottom.id, ConstraintSet.BOTTOM)
        set.connect(imageView.id, ConstraintSet.LEFT, guidelineLeft.id, ConstraintSet.LEFT)
        set.connect(imageView.id, ConstraintSet.RIGHT, labelTextView.id, ConstraintSet.LEFT)
        set.connect(labelTextView.id, ConstraintSet.LEFT, imageView.id, ConstraintSet.RIGHT)
        addView(imageView)
        set.applyTo(this)

    }


    private fun configureRightImageTypeWidget() {
        imageView.id = ViewIDGenerator.generateViewId()
        val set = ConstraintSet()
        set.clone(this)
        set.clear(imageView.id)
        imageView.adjustViewBounds = true
        set.setMargin(imageView.id, ConstraintSet.LEFT, getDimension(R.dimen.uxsdk_telemetry_view_margin).toInt())
        set.clear(unitTextView.id, ConstraintSet.RIGHT)
        set.constrainHeight(imageView.id, 0)
        set.setDimensionRatio(imageView.id, "1:1")
        set.setHorizontalChainStyle(imageView.id, ConstraintSet.CHAIN_SPREAD)
        set.connect(imageView.id, ConstraintSet.TOP, guidelineTop.id, ConstraintSet.TOP)
        set.connect(imageView.id, ConstraintSet.BOTTOM, guidelineBottom.id, ConstraintSet.BOTTOM)
        set.connect(imageView.id, ConstraintSet.RIGHT, guidelineRight.id, ConstraintSet.RIGHT)
        set.connect(imageView.id, ConstraintSet.LEFT, unitTextView.id, ConstraintSet.RIGHT)
        set.connect(unitTextView.id, ConstraintSet.RIGHT, imageView.id, ConstraintSet.LEFT)
        addView(imageView)
        set.applyTo(this)

    }

    protected fun setValueTextViewMinWidthByText(maxText: String) {
        val textPaint = TextPaint()
        textPaint.textSize = valueTextView.textSize
        valueTextView.minWidth = textPaint.measureText(maxText).toInt()
    }

    protected fun getDecimalFormat(unitType: UnitConversionUtil.UnitType): DecimalFormat {
        return if (unitType == UnitConversionUtil.UnitType.IMPERIAL) {
            imperialDecimalFormat
        } else {
            metricDecimalFormat
        }
    }

    /**
     * Set the icon
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setIcon(@DrawableRes resourceId: Int) {
        widgetIcon = getDrawable(resourceId)
    }

    /**
     * Set the background of the icon
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setIconBackground(@DrawableRes resourceId: Int) {
        widgetIconBackground = getDrawable(resourceId)
    }

    /**
     * Set the background of the label
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setLabelBackground(@DrawableRes resourceId: Int) {
        labelBackground = getDrawable(resourceId)
    }

    /**
     * Set the text appearance of the label
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    fun setLabelTextAppearance(@StyleRes textAppearanceResId: Int) {
        labelTextView.setTextAppearance(context, textAppearanceResId)
    }

    /**
     * Set the background of the value
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setValueBackground(@DrawableRes resourceId: Int) {
        valueBackground = getDrawable(resourceId)
    }

    /**
     * Set the text appearance of the value
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    fun setValueTextAppearance(@StyleRes textAppearanceResId: Int) {
        valueTextView.setTextAppearance(context, textAppearanceResId)
    }

    /**
     * Set the background of the unit
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setUnitBackground(@DrawableRes resourceId: Int) {
        unitBackground = getDrawable(resourceId)
    }

    /**
     * Set the text appearance of the unit
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    fun setUnitTextAppearance(@StyleRes textAppearanceResId: Int) {
        unitTextView.setTextAppearance(context, textAppearanceResId)
    }

    /**
     * Set padding to the content of the  item
     */
    fun setContentPadding(left: Int, top: Int, right: Int, bottom: Int) {
        contentPaddingLeft = left
        contentPaddingTop = top
        contentPaddingRight = right
        contentPaddingBottom = bottom
    }


    /**
     * Defines the type of widget
     */
    enum class WidgetType {

        /**
         * The type represents
         *  | LABEL | VALUE | UNIT |
         */
        TEXT,

        /**
         * The class represents the with icon,
         *  | IMAGE | LABEL | VALUE | UNIT |
         */
        TEXT_IMAGE_LEFT,

        /**
         * The class represents the with icon,
         *  | LABEL | VALUE | UNIT | IMAGE |
         */
        TEXT_IMAGE_RIGHT
    }

}