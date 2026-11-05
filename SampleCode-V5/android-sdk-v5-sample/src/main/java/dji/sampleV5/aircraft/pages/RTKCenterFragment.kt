package dji.sampleV5.moduledrone.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.databinding.FragRtkCenterPageBinding
import dji.sampleV5.aircraft.models.RTKCenterVM
import dji.sampleV5.aircraft.pages.DJIFragment
import dji.v5.ux.util.ToastUtils
import dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource
import dji.sdk.keyvalue.value.rtkmobilestation.RTKLocation
import dji.sdk.keyvalue.value.rtkmobilestation.RTKSatelliteInfo
import dji.v5.common.utils.GpsUtils
import dji.v5.manager.aircraft.rtk.RTKLocationInfo
import dji.v5.manager.aircraft.rtk.RTKSystemState
import dji.v5.utils.common.LogUtils
import dji.v5.utils.common.StringUtils
import dji.v5.ux.core.extension.hide
import dji.v5.ux.core.extension.show


/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2022/3/19
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class RTKCenterFragment : DJIFragment(), CompoundButton.OnCheckedChangeListener,
    RadioGroup.OnCheckedChangeListener {
    private val TAG = LogUtils.getTag("RTKCenterFragment")

    companion object {
        const val KEY_IS_QX_RTK = "key_is_qx_rtk"
        const val KEY_IS_CMCC_RTK = "key_is_cmcc_rtk"
    }

    private var binding: FragRtkCenterPageBinding? = null
    private val rtkCenterVM: RTKCenterVM by activityViewModels()
    private var mIsUpdatingKeepStatus = false
    private var mIsUpdatingPrecisionStatus = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragRtkCenterPageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //初始化
        initListener()
        rtkCenterVM.addRTKLocationInfoListener()
        rtkCenterVM.addRTKSystemStateListener()
    }

    private fun initListener() {
        binding?.tbRtkKeepStatusSwitch?.setOnCheckedChangeListener(this)
        binding?.tbPrecisionPreservationSwitch?.setOnCheckedChangeListener(this)
        binding?.rtkSourceRadioGroup?.setOnCheckedChangeListener(this)

        //处理打开相关RTK逻辑
        binding?.btOpenNetworkRtk?.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_open_network_trk_pag, networkRTKParam)
        }
        binding?.btOpenRtkStation?.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_open_rtk_station_page)
        }
        binding?.btOpenCmccRtk?.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_open_network_trk_pag, networkRTKParam)
        }

        //RTK开启状态
        rtkCenterVM.aircraftRTKModuleEnabledLD.observe(viewLifecycleOwner) {
            updateRTKOpenSwitchStatus(it)
        }
        //RTK 位置信息
        rtkCenterVM.rtkLocationInfoLD.observe(viewLifecycleOwner) {
            showRTKInfo(it)
        }
        //RTK SystemState信息
        rtkCenterVM.rtkSystemStateLD.observe(viewLifecycleOwner) {
            showRTKSystemStateInfo(it)
        }
        //RTK精度维持
        rtkCenterVM.rtkAccuracyMaintainLD.observe(viewLifecycleOwner) {
            updateRtkAccuracyMaintainStatus(it)
        }
    }

    private fun updateRtkAccuracyMaintainStatus(status: Boolean?) {
        if (status == true) {
            binding?.tvRtkPrecisionPreservationHintInfo?.text = StringUtils.getResStr(R.string.tv_rtk_precision_preservation_turn_on)
        } else {
            binding?.tvRtkPrecisionPreservationHintInfo?.text = StringUtils.getResStr(R.string.tv_rtk_precision_preservation_turn_off)
        }
        LogUtils.i(TAG, "精度保持状态$status")
        updateRTKMaintainAccuracy(status)
    }

    private fun showRTKSystemStateInfo(rtkSystemState: RTKSystemState?) {
        rtkSystemState?.run {
            binding?.tvRtkHeadInfo?.text = if (rtkHealthy) {
                "healthy"
            } else {
                "unhealthy"
            }

            binding?.tvRtkErrorInfo?.text = error?.toString()
            //展示卫星数
            showSatelliteInfo(satelliteInfo)
            //更新RTK服务类型
            updateRTKUI(rtkReferenceStationSource)
            //更新飞控和机身的RTK是否正常连接
            updateRTKOpenSwitchStatus(isRTKEnabled)
            //rtk精度保持是否开启
            updateRtkAccuracyMaintainStatus(rtkMaintainAccuracyEnabled)

        }
    }

    private fun showRTKInfo(rtkLocationInfo: RTKLocationInfo?) {
        rtkLocationInfo?.run {
            binding?.tvTrkLocationStrategy?.text = rtkLocation?.positioningSolution?.name
            binding?.tvRtkStationPositionInfo?.text = rtkLocation?.baseStationLocation?.toString()
            binding?.tvRtkMobilePositionInfo?.text = rtkLocation?.mobileStationLocation?.toString()
            binding?.tvRtkPositionStdDistanceInfo?.text = getRTKLocationDistance(rtkLocation)?.toString()
            binding?.tvRtkStdPositionInfo?.text = "stdLongitude:${rtkLocation?.stdLongitude}" +
                    ",stdLatitude:${rtkLocation?.stdLatitude}" +
                    ",stdAltitude=${rtkLocation?.stdAltitude}"
            binding?.tvRtkHeadInfo?.text = rtkHeading?.toString()
            binding?.tvRtkRealHeadInfo?.text = realHeading?.toString()
            binding?.tvRtkRealLocationInfo?.text = real3DLocation?.toString()
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

            binding?.tvRtkAntenna1Info?.text = mobileStationReceiver1Info
            binding?.tvRtkAntenna2Info?.text = mobileStationReceiver2Info
            binding?.tvRtkStationInfo?.text = baseStationReceiverInfo
        }
    }

    private fun getRTKLocationDistance(rtklocation: RTKLocation?): Double? {
        rtklocation?.run {
            baseStationLocation?.let { baseStationLocation ->
                mobileStationLocation?.let { mobileStationLocation ->
                    return GpsUtils.gps2m(
                        baseStationLocation.latitude, baseStationLocation.longitude, baseStationLocation.altitude,
                        mobileStationLocation.latitude, mobileStationLocation.longitude, mobileStationLocation.altitude
                    )
                }
            }
        }
        return null
    }


    //基站开启状态和精度维持Listener
    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        when (buttonView) {
            binding?.tbRtkKeepStatusSwitch -> {
                if (mIsUpdatingKeepStatus) {
                    return
                }
                mIsUpdatingKeepStatus = true
                rtkCenterVM.setAircraftRTKModuleEnabled(isChecked)

            }

            binding?.tbPrecisionPreservationSwitch -> {
                if (mIsUpdatingPrecisionStatus) {
                    //上次set之后还没拿到最新值，则不响应此次设置
                    binding?.tbPrecisionPreservationSwitch?.setOnCheckedChangeListener(null)
                    binding?.tbPrecisionPreservationSwitch?.isChecked = !isChecked
                    binding?.tbPrecisionPreservationSwitch?.setOnCheckedChangeListener(this)
                    return
                }
                mIsUpdatingPrecisionStatus = true
                rtkCenterVM.setRTKMaintainAccuracyEnabled(isChecked)
            }
        }
    }

    //RTK源切换
    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        val rtkReferenceStationSource = rtkCenterVM.rtkSystemStateLD.value?.rtkReferenceStationSource
        val selectRTKReferenceStationSource =
            when (checkedId) {
                R.id.btn_rtk_source_base_rtk -> {
                    RTKReferenceStationSource.BASE_STATION
                }

                R.id.btn_rtk_source_network -> {
                    RTKReferenceStationSource.CUSTOM_NETWORK_SERVICE
                }

                R.id.btn_rtk_source_qx -> {
                    RTKReferenceStationSource.QX_NETWORK_SERVICE

                }

                R.id.btn_rtk_source_cmcc_rtk -> {
                    RTKReferenceStationSource.NTRIP_NETWORK_SERVICE
                }

                else -> RTKReferenceStationSource.UNKNOWN
            }

        if (rtkReferenceStationSource != selectRTKReferenceStationSource) {
            LogUtils.i(TAG, "Turn on switch to ${selectRTKReferenceStationSource.name}")
            rtkCenterVM.setRTKReferenceStationSource(selectRTKReferenceStationSource)
            ToastUtils.showToast(StringUtils.getResStr(R.string.switch_rtk_type_tip))

        }
    }


    private fun updateRTKOpenSwitchStatus(isChecked: Boolean?) {
        binding?.tbRtkKeepStatusSwitch?.setOnCheckedChangeListener(null)
        binding?.tbRtkKeepStatusSwitch?.isChecked = isChecked ?: false
        binding?.tbRtkKeepStatusSwitch?.setOnCheckedChangeListener(this)
        binding?.rlRtkAll?.isVisible = isChecked ?: false
        mIsUpdatingKeepStatus = false

        if (isChecked == null || !isChecked) {
            binding?.tvRtkEnable?.text = "RTK is off"
        } else {
            binding?.tvRtkEnable?.text = "RTK is on"
        }
    }

    private fun updateRTKMaintainAccuracy(isChecked: Boolean?) {
        binding?.tbPrecisionPreservationSwitch?.setOnCheckedChangeListener(null)
        binding?.tbPrecisionPreservationSwitch?.isChecked = isChecked ?: false
        binding?.tbPrecisionPreservationSwitch?.setOnCheckedChangeListener(this)
        mIsUpdatingPrecisionStatus = false
    }

    //用于区分是哪个网络rtk
    private var networkRTKParam = Bundle()
    private fun updateRTKUI(rtkReferenceStationSource: RTKReferenceStationSource?) {
        var checkedRadioButton: RadioButton? = null
        when (rtkReferenceStationSource) {
            RTKReferenceStationSource.BASE_STATION -> {
                binding?.btOpenRtkStation?.show()
                binding?.btOpenNetworkRtk?.hide()
                binding?.btOpenCmccRtk?.hide()
                checkedRadioButton = binding?.btnRtkSourceBaseRtk
                networkRTKParam.putBoolean(KEY_IS_QX_RTK, false)
                networkRTKParam.putBoolean(KEY_IS_CMCC_RTK, false)

            }

            RTKReferenceStationSource.CUSTOM_NETWORK_SERVICE -> {
                binding?.btOpenNetworkRtk?.show()
                binding?.btOpenRtkStation?.hide()
                binding?.btOpenCmccRtk?.hide()
                checkedRadioButton = binding?.btnRtkSourceNetwork
                networkRTKParam.putBoolean(KEY_IS_QX_RTK, false)
                networkRTKParam.putBoolean(KEY_IS_CMCC_RTK, false)

            }

            RTKReferenceStationSource.QX_NETWORK_SERVICE -> {
                binding?.btOpenNetworkRtk?.show()
                binding?.btOpenRtkStation?.hide()
                binding?.btOpenCmccRtk?.hide()
                checkedRadioButton = binding?.btnRtkSourceQx
                networkRTKParam.putBoolean(KEY_IS_QX_RTK, true)
                networkRTKParam.putBoolean(KEY_IS_CMCC_RTK, false)

            }

            RTKReferenceStationSource.NTRIP_NETWORK_SERVICE -> {
                binding?.btOpenCmccRtk?.show()
                binding?.btOpenRtkStation?.hide()
                binding?.btOpenNetworkRtk?.hide()
                checkedRadioButton = binding?.btnRtkSourceCmccRtk
                networkRTKParam.putBoolean(KEY_IS_QX_RTK, false)
                networkRTKParam.putBoolean(KEY_IS_CMCC_RTK, true)
            }

            else -> {
                ToastUtils.showToast("Current rtk reference station source is:$rtkReferenceStationSource")
            }
        }

        checkedRadioButton?.let {
            binding?.rtkSourceRadioGroup?.setOnCheckedChangeListener(null)
            binding?.rtkSourceRadioGroup?.check(it.id)
            binding?.rtkSourceRadioGroup?.setOnCheckedChangeListener(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        rtkCenterVM.removeRTKLocationInfoListener()
        rtkCenterVM.removeRTKSystemStateListener()
    }
}