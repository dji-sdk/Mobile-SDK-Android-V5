package dji.sample.patrol.manager

import android.os.Handler
import android.os.Looper
import android.util.Log
import dji.sample.patrol.data.Asset
import dji.sample.patrol.data.CategorizedException
import dji.sample.patrol.data.MediaReference
import dji.sample.patrol.data.RecognitionFailureCategory
import dji.sample.patrol.data.ResultCallback
import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.key.DJICameraKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.camera.CameraMode
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.common.EmptyMsg
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.KeyManager

/** 应用层相机组件索引（映射到 MSDK `ComponentIndexType`，保持 set/拍照同源）。 */
enum class CameraComponent { LEFT_OR_MAIN }

/** 应用层拍照模式。 */
enum class PhotoMode { PHOTO_NORMAL }

/** 拍照失败阶段：PRE_FLIGHT 表示快门命令尚未下发，SHUTTER 表示已下发后失败。 */
enum class CapturePhase { PRE_FLIGHT, SHUTTER }

/** 带阶段的拍照失败，供编排层区分「没拍成」与「已发令后失败」。 */
class CaptureFailure(
    val phase: CapturePhase,
    val category: RecognitionFailureCategory,
    message: String,
) : Exception(message)

/** 相机命令端口（design D4/D9）：可 JVM fake，隔离 `CameraKey`/`KeyManager`。 */
interface CameraCommandPort {
    fun isPlayingBack(component: CameraComponent): Boolean
    fun setCameraMode(component: CameraComponent, mode: PhotoMode, callback: ResultCallback<Unit>)
    fun shootPhoto(component: CameraComponent, callback: ResultCallback<Unit>)
}

/** 媒体采集边界（复用既有 [MediaManager]，仅暴露拍照编排需要的能力，便于 fake）。 */
interface MediaCapturePort {
    /**
     * 保证工单存在当前轮的执行窗口；`onReady` 在窗口可用后回调。
     *
     * 设计意图：拍照是独立的媒体入口，必须自己保证窗口有效，不能依赖航线加载/恢复路径。
     */
    fun ensureExecutionWindow(jobId: String, onReady: () -> Unit)

    fun beginJob(jobId: String, onNewMedia: (MediaReference) -> Unit)
    fun endJob()
    fun downloadNewPhotos(jobId: String, callback: ResultCallback<List<Asset>>)
}

/** 生产媒体边界：纯委托 [MediaManager]，不重写采集/下载/校验。 */
object MediaManagerCapturePort : MediaCapturePort {
    override fun ensureExecutionWindow(jobId: String, onReady: () -> Unit) =
        MediaManager.ensureCurrentExecutionWindow(jobId, "media-capture", onReady)

    override fun beginJob(jobId: String, onNewMedia: (MediaReference) -> Unit) =
        MediaManager.beginJob(jobId, onNewMedia)

    override fun endJob() = MediaManager.endJob()

    override fun downloadNewPhotos(jobId: String, callback: ResultCallback<List<Asset>>) {
        // 飞行中拍照路径不进入回放：等生成事件/相机空闲后再拉列表，避免相机忙时中止请求。
        MediaManager.downloadNewPhotos(jobId, false, object : ResultCallback<List<Asset>> {
            override fun onSuccess(data: List<Asset>) {
                if (data.isEmpty()) {
                    callback.onFailure(
                        CategorizedException(
                            RecognitionFailureCategory.CAPTURE_ERROR,
                            "No photo was downloaded",
                        ),
                    )
                } else {
                    callback.onSuccess(data)
                }
            }

            override fun onFailure(error: Throwable) = callback.onFailure(error)
        })
    }
}

/**
 * 定点拍照（design D4）。
 *
 * 严格串行「设拍照模式 → 等 onComplete → 下发拍照」；**相机接受拍照命令即视为本次拍照成功**，
 * 立即回调 [capture] 的 `onShutter`，让编排层恢复航线；照片下载转后台，结果经 `onMedia` 返回，
 * 只用于补全点位与提示，不影响航线。
 *
 * 门控：负载已连接（端口非空）、非回放、存储可用；失败标 `capture-error`/`not-ready` 且保留可重试。
 */
