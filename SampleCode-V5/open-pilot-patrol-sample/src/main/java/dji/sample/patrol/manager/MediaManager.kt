package dji.sample.patrol.manager

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.AtomicFile
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dji.sample.patrol.R
import dji.sample.patrol.data.Asset
import dji.sample.patrol.data.AssetState
import dji.sample.patrol.data.CategorizedException
import dji.sample.patrol.data.MediaReference
import dji.sample.patrol.data.MediaDownloadState
import dji.sample.patrol.data.MediaDownloadStatus
import dji.sample.patrol.data.JobStatus
import dji.sample.patrol.data.RecognitionFailureCategory
import dji.sample.patrol.data.ResultCallback
import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.key.RemoteControllerKey
import dji.sdk.keyvalue.utils.ProductUtil
import dji.sdk.keyvalue.value.camera.CameraStorageLocation
import dji.sdk.keyvalue.value.camera.CustomExpandNameSettings
import dji.sdk.keyvalue.value.camera.GeneratedMediaFileInfo
import dji.sdk.keyvalue.value.camera.MediaFileType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.common.EnCodingType
import dji.sdk.keyvalue.value.common.MultiControlChannel
import dji.sdk.keyvalue.value.common.RelativePosition
import dji.sdk.keyvalue.value.remotecontroller.MultiRCExclusiveTaskAuthOwnerMsg
import dji.sdk.keyvalue.value.remotecontroller.RCMode
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.KeyManager
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.datacenter.media.MediaFile
import dji.v5.manager.datacenter.media.MediaFileDownloadListener
import dji.v5.manager.datacenter.media.MediaFileFilter
import dji.v5.manager.datacenter.media.MediaFileListDataSource
import dji.v5.manager.datacenter.media.PullMediaFileListParam
import dji.v5.manager.interfaces.IMediaManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.Calendar
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

object MediaManager {
    private val mutableLocalAssets = MutableLiveData<List<Asset>>(emptyList())
    val localAssets: LiveData<List<Asset>> = mutableLocalAssets

    private val mutableMediaReferences = MutableLiveData<List<MediaReference>>(emptyList())
    val mediaReferences: LiveData<List<MediaReference>> = mutableMediaReferences

    private val mutableManifestAssets = MutableLiveData<List<Asset>>(emptyList())
    val manifestAssets: LiveData<List<Asset>> = mutableManifestAssets

    private val mutableThumbnailPaths = MutableLiveData<Map<String, String>>(emptyMap())
    val thumbnailPaths: LiveData<Map<String, String>> = mutableThumbnailPaths

    private val mutableDownloadStates = MutableLiveData<Map<String, MediaDownloadState>>(emptyMap())
    val downloadStates: LiveData<Map<String, MediaDownloadState>> = mutableDownloadStates

    private val mutablePendingCount = MutableLiveData(0)
    val pendingCount: LiveData<Int> = mutablePendingCount

    /** 媒体任务占用态：文件列表拉取、缩略图拉取或文件传输进行中为 true。 */
    private val mutableBusy = MutableLiveData(false)
    val busy: LiveData<Boolean> = mutableBusy
    private val busyGeneration = AtomicLong(0)

    private val mainHandler = Handler(Looper.getMainLooper())
    private val mediaKey = KeyTools.createKey(
        CameraKey.KeyNewlyGeneratedMediaFile,
        ComponentIndexType.LEFT_OR_MAIN,
    )
    private val folderNameKey = KeyTools.createKey(
        CameraKey.KeyCustomExpandDirectoryNameSettings,
        ComponentIndexType.LEFT_OR_MAIN,
    )

    private var appContext: Context? = null
    private var sdkManager: IMediaManager? = null
    private var executor: ExecutorService? = null
    private var activeJobId: String? = null
    private var onNewMedia: ((MediaReference) -> Unit)? = null
    private val references = linkedMapOf<String, MutableList<MediaReference>>()
    private val jobDirectoryIndexes = linkedMapOf<String, Int>()
    private val executionWindows = linkedMapOf<String, ExecutionWindow>()

    /** 各工单最新执行窗口起点的只读快照，供主线程/页面判定本轮资产；由串行执行器更新。 */
    @Volatile
    private var roundStartByJob: Map<String, Long> = emptyMap()
    private var finalizingJobIds: Set<String> = emptySet()
    private val assets = linkedMapOf<String, Asset>()
    private val downloadStateMap = linkedMapOf<String, MediaDownloadState>()
    private var activeDownload: ActiveDownload? = null
    private var operationId = 0L
    private var folderConfiguredJobId: String? = null
    private var transferOwnershipAcquired = false

    /** 启动前媒体准备的占用门：同一时刻只允许一个采集会话，含其清理阶段。 */
    private val boundaryCaptureGate = AtomicBoolean(false)

    /** 在途的启动前采集会话；断连与释放据此使其失效。 */
    @Volatile
    private var boundaryCapture: MediaBoundaryCapture? = null

    fun init(context: Context) {
        release()
        appContext = context.applicationContext
        executor = Executors.newSingleThreadExecutor()
        sdkManager = MediaDataCenter.getInstance().mediaManager.apply {
            setMediaFileDataSource(
                MediaFileListDataSource.Builder()
                    .setIndexType(ComponentIndexType.LEFT_OR_MAIN)
                    .setLocation(CameraStorageLocation.SDCARD)
                    .build()
            )
        }
        loadAssets()
        publishLocalState()
    }

    fun beginJob(jobId: String, onNewMedia: (MediaReference) -> Unit) {
        checkInitialized()
        endJob()
        activeJobId = jobId
        this.onNewMedia = onNewMedia
        references.getOrPut(jobId) { mutableListOf() }
        KeyManager.getInstance().listen(mediaKey, this, false) { _, value ->
            if (value != null) submit { recordMedia(value) }
        }
    }

    fun updateJobStatuses(statuses: Map<String, JobStatus>) {
        submit {
            finalizingJobIds = statuses.filterValues { it == JobStatus.FINALIZING }.keys
            statuses.filterValues { it == JobStatus.CANCELLED || it == JobStatus.COMPLETED }.keys
                .forEach(::discardUnresolvedReferences)
            publishLocalState()
        }
    }

    fun restartJob(jobId: String, callback: () -> Unit) {
        submit {
            // 历史引用保留在本地作为记录，但不再参与新一轮下载与统计；窗口必须重建，
            // 避免新照片沿用旧区间。
            val pending = references[jobId].orEmpty().count { !isDownloaded(it) }
            if (pending > 0) {
                Log.i(TAG, "restart: keep $pending pending references job=$jobId")
            }
            executionWindows.remove(jobId)
            jobDirectoryIndexes.remove(jobId)
            if (folderConfiguredJobId == jobId) folderConfiguredJobId = null
            persistAssets()
            publishLocalState()
            mainHandler.post(callback)
        }
    }

    /**
     * 建立本工单的相机目录并采集照片起始边界。
     *
     * 设计意图：这是「执行航线」的启动门禁——只有目录设置成功、媒体列表采集成功且相机已
     * 退出回放之后才提交起始边界；任一步失败都不提交，调用方不得继续启动航线。采集期间
     * 占用 [boundaryCaptureGate]，重复调用直接失败，不并发建立第二个媒体会话。
     */
    fun prepareJobFolder(jobId: String, callback: ResultCallback<Unit>) {
        checkInitialized()
        val existingWindow = executionWindows[jobId]
        // 只有「同一轮执行、窗口未闭合」才短路；已结束的窗口必须为新一轮重新建立边界。
        if (folderConfiguredJobId == jobId && existingWindow != null &&
            !needsFreshExecutionWindow(true, existingWindow.endTime, existingWindow.frozen)
        ) {
            succeed(callback, Unit)
            return
        }
        if (existingWindow != null) {
            Log.i(
                TAG,
                "prepare: rotate execution window job=$jobId previous=${existingWindow.startTime}.." +
                    "${existingWindow.endTime} frozen=${existingWindow.frozen}",
            )
        }
        if (!boundaryCaptureGate.compareAndSet(false, true)) {
            fail(
                callback,
                MediaBoundaryFailure(MediaBoundaryFailureCategory.PREREQUISITE, "媒体准备或清理仍在进行"),
            )
            return
        }
        val settings = CustomExpandNameSettings(
            EnCodingType.UTF8,
            RelativePosition.POSITION_END,
            safeName(jobId).take(MAX_FOLDER_SUFFIX_LENGTH),
            1,
            true,
        )
        captureStartBoundary(jobId, settings, callback)
    }

