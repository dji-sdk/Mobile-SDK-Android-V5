package com.dji.open.pilot.plugin.sampleitem.mapmanager.drawing

import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.drawing.ActionButtonConfig
import com.dji.open.pilot.plugin.common.drawing.DrawingContext
import dji.v5.ux.remoteview.RemoteLinearLayout

/**
 * 批量控件（MapWidget 批量）用例的悬浮窗绘制。
 * 一次性对「Marker + 线 + 面 + 圆」全套图元做批量增/改/删。
 * 批量更新演示将全套颜色改为半透明绿 + 深绿描边。
 */
object MapBatchDrawer {

    fun draw(
        ctx: DrawingContext,
        container: RemoteLinearLayout,
        onBatchAdd: () -> Unit,
        onBatchUpdate: () -> Unit,
        onBatchRemove: () -> Unit
    ): String {
        val resultId = ctx.drawResultCard(container)
        ctx.drawActionCard(container, listOf(
            ActionButtonConfig(R.string.map_btn_batch_add, onBatchAdd),
            ActionButtonConfig(R.string.map_btn_batch_update, onBatchUpdate),
            ActionButtonConfig(R.string.map_btn_batch_remove, onBatchRemove),
        ))
        return resultId
    }
}
