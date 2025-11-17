package dji.sampleV5.aircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dji.sampleV5.aircraft.databinding.FragUasEuropeanPageBinding
import dji.sampleV5.aircraft.keyvalue.KeyValueDialogUtil
import dji.sampleV5.aircraft.models.UASEuropeanVM
import dji.v5.utils.common.JsonUtil

/**
 * Description :欧洲无人机远程识别示例
 *
 * @author: Byte.Cai
 *  date : 2022/6/27
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class UASEuropeanFragment : DJIFragment() {
    private val uasEuropeanVM: UASEuropeanVM by viewModels()
    private var binding: FragUasEuropeanPageBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragUasEuropeanPageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListener()
    }

    private fun initListener() {
        uasEuropeanVM.addRemoteIdStatusListener()
        uasEuropeanVM.addOperatorRegistrationNumberStatusListener()
        uasEuropeanVM.addCClassStatusListener()
        uasEuropeanVM.uasRemoteIDStatus.observe(viewLifecycleOwner) {
            updateUASInfo()
        }
        uasEuropeanVM.operatorRegistrationNumberStatus.observe(viewLifecycleOwner) {
            updateUASInfo()
        }
        uasEuropeanVM.currentOperatorRegistrationNumber.observe(viewLifecycleOwner) {
            updateUASInfo()
        }
        uasEuropeanVM.currentCClassStatus.observe(viewLifecycleOwner) {
            updateUASInfo()
        }
        uasEuropeanVM.M3ELittleBatteryType.observe(viewLifecycleOwner) {
            updateUASInfo()
        }
        binding?.btSetOperatorRegistrationNumber?.setOnClickListener {
            KeyValueDialogUtil.showInputDialog(
                activity, "Operator Registration Number",
                uasEuropeanVM.currentOperatorRegistrationNumber.value + "-xxx", "", false
            ) {
                it?.apply {
                    uasEuropeanVM.setOperatorRegistrationNumber(this)
                }
            }
        }
        binding?.btGetOperatorRegistrationNumber?.setOnClickListener {
            uasEuropeanVM.getOperatorRegistrationNumber()
        }
        uasEuropeanVM.getOperatorRegistrationNumber()
    }

    override fun onDestroy() {
        super.onDestroy()
        uasEuropeanVM.clearRemoteIdStatusListener()
        uasEuropeanVM.removeOperatorRegistrationNumberStatusListener()
        uasEuropeanVM.clearAllCClassStatusListener()
    }

    private fun updateUASInfo() {
        val builder = StringBuilder()
        builder.append("Uas Remote ID Status:").append(JsonUtil.toJson(uasEuropeanVM.uasRemoteIDStatus.value))
        builder.append("\n")
        builder.append("Uas Remote Operator Registration Number:").append(JsonUtil.toJson(uasEuropeanVM.currentOperatorRegistrationNumber.value))
        builder.append("\n")
        builder.append("Uas Operator Registration Number Status:").append(JsonUtil.toJson(uasEuropeanVM.operatorRegistrationNumberStatus.value))
        builder.append("\n")
        builder.append("CClassStatus:").append(uasEuropeanVM.currentCClassStatus.value!!.name)
        builder.append("\n")
        uasEuropeanVM.M3ELittleBatteryType.value?.let {
            builder.append("M3E Battery Type:").append(it)
            builder.append("\n")
        }
        mainHandler.post {
            binding?.tvUasEuropeanInfo?.text = builder.toString()
        }
    }
}