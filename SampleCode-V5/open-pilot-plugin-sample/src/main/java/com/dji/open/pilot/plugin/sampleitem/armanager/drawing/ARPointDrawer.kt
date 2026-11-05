package com.dji.open.pilot.plugin.sampleitem.armanager.drawing

import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.drawing.ActionButtonConfig
import com.dji.open.pilot.plugin.common.drawing.DrawingContext
import dji.v5.ux.remoteview.RemoteLinearLayout

/**
 * AR 点 Tab 的悬浮窗绘制。
 * 卡片1：查询结果 TextView；卡片2：查询/上传/更新/删除 4 个按钮。
 */
object ARPointDrawer {

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
