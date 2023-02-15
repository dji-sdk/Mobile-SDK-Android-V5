package dji.sampleV5.moduleaircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dji.sampleV5.moduleaircraft.R
import dji.sampleV5.moduleaircraft.models.UASJapanVM
import dji.sampleV5.modulecommon.keyvalue.KeyItemActionListener
import dji.sampleV5.modulecommon.keyvalue.KeyValueDialogUtil
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.v5.utils.common.JsonUtil
import dji.v5.utils.common.LogUtils
import kotlinx.android.synthetic.main.frag_uas_jp_page.*

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
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_uas_jp_page, container, false)
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

        bt_set_registration_number.setOnClickListener {
            val uaTest = JsonUtil.toJson(UATest())
            showDialog("输入从民航局获取的信息", uaTest.toString()) {
                it?.let {
                    LogUtils.i(TAG, it)
                    uasJapanVM.setUARegistrationNumber(it)
                }
            }
        }
        bt_get_registration_number.setOnClickListener {
            uasJapanVM.getUARegistrationNumber()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        uasJapanVM.clearAllUARegistrationNumberStatusListener()
    }

    inner class UATest {
        val registration_code: String = "111111111111111111111111111111"
        val key_info: String = "11111111111111111111111111111111"
        val nonce_info: String = "111111111111"
        override fun toString(): String {
            return "UATest(registration_code='$registration_code', key_info='$key_info', nonce_info='$nonce_info')"
        }

    }

    private fun updateInfo() {
        tv_ua_import_tip.text = "isUARegistrationNumberImport:${uasJapanVM.uaRegNumberStatus.value?.isUARegistrationNumberImport},\n" +
                "uasRemoteIDStatus=${uasJapanVM.uasRemoteIDStatus.value},\n" +
                "uaRegistrationNumber=${uasJapanVM.uaRegistrationNumber.value}"


    }

}