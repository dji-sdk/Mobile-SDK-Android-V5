package dji.sampleV5.modulecommon.util

import android.widget.TextView

/**
 * Description :Toast的封装类，主要操作的结果Toast和错误信息的展示
 *
 * @author: Byte.Cai
 *  date : 2022/4/24
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
object ToastHelper {
    fun Boolean.showToast(positiveMsg: String, negativeMsg: String, errorText: TextView? = null, clearErrorText: Boolean = false) {
        //消息提示，如果有设置显示TextView只直接显示不Toast；否则只Toast
        if (errorText == null) {
            if (this) {
                ToastUtils.showToast(positiveMsg)
            } else {
                ToastUtils.showToast(negativeMsg)
            }
        } else {
            errorText.text = if (!this@showToast) {
                negativeMsg
            } else {
                if (clearErrorText) {
                    "null"
                } else {
                    positiveMsg
                }
            }
        }
    }
}