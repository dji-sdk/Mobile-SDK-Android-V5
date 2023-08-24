package dji.v5.ux.obstacle.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.google.android.material.tabs.TabLayout

/**
 * TabLayout 设置 enable 为 false 之后还可以点击，在这里进行修改
 */
class FixedEnableTabLayout @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyle: Int = 0,
) : TabLayout(context, attrs, defStyle) {
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (!isEnabled) {
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }
}