package dji.sample.patrol.models

import dji.sample.patrol.R
import dji.sample.patrol.manager.RouteController

/**
 * 航线加载 / 起飞 / 返航被拒原因 → 本地化文案资源映射。
 *
 * 设计意图：
 * - 与 [RouteController] 的原因常量共用同一字符串来源，避免「判定改了、提示没跟上」而静默失配；
 * - 纯函数、不依赖 `Context` 与 SDK，可直接 JVM 单测；
 * - 未命中返回 `null`，由调用方回落通用文案并保留「未映射原因」诊断日志。
 */
object FailureReasonText {

    /** 返回原因对应的字符串资源 id；未映射（含 `null`）返回 `null`。 */
    fun resIdOf(message: String?): Int? = when (message) {
        RouteController.REASON_AIRCRAFT_NOT_CONNECTED -> R.string.fw_reason_aircraft_not_connected
        RouteController.REASON_CLOUD_NOT_CONFIGURED -> R.string.fw_reason_cloud_not_configured
        RouteController.REASON_ROUTE_NOT_READY -> R.string.fw_reason_route_not_ready
        RouteController.REASON_MISSION_RUNNING -> R.string.fw_reason_mission_running
        RouteController.REASON_NOT_AIRBORNE -> R.string.fw_reason_not_airborne
        RouteController.REASON_ALREADY_RETURNING -> R.string.fw_reason_already_returning
        RouteController.REASON_GPS_INVALID -> R.string.fw_reason_gps_invalid
        RouteController.REASON_HOME_POINT_NOT_RECORDED -> R.string.fw_reason_home_point_not_recorded
        else -> null
    }
}
