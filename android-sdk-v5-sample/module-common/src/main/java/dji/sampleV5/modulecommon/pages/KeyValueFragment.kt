package dji.sampleV5.modulecommon.pages

import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dji.sampleV5.modulecommon.R
import dji.sampleV5.modulecommon.keyvalue.*
import dji.sampleV5.modulecommon.keyvalue.KeyItemHelper.processSubListLogic
import dji.sampleV5.modulecommon.models.KeyValueVM
import dji.sampleV5.modulecommon.util.ToastUtils.showToast
import dji.sampleV5.modulecommon.util.Util
import dji.sdk.keyvalue.converter.EmptyValueConverter

import kotlinx.android.synthetic.main.fragment_key_list.*
import kotlinx.android.synthetic.main.layout_key_operate.*
import java.lang.Exception
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/5/11
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
 class KeyValueFragment  : DJIFragment() , View.OnClickListener{

    private val keyValueVM: KeyValueVM by activityViewModels()

    var currentChannelType: ChannelType? = ChannelType.CHANNEL_TYPE_CAMERA
    val LISTEN_RECORD_MAX_LENGTH = 2000
    val HIGH_FREQUENCY_KEY_SP_NAME = "highfrequencykey"

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
        if (parent != null) {
            parent.removeView(contentView)
        }
    }

    fun initLocalData() {
        data.clear()
        cameraParamsAdapter = KeyItemAdapter(getActivity(), data, itemClickCallback)
        keyValuesharedPreferences =
            getActivity()?.getSharedPreferences(HIGH_FREQUENCY_KEY_SP_NAME, Context.MODE_PRIVATE)
    }


    private fun initView(view: View) {
        initViewAndListener(view)
        tv_result!!.setOnLongClickListener {
            val cmb = getActivity()
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
        setSPColor(sp_index)
        setSPColor(sp_subindex)
        setSPColor(sp_subtype)
    }
    private fun initViewAndListener(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        tv_operate_title.setOnClickListener {
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
                cameraParamsAdapter?.getFilter()?.filter(s.toString())
            }
        })
    }

    private fun initRemoteData() {
        recyclerView!!.layoutManager = LinearLayoutManager(getActivity())
        recyclerView!!.adapter = cameraParamsAdapter
        tv_tip!!.movementMethod = ScrollingMovementMethod.getInstance()
        tv_result!!.movementMethod = ScrollingMovementMethod.getInstance()
    }

    override fun onDestroyView() {
        super.onDestroyView()

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
        }
    }


    /**
     * key操作结果回调
     */
    val keyItemOperateCallBack: KeyItemActionListener<Any> =
        KeyItemActionListener<Any> { t -> //  processListenLogic();
            t?.let {
                tv_result.text = appendLogMessageRecord(t.toString())
                val scrollOffset = (tv_result!!.layout.getLineTop(tv_result!!.lineCount)
                        - tv_result!!.height)
                if (scrollOffset > 0) {
                    tv_result!!.scrollTo(0, scrollOffset)
                } else {
                    tv_result!!.scrollTo(0, 0)
                }
            }

        }



    private  fun appendLogMessageRecord(appendStr: String): String? {
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

        }


    private  fun setSPColor(sp: Spinner) {
        sp.viewTreeObserver.addOnGlobalLayoutListener {
            (sp.selectedView as TextView).setTextColor(
                Color.WHITE
            )
            (sp.selectedView as TextView).setGravity(
                Gravity.RIGHT
            )
        }
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
        bt_add_command.setVisibility(if (selectMode) View.VISIBLE else View.GONE)
        processListenLogic()
        bt_gpscoord.setVisibility(View.GONE)
        tv_tip.setVisibility(View.GONE)
        keyItem.count = keyItem.count + 1
        bt_set.setEnabled(currentKeyItem!!.canSet())
        bt_get.setEnabled(currentKeyItem!!.canGet())
        bt_listen.setEnabled(currentKeyItem!!.canListen())
        bt_action.setEnabled(currentKeyItem!!.canAction())
        keyValuesharedPreferences?.edit()?.putInt(keyItem.toString(), keyItem.count)?.commit()
    }

    /**
     * 处理监听显示控件
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
            tv_tip.setVisibility(View.VISIBLE)
            tv_tip.setText(currentKeyItem!!.getListenRecord())
        } else {
            tv_tip.setVisibility(View.GONE)
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

        }
        for (item in currentKeyItemList) {
            val count = keyValuesharedPreferences?.getInt(item.toString(), 0)
            if (count != null && count != 0) {
                item.count = count
            }
        }

        tv_operate_title?.text = tips
        data.clear()
        data.addAll(currentKeyItemList)
        cameraParamsAdapter?.notifyDataSetChanged()
    }


    override fun onClick(view: View) {

        if (Util.isBlank(tv_name.text.toString()) || currentKeyItem == null) {
            showToast("please select key first")
            return
        }
        setKeyInfo()

        when(view?.id) {
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
                KeyValueDialogUtil.showNormalDialog(getActivity(), "提示")
            }

        }

    }




    /**
     * key列表条件过滤
     */
    private fun keyFilterOperate() {
        val sortlist: List<KeyItem<*, *>> = ArrayList(currentKeyItemList)
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

    private  fun channelTypeFilterOperate() {
        KeyValueDialogUtil.showChannelFilterListWindow(
            tv_operate_title,
            currentChannelList,
            object :
                KeyItemActionListener<ChannelType?> {
                override fun actionChange(channelType: ChannelType?) {
                    currentChannelType = channelType
                    currentKeyItem = null
                    processChannelInfo()
                    processListenLogic()
                }
            })
    }


    private fun setKeyInfo() {
        if (currentKeyItem == null) {
            return
        }
        try {
            val index = sp_index.getSelectedItem().toString().toInt()
            if (index != -1) {
                currentKeyItem!!.setComponetIndex(index)
            }
            val subtype = sp_subtype.getSelectedItem().toString().toInt()
            if (subtype != -1) {
                currentKeyItem!!.setSubComponetType(subtype)
            }
            val subIndex = sp_subindex.getSelectedItem().toString().toInt()
            if (subIndex != -1) {
                currentKeyItem!!.setSubComponetIndex(subIndex)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 获取操作
     */
    private  fun get() {
        if (!currentKeyItem?.canGet()!!) {
            showToast("not support get")
            return
        }
        currentKeyItem!!.doGet()
    }

    private  fun unListenAll() {
        release()
        processListenLogic()
    }

    /**
     * 监听操作
     */
    private  fun listen() {
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
    private  fun set() {
        if (!currentKeyItem?.canSet()!!) {
            showToast("not support set")
            return
        }
        if (currentKeyItem!!.getSubItemMap().size != 0) {
            processSubListLogic(
                bt_set,
                currentKeyItem!!.getParam(),
                currentKeyItem!!.getSubItemMap() as Map<String?, List<EnumItem>>,
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
                getActivity(),
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

    /**
     * 动作操作
     */
    private  fun action() {
        if (!currentKeyItem?.canAction()!!) {
            showToast("not support action")
            return
        }

        if (currentKeyItem!!.keyInfo.typeConverter === EmptyValueConverter.converter) {
            currentKeyItem?.doAction("")
        } else if (currentKeyItem?.getSubItemMap()!!.isNotEmpty()) {
            processSubListLogic(
                bt_set,
                currentKeyItem?.getParam(),
                currentKeyItem?.getSubItemMap() as Map<String?, List<EnumItem>>,
                object :
                    KeyItemActionListener<String?> {
                    override fun actionChange(paramJsonStr: String?) {
                        if (Util.isBlank(paramJsonStr)) {
                            return
                        }
                        currentKeyItem!!.doAction(paramJsonStr)
                    }
                })
        } else if (currentKeyItem!!.getParamJsonStr() != null && currentKeyItem!!.getParamJsonStr() == "{}") {
            currentKeyItem!!.doAction(currentKeyItem!!.getParamJsonStr())
        } else {
            KeyValueDialogUtil.showInputDialog(
                getActivity(),
                currentKeyItem,
                object :
                    KeyItemActionListener<String?> {
                    override fun actionChange(s: String?) {
                        currentKeyItem!!.doAction(s)
                    }
                })
        }
    }

    /**
     * 注销监听，移除业务回调
     *
     * @param list
     */
    private  fun releaseKeyInfo(list: MutableList<KeyItem<*, *>>?) {
        if (list == null) {
            return
        }
        for (item in list) {
            item.removeCallBack()
            item.cancelListen(this)
        }
        list.clear()
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