package com.dji.industry.pandora.pilot2.uikit.popover

import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable

class TintedBitmapDrawable(res: Resources?, bitmap: Bitmap?, private var tint: Int) : BitmapDrawable(res, bitmap) {
    private var tintAlpha = 0

    init {
        tintAlpha = Color.alpha(tint)
    }

    override fun setTint(tint: Int) {
        this.tint = tint
        tintAlpha = Color.alpha(tint)
    }

    override fun draw(canvas: Canvas) {
        val paint = paint
        if (paint.colorFilter == null) {
            paint.colorFilter = LightingColorFilter(tint, 0)
            paint.alpha = tintAlpha
        }
        super.draw(canvas)
    }
}