object CaptureController {

    private const val TAG = "PatrolCapture"

    /** 收到生成事件后静置，让相机完成照片写入。 */
    private const val MEDIA_SETTLE_MS = 800L

    /** 未收到生成事件时的兜底拉列表时间。 */
    private const val MEDIA_EVENT_TIMEOUT_MS = 2_000L

    /** 列表拉取失败后的有界重试间隔。 */
    private val MEDIA_RETRY_DELAYS_MS = longArrayOf(2_000L, 4_000L)

    @Volatile
    var cameraPort: CameraCommandPort? = null

    @Volatile
    var mediaPort: MediaCapturePort = MediaManagerCapturePort

    @Volatile
    var component: CameraComponent = CameraComponent.LEFT_OR_MAIN

    /** 存储可用门控（生产可接 KeyManager，默认可用）。 */
    @Volatile
    var storageAvailable: () -> Boolean = { true }

    /** 当前工单 id（生产接 [WaylineManager.preparedJobId]）。 */
    @Volatile
    var currentJobId: () -> String? = { WaylineManager.preparedJobId }

    @Volatile
    private var capturing = false

    /** 媒体采集（等待生成事件、拉列表、重试）在飞：期间拒绝新的拍照，避免并发下载。 */
    @Volatile
    private var mediaCollectionInFlight = false

    /** 媒体采集代际：取消/重新配置后，迟到的生成事件与重试任务直接丢弃。 */
    @Volatile
    private var mediaGeneration = 0L

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 延迟调度端口：生产投递主线程，JVM 单测注入同步/手工队列执行。
     */
    @Volatile
    var delay: (Long, () -> Unit) -> Unit = { millis, task -> mainHandler.postDelayed(task, millis) }

    /** 是否有拍照在飞（单次在飞保护，design D4）。 */
    val isCapturing: Boolean get() = capturing

    /**
     * 命中前置：返回 null 表示可以拍照；非 null 为不可拍照的可读原因。
     *
     * 只读判定，不产生副作用；供命中编排在暂停前或等待结束后使用。
     */
    fun readinessFailure(): String? {
        if (capturing) return "A capture is already in flight"
        if (mediaCollectionInFlight) return "Media sync is in progress"
        val port = cameraPort ?: return "Camera is not connected"
        if (port.isPlayingBack(component)) return "Camera is playing back"
        if (!storageAvailable()) return "Camera storage is not available"
        return null
    }

