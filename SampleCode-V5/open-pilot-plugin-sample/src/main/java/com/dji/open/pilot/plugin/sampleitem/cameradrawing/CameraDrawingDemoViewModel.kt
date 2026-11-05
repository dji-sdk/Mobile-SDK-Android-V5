package com.dji.open.pilot.plugin.sampleitem.cameradrawing

import android.graphics.Color
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.NavigationHelper
import com.dji.open.pilot.plugin.common.drawing.DrawingContext
import com.dji.open.pilot.plugin.common.drawing.FloatingWindowOwner
import com.dji.open.pilot.plugin.common.drawing.NavBarHelper
import com.dji.open.pilot.plugin.main.PluginApplication
import com.dji.open.pilot.plugin.sampleitem.cameradrawing.drawing.BoxDrawer
import com.dji.open.pilot.plugin.sampleitem.cameradrawing.drawing.PolygonDrawer
import com.dji.open.pilot.plugin.sampleitem.cameradrawing.drawing.TextDrawer
import dji.open.pilot.base.manager.intelligent.OpenDoubleRect
import dji.open.pilot.base.manager.intelligent.OpenPoint
import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.base.ui.model.VisibilityType
import dji.open.pilot.base.ui.cameraview.DrawingBoxInfo
import dji.open.pilot.base.ui.cameraview.DrawingPolygonInfo
import dji.open.pilot.base.ui.cameraview.DrawingTextInfo
import dji.open.pilot.client.manager.ui.CameraViewDrawingManager
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.ux.remoteview.RemoteViewHelper
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 图传绘制 Demo 调度层 ViewModel。
 *
 * 职责：
 * - 悬浮窗一次性全量绘制 + 切换可见性
 * - 三类绘制（矩形框 / 多边形 / 文字）的更新/清除分派到 CameraViewDrawingManager
 * - 操作结果回写悬浮窗 TextView + Toast
 *
 * 说明：图传绘制为「整表替换」语义，更新即传全量列表，清除即传空列表；
 * 坐标均为归一化 0~1，颜色为 ARGB int。
 */
class CameraDrawingDemoViewModel : ViewModel() {

    companion object {
        private const val TAG = "CameraDrawingDemoVM"
        private const val OWNER_TAG = "camera_view_drawing"
    }

    val selectedType = MutableLiveData<CameraDrawingType?>(null)

    /** 各 Tab 对应的结果 TextView viewId */
    private val resultTextIds = mutableMapOf<CameraDrawingType, String>()

    /** 各 Tab 对应的容器 viewId（用于切换 VISIBILITY） */
    private val containerIds = mutableMapOf<CameraDrawingType, String>()

    // 容器可见性的唯一事实来源，仅在主线程读写；selectedType 只通知 Activity。
    // postValue 异步更新，Activity 菜单也会修改 selectedType，不能据此切换容器。
    private var currentVisibleType: CameraDrawingType? = null

    /** NavBar 标题组件（持引用，供 setTextByResId 切换，走 resIdMap 支持语言刷新） */
    private var navBarTitle: dji.v5.ux.remoteview.RemoteText? = null

    private val initialized = AtomicBoolean(false)
    private var drawingContext: DrawingContext? = null

    /** 距前方障碍物距离(米)，暂固定；后续可由雷达/激光实时更新后重绘 */
    private var obstacleDistanceMeters: Double = 25.6

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

        val root = helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        root.addToRoot(null)

        val initialType = CameraDrawingType.BOX
        navBarTitle = NavBarHelper.draw(ctx, root, initialType.displayNameRes,
            onPrev = { if (isCurrentLayout(ctx)) switchToPrev() },
            onNext = { if (isCurrentLayout(ctx)) switchToNext() }
        )

