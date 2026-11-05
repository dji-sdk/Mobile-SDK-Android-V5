package com.dji.sample.gasdetection.gas.model

/**
 * 一条气体测量记录，是载荷适配器、界面、告警与归档之间的统一契约。
 *
 * 设计意图：把「设备给了什么」与「插件补了什么」放在同一结构中但可区分：
 * - 时间分设备源时间 [sourceTimestampMs] 与插件接收时间 [receivedTimestampMs]；
 * - 位置带有效性 [GeoPosition.valid] 与来源 [locationSource]；
 * - 任务、载荷标识与飞行阶段的补全来源记录在 [origins]。
 *
 * [measurementId] 由 `sessionId#sequence` 确定性派生，供后续归档层做幂等。
 * [targetConfidence] 为 `null` 表示识别接口未提供，不得用 0 代替。
 */
data class GasMeasurement(
    /** 幂等标识，格式为 `sessionId#sequence`。 */
    val measurementId: String,
    /** 采样会话标识，同一次采样任务内稳定。 */
    val sessionId: String,
    /** 会话内递增序列号，用于重传去重。 */
    val sequence: Long,
    /** 载荷标识；`null` 表示未知。 */
    val payloadId: String?,
    /** 设备产生该样本的时间（毫秒）。 */
    val sourceTimestampMs: Long,
    /** 插件接收到该样本的时间（毫秒）。 */
    val receivedTimestampMs: Long,
    /** 本条测量包含的通道值，顺序由适配器决定。 */
    val channels: List<ChannelValue>,
    /** 采样点位置。 */
    val position: GeoPosition,
    /** 位置来源，无法确认时为 [LocationSource.UNKNOWN]。 */
    val locationSource: LocationSource,
    /** 任务标识；`null` 表示未关联任务。 */
    val taskId: String? = null,
    /** 飞行阶段，无任务上下文时按 [FlightPhase.MANUAL] 处理。 */
    val flightPhase: FlightPhase = FlightPhase.MANUAL,
    /** 是否处于自动作业模式。 */
    val automaticMode: Boolean = false,
    /** 关联的船舶跟踪标识；`null` 表示未关联。 */
    val targetId: String? = null,
    /** 目标识别置信度；`null` 表示识别接口未提供。 */
    val targetConfidence: Double? = null,
    /** 适配器版本；`null` 表示未知。 */
    val adapterVersion: String? = null,
    /** 易混淆字段的来源标记。 */
    val origins: MeasurementOrigins = MeasurementOrigins(),
)
