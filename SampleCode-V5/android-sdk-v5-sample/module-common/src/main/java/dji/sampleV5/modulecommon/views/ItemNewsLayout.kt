package dji.sampleV5.modulecommon.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import dji.sampleV5.modulecommon.R
import kotlinx.android.synthetic.main.item_news.view.*

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/2/15
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class ItemNewsLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        initView(context)
    }

    private fun initView(context: Context) {
        View.inflate(context, R.layout.item_news, this)
    }

    fun setTitle(title : String){
        item_title.text = title
    }

    fun setDate(date : String){
        item_date.text = date
    }

    fun setDescription(description : String){
        item_description.text = description
    }

    fun showAlert(isShow: Boolean) {
        if (isShow) {
            view_alert.visibility = View.VISIBLE
        } else {
            view_alert.visibility = View.GONE
        }
    }
}