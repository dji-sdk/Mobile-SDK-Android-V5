package com.dji.open.pilot.plugin.sampleitem.keymanager

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.BaseDemoActivity
import com.dji.open.pilot.plugin.common.globalViewModels
import com.dji.open.pilot.plugin.databinding.ActivityKeyDemoBinding
import com.dji.open.pilot.plugin.sampleitem.keymanager.keyvalue.CapabilityKeyChecker
import com.dji.open.pilot.plugin.sampleitem.keymanager.keyvalue.ChannelType
import com.dji.open.pilot.plugin.sampleitem.keymanager.keyvalue.EnumItem
import com.dji.open.pilot.plugin.sampleitem.keymanager.keyvalue.KeyItem
import com.dji.open.pilot.plugin.sampleitem.keymanager.keyvalue.KeyItemActionListener
import com.dji.open.pilot.plugin.sampleitem.keymanager.keyvalue.KeyItemAdapter
import com.dji.open.pilot.plugin.sampleitem.keymanager.keyvalue.KeyItemDataUtil
import com.dji.open.pilot.plugin.sampleitem.keymanager.keyvalue.KeyItemHelper
import com.dji.open.pilot.plugin.sampleitem.keymanager.keyvalue.KeyUiUtil
import com.dji.open.pilot.plugin.sampleitem.keymanager.keyvalue.KeyValueDialogUtil
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
import dji.v5.ux.util.ToastUtils.showToast
import io.reactivex.rxjava3.schedulers.Schedulers
import java.text.SimpleDateFormat
import java.util.Date

/**
 * KeyManager 演示页，由 android-sdk-v5-sample 的 KeyValueFragment 迁移而来。
 */
class KeyDemoActivity : BaseDemoActivity(), View.OnClickListener {

    private val TAG = LogUtils.getTag("KeyDemoActivity")

    private lateinit var binding: ActivityKeyDemoBinding
    private val viewModel: KeyDemoViewModel by globalViewModels()

    private var currentChannelType: ChannelType = ChannelType.CHANNEL_TYPE_CAMERA
    private val currentChannelList: List<ChannelType> = ChannelType.values().toList()
    private var currentKeyItem: KeyItem<*, *>? = null
    private val currentKeyItemList: MutableList<KeyItem<*, *>> = ArrayList()
    private val data: MutableList<KeyItem<*, *>> = ArrayList()
    private var keyItemAdapter: KeyItemAdapter? = null
    private var keyValuesharedPreferences: SharedPreferences? = null
    private val logMessage = StringBuilder()

    private var totalKeyCount: Int? = null
    private var capabilityKeyCount: Int? = null

    private var isSuppressingFilterWatcher = false

