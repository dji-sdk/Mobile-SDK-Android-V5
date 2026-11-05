package com.dji.open.pilot.plugin.sampleitem.mediatask

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dji.open.pilot.plugin.R
import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.camera.CameraMode
import dji.sdk.keyvalue.value.camera.CameraStorageLocation
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.common.EmptyMsg
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.KeyManager
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.datacenter.media.MediaFile
import dji.v5.manager.datacenter.media.MediaFileDownloadListener
import dji.v5.manager.datacenter.media.MediaFileListDataSource
import dji.v5.manager.datacenter.media.MediaFileListState
import dji.v5.manager.datacenter.media.MediaFileListStateListener
import dji.v5.manager.datacenter.media.PullMediaFileListParam
import dji.v5.manager.intelligent.media.ThermalPhotoInfo
import java.io.File

/**
 * 多媒体演示页调度层。
 *
 * 职责：
 * - 持有 [MediaDataCenter] 的 MediaManager 引用，集中处理列表拉取、回放模式、相机操作与文件下载。
 * - 精确管理监听器所有权（媒体列表状态 / Key 回放状态），禁止 `removeAll...Listener()`。
 * - 一次性远端操作带 15s 超时；流式下载带 30s 空闲超时，数据续期节流 2s。
 * - 下载统一走 [MediaDownloadSession]，按到达顺序顺序写入，每次覆盖旧文件（offset=0，不续传）。
 *
 * 能力边界：REMOTE 模式下删除与视频播放不受支持，本 ViewModel 不调用相关 API。
 * Activity 传入 app-specific 目录（[downloadSelectedFile] / [downloadSelectedFiles] / [parseThermal]），
 * 因此本类不持有任何 Context。
 */
class MediaTaskDemoViewModel : ViewModel() {

    private companion object {
        const val TAG = "MediaTaskDemoViewModel"
        const val OPERATION_TIMEOUT_MS = 15_000L

        const val TIMEOUT_PULL = "pull"
        const val TIMEOUT_PLAYBACK = "playback"
        const val TIMEOUT_PREVIEW = "preview"
        const val TIMEOUT_XMP = "xmp"
        const val TIMEOUT_THERMAL = "thermal"
        const val TIMEOUT_CAMERA_OP = "camera_op"
    }

    private val mediaManager = MediaDataCenter.getInstance().mediaManager
    private val keyManager = KeyManager.getInstance()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _uiState = MutableLiveData(MediaTaskUiState())
    val uiState: LiveData<MediaTaskUiState> = _uiState

    private val _toast = MutableLiveData<ToastMessage?>()
    val toast: LiveData<ToastMessage?> = _toast

    private val mediaListStateListener = MediaFileListStateListener { state -> onMediaListState(state) }
    private val keyListenerOwner = Any()

    private val timeouts = mutableMapOf<String, Runnable>()

    private var listenerRegistered = false
    private var playbackEnteredByPage = false

    private data class DownloadOutcome(val state: DownloadUiState, val toastResId: Int)
    private enum class DownloadSettlement { FINISHED, STOPPED, STOP_UNCONFIRMED }

    /** Callback ownership never changes, even when the next item has the same file index/name. */
    private class DownloadItem(val file: MediaFile, val task: MediaDownloadTask, val isBatchItem: Boolean) {
        var idleTimeout: Runnable? = null
        var stopTimeout: Runnable? = null
        var shouldPublish = true
    }

    // Only accessed on main; media callbacks use their captured task, never this pointer for IO.
    private var activeDownload: DownloadItem? = null

    private val batchQueue = ArrayDeque<MediaFile>()
    private var batchBaseDir: File? = null
    private var batchSuccessCount = 0
    private var batchFailCount = 0
    private var batchItemSettled = true

    // ---------------------------------------------------------------- 生命周期

    /** Server 连接状态由 Activity 观察 MainViewModel 后转发。 */
    fun onServerStateChanged(connected: Boolean) {
        val wasConnected = _uiState.value?.isServerConnected == true
        updateState { it.copy(isServerConnected = connected) }
        if (connected && !wasConnected) {
            initializeMediaListeners()
        } else if (!connected && wasConnected) {
            onDisconnected()
        }
    }

    private fun initializeMediaListeners() {
        if (!listenerRegistered) {
            mediaManager.addMediaFileListStateListener(mediaListStateListener)
            listenerRegistered = true
        }
        listenPlayingBack()
    }

    private fun listenPlayingBack() {
        val component = _uiState.value?.component ?: return
        keyManager.cancelListen(keyListenerOwner)
        keyManager.listen(
            KeyTools.createKey(CameraKey.KeyIsPlayingBack, component),
            keyListenerOwner
        ) { _, newValue ->
            if (newValue != null) {
                updateStateOnMain {
                    it.copy(playbackState = if (newValue) PlaybackUiState.ENTERED else PlaybackUiState.EXITED)
                }
            }
        }
    }

    private fun onDisconnected() {
        clearAllTimeouts()
        abandonDownload()
        settleLoadingOnDisconnect()
    }

