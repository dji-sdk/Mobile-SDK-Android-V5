package com.dji.open.pilot.plugin.sampleitem.remoteviewpage

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dji.open.pilot.plugin.main.PluginApplication
import com.dji.open.pilot.plugin.sampleitem.remoteviewpage.ComponentType
import com.dji.open.pilot.plugin.sampleitem.remoteviewpage.drawing.ButtonDrawer
import com.dji.open.pilot.plugin.sampleitem.remoteviewpage.drawing.CheckBoxDrawer
import com.dji.open.pilot.plugin.common.drawing.DrawingContext
import com.dji.open.pilot.plugin.common.drawing.FloatingWindowOwner
import com.dji.open.pilot.plugin.sampleitem.remoteviewpage.drawing.EditTextDrawer
import com.dji.open.pilot.plugin.sampleitem.remoteviewpage.drawing.ImageViewDrawer
import com.dji.open.pilot.plugin.sampleitem.remoteviewpage.drawing.LayoutDrawer
import com.dji.open.pilot.plugin.sampleitem.remoteviewpage.drawing.ListViewDrawer
import com.dji.open.pilot.plugin.sampleitem.remoteviewpage.drawing.ProgressBarDrawer
import com.dji.open.pilot.plugin.sampleitem.remoteviewpage.drawing.SpinnerDrawer
import com.dji.open.pilot.plugin.sampleitem.remoteviewpage.drawing.SwitchDrawer
import com.dji.open.pilot.plugin.sampleitem.remoteviewpage.drawing.TextViewDrawer
import com.dji.open.pilot.plugin.sampleitem.remoteviewpage.drawing.TopNavBarDrawer
import com.dji.open.pilot.plugin.sampleitem.remoteviewpage.drawing.WebViewDrawer
import dji.open.pilot.base.ui.NavigateAction
import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.base.ui.model.VisibilityType
import dji.open.pilot.client.manager.ui.NavigateManager
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.utils.common.LogUtils
import dji.v5.ux.remoteview.RemoteLinearLayout
import dji.v5.ux.remoteview.RemoteViewHelper

/**
 * IRemoteViewPageManager Demo 页的调度层 ViewModel。
 *
 * 职责：
 * - 维护当前选中组件类型和用例列表的 LiveData
 * - 构造 DrawingContext，分派绘制任务到对应的 XxxDrawer
 * - 处理页面导航（navigateToPilot / navigateToPluginApp）
 *
 * 绘制细节完全下放到 component/ 包下各 Drawer object，本类不包含任何绘制代码。
 */
class RemoteViewPageDemoViewModel : ViewModel() {

    private val TAG = "RemoteViewPageDemoVM"
    private val mainHandler = Handler(Looper.getMainLooper())

    private val helper = RemoteViewHelper.floatingBoxHelper
    private val ctx by lazy { DrawingContext(helper) }

    companion object {
        private const val OWNER_TAG = "remote_view_page"
    }

    /** 当前选中的组件类型，null 表示未选中（右侧显示空白提示） */
    val selectedType = MutableLiveData<ComponentType?>(null)

    // ─── 用例数据 ────────────────────────────────────────────────────────────

    fun loadCases(type: ComponentType) {
        selectedType.value = type
    }

    // ─── 绘制调度（一次性全量绘制 + VISIBILITY 切换） ──────────────────────────

    /** 布局是否已初始化（全量绘制完成），AtomicBoolean 保证多线程安全 */
    private val isLayoutInitialized = AtomicBoolean(false)

    /** 每个 ComponentType 对应的内容容器 id */
    private val containerIds = mutableMapOf<ComponentType, String>()

    /** 共用 NavBar 中 title TextView 的 id，供切换时 updateView 改文字 */
    private var navBarTitle: dji.v5.ux.remoteview.RemoteText? = null

    /** 当前处于 VISIBLE 状态的组件类型 */
    private var currentVisibleType: ComponentType? = null

    /**
     * 一次性全量绘制所有组件布局。
     * 在 Activity onCreate 时调用，后续切换只做 VISIBILITY 切换。
     */
    fun initAllLayouts() {
        if (!isLayoutInitialized.compareAndSet(false, true)) return  // 原子性保证只执行一次
        Log.i(TAG, "initAllLayouts: start")

        helper.clearLayout()
        helper.setPageLayoutParamWrapContent()

        val root = helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setPadding(4, 4, 4, 4)
        }
        root.addToRoot()

        // 共用 NavBar（返回 title id 供后续 updateView）
        navBarTitle = TopNavBarDrawer.drawShared(ctx, root, ComponentType.values().first(),
            onPrev = { switchToPrev() },
            onNext = { switchToNext() }
        )

        // 10 个内容容器，全部默认 GONE
        for (type in ComponentType.values()) {
            val container = helper.createLinearLayoutVertical().apply {
                setWidth(RemoteView.MATCH_PARENT)
                setHeight(RemoteView.WRAP_CONTENT)
                setVisibility(VisibilityType.GONE)
            }
            root.addToRemote(container, ctx.marginParam(0))
            containerIds[type] = container.getId()

            // 绘制该组件的全部内容到 container
            dispatchDraw(type, container)
        }

        // 共用返回按钮
        ctx.addBackButton(root, ::navigateToPluginApp)

