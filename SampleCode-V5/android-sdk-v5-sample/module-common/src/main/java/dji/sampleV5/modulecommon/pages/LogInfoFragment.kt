package dji.sampleV5.modulecommon.pages

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import dji.sampleV5.modulecommon.R
import dji.sampleV5.modulecommon.models.MSDKLogVM
import dji.sampleV5.modulecommon.util.ToastUtils
import kotlinx.android.synthetic.main.frag_log_info_page.*

/**
 * ClassName : LogInfoFragment
 * Description : 展示最新崩溃日志信息
 * Author : daniel.chen
 * CreateDate : 2022/5/7 2:33 下午
 * Copyright : ©2022 DJI All Rights Reserved.
 */
class LogInfoFragment: DJIFragment() {
    private val logVm:MSDKLogVM by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_log_info_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tv_log_info.movementMethod=ScrollingMovementMethod.getInstance()
        logVm.logInfo.observe(viewLifecycleOwner) {
            updateLogInfo()
        }
        initBtn()
    }

    private fun initBtn() {
        btn_get_log_info.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                logVm.updateLogInfo()
                ToastUtils.showToast("Get Log Count: " + logVm.logCount.value)
            }
        })
    }

    private fun updateLogInfo() {
        tv_log_info.text = logVm.logInfo.value
    }
}