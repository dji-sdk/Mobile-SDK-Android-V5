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
package dji.v5.ux.core.widget.compass

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.Px
import dji.v5.ux.R
import dji.v5.ux.core.extension.getColor
import dji.v5.ux.core.extension.getDimension
import kotlin.math.ln

private const val DEFAULT_INTERVAL = 100
private const val DEFAULT_DISTANCE = 400
private const val DEFAULT_NUMBER_OF_LINES = 4

/**
 * Custom view to display the compass view for the aircraft
 */
class VisualCompassView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    //region Fields
    private val paint = Paint()
    private var distance = DEFAULT_DISTANCE.toFloat()

    /**
     * The interval between the lines
     */
    var lineInterval = DEFAULT_INTERVAL
        set(interval) {
            field = interval
            postInvalidate()
        }

    /**
     * The number of lines to be drawn
     */
    var numberOfLines = DEFAULT_NUMBER_OF_LINES
        set(lines) {
            field = lines
            postInvalidate()
        }

    /**
     * The stroke width for the lines in pixels
     */
    @Px
    @FloatRange(from = 1.0, to = MAX_LINE_WIDTH.toDouble())
    var strokeWidth = getDimension(R.dimen.uxsdk_line_width)
        set(@FloatRange(from = 1.0, to = MAX_LINE_WIDTH.toDouble()) strokeWidth) {
            field = strokeWidth
            postInvalidate()
        }

    /**
     * The color for the lines
     */
    @get:ColorInt
    var lineColor: Int = getColor(R.color.uxsdk_white_47_percent)
        set(color) {
            field = color
            postInvalidate()
        }

    //endregion

    //region Constructor
    init {
        init()
    }

    private fun init() {
        if (isInEditMode) {
            return
        }

        paint.isAntiAlias = true
        paint.color = getColor(R.color.uxsdk_white_20_percent)
        paint.style = Paint.Style.STROKE
        if (strokeWidth > MAX_LINE_WIDTH) {
            strokeWidth = MAX_LINE_WIDTH.toFloat()
        }
        paint.strokeWidth = strokeWidth
    }
    //endregion

    //region UI Logic
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawObstacleInfo(canvas)
    }

    private fun findMod(distance: Float): Float {
        val radarRadius = lineInterval * numberOfLines.toFloat()
        var result = 0.0f
        if (distance > radarRadius) {
            result = (ln(distance / (lineInterval * numberOfLines).toDouble()) / ln(2.0)).toFloat()
            result -= result.toInt()
        }
        return result
    }

    private fun getVirtualColor(mod: Float): Int {
        var color = lineColor
        var alpha = Color.alpha(color)
        alpha = (alpha * (1.0f - mod)).toInt()
        color = Color.argb(alpha, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor))
        return color
    }

    private fun drawDistance(canvas: Canvas) {
        val mod = findMod(distance)
        val width = width - 2
        val radius = width * 0.5f
        val center = radius + 1
        val unitRadius = radius / (4.0f * (mod + 1.0f))
        val vColor = getVirtualColor(mod)
        var dRadius: Float
        paint.style = Paint.Style.STROKE
        paint.color = lineColor
        canvas.drawCircle(center, center, radius, paint)
        var i = 1
        while (i * unitRadius < radius) {
            if (i % 2 == 0) {
                paint.color = lineColor
            } else {
                paint.color = vColor
            }
            dRadius = i * unitRadius
            canvas.drawCircle(center, center, dRadius, paint)
            i++
        }
    }

    private fun drawObstacleInfo(canvas: Canvas) {
        drawDistance(canvas)
    }

    /**
     * Set the real-world distance that is represented by the length from the center to the edge
     * of the view.
     */
    fun setDistance(distance: Float) {
        if (this.distance != distance) {
            this.distance = distance
            postInvalidate()
        }
    }
    //endregion

    companion object {

        /**
         * The maximum width of the lines
         */
        const val MAX_LINE_WIDTH = 4
    }
}