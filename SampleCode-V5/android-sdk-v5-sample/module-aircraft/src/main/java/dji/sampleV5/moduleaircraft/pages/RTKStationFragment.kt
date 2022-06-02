package dji.sampleV5.moduleaircraft.pages

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.sampleV5.modulecommon.util.ToastUtils
import dji.sampleV5.moduleaircraft.R
import dji.sampleV5.moduleaircraft.data.DJIRTKBaseStationConnectInfo
import dji.sampleV5.moduleaircraft.data.RtkStationScanAdapter
import dji.sampleV5.moduleaircraft.models.RTKStationVM
import dji.sampleV5.modulecommon.keyvalue.KeyItemActionListener
import dji.sampleV5.modulecommon.keyvalue.KeyValueDialogUtil
import dji.sdk.keyvalue.value.common.LocationCoordinate3D
import dji.sdk.keyvalue.value.rtkbasestation.RTKBaseStationResetPasswordInfo
import dji.sdk.keyvalue.value.rtkbasestation.RTKStationConnetState
import dji.sdk.keyvalue.value.rtkbasestation.RTKStationInfo
import dji.v5.manager.aircraft.rtk.station.ConnectedRTKStationInfo
import dji.v5.utils.common.LogUtils
import dji.v5.ux.core.extension.hide
import dji.v5.ux.core.extension.show
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
    //这里只能用fragment级别的viewModel，否则会出现粘性事件
    private val rtkStationVM: RTKStationVM by viewModels()
    private lateinit var rtkStationScanAdapter: RtkStationScanAdapter
    private var loginStatus = false
    private var connectState = RTKStationConnetState.UNKNOWN

    companion object {
        private const val STATION_PASSWORD_LENGTH = 6//密码长度必须为6
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

        rtkStationVM.startSearchStationLD.observe(viewLifecycleOwner
        ) {
            it.isSuccess.processResult(
                "Searing rtk station...",
                "Search station  fail：${it.msg}"
            )
        }

        rtkStationVM.stopSearchStationLD.observe(viewLifecycleOwner) {
            it.isSuccess.processResult(
                "Stop search station  success",
                "Stop search station  fail：${it.msg}!!!"
            )
        }


        rtkStationVM.stationListLD.observe(viewLifecycleOwner) { result ->
            result.isSuccess.processResult("Listener stationList fail:${result.msg}") {
                handleStationRTKList(result.data)
            }

        }

        rtkStationVM.connectRTKStationLD.observe(viewLifecycleOwner) { result ->
            result.isSuccess.processResult(
                "Station connecting...",
                "Station connect fail:${result.msg}"
            )

        }

        rtkStationVM.appStationConnectStatusLD.observe(viewLifecycleOwner) { result ->
            result.isSuccess.processResult("App station connect status fail:${result.msg}") {
                handleConnectStatus(result.data)
            }
        }

        rtkStationVM.appStationConnectedInfoLD.observe(viewLifecycleOwner) { result ->
            result.isSuccess.processResult("Listener station connected information fail:${result.msg}") {
                showConnectStationInfo(result.data)
                handleReconnectedStationInfo(result.data)
            }
        }

        rtkStationVM.loginLD.observe(viewLifecycleOwner) { result ->
            if (result.isSuccess) {
                loginStatus = true
                ToastUtils.showToast("Login success")
            } else {
                loginStatus = false
                ToastUtils.showToast("Login fail:${result.msg}")
            }
        }

        rtkStationVM.setStationPositionLD.observe(viewLifecycleOwner) { result ->
            result.isSuccess.processResult(
                "Set station position success",
                "Set station position fail:${result.msg}"
            )
        }
        rtkStationVM.resetStationPositionLD.observe(viewLifecycleOwner) { result ->
            result.isSuccess.processResult(
                "Reset station position success",
                "Reset station position fail:${result.msg}"
            )
        }
        rtkStationVM.resetStationPasswordLD.observe(viewLifecycleOwner) { result ->
            result.isSuccess.processResult(
                "Reset station password success",
                "Reset station password fail:${result.msg}"
            )
        }

        rtkStationVM.setStationNameLD.observe(viewLifecycleOwner) { result ->
            result.isSuccess.processResult(
                "Set station name success",
                "Set station name fail:${result.msg}"
            )

        }
        //清除数据
        showConnectStationInfo(ConnectedRTKStationInfo())
    }

    override fun onResume() {
        loginStatus = false
        super.onResume()
    }


    private fun initListener() {
        //一进入界面就开始Listen基站RTK连接情况
        rtkStationVM.addSearchRTKStationListener()
        rtkStationVM.addStationConnectStatusListener()
        rtkStationVM.addConnectedRTKStationInfoListener()
        rtkStationScanAdapter.setOnItemClickListener(this)

        btn_start_search_station.setOnClickListener {
            //初始化UI
            handleStationRTKList(null)
            tv_station_list_tip.hide()
            rtkStationVM.startSearchStation()
        }

        btn_stop_search_station.setOnClickListener {
            rtkStationVM.stopSearchStation()
        }

        btn_login.setOnClickListener {
            //登录操作只允许在基站连接之后才可以进行
            if (connectState != RTKStationConnetState.CONNECTED) {
                ToastUtils.showToast("Please connect to the base station first！")
                return@setOnClickListener
            }
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
                showDialog("重置密码，注意密码必须由0-9的6个数字组成的字符串，比如“012345”", rtkBaseStationResetPasswordInfo.toString()) {
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

        rtkStationVM.getStationPositionLD.observe(viewLifecycleOwner, {
            ToastUtils.showToast(it.data.toString())
        })


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
            LogUtils.d(TAG, "clear stationList")
            list?.let {
                for (i in it) {
                    LogUtils.d(TAG, "stationName=${i.rtkStationName}+,signalLevel=${i.signalLevel}")
                    stationList.add(i)
                    tv_station_list_tip.show()
                }
            }
            rtkStationScanAdapter.notifyDataSetChanged()
        }
    }

    /**
     * 选中某个基站,注意这里selectedRTKStationConnectInfo不能初始化为null
     */
    private var selectedRTKStationConnectInfo: DJIRTKBaseStationConnectInfo = DJIRTKBaseStationConnectInfo()
    override fun onItemClick(view: View?, position: Int) {
        selectedRTKStationConnectInfo = stationList[position]
        //连接某一个基站时，将其他基站的连接状态重置为空闲，因为一次只能连接一个基站
        for (stationInfo in stationList) {
            stationInfo.connectStatus = RTKStationConnetState.IDLE
        }
        selectedRTKStationConnectInfo.run {
            rtkStationVM.startConnectToRTKStation(baseStationId)
            selectedRTKStationConnectInfo.refresh(RTKStationConnetState.CONNECTING)
            LogUtils.d(TAG, "click and connecting rtk")
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
            LogUtils.d(TAG, "connectState=$connectState")
            this.connectStatus = it
        }
        rtkStationScanAdapter.notifyDataSetChanged()

    }

    private fun handleConnectStatus(rtkBaseStationConnectState: RTKStationConnetState?) {
        if (rtkBaseStationConnectState == null) {
            return
        }
        LogUtils.d(TAG, rtkBaseStationConnectState.name)
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
        connectState = rtkBaseStationConnectState
        //返回连接状态，更新UI
        selectedRTKStationConnectInfo.refresh(rtkBaseStationConnectState)

    }


    private var lastStationName = ""
    private fun showConnectStationInfo(infoConnected: ConnectedRTKStationInfo?) {
        infoConnected?.run {
            tv_station_name_info.text = "$stationName"
            tv_station_id_info.text = "$stationId"
            tv_station_signal_level_info.text = "$signalLevel"

            tv_station_battery_current_info.text = "$batteryCurrent"
            tv_station_battery_voltage_info.text = "$batteryVoltage"
            tv_station_battery_temperature_info.text = "$batteryTemperature"
            tv_station_battery_capacity_percent_info.text = "$batteryCapacityPercent"

            //初始化名字
            if (TextUtils.isEmpty(lastStationName)) {
                lastStationName = stationName
                //这里加上非空判断，是因为断开连接后stationName也是为空，排除这种情况
            } else if (lastStationName != stationName && !TextUtils.isEmpty(stationName)) {
                //修改名字后刷新UI
                selectedRTKStationConnectInfo.rtkStationName = stationName
                lastStationName = stationName
                rtkStationScanAdapter.notifyDataSetChanged()

            }
        }
    }


    private fun handleReconnectedStationInfo(infoConnected: ConnectedRTKStationInfo?) {
        infoConnected?.run {
            //第一次连接过基站后，再次重启飞机或者重启App固件会帮忙自动连接基站。这里就是为了构建自动重连的基站信息
            if (selectedRTKStationConnectInfo.baseStationId == 0) {
                LogUtils.i(TAG, "RTK Station has reconnected")
                selectedRTKStationConnectInfo = DJIRTKBaseStationConnectInfo(stationId, signalLevel, stationName, RTKStationConnetState.CONNECTED)
                handleStationRTKList(arrayListOf(selectedRTKStationConnectInfo))
                connectState = RTKStationConnetState.CONNECTED;
            }
        }
    }

    private fun Boolean.processResult(positiveMsg: String, negativeMsg: String) {
        if (this) {
            ToastUtils.showToast(positiveMsg)
        } else {
            ToastUtils.showToast(negativeMsg)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        selectedRTKStationConnectInfo = DJIRTKBaseStationConnectInfo()
        rtkStationVM.clearAllStationConnectStatusListener()
        rtkStationVM.clearAllSearchRTKStationListener()
        rtkStationVM.clearAllConnectedRTKStationInfoListener()

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