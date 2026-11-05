package com.dji.open.pilot.plugin.sampleitem.remoteviewpage.drawing

import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.drawing.DrawingContext
import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.GravityType
import dji.open.pilot.base.ui.model.WidgetLayoutParam
import dji.v5.ux.remoteview.RemoteLinearLayout
import dji.v5.ux.util.RemoteViewColors

/**
 * Switch 开关组件演示：
 * Card 01 — 组件类型：开关 Switch
 * Card 02 — 组件状态：禁用态
 * Card 03 — 组件样式：开关大小 / 开关颜色
 */
object SwitchDrawer {

    fun draw(ctx: DrawingContext, root: RemoteLinearLayout) {
        drawCard01(ctx, root)
        drawCard02(ctx, root)
        drawCard03(ctx, root)
    }

    private fun drawCard01(ctx: DrawingContext, root: RemoteLinearLayout) {
        val helper = ctx.helper
        ctx.addCardTitle(root, R.string.demo_card_type)
        val card = ctx.card(root)

        ctx.addSectionLabel(card, R.string.demo_switch_type)
        val switchRow = helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        card.addToRemote(switchRow, ctx.marginParam(6, 2, 6, 2))

        switchRow.addToRemote(helper.createSwitch().apply {
            setWidth(40)
            setHeight(20)
            setChecked(false)
        }, WidgetLayoutParam().apply {
            setMargin(4, 4, 4, 4)
            layoutGravity = GravityType.CENTER_VERTICAL
        })
    }

    private fun drawCard02(ctx: DrawingContext, root: RemoteLinearLayout) {
        val helper = ctx.helper
        ctx.addCardTitle(root, R.string.demo_card_state)
        val card = ctx.card(root)

        ctx.addSectionLabel(card, R.string.demo_switch_disabled)
        val disabledSwitchRow = helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        card.addToRemote(disabledSwitchRow, ctx.marginParam(6, 2, 6, 2))

        listOf(false, true).forEach { isChecked ->
            disabledSwitchRow.addToRemote(helper.createSwitch().apply {
                setWidth(40)
                setHeight(20)
                setChecked(isChecked)
                setEnabled(false)
                setAlpha(0.6f)
            }, WidgetLayoutParam().apply {
                setMargin(4, 4, 4, 4)
                layoutGravity = GravityType.CENTER_VERTICAL
            })
        }
    }

    private fun drawCard03(ctx: DrawingContext, root: RemoteLinearLayout) {
        val helper = ctx.helper
        ctx.addCardTitle(root, R.string.demo_card_style)
        val card = ctx.card(root)

        // Section 1: 开关大小
        ctx.addSectionLabel(card, R.string.demo_switch_size)
        val sizeColumn = helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        card.addToRemote(sizeColumn, ctx.marginParam(6, 2, 6, 2))

        listOf(
            Pair(35, 15),
            Pair(45, 20),
            Pair(55, 25)
        ).forEach { (switchWidth, switchHeight) ->
            sizeColumn.addToRemote(helper.createSwitch().apply {
                setWidth(switchWidth)
                setHeight(switchHeight)
                setChecked(true)
            }, WidgetLayoutParam().apply {
                setMargin(4, 4, 4, 4)
            })
        }

        ctx.addDivider(card)

        // Section 2: 开关颜色
        ctx.addSectionLabel(card, R.string.demo_switch_color)
        val colorColumn = helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        card.addToRemote(colorColumn, ctx.marginParam(6, 2, 6, 2))

        listOf(
            RemoteViewColors.GREEN_PRIMARY,
            RemoteViewColors.BLUE_MEDIUM,
            RemoteViewColors.RED_PRIMARY
        ).forEach { trackTint ->
            colorColumn.addToRemote(helper.createSwitch().apply {
                setWidth(55)
                setHeight(25)
                setChecked(true)
                setTrackColor(trackTint)
            }, WidgetLayoutParam().apply {
                setMargin(4, 4, 4, 4)
            })
        }
    }
}
