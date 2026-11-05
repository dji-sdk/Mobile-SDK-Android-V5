package com.dji.sample.gasdetection.gas.monitor

import com.dji.sample.gasdetection.gas.catalog.GasConfig
import com.dji.sample.gasdetection.gas.model.ChannelValue
import com.dji.sample.gasdetection.gas.model.GasMeasurement
import com.dji.sample.gasdetection.gas.model.MeasurementQuality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 监测页的数据聚合器。
 *
 * 设计意图：把「最近读数 + 趋势窗口 + 采样批次」收敛为一份只读快照，界面不直接遍历测量流；
 * 聚合规则全部是纯计算，便于 JVM 单测。线程约束：所有方法在主线程调用（与
 * `PsdkApplication` 的收集作用域一致），本类内部不加锁。
 *
 * 关键规则：
 * - 只有质量码为 [MeasurementQuality.VALID] 的样本进入趋势窗口，窗口容量固定，
 *   超出后丢弃最旧样本。
 * - 会话标识变化视为新批次：计数归零、趋势窗口清空，避免跨会话混合。
 * - 接入离线时批次标记非实时，计数不再增长，但保留最后摘要供排障。
 *
 * @param configProvider 惰性提供气体配置；配置未就绪时只展示已收到的通道。
 * @param windowSize 每通道保留的有效样本数，至少为 2。
 * @param now 当前时间提供者，测试可注入固定时钟。
 */
class MonitorStore(
    private val configProvider: () -> GasConfig?,
    private val windowSize: Int = DEFAULT_WINDOW_SIZE,
    private val now: () -> Long = System::currentTimeMillis,
) {

    init {
        require(windowSize >= 2) { "windowSize must be >= 2" }
    }

    private val mutableState = MutableStateFlow(MonitorState(updatedAtMs = now()))
    val state: StateFlow<MonitorState> = mutableState.asStateFlow()

    private val readings = LinkedHashMap<String, MutableReading>()
    private var sessionId: String? = null
    private var totalSamples = 0
    private var validSamples = 0
    private var invalidSamples = 0
    private var lastReceivedAtMs: Long? = null
    private var listening = false
    private var onlinePayloads = 0

    /**
     * 更新链路实时性。
     *
     * @param listening 接入服务是否在监听。
     * @param onlinePayloads 当前在线载荷数。
     */
    fun setLink(listening: Boolean, onlinePayloads: Int) {
        this.listening = listening
        this.onlinePayloads = onlinePayloads
        publish()
    }

    /**
     * 记录一条测量。
     *
     * 同一会话内累加样本数；会话变化时开启新批次。通道元数据（显示名/单位）取最近一次
     * 收到的值，空缺时保留目录或既有值，不凭空补默认字符串。
     *
     * 接入服务未监听时丢弃迟到帧：停止监听后不应再增加批次计数，避免把已结束的批次
     * 显示成仍在采样。
     */
    fun record(measurement: GasMeasurement) {
        if (!listening) return
        if (sessionId != measurement.sessionId) {
            sessionId = measurement.sessionId
            totalSamples = 0
            validSamples = 0
            invalidSamples = 0
            readings.values.forEach { it.samples.clear() }
        }

        totalSamples += 1
        val measurementHasValidChannel = measurement.channels.any { it.quality == MeasurementQuality.VALID }
        if (measurementHasValidChannel) validSamples += 1 else invalidSamples += 1
        lastReceivedAtMs = measurement.receivedTimestampMs

        measurement.channels.forEach { channel ->
            val reading = readings.getOrPut(channel.channelId) {
                MutableReading(channel.channelId, channel.displayName, channel.unit)
            }
            reading.update(channel, measurement.sourceTimestampMs, measurement.receivedTimestampMs)
            if (channel.quality == MeasurementQuality.VALID) {
                reading.samples.addLast(TrendSample(channel.value, measurement.receivedTimestampMs))
                while (reading.samples.size > windowSize) reading.samples.removeFirst()
            }
        }
        publish()
    }

    /** 生成一份新快照，按目录顺序排列通道，并带上目录中尚未收到数据的通道。 */
    private fun publish() {
        val catalog = configProvider()?.catalog
        catalog?.descriptors()?.forEach { descriptor ->
            readings.getOrPut(descriptor.channelId) {
                MutableReading(descriptor.channelId, descriptor.displayName, descriptor.unit)
            }
        }

        val ordered = buildList {
            catalog?.descriptors()?.forEach { descriptor ->
                readings[descriptor.channelId]?.let(::add)
            }
            readings.values.forEach { reading ->
                if (none { it.channelId == reading.channelId }) add(reading)
            }
        }

        mutableState.value = MonitorState(
            readings = ordered.map { it.toSnapshot() },
            batch = SamplingBatchSummary(
                sessionId = sessionId,
                totalSamples = totalSamples,
                validSamples = validSamples,
                invalidSamples = invalidSamples,
                lastReceivedAtMs = lastReceivedAtMs,
                live = listening && onlinePayloads > 0,
            ),
            updatedAtMs = now(),
        )
    }

    /** 单通道的可变累加状态，只在主线程读写。 */
    private class MutableReading(
        val channelId: String,
        var displayName: String?,
        var unit: String?,
    ) {
        var value: Double? = null
        var quality: MeasurementQuality? = null
        var sourceTimestampMs: Long? = null
        var receivedTimestampMs: Long? = null
        val samples = ArrayDeque<TrendSample>()

        fun update(channel: ChannelValue, sourceTimeMs: Long, receivedTimeMs: Long) {
            value = channel.value
            quality = channel.quality
            sourceTimestampMs = sourceTimeMs
            receivedTimestampMs = receivedTimeMs
            channel.displayName?.let { displayName = it }
            channel.unit?.let { unit = it }
        }

        fun toSnapshot(): ChannelReading = ChannelReading(
            channelId = channelId,
            displayName = displayName,
            unit = unit,
            value = value,
            quality = quality,
            sourceTimestampMs = sourceTimestampMs,
            receivedTimestampMs = receivedTimestampMs,
            samples = samples.toList(),
            trend = trendOf(samples.toList()),
        )
    }

    companion object {
        /** 默认趋势窗口容量；约 30 个有效样本足以看出一分钟内的短期变化。 */
        const val DEFAULT_WINDOW_SIZE = 30
    }
}