    /**
     * 恢复执行中工单时保证窗口可用（幂等）。
     *
     * 设计意图：冷启动或重连恢复正在执行的工单时，若窗口缺失或已被上一轮关闭，用当前时间与
     * 当前最大文件号重建开放窗口，使恢复后拍摄的照片仍能被归集。
     * 能力契约：窗口未闭合时不动作；只清空/重建工单级窗口，不删除引用、资产与上传队列。
     */
    fun ensureExecutionWindowForResume(jobId: String, onReady: () -> Unit = {}) =
        ensureCurrentExecutionWindow(jobId, "resume", onReady)

    /**
     * 保证当前工单存在一个开放的执行窗口（幂等）。
     *
     * 设计意图：任何会拍照或采集媒体的执行入口（航线进入执行、任务列表恢复、识别/模拟命中）
     * 都必须先确认本轮窗口可用；窗口缺失或已结束（`endTime > 0` / `frozen`）时用当前时间与
     * 当前最大引用序号开启新一轮，避免新照片被上一轮已关闭窗口排除。
     * 能力契约：同一轮重复调用不改变起点与游标；仅重建工单级窗口，不删除历史引用、资产与
     * 上传队列。所有状态变更在串行执行器内完成，结果经主线程回调。
     */
    fun ensureCurrentExecutionWindow(jobId: String, reason: String, onReady: () -> Unit = {}) {
        if (executor == null) {
            onReady()
            return
        }
        submit {
            val existing = executionWindows[jobId]
            if (existing == null ||
                needsFreshExecutionWindow(true, existing.endTime, existing.frozen)
            ) {
                openWindowLocked(jobId, reason)
            } else {
                Log.i(
                    TAG,
                    "window ensure job=$jobId reason=$reason window=${describeWindow(existing)} " +
                        "result=kept",
                )
            }
            try {
                persistAssets()
                publishLocalState()
            } finally {
                // 持久化失败也必须放行调用方，避免拍照媒体采集永久卡在「窗口准备中」。
                mainHandler.post(onReady)
            }
        }
    }

    /**
     * 在串行执行器线程内开启本轮窗口。
     *
     * 起点取当前时刻，游标取当前已知最大引用序号；时间窗仍是区分历史照片的主判定，
     * 游标只作为收尾清单的次级过滤。调用方必须已持有执行器线程。
     */
    private fun openWindowLocked(jobId: String, reason: String): ExecutionWindow {
        val previous = executionWindows[jobId]
        val startCursor = references[jobId].orEmpty().maxOfOrNull(MediaReference::fileIndex) ?: 0
        val window = ExecutionWindow(
            startTime = System.currentTimeMillis(),
            startCursor = startCursor,
            directoryIndex = jobDirectoryIndexes[jobId],
        )
        executionWindows[jobId] = window
        Log.i(
            TAG,
            "window ensure job=$jobId reason=$reason window=${describeWindow(window)} " +
                "previous=${describeWindow(previous)} result=rotated",
        )
        return window
    }

    /**
     * 使在途的启动前媒体准备失效（断连、任务切换、管理器释放）。
     *
     * 收敛后仍会完成退出回放与解订阅；迟到回调不得提交起始边界，也不得启动航线。
     */
    fun cancelBoundaryCapture() {
        boundaryCapture?.cancel(
            MediaBoundaryFailure(MediaBoundaryFailureCategory.CANCELLED, "媒体准备已取消"),
        )
    }

    fun markJobFinished(jobId: String) {
        val finishedAt = System.currentTimeMillis()
        submit {
            val window = executionWindows[jobId] ?: legacyWindow(jobId)
            executionWindows[jobId] = window.copy(endTime = finishedAt)
            persistAssets()
        }
    }

    fun freezeJobManifest(jobId: String, callback: ResultCallback<Unit>) {
        checkInitialized()
        submit {
            val existing = executionWindows[jobId] ?: legacyWindow(jobId).also {
                executionWindows[jobId] = it
            }
            if (existing.frozen && !hasPendingReferences(jobId)) {
                resetJobFolder(jobId)
                succeed(callback, Unit)
            } else {
                scanManifest(jobId, previousKeys = emptySet(), stableScans = 0, attempt = 0, callback = callback)
            }
        }
    }

    /** 当前轮窗口起点；无窗口（老数据/未开始）时返回 null。主线程安全。 */
    fun currentRoundStart(jobId: String): Long? = roundStartByJob[jobId]

    /**
     * 资产/照片是否属于当前执行轮。
     *
     * 设计意图：页面与浮窗计数与媒体下载使用同一轮次口径；`capturedAtMillis` 缺失（老资产）
     * 或没有执行窗口时返回 false，按历史处理。沿用执行窗口的 2s 相机时钟容差。
     */
    fun isCurrentRound(jobId: String, capturedAtMillis: Long): Boolean {
        val start = roundStartByJob[jobId] ?: return false
        return isCapturedInRound(capturedAtMillis, start)
    }

    /**
     * 清单冻结后查询本工单本轮照片的同步进度：总数（本轮引用数）与已下载数。
     *
     * 设计意图：完成判据统一为「本轮已同步数 == 本轮总数」，0 张时 0 == 0 自然成立；
     * 上一轮照片不参与统计、不阻塞本轮完成。全部已下载时不必再申请传输权限、拉取相机列表。
     * 读取不到执行器时按「未同步完」回调，走原拉取流程。
     */
    fun photoSyncCounts(jobId: String, callback: (total: Int, downloaded: Int) -> Unit) {
        if (executor == null) {
            mainHandler.post { callback(1, 0) }
            return
        }
        submit {
            val refs = references[jobId].orEmpty().filter { isCurrentExecutionReference(jobId, it) }
            val total = refs.size
            val downloaded = refs.count(::isDownloaded)
            mainHandler.post { callback(total, downloaded) }
        }
    }

    fun endJob() {
        KeyManager.getInstance().cancelListen(mediaKey, this)
        activeJobId = null
        onNewMedia = null
    }

    fun downloadNewPhotos(
        jobId: String,
        allowPlayback: Boolean,
        callback: ResultCallback<List<Asset>>,
    ) {
        checkInitialized()
        val busyToken = beginBusy("downloadNewPhotos")
        val terminal = terminalCallback(callback, busyToken)
        submit {
            val id = ++operationId
            cancelActiveDownload()
            acquireTransferOwnership(object : ResultCallback<Unit> {
                override fun onSuccess(data: Unit) = submit {
                    if (id == operationId) {
                        pullAndDownload(jobId, id, false, allowPlayback, terminal)
                    } else {
                        terminal.onFailure(IllegalStateException("Download superseded"))
                    }
                }

                override fun onFailure(error: Throwable) = fail(terminal, error)
            })
        }
    }

    fun cancelDownload() {
        endBusy(busyGeneration.get(), "download cancelled")
        submit {
            operationId++
            cancelActiveDownload()
            releaseTransferOwnership()
        }
    }

    fun cancelDownload(assetId: String) {
        submit {
            if (activeDownload?.assetId == assetId) {
                endBusy(busyGeneration.get(), "download cancelled: $assetId")
                operationId++
                cancelActiveDownload()
            }
        }
    }

    fun deleteLocal(asset: Asset): Boolean {
        val file = asset.localUri?.let(::File) ?: return false
        val deleted = !file.exists() || file.delete()
        if (deleted) {
            assets.remove(assetKey(asset.jobId, asset.assetId))
            downloadStateMap.remove(asset.assetId)
            persistAssets()
            publishLocalState()
        }
        return deleted
    }

    /** 释放管理器；先废止在途的启动前采集，再停止下载与清空内存状态。 */
    fun release() {
        cancelBoundaryCapture()
        endJob()
        endBusy(busyGeneration.get(), "released")
        operationId++
        cancelActiveDownload()
        releaseTransferOwnership()
        executor?.shutdownNow()
        executor = null
        sdkManager = null
        appContext = null
        references.clear()
        jobDirectoryIndexes.clear()
        executionWindows.clear()
        finalizingJobIds = emptySet()
        assets.clear()
        downloadStateMap.clear()
        folderConfiguredJobId = null
        transferOwnershipAcquired = false
        publishLocalState()
    }

