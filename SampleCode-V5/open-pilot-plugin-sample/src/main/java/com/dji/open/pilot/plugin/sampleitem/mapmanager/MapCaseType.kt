package com.dji.open.pilot.plugin.sampleitem.mapmanager

import com.dji.open.pilot.plugin.R

/**
 * 地图控件演示模块的用例类型枚举。
 * 每个类型对应悬浮窗中的一个 Tab 页和 Demo 页左侧菜单的一项。
 *
 * 当前可用菜单按能力域分为两组（见 MapMenuAdapter）：
 * - 地图控件：WIDGET / BATCH / LAYER —— 均操作 MapWidget（插件自绘、可交互、可分层）
 * - 地图元素：ELEMENT —— 操作 MapElement（Pilot 原生地图/AR 渲染）
 *
 * 地图状态监听（addMapStateListener/removeMapStateListener）不作为独立用例，
 * 进入 Demo 悬浮窗即常开监听，退出即注销，回调实时回显在每个用例结果卡片顶部。
 *
 * titleRes       = 菜单主标题
 * descRes        = 菜单副标题
 * displayNameRes = Pilot 悬浮窗顶部切换栏显示的名称（多语言资源 ID）
 * interfaceRef   = 该用例对应的开源接口类相对路径（源码位置提示）
 * sampleRef      = 本 Demo 实际绘制该用例的源码相对路径
 * previewResIds  = 右侧效果预览图 drawable 资源 ID 列表（多张时轮播，空列表表示暂无预览图）
 */
enum class MapCaseType(
    val titleRes: Int,
    val descRes: Int,
    val displayNameRes: Int,
    val interfaceRef: String,
    val sampleRef: String,
    val previewResIds: List<Int>
) {
    WIDGET(
        R.string.map_widget_title, R.string.map_widget_desc, R.string.map_nav_widget,
        "dji/open/pilot/client/interfaces/IMapWidgetManager.java",
        "sampleitem/mapmanager/drawing/MapWidgetDrawer.kt",
        listOf(
            R.drawable.preview_map_widget_1,
            R.drawable.preview_map_widget_2,
            R.drawable.preview_map_widget_3,
            R.drawable.preview_map_widget_4
        )
    ),
    BATCH(
        R.string.map_batch_title, R.string.map_batch_desc, R.string.map_nav_batch,
        "dji/open/pilot/client/interfaces/IMapWidgetManager.java",
        "sampleitem/mapmanager/drawing/MapBatchDrawer.kt",
        listOf(
            R.drawable.preview_map_batch_1,
            R.drawable.preview_map_batch_2
        )
    ),
    LAYER(
        R.string.map_layer_title, R.string.map_layer_desc, R.string.map_nav_layer,
        "dji/open/pilot/client/interfaces/IMapWidgetManager.java",
        "sampleitem/mapmanager/drawing/MapLayerDrawer.kt",
        listOf(
            R.drawable.preview_map_layer_1,
            R.drawable.preview_map_layer_2
        )
    ),
    ELEMENT(
        R.string.map_element_title, R.string.map_element_desc, R.string.map_nav_element,
        "dji/open/pilot/client/interfaces/IMapWidgetManager.java",
        "sampleitem/mapmanager/drawing/MapElementDrawer.kt",
        listOf(R.drawable.preview_map_element)
    ),
    ZOOM(
        R.string.map_zoom_title, R.string.map_zoom_desc, R.string.map_nav_zoom,
        "dji/open/pilot/client/interfaces/IMapWidgetManager.java",
        "sampleitem/mapmanager/drawing/MapZoomDrawer.kt",
        listOf(
            R.drawable.preview_map_zoom_1,
            R.drawable.preview_map_zoom_2
        )
    ),
    CENTER_MODE(
        R.string.map_center_mode_title, R.string.map_center_mode_desc, R.string.map_nav_center_mode,
        "dji/open/pilot/client/interfaces/IMapWidgetManager.java",
        "sampleitem/mapmanager/drawing/MapCenterModeDrawer.kt",
        listOf(R.drawable.preview_map_center)
    );

    companion object {
        /** Demo 菜单、悬浮窗绘制和前后切换共用的可用用例及顺序。 */
        val VISIBLE_CASES: List<MapCaseType> = listOf(WIDGET, BATCH, LAYER, ELEMENT)
    }
}
