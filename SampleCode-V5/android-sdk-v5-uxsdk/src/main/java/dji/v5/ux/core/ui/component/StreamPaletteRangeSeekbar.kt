package dji.v5.ux.core.ui.component

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import dji.v5.ux.R
import java.text.DecimalFormat
/**
 * 码流调试融合强度调整
 */
class StreamPaletteRangeSeekbar(context: Context, attrs: AttributeSet? = null) : IsothermRangeSeekbar(context, attrs) {
    var zoomMultiple  = 1.0
    var df: DecimalFormat = DecimalFormat("#.#")

    override fun drawLeftThumb(canvas: Canvas) {
        val leftTop = height / 2 - thumbHeight/2
        val leftX = getThumbPosition(mLeftValue) - thumbWidth
        mLeftThumb.setBounds(leftX, leftTop, leftX + thumbWidth, leftTop + thumbHeight)
        if (isEnabled) {
            mLeftThumb.draw(canvas)
        }

        // 绘制文字
        val text = if (zoomMultiple == 1.0) leftValue.toString() else (df.format(leftValue * zoomMultiple)).toString()
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val textRight = leftX + textBounds.width()
        // 超出显示范围，画在左边
        if (textRight > width) {
            textPaint.textAlign = Paint.Align.RIGHT
        } else {
            textPaint.textAlign = Paint.Align.LEFT
        }
        val textTop = mLeftThumb.bounds.bottom + resources.getDimension(R.dimen.uxsdk_4_dp)
        val textLeft = mLeftThumb.bounds.left.toFloat()
        canvas.drawText(text, textLeft, textTop + textBounds.height(), textPaint)
    }

    override fun drawRightThumb(canvas: Canvas) {
        val rightX = getThumbPosition(mRightValue)
        val rightTop = height / 2 - thumbHeight/2
        mRightThumb.setBounds(rightX, rightTop, rightX + thumbWidth, rightTop + thumbHeight)
        if (isEnabled) {
            mRightThumb.draw(canvas)
        }

        // 绘制文字
        val text = if (zoomMultiple == 1.0) rightValue.toString() else (df.format(rightValue * zoomMultiple)).toString()
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val textRight = rightX - textBounds.width()
        // 超出显示范围，画在左边
        if (textRight < 0) {
            textPaint.textAlign = Paint.Align.LEFT
        } else {
            textPaint.textAlign = Paint.Align.RIGHT
        }
        val textTop = mRightThumb.bounds.top + resources.getDimension(R.dimen.uxsdk_4_dp)
        val textLeft = mRightThumb.bounds.right.toFloat()
        canvas.drawText(text, textLeft, textTop - textBounds.height(), textPaint)
    }

    override fun getTouchRect(thumb: Drawable?): Rect {
        mTouchRect = super.getTouchRect(thumb)
        mTouchRect.left -= thumbWidth
        mTouchRect.right += thumbWidth
        mTouchRect.top -= thumbHeight/2
        mTouchRect.bottom += thumbHeight/2
        return mTouchRect
    }
}