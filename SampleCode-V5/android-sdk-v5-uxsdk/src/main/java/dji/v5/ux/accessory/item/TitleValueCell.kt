package dji.v5.ux.accessory.item

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.TextView
import dji.v5.ux.R


/**
 * Description :垂直显示标题和值的控件
 *
 * @author: Byte.Cai
 *  date : 2022/9/7
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */

class TitleValueCell @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private var tvTitle: TextView
    private var tvValue: TextView


    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.FpvTitleValueCell, defStyleAttr, 0)
        val title = ta.getString(R.styleable.FpvTitleValueCell_uxsdk_title)
        val value = ta.getString(R.styleable.FpvTitleValueCell_uxsdk_value)
        val layoutId = ta.getResourceId(R.styleable.FpvTitleValueCell_uxsdk_layout, R.layout.uxsdk_fpv_top_bar_popover_title_value_cell)
        if (layoutId == 0) {
            throw IllegalArgumentException("layout can not be null.")
        }
        inflate(context, layoutId, this)

        ta.recycle()

        tvTitle = findViewById(R.id.tv_title)
        tvValue = findViewById(R.id.tv_value)
        tvTitle.text = title
        tvValue.text = value
    }

    var title: CharSequence?
        get() = tvTitle.text
        set(value) {
            if (value != tvTitle.text) {
                tvTitle.text = value
            }
        }
    var value: CharSequence?
        get() = tvValue.text
        set(value) {
            if (value != tvValue.text) {
                tvValue.text = value
            }
        }

    fun setValueTextColor(color: Int) {
        tvValue.setTextColor(color)
    }

    fun setTitleTextColor(color: Int) {
        tvTitle.setTextColor(color)
    }

}