    /**
     * 触发一次定点拍照（单次在飞保护）。
     *
     * @param onShutter 快门结果：成功代表相机已接受拍照命令，调用方可立即恢复航线。
     * @param onMedia 后台媒体采集结果（可空）；成功用于补全点位，失败只记日志/提示，不影响航线。
     */
    fun capture(onShutter: ResultCallback<Unit>, onMedia: ResultCallback<List<Asset>>? = null) {
        val reason = readinessFailure()
        if (reason != null) {
            Log.w(TAG, "capture rejected: $reason")
            reject(onShutter, RecognitionFailureCategory.NOT_READY, reason)
            return
        }
        val port = cameraPort ?: run {
            reject(onShutter, RecognitionFailureCategory.NOT_READY, "Camera is not connected")
            return
        }
        capturing = true
        Log.i(TAG, "capture begin component=$component")
        port.setCameraMode(component, PhotoMode.PHOTO_NORMAL, object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) {
                Log.i(TAG, "camera mode set to PHOTO_NORMAL")
                shoot(port, onShutter, onMedia)
            }

            override fun onFailure(error: Throwable) {
                capturing = false
                Log.e(TAG, "setCameraMode failed: ${error.message}")
                failShutter(onShutter, CapturePhase.PRE_FLIGHT, RecognitionFailureCategory.CAPTURE_ERROR, error)
            }
        })
    }

    /** 用户中途取消/断连：结束媒体 job，丢弃待执行的生成事件/重试任务。 */
    fun cancel() {
        capturing = false
        mediaCollectionInFlight = false
        mediaGeneration++
        runCatching { mediaPort.endJob() }
    }

    private fun shoot(
        port: CameraCommandPort,
        onShutter: ResultCallback<Unit>,
        onMedia: ResultCallback<List<Asset>>?,
    ) {
        port.shootPhoto(component, object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) {
                // 快门接受即释放单次在飞保护，并先让编排层恢复航线；媒体采集随后在后台进行。
                capturing = false
                Log.i(TAG, "shutter accepted; route may resume")
                onShutter.onSuccess(Unit)
                collectMedia(onMedia)
            }

            override fun onFailure(error: Throwable) {
                capturing = false
                Log.e(TAG, "shootPhoto failed: ${error.message}")
                failShutter(onShutter, CapturePhase.SHUTTER, RecognitionFailureCategory.CAPTURE_ERROR, error)
            }
        })
    }

    private fun collectMedia(onMedia: ResultCallback<List<Asset>>?) {
        val jobId = currentJobId()
        if (jobId.isNullOrBlank()) {
            Log.e(TAG, "media pipeline skipped: no active job")
            onMedia?.onFailure(
                CategorizedException(
                    RecognitionFailureCategory.CAPTURE_ERROR,
                    "No active job for media pipeline",
                ),
            )
            return
        }
        val generation = ++mediaGeneration
        mediaCollectionInFlight = true
        var attempt = 0
        var downloadStarted = false
        var completed = false

        fun finish(assets: List<Asset>?, error: Throwable?) {
            if (generation != mediaGeneration || completed) return
            completed = true
            mediaCollectionInFlight = false
            if (!assets.isNullOrEmpty()) {
                Log.i(
                    TAG,
                    "media pipeline complete job=$jobId assets=${assets.map { it.assetId }}",
                )
                onMedia?.onSuccess(assets)
            } else {
                Log.e(TAG, "media pipeline failed (background) job=$jobId: ${error?.message}")
                onMedia?.onFailure(error ?: IllegalStateException("Media download failed"))
            }
        }

        fun startDownload() {
            if (generation != mediaGeneration || downloadStarted || completed) return
            downloadStarted = true
            Log.i(TAG, "media pipeline download start job=$jobId attempt=${attempt + 1}")
            mediaPort.downloadNewPhotos(jobId, object : ResultCallback<List<Asset>> {
                override fun onSuccess(data: List<Asset>) = finish(data, null)

                override fun onFailure(error: Throwable) {
                    if (generation != mediaGeneration || completed) return
                    if (attempt < MEDIA_RETRY_DELAYS_MS.size) {
                        val wait = MEDIA_RETRY_DELAYS_MS[attempt++]
                        Log.w(
                            TAG,
                            "media pipeline retry in ${wait}ms job=$jobId attempt=${attempt}: ${error.message}",
                        )
                        downloadStarted = false
                        delay(wait) { startDownload() }
                    } else {
                        finish(null, error)
                    }
                }
            })
        }

        // 先保证本轮窗口可用，再注册媒体事件：新照片必须落在本轮窗口内，不能被上一轮已关闭窗口排除。
        mediaPort.ensureExecutionWindow(jobId) {
            if (generation != mediaGeneration || completed) return@ensureExecutionWindow
            Log.i(TAG, "media pipeline begin (background) job=$jobId; awaiting generated media")
            mediaPort.beginJob(jobId) { reference ->
                Log.i(TAG, "new media job=$jobId index=${reference.fileIndex}; settle before list pull")
                delay(MEDIA_SETTLE_MS) {
                    if (generation == mediaGeneration) startDownload()
                }
            }
            // 生成事件未到达时的兜底；到达后 startDownload 的幂等保护会忽略重复调用。
            delay(MEDIA_EVENT_TIMEOUT_MS) {
                if (generation == mediaGeneration && !downloadStarted) {
                    Log.w(TAG, "generated media event timeout; fallback to list job=$jobId")
                    startDownload()
                }
            }
        }
    }

    private fun reject(onShutter: ResultCallback<Unit>, category: RecognitionFailureCategory, message: String) {
        onShutter.onFailure(CaptureFailure(CapturePhase.PRE_FLIGHT, category, message))
    }

    private fun failShutter(
        onShutter: ResultCallback<Unit>,
        phase: CapturePhase,
        category: RecognitionFailureCategory,
        error: Throwable,
    ) {
        Log.w(TAG, "capture failed: phase=$phase reason=${error.message}")
        onShutter.onFailure(CaptureFailure(phase, category, error.message ?: "capture failed"))
    }

    /** 测试注入点：替换端口/门控并复位在飞标志。 */
    fun configure(
        port: CameraCommandPort?,
        media: MediaCapturePort,
        component: CameraComponent = CameraComponent.LEFT_OR_MAIN,
        storage: () -> Boolean = { true },
        jobId: () -> String? = { "test-job" },
    ) {
        cameraPort = port
        mediaPort = media
        this.component = component
        storageAvailable = storage
        currentJobId = jobId
        mediaGeneration++
        mediaCollectionInFlight = false
        capturing = false
        delay = { millis, task -> mainHandler.postDelayed(task, millis) }
        capturing = false
    }
}

