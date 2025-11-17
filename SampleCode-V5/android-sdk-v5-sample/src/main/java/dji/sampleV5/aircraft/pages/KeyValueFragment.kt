package dji.sampleV5.aircraft.pages


import android.annotation.SuppressLint
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
import androidx.recyclerview.widget.LinearLayoutManager
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.databinding.FragmentKeyListBinding
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
import dji.v5.utils.common.JsonUtil
import dji.v5.utils.common.LogPath
import dji.v5.utils.common.LogUtils
import io.reactivex.rxjava3.schedulers.Schedulers
import java.text.SimpleDateFormat
import java.util.Arrays
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

    private var binding: FragmentKeyListBinding? = null
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
    val intelligentBoxList: MutableList<dji.sampleV5.aircraft.keyvalue.KeyItem<*, *>> = ArrayList()
    var keyValuesharedPreferences: SharedPreferences? = null
    val selectMode = false
    var totalKeyCount: Int? = null
    var capabilityKeyCount: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentKeyListBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initLocalData()
        binding?.root?.let { initView(it) }
        initRemoteData()
        val parent = binding?.root?.parent as ViewGroup
        parent.removeView(binding?.root)
    }

    private fun initLocalData() {
        data.clear()
        cameraParamsAdapter = dji.sampleV5.aircraft.keyvalue.KeyItemAdapter(activity, data, itemClickCallback)
        keyValuesharedPreferences =
            activity?.getSharedPreferences(HIGH_FREQUENCY_KEY_SP_NAME, Context.MODE_PRIVATE)
    }


    private fun initView(view: View) {
        initViewAndListener(view)
        binding?.layoutKeyOperate?.tvResult?.setOnLongClickListener {
            val cmb = activity
                ?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cmb.text = binding?.layoutKeyOperate?.tvResult?.text.toString()
            true
        }
        binding?.layoutKeyOperate?.btGet?.setOnClickListener(this)
        binding?.layoutKeyOperate?.btSet?.setOnClickListener(this)
        binding?.layoutKeyOperate?.btListen?.setOnClickListener(this)
        binding?.layoutKeyOperate?.btAction?.setOnClickListener(this)
        binding?.layoutKeyOperate?.btnClearlog?.setOnClickListener(this)
        binding?.layoutKeyOperate?.btUnlistenall?.setOnClickListener(this)
        binding?.ivQuestionMark?.setOnClickListener(this)

        binding?.ivCapability?.isChecked = isCapabilitySwitchOn()
        msdkInfoVm.msdkInfo.observe(viewLifecycleOwner) {
            binding?.ivCapability?.isEnabled = it.productType != ProductType.UNRECOGNIZED
            setDataWithCapability(binding?.ivCapability?.isChecked ?: false)
            Schedulers.single().scheduleDirect {
                if (totalKeyCount == null || capabilityKeyCount == null) {
                    totalKeyCount = dji.sampleV5.aircraft.keyvalue.KeyItemDataUtil.getAllKeyListCount();
                    capabilityKeyCount = CapabilityManager.getInstance().getCapabilityKeyCount(it.productType.name)
                }
            }
        }
        binding?.layoutKeyOperate?.spIndex?.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                setKeyInfo()
                currentKeyItem?.let { updateComponentSpinner(it) }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                //do nothing
            }
        }
        binding?.ivCapability?.setOnCheckedChangeListener { _, enable ->
            if (enable) {
                capabilityKeyCount?.let { showToast(binding?.ivCapability?.text.toString() + " count:$it") }
            } else {
                totalKeyCount?.let { showToast(binding?.ivCapability?.text.toString() + " count:$it") }
            }
            setDataWithCapability(enable)
        }
    }

    private fun initViewAndListener(view: View) {
        binding?.tvOperateTitleLyt?.setOnClickListener {
            channelTypeFilterOperate()
        }
        binding?.llFilterContainer?.setOnClickListener {
            keyFilterOperate()
        }

        binding?.etFilter?.addTextChangedListener(object : TextWatcher {
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
        binding?.recyclerView?.layoutManager = LinearLayoutManager(activity)
        binding?.recyclerView?.adapter = cameraParamsAdapter
        binding?.layoutKeyOperate?.tvTip?.movementMethod = ScrollingMovementMethod.getInstance()
        binding?.layoutKeyOperate?.tvResult?.movementMethod = ScrollingMovementMethod.getInstance()
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

        @SuppressLint("NotifyDataSetChanged")
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
                binding?.layoutKeyOperate?.tvResult?.text = appendLogMessageRecord(t.toString())
                scrollToBottom()
            }

        }

    private fun scrollToBottom() {
        val tvResult = binding?.layoutKeyOperate?.tvResult
        tvResult?.let {
            val scrollOffset = (it.layout.getLineTop(it.lineCount) - it.height)
            if (scrollOffset > 0) {
                it.scrollTo(0, scrollOffset)
            } else {
                it.scrollTo(0, 0)
            }
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
            binding?.layoutKeyOperate?.tvResult?.text = appendLogMessageRecord(t)
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
        binding?.layoutKeyOperate?.tvName?.text = keyItem.name
        binding?.layoutKeyOperate?.btAddCommand?.visibility = if (selectMode) View.VISIBLE else View.GONE
        processListenLogic()
        binding?.layoutKeyOperate?.btGpscoord?.visibility = View.GONE
        binding?.layoutKeyOperate?.tvTip?.visibility = View.GONE
        keyItem.count = System.currentTimeMillis()
        resetSelected()
        binding?.layoutKeyOperate?.btSet?.isEnabled = currentKeyItem!!.canSet()
        binding?.layoutKeyOperate?.btGet?.isEnabled = currentKeyItem!!.canGet()
        binding?.layoutKeyOperate?.btListen?.isEnabled = currentKeyItem!!.canListen()
        binding?.layoutKeyOperate?.btUnlistenall?.isEnabled = currentKeyItem!!.canListen()
        binding?.layoutKeyOperate?.btAction?.isEnabled = currentKeyItem!!.canAction()
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
            binding?.layoutKeyOperate?.spSubtype?.adapter = adapter
            binding?.layoutKeyOperate?.tvSubtype?.text = "lenstype"
            val defalutIndex = list.indexOf("DEFAULT")
            if (defalutIndex != -1) {
                binding?.layoutKeyOperate?.spSubtype?.setSelection(defalutIndex)
            }
        } else {
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                requireContext().resources.getStringArray(R.array.sub_type_arrays)
            )
            binding?.layoutKeyOperate?.spSubtype?.adapter = adapter
            binding?.layoutKeyOperate?.tvSubtype?.text = "subtype"
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
            binding?.layoutKeyOperate?.btListen?.text = "Listen"
            binding?.layoutKeyOperate?.tvName?.text = ""
            return
        }
        val needShowListenView =
            currentKeyItem!!.canListen() && currentKeyItem!!.listenHolder is KeyValueFragment && Util.isNotBlank(
                currentKeyItem!!.listenRecord
            )
        if (needShowListenView) {
            binding?.layoutKeyOperate?.tvTip?.visibility = View.VISIBLE
            binding?.layoutKeyOperate?.tvTip?.text = currentKeyItem!!.getListenRecord()
        } else {
            binding?.layoutKeyOperate?.tvTip?.visibility = View.GONE
            binding?.layoutKeyOperate?.tvTip?.setText(R.string.operate_listen_record_tips)
        }
        if (currentKeyItem!!.listenHolder == null) {
            binding?.layoutKeyOperate?.btListen?.text = "Listen"
        } else {
            binding?.layoutKeyOperate?.btListen?.text = "UNListen"
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

            dji.sampleV5.aircraft.keyvalue.ChannelType.INTELLIGENT_BOX -> {
                tips = Util.getString(R.string.intelligent_box)
                dji.sampleV5.aircraft.keyvalue.KeyItemDataUtil.initIntelligentBoxList(intelligentBoxList)
                currentKeyItemList.addAll(intelligentBoxList)
            }


            else -> {
                LogUtils.d(TAG, "nothing to do")
            }
        }
        for (item in currentKeyItemList) {
            item.isItemSelected = false;
            val count = keyValuesharedPreferences?.getLong(item.toString(), 0L)
            if (count != null && count != 0L) {
                item.count = count
            }
        }

        binding?.tvOperateTitle?.text = tips
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
            binding?.tvCapablity?.text = "Officially released key"
        } else {
            binding?.tvCapablity?.text = "All key"
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
        binding?.etFilter?.setText("")
        cameraParamsAdapter?.filter?.filter("")
    }

    private fun isCapabilitySwitchOn(): Boolean {
        return DjiSharedPreferencesManager.getBoolean(context, CAPABILITY_ENABLE, false)
    }

    private fun setKeyCount(count: Int) {
        binding?.tvCount?.text = "(${count})";
    }

    override fun onClick(view: View) {
        if (Util.isBlank(binding?.layoutKeyOperate?.tvName?.text?.toString()) || currentKeyItem == null) {
            showToast("please select key first")
            return
        }
        setKeyInfo()

        when (view.id) {
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
                binding?.layoutKeyOperate?.tvResult?.text = ""
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
                        it,
                        CapabilityManager.getInstance().componentIndex
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
        sortlist.sort()
        dji.sampleV5.aircraft.keyvalue.KeyValueDialogUtil.showFilterListWindow(
            binding?.llChannelFilterContainer,
            sortlist
        ) { item -> itemClickCallback.actionChange(item) }
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
            binding?.tvOperateTitle,
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
        return ComponentIndexType.valueOf(compentName).value()
    }

    private fun setKeyInfo() {
        if (currentKeyItem == null) {
            return
        }
        try {
            val index = getComponentIndex(binding?.layoutKeyOperate?.spIndex?.selectedItem?.toString() ?: "")

            if (index != -1) {
                currentKeyItem!!.componetIndex = index
                CapabilityManager.getInstance().setComponetIndex(index)
            }
            val subtype: Int
            if (ComponentType.find(currentKeyItem!!.keyInfo.componentType) == ComponentType.CAMERA && isCapabilitySwitchOn()) {
                subtype = getCameraSubIndex(LENS_TAG + binding?.layoutKeyOperate?.spSubtype?.selectedItem?.toString())

            } else {
                subtype = binding?.layoutKeyOperate?.spSubtype?.selectedItem?.toString()?.toInt() ?: -1
            }

            if (subtype != -1) {
                currentKeyItem!!.subComponetType = subtype
            }
            val subIndex = binding?.layoutKeyOperate?.spSubindex?.selectedItem?.toString()?.toInt() ?: -1
            if (subIndex != -1) {
                currentKeyItem!!.subComponetIndex = subIndex
            }
            LogUtils.i(LogPath.SAMPLE,"setKeyInfo:",JsonUtil.toJson(currentKeyItem))
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
        val listenHolder = currentKeyItem!!.listenHolder
        if (listenHolder == null) {
            currentKeyItem!!.listen(this)
            currentKeyItem!!.setKeyOperateCallBack(keyItemOperateCallBack)
            binding?.layoutKeyOperate?.btListen?.text = "Un-Listen"
        } else if (listenHolder is KeyValueFragment) {
            currentKeyItem!!.cancelListen(this)
            binding?.layoutKeyOperate?.btListen?.text = "Listen"
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
                binding?.layoutKeyOperate?.btSet as View,
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
                binding?.layoutKeyOperate?.btSet as View,
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