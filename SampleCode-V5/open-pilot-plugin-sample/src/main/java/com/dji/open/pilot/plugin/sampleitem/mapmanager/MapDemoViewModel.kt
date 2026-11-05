package com.dji.open.pilot.plugin.sampleitem.mapmanager

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.NavigationHelper
import com.dji.open.pilot.plugin.common.data.SharedDataManager
import com.dji.open.pilot.plugin.common.drawing.DrawingContext
import com.dji.open.pilot.plugin.common.drawing.FloatingWindowOwner
import com.dji.open.pilot.plugin.common.drawing.NavBarHelper
import com.dji.open.pilot.plugin.main.PluginApplication
import com.dji.open.pilot.plugin.sampleitem.mapmanager.drawing.MapBatchDrawer
import com.dji.open.pilot.plugin.sampleitem.mapmanager.drawing.MapElementDrawer
import com.dji.open.pilot.plugin.sampleitem.mapmanager.drawing.MapLayerDrawer
import com.dji.open.pilot.plugin.sampleitem.mapmanager.drawing.MapWidgetDrawer
import dji.open.pilot.base.manager.OpenLocation
import dji.open.pilot.base.manager.map.MapCircleOptions
import dji.open.pilot.base.manager.map.MapDrawerType
import dji.open.pilot.base.manager.map.MapElement
import dji.open.pilot.base.manager.map.MapElementColor
import dji.open.pilot.base.manager.map.MapMarkerOptions
import dji.open.pilot.base.manager.map.MapPolygonOptions
import dji.open.pilot.base.manager.map.MapPolylineOptions
import dji.open.pilot.base.manager.map.MapState
import dji.open.pilot.base.manager.map.MapWidget
import dji.open.pilot.base.manager.map.MapWidgetItem
import dji.open.pilot.base.manager.map.MapWidgetItemType
import dji.open.pilot.base.manager.map.MapWidgetLayer
import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.base.ui.model.VisibilityType
import dji.open.pilot.client.manager.ui.IMapStateListener
import dji.open.pilot.client.manager.ui.MapWidgetManager
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.ux.remoteview.RemoteViewHelper
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 地图控件 Demo 调度层 ViewModel。
 *
 * 职责：
 * - 地图状态监听常开：悬浮窗绘制时 addMapStateListener，onCleared 时 removeMapStateListener
 * - 悬浮窗一次性绘制 NavBar + 可用用例容器（控件 / 批量 / 图层 / 元素）
 * - MapState 回调实时回显到每个用例结果卡片；操作结果回写结果卡片 + Toast
 */
class MapDemoViewModel : ViewModel() {

    companion object {
        private const val TAG = "MapDemoViewModel"
        private const val OWNER_TAG = "map_manager"

        // marker 图标（webp/png，服务端 BitmapFactory 解码，不支持 svg）
        private const val ICON_RC = "map_marker_rc.png"
        private const val ICON_DIRECTION = "map_marker_direction.png"
        private const val ICON_CENTER = "map_marker_center.png"

    }

    private val mainHandler = Handler(Looper.getMainLooper())

    val selectedType = MutableLiveData<MapCaseType?>(null)

    /** 各用例对应的结果 TextView viewId */
    private val resultTextIds = mutableMapOf<MapCaseType, String>()

    /** 各用例对应的容器 viewId（用于切换 VISIBILITY） */
    private val containerIds = mutableMapOf<MapCaseType, String>()

    // 容器可见性的唯一事实来源，仅在主线程读写；selectedType 只通知 Activity。
    // postValue 异步更新，Activity 菜单也会修改 selectedType，不能据此切换容器。
    private var currentVisibleType: MapCaseType? = null

    /** NavBar 标题组件 */
    private var navBarTitle: dji.v5.ux.remoteview.RemoteText? = null

    private val initialized = AtomicBoolean(false)
    private var drawingContext: DrawingContext? = null

    /** 最近一次 MapState 回调缓存，供回显使用 */
    @Volatile
    private var lastMapState: MapState = MapState()

    /** 地图状态监听器：常开，回调时缓存并刷新所有用例结果卡片 */
    private val mapStateListener = IMapStateListener { state ->
        lastMapState = state
        Log.i(TAG, "onUpdate: $state")
        refreshAllStateHeaders()
    }

