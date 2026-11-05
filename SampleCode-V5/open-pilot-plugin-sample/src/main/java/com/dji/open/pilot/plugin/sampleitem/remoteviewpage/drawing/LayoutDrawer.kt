package com.dji.open.pilot.plugin.sampleitem.remoteviewpage.drawing

import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.drawing.DrawingContext
import com.dji.open.pilot.plugin.main.PluginApplication

import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.GravityType
import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.base.ui.model.ShapeDescriptor
import dji.open.pilot.base.ui.model.ViewSide
import dji.v5.ux.remoteview.RemoteLinearLayout
import dji.v5.ux.util.RemoteViewColors

/**
 * 布局组件演示：水平/垂直线性布局、约束布局、嵌套布局。
 */
object LayoutDrawer {
    private const val COLOR_HEADER = 0xFF4196FF.toInt()  // 标头色块
    private const val COLOR_CONTENT_BACKGROUND = 0xFF0659D9.toInt()  // 内容色块 / 容器背景
    private const val COLOR_CONTENT = 0x26FFFFFF.toInt()  // 内容色块 / 容器背景
    private const val COLOR_BUTTON = 0xFF2696FF.toInt()  // 按钮色块
    private val blockColorPair = Pair(COLOR_BUTTON, RemoteViewColors.WHITE)

    fun draw(ctx: DrawingContext, root: RemoteLinearLayout) {
        val helper = ctx.helper

        // ── 01 组件类型（布局应用示例）─────────────────────────────────────────
        ctx.addCardTitle(root, R.string.demo_card_type)
        val typeCard = ctx.card(root)

        // Section：基础用法 — 标头 + 内容区
        ctx.addSectionLabel(typeCard, R.string.demo_layout_basic)
        val basicContainer = helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(RemoteView.WRAP_CONTENT)
            setBackgroundShape(
                ShapeDescriptor.Builder().fillColor(COLOR_CONTENT_BACKGROUND)
                    .cornerRadius(DrawingContext.CORNER_DEFAULT).build()
            )
        }
        typeCard.addToRemote(basicContainer, ctx.marginParam(6, 2, 6, 4))

        // 标头
        val basicHeader = helper.createText().apply {
            setTextByResId(R.string.demo_layout_linear_h)
            setWidth(RemoteView.MATCH_PARENT); setHeight(36)
            setTextColor(RemoteViewColors.WHITE)
            setBackgroundShape(
                ShapeDescriptor.Builder().fillColor(COLOR_HEADER)
                    .cornerRadii(
                        floatArrayOf(
                            DrawingContext.CORNER_DEFAULT,
                            DrawingContext.CORNER_DEFAULT,
                            DrawingContext.CORNER_DEFAULT,
                            DrawingContext.CORNER_DEFAULT,
                            0f,
                            0f,
                            0f,
                            0f
                        )
                    ).build()
            )
        }
        basicContainer.addToRemote(basicHeader, ctx.marginParam(0))
        helper.updateView(
            basicHeader.getId(),
            RemoteViewProperty.GRAVITY,
            GravityType.CENTER.value.toString()
        )

