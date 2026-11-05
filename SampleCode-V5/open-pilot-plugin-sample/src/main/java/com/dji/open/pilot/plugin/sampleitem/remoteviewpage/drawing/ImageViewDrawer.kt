package com.dji.open.pilot.plugin.sampleitem.remoteviewpage.drawing

import android.graphics.Color
import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.sampleitem.remoteviewpage.ComponentType
import com.dji.open.pilot.plugin.common.drawing.DrawingContext
import com.dji.open.pilot.plugin.common.drawing.DrawingContext.Companion.PHOTO_SEA_VIEW
import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.GravityType
import dji.open.pilot.base.ui.model.ScaleType
import dji.open.pilot.base.ui.model.ShapeDescriptor
import dji.open.pilot.base.ui.model.WidgetLayoutParam
import dji.v5.ux.remoteview.RemoteLinearLayout

/**
 * ImageView 组件演示。
 *
 * Card 01 — 组件类型：
 *   - 方形图片 / 圆角图片 / 圆形图片（每张独占一行，全宽展示）
 *
 * Card 02 — 组件样式：
 *   - 基础图片（无额外属性）
 *   - 颜色填充图片（imageTint #4DFFCC00）
 *   - 颜色描边图片（stroke 2dp #FFCC00）
 *
 * 注：ImageView 不支持文字叠加（渲染层静默忽略 TEXT 属性），
 *     如需图片上叠加文字，应在 ConstraintLayout 中同时放置
 *     ImageView + TextView 并设置约束。
 */
object ImageViewDrawer {

    private const val SAMPLE_IMAGE_FILE = PHOTO_SEA_VIEW

    private val COLOR_TINT   = 0x4DFFCC00.toInt()  // 颜色填充：50% 透明黄
    private val COLOR_STROKE = 0xFFFFCC00.toInt()   // 颜色描边：不透明黄

    fun draw(ctx: DrawingContext, root: RemoteLinearLayout) {
        val helper = ctx.helper

        // ── Card 01 组件类型 ─────────────────────────────────────────────────
        ctx.addCardTitle(root, R.string.demo_card_type)
        val typeCard = ctx.card(root)

        // 方形图片
        ctx.addSectionLabel(typeCard, R.string.demo_image_square)
        typeCard.addToRemote(helper.createImage().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(80)
            setImageFileName(SAMPLE_IMAGE_FILE)
            setScaleType(ScaleType.CENTER_CROP)
        }, ctx.marginParam(6, 2, 6, 4))

        // 圆角图片
        ctx.addDivider(typeCard)
        ctx.addSectionLabel(typeCard, R.string.demo_image_round)
        typeCard.addToRemote(helper.createImage().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(80)
            setImageFileName(SAMPLE_IMAGE_FILE)
            setScaleType(ScaleType.CENTER_CROP)
            setBackgroundShape(ShapeDescriptor.Builder().cornerRadius(12f).build())
        }, ctx.marginParam(6, 2, 6, 4))

        // 圆形图片
        ctx.addDivider(typeCard)
        ctx.addSectionLabel(typeCard, R.string.demo_image_circle)
        typeCard.addToRemote(helper.createImage().apply {
            setWidth(80); setHeight(80)
            setImageFileName(SAMPLE_IMAGE_FILE)
            setScaleType(ScaleType.CENTER_CROP)
            setBackgroundShape(
                ShapeDescriptor.Builder().shape(ShapeDescriptor.SHAPE_OVAL).fillColor(
                    Color.RED
                ).build()
            )
        }, WidgetLayoutParam().apply {
            setMargin(6, 2, 6, 4)
            layoutGravity = GravityType.CENTER_HORIZONTAL
        })

        // ── Card 02 组件样式 ─────────────────────────────────────────────────
        ctx.addCardTitle(root, R.string.demo_card_style_02)
        val styleCard = ctx.card(root)

        // 基础图片
        ctx.addSectionLabel(styleCard, R.string.demo_image_basic)
        styleCard.addToRemote(helper.createImage().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(80)
            setImageFileName(SAMPLE_IMAGE_FILE)
            setScaleType(ScaleType.CENTER_CROP)
            setBackgroundShape(ShapeDescriptor.Builder().cornerRadius(DrawingContext.CORNER_DEFAULT).build())
        }, ctx.marginParam(6, 2, 6, 4))

        // 基础图片
        ctx.addSectionLabel(styleCard, R.string.demo_image_alpha)
        styleCard.addToRemote(helper.createImage().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(80)
            setImageFileName(SAMPLE_IMAGE_FILE)
            setAlpha(0.4f)
            setScaleType(ScaleType.CENTER_CROP)
            setBackgroundShape(ShapeDescriptor.Builder().cornerRadius(DrawingContext.CORNER_DEFAULT).build())
        }, ctx.marginParam(6, 2, 6, 4))

        // 颜色填充图片（imageTint #4DFFCC00，约 30% 透明黄色蒙版）
        ctx.addDivider(styleCard)
        ctx.addSectionLabel(styleCard, R.string.demo_image_fill)
        styleCard.addToRemote(helper.createImage().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(80)
            setImageFileName(SAMPLE_IMAGE_FILE)
            setScaleType(ScaleType.CENTER_CROP)
            setImageTint(COLOR_TINT)
            setBackgroundShape(ShapeDescriptor.Builder().cornerRadius(DrawingContext.CORNER_DEFAULT).build())
        }, ctx.marginParam(6, 2, 6, 4))

        // 颜色描边图片（stroke 2dp #FFCC00）
        ctx.addDivider(styleCard)
        ctx.addSectionLabel(styleCard, R.string.demo_image_stroke)
        styleCard.addToRemote(helper.createImage().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(80)
            setImageFileName(SAMPLE_IMAGE_FILE)
            setScaleType(ScaleType.CENTER_CROP)
            setBackgroundShape(
                ShapeDescriptor.Builder()
                    .cornerRadius(8f)
                    .stroke(3, COLOR_STROKE)
                    .build()
            )
        }, ctx.marginParam(6, 2, 6, 4))

    }
}
