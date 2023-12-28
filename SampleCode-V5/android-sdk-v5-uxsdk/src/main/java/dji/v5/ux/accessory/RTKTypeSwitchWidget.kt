package dji.v5.ux.accessory

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import dji.rtk.CoordinateSystem
import dji.sdk.keyvalue.value.rtkbasestation.RTKCustomNetworkSetting
import dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.rtk.RTKCenter
import dji.v5.manager.areacode.AreaCodeManager
import dji.v5.utils.common.LogUtils
import dji.v5.utils.common.StringUtils
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.getString
import dji.v5.ux.core.extension.hide
import dji.v5.ux.core.extension.isFastClick
import dji.v5.ux.core.extension.show
import dji.v5.ux.core.util.ViewUtil
import dji.v5.ux.util.RtkSettingWatcher

/**
 * Description :基站启动页
 *
 * @author: Byte.Cai
 *  date : 2022/8/1
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
private const val TAG = "RTKTypeSwitchWidget"

open class RTKTypeSwitchWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayoutWidget<Boolean>(context, attrs, defStyleAttr), RtkSettingWatcher.OnEditTextEmptyChangedListener {
    private val rtkTypeCell: DescSpinnerCell = findViewById(R.id.cell_rtk_type)
    private val coordinateSystemCell: DescSpinnerCell = findViewById(R.id.cell_coordinate_system)
    private val edHost: TextView = findViewById(R.id.net_rtk_ntrip_host)
    private val edPort: TextView = findViewById(R.id.net_rtk_ntrip_port)
    private val edUser: TextView = findViewById(R.id.net_rtk_ntrip_user)
    private val edMountPoint: TextView = findViewById(R.id.net_rtk_ntrip_mountpoint)
    private val edPassword: TextView = findViewById(R.id.net_rtk_ntrip_pwd)
    private val btSaveRtkInfo: Button = findViewById(R.id.btn_set_net_rtk_info)
    private val customSetting: LinearLayout = findViewById(R.id.ll_rtk_custom_detail_view)

    private var rtkSourceList: List<RTKReferenceStationSource> = ArrayList()
    private var coordinateSystemList: List<CoordinateSystem> = arrayListOf()
    private var isMotorsOn = false
    private var currentRTKSource: RTKReferenceStationSource = RTKReferenceStationSource.UNKNOWN
    private val textWatcher = RtkSettingWatcher(this)
    private val INITIAL_INDEX: Int = -1
    private var lastSelectedRTKTypeIndex: Int = INITIAL_INDEX
    private var lastSelectedCoordinateSystemIndex: Int = INITIAL_INDEX

    private val widgetModel by lazy {
        RTKTypeSwitchWidgetModel(
            DJISDKModel.getInstance(),
            ObservableInMemoryKeyedStore.getInstance(),
            AreaCodeManager.getInstance(), RTKCenter.getInstance())
    }

    private val rtkTypeSelectListener = object : DescSpinnerCell.OnItemSelectedListener {
        override fun onItemSelected(position: Int) {
            if (position == lastSelectedRTKTypeIndex) {
                return
            }
            // B控不可以打开网络RTK。
            if (RTKStartServiceHelper.isChannelB() && RTKStartServiceHelper.isNetworkRTK(rtkSourceList[position])) {
                //回滚之前的选择,并提示用户
                rtkTypeCell.select(lastSelectedRTKTypeIndex)
                Toast.makeText(getContext(), getTip(position), Toast.LENGTH_SHORT).show()
                return
            }
            //电机起转后,并且选择过服务类型，则不允许再次切换RTK类型
            if (isMotorsOn && lastSelectedRTKTypeIndex != -1) {
                rtkTypeCell.select(lastSelectedRTKTypeIndex)
                val tip = StringUtils.getResStr(R.string.uxsdk_rtk_setting_menu_esc_beeping_tip)
                Toast.makeText(getContext(), tip, Toast.LENGTH_SHORT).show()
                return
            }
            setRTKType(position)
        }

    }

    private val coordinateSelectListener = object : DescSpinnerCell.OnItemSelectedListener {
        override fun onItemSelected(position: Int) {
            if (position == lastSelectedCoordinateSystemIndex || coordinateSystemList.isEmpty() || position >= coordinateSystemList.size || position < 0) {
                return
            }
            lastSelectedCoordinateSystemIndex = position
            val coordinate = coordinateSystemList[position]
            RTKUtil.saveRTKCoordinateSystem(currentRTKSource, coordinate)
            LogUtils.i(TAG, "select:$coordinate, reStartRtkService now!(Thread.currentThread().name${Thread.currentThread().name})")
            RTKStartServiceHelper.startRtkService(true)
        }
    }


    init {
        edHost.addTextChangedListener(textWatcher)
        edUser.addTextChangedListener(textWatcher)
        edPassword.addTextChangedListener(textWatcher)
        edMountPoint.addTextChangedListener(textWatcher)
        edPort.addTextChangedListener(textWatcher)
        btSaveRtkInfo.setOnClickListener {
            if (!btSaveRtkInfo.isFastClick()) {
                saveRtkCustomUserInfo()
            }
        }
        //读取默认的配置，并启动RTK
        LogUtils.i(TAG, "RTKTypeSwitchWidget init,startRtkService now!(Thread.currentThread().name=${Thread.currentThread().name})")
        RTKStartServiceHelper.startRtkService()
    }


    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_widget_rtk_type_switch, this)
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
        }
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
        edHost.removeTextChangedListener(textWatcher)
        edUser.removeTextChangedListener(textWatcher)
        edMountPoint.removeTextChangedListener(textWatcher)
        edPort.removeTextChangedListener(textWatcher)
        edPassword.removeTextChangedListener(textWatcher)
        super.onDetachedFromWindow()
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.isMotorsOn.subscribe {
            isMotorsOn = it
        })
        addReaction(widgetModel.rtkSource.observeOn(SchedulerProvider.ui()).subscribe {
            LogUtils.i(TAG, "currentRTKSource=$it")
            //获取RTK服务类型，并切换UI
            currentRTKSource = it
            updateRTKView()
            initDefaultNetRtkUI()
        })
        addReaction(widgetModel.supportReferenceStationList.observeOn(SchedulerProvider.ui()).subscribe {
            //更新支持的RTK列表
            if (it.isNotEmpty() && !rtkSourceList.containsAll(it)) {
                LogUtils.i(TAG, "supportReferenceStationList=$it")
                rtkSourceList = it
                val referenceStationSourceNames = getReferenceStationSourceNames(it)
                rtkTypeCell.setEntries(referenceStationSourceNames)
                rtkTypeCell.addOnItemSelectedListener(rtkTypeSelectListener)
                initDefaultNetRtkUI()
            }
        })

        addReaction(widgetModel.coordinateSystemList.observeOn(SchedulerProvider.ui()).subscribe {
            LogUtils.i(TAG, "coordinateSystemList=$it")
            //更新坐标系
            if (it.isNotEmpty() && !coordinateSystemList.containsAll(it)) {
                LogUtils.i(TAG, "coordinateSystemList=$it")
                coordinateSystemList = it
                coordinateSystemCell.addOnItemSelectedListener(null)
                coordinateSystemCell.setEntries(getCoordinateSystemName(it))
                coordinateSystemCell.addOnItemSelectedListener(coordinateSelectListener)
                initDefaultNetRtkUI()
            }
        })

        //进入App后初始化用户上次选择
        initDefaultCustomSetting()

    }

    override fun getIdealDimensionRatioString(): String? {
        return getString(R.string.uxsdk_widget_rtk_keep_status_ratio)
    }

    private fun setRTKType(position: Int) {
        if (rtkSourceList.isEmpty() || position >= rtkSourceList.size || position < 0) {
            return
        }
        val rtkSource: RTKReferenceStationSource = rtkSourceList[position]
        LogUtils.i(TAG, "selected $rtkSource")
        rtkTypeCell.isEnabled = false

        RTKCenter.getInstance().setRTKReferenceStationSource(rtkSource, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                lastSelectedRTKTypeIndex = position
                rtkTypeCell.isEnabled = true
            }

            override fun onFailure(error: IDJIError) {
                rtkTypeCell.isEnabled = true
                //切换RTK服务类型失败，回滚到上次选择
                for ((index, source) in rtkSourceList.withIndex()) {
                    if (source == currentRTKSource) {
                        rtkTypeCell.select(index)
                    }
                }
            }

        })
    }


    private fun updateRTKView() {
        val rtkSwitchDec = StringUtils.getResStr(R.string.uxsdk_rtk_setting_menu_switch_des_info)
        var rtkSwitchDecDetail = ""
        when (currentRTKSource) {
            RTKReferenceStationSource.BASE_STATION -> {
                customSetting.hide()
                coordinateSystemCell.hide()
                rtkSwitchDecDetail = StringUtils.getResStr(R.string.uxsdk_rtk_setting_menu_base_gps_input_desc)
            }
            RTKReferenceStationSource.QX_NETWORK_SERVICE,
            RTKReferenceStationSource.NTRIP_NETWORK_SERVICE,
            -> {
                if (!RTKStartServiceHelper.isChannelB()) {
                    customSetting.hide()
                    coordinateSystemCell.show()
                } else {
                    customSetting.hide()
                    coordinateSystemCell.hide()
                }
                rtkSwitchDecDetail = StringUtils.getResStr(R.string.uxsdk_rtk_setting_menu_station_net_rtk_desc)
            }
            RTKReferenceStationSource.CUSTOM_NETWORK_SERVICE -> {
                if (!RTKStartServiceHelper.isChannelB()) {
                    customSetting.show()
                    coordinateSystemCell.hide()
                } else {
                    customSetting.hide()
                    coordinateSystemCell.hide()
                }
                rtkSwitchDecDetail = StringUtils.getResStr(R.string.uxsdk_rtk_setting_menu_station_net_rtk_desc)
            }
            else -> {
                customSetting.hide()
                coordinateSystemCell.hide()
            }
        }
        if (TextUtils.isEmpty(rtkSwitchDecDetail)) {
            rtkTypeCell.setSDescText(rtkSwitchDec)
        } else {
            rtkTypeCell.setSDescText(rtkSwitchDec + "\n" + rtkSwitchDecDetail)
        }
    }

    private fun getReferenceStationSourceNames(list: List<RTKReferenceStationSource>): List<String> {
        return list.map {
            val res = when (it) {
                RTKReferenceStationSource.BASE_STATION ->
                    R.string.uxsdk_rtk_setting_menu_type_rtk_station
                RTKReferenceStationSource.CUSTOM_NETWORK_SERVICE ->
                    R.string.uxsdk_rtk_setting_menu_type_custom_rtk
                RTKReferenceStationSource.QX_NETWORK_SERVICE ->
                    R.string.uxsdk_rtk_setting_menu_type_qx_rtk
                RTKReferenceStationSource.NTRIP_NETWORK_SERVICE ->
                    R.string.uxsdk_rtk_setting_menu_type_cmcc_rtk
                else ->
                    R.string.uxsdk_rtk_setting_menu_type_rtk_none
            }
            StringUtils.getResStr(res)
        }

    }

    private fun getCoordinateSystemName(list: List<CoordinateSystem>): List<String> {
        return list.map {
            val coordinateName: String = when (it) {
                CoordinateSystem.CGCS2000 ->
                    CoordinateSystem.CGCS2000.name
                CoordinateSystem.WGS84 ->
                    CoordinateSystem.WGS84.name
                else -> {
                    LogUtils.e(TAG, "UnSupport CoordinateSystem:$it")
                    CoordinateSystem.UNKNOWN.name
                }
            }
            coordinateName
        }
    }

    private fun getTip(position: Int): String {
        return if (rtkSourceList[position] == RTKReferenceStationSource.QX_NETWORK_SERVICE || rtkSourceList[position] == RTKReferenceStationSource.NTRIP_NETWORK_SERVICE) {
            StringUtils.getResStr(R.string.uxsdk_rtk_channel_b_not_support_net_rtk)
        } else {
            StringUtils.getResStr(R.string.uxsdk_rtk_channel_b_not_support_net_custom_rtk)
        }
    }

    override fun `isTextEmptyChanged`() {
        //检查RTK设置项的填写状态，并更新设置button的状态
        var shouldEnableButton = true
        if (TextUtils.isEmpty(edUser.text)) {
            shouldEnableButton = false
        }
        if (TextUtils.isEmpty(edHost.text)) {
            shouldEnableButton = false
        }
        if (TextUtils.isEmpty(edMountPoint.text)) {
            shouldEnableButton = false
        }
        if (TextUtils.isEmpty(edPort.text)) {
            shouldEnableButton = false
        }
        if (TextUtils.isEmpty(edPassword.text)) {
            shouldEnableButton = false
        }
        //根据各项填写情况，更新保存按钮的值
        if (shouldEnableButton) {
            btSaveRtkInfo.isEnabled = true
            btSaveRtkInfo.setTextColor(resources.getColor(R.color.uxsdk_setting_menu_rtk_tiny_green))
        } else {
            btSaveRtkInfo.isEnabled = false
            btSaveRtkInfo.setTextColor(resources.getColor(R.color.uxsdk_setting_menu_rtk_txt_gray))

        }
    }

    private fun saveRtkCustomUserInfo() {
        // 检查输入是否有效
        var isHostValid = true
        var isPortValid = true
        var isUserValid = true
        var isPwdValid = true
        var isMountPointValid = true
        val host: String = edHost.text.toString()
        if (TextUtils.isEmpty(host)) {
            isHostValid = false
        }

        val portStr: String = edPort.text.toString()
        var port = -1
        if (!TextUtils.isEmpty(portStr) && TextUtils.isDigitsOnly(portStr)) {
            port = portStr.toInt()
            if (port < 0) {
                isPortValid = false
            }
        } else {
            isPortValid = false
        }

        val user: String = edUser.text.toString()
        if (TextUtils.isEmpty(user)) {
            isUserValid = false
        }

        val pw: String = edPassword.text.toString()
        if (TextUtils.isEmpty(pw)) {
            isPwdValid = false
        }

        val mountPint: String = edMountPoint.text.toString()
        if (TextUtils.isEmpty(mountPint)) {
            isMountPointValid = false
        }


        // 参数无效，select设为true时，EditText边框设为红色
        edHost.isSelected = !isHostValid
        edPort.isSelected = !isPortValid
        edUser.isSelected = !isUserValid
        edPassword.isSelected = !isPwdValid
        edMountPoint.isSelected = !isMountPointValid

        val isParamsValid = (isHostValid && isMountPointValid
                && isPortValid && isUserValid && isPwdValid)
        if (!isParamsValid) {
            ViewUtil.showToast(context, R.string.uxsdk_rtk_setting_menu_customer_rtk_save_failed_tips, Toast.LENGTH_SHORT)
            return
        }
        startRtkCustomNetwork(host, port, user, pw, mountPint)

    }

    private fun startRtkCustomNetwork(host: String, port: Int, user: String, pw: String, mountPint: String) {
        val rtkSetting = RTKCustomNetworkSetting()
        rtkSetting.serverAddress = host
        rtkSetting.port = port
        rtkSetting.userName = user
        rtkSetting.password = pw
        rtkSetting.mountPoint = mountPint
        RTKUtil.saveRtkCustomNetworkSetting(rtkSetting)
        LogUtils.i(TAG, "rtkSetting=$rtkSetting,startRtkCustomNetwork now!(Thread.currentThread().name${Thread.currentThread().name})")
        RTKStartServiceHelper.startRtkService(true)
    }

    /**
     * 初始化用户设置的自定义网络RTK设置的信息
     */
    private fun initDefaultCustomSetting() {
        RTKUtil.getRtkCustomNetworkSetting()?.run {
            LogUtils.i(TAG, "getRtkCustomNetworkSetting=$this")
            edMountPoint.text = mountPoint
            edHost.text = serverAddress
            edPassword.text = password
            edUser.text = userName
            edPort.text = port.toString()
        }
        isTextEmptyChanged()
    }

    private fun initDefaultNetRtkUI() {
        //初始化用户上次选择的RTK服务类型
        if (currentRTKSource != RTKReferenceStationSource.UNKNOWN && rtkSourceList.isNotEmpty()) {
            for ((index, rtkSource) in rtkSourceList.withIndex()) {
                if (rtkSource == currentRTKSource) {
                    lastSelectedRTKTypeIndex = index
                    rtkTypeCell.select(index)
                }
            }
        }

        //初始化用户上次选择的坐标系
        val netRTKCoordinateSystem = RTKUtil.getNetRTKCoordinateSystem(currentRTKSource)
        for ((index, coordinate) in coordinateSystemList.withIndex()) {
            if (netRTKCoordinateSystem == coordinate) {
                lastSelectedCoordinateSystemIndex = index
                coordinateSystemCell.select(index)
            }

        }

    }


}

