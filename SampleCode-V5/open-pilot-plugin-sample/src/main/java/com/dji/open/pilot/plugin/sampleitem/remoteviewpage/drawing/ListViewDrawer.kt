package com.dji.open.pilot.plugin.sampleitem.remoteviewpage.drawing

import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.drawing.DrawingContext
import com.dji.open.pilot.plugin.main.PluginApplication
import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.DividerConfig
import dji.open.pilot.base.ui.model.ListItemData
import dji.open.pilot.base.ui.model.ShapeDescriptor
import dji.v5.ux.remoteview.RemoteLinearLayout

/**
 * ListView 组件演示：支持基础列表、文本、图标文本和双行图文模式。
 */
object ListViewDrawer {

    fun draw(ctx: DrawingContext, root: RemoteLinearLayout) {
        val helper = ctx.helper

        // 01 组件类型
        ctx.addCardTitle(root, R.string.demo_card_type)
        val typeCard = ctx.card(root)

        ctx.addSectionLabel(typeCard, R.string.demo_list_basic)
        val baseListView = helper.createListView().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setBackgroundShape(ShapeDescriptor.Builder().cornerRadius(2f).stroke(1, -0x1).build())
        }
        typeCard.addToRemote(baseListView, ctx.marginParam(6, 2, 6, 4))
        baseListView.setItems((1..3).map { ListItemData.text("item_$it", "Item $it") })
        baseListView.setDividerConfig(DividerConfig(1, "#FFECF0F1", 0, 0, false))
        baseListView.setItemClickEnabled(true)

        // 02 组件样式
        ctx.addCardTitle(root, R.string.demo_card_style_02)
        val styleCard = ctx.card(root)

        ctx.addSectionLabel(styleCard, R.string.demo_list_text)
        val textListView = helper.createListView().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setBackgroundShape(ShapeDescriptor.Builder().cornerRadius(2f).stroke(1, -0x1).build())
        }
        styleCard.addToRemote(textListView, ctx.marginParam(6, 2, 6, 4))
        textListView.setItems((1..3).map { ListItemData.text("text_item_$it", "Item $it") })
        textListView.setDividerConfig(DividerConfig(1, "#FFECF0F1", 0, 0, false))
        textListView.setItemClickEnabled(true)

        ctx.addDivider(styleCard)

        ctx.addSectionLabel(styleCard, R.string.demo_list_icon_text)
        val iconTextListView = helper.createListView().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setBackgroundShape(ShapeDescriptor.Builder().cornerRadius(2f).stroke(1, -0x1).build())
        }
        styleCard.addToRemote(iconTextListView, ctx.marginParam(6, 2, 6, 4))
        val iconTextItems = listOf(
            ListItemData.iconText("icon_text_success", DrawingContext.ICON_IDF_STATUS_SUCCESS, PluginApplication.instance?.getString(R.string.demo_list_success) ?: ""),
            ListItemData.iconText("icon_text_error", DrawingContext.ICON_IDF_STATUS_ERROR, PluginApplication.instance?.getString(R.string.demo_list_fail) ?: "")
        )
        iconTextListView.setItems(iconTextItems)
        iconTextListView.setDividerConfig(DividerConfig(1, "#FFECF0F1", 0, 0, false))
        iconTextListView.setItemClickEnabled(true)

        ctx.addDivider(styleCard)

        ctx.addSectionLabel(styleCard, R.string.demo_list_icon_twoline)
        val iconTwoLineListView = helper.createListView().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setBackgroundShape(ShapeDescriptor.Builder().cornerRadius(2f).stroke(1, -0x1).build())
        }
        styleCard.addToRemote(iconTwoLineListView, ctx.marginParam(6, 2, 6, 4))
        val iconTwoLineItems = listOf(
            ListItemData.iconTwoLine("icon_two_line_success", DrawingContext.ICON_IDF_STATUS_SUCCESS, PluginApplication.instance?.getString(R.string.demo_list_success) ?: "", PluginApplication.instance?.getString(R.string.demo_list_success_desc) ?: ""),
            ListItemData.iconTwoLine("icon_two_line_error", DrawingContext.ICON_IDF_STATUS_ERROR, PluginApplication.instance?.getString(R.string.demo_list_fail) ?: "", PluginApplication.instance?.getString(R.string.demo_list_fail_desc) ?: "")
        )
        iconTwoLineListView.setItems(iconTwoLineItems)
        iconTwoLineListView.setDividerConfig(DividerConfig(1, "#FFECF0F1", 0, 0, false))
        iconTwoLineListView.setItemClickEnabled(true)
    }
}
