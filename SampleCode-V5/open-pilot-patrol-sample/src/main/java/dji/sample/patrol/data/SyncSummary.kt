package dji.sample.patrol.data

/**
 * 同步进度统一摘要（插件页面 / 浮窗 / 通知栏共用的口径）。
 *
 * 设计意图：上传文件数与字节数只来自 [UploadProgress]（上传队列，含巡检报告、排除航线资产）；
 * 照片下载进度只来自媒体清单 [MediaReference] 与已下载资源 [Asset]。两类语义分开，
 * 避免页面用队列、浮窗用清单导致同一任务出现不同分母。
 */
data class SyncSummary(
    /** 已上传成功的文件数（照片 + 巡检报告）。 */
    val uploadedFiles: Int,
    /** 进入上传队列的文件总数（照片 + 巡检报告）。 */
    val totalFiles: Int,
    /** 上传失败的文件数。 */
    val failedFiles: Int,
    /** 已上传字节数。 */
    val uploadedBytes: Long,
    /** 上传队列总字节数。 */
    val totalBytes: Long,
    /** 已下载到本地的照片数。 */
    val downloadedPhotos: Int,
    /** 媒体清单中的照片总数。 */
    val totalPhotos: Int,
) {
    /** 是否存在可展示的上传进度；无队列时页面应显示空态而不是 `0/0`。 */
    val hasUpload: Boolean get() = totalFiles > 0
}

/**
 * 计算某工单的统一同步摘要（纯函数）。
 *
 * @param jobId 目标工单；`null` 时返回全零摘要
 * @param uploadProgress 该工单的上传队列进度；`null` 表示尚未进入上传队列
 * @param references 媒体清单（照片下载进度的分母）
 * @param assets 本地已下载资源（照片下载进度的分子）
 */
fun syncSummary(
    jobId: String?,
    uploadProgress: UploadProgress?,
    references: List<MediaReference>,
    assets: List<Asset>,
): SyncSummary {
    if (jobId == null) {
        return SyncSummary(
            uploadedFiles = 0,
            totalFiles = 0,
            failedFiles = 0,
            uploadedBytes = 0,
            totalBytes = 0,
            downloadedPhotos = 0,
            totalPhotos = 0,
        )
    }
    val totalPhotos = references.count { it.jobId == jobId }
    val downloadedPhotos = assets.count { it.jobId == jobId && it.typeId == "photo" }
    return SyncSummary(
        uploadedFiles = uploadProgress?.uploaded ?: 0,
        totalFiles = uploadProgress?.total ?: 0,
        failedFiles = uploadProgress?.failed ?: 0,
        uploadedBytes = uploadProgress?.uploadedBytes ?: 0,
        totalBytes = uploadProgress?.totalBytes ?: 0,
        downloadedPhotos = downloadedPhotos,
        totalPhotos = totalPhotos,
    )
}
