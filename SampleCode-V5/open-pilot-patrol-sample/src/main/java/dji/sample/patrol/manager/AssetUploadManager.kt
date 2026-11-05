package dji.sample.patrol.manager

import android.content.Context
import android.util.AtomicFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dji.sample.patrol.data.Asset
import dji.sample.patrol.data.AssetState
import dji.sample.patrol.data.ChunkResult
import dji.sample.patrol.data.ResultCallback
import dji.sample.patrol.data.RequestHandle
import dji.sample.patrol.data.SessionInfo
import dji.sample.patrol.data.UploadProgress
import dji.sample.patrol.data.UploadQueueItem
import dji.sample.patrol.data.UploadStatus
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** 通用资源上传队列；当前 Demo 使用照片和航线，后续可复用到视频、模型和日志。 */
object AssetUploadManager {
    private val mutableQueue = MutableLiveData<List<Asset>>(emptyList())
    val queue: LiveData<List<Asset>> = mutableQueue

    private val mutableProgress = MutableLiveData<Map<String, UploadProgress>>(emptyMap())
    val progress: LiveData<Map<String, UploadProgress>> = mutableProgress

    private val mutableDetails = MutableLiveData<List<UploadQueueItem>>(emptyList())
    val details: LiveData<List<UploadQueueItem>> = mutableDetails

    private var appContext: Context? = null
    private var executor: ScheduledExecutorService? = null
    private val items = mutableListOf<UploadQueueItem>()
    private var generation = 0L
    private var busy = false
    private var activeRequest: RequestHandle? = null

    fun init(context: Context) {
        release()
        appContext = context.applicationContext
        executor = Executors.newSingleThreadScheduledExecutor()
        executor?.execute {
            restoreQueue()
            // 航线由 Web 发布，插件只下载执行；清理旧版本遗留的 route 上传项。
            items.removeAll { it.asset.typeId == "route" }
            persistQueue()
            publish()
            pump()
        }
    }

    fun enqueue(jobId: String, assets: List<Asset>, onQueued: (() -> Unit)? = null) {
        submit {
            val existing = items.map { it.asset.jobId to it.asset.assetId }.toSet()
            assets.filter(::isCompleteLocalAsset)
                .filterNot { (jobId to it.assetId) in existing }
                .forEach {
                    items += UploadQueueItem(
                        asset = it.copy(jobId = jobId, state = AssetState.QUEUED),
                    )
                }
            persistAndPublish()
            onQueued?.invoke()
            pump()
        }
    }

    fun pause(jobId: String) {
        submit {
            val wasActive = items.any {
                it.asset.jobId == jobId && it.status == UploadStatus.UPLOADING
            }
            replaceAll { item ->
                if (item.asset.jobId == jobId && item.status in setOf(UploadStatus.QUEUED, UploadStatus.UPLOADING)) {
                    item.copy(status = UploadStatus.PAUSED)
                } else item
            }
            if (wasActive) {
                activeRequest?.cancel()
                activeRequest = null
                busy = false
                generation++
            }
            persistAndPublish()
            pump()
        }
    }

    fun resume(jobId: String) {
        submit {
            replaceAll { item ->
                if (item.asset.jobId == jobId && item.status == UploadStatus.PAUSED) {
                    item.copy(status = UploadStatus.QUEUED)
                } else item
            }
            persistAndPublish()
            pump()
        }
    }

    fun retry(jobId: String, assetId: String) {
        submit {
            replace(jobId, assetId) { item ->
                if (item.status == UploadStatus.FAILED) {
                    item.copy(status = UploadStatus.QUEUED, retryCount = 0)
                } else {
                    item
                }
            }
            persistAndPublish()
            pump()
        }
    }

    fun retry(jobId: String) {
        submit {
            replaceAll { item ->
                if (item.asset.jobId == jobId && item.status == UploadStatus.FAILED) {
                    item.copy(status = UploadStatus.QUEUED, retryCount = 0)
                } else item
            }
            persistAndPublish()
            pump()
        }
    }

