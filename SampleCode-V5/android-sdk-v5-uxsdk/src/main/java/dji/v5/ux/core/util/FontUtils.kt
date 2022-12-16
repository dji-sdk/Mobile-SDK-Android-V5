package dji.v5.ux.core.util

import android.graphics.Paint
import android.graphics.Rect

/**
 * 在 FPV 页面绘制过程中，使用的字体计算问题高度时部分场景异常，相关计算由此类完成
 */
class FontUtils {
    companion object {
        var calcCache: ThreadLocal<Rect> = ThreadLocal()
        private const val DIGITAL_TEXT = "1.234567890"

        /**
         * 获取文字绘制高度
         * 使用的字体在上下分别有 FontMetrics.bottom 高的空白
         */
        @JvmStatic
        @JvmOverloads
        fun getDigitalTextDrawHeight(paint: Paint, text: String = DIGITAL_TEXT): Float {
            val rect = getCachedRect()
            paint.getTextBounds(text, 0, text.length, rect)
            return rect.height().toFloat()
        }

        @JvmStatic
        @JvmOverloads
        fun getDigitalBaselineFromTop(paint: Paint, top: Float, text: String = DIGITAL_TEXT): Float {
            val rect = getCachedRect()
            paint.getTextBounds(text, 0, text.length, rect)
            return top - rect.top
        }

        @JvmStatic
        @JvmOverloads
        fun getDigitalBaselineFromCenter(paint: Paint, center: Float, text: String = DIGITAL_TEXT): Float {
            val rect = getCachedRect()
            paint.getTextBounds(text, 0, text.length, rect)
            return center + rect.height() / 2f - rect.bottom
        }

        @JvmStatic
        @JvmOverloads
        fun getDigitalBaselineFromBottom(paint: Paint, bottom: Float, text: String = DIGITAL_TEXT): Float {
            val rect = getCachedRect()
            paint.getTextBounds(text, 0, text.length, rect)
            return bottom - rect.bottom
        }

        private fun getCachedRect(): Rect {
            var cache = calcCache.get()
            if (cache == null) {
                cache = Rect()
                calcCache.set(cache)
            }
            return cache
        }
    }
}