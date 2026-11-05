package com.dji.open.pilot.plugin.sampleitem.mapmanager.drawing

import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.drawing.ActionButtonConfig
import com.dji.open.pilot.plugin.common.drawing.DrawingContext
import dji.v5.ux.remoteview.RemoteLinearLayout

/**
 * 地图元素（MapElement）用例的悬浮窗绘制。
 * 参考地图控件用例，但走 MapElement（Pilot 原生地图 + AR 渲染）。
 * 以飞机为中心（抬高一定高度便于 AR 观察）绘制等边三角形折线/面 + PIN。
 * 额外提供「隐藏/显示 AR 注记」演示 MapElement 独有的 AR 联动能力。
 */
object MapElementDrawer {

    fun draw(
        ctx: DrawingContext,
        container: RemoteLinearLayout,
        onInsert: () -> Unit,
        onQuery: () -> Unit,
        onUpdate: () -> Unit,
        onDelete: () -> Unit,
        onToggleAr: () -> Unit
    ): String {
        val resultId = ctx.drawResultCard(container)
        ctx.drawActionCard(container, listOf(
            ActionButtonConfig(R.string.map_btn_element_insert, onInsert),
            ActionButtonConfig(R.string.map_btn_element_query, onQuery),
        ))
        ctx.drawActionCard(container, listOf(
            ActionButtonConfig(R.string.map_btn_element_update, onUpdate),
            ActionButtonConfig(R.string.map_btn_element_delete, onDelete),
            ActionButtonConfig(R.string.map_btn_element_toggle_ar, onToggleAr),
        ))
        return resultId
    }
}
