package com.dji.sample.gasdetection.gas.recognition

import android.util.Log
import com.dji.sample.gasdetection.gas.model.GasMeasurement
import com.dji.sample.gasdetection.gas.model.IdentificationSource
import com.dji.sample.gasdetection.gas.model.VesselReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 原生识别应用服务。
 *
 * 设计意图：把「何时开启识别、什么算锁定、锁定对象如何与采样关联、识别不可用时怎么办」
 * 收敛到一处，并保证所有退出路径都精确释放监听。它不直接读 SDK（经 [RecognitionPort]），
 * 也不直接改任务阶段（由 `PsdkApplication` 在建会话前调用任务阶段机）。
 *
 * 线程约束：对外方法在主线程调用；端口回调统一投递到传入的 [scope]（应用内为主线程调度器）。
 *
 * @param lockHoldMs 新鲜窗口：连续命中的最大间隔，也是锁定后的失锁阈值。
 * @param requiredHits 锁定所需的连续有效次数。
 * @param startTimeoutMs 开启看门狗：命令被接受后等待权威开启态的上限。
 * @param tickIntervalMs 新鲜度检查节拍。
 */
class RecognitionController(
    private val port: RecognitionPort,
    private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
    private val lockHoldMs: Long = VesselTracker.DEFAULT_LOCK_HOLD_MS,
    private val requiredHits: Int = VesselTracker.REQUIRED_HITS,
    private val startTimeoutMs: Long = DEFAULT_START_TIMEOUT_MS,
    private val tickIntervalMs: Long = TICK_INTERVAL_MS,
) {

    private val tracker = VesselTracker(lockHoldMs = lockHoldMs, requiredHits = requiredHits)
    private val mutableState = MutableStateFlow(RecognitionState(requiredHits = requiredHits))
    val state: StateFlow<RecognitionState> = mutableState.asStateFlow()

    private var listener: SensingListener? = null
    private var watchdogJob: Job? = null
    private var tickerJob: Job? = null
    private var sensingRequested = false
    private var taskId: String? = null
    private var vesselSeq = 0
    private var vessel: VesselReference? = null
    private var lastHeldSeconds = -1L

    /**
     * 前置条件判定；返回 `null` 表示可以开始搜索。
     *
     * 识别是否真的可用由开启结果与权威开启态决定，这里只拦「现在不该开」的客观条件。
     */
    fun canStart(preconditions: RecognitionPreconditions): VesselSearchBlock? = when {
        !preconditions.connected -> VesselSearchBlock.AIRCRAFT_DISCONNECTED
        !preconditions.flying -> VesselSearchBlock.NOT_FLYING
        preconditions.missionIdle == null -> VesselSearchBlock.MISSION_UNKNOWN
        !preconditions.missionIdle -> VesselSearchBlock.MISSION_RUNNING
        else -> null
    }

    /**
     * 开启识别会话。
     *
     * 先注册监听再下发命令；命令成功不等于识别已开启，进入 [RecognitionPhase.SENSING]
     * 必须等订阅回调的权威开启态。
     */
    fun start(taskId: String) {
        val phase = mutableState.value.phase
        if (phase == RecognitionPhase.STARTING ||
            phase == RecognitionPhase.SENSING ||
            phase == RecognitionPhase.LOCKED
        ) {
            Log.i(TAG, "start ignored: already $phase")
            return
        }
        releaseSubscription(stopSensing = false)
        cancelJobs()
        tracker.reset()
        vessel = null
        lastHeldSeconds = -1L
        vesselSeq = 0
        this.taskId = taskId
        sensingRequested = true
        val newListener = SensingListener { frame ->
            scope.launch { onFrame(frame) }
        }
        listener = newListener
        mutableState.value = RecognitionState(
            phase = RecognitionPhase.STARTING,
            taskId = taskId,
            requiredHits = requiredHits,
        )
        Log.i(TAG, "search start task=$taskId")
        // 严格次序：先注册，再发令。
        port.addListener(newListener)
        port.start { result ->
            scope.launch { onStartResult(result) }
        }
        watchdogJob = scope.launch {
            delay(startTimeoutMs)
            if (mutableState.value.phase == RecognitionPhase.STARTING) {
                Log.e(TAG, "isAutoSensingEnable timeout")
                degrade(RecognitionFailure.ENABLE_TIMEOUT)
            }
        }
    }

    /** 识别不可用后由飞手重试；先释放既有订阅再重开。 */
    fun retry() {
        if (mutableState.value.phase != RecognitionPhase.DEGRADED) return
        val currentTask = taskId ?: return
        Log.i(TAG, "retry recognition task=$currentTask")
        start(currentTask)
    }

    /** 停止识别并清空状态；Pilot 断开或应用结束时调用。 */
    fun stop() {
        cancelJobs()
        releaseSubscription(stopSensing = sensingRequested)
        sensingRequested = false
        tracker.reset()
        vessel = null
        lastHeldSeconds = -1L
        Log.d(TAG, "released")
        mutableState.value = RecognitionState(
            phase = RecognitionPhase.STOPPED,
            requiredHits = requiredHits,
        )
    }

    /** 复位到初始状态（断连/重连），不保留任务上下文。 */
    fun reset() {
        cancelJobs()
        releaseSubscription(stopSensing = sensingRequested)
        sensingRequested = false
        tracker.reset()
        vessel = null
        taskId = null
        vesselSeq = 0
        lastHeldSeconds = -1L
        mutableState.value = RecognitionState(requiredHits = requiredHits)
    }

    /**
     * 记录一次被拒绝的开始搜索，供入口页展示原因。
     */
    fun reportBlock(block: VesselSearchBlock) {
        Log.i(TAG, "search rejected reason=$block")
        mutableState.value = mutableState.value.copy(block = block)
    }

    /**
     * 为测量补关联：仅在已锁定时写入业务船舶标识，其余情况原样返回。
     */
    fun attachTarget(measurement: GasMeasurement): GasMeasurement {
        val current = vessel ?: return measurement
        if (mutableState.value.phase != RecognitionPhase.LOCKED) return measurement
        return measurement.copy(targetId = current.vesselId)
    }

    /**
     * 确认当前锁定对象的船名或编号。
     *
     * @return `true` 表示已写入；空白内容、未锁定或没有对象时返回 `false`，不改变状态。
     */
    fun confirmVessel(nameOrNumber: String): Boolean {
        val current = vessel ?: return false
        if (mutableState.value.phase != RecognitionPhase.LOCKED) return false
        val trimmed = nameOrNumber.trim()
        if (trimmed.isEmpty()) return false
        val updated = current.copy(nameOrNumber = trimmed, humanConfirmed = true)
        vessel = updated
        mutableState.value = mutableState.value.copy(vessel = updated)
        Log.i(TAG, "vessel confirmed length=${trimmed.length}")
        return true
    }

    private fun onStartResult(result: RecognitionCommandResult) {
        when (result) {
            is RecognitionCommandResult.Accepted -> {
                // 只代表命令被接受，继续等权威开启态。
                Log.d(TAG, "startAutoSensing accepted")
            }

            is RecognitionCommandResult.Failed -> {
                Log.e(
                    TAG,
                    "startAutoSensing failed code=${result.errorCode} desc=${result.description}",
                )
                degrade(RecognitionFailure.START_FAILED)
            }
        }
    }

    private fun onFrame(frame: SensingFrame) {
        if (listener == null) return
        val phase = mutableState.value.phase
        if (phase == RecognitionPhase.IDLE ||
            phase == RecognitionPhase.STOPPED ||
            phase == RecognitionPhase.DEGRADED
        ) {
            return
        }
        if (!frame.enabled) {
            if (phase == RecognitionPhase.SENSING || phase == RecognitionPhase.LOCKED) {
                Log.w(TAG, "authoritative autoSensing disabled")
                degrade(RecognitionFailure.ENABLE_LOST)
            }
            return
        }

        watchdogJob?.cancel()
        watchdogJob = null
        if (phase == RecognitionPhase.STARTING) {
            mutableState.value = mutableState.value.copy(phase = RecognitionPhase.SENSING)
            startTicker()
            Log.i(TAG, "phase=SENSING targets=${frame.targets.size} enabled=true")
        }

        val nowMs = now()
        val update = tracker.onFrame(frame.targets, nowMs)
        val newlyLocked = update.newlyLockedIndex
        if (newlyLocked != null) {
            val target = frame.targets.firstOrNull {
                it.index == newlyLocked && it.kind == TargetKind.BOAT
            }
            lockVessel(newlyLocked, update.hits, target?.rect, nowMs)
            return
        }
        if (mutableState.value.phase == RecognitionPhase.LOCKED) {
            val lockedIndex = mutableState.value.nativeIndex
            val target = frame.targets.firstOrNull {
                it.index == lockedIndex && it.kind == TargetKind.BOAT
            }
            if (target != null) {
                mutableState.value = mutableState.value.copy(
                    targetRect = target.rect,
                    targetSeenAtMs = nowMs,
                )
            }
            publishHeld()
            return
        }
        mutableState.value = mutableState.value.copy(
            nativeIndex = update.candidateIndex,
            hits = update.hits,
            vessel = null,
            heldMs = 0L,
            lost = false,
            targetRect = null,
            targetSeenAtMs = null,
        )
    }

    private fun lockVessel(
        nativeIndex: Int,
        hits: Int,
        rect: TargetRect?,
        seenAtMs: Long,
    ) {
        vesselSeq += 1
        val currentTask = taskId ?: return
        val reference = VesselReference(
            vesselId = "$currentTask#vessel-$vesselSeq",
            taskId = currentTask,
            nameOrNumber = null,
            identificationSource = IdentificationSource.NATIVE_AI,
            humanConfirmed = false,
            timestampMs = now(),
        )
        vessel = reference
        lastHeldSeconds = -1L
        mutableState.value = mutableState.value.copy(
            phase = RecognitionPhase.LOCKED,
            nativeIndex = nativeIndex,
            hits = hits,
            vessel = reference,
            heldMs = 0L,
            lost = false,
            targetRect = rect,
            targetSeenAtMs = seenAtMs,
            failure = null,
        )
        Log.i(TAG, "locked index=$nativeIndex vesselId=${reference.vesselId}")
    }

    private fun publishHeld() {
        val current = vessel ?: return
        val heldSeconds = tracker.heldMs(now()) / 1000
        if (heldSeconds == lastHeldSeconds) return
        lastHeldSeconds = heldSeconds
        mutableState.value = mutableState.value.copy(
            vessel = current,
            heldMs = heldSeconds * 1000,
        )
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(tickIntervalMs)
                onTick()
            }
        }
    }

    private fun onTick() {
        val phase = mutableState.value.phase
        if (phase != RecognitionPhase.SENSING && phase != RecognitionPhase.LOCKED) return
        val update = tracker.tick(now())
        val lost = update.lostIndex
        if (lost != null) {
            Log.w(TAG, "lost index=$lost")
            vessel = null
            lastHeldSeconds = -1L
            mutableState.value = mutableState.value.copy(
                phase = RecognitionPhase.SENSING,
                nativeIndex = null,
                hits = 0,
                vessel = null,
                heldMs = 0L,
                lost = true,
                targetRect = null,
                targetSeenAtMs = null,
            )
            return
        }
        if (phase == RecognitionPhase.LOCKED) {
            publishHeld()
        } else {
            mutableState.value = mutableState.value.copy(
                nativeIndex = update.candidateIndex,
                hits = update.hits,
                targetRect = null,
                targetSeenAtMs = null,
            )
        }
    }

    private fun degrade(cause: RecognitionFailure) {
        cancelJobs()
        releaseSubscription(stopSensing = sensingRequested)
        sensingRequested = false
        tracker.reset()
        vessel = null
        lastHeldSeconds = -1L
        Log.w(TAG, "degraded cause=$cause")
        mutableState.value = mutableState.value.copy(
            phase = RecognitionPhase.DEGRADED,
            nativeIndex = null,
            hits = 0,
            vessel = null,
            heldMs = 0L,
            lost = false,
            targetRect = null,
            targetSeenAtMs = null,
            failure = cause,
        )
    }

    private fun releaseSubscription(stopSensing: Boolean) {
        listener?.let(port::removeListener)
        listener = null
        if (stopSensing) port.stop()
    }

    private fun cancelJobs() {
        watchdogJob?.cancel()
        watchdogJob = null
        tickerJob?.cancel()
        tickerJob = null
    }

    companion object {
        /** 开启看门狗时长；命令被接受后仍未收到权威开启态即视为不可用。 */
        const val DEFAULT_START_TIMEOUT_MS = 5_000L

        /** 新鲜度检查节拍。 */
        const val TICK_INTERVAL_MS = 500L

        private const val TAG = "GasVesselRecognition"
    }
}
