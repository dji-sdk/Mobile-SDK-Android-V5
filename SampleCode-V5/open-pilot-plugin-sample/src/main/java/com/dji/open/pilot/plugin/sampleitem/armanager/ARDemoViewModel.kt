package com.dji.open.pilot.plugin.sampleitem.armanager

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.ColorInt
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
import com.dji.open.pilot.plugin.sampleitem.armanager.drawing.ARCircleDrawer
import com.dji.open.pilot.plugin.sampleitem.armanager.drawing.ARLineDrawer
import com.dji.open.pilot.plugin.sampleitem.armanager.drawing.ARPointDrawer
import com.dji.open.pilot.plugin.sampleitem.armanager.drawing.ARPolygonDrawer
import com.dji.open.pilot.plugin.sampleitem.armanager.drawing.ARProjectionDrawer
import com.dji.open.pilot.plugin.sampleitem.armanager.drawing.ARRadarDrawer
import dji.open.pilot.base.manager.OpenLocation
import dji.open.pilot.base.manager.ar.ARCircleInfo
import dji.open.pilot.base.manager.ar.ARDistAlpha
import dji.open.pilot.base.manager.ar.ARDistLineWidth
import dji.open.pilot.base.manager.ar.ARFaceStroke
import dji.open.pilot.base.manager.ar.ARFontGravity
import dji.open.pilot.base.manager.ar.ARIconOrthographicConfig
import dji.open.pilot.base.manager.ar.ARIconPerspectiveConfig
import dji.open.pilot.base.manager.ar.ARIconProjectionType
import dji.open.pilot.base.manager.ar.ARLineInfo
import dji.open.pilot.base.manager.ar.ARPointConfig
import dji.open.pilot.base.manager.ar.ARPointInfo
import dji.open.pilot.base.manager.ar.ARPolygonFace
import dji.open.pilot.base.manager.ar.ARPolygonInfo
import dji.open.pilot.base.manager.ar.ARRadarConfig
import dji.open.pilot.base.manager.ar.ARStrokeInfo
import dji.open.pilot.base.manager.ar.ARTextConfig
import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.base.ui.model.VisibilityType
import dji.open.pilot.client.manager.ui.ARManager
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.ux.remoteview.RemoteLinearLayout
import dji.v5.ux.remoteview.RemoteViewHelper
import dji.v5.ux.util.RemoteViewColors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AR Demo 调度层 ViewModel。
 *
 * 职责：
 * - 管理 ARPointConfig 的自动上传与 configId 缓存
 * - 悬浮窗一次性全量绘制 + 切换可见性
 * - CRUD 操作分派到 ARManager
 * - 操作结果回写悬浮窗 TextView + Toast
 */
class ARDemoViewModel : ViewModel() {

