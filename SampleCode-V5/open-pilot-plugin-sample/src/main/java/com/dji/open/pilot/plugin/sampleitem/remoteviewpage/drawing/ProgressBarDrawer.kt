package com.dji.open.pilot.plugin.sampleitem.remoteviewpage.drawing

import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.drawing.DrawingContext

import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.ProgressStatusType
import dji.v5.ux.remoteview.RemoteLinearLayout
import dji.v5.ux.util.RemoteViewColors

/**
 * ProgressBar 组件演示：水平进度条、圆形进度条（进度/颜色/文案/状态）。
 */
object ProgressBarDrawer {

    fun draw(ctx: DrawingContext, root: RemoteLinearLayout) {
        drawCard01Type(ctx, root)
        drawCard02Status(ctx, root)
        drawCard03Style(ctx, root)
    }

    private fun drawCard01Type(ctx: DrawingContext, root: RemoteLinearLayout) {
        ctx.addCardTitle(root, R.string.demo_card_type)
        val card = ctx.card(root)

        ctx.addSectionLabel(card, R.string.demo_progress_horizontal)
        listOf(
            33 to RemoteViewColors.BLUE_PRIMARY,
            66 to RemoteViewColors.BLUE_PRIMARY,
            100 to RemoteViewColors.BLUE_PRIMARY
        ).forEach { (progress, color) ->
            card.addToRemote(ctx.helper.createProgressBar().apply {
                setWidth(RemoteView.MATCH_PARENT)
                setHeight(12)
                setMax(100)
                setProgress(progress)
                setProgressColor(color)
            }, ctx.marginParam(6, 4, 6, 4))
        }

        ctx.addDivider(card)
        ctx.addSectionLabel(card, R.string.demo_progress_circle)
        val horizontalContainer = ctx.helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        card.addToRemote(horizontalContainer, ctx.marginParam(6, 2, 6, 2))

        listOf(33, 66, 100).forEach { progress ->
            val circleProgress = ctx.helper.createCircleProgressBar().apply {
                setWidth(30)
                setHeight(30)
                setProgress(progress)
                setMax(100)
                setText("45%") // 根据设计要求固定文字为 "45%"
                setTextSize(10f)
            }
            horizontalContainer.addToRemote(circleProgress, ctx.marginParam(4))
        }
    }

    private fun drawCard02Status(ctx: DrawingContext, root: RemoteLinearLayout) {
        ctx.addCardTitle(root, R.string.demo_card_state)
        val card = ctx.card(root)

        ctx.addSectionLabel(card, R.string.demo_progress_circle_state)
        val horizontalContainer = ctx.helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        card.addToRemote(horizontalContainer, ctx.marginParam(6, 2, 6, 2))

        val circleProgressFail = ctx.helper.createCircleProgressBar().apply {
            setWidth(30)
            setHeight(30)
            setProgress(25)
            setMax(100)
            setStatus(ProgressStatusType.FAIL)
        }
        horizontalContainer.addToRemote(circleProgressFail, ctx.marginParam(4))

        val cpNormal = ctx.helper.createCircleProgressBar().apply {
            setWidth(30)
            setHeight(30)
            setProgress(45)
            setMax(100)
            setText("45%")
            setTextSize(10f)
            setStatus(ProgressStatusType.NORMAL)
        }
        horizontalContainer.addToRemote(cpNormal, ctx.marginParam(4))

        val cpSuccess = ctx.helper.createCircleProgressBar().apply {
            setWidth(30)
            setHeight(30)
            setProgress(100)
            setMax(100)
            setStatus(ProgressStatusType.SUCCESS)
        }
        horizontalContainer.addToRemote(cpSuccess, ctx.marginParam(4))
    }

    private fun drawCard03Style(ctx: DrawingContext, root: RemoteLinearLayout) {
        ctx.addCardTitle(root, R.string.demo_card_style)
        val card = ctx.card(root)

        // Section 1: 水平进度条大小
        ctx.addSectionLabel(card, R.string.demo_progress_horizontal_size)
        listOf(
            Pair(33, 4),
            Pair(66, 8),
            Pair(100, 12)
        ).forEach { (progress, height) ->
            card.addToRemote(ctx.helper.createProgressBar().apply {
                setWidth(RemoteView.MATCH_PARENT)
                setHeight(height)
                setMax(100)
                setProgress(progress)
                setProgressColor(RemoteViewColors.BLUE_PRIMARY)
            }, ctx.marginParam(6, 4, 6, 4))
        }

        ctx.addDivider(card)
        // Section 2: 圆形进度条大小
        ctx.addSectionLabel(card, R.string.demo_progress_circle_size)
        val sizeContainer = ctx.helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        card.addToRemote(sizeContainer, ctx.marginParam(6, 2, 6, 2))

        data class CircleSizeParams(val size: Int, val progress: Int, val text: String, val textSize: Float)
        listOf(
            CircleSizeParams(20, 33, "33%", 8f),
            CircleSizeParams(25, 66, "66%", 9f),
            CircleSizeParams(30, 100, "100%", 10f)
        ).forEach { params ->
            val circleProgress = ctx.helper.createCircleProgressBar().apply {
                setWidth(params.size)
                setHeight(params.size)
                setProgress(params.progress)
                setMax(100)
                setText(params.text)
                setTextSize(params.textSize)
                setProgressColor(RemoteViewColors.BLUE_PRIMARY)
            }
            sizeContainer.addToRemote(circleProgress, ctx.marginParam(4))
        }

        ctx.addDivider(card)
        // Section 3: 水平进度条颜色
        ctx.addSectionLabel(card, R.string.demo_progress_horizontal_color)
        listOf(
            33 to RemoteViewColors.BLUE_PRIMARY,
            66 to RemoteViewColors.ORANGE_PRIMARY,
            88 to RemoteViewColors.RED_PRIMARY
        ).forEach { (progress, color) ->
            card.addToRemote(ctx.helper.createProgressBar().apply {
                setWidth(RemoteView.MATCH_PARENT)
                setHeight(12)
                setMax(100)
                setProgress(progress)
                setProgressColor(color)
            }, ctx.marginParam(6, 4, 6, 4))
        }

        ctx.addDivider(card)
        // Section 4: 圆形进度条颜色
        ctx.addSectionLabel(card, R.string.demo_progress_circle_color)
        val colorContainer = ctx.helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        card.addToRemote(colorContainer, ctx.marginParam(6, 2, 6, 2))

        val circleProgressDefault = ctx.helper.createCircleProgressBar().apply {
            setWidth(30)
            setHeight(30)
            setProgress(33)
            setMax(100)
            setText("33%")
            setTextSize(10f)
            // 默认颜色，不调用 setProgressColor
        }
        colorContainer.addToRemote(circleProgressDefault, ctx.marginParam(4))

        val circleProgressOrange = ctx.helper.createCircleProgressBar().apply {
            setWidth(30)
            setHeight(30)
            setProgress(66)
            setMax(100)
            setText("66%")
            setTextSize(10f)
            setProgressColor(RemoteViewColors.ORANGE_PRIMARY)
        }
        colorContainer.addToRemote(circleProgressOrange, ctx.marginParam(4))

        val circleProgressRed = ctx.helper.createCircleProgressBar().apply {
            setWidth(30)
            setHeight(30)
            setProgress(88)
            setMax(100)
            setText("88%")
            setTextSize(10f)
            setProgressColor(RemoteViewColors.RED_PRIMARY)
        }
        colorContainer.addToRemote(circleProgressRed, ctx.marginParam(4))
    }
}
