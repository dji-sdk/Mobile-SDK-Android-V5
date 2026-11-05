package dji.sample.patrol.data

/** 可取消的异步请求。取消后仍可能收到一次失败回调。 */
interface RequestHandle {
    fun cancel()
    val isCanceled: Boolean
}

enum class CloudJobStatus {
    DISPATCHED,
    ACCEPTED,
    IN_PROGRESS,
    FINALIZING,
    COMPLETED,
    CANCELLED,
}

data class JobStatusUpdate(
    val job: Job?,
)

data class CloudHttpException(
    val statusCode: Int,
    val responseBody: String,
) : Exception("HTTP $statusCode${if (responseBody.isBlank()) "" else ": $responseBody"}")

internal data class JobStatusRequest(val status: CloudJobStatus)
internal data class UploadSessionRequest(
    val assetId: String,
    val jobId: String,
    val typeId: String,
    val fileName: String,
    val sizeBytes: Long,
    val md5: String?,
) {
    constructor(asset: Asset) : this(
        assetId = asset.assetId,
        jobId = asset.jobId,
        typeId = asset.typeId,
        fileName = asset.meta["fileName"] ?: asset.assetId,
        sizeBytes = asset.sizeBytes,
        md5 = asset.md5,
    )
}
internal data class CompleteUploadRequest(val parts: List<ChunkResult>)
