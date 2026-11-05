package com.dji.sample.gasdetection.ui

import android.content.Context
import androidx.annotation.StringRes
import com.dji.sample.gasdetection.R
import com.dji.sample.gasdetection.gas.record.InspectionRecord
import com.dji.sample.gasdetection.gas.record.RecordStatus

/**
 * 检测记录文案映射。
 *
 * 设计意图：传输页与任务结束提示共用同一套状态文案，避免「页面显示已上传、提示说待上传」。
 */
object RecordTexts {

    /** 记录摘要：任务标题 · 采样 X · 异常 Y。 */
    fun summary(context: Context, record: InspectionRecord): String =
        context.getString(
            R.string.gas_record_summary,
            record.jobTitle.ifBlank { record.jobId },
            record.totalSamples,
            record.alerts.sumOf { it.count },
        )

    /** 上传状态文案。 */
    fun status(context: Context, status: RecordStatus): String =
        context.getString(statusRes(status))

    @StringRes
    fun statusRes(status: RecordStatus): Int = when (status) {
        RecordStatus.QUEUED -> R.string.gas_record_status_queued
        RecordStatus.UPLOADING -> R.string.gas_record_status_uploading
        RecordStatus.UPLOADED -> R.string.gas_record_status_uploaded
        RecordStatus.FAILED -> R.string.gas_record_status_failed
    }
}