/** 生产相机端口：读 `KeyManager`，`CameraKey`/`DJICameraKey` 全程携带同一 `componentIndex`。 */
object KeyManagerCameraCommandPort : CameraCommandPort {

    private const val TAG = "PatrolCapture"

    private fun indexOf(component: CameraComponent): ComponentIndexType = when (component) {
        CameraComponent.LEFT_OR_MAIN -> ComponentIndexType.LEFT_OR_MAIN
    }

    override fun isPlayingBack(component: CameraComponent): Boolean {
        val key = KeyTools.createKey(DJICameraKey.KeyIsPlayingBack, indexOf(component))
        return KeyManager.getInstance().getValue(key) == true
    }

    override fun setCameraMode(component: CameraComponent, mode: PhotoMode, callback: ResultCallback<Unit>) {
        val key = KeyTools.createKey(CameraKey.KeyCameraMode, indexOf(component))
        KeyManager.getInstance().setValue(key, mode.toSdk(), object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                Log.i(TAG, "setValue KeyCameraMode onSuccess")
                callback.onSuccess(Unit)
            }

            override fun onFailure(error: IDJIError) {
                Log.e(TAG, "setValue KeyCameraMode onFailure ${error.errorCode()} ${error.description()}")
                callback.onFailure(IllegalStateException("${error.errorCode()}: ${error.description()}"))
            }
        })
    }

    override fun shootPhoto(component: CameraComponent, callback: ResultCallback<Unit>) {
        val key = KeyTools.createKey(CameraKey.KeyStartShootPhoto, indexOf(component))
        KeyManager.getInstance().performAction(
            key,
            object : CommonCallbacks.CompletionCallbackWithParam<EmptyMsg> {
                override fun onSuccess(data: EmptyMsg?) {
                    Log.i(TAG, "performAction KeyStartShootPhoto onSuccess")
                    callback.onSuccess(Unit)
                }

                override fun onFailure(error: IDJIError) {
                    Log.e(TAG, "performAction KeyStartShootPhoto onFailure ${error.errorCode()} ${error.description()}")
                    callback.onFailure(IllegalStateException("${error.errorCode()}: ${error.description()}"))
                }
            },
        )
    }
}

private fun PhotoMode.toSdk(): CameraMode = when (this) {
    PhotoMode.PHOTO_NORMAL -> CameraMode.PHOTO_NORMAL
}
