package com.dji.sample.gasdetection.gas.follow

import android.util.Log
import com.dji.sample.gasdetection.gas.autoflight.AutoFlightEvent
import com.dji.sample.gasdetection.gas.autoflight.AutoFlightGateway
import com.dji.sample.gasdetection.gas.autoflight.AutoFlightSafetySnapshot
import com.dji.sample.gasdetection.gas.autoflight.FlightAuthority
import com.dji.sample.gasdetection.gas.autoflight.StartOutcome
import com.dji.sample.gasdetection.gas.autoflight.StopOutcome
import com.dji.sample.gasdetection.gas.autoflight.VirtualStickCommand
import com.dji.sample.gasdetection.gas.recognition.RecognitionPhase
import com.dji.sample.gasdetection.gas.recognition.RecognitionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

/**
 * 跟随阶段的任务阶段写入端口。
 *
 * 设计意图：控制器只负责虚拟摇杆会话，任务阶段仍由 `TaskStore` 唯一写入；抽成极小接口便于
 * 在 JVM 单测里验证「何时进入 FOLLOW / 何时回手动」。
 */
interface FollowStageSink {
    /** 控制权确认后进入 `FOLLOW`；阶段不允许时返回 `false`。 */
    fun beginFollow(): Boolean

    /** 跟随退出（释放确认后）统一回手动。 */
    fun handoverFollow()
}

/**
 * 虚拟摇杆跟随应用服务。
 *
 * 设计意图：把「开启顺序、权威控制权确认、受限控制环、安全退出与释放」收敛到一处，任何异常都
 * 汇聚到同一个释放出口。命令回调只表示命令被接受，是否获得控制权只看 [AutoFlightEvent] 推送的
 * 权威状态；未确认前绝不发送控制量。
 *
 * 线程约束：对外方法在主线程调用；控制环与看门狗运行在传入的 [scope]（应用内为主线程调度器）。
 */
