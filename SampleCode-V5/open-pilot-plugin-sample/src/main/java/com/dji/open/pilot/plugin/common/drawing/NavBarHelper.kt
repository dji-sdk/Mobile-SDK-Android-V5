package com.dji.open.pilot.plugin.common.drawing

import android.os.Handler
import android.os.Looper
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
 * 通用顶部切换导航栏：[左箭头] [标题 居中] [右箭头]。
 * 点击箭头触发 onPrev/onNext 回调，由调用方处理切换逻辑。
 */
object NavBarHelper {

    // oneway AIDL 点击来自 Binder 线程池，统一排队，避免交错更新容器可见性。
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 绘制切换栏，返回 title 组件（RemoteText），供后续 setTextByResId 切换文案。
     * 标题走 setTextByResId 进入 resIdMap，支持服务端语言切换实时刷新。
     *
     * @param ctx DrawingContext
     * @param root 父容器
     * @param titleResId 初始标题文案资源 ID
     * @param onPrev 左箭头点击回调
     * @param onNext 右箭头点击回调
     * @return title 组件（RemoteText）
     */
    fun draw(
        ctx: DrawingContext,
        root: RemoteLinearLayout,
        titleResId: Int,
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
            setTextByResId(titleResId)
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

        leftArrow.setOnClickListener { mainHandler.post { onPrev() } }
        rightArrow.setOnClickListener { mainHandler.post { onNext() } }

        return title
    }
}
