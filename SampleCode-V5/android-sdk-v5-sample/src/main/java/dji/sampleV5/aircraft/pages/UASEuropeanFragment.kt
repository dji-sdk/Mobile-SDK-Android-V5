package dji.sampleV5.aircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.models.UASEuropeanVM
import dji.sampleV5.aircraft.keyvalue.KeyValueDialogUtil
import dji.v5.utils.common.JsonUtil
import kotlinx.android.synthetic.main.frag_uas_european_page.bt_get_operator_registration_number
import kotlinx.android.synthetic.main.frag_uas_european_page.bt_set_operator_registration_number
import kotlinx.android.synthetic.main.frag_uas_european_page.tv_uas_european_info

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
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_uas_european_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListener()
    }

    private fun initListener() {
        uasEuropeanVM.addRemoteIdStatusListener()
        uasEuropeanVM.addOperatorRegistrationNumberStatusListener()
        uasEuropeanVM.uasRemoteIDStatus.observe(viewLifecycleOwner) {
            updateUASInfo()
        }
        uasEuropeanVM.operatorRegistrationNumberStatus.observe(viewLifecycleOwner) {
            updateUASInfo()
        }
        uasEuropeanVM.currentOperatorRegistrationNumber.observe(viewLifecycleOwner) {
            updateUASInfo()
        }
        bt_set_operator_registration_number.setOnClickListener {
            KeyValueDialogUtil.showInputDialog(
                activity, "Operator Registration Number",
                uasEuropeanVM.currentOperatorRegistrationNumber.value + "-xxx", "", false
            ) {
                it?.apply {
                    uasEuropeanVM.setOperatorRegistrationNumber(this)
                }
            }
        }
        bt_get_operator_registration_number.setOnClickListener {
            uasEuropeanVM.getOperatorRegistrationNumber()
        }
        uasEuropeanVM.getOperatorRegistrationNumber()
    }

    override fun onDestroy() {
        super.onDestroy()
        uasEuropeanVM.clearRemoteIdStatusListener()
        uasEuropeanVM.removeOperatorRegistrationNumberStatusListener()
    }

    private fun updateUASInfo() {
        val builder = StringBuilder()
        builder.append("Uas Remote ID Status:").append(JsonUtil.toJson(uasEuropeanVM.uasRemoteIDStatus.value))
        builder.append("\n")
        builder.append("Uas Remote Operator Registration Number:").append(JsonUtil.toJson(uasEuropeanVM.currentOperatorRegistrationNumber.value))
        builder.append("\n")
        builder.append("Uas Operator Registration Number Status:").append(JsonUtil.toJson(uasEuropeanVM.operatorRegistrationNumberStatus.value))
        builder.append("\n")
        mainHandler.post {
            tv_uas_european_info.text = builder.toString()
        }
    }
}