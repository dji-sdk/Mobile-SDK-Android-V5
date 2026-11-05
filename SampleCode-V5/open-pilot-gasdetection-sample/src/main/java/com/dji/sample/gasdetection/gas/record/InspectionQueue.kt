package com.dji.sample.gasdetection.gas.record

import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/** 队列调度抽象：生产用单线程调度器，测试用手动推进的调度器。 */
interface InspectionScheduler {
    fun execute(task: () -> Unit)
    fun schedule(delayMs: Long, task: () -> Unit)
}

/** 生产实现：单线程 `ScheduledExecutorService`，与巡逻 Demo 的上传队列一致。 */
class ExecutorInspectionScheduler(
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(),
) : InspectionScheduler {
    override fun execute(task: () -> Unit) {
        executor.execute(task)
    }

    override fun schedule(delayMs: Long, task: () -> Unit) {
        executor.schedule(task, delayMs, TimeUnit.MILLISECONDS)
    }

    /** 应用退出时停止调度。 */
    fun shutdown() {
        executor.shutdownNow()
    }
}

/** 队列文件读写抽象；生产实现使用 `AtomicFile`。 */
interface InspectionQueueFile {
    fun read(): String?
    fun write(value: String)
}

/**
 * 检测记录上传队列。
 *
 * 设计意图：直接移植巡逻 Demo `AssetUploadManager` 的队列模型——单线程串行、`busy + pump()` 单飞、
 * `QUEUED/UPLOADING/UPLOADED/FAILED` 状态、失败按 `1s << retryCount` 指数退避（上限 [maxRetries]）、
 * 恢复时把残留 `UPLOADING` 改回 `QUEUED`、`UPLOADED` 记录保留。区别仅在于上传载荷是检测记录 JSON，
 * 传输层使用本模块的 `HttpURLConnection` 适配。
 */
class InspectionQueue(
    private val file: InspectionQueueFile,
    private val transport: InspectionTransport,
    private val scheduler: InspectionScheduler,
    private val maxRetries: Int = MAX_RETRIES,
    private val now: () -> Long = System::currentTimeMillis,
    private val onStateChanged: (List<InspectionRecord>) -> Unit = {},
) {

    private val items = mutableListOf<InspectionRecord>()
    private var busy = false

    /** 启动：恢复队列并立即尝试补传。 */
    fun start() = submit {
        restore()
        persistAndPublish()
        pump()
    }

    /** 入队一条记录；按 `recordId` 去重。 */
    fun enqueue(record: InspectionRecord) = submit {
        if (items.none { it.recordId == record.recordId }) {
            items += record.copy(status = RecordStatus.QUEUED, retryCount = 0, lastError = null)
            persistAndPublish()
        }
        pump()
    }

    /** 手动重试一条失败记录。 */
    fun retry(recordId: String) = submit {
        val index = items.indexOfFirst { it.recordId == recordId }
        if (index >= 0 && items[index].status != RecordStatus.UPLOADED) {
            items[index] = items[index].copy(
                status = RecordStatus.QUEUED,
                retryCount = 0,
                lastError = null,
            )
            Log.i(TAG, "manual retry id=$recordId")
            persistAndPublish()
        }
        pump()
    }

    /** 当前队列快照（供测试与界面读取）。 */
    fun snapshot(): List<InspectionRecord> = synchronized(items) { items.toList() }

    private fun pump() {
        if (busy) return
        // 先处理待上传，避免完成回写的失败阻塞其它记录。
        val upload = items.firstOrNull { it.status == RecordStatus.QUEUED }
        if (upload != null) {
            beginUpload(upload)
            return
        }
        val completion = items.firstOrNull { it.completionPending && it.status == RecordStatus.UPLOADED }
        if (completion != null) beginCompletion(completion)
    }

    private fun beginUpload(record: InspectionRecord) {
        busy = true
        replace(record.recordId) { it.copy(status = RecordStatus.UPLOADING) }
        persistAndPublish()
        transport.upload(record) { result ->
            scheduler.execute { onUploadResult(record.recordId, result) }
        }
    }

    private fun onUploadResult(recordId: String, result: InspectionUploadResult) {
        busy = false
        val current = items.firstOrNull { it.recordId == recordId } ?: return
        when (result) {
            is InspectionUploadResult.Success -> {
                Log.i(TAG, "upload ok id=$recordId")
                replace(recordId) {
                    it.copy(
                        status = RecordStatus.UPLOADED,
                        uploadedAtMs = now(),
                        completionPending = true,
                        retryCount = 0,
                        lastError = null,
                    )
                }
                persistAndPublish()
                pump()
            }

            is InspectionUploadResult.Failure -> {
                Log.e(TAG, "upload failed id=$recordId: ${result.message}")
                if (current.retryCount < maxRetries) {
                    val nextRetry = current.retryCount + 1
                    replace(recordId) {
                        it.copy(
                            status = RecordStatus.QUEUED,
                            retryCount = nextRetry,
                            lastError = result.message,
                        )
                    }
                    persistAndPublish()
                    scheduler.schedule(1L shl current.retryCount) { pump() }
                } else {
                    replace(recordId) {
                        it.copy(status = RecordStatus.FAILED, lastError = result.message)
                    }
                    persistAndPublish()
                    pump()
                }
            }
        }
    }

    private fun beginCompletion(record: InspectionRecord) {
        busy = true
        transport.completeJob(record.jobId) { result ->
            scheduler.execute {
                busy = false
                when (result) {
                    is InspectionUploadResult.Success -> {
                        replace(record.recordId) { it.copy(completionPending = false, lastError = null) }
                        persistAndPublish()
                        pump()
                    }

                    is InspectionUploadResult.Failure -> {
                        Log.w(TAG, "complete write failed job=${record.jobId}: ${result.message}")
                        replace(record.recordId) { it.copy(lastError = result.message) }
                        persistAndPublish()
                    }
                }
            }
        }
    }

    private fun restore() {
        val restored = InspectionJson.decodeQueue(file.read())
        items.clear()
        restored.forEach {
            items += if (it.status == RecordStatus.UPLOADING) {
                it.copy(status = RecordStatus.QUEUED)
            } else {
                it
            }
        }
    }

    private fun replace(recordId: String, block: (InspectionRecord) -> InspectionRecord) {
        val index = items.indexOfFirst { it.recordId == recordId }
        if (index >= 0) items[index] = block(items[index])
    }

    private fun persistAndPublish() {
        runCatching { file.write(InspectionJson.encodeQueue(items)) }
            .onFailure { Log.e(TAG, "store write failed: ${it.message}") }
        val snapshot = items.toList()
        onStateChanged(snapshot)
    }

    private fun submit(task: () -> Unit) {
        scheduler.execute {
            runCatching(task).onFailure { Log.e(TAG, "queue task failed: ${it.message}") }
        }
    }

    companion object {
        /** 自动重试上限，与巡逻 Demo 保持一致。 */
        const val MAX_RETRIES = 5

        private const val TAG = "GasInspectionRecord"
    }
}
