package com.dji.sample.gasdetection.gas.model

/**
 * 告警事件类型。
 *
 * 设计意图：把阈值越限、传感器故障、数据过期与数据质量四类问题分开，避免把质量异常
 * 混成浓度超标。本 change 只定义结构，判定与状态流转由 FR-6 实现。
 */
enum class AlertType {
    THRESHOLD_EXCEEDED,
    SENSOR_FAULT,
    DATA_STALE,
    DATA_QUALITY,
}

/**
 * 告警处理状态。
 *
 * 设计意图：告警一旦产生就需要可追溯的处理链路，状态只由后续告警模块推进。
 */
enum class AlertState {
    NEW,
    ACKNOWLEDGED,
    UNDER_REVIEW,
    DISMISSED,
    CLOSED,
}

/**
 * 告警事件模型骨架。
 *
 * 设计意图：与测量记录共用通道、时间与位置语义，并携带规则标识与版本，保证「哪条规则在
 * 哪个版本下判定出这次告警」可回溯。字段缺失时保持 `null`，不填默认阈值或猜测来源。
 */
data class GasAlert(
    /** 告警事件幂等标识。 */
    val alertId: String,
    /** 关联任务标识。 */
    val taskId: String?,
    /** 触发告警的测量标识。 */
    val measurementId: String?,
    /** 触发通道标识。 */
    val channelId: String?,
    /** 观测值。 */
    val observedValue: Double,
    /** 观测值单位，与通道单位一致。 */
    val unit: String?,
    /** 触发阈值。 */
    val threshold: Double,
    /** 规则标识。 */
    val ruleId: String,
    /** 规则版本，用于回溯判定依据。 */
    val ruleVersion: String,
    /** 告警类型。 */
    val type: AlertType,
    /** 当前处理状态。 */
    val state: AlertState = AlertState.NEW,
    /** 事件时间（毫秒）。 */
    val timestampMs: Long,
    /** 事件位置纬度；无位置时为 `null`。 */
    val latitude: Double? = null,
    /** 事件位置经度；无位置时为 `null`。 */
    val longitude: Double? = null,
    /** 关联船舶标识；未关联时为 `null`。 */
    val vesselId: String? = null,
    /** 取证附件标识；未关联时为 `null`。 */
    val evidenceAssetId: String? = null,
)