        CameraDrawingType.values().forEach { type ->
            val container = helper.createLinearLayoutVertical().apply {
                setWidth(RemoteView.MATCH_PARENT)
                setHeight(RemoteView.WRAP_CONTENT)
                setVisibility(if (type == initialType) VisibilityType.VISIBLE else VisibilityType.GONE)
            }
            root.addToRemote(container)
            containerIds[type] = container.getId()

            val resultId = when (type) {
                CameraDrawingType.BOX -> BoxDrawer.draw(ctx, container,
                    onUpdate = { updateBox() }, onClear = { clearBox() })
                CameraDrawingType.POLYGON -> PolygonDrawer.draw(ctx, container,
                    onUpdate = { updatePolygon() }, onClear = { clearPolygon() })
                CameraDrawingType.TEXT -> TextDrawer.draw(ctx, container,
                    onUpdate = { updateText() }, onClear = { clearText() })
            }
            resultTextIds[type] = resultId
        }

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

    private fun switchVisibility(newType: CameraDrawingType) {
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
        val values = CameraDrawingType.values()
        val prev = values[(current.ordinal - 1 + values.size) % values.size]
        switchVisibility(prev)
    }

    private fun switchToNext() {
        val current = currentVisibleType ?: return
        val values = CameraDrawingType.values()
        val next = values[(current.ordinal + 1) % values.size]
        switchVisibility(next)
    }

    // ── 矩形框 ───────────────────────────────────────────────────────────────

    /** 绘制 4 个红色竖长条形框（归一化坐标，x/y 为中心点，无标签）。 */
    private fun updateBox() {
        val boxList = mutableListOf(
            // 框1：左侧宽矮楼
            DrawingBoxInfo().apply {
                rect = OpenDoubleRect(0.22, 0.58, 0.19, 0.21)
                label = "draw1"
                color = Color.RED
                labelColor = Color.YELLOW
            },
            // 框2：中左窄高塔
            DrawingBoxInfo().apply {
                rect = OpenDoubleRect(0.35, 0.517, 0.055, 0.22)
                label = "draw2"
                color = Color.RED
                labelColor = Color.YELLOW
            },
            // 框3：中右窄高塔
            DrawingBoxInfo().apply {
                rect = OpenDoubleRect(0.65, 0.650, 0.09, 0.42)
                label = "draw3"
                color = Color.RED
                labelColor = Color.YELLOW
            },
            // 框4：右侧窄高塔
            DrawingBoxInfo().apply {
                rect = OpenDoubleRect(0.78, 0.63, 0.075, 0.38)
                label = "draw4"
                color = Color.RED
                labelColor = Color.YELLOW
            }
        )
        CameraViewDrawingManager.getInstance().updateDrawingBoxInfo(boxList, callback(CameraDrawingType.BOX))
        updateResult(CameraDrawingType.BOX, str(R.string.cvd_box_updating, boxList.size))
    }

    private fun clearBox() {
        CameraViewDrawingManager.getInstance()
            .updateDrawingBoxInfo(mutableListOf(), callback(CameraDrawingType.BOX))
        updateResult(CameraDrawingType.BOX, str(R.string.cvd_cleared))
    }

    // ── 多边形 ───────────────────────────────────────────────────────────────

    /**
     * 绘制 6 个半透明绿多边形（3 行×左右叶片），模拟风机旋转平面，
     * 坐标参考 windTurbApp LiveViewOverlayManager；并附带一条距障碍物距离文案。
     */
    private fun updatePolygon() {
        val polygonList = mutableListOf(
            buildPolygon(0.415, 0.660, 0.495, 0.660, 0.495, 0.675, 0.400, 0.675),
            buildPolygon(0.505, 0.660, 0.585, 0.660, 0.600, 0.675, 0.505, 0.675),
            buildPolygon(0.400, 0.680, 0.495, 0.680, 0.495, 0.695, 0.385, 0.695),
            buildPolygon(0.505, 0.680, 0.600, 0.680, 0.615, 0.695, 0.505, 0.695),
            buildPolygon(0.385, 0.700, 0.495, 0.700, 0.495, 0.715, 0.370, 0.715),
            buildPolygon(0.505, 0.700, 0.615, 0.700, 0.630, 0.715, 0.505, 0.715)
        )
        CameraViewDrawingManager.getInstance()
            .updateDrawingPolygonInfo(polygonList, callback(CameraDrawingType.POLYGON))

        // 附带距障碍物距离文案（距离值暂固定，后续可由雷达/激光实时更新）
        val distanceText = mutableListOf(buildDistanceText())
        CameraViewDrawingManager.getInstance().updateDrawingTextInfo(distanceText, null)

        updateResult(CameraDrawingType.POLYGON, str(R.string.cvd_polygon_updating, polygonList.size))
    }

