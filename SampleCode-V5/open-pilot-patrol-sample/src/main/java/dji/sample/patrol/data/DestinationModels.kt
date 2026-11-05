package dji.sample.patrol.data

/**
 * 目的地点位（工程自有类型，不携带 `dji.*`）。
 *
 * 设计意图：承载飞手选定的目的地点位坐标，供地图标注、AR 投射与 FlyTo 复用；与 [GeoPoint3D]
 * 语义一致但表达「目的地」业务含义。
 *
 * @property latitude WGS-84 纬度。
 * @property longitude WGS-84 经度。
 * @property altitude 椭球高（HAE，米）。
 */
data class DestinationPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
) {
    /** 坐标有效（经纬在合法范围）。 */
    val isValid: Boolean get() = latitude in -90.0..90.0 && longitude in -180.0..180.0
}

/**
 * 目的地/FlyTo 编排态（State Machine·stage）。
 *
 * 设计意图：把「标注目的地 → 二次确认 → FlyTo 飞行 → 到达/取消」的状态机与 UI 解耦，UI 只读订阅。
 */
enum class FlyToStage {
    /** 无目的地。 */
    IDLE,

    /** 已选定目的地并标注，等待飞手二次确认。 */
    MARKED,

    /** 已确认，FlyTo 命令已下发，等待权威状态。 */
    REQUESTED,

    /** `FlyToMissionState.MARCHON`：飞行中。 */
    FLYING,

    /** `FINISHED`：已到达。 */
    ARRIVED,

    /** `INTERRUPTED` 或用户 `stopMission()`。 */
    CANCELLED,

    /** 命令失败/前置不满足/机型不支持。 */
    FAILED,
}

/** 目的地/FlyTo 失败分类（State Machine·failure category）。 */
enum class DestinationFailureCategory(val id: String) {
    FLYTO_ERROR("flyto-error"),
    NOT_READY("not-ready"),
    MAP_ERROR("map-error"),
    AR_ERROR("ar-error"),
    CANCELLED("cancelled"),
}

/** FlyTo 模式（工程枚举，映射 SDK `FlyToMode`）。 */
enum class FlyToModeKind {
    SET_HEIGHT,
    SMART_HEIGHT,
}
