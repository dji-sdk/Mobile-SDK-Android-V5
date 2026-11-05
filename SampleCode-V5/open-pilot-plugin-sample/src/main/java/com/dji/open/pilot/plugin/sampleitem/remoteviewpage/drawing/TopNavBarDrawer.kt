package com.dji.open.pilot.plugin.sampleitem.remoteviewpage.drawing

import com.dji.open.pilot.plugin.common.drawing.DrawingContext

import com.dji.open.pilot.plugin.sampleitem.remoteviewpage.ComponentType
import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.FontStyleType
import dji.open.pilot.base.ui.model.GravityType
import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.base.ui.model.ScaleType
import dji.open.pilot.base.ui.model.ShapeDescriptor
import dji.v5.ux.remoteview.RemoteLinearLayout
import dji.v5.ux.remoteview.RemoteText
import dji.v5.ux.util.RemoteViewColors

/**
 * 顶部切换导航栏：[左箭头 ImageView] [当前组件名 TextView 居中] [右箭头 ImageView]。
 * 点击箭头按 ComponentType 枚举顺序循环切换，触发 onSwitch 回调。
 */
object TopNavBarDrawer {

    /**
     * 共用 NavBar 绘制入口（仅绘制一次）。
     * 返回 title 组件（RemoteText），供后续 setTextByResId 动态改文案（走 resIdMap，支持语言切换刷新）。
     * 箭头点击直接调用 onPrev/onNext 回调，由 ViewModel 根据当前实际显示类型计算目标。
     */
    fun drawShared(
        ctx: DrawingContext,
        root: RemoteLinearLayout,
        initialType: ComponentType,
        onPrev: () -> Unit,
        onNext: () -> Unit
    ): RemoteText {
        val helper = ctx.helper
        val navBar = helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        root.addToRemote(navBar, ctx.marginParam(2))

        // 左箭头
        val leftArrow = helper.createImage().apply {
            setWidth(28); setHeight(28)
            setImageFileName(DrawingContext.ICON_FRONT_ARROW)
            setPadding(6, 6, 6, 6)
            setScaleType(ScaleType.FIT_CENTER)
            setBackgroundShape(
                ShapeDescriptor.Builder()
                    .fillColor(RemoteViewColors.WHITE_08)
                    .cornerRadius(4f)
                    .build()
            )
        }
        navBar.addToRemote(leftArrow, ctx.marginParam(4))

        // 中间标题
        val title = helper.createText().apply {
            setTextByResId(initialType.displayNameRes)
            setWidth(0); setHeight(RemoteView.MATCH_PARENT)
            setFontStyle(FontStyleType.BOLD)
        }
        navBar.addToRemote(title, ctx.weightParam(1f))
        helper.updateView(title.getId(), RemoteViewProperty.GRAVITY, GravityType.CENTER.value.toString())

        // 右箭头
        val rightArrow = helper.createImage().apply {
            setWidth(28); setHeight(28)
            setImageFileName(DrawingContext.ICON_NEXT_ARROW)
            setPadding(6, 6, 6, 6)
            setScaleType(ScaleType.FIT_CENTER)
            setBackgroundShape(
                ShapeDescriptor.Builder()
                    .fillColor(RemoteViewColors.WHITE_08)
                    .cornerRadius(4f)
                    .build()
            )
        }
        navBar.addToRemote(rightArrow, ctx.marginParam(4))

        // 点击切换：由 ViewModel 的 onPrev/onNext 回调处理实际逻辑
        leftArrow.setOnClickListener { onPrev() }
        rightArrow.setOnClickListener { onNext() }

        return title
    }

    fun draw(
        ctx: DrawingContext,
        root: RemoteLinearLayout,
        currentType: ComponentType,
        onSwitch: (ComponentType, Boolean) -> Unit
    ) {
        val helper = ctx.helper
        val navBar = helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        root.addToRemote(navBar, ctx.marginParam(2))

        // 左箭头
        val leftArrow = helper.createImage().apply {
            setWidth(28); setHeight(28)
            setImageFileName(DrawingContext.ICON_FRONT_ARROW)
            setPadding(6, 6, 6, 6)
            setScaleType(ScaleType.FIT_CENTER)
            setBackgroundShape(
                ShapeDescriptor.Builder()
                    .fillColor(RemoteViewColors.WHITE_08)
                    .cornerRadius(4f)
                    .build()
            )
        }
        navBar.addToRemote(leftArrow, ctx.marginParam(4))

        // 中间标题
        val title = helper.createText().apply {
            setTextByResId(currentType.displayNameRes)
            setWidth(0); setHeight(RemoteView.MATCH_PARENT)
            setFontStyle(FontStyleType.BOLD)
        }
        navBar.addToRemote(title, ctx.weightParam(1f))
        helper.updateView(title.getId(), RemoteViewProperty.GRAVITY, GravityType.CENTER.value.toString())

        // 右箭头
        val rightArrow = helper.createImage().apply {
            setWidth(28); setHeight(28)
            setImageFileName(DrawingContext.ICON_NEXT_ARROW)
            setPadding(6, 6, 6, 6)
            setScaleType(ScaleType.FIT_CENTER)
            setBackgroundShape(
                ShapeDescriptor.Builder()
                    .fillColor(RemoteViewColors.WHITE_08)
                    .cornerRadius(4f)
                    .build()
            )
        }
        navBar.addToRemote(rightArrow, ctx.marginParam(4))

        // 点击切换：循环遍历 ComponentType 枚举
        val values = ComponentType.values()
        leftArrow.setOnClickListener {
            val prev = values[(currentType.ordinal - 1 + values.size) % values.size]
            onSwitch(prev, false)
        }
        rightArrow.setOnClickListener {
            val next = values[(currentType.ordinal + 1) % values.size]
            onSwitch(next, false)
        }
    }
}
