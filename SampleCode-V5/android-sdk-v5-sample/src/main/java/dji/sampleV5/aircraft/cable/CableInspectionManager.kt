package dji.sampleV5.aircraft.cable

import android.os.Handler
import android.os.Looper
import dji.open.pilot.base.manager.OpenLocation
import dji.open.pilot.base.manager.ar.ARIconOrthographicConfig
import dji.open.pilot.base.manager.ar.ARIconPerspectiveConfig
import dji.open.pilot.base.manager.ar.ARIconProjectionType
import dji.open.pilot.base.manager.ar.ARPointConfig
import dji.open.pilot.base.manager.ar.ARPointInfo
import dji.open.pilot.base.manager.ar.ARTextConfig
import dji.open.pilot.client.manager.ui.ARManager
import dji.v5.ux.util.Helper
import dji.sampleV5.aircraft.util.LocationUtil
import dji.sampleV5.aircraft.util.PolygonUtils
import dji.sampleV5.aircraft.util.compareVer
import dji.sampleV5.aircraft.util.geometry.DoubleVector3
import dji.sdk.keyvalue.key.AirLinkKey
import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.key.FlightAssistantKey
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.RtkMobileStationKey
import dji.sdk.keyvalue.utils.MultiComponentManager.converterIndexVideoToNormal
import dji.sdk.keyvalue.value.airlink.VideoSourceEntity
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.common.SourceMajorType
import dji.sdk.keyvalue.value.flightassistant.CableInspectionCmd
import dji.sdk.keyvalue.value.flightassistant.CableInspectionConfigMsg
import dji.sdk.keyvalue.value.flightassistant.CableInspectionDir
import dji.sdk.keyvalue.value.flightassistant.CableInspectionErrCode
import dji.sdk.keyvalue.value.flightassistant.CableInspectionErrCodeMsg
import dji.sdk.keyvalue.value.flightassistant.CableInspectionGimConfig
import dji.sdk.keyvalue.value.flightassistant.CableInspectionState
import dji.sdk.keyvalue.value.flightcontroller.CableArInfo
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.DJICommonError
import dji.v5.common.error.IDJIError
import dji.v5.et.create
import dji.v5.et.get
import dji.v5.et.listen
import dji.v5.et.set
import dji.v5.manager.KeyManager
import dji.v5.manager.areacode.AreaCode
import dji.v5.manager.areacode.AreaCodeChangeListener
import dji.v5.manager.areacode.AreaCodeManager
import dji.v5.utils.common.LogUtils
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors

/**
 * 导线巡检管理器实现类。
 *
 * 封装导线巡检所有原生 Key 操作、业务逻辑与计算，参照
 * CameraStreamManager 的设计模式：
 * - 双重检查锁定单例（DCL），getInstance 返回接口类型 ICableInspectionManager
 * - init 中按顺序注册所有 Key 订阅，destroy 中统一注销并执行兜底清理
 * - 支持检查相关 Key（飞控版本、相机型号/固件、地区码）全部收敛在内部，
 *   仅通过 SupportDetail 和 SupportStatusListener 对外暴露综合结果
 * - 通过 CopyOnWriteArraySet 管理 Listener，支持多订阅方，线程安全
 */
class CableInspectionManager private constructor() : ICableInspectionManager {

    // ══════════════════════════════════════════════════════════════════
    // 单例
    // ══════════════════════════════════════════════════════════════════

