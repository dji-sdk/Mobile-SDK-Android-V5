package com.dji.open.pilot.plugin.sampleitem.armanager.drawing

import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.drawing.ActionButtonConfig
import com.dji.open.pilot.plugin.common.drawing.DrawingContext
import dji.v5.ux.remoteview.RemoteLinearLayout

/**
 * AR 面（多边形）Tab 的悬浮窗绘制。
 */
object ARPolygonDrawer {

    fun draw(
        ctx: DrawingContext,
        container: RemoteLinearLayout,
        onQuery: () -> Unit,
        onUpload: () -> Unit,
        onUpdate: () -> Unit,
        onDelete: () -> Unit
    ): String {
        val resultId = ctx.drawResultCard(container)
        ctx.drawActionCard(container, listOf(
            ActionButtonConfig(R.string.ar_btn_query, onQuery),
            ActionButtonConfig(R.string.ar_btn_upload, onUpload),
            ActionButtonConfig(R.string.ar_btn_update, onUpdate),
            ActionButtonConfig(R.string.ar_btn_delete, onDelete),
        ))
        return resultId
    }
}
