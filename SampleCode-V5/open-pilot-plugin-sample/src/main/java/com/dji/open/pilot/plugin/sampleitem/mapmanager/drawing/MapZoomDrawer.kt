package com.dji.open.pilot.plugin.sampleitem.mapmanager.drawing

import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.drawing.ActionButtonConfig
import com.dji.open.pilot.plugin.common.drawing.DrawingContext
import dji.v5.ux.remoteview.RemoteLinearLayout

/**
 * 地图缩放级别用例的悬浮窗绘制。
 * 两个按钮：放大（当前 zoom + 1）、缩小（当前 zoom - 1）。
 * 结果卡片顶部实时回显 MapState（isMapReady / zoom / center / mapCenterMode）。
 */
object MapZoomDrawer {

    fun draw(
        ctx: DrawingContext,
        container: RemoteLinearLayout,
        onZoomIn: () -> Unit,
        onZoomOut: () -> Unit
    ): String {
        val resultId = ctx.drawResultCard(container)
        ctx.drawActionCard(container, listOf(
            ActionButtonConfig(R.string.map_btn_zoom_in, onZoomIn),
            ActionButtonConfig(R.string.map_btn_zoom_out, onZoomOut),
        ))
        return resultId
    }
}
