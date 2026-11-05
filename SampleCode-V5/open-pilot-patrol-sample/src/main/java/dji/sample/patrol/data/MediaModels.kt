package dji.sample.patrol.data

/**
 * 工单执行期间相机上报的一条媒体引用。
 *
 * 设计意图：引用除了标识相机文件，还记录产生它时的执行窗口快照，使窗口在后续轮换后仍能
 * 按「产生当时的区间」判断归属，避免同一工单重新执行时把上一轮未取回的照片误判为历史照片。
 */
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
    /** 产生该引用时的执行窗口起点；0 表示旧缓存或窗口缺失，按历史引用处理，不参与本轮匹配。 */
    val windowStartTime: Long = 0L,
    /** 产生该引用时的执行窗口终点；0 表示窗口当时尚未关闭。 */
    val windowEndTime: Long = 0L,
    /** 产生该引用时的相机文件游标下界，用于排除上一轮遗留文件。 */
    val windowStartCursor: Int = 0,
) {
    /**
     * 一次快门的照片组键。
     *
     * 设计意图：同一次快门可能产生多个镜头文件（如 `_W`/`_Z`/`_T`），它们共享文件名前缀；
     * 组键用于把整组照片绑定到同一条拍照点位记录。文件名缺失时退回「目录-序号」，
     * 避免把不同文件错误合并。
     */
    val groupKey: String
        get() {
            val prefix = if (fileName.contains('_')) fileName.substringBeforeLast('_') else ""
            return if (prefix.isNotBlank()) "$directoryIndex/$prefix" else "$directoryIndex-$fileIndex"
        }
}

enum class MediaDownloadStatus { PENDING, DOWNLOADING, DOWNLOADED, FAILED }

data class MediaDownloadState(
    val assetId: String,
    val jobId: String,
    val status: MediaDownloadStatus,
    val progress: Int = 0,
    val error: String? = null,
)
