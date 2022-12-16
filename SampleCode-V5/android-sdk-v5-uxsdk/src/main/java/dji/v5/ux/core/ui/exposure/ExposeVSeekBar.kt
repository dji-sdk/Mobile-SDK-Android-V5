package dji.v5.ux.core.ui.exposure

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import dji.v5.ux.R
import dji.v5.ux.core.ui.VerticalSeekBar
import dji.v5.utils.common.AndUtil
import kotlin.math.max

class ExposeVSeekBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : VerticalSeekBar(context, attrs) {

    var isShowSeekBar = false
    private val sunSize = getContext().resources.getDimensionPixelSize(R.dimen.uxsdk_14_dp)
    private val rectWidth = getContext().resources.getDimensionPixelSize(R.dimen.uxsdk_1_dp)
    private val sunMargin = getContext().resources.getDimensionPixelSize(R.dimen.uxsdk_2_dp)
    private val rectStrokeWidth = AndUtil.dip2px(context, 0.5f)
    private val halfSunSize = sunSize shr 1

    private val rect = Rect()
    private val paint = Paint()

    private val rectSolidColor = ContextCompat.getColor(context, R.color.uxsdk_yellow_500)
    private val rectStrokeColor = ContextCompat.getColor(context, R.color.uxsdk_black_30_percent)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val thumbWidth = if (mThumb == null) 0 else mThumb.intrinsicWidth
        var dw = mProgressWidth
        var dh = MeasureSpec.getSize(heightMeasureSpec)
        dw = max(thumbWidth shl 1, dw)
        dw += paddingLeft + paddingRight
        dh += paddingTop + paddingBottom
        setMeasuredDimension(dw, dh)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if ((event != null && event.action == MotionEvent.ACTION_DOWN && mThumb != null)
            && (event.y < mThumb.bounds.top - mThumb.intrinsicWidth
                    || event.y > mThumb.bounds.bottom + mThumb.intrinsicWidth)
        ) {
            //只有点击小太阳图标时才有效，否则不处理点击事件
            return false
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.takeIf { isShowSeekBar }?.let {
            drawRect(it)
        }
        super.onDraw(canvas)
    }

    private fun drawRect(canvas: Canvas) {
        val thumbRect = mThumb.bounds

        val startOffX = (thumbRect.left + thumbRect.right - rectWidth) shr 1
        val topEndOffY = thumbRect.top - sunMargin

        if (topEndOffY.toFloat() > halfSunSize) {
            rect.set(startOffX, halfSunSize, startOffX + rectWidth, topEndOffY)
            onDrawRect(canvas)
        }
        val bottomStartOffY = thumbRect.bottom + sunMargin
        val bottomEndOffY = layoutParams.height - halfSunSize
        if (bottomEndOffY > bottomStartOffY) {
            rect.set(startOffX, bottomStartOffY, startOffX + rectWidth, bottomEndOffY)
            onDrawRect(canvas)
        }
    }

    private fun onDrawRect(canvas: Canvas) {
        //绘制实体线
        paint.color = rectSolidColor
        paint.style = Paint.Style.FILL
        canvas.drawRect(rect, paint)
        //绘制黑色描边
        paint.color = rectStrokeColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = rectStrokeWidth.toFloat()
        with(rect) {
            right += rectStrokeWidth
            bottom += rectStrokeWidth
        }
        canvas.drawRect(rect, paint)
    }
}