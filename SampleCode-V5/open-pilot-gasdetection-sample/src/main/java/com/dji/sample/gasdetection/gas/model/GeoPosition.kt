package com.dji.sample.gasdetection.gas.model

/**
 * 采样点位置。
 *
 * 设计意图：定位不可信时必须显式表达，不能把上一次位置或 `0,0` 当作本次坐标。
 * [valid] 为 `false` 时坐标字段一律为 `null`，下游展示与归档按「定位无效」处理。
 */
data class GeoPosition(
    /** 纬度；定位无效时为 `null`。 */
    val latitude: Double?,
    /** 经度；定位无效时为 `null`。 */
    val longitude: Double?,
    /** 海拔（米）；定位无效时为 `null`。 */
    val altitudeM: Double?,
    /** 水平精度（米）；未提供时为 `null`。 */
    val horizontalAccuracyM: Double?,
    /** 本次定位是否可信。 */
    val valid: Boolean,
) {
    companion object {
        /** 无可用定位时的占位值，坐标全部为空且标记无效。 */
        val UNKNOWN = GeoPosition(
            latitude = null,
            longitude = null,
            altitudeM = null,
            horizontalAccuracyM = null,
            valid = false,
        )
    }
}
