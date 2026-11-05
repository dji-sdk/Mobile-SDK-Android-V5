package dji.sample.patrol.manager

import android.util.Log
import dji.open.pilot.client.manager.file.FileTransferManager
import dji.sample.patrol.data.ResultCallback
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import java.io.File

/**
 * 文件推送边界（design D5）：把「本地文件推到 Pilot」从队列编排中隔离，便于 JVM fake。
 *
 * @param onSuccess 远端文件名（Pilot 端以文件名作为唯一标识）
 * @param onFailure 错误码与描述（错误码用于识别 `UPLOAD_FILE_BUSY`）
 */
interface FilePushTransport {
    fun push(
        localPath: String,
        onSuccess: (remoteName: String) -> Unit,
        onFailure: (code: String, message: String) -> Unit,
    )
}

/**
 * 串行文件推送队列（design D5）。
 *
 * Pilot 服务端同一时刻只处理一个上传任务，并发调用立即返回 `UPLOAD_FILE_BUSY`。地图 PNG 图标与
 * AR SVG 图标共用本队列，避免互相打断导致一方降级；BUSY 时把任务放回队尾有界重试（最多 3 次），
 * 其它错误直接回调失败，不使用固定延时轮询。
 */
object RemoteFilePusher {

    private const val TAG = "PatrolFilePush"
    private const val BUSY_CODE = "UPLOAD_FILE_BUSY"
    private const val MAX_BUSY_RETRY = 3

    private data class Task(
        val localPath: String,
        val callback: ResultCallback<String>,
        val busyRetries: Int = 0,
    )

    private val lock = Any()
    private val queue = ArrayDeque<Task>()
    private var inFlight = false

    /** 传输实现（生产为 [PilotFilePushTransport]，测试可替换）。 */
    @Volatile
    var transport: FilePushTransport = PilotFilePushTransport

    /** 入队并尝试推进队列；回调保证在对应文件上传结束后触发。 */
    fun push(localPath: String, callback: ResultCallback<String>) {
        synchronized(lock) { queue.addLast(Task(localPath, callback)) }
        drain()
    }

    private fun drain() {
        val task = synchronized(lock) {
            if (inFlight) return
            val next = queue.removeFirstOrNull() ?: return
            inFlight = true
            next
        }
        Log.i(TAG, "push start path=${task.localPath} retry=${task.busyRetries}")
        runCatching {
            transport.push(
                task.localPath,
                onSuccess = { remoteName ->
                    synchronized(lock) { inFlight = false }
                    Log.i(TAG, "push success path=${task.localPath} remote=$remoteName")
                    task.callback.onSuccess(remoteName)
                    drain()
                },
                onFailure = { code, message -> onTaskFailure(task, code, message) },
            )
        }.onFailure { onTaskFailure(task, "TRANSPORT_ERROR", it.message ?: "transport push failed") }
    }

    private fun onTaskFailure(task: Task, code: String, message: String) {
        synchronized(lock) { inFlight = false }
        if (code == BUSY_CODE && task.busyRetries < MAX_BUSY_RETRY) {
            Log.w(TAG, "push busy, requeue path=${task.localPath} retry=${task.busyRetries + 1}")
            synchronized(lock) { queue.addLast(task.copy(busyRetries = task.busyRetries + 1)) }
            drain()
            return
        }
        Log.e(TAG, "push failed path=${task.localPath} code=$code desc=$message")
        task.callback.onFailure(IllegalStateException("$code: $message"))
        drain()
    }

    /**
     * 断连清理：丢弃排队任务并复位串行状态。
     *
     * 断连时发起中的上传可能永远不回调，若不复位 `inFlight`，重连后的图标推送会全部卡在队列里。
     */
    fun reset() {
        synchronized(lock) {
            queue.clear()
            inFlight = false
        }
        Log.i(TAG, "queue reset on disconnect")
    }

    /** 测试辅助：恢复初始状态。 */
    fun resetForTest(transportImpl: FilePushTransport = PilotFilePushTransport) {
        reset()
        transport = transportImpl
    }
}

/** 生产实现：`FileTransferManager.pushFileToRemote`；`dji.*` 调用集中在此。 */
object PilotFilePushTransport : FilePushTransport {

    private const val TAG = "PatrolFilePush"

    override fun push(
        localPath: String,
        onSuccess: (remoteName: String) -> Unit,
        onFailure: (code: String, message: String) -> Unit,
    ) {
        FileTransferManager.getInstance().pushFileToRemote(
            localPath,
            object : CommonCallbacks.CompletionCallbackWithProgress<Double> {
                override fun onProgressUpdate(progress: Double) = Unit

                override fun onSuccess() {
                    val remoteName = File(localPath).name
                    Log.i(TAG, "pushFileToRemote onSuccess $remoteName")
                    onSuccess(remoteName)
                }

                override fun onFailure(error: IDJIError) {
                    Log.e(TAG, "pushFileToRemote onFailure ${error.errorCode()} ${error.description()}")
                    onFailure(error.errorCode(), error.description())
                }
            },
        )
    }
}
