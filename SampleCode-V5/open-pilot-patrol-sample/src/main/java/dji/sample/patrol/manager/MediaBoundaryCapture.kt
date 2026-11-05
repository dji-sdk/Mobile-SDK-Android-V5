package dji.sample.patrol.manager

import android.os.Handler
import android.os.Looper
import android.util.Log
import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.key.DJICameraKey
import dji.sdk.keyvalue.key.DJIKeyInfo
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.camera.CameraStorageLocation
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.datacenter.media.MediaFileFilter
import dji.v5.manager.datacenter.media.MediaFileListDataSource
import dji.v5.manager.datacenter.media.MediaFileListState
import dji.v5.manager.datacenter.media.MediaFileListStateListener
import dji.v5.manager.datacenter.media.PullMediaFileListParam
import dji.v5.manager.interfaces.ICameraStreamManager
import dji.v5.manager.interfaces.IKeyManager
import dji.v5.manager.interfaces.IMediaManager
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 单次「启动前照片起始边界采集」会话。
 *
 * 设计意图：把「进入媒体管理模式 → 拉取照片列表 → 退出媒体管理模式」这串串行异步 SDK 调用
 * 收敛到一个对象，使成功与失败路径共用同一条清理漏斗——停止拉取、退出回放、精确解订阅只写一次，
 * 避免每个失败分支各自遗漏（本次故障正是「未进入媒体模式、也未成对释放」这类遗漏）。
 *
 * 能力契约：
 * - [start] 只交付一次结果：成功给出照片文件索引快照（真实空相册返回空集合），
 *   失败给出带分类的 [MediaBoundaryFailure]。
 * - 所有状态变更都在主线程串行推进；SDK 回调先切回主线程再参与判定。
 * - `enable` / `disable` 的业务终态以 `DJICameraKey.KeyIsPlayingBack` 为准，
 *   命令 `onSuccess` 只代表命令被接受。
 * - 列表终态 = 拉取回调 `onSuccess` 且列表状态为 `MediaFileListState.UP_TO_DATE`；
 *   拉取失败产生的空列表永远不会被当成真实空相册。
 * - 依赖全部经构造参数注入，便于在 JVM 侧用 fake 验证顺序。
 */
