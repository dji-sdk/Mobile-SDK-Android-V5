package dji.sampleV5.aircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.models.LTEVM
import dji.sampleV5.aircraft.keyvalue.KeyValueDialogUtil
import dji.sampleV5.aircraft.util.Helper
import dji.v5.manager.aircraft.lte.LTELinkType
import dji.v5.manager.aircraft.lte.LTEPrivatizationServerInfo
import dji.v5.utils.common.JsonUtil
import dji.sampleV5.aircraft.util.ToastUtils
import kotlinx.android.synthetic.main.frag_lte_page.*

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/8/12
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class LTEFragment : DJIFragment() {

    private val lteVm: LTEVM by viewModels()
    private val lteMsgBuilder = StringBuffer()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_lte_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lteVm.initListener()
        lteVm.lteAuthenticationInfo.observe(viewLifecycleOwner) {
            updateLteMsg()
        }
        lteVm.lteLinkInfo.observe(viewLifecycleOwner) {
            updateLteMsg()
        }
        lteVm.acWlmDongleInfo.observe(viewLifecycleOwner) {
            updateLteMsg()
        }
        lteVm.rcWlmDongleInfo.observe(viewLifecycleOwner) {
            updateLteMsg()
        }
        lteVm.toastResult?.observe(viewLifecycleOwner) { result ->
            result?.msg?.let {
                lte_toast.text = it
            }
        }
        initBtnClickListener()
    }

    private fun initBtnClickListener() {
        btn_update_lte_authentication_info.setOnClickListener {
            lteVm.updateLTEAuthenticationInfo()
        }
        btn_get_lte_authentication_verification_code.setOnClickListener {
            KeyValueDialogUtil.showInputDialog(
                activity, "(PhoneAreacode,PhoneNumber)",
                "86,12345678900", "", false
            ) {
                it?.split(",")?.apply {
                    if (this.size < 2) {
                        ToastUtils.showToast("Value Parse Error")
                        return@showInputDialog
                    }
                    lteVm.getLTEAuthenticationVerificationCode(this[0], this[1])
                }
            }
        }
        btn_start_lte_authentication.setOnClickListener {
            KeyValueDialogUtil.showInputDialog(
                activity, "(PhoneAreacode,PhoneNumber,VerificationCode)",
                "86,12345678900,123456", "", false
            ) {
                it?.split(",")?.apply {
                    if (this.size < 3) {
                        ToastUtils.showToast("Value Parse Error")
                        return@showInputDialog
                    }
                    lteVm.startLTEAuthentication(this[0], this[1], this[2])
                }
            }
        }
        btn_set_lte_enhanced_transmission_type.setOnClickListener {
            initPopupNumberPicker(Helper.makeList(LTELinkType.values())) {
                lteVm.setLTEEnhancedTransmissionType(LTELinkType.values()[indexChosen[0]])
                resetIndex()
            }
        }
        btn_get_lte_enhanced_transmission_type.setOnClickListener {
            lteVm.getLTEEnhancedTransmissionType()
        }
        btn_set_lte_ac_privatization_server_info.setOnClickListener {
            KeyValueDialogUtil.showInputDialog(
                activity, "AC Privatization Server Info",
                JsonUtil.toJson(lteVm.lteLinkInfo.value?.aircraftPrivatizationServerInfo), "", false
            ) {
                it?.let {
                    val info = JsonUtil.toBean(it, LTEPrivatizationServerInfo::class.java)
                    if (info == null) {
                        ToastUtils.showToast("Value Parse Error")
                        return@showInputDialog
                    }
                    lteVm.setLTEAcPrivatizationServerMsg(info)
                }
            }
        }
        btn_set_lte_rc_privatization_server_info.setOnClickListener {
            KeyValueDialogUtil.showInputDialog(
                activity, "RC Privatization Server Info",
                JsonUtil.toJson(lteVm.lteLinkInfo.value?.remoteControllerPrivatizationServerInfo), "", false
            ) {
                it?.let {
                    val info = JsonUtil.toBean(it, LTEPrivatizationServerInfo::class.java)
                    if (info == null) {
                        ToastUtils.showToast("Value Parse Error")
                        return@showInputDialog
                    }
                    lteVm.setLTERcPrivatizationServerMsg(info)
                }
            }
        }
        btn_clear_ac_lte_privatization_server_info.setOnClickListener {
            lteVm.clearLTEAcPrivatizationServerMsg()
        }

        btn_clear_rc_lte_privatization_server_info.setOnClickListener {
            lteVm.clearLTERcPrivatizationServerMsg()
        }
    }

    private fun updateLteMsg() {
        lteMsgBuilder.setLength(0)

        lteMsgBuilder.append("LTEAuthenticationInfo:").append("\n")
        lteVm.lteAuthenticationInfo.value?.let {
            lteMsgBuilder.append(JsonUtil.toJson(it))
        }
        lteMsgBuilder.append("\n<---------------------------------------------------->\n")

        lteMsgBuilder.append("LTELinkInfo:").append("\n")
        lteVm.lteLinkInfo.value?.let {
            lteMsgBuilder.append(JsonUtil.toJson(it))
        }
        lteMsgBuilder.append("\n<---------------------------------------------------->\n")

        lteMsgBuilder.append("AcWlmDongleInfo:").append("\n")
        lteVm.acWlmDongleInfo.value?.let {
            lteMsgBuilder.append(JsonUtil.toJson(it))
        }
        lteMsgBuilder.append("\n<---------------------------------------------------->\n")

        lteMsgBuilder.append("RcWlmDongleInfo:").append("\n")
        lteVm.rcWlmDongleInfo.value?.let {
            lteMsgBuilder.append(JsonUtil.toJson(it))
        }

        lte_msg.text = lteMsgBuilder.toString()
    }
}