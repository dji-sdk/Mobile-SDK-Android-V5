package dji.v5.ux.core.ui.component

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import dji.v5.ux.R

open class UXRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    var fadingEdgeColor: Int = 0

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.UXRecyclerView)
        fadingEdgeColor = ta.getColor(R.styleable.UXRecyclerView_uxsdk_fadingEdgeColor, 0)
        ta.recycle()
    }

    override fun getSolidColor(): Int {
        return fadingEdgeColor
    }

    override fun getTopFadingEdgeStrength(): Float {
        return 0F
    }
}