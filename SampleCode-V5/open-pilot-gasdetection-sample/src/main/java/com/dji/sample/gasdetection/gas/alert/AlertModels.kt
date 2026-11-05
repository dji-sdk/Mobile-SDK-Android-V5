package com.dji.sample.gasdetection.gas.alert

import com.dji.sample.gasdetection.gas.model.AlertState
import com.dji.sample.gasdetection.gas.model.AlertType
import com.dji.sample.gasdetection.gas.model.GasAlert

/**
 * 告警展示等级。
 *
 * 设计意图：业务类型决定提醒强度——越阈与传感器故障是错误级，数据质量与数据过期是提醒级；
 * 浮窗按等级挑选摘要，页面按等级着色，不需要各界面各自判断类型。
 */
enum class AlertSeverity {
    ERROR,
    WARNING,
}

/** 由告警类型派生展示等级；未知类型按提醒级处理，避免夸大严重性。 */
fun AlertType.severity(): AlertSeverity = when (this) {
    AlertType.THRESHOLD_EXCEEDED, AlertType.SENSOR_FAULT -> AlertSeverity.ERROR
    AlertType.DATA_STALE, AlertType.DATA_QUALITY -> AlertSeverity.WARNING
}

/** 活动状态：仍需要飞手处置，进入列表与浮窗摘要。 */
val AlertState.isActive: Boolean
    get() = this == AlertState.NEW || this == AlertState.ACKNOWLEDGED || this == AlertState.UNDER_REVIEW

/** 飞手对告警的处置动作。 */
enum class AlertAction {
    ACKNOWLEDGE,
    MARK_UNDER_REVIEW,
    DISMISS,
}

/**
 * 告警状态机。
 *
 * 设计意图：把允许的迁移集中在一处，界面按钮与接口调用都只能通过它推进状态；非法迁移返回
 * `null`，调用方保持原状态，不做乐观更新。
 */
object AlertStateMachine {

    /** 返回目标状态；当前状态不允许该动作时返回 `null`。 */
    fun next(current: AlertState, action: AlertAction): AlertState? = when (action) {
        AlertAction.ACKNOWLEDGE -> if (current == AlertState.NEW) AlertState.ACKNOWLEDGED else null
        AlertAction.MARK_UNDER_REVIEW ->
            if (current == AlertState.NEW || current == AlertState.ACKNOWLEDGED) {
                AlertState.UNDER_REVIEW
            } else {
                null
            }

        AlertAction.DISMISS ->
            if (current.isActive) AlertState.DISMISSED else null
    }
}

/**
 * 判定阶段生成的候选告警。
 *
 * 设计意图：判定器只描述「哪条测量在什么规则下命中」，不负责编号、去重与状态；这些由
 * [AlertCenter] 统一处理，便于单测判定规则而不依赖存储。
 */
data class AlertCandidate(
    val taskId: String,
    val measurementId: String?,
    val channelId: String,
    val channelName: String,
    val unit: String?,
    val observedValue: Double,
    val threshold: Double,
    val ruleId: String,
    val ruleVersion: String,
    val type: AlertType,
    val timestampMs: Long,
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** 关联船舶标识；来自测量上的目标关联，未锁定或未关联时为 `null`。 */
    val vesselId: String? = null,
)

/**
 * 告警列表快照。
 *
 * @property alerts 全部告警（含已忽略），按时间倒序。
 */
data class AlertUiState(
    val alerts: List<GasAlert> = emptyList(),
) {
    /** 仍需处置的告警。 */
    val activeAlerts: List<GasAlert> get() = alerts.filter { it.state.isActive }

    /** 浮窗摘要使用的最高优先级告警：先比等级，再比时间。 */
    fun highestPriority(): GasAlert? = activeAlerts.maxWithOrNull(
        compareBy<GasAlert> { it.type.severity().ordinal }.thenBy { it.timestampMs },
    )
}

/** 去重键：同一任务、通道与类型共享一个窗口。 */
internal fun dedupeKeyOf(taskId: String?, channelId: String?, type: AlertType): String =
    "${taskId.orEmpty()}|${channelId.orEmpty()}|${type.name}"

internal fun dedupeKeyOf(alert: GasAlert): String = dedupeKeyOf(alert.taskId, alert.channelId, alert.type)
