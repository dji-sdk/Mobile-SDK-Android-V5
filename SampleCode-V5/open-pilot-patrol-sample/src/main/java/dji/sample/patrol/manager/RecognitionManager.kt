package dji.sample.patrol.manager

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import dji.sample.patrol.data.DetectionTarget
import dji.sample.patrol.data.RecognitionFailureCategory
import dji.sample.patrol.data.RecognitionStage
import dji.sample.patrol.data.ResultCallback
import dji.sample.patrol.data.RouteState
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 识别状态机（design D2/D3）：
 * - 观察 [RouteController.routeState]：进入 `EXECUTING` 且前置满足时开启识别，离开 `EXECUTING`/停止/断连时停止识别；
 * - 严格遵守「先注册 listener 再发令」：先 `addAutoSensingInfoListener` 再 `startAutoSensing`；
 * - 权威开启态以 [AutoSensingStatus.enabled]（`AutoSensingInfo.isAutoSensingEnable == true`）为准，
 *   命令成功但看门狗超时仍未达 true 按开启失败处理（`DEGRADED`，不阻塞航线）；
 * - 命中经 `(targetIndex, targetType)` 去重后投影为 [DetectionTarget] 并触发一次 [onMatch]；
 * - 释放时精确 `removeAutoSensingInfoListener(listener)`，禁止 `clearAll*`/`removeAll*`。
 *
 * 端口/判定/回调均可注入，使状态机可脱离真机与 `dji.*` 在 JVM 上单测。
 */
object RecognitionManager {

    private const val TAG = "PatrolRecognition"

    private val mutableStage = MutableLiveData(RecognitionStage.IDLE)
    val stage: LiveData<RecognitionStage> = mutableStage

    private val mutableFailure = MutableLiveData<RecognitionFailureCategory?>(null)
    val failureCategory: LiveData<RecognitionFailureCategory?> = mutableFailure

    private val mutableError = MutableLiveData<String?>(null)
    val error: LiveData<String?> = mutableError

    /** 等待类提示（如「等待媒体同步完成」），与失败原因分开。 */
    private val mutableNotice = MutableLiveData<String?>(null)
    val matchNotice: LiveData<String?> = mutableNotice

    @Volatile
    var recognitionPort: RecognitionPort? = null

    @Volatile
    var statusProbe: IntelligentStatusProbe? = null

    /** 命中去重判定（生产接到 [PointReporter.isRecorded]）。 */
    @Volatile
    var isTargetRecorded: (DetectionTarget) -> Boolean = { false }

    /** 命中回调（生产接到 [RouteController.handleMatch]）。 */
    @Volatile
    var onMatch: (DetectionTarget) -> Unit = {}

    /** 停止/断连时清理进行中记录的钩子（生产接到 [PointReporter.cancelInFlight]）。 */
    @Volatile
    var onCancelled: () -> Unit = {}

    /** 开启看门狗超时（ms）；命令被接受后此窗口内未收到权威 enable 即判开启失败。 */
    @Volatile
    var startTimeoutMs: Long = 5_000L

    private val mainHandler = Handler(Looper.getMainLooper())
    private val observing = AtomicBoolean(false)

    @Volatile
    private var listener: AutoSensingListener? = null

    @Volatile
    private var started = false

    @Volatile
    private var sensingRequested = false

    @Volatile
    private var inFlightMatch = false

    @Volatile
    private var watchdog: Runnable? = null

    private val routeObserver = Observer<RouteState> { handleRouteState(it) }

    /** 生产入口：观察航线状态。 */
    fun start() {
        if (observing.compareAndSet(false, true)) {
            RouteController.routeState.observeForever(routeObserver)
        }
    }

    /** 释放观察者、订阅与探针（退出/断连）。 */
    fun release() {
        if (observing.compareAndSet(true, false)) {
            RouteController.routeState.removeObserver(routeObserver)
        }
        stop("released")
        statusProbe?.release()
    }

    /**
     * 按航线状态驱动识别：仅 `EXECUTING` 开启，其余（暂停/终态/返回）停止订阅。
     * 对测试暴露为直接入口。
     */
    fun handleRouteState(state: RouteState) = runOnMain {
        if (state == RouteState.EXECUTING) beginRecognition() else endRecognition()
    }

