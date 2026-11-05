package dji.v5.ux.remoteview

import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.OperationResult
import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.base.ui.model.ViewPageType
import dji.open.pilot.base.ui.model.ViewSide
import dji.open.pilot.base.ui.model.ViewType
import dji.open.pilot.base.ui.model.WidgetLayoutParam
import dji.open.pilot.client.manager.RemoteServerConfigManager
import dji.open.pilot.client.manager.callback.IRemoteLanguageChangeListener
import dji.open.pilot.client.manager.file.FileTransferManager
import dji.open.pilot.client.manager.ui.RemoteViewPageManager
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.utils.common.LogUtils
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class RemoteViewHelper private constructor(
    private val pageType: ViewPageType
) {

    /** 当前 Helper 管理的所有 Provider，用于语言切换时批量更新文案 */
    private val providers = mutableListOf<RemoteViewProvider>()

    /** 已注册的语言订阅器 ID，-1 表示未注册 */
    private var languageListenerId = -1

    /** 语言变更订阅：遍历所有 providers，更新含 ResId 的文案 */
    private val languageListener =
        IRemoteLanguageChangeListener { remoteLanguage ->
            providers.forEach {
                LogUtils.i(
                    TAG,
                    "onLanguageChanged: $remoteLanguage, providers."
                )
                it.updateTextByResId()
            }
        }

    /**
     * 追踪 provider，首次有 provider 时懒注册语言订阅。
     */
    private fun trackProvider(provider: RemoteViewProvider) {
        providers.add(provider)
        if (languageListenerId == -1) {
            languageListenerId = RemoteServerConfigManager.getInstance()
                .addLanguageObserver(languageListener)
        }
    }

    /**
     * 注销语言订阅，清空 providers 及其 resIdMap。
     * 在 clearLayout 时调用，确保已销毁的视图不会再接收语言切换回调。
     */
    private fun unregisterLanguageListener() {
        if (languageListenerId != -1) {
            RemoteServerConfigManager.getInstance().removeLanguageObserver(languageListenerId)
            languageListenerId = -1
        }
        providers.forEach { it.clearResIds() }
        providers.clear()
    }

    fun createButton(): RemoteButton = RemoteButton(this).also { trackProvider(it) }

    fun createText(): RemoteText = RemoteText(this).also { trackProvider(it) }

    fun createImage(): RemoteImage = RemoteImage(this).also { trackProvider(it) }

    fun createEditText(): RemoteEditText = RemoteEditText(this).also { trackProvider(it) }

    fun createSwitch(): RemoteSwitch = RemoteSwitch(this).also { trackProvider(it) }

    fun createCheckBox(): RemoteCheckBox = RemoteCheckBox(this).also { trackProvider(it) }

    fun createWebView(): RemoteWebView = RemoteWebView(this).also { trackProvider(it) }

    fun createProgressBar(): RemoteProgressBar = RemoteProgressBar(this).also { trackProvider(it) }

    fun createCircleProgressBar(): RemoteCircleProgressBar = RemoteCircleProgressBar(this).also { trackProvider(it) }

    fun createSpinner(): RemoteSpinner = RemoteSpinner(this).also { trackProvider(it) }

    fun createToggleButton(): RemoteToggleButton = RemoteToggleButton(this).also { trackProvider(it) }

    fun createLinearLayoutVertical(): RemoteLinearLayout =
        RemoteLinearLayout(this, ViewType.LINEAR_LAYOUT_VERTICAL).also { trackProvider(it) }

    fun createLinearLayoutHorizontal(): RemoteLinearLayout =
        RemoteLinearLayout(this, ViewType.LINEAR_LAYOUT_HORIZONTAL).also { trackProvider(it) }

    fun createConstraintLayout(): RemoteConstraintLayout =
        RemoteConstraintLayout(this).also { trackProvider(it) }

    fun createListView(): RemoteListView = RemoteListView(this).also { trackProvider(it) }

    fun margin(all: Int): WidgetLayoutParam {
        val params = WidgetLayoutParam()
        params.marginLeft = all
        params.marginTop = all
        params.marginRight = all
        params.marginBottom = all
        return params
    }

    fun margin(top: Int = 0, bottom: Int = 0, left: Int = 0, right: Int = 0): WidgetLayoutParam {
        val params = WidgetLayoutParam()
        params.marginTop = top
        params.marginBottom = bottom
        params.marginLeft = left
        params.marginRight = right
        return params
    }

    fun getPageType(): ViewPageType = pageType

    fun addToRoot(view: RemoteView) {
        RemoteViewPageManager.getInstance().addView(pageType, view)
    }

    fun addViewToParent(view: RemoteView, parentId: String, params: WidgetLayoutParam?) {
        RemoteViewPageManager.getInstance().addView(pageType, view, parentId, params)
    }

    fun updateView(viewId: String, property: RemoteViewProperty, value: String): OperationResult {
        return RemoteViewPageManager.getInstance().updateView(pageType, viewId, property, value)
    }

    fun setConstraints(layoutId: String, viewId: String, targetId: String, viewDirection: ViewSide, targetDirection: ViewSide, margin: Int = 0): OperationResult {
        return RemoteViewPageManager.getInstance()
            .setConstraints(pageType, layoutId, viewId, targetId, viewDirection, targetDirection, margin)
    }

    /**
     * 清空整个页面的视图，同时注销语言变更订阅。
     * 业务方在重新构建 UI 时，createXxx() 会自动懒注册订阅。
     */
    fun clearLayout() {
        unregisterLanguageListener()
        RemoteViewPageManager.getInstance().clearLayout(pageType)
    }

    /**
     * 清空指定布局容器的子视图，同时注销语言变更订阅。
     * 注意：当前实现会清除所有 providers 的 resId 追踪，
     * 若业务方仅清除部分容器后继续使用其他 provider，需在 clearLayout 后重新设置 ResId。
     */
    fun clearLayout(layoutId: String) {
        unregisterLanguageListener()
        RemoteViewPageManager.getInstance().clearLayout(pageType, layoutId)
    }

    fun setPageLayoutParam(height: Int) {
        val param = dji.open.pilot.base.ui.model.LayoutParam(height)
        RemoteViewPageManager.getInstance().setPageLayoutParam(pageType, param)
    }

    /**
     * 设置悬浮窗高度为自适应模式，根据内容自动调整高度。
     */
    fun setPageLayoutParamWrapContent() {
        setPageLayoutParam(RemoteView.WRAP_CONTENT)
    }

    /**
     * 上传图片文件到 Pilot Server，自动跳过已上传的文件。
     * <p>
     * 上传成功后，可通过文件名在 RemoteImage.setImageFileName() 或 ListItemData.iconFileName 中引用。
     * 注意：必须在上传成功回调后再设置图片文件名，否则 Server 端可能找不到文件。
     *
     * @param filePath 本地文件绝对路径
     * @param callback 上传回调（进度/成功/失败），可为 null
     */
    fun pushImageFile(filePath: String, callback: CommonCallbacks.CompletionCallbackWithProgress<Double>?) {
        val fileName = File(filePath).name
        if (uploadedFiles.contains(fileName)) {
            callback?.onSuccess()
            return
        }
        FileTransferManager.getInstance().pushFileToRemote(filePath,
            object : CommonCallbacks.CompletionCallbackWithProgress<Double> {
                override fun onProgressUpdate(progress: Double) {
                    callback?.onProgressUpdate(progress)
                }

                override fun onSuccess() {
                    uploadedFiles.add(fileName)
                    callback?.onSuccess()
                }

                override fun onFailure(error: IDJIError) {
                    callback?.onFailure(error)
                }
            }
        )
    }

    companion object {
        /**
         * 全局已上传文件名缓存（线程安全），避免同一文件重复上传
         */
        private val uploadedFiles: MutableSet<String> = ConcurrentHashMap.newKeySet()
        private const val  TAG = "RemoteViewHelper"
        /**
         * 悬浮框页面单例。
         * 对应 ViewPageType.FLOATING_BOX，用于在 Pilot 悬浮窗口中绘制自定义 UI。
         */
        @JvmStatic
        val floatingBoxHelper: RemoteViewHelper by lazy { RemoteViewHelper(ViewPageType.FLOATING_BOX) }

        /**
         * Activity 页面单例。
         * 对应 ViewPageType.ACTIVITY，用于在 Pilot 插件 Activity 中绘制自定义 UI。
         */
        @JvmStatic
        val activityHelper: RemoteViewHelper by lazy { RemoteViewHelper(ViewPageType.ACTIVITY) }

        @JvmStatic
        fun obtain(pageType: ViewPageType): RemoteViewHelper = when (pageType) {
            ViewPageType.FLOATING_BOX -> floatingBoxHelper
            ViewPageType.ACTIVITY -> activityHelper
            else -> floatingBoxHelper
        }

        /**
         * 清除全局已上传文件缓存。
         * 调用后后续的 pushImageFile 将重新上传所有文件。
         */
        @JvmStatic
        fun clearUploadCache() {
            uploadedFiles.clear()
        }
    }
}
