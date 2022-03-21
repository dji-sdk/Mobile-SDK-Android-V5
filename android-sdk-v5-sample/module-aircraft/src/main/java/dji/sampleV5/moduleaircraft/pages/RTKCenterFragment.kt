package dji.sampleV5.moduledrone.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import dji.sampleV5.moduleaircraft.R
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.sampleV5.modulecommon.util.ToastUtils
import dji.sampleV5.moduleaircraft.models.RTKCenterVM
import dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource
import dji.sdk.keyvalue.value.rtkmobilestation.RTKSatelliteInfo
import dji.v5.manager.aircraft.rtk.RTKLocationInfo
import dji.v5.manager.aircraft.rtk.RTKSystemState

import kotlinx.android.synthetic.main.frag_rtk_center_page.*

/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2022/3/19
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class RTKCenterFragment : DJIFragment() {
    private val rtkCenterVM: RTKCenterVM by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_rtk_center_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initListener()

        //只要打开RTK开关才会显示相关功能界面
        rtk_open_state_radio_group.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.btn_enable_rtk) {
                rl_rtk_all.visible()
                rtkCenterVM.setAircraftRTKModuleEnabled(true)
            } else {
                rl_rtk_all.gone()
                rtkCenterVM.setAircraftRTKModuleEnabled(false)
            }

            //每次开启或者关闭RTK模块后，都需要再次获取其开启状态
            rtkCenterVM.getAircraftRTKModuleEnabled()
        }
        //选择RTK服务类型并展示相关界面，默认选择基站RTK
        rtkCenterVM.setRTKReferenceStationSource(RTKReferenceStationSource.BASE_STATION)
        rtk_source_radio_group.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.btn_rtk_source_base_rtk -> {
                    //将打开对应入口的按钮进行显示或隐藏操作
                    bt_open_rtk_station.visible()
                    bt_open_network_rtk.gone()
                    rl_rtk_info_show.visible()
                    //显示基站RTK的信息
                    tv_rtk_station_info.visible()
                    tv_rtk_station.visible()
                    rtkCenterVM.setRTKReferenceStationSource(RTKReferenceStationSource.BASE_STATION)
                }
                R.id.btn_rtk_source_network -> {
                    //将打开对应入口的按钮进行显示或隐藏操作
                    bt_open_rtk_station.gone()
                    bt_open_network_rtk.visible()
                    rl_rtk_info_show.visible()
                    //隐藏基站RTK的信息
                    tv_rtk_station_info.gone()
                    tv_rtk_station.gone()

                    rtkCenterVM.setRTKReferenceStationSource(RTKReferenceStationSource.CUSTOM_NETWORK_SERVICE)
                }
                else -> {
                    //将网络RTK入口和基站RTK入口关闭
                    bt_open_rtk_station.gone()
                    bt_open_network_rtk.gone()
                    rl_rtk_info_show.gone()
                    //隐藏基站RTK的信息
                    tv_rtk_station_info.gone()
                    tv_rtk_station.gone()
                    ToastUtils.showToast("Qianxun RTK does not currently support")
                }
            }
        }

        //处理打开相关RTK逻辑
        bt_open_network_rtk.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_open_network_trk_pag)
        }
        bt_open_rtk_station.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_open_rtk_station_page)
        }

        //监听数据源
        rtkCenterVM.addRTKLocationInfoListener()
        rtkCenterVM.addRTKSystemStateListener()
        rtkCenterVM.getAircraftRTKModuleEnabled()

    }

    private fun initListener() {
        rtkCenterVM.setAircraftRTKModuleEnableLD.observe(viewLifecycleOwner, {
            it.isSuccess.processResult("Set successfully", "Set failed")
        })

        rtkCenterVM.getAircraftRTKModuleEnabledLD.observe(viewLifecycleOwner, {
            it.data?.processResult("RTK is on", "RTK is off", tv_rtk_enable)

        })
        rtkCenterVM.setRTKReferenceStationSourceLD.observe(viewLifecycleOwner, {
            it.isSuccess.processResult("Switch RTK service type successfully", "Switch RTK service type failed")
        })
        rtkCenterVM.rtkLocationInfoLD.observe(viewLifecycleOwner, {
            showRTKInfo(it.data)
        })
        rtkCenterVM.rtkSystemStateLD.observe(viewLifecycleOwner, {
            showRTKSystemStateInfo(it.data)
        })
    }

    private fun showRTKSystemStateInfo(rtkSystemState: RTKSystemState?) {
        rtkSystemState?.run {
            rtkConnected?.let {
                tv_tv_rtk_connect_info.text = if (rtkConnected) {
                    "Connected"
                } else {
                    "Disconnected"
                }
            }
            rtkHealthy?.let {
                tv_rtk_healthy_info.text = if (rtkHealthy) {
                    "healthy"
                } else {
                    "unhealthy"
                }
            }
            tv_rtk_error_info.text = error?.toString()
            //展示卫星数
            showSatelliteInfo(satelliteInfo)
        }
    }

    private fun showRTKInfo(rtkLocationInfo: RTKLocationInfo?) {
        rtkLocationInfo?.run {
            tv_trk_location_strategy.text = rtkLocation?.positioningSolution?.name
            tv_rtk_station_position_info.text = rtkLocation?.baseStationLocation?.toString()
            tv_rtk_mobile_position_info.text = rtkLocation?.mobileStationLocation?.toString()
            tv_rtk_std_position_info.text = "stdLongitude:${rtkLocation?.stdLongitude}" +
                    ",stdLatitude:${rtkLocation?.stdLatitude}" +
                    ",stdAltitude=${rtkLocation?.stdAltitude}"

            tv_rtk_head_info.text = rtkHeading?.toString()
            tv_rtk_real_head_info.text = realHeading?.toString()
            tv_rtk_real_location_info.text = real3DLocation?.toString()


        }

    }

    private fun showSatelliteInfo(rtkSatelliteInfo: RTKSatelliteInfo?) {
        rtkSatelliteInfo?.run {
            var baseStationReceiverInfo = ""
            var mobileStationReceiver2Info = ""
            var mobileStationReceiver1Info = ""
            for (receiver1 in rtkSatelliteInfo.mobileStationReceiver1Info) {
                mobileStationReceiver1Info += "${receiver1.type.name}:${receiver1.count};"
            }
            for (receiver2 in rtkSatelliteInfo.mobileStationReceiver2Info) {
                mobileStationReceiver2Info += "${receiver2.type.name}:${receiver2.count};"
            }
            for (receiver3 in rtkSatelliteInfo.baseStationReceiverInfo) {
                baseStationReceiverInfo += "${receiver3.type.name}:${receiver3.count};"
            }

            tv_rtk_antenna_1_info.text = mobileStationReceiver1Info
            tv_rtk_antenna_2_info.text = mobileStationReceiver2Info
            tv_rtk_station_info.text = baseStationReceiverInfo

        }
    }


    private fun View.visible() {
        this.visibility = View.VISIBLE
    }

    private fun View.gone() {
        this.visibility = View.GONE
    }


    private fun Boolean.processResult(
        positiveMsg: String,
        negativeMsg: String,
        textView: TextView? = null
    ) {
        textView?.run {
            text = if (this@processResult) {
                positiveMsg
            } else {
                negativeMsg
            }
            return@processResult
        }
        if (this) {
            ToastUtils.showToast(positiveMsg)
        } else {
            ToastUtils.showToast(negativeMsg)
        }
    }

    override fun onStop() {
        super.onStop()
        rtkCenterVM.clearAllRTKLocationInfoListener()
        rtkCenterVM.clearAllRTKSystemStateListener()
    }

}