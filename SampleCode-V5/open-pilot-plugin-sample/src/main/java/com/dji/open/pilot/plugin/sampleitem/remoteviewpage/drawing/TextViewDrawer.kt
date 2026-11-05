package com.dji.open.pilot.plugin.sampleitem.remoteviewpage.drawing

import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.sampleitem.remoteviewpage.ComponentType
import com.dji.open.pilot.plugin.common.drawing.DrawingContext
import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.EllipsizeType
import dji.open.pilot.base.ui.model.FontStyleType
import dji.open.pilot.base.ui.model.TextDecorationType
import dji.v5.ux.remoteview.RemoteLinearLayout
import dji.v5.ux.util.RemoteViewColors

/**
 * TextView 组件演示。
 *
 * Card 01 — 组件类型：
 *   - 基础文本：普通 / 下划线 / 删除线 / 加粗 / 色彩
 *   - 特殊类型：单行 / 截断 / 折行 / 可滚动
 *
 * Card 02 — 组件状态：
 *   - 文字禁用态：正常态对比 + 置灰态
 *
 * Card 03 — 组件样式：
 *   - 字体尺寸：大/中/小三档（字号 + 行高 + 字间距组合）
 *   - 自定义颜色：蓝 / 红 / 橙
 */
object TextViewDrawer {

    // 设计稿指定色值
    private val COLOR_BLUE   = 0xFF1890FF.toInt()
    private val COLOR_RED    = 0xFFE41F29.toInt()
    private val COLOR_ORANGE = 0xFFFF9F0A.toInt()

    // 置灰色
    private val COLOR_DISABLED = 0xFF888888.toInt()

    // section 小标题样式（浅灰色，用于区分 section）— 统一使用 ctx.addSectionLabel()
    // 分割线（section 之间）— 统一使用 ctx.addDivider()