        FloatingWindowOwner.claim(OWNER_TAG)
        Log.i(TAG, "initAllLayouts: done, ${containerIds.size} containers created")
    }

    /**
     * 切换可见性：隐藏旧容器、显示新容器、更新 NavBar 标题。
     * 仅 3 次 updateView IPC，同一帧内完成，无闪动。
     */
    private fun switchVisibility(newType: ComponentType) {
        if (currentVisibleType == newType) {
            LogUtils.d(TAG,"currentVisibleType: $currentVisibleType,  newType: $newType")
            return
        }

        // 显示新容器
        containerIds[newType]?.let { id ->
            helper.updateView(id, RemoteViewProperty.VISIBILITY, "0") // VISIBLE
        }

        // 隐藏旧容器
        currentVisibleType?.let { old ->
            if (old != newType) {
                containerIds[old]?.let { id ->
                    helper.updateView(id, RemoteViewProperty.VISIBILITY, "8") // GONE
                }
            }
        }

        // 更新 NavBar 标题（setTextByResId 进 resIdMap，支持语言切换刷新）
        navBarTitle?.apply {
            setTextByResId(newType.displayNameRes)
            syncToRemote()
        }
        currentVisibleType = newType
    }

    /**
     * 对外入口：切换到当前选中组件并跳转到 Pilot。
     * 首次调用时触发 initAllLayouts()（如果尚未初始化），后续仅 switchVisibility。
     */
    fun drawAndNavigate() {
        val type = selectedType.value ?: return
        Log.i(TAG, "drawAndNavigate: type=$type")
        if (!FloatingWindowOwner.isOwnedBy(OWNER_TAG)) {
            isLayoutInitialized.set(false)
        }
        if (!isLayoutInitialized.get()) initAllLayouts()
        switchVisibility(type)
        navigateToPilot()
    }

    /**
     * TopNavBar 箭头点击触发：切换组件类型。
     * 由于 AIDL onClick 回调在 Binder 线程池执行，通过 mainHandler 切回主线程。
     * 仅做 switchVisibility + loadCases，不再全量重绘，不 navigateToPilot。
     */
    private fun switchAndRedraw(newType: ComponentType, isNavigate: Boolean = false) {
        mainHandler.post {
            loadCases(newType)
            if (!FloatingWindowOwner.isOwnedBy(OWNER_TAG)) {
                isLayoutInitialized.set(false)
            }
            if (!isLayoutInitialized.get()) {
                initAllLayouts()
            }
            switchVisibility(newType)
            if (isNavigate) navigateToPilot()
        }
    }

    /** 切换到上一个组件类型（由 NavBar 左箭头触发） */
    private fun switchToPrev() {
        val values = ComponentType.values()
        val current = currentVisibleType ?: values.first()
        val prev = values[(current.ordinal - 1 + values.size) % values.size]
        switchAndRedraw(prev, false)
    }

    /** 切换到下一个组件类型（由 NavBar 右箭头触发） */
    private fun switchToNext() {
        val values = ComponentType.values()
        val current = currentVisibleType ?: values.first()
        val next = values[(current.ordinal + 1) % values.size]
        switchAndRedraw(next, false)
    }

    /** 把对应组件的绘制内容派发到指定 container */
    private fun dispatchDraw(type: ComponentType, container: RemoteLinearLayout) {
        when (type) {
            ComponentType.LAYOUT       -> LayoutDrawer.draw(ctx, container)
            ComponentType.TEXT_VIEW    -> TextViewDrawer.draw(ctx, container)
            ComponentType.IMAGE_VIEW   -> ImageViewDrawer.draw(ctx, container)
            ComponentType.PROGRESS_BAR -> ProgressBarDrawer.draw(ctx, container)
            ComponentType.WEB_VIEW     -> WebViewDrawer.draw(ctx, container)
            ComponentType.LIST_VIEW    -> ListViewDrawer.draw(ctx, container)
            ComponentType.BUTTON       -> ButtonDrawer.draw(ctx, container)
            ComponentType.EDIT_TEXT    -> EditTextDrawer.draw(ctx, container)
            ComponentType.CHECK_BOX   -> CheckBoxDrawer.draw(ctx, container)
            ComponentType.SWITCH      -> SwitchDrawer.draw(ctx, container)
            ComponentType.SPINNER      -> SpinnerDrawer.draw(ctx, container)
        }
    }

    // ─── 导航 ────────────────────────────────────────────────────────────────

    private fun navigateToPilot() {
        NavigateManager.getInstance().navigateToPage(
            NavigateAction(NavigateAction.Value.PILOT_DEFAULT, ""),
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() { Log.i(TAG, "navigateToPilot success") }
                override fun onFailure(error: IDJIError) { Log.e(TAG, "navigateToPilot failed: $error") }
            }
        )
    }

    private fun navigateToPluginApp() {
        NavigateManager.getInstance().navigateToPage(
            NavigateAction(NavigateAction.Value.SELF_DEFINE, PluginApplication.currentActivityClassName),
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() { Log.i(TAG, "navigateToPluginApp success") }
                override fun onFailure(error: IDJIError) { Log.e(TAG, "navigateToPluginApp failed: $error") }
            }
        )
    }
}
