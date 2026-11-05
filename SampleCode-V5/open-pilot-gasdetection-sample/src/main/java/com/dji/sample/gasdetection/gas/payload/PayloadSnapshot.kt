package com.dji.sample.gasdetection.gas.payload

/**
 * 单个端口的载荷在线快照。
 *
 * 设计意图：业务层需要「哪个端口有载荷、是什么设备」的稳定视图，但不应直接持有 SDK 的
 * 回调对象。设备未返回的字段保持 `null`（对外「未提供」），不用端口名或类型名冒充产品名。
 */
data class PayloadSnapshot(
    /** 端口标识。 */
    val port: PayloadPort,
    /** 载荷是否连接。 */
    val connected: Boolean,
    /** 载荷类型枚举名；`null` 表示未提供。 */
    val payloadType: String?,
    /** 设备制造商定义的产品名；`null` 表示未提供。 */
    val productName: String?,
    /** 产品阶段枚举名；`null` 表示未提供。 */
    val productPhase: String?,
    /** 序列号；`null` 表示未提供。 */
    val serialNumber: String?,
    /** 固件版本；`null` 表示未提供。 */
    val firmwareVersion: String?,
    /** 产品功能是否已打开（与连接状态分开表达）。 */
    val featureOpened: Boolean,
    /** 快照更新时间（毫秒）。 */
    val updatedAtMs: Long,
)