    fun cancel(jobId: String, assetId: String) {
        submit {
            val wasActive = items.any {
                it.asset.jobId == jobId && it.asset.assetId == assetId && it.status == UploadStatus.UPLOADING
            }
            items.removeAll { it.asset.jobId == jobId && it.asset.assetId == assetId }
            if (wasActive) {
                activeRequest?.cancel()
                activeRequest = null
                busy = false
                generation++
            }
            persistAndPublish()
            pump()
        }
    }

    fun release() {
        generation++
        busy = false
        activeRequest?.cancel()
        activeRequest = null
        if (appContext != null) runCatching { persistQueue() }
        executor?.shutdownNow()
        executor = null
        appContext = null
        items.clear()
        mutableQueue.postValue(emptyList())
        mutableProgress.postValue(emptyMap())
    }

    private fun pump() {
        if (busy) return
        val index = items.indexOfFirst { it.status == UploadStatus.QUEUED }
        if (index < 0) return
        val item = items[index]
        if (!isCompleteLocalAsset(item.asset)) {
            items[index] = item.copy(status = UploadStatus.FAILED)
            persistAndPublish()
            pump()
            return
        }
        busy = true
        val token = ++generation
        items[index] = item.copy(status = UploadStatus.UPLOADING)
        persistAndPublish()
        val uploading = items[index]
        if (!CloudManager.isConfigured()) {
            busy = false
            replace(uploading.asset.jobId, uploading.asset.assetId) {
                it.copy(asset = it.asset.copy(state = AssetState.FAILED), status = UploadStatus.FAILED)
            }
            persistAndPublish()
            pump()
            return
        }
        if (uploading.sessionId == null) createSession(uploading, token) else uploadNextPart(uploading, token)
    }

    private fun createSession(item: UploadQueueItem, token: Long) {
        activeRequest = CloudManager.createUploadSession(item.asset, object : ResultCallback<SessionInfo> {
            override fun onSuccess(data: SessionInfo) = submit {
                if (!isCurrent(item.asset.jobId, item.asset.assetId, token)) return@submit
                activeRequest = null
                val restoredParts = data.uploadedChunks.map { ChunkResult(it, true) }
                val updated = item.copy(
                    sessionId = data.sessionId,
                    chunkSizeBytes = data.chunkSizeBytes,
                    partUrls = data.partUrls,
                    completedParts = restoredParts,
                )
                replace(item.asset.jobId, item.asset.assetId) { updated }
                persistAndPublish()
                uploadNextPart(updated, token)
            }

            override fun onFailure(error: Throwable) = submit {
                uploadFailed(item.asset.jobId, item.asset.assetId, token)
            }
        })
    }

    private fun uploadNextPart(item: UploadQueueItem, token: Long) {
        if (!isCurrent(item.asset.jobId, item.asset.assetId, token)) return
        val file = File(requireNotNull(item.asset.localUri))
        val chunkSize = item.chunkSizeBytes
        if (chunkSize <= 0) {
            uploadFailed(item.asset.jobId, item.asset.assetId, token)
            return
        }
        val partCount = ((file.length() + chunkSize - 1) / chunkSize).toInt()
        val completed = item.completedParts.filter { it.success }.map { it.index }.toSet()
        val partIndex = (0 until partCount).firstOrNull { it !in completed }
        if (partIndex == null) {
            completeUpload(item, token)
            return
        }
        val offset = partIndex * chunkSize
        val length = minOf(chunkSize, file.length() - offset).toInt()
        val data = ByteArray(length)
        RandomAccessFile(file, "r").use {
            it.seek(offset)
            it.readFully(data)
        }
        val uploadUrl = item.partUrls.getOrNull(partIndex)
        if (uploadUrl.isNullOrBlank()) {
            uploadFailed(item.asset.jobId, item.asset.assetId, token)
            return
        }
        activeRequest = CloudManager.uploadPart(uploadUrl, data, object : ResultCallback<ChunkResult> {
            override fun onSuccess(result: ChunkResult) = submit {
                if (!isCurrent(item.asset.jobId, item.asset.assetId, token)) return@submit
                activeRequest = null
                val part = result.copy(index = partIndex)
                if (!part.success) {
                    uploadFailed(item.asset.jobId, item.asset.assetId, token)
                    return@submit
                }
                val updated = item.copy(completedParts = item.completedParts.filterNot { it.index == partIndex } + part)
                replace(item.asset.jobId, item.asset.assetId) { updated }
                persistAndPublish()
                uploadNextPart(updated, token)
            }

            override fun onFailure(error: Throwable) = submit {
                uploadFailed(item.asset.jobId, item.asset.assetId, token)
            }
        })
    }

