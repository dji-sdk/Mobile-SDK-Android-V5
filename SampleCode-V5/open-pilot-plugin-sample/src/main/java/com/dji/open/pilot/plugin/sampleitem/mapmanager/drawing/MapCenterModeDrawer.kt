package com.dji.open.pilot.plugin.sampleitem.mapmanager.drawing

import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.drawing.ActionButtonConfig
import com.dji.open.pilot.plugin.common.drawing.DrawingContext
import dji.v5.ux.remoteview.RemoteLinearLayout

/**
 * 地图中心点模式用例的悬浮窗绘制。
 * 两个按钮：跟随飞机（FOLLOW_AIRCRAFT）、自由（FREE）。
 * 结果卡片顶部实时回显 MapState（isMapReady / zoom / center / mapCenterMode）。
 */
object MapCenterModeDrawer {

    fun draw(
        ctx: DrawingContext,
        container: RemoteLinearLayout,
        onFollowAircraft: () -> Unit,
        onFree: () -> Unit
    ): String {
        val resultId = ctx.drawResultCard(container)
        ctx.drawActionCard(container, listOf(
            ActionButtonConfig(R.string.map_btn_follow_aircraft, onFollowAircraft),
            ActionButtonConfig(R.string.map_btn_free, onFree),
        ))
        return resultId
    }
}
