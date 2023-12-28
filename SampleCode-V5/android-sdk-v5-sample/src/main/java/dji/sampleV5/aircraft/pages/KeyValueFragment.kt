package dji.sampleV5.aircraft.pages


import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.keyvalue.KeyItemHelper.processSubListLogic
import dji.sampleV5.aircraft.util.ToastUtils.showToast
import dji.sampleV5.aircraft.util.Util
import dji.sdk.keyvalue.converter.EmptyValueConverter
import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.key.ComponentType
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.common.BoolMsg
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.product.ProductType
import dji.v5.manager.KeyManager
import dji.v5.manager.capability.CapabilityManager
import dji.v5.utils.common.DjiSharedPreferencesManager
import dji.v5.utils.common.LogUtils
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_key_list.et_filter
import kotlinx.android.synthetic.main.fragment_key_list.iv_capability
import kotlinx.android.synthetic.main.fragment_key_list.iv_question_mark
import kotlinx.android.synthetic.main.fragment_key_list.ll_channel_filter_container
import kotlinx.android.synthetic.main.fragment_key_list.ll_filter_container
import kotlinx.android.synthetic.main.fragment_key_list.tv_capablity
import kotlinx.android.synthetic.main.fragment_key_list.tv_count
import kotlinx.android.synthetic.main.fragment_key_list.tv_operate_title
import kotlinx.android.synthetic.main.fragment_key_list.tv_operate_title_lyt
import kotlinx.android.synthetic.main.layout_key_operate.bt_action
import kotlinx.android.synthetic.main.layout_key_operate.bt_add_command
import kotlinx.android.synthetic.main.layout_key_operate.bt_get
import kotlinx.android.synthetic.main.layout_key_operate.bt_gpscoord
import kotlinx.android.synthetic.main.layout_key_operate.bt_listen
import kotlinx.android.synthetic.main.layout_key_operate.bt_set
import kotlinx.android.synthetic.main.layout_key_operate.bt_unlistenall
import kotlinx.android.synthetic.main.layout_key_operate.btn_clearlog
import kotlinx.android.synthetic.main.layout_key_operate.sp_index
import kotlinx.android.synthetic.main.layout_key_operate.sp_subindex
import kotlinx.android.synthetic.main.layout_key_operate.sp_subtype
import kotlinx.android.synthetic.main.layout_key_operate.tv_name
import kotlinx.android.synthetic.main.layout_key_operate.tv_result
import kotlinx.android.synthetic.main.layout_key_operate.tv_subtype
import kotlinx.android.synthetic.main.layout_key_operate.tv_tip
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Collections
import java.util.Date


/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/5/11
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
class KeyValueFragment : DJIFragment(), View.OnClickListener {
    private val TAG = LogUtils.getTag("KeyValueFragment")

    val CAPABILITY_ENABLE = "capabilityenable"
    var currentChannelType: dji.sampleV5.aircraft.keyvalue.ChannelType? = dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_CAMERA
    val LISTEN_RECORD_MAX_LENGTH = 6000
    val HIGH_FREQUENCY_KEY_SP_NAME = "highfrequencykey"
    val LENS_TAG = "CAMERA_LENS_"

    var contentView: View? = null
    var recyclerView: RecyclerView? = null
    var btAction: Button? = null
    val logMessage = StringBuilder()