    /** 断连/停止/离开任务：终止进行中命中（不 resume），清理点位并进入 `STOPPED`。 */
    fun stop(reason: String = "stopped") = runOnMain {
        val hadMatch = inFlightMatch
        releaseSubscription(stopSensing = sensingRequested)
        sensingRequested = false
        started = false
        inFlightMatch = false
        cancelWatchdog()
        publish(mutableNotice, null)
        if (hadMatch) {
            publishFailure(RecognitionFailureCategory.CANCELLED, reason)
            runCatching(onCancelled)
        }
        publishStage(RecognitionStage.STOPPED)
    }

    /** 写入命中失败原因，供浮窗状态行展示；原始错误码由调用方落日志。 */
    fun reportExternalFailure(category: RecognitionFailureCategory, message: String) = runOnMain {
        publishFailure(category, message)
    }

    /** 写入/清除等待类提示（如媒体同步进行中）。 */
    fun reportMatchNotice(message: String?) = runOnMain {
        publish(mutableNotice, message)
    }

    /** 命中被门禁拒绝或编排中止：复位在飞标记，离开 MATCHED，避免浮窗停在无出口阶段。 */
    fun abortMatch(reason: String) = runOnMain {
        if (!inFlightMatch && mutableStage.value != RecognitionStage.MATCHED) return@runOnMain
        Log.i(TAG, "match aborted: $reason")
        inFlightMatch = false
        publish(mutableNotice, null)
        publishStage(if (started) RecognitionStage.SENSING else RecognitionStage.IDLE)
    }

    /** 测试注入点：替换端口与回调并复位状态（生产由 [MainApplication] 组装）。 */
    fun configure(
        port: RecognitionPort?,
        probe: IntelligentStatusProbe?,
        recorded: (DetectionTarget) -> Boolean = { false },
        matched: (DetectionTarget) -> Unit = {},
        cancelled: () -> Unit = {},
    ) {
        releaseSubscription(stopSensing = false)
        recognitionPort = port
        statusProbe = probe
        isTargetRecorded = recorded
        onMatch = matched
        onCancelled = cancelled
        started = false
        sensingRequested = false
        inFlightMatch = false
        cancelWatchdog()
        reset()
    }

    /** 生产实现/测试用：观察 [dji.sample.patrol.manager.MediaManager] 无关的点位去重由注入回调决定。 */

