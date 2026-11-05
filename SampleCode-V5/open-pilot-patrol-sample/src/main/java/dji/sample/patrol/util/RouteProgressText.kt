package dji.sample.patrol.util

import android.content.Context
import dji.sample.patrol.R
import dji.sample.patrol.data.RouteProgressDisplay
import dji.sample.patrol.data.WaylineProgress
import dji.sample.patrol.data.routeProgressDisplay

/**
 * 把航线进度映射为本地化文案。
 *
 * 设计意图：任务页与浮窗加载提示共用同一套映射，保证界面只出现用户可读的阶段/航段/航点文案，
 * 不输出内部调试串。返回 `null` 表示无进度可展示，调用方应隐藏该字段或回退到加载提示。
 */
internal fun routeProgressText(context: Context, progress: WaylineProgress): String? =
    when (val display = routeProgressDisplay(progress)) {
        RouteProgressDisplay.Hidden -> null
        RouteProgressDisplay.Preparing ->
            context.getString(R.string.fw_route_stage_preparing)
        is RouteProgressDisplay.Downloading ->
            context.getString(R.string.fw_route_stage_downloading, display.percent)
        is RouteProgressDisplay.Uploading ->
            context.getString(R.string.fw_route_stage_uploading, display.percent)
        is RouteProgressDisplay.Executing -> when {
            display.waylineIndex != null && display.total > 0 -> context.getString(
                R.string.fw_route_stage_executing_wayline,
                display.waylineIndex,
                display.waylineCount,
                display.current,
                display.total,
            )
            display.waylineIndex != null -> context.getString(
                R.string.fw_route_stage_executing_wayline_only,
                display.waylineIndex,
                display.waylineCount,
            )
            display.total > 0 -> context.getString(
                R.string.fw_route_stage_executing_waypoint,
                display.current,
                display.total,
            )
            else -> context.getString(R.string.fw_route_stage_executing)
        }
        is RouteProgressDisplay.Interrupted ->
            display.code?.let { context.getString(R.string.fw_route_stage_interrupted, it) }
                ?: context.getString(R.string.fw_route_stage_interrupted_no_code)
    }
