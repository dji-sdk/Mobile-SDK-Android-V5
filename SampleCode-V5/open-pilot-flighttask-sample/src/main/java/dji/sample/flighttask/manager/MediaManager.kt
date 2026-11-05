package dji.sample.flighttask.manager

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.AtomicFile
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dji.sample.flighttask.data.Asset
import dji.sample.flighttask.data.AssetState
import dji.sample.flighttask.data.MediaReference
import dji.sample.flighttask.data.MediaDownloadState
import dji.sample.flighttask.data.MediaDownloadStatus
import dji.sample.flighttask.data.JobStatus
import dji.sample.flighttask.data.ResultCallback
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
    private var finalizingJobIds: Set<String> = emptySet()
    private val assets = linkedMapOf<String, Asset>()
    private val downloadStateMap = linkedMapOf<String, MediaDownloadState>()
    private var activeDownload: ActiveDownload? = null
    private var operationId = 0L
    private var folderConfiguredJobId: String? = null
    private var transferOwnershipAcquired = false

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
            discardUnresolvedReferences(jobId)
            executionWindows.remove(jobId)
            jobDirectoryIndexes.remove(jobId)
            if (folderConfiguredJobId == jobId) folderConfiguredJobId = null
            persistAssets()
            publishLocalState()
            mainHandler.post(callback)
        }
    }

    fun prepareJobFolder(jobId: String, callback: ResultCallback<Unit>) {
        checkInitialized()
        if (folderConfiguredJobId == jobId && executionWindows[jobId] != null) {
            succeed(callback, Unit)
            return
        }
        val settings = CustomExpandNameSettings(
            EnCodingType.UTF8,
            RelativePosition.POSITION_END,
            safeName(jobId).take(MAX_FOLDER_SUFFIX_LENGTH),
            1,
            true,
        )
        KeyManager.getInstance().setValue(folderNameKey, settings, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                folderConfiguredJobId = jobId
                captureStartBoundary(jobId, callback)
            }

            override fun onFailure(error: IDJIError) {
                fail(callback, sdkError(error))
            }
        })
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
        submit {
            val id = ++operationId
            cancelActiveDownload()
            acquireTransferOwnership(object : ResultCallback<Unit> {
                override fun onSuccess(data: Unit) = submit {
                    if (id == operationId) pullAndDownload(jobId, id, false, allowPlayback, callback)
                }

                override fun onFailure(error: Throwable) = fail(callback, error)
            })
        }
    }

    fun cancelDownload() {
        submit {
            operationId++
            cancelActiveDownload()
            releaseTransferOwnership()
        }
    }

    fun cancelDownload(assetId: String) {
        submit {
            if (activeDownload?.assetId == assetId) {
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

    fun release() {
        endJob()
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
        val reference = MediaReference(
            jobId = jobId,
            fileIndex = info.index ?: return,
            directoryIndex = directoryIndex,
            fileNumber = info.file_no ?: 0,
            fileSize = (info.fileSize ?: 0).toLong(),
            fileType = info.type.name,
            createdAt = info.createTime?.toString().orEmpty(),
        )
        val jobReferences = references.getOrPut(jobId) { mutableListOf() }
        if (jobReferences.none { it.fileIndex == reference.fileIndex && it.directoryIndex == reference.directoryIndex }) {
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
                    val matching = matchingFiles(jobId, files)
                    enrichReferences(jobId, matching)
                    Log.i(TAG, "download match job=$jobId references=${references[jobId].orEmpty().map(::referenceKey)} matching=${matching.map { "${it.dirIndex}-${it.fileIndex}" }}")
                    if (matching.isEmpty() && hasPendingReferences(jobId)) {
                        handleAttemptFailure(
                            jobId,
                            id,
                            playbackEnabled,
                            allowPlayback,
                            callback,
                            downloaded,
                            IllegalStateException("Pending photos are not available in the camera list yet"),
                        )
                    } else {
                        pullMissingThumbnails(jobId, matching, id) {
                            downloadFiles(jobId, matching, id, playbackEnabled, allowPlayback, callback, downloaded)
                        }
                    }
                }

                override fun onFailure(error: IDJIError) = submit {
                    handleAttemptFailure(jobId, id, playbackEnabled, allowPlayback, callback, downloaded, sdkError(error))
                }
            },
        )
    }

    private fun captureStartBoundary(jobId: String, callback: ResultCallback<Unit>) {
        val manager = sdkManager ?: return fail(callback, IllegalStateException("MediaManager is not initialized"))
        manager.pullMediaFileListFromCamera(allPhotosParam(), object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() = submit {
                val files = manager.mediaFileListData?.data.orEmpty()
                logFileList("start-boundary", jobId, files)
                executionWindows[jobId] = ExecutionWindow(
                    startTime = System.currentTimeMillis(),
                    startCursor = files.maxOfOrNull(MediaFile::getFileIndex) ?: 0,
                    directoryIndex = jobDirectoryIndexes[jobId],
                )
                persistAssets()
                succeed(callback, Unit)
            }

            override fun onFailure(error: IDJIError) = fail(callback, sdkError(error))
        })
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
                val window = executionWindows[jobId] ?: return@submit fail(callback, IllegalStateException("Missing execution window"))
                val files = manager.mediaFileListData?.data.orEmpty()
                logFileList("manifest-$attempt", jobId, files)
                val candidates = files.filter { isWindowCandidate(it, window) }
                Log.i(TAG, "manifest candidates job=$jobId start=${window.startCursor} time=${window.startTime}..${window.endTime} dir=${window.directoryIndex} keys=${candidates.map { "${it.dirIndex}-${it.fileIndex}" }}")
                val jobReferences = references.getOrPut(jobId) { mutableListOf() }
                candidates.forEach { file ->
                    val existing = jobReferences.indexOfFirst {
                        it.fileIndex == file.fileIndex && it.directoryIndex == file.dirIndex
                    }
                    if (existing >= 0) {
                        jobReferences[existing] = file.toReference(jobId)
                    } else {
                        jobReferences += file.toReference(jobId)
                    }
                }
                val downloadedReferences = jobReferences.filter(::isDownloaded)
                val reconciled = (candidates.map { it.toReference(jobId) } + downloadedReferences)
                    .distinctBy(::referenceKey)
                    .sortedBy(MediaReference::capturedAtMillis)
                val keys = reconciled.map(::referenceKey).toSet()
                val nextStable = if (keys == previousKeys) stableScans + 1 else 0
                val finished = nextStable >= REQUIRED_STABLE_SCANS || attempt + 1 >= MAX_MANIFEST_SCANS
                executionWindows[jobId] = window.copy(
                    endCursor = keys.mapNotNull { key -> key.substringAfter('-').toIntOrNull() }.maxOrNull() ?: window.startCursor,
                    frozen = finished,
                )
                if (finished) {
                    val removed = jobReferences.filter { referenceKey(it) !in keys }
                    references[jobId] = reconciled.toMutableList()
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

            override fun onFailure(error: IDJIError) = fail(callback, sdkError(error))
        })
    }

    private fun allPhotosParam() = PullMediaFileListParam.Builder()
        .filter(MediaFileFilter.PHOTO)
        .mediaFileIndex(1)
        .count(-1)
        .build()

    private fun isWindowCandidate(file: MediaFile, window: ExecutionWindow): Boolean {
        val createdAt = file.date?.toEpochMillis() ?: return false
        val endTime = window.endTime.takeIf { it > 0 } ?: System.currentTimeMillis()
        return file.fileIndex > window.startCursor &&
            createdAt >= window.startTime - CLOCK_TOLERANCE_MS &&
            createdAt <= endTime + FINAL_MEDIA_GRACE_MS
    }

    private fun legacyWindow(jobId: String): ExecutionWindow {
        val jobReferences = references[jobId].orEmpty()
        val firstCapture = jobReferences.minOfOrNull(MediaReference::capturedAtMillis) ?: System.currentTimeMillis()
        val lastCapture = jobReferences.maxOfOrNull(MediaReference::capturedAtMillis) ?: firstCapture
        val firstIndex = jobReferences.minOfOrNull(MediaReference::fileIndex) ?: 1
        return ExecutionWindow(
            startTime = firstCapture - CLOCK_TOLERANCE_MS,
            endTime = lastCapture + FINAL_MEDIA_GRACE_MS,
            startCursor = (firstIndex - 1).coerceAtLeast(0),
            directoryIndex = jobDirectoryIndexes[jobId],
        )
    }

    private fun matchingFiles(jobId: String, files: List<MediaFile>): List<MediaFile> {
        val pending = references[jobId].orEmpty().filterNot {
            assets.containsKey(assetKey(jobId, referenceAssetId(it, files)))
        }
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
            if (index >= 0) jobReferences[index] = file.toReference(jobId)
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
        Log.i(TAG, "original request job=$jobId key=${mediaFile.dirIndex}-${mediaFile.fileIndex} name=${mediaFile.fileName} size=${mediaFile.fileSize} offset=$offset")
        mediaFile.pullOriginalMediaFileFromCamera(offset, object : MediaFileDownloadListener {
            override fun onStart() {
                Log.i(TAG, "original start job=$jobId key=${mediaFile.dirIndex}-${mediaFile.fileIndex}")
            }
            override fun onProgress(total: Long, current: Long) = submit {
                val fullSize = offset + total
                val downloaded = offset + current
                val percent = if (fullSize > 0) ((downloaded * 100 / fullSize).coerceAtMost(100)).toInt() else 0
                updateDownloadState(assetId, jobId, MediaDownloadStatus.DOWNLOADING, percent)
                Log.i(TAG, "original progress job=$jobId key=${mediaFile.dirIndex}-${mediaFile.fileIndex} current=$current total=$total percent=$percent")
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
                        ),
                    )
                    assets[assetKey(jobId, assetId)] = asset
                    updateDownloadState(assetId, jobId, MediaDownloadStatus.DOWNLOADED, 100)
                    persistAssets()
                    publishLocalState()
                    callback.onSuccess(asset)
                    Log.i(TAG, "original finish job=$jobId key=${mediaFile.dirIndex}-${mediaFile.fileIndex} bytes=${target.length()}")
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
        val payloadIndex = if (ProductUtil.isM5Product()) ComponentIndexType.RIGHT else ComponentIndexType.LEFT_OR_MAIN
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
        val payloadIndex = if (ProductUtil.isM5Product()) ComponentIndexType.RIGHT else ComponentIndexType.LEFT_OR_MAIN
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
        }
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
        return references[jobId].orEmpty().any { !isDownloaded(it) }
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
        mutableLocalAssets.postValue(assets.values.toList())
        val allReferences = references.values.flatten()
        mutableMediaReferences.postValue(allReferences)
        mutableManifestAssets.postValue(allReferences.filter { it.jobId in finalizingJobIds }.map { reference ->
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
                ),
            )
        })
        mutableThumbnailPaths.postValue(allReferences.mapNotNull { reference ->
            val file = thumbnailFile(reference.jobId, reference.directoryIndex, reference.fileIndex)
            if (file.isFile) mediaKey(reference.jobId, reference.directoryIndex, reference.fileIndex) to file.absolutePath else null
        }.toMap())
        mutableDownloadStates.postValue(downloadStateMap.toMap())
        mutablePendingCount.postValue(references.values.sumOf { jobRefs ->
            jobRefs.count { ref ->
                assets.values.none { asset ->
                    asset.jobId == ref.jobId &&
                        asset.meta["fileIndex"] == ref.fileIndex.toString() &&
                        asset.meta["dirIndex"] == ref.directoryIndex.toString()
                }
            }
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

    private fun MediaFile.toReference(jobId: String) = MediaReference(
        jobId = jobId,
        fileIndex = fileIndex,
        directoryIndex = dirIndex,
        fileNumber = 0,
        fileSize = fileSize,
        fileType = fileType.name,
        createdAt = date?.toString().orEmpty(),
        fileName = fileName,
        capturedAtMillis = date?.toEpochMillis() ?: System.currentTimeMillis(),
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

    private data class ExecutionWindow(
        val startTime: Long,
        val endTime: Long = 0,
        val startCursor: Int = 0,
        val endCursor: Int = 0,
        val directoryIndex: Int? = null,
        val frozen: Boolean = false,
    )

    private val PHOTO_TYPES = setOf(
        MediaFileType.JPEG,
        MediaFileType.DNG,
        MediaFileType.PANORAMA,
        MediaFileType.TIFF,
    )

    private const val NEW_MEDIA_DELAY_MS = 800L
    private const val MAX_FOLDER_SUFFIX_LENGTH = 40
    private const val CLOCK_TOLERANCE_MS = 2_000L
    private const val FINAL_MEDIA_GRACE_MS = 2_000L
    private const val MANIFEST_SCAN_INTERVAL_MS = 1_000L
    private const val REQUIRED_STABLE_SCANS = 2
    private const val MAX_MANIFEST_SCANS = 10
    private const val MAX_LOGGED_FILES = 30
    private const val THUMBNAIL_JPEG_QUALITY = 85
    private const val TAG = "FlightTaskMedia"
}