    private fun recordMedia(info: GeneratedMediaFileInfo) {
        val jobId = activeJobId ?: return
        if (info.type !in PHOTO_TYPES) return
        val directoryIndex = info.dir_no ?: return
        val expectedDirectory = jobDirectoryIndexes[jobId]
        if (expectedDirectory == null) {
            jobDirectoryIndexes[jobId] = directoryIndex
            executionWindows[jobId]?.let { window ->
                executionWindows[jobId] = window.copy(directoryIndex = directoryIndex)
            }
        }
        // 防御迟到事件：窗口已结束或缺失时，先开启本轮窗口，绝不把新照片写进已关闭窗口。
        val currentWindow = executionWindows[jobId]
        if (currentWindow == null ||
            needsFreshExecutionWindow(true, currentWindow.endTime, currentWindow.frozen)
        ) {
            openWindowLocked(jobId, "media-event")
        }
        val snapshot = windowSnapshot(executionWindows[jobId])
        val reference = MediaReference(
            jobId = jobId,
            fileIndex = info.index ?: return,
            directoryIndex = directoryIndex,
            fileNumber = info.file_no ?: 0,
            fileSize = (info.fileSize ?: 0).toLong(),
            fileType = info.type.name,
            createdAt = info.createTime?.toString().orEmpty(),
            windowStartTime = snapshot.startTime,
            windowEndTime = snapshot.endTime,
            windowStartCursor = snapshot.startCursor,
        )
        val jobReferences = references.getOrPut(jobId) { mutableListOf() }
        if (jobReferences.none {
            it.fileIndex == reference.fileIndex && it.directoryIndex == reference.directoryIndex
        }) {
            jobReferences += reference
            downloadStateMap["${reference.directoryIndex}-${reference.fileIndex}"] = MediaDownloadState(
                assetId = "${reference.directoryIndex}-${reference.fileIndex}",
                jobId = jobId,
                status = MediaDownloadStatus.PENDING,
            )
            publishLocalState()
            persistAssets()
            onNewMedia?.let { listener -> mainHandler.postDelayed({ listener(reference) }, NEW_MEDIA_DELAY_MS) }
        }
    }

