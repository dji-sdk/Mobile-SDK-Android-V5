package com.dji.sample.gasdetection.gas.model

/**
 * 单个字段的来源标记。
 *
 * 设计意图：测量记录里既有设备上报值，也有插件接收时补全的本机事实（任务、载荷标识、
 * 阶段、接收时间）。两者必须可区分，后续归档与排障才不会把本机补全值当成设备原值。
 */
enum class ValueOrigin {
    /** 设备或适配器在帧内显式提供。 */
    DEVICE,

    /** 插件按本地事实补全。 */
    LOCAL,
}

/**
 * 测量记录中易混淆字段的来源集合。
 *
 * 默认全部为本机补全；解码时只有帧内显式出现且确属设备提供的字段才标 [ValueOrigin.DEVICE]。
 * 位置来源不使用本类型，它本身是 [LocationSource] 三值枚举。
 */
data class MeasurementOrigins(
    /** 任务标识来源。 */
    val taskId: ValueOrigin = ValueOrigin.LOCAL,
    /** 载荷标识来源。 */
    val payloadId: ValueOrigin = ValueOrigin.LOCAL,
    /** 飞行阶段来源。 */
    val flightPhase: ValueOrigin = ValueOrigin.LOCAL,
    /** 接收时间来源，固定为本机补全。 */
    val receivedTime: ValueOrigin = ValueOrigin.LOCAL,
)
