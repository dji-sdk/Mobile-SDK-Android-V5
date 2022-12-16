package dji.v5.ux.core.ui.component

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.IdRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import kotlin.math.roundToInt
import dji.v5.ux.R
/**
 * 类似iOS的分段控件
 */
class SegmentedButtonGroup @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(ContextThemeWrapper(context, R.style.SegmentedButtonGroup), attrs) {

    private val NOT_SET = Float.MAX_VALUE

    var checkedItemBgColor = Color.TRANSPARENT

    var checkedItemRect = RectF()

    var checkedItemLeft: Float = NOT_SET
    // 动画
    var animator: ValueAnimator? = null

    var paint = Paint()
    private var radius = 0F

    private var checkedItemView: View? = null

    var onCheckedChangedListener: OnCheckedListener? = null

    init {
        val themeContext = getContext()
        val ta = themeContext.obtainStyledAttributes(attrs, R.styleable.SegmentedButtonGroup)
        checkedItemBgColor = ta.getColor(R.styleable.SegmentedButtonGroup_uxsdk_checkedItemBackgroundColor, Color.TRANSPARENT)
        ta.recycle()

        setBackgroundResource(R.drawable.uxsdk_segmented_button_group_bg)
        clipToOutline = true

        orientation = HORIZONTAL
        val padding = resources.getDimension(R.dimen.uxsdk_2_dp).roundToInt()
        setPadding(padding, padding, padding, padding)
        setWillNotDraw(false)
        gravity = Gravity.CENTER_VERTICAL

        radius = resources.getDimension(R.dimen.uxsdk_3_dp)

        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.setOnClickListener {
                onClickItem(it)
            }
        }

        if (checkedItemView == null && childCount > 0) {
            checkedItemView = getChildAt(0)
            checkedItemView?.isSelected = true
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        checkedItemView?.let { child->
            if (checkedItemLeft == NOT_SET) {
                checkedItemLeft = child.left.toFloat()
            }
            child.background = null
            checkedItemRect.set(checkedItemLeft, child.top.toFloat(), checkedItemLeft + child.width, child.bottom.toFloat())
            paint.color = checkedItemBgColor
            canvas?.drawRoundRect(checkedItemRect, radius, radius, paint)
        }
    }

    private fun onClickItem(view: View) {
        // 取消上次选中的view的selection
        checkedItemView?.let {
            it.isSelected = false
        }
        checkedItemView = view
        view.isSelected = true
        onCheckedChangedListener?.onChecked(view.id)

        if (view.width > 0) {
            animator?.cancel()
            animator = ValueAnimator.ofFloat(checkedItemRect.left, view.left.toFloat())
                .apply {
                    duration = 200
                    interpolator = FastOutSlowInInterpolator()
                    addUpdateListener {
                        checkedItemLeft = it.animatedValue as Float
                        postInvalidate()
                    }
                    start()
                }
        } else {
            checkedItemLeft = NOT_SET
            postInvalidate()
        }
    }

    fun check(@IdRes id: Int) {
        findViewById<View>(id)?.let {
            if (checkedItemView != it) {
                onClickItem(it)
            }
        }
    }

    fun interface OnCheckedListener {
        fun onChecked(@IdRes id: Int)
    }
}