    companion object {
        private const val TAG = "CableInspectionManager"

        /* 飞控固件版本最低要求，低于此版本不支持导线巡检 */
        const val FLIGHT_MIN_VER = "02.00.00.12"

        /* 相机固件版本最低要求，低于此版本不支持导线巡检 */
        const val CAMERA_MIN_VER = "10.03.1201"

        /* 飞机到导线的最大有效水平距离（m）。超出此范围的导线视为无效，参照 Pilot 40m 阈值 */
        private const val MAX_CABLE_DIST = 40.0

        /* 方向箭头 SVG 文件名（存放在 assets/plugin/ 目录下） */
        private const val AR_ARROW_LEFT_SVG  = "ar_cable_arrow_left_2.svg"
        private const val AR_ARROW_RIGHT_SVG = "ar_cable_arrow_right_2.svg"

        /* 导线高亮圆点 SVG 文件名 */
        private const val AR_HIGHLIGHT_DOT_SVG = "ar_cable_highlight_dot.svg"

        @Volatile
        private var instance: CableInspectionManager? = null

        /*
         * 获取 CableInspectionManager 单例。
         * 双重检查锁定（DCL），线程安全。
         */
        fun getInstance(): CableInspectionManager {
            return instance ?: synchronized(this) {
                instance ?: CableInspectionManager().also { instance = it }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // 内部常量
    // ══════════════════════════════════════════════════════════════════

    /* H30/H30T Zoom 镜头 1× 基础焦距（mm）。来源：Pilot 日志 base focus=5110（单位 0.001mm） */
    private val baseFocalLength1x = 5.11

    /* 支持的相机型号正则，忽略大小写匹配。当前仅支持 H30 和 H30T */
    private val camTypePattern = "ZENMUSE_H30|ZENMUSE_H30T"

    // ══════════════════════════════════════════════════════════════════
    // 线程工具
    // ══════════════════════════════════════════════════════════════════

    /* 主线程 Handler，用于需要切主线程的操作 */
    private val uiHandler = Handler(Looper.getMainLooper())

    /* 单线程池，将耗时的 IPC updateView 调用移出主线程，避免阻塞 UI */
    private val ipcExecutor = Executors.newSingleThreadExecutor()

    // ══════════════════════════════════════════════════════════════════
    // Listener 集合（线程安全）
    // ══════════════════════════════════════════════════════════════════

    /* 使用 CopyOnWriteArraySet 保证多线程下的 add/remove/遍历安全 */
    private val listeners = CopyOnWriteArraySet<ICableInspectionManager.CableInspectionListener>()

    // ══════════════════════════════════════════════════════════════════
    // 内部状态
    // ══════════════════════════════════════════════════════════════════

    /* 飞控侧当前巡检状态，用于 enterExitMissionState 的前置判断（STANDBY 时跳过发送） */
    private var fcCableStatus: CableInspectionState = CableInspectionState.STANDBY

    // ── 飞机实时位置（由 KeyAircraftLocation3D 持续更新，用于最近导线距离计算）──
    private var droneLatitude  = 0.0  // 度
    private var droneLongitude = 0.0  // 度
    private var droneAltitude  = 0.0  // 米，相对起飞高度（气压计）

    /* 起飞点 WGS84 椭球高度（m），由 KeyRTKTakeoffAltitudeInfo 更新。
     * 导线高度为绝对高度，减去此值后与飞机气压计高度统一参考系 */
    private var takeOffAltitude = 0.0

    /* 起飞点 MSL 海拔高度（m），由 KeyTakeoffLocationAltitude（GPS）更新。
     * 无 RTK 时用作 AR 文字高度转换：cable绝对高度 - takeoffLocationAltitude = 相对起飞点高度。
     * 精度低于 RTK 但无需额外硬件。 */
    private var takeoffLocationAltitude = 0.0

    /* 当前混合变焦焦距，单位 0.1mm，由 KeyCameraHybridZoomFocalLength 订阅更新 */
    private var currentHybridFocalLength: Int = 0

    // ── 支持检查内部数据（不单独对外暴露，全部收敛到 SupportDetail 汇总后通知）──
    private var internalFlyVer: String? = null
    private val internalCameraInfoMap  = mutableMapOf<ComponentIndexType, CameraInfo>()
    private var internalSelectedCameraIndex: ComponentIndexType? = null
    private var internalAreaCode: String? = null

    /* Key 订阅的生命周期 holder 对象，destroy 时通过 cancelListen(listenerHolder) 统一注销所有 Key 订阅 */
    private val listenerHolder = Any()

    /* 地区码变化订阅器引用，注销时需持有此引用 */
    private var areaCodeChangeListener: AreaCodeChangeListener? = null

    // ── AR 标注配置 ──
    val arAnnotationConfig = CableARAnnotationConfig()

    // ── AR Config 实例（uploadCableARConfigs 中 build，直接通过 .id 引用，参照 ARProjectionVM 模式）──
    /** 中心点 config：圆点图标 + 文字一体（ARFontGravity.TOP 保证文字在图标正上方） */
    private var centerDotTextConfig: ARPointConfig? = null
    /** 首尾端圆点 config：仅圆点图标，无文字 */
    private var plainDotConfig: ARPointConfig?      = null
    private var arrowLeftConfig: ARPointConfig?     = null  // 左箭头 ARPointConfig
    private var arrowRightConfig: ARPointConfig?    = null  // 右箭头 ARPointConfig
    /** 全部 config 是否上传成功；为 false 时跳过 AR 标注创建 */
    @Volatile private var arConfigsReady   = false
    private var arConfigUploadCount        = 0

    // ── AR PointInfo 实例（用于 update / delete，null 表示尚未上传）──
    /** 中心点：圆点 + 文字一体（导线几何中点） */
    private var centerPointInfo:    ARPointInfo? = null
    /** 首端圆点（仅图标） */
    private var highlightDotStart:  ARPointInfo? = null
    /** 尾端圆点（仅图标） */
    private var highlightDotEnd:    ARPointInfo? = null
    /** 方向箭头（同时只有 1 个） */
    private var arrowPointInfo:     ARPointInfo? = null

    /** 上次自动高亮的导线 ID，防止数据频繁推送时重复重建同一条导线的 AR 标注 */
    private var lastAutoHighlightId: Int = -1

    // ══════════════════════════════════════════════════════════════════
    // 状态属性（实现类直接暴露，不在接口中定义，VM 通过 Listener 回调获取变化）
    // ══════════════════════════════════════════════════════════════════

    @Volatile
    var currentState: InspectionState = InspectionState.IDLE
        private set

    private var _currentCableArInfoList: List<CableArInfo> = emptyList()

    var currentSelectedCableId: Int = -1
        private set

    private var _zoomRange: IntArray? = null

    // ══════════════════════════════════════════════════════════════════
    // 配置参数（实现类直接暴露，VM 通过属性读写）
    // ══════════════════════════════════════════════════════════════════

    val inspectionConfig = InspectionConfig()

    // ══════════════════════════════════════════════════════════════════
    // 生命周期
    // ══════════════════════════════════════════════════════════════════

    /**
     * 初始化 Manager，按以下顺序注册所有 Key 订阅：
     * 1. 启动清理：无条件发送 CMD_EXIT，清理上次异常退出（崩溃/强杀）可能残留的飞控状态
     * 2. 启用电线 AR 功能：KeyCableArEnabled set true，飞控开始运行导线识别管线
     * 3. 注册 KeyCableArInfo 数据订阅（不等待 AR 启用完成，避免首次进入 DIR 时数据延迟）
     * 3. KeyCableInspectionState 状态订阅（驱动 App 侧状态机）
     * 4. KeyAircraftLocation3D 位置订阅（为 findNearestCableId 提供飞机坐标）
     * 5. KeyRTKTakeoffAltitudeInfo 先 get 当前值再 listen（起飞点椭球高度，统一高度参考系）
     * 6. KeyCameraHybridZoomFocalLength 焦距订阅（动态计算默认变焦倍率时备用）
     * 7. 支持检查订阅：KeyFirmwareVersion（飞控）、KeyAvailableVideoSource（相机流发现）、
     *    AreaCodeManager（地区码），任一变化触发 recalculateSupport
     * 8. KeyCameraZoomRatiosRange get（读取相机变焦档位范围，供 UI 显示上下限）
     */
    override fun init() {

        // 启动清理：飞控在 STANDBY 下收到 CMD_EXIT 直接忽略，无副作用
        KeyManager.getInstance().performAction(
            FlightAssistantKey.KeyCableInspection.create(),
            CableInspectionConfigMsg().apply { cmd = CableInspectionCmd.CMD_EXIT },
            object : CommonCallbacks.CompletionCallbackWithParam<CableInspectionErrCodeMsg> {
                override fun onSuccess(result: CableInspectionErrCodeMsg?) {
                    LogUtils.i(TAG, "init: 启动清理 CMD_EXIT 完成, errCode=${result?.err_code}")
                }
                override fun onFailure(error: IDJIError) {
                    LogUtils.w(TAG, "init: 启动清理 CMD_EXIT 失败（可忽略）: $error")
                }
            })

        // 启用电线 AR 功能，飞控开始将识别到的导线信息推送给 KeyCableArInfo
        KeyManager.getInstance().setValue(
            FlightControllerKey.KeyCableArEnabled.create(), true,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    LogUtils.i(TAG, "init: KeyCableArEnabled set true 成功")
                }
                override fun onFailure(error: IDJIError) {
                    LogUtils.e(TAG, "init: KeyCableArEnabled set true 失败: $error")
                }
            })

        // 提前注册导线数据订阅（不等待 KeyCableArEnabled 成功回调），确保用户进入方向选择页时能立即响应数据推送
        FlightControllerKey.KeyCableArInfo.create().listen(listenerHolder) { info ->
            info?.let {
                val cables = it.cables ?: emptyList()
                onCableArInfoReceived(cables)
            }
        }

        // 状态订阅：飞控状态变化 → onFcStateChanged 映射为 App 侧状态并通知 StateListener
        FlightAssistantKey.KeyCableInspectionState.create().listen(listenerHolder) { msg ->
            msg?.let { onFcStateChanged(it.state, it.err_code) }
        }

        // 位置订阅：实时更新飞机 LLA 坐标，供 findNearestCableId 计算水平距离
        FlightControllerKey.KeyAircraftLocation3D.create().listen(listenerHolder) { loc ->
            loc?.let {
                droneLatitude  = it.latitude
                droneLongitude = it.longitude
                droneAltitude  = it.altitude
            }
        }

        // 起飞点椭球高度：先同步 get 当前值，再 listen 后续变化，避免 RTK 已就绪但未触发回调的场景
        RtkMobileStationKey.KeyRTKTakeoffAltitudeInfo.create().get()?.let {
            if (it.valid == true) takeOffAltitude = it.altitude
        }
        RtkMobileStationKey.KeyRTKTakeoffAltitudeInfo.create().listen(listenerHolder) { info ->
            info?.let { if (it.valid == true) takeOffAltitude = it.altitude }
        }

        // 起飞点 MSL 海拔高度（GPS，无需 RTK）：用于 AR 文字高度转换
        FlightControllerKey.KeyTakeoffLocationAltitude.create().get()?.let {
            takeoffLocationAltitude = it
        }
        FlightControllerKey.KeyTakeoffLocationAltitude.create().listen(listenerHolder) { alt ->
            alt?.let { takeoffLocationAltitude = it }
        }

        // 混合变焦焦距订阅：使用选中相机 index，未选时默认 LEFT_OR_MAIN
        val camIdx = internalSelectedCameraIndex ?: ComponentIndexType.LEFT_OR_MAIN
        CameraKey.KeyCameraHybridZoomFocalLength.create(camIdx).listen(listenerHolder) { focal ->
            focal?.let { currentHybridFocalLength = it }
        }

        // ── 支持检查：飞控固件版本 ─────────────────────────────────────
        FlightControllerKey.KeyFirmwareVersion.create().listen(listenerHolder) { ver ->
            internalFlyVer = ver
            recalculateSupport()
        }

        // ── 支持检查：可用相机流发现，动态为每路相机注册固件版本和型号订阅 ──
        // sources 变化时清空 map 重建，确保相机热插拔场景下数据一致
        AirLinkKey.KeyAvailableVideoSource.create().listen(listenerHolder) { sources ->
            internalCameraInfoMap.clear()
            val normalizedSources = sources?.onEach { updateSourceInternal(it) } ?: emptyList()
            for (source in normalizedSources) {
                val index = source.getPosition()
                internalCameraInfoMap.getOrPut(index) { CameraInfo(index, null, null) }
                listenCameraInfo(index)
            }
            if (normalizedSources.isEmpty()) recalculateSupport()
        }

        // ── 支持检查：地区码订阅 ───────────────────────────────────────
        // 同时同步读取当前值，避免地区码已就绪但尚未触发变化回调的场景
        areaCodeChangeListener = AreaCodeChangeListener { _, _ ->
            internalAreaCode = AreaCodeManager.getInstance().areaCode.areaCode
            recalculateSupport()
        }
        AreaCodeManager.getInstance().addAreaCodeChangeListener(areaCodeChangeListener!!)
        val currentAreaCode = AreaCodeManager.getInstance().areaCode.areaCode
        if (!currentAreaCode.isNullOrEmpty() && currentAreaCode != "N/A") {
            internalAreaCode = currentAreaCode
        }

        // 读取相机变焦档位范围，供 calculateDefaultZoom 的 clamp 和 UI 范围显示
        _zoomRange = CameraKey.KeyCameraZoomRatiosRange.create().get()?.gears
    }

    /**
     * 销毁 Manager，按以下顺序执行清理：
     * 1. 清除主线程 Handler 中所有待执行消息和回调
     * 2. 关闭 IPC 单线程池
     * 3. 注销所有 Key 订阅（通过 listenerHolder 统一 cancelListen）
     * 4. 注销 AreaCodeManager 地区码订阅
     * 5. 发送 CMD_EXIT 兜底（确保飞控完全退出巡检状态，覆盖进程被杀等异常场景）
     * 6. 关闭电线 AR 功能（KeyCableArEnabled set false）
     * 7. 重置所有内部状态到初始值，为下次 init 做准备
     */
    override fun destroy() {
        uiHandler.removeCallbacksAndMessages(null)
        ipcExecutor.shutdown()
        KeyManager.getInstance().cancelListen(listenerHolder)
        areaCodeChangeListener?.let {
            AreaCodeManager.getInstance().removeAreaCodeChangeListener(it)
        }
        areaCodeChangeListener = null

        // CMD_EXIT 兜底：覆盖 ViewModel.onCleared 之前未能正常发送命令的场景
        KeyManager.getInstance().performAction(
            FlightAssistantKey.KeyCableInspection.create(),
            CableInspectionConfigMsg().apply { cmd = CableInspectionCmd.CMD_EXIT },
            object : CommonCallbacks.CompletionCallbackWithParam<CableInspectionErrCodeMsg> {
                override fun onSuccess(result: CableInspectionErrCodeMsg?) {
                    LogUtils.i(TAG, "destroy: CMD_EXIT 成功, errCode=${result?.err_code}")
                }
                override fun onFailure(error: IDJIError) {
                    LogUtils.e(TAG, "destroy: CMD_EXIT 失败: $error")
                }
            })

        // 关闭电线 AR：防止 Manager 销毁后 AR 仍持续运行消耗资源
        FlightControllerKey.KeyCableArEnabled.create().set(false)

        // 重置内部状态，保证下次 init 从干净状态开始
        currentState = InspectionState.IDLE
        _currentCableArInfoList = emptyList()
        currentSelectedCableId = -1
        fcCableStatus = CableInspectionState.STANDBY
        internalFlyVer = null
        internalCameraInfoMap.clear()
        internalSelectedCameraIndex = null
        internalAreaCode = null
        _zoomRange = null

        // 重置巡检配置参数
        inspectionConfig.reset()

        // AR 标注清理：删除已上传的 AR 点，重置所有 AR 状态
        clearCableARAnnotations()
        centerDotTextConfig = null
        plainDotConfig     = null
        arrowLeftConfig    = null
        arrowRightConfig   = null
        arConfigsReady     = false
        arConfigUploadCount = 0
        arAnnotationConfig.highlightText = "当前选中导线"
        arAnnotationConfig.textColor     = 0xFF1E90FF.toInt()
        lastAutoHighlightId = -1
        takeoffLocationAltitude = 0.0
    }

    // ══════════════════════════════════════════════════════════════════
    // 支持检查相关
    // ══════════════════════════════════════════════════════════════════

    /**
     * 设置用户选中的相机 index，并立即触发支持状态重算。
     * 多相机场景下，支持判断依赖此值；单相机场景下无需调用。
     */
    fun selectCamera(index: ComponentIndexType) {
        internalSelectedCameraIndex = index
        recalculateSupport()
    }

    /**
     * 为指定相机注册固件版本（KeyFirmwareVersion）和型号（KeyCameraType）的持续订阅。
     * 任一回调触发时更新 internalCameraInfoMap 并调用 recalculateSupport 重算支持状态。
     * 使用 CameraInfo.copy 保证 data class 不可变性。
     */
    private fun listenCameraInfo(index: ComponentIndexType) {
        CameraKey.KeyFirmwareVersion.create(index).listen(listenerHolder) { ver ->
            val old = internalCameraInfoMap[index]
            val existing = old ?: CameraInfo(index, null, null)
            internalCameraInfoMap[index] = CameraInfo(existing.index, ver, existing.camType)
            recalculateSupport()
        }
        CameraKey.KeyCameraType.create(index).listen(listenerHolder) { type ->
            val old = internalCameraInfoMap[index]
            val existing = old ?: CameraInfo(index, null, null)
            internalCameraInfoMap[index] = CameraInfo(existing.index, existing.camVer, type?.toString())
            recalculateSupport()
        }
    }

    /**
     * 综合判断是否支持导线巡检，并通过 SupportStatusListener 通知结果。
     * 所有支持检查条件（飞控版本、相机型号/固件、地区码）全部在此收敛，不对外单独暴露。
     *
     * 判断规则：
     * - 飞控固件 >= FLIGHT_MIN_VER（compareVer 返回 true）
     * - 相机条件（camVer >= CAMERA_MIN_VER 且 camType 匹配 H30/H30T）：
     *     · 单相机：直接对 map 中唯一相机判断
     *     · 多相机：对 internalSelectedCameraIndex 所指相机判断，未选则直接置 false 并返回
     * - 地区码条件（当前已注释，预留扩展）：AreaCodeManager.areaCode.areaCodeEnum == CHINA
     *
     * 注：地区码从 AreaCodeManager 实时读取枚举值，避免依赖 internalAreaCode 的异步时序。
     * overrideSelected: selectCamera 调用时传入最新值，避免 postValue 时序问题覆盖为 null
     */
    private fun recalculateSupport(overrideSelected: ComponentIndexType? = internalSelectedCameraIndex) {
        val fly = compareVer(FLIGHT_MIN_VER, internalFlyVer)
        // 直接从 AreaCodeManager 实时读取枚举，避免依赖 internalAreaCode 的异步时序
        val countryOk = AreaCodeManager.getInstance().areaCode.areaCodeEnum == AreaCode.CHINA

        val camOk = when {
            internalCameraInfoMap.isEmpty() -> false
            internalCameraInfoMap.size == 1 -> {
                val info = internalCameraInfoMap.values.first()
                compareVer(CAMERA_MIN_VER, info.camVer) && isCamSupport(info.camType ?: "")
            }
            else -> {
                // 多相机未选时直接置 false 并提前返回，避免支持状态在选相机前被误判为 true
                val selected = overrideSelected ?: run {
                    notifySupportStatus(buildSupportDetailWith(false, overrideSelected))
                    return
                }
                val info = internalCameraInfoMap[selected]
                info != null && compareVer(CAMERA_MIN_VER, info.camVer) && isCamSupport(info.camType ?: "")
            }
        }

        // 当前只检查飞控版本和相机条件，国家码检查已注释，与业务侧保持一致
//        val isSupported = fly && camOk && countryOk
        val isSupported = fly && camOk
        notifySupportStatus(buildSupportDetailWith(isSupported, overrideSelected))
    }

    /**
     *内部辅助：根据给定的 isSupported 和 selected 构造 SupportDetail 实例
     */
    private fun buildSupportDetailWith(isSupported: Boolean, selected: ComponentIndexType?): SupportDetail {
        return SupportDetail(
            isSupported,
            internalFlyVer,
            internalCameraInfoMap.values.toList(),
            selected ?: internalSelectedCameraIndex,
            internalAreaCode
        )
    }

    // ══════════════════════════════════════════════════════════════════
    // 状态机：飞控状态 → App 侧状态映射
    // ══════════════════════════════════════════════════════════════════

    /**
     * 处理飞控状态推送，将飞控侧 CableInspectionState 映射为 App 侧 InspectionState。
     *
     * 映射规则：
     *   ADJUST_POS   → POS        WAITING      → WAITING
     *   CALIBRATION  → CAM_CALI   CAM_CONFIG   → CAM_CONFIG
     *   FOLLOW       → FOLLOW     FINISH       → FINISH
     *   SELECT_CABLE → DIR
     *   STANDBY      → 若 oldState 为 IDLE 或 DIR 则保持不变（用户 goBack 已预设状态为 DIR），
     *                  否则回落到 IDLE（表示任务被意外终止）
     *
     * 特殊行为：
     * - FINISH：立即自动发送 CMD_EXIT，防止 2s 延迟内进程被杀导致命令丢失
     * - 异常回落到 IDLE（从非 IDLE/DIR 跳到 IDLE）：通过 ErrorListener 通知错误详情
     */
    private fun onFcStateChanged(fcState: CableInspectionState, errCode: CableInspectionErrCode) {
        val oldState = currentState
        fcCableStatus = fcState
        LogUtils.i(TAG, "onFcStateChanged: fcState=$fcState errCode=$errCode oldAppState=$oldState")

        val newState = when (fcState) {
            CableInspectionState.ADJUST_POS   -> InspectionState.ADJUSTING_POSITION
            CableInspectionState.WAITING      -> InspectionState.WAITING_FOR_CONFIG
            CableInspectionState.CALIBRATION  -> InspectionState.CAMERA_CALIBRATING
            CableInspectionState.CAM_CONFIG   -> InspectionState.CAMERA_CONFIGURING
            CableInspectionState.FOLLOW       -> InspectionState.FOLLOWING
            CableInspectionState.FINISH       -> InspectionState.FINISHED
            CableInspectionState.SELECT_CABLE -> InspectionState.SELECTING_CABLE
            CableInspectionState.STANDBY      -> when {
                    // MSDK 主动 goBack 已预设 currentState=DIR → 保持 DIR
                    // 首次进入/IDLE 状态 → 保持不变
                    oldState == InspectionState.IDLE
                        || oldState == InspectionState.SELECTING_CABLE -> oldState

                    // 从 POS/WAITING 退回 STANDBY（Pilot 端 goBack 或 MSDK goBack）
                    // 这是正常的返回流程，映射为 DIR（等价于 Pilot goBack 先设 state=DIR）
                    oldState == InspectionState.ADJUSTING_POSITION
                        || oldState == InspectionState.WAITING_FOR_CONFIG -> InspectionState.SELECTING_CABLE

                    // 从 FOLLOW/CAM_CALI/CAM_CONFIG/FINISH 等退回 STANDBY → 异常终止
                    else -> InspectionState.IDLE
                }
            else -> oldState
        }

        currentState = newState

        // FINISH 时立即发 CMD_EXIT，不等上层 2s 延迟，防止进程被杀导致命令丢失
        if (newState == InspectionState.FINISHED) {
            enterExitMissionState(null)
        }

        // 从非正常状态异常回落到 IDLE 时，通知 ErrorListener（OK 和 USER_REQ_EXIT 不触发）
        if (newState == InspectionState.IDLE && oldState != InspectionState.IDLE
            && oldState != InspectionState.SELECTING_CABLE) {
            val errMsg = mapErrCode(errCode)
            if (errMsg != null) {
                notifyError(errCode, errMsg)
            }
        }

        notifyStateChanged(oldState, newState, errCode)
    }

    // ══════════════════════════════════════════════════════════════════
    // 命令方法
    // ══════════════════════════════════════════════════════════════════

    /**
     * 发送 CMD_EXIT 命令，终止当前导线巡检任务。
     * 前置判断：飞控处于 STANDBY 时跳过发送（无意义且浪费 IPC），直接调用 onSuccess。
     * 多次调用安全：飞控在 STANDBY 下忽略 CMD_EXIT，无副作用。
     */
    override fun enterExitMissionState(callback: CommonCallbacks.CompletionCallback?) {
        if (fcCableStatus != CableInspectionState.STANDBY) {
            KeyManager.getInstance().performAction(
                FlightAssistantKey.KeyCableInspection.create(),
                CableInspectionConfigMsg().apply { cmd = CableInspectionCmd.CMD_EXIT },
                object : CommonCallbacks.CompletionCallbackWithParam<CableInspectionErrCodeMsg> {
                    override fun onSuccess(result: CableInspectionErrCodeMsg?) {
                        LogUtils.i(TAG, "enterExitMissionState: 成功, errCode=${result?.err_code}")
                        callback?.onSuccess()
                    }
                    override fun onFailure(error: IDJIError) {
                        LogUtils.e(TAG, "enterExitMissionState: 失败: $error")
                        callback?.onFailure(error)
                    }
                })
        } else {
            callback?.onSuccess()
        }
    }

    /**
     * 发送 SELECT_CABLE 命令，通知飞控进入选线识别状态。
     * 飞控收到后开始识别视野内的导线，并通过 KeyCableArInfo 推送识别结果。
     * init() 中已注册 KeyCableArInfo.listen()，导线数据会通过订阅回调实时获取。
     */
    override fun enterSelectCableState(callback: CommonCallbacks.CompletionCallback?) {
        KeyManager.getInstance().performAction(
            FlightAssistantKey.KeyCableInspection.create(),
            CableInspectionConfigMsg().apply { cmd = CableInspectionCmd.SELECT_CABLE },
            object : CommonCallbacks.CompletionCallbackWithParam<CableInspectionErrCodeMsg> {
                override fun onSuccess(result: CableInspectionErrCodeMsg?) {
                    LogUtils.i(TAG, "enterSelectCableState: 成功, errCode=${result?.err_code}")
                    callback?.onSuccess()
                }
                override fun onFailure(error: IDJIError) {
                    LogUtils.e(TAG, "enterSelectCableState: 失败: $error")
                    callback?.onFailure(error)
                }
            })
    }

    /**
     * 发送 ADJUST_POS 命令，启动导线巡检第一阶段（飞向起始点）。
     *
     * 前置校验：currentSelectedCableId < 0 时直接调用 onFailure 并返回，不发送命令。
     * 参数组装：
     *   - follow_dir：inspectionDirection==LEFT 时为 RIGHT_TO_LEFT，否则 LEFT_TO_RIGHT
     *   - cid：通过 convertToPhysicalPos 将 ComponentIndexType 转为 1-based 物理位置
     *   - version1（GimConfig）：携带俯仰角范围和手动控制标志
     *   - is_user_ctrl_gim：固定 false（废弃字段，与 Pilot 保持一致）
     */
    override fun enterAdjustPosState(callback: CommonCallbacks.CompletionCallback?) {
        if (currentSelectedCableId < 0) {
            val msg = "未检测到有效导线，selectedCableId=$currentSelectedCableId"
            LogUtils.w(TAG, "enterAdjustPosState: $msg")
            callback?.onFailure(DJICommonError.FACTORY.build(DJICommonError.EXECUTION_FAILED, null, msg))
            return
        }

        val cfg = inspectionConfig
        val followDir = if (cfg.direction == InspectionDirection.LEFT)
            CableInspectionDir.RIGHT_TO_LEFT else CableInspectionDir.LEFT_TO_RIGHT

        val config = CableInspectionConfigMsg().apply {
            cmd              = CableInspectionCmd.ADJUST_POS
            cable_id         = currentSelectedCableId
            distance         = cfg.flightDistance
            follow_dir       = followDir
            custom_dir       = cfg.manualInputText
            is_user_ctrl_gim = false  // 废弃字段，与 Pilot 一致固定为 false
            cid              = convertToPhysicalPos(internalSelectedCameraIndex)
            version1         = CableInspectionGimConfig().apply {
                gim_pitch_default        = cfg.pitchAngle
                gim_pitch_max            = 35.0
                gim_pitch_min            = 0.0
                user_ctrl_relative_pitch = if (cfg.allowManualPitch) 1 else 0
            }
        }

        KeyManager.getInstance().performAction(
            FlightAssistantKey.KeyCableInspection.create(),
            config,
            object : CommonCallbacks.CompletionCallbackWithParam<CableInspectionErrCodeMsg> {
                override fun onSuccess(result: CableInspectionErrCodeMsg?) {
                    LogUtils.i(TAG, "enterAdjustPosState: 成功" +
                            " cable_id=${config.cable_id} cid=${config.cid}" +
                            " dist=${config.distance} dir=${config.follow_dir}" +
                            " pitch=${config.version1?.gim_pitch_default}")
                    callback?.onSuccess()
                }
                override fun onFailure(error: IDJIError) {
                    LogUtils.e(TAG, "enterAdjustPosState: 失败: $error")
                    callback?.onFailure(error)
                }
            })
    }

    /**
     * 发送 BEGIN_FOLLOW 命令，启动导线巡检第二阶段（咬线跟随）。
     *
     * 内部自动调用 calculatePhotoInterval 获取拍照间隔（含自动降速逻辑）。
     * 参数组装：
     *   - cruise_vel：当前 inspectionSpeed（calculatePhotoInterval 可能已自动降速）
     *   - shot_interval_ms：calculatePhotoInterval 返回的最终间隔（ms）
     *   - is_cam_calibration：固定 true，由飞控侧决定是否执行相机标定
     *   - 其余参数与 enterAdjustPosState 保持一致
     */
    override fun enterBeginFollowState(callback: CommonCallbacks.CompletionCallback?) {
        val cfg = inspectionConfig
        val followDir = if (cfg.direction == InspectionDirection.LEFT)
            CableInspectionDir.RIGHT_TO_LEFT else CableInspectionDir.LEFT_TO_RIGHT

        val photoResult = calculatePhotoInterval(cfg.flightDistance, cfg.zoomLevel, cfg.inspectionSpeed)
        // 自动降速时更新 config，使命令携带降速后的速度
        if (photoResult.speedAdjusted) {
            cfg.inspectionSpeed = photoResult.adjustedSpeed
        }

        val config = CableInspectionConfigMsg().apply {
            cmd                = CableInspectionCmd.BEGIN_FOLLOW
            cable_id           = currentSelectedCableId
            distance           = cfg.flightDistance
            follow_dir         = followDir
            cruise_vel         = cfg.inspectionSpeed  // 可能已被降速更新
            shot_interval_ms   = photoResult.intervalMs
            is_cam_calibration = true
            custom_dir         = cfg.manualInputText
            is_user_ctrl_gim   = false  // 废弃字段，与 Pilot 一致固定为 false
            cid                = convertToPhysicalPos(internalSelectedCameraIndex)
            version1           = CableInspectionGimConfig().apply {
                gim_pitch_default        = cfg.pitchAngle
                gim_pitch_max            = 35.0
                gim_pitch_min            = 0.0
                user_ctrl_relative_pitch = if (cfg.allowManualPitch) 1 else 0
            }
        }
        LogUtils.i(TAG, "enterBeginFollowState: speed=${cfg.inspectionSpeed} zoom=${cfg.zoomLevel}" +
                " photoInterval=${config.shot_interval_ms}ms cable_id=$currentSelectedCableId")

        KeyManager.getInstance().performAction(
            FlightAssistantKey.KeyCableInspection.create(),
            config,
            object : CommonCallbacks.CompletionCallbackWithParam<CableInspectionErrCodeMsg> {
                override fun onSuccess(result: CableInspectionErrCodeMsg?) {
                    LogUtils.i(TAG, "enterBeginFollowState: 成功")
                    callback?.onSuccess()
                }
                override fun onFailure(error: IDJIError) {
                    LogUtils.e(TAG, "enterBeginFollowState: 失败: $error")
                    callback?.onFailure(error)
                }
            })
    }

    /**
     * 准备返回方向选择页。
     * 1. 预设 currentState 为 DIR（@Volatile，多线程可见），防止飞控 STANDBY 回推
     *    在 onFcStateChanged 中误判为异常终止
     * 2. 重置巡检方向为 NONE
     * 3. 发送 CMD_EXIT，结果通过 callback 返回
     */
    override fun prepareGoBack(callback: CommonCallbacks.CompletionCallback?) {
        LogUtils.i(TAG, "prepareGoBack: currentState=$currentState, 预设为 DIR")
        currentState = InspectionState.SELECTING_CABLE
        inspectionConfig.direction = InspectionDirection.NONE
        enterExitMissionState(callback)
    }

    // ══════════════════════════════════════════════════════════════════
    // 导线 AR 操作
    // ══════════════════════════════════════════════════════════════════

    // ══════════════════════════════════════════════════════════════════
    // AR 标注扩展：文字 + 方向箭头
    // ══════════════════════════════════════════════════════════════════

    /**
     * 上传导线 AR 标注所需的 ARPointConfig（文字 + 高亮圆点）。
     *
     * 参照 ARProjectionVM 模式：
     * 1. 先通过 FileTransferManager 将 SVG 传输到 Pilot Server
     * 2. config 实例在 build() 时即生成 UUID，直接通过实例的 .id 属性引用
     * 3. 同时设置 perspectiveConfig 和 orthographicConfig（与 ARProjectionVM 保持一致）
     * 4. 全部 config 上传成功后立即检查已缓存导线数据，主动触发 autoHighlightCable
     */
    fun uploadCableARConfigs() {
        if (arConfigsReady) {
            LogUtils.d(TAG, "uploadCableARConfigs: 已就绪，跳过重复上传")
            return
        }
        arConfigUploadCount = 0
        arConfigsReady      = false
        val cfg = arAnnotationConfig

        // 步骤 1：传输 SVG 到 Pilot Server
        Helper.pushSingleIconToRemote(AR_ARROW_LEFT_SVG, isShowToast = false)
        Helper.pushSingleIconToRemote(AR_ARROW_RIGHT_SVG, isShowToast = false)
        Helper.pushSingleIconToRemote(AR_HIGHLIGHT_DOT_SVG, isShowToast = false)
        LogUtils.i(TAG, "uploadCableARConfigs: SVG 传输已发起")

        // 步骤 2：build config 实例

        // 圆点图标的透视 + 正交配置（中心点和首尾点共用）
        val dotPerspective = ARIconPerspectiveConfig.Builder.builder()
            .withRealSize(1.5f)
            .withMinSize(20f)
            .withMaxSize(80f)
            .withStrokeWidthRatio(1f)
            .withMaxIconFileName(AR_HIGHLIGHT_DOT_SVG)
            .withMinIconFileName(AR_HIGHLIGHT_DOT_SVG)
            .build()
        val dotOrthographic = ARIconOrthographicConfig.Builder.builder()
            .withSize(40f)
            .withIconFileName(AR_HIGHLIGHT_DOT_SVG)
            .build()

        // Config 1：中心点（圆点图标 + 文字一体）
        // ARFontGravity.TOP 保证文字始终在圆点图标正上方，无需手动 altitude 偏移
        centerDotTextConfig = ARPointConfig.Builder.builder()
            .withName("CableCenterDotText")
            .withUseText(true)
            .withTextConfig(
                ARTextConfig.Builder.builder()
                    .withTextSize(cfg.textSize)
                    .withFontGravity(cfg.textGravity)
                    .withShadow(cfg.textShadow)
                    .withBackgroundColor(cfg.textBgColor)
                    .build()
            )
            .withArProjectionType(ARIconProjectionType.PERSPECTIVE)
            .withIconPerspectiveConfig(dotPerspective)
            .withIconOrthographicConfig(dotOrthographic)
            .build()

        // Config 2：首尾端圆点（仅图标，无文字）
        plainDotConfig = ARPointConfig.Builder.builder()
            .withName("CablePlainDot")
            .withUseText(false)
            .withArProjectionType(ARIconProjectionType.PERSPECTIVE)
            .withIconPerspectiveConfig(dotPerspective)
            .withIconOrthographicConfig(dotOrthographic)
            .build()

        // Config 3：向左方向箭头
        arrowLeftConfig = ARPointConfig.Builder.builder()
            .withName("CableArrowLeft")
            .withUseText(false)
            .withArProjectionType(ARIconProjectionType.PERSPECTIVE)
            .withIconPerspectiveConfig(
                ARIconPerspectiveConfig.Builder.builder()
                    .withRealSize(cfg.arrowRealSizeM)
                    .withMinSize(cfg.arrowMinPx)
                    .withMaxSize(cfg.arrowMaxPx)
                    .withStrokeWidthRatio(1f)
                    .withMaxIconFileName(AR_ARROW_LEFT_SVG)
                    .withMinIconFileName(AR_ARROW_LEFT_SVG)
                    .build()
            )
            .withIconOrthographicConfig(
                ARIconOrthographicConfig.Builder.builder()
                    .withSize(cfg.arrowMaxPx)
                    .withIconFileName(AR_ARROW_LEFT_SVG)
                    .build()
            )
            .build()

        // Config 4：向右方向箭头
        arrowRightConfig = ARPointConfig.Builder.builder()
            .withName("CableArrowRight")
            .withUseText(false)
            .withArProjectionType(ARIconProjectionType.PERSPECTIVE)
            .withIconPerspectiveConfig(
                ARIconPerspectiveConfig.Builder.builder()
                    .withRealSize(cfg.arrowRealSizeM)
                    .withMinSize(cfg.arrowMinPx)
                    .withMaxSize(cfg.arrowMaxPx)
                    .withStrokeWidthRatio(1f)
                    .withMaxIconFileName(AR_ARROW_RIGHT_SVG)
                    .withMinIconFileName(AR_ARROW_RIGHT_SVG)
                    .build()
            )
            .withIconOrthographicConfig(
                ARIconOrthographicConfig.Builder.builder()
                    .withSize(cfg.arrowMaxPx)
                    .withIconFileName(AR_ARROW_RIGHT_SVG)
                    .build()
            )
            .build()

        // 步骤 3：上传 4 个 config，全部成功后触发高亮
        uploadSingleARConfig(centerDotTextConfig!!)
        uploadSingleARConfig(plainDotConfig!!)
        uploadSingleARConfig(arrowLeftConfig!!)
        uploadSingleARConfig(arrowRightConfig!!)
    }

    /**
     * 上传单个 ARPointConfig。
     * 计数所有成功的 config，全部 4 个完成后置 arConfigsReady=true 并主动触发 autoHighlightCable。
     */
    private fun uploadSingleARConfig(config: ARPointConfig) {
        ARManager.getInstance().uploadARPointConfig(config,
            object : CommonCallbacks.CompletionCallbackWithParam<String> {
                override fun onSuccess(result: String?) {
                    synchronized(this@CableInspectionManager) {
                        arConfigUploadCount++
                        if (arConfigUploadCount >= 4) {
                            arConfigsReady = true
                            LogUtils.i(TAG, "uploadCableARConfigs: 4 个 config 全部就绪")
                        }
                    }
                    LogUtils.i(TAG, "uploadARPointConfig[${config.name}]: 成功 id=$result")

                    // 全部 config ready 后主动检查已缓存的导线数据并触发高亮
                    if (arConfigsReady) {
                        val cables  = _currentCableArInfoList
                        val nearest = currentSelectedCableId
                        if (nearest >= 0 && cables.isNotEmpty()) {
                            val cable = cables.firstOrNull { it.cable_id == nearest }
                            if (cable != null) {
                                LogUtils.i(TAG, "uploadARPointConfig: 发现已缓存导线 id=$nearest，立即触发 autoHighlightCable")
                                autoHighlightCable(cable)
                            }
                        }
                    }
                }
                override fun onFailure(error: IDJIError) {
                    LogUtils.e(TAG, "uploadARPointConfig[${config.name}]: 失败: $error")
                }
            })
    }

    /**
     * 自动高亮选中导线并在中点显示 AR 文字标注（Phase 1，由 onCableArInfoReceived 内部调用）。
     *
     * 流程：
     * 1. selectCableDetectionLine(id, NONE) → 高亮导线本身（不带方向箭头样式）
     * 2. 计算导线几何中点（LLA 空间直接插值）
     * 3. 如果 textPointInfo 已存在 → updateARPointInfo 更新位置/文字/颜色
     *    否则 → uploadARPointInfo 创建新实例（configId 直接引用 textConfig.id）
     * 4. 记录 lastAutoHighlightId
     *
     * 坐标策略（零转换）：
     *   latitude / longitude / altitude 全部直接取自 CableArInfo 端点数据，
     *   altitude 仅做 mm → m 单位转换，不做任何坐标系变换。
     *   Pilot 原生 AR 渲染引擎能用同一组坐标正确绘制导线，
     *   用相同坐标放置的 AR 文字自然与导线对齐。
     */
    private fun autoHighlightCable(cable: CableArInfo) {
        if (!arConfigsReady || centerDotTextConfig == null || plainDotConfig == null) {
            LogUtils.w(TAG, "autoHighlightCable: AR configs 未就绪，跳过")
            return
        }

        // 2. 计算 3 个点的位置（导线原始坐标，altitude mm→m 减 takeoffLocationAltitude）
        val startAlt = cable.start.altitude / 1000.0 - takeoffLocationAltitude
        val endAlt   = cable.end.altitude   / 1000.0 - takeoffLocationAltitude
        val midLat   = (cable.start.latitude  + cable.end.latitude)  / 2.0
        val midLon   = (cable.start.longitude + cable.end.longitude) / 2.0
        val midAlt   = (startAlt + endAlt) / 2.0

        val startLoc  = OpenLocation(cable.start.latitude, cable.start.longitude, startAlt)
        val centerLoc = OpenLocation(midLat, midLon, midAlt)
        val endLoc    = OpenLocation(cable.end.latitude,   cable.end.longitude,   endAlt)

        // 3. 中心点（圆点图标 + 文字一体，ARFontGravity.TOP 保证文字在图标正上方）
        val existingCenter = centerPointInfo
        if (existingCenter != null) {
            existingCenter.location  = centerLoc
            existingCenter.text      = arAnnotationConfig.highlightText
            existingCenter.textColor = arAnnotationConfig.textColor
            existingCenter.iconColor = arAnnotationConfig.textColor
            updateARPoint(existingCenter)
        } else {
            val info = ARPointInfo.Builder.builder()
                .withName("CableCenterLabel")
                .withConfigId(centerDotTextConfig!!.id)
                .withVisible(true)
                .withIconVisible(true)       // 显示圆点图标
                .withTextVisible(true)       // 显示文字（在图标正上方）
                .withShowEdgeGuide(false)
                .withText(arAnnotationConfig.highlightText)
                .withTextColor(arAnnotationConfig.textColor)
                .withIconColor(arAnnotationConfig.textColor)
                .withLocation(centerLoc)
                .build()
            ARManager.getInstance().uploadARPointInfo(info,
                object : CommonCallbacks.CompletionCallbackWithParam<String> {
                    override fun onSuccess(result: String?) {
                        centerPointInfo = info
                        LogUtils.i(TAG, "autoHighlight: uploadCenterPoint 成功 cableId=${cable.cable_id}")
                    }
                    override fun onFailure(error: IDJIError) {
                        LogUtils.e(TAG, "autoHighlight: uploadCenterPoint 失败: $error")
                    }
                })
        }

        // 4. 首端 + 尾端圆点（仅图标，无文字）
        highlightDotStart = updateOrCreateDot(highlightDotStart, "CableDotStart", startLoc)
        highlightDotEnd   = updateOrCreateDot(highlightDotEnd,   "CableDotEnd",   endLoc)

        lastAutoHighlightId = cable.cable_id
    }

    /** 更新或创建一个纯圆点 ARPointInfo（无文字）。已存在则 update 位置/颜色，否则 upload 新建。 */
    private fun updateOrCreateDot(existing: ARPointInfo?, name: String, location: OpenLocation): ARPointInfo {
        if (existing != null) {
            existing.location  = location
            existing.iconColor = arAnnotationConfig.textColor
            updateARPoint(existing)
            return existing
        }
        val info = ARPointInfo.Builder.builder()
            .withName(name)
            .withConfigId(plainDotConfig!!.id)
            .withVisible(true)
            .withIconVisible(true)
            .withTextVisible(false)
            .withShowEdgeGuide(false)
            .withText("")
            .withIconColor(arAnnotationConfig.textColor)
            .withLocation(location)
            .build()
        ARManager.getInstance().uploadARPointInfo(info,
            object : CommonCallbacks.CompletionCallbackWithParam<String> {
                override fun onSuccess(result: String?) {
                    LogUtils.i(TAG, "uploadDot[$name]: 成功")
                }
                override fun onFailure(error: IDJIError) {
                    LogUtils.e(TAG, "uploadDot[$name]: 失败: $error")
                }
            })
        return info
    }

    /**
     * 计算飞机在导线段上的水平投影参数 t ∈ [0.05, 0.95]。
     *
     * 将飞机位置和导线端点转换到 NED 坐标系，仅用水平分量（North/East）做投影。
     * 结果 t 用于在导线 LLA 上插值，得到飞机正对着的导线位置。
     * 高度参数不参与计算（只用水平分量），所以对 altitude 坐标系不敏感。
     */
    private fun projectDroneOnCable(cable: CableArInfo): Double {
        val droneLLA = DoubleVector3(droneLatitude, droneLongitude, droneAltitude)
        val startLLA = DoubleVector3(
            cable.start.latitude, cable.start.longitude,
            cable.start.altitude / 1000.0 - takeOffAltitude
        )
        val endLLA = DoubleVector3(
            cable.end.latitude, cable.end.longitude,
            cable.end.altitude / 1000.0 - takeOffAltitude
        )
        val startNED = LocationUtil.lla2ned(startLLA, droneLLA)
        val endNED   = LocationUtil.lla2ned(endLLA,   droneLLA)

        val dx = endNED.x - startNED.x
        val dy = endNED.y - startNED.y
        val dot_dd = dx * dx + dy * dy
        return if (dot_dd < 1e-9) 0.5 else {
            (-startNED.x * dx + -startNED.y * dy) / dot_dd
        }.coerceIn(0.05, 0.95)
    }

    /**
     * 在选中导线上显示方向箭头。
     * 箭头位置基于飞机在导线段上的投影点，偏移 7% 后靠近图传视觉中心。
     * direction: LEFT 表示向左（箭头偏向导线起点），RIGHT 表示向右（偏向终点）。
     */
    fun showDirectionArrow(direction: InspectionDirection) {
        if (!arConfigsReady) {
            LogUtils.w(TAG, "showDirectionArrow: AR configs 未就绪")
            return
        }
        if (currentSelectedCableId < 0) {
            LogUtils.w(TAG, "showDirectionArrow: 无选中导线")
            return
        }
        val cable = _currentCableArInfoList.firstOrNull { it.cable_id == currentSelectedCableId }
        if (cable == null) {
            LogUtils.w(TAG, "showDirectionArrow: 未找到导线 id=$currentSelectedCableId")
            return
        }

        val configId = when (direction) {
            InspectionDirection.LEFT  -> arrowLeftConfig?.id
            InspectionDirection.RIGHT -> arrowRightConfig?.id
            else -> null
        } ?: run {
            LogUtils.w(TAG, "showDirectionArrow: configId 为空，direction=$direction")
            return
        }

        // 基于投影点偏移：left 往起点方向，right 往终点方向
        val tCenter = projectDroneOnCable(cable)
        val tOffset = 0.07
        val tArrow = when (direction) {
            InspectionDirection.LEFT  -> (tCenter - tOffset).coerceIn(0.02, 0.98)
            InspectionDirection.RIGHT -> (tCenter + tOffset).coerceIn(0.02, 0.98)
            else -> tCenter
        }
        val arrowLat = cable.start.latitude  + tArrow * (cable.end.latitude  - cable.start.latitude)
        val arrowLon = cable.start.longitude + tArrow * (cable.end.longitude - cable.start.longitude)
        val arrowAbsAlt = (cable.start.altitude + tArrow * (cable.end.altitude - cable.start.altitude)) / 1000.0
        val arrowAlt = arrowAbsAlt - takeoffLocationAltitude
        val arrowLoc = OpenLocation(arrowLat, arrowLon, arrowAlt)

        // 先删除旧箭头（configId 可能不同）
        arrowPointInfo?.let { old ->
            ARManager.getInstance().deleteARPointInfo(old,
                object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() { LogUtils.d(TAG, "showDirectionArrow: 删除旧箭头成功") }
                    override fun onFailure(e: IDJIError) { LogUtils.e(TAG, "showDirectionArrow: 删除旧箭头失败: $e") }
                })
            arrowPointInfo = null
        }

        // 上传新箭头
        val info = ARPointInfo.Builder.builder()
            .withName("CableDirectionArrow")
            .withConfigId(configId)
            .withVisible(true)
            .withIconVisible(true)
            .withTextVisible(false)
            .withShowEdgeGuide(false)
            .withText("")
            .withIconColor(arAnnotationConfig.textColor)
            .withLocation(arrowLoc)
            .build()
        ARManager.getInstance().uploadARPointInfo(info,
            object : CommonCallbacks.CompletionCallbackWithParam<String> {
                override fun onSuccess(result: String?) {
                    arrowPointInfo = info
                    LogUtils.i(TAG, "showDirectionArrow: 上传成功 direction=$direction")
                }
                override fun onFailure(error: IDJIError) {
                    LogUtils.e(TAG, "showDirectionArrow: 上传失败: $error")
                }
            })
    }

