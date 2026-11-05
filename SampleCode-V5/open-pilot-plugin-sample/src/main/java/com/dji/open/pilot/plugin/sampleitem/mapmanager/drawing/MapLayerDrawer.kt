package com.dji.open.pilot.plugin.sampleitem.mapmanager.drawing

import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.drawing.DrawingContext
import dji.open.pilot.base.ui.IRemoteViewOnItemSelectedListener
import dji.open.pilot.base.ui.RemoteView
import dji.v5.ux.remoteview.RemoteLinearLayout

/**
 * 图层管理（MapWidgetLayer）用例的悬浮窗绘制。
 *
 * 演示控件：在飞机的东/西/南/北四个方位分别绘制「等边三角形（底朝飞机）+ 内切圆 + Marker（圆心）」，
 * 并把同类图元归入三个图层：三角形层 / 内切圆层 / Marker层。
 *
 * 顶部下拉框选择操作对象（三角形层 / 内切圆层 / Marker层 / 全部图层），
 * 下方按钮对当前选中层执行：建层并绘制 / 隐藏 / 显示 / 按层查询 / 清空 / 删除。
 *
 * 下拉框选中索引通过 onTargetChanged 回传给 ViewModel；各操作按钮回调不带参，
 * ViewModel 读取最近一次选中的图层目标执行。
 */
object MapLayerDrawer {

    fun draw(
        ctx: DrawingContext,
        container: RemoteLinearLayout,
        onTargetChanged: (index: Int) -> Unit,
        onBuild: () -> Unit,
        onHide: () -> Unit,
        onShow: () -> Unit,
        onQuery: () -> Unit,
        onClear: () -> Unit,
        onRemove: () -> Unit
    ): String {
        val resultId = ctx.drawResultCard(container)

        // 下拉框：选择操作的目标图层
        ctx.titledCard(container, R.string.map_layer_target_title) { card ->
            val spinner = ctx.helper.createSpinner().apply {
                setWidth(RemoteView.MATCH_PARENT)
                setHeight(36)
                setItemsByResIds(listOf(
                    R.string.map_layer_triangle,
                    R.string.map_layer_circle,
                    R.string.map_layer_marker,
                    R.string.map_layer_all
                ))
                setSelectedIndex(0)
                setOnItemSelectedListener(object : IRemoteViewOnItemSelectedListener {
                    override fun onItemSelected(index: Int, item: String?) {
                        onTargetChanged(index)
                    }
                })
            }
            card.addToRemote(spinner, ctx.marginParam(2))
        }

        // 建层并绘制 + 对下拉框选中层的操作，统一放一张操作卡片
        ctx.titledCard(container, R.string.card_title_action) { card ->
            card.addToRemote(ctx.primaryButton(R.string.map_btn_layer_build, onBuild), ctx.marginParam(2))
            tripleRow(ctx, card,
                R.string.map_btn_layer_hide, onHide,
                R.string.map_btn_layer_show, onShow,
                R.string.map_btn_layer_query, onQuery)
            pairRow(ctx, card,
                R.string.map_btn_layer_clear, onClear,
                R.string.map_btn_layer_remove, onRemove)
        }
        return resultId
    }

    private fun tripleRow(
        ctx: DrawingContext, card: RemoteLinearLayout,
        r1: Int, o1: () -> Unit, r2: Int, o2: () -> Unit, r3: Int, o3: () -> Unit
    ) {
        val row = ctx.helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(RemoteView.WRAP_CONTENT)
        }
        card.addToRemote(row, ctx.marginParam(2))
        row.addToRemote(ctx.primaryButton(r1, o1, width = 0), ctx.weightParam(1f, 2))
        row.addToRemote(ctx.primaryButton(r2, o2, width = 0), ctx.weightParam(1f, 2))
        row.addToRemote(ctx.primaryButton(r3, o3, width = 0), ctx.weightParam(1f, 2))
    }

    private fun pairRow(
        ctx: DrawingContext, card: RemoteLinearLayout,
        r1: Int, o1: () -> Unit, r2: Int, o2: () -> Unit
    ) {
        val row = ctx.helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(RemoteView.WRAP_CONTENT)
        }
        card.addToRemote(row, ctx.marginParam(2))
        row.addToRemote(ctx.primaryButton(r1, o1, width = 0), ctx.weightParam(1f, 2))
        row.addToRemote(ctx.primaryButton(r2, o2, width = 0), ctx.weightParam(1f, 2))
    }
}
