package com.dji.sample.gasdetection.gas.model

/**
 * 单个气体通道的测量值。
 *
 * 设计意图：业务层只依赖这一结构，不解析任何厂商私有字段。通道的身份是稳定字符串
 * [channelId]；名称、单位、量程、精度、采样周期与校准状态都来自通道目录，目录未提供时为
 * `null`（对外语义为「未提供」），不得推断为正常或填入默认常量。
 *
 * 单位按通道独立：同一条测量里 SO2 可能以 `µg/m³` 计、CO2 以 `ppm` 计，本结构不做换算，
 * 也不合并单位组。
 */
data class ChannelValue(
    /** 设备/适配器侧稳定通道标识，例如 `so2`。 */
    val channelId: String,
    /** 测量值，保留设备原文数值。 */
    val value: Double,
    /** 单位，来自通道目录；`null` 表示未提供。 */
    val unit: String?,
    /** 质量码，决定该值能否参与趋势与告警判定。 */
    val quality: MeasurementQuality,
    /** 显示名称，来自通道目录；`null` 表示未提供，不做翻译。 */
    val displayName: String? = null,
    /** 量程下限；`null` 表示设备未提供。 */
    val rangeMin: Double? = null,
    /** 量程上限；`null` 表示设备未提供。 */
    val rangeMax: Double? = null,
    /** 精度；`null` 表示设备未提供。 */
    val accuracy: Double? = null,
    /** 采样周期（毫秒）；`null` 表示设备未提供。 */
    val samplingPeriodMs: Long? = null,
    /** 校准状态原文；`null` 表示设备未提供。 */
    val calibrationStatus: String? = null,
)
