package com.dji.open.pilot.plugin.sampleitem.remoteviewpage.drawing

import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.drawing.DrawingContext

import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.FontStyleType
import dji.open.pilot.base.ui.model.GravityType
import dji.open.pilot.base.ui.model.InputType
import dji.open.pilot.base.ui.model.ShapeDescriptor
import dji.v5.ux.remoteview.RemoteLinearLayout
import dji.v5.ux.util.RemoteViewColors

/**
 * EditText 组件演示（三段式）：
 * 01 组件类型 — 基础输入框（填充、描边、默认）
 * 02 组件状态 — 禁用态
 * 03 组件样式 — 键盘类型、输入限制、文字样式、自定义背景
 */
object EditTextDrawer {

    // 提取自定义颜色常量以提升维护性
    private val CUSTOM_GREEN = 0xFF00C774.toInt()

    fun draw(ctx: DrawingContext, root: RemoteLinearLayout) {
        drawCard01(ctx, root)
        drawCard02(ctx, root)
        drawCard03(ctx, root)
    }

    private fun drawCard01(ctx: DrawingContext, root: RemoteLinearLayout) {
        val helper = ctx.helper
        ctx.addCardTitle(root, R.string.demo_card_type)
        val typeCard = ctx.card(root)

        ctx.addSectionLabel(typeCard, R.string.demo_edit_basic)

        typeCard.addToRemote(helper.createEditText().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(36)
            setTextByResId(R.string.demo_edit_filled)
            setTextColor(RemoteViewColors.BLUE_PRIMARY)
            setGravity(GravityType.CENTER)
            setBackgroundShape(
                ShapeDescriptor.Builder().fillColor(RemoteViewColors.BLACK).cornerRadius(4f).build()
            )
        }, ctx.marginParam(6, 4, 6, 4))