    var currentKeyItem: dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>? = null
    val currentKeyTypeList: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    val currentKeyItemList: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    val currentChannelList = Arrays.asList(*dji.sampleV5.aircraft.keyvalue.ChannelType.values())
    val data: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    var cameraParamsAdapter: dji.sampleV5.aircraft.keyvalue.KeyItemAdapter? = null
    val batteryKeyList: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    val wifiKeyList: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    val bleList: List<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    val gimbalKeyList: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    val cameraKeyList: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    val flightAssistantKeyList: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    val flightControlKeyList: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    val airlinkKeyList: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    val remoteControllerKeyList: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    val productKeyList: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    val rtkBaseKeyList: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    val rtkMobileKeyList: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    val ocuSyncKeyList: List<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    val radarKeyList: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    val appKeyList: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    val mobileNetworkKeyList: List<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    val mobileNetworkLinkRCKeyList: List<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    val batteryBoxKeyList: List<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    val onBoardKeyList: List<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    val payloadKeyList: List<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    val lidarKeyList: List<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    var keyValuesharedPreferences: SharedPreferences? = null
    val selectMode = false
    var totalKeyCount: Int? = null
    var capabilityKeyCount: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        if (contentView == null) {
            contentView = inflater.inflate(R.layout.fragment_key_list, container, false)
        }
        return contentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initLocalData()
        contentView?.let { initView(it) }
        initRemoteData()
        val parent = contentView!!.parent as ViewGroup
        parent.removeView(contentView)
    }

    private fun initLocalData() {
        data.clear()
        cameraParamsAdapter = dji.sampleV5.aircraft.keyvalue.KeyItemAdapter(activity, data, itemClickCallback)
        keyValuesharedPreferences =
            activity?.getSharedPreferences(HIGH_FREQUENCY_KEY_SP_NAME, Context.MODE_PRIVATE)
    }


    private fun initView(view: View) {
        initViewAndListener(view)
        tv_result!!.setOnLongClickListener {
            val cmb = activity
                ?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cmb.text = tv_result!!.text.toString()
            true
        }
        bt_get.setOnClickListener(this)
        bt_set.setOnClickListener(this)
        bt_listen.setOnClickListener(this)
        bt_action.setOnClickListener(this)
        btn_clearlog.setOnClickListener(this)
        bt_unlistenall.setOnClickListener(this)
        iv_question_mark.setOnClickListener(this)

        iv_capability.isChecked = isCapabilitySwitchOn()
        msdkInfoVm.msdkInfo.observe(viewLifecycleOwner) {
            iv_capability.isEnabled = it.productType != ProductType.UNRECOGNIZED
            setDataWithCapability(iv_capability.isChecked)
            Schedulers.single().scheduleDirect {
                if (totalKeyCount == null || capabilityKeyCount == null) {
                    totalKeyCount = dji.sampleV5.aircraft.keyvalue.KeyItemDataUtil.getAllKeyListCount();
                    capabilityKeyCount = CapabilityManager.getInstance().getCapabilityKeyCount(it.productType.name)
                }
            }
        }
        sp_index.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                setKeyInfo()
                currentKeyItem?.let { updateComponentSpinner(it) }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                //do nothing
            }
        }
        iv_capability.setOnCheckedChangeListener { _, enable ->
            if (enable) {
                capabilityKeyCount?.let { showToast(tv_capablity?.text.toString() + " count:$it") }
            } else {
                totalKeyCount?.let { showToast(tv_capablity?.text.toString() + " count:$it") }
            }
            setDataWithCapability(enable)
        }
    }

    private fun initViewAndListener(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        tv_operate_title_lyt.setOnClickListener {
            channelTypeFilterOperate()
        }
        ll_filter_container.setOnClickListener {
            keyFilterOperate()
        }

        btAction = view.findViewById(R.id.bt_action)
        et_filter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                //Do Something
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                //Do Something
            }

            override fun afterTextChanged(s: Editable) {
                cameraParamsAdapter?.filter?.filter(s.toString())
            }
        })
    }

    private fun initRemoteData() {
        recyclerView!!.layoutManager = LinearLayoutManager(activity)
        recyclerView!!.adapter = cameraParamsAdapter
        tv_tip!!.movementMethod = ScrollingMovementMethod.getInstance()
        tv_result!!.movementMethod = ScrollingMovementMethod.getInstance()
    }

    override fun onResume() {
        processChannelInfo()
        super.onResume()
    }

    /**
     * key列表点击回调
     */
    val itemClickCallback: dji.sampleV5.aircraft.keyvalue.KeyItemActionListener<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>?> = object :
        dji.sampleV5.aircraft.keyvalue.KeyItemActionListener<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>?> {

        override fun actionChange(keyItem: dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>?) {
            if (keyItem == null) {
                return
            }
            initKeyInfo(keyItem)
            cameraParamsAdapter?.notifyDataSetChanged()
        }
    }

    /**
     * key操作结果回调
     */
    private val keyItemOperateCallBack: dji.sampleV5.aircraft.keyvalue.KeyItemActionListener<Any> =
        dji.sampleV5.aircraft.keyvalue.KeyItemActionListener<Any> { t -> //  processListenLogic();
            t?.let {
                tv_result.text = appendLogMessageRecord(t.toString())
                scrollToBottom()
            }

        }

    private fun scrollToBottom() {
        val scrollOffset = (tv_result!!.layout.getLineTop(tv_result!!.lineCount)
                - tv_result!!.height)
        if (scrollOffset > 0) {
            tv_result!!.scrollTo(0, scrollOffset)
        } else {
            tv_result!!.scrollTo(0, 0)
        }
    }

    private fun appendLogMessageRecord(appendStr: String?): String {
        val curTime = SimpleDateFormat("HH:mm:ss").format(Date())
        logMessage.append(curTime)
            .append(":")
            .append(appendStr)
            .append("\n")

        //长度限制
        var result = logMessage.toString()
        if (result.length > LISTEN_RECORD_MAX_LENGTH) {
            result = result.substring(result.length - LISTEN_RECORD_MAX_LENGTH)
        }
        return result
    }

    /**
     * 推送结果回调
     */
    val pushCallback: dji.sampleV5.aircraft.keyvalue.KeyItemActionListener<String> =
        dji.sampleV5.aircraft.keyvalue.KeyItemActionListener<String> { t -> //  processListenLogic();
            tv_result?.text = appendLogMessageRecord(t)
            scrollToBottom()

        }

    /**
     * 初始化Key的信息
     *
     * @param keyItem
     */
    private fun initKeyInfo(keyItem: dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>) {
        currentKeyItem = keyItem
        currentKeyItem!!.setKeyOperateCallBack(keyItemOperateCallBack)
        tv_name?.text = keyItem.name
        bt_add_command.visibility = if (selectMode) View.VISIBLE else View.GONE
        processListenLogic()
        bt_gpscoord.visibility = View.GONE
        tv_tip.visibility = View.GONE
        keyItem.count = System.currentTimeMillis()
        resetSelected()
        bt_set.isEnabled = currentKeyItem!!.canSet()
        bt_get.isEnabled = currentKeyItem!!.canGet()
        bt_listen.isEnabled = currentKeyItem!!.canListen()
        bt_action.isEnabled = currentKeyItem!!.canAction()
        keyValuesharedPreferences?.edit()?.putLong(keyItem.toString(), keyItem.count)?.apply()
        keyItem.isItemSelected = true

        updateComponentSpinner(keyItem)

    }

    private fun updateComponentSpinner(keyItem: dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>) {
        val componentType = ComponentType.find(keyItem.keyInfo.componentType)
        if (componentType == ComponentType.CAMERA && isCapabilitySwitchOn()) {
            val list = CapabilityManager.getInstance().getSupportLens("Key" + keyItem.name)
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                list
            )
            sp_subtype.adapter = adapter
            tv_subtype.text = "lenstype"
            val defalutIndex = list.indexOf("DEFAULT")
            if (defalutIndex != -1) {
                sp_subtype.setSelection(defalutIndex)
            }
        } else {
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                requireContext().resources.getStringArray(R.array.sub_type_arrays)
            )
            sp_subtype.adapter = adapter
            tv_subtype.text = "subtype"
        }
    }

    private fun resetSelected() {
        for (item in data) {
            if (item.isItemSelected) {
                item.isItemSelected = false
            }
        }
    }

    /**
     * 处理Listen显示控件
     */
    private fun processListenLogic() {
        if (currentKeyItem == null) {

            bt_listen?.text = "Listen"
            tv_name?.text = ""
            return
        }
        val needShowListenView =
            currentKeyItem!!.canListen() && currentKeyItem!!.getListenHolder() is KeyValueFragment && Util.isNotBlank(
                currentKeyItem!!.getListenRecord()
            )
        if (needShowListenView) {
            tv_tip.visibility = View.VISIBLE
            tv_tip.text = currentKeyItem!!.getListenRecord()
        } else {
            tv_tip.visibility = View.GONE
            tv_tip.setText(R.string.operate_listen_record_tips)
        }
        if (currentKeyItem!!.getListenHolder() == null) {

            bt_listen?.text = "Listen"
        } else {

            bt_listen?.text = "UNListen"
        }
    }

    /**
     * 根据不同类型入口，初始化不同数据
     */
    private fun processChannelInfo() {
        currentKeyTypeList.clear()
        currentKeyItemList.clear()
        var tips: String? = ""
        when (currentChannelType) {
            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_BATTERY -> {
                dji.sampleV5.aircraft.keyvalue.KeyItemDataUtil.initBatteryKeyList(batteryKeyList)
                tips = Util.getString(R.string.battery)
                currentKeyItemList.addAll(batteryKeyList)
            }

            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_GIMBAL -> {
                dji.sampleV5.aircraft.keyvalue.KeyItemDataUtil.initGimbalKeyList(gimbalKeyList)
                tips = Util.getString(R.string.gimbal)
                currentKeyItemList.addAll(gimbalKeyList)
            }

            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_CAMERA -> {
                dji.sampleV5.aircraft.keyvalue.KeyItemDataUtil.initCameraKeyList(cameraKeyList)
                tips = Util.getString(R.string.camera)
                currentKeyItemList.addAll(cameraKeyList)
            }

            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_FLIGHT_ASSISTANT -> {
                tips = Util.getString(R.string.flight_assistant)
                dji.sampleV5.aircraft.keyvalue.KeyItemDataUtil.initFlightAssistantKeyList(flightAssistantKeyList)
                currentKeyItemList.addAll(flightAssistantKeyList)
            }

            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_FLIGHT_CONTROL -> {
                tips = Util.getString(R.string.flight_control)
                dji.sampleV5.aircraft.keyvalue.KeyItemDataUtil.initFlightControllerKeyList(flightControlKeyList)
                currentKeyItemList.addAll(flightControlKeyList)
            }

            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_AIRLINK -> {
                tips = Util.getString(R.string.airlink)
                dji.sampleV5.aircraft.keyvalue.KeyItemDataUtil.initAirlinkKeyList(airlinkKeyList)
                currentKeyItemList.addAll(airlinkKeyList)
            }

            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_REMOTE_CONTROLLER -> {
                tips = Util.getString(R.string.remote_controller)
                dji.sampleV5.aircraft.keyvalue.KeyItemDataUtil.initRemoteControllerKeyList(remoteControllerKeyList)
                currentKeyItemList.addAll(remoteControllerKeyList)
            }

            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_BLE -> {
                tips = Util.getString(R.string.ble)
                dji.sampleV5.aircraft.keyvalue.KeyItemDataUtil.initBleKeyList(bleList)
                currentKeyItemList.addAll(bleList)
            }

            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_PRODUCT -> {
                tips = Util.getString(R.string.product)
                dji.sampleV5.aircraft.keyvalue.KeyItemDataUtil.initProductKeyList(productKeyList)
                currentKeyItemList.addAll(productKeyList)
            }

            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_RTK_BASE_STATION -> {
                tips = Util.getString(R.string.rtkbase)
                dji.sampleV5.aircraft.keyvalue.KeyItemDataUtil.initRtkBaseStationKeyList(rtkBaseKeyList)
                currentKeyItemList.addAll(rtkBaseKeyList)
            }

            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_RTK_MOBILE_STATION -> {
                tips = Util.getString(R.string.rtkmobile)
                dji.sampleV5.aircraft.keyvalue.KeyItemDataUtil.initRtkMobileStationKeyList(rtkMobileKeyList)
                currentKeyItemList.addAll(rtkMobileKeyList)
            }

            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_OCU_SYNC -> {
                tips = Util.getString(R.string.ocusync)
                dji.sampleV5.aircraft.keyvalue.KeyItemDataUtil.initOcuSyncKeyList(ocuSyncKeyList)
                currentKeyItemList.addAll(ocuSyncKeyList)
            }

            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_RADAR -> {
                tips = Util.getString(R.string.radar)
                dji.sampleV5.aircraft.keyvalue.KeyItemDataUtil.initRadarKeyList(radarKeyList)
                currentKeyItemList.addAll(radarKeyList)
            }

            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_MOBILE_NETWORK -> {
                tips = Util.getString(R.string.mobile_network)
                dji.sampleV5.aircraft.keyvalue.KeyItemDataUtil.initMobileNetworkKeyList(mobileNetworkKeyList)
                dji.sampleV5.aircraft.keyvalue.KeyItemDataUtil.initMobileNetworkLinkRCKeyList(mobileNetworkLinkRCKeyList)
                currentKeyItemList.addAll(mobileNetworkKeyList)
                currentKeyItemList.addAll(mobileNetworkLinkRCKeyList)
            }

            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_ON_BOARD -> {
                tips = Util.getString(R.string.on_board)
                dji.sampleV5.aircraft.keyvalue.KeyItemDataUtil.initOnboardKeyList(onBoardKeyList)
                currentKeyItemList.addAll(onBoardKeyList)
            }

            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_ON_PAYLOAD -> {
                tips = Util.getString(R.string.payload)
                dji.sampleV5.aircraft.keyvalue.KeyItemDataUtil.initPayloadKeyList(payloadKeyList)
                currentKeyItemList.addAll(payloadKeyList)
            }

            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_LIDAR -> {
                tips = Util.getString(R.string.lidar)
                dji.sampleV5.aircraft.keyvalue.KeyItemDataUtil.initLidarKeyList(lidarKeyList)
                currentKeyItemList.addAll(lidarKeyList)
            }

            else -> {
               LogUtils.d(TAG , "nothing to do")
            }
        }
        for (item in currentKeyItemList) {
            item.isItemSelected = false;
            val count = keyValuesharedPreferences?.getLong(item.toString(), 0L)
            if (count != null && count != 0L) {
                item.count = count
            }
        }

        tv_operate_title?.text = tips
        setDataWithCapability(isCapabilitySwitchOn())
    }


    private fun setDataWithCapability(enable: Boolean) {
        val showList: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList(currentKeyItemList)
        changeCurrentList(enable, showList)
        data.clear()
        data.addAll(showList)
        data.sortWith { o1, o2 -> o1.name?.compareTo(o2.name) ?: 0 }
        resetSearchFilter()
        setKeyCount(showList.size)
        resetSelected()
        cameraParamsAdapter?.notifyDataSetChanged()
        DjiSharedPreferencesManager.putBoolean(context, CAPABILITY_ENABLE, enable)
        if (enable) {
            tv_capablity?.text = "Officially released key"
        } else {
            tv_capablity?.text = "All key"
        }
    }


    /**
     *  能力集开关打开，并且获取的产品名称在能力集列表中则更新列表
     */
    private fun changeCurrentList(enable: Boolean, showList: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>>) {
        val type = msdkInfoVm.msdkInfo.value?.productType?.name
        if (enable && CapabilityManager.getInstance().isProductSupported(type)) {
            val iterator = showList.iterator();
            while (iterator.hasNext()) {
                if (isNeedRemove("Key" + iterator.next().name)) {
                    iterator.remove()
                }
            }
        }
    }


    private fun isNeedRemove(keyName: String): Boolean {
        var isNeedRemove = false;
        val type = msdkInfoVm.msdkInfo.value?.productType?.name

        val cameraType = KeyManager.getInstance().getValue(
            KeyTools.createKey(
                CameraKey.KeyCameraType,
                CapabilityManager.getInstance().componentIndex
            )
        )

        when (currentChannelType) {
            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_CAMERA -> {
                if (!CapabilityManager.getInstance()
                        .isCameraKeySupported(type, cameraType?.name, keyName)
                ) {
                    isNeedRemove = true
                }
            }

            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_AIRLINK -> {
                if (!CapabilityManager.getInstance()
                        .isKeySupported(type, "", ComponentType.AIRLINK, keyName)
                ) {
                    isNeedRemove = true
                }
            }

            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_GIMBAL -> {
                if (!CapabilityManager.getInstance()
                        .isKeySupported(type, "", ComponentType.GIMBAL, keyName)
                ) {
                    isNeedRemove = true
                }
            }

            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_REMOTE_CONTROLLER -> {
                if (!CapabilityManager.getInstance()
                        .isKeySupported(type, "", ComponentType.REMOTECONTROLLER, keyName)
                ) {
                    isNeedRemove = true
                }
            }

            else -> {
                if (!CapabilityManager.getInstance()
                        .isKeySupported(type, keyName)
                ) {
                    isNeedRemove = true
                }
            }
        }

        return isNeedRemove
    }

    /**
     * 清空search框
     */
    private fun resetSearchFilter() {
        et_filter.setText("")
        cameraParamsAdapter?.getFilter()?.filter("")
    }

    private fun isCapabilitySwitchOn(): Boolean {
        return DjiSharedPreferencesManager.getBoolean(context, CAPABILITY_ENABLE, false)
    }

    private fun setKeyCount(count: Int) {
        tv_count.text = "(${count})";
    }

    override fun onClick(view: View) {

        if (Util.isBlank(tv_name.text?.toString()) || currentKeyItem == null) {
            showToast("please select key first")
            return
        }
        setKeyInfo()

        when (view?.id) {
            R.id.bt_get -> {
                get()
            }

            R.id.bt_unlistenall -> {
                unListenAll()
            }

            R.id.bt_set -> {
                set()
            }

            R.id.bt_listen -> {
                listen()
            }

            R.id.bt_action -> {
                action()
            }

            R.id.btn_clearlog -> {
                tv_result?.text = ""
                logMessage.delete(0, logMessage.length)
            }

            R.id.iv_question_mark -> {

                val cameraType = KeyManager.getInstance().getValue(
                    KeyTools.createKey(
                        CameraKey.KeyCameraType,
                        CapabilityManager.getInstance().componentIndex
                    )
                )
                cameraType?.name?.let {
                    dji.sampleV5.aircraft.keyvalue.CapabilityKeyChecker.check(
                        msdkInfoVm.msdkInfo.value?.productType?.name!!,
                        it
                    )
                }
                // KeyValueDialogUtil.showNormalDialog(getActivity(), "提示")
                //CapabilityKeyChecker.generateAllEnumList(msdkInfoVm.msdkInfo.value?.productType?.name!! , cameraType!!.name )

            }
        }
    }


    /**
     * key列表条件过滤
     */
    private fun keyFilterOperate() {
        val sortlist: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList(currentKeyItemList)
        changeCurrentList(isCapabilitySwitchOn(), sortlist)
        Collections.sort(sortlist)
        dji.sampleV5.aircraft.keyvalue.KeyValueDialogUtil.showFilterListWindow(
            ll_channel_filter_container,
            sortlist,
            object :
                dji.sampleV5.aircraft.keyvalue.KeyItemActionListener<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>?> {
                override fun actionChange(item: dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>?) {
                    itemClickCallback.actionChange(item)
                }
            })
    }

    private fun channelTypeFilterOperate() {
        var showChannelList: MutableList<dji.sampleV5.aircraft.keyvalue.ChannelType> = ArrayList()
        val capabilityChannelList = arrayOf(
            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_BATTERY, dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_AIRLINK, dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_CAMERA,
            dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_GIMBAL, dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_REMOTE_CONTROLLER, dji.sampleV5.aircraft.keyvalue.ChannelType.CHANNEL_TYPE_FLIGHT_CONTROL
        )
        if (isCapabilitySwitchOn()) {
            showChannelList = capabilityChannelList.toMutableList()
        } else {
            showChannelList = currentChannelList
        }
        dji.sampleV5.aircraft.keyvalue.KeyValueDialogUtil.showChannelFilterListWindow(
            tv_operate_title,
            showChannelList
        ) { channelType ->
            currentChannelType = channelType
            currentKeyItem = null
            processChannelInfo()
            processListenLogic()
        }
    }


    private fun getCameraSubIndex(lensName: String): Int {
        CameraLensType.values().forEach {
            if (lensName == it.name) {
                return it.value()
            }
        }
        return CameraLensType.UNKNOWN.value()
    }

    private fun getComponentIndex(compentName: String): Int {
        return when (compentName) {
            ComponentIndexType.LEFT_OR_MAIN.name -> ComponentIndexType.LEFT_OR_MAIN.value()
            ComponentIndexType.RIGHT.name -> ComponentIndexType.RIGHT.value()
            ComponentIndexType.UP.name -> ComponentIndexType.UP.value()
            else -> {
                ComponentIndexType.UNKNOWN.value()
            }
        }
    }

    private fun setKeyInfo() {
        if (currentKeyItem == null) {
            return
        }
        try {
            val index = getComponentIndex(sp_index.selectedItem.toString())

            if (index != -1) {
                currentKeyItem!!.componetIndex = index
                CapabilityManager.getInstance().setComponetIndex(index)
            }
            val subtype: Int
            if (ComponentType.find(currentKeyItem!!.keyInfo.componentType) == ComponentType.CAMERA && isCapabilitySwitchOn()) {
                subtype = getCameraSubIndex(LENS_TAG + sp_subtype.selectedItem.toString())

            } else {
                subtype = sp_subtype.selectedItem.toString().toInt()
            }

            if (subtype != -1) {
                currentKeyItem!!.subComponetType = subtype
            }
            val subIndex = sp_subindex.selectedItem.toString().toInt()
            if (subIndex != -1) {
                currentKeyItem!!.subComponetIndex = subIndex
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, e.message)
        }
    }

    /**
     * 获取操作
     */
    private fun get() {
        if (!currentKeyItem?.canGet()!!) {
            showToast("not support get")
            return
        }
        currentKeyItem!!.doGet()
    }

    private fun unListenAll() {
        release()
        processListenLogic()
    }

    /**
     * Listen操作
     */
    private fun listen() {
        if (!currentKeyItem?.canListen()!!) {
            showToast("not support listen")
            return
        }
        currentKeyItem!!.setPushCallBack(pushCallback)
        val listenHolder = currentKeyItem!!.getListenHolder()
        if (listenHolder == null) {
            currentKeyItem!!.listen(this)
            currentKeyItem!!.setKeyOperateCallBack(keyItemOperateCallBack)

            bt_listen?.text = "Un-Listen"
        } else if (listenHolder is KeyValueFragment) {
            currentKeyItem!!.cancelListen(this)
            bt_listen?.text = "Listen"
        }
        processListenLogic()
    }

    /**
     * 设置操作
     */
    private fun set() {
        if (!currentKeyItem?.canSet()!!) {
            showToast("not support set")
            return
        }
        if (currentKeyItem!!.param is BoolMsg) {
            processBoolMsgDlg(currentKeyItem!!)
            return
        }
        if (currentKeyItem!!.subItemMap.isNotEmpty()) {
            processSubListLogic(
                bt_set,
                currentKeyItem!!.param,
                currentKeyItem!!.subItemMap as Map<String?, List<dji.sampleV5.aircraft.keyvalue.EnumItem>>,
                object :
                    dji.sampleV5.aircraft.keyvalue.KeyItemActionListener<String?> {


                    override fun actionChange(paramJsonStr: String?) {
                        if (Util.isBlank(paramJsonStr)) {
                            return
                        }
                        currentKeyItem!!.doSet(paramJsonStr)
                    }
                })
        } else {
            dji.sampleV5.aircraft.keyvalue.KeyValueDialogUtil.showInputDialog(
                activity,
                currentKeyItem,
                object :
                    dji.sampleV5.aircraft.keyvalue.KeyItemActionListener<String?> {
                    override fun actionChange(s: String?) {
                        if (Util.isBlank(s)) {
                            return
                        }
                        currentKeyItem!!.doSet(s)
                    }
                })
        }
    }

    private fun processBoolMsgDlg(keyitem: dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>) {
        val boolValueList: MutableList<String> = java.util.ArrayList()
        boolValueList.add("false")
        boolValueList.add("true")

        dji.sampleV5.aircraft.keyvalue.KeyValueDialogUtil.showSingleChoiceDialog(
            context,
            boolValueList,
            -1,
            object : dji.sampleV5.aircraft.keyvalue.KeyItemActionListener<List<String>?> {
                override fun actionChange(values: List<String>?) {
                    val param = "{\"value\":${values?.get(0)}}"
                    keyitem.doSet(param)
                }
            })
    }

    /**
     * 动作操作
     */
    private fun action() {
        if (!currentKeyItem?.canAction()!!) {
            showToast("not support action")
            return
        }

        if (currentKeyItem!!.keyInfo.typeConverter === EmptyValueConverter.converter) {
            currentKeyItem?.doAction("")
        } else if (currentKeyItem?.subItemMap!!.isNotEmpty()) {
            processSubListLogic(
                bt_set,
                currentKeyItem?.param,
                currentKeyItem?.subItemMap as Map<String?, List<dji.sampleV5.aircraft.keyvalue.EnumItem>>,
                object :
                    dji.sampleV5.aircraft.keyvalue.KeyItemActionListener<String?> {
                    override fun actionChange(paramJsonStr: String?) {
                        if (Util.isBlank(paramJsonStr)) {
                            return
                        }
                        currentKeyItem!!.doAction(paramJsonStr)
                    }
                })
        } else if (currentKeyItem!!.paramJsonStr != null && currentKeyItem!!.paramJsonStr == "{}") {
            currentKeyItem!!.doAction(currentKeyItem!!.paramJsonStr)
        } else {
            dji.sampleV5.aircraft.keyvalue.KeyValueDialogUtil.showInputDialog(
                activity,
                currentKeyItem
            ) { s -> currentKeyItem!!.doAction(s) }
        }
    }

    /**
     * 注销Listen，移除业务回调
     *
     * @param list
     */
    private fun releaseKeyInfo(list: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>>?) {
        if (list == null) {
            return
        }
        for (item in list) {
            item.removeCallBack()
            item.cancelListen(this)
        }

    }

    open fun release() {
        if (currentKeyItem != null) {
            currentKeyItem!!.cancelListen(this)
        }
        releaseKeyInfo(batteryKeyList)
        releaseKeyInfo(gimbalKeyList)
        releaseKeyInfo(cameraKeyList)
        releaseKeyInfo(wifiKeyList)
        releaseKeyInfo(flightAssistantKeyList)
        releaseKeyInfo(flightControlKeyList)
        releaseKeyInfo(airlinkKeyList)
        releaseKeyInfo(productKeyList)
        releaseKeyInfo(rtkBaseKeyList)
        releaseKeyInfo(rtkMobileKeyList)
        releaseKeyInfo(remoteControllerKeyList)
        releaseKeyInfo(radarKeyList)
        releaseKeyInfo(appKeyList)
    }

    override fun onDestroy() {
        super.onDestroy()
        release()
    }
}