    private fun settleLoadingOnDisconnect() {
        updateStateOnMain {
            it.copy(
                playbackState = PlaybackUiState.UNKNOWN,
                contentState = if (it.contentState == ContentUiState.LOADING) ContentUiState.ERROR else it.contentState,
                downloadState = if (it.downloadState == DownloadUiState.RUNNING) DownloadUiState.FAILED else it.downloadState,
                downloadSpeedBytesPerSecond = 0L,
                previewState = if (it.previewState == DetailUiState.LOADING) DetailUiState.ERROR else it.previewState,
                xmpFileState = if (it.xmpFileState == DetailUiState.LOADING) DetailUiState.ERROR else it.xmpFileState,
                xmpCustomState = if (it.xmpCustomState == DetailUiState.LOADING) DetailUiState.ERROR else it.xmpCustomState,
                thermalState = if (it.thermalState == DetailUiState.LOADING) DetailUiState.ERROR else it.thermalState,
                operationInProgress = false
            )
        }
    }

    /** 页面销毁时调用：停止拉取、取消下载、精确移除监听、关闭文件句柄、释放 Bitmap。 */
    fun releasePageResources() {
        Log.i(TAG, "releasePageResources")
        clearAllTimeouts()
        abandonDownload()

        mediaManager.stopPullMediaFileListFromCamera()

        if (listenerRegistered) {
            mediaManager.removeMediaFileListStateListener(mediaListStateListener)
            listenerRegistered = false
        }
        keyManager.cancelListen(keyListenerOwner)

        // 本页进入的回放模式只在本页退出时恢复；不使用全局 release()，避免影响其他页面。
        if (playbackEnteredByPage) {
            playbackEnteredByPage = false
            mediaManager.disable(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    Log.i(TAG, "exit playback on release success")
                }

                override fun onFailure(error: IDJIError) {
                    Log.w(TAG, "exit playback on release failed: ${error.description()}")
                }
            })
        }

        updateState {
            it.copy(
                files = emptyList(),
                selectedFileIndex = null,
                selectedFileIndices = emptySet(),
                previewBitmap = null,
                previewState = DetailUiState.IDLE,
                contentState = ContentUiState.IDLE,
                downloadState = DownloadUiState.IDLE,
                downloadProgress = 0,
                isDownloadIndeterminate = false,
                downloadReceivedBytes = 0L,
                downloadSpeedBytesPerSecond = 0L,
                operationInProgress = false
            )
        }
    }

    override fun onCleared() {
        releasePageResources()
        super.onCleared()
    }

    // ---------------------------------------------------------------- 数据源 / 回放

    fun selectComponent(component: ComponentIndexType) {
        val current = _uiState.value ?: return
        if (current.component == component) {
            return
        }
        mediaManager.stopPullMediaFileListFromCamera()
        clearTimeout(TIMEOUT_PULL)
        updateState {
            it.copy(
                component = component,
                playbackState = PlaybackUiState.UNKNOWN,
                contentState = ContentUiState.IDLE,
                files = emptyList(),
                selectedFileIndex = null,
                selectedFileIndices = emptySet()
            )
        }
        applyDataSource()
        if (current.isServerConnected) {
            listenPlayingBack()
        }
    }

    fun selectStorage(storage: CameraStorageLocation) {
        val current = _uiState.value ?: return
        if (current.storage == storage) {
            return
        }
        mediaManager.stopPullMediaFileListFromCamera()
        clearTimeout(TIMEOUT_PULL)
        updateState {
            it.copy(
                storage = storage,
                contentState = ContentUiState.IDLE,
                files = emptyList(),
                selectedFileIndex = null,
                selectedFileIndices = emptySet()
            )
        }
        applyDataSource()
    }

    /** 一次性使用当前相机与存储构建数据源，避免 Builder 默认值重置另一字段。 */
    private fun applyDataSource() {
        val state = _uiState.value ?: return
        mediaManager.setMediaFileDataSource(
            MediaFileListDataSource.Builder()
                .setIndexType(state.component)
                .setLocation(state.storage)
                .build()
        )
    }

    fun enterPlayback() {
        if (!isConnected() || _uiState.value?.playbackState == PlaybackUiState.ENTERING) {
            return
        }
        applyDataSource()
        updateState { it.copy(playbackState = PlaybackUiState.ENTERING) }
        scheduleTimeout(TIMEOUT_PLAYBACK) {
            updateState { it.copy(playbackState = PlaybackUiState.FAILED) }
            showToast(R.string.media_operation_timeout)
        }
        mediaManager.enable(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                clearTimeout(TIMEOUT_PLAYBACK)
                playbackEnteredByPage = true
                updateStateOnMain { it.copy(playbackState = PlaybackUiState.ENTERED) }
                showToastOnMain(R.string.media_playback_entered)
            }

            override fun onFailure(error: IDJIError) {
                clearTimeout(TIMEOUT_PLAYBACK)
                updateStateOnMain { it.copy(playbackState = PlaybackUiState.FAILED) }
                showToastOnMain(R.string.media_operation_failed, error.description())
            }
        })
    }

    fun exitPlayback() {
        if (!isConnected() || _uiState.value?.playbackState == PlaybackUiState.EXITING) {
            return
        }
        updateState { it.copy(playbackState = PlaybackUiState.EXITING) }
        scheduleTimeout(TIMEOUT_PLAYBACK) {
            updateState { it.copy(playbackState = PlaybackUiState.FAILED) }
            showToast(R.string.media_operation_timeout)
        }
        mediaManager.disable(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                clearTimeout(TIMEOUT_PLAYBACK)
                playbackEnteredByPage = false
                updateStateOnMain { it.copy(playbackState = PlaybackUiState.EXITED) }
                showToastOnMain(R.string.media_playback_exited)
            }

            override fun onFailure(error: IDJIError) {
                clearTimeout(TIMEOUT_PLAYBACK)
                updateStateOnMain { it.copy(playbackState = PlaybackUiState.FAILED) }
                showToastOnMain(R.string.media_operation_failed, error.description())
            }
        })
    }

    // ---------------------------------------------------------------- 列表

    fun pullMediaFiles(indexText: String, countText: String) {
        if (!isConnected()) {
            return
        }
        val range = try {
            parsePullRange(indexText, countText)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "invalid pull range", e)
            showToast(R.string.media_invalid_range)
            return
        }
        applyDataSource()
        updateState {
            it.copy(
                contentState = ContentUiState.LOADING,
                files = emptyList(),
                selectedFileIndex = null,
                selectedFileIndices = emptySet()
            )
        }
        scheduleTimeout(TIMEOUT_PULL) {
            updateState { it.copy(contentState = ContentUiState.ERROR) }
            showToast(R.string.media_operation_timeout)
        }
        mediaManager.pullMediaFileListFromCamera(
            PullMediaFileListParam.Builder().mediaFileIndex(range.index).count(range.count).build(),
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    Log.i(TAG, "pull list success")
                }

                override fun onFailure(error: IDJIError) {
                    clearTimeout(TIMEOUT_PULL)
                    updateStateOnMain { it.copy(contentState = ContentUiState.ERROR) }
                    showToastOnMain(R.string.media_operation_failed, error.description())
                }
            }
        )
    }

    fun stopPullMediaFiles() {
        mediaManager.stopPullMediaFileListFromCamera()
        clearTimeout(TIMEOUT_PULL)
        updateState {
            if (it.contentState == ContentUiState.LOADING) it.copy(contentState = ContentUiState.IDLE) else it
        }
    }

    private fun onMediaListState(listState: MediaFileListState) {
        when (listState) {
            MediaFileListState.UPDATING -> updateStateOnMain { it.copy(contentState = ContentUiState.LOADING) }

            MediaFileListState.UP_TO_DATE -> {
                clearTimeout(TIMEOUT_PULL)
                val files = mediaManager.mediaFileListData?.data.orEmpty().toList()
                updateStateOnMain {
                    val validIndices = files.map { file -> file.fileIndex }.toSet()
                    it.copy(
                        contentState = if (files.isEmpty()) ContentUiState.EMPTY else ContentUiState.CONTENT,
                        files = files,
                        selectedFileIndex = it.selectedFileIndex?.takeIf { index -> validIndices.contains(index) },
                        selectedFileIndices = it.selectedFileIndices.intersect(validIndices)
                    )
                }
            }

            else -> Unit
        }
    }

    // ---------------------------------------------------------------- 选择

    fun clearSelection() {
        updateState {
            it.copy(
                selectedFileIndex = null,
                previewState = DetailUiState.IDLE,
                previewBitmap = null,
                downloadState = DownloadUiState.IDLE,
                downloadProgress = 0,
                isDownloadIndeterminate = false,
                downloadReceivedBytes = 0L,
                downloadSpeedBytesPerSecond = 0L,
                xmpFileState = DetailUiState.IDLE,
                xmpFileData = null,
                xmpCustomState = DetailUiState.IDLE,
                xmpCustomInfo = null,
                thermalState = DetailUiState.IDLE,
                thermalResult = null
            )
        }
    }

    fun onFileClicked(fileIndex: Int) {
        val state = _uiState.value ?: return
        if (state.selectionMode) {
            toggleSelection(fileIndex)
        } else {
            selectFile(fileIndex)
        }
    }

    fun setSelectionMode(enabled: Boolean) {
        updateState {
            it.copy(
                selectionMode = enabled,
                selectedFileIndices = if (enabled) it.selectedFileIndices else emptySet()
            )
        }
    }

    private fun selectFile(fileIndex: Int) {
        updateState {
            it.copy(
                selectedFileIndex = fileIndex,
                previewState = DetailUiState.IDLE,
                previewBitmap = null,
                previewIsThumbnail = false,
                downloadState = DownloadUiState.IDLE,
                downloadProgress = 0,
                isDownloadIndeterminate = false,
                downloadReceivedBytes = 0L,
                downloadSpeedBytesPerSecond = 0L,
                xmpFileState = DetailUiState.IDLE,
                xmpFileData = null,
                xmpCustomState = DetailUiState.IDLE,
                xmpCustomInfo = null,
                thermalState = DetailUiState.IDLE,
                thermalResult = null
            )
        }
        loadSelectedThumbnailAsPreview(fileIndex)
    }

    /**
     * 进入详情页时自动用缩略图作预览占位（高清预览仍需手动拉取）。
     *
     * 仅在当前仍选中该文件、且未拉过高清预览、且当前预览为空或仍是缩略图占位时生效，
     * 避免覆盖用户已拉取的高清图；视频等无缩略图的文件保持占位背景，不报错。
     */
    private fun loadSelectedThumbnailAsPreview(fileIndex: Int) {
        val file = _uiState.value?.files?.firstOrNull { it.fileIndex == fileIndex } ?: return
        loadThumbnail(file) { result ->
            val bitmap = result.getOrNull() ?: return@loadThumbnail
            if (bitmap.isRecycled) {
                return@loadThumbnail
            }
            updateStateOnMain { state ->
                val stillSelected = state.selectedFileIndex == fileIndex
                val notHighQuality = state.previewState != DetailUiState.CONTENT
                val safeToPlace = state.previewBitmap == null || state.previewIsThumbnail
                if (stillSelected && notHighQuality && safeToPlace) {
                    state.copy(previewBitmap = bitmap, previewIsThumbnail = true)
                } else {
                    state
                }
            }
        }
    }

    private fun toggleSelection(fileIndex: Int) {
        updateState {
            val next = it.selectedFileIndices.toMutableSet()
            if (!next.add(fileIndex)) {
                next.remove(fileIndex)
            }
            it.copy(selectedFileIndices = next)
        }
    }

    private fun selectedFiles(): List<MediaFile> {
        val state = _uiState.value ?: return emptyList()
        return state.files.filter { state.selectedFileIndices.contains(it.fileIndex) }
    }

    // ---------------------------------------------------------------- 缩略图 / 预览

    /** 加载缩略图；命中缓存时同步回调，失败返回 [Result.failure]。回调保证在主线程。 */
    fun loadThumbnail(file: MediaFile, onResult: (Result<Bitmap>) -> Unit) {
        val cached = file.thumbNail
        if (cached != null && !cached.isRecycled) {
            onResult(Result.success(cached))
            return
        }
        file.pullThumbnailFromCamera(object : CommonCallbacks.CompletionCallbackWithParam<Bitmap> {
            override fun onSuccess(bitmap: Bitmap) {
                mainHandler.post { onResult(Result.success(bitmap)) }
            }

            override fun onFailure(error: IDJIError) {
                val failure = Result.failure<Bitmap>(IllegalStateException(error.description()))
                mainHandler.post { onResult(failure) }
            }
        })
    }

    fun pullPreview() {
        if (!isConnected()) {
            return
        }
        val file = _uiState.value?.selectedFile ?: return
        val fileIndex = file.fileIndex
        updateState { it.copy(previewState = DetailUiState.LOADING) }
        scheduleTimeout(TIMEOUT_PREVIEW) {
            updateState { if (it.selectedFileIndex == fileIndex) it.copy(previewState = DetailUiState.ERROR) else it }
            showToast(R.string.media_operation_timeout)
        }
        file.pullPreviewFromCamera(object : CommonCallbacks.CompletionCallbackWithParam<Bitmap> {
            override fun onSuccess(bitmap: Bitmap) {
                clearTimeout(TIMEOUT_PREVIEW)
                updateStateOnMain {
                    if (it.selectedFileIndex == fileIndex) {
                        it.copy(
                            previewState = DetailUiState.CONTENT,
                            previewBitmap = bitmap,
                            previewIsThumbnail = false
                        )
                    } else {
                        it
                    }
                }
            }

            override fun onFailure(error: IDJIError) {
                clearTimeout(TIMEOUT_PREVIEW)
                updateStateOnMain {
                    if (it.selectedFileIndex == fileIndex) it.copy(previewState = DetailUiState.ERROR) else it
                }
                showToastOnMain(R.string.media_operation_failed, error.description())
            }
        })
    }

    // ---------------------------------------------------------------- 下载

    /** 下载当前选中文件。 */
    fun downloadSelectedFile(baseDir: File) {
        if (activeDownload != null || _uiState.value?.operationInProgress == true) {
            return
        }
        val file = _uiState.value?.selectedFile ?: return
        startDownload(baseDir, file, forBatch = false)
    }

    /** 串行下载所有已选文件，逐项汇总结果。 */
    fun downloadSelectedFiles(baseDir: File) {
        if (activeDownload != null || _uiState.value?.operationInProgress == true) {
            return
        }
        val files = selectedFiles()
        if (files.isEmpty()) {
            showToast(R.string.media_no_selection)
            return
        }
        batchQueue.clear()
        batchQueue.addAll(files)
        batchBaseDir = baseDir
        batchSuccessCount = 0
        batchFailCount = 0
        updateState { it.copy(operationInProgress = true) }
        runNextBatchItem()
    }

    private fun runNextBatchItem() {
        val file = batchQueue.removeFirstOrNull()
        val baseDir = batchBaseDir
        if (file == null || baseDir == null) {
            val succeeded = batchSuccessCount
            val failed = batchFailCount
            updateState { it.copy(operationInProgress = false) }
            showToast(R.string.media_batch_download_result, succeeded, failed)
            return
        }
        startDownload(baseDir, file, forBatch = true)
    }

    private fun startDownload(baseDir: File, file: MediaFile, forBatch: Boolean) {
        if (activeDownload != null) return
        if (forBatch) {
            batchItemSettled = false
        }
        val expectedSize = file.fileSize
        updateState {
            it.copy(
                downloadState = DownloadUiState.IDLE,
                downloadProgress = 0,
                isDownloadIndeterminate = expectedSize <= 0L,
                downloadReceivedBytes = 0L,
                downloadSpeedBytesPerSecond = 0L
            )
        }
        if (!isConnected()) {
            updateState { it.copy(downloadState = DownloadUiState.FAILED) }
            if (forBatch) {
                settleBatch(false)
            }
            return
        }
        val session = try {
            MediaDownloadSession.open(baseDir, file.fileName ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "open download session failed", e)
            updateState { it.copy(downloadState = DownloadUiState.FAILED) }
            showToast(R.string.media_download_failed)
            if (forBatch) {
                settleBatch(false)
            }
            return
        }
        val item = DownloadItem(file, MediaDownloadTask(session, expectedSize, SystemClock.elapsedRealtime()), forBatch)
        activeDownload = item

        updateState { it.copy(downloadState = DownloadUiState.RUNNING) }
        renewDownloadTimeout(item)

        // 方案 B2：offset 固定 0，覆盖式全量下载，不做断点续传。
        file.pullOriginalMediaFileFromCamera(0L, DownloadCallback(item))
    }

    private inner class DownloadCallback(private val item: DownloadItem) : MediaFileDownloadListener {
        override fun onStart() {
            Log.i(TAG, "download start: ${item.file.fileName}")
        }

        override fun onProgress(total: Long, current: Long) {
            val nowMs = SystemClock.elapsedRealtime()
            if (!item.task.recordProgress(nowMs)) return
            runOnMain {
                if (activeDownload !== item || !item.task.isRunning()) return@runOnMain
                renewDownloadTimeout(item)
                // SDK current/total count an individual CSDK request, not the whole file.
                publishDownloadProgress(item)
            }
        }

        override fun onRealtimeDataUpdate(data: ByteArray, position: Long) {
            // SDK position is the fixed initial offset; write sequentially to this callback's session.
            try {
                if (item.task.append(data, SystemClock.elapsedRealtime())) {
                    mainHandler.post {
                        renewDownloadTimeout(item)
                        publishDownloadProgress(item)
                    }
                }
            } catch (exception: Exception) {
                // append already entered STOPPING under its lock, before posting to main.
                Log.e(TAG, "write download data failed", exception)
                runOnMain {
                    requestDownloadStop(item, DownloadOutcome(DownloadUiState.FAILED, R.string.media_download_failed))
                }
            }
        }

        override fun onFinish() {
            runOnMain { finishDownloadItem(item) }
        }

        override fun onFailure(error: IDJIError) {
            Log.w(TAG, "download failed: ${error.description()}")
            runOnMain {
                finishDownloadItem(item, DownloadOutcome(DownloadUiState.FAILED, R.string.media_download_failed))
            }
        }
    }

    /** Read a fresh snapshot on main so delayed callbacks cannot publish stale progress. */
    private fun publishDownloadProgress(item: DownloadItem) {
        if (activeDownload !== item || !item.task.isRunning()) return
        val snapshot = item.task.progressSnapshot(SystemClock.elapsedRealtime())
        updateState {
            it.copy(
                downloadProgress = snapshot.percent ?: 0,
                isDownloadIndeterminate = snapshot.percent == null,
                downloadReceivedBytes = snapshot.receivedBytes,
                downloadSpeedBytesPerSecond = snapshot.speedBytesPerSecond
            )
        }
    }

    fun cancelDownload() {
        runOnMain {
            clearDownloadBatch()
            val item = activeDownload ?: return@runOnMain
            item.task.beginStop()
            requestDownloadStop(item, DownloadOutcome(DownloadUiState.CANCELLED, R.string.media_download_cancelled))
        }
    }

    /** Idle and stop deadlines are separate, so queued progress can never replace the stop fallback. */
    private fun renewDownloadTimeout(item: DownloadItem) {
        if (activeDownload !== item || !item.task.isRunning()) return
        item.idleTimeout?.let { mainHandler.removeCallbacks(it) }
        val timeout = Runnable {
            if (activeDownload !== item) return@Runnable
            if (item.task.beginIdleStop(SystemClock.elapsedRealtime())) {
                requestDownloadStop(item, DownloadOutcome(DownloadUiState.FAILED, R.string.media_download_timeout))
            } else {
                renewDownloadTimeout(item)
            }
        }
        item.idleTimeout = timeout
        mainHandler.postDelayed(timeout, item.task.idleRemainingMs(SystemClock.elapsedRealtime()))
    }

    /** Called on main after the task has blocked IO; close locally even if remote stop fails. */
    private fun requestDownloadStop(item: DownloadItem, outcome: DownloadOutcome) {
        if (activeDownload !== item || !item.task.claimStopRequest(SystemClock.elapsedRealtime())) return
        item.idleTimeout?.let { mainHandler.removeCallbacks(it) }
        item.idleTimeout = null
        closeDownloadTask(item)
        val timeout = Runnable {
            Log.w(TAG, "stop download no response: ${item.file.fileName}")
            finishDownloadItem(item, outcome, DownloadSettlement.STOP_UNCONFIRMED)
        }
        item.stopTimeout = timeout
        mainHandler.postDelayed(timeout, item.task.stopRemainingMs(SystemClock.elapsedRealtime()))
        val callback = object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                runOnMain { finishDownloadItem(item, outcome, DownloadSettlement.STOPPED) }
            }

            override fun onFailure(error: IDJIError) {
                Log.w(TAG, "stop download failed: ${error.description()}")
                runOnMain { finishDownloadItem(item, outcome, DownloadSettlement.STOP_UNCONFIRMED) }
            }
        }
        try {
            item.file.stopPullOriginalMediaFileFromCamera(callback)
        } catch (exception: Exception) {
            Log.e(TAG, "stop download threw", exception)
            finishDownloadItem(item, outcome, DownloadSettlement.STOP_UNCONFIRMED)
        }
    }

    /** All terminal callbacks arrive here on main and claim settlement exactly once. */
    private fun finishDownloadItem(
        item: DownloadItem,
        outcome: DownloadOutcome? = null,
        settlement: DownloadSettlement = DownloadSettlement.FINISHED
    ) {
        if (activeDownload !== item) return
        val canSettle = if (settlement == DownloadSettlement.FINISHED) item.task.settleRunning() else item.task.settleStopped()
        if (!canSettle) return
        // Settlement freezes append before size validation; no global session/file lookup.
        val terminalOutcome = outcome ?: completedDownloadOutcome(item)
        val snapshot = item.task.progressSnapshot(SystemClock.elapsedRealtime())
        item.idleTimeout?.let { mainHandler.removeCallbacks(it) }
        item.stopTimeout?.let { mainHandler.removeCallbacks(it) }
        closeDownloadTask(item)
        activeDownload = null
        if (!item.shouldPublish) return
        updateState {
            it.copy(
                downloadState = terminalOutcome.state,
                downloadProgress = snapshot.percent ?: 0,
                isDownloadIndeterminate = snapshot.percent == null,
                downloadReceivedBytes = snapshot.receivedBytes,
                downloadSpeedBytesPerSecond = 0L
            )
        }
        showToast(terminalOutcome.toastResId)
        if (settlement == DownloadSettlement.STOP_UNCONFIRMED) clearDownloadBatch()
        if (item.isBatchItem && batchBaseDir != null) {
            settleBatch(terminalOutcome.state == DownloadUiState.SUCCEEDED)
        } else if (item.isBatchItem) {
            updateState { it.copy(operationInProgress = false) }
        }
    }

    private fun completedDownloadOutcome(item: DownloadItem): DownloadOutcome = try {
        if (item.task.validateFinished()) {
            DownloadOutcome(DownloadUiState.SUCCEEDED, R.string.media_download_completed)
        } else {
            DownloadOutcome(DownloadUiState.FAILED, R.string.media_download_incomplete)
        }
    } catch (exception: Exception) {
        Log.e(TAG, "validate download failed", exception)
        DownloadOutcome(DownloadUiState.FAILED, R.string.media_download_failed)
    }

    private fun clearDownloadBatch() {
        batchQueue.clear()
        batchBaseDir = null
        batchItemSettled = true
    }

    /** Disconnect/release keep their existing UI cleanup; late stop callbacks must not republish. */
    private fun abandonDownload() {
        clearDownloadBatch()
        val item = activeDownload ?: return
        item.shouldPublish = false
        item.task.beginStop()
        requestDownloadStop(item, DownloadOutcome(DownloadUiState.CANCELLED, R.string.media_download_cancelled))
    }

    private fun settleBatch(success: Boolean) {
        if (batchItemSettled) {
            return
        }
        batchItemSettled = true
        if (success) {
            batchSuccessCount++
        } else {
            batchFailCount++
        }
        runNextBatchItem()
    }

    private fun closeDownloadTask(item: DownloadItem) {
        try {
            item.task.close()
        } catch (e: Exception) {
            Log.e(TAG, "close download session failed", e)
        }
    }

    // ---------------------------------------------------------------- XMP / 热解析

    fun pullXmpFileData() {
        if (!isConnected()) {
            return
        }
        val file = _uiState.value?.selectedFile ?: return
        val fileIndex = file.fileIndex
        updateState { it.copy(xmpFileState = DetailUiState.LOADING, xmpFileData = null) }
        scheduleTimeout(TIMEOUT_XMP) {
            updateState { if (it.selectedFileIndex == fileIndex) it.copy(xmpFileState = DetailUiState.ERROR) else it }
            showToast(R.string.media_operation_timeout)
        }
        file.pullXMPFileDataFromCamera(object : CommonCallbacks.CompletionCallbackWithParam<String> {
            override fun onSuccess(value: String) {
                clearTimeout(TIMEOUT_XMP)
                updateStateOnMain {
                    if (it.selectedFileIndex == fileIndex) {
                        it.copy(xmpFileState = DetailUiState.CONTENT, xmpFileData = value)
                    } else {
                        it
                    }
                }
            }

            override fun onFailure(error: IDJIError) {
                clearTimeout(TIMEOUT_XMP)
                updateStateOnMain {
                    if (it.selectedFileIndex == fileIndex) it.copy(xmpFileState = DetailUiState.ERROR) else it
                }
                showToastOnMain(R.string.media_operation_failed, error.description())
            }
        })
    }

    fun pullXmpCustomInfo() {
        if (!isConnected()) {
            return
        }
        val file = _uiState.value?.selectedFile ?: return
        val fileIndex = file.fileIndex
        updateState { it.copy(xmpCustomState = DetailUiState.LOADING, xmpCustomInfo = null) }
        scheduleTimeout(TIMEOUT_XMP) {
            updateState { if (it.selectedFileIndex == fileIndex) it.copy(xmpCustomState = DetailUiState.ERROR) else it }
            showToast(R.string.media_operation_timeout)
        }
        file.pullXMPCustomInfoFromCamera(object : CommonCallbacks.CompletionCallbackWithParam<String> {
            override fun onSuccess(value: String) {
                clearTimeout(TIMEOUT_XMP)
                updateStateOnMain {
                    if (it.selectedFileIndex == fileIndex) {
                        it.copy(xmpCustomState = DetailUiState.CONTENT, xmpCustomInfo = value)
                    } else {
                        it
                    }
                }
            }

            override fun onFailure(error: IDJIError) {
                clearTimeout(TIMEOUT_XMP)
                updateStateOnMain {
                    if (it.selectedFileIndex == fileIndex) it.copy(xmpCustomState = DetailUiState.ERROR) else it
                }
                showToastOnMain(R.string.media_operation_failed, error.description())
            }
        })
    }

    /** 设置相机级 XMP Custom Info；远端 Key 失败回调异常，成功后必须再读校验。 */
    fun setCameraXmpCustomInfo(information: String) {
        if (!isConnected()) {
            return
        }
        applyDataSource()
        updateState { it.copy(operationInProgress = true) }
        scheduleTimeout(TIMEOUT_CAMERA_OP) {
            updateState { it.copy(operationInProgress = false) }
            showToast(R.string.media_operation_timeout)
        }
        mediaManager.setMediaFileXMPCustomInfo(information, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                verifyCameraXmpCustomInfo(information)
            }

            override fun onFailure(error: IDJIError) {
                clearTimeout(TIMEOUT_CAMERA_OP)
                updateStateOnMain { it.copy(operationInProgress = false) }
                showToastOnMain(R.string.media_operation_failed, error.description())
            }
        })
    }

    private fun verifyCameraXmpCustomInfo(expected: String) {
        mediaManager.getMediaFileXMPCustomInfo(object : CommonCallbacks.CompletionCallbackWithParam<String> {
            override fun onSuccess(value: String) {
                clearTimeout(TIMEOUT_CAMERA_OP)
                updateStateOnMain { it.copy(operationInProgress = false, cameraXmpCustomInfo = value) }
                val matched = value == expected
                showToastOnMain(
                    if (matched) R.string.media_xmp_set_success else R.string.media_xmp_set_mismatch
                )
            }

            override fun onFailure(error: IDJIError) {
                clearTimeout(TIMEOUT_CAMERA_OP)
                updateStateOnMain { it.copy(operationInProgress = false) }
                showToastOnMain(R.string.media_operation_failed, error.description())
            }
        })
    }

    fun getCameraXmpCustomInfo() {
        if (!isConnected()) {
            return
        }
        applyDataSource()
        updateState { it.copy(operationInProgress = true) }
        scheduleTimeout(TIMEOUT_CAMERA_OP) {
            updateState { it.copy(operationInProgress = false) }
            showToast(R.string.media_operation_timeout)
        }
        mediaManager.getMediaFileXMPCustomInfo(object : CommonCallbacks.CompletionCallbackWithParam<String> {
            override fun onSuccess(value: String) {
                clearTimeout(TIMEOUT_CAMERA_OP)
                updateStateOnMain { it.copy(operationInProgress = false, cameraXmpCustomInfo = value) }
                showToastOnMain(R.string.media_xmp_get_success)
            }

            override fun onFailure(error: IDJIError) {
                clearTimeout(TIMEOUT_CAMERA_OP)
                updateStateOnMain { it.copy(operationInProgress = false) }
                showToastOnMain(R.string.media_operation_failed, error.description())
            }
        })
    }

    /** 解析已完整下载的热成像原图；需 [downloadState] 为成功且本地文件存在。 */
    fun parseThermal(baseDir: File) {
        if (!isConnected()) {
            return
        }
        val file = _uiState.value?.selectedFile ?: return
        val expectedSize = file.fileSize
        val localFile = File(File(baseDir, "media-task"), File(file.fileName ?: "").name)
        if (!localFile.isFile || localFile.length() != expectedSize) {
            showToast(R.string.media_thermal_need_download)
            return
        }
        val fileIndex = file.fileIndex
        updateState { it.copy(thermalState = DetailUiState.LOADING, thermalResult = null) }
        scheduleTimeout(TIMEOUT_THERMAL) {
            updateState { if (it.selectedFileIndex == fileIndex) it.copy(thermalState = DetailUiState.ERROR) else it }
            showToast(R.string.media_operation_timeout)
        }
        mediaManager.parseThermalTemperature(
            localFile.absolutePath,
            object : CommonCallbacks.CompletionCallbackWithParam<ThermalPhotoInfo> {
                override fun onSuccess(info: ThermalPhotoInfo) {
                    clearTimeout(TIMEOUT_THERMAL)
                    updateStateOnMain {
                        if (it.selectedFileIndex == fileIndex) {
                            it.copy(thermalState = DetailUiState.CONTENT, thermalResult = info)
                        } else {
                            it
                        }
                    }
                }

                override fun onFailure(error: IDJIError) {
                    clearTimeout(TIMEOUT_THERMAL)
                    updateStateOnMain {
                        if (it.selectedFileIndex == fileIndex) it.copy(thermalState = DetailUiState.ERROR) else it
                    }
                    showToastOnMain(R.string.media_operation_failed, error.description())
                }
            }
        )
    }

    // ---------------------------------------------------------------- 相机操作

    fun takePhoto() {
        if (!isConnected()) {
            return
        }
        val component = _uiState.value?.component ?: return
        updateState { it.copy(operationInProgress = true) }
        scheduleTimeout(TIMEOUT_CAMERA_OP) {
            updateState { it.copy(operationInProgress = false) }
            showToast(R.string.media_operation_timeout)
        }
        keyManager.setValue(
            KeyTools.createKey(CameraKey.KeyCameraMode, component),
            CameraMode.PHOTO_NORMAL,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    keyManager.performAction(
                        KeyTools.createKey(CameraKey.KeyStartShootPhoto, component),
                        object : CommonCallbacks.CompletionCallbackWithParam<EmptyMsg> {
                            override fun onSuccess(result: EmptyMsg) {
                                clearTimeout(TIMEOUT_CAMERA_OP)
                                updateStateOnMain { it.copy(operationInProgress = false) }
                                showToastOnMain(R.string.media_take_photo_success)
                            }

                            override fun onFailure(error: IDJIError) {
                                clearTimeout(TIMEOUT_CAMERA_OP)
                                updateStateOnMain { it.copy(operationInProgress = false) }
                                showToastOnMain(R.string.media_operation_failed, error.description())
                            }
                        }
                    )
                }

                override fun onFailure(error: IDJIError) {
                    clearTimeout(TIMEOUT_CAMERA_OP)
                    updateStateOnMain { it.copy(operationInProgress = false) }
                    showToastOnMain(R.string.media_operation_failed, error.description())
                }
            }
        )
    }

    fun confirmFormatStorage() {
        if (!isConnected()) {
            return
        }
        val component = _uiState.value?.component ?: return
        updateState { it.copy(operationInProgress = true) }
        scheduleTimeout(TIMEOUT_CAMERA_OP) {
            updateState { it.copy(operationInProgress = false) }
            showToast(R.string.media_operation_timeout)
        }
        keyManager.performAction(
            KeyTools.createKey(CameraKey.KeyFormatStorage, component),
            CameraStorageLocation.SDCARD,
            object : CommonCallbacks.CompletionCallbackWithParam<EmptyMsg> {
                override fun onSuccess(result: EmptyMsg) {
                    clearTimeout(TIMEOUT_CAMERA_OP)
                    updateStateOnMain { it.copy(operationInProgress = false) }
                    showToastOnMain(R.string.media_format_storage_success)
                }

                override fun onFailure(error: IDJIError) {
                    clearTimeout(TIMEOUT_CAMERA_OP)
                    updateStateOnMain { it.copy(operationInProgress = false) }
                    showToastOnMain(R.string.media_operation_failed, error.description())
                }
            }
        )
    }

    // ---------------------------------------------------------------- 提示 / 超时 / 状态

    fun consumeToast() {
        _toast.value = null
    }

    private fun isConnected(): Boolean {
        if (_uiState.value?.isServerConnected != true) {
            showToast(R.string.media_disconnected_error)
            return false
        }
        return true
    }

    private fun scheduleTimeout(tag: String, onTimeout: () -> Unit) {
        clearTimeout(tag)
        val runnable = Runnable {
            timeouts.remove(tag)
            onTimeout()
        }
        timeouts[tag] = runnable
        mainHandler.postDelayed(runnable, OPERATION_TIMEOUT_MS)
    }

    private fun clearTimeout(tag: String) {
        timeouts.remove(tag)?.let { mainHandler.removeCallbacks(it) }
    }

    private fun clearAllTimeouts() {
        timeouts.values.forEach { mainHandler.removeCallbacks(it) }
        timeouts.clear()
    }

    private fun updateState(transform: (MediaTaskUiState) -> MediaTaskUiState) {
        _uiState.value = transform(_uiState.value ?: MediaTaskUiState())
    }

    private fun updateStateOnMain(transform: (MediaTaskUiState) -> MediaTaskUiState) {
        runOnMain { updateState(transform) }
    }

    private fun runOnMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post { action() }
        }
    }

    private fun showToast(resId: Int, vararg args: Any) {
        _toast.value = ToastMessage(resId, args.toList())
    }

    private fun showToastOnMain(resId: Int, vararg args: Any) {
        val message = ToastMessage(resId, args.toList())
        if (Looper.myLooper() == Looper.getMainLooper()) {
            _toast.value = message
        } else {
            mainHandler.post { _toast.value = message }
        }
    }
}