    private fun beginRecognition() {
        if (started) return
        val port = recognitionPort
        val probe = statusProbe
        if (port == null || probe == null) {
            publishFailure(RecognitionFailureCategory.RECOGNITION_UNAVAILABLE, "Recognition port is not connected")
            Log.w(TAG, "beginRecognition skipped: port/probe not connected")
            return
        }
        val reason = readyFailureReason(probe)
        if (reason != null) {
            inFlightMatch = false
            clearFailure()
            publishStage(RecognitionStage.IDLE)
            Log.i(TAG, "beginRecognition skipped: $reason")
            return
        }
        started = true
        inFlightMatch = false
        clearFailure()
        publish(mutableNotice, null)
        publishStage(RecognitionStage.IDLE)
        val newListener = AutoSensingListener { event -> onAutoSensingUpdate(event) }
        listener = newListener
        // 严格次序：先注册，再发令。
        port.addAutoSensingInfoListener(newListener)
        sensingRequested = true
        port.startAutoSensing(object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) {
                Log.i(TAG, "startAutoSensing accepted; awaiting authoritative isAutoSensingEnable")
                scheduleWatchdog()
            }

            override fun onFailure(error: Throwable) {
                Log.e(TAG, "startAutoSensing failed: ${error.message}")
                degrade(error.message ?: "startAutoSensing failed")
            }
        })
    }

    private fun onAutoSensingUpdate(event: AutoSensingStatus) = runOnMain {
        if (!started) {
            Log.i(TAG, "autoSensing update ignored: recognition is not started")
            return@runOnMain
        }
        if (event.enabled) {
            cancelWatchdog()
            if (mutableStage.value != RecognitionStage.MATCHED) publishStage(RecognitionStage.SENSING)
            Log.i(TAG, "authoritative isAutoSensingEnable=true")
        } else {
            Log.i(TAG, "authoritative isAutoSensingEnable=false")
        }
        if (!event.enabled || event.targets.isEmpty()) return@runOnMain
        val probe = statusProbe
        val reason = probe?.let { readyFailureReason(it) }
        if (reason != null) {
            Log.i(TAG, "match ignored: $reason")
            return@runOnMain
        }
        if (inFlightMatch) {
            Log.i(TAG, "match ignored: capture already in flight")
            return@runOnMain
        }
        val raw = event.targets.first()
        val target = DetectionTarget(raw.targetIndex, raw.targetType, raw.label)
        if (isTargetRecorded(target)) {
            Log.i(TAG, "match ignored: ${target.dedupKey} already recorded")
            return@runOnMain
        }
        when (val gate = RouteController.matchGate(target)) {
            is MatchGate.Reject -> {
                Log.i(TAG, "match rejected: ${gate.reason}")
                publishFailure(RecognitionFailureCategory.NOT_READY, gate.reason)
                return@runOnMain
            }
            else -> Unit
        }
        inFlightMatch = true
        publishStage(RecognitionStage.MATCHED)
        Log.i(TAG, "match accepted: ${target.dedupKey} label=${target.label}")
        onMatch(target)
    }

    private fun endRecognition() {
        if (!started && listener == null && !inFlightMatch && !sensingRequested) return
        cancelWatchdog()
        val wasMatch = inFlightMatch
        releaseSubscription(stopSensing = sensingRequested)
        sensingRequested = false
        started = false
        if (wasMatch) {
            // 暂停由在飞命中触发：释放订阅但保留 MATCHED，恢复后再重开识别。
            inFlightMatch = false
            Log.i(TAG, "recognition released during in-flight match; keeping MATCHED")
        } else {
            publishStage(RecognitionStage.STOPPED)
        }
    }

    /** 看门狗到点：仍未收到权威 enable 视为开启失败。测试可直接调用。 */
    fun onWatchdogTimeout() = runOnMain {
        if (started && mutableStage.value == RecognitionStage.IDLE && !inFlightMatch) {
            Log.e(TAG, "isAutoSensingEnable watchdog timeout")
            degrade("Auto-sensing did not become enabled before timeout")
        }
    }

    private fun degrade(message: String) {
        publishFailure(RecognitionFailureCategory.RECOGNITION_UNAVAILABLE, message)
        releaseSubscription(stopSensing = sensingRequested)
        sensingRequested = false
        started = false
        inFlightMatch = false
        cancelWatchdog()
        publishStage(RecognitionStage.DEGRADED)
        Log.w(TAG, "recognition degraded: $message")
    }

    private fun readyFailureReason(probe: IntelligentStatusProbe): String? = when {
        !probe.connected -> "Aircraft is not connected"
        probe.runningMission != IntelligentMission.NONE ->
            "Another intelligent mission is running: ${probe.runningMission}"
        probe.supportedMissions.isEmpty() -> "AutoSensing is not supported by this aircraft"
        probe.isIndustry && !probe.isFlying -> "Industry aircraft must be flying for AutoSensing"
        else -> null
    }

    private fun releaseSubscription(stopSensing: Boolean) {
        val port = recognitionPort ?: return
        listener?.let { port.removeAutoSensingInfoListener(it) }
        listener = null
        if (stopSensing) {
            port.stopAutoSensing(object : ResultCallback<Unit> {
                override fun onSuccess(data: Unit) {
                    Log.i(TAG, "stopAutoSensing onSuccess")
                }

                override fun onFailure(error: Throwable) {
                    Log.e(TAG, "stopAutoSensing failed: ${error.message}")
                }
            })
        }
    }

    private fun scheduleWatchdog() {
        cancelWatchdog()
        val runnable = Runnable { onWatchdogTimeout() }
        watchdog = runnable
        mainHandler.postDelayed(runnable, startTimeoutMs)
    }

    private fun cancelWatchdog() {
        watchdog?.let { mainHandler.removeCallbacks(it) }
        watchdog = null
    }

    private fun reset() {
        publishStage(RecognitionStage.IDLE)
        publishFailure(null, null)
        publishError(null)
        publish(mutableNotice, null)
    }

    private fun clearFailure() {
        publishFailure(null, null)
        publishError(null)
    }

    private fun publishFailure(category: RecognitionFailureCategory?, message: String?) {
        publish(mutableFailure, category)
        publishError(message)
    }

    private fun publishError(message: String?) = publish(mutableError, message)

    private fun publishStage(value: RecognitionStage) = publish(mutableStage, value)

    private fun <T> publish(liveData: MutableLiveData<T>, value: T) {
        if (Looper.myLooper() == Looper.getMainLooper()) liveData.value = value else liveData.postValue(value)
    }

    private fun runOnMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action() else mainHandler.post(action)
    }
}
