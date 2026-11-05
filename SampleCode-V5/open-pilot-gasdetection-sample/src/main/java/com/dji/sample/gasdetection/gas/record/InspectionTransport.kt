package com.dji.sample.gasdetection.gas.record

/** 上传结果；失败时保留可读摘要用于界面与日志。 */
sealed class InspectionUploadResult {
    data object Success : InspectionUploadResult()
    data class Failure(val message: String) : InspectionUploadResult()
}

/**
 * 检测记录上传端口。
 *
 * 设计意图：把 HTTP 细节隔离在适配层，队列与重试逻辑可用假实现完整验证。
 */
interface InspectionTransport {
    /** 上传一条记录（要求服务端按 `recordId` 幂等）。 */
    fun upload(record: InspectionRecord, callback: (InspectionUploadResult) -> Unit)

    /** 记录上传成功后把任务状态回写为完成。 */
    fun completeJob(jobId: String, callback: (InspectionUploadResult) -> Unit)
}
