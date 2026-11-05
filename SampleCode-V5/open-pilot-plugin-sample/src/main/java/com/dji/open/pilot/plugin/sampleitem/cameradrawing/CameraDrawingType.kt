package com.dji.open.pilot.plugin.sampleitem.cameradrawing

import com.dji.open.pilot.plugin.R

/**
 * 图传绘制演示模块的元素类型枚举。
 * 每个类型对应悬浮窗中的一个 Tab 页和 Demo 页左侧菜单的一项。
 *
 * titleRes       = 菜单主标题
 * descRes        = 菜单副标题
 * displayNameRes = Pilot 悬浮窗顶部切换栏显示的名称（多语言资源 ID）
 * interfaceRef   = 该用例对应的开源接口类相对路径（源码位置提示）
 * sampleRef      = 本 Demo 实际绘制该用例的源码相对路径
 * previewResId   = 右侧效果预览图 drawable 资源 ID（0 表示暂无预览图，界面保持原样留空）
 */
enum class CameraDrawingType(
    val titleRes: Int,
    val descRes: Int,
    val displayNameRes: Int,
    val interfaceRef: String,
    val sampleRef: String,
    val previewResId: Int
) {
    BOX(
        R.string.cvd_box_title, R.string.cvd_box_desc, R.string.cvd_nav_box,
        "dji/open/pilot/client/interfaces/ICameraViewDrawingManager.java",
        "sampleitem/cameradrawing/drawing/BoxDrawer.kt",
        R.drawable.preview_cvd_box
    ),
    POLYGON(
        R.string.cvd_polygon_title, R.string.cvd_polygon_desc, R.string.cvd_nav_polygon,
        "dji/open/pilot/client/interfaces/ICameraViewDrawingManager.java",
        "sampleitem/cameradrawing/drawing/PolygonDrawer.kt",
        R.drawable.preview_cvd_polygon
    ),
    TEXT(
        R.string.cvd_text_title, R.string.cvd_text_desc, R.string.cvd_nav_text,
        "dji/open/pilot/client/interfaces/ICameraViewDrawingManager.java",
        "sampleitem/cameradrawing/drawing/TextDrawer.kt",
        R.drawable.preview_cvd_polygon
    );
}