    private fun pullAndDownload(
        jobId: String,
        id: Long,
        playbackEnabled: Boolean,
        allowPlayback: Boolean,
        callback: ResultCallback<List<Asset>>,
        downloaded: MutableList<Asset> = mutableListOf(),
    ) {
        if (id != operationId) return
        val manager = sdkManager ?: return fail(callback, IllegalStateException("MediaManager is not initialized"))
        manager.pullMediaFileListFromCamera(
            PullMediaFileListParam.Builder()
                .filter(MediaFileFilter.PHOTO)
                .mediaFileIndex(1)
                .count(-1)
                .build(),
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() = submit {
                    if (id != operationId) return@submit
                    val files = manager.mediaFileListData?.data.orEmpty()
                    logFileList("download", jobId, files)
                    val window = executionWindows[jobId]
                    val (currentRound, historical) = partitionReferencesByWindow(
                        references[jobId].orEmpty(),
                        window,
                    )
                    if (historical.isNotEmpty()) {
                        Log.w(
                            TAG,
                            "download round filter job=$jobId window=${describeWindow(window)} " +
                                "historical=${historical.size} " +
                                "refs=${historical.take(MAX_LOGGED_FILES).map(::referenceWindowKey)}",
                        )
                    }
                    val matching = matchingFiles(jobId, files)
                    enrichReferences(jobId, matching)
                    Log.i(
                        TAG,
                        "download match job=$jobId references=${references[jobId].orEmpty().map(::referenceKey)} " +
                            "matching=${matching.map { "${it.dirIndex}-${it.fileIndex}" }}",
                    )

                    when {
                        matching.isNotEmpty() -> pullMissingThumbnails(jobId, matching, id) {
                            downloadFiles(jobId, matching, id, playbackEnabled, allowPlayback, callback, downloaded)
                        }
                        hasPendingReferences(jobId) -> {
                            // 本轮引用若仍在但拍摄时间/游标落在窗口外，给出与「相机列表未就绪」不同的可读原因。
                            val now = System.currentTimeMillis()
                            val excludedCurrent = if (window == null) {
                                emptyList()
                            } else {
                                currentRound.filterNot { referenceWithinWindow(it, window, now) }
                            }
                            val error = if (excludedCurrent.isNotEmpty()) {
                                CategorizedException(
                                    RecognitionFailureCategory.CAPTURE_ERROR,
                                    windowExcludedMessage(excludedCurrent.size),
                                )
                            } else {
                                IllegalStateException("Pending photos are not available in the camera list yet")
                            }
                            handleAttemptFailure(
                                jobId,
                                id,
                                playbackEnabled,
                                allowPlayback,
                                callback,
                                downloaded,
                                error,
                            )
                        }
                        else -> pullMissingThumbnails(jobId, matching, id) {
                            downloadFiles(jobId, matching, id, playbackEnabled, allowPlayback, callback, downloaded)
                        }
                    }
                }

                override fun onFailure(error: IDJIError) = submit {
                    handleAttemptFailure(
                        jobId,
                        id,
                        playbackEnabled,
                        allowPlayback,
                        callback,
                        downloaded,
                        sdkError(error),
                    )
                }
            },
        )
    }

    /**
     * 按媒体管理生命周期采集起始边界：前置检查 → 配置工单相机目录 → 进入媒体管理模式 →
     * 拉取照片列表 → 退出媒体管理模式，全部成功后才提交。
     *
     * [MediaBoundaryCapture] 负责调用顺序与清理漏斗；本方法只负责接线、把目录设置作为有序步骤
     * 交给采集序列（前置检查未通过时不会去设置相机目录），以及成功后的起始边界提交。
     */
    private fun captureStartBoundary(
        jobId: String,
        settings: CustomExpandNameSettings,
        callback: ResultCallback<Unit>,
    ) {
        val media = sdkManager
        if (media == null) {
            boundaryCaptureGate.set(false)
            fail(callback, IllegalStateException("MediaManager is not initialized"))
            return
        }
        val capture = MediaBoundaryCapture(
            mediaManager = media,
            keyManager = KeyManager.getInstance(),
            cameraStreamManager = MediaDataCenter.getInstance().cameraStreamManager,
            cameraIndex = ComponentIndexType.LEFT_OR_MAIN,
        )
        boundaryCapture = capture
        capture.start(
            configureFolder = { done ->
                KeyManager.getInstance().setValue(
                    folderNameKey,
                    settings,
                    object : CommonCallbacks.CompletionCallback {
                        override fun onSuccess() {
                            folderConfiguredJobId = jobId
                            done(Result.success(Unit))
                        }

                        override fun onFailure(error: IDJIError) {
                            done(Result.failure(sdkError(error)))
                        }
                    },
                )
            },
            callback = { result ->
                boundaryCapture = null
                val indices = result.getOrNull()
                if (indices == null) {
                    boundaryCaptureGate.set(false)
                    fail(callback, result.exceptionOrNull() ?: IllegalStateException("媒体准备失败"))
                    return@start
                }
                Log.i(TAG, "start-boundary list job=$jobId count=${indices.size}")
                commitStartBoundary(
                    jobId,
                    indices.maxOrNull() ?: 0,
                    object : ResultCallback<Unit> {
                        override fun onSuccess(data: Unit) {
                            boundaryCaptureGate.set(false)
                            callback.onSuccess(data)
                        }

                        override fun onFailure(error: Throwable) {
                            boundaryCaptureGate.set(false)
                            callback.onFailure(error)
                        }
                    },
                )
            },
        )
    }

    /**
     * 原子提交照片起始边界：先写内存窗口再持久化，持久化失败时回滚到提交前状态。
     * 失败保留原始 `Throwable` 为 cause，分类为 [MediaBoundaryFailureCategory.PERSISTENCE]。
     */
    private fun commitStartBoundary(jobId: String, startCursor: Int, callback: ResultCallback<Unit>) {
        submit {
            val previous = executionWindows[jobId]
            executionWindows[jobId] = ExecutionWindow(
                startTime = System.currentTimeMillis(),
                startCursor = startCursor,
                directoryIndex = jobDirectoryIndexes[jobId],
            )
            try {
                persistAssets()
                Log.i(TAG, "media-prepare: boundary committed job=$jobId startCursor=$startCursor")
                succeed(callback, Unit)
            } catch (error: Throwable) {
                if (previous == null) {
                    executionWindows.remove(jobId)
                } else {
                    executionWindows[jobId] = previous
                }
                Log.w(TAG, "media-prepare: boundary commit failed job=$jobId error=$error")
                fail(
                    callback,
                    MediaBoundaryFailure(MediaBoundaryFailureCategory.PERSISTENCE, "起始边界持久化失败", error),
                )
            }
        }
    }

    private fun scanManifest(
        jobId: String,
        previousKeys: Set<String>,
        stableScans: Int,
        attempt: Int,
        callback: ResultCallback<Unit>,
    ) {
        val manager = sdkManager ?: return fail(callback, IllegalStateException("MediaManager is not initialized"))
        manager.pullMediaFileListFromCamera(allPhotosParam(), object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() = submit {
                val window = executionWindows[jobId]
                    ?: return@submit fail(callback, IllegalStateException("Missing execution window"))
                val files = manager.mediaFileListData?.data.orEmpty()
                logFileList("manifest-$attempt", jobId, files)
                val candidates = files.filter { isWindowCandidate(it, window) }
                Log.i(
                    TAG,
                    "manifest candidates job=$jobId start=${window.startCursor} " +
                        "time=${window.startTime}..${window.endTime} dir=${window.directoryIndex} " +
                        "keys=${candidates.map { "${it.dirIndex}-${it.fileIndex}" }}",
                )
                val snapshot = windowSnapshot(window)
                val jobReferences = references.getOrPut(jobId) { mutableListOf() }
                candidates.forEach { file ->
                    val existing = jobReferences.indexOfFirst {
                        it.fileIndex == file.fileIndex && it.directoryIndex == file.dirIndex
                    }
                    if (existing >= 0) {
                        jobReferences[existing] = file.toReference(jobId, snapshot)
                    } else {
                        jobReferences += file.toReference(jobId, snapshot)
                    }
                }
                val downloadedReferences = jobReferences.filter {
                    isDownloaded(it) && isCurrentWindowReference(window, it)
                }
                val reconciled = (candidates.map { it.toReference(jobId, snapshot) } + downloadedReferences)
                    .distinctBy(::referenceKey)
                    .sortedBy(MediaReference::capturedAtMillis)
                val keys = reconciled.map(::referenceKey).toSet()
                val nextStable = if (keys == previousKeys) stableScans + 1 else 0
                val finished = nextStable >= REQUIRED_STABLE_SCANS || attempt + 1 >= MAX_MANIFEST_SCANS
                executionWindows[jobId] = window.copy(
                    endCursor = keys.mapNotNull { key -> key.substringAfter('-').toIntOrNull() }
                        .maxOrNull() ?: window.startCursor,
                    frozen = finished,
                )
                if (finished) {
                    // 只清理「本轮范围内但相机列表中已不存在」的引用；历史引用保留，不作为本轮清单。
                    val removed = jobReferences.filter {
                        isCurrentWindowReference(window, it) && referenceKey(it) !in keys
                    }
                    jobReferences.removeAll(removed.toSet())
                    removed.forEach { reference ->
                        downloadStateMap.remove(referenceKey(reference))
                        thumbnailFile(jobId, reference.directoryIndex, reference.fileIndex).delete()
                    }
                    resetJobFolder(jobId)
                    Log.i(TAG, "manifest frozen job=$jobId files=${keys.size} discarded=${removed.map(::referenceKey)}")
                }
                persistAssets()
                publishLocalState()
                if (finished) {
                    succeed(callback, Unit)
                } else {
                    mainHandler.postDelayed({
                        submit { scanManifest(jobId, keys, nextStable, attempt + 1, callback) }
                    }, MANIFEST_SCAN_INTERVAL_MS)
                }
            }

            override fun onFailure(error: IDJIError) = submit {
                // 相机列表拉取失败（如 SDK retCode -3）且飞行中未记录到任何照片：照片总数为 0，
                // 无可同步内容，按 0 张冻结清单，让「已同步数 == 总数」统一判定为完成；
                // 已有照片引用时仍视为失败，保留重试，避免漏传。
                val window = executionWindows[jobId]
                if (window != null && references[jobId].orEmpty().isEmpty()) {
                    Log.w(
                        TAG,
                        "manifest list unavailable job=$jobId error=$error, no photo recorded, treat as 0 photos",
                    )
                    executionWindows[jobId] = window.copy(frozen = true)
                    resetJobFolder(jobId)
                    persistAssets()
                    publishLocalState()
                    succeed(callback, Unit)
                } else {
                    fail(callback, sdkError(error))
                }
            }
        })
    }

    private fun allPhotosParam() = PullMediaFileListParam.Builder()
        .filter(MediaFileFilter.PHOTO)
        .mediaFileIndex(1)
        .count(-1)
        .build()

    private fun isWindowCandidate(file: MediaFile, window: ExecutionWindow): Boolean {
        val createdAt = file.date?.toEpochMillis() ?: return false
        return isWithinExecutionWindow(
            fileIndex = file.fileIndex,
            capturedAtMillis = createdAt,
            startCursor = window.startCursor,
            startTime = window.startTime,
            endTime = window.endTime,
            now = System.currentTimeMillis(),
        )
    }

    /** 引用是否属于当前执行轮：快照起点必须等于当前窗口起点。 */
    private fun isCurrentExecutionReference(jobId: String, reference: MediaReference): Boolean =
        isCurrentWindowReference(executionWindows[jobId], reference)

    private fun legacyWindow(jobId: String): ExecutionWindow {
        val jobReferences = references[jobId].orEmpty()
        val firstCapture = jobReferences.minOfOrNull(MediaReference::capturedAtMillis) ?: System.currentTimeMillis()
        val lastCapture = jobReferences.maxOfOrNull(MediaReference::capturedAtMillis) ?: firstCapture
        val firstIndex = jobReferences.minOfOrNull(MediaReference::fileIndex) ?: 1
        return ExecutionWindow(
            startTime = firstCapture - WINDOW_CLOCK_TOLERANCE_MS,
            endTime = lastCapture + WINDOW_FINAL_GRACE_MS,
            startCursor = (firstIndex - 1).coerceAtLeast(0),
            directoryIndex = jobDirectoryIndexes[jobId],
        )
    }

    private fun matchingFiles(jobId: String, files: List<MediaFile>): List<MediaFile> {
        val pending = references[jobId].orEmpty()
            .filter { isCurrentExecutionReference(jobId, it) }
            .filterNot { assets.containsKey(assetKey(jobId, referenceAssetId(it, files))) }
        return pending.mapNotNull { reference ->
            files.firstOrNull {
                it.fileIndex == reference.fileIndex &&
                    it.dirIndex == reference.directoryIndex
            }
        }.distinctBy { it.dirIndex to it.fileIndex }
    }

    private fun isDownloaded(reference: MediaReference): Boolean = assets.values.any { asset ->
        asset.jobId == reference.jobId &&
            asset.meta["fileIndex"] == reference.fileIndex.toString() &&
            asset.meta["dirIndex"] == reference.directoryIndex.toString()
    }

    private fun resetJobFolder(jobId: String) {
        if (folderConfiguredJobId != jobId) return
        folderConfiguredJobId = null
        KeyManager.getInstance().setValue(
            folderNameKey,
            CustomExpandNameSettings(
                EnCodingType.UTF8,
                RelativePosition.POSITION_END,
                "",
                1,
                false,
            ),
            null,
        )
    }

    private fun enrichReferences(jobId: String, files: List<MediaFile>) {
        val jobReferences = references[jobId] ?: return
        files.forEach { file ->
            val index = jobReferences.indexOfFirst {
                it.fileIndex == file.fileIndex && it.directoryIndex == file.dirIndex
            }
            if (index >= 0) {
                // 保留引用产生时的窗口快照：用相机文件名/大小补全时不得把本轮引用降级为历史。
                val existing = jobReferences[index]
                jobReferences[index] = file.toReference(
                    jobId,
                    WindowSnapshot(
                        startTime = existing.windowStartTime,
                        endTime = existing.windowEndTime,
                        startCursor = existing.windowStartCursor,
                    ),
                )
            }
        }
        persistAssets()
        publishLocalState()
    }

    private fun pullMissingThumbnails(
        jobId: String,
        files: List<MediaFile>,
        id: Long,
        complete: () -> Unit,
    ) {
        if (id != operationId) return
        val pending = files.filterNot { thumbnailFile(jobId, it.dirIndex, it.fileIndex).isFile }
        pullThumbnailAt(jobId, pending, 0, id, complete)
    }

    private fun pullThumbnailAt(
        jobId: String,
        files: List<MediaFile>,
        index: Int,
        id: Long,
        complete: () -> Unit,
    ) {
        if (id != operationId) return
        val mediaFile = files.getOrNull(index)
        if (mediaFile == null) {
            complete()
            return
        }
        mediaFile.pullThumbnailFromCamera(object : CommonCallbacks.CompletionCallbackWithParam<Bitmap> {
            override fun onSuccess(data: Bitmap?) = submit {
                if (id != operationId) return@submit
                if (data != null) cacheThumbnail(jobId, mediaFile, data)
                pullThumbnailAt(jobId, files, index + 1, id, complete)
            }

            override fun onFailure(error: IDJIError) = submit {
                Log.w(TAG, "thumbnail failure job=$jobId key=${mediaFile.dirIndex}-${mediaFile.fileIndex} error=$error")
                pullThumbnailAt(jobId, files, index + 1, id, complete)
            }
        })
    }

    private fun cacheThumbnail(jobId: String, mediaFile: MediaFile, bitmap: Bitmap) {
        val target = thumbnailFile(jobId, mediaFile.dirIndex, mediaFile.fileIndex)
        target.parentFile?.mkdirs()
        val part = File(target.parentFile, "${target.name}.part")
        try {
            FileOutputStream(part).use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_JPEG_QUALITY, output))
                output.fd.sync()
            }
            if (target.exists() && !target.delete()) error("Cannot replace ${target.absolutePath}")
            if (!part.renameTo(target)) error("Cannot finalize ${target.absolutePath}")
            publishLocalState()
        } catch (error: Throwable) {
            part.delete()
            Log.w(TAG, "Cannot cache thumbnail ${target.absolutePath}", error)
        }
    }

    private fun referenceAssetId(reference: MediaReference, files: List<MediaFile>): String {
        val file = files.firstOrNull {
            it.fileIndex == reference.fileIndex && it.dirIndex == reference.directoryIndex
        }
        return file?.let(::assetId) ?: "${reference.directoryIndex}-${reference.fileIndex}"
    }

    /** 执行窗口的可读描述，用于排障日志。 */
    private fun describeWindow(window: ExecutionWindow?): String =
        window?.let { "${it.startTime}..${it.endTime}(cursor=${it.startCursor},frozen=${it.frozen})" }
            ?: "none"

    /** 引用键与其窗口快照，用于排障日志。 */
    private fun referenceWindowKey(reference: MediaReference): String =
        "${reference.directoryIndex}-${reference.fileIndex}@" +
            "${reference.windowStartTime}..${reference.windowEndTime}"

    /** 窗口外引用的用户可见原因（中英文资源）。 */
    private fun windowExcludedMessage(count: Int): String =
        appContext?.getString(R.string.fw_media_window_excluded, count)
            ?: "Photos outside the current execution window: $count"

    private fun downloadFiles(
        jobId: String,
        files: List<MediaFile>,
        id: Long,
        playbackEnabled: Boolean,
        allowPlayback: Boolean,
        callback: ResultCallback<List<Asset>>,
        downloaded: MutableList<Asset> = mutableListOf(),
    ) {
        if (id != operationId) return
        if (files.isEmpty()) {
            if (hasPendingReferences(jobId)) {
                handleAttemptFailure(
                    jobId,
                    id,
                    playbackEnabled,
                    allowPlayback,
                    callback,
                    downloaded,
                    IllegalStateException("Some task photos are still pending"),
                )
            } else {
                finishAttempt(playbackEnabled) { succeed(callback, downloaded) }
            }
            return
        }
        downloadFile(jobId, files.first(), id, object : ResultCallback<Asset> {
            override fun onSuccess(data: Asset) = submit {
                downloaded += data
                downloadFiles(jobId, files.drop(1), id, playbackEnabled, allowPlayback, callback, downloaded)
            }

            override fun onFailure(error: Throwable) = submit {
                handleAttemptFailure(jobId, id, playbackEnabled, allowPlayback, callback, downloaded, error)
            }
        })
    }

    private fun downloadFile(jobId: String, mediaFile: MediaFile, id: Long, callback: ResultCallback<Asset>) {
        val root = assetRoot() ?: return callback.onFailure(IllegalStateException("MediaManager is not initialized"))
        val jobDir = File(root, safeName(jobId)).apply { mkdirs() }
        val assetId = assetId(mediaFile)
        val part = File(jobDir, "$assetId.part")
        val target = File(jobDir, assetId)
        if (part.length() > mediaFile.fileSize) part.delete()
        val output = try {
            RandomAccessFile(part, "rw")
        } catch (error: Throwable) {
            callback.onFailure(error)
            return
        }
        val offset = part.length()
        output.seek(offset)
        activeDownload = ActiveDownload(mediaFile, output, assetId, jobId)
        updateDownloadState(assetId, jobId, MediaDownloadStatus.DOWNLOADING, 0)
        Log.i(
            TAG,
            "original request job=$jobId key=${mediaFile.dirIndex}-${mediaFile.fileIndex} " +
                "name=${mediaFile.fileName} size=${mediaFile.fileSize} offset=$offset",
        )
        mediaFile.pullOriginalMediaFileFromCamera(offset, object : MediaFileDownloadListener {
            override fun onStart() {
                Log.i(TAG, "original start job=$jobId key=${mediaFile.dirIndex}-${mediaFile.fileIndex}")
            }
            override fun onProgress(total: Long, current: Long) = submit {
                val fullSize = offset + total
                val downloaded = offset + current
                val percent = if (fullSize > 0) ((downloaded * 100 / fullSize).coerceAtMost(100)).toInt() else 0
                updateDownloadState(assetId, jobId, MediaDownloadStatus.DOWNLOADING, percent)
                Log.i(
                    TAG,
                    "original progress job=$jobId key=${mediaFile.dirIndex}-${mediaFile.fileIndex} " +
                        "current=$current total=$total percent=$percent",
                )
            }

            override fun onRealtimeDataUpdate(data: ByteArray, position: Long) = submit {
                val active = activeDownload
                if (id != operationId || active?.file !== mediaFile) return@submit
                try {
                    // MSDK Sample 按回调顺序追加数据；position 不是稳定的本地写入偏移。
                    active.output.write(data)
                } catch (error: Throwable) {
                    failActive(callback, error)
                }
            }

            override fun onFinish() = submit {
                val active = activeDownload
                if (id != operationId || active?.file !== mediaFile) return@submit
                try {
                    active.output.fd.sync()
                    active.output.close()
                    activeDownload = null
                    if (part.length() != mediaFile.fileSize) {
                        throw IllegalStateException("Downloaded ${part.length()} bytes, expected ${mediaFile.fileSize}")
                    }
                    if (target.exists() && !target.delete()) {
                        throw IllegalStateException("Cannot replace ${target.absolutePath}")
                    }
                    if (!part.renameTo(target)) {
                        throw IllegalStateException("Cannot finalize ${target.absolutePath}")
                    }
                    val asset = Asset(
                        assetId = assetId,
                        jobId = jobId,
                        typeId = "photo",
                        localUri = target.absolutePath,
                        sizeBytes = target.length(),
                        md5 = md5(target),
                        state = AssetState.DETECTED,
                        meta = mapOf(
                            "camera" to ComponentIndexType.LEFT_OR_MAIN.name,
                            "storage" to CameraStorageLocation.SDCARD.name,
                            "dirIndex" to mediaFile.dirIndex.toString(),
                            "fileIndex" to mediaFile.fileIndex.toString(),
                            "fileName" to mediaFile.fileName,
                            "capturedAtMillis" to (mediaFile.date?.toEpochMillis() ?: 0L).toString(),
                            "groupKey" to photoGroupKey(
                                mediaFile.dirIndex,
                                mediaFile.fileName,
                                mediaFile.fileIndex,
                            ),
                        ),
                    )
                    assets[assetKey(jobId, assetId)] = asset
                    updateDownloadState(assetId, jobId, MediaDownloadStatus.DOWNLOADED, 100)
                    persistAssets()
                    publishLocalState()
                    callback.onSuccess(asset)
                    Log.i(
                        TAG,
                        "original finish job=$jobId key=${mediaFile.dirIndex}-${mediaFile.fileIndex} " +
                            "bytes=${target.length()}",
                    )
                } catch (error: Throwable) {
                    activeDownload = null
                    runCatching { active.output.close() }
                    callback.onFailure(error)
                }
            }

            override fun onFailure(error: IDJIError) = submit {
                Log.e(TAG, "original failure job=$jobId key=${mediaFile.dirIndex}-${mediaFile.fileIndex} error=$error")
                updateDownloadState(assetId, jobId, MediaDownloadStatus.FAILED, error = error.toString())
                failActive(callback, sdkError(error))
            }
        })
    }

    private fun handleAttemptFailure(
        jobId: String,
        id: Long,
        playbackEnabled: Boolean,
        allowPlayback: Boolean,
        callback: ResultCallback<List<Asset>>,
        downloaded: MutableList<Asset>,
        error: Throwable,
    ) {
        if (id != operationId) return
        cancelActiveDownload()
        if (!playbackEnabled && allowPlayback) {
            val manager = sdkManager ?: return fail(callback, error)
            manager.enable(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() = submit { pullAndDownload(jobId, id, true, false, callback, downloaded) }
                override fun onFailure(enableError: IDJIError) = submit { fail(callback, sdkError(enableError)) }
            })
        } else {
            finishAttempt(playbackEnabled) { fail(callback, error) }
        }
    }

    private fun finishAttempt(playbackEnabled: Boolean, complete: () -> Unit) {
        if (!playbackEnabled) {
            releaseTransferOwnership(complete)
            return
        }
        val manager = sdkManager
        if (manager == null) {
            complete()
            return
        }
        manager.disable(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() = submit { releaseTransferOwnership(complete) }
            override fun onFailure(error: IDJIError) = submit { releaseTransferOwnership(complete) }
        })
    }

    private fun acquireTransferOwnership(callback: ResultCallback<Unit>) {
        if (transferOwnershipAcquired || !ProductUtil.isIndustryV3()) {
            callback.onSuccess(Unit)
            return
        }
        val rcMode = KeyManager.getInstance().getValue(
            KeyTools.createKey(RemoteControllerKey.KeyRcMachineMode),
            RCMode.UNKNOWN,
        )
        val owner = when (rcMode) {
            RCMode.CHANNEL_A -> MultiControlChannel.CHANNEL_A
            RCMode.CHANNEL_B -> MultiControlChannel.CHANNEL_B
            else -> MultiControlChannel.UNKNOWN
        }
        val payloadIndex = payloadIndexForExclusiveTask()
        val message = MultiRCExclusiveTaskAuthOwnerMsg(owner, payloadIndex)
        KeyManager.getInstance().performAction(
            KeyTools.createKey(RemoteControllerKey.KeySetExclusiveTaskAuthOwner),
            message,
            object : CommonCallbacks.CompletionCallbackWithParam<Boolean> {
                override fun onSuccess(data: Boolean?) {
                    transferOwnershipAcquired = true
                    Log.i(TAG, "media transfer ownership acquired owner=$owner payload=$payloadIndex")
                    callback.onSuccess(Unit)
                }

                override fun onFailure(error: IDJIError) {
                    Log.e(TAG, "media transfer ownership failed error=$error")
                    callback.onFailure(sdkError(error))
                }
            },
        )
    }

    private fun releaseTransferOwnership(complete: () -> Unit = {}) {
        if (!transferOwnershipAcquired || !ProductUtil.isIndustryV3()) {
            transferOwnershipAcquired = false
            complete()
            return
        }
        transferOwnershipAcquired = false
        val payloadIndex = payloadIndexForExclusiveTask()
        val message = MultiRCExclusiveTaskAuthOwnerMsg(MultiControlChannel.UNKNOWN, payloadIndex)
        KeyManager.getInstance().performAction(
            KeyTools.createKey(RemoteControllerKey.KeySetExclusiveTaskAuthOwner),
            message,
            object : CommonCallbacks.CompletionCallbackWithParam<Boolean> {
                override fun onSuccess(data: Boolean?) = complete()
                override fun onFailure(error: IDJIError) = complete()
            },
        )
    }

    /**
        * 多控独占任务需要登记的实际载荷索引。
        *
        * 设计意图：媒体传输属独占任务，索引必须与机型对应的相机载荷一致。机型 169 登记
        * `LEFT_OR_MAIN` 时相机侧会拒绝媒体文件列表请求（`MEDIA_MANAGER_FETCH_FILE_LIST_FAILED`），
        * 照片拍完也取不回；该机型需用 `RIGHT`，与 `open-pilot-flighttask-sample` 的机型分支一致。
        *
        * 能力契约：判断依据是 `ProductUtil.productType()` 的枚举名；该值缺失时 SDK 返回
        * `UNRECOGNIZED`，自然落到 `LEFT_OR_MAIN`（保持修复前行为）。获取与释放必须使用同一结果。
        * 说明：SDK 5.21 另提供机型专用判断方法，但当前知识镜像基于 5.20.1、未收录该符号，
        * 故这里按机型枚举名判断（见 design「API 校验」）。
        */
    private fun payloadIndexForExclusiveTask(): ComponentIndexType =
        exclusiveTaskPayloadIndex(ProductUtil.productType().name)

    private fun failActive(callback: ResultCallback<Asset>, error: Throwable) {
        cancelActiveDownload()
        callback.onFailure(error)
    }

    private fun cancelActiveDownload() {
        val active = activeDownload ?: return
        activeDownload = null
        runCatching { active.output.close() }
        active.file.stopPullOriginalMediaFileFromCamera(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() = Unit
            override fun onFailure(error: IDJIError) = Unit
        })
    }

    private fun loadAssets() {
        assets.clear()
        references.clear()
        val file = indexFile() ?: return
        if (!file.exists()) return
        runCatching {
            val root = JSONObject(file.readText())
            val array = root.optJSONArray("assets") ?: JSONArray()
            for (i in 0 until array.length()) {
                val json = array.getJSONObject(i)
                val meta = linkedMapOf<String, String>()
                json.optJSONObject("meta")?.let { values ->
                    values.keys().forEach { key -> meta[key] = values.getString(key) }
                }
                val asset = Asset(
                    assetId = json.getString("assetId"),
                    jobId = json.getString("jobId"),
                    typeId = json.getString("typeId"),
                    localUri = json.optString("localUri").ifBlank { null },
                    sizeBytes = json.getLong("sizeBytes"),
                    md5 = json.optString("md5").ifBlank { null },
                    state = AssetState.valueOf(json.optString("state", AssetState.DETECTED.name)),
                    meta = meta,
                )
                if (asset.localUri?.let(::File)?.exists() == true) assets[assetKey(asset.jobId, asset.assetId)] = asset
            }
            val referenceArray = root.optJSONArray("references") ?: JSONArray()
            for (i in 0 until referenceArray.length()) {
                val json = referenceArray.getJSONObject(i)
                val reference = MediaReference(
                    jobId = json.getString("jobId"),
                    fileIndex = json.getInt("fileIndex"),
                    directoryIndex = json.getInt("directoryIndex"),
                    fileNumber = json.optInt("fileNumber"),
                    fileSize = json.optLong("fileSize"),
                    fileType = json.optString("fileType"),
                    createdAt = json.optString("createdAt"),
                    fileName = json.optString("fileName"),
                    capturedAtMillis = json.optLong("capturedAtMillis"),
                    windowStartTime = json.optLong("windowStartTime"),
                    windowEndTime = json.optLong("windowEndTime"),
                    windowStartCursor = json.optInt("windowStartCursor"),
                )
                references.getOrPut(reference.jobId) { mutableListOf() }.add(reference)
            }
            root.optJSONObject("jobDirectoryIndexes")?.let { indexes ->
                indexes.keys().forEach { jobId -> jobDirectoryIndexes[jobId] = indexes.getInt(jobId) }
            }
            root.optJSONObject("executionWindows")?.let { windows ->
                windows.keys().forEach { jobId ->
                    val json = windows.getJSONObject(jobId)
                    executionWindows[jobId] = ExecutionWindow(
                        startTime = json.getLong("startTime"),
                        endTime = json.optLong("endTime"),
                        startCursor = json.optInt("startCursor"),
                        endCursor = json.optInt("endCursor"),
                        directoryIndex = if (json.isNull("directoryIndex")) null else json.getInt("directoryIndex"),
                        frozen = json.optBoolean("frozen"),
                    )
                }
            }
            if (migrateLegacyReferencesLocked()) persistAssets()
        }
    }

    /**
     * 旧缓存兼容：把「窗口开放且拍摄时间落在窗口内」的无快照引用补写为本轮快照。
     *
     * 设计意图：升级前产生的引用没有窗口快照，无法区分轮次；只有在窗口仍开放、且拍摄时间
     * 落在当前窗口内时才视为本轮，避免把上一轮照片重新纳入。窗口已关闭或时间不符时保持
     * 历史状态，不删除引用与资产。
     */
    private fun migrateLegacyReferencesLocked(): Boolean {
        var changed = false
        val now = System.currentTimeMillis()
        references.forEach { (jobId, jobRefs) ->
            val window = executionWindows[jobId] ?: return@forEach
            if (needsFreshExecutionWindow(true, window.endTime, window.frozen)) return@forEach
            jobRefs.forEachIndexed { index, reference ->
                if (reference.windowStartTime > 0L) return@forEachIndexed
                if (!referenceWithinWindow(reference, window, now)) return@forEachIndexed
                jobRefs[index] = reference.copy(
                    windowStartTime = window.startTime,
                    windowEndTime = window.endTime,
                    windowStartCursor = window.startCursor,
                )
                changed = true
            }
        }
        return changed
    }

    private fun persistAssets() {
        val file = indexFile() ?: return
        file.parentFile?.mkdirs()
        val array = JSONArray()
        assets.values.forEach { asset ->
            array.put(JSONObject().apply {
                put("assetId", asset.assetId)
                put("jobId", asset.jobId)
                put("typeId", asset.typeId)
                put("localUri", asset.localUri ?: "")
                put("sizeBytes", asset.sizeBytes)
                put("md5", asset.md5 ?: "")
                put("state", asset.state.name)
                put("meta", JSONObject(asset.meta))
            })
        }
        val referenceArray = JSONArray()
        references.values.flatten().forEach { reference ->
            referenceArray.put(JSONObject().apply {
                put("jobId", reference.jobId)
                put("fileIndex", reference.fileIndex)
                put("directoryIndex", reference.directoryIndex)
                put("fileNumber", reference.fileNumber)
                put("fileSize", reference.fileSize)
                put("fileType", reference.fileType)
                put("createdAt", reference.createdAt)
                put("fileName", reference.fileName)
                put("capturedAtMillis", reference.capturedAtMillis)
                put("windowStartTime", reference.windowStartTime)
                put("windowEndTime", reference.windowEndTime)
                put("windowStartCursor", reference.windowStartCursor)
            })
        }
        val directoryIndexes = JSONObject()
        jobDirectoryIndexes.forEach { (jobId, directoryIndex) -> directoryIndexes.put(jobId, directoryIndex) }
        val windows = JSONObject()
        executionWindows.forEach { (jobId, window) ->
            windows.put(jobId, JSONObject().apply {
                put("startTime", window.startTime)
                put("endTime", window.endTime)
                put("startCursor", window.startCursor)
                put("endCursor", window.endCursor)
                put("directoryIndex", window.directoryIndex ?: JSONObject.NULL)
                put("frozen", window.frozen)
            })
        }
        atomicWrite(
            file,
            JSONObject()
                .put("assets", array)
                .put("references", referenceArray)
                .put("jobDirectoryIndexes", directoryIndexes)
                .put("executionWindows", windows)
                .toString(),
        )
    }

    private fun hasPendingReferences(jobId: String): Boolean {
        return references[jobId].orEmpty().any {
            isCurrentExecutionReference(jobId, it) && !isDownloaded(it)
        }
    }

    private fun updateDownloadState(
        assetId: String,
        jobId: String,
        status: MediaDownloadStatus,
        progress: Int = 0,
        error: String? = null,
    ) {
        downloadStateMap[assetId] = MediaDownloadState(assetId, jobId, status, progress, error)
        publishLocalState()
    }

    private fun publishLocalState() {
        roundStartByJob = executionWindows.mapValues { (_, window) -> window.startTime }
        mutableLocalAssets.postValue(assets.values.toList())
        val allReferences = references.values.flatten()
        mutableMediaReferences.postValue(allReferences)
        mutableManifestAssets.postValue(
            allReferences.filter {
                it.jobId in finalizingJobIds && isCurrentExecutionReference(it.jobId, it)
            }.map { reference ->
                Asset(
                    assetId = referenceKey(reference),
                    jobId = reference.jobId,
                    typeId = "photo",
                    localUri = null,
                    sizeBytes = reference.fileSize,
                    md5 = null,
                    state = AssetState.DETECTED,
                    meta = mapOf(
                        "dirIndex" to reference.directoryIndex.toString(),
                        "fileIndex" to reference.fileIndex.toString(),
                        "fileName" to reference.fileName.ifBlank { referenceKey(reference) },
                        "capturedAtMillis" to reference.capturedAtMillis.toString(),
                    ),
                )
            },
        )
        mutableThumbnailPaths.postValue(allReferences.mapNotNull { reference ->
            val file = thumbnailFile(reference.jobId, reference.directoryIndex, reference.fileIndex)
            if (file.isFile) {
                mediaKey(reference.jobId, reference.directoryIndex, reference.fileIndex) to file.absolutePath
            } else {
                null
            }
        }.toMap())
        mutableDownloadStates.postValue(downloadStateMap.toMap())
        mutablePendingCount.postValue(references.entries.sumOf { (jobId, jobRefs) ->
            jobRefs.count { ref -> isCurrentExecutionReference(jobId, ref) && !isDownloaded(ref) }
        })
    }

    private fun assetRoot() = appContext?.let { File(it.filesDir, "flight-assets") }
    private fun indexFile() = assetRoot()?.let { File(it, "index.json") }
    private fun thumbnailFile(jobId: String, directoryIndex: Int, fileIndex: Int): File =
        File(File(File(requireNotNull(assetRoot()), safeName(jobId)), ".thumbs"), "$directoryIndex-$fileIndex.jpg")
    private fun assetKey(jobId: String, assetId: String) = "$jobId/$assetId"
    private fun assetId(file: MediaFile): String = safeName("${file.dirIndex}-${file.fileIndex}-${file.fileName}")
    private fun safeName(value: String) = value.replace(Regex("[^A-Za-z0-9._-]"), "_")
    private fun referenceKey(reference: MediaReference) = "${reference.directoryIndex}-${reference.fileIndex}"
    private fun mediaKey(jobId: String, directoryIndex: Int, fileIndex: Int) = "$jobId/$directoryIndex/$fileIndex"

    private fun discardUnresolvedReferences(jobId: String) {
        val completeKeys = assets.values.filter { it.jobId == jobId }.mapNotNull { asset ->
            val directoryIndex = asset.meta["dirIndex"]?.toIntOrNull() ?: return@mapNotNull null
            val fileIndex = asset.meta["fileIndex"]?.toIntOrNull() ?: return@mapNotNull null
            "$directoryIndex-$fileIndex"
        }.toSet()
        val removed = references[jobId].orEmpty().filter { referenceKey(it) !in completeKeys }
        if (removed.isEmpty()) return
        references[jobId]?.removeAll(removed.toSet())
        removed.forEach { reference ->
            downloadStateMap.remove(referenceKey(reference))
            thumbnailFile(jobId, reference.directoryIndex, reference.fileIndex).delete()
            val jobDir = File(requireNotNull(assetRoot()), safeName(jobId))
            jobDir.listFiles()?.filter { it.name.startsWith(referenceKey(reference)) && it.name.endsWith(".part") }
                ?.forEach(File::delete)
        }
        persistAssets()
    }

    private fun logFileList(stage: String, jobId: String, files: List<MediaFile>) {
        Log.i(
            TAG,
            "$stage list job=$jobId count=${files.size} files=${files.take(MAX_LOGGED_FILES).map { file ->
                "${file.dirIndex}-${file.fileIndex}:${file.fileName}:${file.fileSize}:${file.date}"
            }}",
        )
    }

    private fun MediaFile.toReference(
        jobId: String,
        snapshot: WindowSnapshot = WindowSnapshot(0L, 0L, 0),
    ) = MediaReference(
        jobId = jobId,
        fileIndex = fileIndex,
        directoryIndex = dirIndex,
        fileNumber = 0,
        fileSize = fileSize,
        fileType = fileType.name,
        createdAt = date?.toString().orEmpty(),
        fileName = fileName,
        capturedAtMillis = date?.toEpochMillis() ?: System.currentTimeMillis(),
        windowStartTime = snapshot.startTime,
        windowEndTime = snapshot.endTime,
        windowStartCursor = snapshot.startCursor,
    )

    private fun dji.sdk.keyvalue.value.camera.DateTime.toEpochMillis(): Long = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, second)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun md5(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun atomicWrite(file: File, value: String) {
        val atomicFile = AtomicFile(file)
        val output = atomicFile.startWrite()
        try {
            output.write(value.toByteArray())
            atomicFile.finishWrite(output)
        } catch (error: Throwable) {
            atomicFile.failWrite(output)
            throw error
        }
    }

    private fun submit(action: () -> Unit) {
        executor?.execute { runCatching(action) }
    }

    /** 启动一次媒体占用；返回令牌，只有最新令牌可以结束占用。 */
    private fun beginBusy(reason: String): Long {
        val token = busyGeneration.incrementAndGet()
        Log.i(TAG, "media busy=true reason=$reason")
        mutableBusy.postValue(true)
        return token
    }

    /** 结束媒体占用；已被更新的操作接管时忽略。 */
    private fun endBusy(token: Long, reason: String) {
        if (busyGeneration.get() != token) return
        Log.i(TAG, "media busy=false reason=$reason")
        mutableBusy.postValue(false)
    }

    /** 把下载回调收敛为单一终态，并释放媒体占用。 */
    private fun terminalCallback(
        callback: ResultCallback<List<Asset>>,
        busyToken: Long,
    ): ResultCallback<List<Asset>> = object : ResultCallback<List<Asset>> {
        private val finished = AtomicBoolean(false)

        override fun onSuccess(data: List<Asset>) {
            if (finished.compareAndSet(false, true)) {
                endBusy(busyToken, "download complete")
                callback.onSuccess(data)
            }
        }

        override fun onFailure(error: Throwable) {
            if (finished.compareAndSet(false, true)) {
                endBusy(busyToken, "download failed")
                callback.onFailure(error)
            }
        }
    }

    private fun <T> succeed(callback: ResultCallback<T>, value: T) {
        mainHandler.post { callback.onSuccess(value) }
    }

    private fun <T> fail(callback: ResultCallback<T>, error: Throwable) {
        mainHandler.post { callback.onFailure(error) }
    }
    private fun sdkError(error: IDJIError) = IllegalStateException(error.toString())
    private fun checkInitialized() = check(executor != null && sdkManager != null) { "Call MediaManager.init first" }

    private data class ActiveDownload(
        val file: MediaFile,
        val output: RandomAccessFile,
        val assetId: String,
        val jobId: String,
    )

    private val PHOTO_TYPES = setOf(
        MediaFileType.JPEG,
        MediaFileType.DNG,
        MediaFileType.PANORAMA,
        MediaFileType.TIFF,
    )

    private const val NEW_MEDIA_DELAY_MS = 800L
    private const val MAX_FOLDER_SUFFIX_LENGTH = 40
    private const val MANIFEST_SCAN_INTERVAL_MS = 1_000L
    private const val REQUIRED_STABLE_SCANS = 2
    private const val MAX_MANIFEST_SCANS = 10
    private const val MAX_LOGGED_FILES = 30
    private const val THUMBNAIL_JPEG_QUALITY = 85
    private const val TAG = "PatrolMedia"
}