    // ── 悬浮窗绘制 ──────────────────────────────────────────────────────────

    fun initAllLayouts() {
        if (!initialized.compareAndSet(false, true)) return

        drawingContext = null
        currentVisibleType = null
        navBarTitle = null
        containerIds.clear()
        resultTextIds.clear()
        val helper = RemoteViewHelper.floatingBoxHelper
        helper.clearLayout()
        helper.setPageLayoutParamWrapContent()
        val ctx = DrawingContext(helper)
        drawingContext = ctx

        // 根容器
        val root = helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        root.addToRoot(null)

        // NavBar
        val initialType = selectedType.value?.takeIf { it in MapCaseType.VISIBLE_CASES } ?: MapCaseType.WIDGET
        navBarTitle = NavBarHelper.draw(ctx, root, initialType.displayNameRes,
            onPrev = { if (isCurrentLayout(ctx)) switchToPrev() },
            onNext = { if (isCurrentLayout(ctx)) switchToNext() }
        )

        // 为每个用例绘制容器
        MapCaseType.VISIBLE_CASES.forEach { type ->
            val container = helper.createLinearLayoutVertical().apply {
                setWidth(RemoteView.MATCH_PARENT)
                setHeight(RemoteView.WRAP_CONTENT)
                setVisibility(if (type == initialType) VisibilityType.VISIBLE else VisibilityType.GONE)
            }
            root.addToRemote(container)
            containerIds[type] = container.getId()

            val resultId = when (type) {
                MapCaseType.WIDGET -> MapWidgetDrawer.draw(ctx, container,
                    onAddMarker = { addMarker() }, onRemoveMarker = { removeWidgetOfType(MapDrawerType.MARKER) },
                    onAddPolyline = { addPolyline() }, onRemovePolyline = { removeWidgetOfType(MapDrawerType.POLYLINE) },
                    onAddPolygon = { addPolygon() }, onRemovePolygon = { removeWidgetOfType(MapDrawerType.POLYGON) },
                    onAddCircle = { addCircle() }, onRemoveCircle = { removeWidgetOfType(MapDrawerType.CIRCLE) },
                    onQuery = { queryWidgets() })
                MapCaseType.BATCH -> MapBatchDrawer.draw(ctx, container,
                    onBatchAdd = { batchAdd() }, onBatchUpdate = { batchUpdate() }, onBatchRemove = { batchRemove() })
                MapCaseType.LAYER -> MapLayerDrawer.draw(ctx, container,
                    onTargetChanged = { layerTargetIndex = it },
                    onBuild = { buildLayers() }, onHide = { setLayerVisible(false) }, onShow = { setLayerVisible(true) },
                    onQuery = { queryLayer() }, onClear = { clearLayer() }, onRemove = { removeLayer() })
                MapCaseType.ELEMENT -> MapElementDrawer.draw(ctx, container,
                    onInsert = { insertElements() }, onQuery = { queryElements() }, onUpdate = { updateElements() },
                    onDelete = { deleteElements() }, onToggleAr = { toggleArAnnotations() })
                else -> error("Unsupported map case: $type")
            }
            resultTextIds[type] = resultId
        }

        // 返回按钮
        ctx.addBackButton(root) { NavigationHelper.navigateToPluginApp() }

        FloatingWindowOwner.claim(OWNER_TAG)
        currentVisibleType = initialType

        // 监听常开：注册后服务端会立即回推一次当前 MapState
        MapWidgetManager.getInstance().addMapStateListener(mapStateListener)

        selectedType.postValue(initialType)
        refreshAllStateHeaders()
    }

    fun drawAndNavigate() {
        if (!FloatingWindowOwner.isOwnedBy(OWNER_TAG)) {
            initialized.set(false)
        }
        initAllLayouts()
        NavigationHelper.navigateToPilotMap()
    }

    override fun onCleared() {
        super.onCleared()
        if (elementControllerDelegate.isInitialized()) elementController.close()
        MapWidgetManager.getInstance().removeMapStateListener(mapStateListener)
    }

    // ── 切换用例 ────────────────────────────────────────────────────────────

