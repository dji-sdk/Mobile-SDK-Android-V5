package com.dji.open.pilot.plugin.sampleitem.remoteviewpage.drawing

import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.sampleitem.remoteviewpage.ComponentType
import com.dji.open.pilot.plugin.common.drawing.DrawingContext
import dji.open.pilot.base.ui.RemoteView
import dji.v5.ux.remoteview.RemoteLinearLayout

/**
 * WebView 组件演示：加载指定 URL。
 */
object WebViewDrawer {

    fun draw(ctx: DrawingContext, root: RemoteLinearLayout) {
        val helper = ctx.helper

        ctx.addCardTitle(root, R.string.demo_card_type)
        val typeCard = ctx.card(root)

        ctx.addSectionLabel(typeCard, R.string.demo_web_load)
        val webView = helper.createWebView().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(150)
            setUrl("https://www.dji.com")
        }
        typeCard.addToRemote(webView, ctx.marginParam(1, 2, 1, 4))

    }
}
