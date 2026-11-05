package dji.sample.flighttask.data

/** A lightweight camera reference captured while a flight job is running. */
data class MediaReference(
    val jobId: String,
    val fileIndex: Int,
    val directoryIndex: Int,
    val fileNumber: Int,
    val fileSize: Long,
    val fileType: String,
    val createdAt: String,
    val fileName: String = "",
    val capturedAtMillis: Long = System.currentTimeMillis(),
)

enum class MediaDownloadStatus { PENDING, DOWNLOADING, DOWNLOADED, FAILED }

data class MediaDownloadState(
    val assetId: String,
    val jobId: String,
    val status: MediaDownloadStatus,
    val progress: Int = 0,
    val error: String? = null,
)
