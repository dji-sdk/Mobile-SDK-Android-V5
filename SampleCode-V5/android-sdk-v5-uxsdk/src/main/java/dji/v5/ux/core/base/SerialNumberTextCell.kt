package dji.v5.ux.core.base

import android.content.Context
import android.util.AttributeSet


class SerialNumberTextCell(context: Context?, attrs: AttributeSet?) : TextCell(context, attrs) {
    init {
        mTitle.isSingleLine = false
        mTitle.layoutParams.width = 0
        mContent.layoutParams.width = LayoutParams.WRAP_CONTENT
    }
}