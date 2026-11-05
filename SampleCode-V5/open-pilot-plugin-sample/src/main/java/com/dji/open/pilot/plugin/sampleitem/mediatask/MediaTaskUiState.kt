package com.dji.open.pilot.plugin.sampleitem.mediatask

import android.graphics.Bitmap
import androidx.annotation.StringRes
import dji.sdk.keyvalue.value.camera.CameraStorageLocation
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.manager.datacenter.media.MediaFile
import dji.v5.manager.intelligent.media.ThermalPhotoInfo

/** 拉取范围：index/count 均以 -1 表示“全部/从头”。 */
data class PullRange(val index: Int, val count: Int)

/** 一次性提示，文案由资源 ID 与参数构成，禁止硬编码。 */
data class ToastMessage(@StringRes val resId: Int, val args: List<Any> = emptyList())

/**
 * 解析列表拉取输入。
 *
 * 规则：
 * - 空白 index/count 归一为 -1/-1（全部）。
 * - index 允许 -1 或非负整数；小于 -1 视为非法。
 * - count 允许 -1 或正整数；0 与小于 -1 视为非法。
 * - 非数字或溢出视为非法。
 *
 * @throws IllegalArgumentException 输入非法时抛出，由 UI 映射为提示文案。
 */
fun parsePullRange(indexText: String, countText: String): PullRange =
    PullRange(parseIndex(indexText), parseCount(countText))

private fun parseIndex(text: String): Int {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) {
        return -1
    }
    val value = trimmed.toIntOrNull() ?: throw IllegalArgumentException("index is not a number: $text")
    if (value < -1) {
        throw IllegalArgumentException("index must be -1 or non-negative: $text")
    }
    return value
}

private fun parseCount(text: String): Int {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) {
        return -1
    }
    val value = trimmed.toIntOrNull() ?: throw IllegalArgumentException("count is not a number: $text")
    if (value == 0 || value < -1) {
        throw IllegalArgumentException("count must be -1 or positive: $text")
    }
    return value
}

/** 回放模式状态；UNKNOWN 表示尚未收到远端回调，不可默认当作 false。 */
enum class PlaybackUiState { UNKNOWN, ENTERING, ENTERED, EXITING, EXITED, FAILED }

/** 列表内容状态。 */
enum class ContentUiState { IDLE, LOADING, CONTENT, EMPTY, ERROR }

/** 详情区单项能力状态（预览/XMP/热解析共用）。 */
enum class DetailUiState { IDLE, LOADING, CONTENT, ERROR }

/** 单文件下载状态。 */
enum class DownloadUiState { IDLE, RUNNING, SUCCEEDED, FAILED, CANCELLED }

/** 多媒体演示页不可变 UI 状态。 */
data class MediaTaskUiState(
    val isServerConnected: Boolean = false,
    val component: ComponentIndexType = ComponentIndexType.LEFT_OR_MAIN,
    val storage: CameraStorageLocation = CameraStorageLocation.SDCARD,
    val playbackState: PlaybackUiState = PlaybackUiState.UNKNOWN,
    val contentState: ContentUiState = ContentUiState.IDLE,
    val files: List<MediaFile> = emptyList(),
    val selectionMode: Boolean = false,
    val selectedFileIndex: Int? = null,
    val selectedFileIndices: Set<Int> = emptySet(),
    val previewState: DetailUiState = DetailUiState.IDLE,
    val previewBitmap: Bitmap? = null,
    val previewIsThumbnail: Boolean = false,
    val downloadState: DownloadUiState = DownloadUiState.IDLE,
    val downloadProgress: Int = 0,
    val isDownloadIndeterminate: Boolean = false,
    val downloadReceivedBytes: Long = 0L,
    val downloadSpeedBytesPerSecond: Long = 0L,
    val xmpFileState: DetailUiState = DetailUiState.IDLE,
    val xmpFileData: String? = null,
    val xmpCustomState: DetailUiState = DetailUiState.IDLE,
    val xmpCustomInfo: String? = null,
    val cameraXmpCustomInfo: String? = null,
    val thermalState: DetailUiState = DetailUiState.IDLE,
    val thermalResult: ThermalPhotoInfo? = null,
    val operationInProgress: Boolean = false
) {
    val selectedFile: MediaFile?
        get() = files.firstOrNull { it.fileIndex == selectedFileIndex }
}
