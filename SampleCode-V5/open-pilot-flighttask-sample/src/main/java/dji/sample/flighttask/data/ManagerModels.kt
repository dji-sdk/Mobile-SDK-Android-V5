package dji.sample.flighttask.data

import java.io.File

/** 简单回调，所有 Manager 共用；不依赖协程。 */
interface ResultCallback<T> {
    fun onSuccess(data: T)
    fun onFailure(error: Throwable)
}

data class CloudConfig(
    val baseUrl: String,
    val rcSerialNumber: String,
    val accessToken: String = "",
)

enum class BackendConnectionState { CHECKING, CONNECTED, UNAVAILABLE }

enum class WaylineState {
    IDLE,
    PREPARING,
    READY,
    EXECUTING,
    PAUSED,
    FINISHED,
    FAILED,
}

data class WaylineProgress(
    val currentWaypoint: Int = 0,
    val totalWaypoints: Int = 0,
    val description: String = "",
)

data class UploadProgress(
    val jobId: String,
    val total: Int,
    val uploaded: Int,
    val failed: Int,
    val totalBytes: Long = 0,
    val uploadedBytes: Long = 0,
)

data class SessionInfo(
    val sessionId: String,
    val chunkSizeBytes: Long,
    /** 服务端按分片顺序返回预签名 URL；照片通常只有一个。 */
    val partUrls: List<String> = emptyList(),
    val uploadedChunks: List<Int> = emptyList(),
)

data class ChunkResult(
    val index: Int,
    val success: Boolean,
    val message: String? = null,
)

data class WaylineFile(
    val jobId: String,
    val file: File,
)