    // 在主线程实际执行点击时校验，拒绝排队中或重绘后迟到的旧箭头。
    private fun isCurrentLayout(ctx: DrawingContext): Boolean =
        drawingContext === ctx && FloatingWindowOwner.isOwnedBy(OWNER_TAG)

    private fun switchVisibility(newType: MapCaseType) {
        val helper = drawingContext?.helper ?: return
        val oldType = currentVisibleType ?: return
        if (oldType == newType) return
        navBarTitle?.apply {
            setTextByResId(newType.displayNameRes)
            syncToRemote()
        }
        // 先隐藏旧容器，再显示新容器，避免中间态同时可见。
        containerIds[oldType]?.let { id ->
            helper.updateView(id, RemoteViewProperty.VISIBILITY, "8") // GONE
        }
        containerIds[newType]?.let { id ->
            helper.updateView(id, RemoteViewProperty.VISIBILITY, "0") // VISIBLE
        }
        currentVisibleType = newType
        selectedType.postValue(newType)
    }

    private fun switchToPrev() {
        val current = currentVisibleType ?: return
        val values = MapCaseType.VISIBLE_CASES
        val currentIndex = values.indexOf(current)
        val prev = if (currentIndex < 0) MapCaseType.WIDGET
            else values[(currentIndex - 1 + values.size) % values.size]
        switchVisibility(prev)
    }

    private fun switchToNext() {
        val current = currentVisibleType ?: return
        val values = MapCaseType.VISIBLE_CASES
        val currentIndex = values.indexOf(current)
        val next = if (currentIndex < 0) MapCaseType.WIDGET
            else values[(currentIndex + 1) % values.size]
        switchVisibility(next)
    }

    // ── 用例：地图控件（MapWidget，单个增删查） ────────────────────────────────

    /** WIDGET 用例缓存：已添加的四类图元（供单独删除/查询） */
    private val widgetCache = mutableListOf<MapWidget>()

    private fun addMarker() {
        val a = aircraft() ?: return notReady(MapCaseType.WIDGET)
        if (hasWidgetType(MapDrawerType.MARKER)) return alreadyAdded(MapDrawerType.MARKER)
        // 中心（home 点近似遥控器/起飞点）+ 东西南北四方位
        val markers = mutableListOf<MapWidget>()
        val home = SharedDataManager.homeLocation.value
        val homeLoc = home?.let { OpenLocation(it.latitude, it.longitude, a.altitude) }
        markers.add(marker(homeLoc ?: OpenLocation(a.latitude, a.longitude, a.altitude),
            str(R.string.map_marker_rc), str(R.string.map_marker_rc_desc), ICON_RC, MapCaseType.WIDGET))
        val dirNames = intArrayOf(R.string.map_dir_north, R.string.map_dir_east, R.string.map_dir_south, R.string.map_dir_west)
        MapGeo.FOUR_DIRECTIONS.forEachIndexed { i, ang ->
            val loc = MapGeo.offset(a.latitude, a.longitude, a.altitude, MapGeo.WIDGET_RADIUS_M, ang)
            markers.add(marker(loc, str(dirNames[i]), str(R.string.map_marker_dir_desc),
                ICON_DIRECTION, MapCaseType.WIDGET, rotation = directionRotation(ang)))
        }
        markers.forEach { w -> addWidget(w, MapCaseType.WIDGET) }
    }

    private fun addPolyline() {
        val a = aircraft() ?: return notReady(MapCaseType.WIDGET)
        if (hasWidgetType(MapDrawerType.POLYLINE)) return alreadyAdded(MapDrawerType.POLYLINE)
        val tri = MapGeo.equilateralTriangle(a.latitude, a.longitude, a.altitude, MapGeo.WIDGET_RADIUS_M)
        val w = MapWidget().apply {
            drawerType = MapDrawerType.POLYLINE
            name = "demo_polyline"
            polylineOptions = MapPolylineOptions().apply {
                points = MapGeo.closed(tri).toMutableList()
                width = 8f
                color = MapGeo.STROKE_BLUE
                isClickable = false
            }
        }
        addWidget(w, MapCaseType.WIDGET)
    }

