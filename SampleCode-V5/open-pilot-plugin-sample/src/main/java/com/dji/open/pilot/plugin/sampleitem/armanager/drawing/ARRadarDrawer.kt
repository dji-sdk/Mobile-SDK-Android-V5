package com.dji.open.pilot.plugin.sampleitem.armanager.drawing

import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.drawing.ActionButtonConfig
import com.dji.open.pilot.plugin.common.drawing.DrawingContext
import dji.v5.ux.remoteview.RemoteLinearLayout

/**
 * AR 雷达 Tab 的悬浮窗绘制。
 * 只有"显示"和"隐藏"两个按钮。
 */
object ARRadarDrawer {

    fun draw(
        ctx: DrawingContext,
        container: RemoteLinearLayout,
        onEnable: () -> Unit,
        onDisable: () -> Unit
    ): String {
        val resultId = ctx.drawResultCard(container)
        ctx.drawActionCard(container, listOf(
            ActionButtonConfig(R.string.ar_btn_radar_show, onEnable),
            ActionButtonConfig(R.string.ar_btn_radar_hide, onDisable),
        ))
        return resultId
    }
}