internal class MediaBoundaryCapture(
    private val mediaManager: IMediaManager,
    private val keyManager: IKeyManager,
    private val cameraStreamManager: ICameraStreamManager,
    private val cameraIndex: ComponentIndexType = ComponentIndexType.LEFT_OR_MAIN,
    private val stepTimeoutMillis: Long = STEP_TIMEOUT_MS,
    private val listTimeoutMillis: Long = LIST_TIMEOUT_MS,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val playingBackKey = KeyTools.createKey(DJICameraKey.KeyIsPlayingBack, cameraIndex)
    private val playbackListenHolder = Any()

    /** 结果交付回调；交付后不再调用。 */
    private var completion: ((Result<List<Int>>) -> Unit)? = null

    /** 由调用方注入的工单相机目录配置步骤；前置检查通过后才执行。 */
    private var configureFolder: (((Result<Unit>) -> Unit) -> Unit)? = null

    /** 终态是否已交付；交付后迟到回调只做自身清理。 */
    private var settled = false

    /** 相机可用性探测的一次性订阅；清理时必须移除。 */
    private var cameraListListener: ICameraStreamManager.AvailableCameraUpdatedListener? = null

    /** 是否已发出 `enable`；清理时据此判断是否需要确认退出回放。 */
    private var enableIssued = false

    /** 是否已发出本次列表拉取；清理时必须停止自有拉取任务。 */
    private var pullIssued = false

    /** 本次建立的列表状态订阅。 */
    private var listStateListener: MediaFileListStateListener? = null

    /** 列表状态订阅是否已建立。 */
    private var listStateSubscribed = false

    /** 最近一次看到的列表状态。 */
    private var listState: MediaFileListState? = null

    /** 回放状态订阅是否已建立。 */
    private var playbackSubscribed = false

    /** `KeyIsPlayingBack` 最近一次权威取值；null 表示尚未收到推送。 */
    private var playingBack: Boolean? = null

    /** 当前步骤等待外部信号收敛时挂入的判定；步骤结束或失败必须清空。 */
    private var settleHook: (() -> Unit)? = null

    /** 当前步骤的超时任务。 */
    private var stepTimeoutTask: Runnable? = null

    /** 清理阶段确认退出回放的超时任务。 */
    private var cleanupTimeoutTask: Runnable? = null

    /**
     * 启动一次采集并交付唯一结果。
     *
     * [configureFolder] 在**前置检查通过之后**执行，用于设置工单相机目录：相机不可用时
     * 绝不先改动相机状态，失败归类为 [MediaBoundaryFailureCategory.PREREQUISITE]。
     * 重复调用（已交付或已在途）是空操作，由调用方（`MediaManager`）的在途标记负责拒绝并发。
     */
    fun start(
        configureFolder: ((Result<Unit>) -> Unit) -> Unit,
        callback: (Result<List<Int>>) -> Unit,
    ) {
        mainHandler.post {
            if (settled || completion != null) return@post
            this.configureFolder = configureFolder
            completion = callback
            Log.i(TAG, "media-prepare: start camera=$cameraIndex")
            checkPrerequisites()
        }
    }

    /**
     * 使在途采集失效（断连、任务切换、管理器销毁）。
     *
     * 收敛后仍会完成清理；迟到回调不得再交付成功。
     */
    fun cancel(reason: Throwable) {
        mainHandler.post { fail(MediaBoundaryFailureCategory.CANCELLED, reason) }
    }

    /** 前置检查第 1 步：确认目标相机在用相机列表里，避免硬编码索引带来的静默错相机。 */
    private fun checkPrerequisites() {
        armStepTimeout(MediaBoundaryFailureCategory.PREREQUISITE, "prerequisites", stepTimeoutMillis)
        val once = AtomicBoolean(false)
        lateinit var listener: ICameraStreamManager.AvailableCameraUpdatedListener
        listener = object : ICameraStreamManager.AvailableCameraUpdatedListener {
            override fun onAvailableCameraUpdated(availableCameraList: List<ComponentIndexType>) {
                if (!once.compareAndSet(false, true)) return
                mainHandler.post {
                    releaseCameraListListener()
                    if (cameraIndex !in availableCameraList) {
                        Log.w(
                            TAG,
                            "media-prepare: prerequisite failed reason=camera unavailable " +
                                "camera=$cameraIndex available=$availableCameraList",
                        )
                        prerequisiteFailure("主相机不可用")
                    } else {
                        checkStatusKeys()
                    }
                }
            }
        }
        cameraListListener = listener
        try {
            cameraStreamManager.addAvailableCameraUpdatedListener(listener)
        } catch (error: Throwable) {
            cameraListListener = null
            prerequisiteFailure("订阅相机列表失败: $error")
        }
    }

    /** 前置检查第 2 步：逐个读取权威状态；任何「未知」都按不满足处理，不放行。 */
    private fun checkStatusKeys() {
        readValue("KeyIsFlying", FlightControllerKey.KeyIsFlying, null) { flying ->
            if (flying != false) {
                prerequisiteFailure("飞行状态未知或飞行中")
                return@readValue
            }
            readValue("KeyConnection", DJICameraKey.KeyConnection, cameraIndex) { connected ->
                if (connected != true) {
                    prerequisiteFailure("主相机未连接")
                    return@readValue
                }
                readValue("KeyIsShootingPhoto", CameraKey.KeyIsShootingPhoto, cameraIndex) { shooting ->
                    if (shooting != false) {
                        prerequisiteFailure("相机正在拍照或状态未知")
                        return@readValue
                    }
                    readValue("KeyIsRecording", CameraKey.KeyIsRecording, cameraIndex) { recording ->
                        if (recording != false) {
                            prerequisiteFailure("相机正在录像或状态未知")
                            return@readValue
                        }
                        readValue("KeyIsPlayingBack", DJICameraKey.KeyIsPlayingBack, cameraIndex) { playing ->
                            if (playing != false) {
                                prerequisiteFailure("回放状态未知或已在回放")
                                return@readValue
                            }
                            Log.i(TAG, "media-prepare: prerequisites ok camera=$cameraIndex")
                            configureJobFolder()
                        }
                    }
                }
            }
        }
    }

    /**
     * 前置检查通过后才配置工单相机目录。
     *
     * 该步骤由调用方注入（目录属于工单状态，由 `MediaManager` 持有）；相机不可用时前置检查
     * 已经失败，因此这里不会去触碰相机 Key。失败归类为 `PREREQUISITE` 并保留原始错误。
     */
    private fun configureJobFolder() {
        val configure = configureFolder
        if (configure == null) {
            prerequisiteFailure("未提供工单目录配置步骤")
            return
        }
        armStepTimeout(MediaBoundaryFailureCategory.PREREQUISITE, "folder", stepTimeoutMillis)
        val delivered = AtomicBoolean(false)
        try {
            configure { result ->
                mainHandler.post {
                    if (!delivered.compareAndSet(false, true)) return@post
                    cancelStepTimeout()
                    val error = result.exceptionOrNull()
                    if (error != null) {
                        Log.w(TAG, "media-prepare: folder failed error=$error")
                        fail(MediaBoundaryFailureCategory.PREREQUISITE, error)
                    } else {
                        Log.i(TAG, "media-prepare: folder configured")
                        configureDataSource()
                    }
                }
            }
        } catch (error: Throwable) {
            if (delivered.compareAndSet(false, true)) {
                fail(MediaBoundaryFailureCategory.PREREQUISITE, error)
            }
        }
    }

    /** 数据源必须显式给出相机索引与存储位置，且先于 `enable`；不依赖 Builder 的隐式默认值。 */
    private fun configureDataSource() {
        try {
            mediaManager.setMediaFileDataSource(
                MediaFileListDataSource.Builder()
                    .setIndexType(cameraIndex)
                    .setLocation(CameraStorageLocation.SDCARD)
                    .build(),
            )
        } catch (error: Throwable) {
            fail(MediaBoundaryFailureCategory.PREREQUISITE, error)
            return
        }
        subscribeListState()
        subscribePlaybackState()
        enterMediaMode()
    }

    /** 先订阅列表状态再发令，否则会错过 `UP_TO_DATE`。 */
    private fun subscribeListState() {
        val listener = MediaFileListStateListener { state ->
            mainHandler.post {
                listState = state
                Log.i(TAG, "media-prepare: list state=$state")
                settleHook?.invoke()
            }
        }
        listStateListener = listener
        listStateSubscribed = true
        try {
            mediaManager.addMediaFileListStateListener(listener)
        } catch (error: Throwable) {
            listStateListener = null
            listStateSubscribed = false
            fail(MediaBoundaryFailureCategory.PREREQUISITE, error)
        }
    }

    /** 回放状态订阅：`enable` / `disable` 的权威终态来源；必须在发令前建立。 */
    private fun subscribePlaybackState() {
        playbackSubscribed = true
        try {
            keyManager.listen(
                playingBackKey,
                playbackListenHolder,
                true,
                object : CommonCallbacks.KeyListener<Boolean> {
                    override fun onValueChange(oldValue: Boolean?, newValue: Boolean?) {
                        mainHandler.post {
                            playingBack = newValue
                            Log.i(TAG, "media-prepare: playingBack=$newValue")
                            settleHook?.invoke()
                        }
                    }
                },
            )
        } catch (error: Throwable) {
            playbackSubscribed = false
            fail(MediaBoundaryFailureCategory.PREREQUISITE, error)
        }
    }

    /** 进入媒体管理模式：命令被接受且 `KeyIsPlayingBack == true` 才算进入。 */
    private fun enterMediaMode() {
        var accepted = false
        armStepTimeout(MediaBoundaryFailureCategory.ENTER, "enter", stepTimeoutMillis)

        fun settleIfReady() {
            if (!accepted || playingBack != true) return
            clearSettleHook()
            cancelStepTimeout()
            Log.i(TAG, "media-prepare: enter ok playingBack=true")
            pullList()
        }

        settleHook = { settleIfReady() }
        enableIssued = true
        try {
            mediaManager.enable(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    accepted = true
                    mainHandler.post { settleIfReady() }
                }

                override fun onFailure(error: IDJIError) {
                    Log.w(TAG, "media-prepare: enter failed error=$error")
                    mainHandler.post { fail(MediaBoundaryFailureCategory.ENTER, sdkError(error)) }
                }
            })
        } catch (error: Throwable) {
            fail(MediaBoundaryFailureCategory.ENTER, error)
        }
    }

    /**
     * 拉取照片列表并等待双信号终态。
     *
     * 列表状态 `UP_TO_DATE` 只表示「更新动作结束」，失败路径同样会到达该状态；
     * 因此必须与拉取回调 `onSuccess` 共同成立，且绝不把失败产生的空列表当成真实空相册。
     */
    private fun pullList() {
        var pullFinished = false
        var pullError: Throwable? = null
        var delivered = false
        armStepTimeout(MediaBoundaryFailureCategory.FETCH, "fetch", listTimeoutMillis)

        fun settleIfReady() {
            if (delivered || !pullFinished) return
            val error = pullError
            if (error != null) {
                delivered = true
                clearSettleHook()
                cancelStepTimeout()
                Log.w(TAG, "media-prepare: list failed state=$listState error=$error")
                fail(MediaBoundaryFailureCategory.FETCH, error)
                return
            }
            if (!hasUpToDate()) return
            delivered = true
            clearSettleHook()
            cancelStepTimeout()
            val indices = runCatching {
                mediaManager.mediaFileListData?.data.orEmpty().map { it.fileIndex }
            }.getOrElse { emptyList() }
            Log.i(
                TAG,
                "media-prepare: list ok count=${indices.size} " +
                    "startCursor=${indices.maxOrNull() ?: 0} state=UP_TO_DATE",
            )
            stopPull()
            exitMediaMode(indices)
        }

        settleHook = { settleIfReady() }
        pullIssued = true
        try {
            mediaManager.pullMediaFileListFromCamera(
                PullMediaFileListParam.Builder()
                    .filter(MediaFileFilter.PHOTO)
                    .mediaFileIndex(1)
                    .count(-1)
                    .build(),
                object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        mainHandler.post {
                            pullFinished = true
                            settleIfReady()
                        }
                    }

                    override fun onFailure(error: IDJIError) {
                        mainHandler.post {
                            pullError = sdkError(error)
                            pullFinished = true
                            settleIfReady()
                        }
                    }
                },
            )
        } catch (error: Throwable) {
            pullIssued = false
            fail(MediaBoundaryFailureCategory.FETCH, error)
        }
    }

    /**
     * 退出媒体管理模式：回放与拍照/录像互斥，启动航线前必须确认退出。
     * 订阅在退出确认之后才释放。
     */
    private fun exitMediaMode(indices: List<Int>) {
        var accepted = false
        var delivered = false
        armStepTimeout(MediaBoundaryFailureCategory.EXIT, "exit", stepTimeoutMillis)

        fun settleIfReady() {
            if (delivered || !accepted || playingBack != false) return
            delivered = true
            clearSettleHook()
            cancelStepTimeout()
            Log.i(TAG, "media-prepare: exit ok playingBack=false")
            releaseSubscriptions()
            Log.i(TAG, "media-prepare: cleanup done")
            if (settled) return
            settled = true
            completion?.invoke(Result.success(indices))
        }

        settleHook = { settleIfReady() }
        try {
            mediaManager.disable(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    accepted = true
                    mainHandler.post { settleIfReady() }
                }

                override fun onFailure(error: IDJIError) {
                    Log.w(TAG, "media-prepare: exit failed error=$error")
                    mainHandler.post { fail(MediaBoundaryFailureCategory.EXIT, sdkError(error)) }
                }
            })
        } catch (error: Throwable) {
            fail(MediaBoundaryFailureCategory.EXIT, error)
        }
    }

    /** 列表状态为 `UP_TO_DATE` 时返回 true；推送可能已到达，因此额外读一次权威状态兜底。 */
    private fun hasUpToDate(): Boolean {
        if (listState == MediaFileListState.UP_TO_DATE) return true
        val current = runCatching { mediaManager.mediaFileListState }.getOrNull()
        if (current == MediaFileListState.UP_TO_DATE) {
            listState = current
            return true
        }
        return false
    }

    /** 停止本次拉取任务；只停止自有任务，不影响其他消费者。 */
    private fun stopPull() {
        if (!pullIssued) return
        pullIssued = false
        runCatching { mediaManager.stopPullMediaFileListFromCamera() }
            .onFailure { Log.w(TAG, "media-prepare: stop pull failed error=$it") }
    }

    /** 精确释放本会话建立的订阅；绝不使用 `removeAll*`。 */
    private fun releaseSubscriptions() {
        removeListStateListener()
        cancelPlaybackSubscription()
    }

    private fun removeListStateListener() {
        val listener = listStateListener ?: return
        listStateListener = null
        listStateSubscribed = false
        runCatching { mediaManager.removeMediaFileListStateListener(listener) }
            .onFailure { Log.w(TAG, "media-prepare: remove list listener failed error=$it") }
    }

    private fun cancelPlaybackSubscription() {
        if (!playbackSubscribed) return
        playbackSubscribed = false
        runCatching { keyManager.cancelListen(playingBackKey, playbackListenHolder) }
            .onFailure { Log.w(TAG, "media-prepare: cancel playback listen failed error=$it") }
    }

    private fun releaseCameraListListener() {
        val listener = cameraListListener ?: return
        cameraListListener = null
        runCatching { cameraStreamManager.removeAvailableCameraUpdatedListener(listener) }
            .onFailure { Log.w(TAG, "media-prepare: remove camera listener failed error=$it") }
    }

    /** 失败收敛：先按逆序清理，再交付带分类的失败；清理自身失败作为 suppressed 保留。 */
    private fun fail(category: MediaBoundaryFailureCategory, error: Throwable) {
        if (settled) return
        clearSettleHook()
        cancelStepTimeout()
        val failure = (error as? MediaBoundaryFailure)
            ?: MediaBoundaryFailure(category, "媒体准备失败: $category", error)
        Log.w(TAG, "media-prepare: failed category=${failure.category} error=$error")
        cleanup { cleanupError ->
            if (cleanupError != null) {
                failure.addSuppressed(cleanupError)
                Log.w(TAG, "media-prepare: cleanup unconfirmed error=$cleanupError")
            }
            if (settled) return@cleanup
            settled = true
            completion?.invoke(Result.failure(failure))
        }
    }

    /**
     * 清理漏斗：停止拉取 → 退出回放并确认 → 精确解订阅。
     * 成功与失败路径都走这一条，且顺序固定为逆序。
     */
    private fun cleanup(onDone: (Throwable?) -> Unit) {
        var cleanupError: Throwable? = null
        releaseCameraListListener()
        if (pullIssued) {
            pullIssued = false
            runCatching { mediaManager.stopPullMediaFileListFromCamera() }
                .onFailure { cleanupError = cleanupError ?: it }
        }
        val finish: (Throwable?) -> Unit = { exitError ->
            if (exitError != null) cleanupError = cleanupError ?: exitError
            removeListStateListener()
            cancelPlaybackSubscription()
            if (cleanupError == null) {
                Log.i(TAG, "media-prepare: cleanup done")
            } else {
                Log.w(TAG, "media-prepare: cleanup unconfirmed error=$cleanupError")
            }
            onDone(cleanupError)
        }
        // 只要发出过 enable 且回放状态不是明确的 false，就必须尝试退出并确认。
        if (enableIssued && playingBack != false) disableDuringCleanup(finish) else finish(null)
    }

    /** 清理阶段的退出确认；失败或超时都视为「清理未确认」，交由调用方保守处理。 */
    private fun disableDuringCleanup(onDone: (Throwable?) -> Unit) {
        val done = AtomicBoolean(false)
        var accepted = false

        fun finish(error: Throwable?) {
            if (!done.compareAndSet(false, true)) return
            clearSettleHook()
            cancelCleanupTimeout()
            onDone(error)
        }

        val timeoutTask = Runnable {
            finish(
                MediaBoundaryFailure(
                    MediaBoundaryFailureCategory.EXIT,
                    "退出媒体管理模式确认超时，清理未确认",
                ),
            )
        }
        cleanupTimeoutTask = timeoutTask
        mainHandler.postDelayed(timeoutTask, stepTimeoutMillis)

        fun settleIfReady() {
            if (accepted && playingBack == false) finish(null)
        }

        settleHook = { settleIfReady() }
        try {
            mediaManager.disable(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    accepted = true
                    mainHandler.post { settleIfReady() }
                }

                override fun onFailure(error: IDJIError) {
                    mainHandler.post { finish(sdkError(error)) }
                }
            })
        } catch (error: Throwable) {
            finish(error)
        }
    }

    private fun armStepTimeout(
        category: MediaBoundaryFailureCategory,
        label: String,
        timeoutMillis: Long,
    ) {
        cancelStepTimeout()
        val task = Runnable {
            Log.w(TAG, "media-prepare: step timeout label=$label")
            fail(category, MediaBoundaryFailure(category, "媒体准备阶段 $label 超时"))
        }
        stepTimeoutTask = task
        mainHandler.postDelayed(task, timeoutMillis)
    }

    private fun cancelStepTimeout() {
        stepTimeoutTask?.let { mainHandler.removeCallbacks(it) }
        stepTimeoutTask = null
    }

    private fun cancelCleanupTimeout() {
        cleanupTimeoutTask?.let { mainHandler.removeCallbacks(it) }
        cleanupTimeoutTask = null
    }

    private fun clearSettleHook() {
        settleHook = null
    }

    private fun prerequisiteFailure(reason: String) {
        Log.w(TAG, "media-prepare: prerequisite failed reason=$reason")
        fail(MediaBoundaryFailureCategory.PREREQUISITE, IllegalStateException(reason))
    }

    /** 读取一个状态 Key；空值或读取失败都按「未知」处理，交由调用方判定是否放行。 */
    private fun <T> readValue(
        label: String,
        info: DJIKeyInfo<T>,
        index: ComponentIndexType?,
        onResult: (T?) -> Unit,
    ) {
        val key = if (index == null) KeyTools.createKey(info) else KeyTools.createKey(info, index)
        try {
            keyManager.getValue(key, object : CommonCallbacks.CompletionCallbackWithParam<T> {
                override fun onSuccess(data: T?) {
                    mainHandler.post { onResult(data) }
                }

                override fun onFailure(error: IDJIError) {
                    mainHandler.post { prerequisiteFailure("读取 $label 失败: $error") }
                }
            })
        } catch (error: Throwable) {
            mainHandler.post { prerequisiteFailure("读取 $label 异常: $error") }
        }
    }

    private fun sdkError(error: IDJIError) = IllegalStateException(error.toString())

    private companion object {
        const val TAG = "MediaPreparation"

        /** 单步等待上限：覆盖 SDK 内部重试与状态推送延迟。 */
        const val STEP_TIMEOUT_MS = 15_000L

        /** 列表拉取等待上限：沿用媒体分册建议的业务级兜底（60s）。 */
        const val LIST_TIMEOUT_MS = 60_000L
    }
}
