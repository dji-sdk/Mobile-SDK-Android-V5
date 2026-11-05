package com.dji.open.pilot.plugin.common.widget

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * 3 列网格均匀间距装饰器。
 *
 * includeEdge = true 时，网格四周边缘间距与卡片间间距相同，视觉完全一致。
 *
 * 原理：
 *   - 每个 item 的左/右 offset 根据其列位置按比例分配，
 *     保证所有列宽度相等且间距均匀。
 *   - 第一行上方留 spacing，其余行上方留 spacing（即行间距也是 spacing）。
 *
 * @param spanCount   列数
 * @param spacing     间距，单位 px
 * @param includeEdge 是否在四周边缘也留 spacing
 */
class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int,
    private val includeEdge: Boolean = true
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val column = position % spanCount  // 当前列（0-indexed）

        if (includeEdge) {
            // 左边距：随列号递减，右边距：随列号递增，保证总宽度相等
            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount

            // 第一行上方留边距，其余行不重复加（由上一行的 bottom 覆盖）
            outRect.top = if (position < spanCount) spacing else 0
            outRect.bottom = spacing
        } else {
            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount
            outRect.top = if (position < spanCount) 0 else spacing
            outRect.bottom = 0
        }
    }
}
