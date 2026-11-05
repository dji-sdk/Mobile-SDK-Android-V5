package com.dji.sample.gasdetection.gas.alert

import com.dji.sample.gasdetection.gas.model.AlertState
import com.dji.sample.gasdetection.gas.model.AlertType
import com.dji.sample.gasdetection.gas.model.GasAlert
import com.dji.sample.gasdetection.gas.model.GasMeasurement
import com.dji.sample.gasdetection.gas.model.MeasurementQuality
import com.dji.sample.gasdetection.gas.task.GasJob
import com.dji.sample.gasdetection.gas.task.TaskStage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 告警中心：判定结果的去重、编号、状态流转与任务生命周期管理。
 *
 * 设计意图：判定规则（[AlertRules]）保持纯函数，本类只负责「同一告警在窗口内只出现一次、
 * 谁能改状态、任务变化时何时清空」。线程约束：所有方法在主线程调用，内部不加锁。
 *
 * @param now 当前时间提供者，测试可注入固定时钟。
 * @param windowMs 去重窗口（毫秒）。
 * @param maxAlerts 列表上限，超出时优先丢弃最旧的已处置告警。
 */
class AlertCenter(
    private val now: () -> Long = System::currentTimeMillis,
    private val windowMs: Long = DEFAULT_WINDOW_MS,
    private val maxAlerts: Int = DEFAULT_MAX_ALERTS,
) {

    init {
        require(windowMs > 0) { "windowMs must be > 0" }
        require(maxAlerts > 0) { "maxAlerts must be > 0" }
    }

    private val mutableState = MutableStateFlow(AlertUiState())
    val state: StateFlow<AlertUiState> = mutableState.asStateFlow()

    private var job: GasJob? = null
    private var stage: TaskStage = TaskStage.IDLE
    private val lastValidAtMs = mutableMapOf<String, Long>()
    private val lastValidValue = mutableMapOf<String, Double>()

    /**
     * 同步任务上下文。
     *
     * 任务标识变化、任务消失或进入收尾/空闲时清空告警、去重窗口与最近有效样本缓存；
     * 同一任务内阶段推进只更新阶段，不清空已有告警。
     */
    fun onTask(job: GasJob?, stage: TaskStage) {
        val reset = job?.jobId != this.job?.jobId ||
            job == null ||
            stage == TaskStage.IDLE ||
            stage == TaskStage.FINALIZING
        this.job = job
        this.stage = stage
        if (reset) {
            lastValidAtMs.clear()
            lastValidValue.clear()
            if (mutableState.value.alerts.isNotEmpty()) mutableState.value = AlertUiState()
        }
    }

    /** 记录一条测量：更新最近有效样本并应用测量类告警。 */
    fun onMeasurement(measurement: GasMeasurement) {
        val currentJob = job ?: return
        if (!isActiveStage(stage)) return
        measurement.channels.forEach { channel ->
            if (channel.quality == MeasurementQuality.VALID) {
                lastValidAtMs[channel.channelId] = measurement.receivedTimestampMs
                lastValidValue[channel.channelId] = channel.value
            }
        }
        apply(AlertRules.forMeasurement(measurement, currentJob))
    }

    /** 周期检查数据过期；[staleAfterMs] 为空表示配置不可用，不判定。 */
    fun onTick(staleAfterMs: Long?) {
        val currentJob = job ?: return
        if (staleAfterMs == null || !isActiveStage(stage)) return
        apply(
            AlertRules.forStale(
                job = currentJob,
                lastValidAtMs = lastValidAtMs,
                lastValidValue = lastValidValue,
                nowMs = now(),
                staleAfterMs = staleAfterMs,
            )
        )
    }

    /** 查看并确认；仅 `NEW` 允许。 */
    fun acknowledge(alertId: String): Boolean = transition(alertId, AlertAction.ACKNOWLEDGE)

    /** 标记异常；`NEW` / `ACKNOWLEDGED` 允许。 */
    fun markUnderReview(alertId: String): Boolean = transition(alertId, AlertAction.MARK_UNDER_REVIEW)

    /** 忽略提醒；活动状态允许。 */
    fun dismiss(alertId: String): Boolean = transition(alertId, AlertAction.DISMISS)

    private fun transition(alertId: String, action: AlertAction): Boolean {
        val list = mutableState.value.alerts.toMutableList()
        val index = list.indexOfFirst { it.alertId == alertId }
        if (index < 0) return false
        val next = AlertStateMachine.next(list[index].state, action) ?: return false
        list[index] = list[index].copy(state = next)
        mutableState.value = AlertUiState(list.sortedByDescending { it.timestampMs })
        return true
    }

    /**
     * 应用候选告警。
     *
     * - 窗口内同键活动告警：更新观测值等字段，不新增；过期告警保持首次发生时间。
     * - 窗口内同键已忽略：抑制，不弹出也不新增。
     * - 否则新增一条 `NEW`。
     */
    private fun apply(candidates: List<AlertCandidate>) {
        if (candidates.isEmpty()) return
        val list = mutableState.value.alerts.toMutableList()
        candidates.forEach { candidate ->
            val key = dedupeKeyOf(candidate.taskId, candidate.channelId, candidate.type)
            if (candidate.type == AlertType.DATA_STALE) {
                // 过期是持续状态：只要自上次有效样本以来已经有一条过期告警（活动或已忽略），
                // 就不再重复新增，避免数据一直不恢复时每分钟刷一条。
                val lastValid = lastValidAtMs[candidate.channelId] ?: Long.MIN_VALUE
                val alreadyAlerted = list.any {
                    dedupeKeyOf(it) == key && it.type == AlertType.DATA_STALE && it.timestampMs >= lastValid
                }
                if (alreadyAlerted) return@forEach
            }
            val existing = list.firstOrNull {
                dedupeKeyOf(it) == key &&
                    it.state.isActive &&
                    candidate.timestampMs - it.timestampMs <= windowMs
            }
            if (existing != null) {
                if (candidate.type != AlertType.DATA_STALE) {
                    val index = list.indexOf(existing)
                    list[index] = existing.copy(
                        measurementId = candidate.measurementId,
                        observedValue = candidate.observedValue,
                        unit = candidate.unit,
                        threshold = candidate.threshold,
                        ruleId = candidate.ruleId,
                        ruleVersion = candidate.ruleVersion,
                        timestampMs = candidate.timestampMs,
                        latitude = candidate.latitude,
                        longitude = candidate.longitude,
                    )
                }
                return@forEach
            }
            val suppressed = list.any {
                dedupeKeyOf(it) == key &&
                    it.state == AlertState.DISMISSED &&
                    candidate.timestampMs - it.timestampMs <= windowMs
            }
            if (suppressed) return@forEach
            list += candidate.toAlert()
        }
        trim(list)
        mutableState.value = AlertUiState(list.sortedByDescending { it.timestampMs })
    }

    private fun AlertCandidate.toAlert(): GasAlert = GasAlert(
        alertId = "$taskId|$channelId|${type.name}|$timestampMs",
        taskId = taskId,
        measurementId = measurementId,
        channelId = channelId,
        observedValue = observedValue,
        unit = unit,
        threshold = threshold,
        ruleId = ruleId,
        ruleVersion = ruleVersion,
        type = type,
        state = AlertState.NEW,
        timestampMs = timestampMs,
        latitude = latitude,
        longitude = longitude,
        vesselId = vesselId,
    )

    /** 超出上限时先丢最旧的已处置告警，再丢最旧的活动告警。 */
    private fun trim(list: MutableList<GasAlert>) {
        if (list.size <= maxAlerts) return
        val dismissed = list.filter { it.state == AlertState.DISMISSED }.sortedBy { it.timestampMs }
        val iterator = dismissed.iterator()
        while (list.size > maxAlerts && iterator.hasNext()) {
            list.remove(iterator.next())
        }
        while (list.size > maxAlerts) {
            val oldest = list.minByOrNull { it.timestampMs } ?: break
            list.remove(oldest)
        }
    }

    private fun isActiveStage(stage: TaskStage): Boolean =
        stage != TaskStage.IDLE && stage != TaskStage.FINALIZING

    private companion object {
        const val DEFAULT_WINDOW_MS = 60_000L
        const val DEFAULT_MAX_ALERTS = 100
    }
}