    private fun completeUpload(item: UploadQueueItem, token: Long) {
        activeRequest = CloudManager.completeUpload(
            requireNotNull(item.sessionId),
            item.completedParts.sortedBy { it.index },
            object : ResultCallback<Asset> {
            override fun onSuccess(data: Asset) = submit {
                if (!isCurrent(item.asset.jobId, item.asset.assetId, token)) return@submit
                activeRequest = null
                replace(item.asset.jobId, item.asset.assetId) {
                    it.copy(asset = it.asset.copy(state = AssetState.UPLOADED), status = UploadStatus.UPLOADED)
                }
                busy = false
                persistAndPublish()
                pump()
            }

            override fun onFailure(error: Throwable) = submit {
                uploadFailed(item.asset.jobId, item.asset.assetId, token)
            }
        })
    }

    private fun uploadFailed(jobId: String, assetId: String, token: Long) {
        if (!isCurrent(jobId, assetId, token)) return
        activeRequest = null
        val item = items.first { it.asset.jobId == jobId && it.asset.assetId == assetId }
        busy = false
        if (item.retryCount < MAX_RETRIES) {
            replace(jobId, assetId) { it.copy(status = UploadStatus.QUEUED, retryCount = it.retryCount + 1) }
            persistAndPublish()
            executor?.schedule({ pump() }, 1L shl item.retryCount, TimeUnit.SECONDS)
        } else {
            replace(jobId, assetId) {
                it.copy(asset = it.asset.copy(state = AssetState.FAILED), status = UploadStatus.FAILED)
            }
            persistAndPublish()
            pump()
        }
    }

    private fun isCurrent(jobId: String, assetId: String, token: Long): Boolean =
        token == generation && items.any {
            it.asset.jobId == jobId && it.asset.assetId == assetId && it.status == UploadStatus.UPLOADING
        }

    private fun isCompleteLocalAsset(asset: Asset): Boolean {
        val path = asset.localUri ?: return false
        val file = File(path)
        if (!file.isFile || file.name.endsWith(".part") || file.length() != asset.sizeBytes) return false
        return asset.md5?.equals(md5(file), ignoreCase = true) == true
    }

    private fun publish() {
        val exposed = items.map { item ->
            val state = when (item.status) {
                UploadStatus.QUEUED, UploadStatus.PAUSED -> AssetState.QUEUED
                UploadStatus.UPLOADING -> AssetState.UPLOADING
                UploadStatus.UPLOADED -> AssetState.UPLOADED
                UploadStatus.FAILED -> AssetState.FAILED
            }
            item.asset.copy(state = state)
        }
        mutableQueue.postValue(exposed)
        mutableDetails.postValue(items.toList())
        mutableProgress.postValue(items.groupBy { it.asset.jobId }.mapValues { (jobId, queueItems) ->
            val assets = queueItems.map { it.asset }
            UploadProgress(
                jobId = jobId,
                total = assets.size,
                uploaded = queueItems.count { it.status == UploadStatus.UPLOADED },
                failed = queueItems.count { it.status == UploadStatus.FAILED },
                totalBytes = assets.sumOf { it.sizeBytes },
                uploadedBytes = queueItems.sumOf(UploadQueueItem::uploadedBytes),
            )
        })
    }

    private fun persistAndPublish() {
        persistQueue()
        publish()
    }

