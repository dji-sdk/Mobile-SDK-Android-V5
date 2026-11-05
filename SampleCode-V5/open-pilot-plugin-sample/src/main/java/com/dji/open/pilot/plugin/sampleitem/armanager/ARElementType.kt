package com.dji.open.pilot.plugin.sampleitem.armanager

import com.dji.open.pilot.plugin.R

/**
 * AR 演示模块的元素类型枚举。
 * 每个类型对应悬浮窗中的一个 Tab 页和 Demo 页左侧菜单的一项。
 *
 * titleRes     = 菜单主标题
 * descRes      = 菜单副标题
 * displayNameRes = Pilot 悬浮窗顶部切换栏显示的名称（多语言资源 ID）
 * interfaceRef = 该用例对应的开源接口类相对路径（源码位置提示，统一展示在"查看演示效果按钮"处）
 * sampleRef    = 本 Demo 实际绘制该用例的源码相对路径
 * previewResId = 右侧效果预览图 drawable 资源 ID（0 表示暂无预览图，界面保持原样留空）
 */
enum class ARElementType(
    val titleRes: Int,
    val descRes: Int,
    val displayNameRes: Int,
    val interfaceRef: String,
    val sampleRef: String,
    val previewResId: Int
) {
    POINT(
        R.string.ar_point_title, R.string.ar_point_desc, R.string.ar_nav_point,
        "dji/open/pilot/client/interfaces/IARManager.java",
        "sampleitem/armanager/drawing/ARPointDrawer.kt",
        R.drawable.preview_ar_point
    ),
    LINE(
        R.string.ar_line_title, R.string.ar_line_desc, R.string.ar_nav_line,
        "dji/open/pilot/client/interfaces/IARManager.java",
        "sampleitem/armanager/drawing/ARLineDrawer.kt",
        R.drawable.preview_ar_line
    ),
    POLYGON(
        R.string.ar_polygon_title, R.string.ar_polygon_desc, R.string.ar_nav_polygon,
        "dji/open/pilot/client/interfaces/IARManager.java",
        "sampleitem/armanager/drawing/ARPolygonDrawer.kt",
        R.drawable.preview_ar_polygon
    ),
    CIRCLE(
        R.string.ar_circle_title, R.string.ar_circle_desc, R.string.ar_nav_circle,
        "dji/open/pilot/client/interfaces/IARManager.java",
        "sampleitem/armanager/drawing/ARCircleDrawer.kt",
        R.drawable.preview_ar_circle
    ),
    PROJECTION(
    R.string.ar_projection_title, R.string.ar_projection_desc, R.string.ar_nav_projection,
    "dji/open/pilot/client/interfaces/IARManager.java",
    "sampleitem/armanager/drawing/ARProjectionDrawer.kt",
    0
    ),
    RADAR(
        R.string.ar_radar_title, R.string.ar_radar_desc, R.string.ar_nav_radar,
        "dji/open/pilot/client/interfaces/IARManager.java",
        "sampleitem/armanager/drawing/ARRadarDrawer.kt",
        0
    );
}
