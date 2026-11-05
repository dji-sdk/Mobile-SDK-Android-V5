package com.dji.open.pilot.plugin.sampleitem.mapmanager.drawing

import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.drawing.DrawingContext
import dji.open.pilot.base.ui.RemoteView
import dji.v5.ux.remoteview.RemoteLinearLayout

/**
 * 地图控件（MapWidget）用例的悬浮窗绘制。
 * 四类图元（Marker / 线 / 面 / 圆）
 * 各自独立的「添加 / 删除」按钮，均以飞机当前位置为中心绘制。底部提供「查询」。
 */
object MapWidgetDrawer {

    fun draw(
        ctx: DrawingContext,
        container: RemoteLinearLayout,
        onAddMarker: () -> Unit, onRemoveMarker: () -> Unit,
        onAddPolyline: () -> Unit, onRemovePolyline: () -> Unit,
        onAddPolygon: () -> Unit, onRemovePolygon: () -> Unit,
        onAddCircle: () -> Unit, onRemoveCircle: () -> Unit,
        onQuery: () -> Unit
    ): String {
        val resultId = ctx.drawResultCard(container)

        ctx.titledCard(container, R.string.card_title_action) { card ->
            pairRow(ctx, card, R.string.map_btn_add_marker, onAddMarker, R.string.map_btn_remove_marker, onRemoveMarker)
            pairRow(ctx, card, R.string.map_btn_add_polyline, onAddPolyline, R.string.map_btn_remove_polyline, onRemovePolyline)
            pairRow(ctx, card, R.string.map_btn_add_polygon, onAddPolygon, R.string.map_btn_remove_polygon, onRemovePolygon)
            pairRow(ctx, card, R.string.map_btn_add_circle, onAddCircle, R.string.map_btn_remove_circle, onRemoveCircle)
            card.addToRemote(ctx.primaryButton(R.string.map_btn_query_widgets, onQuery), ctx.marginParam(2))
        }
        return resultId
    }

    /** 在卡片内追加一行左右等分的两个按钮 */
    private fun pairRow(
        ctx: DrawingContext,
        card: RemoteLinearLayout,
        leftRes: Int, onLeft: () -> Unit,
        rightRes: Int, onRight: () -> Unit
    ) {
        val row = ctx.helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        card.addToRemote(row, ctx.marginParam(2))
        row.addToRemote(ctx.primaryButton(leftRes, onLeft, width = 0), ctx.weightParam(1f, 2))
        row.addToRemote(ctx.primaryButton(rightRes, onRight, width = 0), ctx.weightParam(1f, 2))
    }
}