/** 机型 169 的 SDK 产品类型枚举名（`ProductType.NOT_SUPPORTED_169`），用于独占任务载荷选择。 */
private const val PRODUCT_TYPE_169 = "NOT_SUPPORTED_169"

/**
 * 多控独占任务载荷索引映射（纯函数）。
 *
 * 设计意图：把「机型 → 载荷索引」从 SDK 读取逻辑中剥离，便于 JVM 单测覆盖；机型 169 使用
 * `RIGHT`，其余机型沿用 `LEFT_OR_MAIN`。机型名缺失或未知时保持默认，不改变未受影响机型的行为。
 */
internal fun exclusiveTaskPayloadIndex(productName: String): ComponentIndexType =
    if (productName == PRODUCT_TYPE_169) ComponentIndexType.RIGHT else ComponentIndexType.LEFT_OR_MAIN

/** 执行窗口的时间容差：相机写入时间与插件接收时间可能相差数秒。 */
private const val WINDOW_CLOCK_TOLERANCE_MS = 2_000L

/**
 * 照片拍摄时间是否属于以 [windowStart] 开始的本轮（纯函数）。
 *
 * 设计意图：页面/浮窗过滤与媒体窗口使用同一 2s 时钟容差；拍摄时间未知（旧资产）时返回
 * false，按历史处理，避免把上一轮照片算进本轮。
 */
