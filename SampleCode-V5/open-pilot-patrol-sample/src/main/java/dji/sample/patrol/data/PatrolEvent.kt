package dji.sample.patrol.data

/**
 * 巡逻识别事件（工程自有投影，不携带 `dji.*` 类型）。
 *
 * 设计意图：把识别/拍照环节产生的可上报事件（命中、降级、拍照失败等）与 SDK 类型解耦，
 * 供上报层 [dji.sample.patrol.manager.PatrolAssetReporter] 序列化为 JSON Asset 并边飞边同步。
 *
 * @property id 事件唯一标识（用于去重）。
 * @property kind 目标类别投影。
 * @property targetIndex 目标索引（同类型下序号）。
 * @property at 事件发生时间（epoch millis）。
 * @property geo 事件发生时飞行器经纬高（可空）；无有效定位时为 `null`。
 * @property description 可读描述（可空）。
 */
data class PatrolEvent(
    val id: String,
    val kind: TargetKind,
    val targetIndex: Int,
    val at: Long,
    val geo: GeoPoint3D? = null,
    val description: String? = null,
)
