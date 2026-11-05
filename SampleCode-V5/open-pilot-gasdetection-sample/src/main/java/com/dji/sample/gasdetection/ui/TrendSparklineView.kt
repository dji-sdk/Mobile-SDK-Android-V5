package com.dji.sample.gasdetection.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.dji.sample.gasdetection.R

/**
 * 通道短时趋势折线。
 *
 * 设计意图：监测页只需要「最近一段有效样本怎么走」的直观形状，不引入图表库、坐标轴或
 * 交互；绘制只读 [setPoints] 提供的数值，不参与任何业务判定。趋势方向文字由界面单独
 * 展示，控件本身对无障碍隐藏，避免读屏重复朗读纯装饰内容。
 *
 * 线程约束：与普通 View 一致，只在主线程调用 [setPoints] 与绘制。
 */
class TrendSparklineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val inset = resources.getDimension(R.dimen.sparkline_inset)
    private val strokeWidth = resources.getDimension(R.dimen.sparkline_stroke)
    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.brand_blue)
        style = Paint.Style.STROKE
        this.strokeWidth = this@TrendSparklineView.strokeWidth
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private var points: List<Float> = emptyList()

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    /**
     * 设置折线数值。
     *
     * @param values 按时间升序的测量值；少于两个点时控件不绘制内容。
     */
    fun setPoints(values: List<Float>) {
        points = values.toList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.size < 2) return

        val min = points.min()
        val max = points.max()
        val left = inset
        val right = width - inset
        val top = inset
        val bottom = height - inset
        if (right <= left || bottom <= top) return

        val span = max - min
        val usableWidth = right - left
        val usableHeight = bottom - top
        path.reset()
        points.forEachIndexed { index, value ->
            val x = left + usableWidth * index / (points.size - 1)
            val y = if (span < 1e-6) {
                (top + bottom) / 2f
            } else {
                bottom - usableHeight * (value - min) / span
            }
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, paint)
    }
}
