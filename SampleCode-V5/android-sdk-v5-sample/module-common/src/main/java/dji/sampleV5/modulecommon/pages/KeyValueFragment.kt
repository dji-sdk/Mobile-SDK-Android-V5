package dji.sampleV5.modulecommon.pages

import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.widget.*
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dji.sampleV5.modulecommon.R
import dji.sampleV5.modulecommon.keyvalue.*
import dji.sampleV5.modulecommon.keyvalue.KeyItemHelper.processSubListLogic
import dji.sampleV5.modulecommon.models.KeyValueVM
import dji.sampleV5.modulecommon.util.Util
import dji.sdk.keyvalue.converter.EmptyValueConverter
import dji.sdk.keyvalue.value.product.ProductType
import dji.v5.manager.capability.CapabilityManager
import dji.v5.utils.common.DjiSharedPreferencesManager
import dji.v5.utils.common.LogUtils

import kotlinx.android.synthetic.main.fragment_key_list.*
import kotlinx.android.synthetic.main.layout_key_operate.*
import java.lang.Exception
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import android.widget.ArrayAdapter


import android.view.*
import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.key.ComponentType
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.common.BoolMsg
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.manager.KeyManager
import dji.sampleV5.modulecommon.util.ToastUtils.showToast
import io.reactivex.rxjava3.schedulers.Schedulers


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
    var currentChannelType: ChannelType? = ChannelType.CHANNEL_TYPE_CAMERA
    val LISTEN_RECORD_MAX_LENGTH = 6000
    val HIGH_FREQUENCY_KEY_SP_NAME = "highfrequencykey"
    val LENS_TAG = "CAMERA_LENS_"

    var contentView: View? = null
    var recyclerView: RecyclerView? = null
    var btAction: Button? = null
    val logMessage = StringBuilder()


    var currentKeyItem: KeyItem<*, *>? = null
    val currentKeyTypeList: MutableList<KeyItem<*, *>> = ArrayList()
    val currentKeyItemList: MutableList<KeyItem<*, *>> = ArrayList()
    val currentChannelList = Arrays.asList(*ChannelType.values())
    val data: MutableList<KeyItem<*, *>> = ArrayList()
    var cameraParamsAdapter: KeyItemAdapter? = null
    val batteryKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    val wifiKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    val bleList: List<KeyItem<*, *>> = ArrayList()
    val gimbalKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    val cameraKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    val flightAssistantKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    val flightControlKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    val airlinkKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    val remoteControllerKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    val productKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    val rtkBaseKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    val rtkMobileKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    val ocuSyncKeyList: List<KeyItem<*, *>> = ArrayList()
    val radarKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    val appKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    val mobileNetworkKeyList: List<KeyItem<*, *>> = ArrayList()
    val mobileNetworkLinkRCKeyList: List<KeyItem<*, *>> = ArrayList()
    val batteryBoxKeyList: List<KeyItem<*, *>> = ArrayList()
    val onBoardKeyList: List<KeyItem<*, *>> = ArrayList()
    val payloadKeyList: List<KeyItem<*, *>> = ArrayList()
    val lidarKeyList: List<KeyItem<*, *>> = ArrayList()
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
        cameraParamsAdapter = KeyItemAdapter(activity, data, itemClickCallback)
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
                    totalKeyCount = KeyItemDataUtil.getAllKeyListCount();
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
    val itemClickCallback: KeyItemActionListener<KeyItem<*, *>?> = object :
        KeyItemActionListener<KeyItem<*, *>?> {

        override fun actionChange(keyItem: KeyItem<*, *>?) {
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
    private val keyItemOperateCallBack: KeyItemActionListener<Any> =
        KeyItemActionListener<Any> { t -> //  processListenLogic();
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
    val pushCallback: KeyItemActionListener<String> =
        KeyItemActionListener<String> { t -> //  processListenLogic();
            tv_result?.text = appendLogMessageRecord(t)
            scrollToBottom()

        }

    /**
     * 初始化Key的信息
     *
     * @param keyItem
     */
    private fun initKeyInfo(keyItem: KeyItem<*, *>) {
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

    private fun updateComponentSpinner(keyItem: KeyItem<*, *>) {
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
            ChannelType.CHANNEL_TYPE_BATTERY -> {
                KeyItemDataUtil.initBatteryKeyList(batteryKeyList)
                tips = Util.getString(R.string.battery)
                currentKeyItemList.addAll(batteryKeyList)
            }
            ChannelType.CHANNEL_TYPE_GIMBAL -> {
                KeyItemDataUtil.initGimbalKeyList(gimbalKeyList)
                tips = Util.getString(R.string.gimbal)
                currentKeyItemList.addAll(gimbalKeyList)
            }
            ChannelType.CHANNEL_TYPE_CAMERA -> {
                KeyItemDataUtil.initCameraKeyList(cameraKeyList)
                tips = Util.getString(R.string.camera)
                currentKeyItemList.addAll(cameraKeyList)
            }

            ChannelType.CHANNEL_TYPE_FLIGHT_ASSISTANT -> {
                tips = Util.getString(R.string.flight_assistant)
                KeyItemDataUtil.initFlightAssistantKeyList(flightAssistantKeyList)
                currentKeyItemList.addAll(flightAssistantKeyList)
            }
            ChannelType.CHANNEL_TYPE_FLIGHT_CONTROL -> {
                tips = Util.getString(R.string.flight_control)
                KeyItemDataUtil.initFlightControllerKeyList(flightControlKeyList)
                currentKeyItemList.addAll(flightControlKeyList)
            }
            ChannelType.CHANNEL_TYPE_AIRLINK -> {
                tips = Util.getString(R.string.airlink)
                KeyItemDataUtil.initAirlinkKeyList(airlinkKeyList)
                currentKeyItemList.addAll(airlinkKeyList)
            }
            ChannelType.CHANNEL_TYPE_REMOTE_CONTROLLER -> {
                tips = Util.getString(R.string.remote_controller)
                KeyItemDataUtil.initRemoteControllerKeyList(remoteControllerKeyList)
                currentKeyItemList.addAll(remoteControllerKeyList)
            }
            ChannelType.CHANNEL_TYPE_BLE -> {
                tips = Util.getString(R.string.ble)
                KeyItemDataUtil.initBleKeyList(bleList)
                currentKeyItemList.addAll(bleList)
            }
            ChannelType.CHANNEL_TYPE_PRODUCT -> {
                tips = Util.getString(R.string.product)
                KeyItemDataUtil.initProductKeyList(productKeyList)
                currentKeyItemList.addAll(productKeyList)
            }
            ChannelType.CHANNEL_TYPE_RTK_BASE_STATION -> {
                tips = Util.getString(R.string.rtkbase)
                KeyItemDataUtil.initRtkBaseStationKeyList(rtkBaseKeyList)
                currentKeyItemList.addAll(rtkBaseKeyList)
            }
            ChannelType.CHANNEL_TYPE_RTK_MOBILE_STATION -> {
                tips = Util.getString(R.string.rtkmobile)
                KeyItemDataUtil.initRtkMobileStationKeyList(rtkMobileKeyList)
                currentKeyItemList.addAll(rtkMobileKeyList)
            }
            ChannelType.CHANNEL_TYPE_OCU_SYNC -> {
                tips = Util.getString(R.string.ocusync)
                KeyItemDataUtil.initOcuSyncKeyList(ocuSyncKeyList)
                currentKeyItemList.addAll(ocuSyncKeyList)
            }
            ChannelType.CHANNEL_TYPE_RADAR -> {
                tips = Util.getString(R.string.radar)
                KeyItemDataUtil.initRadarKeyList(radarKeyList)
                currentKeyItemList.addAll(radarKeyList)
            }

            ChannelType.CHANNEL_TYPE_MOBILE_NETWORK -> {
                tips = Util.getString(R.string.mobile_network)
                KeyItemDataUtil.initMobileNetworkKeyList(mobileNetworkKeyList)
                KeyItemDataUtil.initMobileNetworkLinkRCKeyList(mobileNetworkLinkRCKeyList)
                currentKeyItemList.addAll(mobileNetworkKeyList)
                currentKeyItemList.addAll(mobileNetworkLinkRCKeyList)
            }

            ChannelType.CHANNEL_TYPE_ON_BOARD -> {
                tips = Util.getString(R.string.on_board)
                KeyItemDataUtil.initOnboardKeyList(onBoardKeyList)
                currentKeyItemList.addAll(onBoardKeyList)
            }
            ChannelType.CHANNEL_TYPE_ON_PAYLOAD -> {
                tips = Util.getString(R.string.payload)
                KeyItemDataUtil.initPayloadKeyList(payloadKeyList)
                currentKeyItemList.addAll(payloadKeyList)
            }
            ChannelType.CHANNEL_TYPE_LIDAR -> {
                tips = Util.getString(R.string.lidar)
                KeyItemDataUtil.initLidarKeyList(lidarKeyList)
                currentKeyItemList.addAll(lidarKeyList)
            }

            else -> {}
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
        val showList: MutableList<KeyItem<*, *>> = ArrayList(currentKeyItemList)
        changeCurrentList(enable, showList)
        data.clear()
        data.addAll(showList)
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
    private fun changeCurrentList(enable: Boolean, showList: MutableList<KeyItem<*, *>>) {
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
            ChannelType.CHANNEL_TYPE_CAMERA -> {
                if (!CapabilityManager.getInstance()
                        .isCameraKeySupported(type, cameraType?.name, keyName)) {
                    isNeedRemove = true
                }
            }
            ChannelType.CHANNEL_TYPE_AIRLINK -> {
                if (!CapabilityManager.getInstance()
                        .isKeySupported(type, "", ComponentType.AIRLINK, keyName)) {
                    isNeedRemove = true
                }
            }
            ChannelType.CHANNEL_TYPE_GIMBAL -> {
                if (!CapabilityManager.getInstance()
                        .isKeySupported(type, "", ComponentType.GIMBAL, keyName)) {
                    isNeedRemove = true
                }
            }
            ChannelType.CHANNEL_TYPE_REMOTE_CONTROLLER -> {
                if (!CapabilityManager.getInstance()
                        .isKeySupported(type, "", ComponentType.REMOTECONTROLLER, keyName)) {
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
                    CapabilityKeyChecker.check( msdkInfoVm.msdkInfo.value?.productType?.name!! ,
                        it)
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
        val sortlist: MutableList<KeyItem<*, *>> = ArrayList(currentKeyItemList)
        changeCurrentList(isCapabilitySwitchOn(), sortlist)
        Collections.sort(sortlist)
        KeyValueDialogUtil.showFilterListWindow(
            ll_channel_filter_container,
            sortlist,
            object :
                KeyItemActionListener<KeyItem<*, *>?> {
                override fun actionChange(item: KeyItem<*, *>?) {
                    itemClickCallback.actionChange(item)
                }
            })
    }

    private fun channelTypeFilterOperate() {
        var showChannelList: MutableList<ChannelType> = ArrayList()
        val capabilityChannelList = arrayOf(
            ChannelType.CHANNEL_TYPE_BATTERY, ChannelType.CHANNEL_TYPE_AIRLINK, ChannelType.CHANNEL_TYPE_CAMERA,
            ChannelType.CHANNEL_TYPE_GIMBAL, ChannelType.CHANNEL_TYPE_REMOTE_CONTROLLER, ChannelType.CHANNEL_TYPE_FLIGHT_CONTROL
        )
        if (isCapabilitySwitchOn()) {
            showChannelList = capabilityChannelList.toMutableList()
        } else {
            showChannelList = currentChannelList
        }
        KeyValueDialogUtil.showChannelFilterListWindow(
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
                currentKeyItem!!.subItemMap as Map<String?, List<EnumItem>>,
                object :
                    KeyItemActionListener<String?> {


                    override fun actionChange(paramJsonStr: String?) {
                        if (Util.isBlank(paramJsonStr)) {
                            return
                        }
                        currentKeyItem!!.doSet(paramJsonStr)
                    }
                })
        } else {
            KeyValueDialogUtil.showInputDialog(
                activity,
                currentKeyItem,
                object :
                    KeyItemActionListener<String?> {
                    override fun actionChange(s: String?) {
                        if (Util.isBlank(s)) {
                            return
                        }
                        currentKeyItem!!.doSet(s)
                    }
                })
        }
    }

    private fun processBoolMsgDlg(keyitem: KeyItem<*, *>) {
        val boolValueList: MutableList<String> = java.util.ArrayList()
        boolValueList.add("false")
        boolValueList.add("true")

        KeyValueDialogUtil.showSingleChoiceDialog(
            context,
            boolValueList,
            -1,
            object : KeyItemActionListener<List<String>?> {
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
                currentKeyItem?.subItemMap as Map<String?, List<EnumItem>>,
                object :
                    KeyItemActionListener<String?> {
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
            KeyValueDialogUtil.showInputDialog(
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
    private fun releaseKeyInfo(list: MutableList<KeyItem<*, *>>?) {
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