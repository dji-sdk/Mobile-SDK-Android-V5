package dji.sampleV5.moduleaircraft.pages

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.sampleV5.modulecommon.util.ToastUtils
import dji.sampleV5.moduleaircraft.R
import dji.sampleV5.moduleaircraft.data.DJIRTKBaseStationConnectInfo
import dji.sampleV5.moduleaircraft.data.RtkStationScanAdapter
import dji.sampleV5.moduleaircraft.models.RTKCenterVM
import dji.sampleV5.moduleaircraft.models.RTKStationVM
import dji.sampleV5.modulecommon.keyvalue.KeyItemActionListener
import dji.sampleV5.modulecommon.keyvalue.KeyValueDialogUtil
import dji.sdk.keyvalue.value.common.LocationCoordinate3D
import dji.sdk.keyvalue.value.rtkbasestation.RTKBaseStationResetPasswordInfo
import dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource
import dji.sdk.keyvalue.value.rtkbasestation.RTKStationConnetState
import dji.sdk.keyvalue.value.rtkbasestation.RTKStationInfo
import dji.v5.manager.aircraft.rtk.station.ConnectedRTKStationInfo
import dji.v5.utils.common.LogUtils
import kotlinx.android.synthetic.main.frag_station_rtk_page.*
import kotlin.collections.ArrayList

