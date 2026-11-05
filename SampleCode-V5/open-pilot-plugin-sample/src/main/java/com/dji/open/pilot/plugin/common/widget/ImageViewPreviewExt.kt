package com.dji.open.pilot.plugin.common.widget

import android.widget.ImageView
import com.dji.open.pilot.plugin.R

/**
 * 预览图 ImageView 扩展：一次性配置「按图片实际显示区域裁圆角 + 可选浅色描边」。
 *
 * 圆角由 [ImageContentRoundOutline] 裁切保证（真裁图片内容，不依赖描边可见性）；
 * 描边为可选叠加边框，随圆角一同裁切。
 *
 * 调用后，图片资源变化时会自动在布局稳定后重算圆角 outline。
 *
 * @param cornerRadiusDp 圆角半径（dp），默认 8dp。
 * @param showBorder 是否显示浅色描边边框，默认 true。
 */
fun ImageView.applyRoundedPreview(
    cornerRadiusDp: Float = ImageContentRoundOutline.DEFAULT_CORNER_RADIUS_DP,
    showBorder: Boolean = true
) {
    clipToOutline = true
    outlineProvider = ImageContentRoundOutline(cornerRadiusDp)
    // 描边作为叠加前景，随 outline 一同裁成圆角；不参与圆角裁切本身
    foreground = if (showBorder) {
        androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_preview_image)
    } else {
        null
    }
    // 尺寸变化后 imageMatrix 才就绪，重算圆角 outline
    addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
        if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
            v.invalidateOutline()
        }
    }
}
