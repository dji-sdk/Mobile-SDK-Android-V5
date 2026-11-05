package com.dji.open.pilot.plugin.sampleitem.navigatemanager

import com.dji.open.pilot.plugin.R

/**
 * 页面跳转演示模块的类型枚举。
 * 每个类型对应 Demo 页左侧菜单的一项，以及悬浮窗中的一个"跳转"按钮，
 * 覆盖 [dji.open.pilot.base.ui.NavigateAction.Value] 的全部有效跳转目标。
 *
 * titleRes     = 菜单主标题
 * descRes      = 菜单副标题（中文描述）
 * displayName  = 展示名称
 * interfaceRef = 该用例对应的开源接口类相对路径（源码位置提示）
 * sampleRef    = 本 Demo 实际调用该用例的源码相对路径
 * previewResId = 右侧效果预览图 drawable 资源 ID（8 种跳转目标复用同一张悬浮窗用例图）
 */
enum class NavigateType(
    val titleRes: Int,
    val descRes: Int,
    val displayName: String,
    val interfaceRef: String,
    val sampleRef: String,
    val previewResId: Int
) {
    PILOT_HOME(
        R.string.navigate_type_pilot_home_title,
        R.string.navigate_type_pilot_home_desc,
        "Pilot 主页",
        "dji/open/pilot/client/interfaces/INavigateManager.java",
        "sampleitem/navigatemanager/NavigateDemoViewModel.kt",
        R.drawable.preview_navigate
    ),
    PILOT_MAP(
        R.string.navigate_type_pilot_map_title,
        R.string.navigate_type_pilot_map_desc,
        "Pilot 地图页",
        "dji/open/pilot/client/interfaces/INavigateManager.java",
        "sampleitem/navigatemanager/NavigateDemoViewModel.kt",
        R.drawable.preview_navigate
    ),
    PILOT_WAYPOINT_MISSION_LIST(
        R.string.navigate_type_pilot_waypoint_title,
        R.string.navigate_type_pilot_waypoint_desc,
        "Pilot 航线展示页",
        "dji/open/pilot/client/interfaces/INavigateManager.java",
        "sampleitem/navigatemanager/NavigateDemoViewModel.kt",
        R.drawable.preview_navigate
    ),
    PILOT_ALBUM(
        R.string.navigate_type_pilot_album_title,
        R.string.navigate_type_pilot_album_desc,
        "Pilot 相册页",
        "dji/open/pilot/client/interfaces/INavigateManager.java",
        "sampleitem/navigatemanager/NavigateDemoViewModel.kt",
        R.drawable.preview_navigate
    ),
    PILOT_DEFAULT(
        R.string.navigate_type_pilot_default_title,
        R.string.navigate_type_pilot_default_desc,
        "Pilot 当前页",
        "dji/open/pilot/client/interfaces/INavigateManager.java",
        "sampleitem/navigatemanager/NavigateDemoViewModel.kt",
        R.drawable.preview_navigate
    ),
    PLUGIN_APP(
        R.string.navigate_type_plugin_app_title,
        R.string.navigate_type_plugin_app_desc,
        "插件 APP",
        "dji/open/pilot/client/interfaces/INavigateManager.java",
        "sampleitem/navigatemanager/NavigateDemoViewModel.kt",
        R.drawable.preview_navigate
    ),
    SELF_DEFINE(
        R.string.navigate_type_self_define_title,
        R.string.navigate_type_self_define_desc,
        "自定义插件页",
        "dji/open/pilot/client/interfaces/INavigateManager.java",
        "sampleitem/navigatemanager/NavigateDemoViewModel.kt",
        R.drawable.preview_navigate
    ),
    REMOTE_VIEW_PAGE_ACTIVITY(
        R.string.navigate_type_remote_view_title,
        R.string.navigate_type_remote_view_desc,
        "自定义远程页",
        "dji/open/pilot/client/interfaces/INavigateManager.java",
        "sampleitem/navigatemanager/NavigateDemoViewModel.kt",
        R.drawable.preview_navigate
    );
}
