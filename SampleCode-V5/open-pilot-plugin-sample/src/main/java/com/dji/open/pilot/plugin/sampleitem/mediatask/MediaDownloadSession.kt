package com.dji.open.pilot.plugin.sampleitem.mediatask

import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile

/**
 * 单文件下载会话（方案 B2：覆盖式全量下载，不续传）。
 *
 * SDK 的 [MediaFileDownloadListener.onRealtimeDataUpdate] 回传的 position 取自底层
 * 同一个 FileDataRequest.getOffSet()，是固定的初始 offset、不随数据流递增，
 * 因此不能用它做 seek 定位写入（会导致每块数据覆盖同一位置、文件写坏）。
 *
 * 本会话改为“按到达顺序顺序写入”：
 * - open 时 truncate 清空旧文件，保证重复下载同名文件不会追加成两份而损坏；
 * - write 忽略 position，顺序 append 数据块（数据块本身按文件顺序到达）；
 * - 下载 offset 固定传 0，不做断点续传。
 */
class MediaDownloadSession private constructor(
    val file: File,
    private val randomAccessFile: RandomAccessFile
) : Closeable {

    private var isClosed = false

    /**
     * 按到达顺序顺序写入数据块。
     *
     * 忽略 SDK 回传的 position（其值不随数据流递增，不可用于定位）。
     * 数据块由底层按文件顺序推送，顺序 append 即为正确内容。
     */
    @Synchronized
    fun append(data: ByteArray) {
        check(!isClosed) { "Download session is closed" }
        randomAccessFile.write(data)
    }

    /** 判断当前落盘长度是否与远端声明的文件大小一致。 */
    @Synchronized
    fun isComplete(expectedSize: Long): Boolean =
        !isClosed && expectedSize > 0 && randomAccessFile.length() == expectedSize

    @Synchronized
    override fun close() {
        if (isClosed) {
            return
        }
        isClosed = true
        randomAccessFile.close()
    }

    companion object {
        private const val DOWNLOAD_DIR_NAME = "media-task"
        private const val FALLBACK_FILE_NAME = "download_file"

        /**
         * 在 [baseDir]/media-task 下打开目标文件并清空旧内容（truncate）。
         *
         * @param sourceFileName 远端文件名，会被裁剪为纯文件名，防止路径穿越。
         */
        fun open(baseDir: File, sourceFileName: String): MediaDownloadSession {
            val dir = File(baseDir, DOWNLOAD_DIR_NAME).apply { mkdirs() }
            val safeName = File(sourceFileName).name.ifBlank { FALLBACK_FILE_NAME }
            val target = File(dir, safeName)
            val raf = RandomAccessFile(target, "rw")
            // 覆盖式下载：清空旧文件，写指针回到 0，避免重复下载追加损坏。
            raf.setLength(0)
            raf.seek(0)
            return MediaDownloadSession(target, raf)
        }
    }
}