    private val batteryKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    private val gimbalKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    private val cameraKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    private val flightAssistantKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    private val flightControlKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    private val airlinkKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    private val remoteControllerKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    private val productKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    private val rtkBaseKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    private val rtkMobileKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    private val ocuSyncKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    private val radarKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    private val bleList: MutableList<KeyItem<*, *>> = ArrayList()
    private val mobileNetworkKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    private val mobileNetworkLinkRCKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    private val onBoardKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    private val payloadKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    private val lidarKeyList: MutableList<KeyItem<*, *>> = ArrayList()
    private val intelligentBoxList: MutableList<KeyItem<*, *>> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKeyDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.ivBack.setOnClickListener { finish() }
        initLocalData()
        initView()
        initRemoteData()
    }

    override fun onResume() {
        super.onResume()
        processChannelInfo()
    }

    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    private fun initLocalData() {
        data.clear()
        keyItemAdapter = KeyItemAdapter(this, data, itemClickCallback)
        keyValuesharedPreferences = getSharedPreferences(HIGH_FREQUENCY_KEY_SP_NAME, Context.MODE_PRIVATE)
    }

    private fun initView() {
        initViewAndListener()
        binding.layoutKeyOperate.tvResult.setOnLongClickListener {
            val cmb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cmb.text = binding.layoutKeyOperate.tvResult.text.toString()
            true
        }
        binding.layoutKeyOperate.btGet.setOnClickListener(this)
        binding.layoutKeyOperate.btSet.setOnClickListener(this)
        binding.layoutKeyOperate.btListen.setOnClickListener(this)
        binding.layoutKeyOperate.btAction.setOnClickListener(this)
        binding.layoutKeyOperate.btnClearlog.setOnClickListener(this)
        binding.layoutKeyOperate.btUnlistenall.setOnClickListener(this)
        binding.ivQuestionMark.setOnClickListener(this)

        binding.ivCapability.isChecked = isCapabilitySwitchOn()
        viewModel.productTypeName.observe(this) { productTypeName ->
            binding.ivCapability.isEnabled =
                productTypeName != null && productTypeName != ProductType.UNRECOGNIZED.name
            setDataWithCapability(binding.ivCapability.isChecked)
            Schedulers.single().scheduleDirect {
                if (totalKeyCount == null || capabilityKeyCount == null) {
                    totalKeyCount = KeyItemDataUtil.getAllKeyListCount()
                    capabilityKeyCount = productTypeName?.let {
                        CapabilityManager.getInstance().getCapabilityKeyCount(it)
                    }
                }
            }
        }
        val indexList = resources.getStringArray(R.array.component_index_arrays).toList()
        binding.layoutKeyOperate.spIndex.adapter = createAntdSpinnerAdapter(indexList)

        val subIndexList = resources.getStringArray(R.array.sub_index_arrays).toList()
        binding.layoutKeyOperate.spSubindex.adapter = createAntdSpinnerAdapter(subIndexList)

        val subTypeList = resources.getStringArray(R.array.sub_type_arrays).toList()
        binding.layoutKeyOperate.spSubtype.adapter = createAntdSpinnerAdapter(subTypeList)

        binding.layoutKeyOperate.spIndex.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                setKeyInfo()
                currentKeyItem?.let { updateComponentSpinner(it) }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                //do nothing
            }
        }
        binding.ivCapability.setOnCheckedChangeListener { _, enable ->
            refreshChannelList(enable)
            if (enable) {
                capabilityKeyCount?.let { showToast(binding.ivCapability.text.toString() + " count:$it") }
            } else {
                totalKeyCount?.let { showToast(binding.ivCapability.text.toString() + " count:$it") }
            }
            setDataWithCapability(enable)
        }

        binding.spChannel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val channelType = parent?.getItemAtPosition(position) as? ChannelType ?: return
                if (channelType != currentChannelType) {
                    currentChannelType = channelType
                    currentKeyItem = null
                    resetSearchFilter()
                    processChannelInfo()
                    processListenLogic()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        refreshChannelList(isCapabilitySwitchOn())
    }

    private fun initViewAndListener() {
        binding.etFilter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                //Do Something
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                //Do Something
            }

            override fun afterTextChanged(s: Editable) {
                if (isSuppressingFilterWatcher) {
                    return
                }
                applyFilter(s.toString())
            }
        })
    }

    private fun initRemoteData() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = keyItemAdapter
        binding.layoutKeyOperate.tvTip.movementMethod = ScrollingMovementMethod.getInstance()
        binding.layoutKeyOperate.tvResult.movementMethod = ScrollingMovementMethod.getInstance()
    }

    /**
     * key列表点击回调
     */
    private val itemClickCallback: KeyItemActionListener<KeyItem<*, *>?> =
        object : KeyItemActionListener<KeyItem<*, *>?> {

            @SuppressLint("NotifyDataSetChanged")
            override fun actionChange(keyItem: KeyItem<*, *>?) {
                if (keyItem == null) {
                    return
                }
                initKeyInfo(keyItem)
                keyItemAdapter?.notifyDataSetChanged()
            }
        }

    /**
     * key操作结果回调
     */
    private val keyItemOperateCallBack: KeyItemActionListener<Any> =
        KeyItemActionListener<Any> { t ->
            t?.let {
                binding.layoutKeyOperate.tvResult.text = appendLogMessageRecord(it.toString())
                scrollToBottom()
            }
        }

    /**
     * 推送结果回调
     */
    private val pushCallback: KeyItemActionListener<String> =
        KeyItemActionListener<String> { t ->
            binding.layoutKeyOperate.tvResult.text = appendLogMessageRecord(t)
            scrollToBottom()
        }

    private fun scrollToBottom() {
        val tvResult = binding.layoutKeyOperate.tvResult
        val scrollOffset = (tvResult.layout.getLineTop(tvResult.lineCount) - tvResult.height)
        if (scrollOffset > 0) {
            tvResult.scrollTo(0, scrollOffset)
        } else {
            tvResult.scrollTo(0, 0)
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
     * 初始化Key的信息
     */
    private fun initKeyInfo(keyItem: KeyItem<*, *>) {
        currentKeyItem = keyItem
        keyItem.setKeyOperateCallBack(keyItemOperateCallBack)
        binding.layoutKeyOperate.tvName.text = keyItem.name
        binding.layoutKeyOperate.btAddCommand.visibility = View.GONE
        processListenLogic()
        binding.layoutKeyOperate.btGpscoord.visibility = View.GONE
        binding.layoutKeyOperate.tvTip.visibility = View.GONE
        keyItem.count = System.currentTimeMillis()
        resetSelected()
        binding.layoutKeyOperate.btSet.isEnabled = keyItem.canSet()
        binding.layoutKeyOperate.btGet.isEnabled = keyItem.canGet()
        binding.layoutKeyOperate.btListen.isEnabled = keyItem.canListen()
        binding.layoutKeyOperate.btUnlistenall.isEnabled = keyItem.canListen()
        binding.layoutKeyOperate.btAction.isEnabled = keyItem.canAction()
        keyValuesharedPreferences?.edit()?.putLong(keyItem.toString(), keyItem.count)?.apply()
        keyItem.isItemSelected = true

        updateComponentSpinner(keyItem)
    }

    private fun updateComponentSpinner(keyItem: KeyItem<*, *>) {
        val componentType = ComponentType.find(keyItem.keyInfo.componentType)
        if (componentType == ComponentType.CAMERA && isCapabilitySwitchOn()) {
            val list = CapabilityManager.getInstance().getSupportLens("Key" + keyItem.name)
            binding.layoutKeyOperate.spSubtype.adapter = createAntdSpinnerAdapter(list)
            binding.layoutKeyOperate.tvSubtype.text = "lenstype"
            val defalutIndex = list.indexOf("DEFAULT")
            if (defalutIndex != -1) {
                binding.layoutKeyOperate.spSubtype.setSelection(defalutIndex)
            }
        } else {
            val subTypes = resources.getStringArray(R.array.sub_type_arrays).toList()
            binding.layoutKeyOperate.spSubtype.adapter = createAntdSpinnerAdapter(subTypes)
            binding.layoutKeyOperate.tvSubtype.text = "subtype"
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
        val keyItem = currentKeyItem
        if (keyItem == null) {
            binding.layoutKeyOperate.btListen.text = "Listen"
            binding.layoutKeyOperate.tvName.text = ""
            return
        }
        val needShowListenView =
            keyItem.canListen() && keyItem.listenHolder is KeyDemoActivity &&
                KeyUiUtil.isNotBlank(keyItem.listenRecord)
        if (needShowListenView) {
            binding.layoutKeyOperate.tvTip.visibility = View.VISIBLE
            binding.layoutKeyOperate.tvTip.text = keyItem.listenRecord
        } else {
            binding.layoutKeyOperate.tvTip.visibility = View.GONE
            binding.layoutKeyOperate.tvTip.setText(R.string.operate_listen_record_tips)
        }
        if (keyItem.listenHolder == null) {
            binding.layoutKeyOperate.btListen.text = "Listen"
        } else {
            binding.layoutKeyOperate.btListen.text = "UNListen"
        }
    }

    /**
     * 根据不同类型入口，初始化不同数据
     */
    private fun processChannelInfo() {
        currentKeyItemList.clear()
        when (currentChannelType) {
            ChannelType.CHANNEL_TYPE_BATTERY -> {
                KeyItemDataUtil.initBatteryKeyList(batteryKeyList)
                currentKeyItemList.addAll(batteryKeyList)
            }

            ChannelType.CHANNEL_TYPE_GIMBAL -> {
                KeyItemDataUtil.initGimbalKeyList(gimbalKeyList)
                currentKeyItemList.addAll(gimbalKeyList)
            }

            ChannelType.CHANNEL_TYPE_CAMERA -> {
                KeyItemDataUtil.initCameraKeyList(cameraKeyList)
                currentKeyItemList.addAll(cameraKeyList)
            }

            ChannelType.CHANNEL_TYPE_FLIGHT_ASSISTANT -> {
                KeyItemDataUtil.initFlightAssistantKeyList(flightAssistantKeyList)
                currentKeyItemList.addAll(flightAssistantKeyList)
            }

            ChannelType.CHANNEL_TYPE_FLIGHT_CONTROL -> {
                KeyItemDataUtil.initFlightControllerKeyList(flightControlKeyList)
                currentKeyItemList.addAll(flightControlKeyList)
            }

            ChannelType.CHANNEL_TYPE_AIRLINK -> {
                KeyItemDataUtil.initAirlinkKeyList(airlinkKeyList)
                currentKeyItemList.addAll(airlinkKeyList)
            }

            ChannelType.CHANNEL_TYPE_REMOTE_CONTROLLER -> {
                KeyItemDataUtil.initRemoteControllerKeyList(remoteControllerKeyList)
                currentKeyItemList.addAll(remoteControllerKeyList)
            }

            ChannelType.CHANNEL_TYPE_BLE -> {
                KeyItemDataUtil.initBleKeyList(bleList)
                currentKeyItemList.addAll(bleList)
            }

            ChannelType.CHANNEL_TYPE_PRODUCT -> {
                KeyItemDataUtil.initProductKeyList(productKeyList)
                currentKeyItemList.addAll(productKeyList)
            }

            ChannelType.CHANNEL_TYPE_RTK_BASE_STATION -> {
                KeyItemDataUtil.initRtkBaseStationKeyList(rtkBaseKeyList)
                currentKeyItemList.addAll(rtkBaseKeyList)
            }

            ChannelType.CHANNEL_TYPE_RTK_MOBILE_STATION -> {
                KeyItemDataUtil.initRtkMobileStationKeyList(rtkMobileKeyList)
                currentKeyItemList.addAll(rtkMobileKeyList)
            }

            ChannelType.CHANNEL_TYPE_OCU_SYNC -> {
                KeyItemDataUtil.initOcuSyncKeyList(ocuSyncKeyList)
                currentKeyItemList.addAll(ocuSyncKeyList)
            }

            ChannelType.CHANNEL_TYPE_RADAR -> {
                KeyItemDataUtil.initRadarKeyList(radarKeyList)
                currentKeyItemList.addAll(radarKeyList)
            }

            ChannelType.CHANNEL_TYPE_MOBILE_NETWORK -> {
                KeyItemDataUtil.initMobileNetworkKeyList(mobileNetworkKeyList)
                KeyItemDataUtil.initMobileNetworkLinkRCKeyList(mobileNetworkLinkRCKeyList)
                currentKeyItemList.addAll(mobileNetworkKeyList)
                currentKeyItemList.addAll(mobileNetworkLinkRCKeyList)
            }

            ChannelType.CHANNEL_TYPE_ON_BOARD -> {
                KeyItemDataUtil.initOnboardKeyList(onBoardKeyList)
                currentKeyItemList.addAll(onBoardKeyList)
            }

            ChannelType.CHANNEL_TYPE_ON_PAYLOAD -> {
                KeyItemDataUtil.initPayloadKeyList(payloadKeyList)
                currentKeyItemList.addAll(payloadKeyList)
            }

            ChannelType.CHANNEL_TYPE_LIDAR -> {
                KeyItemDataUtil.initLidarKeyList(lidarKeyList)
                currentKeyItemList.addAll(lidarKeyList)
            }

            ChannelType.INTELLIGENT_BOX -> {
                KeyItemDataUtil.initIntelligentBoxList(intelligentBoxList)
                currentKeyItemList.addAll(intelligentBoxList)
            }
        }
        for (item in currentKeyItemList) {
            item.isItemSelected = false
            val count = keyValuesharedPreferences?.getLong(item.toString(), 0L)
            if (count != null && count != 0L) {
                item.count = count
            }
        }

        setDataWithCapability(isCapabilitySwitchOn())
    }

    private fun setDataWithCapability(enable: Boolean) {
        val showList: MutableList<KeyItem<*, *>> = ArrayList(currentKeyItemList)
        changeCurrentList(enable, showList)
        data.clear()
        data.addAll(showList)
        data.sortWith { o1, o2 -> o1.name?.compareTo(o2.name) ?: 0 }

        resetSelected()
        currentKeyItem?.let { current ->
            data.find { it.name == current.name }?.let {
                it.isItemSelected = true
                currentKeyItem = it
            }
        }

        // 数据就绪后统一过滤，列表和 count 只接受同一最新版本的结果。
        applyFilter(binding.etFilter.text?.toString().orEmpty())

        DjiSharedPreferencesManager.putBoolean(this, CAPABILITY_ENABLE, enable)
        binding.tvCapablity.setText(if (enable) R.string.key_capability_official else R.string.key_capability_all)
    }

    private fun applyFilter(query: String) {
        keyItemAdapter?.filterKeys(data, query) { filteredCount ->
            setKeyCount(filteredCount)
        }
    }

    /**
     * 能力集开关打开，并且获取的产品名称在能力集列表中则更新列表
     */
    private fun changeCurrentList(enable: Boolean, showList: MutableList<KeyItem<*, *>>) {
        val type = viewModel.getProductTypeName()
        if (enable && type != null && CapabilityManager.getInstance().isProductSupported(type)) {
            val iterator = showList.iterator()
            while (iterator.hasNext()) {
                if (isNeedRemove("Key" + iterator.next().name)) {
                    iterator.remove()
                }
            }
        }
    }

    private fun isNeedRemove(keyName: String): Boolean {
        var isNeedRemove = false
        val type = viewModel.getProductTypeName()

        val cameraType = KeyManager.getInstance().getValue(
            KeyTools.createKey(
                CameraKey.KeyCameraType,
                CapabilityManager.getInstance().componentIndex
            )
        )

        when (currentChannelType) {
            ChannelType.CHANNEL_TYPE_CAMERA -> {
                if (!CapabilityManager.getInstance()
                        .isCameraKeySupported(type, cameraType?.name, keyName)
                ) {
                    isNeedRemove = true
                }
            }

            ChannelType.CHANNEL_TYPE_AIRLINK -> {
                if (!CapabilityManager.getInstance()
                        .isKeySupported(type, "", ComponentType.AIRLINK, keyName)
                ) {
                    isNeedRemove = true
                }
            }

            ChannelType.CHANNEL_TYPE_GIMBAL -> {
                if (!CapabilityManager.getInstance()
                        .isKeySupported(type, "", ComponentType.GIMBAL, keyName)
                ) {
                    isNeedRemove = true
                }
            }

            ChannelType.CHANNEL_TYPE_REMOTE_CONTROLLER -> {
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
     * 清空搜索框，不在新分类数据填充前发起旧数据过滤。
     */
    private fun resetSearchFilter() {
        isSuppressingFilterWatcher = true
        try {
            binding.etFilter.setText("")
        } finally {
            isSuppressingFilterWatcher = false
        }
    }

    private fun isCapabilitySwitchOn(): Boolean {
        return DjiSharedPreferencesManager.getBoolean(this, CAPABILITY_ENABLE, false)
    }

    private fun setKeyCount(count: Int) {
        binding.tvCount.text = "($count)"
    }

    private fun refreshChannelList(enable: Boolean) {
        val showChannelList = if (enable) CAPABILITY_CHANNELS else currentChannelList
        val adapter = createAntdSpinnerAdapter(showChannelList)
        binding.spChannel.adapter = adapter
        val selectedIndex = showChannelList.indexOf(currentChannelType)
        if (selectedIndex >= 0) {
            binding.spChannel.setSelection(selectedIndex)
        } else if (showChannelList.isNotEmpty()) {
            currentChannelType = showChannelList[0]
            binding.spChannel.setSelection(0)
            currentKeyItem = null
            resetSearchFilter()
            processChannelInfo()
            processListenLogic()
        }
    }

    private fun <T> createAntdSpinnerAdapter(items: List<T>): ArrayAdapter<T> {
        val adapter = object : ArrayAdapter<T>(this, R.layout.item_spinner_antd, ArrayList(items)) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.text = getItem(position)?.toString().orEmpty()
                view.setTextColor(Color.parseColor("#333333"))
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                val listView = parent as? AdapterView<*>
                val isSelected = listView?.selectedItemPosition == position

                view.layoutParams = AbsListView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                val density = context.resources.displayMetrics.density
                val paddingH = (12 * density).toInt()
                val paddingV = (8 * density).toInt()
                view.setPadding(paddingH, paddingV, paddingH, paddingV)

                if (isSelected) {
                    view.setBackgroundColor(Color.parseColor("#E6F7FF"))
                    view.setTextColor(Color.parseColor("#1890FF"))
                } else {
                    view.setBackgroundResource(R.drawable.bg_spinner_item_selector)
                    view.setTextColor(Color.parseColor("#333333"))
                }
                return view
            }
        }
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown_antd)
        return adapter
    }

    override fun onClick(view: View) {
        if (KeyUiUtil.isBlank(binding.layoutKeyOperate.tvName.text?.toString()) || currentKeyItem == null) {
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
                binding.layoutKeyOperate.tvResult.text = ""
                logMessage.delete(0, logMessage.length)
            }

            R.id.iv_question_mark -> {
                val cameraType = KeyManager.getInstance().getValue(
                    KeyTools.createKey(
                        CameraKey.KeyCameraType,
                        CapabilityManager.getInstance().componentIndex
                    )
                )
                val productTypeName = viewModel.getProductTypeName()
                cameraType?.name?.let { cameraName ->
                    productTypeName?.let { productName ->
                        CapabilityKeyChecker.check(
                            productName,
                            cameraName,
                            CapabilityManager.getInstance().componentIndex
                        )
                    }
                }
            }
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

    private fun getComponentIndex(componentName: String): Int {
        return ComponentIndexType.valueOf(componentName).value()
    }

    private fun setKeyInfo() {
        val keyItem = currentKeyItem ?: return
        try {
            val index = getComponentIndex(
                binding.layoutKeyOperate.spIndex.selectedItem?.toString() ?: ""
            )

            if (index != -1) {
                keyItem.componetIndex = index
                CapabilityManager.getInstance().setComponetIndex(index)
            }
            val subtype: Int
            if (ComponentType.find(keyItem.keyInfo.componentType) == ComponentType.CAMERA &&
                isCapabilitySwitchOn()
            ) {
                subtype = getCameraSubIndex(
                    LENS_TAG + binding.layoutKeyOperate.spSubtype.selectedItem?.toString()
                )
            } else {
                subtype = binding.layoutKeyOperate.spSubtype.selectedItem?.toString()?.toInt() ?: -1
            }

            if (subtype != -1) {
                keyItem.subComponetType = subtype
            }
            val subIndex = binding.layoutKeyOperate.spSubindex.selectedItem?.toString()?.toInt() ?: -1
            if (subIndex != -1) {
                keyItem.subComponetIndex = subIndex
            }
            LogUtils.i(LogPath.SAMPLE, "setKeyInfo:", JsonUtil.toJson(keyItem))
        } catch (e: Exception) {
            LogUtils.e(TAG, e.message)
        }
    }

    /**
     * 获取操作
     */
    private fun get() {
        val keyItem = currentKeyItem ?: return
        if (!keyItem.canGet()) {
            showToast("not support get")
            return
        }
        keyItem.doGet()
    }

    private fun unListenAll() {
        release()
        processListenLogic()
    }

    /**
     * Listen操作
     */
    private fun listen() {
        val keyItem = currentKeyItem ?: return
        if (!keyItem.canListen()) {
            showToast("not support listen")
            return
        }
        keyItem.setPushCallBack(pushCallback)
        val listenHolder = keyItem.listenHolder
        if (listenHolder == null) {
            keyItem.listen(this)
            keyItem.setKeyOperateCallBack(keyItemOperateCallBack)
            binding.layoutKeyOperate.btListen.text = "Un-Listen"
        } else if (listenHolder is KeyDemoActivity) {
            keyItem.cancelListen(this)
            binding.layoutKeyOperate.btListen.text = "Listen"
        }
        processListenLogic()
    }

    /**
     * 设置操作
     */
    private fun set() {
        val keyItem = currentKeyItem ?: return
        if (!keyItem.canSet()) {
            showToast("not support set")
            return
        }
        if (keyItem.param is BoolMsg) {
            processBoolMsgDlg(keyItem)
            return
        }
        if (keyItem.subItemMap.isNotEmpty()) {
            KeyItemHelper.processSubListLogic(
                binding.layoutKeyOperate.btSet as View,
                keyItem.param,
                keyItem.subItemMap as Map<String?, List<EnumItem>>,
                object : KeyItemActionListener<String?> {

                    override fun actionChange(paramJsonStr: String?) {
                        if (KeyUiUtil.isBlank(paramJsonStr)) {
                            return
                        }
                        currentKeyItem?.doSet(paramJsonStr)
                    }
                })
        } else {
            KeyValueDialogUtil.showInputDialog(
                this,
                keyItem,
                object : KeyItemActionListener<String?> {
                    override fun actionChange(s: String?) {
                        if (KeyUiUtil.isBlank(s)) {
                            return
                        }
                        currentKeyItem?.doSet(s)
                    }
                })
        }
    }

    private fun processBoolMsgDlg(keyitem: KeyItem<*, *>) {
        val boolValueList: MutableList<String> = ArrayList()
        boolValueList.add("false")
        boolValueList.add("true")

        KeyValueDialogUtil.showSingleChoiceDialog(
            this,
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
        val keyItem = currentKeyItem ?: return
        if (!keyItem.canAction()) {
            showToast("not support action")
            return
        }

        if (keyItem.keyInfo.typeConverter === EmptyValueConverter.converter) {
            keyItem.doAction("")
        } else if (keyItem.subItemMap.isNotEmpty()) {
            KeyItemHelper.processSubListLogic(
                binding.layoutKeyOperate.btSet as View,
                keyItem.param,
                keyItem.subItemMap as Map<String?, List<EnumItem>>,
                object : KeyItemActionListener<String?> {
                    override fun actionChange(paramJsonStr: String?) {
                        if (KeyUiUtil.isBlank(paramJsonStr)) {
                            return
                        }
                        currentKeyItem?.doAction(paramJsonStr)
                    }
                })
        } else if (keyItem.paramJsonStr != null && keyItem.paramJsonStr == "{}") {
            keyItem.doAction(keyItem.paramJsonStr)
        } else {
            KeyValueDialogUtil.showInputDialog(
                this,
                keyItem
            ) { s -> currentKeyItem?.doAction(s) }
        }
    }

    /**
     * 注销Listen，移除业务回调
     */
    private fun releaseKeyInfo(list: MutableList<KeyItem<*, *>>?) {
        list?.forEach {
            it.removeCallBack()
            it.cancelListen(this)
        }
    }

    fun release() {
        currentKeyItem?.cancelListen(this)
        releaseKeyInfo(batteryKeyList)
        releaseKeyInfo(gimbalKeyList)
        releaseKeyInfo(cameraKeyList)
        releaseKeyInfo(flightAssistantKeyList)
        releaseKeyInfo(flightControlKeyList)
        releaseKeyInfo(airlinkKeyList)
        releaseKeyInfo(productKeyList)
        releaseKeyInfo(rtkBaseKeyList)
        releaseKeyInfo(rtkMobileKeyList)
        releaseKeyInfo(remoteControllerKeyList)
        releaseKeyInfo(radarKeyList)
    }

    companion object {
        private const val CAPABILITY_ENABLE = "capabilityenable"
        private const val HIGH_FREQUENCY_KEY_SP_NAME = "highfrequencykey"
        private const val LENS_TAG = "CAMERA_LENS_"
        private const val LISTEN_RECORD_MAX_LENGTH = 6000

        private val CAPABILITY_CHANNELS = listOf(
            ChannelType.CHANNEL_TYPE_BATTERY,
            ChannelType.CHANNEL_TYPE_AIRLINK,
            ChannelType.CHANNEL_TYPE_CAMERA,
            ChannelType.CHANNEL_TYPE_GIMBAL,
            ChannelType.CHANNEL_TYPE_REMOTE_CONTROLLER,
            ChannelType.CHANNEL_TYPE_FLIGHT_CONTROL
        )
    }
}
