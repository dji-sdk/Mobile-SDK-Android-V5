package com.dji.open.pilot.plugin.sampleitem.mediatask

import dji.sdk.keyvalue.value.camera.DateTime
import java.util.Locale

private val FILE_SIZE_UNITS = arrayOf("B", "KB", "MB", "GB", "TB")

/** 将字节数格式化为可读文本；非正数统一返回 "0 B"。 */
fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) {
        return "0 B"
    }
    if (bytes < 1024L) {
        return "$bytes B"
    }
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < FILE_SIZE_UNITS.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    return String.format(Locale.US, "%.1f %s", value, FILE_SIZE_UNITS[unitIndex])
}

/**
 * 将相机时间字段格式化为 `yyyy-MM-dd HH:mm:ss`。
 *
 * 年月日缺失时返回空串；时分秒缺失按 0 处理。
 */
fun formatMediaDate(
    year: Int?,
    month: Int?,
    day: Int?,
    hour: Int?,
    minute: Int?,
    second: Int?
): String {
    if (year == null || month == null || day == null) {
        return ""
    }
    return String.format(
        Locale.US,
        "%04d-%02d-%02d %02d:%02d:%02d",
        year,
        month,
        day,
        hour ?: 0,
        minute ?: 0,
        second ?: 0
    )
}

/** 相机媒体文件创建时间 → 展示文本。 */
fun DateTime.toDisplayDate(): String =
    formatMediaDate(year, month, day, hour, minute, second)