/**
 * Description :基站TRK操作界面
 *
 * @author: Byte.Cai
 *  date : 2022/3/4
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class RTKStationFragment : DJIFragment(), RtkStationScanAdapter.OnItemClickListener {
    private val stationList = ArrayList<DJIRTKBaseStationConnectInfo>()
    private val rtkCenterVM: RTKCenterVM by activityViewModels()
    private val rtkStationVM: RTKStationVM by activityViewModels()
    private lateinit var rtkStationScanAdapter: RtkStationScanAdapter
    var loginStatus = false

    companion object {
        private const val STATION_PASSWORD_LENGTH = 6//密码长度必须为6
        private const val STATION_PASSWORD = "135790"
        private const val STATION_NEW_PASSWORD = "135790"
        private const val STATION_NAME = "Test"//基站名长度必须为4
        private const val TAG = "StationRTKFragment"
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_station_rtk_page, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initListener()
    }


    private fun initView() {
        LogUtils.d(TAG, "initView")
        //初始化stationTRTK列表
        val layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        rv_station_list.layoutManager = layoutManager
        rtkStationScanAdapter = RtkStationScanAdapter(requireContext(), stationList)
        rv_station_list.adapter = rtkStationScanAdapter
        rtkCenterVM.setRTKReferenceStationSource(RTKReferenceStationSource.BASE_STATION)

        rtkStationVM.startSearchStationLD.observe(viewLifecycleOwner,
            {
                it.isSuccess.processResult(
                    "Searing rtk station...",
                    "Search station  fail：${it.msg}"
                )
            })

        rtkStationVM.stopSearchStationLD.observe(viewLifecycleOwner, {
            it.isSuccess.processResult(
                "Stop search station  success",
                "Stop search station  fail：${it.msg}!!!"
            )
        })


        rtkStationVM.stationListLD.observe(viewLifecycleOwner, { result ->
            result.isSuccess.processResult("Listener stationList fail:${result.msg}") {
                handleStationRTKList(result.data)
            }

        })

        rtkStationVM.connectRTKStationLD.observe(viewLifecycleOwner, { result ->
            result.isSuccess.processResult(
                "Station connecting...",
                "Station connect fail:${result.msg}"
            )

        })

        rtkStationVM.appStationConnectStatusLD.observe(viewLifecycleOwner, { result ->
            result.isSuccess.processResult("App station connect status fail:${result.msg}") {
                handleConnectStatus(result.data)
            }
        })

        rtkStationVM.appStationConnectedInfoLD.observe(viewLifecycleOwner, { result ->
            result.isSuccess.processResult("Listener station connected information fail:${result.msg}") {
                showConnectStationInfo(result.data)

            }
        })

        rtkStationVM.loginLD.observe(viewLifecycleOwner, { result ->
            if (result.isSuccess) {
                loginStatus = true
                ToastUtils.showToast("Login success")
            } else {
                loginStatus = false
                ToastUtils.showToast("Login fail:${result.msg}")
            }
        })

        rtkStationVM.setStationPositionLD.observe(viewLifecycleOwner, { result ->
            result.isSuccess.processResult(
                "Set station position success",
                "Set station position fail:${result.msg}"
            )
        })
        rtkStationVM.resetStationPositionLD.observe(viewLifecycleOwner, { result ->
            result.isSuccess.processResult(
                "Reset station position success",
                "Reset station position fail:${result.msg}"
            )
        })
        rtkStationVM.resetStationPasswordLD.observe(viewLifecycleOwner, { result ->
            result.isSuccess.processResult(
                "Reset station password success",
                "Reset station password fail:${result.msg}"
            )
        })

        rtkStationVM.setStationNameLD.observe(viewLifecycleOwner, { result ->
            result.isSuccess.processResult(
                "Set station name success",
                "Set station name fail:${result.msg}"
            )

        })
    }

    override fun onResume() {
        loginStatus = false
        super.onResume()
    }


    private fun initListener() {
        //一进入界面就开始监听基站RTK连接情况h
        rtkStationVM.addSearchRTKStationListener()
        rtkStationVM.addStationConnectStatusListener()
        rtkStationVM.addConnectedRTKStationInfoListener()
        rtkStationScanAdapter.setOnItemClickListener(this)
        rtkStationVM.getRTKStationPosition()//尝试获取基站经纬度，因为有可能之前连接过，那么固件底层自己会帮忙重连

        btn_start_search_station.setOnClickListener {
            handleStationRTKList(null)
            rtkStationVM.startSearchStation()
        }

        btn_stop_search_station.setOnClickListener {
            handleStationRTKList(null)
            rtkStationVM.stopSearchStation()
        }

        btn_login.setOnClickListener {
            showDialog("输入密码，注意密码必须由0-9的6个数字组成的字符串，比如“012345”") {
                it?.run {
                    val password = trim()
                    if (!TextUtils.isEmpty(password) && password.length == STATION_PASSWORD_LENGTH) {
                        rtkStationVM.loginAsAdmin(password)
                    } else {
                        ToastUtils.showToast("The password length does not meet the requirements, the password length must be 6！！！")
                    }
                }
            }

        }
        btn_set_station_position.setOnClickListener {
            process {
                val locationCoordinate3D = LocationCoordinate3D(1.0, 1.0, 1.0)
                showDialog("输入基站坐标", locationCoordinate3D.toString()) {
                    it?.let {
                        rtkStationVM.setRTKStationPosition(LocationCoordinate3D.fromJson(it))
                    }
                }
            }
        }
        btn_get_station_position.setOnClickListener {
            rtkStationVM.getRTKStationPosition()
        }
        btn_reset_station_position.setOnClickListener {
            process {
                rtkStationVM.resetRTKStationPosition()
            }
        }
        btn_reset_station_password.setOnClickListener {
            process {
                val rtkBaseStationResetPasswordInfo =
                    RTKBaseStationResetPasswordInfo("111111", "111111")
                showDialog("重置密码", rtkBaseStationResetPasswordInfo.toString()) {
                    it?.let {
                        val resetPasswordInfo = it.trim()
                        rtkStationVM.resetStationPassword(RTKBaseStationResetPasswordInfo.fromJson(resetPasswordInfo))
                    }

                }
            }
        }
        btn_set_station_name.setOnClickListener {
            process {
                showDialog("修改基站名称,注意：基站名字UTF-8下只取4个字节，比如设置名字为“abcdef”，最后效果是“abcd”") {
                    it?.let {
                        val stationName = it.trim()
                        rtkStationVM.setStationName(stationName)
                    }
                }
            }
        }
    }

    private fun checkNeedUpdateUI(list: List<RTKStationInfo>?): Boolean {
        if (list?.size != stationList.size) {
            return true
        }
        for (i in stationList.indices) {
            if (stationList[i].toString() != list[i].toString()) {
                return true
            }
        }
        return false
    }

    private fun handleStationRTKList(list: List<DJIRTKBaseStationConnectInfo>?) {
        //过滤重复的数据，防止界面重新刷新
        if (checkNeedUpdateUI(list)) {
            stationList.clear()
            list?.let {
                for (i in it) {
                    LogUtils.d(TAG, "stationName=${i.rtkStationName}+,signalLevel=${i.signalLevel}")
                    stationList.add(i)
                }
            }
            rtkStationScanAdapter.notifyDataSetChanged()

        }
    }

    /**
     * 选中某个基站
     */
    private var selectedRTKStationConnectInfo: DJIRTKBaseStationConnectInfo? = null
    override fun onItemClick(view: View?, position: Int) {
        selectedRTKStationConnectInfo = stationList[position]
        selectedRTKStationConnectInfo?.let {
            rtkStationVM.startConnectToRTKStation(it.baseStationId)
            selectedRTKStationConnectInfo?.refresh(RTKStationConnetState.CONNECTING)
        }

    }


    private inline fun process(block: () -> Unit) {
        if (loginStatus) {
            block()
        } else {
            ToastUtils.showToast("You need to login in before operation！！！")
        }
    }

    private fun DJIRTKBaseStationConnectInfo.refresh(connectState: RTKStationConnetState?) {
        connectState?.let {
            this.connectStatus = it
            rtkStationScanAdapter.notifyDataSetChanged()
        }

    }

    private fun handleConnectStatus(rtkBaseStationConnectState: RTKStationConnetState?) {
        when (rtkBaseStationConnectState) {
            RTKStationConnetState.CONNECTED -> {
                ToastUtils.showToast("Station has connected")
            }
            RTKStationConnetState.DISCONNECTED -> {
                ToastUtils.showToast("Station has disconnected")
            }
            else -> {
                LogUtils.d(TAG, "Current station status is $rtkBaseStationConnectState")
            }
        }
        //返回连接状态，更新UI
        selectedRTKStationConnectInfo?.refresh(rtkBaseStationConnectState)

    }

    override fun onStop() {
        super.onStop()
        rtkStationVM.removeSearchRTKStationListener()
        rtkStationVM.removeStationConnectStatusListener()
        rtkStationVM.removeConnectedRTKStationInfoListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        rtkStationVM.clearAllConnectedRTKStationInfoListener()
        rtkStationVM.clearAllSearchRTKStationListener()
        rtkStationVM.clearAllStationConnectStatusListener()
    }


    private fun showConnectStationInfo(infoConnected: ConnectedRTKStationInfo?) {
        infoConnected?.run {
            tv_station_name_info.text = "$stationName"
            tv_station_id_info.text = "$stationId"
            tv_station_signal_level_info.text = "$signalLevel"

            tv_station_battery_current_info.text = "$batteryCurrent"
            tv_station_battery_voltage_info.text = "$batteryVoltage"
            tv_station_battery_temperature_info.text = "$batteryTemperature"
            tv_station_battery_capacity_percent_info.text = "$batteryCapacityPercent"
        }
    }

    private fun Boolean.processResult(positiveMsg: String, negativeMsg: String) {
        if (this) {
            ToastUtils.showToast(positiveMsg)
        } else {
            ToastUtils.showToast(negativeMsg)
        }
    }

    private fun Boolean.processResult(negativeMsg: String, block: () -> Unit) {
        if (this) {
            block()
        } else {
            ToastUtils.showToast(negativeMsg)
        }
    }

    private fun showDialog(title: String, msg: String = "", callback: KeyItemActionListener<String>) {
        KeyValueDialogUtil.showInputDialog(
            activity, title, msg, "", true
        ) {
            callback.actionChange(it)
        }
    }

}