package com.dji.open.pilot.plugin.common.data

import android.util.Log
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.RtkMobileStationKey
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.sdk.keyvalue.value.common.LocationCoordinate3D
import dji.v5.et.create
import dji.v5.et.get
import dji.v5.et.listen
import dji.v5.manager.KeyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全局共享数据管理。
 *
 * 集中管理各演示模块共用的实时数据（飞机位置、遥控器数据、国家码等）。
 * 在 ServerState.CONNECTED 时启动监听，DISCONNECTED 时停止。
 * 各模块通过该类读取数据，不直接订阅 Key。
 *
 * 扩展规则：后续需要新增共享数据时，在本类中按区域追加 StateFlow 字段 + listen/cancel 逻辑。
 */
object SharedDataManager {

    private const val TAG = "SharedDataManager"

    // ── 飞机数据 ──────────────────────────────────────────────────────────────

    private val _aircraftLocation = MutableStateFlow<LocationCoordinate3D?>(null)

    /** 飞机 3D 位置（经纬度 + 高度）。null 表示尚未收到数据。 */
    val aircraftLocation: StateFlow<LocationCoordinate3D?> = _aircraftLocation.asStateFlow()

    // ── 航向角 ────────────────────────────────────────────────────────────────

    private val _compassHeading = MutableStateFlow<Double?>(null)

    /** 飞机航向角（度，0=正北，顺时针）。null 表示尚未收到数据。 */
    val compassHeading: StateFlow<Double?> = _compassHeading.asStateFlow()

    // ── 起飞点 RTK 椭球高度 ───────────────────────────────────────────────────

    private val _rtkTakeoffAltitude = MutableStateFlow<Double?>(null)

    /** 起飞点 WGS84 椭球高度（米），由 RTK 提供。null 表示尚未收到数据。 */
    val rtkTakeoffAltitude: StateFlow<Double?> = _rtkTakeoffAltitude.asStateFlow()

    // ── Home 点经纬度 ─────────────────────────────────────────────────────────

    private val _homeLocation = MutableStateFlow<LocationCoordinate2D?>(null)

    /** Home 点（起飞点）经纬度。null 表示尚未收到数据。 */
    val homeLocation: StateFlow<LocationCoordinate2D?> = _homeLocation.asStateFlow()

    // ── 后续可扩展区域 ────────────────────────────────────────────────────────
    // val rcBatteryPercent: StateFlow<Int?>
    // val areaCode: StateFlow<String?>
    // val pilotLanguage: StateFlow<RemoteLanguage?>

    // ── Key 订阅生命周期 holder ───────────────────────────────────────────────

    private val listenerHolder = Any()

    /**
     * 启动数据监听。在 ServerState.CONNECTED 时调用。
     */
    fun startListening() {
        Log.i(TAG, "startListening")
        FlightControllerKey.KeyAircraftLocation3D.create().listen(listenerHolder) { loc ->
            _aircraftLocation.value = loc
        }
        FlightControllerKey.KeyCompassHeading.create().listen(listenerHolder) { heading ->
            _compassHeading.value = heading
        }
        RtkMobileStationKey.KeyRTKTakeoffAltitudeInfo.create().listen(listenerHolder) { info ->
            _rtkTakeoffAltitude.value = info?.altitude
        }
        FlightControllerKey.KeyHomeLocation.create().listen(listenerHolder) { loc ->
            _homeLocation.value = loc
        }
    }

    /**
     * 停止数据监听并重置数据。在断连或销毁时调用。
     */
    fun stopListening() {
        Log.i(TAG, "stopListening")
        KeyManager.getInstance().cancelListen(listenerHolder)
        _aircraftLocation.value = null
        _compassHeading.value = null
        _rtkTakeoffAltitude.value = null
        _homeLocation.value = null
    }

    /**
     * 同步获取飞机当前位置（一次性读取）。
     * 优先返回已缓存的 StateFlow 值；若为 null 则尝试直接 get。
     */
    fun getAircraftLocationOnce(): LocationCoordinate3D? {
        return _aircraftLocation.value
            ?: FlightControllerKey.KeyAircraftLocation3D.create().get()
    }

    /**
     * 同步获取航向角（一次性读取）。
     * 优先返回已缓存值；若为 null 则尝试直接 get。
     */
    fun getCompassHeadingOnce(): Double? {
        return _compassHeading.value
            ?: FlightControllerKey.KeyCompassHeading.create().get()
    }

    /**
     * 同步获取起飞点 RTK 椭球高度（一次性读取）。
     * 优先返回已缓存值；若为 null 则尝试直接 get。取不到返回 0.0。
     */
    fun getRtkTakeoffAltitudeOnce(): Double {
        return _rtkTakeoffAltitude.value
            ?: RtkMobileStationKey.KeyRTKTakeoffAltitudeInfo.create().get()?.altitude
            ?: 0.0
    }

    /**
     * 同步获取 Home 点经纬度（一次性读取）。
     * 优先返回已缓存值；若为 null 则尝试直接 get。
     */
    fun getHomeLocationOnce(): LocationCoordinate2D? {
        return _homeLocation.value
            ?: FlightControllerKey.KeyHomeLocation.create().get()
    }
}
