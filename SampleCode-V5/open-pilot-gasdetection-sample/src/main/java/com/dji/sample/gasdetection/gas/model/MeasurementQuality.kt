package com.dji.sample.gasdetection.gas.model

/**
 * 通道测量值的质量码。
 *
 * 设计意图：把设备上报的有效性、量程、传感器与时钟问题统一成同一套可判定状态，
 * 让趋势、告警与归档不依赖各厂商私有错误码。取值与规划方案 §4.2 的
 * `MeasurementQuality` 一一对应，帧内以 [code] 传输。
 */
enum class MeasurementQuality(val code: Int) {
    /** 数值有效。 */
    VALID(1),

    /** 设备明确标记无效。 */
    INVALID(2),

    /** 数值超出设备量程。 */
    OUT_OF_RANGE(3),

    /** 传感器故障，数值保留但不作为有效样本。 */
    SENSOR_FAULT(4),

    /** 校准到期。 */
    CALIBRATION_DUE(5),

    /** 源时间晚于接收时间，时间不可信。 */
    TIME_UNCERTAIN(6),
    ;

    companion object {
        /** 按帧内质量码查找；未知码返回 `null`，由调用方标记为解析失败而不是猜测。 */
        fun fromCode(code: Int): MeasurementQuality? = entries.firstOrNull { it.code == code }
    }
}
