package dji.v5.ux.core.extension

import android.content.Context
import android.graphics.Point
import android.util.Size
import android.view.WindowManager

/**
 * 获取横屏的屏幕
 */
fun Context.getLandScreenSize(): Size {
    val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

    var screenWidth: Int
    var screenHeight: Int

    val display = windowManager.defaultDisplay
    val outSize = Point()
    display.getRealSize(outSize)
    screenWidth = if (outSize.x > outSize.y) outSize.x else outSize.y
    screenHeight = if (outSize.x > outSize.y) outSize.y else outSize.x

    return Size(screenWidth, screenHeight)
}