    private fun addPolygon() {
        val a = aircraft() ?: return notReady(MapCaseType.WIDGET)
        if (hasWidgetType(MapDrawerType.POLYGON)) return alreadyAdded(MapDrawerType.POLYGON)
        val tri = MapGeo.equilateralTriangle(a.latitude, a.longitude, a.altitude, MapGeo.WIDGET_RADIUS_M)
        val w = MapWidget().apply {
            drawerType = MapDrawerType.POLYGON
            name = "demo_polygon"
            polygonOptions = MapPolygonOptions().apply {
                points = tri.toMutableList()
                width = 8f
                strokeColor = MapGeo.STROKE_BLUE
                fillColor = MapGeo.FILL_BLUE
                isVisible = true
            }
        }
        addWidget(w, MapCaseType.WIDGET)
    }

    private fun addCircle() {
        val a = aircraft() ?: return notReady(MapCaseType.WIDGET)
        if (hasWidgetType(MapDrawerType.CIRCLE)) return alreadyAdded(MapDrawerType.CIRCLE)
        val w = MapWidget().apply {
            drawerType = MapDrawerType.CIRCLE
            name = "demo_circle"
            circleOptions = MapCircleOptions().apply {
                center = OpenLocation(a.latitude, a.longitude, a.altitude)
                radius = MapGeo.CIRCLE_RADIUS_M
                strokeWidth = 8f
                strokeColor = MapGeo.STROKE_BLUE
                fillColor = MapGeo.FILL_BLUE
            }
        }
        addWidget(w, MapCaseType.WIDGET)
    }

