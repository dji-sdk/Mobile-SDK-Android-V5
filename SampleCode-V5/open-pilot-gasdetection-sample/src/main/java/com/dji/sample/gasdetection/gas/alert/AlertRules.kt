package com.dji.sample.gasdetection.gas.alert

import com.dji.sample.gasdetection.gas.model.AlertType
import com.dji.sample.gasdetection.gas.model.GasMeasurement
import com.dji.sample.gasdetection.gas.model.MeasurementQuality
import com.dji.sample.gasdetection.gas.task.GasJob

/**
 * 告警判定规则。
 *
 * 设计意图：把「什么测量在什么条件下算告警」收敛为纯函数，输入测量与任务配置，输出候选告警；
 * 不读写状态、不编号、不去重。阈值只来自任务通道配置，缺失时不产生越阈告警。
 */
object AlertRules {

    /** 阈值告警使用的规则标识；任务配置不再提供规则字段，统一使用该常量。 */
    const val RULE_THRESHOLD_FALLBACK = "task-channel-threshold"

    /** 阈值告警使用的规则版本。 */
    const val RULE_VERSION_FALLBACK = "task-config"
    const val RULE_SENSOR_FAULT = "sensor-fault-rule"
    const val RULE_DATA_QUALITY = "data-quality-rule"
    const val RULE_DATA_STALE = "data-stale-rule"
    const val RULE_VERSION = "1"

    /**
     * 按一条测量生成候选告警。
     *
     * 只处理任务配置里存在的通道；越阈只看有效样本，其他质量码进入数据质量或传感器故障。
     * 非阈值告警的 `threshold` 为 [Double.NaN]，表示不适用。
     */
    fun forMeasurement(measurement: GasMeasurement, job: GasJob): List<AlertCandidate> {
        val candidates = mutableListOf<AlertCandidate>()
        measurement.channels.forEach { channel ->
            val jobChannel = job.channels.firstOrNull { it.channelId == channel.channelId } ?: return@forEach
            val displayName = jobChannel.displayName.ifBlank { channel.displayName ?: channel.channelId }
            val base = AlertCandidate(
                taskId = job.jobId,
                measurementId = measurement.measurementId,
                channelId = channel.channelId,
                channelName = displayName,
                unit = channel.unit ?: jobChannel.unit,
                observedValue = channel.value,
                threshold = Double.NaN,
                ruleId = RULE_DATA_QUALITY,
                ruleVersion = RULE_VERSION,
                type = AlertType.DATA_QUALITY,
                timestampMs = measurement.receivedTimestampMs,
                latitude = measurement.position.takeIf { it.valid }?.latitude,
                longitude = measurement.position.takeIf { it.valid }?.longitude,
                vesselId = measurement.targetId,
            )
            when (channel.quality) {
                MeasurementQuality.VALID -> {
                    val threshold = jobChannel.threshold
                    if (threshold != null && channel.value > threshold) {
                        candidates += base.copy(
                            threshold = threshold,
                            ruleId = RULE_THRESHOLD_FALLBACK,
                            ruleVersion = RULE_VERSION_FALLBACK,
                            type = AlertType.THRESHOLD_EXCEEDED,
                        )
                    }
                }

                MeasurementQuality.SENSOR_FAULT -> {
                    candidates += base.copy(
                        ruleId = RULE_SENSOR_FAULT,
                        type = AlertType.SENSOR_FAULT,
                    )
                }

                MeasurementQuality.INVALID,
                MeasurementQuality.OUT_OF_RANGE,
                MeasurementQuality.CALIBRATION_DUE,
                MeasurementQuality.TIME_UNCERTAIN,
                -> candidates += base
            }
        }
        return candidates
    }

    /**
     * 按最近有效样本时间生成数据过期候选。
     *
     * 只有曾收到过有效样本的通道才会过期；从未有数据的通道不产生告警。
     */
    fun forStale(
        job: GasJob,
        lastValidAtMs: Map<String, Long>,
        lastValidValue: Map<String, Double>,
        nowMs: Long,
        staleAfterMs: Long,
    ): List<AlertCandidate> = job.channels.mapNotNull { channel ->
        val lastAt = lastValidAtMs[channel.channelId] ?: return@mapNotNull null
        val lastValue = lastValidValue[channel.channelId] ?: return@mapNotNull null
        if (nowMs - lastAt <= staleAfterMs) return@mapNotNull null
        AlertCandidate(
            taskId = job.jobId,
            measurementId = null,
            channelId = channel.channelId,
            channelName = channel.displayName.ifBlank { channel.channelId },
            unit = channel.unit,
            observedValue = lastValue,
            threshold = channel.threshold ?: Double.NaN,
            ruleId = RULE_DATA_STALE,
            ruleVersion = RULE_VERSION,
            type = AlertType.DATA_STALE,
            timestampMs = nowMs,
        )
    }
}
