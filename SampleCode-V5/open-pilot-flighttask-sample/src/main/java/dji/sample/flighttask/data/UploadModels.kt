package dji.sample.flighttask.data

enum class UploadStatus { QUEUED, UPLOADING, PAUSED, UPLOADED, FAILED }

data class UploadQueueItem(
    val asset: Asset,
    val status: UploadStatus = UploadStatus.QUEUED,
    val retryCount: Int = 0,
    val sessionId: String? = null,
    val chunkSizeBytes: Long = 0,
    val partUrls: List<String> = emptyList(),
    val completedParts: List<ChunkResult> = emptyList(),
) {
    fun uploadedBytes(): Long {
        if (status == UploadStatus.UPLOADED) return asset.sizeBytes
        if (chunkSizeBytes <= 0) return 0
        return completedParts.asSequence()
            .filter { it.success }
            .map { it.index }
            .distinct()
            .sumOf { index ->
                val offset = index * chunkSizeBytes
                (asset.sizeBytes - offset).coerceIn(0, chunkSizeBytes)
            }
    }
}
