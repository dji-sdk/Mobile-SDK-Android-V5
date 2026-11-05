package com.dji.sample.gasdetection.gas.record

import com.dji.sample.gasdetection.gas.model.GasAlert
import com.dji.sample.gasdetection.gas.model.VesselReference
import com.dji.sample.gasdetection.gas.task.GasJob

/**
 * 检测记录构建器。
 *
 * 设计意图：把「任务 + 累计快照 + 告警 + 船舶」组合成一条可持久化记录，保持纯函数便于单测；
 * 同一任务重复结束时直接复用已有记录，避免产生重复归档。
 */
object InspectionRecordBuilder {

    /**
     * 生成一条记录；[existing] 非空时直接返回它（重复结束复用）。
     */
    fun build(
        job: GasJob,
        snapshot: InspectionAggregator.Snapshot,
        alerts: List<GasAlert>,
        vessel: VesselReference?,
        endedAtMs: Long,
        endReason: RecordEndReason,
        existing: InspectionRecord? = null,
    ): InspectionRecord {
        if (existing != null) return existing
        val taskAlerts = alerts.filter { it.taskId == null || it.taskId == job.jobId }
        val alertIds = taskAlerts.map { it.alertId }.distinct()
        val summaries = taskAlerts
            .groupingBy { it.type }
            .eachCount()
            .map { (type, count) -> AlertSummary(type, count) }
            .sortedBy { it.type.ordinal }
        val reference = snapshot.vessel ?: vessel
        return InspectionRecord(
            recordId = "${job.jobId}-$endedAtMs",
            jobId = job.jobId,
            jobTitle = job.title,
            startedAtMs = snapshot.startedAtMs,
            endedAtMs = endedAtMs,
            endReason = endReason,
            totalSamples = snapshot.totalSamples,
            validSamples = snapshot.validSamples,
            invalidSamples = snapshot.invalidSamples,
            channels = snapshot.channels,
            alerts = summaries,
            alertIds = alertIds,
            vesselId = reference?.vesselId,
            vesselName = reference?.nameOrNumber,
            vesselConfirmed = reference?.humanConfirmed == true,
        )
    }
}