internal fun isCapturedInRound(capturedAtMillis: Long, windowStart: Long): Boolean =
    capturedAtMillis > 0L && capturedAtMillis >= windowStart - WINDOW_CLOCK_TOLERANCE_MS

/** 收尾宽限：清单冻结后短暂延迟内生成的媒体仍属于本次执行。 */
private const val WINDOW_FINAL_GRACE_MS = 2_000L

/**
 * 文件/引用是否落在本次执行窗口内（纯函数）。
 *
 * 设计意图：飞行中下载、待处理判定与收尾清单必须用同一口径，避免同一工单历史执行的照片
 * 被当成本次任务下载。`capturedAtMillis <= 0` 表示拍摄时间未知（旧缓存），一律不通过。
 */
internal fun isWithinExecutionWindow(
    fileIndex: Int,
    capturedAtMillis: Long,
    startCursor: Int,
    startTime: Long,
    endTime: Long,
    now: Long,
): Boolean {
    if (fileIndex <= startCursor) return false
    if (capturedAtMillis <= 0L) return false
    val windowEnd = if (endTime > 0L) endTime else now
    return capturedAtMillis >= startTime - WINDOW_CLOCK_TOLERANCE_MS &&
        capturedAtMillis <= windowEnd + WINDOW_FINAL_GRACE_MS
}

