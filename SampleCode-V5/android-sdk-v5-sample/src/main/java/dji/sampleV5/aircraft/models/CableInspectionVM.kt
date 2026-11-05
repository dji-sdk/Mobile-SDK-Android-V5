package dji.sampleV5.aircraft.models

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import dji.open.pilot.base.ui.IOpenViewOnClickListener
import dji.open.pilot.base.ui.NavigateAction
import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.FontStyleType
import dji.open.pilot.base.ui.model.GravityType
import dji.open.pilot.base.ui.model.LayoutParam
import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.base.ui.model.ViewPageType
import dji.open.pilot.base.ui.model.ViewSide
import dji.open.pilot.base.ui.model.ViewType
import dji.open.pilot.base.ui.IRemoteViewTextWatcher
import dji.open.pilot.client.manager.ui.RemoteDialogManager
import dji.open.pilot.client.manager.ui.RemoteViewPageManager
import dji.open.pilot.base.manager.ui.config.DialogConfig
import dji.sampleV5.aircraft.cable.CableInspectionManager
import dji.sampleV5.aircraft.cable.CameraInfo
import dji.sampleV5.aircraft.cable.ICableInspectionManager
import dji.sampleV5.aircraft.cable.InspectionDirection
import dji.sampleV5.aircraft.cable.InspectionState
import dji.sampleV5.aircraft.cable.SupportDetail
import dji.v5.ux.util.ToastUtils
import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.flightassistant.CableInspectionErrCode
import dji.sdk.keyvalue.value.flightcontroller.CableArInfo
import dji.sdk.keyvalue.value.flightcontroller.GoHomeState
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.et.action
import dji.v5.et.create
import dji.v5.et.get
import dji.v5.et.listen
import dji.v5.utils.common.LogUtils
import java.util.concurrent.Executors

class CableInspectionVM : DJIViewModel() {

    // ══════════════════════════════════════════════════════════════════
    // Manager 引用
    // ══════════════════════════════════════════════════════════════════

    private val manager: CableInspectionManager = CableInspectionManager.getInstance()

    // ══════════════════════════════════════════════════════════════════
    // 公开常量（供 Fragment UI 展示用）
    // ══════════════════════════════════════════════════════════════════

    val flightMinVer = CableInspectionManager.FLIGHT_MIN_VER
    val cameraMinVer = CableInspectionManager.CAMERA_MIN_VER

    val openPluginBoxPageType = ViewPageType.FLOATING_BOX

    // ══════════════════════════════════════════════════════════════════
    // 公开 LiveData（供 Fragment 观察）
    // 所有数据来源统一从 Manager 的 Listener 回调桥接，不再自行订阅任何 Key
    // ══════════════════════════════════════════════════════════════════

    /** 当前导线巡检 App 侧状态 */
    val inspectionState = MutableLiveData(InspectionState.IDLE)

    /** 飞控推送的电线 AR 数据列表 */
    val cableArInfoList = MutableLiveData<List<CableArInfo>>(emptyList())

    /**
     * 导线巡检功能支持详情（整合飞控版本、相机信息、地区码）。
     * 所有支持检查 Key 均在 Manager 内部收敛，VM 只桥接结果。
     */
    val supportDetailData = MutableLiveData(
        SupportDetail(false, null, emptyList(), null, null)
    )

    // ── 为兼容 Fragment 现有访问方式，提供便捷的只读属性 ──────────────
    val flyVer: MutableLiveData<String?> = MutableLiveData(null)
    val availableCameraInfoList = MutableLiveData<List<CameraInfo>>(emptyList())
    val areaCode = MutableLiveData<String?>(null)
    val isSupport = MutableLiveData<Boolean>(false)
    val selectedCameraIndex = MutableLiveData<ComponentIndexType?>(null)

    // ══════════════════════════════════════════════════════════════════
    // UI 颜色 / 字号常量
    // ══════════════════════════════════════════════════════════════════

    private val textBgColor      = Color.argb(0, 0, 0, 0)
    private val highlightColor   = 0xFF1E90FF.toInt()
    private val selectedBtnColor = 0xFF1E90FF.toInt()
    private val normalBtnColor   = Color.GRAY

    private val fontSizeTitle = "13f"
    private val fontSizeBody  = "11f"

    // ══════════════════════════════════════════════════════════════════
    // UI 内部状态（仅服务于浮窗构建，不属于业务逻辑）
    // ══════════════════════════════════════════════════════════════════

    /** 5s 超时检查 Runnable */
    private var cableTimeoutRunnable: Runnable? = null

    /**
     * UI 流程阶段，用于区分不同场景下 IDLE 状态的处理方式。
     * 替代原有的 isReturnHomeActive 布尔标志，消除状态残留导致的歧义。
     */
    private enum class UiFlowPhase {
        /** 正常巡检流程中，IDLE 意味着异常终止 */
        INSPECTING,
        /** 巡检完成，2s 延迟等待中，准备进入返航 */
        FINISHING,
        /** 返航指令已发送，正在返航 */
        RETURNING_HOME
    }

    private var uiFlowPhase = UiFlowPhase.INSPECTING

    /** FINISH 后 2s 延迟进入返航的 Runnable 引用，供 goBack/initCable 时取消 */
    private var finishDelayRunnable: Runnable? = null

    /** 方向选择页中用户是否已完成方向选择（纯 UI 状态，不发给飞机） */
    private var directionSelected = false

    /** Manager 推送的最新巡检状态（通过 InspectionStateListener 缓存） */
    @Volatile
    private var cachedState = InspectionState.IDLE

    /** Manager 推送的最新最近导线 ID（通过 CableArInfoListener 缓存） */
    private var cachedSelectedCableId = -1

    /** 方向按钮引用（动态切换选中状态） */
    private var leftDirBtn: RemoteView?  = null
    private var rightDirBtn: RemoteView? = null

    // View 引用（供动态更新使用）
    private lateinit var manualInputEditView: RemoteView
    private lateinit var distanceEditView: RemoteView
    private lateinit var pitchEditView: RemoteView
    private lateinit var executingStatusView: RemoteView
    private lateinit var zoomEditView: RemoteView
    private lateinit var speedEditView: RemoteView
    private lateinit var photoIntervalView: RemoteView
    private var cableStatusView: RemoteView?   = null
    private var selectedCableView: RemoteView? = null
    private var dirNextBtn: RemoteView?        = null
    private var returnHomeStatusView: RemoteView? = null

    /** 主线程 Handler */
    private val uiHandler = Handler(Looper.getMainLooper())

    /** 单线程池，将 IPC updateView 调用移出主线程 */
    private val ipcExecutor = Executors.newSingleThreadExecutor()

    /** 记录每个控件最后一次 pending 的 Runnable，实现 trailing-edge throttle */
    private val pendingUpdates = mutableMapOf<String, Runnable>()

    // ══════════════════════════════════════════════════════════════════
    // Manager Listener 实现（桥接到 LiveData / 浮窗）
    // ══════════════════════════════════════════════════════════════════

