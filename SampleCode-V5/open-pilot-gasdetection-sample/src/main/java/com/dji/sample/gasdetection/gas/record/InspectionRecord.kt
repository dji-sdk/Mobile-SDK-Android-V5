package com.dji.sample.gasdetection.gas.record

import com.dji.sample.gasdetection.gas.model.AlertType

/** 记录上传状态；与巡逻 Demo 的上传队列语义保持一致。 */
enum class RecordStatus {
    /** 等待上传。 */
    QUEUED,

    /** 正在上传。 */
    UPLOADING,

    /** 已上传（终态）。 */
    UPLOADED,

    /** 自动重试耗尽，等待手动重试。 */
    FAILED,
}

/** 任务结束方式。 */
enum class RecordEndReason {
    /** 飞手在手动/搜索阶段结束。 */
    PILOT_FINISH,

    /** 跟随退出后结束。 */
    FOLLOW_EXIT_THEN_FINISH,
}

/**
 * 单个通道的检测摘要。
 *
 * 只统计质量码为有效的样本；没有有效样本的通道不会出现在记录里。
 */
data class ChannelSummary(
    val channelId: String,
    val displayName: String?,
    val unit: String?,
    val validSamples: Int,
    val min: Double,
    val max: Double,
    val average: Double,
)

/** 告警按类型的计数。 */
data class AlertSummary(
    val type: AlertType,
    val count: Int,
)

/**
 * 一条本机检测记录。
 *
 * 设计意图：任务结束后可离线保存、幂等上传并可追溯的记录快照。`recordId` 是上传幂等键；
 * 上传状态与重试信息随记录一起持久化，`UPLOADED` 记录不因重试或重启删除。
 */
data class InspectionRecord(
    /** 幂等键，格式 `<jobId>-<endedAtMs>`。 */
    val recordId: String,
    val jobId: String,
    val jobTitle: String,
    /** 首条测量时间；没有测量时为 `null`。 */
    val startedAtMs: Long?,
    val endedAtMs: Long,
    val endReason: RecordEndReason,
    val totalSamples: Int,
    val validSamples: Int,
    val invalidSamples: Int,
    val channels: List<ChannelSummary>,
    val alerts: List<AlertSummary>,
    val alertIds: List<String>,
    /** 关联船舶标识；未锁定时为 `null`。 */
    val vesselId: String? = null,
    val vesselName: String? = null,
    val vesselConfirmed: Boolean = false,
    val status: RecordStatus = RecordStatus.QUEUED,
    val retryCount: Int = 0,
    val lastError: String? = null,
    /** 已上传但任务完成状态尚未回写成功。 */
    val completionPending: Boolean = false,
    val uploadedAtMs: Long? = null,
) {
    /** 是否需要（重新）上传。 */
    val needsUpload: Boolean
        get() = status == RecordStatus.QUEUED
}