/**
 * 一次航线执行对应的照片归集区间。
 *
 * 设计意图：区分「本轮执行拍摄的照片」与相机中的历史照片；`endTime > 0` 或 `frozen` 表示
 * 该轮执行已结束，后续执行必须建立新窗口而不是复用它。
 */
internal data class ExecutionWindow(
    val startTime: Long,
    val endTime: Long = 0,
    val startCursor: Int = 0,
    val endCursor: Int = 0,
    val directoryIndex: Int? = null,
    val frozen: Boolean = false,
)

/**
 * 引用携带的窗口快照（纯函数输出）。
 *
 * 设计意图：把「当前窗口 → 引用字段」的映射从记录逻辑中剥离，便于单测覆盖；无窗口时写 0。
 * 本轮判定要求快照起点与当前窗口起点一致，缺少快照的引用按历史处理。
 */
internal data class WindowSnapshot(
    val startTime: Long,
    val endTime: Long,
    val startCursor: Int,
)

/** 读取当前执行窗口的快照；窗口缺失时返回全 0 快照。 */
internal fun windowSnapshot(window: ExecutionWindow?): WindowSnapshot =
    WindowSnapshot(
        startTime = window?.startTime ?: 0L,
        endTime = window?.endTime ?: 0L,
        startCursor = window?.startCursor ?: 0,
    )

