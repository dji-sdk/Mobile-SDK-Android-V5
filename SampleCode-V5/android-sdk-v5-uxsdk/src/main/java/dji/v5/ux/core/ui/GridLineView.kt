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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import dji.v5.ux.R
import dji.v5.ux.core.communication.GlobalPreferencesManager
import dji.v5.ux.core.extension.getColor

private const val DISABLED = 0
private const val DEFAULT_NUM_LINES = 4
private const val DEFAULT_LINE_WIDTH = 1

/**
 * Displays a grid centered in the view.
 */
class GridLineView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    //region Fields
    private val paint: Paint = Paint()
    private var gridWidth = DISABLED
    private var gridHeight = DISABLED

    /**
     * The color of the grid lines
     */
    @get:ColorInt
    var lineColor = getColor(R.color.uxsdk_white_80_percent)
        set(@ColorInt color) {
            field = color
            if (!isInEditMode) {
                paint.color = lineColor
            }
            invalidate()
        }

    /**
     * The type of grid line.
     */
    var type: GridLineType
        get() {
            return if (GlobalPreferencesManager.getInstance() != null) {
                GlobalPreferencesManager.getInstance().gridLineType
            } else {
                GridLineType.NONE
            }
        }
        set(type) {
            if (this.type != type && GlobalPreferencesManager.getInstance() != null) {
                GlobalPreferencesManager.getInstance().gridLineType = type
                invalidate()
            }
        }

    /**
     * The width of the grid lines
     *
     * @return The width of the grid lines
     */
    var lineWidth: Float = DEFAULT_LINE_WIDTH.toFloat()
        set(lineWidth) {
            field = lineWidth
            if (!isInEditMode) {
                paint.strokeWidth = lineWidth
            }
            invalidate()
        }

    /**
     * The number of lines drawn both horizontally and vertically on the screen, including the
     * two border lines.
     */
    var numberOfLines: Int = DEFAULT_NUM_LINES
        set(numLines) {
            field = numLines
            invalidate()
        }

    //endregion

    //region Constructor
    init {
        if (!isInEditMode) {
            setWillNotDraw(false)
            paint.isAntiAlias = true
            paint.color = lineColor
            paint.strokeWidth = lineWidth
        }
    }
    //endregion

    //region Customization
    /**
     * Adjust the width and height of the grid lines. The grid will be centered within the view.
     *
     * @param width  The new width of the grid lines.
     * @param height The new height of the grid lines.
     */
    fun adjustDimensions(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            gridWidth = width
            gridHeight = height
        } else {
            gridWidth = DISABLED
            gridHeight = DISABLED
        }
        invalidate()
    }
    //endregion

    //region Lifecycle
    override fun onDraw(canvas: Canvas) {
        if (gridHeight == DISABLED || gridWidth == DISABLED) {
            return
        }
        var measureWidth = measuredWidth.toFloat()
        var measureHeight = measuredHeight.toFloat()

        // Offset by 1 because canvas origin is at 0
        measureHeight -= 1f
        measureWidth -= 1f

        // Calculate offset for different aspect ratios
        var widthOffset = ((measureWidth - gridWidth) / 2).toInt()
        if (widthOffset < 0) {
            widthOffset = 0
        }
        var heightOffset = ((measureHeight - gridHeight) / 2).toInt()
        if (heightOffset < 0) {
            heightOffset = 0
        }
        if (type != GridLineType.NONE) {
            // Draw horizontal lines
            val horizontalOffset = (measureHeight - heightOffset - heightOffset) / (numberOfLines - 1)
            var y = heightOffset.toFloat()
            while (y <= measureHeight - heightOffset) {
                canvas.drawLine(widthOffset.toFloat(), y, measureWidth - widthOffset, y, paint)
                y += horizontalOffset
            }

            // Draw vertical lines
            val verticalOffset = (measureWidth - widthOffset - widthOffset) / (numberOfLines - 1)
            var x = widthOffset.toFloat()
            while (x <= measureWidth - widthOffset) {
                canvas.drawLine(x, heightOffset.toFloat(), x, measureHeight - heightOffset, paint)
                x += verticalOffset
            }

            // Draw diagonal lines
            if (type == GridLineType.PARALLEL_DIAGONAL) {
                canvas.drawLine(widthOffset.toFloat(), heightOffset.toFloat(), measureWidth - widthOffset, measureHeight - heightOffset, paint)
                canvas.drawLine(widthOffset.toFloat(), measureHeight - heightOffset, measureWidth - widthOffset, heightOffset.toFloat(), paint)
            }
        }
    }
    //endregion

    //region Classes
    /**
     * Represents the types of grid lines that can be set.
     *
     * @property value Identifier for the item
     */
    enum class GridLineType(@get:JvmName("value") val value: Int) {
        /**
         * No grid lines are visible.
         */
        NONE(0),

        /**
         * Horizontal and vertical grid lines are visible using a NxN grid.
         */
        PARALLEL(1),

        /**
         * Same as PARALLEL with the addition of 2 diagonal lines running through the center.
         */
        PARALLEL_DIAGONAL(2),

        /**
         * The type of grid is unknown.
         */
        UNKNOWN(3);

        companion object {
            @JvmStatic
            val values = values()

            @JvmStatic
            fun find(value: Int): GridLineType {
                return values.find { it.value == value } ?: UNKNOWN
            }
        }

    }
    //endregion
}