    private val listener = object : ICableInspectionManager.CableInspectionListener {
        override fun onStateChanged(
            oldState: InspectionState,
            newState: InspectionState,
            errCode: CableInspectionErrCode?
        ) {
            cachedState = newState
            inspectionState.postValue(newState)

            when (newState) {
                InspectionState.ADJUSTING_POSITION -> uiHandler.post {
                    showExecutingPage("正在飞向起始点...", "返回上一步") { goBackToDirectionPage() }
                }
                InspectionState.WAITING_FOR_CONFIG -> uiHandler.post { showZoomSpeedPage() }
                InspectionState.CAMERA_CALIBRATING -> uiHandler.post {
                    showExecutingPage("相机标定中...", "终止巡检") { showExitConfirmDialog() }
                }
                InspectionState.CAMERA_CONFIGURING -> uiHandler.post {
                    updateExecutingStatus("相机配置中...")
                }
                InspectionState.FOLLOWING -> uiHandler.post {
                    showExecutingPage("导线巡检中...", "终止巡检") { showExitConfirmDialog() }
                }
                InspectionState.FINISHED -> uiHandler.post {
                    uiFlowPhase = UiFlowPhase.FINISHING
                    updateExecutingStatus("巡检完成")
                    finishDelayRunnable = Runnable {
                        directionSelected   = false
                        manager.inspectionConfig.direction = InspectionDirection.NONE
                        inspectionState.value = InspectionState.IDLE
                        RemoteViewPageManager.getInstance().clearLayout(openPluginBoxPageType)
                        showReturnHomePage()
                    }
                    uiHandler.postDelayed(finishDelayRunnable!!, 2000)
                }
                InspectionState.IDLE -> {
                    if (oldState != InspectionState.IDLE && oldState != InspectionState.SELECTING_CABLE) {
                        when (uiFlowPhase) {
                            UiFlowPhase.FINISHING, UiFlowPhase.RETURNING_HOME -> {
                                LogUtils.d(TAG, "stateListener: IDLE skipped, uiFlowPhase=$uiFlowPhase")
                            }
                            UiFlowPhase.INSPECTING -> {
                                uiHandler.post {
                                    directionSelected   = false
                                    manager.inspectionConfig.direction = InspectionDirection.NONE
                                    try { manager.clearCableARAnnotations() } catch (e: Exception) {
                                        LogUtils.w(TAG, "stateListener: 异常终止 clearCableARAnnotations 异常（忽略）: $e")
                                    }
                                    inspectionState.value = InspectionState.IDLE
                                    restoreDefaultFloatingBox()
                                }
                            }
                        }
                    }
                }
                InspectionState.SELECTING_CABLE -> {
                    if (oldState != InspectionState.SELECTING_CABLE && oldState != InspectionState.IDLE) {
                        LogUtils.i(TAG, "stateListener: 飞控进入 DIR，同步切换浮窗 (from $oldState)")
                        uiHandler.post {
                            directionSelected   = false
                            manager.inspectionConfig.direction = InspectionDirection.NONE
                            showDirectionPage()
                        }
                    }
                }
            }
        }

        override fun onCableArInfoUpdated(cables: List<CableArInfo>, nearestCableId: Int) {
            cachedSelectedCableId = nearestCableId
            cableArInfoList.postValue(cables)
            if (cachedState == InspectionState.SELECTING_CABLE) {
                uiHandler.post { updateDirectionPageCableStatus(cables, nearestCableId) }
            }
        }

        override fun onSupportStatusChanged(detail: SupportDetail) {
            supportDetailData.postValue(detail)
            flyVer.postValue(detail.flyVer)
            availableCameraInfoList.postValue(detail.cameraInfoList)
            areaCode.postValue(detail.areaCode)
            isSupport.postValue(detail.isSupported)
            selectedCameraIndex.postValue(detail.selectedCameraIndex)
        }

        override fun onError(errCode: CableInspectionErrCode, message: String) {
            ToastUtils.showLongToast("异常结束导线巡检: $message")
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // 生命周期方法
    // ══════════════════════════════════════════════════════════════════

    fun initListener() {
        // 注册 Manager Listener
        manager.addCableInspectionListener(listener)

        // 初始化 Manager（注册所有 Key 订阅）
        manager.init()

        // AR config 上传移到 showDirectionPage() 中，确保只在进入方向选择浮窗时才启用 AR 投射
    }

    override fun onCleared() {
        uiFlowPhase = UiFlowPhase.INSPECTING
        finishDelayRunnable?.let { uiHandler.removeCallbacks(it) }
        finishDelayRunnable = null
        cableTimeoutRunnable?.let { uiHandler.removeCallbacks(it) }
        uiHandler.removeCallbacksAndMessages(null)
        ipcExecutor.shutdown()

        // 注销 Manager Listener
        manager.removeCableInspectionListener(listener)

        // 销毁 Manager（注销 Key 订阅 + CMD_EXIT 兜底）
        manager.destroy()
    }

    // ══════════════════════════════════════════════════════════════════
    // 公开 API（供 Fragment 调用）
    // ══════════════════════════════════════════════════════════════════

    /** 选择相机（委托给 Manager，触发支持状态重算） */
    fun selectCamera(index: ComponentIndexType) {
        manager.selectCamera(index)
    }

    /** 获取当前选中导线 ID（供 Fragment 展示） */
    fun getSelectedCableId(): Int = cachedSelectedCableId

    /** 发送 CMD_EXIT 通知飞控退出巡检状态（enterExitMissionState 在 STANDBY 时自动跳过，安全幂等） */
    fun exit() = manager.enterExitMissionState(null)

    /** 更新执行状态文字 */
    fun updateExecutingStatus(status: String) {
        if (::executingStatusView.isInitialized) {
            RemoteViewPageManager.getInstance().updateView(
                openPluginBoxPageType, executingStatusView.id,
                RemoteViewProperty.TEXT, status
            )
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // 浮窗页面构建（纯 UI，不包含任何 Key 操作）
    // ══════════════════════════════════════════════════════════════════

    /**
     * 初始化设置浮窗（入口页面）。
     */
    fun initCableInspection(navigate: Boolean = true) {
        // 重置 UI 流程阶段 + 取消残留的 FINISH 延迟 Runnable
        uiFlowPhase = UiFlowPhase.INSPECTING
        finishDelayRunnable?.let { uiHandler.removeCallbacks(it) }
        finishDelayRunnable = null

        // 初始化时清除上次残留的 AR 投射内容（异常退出/上次未正常清理等场景）
        try { manager.clearCableARAnnotations() } catch (_: Exception) {}

        RemoteViewPageManager.getInstance().clearLayout(openPluginBoxPageType)
        RemoteViewPageManager.getInstance().setPageLayoutParam(openPluginBoxPageType, LayoutParam(400))

        val mgn     = 10
        val gap     = 6
        val vGapSec = 4
        val vGapBlk = 10
        val labelH  = 20
        val rowH    = 28
        val btnW    = 46

        // ── 标题 ─────────────────────────────────────────────────────
        val titleView = RemoteView(ViewType.TEXT_VIEW, "导线巡检初始设置", 1, 32, textBgColor, "")
        titleView.width = 0
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, titleView)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, titleView.id,
            RemoteViewProperty.FONT_STYLE_TYPE, FontStyleType.BOLD.value.toString())
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, titleView.id,
            RemoteViewProperty.TEXT_SIZE, fontSizeTitle)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, titleView.id,
            RemoteViewProperty.GRAVITY, GravityType.CENTER.value.toString())

        // 分割线
        val divider = RemoteView(ViewType.TEXT_VIEW, "", 1, 1, Color.GRAY, "")
        divider.width = 0
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, divider)

