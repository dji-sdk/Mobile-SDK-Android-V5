package dji.sample.patrol.data

/**
 * 上传编排态（区别于工单 [JobStatus]，用于浮窗/通知栏展示「边飞边同步 / 收尾补传 / 完成」）。
 *
 * 设计意图：把「上传生命周期」从工单状态机中分离出来，供 UI 只读订阅，不参与业务决策。
 */
enum class UploadStage {
    /** 无活动工单或未开始上传。 */
    IDLE,

    /** 航线执行中，边飞边同步中。 */
    LIVE,

    /** 航线结束/停止，收尾补传中。 */
    FINALIZING,

    /** 该工单全部 Asset 上传成功。 */
    COMPLETED,

    /** 存在上传失败且自动重试耗尽，等待人工重试。 */
    FAILED,
}

/**
 * 上报批次中的拍照点位条目（JSON 载荷）。
 *
 * @property assetId 关联的照片 Asset id（可空：照片尚未落盘时不上报该点位，正常不为空）。
 */
data class PointEntry(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val kind: String,
    val targetIndex: Int,
    val capturedAt: Long,
    val assetId: String?,
)

/** 上报批次中的识别事件条目（JSON 载荷）。 */
data class EventEntry(
    val id: String,
    val kind: String,
    val targetIndex: Int,
    val at: Long,
    val description: String?,
)

/**
 * 一次 JSON 上报载荷（按事件时间窗合批）。
 *
 * @property windowStart 批次窗口开始时间（epoch millis）。
 * @property windowEnd 批次窗口结束时间（epoch millis）。
 */
data class PatrolReportBatch(
    val jobId: String,
    val windowStart: Long,
    val windowEnd: Long,
    val points: List<PointEntry> = emptyList(),
    val events: List<EventEntry> = emptyList(),
)
