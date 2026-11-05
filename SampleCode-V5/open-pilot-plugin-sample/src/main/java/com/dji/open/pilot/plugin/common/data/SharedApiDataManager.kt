package com.dji.open.pilot.plugin.common.data

import android.util.Log
import corp.areacode.AreaCodeChangedCallback
import dji.open.pilot.client.manager.areacode.RemoteAreaCodeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 接口型（AIDL manager）共享数据管理。
 *
 * 与 [SharedDataManager]（Key 型数据）并列：SharedDataManager 负责通过 KeyManager 订阅的实时数据，
 * 本类负责通过 open-pilot 接口（如 RemoteAreaCodeManager）订阅的数据。
 * 在 ServerState.CONNECTED 时启动监听，断连或销毁时停止。
 * 各模块通过本类读取数据，不直接调用接口订阅。
 *
 * 扩展规则：后续需要新增接口型共享数据时，在本类中按区域追加 StateFlow 字段 + 注册/反注册逻辑。
 */
object SharedApiDataManager {

    private const val TAG = "SharedApiDataManager"

    /** 国家码查询/订阅使用的默认 product / device 标识 */
    private const val DEFAULT_PRODUCT_ID = 0
    private const val DEFAULT_DEVICE_ID = 0

    // ── 国家码（飞行区域码） ────────────────────────────────────────────────────

    private val _countryCode = MutableStateFlow<String?>(null)

    /** 当前国家码。null 表示尚未获取到。 */
    val countryCode: StateFlow<String?> = _countryCode.asStateFlow()

    /**
     * 国家码变更回调。供不便使用协程收集 StateFlow 的调用方（如 ViewModel）注册，
     * 每次国家码更新（含初值、变更、清空）时回调。
     */
    var onCountryCodeChanged: ((String?) -> Unit)? = null

    /** addAreaCodeObserver 返回的监听 ID，0 表示未订阅 */
    private var areaCodeListenId = 0

    private val areaCodeChangedCallback =
        AreaCodeChangedCallback { _, _, areaCode, _, _ ->
            updateCountryCode(areaCode)
        }

    private fun updateCountryCode(code: String?) {
        _countryCode.value = code
        onCountryCodeChanged?.invoke(code)
    }

    /**
     * 启动接口型数据监听。在 ServerState.CONNECTED 时调用。
     */
    fun startListening() {
        Log.i(TAG, "startListening")
        val manager = RemoteAreaCodeManager.getInstance()
        // 先查询一次当前值
        updateCountryCode(manager.getAreaCode(DEFAULT_PRODUCT_ID, DEFAULT_DEVICE_ID)?.areacode)
        // 避免重复订阅
        if (areaCodeListenId == 0) {
            areaCodeListenId = manager.addAreaCodeObserver(
                DEFAULT_PRODUCT_ID, DEFAULT_DEVICE_ID, areaCodeChangedCallback
            )
        }
    }

    /**
     * 停止接口型数据监听并重置数据。在断连或销毁时调用。
     */
    fun stopListening() {
        Log.i(TAG, "stopListening")
        if (areaCodeListenId != 0) {
            RemoteAreaCodeManager.getInstance().removeAreaCodeObserver(areaCodeListenId)
            areaCodeListenId = 0
        }
        updateCountryCode(null)
    }

    /**
     * 同步获取国家码（一次性读取）。
     * 优先返回已缓存值；若为 null 则尝试直接查询接口。
     */
    fun getCountryCodeOnce(): String? {
        return _countryCode.value
            ?: RemoteAreaCodeManager.getInstance()
                .getAreaCode(DEFAULT_PRODUCT_ID, DEFAULT_DEVICE_ID)?.areacode
    }
}
