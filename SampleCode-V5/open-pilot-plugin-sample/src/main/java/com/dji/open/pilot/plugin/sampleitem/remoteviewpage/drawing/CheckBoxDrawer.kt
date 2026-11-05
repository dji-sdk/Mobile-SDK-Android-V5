package com.dji.open.pilot.plugin.sampleitem.remoteviewpage.drawing

import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.sampleitem.remoteviewpage.ComponentType
import com.dji.open.pilot.plugin.common.drawing.DrawingContext
import dji.open.pilot.base.ui.RemoteView
import dji.v5.ux.remoteview.RemoteLinearLayout
import dji.v5.ux.util.RemoteViewColors

/**
 * CheckBox 复选框组件演示：
 * Card 01 — 组件类型：未选中 / 已选中
 * Card 02 — 组件状态：禁用态（已选中禁用 / 未选中禁用）
 * Card 03 — 组件样式：复选框颜色（蓝 / 红 / 绿）
 */
object CheckBoxDrawer {

    fun draw(ctx: DrawingContext, root: RemoteLinearLayout) {
        val helper = ctx.helper

        // ── Card 01 组件类型 ─────────────────────────────────────────────────
        ctx.addCardTitle(root, R.string.demo_card_type)
        val typeCard = ctx.card(root)

        ctx.addSectionLabel(typeCard, R.string.demo_checkbox_type)
        val checkBoxRow = helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(RemoteView.WRAP_CONTENT)
        }
        typeCard.addToRemote(checkBoxRow, ctx.marginParam(6, 2, 6, 2))
        listOf(Pair(R.string.demo_checkbox_left, false), Pair(R.string.demo_checkbox_right, true)).forEach { (textResId, checked) ->
            checkBoxRow.addToRemote(helper.createCheckBox().apply {
                setTextByResId(textResId); setWidth(0); setHeight(30); setChecked(checked)
            }, ctx.weightParam(1f, 2))
        }

        // ── Card 02 组件状态 ─────────────────────────────────────────────────
        ctx.addCardTitle(root, R.string.demo_card_state)
        val stateCard = ctx.card(root)

        ctx.addSectionLabel(stateCard, R.string.demo_checkbox_disabled)
        val disabledCheckBoxRow = helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(RemoteView.WRAP_CONTENT)
        }
        stateCard.addToRemote(disabledCheckBoxRow, ctx.marginParam(6, 2, 6, 2))
        listOf(Pair(R.string.demo_checkbox_checked, true), Pair(R.string.demo_checkbox_unchecked, false)).forEach { (textResId, checked) ->
            disabledCheckBoxRow.addToRemote(helper.createCheckBox().apply {
                setTextByResId(textResId); setWidth(0); setHeight(30)
                setChecked(checked); setEnabled(false); setAlpha(0.6f)
            }, ctx.weightParam(1f, 4))
        }

        // ── Card 03 组件样式 ─────────────────────────────────────────────────
        ctx.addCardTitle(root, R.string.demo_card_style)
        val styleCard = ctx.card(root)

        ctx.addSectionLabel(styleCard, R.string.demo_checkbox_color)
        val checkBoxColorRow = helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(RemoteView.WRAP_CONTENT)
        }
        styleCard.addToRemote(checkBoxColorRow, ctx.marginParam(6, 2, 6, 2))
        listOf(
            Pair(R.string.demo_color_blue, RemoteViewColors.BLUE_PRIMARY),
            Pair(R.string.demo_color_red, RemoteViewColors.RED_PRIMARY),
            Pair(R.string.demo_color_green, RemoteViewColors.GREEN_PRIMARY)
        ).forEach { (textResId, tint) ->
            checkBoxColorRow.addToRemote(helper.createCheckBox().apply {
                setTextByResId(textResId); setWidth(0); setHeight(30)
                setChecked(true); setCheckBoxTint(tint)
            }, ctx.weightParam(1f, 4))
        }

    }
}
