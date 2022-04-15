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
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatSeekBar
import dji.v5.ux.R
import dji.v5.ux.core.extension.getColor

/**
 * A SeekBar that features a progress color
 */
class SlideAndFillSeekBar @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : AppCompatSeekBar(context, attrs, defStyleAttr) {
    // Paint of the progress
    private var reachedPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Paint of the thumb
    private var thumbPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Paint that always stays at the start to make it circular
    // Because progress alone has a square corner
    private var startPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var outerListener: OnSeekBarChangeListener? = null
    private var currentProgress = 0

    /**
     * The color of the thumb when selected
     */
    var thumbSelectedColor = getColor(R.color.uxsdk_slider_thumb_selected)

    /**
     * The color of the thumb when unselected
     */
    var thumbNormalColor = getColor(R.color.uxsdk_white)

    /**
     * The color of the filled section of the SeekBar
     */
    var progressColor = getColor(R.color.uxsdk_slider_filled)
        set(value) {
            field = value
            initPaints()
        }

    private fun initListener() {
        val innerListener: OnSeekBarChangeListener = object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                currentProgress = progress
                outerListener?.onProgressChanged(this@SlideAndFillSeekBar, progress, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                outerListener?.onStartTrackingTouch(this@SlideAndFillSeekBar)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                outerListener?.onStopTrackingTouch(this@SlideAndFillSeekBar)
            }
        }
        super.setOnSeekBarChangeListener(innerListener)
    }

    override fun setOnSeekBarChangeListener(l: OnSeekBarChangeListener) {
        outerListener = l
    }

    private fun initPaints() {
        reachedPaint.color = progressColor
        reachedPaint.style = Paint.Style.STROKE
        reachedPaint.strokeWidth = measuredHeight.toFloat()
        reachedPaint.strokeCap = Paint.Cap.ROUND
        startPaint.color = progressColor
        startPaint.style = Paint.Style.STROKE
        startPaint.strokeWidth = measuredHeight.toFloat()
        startPaint.strokeCap = Paint.Cap.ROUND
        thumbPaint.color = thumbNormalColor
        thumbPaint.style = Paint.Style.STROKE
        thumbPaint.strokeWidth = measuredHeight.toFloat()
        thumbPaint.strokeCap = Paint.Cap.ROUND
    }

    @Synchronized
    override fun onDraw(canvas: Canvas) {
        val thumb = thumb
        val thumbRect = thumb.bounds
        reachedPaint.strokeWidth = thumb.intrinsicHeight.toFloat()
        startPaint.strokeWidth = thumb.intrinsicHeight.toFloat()
        thumbPaint.strokeWidth = thumb.intrinsicHeight.toFloat()
        if (currentProgress > 0) {
            canvas.drawLine(thumb.intrinsicWidth / 2.toFloat(),
                    thumbRect.centerY().toFloat(),
                    thumbRect.centerX().toFloat(),
                    thumbRect.centerY().toFloat(),
                    reachedPaint)
            canvas.drawPoint(thumb.intrinsicWidth / 2.toFloat(), thumbRect.centerY().toFloat(), startPaint)
        }
        if (currentProgress == 0 || currentProgress == 100) {
            thumbPaint.color = thumbNormalColor
        } else {
            thumbPaint.color = thumbSelectedColor
        }
        canvas.drawPoint(thumbRect.centerX().toFloat(), thumbRect.centerY().toFloat(), thumbPaint)
    }

    init {
        initListener()
        initPaints()
    }
}