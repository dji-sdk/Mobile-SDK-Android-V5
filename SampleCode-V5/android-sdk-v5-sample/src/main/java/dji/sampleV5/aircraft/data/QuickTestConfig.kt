package dji.sampleV5.aircraft.data

import android.graphics.Color
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.v5.manager.aircraft.lte.LTEPrivatizationServerInfo
import dji.v5.manager.areacode.AreaCode
import dji.v5.utils.common.ContextUtil
import dji.v5.utils.common.DjiSharedPreferencesManager
import dji.v5.utils.common.JsonUtil

object QuickTestConfig {

    private const val KEY_LTE_AUTHENTICATION_INFO = "key_lte_authentication_info"
    private const val KEY_AC_LTE_PRIVATIZATION_SERVER_INFO = "key_ac_lte_privatization_server_info"
    private const val KEY_RC_LTE_PRIVATIZATION_SERVER_INFO = "key_rc_lte_privatization_server_info"

    val simulatorAreaList = listOf(
        SimulatorArea("中国", LocationCoordinate2D(22.5797650, 113.9411710), AreaCode.CHINA),
        SimulatorArea("美国", LocationCoordinate2D(34.063191, -118.121621), AreaCode.UNITED_STATES_OF_AMERICA),
        SimulatorArea("日本", LocationCoordinate2D(35.658890, 139.746074), AreaCode.JAPAN),
        SimulatorArea("法国", LocationCoordinate2D(48.860284, 2.336282), AreaCode.FRANCE),
        SimulatorArea("德国", LocationCoordinate2D(52.516294, 13.376631), AreaCode.GERMANY),
        SimulatorArea("禁飞区", LocationCoordinate2D(22.645945, 113.816311), AreaCode.CHINA),
        SimulatorArea("授权区", LocationCoordinate2D(22.395237, 114.203203), AreaCode.CHINA),
        SimulatorArea("加强警告区", LocationCoordinate2D(22.208262, 114.03056), AreaCode.CHINA),
    )

    val CR810_OBSTACLE_COLOR_MAP: Map<Int, Int> = mapOf(
        0 to Color.argb(128, 128, 0, 0),      // floor
        1 to Color.argb(128, 220, 220, 0),    // wire
        2 to Color.argb(128, 128, 128, 128),  // fabric
        3 to Color.argb(128, 0, 0, 128),      // paper
        4 to Color.argb(128, 0, 64, 128),     // danger
        5 to Color.argb(128, 192, 0, 0),      // stool
        6 to Color.argb(128, 192, 128, 0),    // scale
        7 to Color.argb(128, 64, 0, 128),     // base
        8 to Color.argb(128, 128, 64, 0),     // stuck
        9 to Color.argb(128, 64, 0, 0),       // shoes
        10 to Color.argb(128, 64, 128, 128),   // door
        11 to Color.argb(128, 192, 0, 128),    // threshold
        12 to Color.argb(128, 192, 128, 128),   // carpet
        13 to Color.argb(128, 128, 0, 128),    // curtain
        14 to Color.argb(128, 128, 128, 0),    // cleanable
        15 to Color.argb(128, 0, 192, 0),      // particulate
        16 to Color.argb(128, 224, 224, 192),  // liquid
        17 to Color.argb(128, 0, 128, 0),      // background
        18 to Color.argb(128, 0, 0, 192),      // test
        -1 to Color.argb(128, 255, 255, 255)   // ignore
    )

    fun getCacheLTEAuthenticationInfo(): LTEAuthCacheInfo? {
        val str = DjiSharedPreferencesManager.getString(ContextUtil.getContext(), KEY_LTE_AUTHENTICATION_INFO, "")
        if (str.isEmpty()) {
            return LTEAuthCacheInfo()
        }
        return JsonUtil.toBean(DjiSharedPreferencesManager.getString(ContextUtil.getContext(), KEY_LTE_AUTHENTICATION_INFO, ""), LTEAuthCacheInfo::class.java)
    }

    fun updateCacheLTEAuthenticationInfo(info: LTEAuthCacheInfo) {
        DjiSharedPreferencesManager.putString(ContextUtil.getContext(), KEY_LTE_AUTHENTICATION_INFO, JsonUtil.toJson(info))
    }

    fun getCacheACLTEPrivatizationServerInfo(): LTEPrivatizationServerInfo? {
        val str = DjiSharedPreferencesManager.getString(ContextUtil.getContext(), KEY_AC_LTE_PRIVATIZATION_SERVER_INFO, "")
        if (str.isEmpty()) {
            return LTEPrivatizationServerInfo()
        }
        return JsonUtil.toBean(DjiSharedPreferencesManager.getString(ContextUtil.getContext(), KEY_AC_LTE_PRIVATIZATION_SERVER_INFO, ""), LTEPrivatizationServerInfo::class.java)
    }

    fun updateCacheACLTEPrivatizationServerInfo(info: LTEPrivatizationServerInfo) {
        DjiSharedPreferencesManager.putString(ContextUtil.getContext(), KEY_AC_LTE_PRIVATIZATION_SERVER_INFO, JsonUtil.toJson(info))
    }

    fun getCacheRCLTEPrivatizationServerInfo(): LTEPrivatizationServerInfo? {
        val str = DjiSharedPreferencesManager.getString(ContextUtil.getContext(), KEY_RC_LTE_PRIVATIZATION_SERVER_INFO, "")
        if (str.isEmpty()) {
            return LTEPrivatizationServerInfo()
        }
        return JsonUtil.toBean(DjiSharedPreferencesManager.getString(ContextUtil.getContext(), KEY_RC_LTE_PRIVATIZATION_SERVER_INFO, ""), LTEPrivatizationServerInfo::class.java)
    }

    fun updateCacheRCLTEPrivatizationServerInfo(info: LTEPrivatizationServerInfo) {
        DjiSharedPreferencesManager.putString(ContextUtil.getContext(), KEY_RC_LTE_PRIVATIZATION_SERVER_INFO, JsonUtil.toJson(info))
    }

    data class SimulatorArea(
        val name: String, val location: LocationCoordinate2D, val areaCode: AreaCode
    ) {
        override fun toString(): String {
            return name
        }
    }

    data class LTEAuthCacheInfo(
        val phoneAreaCode: String = "86",
        val phoneNumber: String = "1234567890",
        val verificationCode: String = "123456"
    )
}