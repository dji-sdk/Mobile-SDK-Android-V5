package dji.sample.patrol.data

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
    val routeFileName: String? = null,
    val extensions: List<JobExtension> = emptyList(),
    val allowedActions: List<String> = emptyList(),
    val externalId: String? = null,
    val revision: Long? = null,
    val timeline: List<JobEvent> = emptyList(),
    val extras: Map<String, String> = emptyMap(), // missionType / requirements 等业务扩展字段
)

/** 云端下发的 Job 扩展项（如 `dji.flight-route`）；全部可选、gson 缺省，旧服务端响应向后兼容。 */
data class JobExtension(
    val type: String,
    val schemaVersion: String? = null,
    val payload: FlightRoutePayload? = null,
)

/** `dji.flight-route` 扩展项的载荷：航线状态与航线资源标识。 */
data class FlightRoutePayload(
    val routeStatus: String? = null, // CLOUD_PROVIDED / NOT_REQUIRED / INVALID
    val assetId: String? = null,
)

/**
 * 可配置浮窗流程（UI 图「支持自定义替换」区块①）：业务方实现 [FlowConfig] 并注入，
 * 框架据此一次性创建阶段容器、按状态切换可见性，而不是把阶段集合硬编码在框架内。
 * 未注入配置时框架退回其默认八状态流程。
 */
interface FlowConfig {
    val kind: String
    val stages: List<FlowStage>

    /** 唯一阶段入口：把共享派生阶段映射为浮窗 stage id；`null` 表示走既有 legacy 流程。 */
    fun resolveStage(stage: PatrolStage): String?
}

data class FlowStage(
    val id: String,
    val labelResId: Int,
    val controls: List<FlowControl> = emptyList(),
)

data class FlowControl(
    val action: String,
    val labelResId: Int,
    val primary: Boolean = false,
)

/**
 * 兜底 Gson 反序列化：Gson 走 Unsafe 分配、不执行 Kotlin 默认值，服务端省略的集合字段
 * （extensions/allowedActions/timeline/extras）会被解析成 null。反序列化后调用本函数，
 * 保证 [Job] 的集合字段恒非空，下游遍历/索引不会 NPE。
 */
fun Job.withSafeCollections(): Job = copy(
    extensions = asNullable<List<JobExtension>>(extensions) ?: emptyList(),
    allowedActions = asNullable<List<String>>(allowedActions) ?: emptyList(),
    timeline = asNullable<List<JobEvent>>(timeline) ?: emptyList(),
    extras = asNullable<Map<String, String>>(extras) ?: emptyMap(),
)

/** 抹掉编译器的非空假设，使 `?:` 兜底在运行期真正生效（Gson 可能写入 null）。 */
@Suppress("UNCHECKED_CAST")
private fun <T> asNullable(value: Any?): T? = value as T?
