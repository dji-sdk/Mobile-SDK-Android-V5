package com.dji.open.pilot.plugin.sampleitem.navigatemanager

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.NavigationHelper
import com.dji.open.pilot.plugin.common.drawing.DrawingContext
import com.dji.open.pilot.plugin.common.drawing.FloatingWindowOwner
import com.dji.open.pilot.plugin.main.PluginApplication
import dji.open.pilot.base.ui.NavigateAction
import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.client.manager.ui.NavigateManager
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.ux.remoteview.RemoteViewHelper
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 页面跳转 Demo 调度层 ViewModel。
 *
 * 职责：
 * - 悬浮窗单页绘制：结果区 + 8 个"跳转"按钮，覆盖 [NavigateAction.Value] 全部有效跳转目标
 * - 每个按钮调用 NavigateManager.getInstance().navigateToPage(action, callback)
 * - 回调结果（onSuccess / onFailure）回写悬浮窗结果 TextView
 *
 * 结构完全对齐 DialogDemoViewModel。
 */
class NavigateDemoViewModel : ViewModel() {

    companion object {
        private const val TAG = "NavigateDemoViewModel"
        private const val OWNER_TAG = "navigate_manager"

        /** 相册页默认相机 index */
        private const val DEFAULT_CAMERA_INDEX = "0"
    }

    val selectedType = MutableLiveData<NavigateType?>(null)

    private val initialized = AtomicBoolean(false)
    private var drawingContext: DrawingContext? = null

    /** 结果展示 TextView 的 viewId */
    private var resultTextId: String? = null

    // ── 悬浮窗绘制 ──────────────────────────────────────────────────────────

    fun initAllLayouts() {
        if (!initialized.compareAndSet(false, true)) return

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

        // 结果展示卡片
        resultTextId = ctx.drawResultCard(root)

        // 操作按钮卡片（8 个跳转按钮，垂直堆叠：每个按钮独占一行）
        ctx.titledCard(root, R.string.card_title_action) { card ->
            listOf(
                R.string.navigate_btn_pilot_home to ::navigateToPilotHome,
                R.string.navigate_btn_pilot_map to ::navigateToPilotMap,
                R.string.navigate_btn_pilot_waypoint to ::navigateToWaypointList,
                R.string.navigate_btn_pilot_album to ::navigateToPilotAlbum,
                R.string.navigate_btn_pilot_default to ::navigateToPilotDefault,
                R.string.navigate_btn_plugin_app to ::navigateToPluginApp,
                R.string.navigate_btn_self_define to ::navigateToSelfDefine,
                R.string.navigate_btn_remote_view to ::navigateToRemoteViewPage,
            ).forEach { (textResId, action) ->
                card.addToRemote(ctx.primaryButton(textResId, action), ctx.marginParam(2))
            }
        }

        // 返回按钮
        ctx.addBackButton(root) { NavigationHelper.navigateToPluginApp() }

        FloatingWindowOwner.claim(OWNER_TAG)
    }

    fun drawAndNavigate() {
        if (!FloatingWindowOwner.isOwnedBy(OWNER_TAG)) {
            initialized.set(false)
        }
        initAllLayouts()
        NavigationHelper.navigateToPilot()
    }

    // ── 跳转调用 ──────────────────────────────────────────────────────────

    private fun navigateToPilotHome() =
        navigate("PILOT_HOME", NavigateAction(NavigateAction.Value.PILOT_HOME))

    private fun navigateToPilotMap() =
        navigate("PILOT_MAP", NavigateAction(NavigateAction.Value.PILOT_MAP))

    private fun navigateToWaypointList() =
        navigate(
            "PILOT_WAYPOINT_MISSION_LIST",
            NavigateAction(NavigateAction.Value.PILOT_WAYPOINT_MISSION_LIST)
        )

    private fun navigateToPilotAlbum() =
        navigate(
            "PILOT_ALBUM (cameraIndex=$DEFAULT_CAMERA_INDEX)",
            NavigateAction(NavigateAction.Value.PILOT_ALBUM, DEFAULT_CAMERA_INDEX)
        )

    private fun navigateToPilotDefault() =
        navigate("PILOT_DEFAULT", NavigateAction(NavigateAction.Value.PILOT_DEFAULT))

    private fun navigateToPluginApp() =
        navigate("PLUGIN_APP", NavigateAction(NavigateAction.Value.PLUGIN_APP, "com.dji.open.pilot.plugin.main.MainActivity"))

    private fun navigateToSelfDefine() =
        navigate(
            "SELF_DEFINE (${PluginApplication.currentActivityClassName})",
            NavigateAction(
                NavigateAction.Value.SELF_DEFINE,
                PluginApplication.currentActivityClassName
            )
        )

    private fun navigateToRemoteViewPage() =
        navigate(
            "REMOTE_VIEW_PAGE_ACTIVITY",
            NavigateAction(NavigateAction.Value.REMOTE_VIEW_PAGE_ACTIVITY)
        )

    /**
     * 统一的跳转调用：执行 navigateToPage 并把回调结果写回结果卡片。
     * @param label 结果区展示的动作标签
     */
    private fun navigate(label: String, action: NavigateAction) {
        NavigateManager.getInstance().navigateToPage(
            action,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    Log.i(TAG, "navigate $label success")
                    pushResult("[Navigate]\n\n[Action] $label\n\n[Result] Success")
                }

                override fun onFailure(error: IDJIError) {
                    Log.e(TAG, "navigate $label failed: $error")
                    pushResult("[Navigate]\n\n[Action] $label\n\n[Result] Failed\n$error")
                }
            }
        )
    }

    private fun pushResult(message: String) {
        val id = resultTextId ?: return
        drawingContext?.helper?.updateView(id, RemoteViewProperty.TEXT, message)
    }
}