    private fun addWidget(w: MapWidget, tab: MapCaseType) {
        MapWidgetManager.getInstance().addMapWidget(w, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                widgetCache.add(w)
                report(tab, str(R.string.map_widget_add_success, w.name, w.drawerType.name))
            }
            override fun onFailure(error: IDJIError) {
                report(tab, str(R.string.map_op_failed, "addMapWidget", error.description()))
            }
        })
    }

    private fun removeWidgetOfType(type: MapDrawerType) {
        val targets = widgetCache.filter { it.drawerType == type }
        if (targets.isEmpty()) {
            report(MapCaseType.WIDGET, str(R.string.map_widget_none_to_remove, type.name))
            return
        }
        targets.forEach { w ->
            MapWidgetManager.getInstance().removeMapWidget(w, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    widgetCache.remove(w)
                    report(MapCaseType.WIDGET, str(R.string.map_widget_remove_success, type.name))
                }
                override fun onFailure(error: IDJIError) {
                    report(MapCaseType.WIDGET, str(R.string.map_op_failed, "removeMapWidget", error.description()))
                }
            })
        }
    }

    private fun queryWidgets() {
        MapWidgetManager.getInstance().queryMapWidgets(object :
            CommonCallbacks.CompletionCallbackWithParam<List<MapWidget>> {
            override fun onSuccess(list: List<MapWidget>?) {
                val widgets = list ?: emptyList()
                val detail = widgets.joinToString("\n") { "  ${it.drawerType.name} ${it.name}" }
                report(MapCaseType.WIDGET, str(R.string.map_widget_query_result, widgets.size) + "\n" + detail)
            }
            override fun onFailure(error: IDJIError) {
                report(MapCaseType.WIDGET, str(R.string.map_op_failed, "queryMapWidgets", error.description()))
            }
        })
    }

    // ── 用例：批量控件（MapWidget 批量） ───────────────────────────────────────

    /** BATCH 用例缓存：批量添加的全套图元 */
    private val batchCache = mutableListOf<MapWidget>()

    private fun batchAdd() {
        val a = aircraft() ?: return notReady(MapCaseType.BATCH)
        if (batchCache.isNotEmpty()) {
            report(MapCaseType.BATCH, str(R.string.map_batch_already_added))
            return
        }
        val widgets = buildFullSet(a, MapGeo.STROKE_BLUE, MapGeo.FILL_BLUE)
        MapWidgetManager.getInstance().addMapWidgets(widgets, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                batchCache.addAll(widgets)
                report(MapCaseType.BATCH, str(R.string.map_batch_add_success, widgets.size))
            }
            override fun onFailure(error: IDJIError) {
                report(MapCaseType.BATCH, str(R.string.map_op_failed, "addMapWidgets", error.description()))
            }
        })
    }

    private fun batchUpdate() {
        if (batchCache.isEmpty()) {
            report(MapCaseType.BATCH, str(R.string.map_batch_add_first))
            return
        }
        // marker 不支持改色：批量更新时把 marker 从服务端移除，只保留变绿的线/面/圆
        val markers = batchCache.filter { it.drawerType == MapDrawerType.MARKER }
        val shapes = batchCache.filter { it.drawerType != MapDrawerType.MARKER }
        // 就地把线/面/圆颜色改半透明绿 + 深绿描边
        shapes.forEach { w ->
            when (w.drawerType) {
                MapDrawerType.POLYLINE -> w.polylineOptions.color = MapGeo.STROKE_GREEN
                MapDrawerType.POLYGON -> {
                    w.polygonOptions.strokeColor = MapGeo.STROKE_GREEN
                    w.polygonOptions.fillColor = MapGeo.FILL_GREEN
                }
                MapDrawerType.CIRCLE -> {
                    w.circleOptions.strokeColor = MapGeo.STROKE_GREEN
                    w.circleOptions.fillColor = MapGeo.FILL_GREEN
                }
                else -> { /* no-op */ }
            }
        }

        val doUpdate = {
            MapWidgetManager.getInstance().updateMapWidgets(shapes, object :
                CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    report(MapCaseType.BATCH, str(R.string.map_batch_update_success))
                }
                override fun onFailure(error: IDJIError) {
                    report(MapCaseType.BATCH, str(R.string.map_op_failed, "updateMapWidgets", error.description()))
                }
            })
        }

        if (markers.isEmpty()) {
            doUpdate()
            return
        }
        // 先删 marker，成功后再更新剩余图元
        MapWidgetManager.getInstance().removeMapWidgetsByIds(markers.map { it.id }, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                batchCache.removeAll(markers)
                doUpdate()
            }
            override fun onFailure(error: IDJIError) {
                report(MapCaseType.BATCH, str(R.string.map_op_failed, "removeMapWidgetsByIds", error.description()))
            }
        })
    }

    private fun batchRemove() {
        if (batchCache.isEmpty()) {
            report(MapCaseType.BATCH, str(R.string.map_batch_add_first))
            return
        }
        val ids = batchCache.map { it.id }
        MapWidgetManager.getInstance().removeMapWidgetsByIds(ids, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                val count = batchCache.size
                batchCache.clear()
                report(MapCaseType.BATCH, str(R.string.map_batch_remove_success, count))
            }
            override fun onFailure(error: IDJIError) {
                report(MapCaseType.BATCH, str(R.string.map_op_failed, "removeMapWidgetsByIds", error.description()))
            }
        })
    }

    // ── 用例：图层管理（MapWidgetLayer） ───────────────────────────────────────

    /** 图层目标下拉框：0=三角形层 1=内切圆层 2=Marker层 3=全部 */
    @Volatile private var layerTargetIndex: Int = 0

    private var triangleLayer: MapWidgetLayer? = null
    private var circleLayer: MapWidgetLayer? = null
    private var markerLayer: MapWidgetLayer? = null

    private fun buildLayers() {
        val a = aircraft() ?: return notReady(MapCaseType.LAYER)
        if (triangleLayer != null) {
            report(MapCaseType.LAYER, str(R.string.map_layer_already_built))
            return
        }
        val tri = MapWidgetLayer(str(R.string.map_layer_triangle)).apply { zIndex = 10 }
        val cir = MapWidgetLayer(str(R.string.map_layer_circle)).apply { zIndex = 11 }
        val mk = MapWidgetLayer(str(R.string.map_layer_marker)).apply { zIndex = 12 }
        triangleLayer = tri; circleLayer = cir; markerLayer = mk

        // 依次建三层，全部成功后铺满四方位控件
        addLayerThen(tri) {
            addLayerThen(cir) {
                addLayerThen(mk) {
                    val widgets = buildFourDirectionGroups(a, tri.id, cir.id, mk.id)
                    MapWidgetManager.getInstance().addMapWidgets(widgets, object :
                        CommonCallbacks.CompletionCallback {
                        override fun onSuccess() {
                            report(MapCaseType.LAYER, str(R.string.map_layer_build_success, widgets.size))
                        }
                        override fun onFailure(error: IDJIError) {
                            report(MapCaseType.LAYER, str(R.string.map_op_failed, "addMapWidgets", error.description()))
                        }
                    })
                }
            }
        }
    }

    private fun addLayerThen(layer: MapWidgetLayer, onOk: () -> Unit) {
        MapWidgetManager.getInstance().addLayer(layer, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() { onOk() }
            override fun onFailure(error: IDJIError) {
                report(MapCaseType.LAYER, str(R.string.map_op_failed, "addLayer", error.description()))
            }
        })
    }

    /** 返回当前下拉框选中的图层 id 列表（全部时返回三层） */
    private fun targetLayerIds(): List<String> {
        val tri = triangleLayer?.id
        val cir = circleLayer?.id
        val mk = markerLayer?.id
        return when (layerTargetIndex) {
            0 -> listOfNotNull(tri)
            1 -> listOfNotNull(cir)
            2 -> listOfNotNull(mk)
            else -> listOfNotNull(tri, cir, mk)
        }
    }

    private fun setLayerVisible(visible: Boolean) {
        val ids = targetLayerIds()
        if (ids.isEmpty()) return report(MapCaseType.LAYER, str(R.string.map_layer_build_first))
        ids.forEach { id ->
            MapWidgetManager.getInstance().setLayerVisible(id, visible, object :
                CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    report(MapCaseType.LAYER, str(
                        if (visible) R.string.map_layer_show_success else R.string.map_layer_hide_success))
                }
                override fun onFailure(error: IDJIError) {
                    report(MapCaseType.LAYER, str(R.string.map_op_failed, "setLayerVisible", error.description()))
                }
            })
        }
    }

    private fun queryLayer() {
        val ids = targetLayerIds()
        if (ids.isEmpty()) return report(MapCaseType.LAYER, str(R.string.map_layer_build_first))
        ids.forEach { id ->
            MapWidgetManager.getInstance().queryMapWidgetsByLayer(id, object :
                CommonCallbacks.CompletionCallbackWithParam<List<MapWidget>> {
                override fun onSuccess(list: List<MapWidget>?) {
                    report(MapCaseType.LAYER, str(R.string.map_layer_query_result, id, (list ?: emptyList()).size))
                }
                override fun onFailure(error: IDJIError) {
                    report(MapCaseType.LAYER, str(R.string.map_op_failed, "queryMapWidgetsByLayer", error.description()))
                }
            })
        }
    }

    private fun clearLayer() {
        val ids = targetLayerIds()
        if (ids.isEmpty()) return report(MapCaseType.LAYER, str(R.string.map_layer_build_first))
        ids.forEach { id ->
            MapWidgetManager.getInstance().clearLayer(id, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() { report(MapCaseType.LAYER, str(R.string.map_layer_clear_success)) }
                override fun onFailure(error: IDJIError) {
                    report(MapCaseType.LAYER, str(R.string.map_op_failed, "clearLayer", error.description()))
                }
            })
        }
    }

    private fun removeLayer() {
        val ids = targetLayerIds()
        if (ids.isEmpty()) return report(MapCaseType.LAYER, str(R.string.map_layer_build_first))
        ids.forEach { id ->
            MapWidgetManager.getInstance().removeLayer(id, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    when (id) {
                        triangleLayer?.id -> triangleLayer = null
                        circleLayer?.id -> circleLayer = null
                        markerLayer?.id -> markerLayer = null
                    }
                    report(MapCaseType.LAYER, str(R.string.map_layer_remove_success))
                }
                override fun onFailure(error: IDJIError) {
                    report(MapCaseType.LAYER, str(R.string.map_op_failed, "removeLayer", error.description()))
                }
            })
        }
    }

    /** 四方位「等边三角形（底朝飞机）+ 内切圆 + Marker（圆心）」，分别归入三层 */
    private fun buildFourDirectionGroups(
        a: LocXY, triLayerId: String, circleLayerId: String, markerLayerId: String
    ): List<MapWidget> {
        val result = mutableListOf<MapWidget>()
        MapGeo.FOUR_DIRECTIONS.forEach { bearing ->
            val (verts, center) = MapGeo.triangleFacingAircraft(
                a.latitude, a.longitude, a.altitude, bearing, MapGeo.LAYER_DIST_M, MapGeo.LAYER_TRI_RADIUS_M)
            // 三角形（折线闭合）
            result.add(MapWidget().apply {
                drawerType = MapDrawerType.POLYGON
                name = "layer_tri"
                layerId = triLayerId
                polygonOptions = MapPolygonOptions().apply {
                    points = verts.toMutableList()
                    width = 6f
                    strokeColor = MapGeo.STROKE_BLUE
                    fillColor = MapGeo.FILL_BLUE
                    isVisible = true
                }
            })
            // 内切圆
            result.add(MapWidget().apply {
                drawerType = MapDrawerType.CIRCLE
                name = "layer_circle"
                layerId = circleLayerId
                circleOptions = MapCircleOptions().apply {
                    this.center = center
                    radius = MapGeo.incircleRadius(MapGeo.LAYER_TRI_RADIUS_M)
                    strokeWidth = 4f
                    strokeColor = MapGeo.STROKE_GREEN
                    fillColor = MapGeo.FILL_GREEN
                }
            })
            // Marker：四方位每个内切圆圆心各 1 个
            result.add(marker(center, str(R.string.map_marker_center), "", ICON_CENTER, MapCaseType.LAYER).apply {
                layerId = markerLayerId
            })
        }
        return result
    }

    // ── 用例：地图元素（MapElement，含 AR） ────────────────────────────────────

    private var lastElementResult = ""
    private val elementControllerDelegate = lazy {
        MapElementController(requireNotNull(PluginApplication.instance)) { message ->
            report(MapCaseType.ELEMENT, message)
        }
    }
    private val elementController by elementControllerDelegate

    private fun insertElements() = elementController.insert { buildElements() }

    private fun buildElements(): List<MapElement>? {
        val a = aircraft() ?: return null
        val alt = a.altitude + MapGeo.ELEMENT_ALT_OFFSET_M
        val tri = MapGeo.equilateralTriangle(a.latitude, a.longitude, alt, MapGeo.WIDGET_RADIUS_M)

        val polyline = MapElement().apply {
            drawerType = MapDrawerType.POLYLINE
            points = MapGeo.closed(tri).toMutableList()
            color = MapElementColor.BLUE
            name = "element_polyline"
        }
        val polygon = MapElement().apply {
            drawerType = MapDrawerType.POLYGON
            points = tri.toMutableList()
            color = MapElementColor.BLUE
            name = "element_polygon"
        }
        val pin = MapElement().apply {
            drawerType = MapDrawerType.PIN
            points = mutableListOf(OpenLocation(a.latitude, a.longitude, alt))
            name = "element_pin"
        }
        return listOf(polyline, polygon, pin)
    }

    private fun queryElements() = elementController.query()
    private fun updateElements() = elementController.update()
    private fun deleteElements() = elementController.delete()
    private fun toggleArAnnotations() = elementController.toggleAr()

    // ── 图元构造 / 通用工具 ────────────────────────────────────────────────────

    /** 简化的飞机位置载体（经纬度 + 绝对高度） */
    private data class LocXY(val latitude: Double, val longitude: Double, val altitude: Double)

    /** 取飞机当前位置；取不到返回 null */
    private fun aircraft(): LocXY? {
        val loc = SharedDataManager.getAircraftLocationOnce() ?: return null
        return LocXY(loc.latitude, loc.longitude, loc.altitude)
    }

    private fun marker(
        loc: OpenLocation, title: String, desc: String,
        icon: String = ICON_DIRECTION, tab: MapCaseType = MapCaseType.WIDGET,
        rotation: Float = 0f
    ): MapWidget =
        MapWidget().apply {
            drawerType = MapDrawerType.MARKER
            name = title
            markerOptions = MapMarkerOptions().apply {
                this.title = title
                description = desc
                point = loc
                this.rotation = rotation
                normalIConFileName = icon
                // 未提供独立点击态图标，点击不换图，仅在结果卡片提示
                clickedIConFileName = icon
                isClickable = true
                isVisible = true
            }
            associatedItem = MapWidgetItem().apply {
                itemType = MapWidgetItemType.DIALOG
                this.title = title
                content = str(R.string.map_marker_click_content)
                btnContent = str(R.string.map_marker_click_btn)
                setOnClickListener { report(tab, str(R.string.map_marker_clicked_toast, title)) }
            }
        }

    /** 构造以飞机为中心的全套图元（Marker + 线 + 面 + 圆） */
    private fun buildFullSet(a: LocXY, stroke: Int, fill: Int): List<MapWidget> {
        val list = mutableListOf<MapWidget>()
        // 东西南北四方位 Marker
        MapGeo.FOUR_DIRECTIONS.forEach { ang ->
            val loc = MapGeo.offset(a.latitude, a.longitude, a.altitude, MapGeo.WIDGET_RADIUS_M + 10, ang)
            list.add(marker(loc, "batch_marker", "", ICON_DIRECTION, MapCaseType.BATCH,
                rotation = directionRotation(ang)))
        }
        val tri = MapGeo.equilateralTriangle(a.latitude, a.longitude, a.altitude, MapGeo.WIDGET_RADIUS_M)
        list.add(MapWidget().apply {
            drawerType = MapDrawerType.POLYLINE
            name = "batch_polyline"
            polylineOptions = MapPolylineOptions().apply {
                points = MapGeo.closed(tri).toMutableList(); width = 8f; color = stroke
            }
        })
        list.add(MapWidget().apply {
            drawerType = MapDrawerType.POLYGON
            name = "batch_polygon"
            polygonOptions = MapPolygonOptions().apply {
                points = tri.toMutableList(); width = 8f; strokeColor = stroke; fillColor = fill; isVisible = true
            }
        })
        list.add(MapWidget().apply {
            drawerType = MapDrawerType.CIRCLE
            name = "batch_circle"
            circleOptions = MapCircleOptions().apply {
                center = OpenLocation(a.latitude, a.longitude, a.altitude)
                radius = MapGeo.CIRCLE_RADIUS_M; strokeWidth = 8f; strokeColor = stroke; fillColor = fill
            }
        })
        return list
    }

    private fun notReady(tab: MapCaseType) {
        report(tab, str(R.string.map_no_location))
    }

    /** WIDGET 缓存中是否已存在该类型图元（防重复添加） */
    private fun hasWidgetType(type: MapDrawerType): Boolean =
        widgetCache.any { it.drawerType == type }

    private fun alreadyAdded(type: MapDrawerType) {
        report(MapCaseType.WIDGET, str(R.string.map_widget_already_added, type.name))
    }

    /**
     * 方位图标默认朝向为「正东」，因此显示旋转角 = 方位角(度) − 90。
     * angleRad: 0=北, 顺时针。
     */
    private fun directionRotation(angleRad: Double): Float =
        (Math.toDegrees(angleRad) - 90.0).toFloat()

    /** 操作结果回显：结果卡片（保留状态头 + 追加结果行）+ Toast */
    private fun report(tab: MapCaseType, msg: String) {
        if (tab == MapCaseType.ELEMENT) lastElementResult = msg
        val header = buildStateHeader(lastMapState)
        updateResult(tab, "$header\n$msg")
        showToast(msg)
    }

    // ── 结果 / 状态回显 ───────────────────────────────────────────────────────

    /** 刷新所有用例结果卡片顶部的"当前地图状态"信息 */
    private fun refreshAllStateHeaders() {
        mainHandler.post {
            val header = buildStateHeader(lastMapState)
            MapCaseType.values().forEach { type ->
                val body = if (type == MapCaseType.ELEMENT) lastElementResult else ""
                updateResult(type, if (body.isEmpty()) header else "$header\n$body")
            }
        }
    }

    private fun buildStateHeader(state: MapState): String {
        return str(
            R.string.map_state_header,
            state.isMapReady.toString(),
            state.zoom
        )
    }

    private fun updateResult(type: MapCaseType, text: String) {
        val helper = drawingContext?.helper ?: return
        val id = resultTextIds[type] ?: return
        helper.updateView(id, RemoteViewProperty.TEXT, text)
    }

    // ── 工具方法 ─────────────────────────────────────────────────────────────

    private fun str(@StringRes resId: Int, vararg args: Any): String {
        return PluginApplication.instance?.getString(resId, *args) ?: ""
    }

    private fun showToast(msg: String) {
        mainHandler.post {
            val ctx = PluginApplication.instance
            if (ctx != null) {
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
