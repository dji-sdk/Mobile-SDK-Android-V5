package com.dji.open.pilot.plugin.common.widget

import android.graphics.Outline
import android.graphics.RectF
import android.util.TypedValue
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import kotlin.math.roundToInt

/**
 * 按 [ImageView] 内图片（fitCenter 后）的实际显示矩形裁圆角的 OutlineProvider。
 *
 * 保证图片不被裁剪、不改变比例，仅四角圆角。适用于预览图（单图 / 轮播）等场景，
 * 圆角由 View outline 裁切保证，不依赖 background 描边是否可见。
 *
 * @param cornerRadiusDp 圆角半径（dp），默认 8dp。
 */
class ImageContentRoundOutline(
    private val cornerRadiusDp: Float = DEFAULT_CORNER_RADIUS_DP
) : ViewOutlineProvider() {

    private val displayRect = RectF()

    override fun getOutline(view: View, outline: Outline) {
        val radius = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, cornerRadiusDp, view.resources.displayMetrics
        )
        val imageView = view as? ImageView
        val drawable = imageView?.drawable
        if (drawable == null) {
            outline.setRoundRect(0, 0, view.width, view.height, radius)
            return
        }
        displayRect.set(
            0f, 0f,
            drawable.intrinsicWidth.toFloat(),
            drawable.intrinsicHeight.toFloat()
        )
        imageView.imageMatrix.mapRect(displayRect)
        displayRect.offset(imageView.paddingLeft.toFloat(), imageView.paddingTop.toFloat())
        outline.setRoundRect(
            displayRect.left.roundToInt(),
            displayRect.top.roundToInt(),
            displayRect.right.roundToInt(),
            displayRect.bottom.roundToInt(),
            radius
        )
    }

    companion object {
        const val DEFAULT_CORNER_RADIUS_DP = 8f
    }
}
