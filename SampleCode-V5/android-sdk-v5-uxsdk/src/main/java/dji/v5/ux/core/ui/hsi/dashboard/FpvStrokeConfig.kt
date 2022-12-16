package dji.v5.ux.core.ui.hsi.dashboard

import android.content.Context
import android.graphics.Color

/**
 * Pilot UI 2.0 在 FPV 绘制中，需要添加描边
 * 描边不同级别属性在此类中统一配置，方便统一修改
 */
class FpvStrokeConfig(
        context: Context,
        boldWidth: Float,
        thinWidth: Float,
        val strokeDeepColor: Int,
        val strokeShallowColor: Int
) {

    constructor(context: Context, boldWidth: Float, thinWidth: Float)
            : this(context, boldWidth, thinWidth, Color.argb(0x99, 0, 0, 0), 0x4D000000)

    constructor(context: Context) : this(context, 0.5f, 0.2f)

    val strokeBoldWidth: Float
    val strokeThinWidth: Float

    init {
        val density = context.resources.displayMetrics.density
        strokeBoldWidth = boldWidth * density
        strokeThinWidth = thinWidth * density
    }
}