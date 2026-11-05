package com.dji.open.pilot.plugin.sampleitem.cameradrawing.drawing

import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.drawing.ActionButtonConfig
import com.dji.open.pilot.plugin.common.drawing.DrawingContext
import dji.v5.ux.remoteview.RemoteLinearLayout

/**
 * 图传绘制「多边形」Tab 的悬浮窗绘制。
 * 卡片1：结果 TextView；卡片2：更新/清除 两个按钮。
 */
object PolygonDrawer {

    fun draw(
        ctx: DrawingContext,
        container: RemoteLinearLayout,
        onUpdate: () -> Unit,
        onClear: () -> Unit
    ): String {
        val resultId = ctx.drawResultCard(container)
        ctx.drawActionCard(container, listOf(
            ActionButtonConfig(R.string.cvd_btn_update, onUpdate),
            ActionButtonConfig(R.string.cvd_btn_clear, onClear),
        ))
        return resultId
    }
}