        typeCard.addToRemote(helper.createEditText().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(36)
            setTextByResId(R.string.demo_edit_stroke)
            setGravity(GravityType.CENTER)
            setBackgroundShape(
                ShapeDescriptor.Builder().stroke(1, RemoteViewColors.WHITE).cornerRadius(4f).build()
            )
        }, ctx.marginParam(6, 4, 6, 4))

        typeCard.addToRemote(helper.createEditText().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(36)
            setTextByResId(R.string.demo_edit_default)
            setGravity(GravityType.CENTER)
        }, ctx.marginParam(6, 4, 6, 4))
    }

    private fun drawCard02(ctx: DrawingContext, root: RemoteLinearLayout) {
        val helper = ctx.helper
        ctx.addCardTitle(root, R.string.demo_card_state)
        val stateCard = ctx.card(root)

        ctx.addSectionLabel(stateCard, R.string.demo_edit_hint_state)

        stateCard.addToRemote(helper.createEditText().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(36)
            setHintByResId(R.string.demo_edit_hint_input)
            setHintTextColor(RemoteViewColors.WHITE_33)
            setTextColor(RemoteViewColors.BLUE_PRIMARY)
            setGravity(GravityType.CENTER)
            setBackgroundShape(
                ShapeDescriptor.Builder().fillColor(RemoteViewColors.BLACK).cornerRadius(4f).build()
            )
        }, ctx.marginParam(6, 4, 6, 4))

        stateCard.addToRemote(helper.createEditText().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(36)
            setHintByResId(R.string.demo_edit_hint_input)
            setHintTextColor(RemoteViewColors.RED_PRIMARY_33)
            setTextColor(RemoteViewColors.BLUE_PRIMARY)
            setGravity(GravityType.CENTER)
            setBackgroundShape(
                ShapeDescriptor.Builder().fillColor(RemoteViewColors.BLACK).cornerRadius(4f).build()
            )
        }, ctx.marginParam(6, 4, 6, 4))

        ctx.addSectionLabel(stateCard, R.string.demo_edit_disabled)

        stateCard.addToRemote(helper.createEditText().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(36)
            setTextByResId(R.string.demo_edit_filled)
            setTextColor(RemoteViewColors.BLUE_PRIMARY)
            setGravity(GravityType.CENTER)
            setEnabled(false)
            setAlpha(0.6f)
            setBackgroundShape(
                ShapeDescriptor.Builder().fillColor(RemoteViewColors.BLACK).cornerRadius(4f).build()
            )
        }, ctx.marginParam(6, 4, 6, 4))

        stateCard.addToRemote(helper.createEditText().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(36)
            setTextByResId(R.string.demo_edit_stroke)
            setGravity(GravityType.CENTER)
            setEnabled(false)
            setAlpha(0.6f)
            setBackgroundShape(
                ShapeDescriptor.Builder().stroke(1, RemoteViewColors.WHITE).cornerRadius(4f).build()
            )
        }, ctx.marginParam(6, 4, 6, 4))

        stateCard.addToRemote(helper.createEditText().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(36)
            setTextByResId(R.string.demo_edit_default)
            setGravity(GravityType.CENTER)
            setEnabled(false)
            setAlpha(0.6f)
        }, ctx.marginParam(6, 4, 6, 4))
    }

    private fun drawCard03(ctx: DrawingContext, root: RemoteLinearLayout) {
        val helper = ctx.helper
        ctx.addCardTitle(root, R.string.demo_card_style)
        val styleCard = ctx.card(root)

        // Section 1: 键盘类型
        ctx.addSectionLabel(styleCard, R.string.demo_edit_keyboard)

        styleCard.addToRemote(helper.createEditText().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(36)
            setHintByResId(R.string.demo_edit_hint_number)
            setHintTextColor(RemoteViewColors.LIGHT_GREY_53)
            setInputType(InputType.NUMBER)
            setText("012345678910")
            setTextColor(RemoteViewColors.BLUE_PRIMARY)
            setGravity(GravityType.CENTER)
            setBackgroundShape(
                ShapeDescriptor.Builder().fillColor(RemoteViewColors.BLACK).cornerRadius(4f).build()
            )
        }, ctx.marginParam(6, 4, 6, 4))

        styleCard.addToRemote(helper.createEditText().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(36)
            setHintByResId(R.string.demo_edit_hint_password)
            setHintTextColor(RemoteViewColors.LIGHT_GREY_53)
            setInputType(InputType.PASSWORD)
            setText("012345678910")
            setTextColor(RemoteViewColors.BLUE_PRIMARY)
            setGravity(GravityType.CENTER)
            setBackgroundShape(
                ShapeDescriptor.Builder().fillColor(RemoteViewColors.BLACK).cornerRadius(4f).build()
            )
        }, ctx.marginParam(6, 4, 6, 4))

        ctx.addDivider(styleCard)

        // Section 2: 输入限制
        ctx.addSectionLabel(styleCard, R.string.demo_edit_limit)

        styleCard.addToRemote(helper.createEditText().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(36)
            setHintByResId(R.string.demo_edit_hint_bankcard)
            setHintTextColor(RemoteViewColors.LIGHT_GREY_53)
            setGrouping("4")
            setText("0000111122223333")
            setTextColor(RemoteViewColors.BLUE_PRIMARY)
            setGravity(GravityType.CENTER)
            setBackgroundShape(
                ShapeDescriptor.Builder().fillColor(RemoteViewColors.BLACK).cornerRadius(4f).build()
            )
        }, ctx.marginParam(6, 4, 6, 4))

        styleCard.addToRemote(helper.createEditText().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(36)
            setHintByResId(R.string.demo_edit_max8)
            setTextByResId(R.string.demo_edit_max8)
            setHintTextColor(RemoteViewColors.LIGHT_GREY_53)
            setMaxLength(8)
            setTextColor(RemoteViewColors.BLUE_PRIMARY)
            setGravity(GravityType.CENTER)
            setBackgroundShape(
                ShapeDescriptor.Builder().fillColor(RemoteViewColors.BLACK).cornerRadius(4f).build()
            )
        }, ctx.marginParam(6, 4, 6, 4))

        ctx.addDivider(styleCard)

        // Section 3: 文字样式
        ctx.addSectionLabel(styleCard, R.string.demo_edit_text_style)

        styleCard.addToRemote(helper.createEditText().apply {
            setWidth(RemoteView.MATCH_PARENT);
            setHeight(36)
            setPadding(2,2,2,2)
            setTextByResId(R.string.demo_edit_blue_left)
            setHintTextColor(RemoteViewColors.LIGHT_GREY_53)
            setGravity(GravityType.CENTER_VERTICAL)
            setTextSize(13f)
            setTextColor(RemoteViewColors.BLUE_PRIMARY)
            setBackgroundShape(
                ShapeDescriptor.Builder().fillColor(RemoteViewColors.BLACK).cornerRadius(4f).build()
            )
        }, ctx.marginParam(6, 4, 6, 4))

        styleCard.addToRemote(helper.createEditText().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(36)
            setTextByResId(R.string.demo_edit_center_bold)
            setHintTextColor(RemoteViewColors.LIGHT_GREY_53)
            setGravity(GravityType.CENTER)
            setTextSize(13f)
            setFontStyle(FontStyleType.BOLD)
            setTextColor(RemoteViewColors.BLUE_PRIMARY)
            setBackgroundShape(
                ShapeDescriptor.Builder().fillColor(RemoteViewColors.BLACK).cornerRadius(4f).build()
            )
        }, ctx.marginParam(6, 4, 6, 4))

        ctx.addDivider(styleCard)

        // Section 4: 自定义背景
        ctx.addSectionLabel(styleCard, R.string.demo_edit_custom_bg)

        styleCard.addToRemote(helper.createEditText().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(36)
            setHintByResId(R.string.demo_edit_filled)
            setTextByResId(R.string.demo_edit_filled)
            setGravity(GravityType.CENTER)
            setTextColor(RemoteViewColors.BLACK)
            setBackgroundShape(
                ShapeDescriptor.Builder().fillColor(RemoteViewColors.LIGHT_GREY_53).cornerRadius(4f).build()
            )
        }, ctx.marginParam(6, 4, 6, 4))

        styleCard.addToRemote(helper.createEditText().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(36)
            setHintByResId(R.string.demo_edit_stroke)
            setTextByResId(R.string.demo_edit_stroke)
            setGravity(GravityType.CENTER)
            setTextColor(RemoteViewColors.WHITE)
            setBackgroundShape(
                ShapeDescriptor.Builder().stroke(2, RemoteViewColors.BLUE_PRIMARY).cornerRadius(4f).build()
            )
        }, ctx.marginParam(6, 4, 6, 4))

        styleCard.addToRemote(helper.createEditText().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(36)
            setHintByResId(R.string.demo_edit_default)
            setTextByResId(R.string.demo_edit_default)
            setGravity(GravityType.CENTER)
            setTextColor(RemoteViewColors.WHITE)
        }, ctx.marginParam(6, 4, 6, 4))
    }
}