    private fun restoreQueue() {
        items.clear()
        val file = queueFile() ?: return
        if (!file.exists()) return
        runCatching {
            val array = JSONObject(file.readText()).optJSONArray("queue") ?: JSONArray()
            for (i in 0 until array.length()) {
                val json = array.getJSONObject(i)
                val assetJson = json.getJSONObject("asset")
                val meta = linkedMapOf<String, String>()
                assetJson.optJSONObject("meta")?.let { values ->
                    values.keys().forEach { key -> meta[key] = values.getString(key) }
                }
                val asset = Asset(
                    assetId = assetJson.getString("assetId"),
                    jobId = assetJson.getString("jobId"),
                    typeId = assetJson.getString("typeId"),
                    localUri = assetJson.optString("localUri").ifBlank { null },
                    sizeBytes = assetJson.getLong("sizeBytes"),
                    md5 = assetJson.optString("md5").ifBlank { null },
                    state = AssetState.valueOf(assetJson.getString("state")),
                    meta = meta,
                )
                if (!isCompleteLocalAsset(asset)) continue
                val parts = mutableListOf<ChunkResult>()
                val partArray = json.optJSONArray("completedParts") ?: JSONArray()
                for (partIndex in 0 until partArray.length()) {
                    val part = partArray.getJSONObject(partIndex)
                    parts += ChunkResult(
                        part.getInt("index"),
                        part.getBoolean("success"),
                        part.optString("message").ifBlank { null },
                    )
                }
                val restoredStatus = UploadStatus.valueOf(json.getString("status")).let {
                    if (it == UploadStatus.UPLOADING) UploadStatus.QUEUED else it
                }
                items += UploadQueueItem(
                    asset = asset,
                    status = restoredStatus,
                    retryCount = json.optInt("retryCount"),
                    // 预签名 URL 可能已过期；进程恢复后重新创建/恢复服务端会话。
                    sessionId = if (restoredStatus == UploadStatus.UPLOADED) {
                        json.optString("sessionId").ifBlank { null }
                    } else null,
                    chunkSizeBytes = if (restoredStatus == UploadStatus.UPLOADED) json.optLong("chunkSizeBytes") else 0,
                    partUrls = emptyList(),
                    completedParts = if (restoredStatus == UploadStatus.UPLOADED) parts else emptyList(),
                )
            }
        }
    }

    private fun persistQueue() {
        val file = queueFile() ?: return
        file.parentFile?.mkdirs()
        val array = JSONArray()
        items.forEach { item ->
            val asset = item.asset
            array.put(JSONObject().apply {
                put("asset", JSONObject().apply {
                    put("assetId", asset.assetId)
                    put("jobId", asset.jobId)
                    put("typeId", asset.typeId)
                    put("localUri", asset.localUri ?: "")
                    put("sizeBytes", asset.sizeBytes)
                    put("md5", asset.md5 ?: "")
                    put("state", asset.state.name)
                    put("meta", JSONObject(asset.meta))
                })
                put("status", item.status.name)
                put("retryCount", item.retryCount)
                put("sessionId", item.sessionId ?: "")
                put("chunkSizeBytes", item.chunkSizeBytes)
                put("partUrls", JSONArray(item.partUrls))
                put("completedParts", JSONArray().apply {
                    item.completedParts.forEach { part ->
                        put(JSONObject().apply {
                            put("index", part.index)
                            put("success", part.success)
                            put("message", part.message ?: "")
                        })
                    }
                })
            })
        }
        atomicWrite(file, JSONObject().put("queue", array).toString())
    }

    private fun queueFile() = appContext?.let { File(it.filesDir, "flight-assets/upload-queue.json") }

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

    private fun replace(jobId: String, assetId: String, transform: (UploadQueueItem) -> UploadQueueItem) {
        val index = items.indexOfFirst { it.asset.jobId == jobId && it.asset.assetId == assetId }
        if (index >= 0) items[index] = transform(items[index])
    }

    private fun replaceAll(transform: (UploadQueueItem) -> UploadQueueItem) {
        items.indices.forEach { items[it] = transform(items[it]) }
    }

    private fun submit(action: () -> Unit) {
        executor?.execute {
            try {
                action()
            } catch (error: Throwable) {
                activeRequest?.cancel()
                activeRequest = null
                busy = false
                val activeIndex = items.indexOfFirst { it.status == UploadStatus.UPLOADING }
                if (activeIndex >= 0) {
                    val item = items[activeIndex]
                    items[activeIndex] = item.copy(
                        asset = item.asset.copy(state = AssetState.FAILED),
                        status = UploadStatus.FAILED,
                    )
                    runCatching { persistAndPublish() }
                }
            }
        }
    }

    private const val MAX_RETRIES = 3
}
