package dji.v5.ux.core.widget.hsi

import android.content.Context
import android.util.AttributeSet
import android.view.View
import dji.v5.ux.R

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/12/2
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
class SpeedDisplayFpvWidget @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : SpeedDisplayWidget(ctx, attrs, defStyleAttr) {

    override fun loadLayout(context: Context) {
        View.inflate(context, R.layout.uxsdk_fpv_pfd_speed_display_widget, this)
    }

}