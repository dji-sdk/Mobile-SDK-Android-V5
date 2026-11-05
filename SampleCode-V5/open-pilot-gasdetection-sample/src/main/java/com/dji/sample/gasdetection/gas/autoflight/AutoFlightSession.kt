package com.dji.sample.gasdetection.gas.autoflight

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 自动飞行会话状态机（纯 Kotlin）。
 *
 * 设计意图：把「命令已下发 / 等待权威状态 / 超时 / 接管」的时序判定从 SDK 与界面里剥离，
 * 输入只有带时间戳的事件，输出只有 [AutoFlightState]。它不持有协程、不读时钟，看门狗由调用方
 * 按固定节奏调用 [checkTimeout] 触发，因此可以在纯 JVM 单测里完整覆盖。
 *
 * 线程约束：所有方法必须在同一线程（应用内为主线程）调用。
 */
class AutoFlightSession(
    private val startTimeoutMs: Long = START_TIMEOUT_MS,
    private val progressTimeoutMs: Long = PROGRESS_TIMEOUT_MS,
) {

    private val mutableState = MutableStateFlow(AutoFlightState())
    val state: StateFlow<AutoFlightState> = mutableState.asStateFlow()

    private var commandSentAtMs: Long? = null
    private var acceptedAtMs: Long? = null
    private var lastProgressAtMs: Long? = null

    /**
     * 记录启动命令已下发；只有空闲态才能开始新的会话。
     *
     * @return `true` 表示进入 `STARTING`，`false` 表示当前阶段不允许再次启动。
     */
    fun onStartCommandSent(nowMs: Long): Boolean {
        if (mutableState.value.phase != AutoFlightPhase.IDLE) return false
        commandSentAtMs = nowMs
        acceptedAtMs = null
        lastProgressAtMs = null
        mutableState.value = AutoFlightState(phase = AutoFlightPhase.STARTING)
        return true
    }

    /** 处理启动命令回调；成功只记录「已接受」，失败直接进入接管态。 */
    fun onStartOutcome(outcome: StartOutcome, nowMs: Long) {
        if (mutableState.value.phase != AutoFlightPhase.STARTING) return
        when (outcome) {
            is StartOutcome.Accepted -> acceptedAtMs = nowMs
            is StartOutcome.Failed -> handover(
                reason = HandoverReason.START_NOT_CONFIRMED,
                exitReason = null,
                errorCode = outcome.errorCode,
                errorDescription = outcome.description,
            )
        }
    }

    /**
     * 处理 FlyTo 权威状态。
     *
     * 映射规则：准备/起飞前/起飞/巡航 → `FLYING`；`FINISHED` → `ARRIVED`；
     * `INTERRUPTED` → 接管；`UNKNOWN` 与已接管后的状态不推进阶段。
     */
    fun onFlyToStage(stage: FlyToStage, exitReason: String?, nowMs: Long) {
        val current = mutableState.value.phase
        if (current != AutoFlightPhase.STARTING && current != AutoFlightPhase.FLYING) return
        when (stage) {
            FlyToStage.UNKNOWN -> Unit
            FlyToStage.INTERRUPTED -> handover(
                reason = HandoverReason.TASK_INTERRUPTED,
                exitReason = exitReason,
                errorCode = null,
                errorDescription = null,
            )

            FlyToStage.FINISHED -> {
                lastProgressAtMs = nowMs
                mutableState.update {
                    it.copy(
                        phase = AutoFlightPhase.ARRIVED,
                        flyToStage = FlyToStage.FINISHED,
                        exitReason = exitReason,
                    )
                }
            }

            FlyToStage.PREPARE,
            FlyToStage.LAUNCH_PRE,
            FlyToStage.LAUNCH,
            FlyToStage.MARCHON,
            -> {
                lastProgressAtMs = nowMs
                mutableState.update {
                    it.copy(phase = AutoFlightPhase.FLYING, flyToStage = stage)
                }
            }
        }
    }

    /**
     * 只判定超时，不改变状态；调用方拿到结果后再调用 [handover]。
     *
     * 规则：`STARTING` 阶段命令被接受后 5 秒无状态推进 → 状态停滞；从未被接受且距下发
     * 超过 10 秒 → 启动未确认；`FLYING` 阶段距上一次状态推进超过 5 秒 → 状态停滞。
     */
    fun checkTimeout(nowMs: Long): AutoFlightTimeout? {
        return when (mutableState.value.phase) {
            AutoFlightPhase.STARTING -> {
                val accepted = acceptedAtMs
                val sent = commandSentAtMs
                when {
                    accepted != null && nowMs - accepted >= progressTimeoutMs ->
                        AutoFlightTimeout.STATE_NOT_ADVANCED

                    sent != null && nowMs - sent >= startTimeoutMs ->
                        AutoFlightTimeout.START_NOT_CONFIRMED

                    else -> null
                }
            }

            AutoFlightPhase.FLYING -> {
                val progressed = lastProgressAtMs ?: return null
                if (nowMs - progressed >= progressTimeoutMs) {
                    AutoFlightTimeout.STATE_NOT_ADVANCED
                } else {
                    null
                }
            }

            else -> null
        }
    }

    /** 进入接管态；首个原因优先，后续原因不覆盖。 */
    fun onHandover(reason: HandoverReason, exitReason: String? = null) {
        handover(
            reason = reason,
            exitReason = exitReason,
            errorCode = null,
            errorDescription = null,
        )
    }

    /** 更新只读控制权快照；任何阶段都可以刷新。 */
    fun onAuthority(
        authority: FlightAuthority,
        virtualStickEnabled: Boolean,
        virtualStickAdvancedMode: Boolean,
    ) {
        mutableState.update {
            it.copy(
                authority = authority,
                virtualStickEnabled = virtualStickEnabled,
                virtualStickAdvancedMode = virtualStickAdvancedMode,
            )
        }
    }

    /** 记录紧急停止命令失败，保留错误信息供界面展示。 */
    fun onStopFailed(errorCode: String?, description: String?) {
        mutableState.update {
            it.copy(errorCode = errorCode, errorDescription = description)
        }
    }

    /** 飞手确认接管：回到空闲，允许下一次重新预检与授权。 */
    fun onHandoverConfirmed() {
        if (mutableState.value.phase != AutoFlightPhase.HANDOVER) return
        reset()
    }

    /** 清空会话（Pilot 断开、任务重置或交付收尾）；只保留只读控制权快照。 */
    fun reset() {
        commandSentAtMs = null
        acceptedAtMs = null
        lastProgressAtMs = null
        val current = mutableState.value
        mutableState.value = AutoFlightState(
            authority = current.authority,
            virtualStickEnabled = current.virtualStickEnabled,
            virtualStickAdvancedMode = current.virtualStickAdvancedMode,
        )
    }

    private fun handover(
        reason: HandoverReason,
        exitReason: String?,
        errorCode: String?,
        errorDescription: String?,
    ) {
        val current = mutableState.value
        if (current.phase == AutoFlightPhase.HANDOVER) return
        mutableState.value = current.copy(
            phase = AutoFlightPhase.HANDOVER,
            handoverReason = reason,
            exitReason = exitReason ?: current.exitReason,
            errorCode = errorCode ?: current.errorCode,
            errorDescription = errorDescription ?: current.errorDescription,
        )
    }

    companion object {
        /** 启动命令确认超时。 */
        const val START_TIMEOUT_MS = 10_000L

        /** 状态推进停滞超时。 */
        const val PROGRESS_TIMEOUT_MS = 5_000L
    }
}
