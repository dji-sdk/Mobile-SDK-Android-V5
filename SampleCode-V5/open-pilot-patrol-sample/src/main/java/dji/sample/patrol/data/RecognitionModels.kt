package dji.sample.patrol.data

/**
 * 识别拍照环节的数据模型（design D1/D2/D5）。
 *
 * 全部为工程自有类型，**不携带 `dji.*` 类型**，使状态机/编排可脱离真机在 JVM 上单测。
 */

/** 识别状态机阶段（spec「State Machine」）。 */
enum class RecognitionStage { IDLE, SENSING, MATCHED, DEGRADED, STOPPED }

/** 识别目标类别投影（`AutoSensingTargetType` 的工程内投影，不依赖 `dji.*`）。 */
enum class TargetKind { PERSON, CAR, BOAT, CUSTOM, UNKNOWN }

/**
 * 命中的识别目标投影。
 *
 * @property targetIndex 目标索引（同类型下的序号）。
 * @property targetType 目标类别；`(targetIndex, targetType)` 为落点去重键。
 * @property label 识别标签（可空串）。
 */
data class DetectionTarget(
    val targetIndex: Int,
    val targetType: TargetKind,
    val label: String = "",
) {
    /** `(targetIndex, targetType)` 去重键。 */
    val dedupKey: String get() = "$targetIndex/$targetType"
}

/** 经纬高快照（不携带 `dji.*` 类型）。 */
data class GeoPoint3D(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
)

/**
 * 拍照点位记录：照片 ↔ 点位 ↔ 类别 ↔ 时间关联（spec「拍照点位记录」）。
 *
 * @property complete 照片尚未成功落盘前为 `false`（标注不完整）。
 * @property cancelled 用户中途取消/离开任务时标记。
 */
data class CapturePoint(
    /** 点位标识：每次拍照递增，进程内唯一；用于把整组照片回绑到对应拍照。 */
    val id: Long = 0L,
    val point: GeoPoint3D,
    val kind: TargetKind,
    val targetIndex: Int,
    val capturedAt: Long,
    /** 组内首张照片（兼容既有地图/报告展示）。 */
    val asset: Asset? = null,
    /** 本次拍照的整组照片（广角/变焦/红外等）。 */
    val assets: List<Asset> = emptyList(),
    val complete: Boolean = false,
    val cancelled: Boolean = false,
)

/** 识别环节失败分类（spec「State Machine·failure category」）。 */
enum class RecognitionFailureCategory(val id: String) {
    RECOGNITION_UNAVAILABLE("recognition-unavailable"),
    CAPTURE_ERROR("capture-error"),
    ANCHOR_ERROR("anchor-error"),
    CANCELLED("cancelled"),
    NOT_READY("not-ready"),

    /** AR 投射：链路或类别配置未就绪，跳过本次投射（非阻塞）。 */
    AR_NOT_READY("ar-not-ready"),

    /** AR 投射：类别图标未推送成功，跳过本次投射（非阻塞）。 */
    AR_ICON_UNAVAILABLE("ar-icon-unavailable"),

    /** AR 投射：配置/点位上传失败（非阻塞）。 */
    AR_UPLOAD_FAILED("ar-upload-failed"),

    /** AR 投射：点位坐标无效，跳过（非阻塞）。 */
    AR_INVALID_LOCATION("ar-invalid-location"),

    /** AR 投射：任务结束清理单个点位失败（不中断收尾）。 */
    AR_CLEANUP_FAILED("ar-cleanup-failed"),
}

/** 携带失败分类的异常，供编排层按类别裁定恢复与否。 */
class CategorizedException(
    val category: RecognitionFailureCategory,
    message: String?,
) : IllegalStateException(message)

/** 按异常携带的分类裁定；未分类异常默认归为拍照错误。 */
fun classifyRecognitionError(error: Throwable): RecognitionFailureCategory =
    (error as? CategorizedException)?.category ?: RecognitionFailureCategory.CAPTURE_ERROR
