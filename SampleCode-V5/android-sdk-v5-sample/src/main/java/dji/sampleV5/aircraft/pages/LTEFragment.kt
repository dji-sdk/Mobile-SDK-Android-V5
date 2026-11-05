package dji.sampleV5.aircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dji.sampleV5.aircraft.data.QuickTestConfig
import dji.sampleV5.aircraft.databinding.FragLtePageBinding
import dji.sampleV5.aircraft.keyvalue.KeyValueDialogUtil
import dji.sampleV5.aircraft.models.LTEVM
import dji.v5.ux.util.Helper
import dji.v5.ux.util.ToastUtils
import dji.v5.manager.aircraft.lte.LTELinkType
import dji.v5.manager.aircraft.lte.LTEPrivatizationServerInfo
import dji.v5.utils.common.JsonUtil

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
    private var binding: FragLtePageBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragLtePageBinding.inflate(inflater, container, false)
        return binding?.root
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
                binding?.lteToast?.text = it
            }
        }
        initBtnClickListener()
    }

    private fun initBtnClickListener() {
        binding?.btnUpdateLteAuthenticationInfo?.setOnClickListener {
            lteVm.updateLTEAuthenticationInfo()
        }
        binding?.btnGetLteAuthenticationVerificationCode?.setOnClickListener {
            val cacheInfo = QuickTestConfig.getCacheLTEAuthenticationInfo()
            KeyValueDialogUtil.showInputDialog(
                activity, "(PhoneAreaCode,PhoneNumber)",
                "${cacheInfo?.phoneAreaCode},${cacheInfo?.phoneNumber}", "", false
            ) {
                it?.split(",")?.apply {
                    if (this.size < 2) {
                        ToastUtils.showToast("Value Parse Error")
                        return@showInputDialog
                    }
                    lteVm.getLTEAuthenticationVerificationCode(this[0], this[1])
                    QuickTestConfig.updateCacheLTEAuthenticationInfo(QuickTestConfig.LTEAuthCacheInfo(this[0], this[1]))
                }
            }
        }
        binding?.btnStartLteAuthentication?.setOnClickListener {
            val cacheInfo = QuickTestConfig.getCacheLTEAuthenticationInfo()
            KeyValueDialogUtil.showInputDialog(
                activity, "(PhoneAreacode,PhoneNumber,VerificationCode)",
                "${cacheInfo?.phoneAreaCode},${cacheInfo?.phoneNumber},${cacheInfo?.verificationCode}", "", false
            ) {
                it?.split(",")?.apply {
                    if (this.size < 3) {
                        ToastUtils.showToast("Value Parse Error")
                        return@showInputDialog
                    }
                    lteVm.startLTEAuthentication(this[0], this[1], this[2])
                    QuickTestConfig.updateCacheLTEAuthenticationInfo(QuickTestConfig.LTEAuthCacheInfo(this[0], this[1], this[2]))
                }
            }
        }
        binding?.btnSetLteEnhancedTransmissionType?.setOnClickListener {
            initPopupNumberPicker(Helper.makeList(LTELinkType.values())) {
                lteVm.setLTEEnhancedTransmissionType(LTELinkType.values()[indexChosen[0]])
                resetIndex()
            }
        }
        binding?.btnGetLteEnhancedTransmissionType?.setOnClickListener {
            lteVm.getLTEEnhancedTransmissionType()
        }
        binding?.btnSetLteAcPrivatizationServerInfo?.setOnClickListener {
            val cacheInfo = QuickTestConfig.getCacheACLTEPrivatizationServerInfo()
            KeyValueDialogUtil.showInputDialog(
                activity, "AC Privatization Server Info",
                JsonUtil.toJson(cacheInfo), "", false
            ) {
                it?.let {
                    val info = JsonUtil.toBean(it, LTEPrivatizationServerInfo::class.java)
                    if (info == null) {
                        ToastUtils.showToast("Value Parse Error")
                        return@showInputDialog
                    }
                    lteVm.setLTEAcPrivatizationServerMsg(info)
                    QuickTestConfig.updateCacheACLTEPrivatizationServerInfo(info)
                }
            }
        }
        binding?.btnSetLteRcPrivatizationServerInfo?.setOnClickListener {
            val cacheInfo = QuickTestConfig.getCacheRCLTEPrivatizationServerInfo()
            KeyValueDialogUtil.showInputDialog(
                activity, "RC Privatization Server Info",
                JsonUtil.toJson(cacheInfo), "", false
            ) {
                it?.let {
                    val info = JsonUtil.toBean(it, LTEPrivatizationServerInfo::class.java)
                    if (info == null) {
                        ToastUtils.showToast("Value Parse Error")
                        return@showInputDialog
                    }
                    lteVm.setLTERcPrivatizationServerMsg(info)
                    QuickTestConfig.updateCacheRCLTEPrivatizationServerInfo(info)
                }
            }
        }
        binding?.btnClearAcLtePrivatizationServerInfo?.setOnClickListener {
            lteVm.clearLTEAcPrivatizationServerMsg()
        }

        binding?.btnClearRcLtePrivatizationServerInfo?.setOnClickListener {
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

        binding?.lteMsg?.text = lteMsgBuilder.toString()
    }
}