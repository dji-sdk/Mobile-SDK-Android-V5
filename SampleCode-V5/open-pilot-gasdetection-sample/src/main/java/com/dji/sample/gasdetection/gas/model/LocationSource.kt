package com.dji.sample.gasdetection.gas.model

/**
 * 测量位置的事实来源。
 *
 * 设计意图：机载定位与遥控器定位的精度和适用场景不同，规划方案要求在无法确认来源时
 * 明确降级而不是默认按机载定位处理。未知即 [UNKNOWN]，调用方不得用其他值兜底。
 */
enum class LocationSource {
    /** 飞行器机载定位。 */
    AIRCRAFT,

    /** 遥控器定位。 */
    REMOTE_CONTROLLER,

    /** 无法确认来源。 */
    UNKNOWN,
}