    /**
     * 刷新已显示的 AR 标注样式（文字内容或颜色变化后调用，实时生效）。
     * 仅更新已存在的 ARPointInfo；未显示时无操作，调用安全。
     */
    fun refreshCableARAnnotations() {
        // 中心点（圆点 + 文字一体）
        centerPointInfo?.let {
            it.text      = arAnnotationConfig.highlightText
            it.textColor = arAnnotationConfig.textColor
            it.iconColor = arAnnotationConfig.textColor
            updateARPoint(it)
        }
        // 首尾端圆点
        listOf(highlightDotStart, highlightDotEnd).forEach { dot ->
            dot?.let {
                it.iconColor = arAnnotationConfig.textColor
                updateARPoint(it)
            }
        }
        // 方向箭头
        arrowPointInfo?.let {
            it.iconColor = arAnnotationConfig.textColor
            updateARPoint(it)
        }
        LogUtils.d(TAG, "refreshARAnnotations: color=#${Integer.toHexString(arAnnotationConfig.textColor)}")
    }

    /**
     * 清除导线上的所有 AR 标注（文字 + 方向箭头）。
     * 已上传的 ARPointInfo 通过 deleteARPointInfo 删除，引用置 null；
     * 不影响 selectCableDetectionLine 的导线高亮状态。
     */
    fun clearCableARAnnotations() {
        // 查询 Pilot Server 端当前所有 ARPointInfo，按 name 前缀过滤删除本模块创建的
        // 解决：本地引用丢失（destroy/进程重启）后无法删除 Server 端残留的问题
        ARManager.getInstance().queryARPointInfo(
            object : CommonCallbacks.CompletionCallbackWithParam<List<ARPointInfo>> {
                override fun onSuccess(infos: List<ARPointInfo>?) {
                    val ours = infos?.filter { it.name?.startsWith("Cable") == true } ?: emptyList()
                    ours.forEach { deleteARPoint(it) }
                    LogUtils.i(TAG, "clearCableARAnnotations: 查到 ${infos?.size ?: 0} 个，删除 ${ours.size} 个")
                }
                override fun onFailure(error: IDJIError) {
                    LogUtils.e(TAG, "clearCableARAnnotations: queryARPointInfo 失败: $error")
                }
            })
        // 清空本地引用
        centerPointInfo     = null
        highlightDotStart   = null
        highlightDotEnd     = null
        arrowPointInfo      = null
        lastAutoHighlightId = -1
        // 重置 config 状态，使下次进入 showDirectionPage 可重新上传
        arConfigsReady      = false
        arConfigUploadCount = 0
        LogUtils.d(TAG, "clearCableARAnnotations: 本地引用已清空，arConfigsReady 已重置")
    }

