package dji.v5.ux.accessory

import android.content.Context
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dji.sdk.keyvalue.value.rtkbasestation.RTKStationConnetState
import dji.sdk.keyvalue.value.rtkbasestation.RTKStationInfo
import dji.v5.manager.aircraft.rtk.RTKCenter
import dji.v5.manager.aircraft.rtk.station.ConnectedRTKStationInfo
import dji.v5.utils.common.LogUtils
import dji.v5.utils.common.StringUtils
import dji.v5.ux.R
import dji.v5.ux.accessory.data.DJIRTKBaseStationConnectInfo
import dji.v5.ux.accessory.data.RtkStationScanAdapter
import dji.v5.ux.accessory.item.RtkGuidanceView
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.getString
import dji.v5.ux.core.extension.hide
import dji.v5.ux.core.extension.show
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.TimeUnit

/**
 * Description :D-RTK2 扫描连接Widget
 *
 * @author: Byte.Cai
 *  date : 2022/9/1
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
private const val TAG = "RTKStationConnectWidget"

class RTKStationConnectWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayoutWidget<Boolean>(context, attrs, defStyleAttr), RtkStationScanAdapter.OnItemClickListener, View.OnClickListener {
    private var rtkStationScanAdapter: RtkStationScanAdapter
    private val stationList = ArrayList<DJIRTKBaseStationConnectInfo>()
    private val searchBt: Button = findViewById(R.id.bt_rtk_signal_search_again)
    private val checkReasonTv: TextView = findViewById(R.id.tv_rtk_signal_problem_checked_reason)
    private val searchIv: ImageView = findViewById(R.id.iv_rtk_signal_search_iv)
    private val stationListView: RecyclerView = findViewById(R.id.rl_rtk_signal_searching_list)
    private val stationScanningView: ConstraintLayout = findViewById(R.id.cl_rtk_has_found)
    private val stationHasNotFoundView: ConstraintLayout = findViewById(R.id.cl_rtk_not_found)
    private var connectState = RTKStationConnetState.UNKNOWN
    private val scanHandler = android.os.Handler(Looper.getMainLooper())
    private var scanTimeOutDisposable: Disposable? = null
    private var isMotorOn = false
    private var firstEnter = false
    private val SCAN_TIME_OUT = 5.0

    private val widgetModel by lazy {
        RTKStationConnectWidgetModel(
            DJISDKModel.getInstance(),
            ObservableInMemoryKeyedStore.getInstance(),
            RTKCenter.getInstance()
        )
    }

    init {
        //初始化stationTRTK列表
        val layoutManager = LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false)
        stationListView.layoutManager = layoutManager
        rtkStationScanAdapter = RtkStationScanAdapter(getContext(), stationList)
        stationListView.adapter = rtkStationScanAdapter
        searchBt.setOnClickListener(this)
        searchIv.setOnClickListener(this)
        rtkStationScanAdapter.setOnItemClickListener(this)
        initCheckReasonContent()

    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_widget_rtk_connect_status_layout, this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
        }
        firstEnter = true
    }

    /**
     * 初始化CheckReason View的属性
     */
    private fun initCheckReasonContent() {
        val reason: String = StringUtils.getResStr(R.string.uxsdk_rtk_base_station_not_found_reason)
        val description: String = StringUtils.getResStr(R.string.uxsdk_rtk_connect_description)
        //设置mCheckReasonTv的部分内容可点击属性
        val spannableStringBuilder = SpannableStringBuilder()
        spannableStringBuilder.append(reason).append(" ").append(description)
        //mCheckReasonTv的部分内容点击事件
        val clickableSpan: ClickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                //RTK引导界面
                val guidanceView = RtkGuidanceView(context)
                guidanceView.showPopupWindow(view)
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.isUnderlineText = false
            }
        }
        //点击范围为description部分
        spannableStringBuilder.setSpan(clickableSpan, reason.length, spannableStringBuilder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        //设置可点击部分的字体颜色
        val foregroundColorSpan = ForegroundColorSpan(resources.getColor(R.color.uxsdk_blue_highlight))
        spannableStringBuilder.setSpan(foregroundColorSpan, reason.length + 1, spannableStringBuilder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        //设置为超链接方式
        checkReasonTv.movementMethod = LinkMovementMethod.getInstance()
        checkReasonTv.text = spannableStringBuilder
    }


    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
        super.onDetachedFromWindow()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.iv_rtk_signal_search_iv,
            R.id.bt_rtk_signal_search_again,
            -> startScanning()
        }
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.connectedRTKStationInfo.observeOn(SchedulerProvider.ui()).subscribe {
            handleReconnectedStationInfo(it)
        })
        addReaction(widgetModel.isMotorOn.subscribe {
            isMotorOn = it
        })
        addReaction(widgetModel.stationConnectStatus.observeOn(SchedulerProvider.ui()).subscribe {
            updateConnectStatus(it)
        })
        addReaction(widgetModel.stationList.observeOn(SchedulerProvider.ui()).subscribe {
            handleStationRTKList(it)
        })

    }

    override fun getIdealDimensionRatioString(): String? {
        return getString(R.string.uxsdk_widget_rtk_keep_status_ratio)
    }


    private fun updateRefreshUI(boolean: Boolean) {
        //底部的重新扫描按钮是否可点击
        searchBt.isClickable = boolean
        searchBt.setBackgroundResource(if (boolean) R.drawable.uxsdk_bg_white_radius else R.drawable.uxsdk_bg_gray_radius)
        searchIv.visibility = if (boolean) visibility else GONE
    }

    private fun handleStationRTKList(list: List<DJIRTKBaseStationConnectInfo>?) {
        //过滤重复的数据，防止界面重新刷新
        if (checkNeedUpdateUI(list)) {
            stationList.clear()
            LogUtils.i(TAG, "has found rtk，clear stationList")
            list?.let {
                for (i in it) {
                    LogUtils.i(TAG, "stationName=${i.rtkStationName},signalLevel=${i.signalLevel}")
                    stationList.add(i)
                }
            }
            searchIv.setImageResource(R.drawable.uxsdk_ic_refresh)
            rtkStationScanAdapter.notifyDataSetChanged()
        }
    }

    private fun updateConnectStatus(rtkBaseStationConnectState: RTKStationConnetState?) {
        if (rtkBaseStationConnectState == null) {
            return
        }

        LogUtils.i(TAG, "Current station status is $rtkBaseStationConnectState")
        when (rtkBaseStationConnectState) {
            RTKStationConnetState.IDLE,
            RTKStationConnetState.UNKNOWN,
            -> {
                if (firstEnter) {
                    LogUtils.i(TAG, "first enter，startScanning auto")
                    firstEnter = false
                    updateConnectStatus(RTKStationConnetState.SCANNING)
                    //这里延时2秒发送扫描命令，避免针对已连上RTK的设备（固件底层会自动重连），再次扫描固件自动重连逻辑会被破坏
                    scanHandler.postDelayed({
                        startScanning()
                    }, 2000)
                } else {
                    searchIv.setImageResource(R.drawable.uxsdk_ic_refresh)
                }
            }
            RTKStationConnetState.DISCONNECTED -> {
                stationHasNotFoundView.hide()
                stationScanningView.show()
                searchIv.setImageResource(R.drawable.uxsdk_ic_refresh)
                Toast.makeText(context, "Station has disconnected", Toast.LENGTH_SHORT).show()
            }
            RTKStationConnetState.SCANNING -> {
                stationHasNotFoundView.hide()
                stationScanningView.show()
                searchIv.setImageResource(R.drawable.uxsdk_rotate_progress_circle)
                LogUtils.i(TAG, "scan rtk ing...")

            }
            RTKStationConnetState.CONNECTED -> {
                LogUtils.i(TAG, "rtk has connected")
                stationHasNotFoundView.hide()
                stationScanningView.show()
                searchIv.setImageResource(R.drawable.uxsdk_ic_refresh)
                //将连接成功的基站放在列表首页
                if (stationList.remove(selectedRTKStationConnectInfo)) {
                    stationList.add(0, selectedRTKStationConnectInfo)
                }
                rtkStationScanAdapter.notifyDataSetChanged()

            }
            else -> {
                stationHasNotFoundView.hide()
                stationScanningView.show()
                searchIv.setImageResource(R.drawable.uxsdk_ic_refresh)
            }
        }
        connectState = rtkBaseStationConnectState
        //返回连接状态，更新UI
        selectedRTKStationConnectInfo.refresh(rtkBaseStationConnectState)
        //飞行过程中不允许点击扫描按钮
        updateRefreshUI(!isMotorOn)

    }


    private fun checkNeedUpdateUI(list: List<RTKStationInfo>?): Boolean {
        if (list?.size != stationList.size) {
            return true
        }
        if (!stationList.containsAll(list) || !list.containsAll(stationList)) {
            return true
        }
        return false
    }


    /**
     * 选中某个基站,注意这里selectedRTKStationConnectInfo不能初始化为null
     */
    private var selectedRTKStationConnectInfo: DJIRTKBaseStationConnectInfo = DJIRTKBaseStationConnectInfo()
    override fun onItemClick(view: View?, position: Int) {
        selectedRTKStationConnectInfo = stationList[position]
        LogUtils.i(TAG, "click and connecting rtk:$selectedRTKStationConnectInfo")
        //连接某一个基站时，将其他基站的连接状态重置为空闲，因为一次只能连接一个基站
        for (stationInfo in stationList) {
            stationInfo.connectStatus = RTKStationConnetState.IDLE
        }
        selectedRTKStationConnectInfo.refresh(RTKStationConnetState.CONNECTING)
        startConnectStation(selectedRTKStationConnectInfo)

    }

    private fun DJIRTKBaseStationConnectInfo.refresh(connectState: RTKStationConnetState?) {
        connectState?.let {
            LogUtils.i(TAG, "connectState=$connectState")
            this.connectStatus = it
        }
        rtkStationScanAdapter.notifyDataSetChanged()

    }


    private fun startConnectStation(selectedRTKStationConnectInfo: DJIRTKBaseStationConnectInfo) {
        selectedRTKStationConnectInfo.run {
            addDisposable(widgetModel.startConnectToRTKStation(baseStationId).observeOn(SchedulerProvider.ui()).subscribe({
                LogUtils.i(TAG, "$rtkStationName connect success")
            }, {
                //连接失败，恢复未连接状态
                selectedRTKStationConnectInfo.refresh(RTKStationConnetState.IDLE)
                Toast.makeText(context, StringUtils.getResStr(R.string.uxsdk_rtk_base_station_connect_fail), Toast.LENGTH_SHORT).show()
                LogUtils.e(TAG, "${selectedRTKStationConnectInfo.rtkStationName}connect fail！！！")
            }))
        }
    }

    private fun startScanning() {
        LogUtils.i(TAG, "startScanning now")
        //添加计时器
        scanTimeOut()
        //清除已有的基站列表
        stationList.clear()
        rtkStationScanAdapter.notifyDataSetChanged()
        //发送命令开始扫描
        addDisposable(widgetModel.startSearchStationRTK().observeOn(SchedulerProvider.ui()).subscribe({
            stationHasNotFoundView.hide()
            stationScanningView.show()
        }, {
            stationHasNotFoundView.show()
            stationScanningView.hide()
            Toast.makeText(context, StringUtils.getResStr(R.string.uxsdk_rtk_base_station_search_false_and_try_again), Toast.LENGTH_SHORT).show()
            LogUtils.e(TAG, "startSearchStationRTK fail:${it.localizedMessage}")

        }))
        //某些情况下不会返回状态，需要手动更新状态
        updateConnectStatus(RTKStationConnetState.SCANNING)
    }


    private fun scanTimeOut() {
        disposeTimeout(scanTimeOutDisposable)
        scanTimeOutDisposable = Observable.timer(SCAN_TIME_OUT.toLong(), TimeUnit.SECONDS)
            .observeOn(SchedulerProvider.ui()).subscribe({
                if (!isHasFoundRTK()) {
                    LogUtils.e(TAG, "scanTimeOut ,stop search station RTK")
                    //扫描到基站列表超时，则停止扫描；同时展示RTK找不到的布局，
                    stationHasNotFoundView.show()
                    stationScanningView.hide()
                    widgetModel.stopSearchStationRTK()
                    //停止扫码不会返回状态，需要手动更新状态
                    updateConnectStatus(RTKStationConnetState.IDLE)
                } else {
                    LogUtils.i(TAG, "scan finish，has found rtk")
                    //扫描到基站，则隐藏基站未找到的界面，显示基站列表页，同时更新对应刷新Image Icon为刷新图片
                    stationHasNotFoundView.hide()
                    stationScanningView.show()
                    //停止扫描
                    widgetModel.stopSearchStationRTK()
                    searchIv.setImageResource(R.drawable.uxsdk_ic_refresh)

                }
            }, {
                LogUtils.e(TAG, it.localizedMessage)
                disposeTimeout(scanTimeOutDisposable)
            })
        scanTimeOutDisposable?.let {
            if (!it.isDisposed) {
                addDisposable(it)
            }
        }
    }


    private fun isHasFoundRTK(): Boolean {
        return connectState == RTKStationConnetState.CONNECTED || connectState == RTKStationConnetState.CONNECTING || stationList.isNotEmpty()
    }

    /**
     * 关闭计时器
     */
    private fun disposeTimeout(timeOutDisposable: Disposable?) {
        timeOutDisposable?.let {
            if (!it.isDisposed) {
                it.dispose()
            }
        }
    }


    private fun handleReconnectedStationInfo(infoConnected: ConnectedRTKStationInfo?) {
        infoConnected?.run {
            //第一次连接过基站后，再次重启飞机或者重启App固件会帮忙自动连接基站。这里就是为了构建自动重连的基站信息
            if (selectedRTKStationConnectInfo.baseStationId == 0) {
                LogUtils.i(TAG, "RTK Station has reconnected and remove scanHandler message")
                selectedRTKStationConnectInfo = DJIRTKBaseStationConnectInfo(stationId, signalLevel, stationName, RTKStationConnetState.CONNECTED)
                scanHandler.removeCallbacksAndMessages(null)
                updateConnectStatus(RTKStationConnetState.CONNECTED)
                handleStationRTKList(arrayListOf(selectedRTKStationConnectInfo))
            }
        }
    }

}