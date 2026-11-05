package com.dji.sample.gasdetection.gas.monitor

import com.dji.sample.gasdetection.gas.model.MeasurementQuality

/**
 * 通道短时趋势方向。
 *
 * 设计意图：监测页只需要「变大 / 变小 / 平稳 / 样本不足」四类可读结论，不引入专业统计或
 * 预测；样本不足时不画折线，避免用单点制造趋势假象。
 */
enum class TrendDirection {
    /** 窗口内末值相对首值上升超过 2%。 */
    RISING,

    /** 窗口内末值相对首值下降超过 2%。 */
    FALLING,

    /** 变化幅度在 ±2% 以内。 */
    STABLE,

    /** 有效样本少于两个，无法判断。 */
    INSUFFICIENT,
}

/**
 * 趋势窗口中的一个有效样本。
 *
 * @property value 测量值。
 * @property receivedAtMs 插件接收该样本的时间（毫秒），用于窗口排序与诊断。
 */
data class TrendSample(
    val value: Double,
    val receivedAtMs: Long,
)

/**
 * 单个通道的监测快照。
 *
 * 设计意图：把「最近一次读数」与「趋势窗口」放在同一结构，界面不需要再回溯测量列表；
 * 字段缺失时保持 `null`，由界面显示「未提供」，不填充 0 或沿用旧值。
 *
 * @property channelId 稳定通道标识。
 * @property displayName 设备原文显示名；`null` 表示未提供。
 * @property unit 设备原文单位；`null` 表示未提供。
 * @property value 最近一次读数；`null` 表示尚未收到。
 * @property quality 最近一次读数的质量码；`null` 表示尚未收到。
 * @property sourceTimestampMs 设备源时间；`null` 表示未提供。
 * @property receivedTimestampMs 插件接收时间；`null` 表示未提供。
 * @property samples 仅包含有效样本的趋势窗口，按接收时间升序。
 * @property trend 由 [samples] 计算出的方向。
 */
data class ChannelReading(
    val channelId: String,
    val displayName: String?,
    val unit: String?,
    val value: Double? = null,
    val quality: MeasurementQuality? = null,
    val sourceTimestampMs: Long? = null,
    val receivedTimestampMs: Long? = null,
    val samples: List<TrendSample> = emptyList(),
    val trend: TrendDirection = TrendDirection.INSUFFICIENT,
)

/**
 * 当前采样批次摘要。
 *
 * 设计意图：批次以最近测量的会话标识为准；链路离线时只冻结计数并标记非实时，不把历史
 * 批次显示成进行中。
 *
 * @property sessionId 当前会话标识；`null` 表示尚未收到测量。
 * @property totalSamples 当前会话累计样本数（按测量条数计）。
 * @property validSamples 至少包含一个有效通道的测量条数。
 * @property invalidSamples 不含任何有效通道的测量条数。
 * @property lastReceivedAtMs 最近一条测量的接收时间；`null` 表示尚未收到。
 * @property live 接入服务在监听且至少一个载荷在线时为 `true`。
 */
data class SamplingBatchSummary(
    val sessionId: String? = null,
    val totalSamples: Int = 0,
    val validSamples: Int = 0,
    val invalidSamples: Int = 0,
    val lastReceivedAtMs: Long? = null,
    val live: Boolean = false,
)

/**
 * 监测页整体快照。
 *
 * @property readings 按通道目录顺序排列的通道快照。
 * @property batch 当前采样批次摘要。
 * @property updatedAtMs 最近一次更新的时间（毫秒）。
 */
data class MonitorState(
    val readings: List<ChannelReading> = emptyList(),
    val batch: SamplingBatchSummary = SamplingBatchSummary(),
    val updatedAtMs: Long = 0,
)

/** 变化超过该比例才判定为上升/下降；低于阈值视为平稳。 */
internal const val TREND_THRESHOLD = 0.02

/**
 * 按有效样本窗口计算趋势方向。
 *
 * 设计意图：只比较窗口首尾值，不引入线性拟合；基准取首值绝对值，避免负值或零值导致
 * 方向反转，零附近用极小值兜底防止除零。
 */
internal fun trendOf(samples: List<TrendSample>): TrendDirection {
    if (samples.size < 2) return TrendDirection.INSUFFICIENT
    val first = samples.first().value
    val last = samples.last().value
    val denominator = maxOf(kotlin.math.abs(first), 1e-9)
    val delta = (last - first) / denominator
    return when {
        delta > TREND_THRESHOLD -> TrendDirection.RISING
        delta < -TREND_THRESHOLD -> TrendDirection.FALLING
        else -> TrendDirection.STABLE
    }
}