    fun draw(ctx: DrawingContext, root: RemoteLinearLayout) {
        val helper = ctx.helper

        // ── Card 01 组件类型 ─────────────────────────────────────────────────
        ctx.addCardTitle(root, R.string.demo_card_type)
        val typeCard = ctx.card(root)

        // Section：基础文本
        ctx.addSectionLabel(typeCard, R.string.demo_text_basic)

        typeCard.addToRemote(helper.createText().apply {
            setTextByResId(R.string.demo_text_normal)
            setWidth(RemoteView.MATCH_PARENT); setHeight(RemoteView.WRAP_CONTENT)
            setFontStyle(FontStyleType.NORMAL)
        }, ctx.marginParam(6, 2, 6, 2))

        typeCard.addToRemote(helper.createText().apply {
            setTextByResId(R.string.demo_text_underline)
            setWidth(RemoteView.MATCH_PARENT); setHeight(RemoteView.WRAP_CONTENT)
            setTextDecoration(TextDecorationType.UNDERLINE)
        }, ctx.marginParam(6, 2, 6, 2))

        typeCard.addToRemote(helper.createText().apply {
            setTextByResId(R.string.demo_text_strike)
            setWidth(RemoteView.MATCH_PARENT); setHeight(RemoteView.WRAP_CONTENT)
            setTextDecoration(TextDecorationType.STRIKE_THROUGH)
        }, ctx.marginParam(6, 2, 6, 2))

        typeCard.addToRemote(helper.createText().apply {
            setTextByResId(R.string.demo_text_bold)
            setWidth(RemoteView.MATCH_PARENT); setHeight(RemoteView.WRAP_CONTENT)
            setFontStyle(FontStyleType.BOLD)
        }, ctx.marginParam(6, 2, 6, 2))

        typeCard.addToRemote(helper.createText().apply {
            setTextByResId(R.string.demo_text_color)
            setWidth(RemoteView.MATCH_PARENT); setHeight(RemoteView.WRAP_CONTENT)
            setTextColor(COLOR_BLUE)
        }, ctx.marginParam(6, 2, 6, 2))

        // Section：特殊类型
        ctx.addDivider(typeCard)
        ctx.addSectionLabel(typeCard, R.string.demo_text_special)

        // 单行（不截断，宽度充足时完整展示）
        typeCard.addToRemote(helper.createText().apply {
            setTextByResId(R.string.demo_text_single)
            setWidth(RemoteView.MATCH_PARENT); setHeight(RemoteView.WRAP_CONTENT)
            setMaxLines(1)
        }, ctx.marginParam(6, 2, 6, 2))

        // 截断：内容超长，END 截断
        typeCard.addToRemote(helper.createText().apply {
            setTextByResId(R.string.demo_text_truncate)
            setWidth(RemoteView.MATCH_PARENT); setHeight(RemoteView.WRAP_CONTENT)
            setMaxLines(1); setEllipsize(EllipsizeType.END)
        }, ctx.marginParam(6, 2, 6, 2))

        // 折行：内容超长，自然折行展示全部
        typeCard.addToRemote(helper.createText().apply {
            setTextByResId(R.string.demo_text_wrap)
            setWidth(RemoteView.MATCH_PARENT); setHeight(RemoteView.WRAP_CONTENT)
        }, ctx.marginParam(6, 2, 6, 2))

        // 可滚动：固定高度，内容超出可手动滑动
        typeCard.addToRemote(helper.createText().apply {
            setTextByResId(R.string.demo_text_scroll)
            setWidth(RemoteView.MATCH_PARENT); setHeight(56)
            setVerticalScrollable(true)
        }, ctx.marginParam(6, 2, 6, 2))

        // HTML 文案兼容示例：5.20.1 在客户端解析后以纯文本下发，不保留内联样式。
        // 保留资源模板及参数，语言切换时重新格式化并刷新电量与动作文本。
        typeCard.addToRemote(helper.createText().apply {
            setRichTextByResId(
                R.string.demo_text_richtext,
                "#E41F29",   // %1$s 模板颜色参数，纯文本降级后不显示颜色
                15,          // %2$d 电量数值
                "#1890FF"    // %3$s 模板颜色参数，纯文本降级后不显示颜色
            )
            setWidth(RemoteView.MATCH_PARENT); setHeight(RemoteView.WRAP_CONTENT)
        }, ctx.marginParam(6, 2, 6, 2))

        // ── Card 02 组件状态 ─────────────────────────────────────────────────
        ctx.addCardTitle(root, R.string.demo_card_state)
        val stateCard = ctx.card(root)

        // 正常态（对比用）
        stateCard.addToRemote(helper.createText().apply {
            setTextByResId(R.string.demo_text_state_normal)
            setWidth(RemoteView.MATCH_PARENT); setHeight(RemoteView.WRAP_CONTENT)
        }, ctx.marginParam(6, 2, 6, 2))

        // 置灰态：alpha 降低模拟禁用
        stateCard.addToRemote(helper.createText().apply {
            setTextByResId(R.string.demo_text_state_disabled)
            setWidth(RemoteView.MATCH_PARENT); setHeight(RemoteView.WRAP_CONTENT)
            setTextColor(COLOR_DISABLED)
            setAlpha(0.6f)
        }, ctx.marginParam(6, 2, 6, 2))

        // ── Card 03 组件样式 ─────────────────────────────────────────────────
        ctx.addCardTitle(root, R.string.demo_card_style)
        val styleCard = ctx.card(root)

        // Section：字体尺寸（字号 + 行高 + 字间距三档组合）
        ctx.addSectionLabel(styleCard, R.string.demo_text_size)

        // 大：10px，行高约16px（lineSpacing add=6f 补足行高），字间距 0.04f
        styleCard.addToRemote(helper.createText().apply {
            setTextByResId(R.string.demo_text_size_l)
            setWidth(RemoteView.MATCH_PARENT); setHeight(RemoteView.WRAP_CONTENT)
            setTextSize(12f)
            setLetterSpacing(0.01f)
        }, ctx.marginParam(6))

        // 中：9px，行高约14px（lineSpacing add=5f），字间距 0.03f
        styleCard.addToRemote(helper.createText().apply {
            setTextByResId(R.string.demo_text_size_m)
            setWidth(RemoteView.MATCH_PARENT); setHeight(RemoteView.WRAP_CONTENT)
            setTextSize(11f)
            setLetterSpacing(0.03f)
            setLineSpacing(1f, 1f)
        }, ctx.marginParam(6))

        // 小：8px，行高约12px（lineSpacing add=4f），字间距 0.02f
        styleCard.addToRemote(helper.createText().apply {
            setTextByResId(R.string.demo_text_size_s)
            setWidth(RemoteView.MATCH_PARENT); setHeight(RemoteView.WRAP_CONTENT)
            setTextSize(9f)
            setLetterSpacing(0.05f)
            setLineSpacing(0f, 1.3f)
        }, ctx.marginParam(6))

        // Section：自定义颜色
        ctx.addDivider(styleCard)
        ctx.addSectionLabel(styleCard, R.string.demo_custom_color)

        listOf(
            Pair(R.string.demo_text_color_sample, COLOR_BLUE),
            Pair(R.string.demo_text_color_sample, COLOR_RED),
            Pair(R.string.demo_text_color_sample, COLOR_ORANGE)
        ).forEach { (textResId, color) ->
            styleCard.addToRemote(helper.createText().apply {
                setTextByResId(textResId)
                setWidth(RemoteView.MATCH_PARENT); setHeight(RemoteView.WRAP_CONTENT)
                setTextColor(color)
            }, ctx.marginParam(6, 2, 6, 2))
        }

    }
}