        // ── 布局a：手动输入文本 ───────────────────────────────────────
        val labelA = RemoteView(ViewType.TEXT_VIEW, "文件夹名称(自定义):", 1, labelH, textBgColor, "")
        labelA.width = 0
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, labelA)
        manualInputEditView = RemoteView(ViewType.EDIT_TEXT, "", 1, rowH, textBgColor, "")
        manualInputEditView.width = 0
        manualInputEditView.background = highlightColor
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, manualInputEditView)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, manualInputEditView.id,
            RemoteViewProperty.TEXT, "")

        // ── 布局b：飞行距离 ───────────────────────────────────────────
        val labelB = RemoteView(ViewType.TEXT_VIEW, "飞行距离(m)    范围: 5 ~ 10", 1, labelH, textBgColor, "")
        labelB.width = 0
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, labelB)
        val distMinus1 = RemoteView(ViewType.BUTTON, "-1", btnW, rowH, normalBtnColor, "")
        distanceEditView = RemoteView(ViewType.TEXT_VIEW, "10.0", 1, rowH, textBgColor, "")
        distanceEditView.width = 0
        distanceEditView.background = highlightColor
        val distPlus1 = RemoteView(ViewType.BUTTON, "+1", btnW, rowH, normalBtnColor, "")
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, distMinus1)
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, distanceEditView)
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, distPlus1)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, distanceEditView.id,
            RemoteViewProperty.GRAVITY, GravityType.CENTER.value.toString())

        // ── 布局c：俯仰角 ─────────────────────────────────────────────
        val labelC = RemoteView(ViewType.TEXT_VIEW, "俯仰角(°)    范围: 0 ~ 35", 1, labelH, textBgColor, "")
        labelC.width = 0
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, labelC)
        val pitchMinus1 = RemoteView(ViewType.BUTTON, "-1", btnW, rowH, normalBtnColor, "")
        pitchEditView = RemoteView(ViewType.TEXT_VIEW, "20", 1, rowH, textBgColor, "")
        pitchEditView.width = 0
        pitchEditView.background = highlightColor
        val pitchPlus1 = RemoteView(ViewType.BUTTON, "+1", btnW, rowH, normalBtnColor, "")
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, pitchMinus1)
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, pitchEditView)
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, pitchPlus1)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, pitchEditView.id,
            RemoteViewProperty.GRAVITY, GravityType.CENTER.value.toString())

        // ── 布局d：是否允许手动控制俯仰角 ────────────────────────────
        val labelD = RemoteView(ViewType.TEXT_VIEW, "是否允许手动控制俯仰角:", 1, labelH, textBgColor, "")
        labelD.width = 0
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, labelD)
        val manualPitchSwitch = RemoteView(ViewType.SWITCH, "是否允许", 1, rowH, normalBtnColor, "")
        manualPitchSwitch.width = 0
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, manualPitchSwitch)

        // ── 确认按钮（满宽） ──────────────────────────────────────────
        val confirmBtn = RemoteView(ViewType.BUTTON, "确认设置", 1, 34, selectedBtnColor, "")
        confirmBtn.width = 0
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, confirmBtn)

        // ════════════════════════════════════════════════════════════
        // 约束布局
        // ════════════════════════════════════════════════════════════
        setC(titleView, RemoteView.PARENT_ID, ViewSide.TOP,   ViewSide.TOP,   mgn)
        setC(titleView, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 0)
        setC(titleView, RemoteView.PARENT_ID, ViewSide.END,   ViewSide.END,   0)

        setC(divider, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 0)
        setC(divider, RemoteView.PARENT_ID, ViewSide.END,   ViewSide.END,   0)
        setC(divider, titleView.id,         ViewSide.TOP,   ViewSide.BOTTOM, 4)

        setC(labelA, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, mgn)
        setC(labelA, RemoteView.PARENT_ID, ViewSide.END,   ViewSide.END,   mgn)
        setC(labelA, divider.id,           ViewSide.TOP,   ViewSide.BOTTOM, vGapBlk)
        setC(manualInputEditView, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, mgn)
        setC(manualInputEditView, RemoteView.PARENT_ID, ViewSide.END,   ViewSide.END,   mgn)
        setC(manualInputEditView, labelA.id,            ViewSide.TOP,   ViewSide.BOTTOM, vGapSec)

        setC(labelB, RemoteView.PARENT_ID,   ViewSide.START, ViewSide.START, mgn)
        setC(labelB, RemoteView.PARENT_ID,   ViewSide.END,   ViewSide.END,   mgn)
        setC(labelB, manualInputEditView.id, ViewSide.TOP,   ViewSide.BOTTOM, vGapBlk)
        setC(distMinus1, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, mgn)
        setC(distMinus1, labelB.id,            ViewSide.TOP,   ViewSide.BOTTOM, vGapSec)
        setC(distanceEditView, distMinus1.id,  ViewSide.START, ViewSide.END,    gap)
        setC(distanceEditView, distPlus1.id,   ViewSide.END,   ViewSide.START,  gap)
        setC(distanceEditView, labelB.id,      ViewSide.TOP,   ViewSide.BOTTOM, vGapSec)
        setC(distPlus1, RemoteView.PARENT_ID,  ViewSide.END,   ViewSide.END,   mgn)
        setC(distPlus1, labelB.id,             ViewSide.TOP,   ViewSide.BOTTOM, vGapSec)

        setC(labelC, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, mgn)
        setC(labelC, RemoteView.PARENT_ID, ViewSide.END,   ViewSide.END,   mgn)
        setC(labelC, distMinus1.id,        ViewSide.TOP,   ViewSide.BOTTOM, vGapBlk)
        setC(pitchMinus1, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, mgn)
        setC(pitchMinus1, labelC.id,            ViewSide.TOP,   ViewSide.BOTTOM, vGapSec)
        setC(pitchEditView, pitchMinus1.id,     ViewSide.START, ViewSide.END,   gap)
        setC(pitchEditView, pitchPlus1.id,      ViewSide.END,   ViewSide.START, gap)
        setC(pitchEditView, labelC.id,          ViewSide.TOP,   ViewSide.BOTTOM, vGapSec)
        setC(pitchPlus1, RemoteView.PARENT_ID,  ViewSide.END,   ViewSide.END,   mgn)
        setC(pitchPlus1, labelC.id,             ViewSide.TOP,   ViewSide.BOTTOM, vGapSec)

        setC(labelD, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, mgn)
        setC(labelD, RemoteView.PARENT_ID, ViewSide.END,   ViewSide.END,   mgn)
        setC(labelD, pitchMinus1.id,       ViewSide.TOP,   ViewSide.BOTTOM, vGapBlk)
        setC(manualPitchSwitch, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, mgn)
        setC(manualPitchSwitch, RemoteView.PARENT_ID, ViewSide.END,   ViewSide.END,   mgn)
        setC(manualPitchSwitch, labelD.id,            ViewSide.TOP,   ViewSide.BOTTOM, vGapSec)

        setC(confirmBtn, RemoteView.PARENT_ID,  ViewSide.START, ViewSide.START, mgn)
        setC(confirmBtn, RemoteView.PARENT_ID,  ViewSide.END,   ViewSide.END,   mgn)
        setC(confirmBtn, manualPitchSwitch.id,  ViewSide.TOP,   ViewSide.BOTTOM, vGapBlk)

        // ════════════════════════════════════════════════════════════
        // 事件绑定
        // ════════════════════════════════════════════════════════════

        manualInputEditView.setOnTextChangeListener(object : IRemoteViewTextWatcher {
            override fun beforeTextChanged(s: String, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: String, start: Int, before: Int, count: Int) {
                manager.inspectionConfig.manualInputText = s
            }
            override fun afterTextChanged(s: String) {}
        })

        distMinus1.setOnClickListener { updateDistance(-1.0) }
        distPlus1.setOnClickListener  { updateDistance(1.0)  }

        pitchMinus1.setOnClickListener { updatePitch(-1.0) }
        pitchPlus1.setOnClickListener  { updatePitch(1.0)  }

        manualPitchSwitch.setOnCheckChangeListener { _, checked ->
            manager.inspectionConfig.allowManualPitch = checked
        }

        confirmBtn.setOnClickListener { showDirectionPage() }

        if (navigate) {
            jumpTo(NavigateAction(NavigateAction.Value.PILOT_HOME))
        }
    }

    /**
     * 方向选择浮窗（第2步）。
     * 包含：导线状态、AR 标注设置（文字 + 高亮颜色）、方向按钮、底部操作按钮。
     */
    private fun showDirectionPage() {
        RemoteViewPageManager.getInstance().clearLayout(openPluginBoxPageType)
        // 高度从 270 扩展到 420 以容纳 AR 设置区域
        RemoteViewPageManager.getInstance().setPageLayoutParam(openPluginBoxPageType, LayoutParam(420))

        val mgn     = 10
        val gap     = 6
        val colorGap = 4
        val vGapBlk = 12
        val vGapSec = 4
        val rowH    = 30
        val btnH    = 34
        val labelH  = 20
        val colorBtnW = 36  // 颜色按钮固定宽度（6 个平铺）

        // 重置 UI 侧方向状态
        directionSelected   = false
        manager.inspectionConfig.direction = InspectionDirection.NONE

        // 进入方向选择页时重置 UI 流程阶段，确保无残留状态
        uiFlowPhase = UiFlowPhase.INSPECTING
        finishDelayRunnable?.let { uiHandler.removeCallbacks(it) }
        finishDelayRunnable = null

        // 上传 AR 标注所需 config（如果已 ready 则跳过重复上传）
        manager.uploadCableARConfigs()

        // ── 标题 ─────────────────────────────────────────────────────
        val titleView = RemoteView(ViewType.TEXT_VIEW, "选择巡检方向", 1, 32, textBgColor, "")
        titleView.width = 0
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, titleView)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, titleView.id,
            RemoteViewProperty.FONT_STYLE_TYPE, FontStyleType.BOLD.value.toString())
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, titleView.id,
            RemoteViewProperty.TEXT_SIZE, fontSizeTitle)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, titleView.id,
            RemoteViewProperty.GRAVITY, GravityType.CENTER.value.toString())

        val divider = RemoteView(ViewType.TEXT_VIEW, "", 1, 1, Color.GRAY, "")
        divider.width = 0
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, divider)

        // ── 导线状态行 ────────────────────────────────────────────────
        cableStatusView = RemoteView(ViewType.TEXT_VIEW, "正在识别导线...", 1, rowH, textBgColor, "")
        cableStatusView!!.width = 0
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, cableStatusView!!)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, cableStatusView!!.id,
            RemoteViewProperty.TEXT_SIZE, fontSizeBody)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, cableStatusView!!.id,
            RemoteViewProperty.GRAVITY, GravityType.CENTER.value.toString())

        // ── 已选中导线行 ──────────────────────────────────────────────
        selectedCableView = RemoteView(ViewType.TEXT_VIEW, "已选中导线: --", 1, rowH, textBgColor, "")
        selectedCableView!!.width = 0
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, selectedCableView!!)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, selectedCableView!!.id,
            RemoteViewProperty.TEXT_SIZE, fontSizeBody)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, selectedCableView!!.id,
            RemoteViewProperty.GRAVITY, GravityType.CENTER.value.toString())

        // ════════════════════════════════════════════════════════════
        // AR 标注设置区域（方向按钮上方）
        // ════════════════════════════════════════════════════════════

        // ── AR 文字 label ────────────────────────────────────────────
        val arTextLabel = RemoteView(ViewType.TEXT_VIEW, "AR标注文字:", 1, labelH, textBgColor, "")
        arTextLabel.width = 0
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, arTextLabel)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, arTextLabel.id,
            RemoteViewProperty.TEXT_SIZE, fontSizeBody)

        // ── AR 文字输入框（初始值取 manager 运行时属性）────────────────
        val arTextEdit = RemoteView(ViewType.EDIT_TEXT, "", 1, rowH, textBgColor, "")
        arTextEdit.width = 0
        arTextEdit.background = highlightColor
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, arTextEdit)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, arTextEdit.id,
            RemoteViewProperty.TEXT, manager.arAnnotationConfig.highlightText)

        // ── 高亮颜色 label ────────────────────────────────────────────
        val colorLabel = RemoteView(ViewType.TEXT_VIEW, "高亮颜色:", 1, labelH, textBgColor, "")
        colorLabel.width = 0
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, colorLabel)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, colorLabel.id,
            RemoteViewProperty.TEXT_SIZE, fontSizeBody)

        // ── 4 个预定义颜色按钮（红/黄/蓝/绿，默认蓝色）────────────────
        // 定义：显示文字 → ARGB 颜色值
        val arColorOptions = listOf(
            "红" to 0xFFE23C39.toInt(),
            "黄" to 0xFFFFBB00.toInt(),
            "蓝" to 0xFF1E90FF.toInt(),
            "绿" to 0xFF19BE6B.toInt(),
        )
        // 找到当前选中颜色对应的 index（初始从 manager.arAnnotationConfig.textColor 匹配，默认蓝色）
        var selectedColorIndex = arColorOptions.indexOfFirst { it.second == manager.arAnnotationConfig.textColor }
            .takeIf { it >= 0 } ?: 2   // 找不到时默认 index=2（蓝色）

        val colorBtnViews = arColorOptions.mapIndexed { idx, (label, color) ->
            // 选中：用实际颜色作为背景；未选中：灰色背景
            val bgColor = if (idx == selectedColorIndex) color else normalBtnColor
            val btn = RemoteView(ViewType.BUTTON, label, colorBtnW, btnH, bgColor, "")
            RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, btn)
            btn
        }

        // ── 方向按钮行 ───────────────────────────────────────────────
        leftDirBtn  = RemoteView(ViewType.BUTTON, "← 向左", 1, btnH, normalBtnColor, "")
        leftDirBtn!!.width = 0
        rightDirBtn = RemoteView(ViewType.BUTTON, "向右 →", 1, btnH, normalBtnColor, "")
        rightDirBtn!!.width = 0
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, leftDirBtn!!)
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, rightDirBtn!!)

        // ── 底部按钮行 ───────────────────────────────────────────────
        val backBtn = RemoteView(ViewType.BUTTON, "返回", 1, btnH, selectedBtnColor, "")
        backBtn.width = 0
        dirNextBtn = RemoteView(ViewType.BUTTON, "下一步", 1, btnH, selectedBtnColor, "")
        dirNextBtn!!.width = 0
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, backBtn)
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, dirNextBtn!!)

        // ════════════════════════════════════════════════════════════
        // 约束布局
        // ════════════════════════════════════════════════════════════
        setC(titleView, RemoteView.PARENT_ID, ViewSide.TOP,   ViewSide.TOP,   mgn)
        setC(titleView, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 0)
        setC(titleView, RemoteView.PARENT_ID, ViewSide.END,   ViewSide.END,   0)
        setC(divider,   RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 0)
        setC(divider,   RemoteView.PARENT_ID, ViewSide.END,   ViewSide.END,   0)
        setC(divider,   titleView.id,         ViewSide.TOP,   ViewSide.BOTTOM, 4)

        setC(cableStatusView!!, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, mgn)
        setC(cableStatusView!!, RemoteView.PARENT_ID, ViewSide.END,   ViewSide.END,   mgn)
        setC(cableStatusView!!, divider.id,           ViewSide.TOP,   ViewSide.BOTTOM, vGapBlk)

        setC(selectedCableView!!, RemoteView.PARENT_ID,    ViewSide.START, ViewSide.START, mgn)
        setC(selectedCableView!!, RemoteView.PARENT_ID,    ViewSide.END,   ViewSide.END,   mgn)
        setC(selectedCableView!!, cableStatusView!!.id,    ViewSide.TOP,   ViewSide.BOTTOM, vGapSec)

        // AR 标注区域：锚定在 selectedCableView 下方
        setC(arTextLabel, RemoteView.PARENT_ID,   ViewSide.START, ViewSide.START, mgn)
        setC(arTextLabel, RemoteView.PARENT_ID,   ViewSide.END,   ViewSide.END,   mgn)
        setC(arTextLabel, selectedCableView!!.id, ViewSide.TOP,   ViewSide.BOTTOM, vGapBlk)

        setC(arTextEdit, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, mgn)
        setC(arTextEdit, RemoteView.PARENT_ID, ViewSide.END,   ViewSide.END,   mgn)
        setC(arTextEdit, arTextLabel.id,       ViewSide.TOP,   ViewSide.BOTTOM, vGapSec)

        setC(colorLabel, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, mgn)
        setC(colorLabel, RemoteView.PARENT_ID, ViewSide.END,   ViewSide.END,   mgn)
        setC(colorLabel, arTextEdit.id,        ViewSide.TOP,   ViewSide.BOTTOM, vGapBlk)

        // 6 个颜色按钮水平排列
        for (i in colorBtnViews.indices) {
            val btn = colorBtnViews[i]
            setC(btn, colorLabel.id, ViewSide.TOP, ViewSide.BOTTOM, vGapSec)
            if (i == 0) {
                setC(btn, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, mgn)
            } else {
                setC(btn, colorBtnViews[i - 1].id, ViewSide.START, ViewSide.END, colorGap)
            }
        }

        // 方向按钮：锚定在颜色按钮下方
        val lastColorBtn = colorBtnViews.last()
        setC(leftDirBtn!!,  RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, mgn)
        setC(leftDirBtn!!,  rightDirBtn!!.id,      ViewSide.END,   ViewSide.START, gap)
        setC(leftDirBtn!!,  lastColorBtn.id,        ViewSide.TOP,   ViewSide.BOTTOM, vGapBlk)
        setC(rightDirBtn!!, leftDirBtn!!.id,        ViewSide.START, ViewSide.END,   gap)
        setC(rightDirBtn!!, RemoteView.PARENT_ID,   ViewSide.END,   ViewSide.END,   mgn)
        setC(rightDirBtn!!, lastColorBtn.id,         ViewSide.TOP,   ViewSide.BOTTOM, vGapBlk)

        setC(backBtn,      RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, mgn)
        setC(backBtn,      dirNextBtn!!.id,      ViewSide.END,   ViewSide.START, gap)
        setC(backBtn,      leftDirBtn!!.id,      ViewSide.TOP,   ViewSide.BOTTOM, vGapBlk)
        setC(dirNextBtn!!, backBtn.id,           ViewSide.START, ViewSide.END,   gap)
        setC(dirNextBtn!!, RemoteView.PARENT_ID, ViewSide.END,   ViewSide.END,   mgn)
        setC(dirNextBtn!!, leftDirBtn!!.id,      ViewSide.TOP,   ViewSide.BOTTOM, vGapBlk)

        // ════════════════════════════════════════════════════════════
        // 事件绑定
        // ════════════════════════════════════════════════════════════

        // AR 文字输入：实时同步到 manager 并刷新已显示的 AR 标注
        arTextEdit.setOnTextChangeListener(object : IRemoteViewTextWatcher {
            override fun beforeTextChanged(s: String, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: String, start: Int, before: Int, count: Int) {
                manager.arAnnotationConfig.highlightText = s
                manager.refreshCableARAnnotations()
            }
            override fun afterTextChanged(s: String) {}
        })

        // 颜色按钮：选中后更新 manager 颜色、刷新 AR 标注、同步按钮选中状态
        for ((idx, pair) in arColorOptions.withIndex()) {
            val (_, color) = pair
            val btn = colorBtnViews[idx]
            btn.setOnClickListener {
                // 更新 manager 运行时颜色并实时刷新 AR 标注
                manager.arAnnotationConfig.textColor = color
                manager.refreshCableARAnnotations()

                // 更新按钮背景：选中用实际颜色，其余恢复灰色
                selectedColorIndex = idx
                for ((i2, b2) in colorBtnViews.withIndex()) {
                    val bgColor2 = if (i2 == selectedColorIndex) arColorOptions[i2].second else normalBtnColor
                    postDebounced("colorBtn$i2") {
                        RemoteViewPageManager.getInstance().updateView(
                            openPluginBoxPageType, b2.id,
                            RemoteViewProperty.BACKGROUND_COLOR,
                            bgColor2.toString())
                    }
                }
            }
        }

        leftDirBtn!!.setOnClickListener {
            val cables = cableArInfoList.value ?: emptyList()
            if (cables.isEmpty() || cachedSelectedCableId < 0) {
                ToastUtils.showToast("未检测到导线，请等待导线识别完成")
                return@setOnClickListener
            }
            manager.inspectionConfig.direction = InspectionDirection.LEFT
            directionSelected   = true
            RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, leftDirBtn!!.id,
                RemoteViewProperty.BACKGROUND_COLOR, selectedBtnColor.toString())
            RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, rightDirBtn!!.id,
                RemoteViewProperty.BACKGROUND_COLOR, normalBtnColor.toString())
            manager.showDirectionArrow(InspectionDirection.LEFT)
        }

        rightDirBtn!!.setOnClickListener {
            val cables = cableArInfoList.value ?: emptyList()
            if (cables.isEmpty() || cachedSelectedCableId < 0) {
                ToastUtils.showToast("未检测到导线，请等待导线识别完成")
                return@setOnClickListener
            }
            manager.inspectionConfig.direction = InspectionDirection.RIGHT
            directionSelected   = true
            RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, rightDirBtn!!.id,
                RemoteViewProperty.BACKGROUND_COLOR, selectedBtnColor.toString())
            RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, leftDirBtn!!.id,
                RemoteViewProperty.BACKGROUND_COLOR, normalBtnColor.toString())
            manager.showDirectionArrow(InspectionDirection.RIGHT)
        }

        backBtn.setOnClickListener {
            LogUtils.i(TAG, "showDirectionPage: 返回初始设置页")
            cableTimeoutRunnable?.let { uiHandler.removeCallbacks(it) }
            directionSelected   = false
            manager.inspectionConfig.direction = InspectionDirection.NONE
            // 离开方向选择浮窗：清除 AR 投射
            try { manager.clearCableARAnnotations() } catch (_: Exception) {}
            uiHandler.post {
                inspectionState.value = InspectionState.IDLE
                cableArInfoList.value = emptyList()
                initCableInspection(navigate = false)
            }
        }

        dirNextBtn!!.setOnClickListener {
            if (!directionSelected) {
                ToastUtils.showToast("请先选择巡检方向（向左/向右）")
                return@setOnClickListener
            }
            val cables = cableArInfoList.value ?: emptyList()
            if (cables.isEmpty() || cachedSelectedCableId < 0) {
                ToastUtils.showToast("未检测到导线，请调整飞行位置")
                return@setOnClickListener
            }
            cableTimeoutRunnable?.let { uiHandler.removeCallbacks(it) }
            // AR 投射保留（后续巡检流程中持续显示）
            showExecutingPage("正在飞向起始点...", "返回上一步") { goBackToDirectionPage() }
            manager.enterAdjustPosState(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {}
                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("发送导线巡检命令失败: ${error.description()}")
                }
            })
        }

        // 5s 超时提示
        cableTimeoutRunnable = Runnable {
            if (inspectionState.value == InspectionState.SELECTING_CABLE &&
                (cableArInfoList.value?.isEmpty() != false)) {
                ToastUtils.showToast("未检测到导线，请检查飞行位置或相机角度")
                LogUtils.w(TAG, "showDirectionPage: 5s 超时，未检测到导线")
            }
        }
        uiHandler.postDelayed(cableTimeoutRunnable!!, 5000)

        // 刷新已缓存的导线数据
        val currentCables = cableArInfoList.value ?: emptyList()
        if (currentCables.isNotEmpty()) {
            updateDirectionPageCableStatus(currentCables, cachedSelectedCableId)
        }

        // ★ 确保飞控在 STANDBY 后再发 SELECT_CABLE：
        // - 首次进入：fcCableStatus=STANDBY → enterExitMissionState 同步调用回调 → 零延迟
        // - 从 POS/WAITING 返回：goBackToDirectionPage 已发 CMD_EXIT，
        //   此处再发一次（幂等），等回调确认飞控已在 STANDBY 后再发 SELECT_CABLE
        inspectionState.value = InspectionState.SELECTING_CABLE
        // 提前设置 cachedState，防止飞控先推 KeyCableArInfo 再推 SELECT_CABLE 状态时
        // onCableArInfoUpdated 中 cachedState 仍为 IDLE 导致首批导线数据被丢弃
        cachedState = InspectionState.SELECTING_CABLE
        manager.enterExitMissionState(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() { manager.enterSelectCableState(null) }
            override fun onFailure(error: IDJIError) { manager.enterSelectCableState(null) }
        })
    }

    /**
     * 执行中状态浮窗。
     */
    private fun showExecutingPage(statusText: String, btnText: String, onBtnClick: () -> Unit) {
        RemoteViewPageManager.getInstance().clearLayout(openPluginBoxPageType)
        RemoteViewPageManager.getInstance().setPageLayoutParam(openPluginBoxPageType, LayoutParam(160))

        val mgn  = 10
        val rowH = 34

        executingStatusView = RemoteView(ViewType.TEXT_VIEW, statusText, 1, 50, textBgColor, "")
        executingStatusView.width = 0
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, executingStatusView)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, executingStatusView.id,
            RemoteViewProperty.TEXT_SIZE, fontSizeBody)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, executingStatusView.id,
            RemoteViewProperty.FONT_STYLE_TYPE, FontStyleType.BOLD.value.toString())
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, executingStatusView.id,
            RemoteViewProperty.GRAVITY, GravityType.CENTER.value.toString())

        val actionBtn = RemoteView(ViewType.BUTTON, btnText, 1, rowH, selectedBtnColor, "")
        actionBtn.width = 0
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, actionBtn)

        setC(executingStatusView, RemoteView.PARENT_ID, ViewSide.TOP,   ViewSide.TOP,   0)
        setC(executingStatusView, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 0)
        setC(executingStatusView, RemoteView.PARENT_ID, ViewSide.END,   ViewSide.END,   0)
        setC(actionBtn, RemoteView.PARENT_ID,    ViewSide.START, ViewSide.START, mgn)
        setC(actionBtn, RemoteView.PARENT_ID,    ViewSide.END,   ViewSide.END,   mgn)
        setC(actionBtn, executingStatusView.id,  ViewSide.TOP,   ViewSide.BOTTOM, mgn)

        actionBtn.setOnClickListener { onBtnClick() }
    }

    /**
     * 变焦与速度设置浮窗。
     */
    private fun showZoomSpeedPage() {
        RemoteViewPageManager.getInstance().clearLayout(openPluginBoxPageType)
        RemoteViewPageManager.getInstance().setPageLayoutParam(openPluginBoxPageType, LayoutParam(340))

        val mgn     = 10
        val gap     = 6
        val vGapSec = 4
        val vGapBlk = 10
        val labelH  = 20
        val rowH    = 28
        val btnW    = 46

        val titleView = RemoteView(ViewType.TEXT_VIEW, "变焦与速度设置", 1, 32, textBgColor, "")
        titleView.width = 0
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, titleView)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, titleView.id,
            RemoteViewProperty.FONT_STYLE_TYPE, FontStyleType.BOLD.value.toString())
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, titleView.id,
            RemoteViewProperty.TEXT_SIZE, fontSizeTitle)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, titleView.id,
            RemoteViewProperty.GRAVITY, GravityType.CENTER.value.toString())

        val divider = RemoteView(ViewType.TEXT_VIEW, "", 1, 1, Color.GRAY, "")
        divider.width = 0
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, divider)

        // ── 变焦倍率 ──────────────────────────────────────────────────
        val zoomRange = CameraKey.KeyCameraZoomRatiosRange.create().get()?.gears
        val labelZoom = RemoteView(ViewType.TEXT_VIEW,
            "变焦倍率   (${zoomRange?.get(0)}~${zoomRange?.last()})", 1, labelH, textBgColor, "")
        labelZoom.width = 0
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, labelZoom)

        val zoomMinus1  = RemoteView(ViewType.BUTTON, "-1", btnW, rowH, normalBtnColor, "")
        val zoomInitVal = manager.calculateDefaultZoom(manager.inspectionConfig.flightDistance)
        manager.inspectionConfig.zoomLevel = zoomInitVal
        zoomEditView = RemoteView(ViewType.TEXT_VIEW, String.format("%.0f", zoomInitVal), 1, rowH, textBgColor, "")
        zoomEditView.width = 0
        zoomEditView.background = highlightColor
        val zoomPlus1 = RemoteView(ViewType.BUTTON, "+1", btnW, rowH, normalBtnColor, "")
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, zoomMinus1)
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, zoomEditView)
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, zoomPlus1)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, zoomEditView.id,
            RemoteViewProperty.GRAVITY, GravityType.CENTER.value.toString())

        // ── 巡检速度 ──────────────────────────────────────────────────
        val labelSpeed  = RemoteView(ViewType.TEXT_VIEW, "巡检速度(m/s)   (1 ~ 10)", 1, labelH, textBgColor, "")
        labelSpeed.width = 0
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, labelSpeed)

        val speedMinus1 = RemoteView(ViewType.BUTTON, "-1", btnW, rowH, normalBtnColor, "")
        speedEditView   = RemoteView(ViewType.TEXT_VIEW, "5", 1, rowH, textBgColor, "")
        speedEditView.width = 0
        speedEditView.background = highlightColor
        val speedPlus1  = RemoteView(ViewType.BUTTON, "+1", btnW, rowH, normalBtnColor, "")
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, speedMinus1)
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, speedEditView)
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, speedPlus1)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, speedEditView.id,
            RemoteViewProperty.GRAVITY, GravityType.CENTER.value.toString())

        // ── 拍照间隔（只读） ─────────────────────────────────────────
        val labelPhoto = RemoteView(ViewType.TEXT_VIEW, "拍照间隔(s):", 80, rowH, textBgColor, "")
        val cfg = manager.inspectionConfig
        val photoResult = manager.calculatePhotoInterval(cfg.flightDistance, cfg.zoomLevel, cfg.inspectionSpeed)
        if (photoResult.speedAdjusted) {
            cfg.inspectionSpeed = photoResult.adjustedSpeed
            RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, speedEditView.id,
                RemoteViewProperty.TEXT, String.format("%.0f", photoResult.adjustedSpeed))
        }
        photoIntervalView = RemoteView(ViewType.TEXT_VIEW,
            "%.1fs".format(photoResult.intervalMs / 1000.0), 1, rowH, textBgColor, "")
        photoIntervalView.width = 0
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, labelPhoto)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, labelPhoto.id,
            RemoteViewProperty.GRAVITY, GravityType.CENTER_VERTICAL.value.toString())
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, photoIntervalView)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, photoIntervalView.id,
            RemoteViewProperty.GRAVITY, GravityType.CENTER_VERTICAL.value.toString())

        // ── 航向重叠率（只读） ────────────────────────────────────────
        val labelOverlap   = RemoteView(ViewType.TEXT_VIEW, "航向重叠率(%):", 90, rowH, textBgColor, "")
        val overlapValueView = RemoteView(ViewType.TEXT_VIEW, "15%", 1, rowH, textBgColor, "")
        overlapValueView.width = 0
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, labelOverlap)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, labelOverlap.id,
            RemoteViewProperty.GRAVITY, GravityType.CENTER_VERTICAL.value.toString())
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, overlapValueView)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, overlapValueView.id,
            RemoteViewProperty.GRAVITY, GravityType.CENTER_VERTICAL.value.toString())

        // ── 按钮行 ───────────────────────────────────────────────────
        val nextBtn = RemoteView(ViewType.BUTTON, "下一步", 1, 34, selectedBtnColor, "")
        nextBtn.width = 0
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, nextBtn)

        val backBtn = RemoteView(ViewType.BUTTON, "返回上一步", 1, 34, normalBtnColor, "")
        backBtn.width = 0
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, backBtn)

        // ════════════════════════════════════════════════════════════
        // 约束布局
        // ════════════════════════════════════════════════════════════
        setC(titleView, RemoteView.PARENT_ID, ViewSide.TOP,   ViewSide.TOP,   mgn)
        setC(titleView, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 0)
        setC(titleView, RemoteView.PARENT_ID, ViewSide.END,   ViewSide.END,   0)
        setC(divider, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 0)
        setC(divider, RemoteView.PARENT_ID, ViewSide.END,   ViewSide.END,   0)
        setC(divider, titleView.id,         ViewSide.TOP,   ViewSide.BOTTOM, 4)

        setC(labelZoom, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, mgn)
        setC(labelZoom, RemoteView.PARENT_ID, ViewSide.END,   ViewSide.END,   mgn)
        setC(labelZoom, divider.id,           ViewSide.TOP,   ViewSide.BOTTOM, vGapBlk)
        setC(zoomMinus1, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, mgn)
        setC(zoomMinus1, labelZoom.id,         ViewSide.TOP,   ViewSide.BOTTOM, vGapSec)
        setC(zoomEditView, zoomMinus1.id,      ViewSide.START, ViewSide.END,    gap)
        setC(zoomEditView, zoomPlus1.id,       ViewSide.END,   ViewSide.START,  gap)
        setC(zoomEditView, labelZoom.id,       ViewSide.TOP,   ViewSide.BOTTOM, vGapSec)
        setC(zoomPlus1, RemoteView.PARENT_ID,  ViewSide.END,   ViewSide.END,   mgn)
        setC(zoomPlus1, labelZoom.id,          ViewSide.TOP,   ViewSide.BOTTOM, vGapSec)

        setC(labelSpeed, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, mgn)
        setC(labelSpeed, RemoteView.PARENT_ID, ViewSide.END,   ViewSide.END,   mgn)
        setC(labelSpeed, zoomMinus1.id,        ViewSide.TOP,   ViewSide.BOTTOM, vGapBlk)
        setC(speedMinus1, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, mgn)
        setC(speedMinus1, labelSpeed.id,        ViewSide.TOP,   ViewSide.BOTTOM, vGapSec)
        setC(speedEditView, speedMinus1.id,     ViewSide.START, ViewSide.END,   gap)
        setC(speedEditView, speedPlus1.id,      ViewSide.END,   ViewSide.START, gap)
        setC(speedEditView, labelSpeed.id,      ViewSide.TOP,   ViewSide.BOTTOM, vGapSec)
        setC(speedPlus1, RemoteView.PARENT_ID,  ViewSide.END,   ViewSide.END,   mgn)
        setC(speedPlus1, labelSpeed.id,         ViewSide.TOP,   ViewSide.BOTTOM, vGapSec)

        setC(labelPhoto,      RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, mgn)
        setC(labelPhoto,      speedMinus1.id,       ViewSide.TOP,   ViewSide.BOTTOM, vGapBlk)
        setC(labelPhoto,      photoIntervalView,     ViewSide.TOP,   ViewSide.TOP,    0)
        setC(labelPhoto,      photoIntervalView,     ViewSide.BOTTOM,ViewSide.BOTTOM, 0)
        setC(photoIntervalView, labelPhoto.id,       ViewSide.START, ViewSide.END,    gap)
        setC(photoIntervalView, RemoteView.PARENT_ID,ViewSide.END,   ViewSide.END,    mgn)
        setC(photoIntervalView, speedMinus1.id,      ViewSide.TOP,   ViewSide.BOTTOM, vGapBlk)

        setC(labelOverlap,     RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, mgn)
        setC(labelOverlap,     photoIntervalView,    ViewSide.TOP,   ViewSide.BOTTOM, vGapBlk)
        setC(labelOverlap,     overlapValueView,     ViewSide.TOP,   ViewSide.TOP,    0)
        setC(labelOverlap,     overlapValueView,     ViewSide.BOTTOM,ViewSide.BOTTOM, 0)
        setC(overlapValueView, labelOverlap.id,      ViewSide.START, ViewSide.END,    gap)
        setC(overlapValueView, RemoteView.PARENT_ID, ViewSide.END,   ViewSide.END,    mgn)
        setC(overlapValueView, photoIntervalView,    ViewSide.TOP,   ViewSide.BOTTOM, vGapBlk)

        setC(nextBtn, overlapValueView,    ViewSide.TOP,   ViewSide.BOTTOM, vGapBlk)
        setC(nextBtn, backBtn.id,          ViewSide.START, ViewSide.END,    gap)
        setC(nextBtn, RemoteView.PARENT_ID,ViewSide.END,   ViewSide.END,    mgn)
        setC(backBtn, RemoteView.PARENT_ID,ViewSide.START, ViewSide.START,  mgn)
        setC(backBtn, nextBtn.id,          ViewSide.END,   ViewSide.START,  gap)
        setC(backBtn, overlapValueView,    ViewSide.TOP,   ViewSide.BOTTOM, vGapBlk)

        // ════════════════════════════════════════════════════════════
        // 事件绑定
        // ════════════════════════════════════════════════════════════

        zoomMinus1.setOnClickListener { updateZoom(-1.0) }
        zoomPlus1.setOnClickListener  { updateZoom(1.0)  }

        speedMinus1.setOnClickListener { updateSpeed(-1.0) }
        speedPlus1.setOnClickListener  { updateSpeed(1.0)  }

        backBtn.setOnClickListener { goBackToDirectionPage() }

        nextBtn.setOnClickListener {
            showExecutingPage("导线巡检中...", "终止巡检") { showExitConfirmDialog() }
            manager.enterBeginFollowState(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {}
                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("开始咬线失败: ${error.description()}")
                }
            })
        }
    }

    /**
     * 返航浮窗（导线巡检终止后显示）。
     */
    private fun showReturnHomePage() {
        // 标记返航流程已激活，后续 IDLE 回调跳过异常终止处理
        uiFlowPhase = UiFlowPhase.RETURNING_HOME
        RemoteViewPageManager.getInstance().clearLayout(openPluginBoxPageType)
        RemoteViewPageManager.getInstance().setPageLayoutParam(openPluginBoxPageType, LayoutParam(120))

        val mgn = 10

        returnHomeStatusView = RemoteView(ViewType.TEXT_VIEW, "导线巡检已结束，正在返航...", 1, 60, textBgColor, "")
        returnHomeStatusView!!.width = 0
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, returnHomeStatusView!!)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, returnHomeStatusView!!.id,
            RemoteViewProperty.TEXT_SIZE, fontSizeBody)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, returnHomeStatusView!!.id,
            RemoteViewProperty.FONT_STYLE_TYPE, FontStyleType.BOLD.value.toString())
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, returnHomeStatusView!!.id,
            RemoteViewProperty.GRAVITY, GravityType.CENTER.value.toString())

        setC(returnHomeStatusView!!, RemoteView.PARENT_ID, ViewSide.TOP,   ViewSide.TOP,   mgn)
        setC(returnHomeStatusView!!, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, mgn)
        setC(returnHomeStatusView!!, RemoteView.PARENT_ID, ViewSide.END,   ViewSide.END,   mgn)

        // 发送返航指令（通用飞控功能，保留在 VM 不纳入 Manager）
        FlightControllerKey.KeyStartGoHome.create().action({
            LogUtils.i(TAG, "返航指令发送成功")
        }, { error: IDJIError ->
            LogUtils.e(TAG, "返航指令发送失败: $error")
            ToastUtils.showToast("返航失败: ${error.description()}")
        })

        // 订阅返航状态（通用飞控功能，保留在 VM）
        FlightControllerKey.KeyGoHomeStatus.create().listen(this) { state ->
            LogUtils.i(TAG, "KeyGoHomeStatus = $state")
            val text = when (state) {
                GoHomeState.RETURNING_TO_HOME -> "正在返航（飞行中）..."
                GoHomeState.LANDING           -> "正在返航（降落中）..."
                GoHomeState.COMPLETED         -> "返航完成"
                GoHomeState.IDLE              -> "返航已结束"
                else                          -> "返航状态未知"
            }
            postDebounced("returnHomeStatus") {
                returnHomeStatusView?.let {
                    RemoteViewPageManager.getInstance().updateView(
                        openPluginBoxPageType, it.id, RemoteViewProperty.TEXT, text)
                }
            }
            if (state == GoHomeState.COMPLETED) {
                LogUtils.i(TAG, "返航结束（COMPLETED），恢复 Pilot 默认浮窗")
                uiHandler.postDelayed({
                    returnHomeStatusView = null
                    restoreDefaultFloatingBox()
                }, 1500)
            }
        }
    }

    /**
     * 恢复 Pilot 默认浮窗。
     */
    private fun restoreDefaultFloatingBox() {
        // 返航流程已结束，后续状态回调可正常处理
        uiFlowPhase = UiFlowPhase.INSPECTING
        RemoteViewPageManager.getInstance().clearLayout(openPluginBoxPageType)
        RemoteViewPageManager.getInstance().setPageLayoutParam(openPluginBoxPageType, LayoutParam(200))

        val jumpBtn = RemoteView(ViewType.BUTTON, "跳转MSDK插件", 0, 30, 0x380055FF, "")
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, jumpBtn)

        RemoteViewPageManager.getInstance().setConstraints(
            openPluginBoxPageType, jumpBtn.id, RemoteView.PARENT_ID, ViewSide.TOP, ViewSide.TOP, 120)
        RemoteViewPageManager.getInstance().setConstraints(
            openPluginBoxPageType, jumpBtn.id, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 0)
        RemoteViewPageManager.getInstance().setConstraints(
            openPluginBoxPageType, jumpBtn.id, RemoteView.PARENT_ID, ViewSide.END, ViewSide.END, 0)
        RemoteViewPageManager.getInstance().setConstraints(
            openPluginBoxPageType, jumpBtn.id, RemoteView.PARENT_ID, ViewSide.BOTTOM, ViewSide.BOTTOM, 0)

        jumpBtn.setOnClickListener {
            LogUtils.i(TAG, "restoreDefaultFloatingBox: 跳转 MSDK 插件")
            jumpTo(NavigateAction(NavigateAction.Value.SELF_DEFINE,
                "dji.sampleV5.aircraft.DJIAircraftMainActivity"))
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // 方向选择页辅助：更新导线状态 UI
    // ══════════════════════════════════════════════════════════════════

    /**
     * 实时更新方向选择页的导线状态显示和下一步按钮状态。
     * 纯 UI 操作，业务计算（findNearestCableId）已在 Manager 内部完成。
     */
    private fun updateDirectionPageCableStatus(cables: List<CableArInfo>, nearestCableId: Int) {
        val hasCable = cables.isNotEmpty() && nearestCableId >= 0

        val statusText = if (!hasCable) "正在识别导线..." else "检测到 ${cables.size} 条导线"
        cableStatusView?.let {
            postDebounced("cableStatus") {
                RemoteViewPageManager.getInstance().updateView(
                    openPluginBoxPageType, it.id, RemoteViewProperty.TEXT, statusText)
            }
        }

        val selectText = if (nearestCableId >= 0) "已选中导线: id=$nearestCableId" else "已选中导线: --"
        selectedCableView?.let {
            postDebounced("selectedCable") {
                RemoteViewPageManager.getInstance().updateView(
                    openPluginBoxPageType, it.id, RemoteViewProperty.TEXT, selectText)
            }
        }

        // 动态切换方向按钮状态
        val dirHintText = if (!hasCable) "识别中..." else null
        leftDirBtn?.let { btn ->
            postDebounced("leftDirBtnHint") {
                if (dirHintText != null) {
                    RemoteViewPageManager.getInstance().updateView(
                        openPluginBoxPageType, btn.id, RemoteViewProperty.TEXT, "← 识别中")
                    RemoteViewPageManager.getInstance().updateView(
                        openPluginBoxPageType, btn.id, RemoteViewProperty.BACKGROUND_COLOR,
                        Color.DKGRAY.toString())
                } else if (!directionSelected || manager.inspectionConfig.direction != InspectionDirection.LEFT) {
                    RemoteViewPageManager.getInstance().updateView(
                        openPluginBoxPageType, btn.id, RemoteViewProperty.TEXT, "← 向左")
                    RemoteViewPageManager.getInstance().updateView(
                        openPluginBoxPageType, btn.id, RemoteViewProperty.BACKGROUND_COLOR,
                        normalBtnColor.toString())
                }
            }
        }
        rightDirBtn?.let { btn ->
            postDebounced("rightDirBtnHint") {
                if (dirHintText != null) {
                    RemoteViewPageManager.getInstance().updateView(
                        openPluginBoxPageType, btn.id, RemoteViewProperty.TEXT, "识别中 →")
                    RemoteViewPageManager.getInstance().updateView(
                        openPluginBoxPageType, btn.id, RemoteViewProperty.BACKGROUND_COLOR,
                        Color.DKGRAY.toString())
                } else if (!directionSelected || manager.inspectionConfig.direction != InspectionDirection.RIGHT) {
                    RemoteViewPageManager.getInstance().updateView(
                        openPluginBoxPageType, btn.id, RemoteViewProperty.TEXT, "向右 →")
                    RemoteViewPageManager.getInstance().updateView(
                        openPluginBoxPageType, btn.id, RemoteViewProperty.BACKGROUND_COLOR,
                        normalBtnColor.toString())
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // 导航辅助方法
    // ══════════════════════════════════════════════════════════════════

    private fun goBackToDirectionPage() {
        LogUtils.i(TAG, "goBackToDirectionPage: 开始, currentState=${cachedState}")
        // 重置 UI 流程阶段 + 取消 FINISH 延迟 Runnable（防止 2s 内返回导致意外返航）
        uiFlowPhase = UiFlowPhase.INSPECTING
        finishDelayRunnable?.let { uiHandler.removeCallbacks(it) }
        finishDelayRunnable = null

        // LiveData 用 postValue（线程安全，RemoteView 回调可能在非主线程）
        inspectionState.postValue(InspectionState.SELECTING_CABLE)

        directionSelected = false
        uiHandler.post {
            LogUtils.i(TAG, "goBackToDirectionPage: 执行 prepareGoBack + showDirectionPage")
            manager.prepareGoBack(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() { LogUtils.i(TAG, "goBackToDirectionPage: prepareGoBack done") }
                override fun onFailure(error: IDJIError) { LogUtils.i(TAG, "goBackToDirectionPage: prepareGoBack failed: $error") }
            })
            showDirectionPage()
        }
    }

    private fun showExitConfirmDialog() {
        val config = DialogConfig.Builder()
            .setTitle("终止导线巡检")
            .setMessage("确认终止导线巡检吗？退出后将终止当前巡检任务。")
            .setStartButtonText("取消")
            .setEndButtonText("确认退出")
            .setOnStartButtonClick(object : IOpenViewOnClickListener {
                override fun onClick() {
                    LogUtils.i(TAG, "showExitConfirmDialog: 用户取消退出")
                }
            })
            .setOnEndButtonClick(object : IOpenViewOnClickListener {
                override fun onClick() {
                    LogUtils.i(TAG, "showExitConfirmDialog: 用户确认退出")
                    uiHandler.post {
                        manager.enterExitMissionState(object : CommonCallbacks.CompletionCallback {
                            override fun onSuccess() { LogUtils.i(TAG, "showExitConfirmDialog: CMD_EXIT done") }
                            override fun onFailure(error: IDJIError) { LogUtils.i(TAG, "showExitConfirmDialog: CMD_EXIT failed: $error") }
                        })
                        directionSelected   = false
                        manager.inspectionConfig.direction = InspectionDirection.NONE
                        try { manager.clearCableARAnnotations() } catch (e: Exception) {
                            LogUtils.w(TAG, "showExitConfirmDialog: clearCableARAnnotations 异常（忽略）: $e")
                        }
                        inspectionState.value = InspectionState.IDLE
                        RemoteViewPageManager.getInstance().clearLayout(openPluginBoxPageType)
                        showReturnHomePage()
                    }
                }
            })
            .build()
        RemoteDialogManager.getInstance().showConfirmDialog(config)
    }

    // ══════════════════════════════════════════════════════════════════
    // UI 控件更新辅助方法（纯 UI，通过 manager 属性读写参数）
    // ══════════════════════════════════════════════════════════════════

    private fun updateDistance(delta: Double) {
        manager.inspectionConfig.flightDistance = (manager.inspectionConfig.flightDistance + delta).coerceIn(5.0, 10.0)
        val text = String.format("%.1f", manager.inspectionConfig.flightDistance)
        postDebounced("distance") {
            RemoteViewPageManager.getInstance().updateView(
                openPluginBoxPageType, distanceEditView.id, RemoteViewProperty.TEXT, text)
        }
    }

    private fun updatePitch(delta: Double) {
        manager.inspectionConfig.pitchAngle = (manager.inspectionConfig.pitchAngle + delta).coerceIn(0.0, 35.0)
        val text = String.format("%.0f", manager.inspectionConfig.pitchAngle)
        postDebounced("pitch") {
            RemoteViewPageManager.getInstance().updateView(
                openPluginBoxPageType, pitchEditView.id, RemoteViewProperty.TEXT, text)
        }
    }

    private fun updateZoom(delta: Double) {
        val range = CameraKey.KeyCameraZoomRatiosRange.create().get()?.gears
        val min = range?.firstOrNull()?.toDouble() ?: 1.0
        val max = range?.lastOrNull()?.toDouble() ?: Double.MAX_VALUE
        manager.inspectionConfig.zoomLevel = (manager.inspectionConfig.zoomLevel + delta).coerceIn(min, max)
        val text = String.format("%.0f", manager.inspectionConfig.zoomLevel)
        postDebounced("zoom") {
            RemoteViewPageManager.getInstance().updateView(
                openPluginBoxPageType, zoomEditView.id, RemoteViewProperty.TEXT, text)
        }
        updatePhotoIntervalView()
    }

    private fun updateSpeed(delta: Double) {
        manager.inspectionConfig.inspectionSpeed = (manager.inspectionConfig.inspectionSpeed + delta).coerceIn(1.0, 10.0)
        val text = String.format("%.0f", manager.inspectionConfig.inspectionSpeed)
        postDebounced("speed") {
            RemoteViewPageManager.getInstance().updateView(
                openPluginBoxPageType, speedEditView.id, RemoteViewProperty.TEXT, text)
        }
        updatePhotoIntervalView()
    }

    private fun updatePhotoIntervalView() {
        if (!::photoIntervalView.isInitialized) return
        val cfg = manager.inspectionConfig
        val result = manager.calculatePhotoInterval(cfg.flightDistance, cfg.zoomLevel, cfg.inspectionSpeed)
        if (result.speedAdjusted) {
            cfg.inspectionSpeed = result.adjustedSpeed
            postDebounced("speed") {
                RemoteViewPageManager.getInstance().updateView(
                    openPluginBoxPageType, speedEditView.id,
                    RemoteViewProperty.TEXT, String.format("%.0f", result.adjustedSpeed))
            }
        }
        val text = "%.1fs".format(result.intervalMs / 1000.0)
        postDebounced("photoInterval") {
            RemoteViewPageManager.getInstance().updateView(
                openPluginBoxPageType, photoIntervalView.id, RemoteViewProperty.TEXT, text)
        }
    }

    private fun postDebounced(key: String, delayMs: Long = 80L, action: () -> Unit) {
        pendingUpdates[key]?.let { uiHandler.removeCallbacks(it) }
        val runnable = Runnable { ipcExecutor.submit(action) }
        pendingUpdates[key] = runnable
        uiHandler.postDelayed(runnable, delayMs)
    }

    private fun setC(view: RemoteView, toId: String, side: ViewSide, toSide: ViewSide, margin: Int) {
        RemoteViewPageManager.getInstance().setConstraints(
            openPluginBoxPageType, view.id, toId, side, toSide, margin)
    }

    // setC 重载：支持直接传 RemoteView 作为 anchor
    private fun setC(view: RemoteView, toView: RemoteView, side: ViewSide, toSide: ViewSide, margin: Int) {
        setC(view, toView.id, side, toSide, margin)
    }

    // ══════════════════════════════════════════════════════════════════
    // companion object
    // ══════════════════════════════════════════════════════════════════

    companion object {
        private const val TAG = "CableInspectionVM"
    }
}
