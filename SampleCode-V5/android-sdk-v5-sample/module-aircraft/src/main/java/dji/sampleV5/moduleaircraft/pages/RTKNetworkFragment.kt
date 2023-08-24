package dji.sampleV5.moduleaircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dji.rtk.CoordinateSystem
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.sampleV5.modulecommon.util.Helper
import dji.sampleV5.moduleaircraft.R
import dji.sampleV5.moduleaircraft.models.RTKNetworkVM
import dji.sampleV5.moduledrone.pages.RTKCenterFragment.Companion.KEY_IS_CMCC_RTK
import dji.v5.ux.core.extension.show
import dji.v5.ux.core.extension.hide
import dji.sampleV5.moduledrone.pages.RTKCenterFragment.Companion.KEY_IS_QX_RTK
import dji.sdk.keyvalue.value.rtkbasestation.RTKCustomNetworkSetting
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.utils.common.JsonUtil
import dji.v5.utils.common.LogUtils
import dji.sampleV5.modulecommon.util.ToastUtils
import kotlinx.android.synthetic.main.frag_network_rtk_page.*
import kotlinx.android.synthetic.main.frag_network_rtk_page.btn_start_cmcc_rtk_service

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/7/23
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
class RTKNetworkFragment : DJIFragment() {

    private val TAG = "RTKNetworkFragment"
    private var currentOtherError: String = ""
    private val rtkNetworkVM: RTKNetworkVM by viewModels()
    private val rtkMsgBuilder: StringBuilder = StringBuilder()
    private var isQXRTK = false
    private var isCMCCRTK = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_network_rtk_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBtnListener()
        initListener()
        initView()
    }

    private fun initView() {
        arguments?.run {
            isQXRTK = getBoolean(KEY_IS_QX_RTK, false)
            isCMCCRTK = getBoolean(KEY_IS_CMCC_RTK, false)
        }
        if (isCMCCRTK) {
            btn_start_custom_network_rtk_service.hide()
            btn_stop_custom_network_rtk_service.hide()
            btn_set_custom_network_rtk_settings.hide()
            btn_start_qx_network_rtk_service.hide()
            btn_stop_qx_network_rtk_service.hide()
            btn_start_cmcc_rtk_service.show()
            btn_stop_cmcc_rtk_service.show()
        } else if (isQXRTK) {
            btn_start_custom_network_rtk_service.hide()
            btn_stop_custom_network_rtk_service.hide()
            btn_set_custom_network_rtk_settings.hide()
            btn_start_cmcc_rtk_service.hide()
            btn_stop_cmcc_rtk_service.hide()
            btn_start_qx_network_rtk_service.show()
            btn_stop_qx_network_rtk_service.show()
        } else {
            btn_start_custom_network_rtk_service.show()
            btn_stop_custom_network_rtk_service.show()
            btn_set_custom_network_rtk_settings.show()
            btn_start_cmcc_rtk_service.hide()
            btn_stop_cmcc_rtk_service.hide()
            btn_start_qx_network_rtk_service.hide()
            btn_stop_qx_network_rtk_service.hide()
        }
    }

    private fun initBtnListener() {
        /**
         * 自定义网络RTK接口
         */
        btn_start_custom_network_rtk_service.setOnClickListener {
            rtkNetworkVM.startCustomNetworkRTKService(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast("StartCustomNetworkRTKService Success")
                    updateErrMsg(isSuccess = true)

                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("StartCustomNetworkRTKService onFailure $error")
                    updateErrMsg(error.toString())

                }
            })
        }
        btn_stop_custom_network_rtk_service.setOnClickListener {
            rtkNetworkVM.stopCustomNetworkRTKService(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast("StopCustomNetworkRTKService Success")
                    updateErrMsg(isSuccess = true)
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("StopCustomNetworkRTKService onFailure $error")
                    updateErrMsg(error.toString())

                }
            })
        }
        btn_set_custom_network_rtk_settings.setOnClickListener {
            val currentCustomNetworkRTKSettingCache = rtkNetworkVM.getCurrentCustomNetworkRTKSettingCache()
            openInputDialog(currentCustomNetworkRTKSettingCache, "Set custom network RTK account information") {
                val setting = JsonUtil.toBean(it, RTKCustomNetworkSetting::class.java)
                if (setting == null) {
                    ToastUtils.showToast("数据解析失败，请重试")
                    return@openInputDialog
                }
                rtkNetworkVM.setCustomNetworkRTKSettings( setting)
            }
        }
        /**
         * 千寻RTK接口
         */
        //获取初始化坐标系
        btn_start_qx_network_rtk_service.setOnClickListener {
            val coordinateSystem = CoordinateSystem.values()
            initPopupNumberPicker(Helper.makeList(coordinateSystem)) {
                rtkNetworkVM.startQXNetworkRTKService(coordinateSystem[indexChosen[0]], object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        ToastUtils.showToast("StartQXNetworkRTKService Success")
                        updateErrMsg(isSuccess = true)
                    }

                    override fun onFailure(error: IDJIError) {
                        ToastUtils.showToast("StartQXNetworkRTKService onFailure $error")
                        updateErrMsg(error.toString())
                    }
                })
                resetIndex()
            }
        }
        btn_stop_qx_network_rtk_service.setOnClickListener {
            rtkNetworkVM.stopQXNetworkRTKService(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast("StopQXNetworkRTKService Success")
                    updateErrMsg(isSuccess = true)
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("StopQXNetworkRTKService onFailure $error")
                    updateErrMsg(error.toString())
                }
            })
        }
        /**
         * CMCC接口
         */
        btn_start_cmcc_rtk_service.setOnClickListener {
            val coordinateSystem = CoordinateSystem.values()
            initPopupNumberPicker(Helper.makeList(coordinateSystem)) {
                rtkNetworkVM.startCMCCRTKService(coordinateSystem[indexChosen[0]], object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        ToastUtils.showToast("startCMCCRTKService Success")
                        updateErrMsg(isSuccess = true)
                    }

                    override fun onFailure(error: IDJIError) {
                        ToastUtils.showToast("startCMCCRTKService onFailure $error")
                        updateErrMsg(error.toString())
                    }
                })
                resetIndex()
            }
        }

        btn_stop_cmcc_rtk_service.setOnClickListener {
            rtkNetworkVM.stopCMCCRTKService(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast("stopCMCCRTKService Success")
                    updateErrMsg(isSuccess = true)
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("stopCMCCRTKService onFailure $error")
                    updateErrMsg(error.toString())
                }
            })
        }

    }

    private fun initListener() {
        rtkNetworkVM.addNetworkRTKServiceInfoCallback()
        rtkNetworkVM.currentRTKState.observe(viewLifecycleOwner) {
            if (isQXRTK) {
                rtkNetworkVM.getQXNetworkRTKCoordinateSystem()
            }
            updateRTKInfo()
        }
        rtkNetworkVM.currentRTKErrorMsg.observe(viewLifecycleOwner) {
            updateRTKInfo()
        }
        rtkNetworkVM.currentQxNetworkCoordinateSystem.observe(viewLifecycleOwner) {
            updateRTKInfo()
        }
        rtkNetworkVM.currentCustomNetworkRTKSettings.observe(viewLifecycleOwner) {
            updateRTKInfo()
        }
        rtkNetworkVM.currentRTKState.observe(viewLifecycleOwner) {
            updateRTKInfo()
        }
    }

    private fun updateRTKInfo() {
        if (!isFragmentShow()) {
            return
        }
        rtkMsgBuilder.apply {
            setLength(0)
            append("CurrentRTKState:").append(rtkNetworkVM.currentRTKState.value).append("\n")
            append("CurrentRTKErrorMsg:").append(rtkNetworkVM.currentRTKErrorMsg.value + ",$currentOtherError").append("\n")
            append("CurrentQxNetworkCoordinateSystem:").append(rtkNetworkVM.currentQxNetworkCoordinateSystem.value)
                .append("\n")
            append("CurrentCustomNetworkRTKSettings:").append(rtkNetworkVM.currentCustomNetworkRTKSettings.value)
                .append("\n")
        }
        activity?.runOnUiThread {
            text_network_rtk_info.text = rtkMsgBuilder.toString()
        }
    }

    fun updateErrMsg(errMsg: String? = null, isSuccess: Boolean = false) {
        //成功之后清除之前的错误信息
        currentOtherError = if (isSuccess) {
            ""
        } else {
            errMsg ?: ""

        }
        updateRTKInfo()
        LogUtils.i(TAG, "[updateErrMsg]currentError=$currentOtherError")
    }

    override fun onDestroy() {
        super.onDestroy()
        rtkNetworkVM.removeNetworkServiceInfoListener()
    }
}
