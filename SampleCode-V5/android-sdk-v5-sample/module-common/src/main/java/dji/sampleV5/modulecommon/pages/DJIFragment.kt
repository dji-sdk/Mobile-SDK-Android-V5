package dji.sampleV5.modulecommon.pages

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import dji.sampleV5.modulecommon.R
import dji.sampleV5.modulecommon.data.MAIN_FRAGMENT_PAGE_TITLE
import dji.sampleV5.modulecommon.keyvalue.KeyItemActionListener
import dji.sampleV5.modulecommon.keyvalue.KeyValueDialogUtil
import dji.sampleV5.modulecommon.models.MSDKInfoVm
import dji.sampleV5.modulecommon.util.wheel.PopupNumberPicker
import dji.sampleV5.modulecommon.util.wheel.PopupNumberPickerDouble
import dji.sampleV5.modulecommon.util.wheel.PopupNumberPickerPosition
import dji.v5.utils.common.LogUtils
import dji.v5.utils.common.StringUtils
import java.util.*

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/5/12
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class DJIFragment : Fragment() {

    protected val logTag = LogUtils.getTag(this)
    protected var mainHandler = Handler(Looper.getMainLooper())
    protected var indexChosen = intArrayOf(-1, -1, -1)

    protected val msdkInfoVm: MSDKInfoVm by activityViewModels()


    open fun updateTitle() {
        arguments?.let {
            val title = it.getInt(MAIN_FRAGMENT_PAGE_TITLE, R.string.testing_tools)
            msdkInfoVm.mainTitle.value = StringUtils.getResStr(context, title)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        updateTitle()
    }

    fun initPopupNumberPicker(list: ArrayList<String>, r: Runnable) {
        var popupNumberPicker: PopupNumberPicker? = null
        val position = PopupNumberPickerPosition(250, 200, 0)
        popupNumberPicker = PopupNumberPicker(
            context, list,
            { pos1, _ ->
                popupNumberPicker?.dismiss()
                popupNumberPicker = null
                indexChosen[0] = pos1
                mainHandler.post(r)
            }, position
        )
        popupNumberPicker?.showAtLocation(view, Gravity.CENTER, 0, 0)
    }

    fun initPopupNumberPicker(list1: ArrayList<String>, list2: ArrayList<String>, r: Runnable) {
        var mPopupDoubleNumberPicker: PopupNumberPickerDouble? = null
        val position = PopupNumberPickerPosition(500, 200, 0)
        mPopupDoubleNumberPicker = PopupNumberPickerDouble(
            context, list1, list2,
            { pos1, pos2 ->
                mPopupDoubleNumberPicker?.dismiss()
                indexChosen[0] = pos1
                indexChosen[1] = pos2
                mainHandler.post(r)
            }, position
        )
        mPopupDoubleNumberPicker.showAtLocation(view, Gravity.CENTER, 0, 0)
    }

    fun resetIndex() {
        indexChosen = intArrayOf(-1, -1, -1)
    }

    fun isFragmentShow(): Boolean {
        return lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)
    }

    fun openInputDialog(text: String, title: String, onStrInput: (str: String) -> Unit) {
        val inputServer = EditText(context)
        inputServer.setText(text)
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title).setView(inputServer).setNegativeButton("CANCEL", null)
        builder.setPositiveButton(
            "OK"
        ) { _, _ -> onStrInput(inputServer.text.toString()) }
        builder.show()
    }

    fun showDialog(title: String, msg: String = "", callback: KeyItemActionListener<String>) {
        KeyValueDialogUtil.showInputDialog(
            activity, title, msg, "", true
        ) {
            callback.actionChange(it)
        }
    }

}