        val basicLinear = helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(60)
            setBackgroundShape(
                ShapeDescriptor.Builder().fillColor(COLOR_CONTENT_BACKGROUND)
                    .cornerRadius(DrawingContext.CORNER_DEFAULT).build()
            )
        }
        basicContainer.addToRemote(basicLinear, ctx.marginParam(2, 4, 2, 4))
        for (i in 1..3) {
            val tv = helper.createText().apply {
                setTextByResId(R.string.demo_layout_content)
                setWidth(0); setHeight(RemoteView.MATCH_PARENT)
                setGravity(GravityType.CENTER)
                setTextColor(RemoteViewColors.WHITE);
                setBackgroundShape(colorBlock(COLOR_CONTENT))
            }
            basicLinear.addToRemote(tv, ctx.weightParam(1f, 2))
        }

        //线性垂直布局
        ctx.addDivider(typeCard)
        val spacingContainer = helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(RemoteView.WRAP_CONTENT)
            setBackgroundShape(
                ShapeDescriptor.Builder().fillColor(COLOR_CONTENT_BACKGROUND)
                    .cornerRadius(DrawingContext.CORNER_DEFAULT).build()
            )
        }
        typeCard.addToRemote(spacingContainer, ctx.marginParam(6, 2, 6, 4))

        // 标头
        val headerView = helper.createText().apply {
            setTextByResId(R.string.demo_layout_linear_v)
            setWidth(RemoteView.MATCH_PARENT); setHeight(36)
            setTextColor(RemoteViewColors.WHITE)
            setGravity(GravityType.CENTER)
            setBackgroundShape(
                ShapeDescriptor.Builder().fillColor(COLOR_HEADER).cornerRadii(
                    floatArrayOf(
                        DrawingContext.CORNER_DEFAULT,
                        DrawingContext.CORNER_DEFAULT,
                        DrawingContext.CORNER_DEFAULT,
                        DrawingContext.CORNER_DEFAULT,
                        0f,
                        0f,
                        0f,
                        0f
                    )
                ).build()
            )
        }
        spacingContainer.addToRemote(headerView, ctx.marginParam(0, 0, 0, 4))

        // 内容1
        val content1 = helper.createText().apply {
            setTextByResId(R.string.demo_layout_content)
            setWidth(RemoteView.MATCH_PARENT); setHeight(50)
            setTextColor(RemoteViewColors.WHITE)
            setBackgroundShape(
                ShapeDescriptor.Builder().fillColor(COLOR_CONTENT)
                    .cornerRadius(DrawingContext.CORNER_DEFAULT).build()
            )
        }
        spacingContainer.addToRemote(content1, ctx.marginParam(8))
        helper.updateView(
            content1.getId(),
            RemoteViewProperty.GRAVITY,
            GravityType.CENTER.value.toString()
        )

        // 内容2
        val content2 = helper.createText().apply {
            setTextByResId(R.string.demo_layout_content)
            setWidth(RemoteView.MATCH_PARENT); setHeight(50)
            setTextColor(RemoteViewColors.WHITE)
            setBackgroundShape(
                ShapeDescriptor.Builder().fillColor(COLOR_CONTENT)
                    .cornerRadius(DrawingContext.CORNER_DEFAULT).build()
            )
        }
        spacingContainer.addToRemote(content2, ctx.marginParam(8))
        helper.updateView(
            content2.getId(),
            RemoteViewProperty.GRAVITY,
            GravityType.CENTER.value.toString()
        )

        // 按钮
        val btnView = helper.createButton().apply {
            setTextByResId(R.string.demo_layout_button)
            setWidth(RemoteView.MATCH_PARENT); setHeight(36)
            setTextColor(RemoteViewColors.WHITE)
            setBackgroundShape(
                ShapeDescriptor.Builder().fillColor(COLOR_BUTTON)
                    .cornerRadius(DrawingContext.CORNER_DEFAULT).build()
            )
        }
        spacingContainer.addToRemote(btnView, ctx.marginParam(8))

        //相对布局
        ctx.addDivider(typeCard)
        val constraintContainer = helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(RemoteView.WRAP_CONTENT)
            setBackgroundShape(
                ShapeDescriptor.Builder().fillColor(COLOR_CONTENT)
                    .cornerRadius(DrawingContext.CORNER_DEFAULT).build()
            )
        }
        typeCard.addToRemote(constraintContainer, ctx.marginParam(6, 2, 6, 4))

        // 标头
        val headerView2 = helper.createText().apply {
            setTextByResId(R.string.demo_layout_constraint)
            setWidth(RemoteView.MATCH_PARENT); setHeight(36)
            setTextColor(RemoteViewColors.WHITE)
            setGravity(GravityType.CENTER)
            setBackgroundShape(
                ShapeDescriptor.Builder().fillColor(COLOR_HEADER).cornerRadii(
                    floatArrayOf(
                        DrawingContext.CORNER_DEFAULT,
                        DrawingContext.CORNER_DEFAULT,
                        DrawingContext.CORNER_DEFAULT,
                        DrawingContext.CORNER_DEFAULT,
                        0f,
                        0f,
                        0f,
                        0f
                    )
                ).build()
            )
        }
        constraintContainer.addToRemote(headerView2, ctx.marginParam(0))

        val constraintLayout = helper.createConstraintLayout().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(120)
            setBackgroundShape(
                ShapeDescriptor.Builder().fillColor(COLOR_CONTENT_BACKGROUND)
                    .build()
            )
        }
        constraintContainer.addToRemote(constraintLayout, ctx.marginParam(0))
        // 公共工具：白色 1dp 描边（约束布局定位块）
        val whiteStroke =
            ShapeDescriptor.Builder().fillColor(COLOR_CONTENT).cornerRadius(2f).build()

        val topLeftLabel = helper.createText().apply {
            setTextByResId(R.string.demo_layout_tl);
            setWidth(50);
            setHeight(28);
            setGravity(GravityType.CENTER)
            setTextColor(RemoteViewColors.WHITE);
            setBackgroundShape(whiteStroke)
        }
        val topRightLabel = helper.createText().apply {
            setTextByResId(R.string.demo_layout_tr);
            setWidth(50);
            setHeight(28);
            setGravity(GravityType.CENTER)
            setTextColor(RemoteViewColors.WHITE);
            setBackgroundShape(whiteStroke)
        }
        val centerLabel = helper.createText().apply {
            setTextByResId(R.string.demo_layout_center);
            setWidth(50);
            setHeight(28);
            setGravity(GravityType.CENTER)
            setTextColor(RemoteViewColors.WHITE);
            setBackgroundShape(whiteStroke)
        }
        val bottomLeftLabel = helper.createText().apply {
            setTextByResId(R.string.demo_layout_bl);
            setWidth(50);
            setHeight(28);
            setGravity(GravityType.CENTER)
            setTextColor(RemoteViewColors.WHITE);
            setBackgroundShape(whiteStroke)
        }
        val bottomRightLabel = helper.createText().apply {
            setTextByResId(R.string.demo_layout_br);
            setWidth(50);
            setHeight(28);
            setGravity(GravityType.CENTER)
            setTextColor(RemoteViewColors.WHITE);
            setBackgroundShape(whiteStroke)
        }
        listOf(topLeftLabel, topRightLabel, centerLabel, bottomLeftLabel, bottomRightLabel).forEach { btn ->
            constraintLayout.addToRemote(btn)
        }

        val parentId = RemoteView.PARENT_ID
        val constraintLayoutId = constraintLayout.getId()
        helper.setConstraints(constraintLayoutId, topLeftLabel.getId(), parentId, ViewSide.START, ViewSide.START, 8)
        helper.setConstraints(constraintLayoutId, topLeftLabel.getId(), parentId, ViewSide.TOP, ViewSide.TOP, 8)
        helper.setConstraints(constraintLayoutId, topRightLabel.getId(), parentId, ViewSide.END, ViewSide.END, 8)
        helper.setConstraints(constraintLayoutId, topRightLabel.getId(), parentId, ViewSide.TOP, ViewSide.TOP, 8)
        helper.setConstraints(constraintLayoutId, centerLabel.getId(), parentId, ViewSide.START, ViewSide.START, 0)
        helper.setConstraints(constraintLayoutId, centerLabel.getId(), parentId, ViewSide.END, ViewSide.END, 0)
        helper.setConstraints(constraintLayoutId, centerLabel.getId(), parentId, ViewSide.TOP, ViewSide.TOP, 0)
        helper.setConstraints(constraintLayoutId, centerLabel.getId(), parentId, ViewSide.BOTTOM, ViewSide.BOTTOM, 0)
        helper.setConstraints(constraintLayoutId, bottomLeftLabel.getId(), parentId, ViewSide.START, ViewSide.START, 8)
        helper.setConstraints(constraintLayoutId, bottomLeftLabel.getId(), parentId, ViewSide.BOTTOM, ViewSide.BOTTOM, 8)
        helper.setConstraints(constraintLayoutId, bottomRightLabel.getId(), parentId, ViewSide.END, ViewSide.END, 8)
        helper.setConstraints(constraintLayoutId, bottomRightLabel.getId(), parentId, ViewSide.BOTTOM, ViewSide.BOTTOM, 8)

        // ── 03 组件样式 ──────────────────────────────────────────────────────
        ctx.addCardTitle(root, R.string.demo_card_style_02)
        val styleCard = ctx.card(root)

        // Weight 不等分
        ctx.addSectionLabel(styleCard, R.string.demo_layout_weight)
        val weightRow = helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(30)
        }
        styleCard.addToRemote(weightRow, ctx.marginParam(4, 2, 4, 2))

        for (i in 1..3) {
            val tv = helper.createText().apply {
                setText("${i}")
                setWidth(0);
                setHeight(RemoteView.MATCH_PARENT)
                setGravity(GravityType.CENTER)
                setTextColor(RemoteViewColors.WHITE);
                setBackgroundShape(colorBlock(RemoteViewColors.BLUE_PRIMARY))
            }
            weightRow.addToRemote(tv, ctx.weightParam((i).toFloat(), 2))
        }

        // 定宽 + 间距
        ctx.addDivider(styleCard)
        ctx.addSectionLabel(styleCard, R.string.demo_layout_fixed)
        val fixedWidthRow = helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(30)
        }
        styleCard.addToRemote(fixedWidthRow, ctx.marginParam(4, 2, 4, 2))
        listOf(
            Triple("30dp", 30, blockColorPair),
            Triple("50dp", 50, blockColorPair),
            Triple("40dp", 40, blockColorPair)
        )
            .forEach { (text, width, colors) ->
                val tv = helper.createText().apply {
                    setText(text)
                    setWidth(width); setHeight(RemoteView.MATCH_PARENT)
                    setTextColor(colors.second); setBackgroundShape(colorBlock(RemoteViewColors.BLUE_PRIMARY))
                }
                fixedWidthRow.addToRemote(tv, ctx.marginParam(2))
                helper.updateView(
                    tv.getId(),
                    RemoteViewProperty.GRAVITY,
                    GravityType.CENTER.value.toString()
                )
            }

        // 嵌套布局
        ctx.addDivider(styleCard)
        ctx.addSectionLabel(styleCard, R.string.demo_layout_nested)
        val nestedRow = helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(100)
            setPadding(1,1,1,1)
            setBackgroundShape(colorBlockWithoutFillColor(1, COLOR_CONTENT_BACKGROUND))
        }
        styleCard.addToRemote(nestedRow, ctx.marginParam(4, 2, 4, 2))

        val nestedColumn1 = helper.createLinearLayoutVertical().apply {
            setWidth(0); setHeight(RemoteView.MATCH_PARENT);
            setPadding(1,1,1,1)
            setBackgroundShape(
                colorBlockWithoutFillColor(1, RemoteViewColors.BLUE_PRIMARY)
        )
        }
        nestedRow.addToRemote(nestedColumn1, ctx.weightParam(1f, 3))
        nestedColumn1.addToRemote(helper.createText().apply {
            setWidth(RemoteView.MATCH_PARENT); setHeight(0)
            setBackgroundShape(colorBlock(COLOR_CONTENT))
        }, ctx.weightParam(1f, 2))

        val nestedColumn2 = helper.createLinearLayoutVertical().apply {
            setWidth(0); setHeight(RemoteView.MATCH_PARENT);
            setPadding(1,1,1,1)
            setBackgroundShape(
                colorBlockWithoutFillColor(1, RemoteViewColors.BLUE_PRIMARY)
            )
        }
        nestedRow.addToRemote(nestedColumn2, ctx.weightParam(1f, 3))
        listOf(1f, 1f).forEach { weight ->
            nestedColumn2.addToRemote(helper.createText().apply {
                setWidth(RemoteView.MATCH_PARENT); setHeight(0)
                setBackgroundShape(colorBlock(COLOR_CONTENT))
            }, ctx.weightParam(weight, 2))
        }

        val nestedColumn3 = helper.createLinearLayoutVertical().apply {
            setWidth(0); setHeight(RemoteView.MATCH_PARENT);
            setPadding(1,1,1,1)
            setBackgroundShape(
                colorBlockWithoutFillColor(1, RemoteViewColors.BLUE_PRIMARY)
        )
        }
        nestedRow.addToRemote(nestedColumn3, ctx.weightParam(1f, 3))
        listOf(1f, 2f, 3f).forEach { weight ->
            nestedColumn3.addToRemote(helper.createText().apply {
                setWidth(RemoteView.MATCH_PARENT); setHeight(0)
                setBackgroundShape(colorBlock(COLOR_CONTENT))
            }, ctx.weightParam(weight, 2))
        }

        val nestedColumn4 = helper.createLinearLayoutVertical().apply {
            setWidth(0); setHeight(RemoteView.MATCH_PARENT);
            setPadding(1,1,1,1)
            setBackgroundShape(
                colorBlockWithoutFillColor(1, RemoteViewColors.BLUE_PRIMARY)
        )
        }
        nestedRow.addToRemote(nestedColumn4, ctx.weightParam(1f, 3))
        listOf(1f, 2f, 3f,4f).forEach { weight ->
            nestedColumn4.addToRemote(helper.createText().apply {
                setWidth(RemoteView.MATCH_PARENT);
                setHeight(0)
                setBackgroundShape(colorBlock(COLOR_CONTENT))
            }, ctx.weightParam(weight, 2))
        }

    }

    // 公共工具：带圆角填充的色块背景
    private fun colorBlock(color: Int): ShapeDescriptor =
        ShapeDescriptor.Builder().fillColor(color).cornerRadius(2f).build()

    fun colorBlockWithoutFillColor(widthDp: Int, strokeColor: Int): ShapeDescriptor =
        ShapeDescriptor.Builder().stroke(widthDp, strokeColor).cornerRadius(2f).build()
}
