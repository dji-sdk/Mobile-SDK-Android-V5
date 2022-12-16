package dji.v5.ux.core.popover

import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import dji.v5.ux.R
import dji.v5.utils.common.AndUtil


object PopoverHelper {

    fun showPopover(
        anchor: View,
        @LayoutRes layoutRes: Int,
        width: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
        height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    ): Popover {
        val popover = baseBuilder(anchor)
            .customView(layoutRes)
            .size(width, height)
            .build()
        popover.show()
        return popover
    }

    fun showPopover(
        anchor: View,
        layout: View,
        width: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
        height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    ): Popover {
        val popover = baseBuilder(anchor)
            .customView(layout)
            .size(width, height)
            .build()
        popover.show()
        return popover
    }

    fun baseBuilder(anchor: View): Popover.Builder {
        return Popover.Builder(anchor)
            .arrowColor(AndUtil.getResColor(R.color.uxsdk_fpv_popover_title_background_color))
            .yOffset(AndUtil.getDimension(R.dimen.uxsdk_2_dp).toInt())
            .backgroundColor(AndUtil.getResColor(R.color.uxsdk_white_0_percent))
            .allScreenMargin(AndUtil.getDimension(R.dimen.uxsdk_32_dp).toInt())
            .enableShadow(false)
    }
}