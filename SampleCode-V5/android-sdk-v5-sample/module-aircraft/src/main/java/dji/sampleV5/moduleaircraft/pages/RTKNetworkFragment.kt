package dji.sampleV5.moduleaircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import dji.rtk.CoordinateSystem
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.sampleV5.modulecommon.util.Helper
import dji.sampleV5.modulecommon.util.ToastUtils
import dji.sampleV5.moduleaircraft.R
import dji.sampleV5.moduleaircraft.models.RTKCenterVM
import dji.sampleV5.moduleaircraft.models.RTKVM
import dji.sdk.keyvalue.value.rtkbasestation.RTKCustomNetworkSetting
import dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.utils.common.JsonUtil
import kotlinx.android.synthetic.main.frag_network_rtk_page.*

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/7/23
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
class RTKNetworkFragment : DJIFragment() {

    private val rtkVM: RTKVM by activityViewModels()
    private val rtkCenterVM: RTKCenterVM by activityViewModels()
    private val rtkMsgBuilder: StringBuilder = StringBuilder()

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
    }

    private fun initBtnListener() {
        rtkCenterVM.setRTKReferenceStationSource(RTKReferenceStationSource.CUSTOM_NETWORK_SERVICE)
        btn_start_custom_network_rtk_service.setOnClickListener {
            rtkVM.startCustomNetworkRTKService(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast(context, "StartCustomNetworkRTKService Success")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast(context, "StartCustomNetworkRTKService onFailure $error")
                }
            })
        }
        btn_stop_custom_network_rtk_service.setOnClickListener {
            rtkVM.stopCustomNetworkRTKService(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast(context, "StopCustomNetworkRTKService Success")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast(context, "StopCustomNetworkRTKService onFailure $error")
                }
            })
        }
        btn_set_custom_network_rtk_settings.setOnClickListener {
            val currentCustomNetworkRTKSettingCache = rtkVM.getCurrentCustomNetworkRTKSettingCache(context)
            openInputDialog(currentCustomNetworkRTKSettingCache, "Set Custom Network Rtk Settings") {
                val setting = JsonUtil.toBean(it, RTKCustomNetworkSetting::class.java)
                setting?.let {
                    rtkVM.setCustomNetworkRTKSettings(context,setting)
                }
            }
        }
        btn_start_qx_network_rtk_service.setOnClickListener {
            rtkVM.startQXNetworkRTKService(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast(context, "StartQXNetworkRTKService Success")
                    rtkVM.getQXNetworkRTKCoordinateSystem()
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast(context, "StartQXNetworkRTKService onFailure $error")
                }
            })
        }
        btn_stop_qx_network_rtk_service.setOnClickListener {
            rtkVM.stopQXNetworkRTKService(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast(context, "StopQXNetworkRTKService Success")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast(context, "StopQXNetworkRTKService onFailure $error")
                }
            })
        }
        btn_set_qx_network_rtk_coordinate_system.setOnClickListener {
            val coordinateSystem = CoordinateSystem.values()
            initPopupNumberPicker(Helper.makeList(coordinateSystem)) {
                rtkVM.setQXNetworkRTKCoordinateSystem(coordinateSystem[indexChosen[0]],
                    object : CommonCallbacks.CompletionCallback {
                        override fun onSuccess() {
                            ToastUtils.showToast(context, "SetQXNetworkRTKCoordinateSystem Success")
                        }

                        override fun onFailure(error: IDJIError) {
                            ToastUtils.showToast(
                                context, "SetQXNetworkRTKCoordinateSystem onFailure $error"
                            )
                        }
                    })
                resetIndex()
            }
        }
    }

    private fun initListener() {

        rtkVM.addNetworkRTKServiceInfoCallback()
        rtkVM.currentRTKState.observe(viewLifecycleOwner) {
            updateRTKInfo()
        }
        rtkVM.currentRTKErrorMsg.observe(viewLifecycleOwner) {
            updateRTKInfo()
        }
        rtkVM.currentQxNetworkCoordinateSystem.observe(viewLifecycleOwner) {
            updateRTKInfo()
        }
        rtkVM.currentCustomNetworkRTKSettings.observe(viewLifecycleOwner) {
            updateRTKInfo()
        }
        rtkVM.currentRTKState.observe(viewLifecycleOwner) {
            updateRTKInfo()
        }
    }

    private fun updateRTKInfo() {
        rtkMsgBuilder.apply {
            setLength(0)
            append("CurrentRTKState:").append(rtkVM.currentRTKState.value).append("\n")
            append("CurrentRTKErrorMsg:").append(rtkVM.currentRTKErrorMsg.value).append("\n")
            append("CurrentQxNetworkCoordinateSystem:").append(rtkVM.currentQxNetworkCoordinateSystem.value)
                .append("\n")
            append("CurrentCustomNetworkRTKSettings:").append(rtkVM.currentCustomNetworkRTKSettings.value)
                .append("\n")
        }
        text_network_rtk_info.text = rtkMsgBuilder.toString()
    }
}
