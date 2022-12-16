package dji.v5.ux.core.ui.component

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class PaletteItemDecoration(val spanCount: Int,
                            val leftEdgeSpacing: Int,
                            val rightEdgeSpacing: Int,
                            val topEdgeSpacing: Int,
                            val hSpacing: Int,
                            val vSpacing: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, itemPosition: Int, parent: RecyclerView) {
        when (itemPosition % spanCount) {
            0 -> {
                outRect.left = leftEdgeSpacing
                outRect.right = hSpacing / 2
            }
            spanCount - 1 -> {
                outRect.left = hSpacing / 2
                outRect.right = rightEdgeSpacing
            }
            else -> {
                outRect.left =  hSpacing / 2
                outRect.right = hSpacing / 2
            }
        }

        val row = itemPosition / spanCount
        val maxRow = (parent.adapter?.itemCount?:0) / spanCount

        when (row) {
            maxRow-> {
                outRect.top = vSpacing / 2
                outRect.bottom = vSpacing
            }
            else-> {
                outRect.top = vSpacing / 2
                outRect.bottom = vSpacing / 2
            }
        }



    }
}