package com.dji.open.pilot.plugin.sampleitem.remoteviewpage.drawing

import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.drawing.DrawingContext

import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.GravityType
import dji.open.pilot.base.ui.model.ShapeDescriptor
import dji.v5.ux.remoteview.RemoteLinearLayout
import dji.v5.ux.util.RemoteViewColors

/**
 * Spinner（下拉框）组件演示：
 * 01 组件类型 — 默认下拉框
 * 02 组件状态 — 禁用态（disabled + 半透明）
 * 03 组件样式 — 自定义文本样式 / 自定义弹窗配色
 */
object SpinnerDrawer {

    fun draw(ctx: DrawingContext, root: RemoteLinearLayout) {
        val helper = ctx.helper
        val items = listOf(
            R.string.demo_spinner_item_option1,
            R.string.demo_spinner_item_option2,
            R.string.demo_spinner_item_option3,
            R.string.demo_spinner_item_option4,
            R.string.demo_spinner_item_option5
        )
        val spinnerItems02 = listOf(
            R.string.demo_spinner_item_land,
            R.string.demo_spinner_item_rth,
            R.string.demo_spinner_item_hover
        )
        val spinnerItems03 = listOf(
            R.string.demo_spinner_item_long,
            R.string.demo_spinner_item_short,
            R.string.demo_spinner_item_long
        )

        // ────────────────────────────────────────────────────────────────────────
        // 01 组件类型
        // ────────────────────────────────────────────────────────────────────────
        ctx.addCardTitle(root, R.string.demo_card_type)
        val typeCard = ctx.card(root)

        ctx.addSectionLabel(typeCard, R.string.demo_spinner_type)
        val defaultSpinner = helper.createSpinner().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(40)
            setBackgroundShape(
                ShapeDescriptor.Builder().stroke(1, RemoteViewColors.WHITE).fillColor(RemoteViewColors.BLACK).cornerRadius(4f).build()
            )
            setItemsByResIds(spinnerItems02);
            setSelectedIndex(0)
            setGravity(GravityType.CENTER)
            setSelectedTextColor(RemoteViewColors.BLUE_PRIMARY)
            setDropdownTextColor(RemoteViewColors.LIGHT_GREY_53)
            setSpinnerIconColor(RemoteViewColors.LIGHT_GREY_53)
        }
        typeCard.addToRemote(defaultSpinner, ctx.marginParam(6, 4, 6, 4))

        // ────────────────────────────────────────────────────────────────────────
        // 02 组件状态
        // ────────────────────────────────────────────────────────────────────────
        ctx.addCardTitle(root, R.string.demo_card_state)
        val stateCard = ctx.card(root)

        ctx.addSectionLabel(stateCard, R.string.demo_spinner_disabled)
        val disabledSpinner = helper.createSpinner().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(40)
            setBackgroundShape(
                ShapeDescriptor.Builder().stroke(1, RemoteViewColors.WHITE).fillColor(RemoteViewColors.BLACK).cornerRadius(4f).build()
            )
            setItemsByResIds(spinnerItems02);
            setSelectedIndex(0)
            setGravity(GravityType.CENTER)
            setSelectedTextColor(RemoteViewColors.BLUE_PRIMARY)
            setDropdownTextColor(RemoteViewColors.LIGHT_GREY_53)
            setSpinnerIconColor(RemoteViewColors.LIGHT_GREY_53)
            setEnabled(false); setAlpha(0.6f)
        }
        stateCard.addToRemote(disabledSpinner, ctx.marginParam(6, 4, 6, 4))

        // ────────────────────────────────────────────────────────────────────────
        // 03 组件样式
        // ────────────────────────────────────────────────────────────────────────
        ctx.addCardTitle(root, R.string.demo_card_style)
        val styleCard = ctx.card(root)

        // Section 1: 自定义弹窗配色（深蓝背景 + 白色文字）
        ctx.addSectionLabel(styleCard, R.string.demo_spinner_text_style)
        val textStyleSpinner = helper.createSpinner().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(40)
            setBackgroundShape(
                ShapeDescriptor.Builder().stroke(1, RemoteViewColors.WHITE).fillColor(RemoteViewColors.BLACK).cornerRadius(4f).build()
            )
            setItemsByResIds(spinnerItems02);
            setSelectedIndex(0)
            setSelectedTextColor(RemoteViewColors.BLUE_PRIMARY)
            setDropdownTextColor(RemoteViewColors.LIGHT_GREY_53)
            setSpinnerIconColor(RemoteViewColors.LIGHT_GREY_53)
        }
        styleCard.addToRemote(textStyleSpinner, ctx.marginParam(6, 4, 6, 4))

        // 分割线
        ctx.addDivider(styleCard)

        // Section 2: 自定义文本（头部文字大小、颜色、对齐、字体样式）
        ctx.addSectionLabel(styleCard, R.string.demo_spinner_custom_bg)
        val customPopupSpinner = helper.createSpinner().apply {
            setWidth(RemoteView.MATCH_PARENT);
            setHeight(40)
            setItemsByResIds(spinnerItems02);
            setGravity(GravityType.CENTER)
            setSelectedIndex(0)
            setSelectedTextColor(RemoteViewColors.WHITE)
            setBackgroundShape(
                ShapeDescriptor.Builder().stroke(1, RemoteViewColors.BLACK).fillColor(RemoteViewColors.DARK_GREY).cornerRadius(4f).build()
            )
            setDropdownTextColor(RemoteViewColors.LIGHT_GREY_53)
            setSpinnerIconColor(RemoteViewColors.WHITE)
        }
        styleCard.addToRemote(customPopupSpinner, ctx.marginParam(6, 4, 6, 4))
    }
}