class FollowController(
    private val gateway: AutoFlightGateway,
    private val scope: CoroutineScope,
    private val recognition: () -> RecognitionState,
    private val safety: () -> AutoFlightSafetySnapshot,
    private val stageSink: FollowStageSink,
    /** 自动退出（非飞手主动）释放完成后回调，由接线层切到任务阶段。 */
    private val onStopped: (FollowAbortReason) -> Unit = {},
    private val now: () -> Long = System::currentTimeMillis,
    private val loopIntervalMs: Long = FollowLimits.TICK_MS,
    private val authorityTimeoutMs: Long = AUTHORITY_TIMEOUT_MS,
    private val advancedTimeoutMs: Long = ADVANCED_TIMEOUT_MS,
    private val releaseTimeoutMs: Long = RELEASE_TIMEOUT_MS,
) {

    private val mutableState = MutableStateFlow(FollowState())
    val state: StateFlow<FollowState> = mutableState.asStateFlow()

    private var eventJob: Job? = null
    private var watchdogJob: Job? = null
    private var loopJob: Job? = null
    private var releaseWatchdogJob: Job? = null
    private var afterRelease: (() -> Unit)? = null
    private var notifyStageOnRelease = false
    private var advancedRequested = false
    private var sessionEnabled = false

    /** 开始收集网关事件；重复调用幂等。 */
    fun start() {
        if (eventJob != null) return
        eventJob = scope.launch {
            gateway.events.collect { event ->
                when (event) {
                    is AutoFlightEvent.AuthorityChanged -> onAuthority(event)
                    is AutoFlightEvent.SafetyChanged -> onSafety()
                    else -> Unit
                }
            }
        }
    }

    /** 停止事件收集并清空状态（Pilot 断开/应用结束）。 */
    fun reset() {
        cancelAllJobs()
        eventJob?.cancel()
        eventJob = null
        afterRelease = null
        advancedRequested = false
        sessionEnabled = false
        mutableState.value = FollowState()
    }

    /**
     * 由飞手确认后开始建立虚拟摇杆会话。
     *
     * @return `false` 表示当前已有会话在进行中。
     */
    fun arm(taskId: String): Boolean {
        if (mutableState.value.isBusy) return false
        advancedRequested = false
        sessionEnabled = false
        mutableState.value = FollowState(
            phase = FollowPhase.ARMING,
            taskId = taskId,
            authority = safety().authority,
        )
        Log.i(TAG, "follow arm request task=$taskId")
        gateway.enableVirtualStick { result ->
            scope.launch { onEnableResult(result) }
        }
        watchdogJob = scope.launch {
            delay(authorityTimeoutMs)
            if (mutableState.value.phase == FollowPhase.ARMING && !advancedRequested) {
                Log.e(TAG, "control authority not confirmed")
                abort(FollowAbortReason.AUTHORITY_TIMEOUT)
            }
        }
        return true
    }

    /**
     * 飞手主动退出（或任务结束）。
     *
     * @param after 释放确认后执行的动作；为空时使用自动退出回调（回手动）。
     */
    fun exit(reason: FollowAbortReason, after: (() -> Unit)? = null) {
        val phase = mutableState.value.phase
        if (phase == FollowPhase.IDLE ||
            phase == FollowPhase.STOPPED ||
            phase == FollowPhase.ABORTED ||
            phase == FollowPhase.RELEASING
        ) {
            return
        }
        afterRelease = after
        // 飞手主动退出时，无论是否已进入控制环，都在释放后交还手动；建立失败由 abort 保持搜索阶段。
        notifyStageOnRelease = phase == FollowPhase.ACTIVE || phase == FollowPhase.ARMING
        Log.w(TAG, "follow exit reason=$reason")
        beginRelease(reason)
    }

    private fun onEnableResult(result: StartOutcome) {
        when (result) {
            is StartOutcome.Accepted -> {
                sessionEnabled = true
                Log.d(TAG, "enable accepted, waiting authority")
            }

            is StartOutcome.Failed -> {
                Log.e(TAG, "enable failed code=${result.errorCode} desc=${result.description}")
                abort(FollowAbortReason.ENABLE_FAILED)
            }
        }
    }

    private fun onAuthority(event: AutoFlightEvent.AuthorityChanged) {
        if (mutableState.value.phase != FollowPhase.RELEASING) {
            mutableState.value = mutableState.value.copy(authority = event.authority)
        }
        when (mutableState.value.phase) {
            FollowPhase.ARMING -> onAuthorityWhileArming(event)
            FollowPhase.ACTIVE -> {
                if (!event.virtualStickEnabled || event.authority != FlightAuthority.MSDK) {
                    Log.w(TAG, "authority lost: authority=${event.authority} enabled=${event.virtualStickEnabled}")
                    exit(FollowAbortReason.CONTROL_AUTHORITY_LOST)
                }
            }

            FollowPhase.RELEASING -> {
                if (!event.virtualStickEnabled) {
                    finishRelease(mutableState.value.reason ?: FollowAbortReason.USER_EXIT, confirmed = true)
                }
            }

            else -> Unit
        }
    }

    private fun onAuthorityWhileArming(event: AutoFlightEvent.AuthorityChanged) {
        if (!event.virtualStickEnabled) {
            if (event.authority == FlightAuthority.OSDK ||
                event.authority == FlightAuthority.AIRPORT ||
                event.authority == FlightAuthority.AUTO_TEST
            ) {
                abort(FollowAbortReason.CONTROL_AUTHORITY_LOST)
            }
            return
        }
        if (event.authority != FlightAuthority.MSDK) {
            abort(FollowAbortReason.CONTROL_AUTHORITY_LOST)
            return
        }
        if (event.virtualStickAdvancedMode) {
            advancedRequested = true
            activate()
            return
        }
        if (!advancedRequested) {
            Log.i(TAG, "authority=MSDK vsEnabled=true")
            advancedRequested = true
            watchdogJob?.cancel()
            gateway.setVirtualStickAdvancedMode(true)
            watchdogJob = scope.launch {
                delay(advancedTimeoutMs)
                if (mutableState.value.phase == FollowPhase.ARMING) {
                    Log.e(TAG, "advanced mode not confirmed")
                    abort(FollowAbortReason.ADVANCED_TIMEOUT)
                }
            }
            return
        }
    }

    private fun activate() {
        watchdogJob?.cancel()
        watchdogJob = null
        if (!stageSink.beginFollow()) {
            Log.e(TAG, "stage rejected follow")
            abort(FollowAbortReason.STAGE_REJECTED)
            return
        }
        mutableState.value = mutableState.value.copy(
            phase = FollowPhase.ACTIVE,
            reason = null,
            releaseConfirmed = false,
        )
        startLoop()
        Log.d(TAG, "loop start")
    }

    private fun startLoop() {
        loopJob?.cancel()
        loopJob = scope.launch {
            while (isActive) {
                delay(loopIntervalMs)
                tick()
            }
        }
    }

    private fun tick() {
        if (mutableState.value.phase != FollowPhase.ACTIVE) return
        val snapshot = safety()
        val safetyReason = safetyReason(snapshot)
        if (safetyReason != null) {
            Log.w(TAG, "safety exit=$safetyReason")
            exit(safetyReason)
            return
        }
        if (!snapshot.virtualStickEnabled || snapshot.authority != FlightAuthority.MSDK) {
            exit(FollowAbortReason.CONTROL_AUTHORITY_LOST)
            return
        }
        val current = recognition()
        if (current.phase != RecognitionPhase.LOCKED || current.vessel == null) {
            exit(FollowAbortReason.TARGET_LOST)
            return
        }
        val command = FollowControlLaw.compute(
            rect = current.targetRect,
            seenAtMs = current.targetSeenAtMs,
            nowMs = now(),
        )
        if (command == null) {
            val rect = current.targetRect
            val reason = if (rect != null && !FollowControlLaw.isNormalized(rect)) {
                FollowAbortReason.INVALID_RECT
            } else {
                FollowAbortReason.TARGET_STALE
            }
            Log.w(TAG, "control input invalid: $reason")
            exit(reason)
            return
        }
        gateway.sendVirtualStickCommand(command.toVirtualStickCommand())
    }

    private fun onSafety() {
        val phase = mutableState.value.phase
        if (phase != FollowPhase.ARMING && phase != FollowPhase.ACTIVE) return
        val reason = safetyReason(safety()) ?: return
        if (phase == FollowPhase.ARMING) {
            abort(reason)
        } else {
            exit(reason)
        }
    }

    private fun safetyReason(snapshot: AutoFlightSafetySnapshot): FollowAbortReason? = when {
        !snapshot.aircraftConnected -> FollowAbortReason.DISCONNECTED
        snapshot.lowBattery || snapshot.criticalLowBattery -> FollowAbortReason.LOW_BATTERY
        !snapshot.gpsReady -> FollowAbortReason.GPS_WEAK
        snapshot.fcTakeover -> FollowAbortReason.FC_TAKEOVER
        else -> null
    }

    private fun abort(reason: FollowAbortReason) {
        val phase = mutableState.value.phase
        if (phase == FollowPhase.IDLE ||
            phase == FollowPhase.STOPPED ||
            phase == FollowPhase.ABORTED ||
            phase == FollowPhase.RELEASING
        ) {
            return
        }
        afterRelease = null
        notifyStageOnRelease = false
        Log.w(TAG, "follow abort reason=$reason")
        beginRelease(reason)
    }

    private fun beginRelease(reason: FollowAbortReason) {
        cancelLoopAndWatchdogs()
        val snapshot = safety()
        val shouldRelease = sessionEnabled ||
            snapshot.virtualStickEnabled ||
            snapshot.virtualStickAdvancedMode
        mutableState.value = mutableState.value.copy(
            phase = FollowPhase.RELEASING,
            reason = reason,
        )
        if (!shouldRelease) {
            finishRelease(reason, confirmed = true)
            return
        }
        if (snapshot.virtualStickAdvancedMode || advancedRequested) {
            gateway.sendVirtualStickCommand(VirtualStickCommand.NEUTRAL)
            Log.d(TAG, "neutral sent")
        }
        gateway.setVirtualStickAdvancedMode(false)
        Log.d(TAG, "advanced=false")
        if (sessionEnabled) {
            gateway.disableVirtualStick { result ->
                scope.launch {
                    when (result) {
                        is StopOutcome.Accepted -> startReleaseWatchdog(reason)
                        is StopOutcome.Failed -> {
                            Log.e(
                                TAG,
                                "disable failed code=${result.errorCode} desc=${result.description}",
                            )
                            finishRelease(reason, confirmed = false)
                        }
                    }
                }
            }
        } else {
            startReleaseWatchdog(reason)
        }
    }

    private fun startReleaseWatchdog(reason: FollowAbortReason) {
        releaseWatchdogJob?.cancel()
        releaseWatchdogJob = scope.launch {
            delay(releaseTimeoutMs)
            Log.e(TAG, "release timeout")
            finishRelease(reason, confirmed = false)
        }
    }

    private fun finishRelease(reason: FollowAbortReason, confirmed: Boolean) {
        releaseWatchdogJob?.cancel()
        releaseWatchdogJob = null
        if (mutableState.value.phase != FollowPhase.RELEASING) return
        Log.d(TAG, "release confirmed=$confirmed")
        val notifyStage = notifyStageOnRelease
        mutableState.value = mutableState.value.copy(
            phase = if (notifyStage) FollowPhase.STOPPED else FollowPhase.ABORTED,
            reason = reason,
            releaseConfirmed = confirmed,
        )
        sessionEnabled = false
        advancedRequested = false
        val callback = afterRelease
        afterRelease = null
        if (callback != null) {
            callback()
        } else if (notifyStage) {
            onStopped(reason)
        }
    }

    private fun cancelLoopAndWatchdogs() {
        loopJob?.cancel()
        loopJob = null
        Log.d(TAG, "loop stop")
        watchdogJob?.cancel()
        watchdogJob = null
        releaseWatchdogJob?.cancel()
        releaseWatchdogJob = null
    }

    private fun cancelAllJobs() {
        cancelLoopAndWatchdogs()
    }

    private fun FollowCommand.toVirtualStickCommand(): VirtualStickCommand = VirtualStickCommand(
        roll = roll,
        pitch = pitch,
        yaw = yaw,
        verticalThrottle = verticalThrottle,
    )

    companion object {
        /** 控制权确认超时。 */
        const val AUTHORITY_TIMEOUT_MS = 5_000L

        /** 高级模式确认超时。 */
        const val ADVANCED_TIMEOUT_MS = 3_000L

        /** 释放确认超时。 */
        const val RELEASE_TIMEOUT_MS = 5_000L

        private const val TAG = "GasVsFollow"
    }
}
