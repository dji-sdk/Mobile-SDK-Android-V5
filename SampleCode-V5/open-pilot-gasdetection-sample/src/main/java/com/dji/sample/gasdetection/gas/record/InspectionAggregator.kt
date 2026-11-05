package com.dji.sample.gasdetection.gas.record

import com.dji.sample.gasdetection.gas.model.GasMeasurement
import com.dji.sample.gasdetection.gas.model.MeasurementQuality
import com.dji.sample.gasdetection.gas.model.VesselReference

/**
 * 任务级检测数据累计器。
 *
 * 设计意图：`MonitorStore` 只保留最近读数与短趋势窗口，不能回答「整个任务采了多少、每通道极值多少」。
 * 本类在主线程累计任务维度的统计，任务切换（jobId 变化）时自动复位；所有计算为纯逻辑，可 JVM 单测。
 */
class InspectionAggregator {

    /** 一次任务累计的快照。 */
    data class Snapshot(
        val jobId: String?,
        val startedAtMs: Long?,
        val totalSamples: Int,
        val validSamples: Int,
        val invalidSamples: Int,
        val channels: List<ChannelSummary>,
        val vessel: VesselReference?,
    )

    private data class ChannelAgg(
        var displayName: String?,
        var unit: String?,
        var count: Int = 0,
        var min: Double = Double.MAX_VALUE,
        var max: Double = -Double.MAX_VALUE,
        var sum: Double = 0.0,
    )

    private var jobId: String? = null
    private var startedAtMs: Long? = null
    private var totalSamples = 0
    private var validSamples = 0
    private var invalidSamples = 0
    private val channels = LinkedHashMap<String, ChannelAgg>()
    private var vessel: VesselReference? = null

    /** 清空全部累计状态。 */
    fun reset() {
        jobId = null
        startedAtMs = null
        totalSamples = 0
        validSamples = 0
        invalidSamples = 0
        channels.clear()
        vessel = null
    }

    /**
     * 累计一条测量。
     *
     * @param jobId 当前任务标识；与上次不同时先复位，避免跨任务混合。
     */
    fun onMeasurement(measurement: GasMeasurement, jobId: String?) {
        if (jobId != null && this.jobId != null && jobId != this.jobId) reset()
        if (jobId != null) this.jobId = jobId
        if (startedAtMs == null) startedAtMs = measurement.receivedTimestampMs
        totalSamples += 1
        val hasValid = measurement.channels.any { it.quality == MeasurementQuality.VALID }
        if (hasValid) validSamples += 1 else invalidSamples += 1
        measurement.channels.forEach { channel ->
            if (channel.quality != MeasurementQuality.VALID) return@forEach
            val agg = channels.getOrPut(channel.channelId) {
                ChannelAgg(displayName = channel.displayName, unit = channel.unit)
            }
            agg.displayName = agg.displayName ?: channel.displayName
            agg.unit = agg.unit ?: channel.unit
            agg.count += 1
            agg.min = minOf(agg.min, channel.value)
            agg.max = maxOf(agg.max, channel.value)
            agg.sum += channel.value
        }
    }

    /** 记录当前或最近一次锁定的船舶引用。 */
    fun onVessel(reference: VesselReference?) {
        if (reference != null) vessel = reference
    }

    /** 取指定任务的累计快照；任务不匹配时返回空快照，不串用其它任务的数据。 */
    fun snapshot(jobId: String?): Snapshot {
        val matches = jobId == null || this.jobId == null || jobId == this.jobId
        if (!matches) {
            return Snapshot(jobId, null, 0, 0, 0, emptyList(), null)
        }
        return Snapshot(
            jobId = this.jobId,
            startedAtMs = startedAtMs,
            totalSamples = totalSamples,
            validSamples = validSamples,
            invalidSamples = invalidSamples,
            channels = channels.map { (channelId, agg) ->
                ChannelSummary(
                    channelId = channelId,
                    displayName = agg.displayName,
                    unit = agg.unit,
                    validSamples = agg.count,
                    min = agg.min,
                    max = agg.max,
                    average = if (agg.count == 0) 0.0 else agg.sum / agg.count,
                )
            },
            vessel = vessel,
        )
    }
}
