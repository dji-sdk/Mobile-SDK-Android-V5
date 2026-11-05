package dji.sample.patrol.data

/**
 * 航线进度的展示意图。
 *
 * 设计意图：把「进度事实」与「用户文案」分离，UI 层只负责把本类型映射到中英文资源；
 * 纯数据、不含 Android 依赖，便于 JVM 单测覆盖各阶段与边界。
 */
sealed interface RouteProgressDisplay {

    /** 无进度可展示，界面应隐藏对应字段。 */
    data object Hidden : RouteProgressDisplay

    /** 航线准备中。 */
    data object Preparing : RouteProgressDisplay

    /** 航线下载中；[percent] 已夹取到 0–100。 */
    data class Downloading(val percent: Int) : RouteProgressDisplay

    /** 航线上传/导入中；[percent] 已夹取到 0–100。 */
    data class Uploading(val percent: Int) : RouteProgressDisplay

    /**
     * 航线执行中。
     *
     * [waylineIndex] 仅在多航段（[waylineCount] > 1）且下标有效时非空；
     * 单航段不展示航段层级，避免「第 1/1 条航段」这类无信息量文案。
     */
    data class Executing(
        val waylineIndex: Int?,
        val waylineCount: Int,
        val current: Int,
        val total: Int,
    ) : RouteProgressDisplay

    /** 航线中断；[code] 可能为空。 */
    data class Interrupted(val code: String?) : RouteProgressDisplay
}

/**
 * 把结构化进度映射为展示意图（纯函数）。
 *
 * 能力契约：未知/空进度返回 [RouteProgressDisplay.Hidden]；百分比夹取到 0–100；
 * 中断码空白视为无码。
 */
internal fun routeProgressDisplay(progress: WaylineProgress): RouteProgressDisplay =
    when (progress.stage) {
        RouteProgressStage.IDLE -> RouteProgressDisplay.Hidden
        RouteProgressStage.PREPARING -> RouteProgressDisplay.Preparing
        RouteProgressStage.DOWNLOADING ->
            RouteProgressDisplay.Downloading(progress.percent.coerceIn(0, 100))
        RouteProgressStage.UPLOADING ->
            RouteProgressDisplay.Uploading(progress.percent.coerceIn(0, 100))
        RouteProgressStage.EXECUTING -> RouteProgressDisplay.Executing(
            waylineIndex = progress.waylineIndex.takeIf {
                progress.waylineCount > 1 && it in 1..progress.waylineCount
            },
            waylineCount = progress.waylineCount,
            current = progress.currentWaypoint,
            total = progress.totalWaypoints,
        )
        RouteProgressStage.INTERRUPTED ->
            RouteProgressDisplay.Interrupted(progress.interruptCode?.takeIf { it.isNotBlank() })
    }
