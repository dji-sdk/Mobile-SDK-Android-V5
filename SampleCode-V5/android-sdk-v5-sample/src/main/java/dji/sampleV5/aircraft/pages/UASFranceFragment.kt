package dji.sampleV5.aircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.viewModels
import dji.sampleV5.aircraft.databinding.FragUasFrancePageBinding
import dji.sampleV5.aircraft.models.UASFranceVM
import dji.sampleV5.aircraft.util.ToastUtils
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.utils.common.JsonUtil

/**
 * Description :法国无人机远程识别示例
 *
 * @author: Byte.Cai
 *  date : 2022/6/27
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class UASFranceFragment : DJIFragment(), CompoundButton.OnCheckedChangeListener {
    private val uasFranceVM: UASFranceVM by viewModels()
    private var binding: FragUasFrancePageBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragUasFrancePageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListener()
    }

    private fun initListener() {
        binding?.tbEidEnableSwitch?.setOnCheckedChangeListener(this)
        uasFranceVM.addElectronicIDStatusListener()
        uasFranceVM.addRemoteIdStatusListener()
        uasFranceVM.uasRemoteIDStatus.observe(viewLifecycleOwner) {
            updateUASInfo()
        }
        uasFranceVM.electronicIDStatus.observe(viewLifecycleOwner) {
            updateUASInfo()
        }
    }

    private fun updateUASInfo() {
        val electronicIDStatus = uasFranceVM.electronicIDStatus.value

        val builder = StringBuilder()
        builder.append("Uas Remote ID Status:").append(JsonUtil.toJson(uasFranceVM.uasRemoteIDStatus.value))
        builder.append("\n")
        builder.append("IsElectronicIDEnabled:").append(electronicIDStatus?.isElectronicIDEnabled)
        builder.append("\n")
        mainHandler.post {
            binding?.tvEidEnableTip?.text = builder.toString()
        }

        binding?.tbEidEnableSwitch?.setOnCheckedChangeListener(null)
        binding?.tbEidEnableSwitch?.isChecked = electronicIDStatus?.isElectronicIDEnabled?:false
        binding?.tbEidEnableSwitch?.setOnCheckedChangeListener(this@UASFranceFragment)

    }


    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        uasFranceVM.setElectronicIDEnabled(isChecked, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                ToastUtils.showToast("setElectronicIDEnabled success")
            }


            override fun onFailure(error: IDJIError) {
                binding?.tbEidEnableSwitch?.setOnCheckedChangeListener(null)
                binding?.tbEidEnableSwitch?.isChecked = !isChecked
                binding?.tbEidEnableSwitch?.setOnCheckedChangeListener(this@UASFranceFragment)
                ToastUtils.showToast(error.toString())
            }

        })
    }

    override fun onDestroy() {
        super.onDestroy()
        uasFranceVM.clearAllListener()
    }
}