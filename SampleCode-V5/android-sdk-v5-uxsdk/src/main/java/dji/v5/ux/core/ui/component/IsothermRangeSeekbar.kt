package dji.v5.ux.core.ui.component

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import dji.v5.utils.common.AndUtil
import dji.v5.ux.R
import dji.v5.ux.core.ui.RangeSeekBar

/**
 * 等温线范围条件控件
 */
open class IsothermRangeSeekbar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : RangeSeekBar(context, attrs) {
    private val DEBUG_TOUCH_RECT = false
    private val paint = Paint()
    val textPaint = Paint()
    val textBounds = Rect()
    val thumbWidth = AndUtil.getDimension(R.dimen.uxsdk_11_dp).toInt()
    val thumbHeight = AndUtil.getDimension(R.dimen.uxsdk_18_dp).toInt()

    var textFormatCallback: TextFormatCallback? = null

    init {
        paint.color = Color.RED
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2F

        textPaint.run {
            isDither = true
            isAntiAlias = true
            color = Color.WHITE
            typeface = Typeface.DEFAULT_BOLD
            textSize = resources.getDimension(R.dimen.uxsdk_10_dp)
        }

    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (DEBUG_TOUCH_RECT) {
            canvas?.drawRect(getTouchRect(mLeftThumb), paint)
            canvas?.drawRect(getTouchRect(mRightThumb), paint)
        }
    }

    override fun getTouchRect(thumb: Drawable?): Rect {
        mTouchRect = super.getTouchRect(thumb)
        if (thumb == mLeftThumb) {
            mTouchRect.left -= thumbWidth
        } else {
            mTouchRect.right += thumbWidth
        }
        mTouchRect.top -= thumbHeight/2
        mTouchRect.bottom += thumbHeight/2
        return mTouchRect
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 高度
        var heightMeasureSpec = heightMeasureSpec
        val heightNeeded: Int = resources.getDimension(R.dimen.uxsdk_42_dp).toInt() + paddingTop + paddingBottom
        var heightSize: Int = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode: Int = MeasureSpec.getMode(heightMeasureSpec)
        heightMeasureSpec = if (heightMode == MeasureSpec.EXACTLY) {
            MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY)
        } else if (heightMode == MeasureSpec.AT_MOST) {
            MeasureSpec.makeMeasureSpec(
                if (heightSize < heightNeeded) heightSize else heightNeeded, MeasureSpec.EXACTLY
            )
        } else {
            MeasureSpec.makeMeasureSpec(
                heightNeeded, MeasureSpec.EXACTLY
            )
        }
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        heightSize = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(widthSize, heightSize)
    }

    override fun drawLeftThumb(canvas: Canvas) {

        val leftTop = height / 2 - thumbHeight/2
        val leftX = getThumbPosition(mLeftValue) - thumbWidth
        mLeftThumb.setBounds(leftX, leftTop, leftX + thumbWidth, leftTop + thumbHeight)
        if (isEnabled) {
            mLeftThumb.draw(canvas)
        }

        // 绘制文字
        val text = textFormatCallback?.format(mLeftValue)?:mLeftValue.toString()
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
        val text = textFormatCallback?.format(mRightValue)?:mRightValue.toString()
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

    override fun drawProgress(canvas: Canvas) {
        val leftX = getThumbPosition(mLeftValue) - thumbWidth
        val rightX = getThumbPosition(mRightValue) + thumbWidth

        mProgressDrawable.setBounds(
            leftX,
            mBackgroundDrawable.bounds.top,
            rightX,
            mBackgroundDrawable.bounds.bottom
        )
        mProgressDrawable.draw(canvas)
    }

    fun setProgressDrawable(drawable: Drawable) {
        mProgressDrawable = drawable
        postInvalidate()
    }

    override fun getThumbPosition(value: Int): Int {
        // progress的总长度
        val width = (width - paddingRight - paddingLeft).toFloat()
        val range = mDrawMax - mDrawMin
        val v = value - mDrawMin
        return Math.round(v / range * width) + paddingLeft
    }

    override fun getValueByDelta(delta: Float): Int {
        val width = (width - paddingRight - thumbWidth / 2 - paddingLeft - thumbWidth / 2).toFloat()
        val range = (mMax - mMin).toFloat()
        return Math.round(delta / width * range)
    }

    fun interface TextFormatCallback {
        fun format(value: Int): String
    }
}