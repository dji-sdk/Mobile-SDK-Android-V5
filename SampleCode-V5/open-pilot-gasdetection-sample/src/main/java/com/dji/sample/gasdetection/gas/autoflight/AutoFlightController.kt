package com.dji.sample.gasdetection.gas.autoflight

import com.dji.sample.gasdetection.gas.task.GasJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 任务阶段的写入端口。
 *
 * 设计意图：控制器只负责飞行会话，任务阶段仍由 [com.dji.sample.gasdetection.gas.task.TaskStore]
 * 唯一写入。抽出这个极小接口是为了让控制器能在纯 JVM 单测里验证「何时进入 FLYTO / 何时回手动」，
 * 不必启动网络层或 Android 环境。
 */
interface AutoFlightStageSink {
    /** 请求进入 `FLYTO`；门控或阶段不允许时返回 `false`。 */
    fun beginAutoFlight(): Boolean

    /** 自动阶段进入接管，回到 `MANUAL`。 */
    fun handoverAutoFlight()
}

/**
 * 自动飞行应用服务。
 *
 * 设计意图：把网关事件、看门狗与任务阶段串起来，并保证所有接管路径都汇聚到同一个出口。
 * 它不直接读 SDK、不直接改任务状态；所有 SDK 交互经 [AutoFlightGateway]，阶段写入经
 * [AutoFlightStageSink]。
 *
 * 线程约束：方法在主线程调用；协程运行在传入的 [scope]（应用内为主线程调度器）。
 */
class AutoFlightController(
    private val gateway: AutoFlightGateway,
    private val session: AutoFlightSession,
    private val stageSink: AutoFlightStageSink,
    private val scope: CoroutineScope,
    private val tickIntervalMs: Long = TICK_INTERVAL_MS,
    private val now: () -> Long = System::currentTimeMillis,
) {

    val state: StateFlow<AutoFlightState> = session.state

    private var eventJob: Job? = null
    private var watchdogJob: Job? = null

    /** 注册网关监听并启动看门狗；重复调用幂等。 */
    fun startMonitoring() {
        if (eventJob != null) return
        gateway.startMonitoring()
        eventJob = scope.launch {
            gateway.events.collect { event -> onEvent(event) }
        }
        watchdogJob = scope.launch {
            while (true) {
                delay(tickIntervalMs)
                val timeout = session.checkTimeout(now()) ?: continue
                val reason = when (timeout) {
                    AutoFlightTimeout.START_NOT_CONFIRMED -> HandoverReason.START_NOT_CONFIRMED
                    AutoFlightTimeout.STATE_NOT_ADVANCED -> HandoverReason.STATE_NOT_ADVANCED
                }
                handover(reason)
            }
        }
    }

    /** 停止监听与看门狗，并清空会话；Pilot 断开时调用。 */
    fun stop() {
        eventJob?.cancel()
        eventJob = null
        watchdogJob?.cancel()
        watchdogJob = null
        gateway.stopMonitoring()
        session.reset()
    }

    /** 门控与展示用的快照；目标点来自当前任务，网关不理解业务任务。 */
    fun safetySnapshot(job: GasJob?): AutoFlightSafetySnapshot {
        val target = job?.target?.let {
            AutoFlightTarget(
                latitude = it.latitude,
                longitude = it.longitude,
                altitudeM = it.altitudeM,
            )
        }
        return gateway.snapshot().copy(target = target)
    }

    /**
     * 飞手在二次确认后请求启动。
     *
     * @return `true` 表示已下发命令并进入启动等待；`false` 表示阶段或门控不允许。
     */
    fun requestStart(target: AutoFlightTarget, heightM: Int): Boolean {
        if (!target.isComplete || heightM <= 0) return false
        // 上一次会话必须先确认接管或收尾，避免带着旧接管态启动。
        if (session.state.value.phase != AutoFlightPhase.IDLE) return false
        if (!stageSink.beginAutoFlight()) return false
        if (!session.onStartCommandSent(now())) {
            stageSink.handoverAutoFlight()
            return false
        }
        gateway.startFlyTo(target, heightM) { outcome ->
            session.onStartOutcome(outcome, now())
        }
        return true
    }

    /** 飞手主动退出自动：只交还控制，不下发停止命令；任务阶段等飞手确认接管后回手动。 */
    fun exitAuto() {
        if (!session.state.value.isAutoActive) return
        session.onHandover(HandoverReason.USER_EXIT)
        gateway.releaseVirtualStickIfOwned { }
    }

    /** 飞手在二次确认后触发紧急停止；结果只记录，终态仍等状态回调。 */
    fun requestEmergencyStop() {
        gateway.stopFlyTo { outcome ->
            when (outcome) {
                is StopOutcome.Accepted -> session.onStopFailed(null, null)
                is StopOutcome.Failed -> {
                    session.onStopFailed(outcome.errorCode, outcome.description)
                    handover(HandoverReason.STOP_FAILED)
                }
            }
        }
    }

    /** 飞手确认接管：会话回到空闲，任务阶段离开 `FLYTO` 回到手动。 */
    fun confirmHandover() {
        if (session.state.value.phase != AutoFlightPhase.HANDOVER) return
        session.onHandoverConfirmed()
        stageSink.handoverAutoFlight()
    }

    /**
     * 关闭当前自动会话但不改任务阶段。
     *
     * 用于「到达后结束任务」或「到达后转手动」：这类收尾由任务阶段自己迁移，控制器只负责
     * 清掉自动会话，避免残留接管态影响下一次启动。
     */
    fun closeSession() {
        session.reset()
    }

    private fun onEvent(event: AutoFlightEvent) {
        when (event) {
            is AutoFlightEvent.FlyToStageChanged -> {
                session.onFlyToStage(event.stage, event.exitReason, now())
            }

            is AutoFlightEvent.AuthorityChanged -> session.onAuthority(
                authority = event.authority,
                virtualStickEnabled = event.virtualStickEnabled,
                virtualStickAdvancedMode = event.virtualStickAdvancedMode,
            )

            is AutoFlightEvent.SafetyChanged -> evaluateSafety()
        }
    }

    /** 飞行中安全项变化：只对已启动的会话生效，到达后不再改变阶段。 */
    private fun evaluateSafety() {
        val phase = session.state.value.phase
        if (phase != AutoFlightPhase.STARTING && phase != AutoFlightPhase.FLYING) return
        val snapshot = gateway.snapshot()
        val reason = when {
            !snapshot.aircraftConnected -> HandoverReason.DISCONNECTED
            snapshot.lowBattery || snapshot.criticalLowBattery -> HandoverReason.BATTERY_LOW
            !snapshot.gpsReady -> HandoverReason.GPS_LOST
            snapshot.fcTakeover -> HandoverReason.FC_TAKEOVER
            else -> null
        } ?: return
        handover(reason)
    }

    private fun handover(reason: HandoverReason) {
        session.onHandover(reason)
    }

    companion object {
        /** 看门狗检查节奏；不对应任何状态确认，只用于触发超时判定。 */
        const val TICK_INTERVAL_MS = 500L
    }
}