    companion object {
        private const val TAG = "ARDemoViewModel"
        private const val OWNER_TAG = "ar_manager"
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    val selectedType = MutableLiveData<ARElementType?>(null)

    /** 缓存的 ARPointConfig ID：40m 正交 */
    private var configId40: String? = null
    /** 缓存的 ARPointConfig ID：80m 透视 */
    private var configId80: String? = null
    /** 缓存上传的 6 个点的 ARPointInfo（含 id），用于更新/删除 */
    private val uploadedPoints40 = mutableListOf<ARPointInfo>()
    private val uploadedPoints80 = mutableListOf<ARPointInfo>()

    /** 各 Tab 对应的结果 TextView viewId */
    private val resultTextIds = mutableMapOf<ARElementType, String>()

    /** 各 Tab 对应的容器 viewId（用于切换 VISIBILITY） */
    private val containerIds = mutableMapOf<ARElementType, String>()

    // 容器可见性的唯一事实来源，仅在主线程读写；selectedType 只通知 Activity。
    // postValue 异步更新，Activity 菜单也会修改 selectedType，不能据此切换容器。
    private var currentVisibleType: ARElementType? = null

    /** NavBar 标题组件（持引用，供 setTextByResId 切换，走 resIdMap 支持语言刷新） */
    private var navBarTitle: dji.v5.ux.remoteview.RemoteText? = null

    private val initialized = AtomicBoolean(false)
    private var drawingContext: DrawingContext? = null

    // ── 初始化：上传两组 ARPointConfig ─────────────────────────────────────────

    fun uploadDefaultConfig() {
        // 40m 正交配置
        val config40 = ARPointConfig.Builder.builder()
            .withName("demo_config_40m_orthographic")
            .withUseText(true)
            .withArProjectionType(ARIconProjectionType.ORTHOGRAPHIC)
            .withIconOrthographicConfig(
                ARIconOrthographicConfig.Builder.builder()
                    .withSize(40f)
                    .withIconFileName("ar_cable_highlight_dot.svg")
                    .build()
            )
            .withTextConfig(
                ARTextConfig.Builder.builder()
                    .withTextSize(15f)
                    .withFontGravity(ARFontGravity.TOP)
                    .withBackgroundColor(0x55141414.toInt())
                    .build()
            )
            .build()

        ARManager.getInstance().uploadARPointConfig(config40, object :
            CommonCallbacks.CompletionCallbackWithParam<String> {
            override fun onSuccess(id: String?) {
                configId40 = id
                Log.i(TAG, "Config40 uploaded, configId=$id")
            }
            override fun onFailure(error: IDJIError) {
                Log.e(TAG, "Upload Config40 failed: ${error.description()}")
            }
        })

        // 80m 透视配置
        val config80 = ARPointConfig.Builder.builder()
            .withName("demo_config_80m_perspective")
            .withUseText(true)
            .withArProjectionType(ARIconProjectionType.PERSPECTIVE)
            .withIconPerspectiveConfig(
                ARIconPerspectiveConfig.Builder.builder()
                    .withRealSize(2f)
                    .withMinSize(40f)
                    .withMaxSize(80f)
                    .withStrokeWidthRatio(0.8f)
                    .withMaxIconFileName("ar_point_star_max_icon.svg")
                    .withMinIconFileName("ar_point_star_min_icon.svg")
                    .build()
            )
            .withTextConfig(
                ARTextConfig.Builder.builder()
                    .withTextSize(15f)
                    .withFontGravity(ARFontGravity.BOTTOM)
                    .withShadow(false)
                    .withBackgroundColor(0x55141414.toInt())
                    .build()
            )
            .build()

        ARManager.getInstance().uploadARPointConfig(config80, object :
            CommonCallbacks.CompletionCallbackWithParam<String> {
            override fun onSuccess(id: String?) {
                configId80 = id
                Log.i(TAG, "Config80 uploaded, configId=$id")
            }
            override fun onFailure(error: IDJIError) {
                Log.e(TAG, "Upload Config80 failed: ${error.description()}")
            }
        })
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
        val initialType = ARElementType.POINT
        navBarTitle = NavBarHelper.draw(ctx, root, initialType.displayNameRes,
            onPrev = { if (isCurrentLayout(ctx)) switchToPrev() },
            onNext = { if (isCurrentLayout(ctx)) switchToNext() }
        )

        // 为每个 Tab 绘制容器
        ARElementType.values().forEach { type ->
            val container = helper.createLinearLayoutVertical().apply {
                setWidth(RemoteView.MATCH_PARENT)
                setHeight(RemoteView.WRAP_CONTENT)
                setVisibility(if (type == initialType) VisibilityType.VISIBLE else VisibilityType.GONE)
            }
            root.addToRemote(container)
            containerIds[type] = container.getId()

            val resultId = when (type) {
                ARElementType.POINT -> ARPointDrawer.draw(ctx, container,
                    onQuery = { queryPoint() }, onUpload = { uploadPoint() },
                    onUpdate = { updatePoint() }, onDelete = { deletePoint() })
                ARElementType.LINE -> ARLineDrawer.draw(ctx, container,
                    onQuery = { queryLine() }, onUpload = { uploadLine() },
                    onUpdate = { updateLine() }, onDelete = { deleteLine() })
                ARElementType.POLYGON -> ARPolygonDrawer.draw(ctx, container,
                    onQuery = { queryPolygon() }, onUpload = { uploadPolygon() },
                    onUpdate = { updatePolygon() }, onDelete = { deletePolygon() })
                ARElementType.CIRCLE -> ARCircleDrawer.draw(ctx, container,
                    onQuery = { queryCircle() }, onUpload = { uploadCircle() },
                    onUpdate = { updateCircle() }, onDelete = { deleteCircle() })
                ARElementType.RADAR -> ARRadarDrawer.draw(ctx, container,
                    onEnable = { enableRadar() }, onDisable = { disableRadar() })
                ARElementType.PROJECTION -> ARProjectionDrawer.draw(ctx, container)
            }
            resultTextIds[type] = resultId
        }

        // 返回按钮
        ctx.addBackButton(root) { NavigationHelper.navigateToPluginApp() }

        FloatingWindowOwner.claim(OWNER_TAG)
        currentVisibleType = initialType
        selectedType.postValue(initialType)
    }

    fun drawAndNavigate() {
        if (!FloatingWindowOwner.isOwnedBy(OWNER_TAG)) {
            initialized.set(false)
        }
        initAllLayouts()
        NavigationHelper.navigateToPilot()
    }

    // ── 切换 Tab ────────────────────────────────────────────────────────────

    // 在主线程实际执行点击时校验，拒绝排队中或重绘后迟到的旧箭头。
    private fun isCurrentLayout(ctx: DrawingContext): Boolean =
        drawingContext === ctx && FloatingWindowOwner.isOwnedBy(OWNER_TAG)

    private fun switchVisibility(newType: ARElementType) {
        val helper = drawingContext?.helper ?: return
        val oldType = currentVisibleType ?: return
        if (oldType == newType) return
        // 更新 NavBar 标题（setTextByResId 进 resIdMap，支持语言切换刷新）
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
        val values = ARElementType.values()
        val prev = values[(current.ordinal - 1 + values.size) % values.size]
        switchVisibility(prev)
    }

    private fun switchToNext() {
        val current = currentVisibleType ?: return
        val values = ARElementType.values()
        val next = values[(current.ordinal + 1) % values.size]
        switchVisibility(next)
    }

    // ── CRUD 操作：点 ────────────────────────────────────────────────────────

    private fun queryPoint() {
        ARManager.getInstance().queryARPointInfo(object :
            CommonCallbacks.CompletionCallbackWithParam<List<ARPointInfo>> {
             override fun onSuccess(list: List<ARPointInfo>?) {
                val points = list ?: emptyList()
                val msg = str(R.string.ar_query_found, points.size, str(R.string.ar_point_title)) + "\n" +
                    points.joinToString("\n") { "  id=${it.id}, name=${it.name}" }
                updateResult(ARElementType.POINT, msg)
                showToast(str(R.string.ar_query_found, points.size, str(R.string.ar_point_title)))
            }

            override fun onFailure(error: IDJIError) {
                val msg = str(R.string.ar_query_failed, str(R.string.ar_point_title), error.description())
                updateResult(ARElementType.POINT, msg)
                showToast(msg)
            }
        })
    }

    private fun uploadPoint() {
        if (uploadedPoints40.isNotEmpty() || uploadedPoints80.isNotEmpty()) {
            updateResult(ARElementType.POINT, str(R.string.ar_already_uploaded, str(R.string.ar_point_title)))
            return
        }

        val cid40 = configId40
        val cid80 = configId80
        if (cid40 == null || cid80 == null) {
            updateResult(ARElementType.POINT, str(R.string.ar_config_not_ready))
            return
        }

        // 获取飞机正前方 40m 的中心经纬度
        val center = getForwardCenter()
        if (center == null) {
            updateResult(ARElementType.POINT, str(R.string.ar_location_failed_check))
            return
        }
        val baseLat = center.first
        val baseLng = center.second
        // 高度基准：起飞点海拔（绝对高度），元素固定在起飞点上方 40~80m，不随飞机当前高度漂移
        val baseAlt = getTakeoffAltitude()

        // 等边三角形顶点计算（中心到顶点距离 40m）
        val radiusM = 20.0
        val angles = doubleArrayOf(0.0, 120.0, 240.0)

        uploadedPoints40.clear()
        uploadedPoints80.clear()
        var successCount = 0
        val totalCount = 6

        // 上传 40m 高度的 3 个点（正交配置）
        for (angle in angles) {
            val rad = Math.toRadians(angle)
            val loc = offsetLocation(baseLat, baseLng, baseAlt+40.0, radiusM, rad)
            val info = ARPointInfo.Builder.builder()
                .withConfigId(cid40)
                .withName("point_40m_${angle.toInt()}")
                .withLocation(loc)
                .withText("${String.format("%.4f", loc.latitude)}, ${String.format("%.4f", loc.longitude)}, 40m")
                .withTextColor(0xFF2696FF.toInt())
                .withIconColor(0xFF2696FF.toInt())
                .withVisible(true)
                .withIconVisible(true)
                .withTextVisible(true)
                .build()

            ARManager.getInstance().uploadARPointInfo(info, object :
                CommonCallbacks.CompletionCallbackWithParam<String> {
                override fun onSuccess(id: String?) {
                    info.setName(info.name) // keep name for reference
                    uploadedPoints40.add(info)
                    successCount++
                     if (successCount == totalCount) {
                        updateResult(ARElementType.POINT, str(R.string.ar_point_upload_done))
                        showToast(str(R.string.ar_point_upload_toast))
                    }
                }
                override fun onFailure(error: IDJIError) {
                    updateResult(ARElementType.POINT, str(R.string.ar_point_upload_failed, error.description()))
                }
            })
        }

        // 上传 80m 高度的 3 个点（透视配置）
        for (angle in angles) {
            val rad = Math.toRadians(angle)
            val loc = offsetLocation(baseLat, baseLng, baseAlt+80.0, radiusM, rad)
            val info = ARPointInfo.Builder.builder()
                .withConfigId(cid80)
                .withName("point_80m_${angle.toInt()}")
                .withLocation(loc)
                .withText("${String.format("%.4f", loc.latitude)}, ${String.format("%.4f", loc.longitude)}, 80m")
                .withTextColor(0xFFFF8100.toInt())
                .withIconColor(0xFFFF8100.toInt())
                .withShowEdgeGuide(true)
                .withVisible(true)
                .withIconVisible(true)
                .withTextVisible(true)
                .build()

            ARManager.getInstance().uploadARPointInfo(info, object :
                CommonCallbacks.CompletionCallbackWithParam<String> {
                override fun onSuccess(id: String?) {
                    uploadedPoints80.add(info)
                    successCount++
                    if (successCount == totalCount) {
                        updateResult(ARElementType.POINT, str(R.string.ar_point_upload_done))
                        showToast(str(R.string.ar_point_upload_toast))
                    }
                }
                override fun onFailure(error: IDJIError) {
                    updateResult(ARElementType.POINT, str(R.string.ar_point_upload_failed, error.description()))
                }
            })
        }
    }

    /** toggle 状态：false=初始态，true=已切换（80m文字隐藏+40m图标隐藏） */
    private var pointsToggled = false

    private fun updatePoint() {
        val totalCount = uploadedPoints40.size + uploadedPoints80.size
        if (totalCount == 0) {
            updateResult(ARElementType.POINT, str(R.string.ar_point_no_uploaded))
            return
        }

        var doneCount = 0
        val toggling = !pointsToggled // 目标状态

        // 80m 点：切换文字可见性
        for (point in uploadedPoints80) {
            point.setTextVisible(!toggling)
            ARManager.getInstance().updateARPointInfo(point, object :
                CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    doneCount++
                    if (doneCount == totalCount) {
                        pointsToggled = toggling
                        val msg = if (toggling) str(R.string.ar_point_update_toggled)
                                  else str(R.string.ar_point_update_restored)
                        updateResult(ARElementType.POINT, msg)
                        showToast(msg)
                    }
                }
                override fun onFailure(error: IDJIError) {
                    updateResult(ARElementType.POINT, str(R.string.ar_point_update_failed, error.description()))
                }
            })
        }

