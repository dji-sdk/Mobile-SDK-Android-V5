package com.dji.open.pilot.plugin.sampleitem.armanager.drawing

import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.drawing.DrawingContext
import dji.open.pilot.base.ui.RemoteView
import dji.v5.ux.remoteview.RemoteButton
import dji.v5.ux.remoteview.RemoteLinearLayout
import dji.v5.ux.util.RemoteViewColors

/**
 * AR 投射开关 Tab 的悬浮窗绘制。
 * 当前 SDK 未提供分码流投射控制，保留 Tab 和禁用按钮说明能力不可用。
 */
object ARProjectionDrawer {

    fun draw(
        ctx: DrawingContext,
        container: RemoteLinearLayout
    ): String {
        val resultId = ctx.drawResultCard(container)
        ctx.addSectionLabel(container, R.string.ar_projection_desc)

        ctx.titledCard(container, R.string.ar_projection_group_fpv) { card ->
            buttonRow(ctx, card)
        }
        ctx.titledCard(container, R.string.ar_projection_group_left_or_main) { card ->
            buttonRow(ctx, card)
        }
        return resultId
    }

    /** 在卡片内追加一行左右等分的两个按钮 */
    private fun buttonRow(
        ctx: DrawingContext,
        card: RemoteLinearLayout
    ) {
        val row = ctx.helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        card.addToRemote(row, ctx.marginParam(2))
        row.addToRemote(disabledButton(ctx, R.string.ar_btn_projection_show), ctx.weightParam(1f, 2))
        row.addToRemote(disabledButton(ctx, R.string.ar_btn_projection_hide), ctx.weightParam(1f, 2))
    }

    private fun disabledButton(ctx: DrawingContext, textResId: Int): RemoteButton =
        ctx.helper.createButton().apply {
            setTextByResId(textResId)
            setWidth(0)
            setHeight(32)
            setTextSize(11f)
            setTextColor(RemoteViewColors.WHITE)
            setBackground(RemoteViewColors.WHITE_20)
            setEnabled(false)
            setAlpha(0.5f)
        }
}