    /** 构造距障碍物距离文案（锚点、样式沿用参考实现）。 */
    private fun buildDistanceText(): DrawingTextInfo = DrawingTextInfo().apply {
        anchor = OpenPoint(0.5, 0.750)
        text = str(R.string.cvd_polygon_distance_text, obstacleDistanceMeters)
        textSize = 12f
        textColor = TEXT_COLOR_GREEN
        backgroundColor = 0x00000000
        horizontalAlignment = DrawingTextInfo.ALIGN_CENTER
    }

    private fun buildPolygon(
        x1: Double, y1: Double, x2: Double, y2: Double,
        x3: Double, y3: Double, x4: Double, y4: Double
    ): DrawingPolygonInfo = DrawingPolygonInfo().apply {
        points = mutableListOf(
            OpenPoint(x1, y1), OpenPoint(x2, y2), OpenPoint(x3, y3), OpenPoint(x4, y4)
        )
        fillColor = FILL_COLOR_GREEN
        strokeWidth = 0f
    }

    private fun clearPolygon() {
        CameraViewDrawingManager.getInstance()
            .updateDrawingPolygonInfo(mutableListOf(), callback(CameraDrawingType.POLYGON))
        // 一并清除多边形附带的距离文案
        CameraViewDrawingManager.getInstance().updateDrawingTextInfo(mutableListOf(), null)
        updateResult(CameraDrawingType.POLYGON, str(R.string.cvd_cleared))
    }

    // ── 文字 ─────────────────────────────────────────────────────────────────

    /** 绘制距离提示文字（v5-sample 示例数据）。 */
    private fun updateText() {
        val textList = mutableListOf(
            DrawingTextInfo().apply {
                anchor = OpenPoint(0.5, 0.690)
                text = str(R.string.cvd_text_content)
                textSize = 12f
                textColor = TEXT_COLOR_GREEN
                backgroundColor = 0x00000000
                horizontalAlignment = DrawingTextInfo.ALIGN_CENTER
            }
        )
        CameraViewDrawingManager.getInstance()
            .updateDrawingTextInfo(textList, callback(CameraDrawingType.TEXT))
        updateResult(CameraDrawingType.TEXT, str(R.string.cvd_text_updating, textList.size))
    }

    private fun clearText() {
        CameraViewDrawingManager.getInstance()
            .updateDrawingTextInfo(mutableListOf(), callback(CameraDrawingType.TEXT))
        updateResult(CameraDrawingType.TEXT, str(R.string.cvd_cleared))
    }

    // ── 工具方法 ─────────────────────────────────────────────────────────────

    /** 统一的完成回调：成功回写完成、失败回写错误并 Toast。 */
    private fun callback(type: CameraDrawingType): CommonCallbacks.CompletionCallback =
        object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                updateResult(type, str(R.string.cvd_done, str(type.titleRes)))
            }

            override fun onFailure(error: IDJIError) {
                val msg = str(R.string.cvd_failed, str(type.titleRes), error.description())
                Log.e(TAG, msg)
                updateResult(type, msg)
                showToast(msg)
            }
        }

    private fun updateResult(type: CameraDrawingType, text: String) {
        val helper = drawingContext?.helper ?: return
        val id = resultTextIds[type] ?: return
        helper.updateView(id, RemoteViewProperty.TEXT, text)
    }

    private fun str(@StringRes resId: Int, vararg args: Any): String {
        return PluginApplication.instance?.getString(resId, *args) ?: ""
    }

    private fun showToast(msg: String) {
        val ctx = PluginApplication.instance ?: return
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
    }
}

/** 多边形填充色：半透明绿（ARGB，alpha 在高位）。 */
private const val FILL_COLOR_GREEN = 0x5500EE8B

/** 文字颜色：不透明绿（ARGB）。 */
private const val TEXT_COLOR_GREEN = 0xFF00EE8B.toInt()
