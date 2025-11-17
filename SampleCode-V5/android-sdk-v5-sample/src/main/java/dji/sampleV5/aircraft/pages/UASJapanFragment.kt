package dji.sampleV5.aircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dji.sampleV5.aircraft.databinding.FragUasJpPageBinding
import dji.sampleV5.aircraft.models.UASJapanVM
import dji.v5.utils.common.JsonUtil
import dji.v5.utils.common.LogUtils

/**
 * Description :日本无人机远程识别示例
 *
 * @author: Byte.Cai
 *  date : 2022/6/27
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class UASJapanFragment : DJIFragment() {
    private val TAG = "UASJapanFragment"
    private val uasJapanVM: UASJapanVM by viewModels()
    private var binding: FragUasJpPageBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragUasJpPageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uasJapanVM.addUASRemoteIDStatusListener()
        uasJapanVM.addUARegistrationNumberStatusListener()
        uasJapanVM.uaRegNumberStatus.observe(viewLifecycleOwner) {
            updateInfo()
        }
        uasJapanVM.uasRemoteIDStatus.observe(viewLifecycleOwner) {
            updateInfo()
        }
        uasJapanVM.uaRegistrationNumber.observe(viewLifecycleOwner) {
            updateInfo()
        }

        binding?.btSetRegistrationNumber?.setOnClickListener {
            val uaTest = JsonUtil.toJson(UATest())
            showDialog("输入从民航局获取的信息", uaTest.toString()) {
                it?.let {
                    LogUtils.i(TAG, it)
                    uasJapanVM.setUARegistrationNumber(it)
                }
            }
        }
        binding?.btGetRegistrationNumber?.setOnClickListener {
            uasJapanVM.getUARegistrationNumber()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        uasJapanVM.clearAllListener()
    }

    inner class UATest {
        private val registration_code: String = "111111111111111111111111111111"
        private val key_info: String = "11111111111111111111111111111111"
        private val nonce_info: String = "111111111111"
        override fun toString(): String {
            return "UATest(registration_code='$registration_code', key_info='$key_info', nonce_info='$nonce_info')"
        }
    }

    private fun updateInfo() {
        binding?.tvUaImportTip?.text = "isUARegistrationNumberImport:${uasJapanVM.uaRegNumberStatus.value?.isUARegistrationNumberImport},\n" +
                "uasRemoteIDStatus=${uasJapanVM.uasRemoteIDStatus.value},\n" +
                "uaRegistrationNumber=${uasJapanVM.uaRegistrationNumber.value}"
    }

}