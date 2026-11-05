package dji.v5.ux.util

import android.graphics.Color
import androidx.annotation.ColorInt

/**
 * RemoteView 体系中统一使用的颜色常量（sRGB，ARGB 格式）。
 * 可直接传入 setBackground / setTextColor 等接口。
 *
 * 设计原则：
 * - Layer 1（基础色板）：纯色定义，命名描述颜色本身
 * - Layer 2（透明度变体）：在 Layer 1 基础上叠加透明度，命名为 颜色名_透明度百分比
 * - 不含任何业务语义，由调用方决定颜色用途
 */
object RemoteViewColors {

    // ═══ Layer 1: 基础色板（纯色，不带透明度）═══

    @ColorInt val WHITE          = Color.parseColor("#FFFFFFFF")
    @ColorInt val BLACK          = Color.parseColor("#FF000000")
    @ColorInt val DARK_GREY      = Color.parseColor("#FF373A41")
    @ColorInt val LIGHT_GREY     = Color.parseColor("#FFE7E7E7")
    @ColorInt val NEAR_WHITE     = Color.parseColor("#FFFAFAFA")
    @ColorInt val BLUE_PRIMARY   = Color.parseColor("#FF2696FF")
    @ColorInt val BLUE_LIGHT     = Color.parseColor("#FFF2F3FF")
    @ColorInt val BLUE_MEDIUM    = Color.parseColor("#FF4478B4")
    @ColorInt val RED_PRIMARY    = Color.parseColor("#FFD54941")
    @ColorInt val GREEN_PRIMARY  = Color.parseColor("#FF00EE8B")
    @ColorInt val ORANGE_PRIMARY = Color.parseColor("#FFFF8100")

    // ═══ Layer 2: 带透明度变体（颜色名_透明度百分比）═══

    @ColorInt val WHITE_60       = Color.parseColor("#99FFFFFF")
    @ColorInt val WHITE_33       = Color.parseColor("#55FFFFFF")
    @ColorInt val WHITE_20       = Color.parseColor("#33FFFFFF")
    @ColorInt val WHITE_08       = Color.parseColor("#14FFFFFF")
    @ColorInt val BLACK_60       = Color.parseColor("#99000000")
    @ColorInt val LIGHT_GREY_53  = Color.parseColor("#88E7E7E7")
    @ColorInt val NEAR_WHITE_20  = Color.parseColor("#33FAFAFA")
    @ColorInt val RED_PRIMARY_33 = Color.parseColor("#55D54941")
    @ColorInt val RED_PRIMARY_08 = Color.parseColor("#14D54941")
    @ColorInt val BLUE_PRIMARY_33 = Color.parseColor("#552696FF")
    @ColorInt val GREEN_PRIMARY_33 = Color.parseColor("#5500EE8B")
    @ColorInt val BLUE_DEEP      = Color.parseColor("#FF1565C0")
    @ColorInt val GREEN_DEEP     = Color.parseColor("#FF2E7D32")
}