        // 40m 点：切换图标可见性
        for (point in uploadedPoints40) {
            point.setIconVisible(!toggling)
            ARManager.getInstance().updateARPointInfo(point, object :
                CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    doneCount++
                    if (doneCount == totalCount) {
                        pointsToggled = toggling
                        val msg = if (toggling) str(R.string.ar_point_update_toggled)
                                  else str(R.string.ar_point_update_restored)
                        updateResult(ARElementType.POINT, msg)
                        showToast(msg)
                    }
                }
                override fun onFailure(error: IDJIError) {
                    updateResult(ARElementType.POINT, str(R.string.ar_point_update_failed, error.description()))
                }
            })
        }
    }

    private fun deletePoint() {
        // 先查询所有点，再逐个删除
        ARManager.getInstance().queryARPointInfo(object :
            CommonCallbacks.CompletionCallbackWithParam<List<ARPointInfo>> {
            override fun onSuccess(list: List<ARPointInfo>?) {
                val points = list ?: emptyList()
                if (points.isEmpty()) {
                    val msg = str(R.string.ar_no_deletable, str(R.string.ar_point_title))
                    updateResult(ARElementType.POINT, msg)
                    showToast(msg)
                    return
                }
                var deletedCount = 0
                for (point in points) {
                    ARManager.getInstance().deleteARPointInfo(point, object :
                        CommonCallbacks.CompletionCallback {
                        override fun onSuccess() {
                            deletedCount++
                            if (deletedCount == points.size) {
                                uploadedPoints40.clear()
                                uploadedPoints80.clear()
                                updateResult(ARElementType.POINT, str(R.string.ar_deleted_all, points.size, str(R.string.ar_point_title)))
                                showToast(str(R.string.ar_delete_done, points.size, str(R.string.ar_point_title)))
                            }
                        }
                        override fun onFailure(error: IDJIError) {
                            updateResult(ARElementType.POINT, str(R.string.ar_point_delete_failed, error.description()))
                        }
                    })
                }
            }

            override fun onFailure(error: IDJIError) {
                val msg = str(R.string.ar_query_failed, str(R.string.ar_point_title), error.description())
                updateResult(ARElementType.POINT, msg)
                showToast(msg)
            }
        })
    }

    // ── 工具：经纬度偏移计算 ─────────────────────────────────────────────────

    /**
     * 以 (baseLat, baseLng, altitude) 为中心，在指定方向上偏移 distanceM 米。
     * @param altitude 绝对高度（米）
     * @param distanceM 水平偏移距离（米）
     * @param angleRad 方向角（弧度，0=正北，顺时针）
     */
    private fun offsetLocation(
        baseLat: Double, baseLng: Double, altitude: Double,
        distanceM: Double, angleRad: Double
    ): OpenLocation {
        val latOffset = distanceM * Math.cos(angleRad) / 111320.0
        val lngOffset = distanceM * Math.sin(angleRad) / (111320.0 * Math.cos(Math.toRadians(baseLat)))
        return OpenLocation(baseLat + latOffset, baseLng + lngOffset, altitude)
    }

    /** 起飞点绝对海拔（米），基于 RTK 椭球高度。取不到时默认 0.0。 */
    private fun getTakeoffAltitude(): Double {
        return SharedDataManager.getRtkTakeoffAltitudeOnce()
    }

    /**
     * 获取飞机正前方 40m 的经纬度中心点。
     * 根据航向角（KeyCompassHeading）将飞机当前位置沿机头方向前移 40m。
     * 若航向角不可用，fallback 到起飞点经纬度；若起飞点也不可用，返回 null。
     */
    private fun getForwardCenter(): Pair<Double, Double>? {
        val location = SharedDataManager.getAircraftLocationOnce() ?: return null
        val baseLat = location.latitude
        val baseLng = location.longitude
        val heading = SharedDataManager.getCompassHeadingOnce()
        if (heading != null) {
            val headingRad = Math.toRadians(heading)
            val fwdLat = baseLat + 40.0 * Math.cos(headingRad) / 111320.0
            val fwdLng = baseLng + 40.0 * Math.sin(headingRad) / (111320.0 * Math.cos(Math.toRadians(baseLat)))
            return fwdLat to fwdLng
        }
        // fallback：Home 点（起飞点）经纬度
        val homeLoc = SharedDataManager.getHomeLocationOnce()
        if (homeLoc != null) return homeLoc.latitude to homeLoc.longitude
        return baseLat to baseLng
    }

    // ── 线/面共用：点配置 ──────────────────────────────────────────────────────

    /** 线/面共用的 ARPointConfig ID（正交 size=20f，无文案） */
    private var sharedShapeConfigId: String? = null

    /**
     * 确保共用的 ARPointConfig 已上传。若已有则直接回调，否则上传后回调。
     */
    private fun ensureSharedShapeConfig(onReady: (String) -> Unit) {
        val existing = sharedShapeConfigId
        if (existing != null) {
            onReady(existing)
            return
        }

        val config = ARPointConfig.Builder.builder()
            .withName("shared_shape_point_config")
            .withUseText(false)
            .withArProjectionType(ARIconProjectionType.ORTHOGRAPHIC)
            .withIconOrthographicConfig(
                ARIconOrthographicConfig.Builder.builder()
                    .withSize(20f)
                    .withIconFileName("ar_cable_highlight_dot.svg")
                    .build()
            )
            .build()

        ARManager.getInstance().uploadARPointConfig(config, object :
            CommonCallbacks.CompletionCallbackWithParam<String> {
            override fun onSuccess(id: String?) {
                sharedShapeConfigId = id
                Log.i(TAG, "Shared shape config uploaded, id=$id")
                id?.let { onReady(it) }
            }

            override fun onFailure(error: IDJIError) {
                Log.e(TAG, "Upload shared shape config failed: ${error.description()}")
            }
        })
    }

    // ── CRUD 操作：线 ────────────────────────────────────────────────────────

    /** 缓存线上传的 6 个点 */
    private val linePoints = mutableListOf<ARPointInfo>()
    /** 缓存上传的线 */
    private var uploadedLine: ARLineInfo? = null

    private fun queryLine() {
        ARManager.getInstance().queryARLineInfo(object :
            CommonCallbacks.CompletionCallbackWithParam<List<ARLineInfo>> {
            override fun onSuccess(list: List<ARLineInfo>?) {
                val lines = list ?: emptyList()
                val msg = str(R.string.ar_query_found, lines.size, str(R.string.ar_line_title)) + "\n" +
                    lines.joinToString("\n") { "  id=${it.id}, name=${it.name}" }
                updateResult(ARElementType.LINE, msg)
                showToast(str(R.string.ar_query_found, lines.size, str(R.string.ar_line_title)))
            }

            override fun onFailure(error: IDJIError) {
                val msg = str(R.string.ar_query_failed, str(R.string.ar_line_title), error.description())
                updateResult(ARElementType.LINE, msg)
                showToast(msg)
            }
        })
    }

    private fun uploadLine() {
        if (uploadedLine != null) {
            updateResult(ARElementType.LINE, str(R.string.ar_already_uploaded, str(R.string.ar_line_title)))
            return
        }

        val center = getForwardCenter()
        if (center == null) {
            updateResult(ARElementType.LINE, str(R.string.ar_location_failed))
            return
        }

        ensureSharedShapeConfig { cid ->
            uploadLineWithConfig(cid, center.first, center.second, getTakeoffAltitude())
        }
    }

    private fun uploadLineWithConfig(cid: String, baseLat: Double, baseLng: Double, baseAlt: Double) {
        val radiusM = 20.0
        val angles = doubleArrayOf(0.0, 120.0, 240.0)

        linePoints.clear()
        val allLocations = mutableListOf<OpenLocation>()

        // 40m 处 3 个顶点 + 80m 处 3 个顶点
        for (altitude in doubleArrayOf(baseAlt+40.0, baseAlt+80.0)) {
            for (angle in angles) {
                val rad = Math.toRadians(angle)
                allLocations.add(offsetLocation(baseLat, baseLng, altitude, radiusM, rad))
            }
        }

        // 构建 6 个点（蓝色图标，无文案）
        val pointInfos = allLocations.mapIndexed { index, loc ->
            ARPointInfo.Builder.builder()
                .withConfigId(cid)
                .withName("line_point_$index")
                .withLocation(loc)
                .withVisible(true)
                .withIconVisible(true)
                .withTextVisible(false)
                .withIconColor(RemoteViewColors.BLUE_PRIMARY)
                .build()
        }

        // 构建线
        val strokeInfo = ARStrokeInfo.Builder.builder()
            .withColor(RemoteViewColors.BLUE_PRIMARY)
            .withIsDash(false)
            .withDistLineWidth(
                ARDistLineWidth.Builder.builder()
                    .withMaxLineWidth(20f)
                    .withMaxWidthDist(30f)
                    .withMinLineWidth(5f)
                    .withMinWidthDist(60f)
                    .build()
            )
            .build()

        val lineInfo = ARLineInfo.Builder.builder()
            .withName("demo_line")
            .withPoints(pointInfos)
            .withStrokeInfo(strokeInfo)
            .withUseDepth(true)
            .build()

        ARManager.getInstance().uploadARLineInfo(lineInfo, object :
            CommonCallbacks.CompletionCallbackWithParam<String> {
            override fun onSuccess(id: String?) {
                linePoints.addAll(pointInfos)
                uploadedLine = lineInfo
                val msg = str(R.string.ar_line_upload_success, id ?: "")
                updateResult(ARElementType.LINE, msg)
                showToast(msg)
            }

            override fun onFailure(error: IDJIError) {
                val msg = str(R.string.ar_line_upload_failed, error.description())
                updateResult(ARElementType.LINE, msg)
                showToast(msg)
            }
        })
    }

    private fun updateLine() {
        val line = uploadedLine
        if (line == null) {
            updateResult(ARElementType.LINE, str(R.string.ar_please_upload_first, str(R.string.ar_line_title)))
            return
        }

        // 线改 33% 透明蓝色虚线
        val newStrokeInfo = ARStrokeInfo.Builder.builder()
            .withColor(RemoteViewColors.BLUE_PRIMARY_33)
            .withIsDash(true)
            .withDistLineWidth(
                ARDistLineWidth.Builder.builder()
                    .withMaxLineWidth(20f)
                    .withMaxWidthDist(30f)
                    .withMinLineWidth(5f)
                    .withMinWidthDist(60f)
                    .build()
            )
            .build()

        line.strokeInfo = newStrokeInfo
        ARManager.getInstance().updateARLineInfo(line, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                // 同时把 6 个点的图标颜色改绿
                val msg = str(R.string.ar_line_update_success)
                updateResult(ARElementType.LINE, msg)
                showToast(msg)
            }

            override fun onFailure(error: IDJIError) {
                val msg = str(R.string.ar_line_update_failed, error.description())
                updateResult(ARElementType.LINE, msg)
                showToast(msg)
            }
        })
    }

    private fun deleteLine() {
        ARManager.getInstance().queryARLineInfo(object :
            CommonCallbacks.CompletionCallbackWithParam<List<ARLineInfo>> {
            override fun onSuccess(list: List<ARLineInfo>?) {
                val lines = list ?: emptyList()
                if (lines.isEmpty()) {
                    updateResult(ARElementType.LINE, str(R.string.ar_no_deletable, str(R.string.ar_line_title)))
                    return
                }
                var deletedCount = 0
                for (line in lines) {
                    ARManager.getInstance().deleteARLineInfo(line, object :
                        CommonCallbacks.CompletionCallback {
                        override fun onSuccess() {
                            deletedCount++
                            if (deletedCount == lines.size) {
                                uploadedLine = null
                                linePoints.clear()
                                updateResult(ARElementType.LINE, str(R.string.ar_deleted_all, lines.size, str(R.string.ar_line_title)))
                                showToast(str(R.string.ar_delete_done, lines.size, str(R.string.ar_line_title)))
                            }
                        }
                        override fun onFailure(error: IDJIError) {
                            updateResult(ARElementType.LINE, str(R.string.ar_line_delete_failed, error.description()))
                        }
                    })
                }
            }

            override fun onFailure(error: IDJIError) {
                updateResult(ARElementType.LINE, str(R.string.ar_query_failed, str(R.string.ar_line_title), error.description()))
            }
        })
    }

    // ── CRUD 操作：面 ────────────────────────────────────────────────────────

    /** 缓存面上传的 3 个点 */
    private val polygonPoints = mutableListOf<ARPointInfo>()
    /** 缓存上传的面 */
    private var uploadedPolygon: ARPolygonInfo? = null

    private fun queryPolygon() {
        ARManager.getInstance().queryARPolygonInfo(object :
            CommonCallbacks.CompletionCallbackWithParam<List<ARPolygonInfo>> {
            override fun onSuccess(list: List<ARPolygonInfo>?) {
                val polygons = list ?: emptyList()
                val msg = str(R.string.ar_query_found, polygons.size, str(R.string.ar_polygon_title)) + "\n" +
                    polygons.joinToString("\n") { "  id=${it.id}, name=${it.name}" }
                updateResult(ARElementType.POLYGON, msg)
                showToast(str(R.string.ar_query_found, polygons.size, str(R.string.ar_polygon_title)))
            }

            override fun onFailure(error: IDJIError) {
                val msg = str(R.string.ar_query_failed, str(R.string.ar_polygon_title), error.description())
                updateResult(ARElementType.POLYGON, msg)
                showToast(msg)
            }
        })
    }

    private fun uploadPolygon() {
        if (uploadedPolygon != null) {
            updateResult(ARElementType.POLYGON, str(R.string.ar_already_uploaded, str(R.string.ar_polygon_title)))
            return
        }

        val center = getForwardCenter()
        if (center == null) {
            updateResult(ARElementType.POLYGON, str(R.string.ar_location_failed))
            return
        }

        ensureSharedShapeConfig { cid ->
            uploadPolygonWithConfig(cid, center.first, center.second, getTakeoffAltitude())
        }
    }

    private fun uploadPolygonWithConfig(cid: String, baseLat: Double, baseLng: Double, baseAlt: Double) {
        val radiusM = 20.0
        val angles = doubleArrayOf(0.0, 120.0, 240.0)

        polygonPoints.clear()

        // 40m 处 3 个等边三角形顶点
        val pointInfos = angles.mapIndexed { index, angle ->
            val rad = Math.toRadians(angle)
            val loc = offsetLocation(baseLat, baseLng, baseAlt + 40.0, radiusM, rad)
            ARPointInfo.Builder.builder()
                .withConfigId(cid)
                .withName("polygon_point_$index")
                .withLocation(loc)
                .withVisible(true)
                .withIconVisible(true)
                .withTextVisible(false)
                .withIconColor(RemoteViewColors.BLUE_PRIMARY)
                .build()
        }

        val distLineWidth = ARDistLineWidth.Builder.builder()
            .withMaxLineWidth(20f).withMaxWidthDist(30f)
            .withMinLineWidth(5f).withMinWidthDist(60f)
            .build()

        val topFace = ARPolygonFace.Builder.builder()
            .withVisible(true)
            .withFrontFaceColor(RemoteViewColors.BLUE_PRIMARY_33)
            .withBackFaceColor(RemoteViewColors.BLUE_PRIMARY_33)
            .build()

        val sideFace = ARPolygonFace.Builder.builder()
            .withVisible(true)
            .withFrontFaceColor(RemoteViewColors.BLUE_PRIMARY_33)
            .withBackFaceColor(RemoteViewColors.BLUE_PRIMARY_33)
            .build()

        val bottomFace = ARPolygonFace.Builder.builder()
            .withVisible(true)
            .withFrontFaceColor(RemoteViewColors.BLUE_PRIMARY_33)
            .withBackFaceColor(RemoteViewColors.BLUE_PRIMARY_33)
            .build()

        val strokeInfo = ARStrokeInfo.Builder.builder()
            .withColor(RemoteViewColors.BLUE_DEEP)
            .withIsDash(false)
            .withDistLineWidth(distLineWidth)
            .build()
        val faceStroke = ARFaceStroke.Builder.builder()
            .withVisible(true).withStroke(strokeInfo).build()

        val info = ARPolygonInfo.Builder.builder()
            .withName("demo_polygon")
            .withPoints(pointInfos)
            .withExtrusionAnchor(OpenLocation(0.0, 0.0, 0.0))
            .withTopFace(topFace)
            .withSideFace(sideFace)
            .withBottomFace(bottomFace)
            .withTopFaceStroke(faceStroke)
            .withSideFaceStroke(faceStroke)
            .withBottomFaceStroke(faceStroke)
            .withFaceDistAlpha(ARDistAlpha.Builder.builder().withEnable(false).build())
            .withFaceStrokeDistAlpha(ARDistAlpha.Builder.builder().withEnable(false).build())
            .withIs3dMesh(false)
            .build()

        ARManager.getInstance().uploadARPolygonInfo(info, object :
            CommonCallbacks.CompletionCallbackWithParam<String> {
            override fun onSuccess(id: String?) {
                polygonPoints.addAll(pointInfos)
                uploadedPolygon = info
                val msg = str(R.string.ar_polygon_upload_success, id ?: "")
                updateResult(ARElementType.POLYGON, msg)
                showToast(msg)
            }

            override fun onFailure(error: IDJIError) {
                val msg = str(R.string.ar_polygon_upload_failed, error.description())
                updateResult(ARElementType.POLYGON, msg)
                showToast(msg)
            }
        })
    }

    private fun updatePolygon() {
        val polygon = uploadedPolygon
        if (polygon == null) {
            updateResult(ARElementType.POLYGON, str(R.string.ar_please_upload_first, str(R.string.ar_polygon_title)))
            return
        }

        // 获取飞机位置用于 extrusionAnchor
        val location = SharedDataManager.getAircraftLocationOnce()
        if (location == null) {
            updateResult(ARElementType.POLYGON, str(R.string.ar_location_failed))
            return
        }

        val distLineWidth = ARDistLineWidth.Builder.builder()
            .withMaxLineWidth(20f).withMaxWidthDist(30f)
            .withMinLineWidth(5f).withMinWidthDist(60f)
            .build()

        val strokeInfo = ARStrokeInfo.Builder.builder()
            .withColor(RemoteViewColors.BLUE_DEEP)
            .withIsDash(false)
            .withDistLineWidth(distLineWidth)
            .build()
        val faceStroke = ARFaceStroke.Builder.builder()
            .withVisible(true).withStroke(strokeInfo).build()

        // 与 40m 上传时保持一致的面样式
        val topFace = ARPolygonFace.Builder.builder()
            .withVisible(true)
            .withFrontFaceColor(RemoteViewColors.BLUE_PRIMARY_33)
            .withBackFaceColor(RemoteViewColors.BLUE_PRIMARY_33)
            .build()
        val sideFace = ARPolygonFace.Builder.builder()
            .withVisible(true)
            .withFrontFaceColor(RemoteViewColors.BLUE_PRIMARY_33)
            .withBackFaceColor(RemoteViewColors.BLUE_PRIMARY_33)
            .build()
        val bottomFace = ARPolygonFace.Builder.builder()
            .withVisible(true)
            .withFrontFaceColor(RemoteViewColors.BLUE_PRIMARY_33)
            .withBackFaceColor(RemoteViewColors.BLUE_PRIMARY_33)
            .build()

        // 直接修改已上传对象的属性（保留原始 id），拉伸到 80m
        polygon.extrusionAnchor = OpenLocation(0.0, 0.0, 40.0)
        polygon.topFace = topFace
        polygon.sideFace = sideFace
        polygon.bottomFace = bottomFace
        polygon.topFaceStroke = faceStroke
        polygon.sideFaceStroke = faceStroke
        polygon.bottomFaceStroke = faceStroke
        polygon.faceDistAlpha = ARDistAlpha.Builder.builder().withEnable(false).build()
        polygon.isIs3dMesh = false

        ARManager.getInstance().updateARPolygonInfo(polygon, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                val msg = str(R.string.ar_polygon_update_success)
                updateResult(ARElementType.POLYGON, msg)
                showToast(msg)
            }

            override fun onFailure(error: IDJIError) {
                val msg = str(R.string.ar_polygon_update_failed, error.description())
                updateResult(ARElementType.POLYGON, msg)
                showToast(msg)
            }
        })
    }

    private fun hidePolygonPoints() {
        for (point in polygonPoints) {
            point.isVisible = false
            ARManager.getInstance().updateARPointInfo(point, object :
                CommonCallbacks.CompletionCallback {
                override fun onSuccess() {}
                override fun onFailure(error: IDJIError) {
                    Log.e(TAG, "hidePolygonPoints failed: ${error.description()}")
                }
            })
        }
    }

    private fun deletePolygon() {
        ARManager.getInstance().queryARPolygonInfo(object :
            CommonCallbacks.CompletionCallbackWithParam<List<ARPolygonInfo>> {
            override fun onSuccess(list: List<ARPolygonInfo>?) {
                val polygons = list ?: emptyList()
                if (polygons.isEmpty()) {
                    updateResult(ARElementType.POLYGON, str(R.string.ar_no_deletable, str(R.string.ar_polygon_title)))
                    return
                }
                var deletedCount = 0
                for (polygon in polygons) {
                    ARManager.getInstance().deleteARPolygonInfo(polygon, object :
                        CommonCallbacks.CompletionCallback {
                        override fun onSuccess() {
                            deletedCount++
                            if (deletedCount == polygons.size) {
                                uploadedPolygon = null
                                polygonPoints.clear()
                                updateResult(ARElementType.POLYGON, str(R.string.ar_deleted_all, polygons.size, str(R.string.ar_polygon_title)))
                                showToast(str(R.string.ar_delete_done, polygons.size, str(R.string.ar_polygon_title)))
                            }
                        }
                        override fun onFailure(error: IDJIError) {
                            updateResult(ARElementType.POLYGON, str(R.string.ar_polygon_delete_failed, error.description()))
                        }
                    })
                }
            }

            override fun onFailure(error: IDJIError) {
                updateResult(ARElementType.POLYGON, str(R.string.ar_query_failed, str(R.string.ar_polygon_title), error.description()))
            }
        })
    }

    // ── CRUD 操作：圆 ────────────────────────────────────────────────────────

    /** 缓存上传的圆 */
    private var uploadedCircle: ARCircleInfo? = null

    private fun queryCircle() {
        ARManager.getInstance().queryARCircleInfo(object :
            CommonCallbacks.CompletionCallbackWithParam<List<ARCircleInfo>> {
            override fun onSuccess(list: List<ARCircleInfo>?) {
                val circles = list ?: emptyList()
                val msg = str(R.string.ar_query_found, circles.size, str(R.string.ar_circle_title)) + "\n" +
                    circles.joinToString("\n") { "  id=${it.id}, name=${it.name}, r=${it.radius}" }
                updateResult(ARElementType.CIRCLE, msg)
                showToast(str(R.string.ar_query_found, circles.size, str(R.string.ar_circle_title)))
            }

            override fun onFailure(error: IDJIError) {
                val msg = str(R.string.ar_query_failed, str(R.string.ar_circle_title), error.description())
                updateResult(ARElementType.CIRCLE, msg)
                showToast(msg)
            }
        })
    }

    private fun uploadCircle() {
        if (uploadedCircle != null) {
            updateResult(ARElementType.CIRCLE, str(R.string.ar_already_uploaded, str(R.string.ar_circle_title)))
            return
        }

        val center = getForwardCenter()
        if (center == null) {
            updateResult(ARElementType.CIRCLE, str(R.string.ar_location_failed))
            return
        }

        val distLineWidth = ARDistLineWidth.Builder.builder()
            .withMaxLineWidth(20f).withMaxWidthDist(30f)
            .withMinLineWidth(5f).withMinWidthDist(60f)
            .build()

        val topFace = ARPolygonFace.Builder.builder()
            .withVisible(true)
            .withFrontFaceColor(RemoteViewColors.BLUE_PRIMARY_33)
            .withBackFaceColor(RemoteViewColors.BLUE_PRIMARY_33)
            .build()

        val sideFace = ARPolygonFace.Builder.builder()
            .withVisible(true)
            .withFrontFaceColor(RemoteViewColors.BLUE_PRIMARY_33)
            .withBackFaceColor(RemoteViewColors.BLUE_PRIMARY_33)
            .build()

        val bottomFace = ARPolygonFace.Builder.builder()
            .withVisible(true)
            .withFrontFaceColor(RemoteViewColors.BLUE_PRIMARY_33)
            .withBackFaceColor(RemoteViewColors.BLUE_PRIMARY_33)
            .build()

        val strokeInfo = ARStrokeInfo.Builder.builder()
            .withColor(RemoteViewColors.BLUE_DEEP)
            .withIsDash(false)
            .withDistLineWidth(distLineWidth)
            .build()
        val faceStroke = ARFaceStroke.Builder.builder()
            .withVisible(true).withStroke(strokeInfo).build()

        val circleCenter = OpenLocation(center.first, center.second, getTakeoffAltitude() + 40.0)

        val info = ARCircleInfo.Builder.builder()
            .withName("demo_circle")
            .withCenter(circleCenter)
            .withRadius(20f)
            .withExtrusionAnchor(OpenLocation(0.0, 0.0, 0.0))
            .withTopFace(topFace)
            .withSideFace(sideFace)
            .withBottomFace(bottomFace)
            .withTopFaceStroke(faceStroke)
            .withSideFaceStroke(faceStroke)
            .withBottomFaceStroke(faceStroke)
            .withFaceDistAlpha(ARDistAlpha.Builder.builder().withEnable(false).build())
            .withFaceStrokeDistAlpha(ARDistAlpha.Builder.builder().withEnable(false).build())
            .build()

        ARManager.getInstance().uploadARCircleInfo(info, object :
            CommonCallbacks.CompletionCallbackWithParam<String> {
            override fun onSuccess(id: String?) {
                uploadedCircle = info
                val msg = str(R.string.ar_circle_upload_success, id ?: "")
                updateResult(ARElementType.CIRCLE, msg)
                showToast(msg)
            }

            override fun onFailure(error: IDJIError) {
                val msg = str(R.string.ar_circle_upload_failed, error.description())
                updateResult(ARElementType.CIRCLE, msg)
                showToast(msg)
            }
        })
    }

    private fun updateCircle() {
        val circle = uploadedCircle
        if (circle == null) {
            updateResult(ARElementType.CIRCLE, str(R.string.ar_please_upload_first, str(R.string.ar_circle_title)))
            return
        }

        val center = getForwardCenter()
        if (center == null) {
            updateResult(ARElementType.CIRCLE, str(R.string.ar_location_failed))
            return
        }

        val distLineWidth = ARDistLineWidth.Builder.builder()
            .withMaxLineWidth(20f).withMaxWidthDist(30f)
            .withMinLineWidth(5f).withMinWidthDist(60f)
            .build()

        val strokeInfo = ARStrokeInfo.Builder.builder()
            .withColor(RemoteViewColors.BLUE_DEEP)
            .withIsDash(true)
            .withDistLineWidth(distLineWidth)
            .build()
        val faceStroke = ARFaceStroke.Builder.builder()
            .withVisible(true).withStroke(strokeInfo).build()

        // 与 40m 上传时保持一致的面样式
        val topFace = ARPolygonFace.Builder.builder()
            .withVisible(true)
            .withFrontFaceColor(RemoteViewColors.BLUE_PRIMARY_33)
            .withBackFaceColor(RemoteViewColors.BLUE_PRIMARY_33)
            .build()
        val sideFace = ARPolygonFace.Builder.builder()
            .withVisible(true)
            .withFrontFaceColor(RemoteViewColors.BLUE_PRIMARY_33)
            .withBackFaceColor(RemoteViewColors.BLUE_PRIMARY_33)
            .build()
        val bottomFace = ARPolygonFace.Builder.builder()
            .withVisible(true)
            .withFrontFaceColor(RemoteViewColors.BLUE_PRIMARY_33)
            .withBackFaceColor(RemoteViewColors.BLUE_PRIMARY_33)
            .build()

        // 直接修改已上传对象的属性（保留原始 id），拉伸到 80m
        circle.center = OpenLocation(center.first, center.second, getTakeoffAltitude() + 40.0)
        circle.radius = 20f
        circle.extrusionAnchor = OpenLocation(0.0, 0.0, 40.0)
        circle.topFace = topFace
        circle.sideFace = sideFace
        circle.bottomFace = bottomFace
        circle.topFaceStroke = faceStroke
        circle.sideFaceStroke = faceStroke
        circle.bottomFaceStroke = faceStroke
        circle.faceDistAlpha = ARDistAlpha.Builder.builder().withEnable(false).build()

        ARManager.getInstance().updateARCircleInfo(circle, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                val msg = str(R.string.ar_circle_update_success)
                updateResult(ARElementType.CIRCLE, msg)
                showToast(msg)
            }

            override fun onFailure(error: IDJIError) {
                val msg = str(R.string.ar_circle_update_failed, error.description())
                updateResult(ARElementType.CIRCLE, msg)
                showToast(msg)
            }
        })
    }

    private fun deleteCircle() {
        ARManager.getInstance().queryARCircleInfo(object :
            CommonCallbacks.CompletionCallbackWithParam<List<ARCircleInfo>> {
            override fun onSuccess(list: List<ARCircleInfo>?) {
                val circles = list ?: emptyList()
                if (circles.isEmpty()) {
                    updateResult(ARElementType.CIRCLE, str(R.string.ar_no_deletable, str(R.string.ar_circle_title)))
                    return
                }
                var deletedCount = 0
                for (circle in circles) {
                    ARManager.getInstance().deleteARCircleInfo(circle, object :
                        CommonCallbacks.CompletionCallback {
                        override fun onSuccess() {
                            deletedCount++
                            if (deletedCount == circles.size) {
                                uploadedCircle = null
                                updateResult(ARElementType.CIRCLE, str(R.string.ar_deleted_all, circles.size, str(R.string.ar_circle_title)))
                                showToast(str(R.string.ar_delete_done, circles.size, str(R.string.ar_circle_title)))
                            }
                        }
                        override fun onFailure(error: IDJIError) {
                            updateResult(ARElementType.CIRCLE, str(R.string.ar_circle_delete_failed, error.description()))
                        }
                    })
                }
            }

            override fun onFailure(error: IDJIError) {
                updateResult(ARElementType.CIRCLE, str(R.string.ar_query_failed, str(R.string.ar_circle_title), error.description()))
            }
        })
    }

    // ── 雷达 ────────────────────────────────────────────────────────────────

    private fun enableRadar() {
        val config = ARRadarConfig.Builder.builder()
            .withEnable(true)
            .withLabel("Demo Radar")
            .build()

        ARManager.getInstance().enableARRadar(config, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                val msg = str(R.string.ar_radar_enabled)
                updateResult(ARElementType.RADAR, msg)
                showToast(msg)
            }

            override fun onFailure(error: IDJIError) {
                val msg = str(R.string.ar_radar_enable_failed, error.description())
                updateResult(ARElementType.RADAR, msg)
                showToast(msg)
            }
        })
    }

    private fun disableRadar() {
        val config = ARRadarConfig.Builder.builder()
            .withEnable(false)
            .build()

        ARManager.getInstance().enableARRadar(config, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                val msg = str(R.string.ar_radar_disabled)
                updateResult(ARElementType.RADAR, msg)
                showToast(msg)
            }

            override fun onFailure(error: IDJIError) {
                val msg = str(R.string.ar_radar_disable_failed, error.description())
                updateResult(ARElementType.RADAR, msg)
                showToast(msg)
            }
        })
    }

    // ── 工具方法 ─────────────────────────────────────────────────────────────

    private fun updateResult(type: ARElementType, text: String) {
        val helper = drawingContext?.helper ?: return
        val id = resultTextIds[type] ?: return
        helper.updateView(id, RemoteViewProperty.TEXT, text)
    }

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
