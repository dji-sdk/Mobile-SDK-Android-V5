package dji.sample.flighttask.data

/**
 * 通用数据模型。
 *
 * 通用性约束：全链路（队列/上传/进度/UI 列表）只认 Job/Asset 抽象；
 * 照片相关代码全工程只允许出现在 AssetSource 的一个实现类和类型目录的一条数据里。
 */

/** 资源类型目录条目：类型不是枚举硬编码，而是数据驱动（来源：服务端 /catalog/asset-types，本地缓存） */
data class AssetTypeDescriptor(
    val typeId: String,            // "photo" / "video" / "model" / "route" / "log"
    val extensions: List<String>,
    val maxSizeBytes: Long,
    val chunkSizeBytes: Long,
    val realtimeEligible: Boolean, // 是否允许边飞边同步
)

enum class AssetState { DETECTED, QUEUED, UPLOADING, UPLOADED, FAILED }

data class Asset(
    val assetId: String,
    val jobId: String,             // 按工单归类，队列/进度/归档均按此分组
    val typeId: String,            // 关联 AssetTypeDescriptor
    val localUri: String? = null,
    val sizeBytes: Long = 0,
    val md5: String? = null,
    val state: AssetState = AssetState.DETECTED,
    val meta: Map<String, String> = emptyMap(), // 扩展元数据（拍摄时间/相机/坐标…），不建专属字段
)

enum class JobStatus { DISPATCHED, ACCEPTED, IN_PROGRESS, FINALIZING, COMPLETED, CANCELLED }

enum class RouteStatus { NOT_REQUIRED, CLOUD_PROVIDED, INVALID }

data class JobEvent(
    val timestamp: Long,
    val description: String,
)

data class Job(
    val jobId: String,
    val title: String,
    val status: JobStatus,
    val routeStatus: RouteStatus,
    val routeFileUrl: String? = null,
    val timeline: List<JobEvent> = emptyList(),
    val extras: Map<String, String> = emptyMap(), // missionType / requirements 等业务扩展字段
)

/** 悬浮窗流程环节（UI 图「支持自定义替换」区块①），业务方替换此列表即可自定义流程 */
data class FlowStage(val id: String, val labelResId: Int)
