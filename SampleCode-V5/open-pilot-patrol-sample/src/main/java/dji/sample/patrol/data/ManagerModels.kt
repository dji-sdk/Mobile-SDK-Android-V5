package dji.sample.patrol.data

import java.io.File

/** 简单回调，所有 Manager 共用；不依赖协程。 */
interface ResultCallback<T> {
    fun onSuccess(data: T)
    fun onFailure(error: Throwable)
}

data class CloudConfig(
    val baseUrl: String,
    val rcSerialNumber: String,
    val accessToken: String = "",
)

enum class BackendConnectionState { CHECKING, CONNECTED, UNAVAILABLE }

enum class WaylineState {
    IDLE,
    PREPARING,
    READY,
    EXECUTING,
    PAUSED,
    FINISHED,
    FAILED,

    /** 飞手主动停止后进入的中性态：航线已终止，飞行器可能仍在空中悬停。 */
    STOPPED,
}

/** 巡逻航线编排态：对上层暴露的权威航线状态（由 `WaylineManager` 权威流映射）。 */
enum class RouteState {
    ROUTE_PENDING,
    ROUTE_PREPARING,
    ROUTE_READY,
    EXECUTING,
    PAUSED,
    FINISHED,
    FAILED,

    /** 用户主动停止：不是失败，也不是“从未加载”；空中安全动作与重新加载由该态承载。 */
    STOPPED,
}

/** 航线失败分类（与 spec 的失败类别对齐）。 */
enum class RouteFailureCategory {
    ROUTE_ERROR,
    NOT_READY,
    CLOUD_ERROR,
    CANCELLED,
    TIMEOUT,
}

/** 航线进度的展示阶段。 */
enum class RouteProgressStage {
    IDLE,
    PREPARING,
    DOWNLOADING,
    UPLOADING,
    EXECUTING,
    INTERRUPTED,
}

/**
 * 航线执行进度的结构化快照。
 *
 * 设计意图：只承载事实字段（阶段、航段、航点、百分比、中断码），不携带调试串或内部标识，
 * 由展示层按用户语言与航段数量决定文案，避免 `wayline=…`、`mission=…` 之类内容进入界面。
 */
data class WaylineProgress(
    /** 当前阶段；`IDLE` 表示无可展示进度。 */
    val stage: RouteProgressStage = RouteProgressStage.IDLE,
    /** 当前航点序号（跨航段累计，1 起）。 */
    val currentWaypoint: Int = 0,
    /** 总航点数；0 表示未知。 */
    val totalWaypoints: Int = 0,
    /** 当前航段序号（1 起）；仅在多航段时有意义。 */
    val waylineIndex: Int = 0,
    /** 本轮航线包含的航段数。 */
    val waylineCount: Int = 0,
    /** 准备/下载/上传百分比，0–100。 */
    val percent: Int = 0,
    /** 中断错误码；仅 [RouteProgressStage.INTERRUPTED] 时可能有值。 */
    val interruptCode: String? = null,
)

/**
 * 航线展示唯一快照（[dji.sample.patrol.manager.RouteController] 对外出口）。
 *
 * 设计意图：把航线状态、进度、错误与**归属工单**绑定为一个不可变快照，页面与浮窗只读同一份数据，
 * 避免「新工单 + 上一工单进度」或「一侧执行中、一侧失败」的中间态。
 * 线程安全：由 `RouteController` 在主线程投递，观察者不应修改字段。
 */
data class RouteSnapshot(
    /** 该航线快照归属的工单；`null` 表示当前没有已加载/已绑定的航线。 */
    val jobId: String? = null,
    /** 航线权威状态（由 `RouteController` 采信后的结果）。 */
    val state: RouteState = RouteState.ROUTE_PENDING,
    /** 航线执行进度；非归属工单不得展示该值。 */
    val progress: WaylineProgress = WaylineProgress(),
    /** 最近一次可读错误；中性态与成功态为 `null`。 */
    val error: String? = null,
    /** 失败分类，供 UI 与日志归因；非失败态为 `null`。 */
    val failureCategory: RouteFailureCategory? = null,
)

data class UploadProgress(
    val jobId: String,
    val total: Int,
    val uploaded: Int,
    val failed: Int,
    val totalBytes: Long = 0,
    val uploadedBytes: Long = 0,
)

data class SessionInfo(
    val sessionId: String,
    val chunkSizeBytes: Long,
    /** 服务端按分片顺序返回预签名 URL；照片通常只有一个。 */
    val partUrls: List<String> = emptyList(),
    val uploadedChunks: List<Int> = emptyList(),
)

data class ChunkResult(
    val index: Int,
    val success: Boolean,
    val message: String? = null,
)

data class WaylineFile(
    val jobId: String,
    val file: File,
)
