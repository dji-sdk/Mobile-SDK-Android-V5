package dji.sampleV5.aircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import dji.sampleV5.aircraft.databinding.FragLogInfoPageBinding
import dji.sampleV5.aircraft.models.MSDKCrashLogVM
import dji.v5.ux.util.ToastUtils

/**
 * ClassName : LogInfoFragment
 * Description : 展示最新崩溃日志信息
 * Author : daniel.chen
 * CreateDate : 2022/5/7 2:33 下午
 * Copyright : ©2022 DJI All Rights Reserved.
 */
class CrashLogInfoFragment : DJIFragment() {
    private val logVm: MSDKCrashLogVM by activityViewModels()
    private var binding: FragLogInfoPageBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragLogInfoPageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logVm.updateLogInfo()
        logVm.logInfo.observe(viewLifecycleOwner) {
            binding?.tvLogInfo?.text = logVm.logInfo.value
        }
        logVm.logMsg.observe(viewLifecycleOwner) {
            ToastUtils.showToast("Msg:$it")
        }
        binding?.btnGetLogInfo?.setOnClickListener {
            logVm.updateLogInfo()
        }
        binding?.btnTestJavaCrash?.setOnClickListener {
            logVm.testJavaCrash(false)
        }
        binding?.btnTestNativeCrash?.setOnClickListener {
            logVm.testNativeCrash(true)
        }
    }
}