    /** 通过 ARManager IPC 更新已有 ARPointInfo */
    private fun updateARPoint(info: ARPointInfo) {
        ARManager.getInstance().updateARPointInfo(info,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() { LogUtils.d(TAG, "updateARPoint[${info.name}]: 成功") }
                override fun onFailure(error: IDJIError) { LogUtils.e(TAG, "updateARPoint[${info.name}]: 失败: $error") }
            })
    }

    /** 通过 ARManager IPC 删除 ARPointInfo */
    private fun deleteARPoint(info: ARPointInfo) {
        ARManager.getInstance().deleteARPointInfo(info,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() { LogUtils.d(TAG, "deleteARPoint[${info.name}]: 成功") }
                override fun onFailure(error: IDJIError) { LogUtils.e(TAG, "deleteARPoint[${info.name}]: 失败: $error") }
            })
    }

    // ══════════════════════════════════════════════════════════════════
    // 计算方法
    // ══════════════════════════════════════════════════════════════════

    /**
     * 动态计算推荐变焦倍率。
     * 公式（参照 Pilot CableInspectCamPanel.initBaseZoom）：
     *   targetFocus = sensorHeight(4.8mm) × flightDistance / 2 × 1.03
     *   targetZoom  = targetFocus / baseFocalLength1x(5.11mm)
     * 目标：使导线在画面中充满约半帧高度，1.03 为经验修正系数。
     * 传感器参数：H30/H30T Zoom 镜头（1/2" CMOS），宽 6.4mm × 高 4.8mm。
     * 结果 clamp 到 zoomRange 范围，flightDistance < 4.0m 时直接返回默认值 5.0。
     */
    override fun calculateDefaultZoom(flightDistance: Double): Double {
        val sensorHeight = 4.8  // H30/H30T Zoom 镜头传感器高度（mm，1/2" CMOS）
        if (flightDistance < 4.0) {
            LogUtils.w(TAG, "calculateDefaultZoom: distance 过小 (${flightDistance}m)，使用默认值 5.0")
            return 5.0
        }
        val targetFocus = sensorHeight * flightDistance / 2.0 * 1.03
        val targetZoom  = targetFocus / baseFocalLength1x

        val min = _zoomRange?.firstOrNull()?.toDouble() ?: 1.0
        val max = _zoomRange?.lastOrNull()?.toDouble()  ?: 200.0
        val result = targetZoom.coerceIn(min, max)
        LogUtils.i(TAG, "calculateDefaultZoom: baseFocal=${baseFocalLength1x}mm" +
                " dist=${flightDistance}m targetZoom=$targetZoom → clamped=$result")
        return result
    }

    /**
     * 计算拍照间隔（ms），在必要时自动降低 inspectionSpeed。
     * 公式（参照 Pilot CableInspectCamPanel.calcPhotoInterval）：
     *   targetFocal = baseFocalLength1x(5.11mm) × zoomLevel
     *   interval    = 2 × dist × (sensorWidth/2 / targetFocal) × (1 - overlap) / speed × 1000
     *   sensorWidth = 6.4mm，overlap = 0.15（15% 航向重叠率，与 Pilot 固定值一致）
     *
     * 自动降速逻辑（参照 Pilot adjustSpeed）：
     *   interval < 200ms 时，反推最大可用速度：maxSpeed = ... × 1000 / 200
     *   adjustedSpeed = floor(maxSpeed).coerceIn(1.0, 10.0)
     *   用 adjustedSpeed 重算最终 interval，确保 interval >= 200ms
     *   同时修改 inspectionConfig.inspectionSpeed，调用方通过 PhotoIntervalResult.speedAdjusted 感知
     */
    override fun calculatePhotoInterval(flightDistance: Double, zoomLevel: Double, inspectionSpeed: Double): PhotoIntervalResult {
        val sensorWidth  = 6.4   // H30/H30T Zoom 镜头传感器宽度（mm）
        val overlapRatio = 0.15  // 航向重叠率，与 Pilot overlayRatio 一致（固定 15%）
        val minInterval  = 200   // ms，最小拍照间隔，与 Pilot minPhotoInterval 一致

        val targetFocalMm = baseFocalLength1x * zoomLevel
        val intervalMs = 2.0 * flightDistance * (sensorWidth / 2.0 / targetFocalMm) *
                (1 - overlapRatio) / inspectionSpeed * 1000.0

        // interval < 200ms：计算建议降速值，由调用方决定是否更新
        if (intervalMs < minInterval) {
            val maxSpeed = 2.0 * flightDistance * (sensorWidth / 2.0 / targetFocalMm) *
                    (1 - overlapRatio) * 1000.0 / minInterval
            val adjustedSpeed = kotlin.math.floor(maxSpeed).coerceIn(1.0, 10.0)
            LogUtils.w(TAG, "calculatePhotoInterval: interval=${intervalMs.toInt()}ms < ${minInterval}ms," +
                    " 建议降速 $inspectionSpeed → $adjustedSpeed")
            val finalMs = 2.0 * flightDistance * (sensorWidth / 2.0 / targetFocalMm) *
                    (1 - overlapRatio) / adjustedSpeed * 1000.0
            val result = finalMs.toInt().coerceAtLeast(minInterval)
            LogUtils.i(TAG, "calculatePhotoInterval(adjusted):" +
                    " zoom=$zoomLevel focal=${String.format("%.1f", targetFocalMm)}mm" +
                    " dist=$flightDistance speed=$adjustedSpeed → ${result}ms")
            return PhotoIntervalResult(result, true, adjustedSpeed)
        }

        val result = intervalMs.toInt().coerceAtLeast(minInterval)
        LogUtils.i(TAG, "calculatePhotoInterval:" +
                " zoom=$zoomLevel focal=${String.format("%.1f", targetFocalMm)}mm" +
                " dist=$flightDistance speed=$inspectionSpeed → ${result}ms")
        return PhotoIntervalResult(result, false, inspectionSpeed)
    }

    /**
     * 从导线列表中找到距飞机最近的导线 ID。
     * 坐标转换链路（参照 Pilot CableInspectViewModel）：
     *   导线端点 LLA → ECEF → NED（以飞机当前位置为 NED 原点）
     * 高度参考系统一：
     *   导线端点 altitude 单位 mm，转 m 后减去 takeOffAltitude（WGS84 椭球高）
     *   与飞机气压计相对高度统一参考系（参照 Pilot toInspectModel）
     * 距离计算：
     *   仅用 NED 的水平分量（North/East，忽略 Down），参照 Pilot 选线阶段 z=0 处理
     *   调用 PolygonUtils.point2SegmentDist 计算飞机到导线线段的最短距离
     * 过滤：超过 MAX_CABLE_DIST(40m) 的导线忽略
     * 兜底：飞机位置未就绪（lat==0 && lon==0）时返回第一条导线的 cable_id
     */
    private fun findNearestCableId(cablesOverride: List<CableArInfo>? = null): Int {
        val cables = cablesOverride ?: _currentCableArInfoList
        if (cables.isEmpty()) {
            LogUtils.w(TAG, "findNearestCableId: cables 为空")
            return -1
        }
        // 飞机位置未就绪（初始值 0,0）时无法计算有效距离，兜底返回第一条
        if (droneLatitude == 0.0 && droneLongitude == 0.0) {
            LogUtils.w(TAG, "findNearestCableId: 飞机位置未就绪 (0,0,0)，兜底返回第一条 id=${cables.first().cable_id}")
            return cables.first().cable_id
        }

        val droneLLA = DoubleVector3(droneLatitude, droneLongitude, droneAltitude)
        var minDist   = Double.MAX_VALUE
        var nearestId = -1

        for (cable in cables) {
            // altitude 单位 mm → m；绝对高度减去起飞点椭球高度，与飞机气压计高度统一参考系
            val startLLA = DoubleVector3(
                cable.start.latitude,
                cable.start.longitude,
                cable.start.altitude / 1000.0 - takeOffAltitude
            )
            val endLLA = DoubleVector3(
                cable.end.latitude,
                cable.end.longitude,
                cable.end.altitude / 1000.0 - takeOffAltitude
            )
            // LLA → NED，以飞机当前位置为坐标系原点（完整 WGS84 转换）
            val startNED = LocationUtil.lla2ned(startLLA, droneLLA)
            val endNED   = LocationUtil.lla2ned(endLLA,   droneLLA)

            // 选线阶段忽略垂直分量（Down），只用水平距离判断远近
            val startNED2D = DoubleVector3(startNED.x, startNED.y, 0.0)
            val endNED2D   = DoubleVector3(endNED.x,   endNED.y,   0.0)
            val dist = PolygonUtils.point2SegmentDist(startNED2D, endNED2D)

            LogUtils.d(TAG, "findNearestCableId: cable_id=${cable.cable_id} dist=%.2fm".format(dist))

            if (dist < minDist && dist <= MAX_CABLE_DIST) {
                minDist   = dist
                nearestId = cable.cable_id
            }
        }

        LogUtils.i(TAG, "findNearestCableId: nearestId=$nearestId minDist=%.2fm".format(minDist))
        return nearestId
    }

    // ══════════════════════════════════════════════════════════════════
    // 工具方法
    // ══════════════════════════════════════════════════════════════════

    /**
     * 判断相机型号是否支持导线巡检。
     * 使用 camTypePattern 正则（ZENMUSE_H30|ZENMUSE_H30T）进行全字符串匹配，忽略大小写。
     * camType: 相机型号字符串，来自 CameraKey.KeyCameraType 的 toString()
     * 返回 true 表示支持
     */
    private fun isCamSupport(camType: String): Boolean {
        if (camTypePattern.isNotEmpty()) {
            return camTypePattern.toRegex(RegexOption.IGNORE_CASE).matches(camType)
        }
        return false
    }

    /**
     * 将 ComponentIndexType 转换为 1-based 物理位置编号，用于 CableInspectionConfigMsg.cid。
     * 两套协议的映射规则（参照 Pilot PhysicalPosConverter.convertPhysicalPos）：
     *   传统协议（value 0-12）：value + 1
     *     LEFT_OR_MAIN(0)→1，RIGHT(1)→2，UP(2)→3
     *   V3 新协议（value 20001-20008）：value - 20000
     *     PORT_1(20001)→1，PORT_2(20002)→2，...
     * index: 相机位置枚举，null 时默认返回 1（第一个相机）
     */
    private fun convertToPhysicalPos(index: ComponentIndexType?): Int {
        if (index == null) return 1
        val v = index.value()
        return when {
            v in 20001..20008 -> v - 20000  // V3 新协议
            v in 0..12        -> v + 1      // 传统协议
            else              -> 1          // 未知类型，默认第一个
        }
    }

    /**
     * 将 VideoSourceEntity 的 Position 正规化为对应的 ComponentIndexType。
     * 与 LocalCameraStreamManager.updateSource 逻辑保持一致，处理不同 MajorType 的 index 映射。
     * 仅供内部 KeyAvailableVideoSource 回调使用，不对外暴露。
     */
    private fun updateSourceInternal(source: VideoSourceEntity) {
        if (source.getMajorType() == SourceMajorType.FLIGHT_ASSISTANT) {
            source.setPosition(ComponentIndexType.VISION_ASSIST)
        } else if (source.getMajorType() == SourceMajorType.CAMERA) {
            source.setPosition(converterIndexVideoToNormal(source.getPosition()))
        } else if (source.getMajorType() == SourceMajorType.AI_BOX) {
            source.setPosition(converterIndexVideoToNormal(source.getPosition()))
        } else if (source.getMajorType() == SourceMajorType.PSDK) {
            source.setPosition(converterIndexVideoToNormal(source.getPosition()))
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // 内部：导线 AR 数据处理
    // ══════════════════════════════════════════════════════════════════

    /**
     * 收到导线数据后的统一处理入口（由 KeyCableArInfo listen 回调触发）。
     * 处理流程：
     * 1. 更新 currentCableArInfoList
     * 2. 调用 findNearestCableId 计算最近导线
     * 3. 更新 currentSelectedCableId
     * 4. Phase 1：若 arConfigsReady 且最近导线 ID 发生变化，自动高亮并显示 AR 文字
     *    注意：不限制 currentState==DIR，因为 Manager 的 currentState 与 VM 的
     *    inspectionState 存在时序差（FC 状态回推有延迟），去掉限制让 arConfigsReady
     *    作为唯一门控，避免漏触发。
     * 5. 通知所有 CableArInfoListener
     */
    private fun onCableArInfoReceived(cables: List<CableArInfo>) {
        _currentCableArInfoList = cables
        val nearestId = findNearestCableId(cables)
        currentSelectedCableId = nearestId

        // AR 投射：arConfigsReady 后持续更新位置（飞机在移动，投影点在变）
        // 进入 showDirectionPage 后开始显示，后续流程中持续保留直到结束/异常
        if (nearestId >= 0 && arConfigsReady) {
            val cable = cables.firstOrNull { it.cable_id == nearestId }
            if (cable != null) {
                autoHighlightCable(cable)
            }
        }

        notifyCableArInfo(cables, nearestId)
    }

    // ══════════════════════════════════════════════════════════════════
    // 错误码映射
    // ══════════════════════════════════════════════════════════════════

    /**
     * 将 CableInspectionErrCode 映射为中文描述字符串。
     * OK 和 USER_REQ_EXIT 属于正常退出，返回 null，不触发 ErrorListener。
     * 其余错误码返回对应的中文描述，可直接用于 Toast 展示。
     * 参照 Pilot CableInspectHMS.onReceiveError。
     */
    private fun mapErrCode(errCode: CableInspectionErrCode): String? {
        if (errCode == CableInspectionErrCode.OK ||
            errCode == CableInspectionErrCode.USER_REQ_EXIT) return null

        return when (errCode) {
            CableInspectionErrCode.USER_SWITCH_MODE       -> "用户切换飞行档位"
            CableInspectionErrCode.MISSION_ERR            -> "被其他任务打断"
            CableInspectionErrCode.LIDAR_INFO_UPDATE_FAIL -> "激光雷达数据获取失败"
            CableInspectionErrCode.CABLE_LOST             -> "电线跟丢"
            CableInspectionErrCode.CAM_CONFIG_FAIL        -> "相机配置失败"
            CableInspectionErrCode.ADJUST_POS_FAIL        -> "调整位置失败"
            CableInspectionErrCode.HEIGHT_LIMIT           -> "触发限高"
            CableInspectionErrCode.RADIUS_LIMIT           -> "触发限远"
            CableInspectionErrCode.LOW_LIMIT              -> "触发限低"
            CableInspectionErrCode.BOUNDARY_LIMIT         -> "触及禁飞区边界"
            CableInspectionErrCode.VFENCE_LIMIT           -> "触及自定义飞行区边界"
            CableInspectionErrCode.DARK                   -> "环境光线过暗"
            CableInspectionErrCode.RC_LOST                -> "遥控器信号丢失"
            CableInspectionErrCode.ATTI_MODE              -> "切入姿态模式"
            CableInspectionErrCode.TRAJ_EMERGENCY_BRAKE   -> "避障紧急刹停"
            CableInspectionErrCode.TRAJ_PLAN_FAIL         -> "轨迹规划失败"
            CableInspectionErrCode.PARAM_ERR              -> "参数错误"
            CableInspectionErrCode.CALIBRATION_FAIL       -> "相机标定失败"
            else                                          -> "异常退出 ($errCode)"
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // Listener 通知
    // ══════════════════════════════════════════════════════════════════

    // errCode 为 OK 时置 null，避免上层将正常转换误判为错误
    private fun notifyStateChanged(old: InspectionState, new: InspectionState, errCode: CableInspectionErrCode) {
        val errCodeOrNull = if (errCode == CableInspectionErrCode.OK) null else errCode
        listeners.forEach { it.onStateChanged(old, new, errCodeOrNull) }
    }

    // 通知所有 Listener 导线数据更新
    private fun notifyCableArInfo(cables: List<CableArInfo>, nearestId: Int) {
        listeners.forEach { it.onCableArInfoUpdated(cables, nearestId) }
    }

    // 通知所有 Listener 支持状态变化
    private fun notifySupportStatus(detail: SupportDetail) {
        listeners.forEach { it.onSupportStatusChanged(detail) }
    }

    // 通知所有 Listener 错误事件
    private fun notifyError(errCode: CableInspectionErrCode, message: String) {
        LogUtils.w(TAG, "notifyError: $errCode → $message")
        listeners.forEach { it.onError(errCode, message) }
    }

    // ══════════════════════════════════════════════════════════════════
    // Listener 注册 / 注销
    // ══════════════════════════════════════════════════════════════════

    override fun addCableInspectionListener(listener: ICableInspectionManager.CableInspectionListener) {
        listeners.add(listener)
    }

    override fun removeCableInspectionListener(listener: ICableInspectionManager.CableInspectionListener) {
        listeners.remove(listener)
    }
}