/**
 * 是否需要为当前执行建立新窗口（纯函数）。
 *
 * 能力契约：无窗口、窗口已写入结束时间或已冻结时返回 `true`；窗口未闭合（同一轮执行）返回
 * `false`，用于「同工单重复加载」保持幂等。
 */
internal fun needsFreshExecutionWindow(
    hasWindow: Boolean,
    endTime: Long,
    frozen: Boolean,
): Boolean = !hasWindow || endTime > 0L || frozen

/**
 * 引用是否属于当前执行轮（纯函数）。
 *
 * 设计意图：执行窗口每次轮换都会写入新的起点；只有快照起点与当前窗口起点完全一致的引用
 * 才属于本轮。没有快照（旧缓存）或快照来自上一轮时一律视为历史，避免新一轮重复下载、重复
 * 统计或阻塞完成。
 */
internal fun isCurrentWindowReference(window: ExecutionWindow?, reference: MediaReference): Boolean =
    window != null && reference.windowStartTime > 0L && reference.windowStartTime == window.startTime

/**
 * 引用是否落在指定窗口的时间/游标区间内（纯函数）。
 *
 * 说明：本轮判定由 [isCurrentWindowReference] 完成；本函数只用于识别「本轮引用但时间/游标
 * 越界」的异常并提供可区分的原因，不参与跨轮隔离。
 */
internal fun referenceWithinWindow(
    reference: MediaReference,
    window: ExecutionWindow,
    now: Long,
): Boolean = isWithinExecutionWindow(
    fileIndex = reference.fileIndex,
    capturedAtMillis = reference.capturedAtMillis,
    startCursor = window.startCursor,
    startTime = window.startTime,
    endTime = window.endTime,
    now = now,
)

/**
 * 把引用按「本轮」与「历史」分区（纯函数）。
 *
 * 设计意图：下载与待处理判定必须使用同一口径；历史引用可被记录数量，但不参与本轮流程。
 */
internal fun partitionReferencesByWindow(
    references: List<MediaReference>,
    jobWindow: ExecutionWindow?,
): Pair<List<MediaReference>, List<MediaReference>> {
    val current = references.filter { isCurrentWindowReference(jobWindow, it) }
    val historical = references.filterNot { isCurrentWindowReference(jobWindow, it) }
    return current to historical
}

/**
 * 一次快门的照片组键：同一组多镜头文件共享文件名前缀（如 `DJI_20261021194406_0001`）。
 *
 * 能力契约：文件名符合 `*_W/_Z/_T.JPG` 命名时按前缀归组；无文件名或无前缀时退回文件序号，
 * 避免把不同文件错误合并。
 */
internal fun photoGroupKey(dirIndex: Int, fileName: String, fileIndex: Int): String {
    val prefix = if (fileName.contains('_')) fileName.substringBeforeLast('_') else ""
    return if (prefix.isNotBlank()) "$dirIndex/$prefix" else "$dirIndex-$fileIndex"
}
