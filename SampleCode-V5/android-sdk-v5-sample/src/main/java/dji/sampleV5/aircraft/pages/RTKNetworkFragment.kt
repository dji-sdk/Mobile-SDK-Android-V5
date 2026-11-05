package dji.sampleV5.aircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dji.rtk.CoordinateSystem
import dji.v5.ux.util.Helper
import dji.sampleV5.aircraft.databinding.FragNetworkRtkPageBinding
import dji.sampleV5.aircraft.models.RTKNetworkVM
import dji.sampleV5.moduledrone.pages.RTKCenterFragment.Companion.KEY_IS_CMCC_RTK
import dji.v5.ux.core.extension.show
import dji.v5.ux.core.extension.hide
import dji.sampleV5.moduledrone.pages.RTKCenterFragment.Companion.KEY_IS_QX_RTK
import dji.sdk.keyvalue.value.rtkbasestation.RTKCustomNetworkSetting
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.utils.common.JsonUtil
import dji.v5.utils.common.LogUtils
import dji.v5.ux.util.ToastUtils

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
    private var binding: FragNetworkRtkPageBinding? = null
    private val rtkMsgBuilder: StringBuilder = StringBuilder()
    private var isQXRTK = false
    private var isCMCCRTK = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragNetworkRtkPageBinding.inflate(inflater, container, false)
        return binding?.root
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
            binding?.btnStartCustomNetworkRtkService?.hide()
            binding?.btnStopCustomNetworkRtkService?.hide()
            binding?.btnSetCustomNetworkRtkSettings?.hide()
            binding?.btnStartQxNetworkRtkService?.hide()
            binding?.btnStopQxNetworkRtkService?.hide()
            binding?.btnStartCmccRtkService?.show()
            binding?.btnStopCmccRtkService?.show()
        } else if (isQXRTK) {
            binding?.btnStartCustomNetworkRtkService?.hide()
            binding?.btnStopCustomNetworkRtkService?.hide()
            binding?.btnSetCustomNetworkRtkSettings?.hide()
            binding?.btnStartCmccRtkService?.hide()
            binding?.btnStopCmccRtkService?.hide()
            binding?.btnStartQxNetworkRtkService?.show()
            binding?.btnStopQxNetworkRtkService?.show()
        } else {
            binding?.btnStartCustomNetworkRtkService?.show()
            binding?.btnStopCustomNetworkRtkService?.show()
            binding?.btnSetCustomNetworkRtkSettings?.show()
            binding?.btnStartCmccRtkService?.hide()
            binding?.btnStopCmccRtkService?.hide()
            binding?.btnStartQxNetworkRtkService?.hide()
            binding?.btnStopQxNetworkRtkService?.hide()
        }
    }

    private fun initBtnListener() {
        /**
         * 自定义网络RTK接口
         */
        binding?.btnStartCustomNetworkRtkService?.setOnClickListener {
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
        binding?.btnStopCustomNetworkRtkService?.setOnClickListener {
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
        binding?.btnSetCustomNetworkRtkSettings?.setOnClickListener {
            val currentCustomNetworkRTKSettingCache = rtkNetworkVM.getCurrentCustomNetworkRTKSettingCache()
            openInputDialog(currentCustomNetworkRTKSettingCache, "Set custom network RTK account information") {
                val setting = JsonUtil.toBean(it, RTKCustomNetworkSetting::class.java)
                if (setting == null) {
                    ToastUtils.showToast("数据解析失败，请重试")
                    return@openInputDialog
                }
                rtkNetworkVM.setCustomNetworkRTKSettings(setting)
            }
        }
        /**
         * 千寻RTK接口
         */
        //获取初始化坐标系
        binding?.btnStartQxNetworkRtkService?.setOnClickListener {
            val coordinateSystem = CoordinateSystem.values()
            initPopupNumberPicker(Helper.makeList(coordinateSystem)) {
                rtkNetworkVM.startQXNetworkRTKService(coordinateSystem[indexChosen[0]], object :
                    CommonCallbacks.CompletionCallback {
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
        binding?.btnStopQxNetworkRtkService?.setOnClickListener {
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
        binding?.btnStartCmccRtkService?.setOnClickListener {
            val coordinateSystem = CoordinateSystem.values()
            initPopupNumberPicker(Helper.makeList(coordinateSystem)) {
                rtkNetworkVM.startCMCCRTKService(coordinateSystem[indexChosen[0]], object :
                    CommonCallbacks.CompletionCallback {
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

        binding?.btnStopCmccRtkService?.setOnClickListener {
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
            binding?.textNetworkRtkInfo?.text = rtkMsgBuilder.toString()
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
