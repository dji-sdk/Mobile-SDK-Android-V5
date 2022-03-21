package dji.sampleV5.moduleaircraft.pages

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.sampleV5.moduleaircraft.R
import dji.sampleV5.moduleaircraft.models.MegaphoneVM
import dji.v5.utils.common.ContextUtil
import kotlinx.android.synthetic.main.frag_record.*

/**
 * Description : 录音Fragment
 * Author : daniel.chen
 * CreateDate : 2022/1/17 2:41 下午
 * Copyright : ©2021 DJI All Rights Reserved.
 */
class RecordFragment:DJIFragment() {
    private val megaphoneVM: MegaphoneVM by activityViewModels()
    private var recordStarted:Boolean = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_record, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBtnListener()
    }

    private fun initBtnListener() {

        //点击触发录音
        btn_record.setOnClickListener {
            if (!recordStarted) {
                megaphoneVM.startRecord()
                chronometer.base = SystemClock.elapsedRealtime()
                chronometer.start()
                var colorStateList: ColorStateList? =
                    ContextCompat.getColorStateList(ContextUtil.getContext(), R.color.red)
//                btn_record.backgroundTintMode = PorterDuff.Mode.SRC_ATOP
//                btn_record.backgroundTintList = colorStateList
                btn_record.imageTintMode = PorterDuff.Mode.SRC_ATOP
                btn_record.imageTintList = colorStateList
                recordStarted = true
            } else {
                megaphoneVM.stopRecord()
                chronometer.stop()
                chronometer.base = SystemClock.elapsedRealtime()
                var colorStateList: ColorStateList? =
                    ContextCompat.getColorStateList(ContextUtil.getContext(), R.color.green)
                btn_record.imageTintMode = PorterDuff.Mode.SRC_ATOP
                btn_record.imageTintList = colorStateList
                recordStarted = false
            }
        }
    }
}