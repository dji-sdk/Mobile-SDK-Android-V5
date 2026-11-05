package com.dji.sample.gasdetection.gas.catalog

/**
 * 通道目录中的一条元数据。
 *
 * 设计意图：把「设备侧稳定身份」与「帧内压缩编码键」分开。[channelId] 是业务与模型对外
 * 使用的稳定标识（如 `so2`），[wireId] 只是本工程紧凑帧里的数值键，按目录版本分配，
 * 不代表设备真实通道号。名称与单位属于设备元数据，不做翻译。
 *
 * 量程、精度、采样周期与校准状态为可选；未提供时保持 `null`，不填推导常量。
 */
data class ChannelDescriptor(
    /** 帧内压缩编码键，由目录分配。 */
    val wireId: Int,
    /** 设备/适配器侧稳定通道标识。 */
    val channelId: String,
    /** 显示名称，设备原文，例如 `SO2`。 */
    val displayName: String,
    /** 单位，设备原文，例如 `µg/m³` 或 `ppm`。 */
    val unit: String,
    /** 量程下限；`null` 表示未提供。 */
    val rangeMin: Double? = null,
    /** 量程上限；`null` 表示未提供。 */
    val rangeMax: Double? = null,
    /** 精度；`null` 表示未提供。 */
    val accuracy: Double? = null,
    /** 采样周期（毫秒）；`null` 表示未提供。 */
    val samplingPeriodMs: Long? = null,
    /** 校准状态原文；`null` 表示未提供。 */
    val calibrationStatus: String? = null,
)
