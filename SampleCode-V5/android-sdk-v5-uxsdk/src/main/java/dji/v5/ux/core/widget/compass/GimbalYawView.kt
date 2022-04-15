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
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.Px
import dji.v5.ux.R
import dji.v5.ux.core.extension.getColor
import dji.v5.ux.core.extension.getDimension

private const val DEAD_ANGLE = 30
private const val BLINK_ANGLE = 270
private const val SHOW_ANGLE = 190
private const val HIDE_ANGLE = 90
private const val DURATION_BLINK: Long = 200

/**
 * Custom view to display the aircraft gimbal's heading
 */
class GimbalYawView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    //region Fields
    private val rect = RectF()
    private val paint: Paint = Paint()
    private var curBlinkColor = 0
    private var beforeShow = false
    private var yaw = 0f
    private var absYaw = 0f
    private var yawStartAngle = 0f
    private var yawSweepAngle = 0f
    private var invalidStartAngle = 0f
    private var invalidSweepAngle = 0f

    /**
     * The stroke width for the lines in pixels
     */
    @Px
    @FloatRange(from = 1.0, to = MAX_LINE_WIDTH.toDouble())
    var strokeWidth = getDimension(R.dimen.uxsdk_gimbal_line_width)
        set(@FloatRange(from = 1.0, to = MAX_LINE_WIDTH.toDouble()) strokeWidth) {
            field = strokeWidth
            postInvalidate()
        }

    /**
     * The yaw color
     */
    @ColorInt
    var yawColor = getColor(R.color.uxsdk_blue_material_A400)
        set(@ColorInt yawColor) {
            field = yawColor
            postInvalidate()
        }

    /**
     * The invalid color
     */
    @ColorInt
    var invalidColor = getColor(R.color.uxsdk_red)
        set(@ColorInt invalidColor) {
            field = invalidColor
            postInvalidate()
        }

    /**
     * The blink color
     */
    @ColorInt
    var blinkColor = getColor(R.color.uxsdk_red_material_900_30_percent)
        set(@ColorInt blinkColor) {
            field = blinkColor
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

        paint.strokeWidth = strokeWidth
        paint.style = Paint.Style.STROKE
        paint.isAntiAlias = true
    }
    //endregion

    //region UI Logic

    /**
     * Set the yaw for the view
     *
     * @param yaw Yaw of the gimbal
     */
    fun setYaw(yaw: Float) {
        if (this.yaw != yaw) {
            this.yaw = yaw
            absYaw = if (yaw >= 0) yaw else 0 - yaw
            if (absYaw >= SHOW_ANGLE) {
                beforeShow = true
            } else if (absYaw < HIDE_ANGLE) {
                beforeShow = false
            }
            yawStartAngle = 0.0f
            yawSweepAngle = 0.0f
            invalidStartAngle = 0.0f
            invalidSweepAngle = DEAD_ANGLE.toFloat()
            if (this.yaw < 0) {
                yawStartAngle = this.yaw
                yawSweepAngle = 0 - this.yaw
            } else {
                invalidStartAngle = 0 - DEAD_ANGLE.toFloat()
                yawSweepAngle = this.yaw
            }
            postInvalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (isInEditMode) {
            return
        }
        val width: Float = if (width < height) {
            width.toFloat()
        } else {
            height.toFloat()
        }
        val halfStroke = strokeWidth / 2.0f
        val radius = width / 2
        rect[halfStroke, halfStroke, width - halfStroke] = width - halfStroke
        canvas.save()
        canvas.translate(radius, radius)
        canvas.rotate(-90.0f)
        canvas.translate(-radius, -radius)
        if (absYaw >= BLINK_ANGLE) {
            curBlinkColor = if (curBlinkColor == invalidColor) blinkColor else invalidColor
            paint.color = curBlinkColor
            canvas.drawArc(rect, invalidStartAngle, invalidSweepAngle, false, paint)
            postInvalidateDelayed(DURATION_BLINK)
        } else if (beforeShow) {
            curBlinkColor = invalidColor
            paint.color = invalidColor
            canvas.drawArc(rect, invalidStartAngle, invalidSweepAngle, false, paint)
        }
        paint.color = yawColor
        canvas.drawArc(rect, yawStartAngle, yawSweepAngle, false, paint)
        canvas.restore()
    }
    //endregion

    companion object {

        /**
         * The maximum width of the lines
         */
        const val MAX_LINE_WIDTH = 4
    }
}