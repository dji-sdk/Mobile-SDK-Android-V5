package dji.v5.ux.remoteview

import android.text.Html
import dji.open.pilot.base.ui.IRemoteViewOnClickListener
import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.OperationResult
import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.base.ui.model.ShapeDescriptor
import dji.open.pilot.base.ui.model.ViewType
import dji.open.pilot.base.ui.model.VisibilityType
import dji.v5.utils.common.LogPath
import dji.v5.utils.common.LogUtils
import dji.v5.ux.util.RemoteLangUtils

abstract class RemoteViewProvider protected constructor(protected val helper: RemoteViewHelper, type: ViewType) {

    internal val remoteView: RemoteView = RemoteView(type, "", RemoteView.WRAP_CONTENT, RemoteView.WRAP_CONTENT, 0, "")

    protected val propertyMap = mutableMapOf<RemoteViewProperty, String>()

    private val resIdMap = mutableMapOf<RemoteViewProperty, Int>()

    open fun getId(): String = remoteView.id

    open fun setText(text: String) {
        clearRichTextRegistration()
        resIdMap.remove(RemoteViewProperty.TEXT)
        remoteView.text = text
        propertyMap[RemoteViewProperty.TEXT] = text
    }

    open fun setWidth(width: Int) {
        remoteView.width = width
    }

    open fun setHeight(height: Int) {
        remoteView.height = height
    }

    open fun setBackground(color: Int) {
        remoteView.background = color
        propertyMap[RemoteViewProperty.BACKGROUND_COLOR] = color.toString()
    }

    open fun setImageFileName(imageFileName: String) {
        remoteView.setImage(imageFileName)
    }

    open fun setOnClickListener(listener: IRemoteViewOnClickListener) {
        remoteView.onClickListener = listener
    }

    open fun setVisibility(visibility: VisibilityType) {
        propertyMap[RemoteViewProperty.VISIBILITY] = visibility.value.toString()
    }

    open fun setEnabled(enabled: Boolean) {
        propertyMap[RemoteViewProperty.ENABLED] = enabled.toString()
    }

    open fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        propertyMap[RemoteViewProperty.PADDING] = "$left,$top,$right,$bottom"
    }

    open fun setBackgroundShape(shape: ShapeDescriptor) {
        propertyMap[RemoteViewProperty.BACKGROUND_SHAPE] = shape.toJson()
    }

    open fun setAlpha(alpha: Float) {
        propertyMap[RemoteViewProperty.ALPHA] = alpha.toString()
    }

    open fun setMinSize(minWidth: Int, minHeight: Int) {
        propertyMap[RemoteViewProperty.MIN_SIZE] = "$minWidth,$minHeight"
    }

    open fun syncToRemote(): OperationResult {
        var result = OperationResult.SUCCESS
        propertyMap.forEach {
            val operationResult = helper.updateView(remoteView.id, it.key, it.value)
            if (operationResult != OperationResult.SUCCESS){
                result = OperationResult.EXECUTION_FAILED
            }
            LogUtils.i(LogPath.SAMPLE, "syncToRemote: id=${remoteView.id}, property=${it.key}, value=${it.value}, result=$operationResult")
        }
        propertyMap.clear()
        return result
    }

    // ──────多语言 ResId 支持───────────────

    /**
     * 设置文案资源 ID，key 对齐 propertyMap 的 RemoteViewProperty。
     * 子类应封装为语义化方法（如 setTextByResId、setHintByResId）。
     *
     * @param property 对应的视图属性（如 RemoteViewProperty.TEXT）
     * @param resId    客户端 R.string.xxx 资源 ID
     */
    fun setResId(property: RemoteViewProperty, resId: Int) {
        if (property == RemoteViewProperty.TEXT) clearRichTextRegistration()
        resIdMap[property] = resId
    }

    open fun setTextByResId(resId: Int) {
        setText(RemoteLangUtils.getString(resId))
        resIdMap[RemoteViewProperty.TEXT] = resId
    }

    open fun updateTextByResId() {
        val richChanged = rebuildRichHtml()
        if (resIdMap.isEmpty()) {
            if (richChanged) syncToRemote()
            return
        }
        resIdMap.forEach { (property, resId) ->
            propertyMap[property] = RemoteLangUtils.getString(resId)
        }
        syncToRemote()
    }

    /**
     * 清除所有已注册的资源 ID。
     */
    open fun clearResIds() {
        resIdMap.clear()
        richHtmlResId = null
        richHtmlArgs = null
    }

    fun hasResIds(): Boolean = resIdMap.isNotEmpty() || richHtmlResId != null

    // ──────富文本 HTML 重建内核（仅 RemoteText / RemoteButton 暴露 public 入口）───────────────
    // 5.20.1 旧协议不支持富文本属性，HTML 在本地转为纯文本，通过 TEXT 更新。
    // public setRichText / setRichTextByResId 只在 RemoteText / RemoteButton 暴露。

    private var richHtmlResId: Int? = null
    private var richHtmlArgs: Array<out Any>? = null

    /**
     * 将成品 HTML 降级为纯文本（文案已固定，不随语言切换刷新）。
     */
    protected fun applyRichHtml(html: String) {
        clearPlainTextRegistration()
        richHtmlResId = null
        richHtmlArgs = null
        updateRichHtmlText(html)
    }

    /**
     * 登记 HTML 文案 resId + 动态参数，用当前语言 format 构造富文本；
     * 语言切换时经 [updateTextByResId] → [rebuildRichHtml] 用新语言模板与同一批参数重建刷新。
     *
     * @param htmlResId 含 HTML 标签（CDATA 存储）与占位符的文案资源 ID
     * @param args      String.format 的动态参数（如颜色 "#RRGGBB"、数值），可为空
     */
    protected fun applyRichHtmlByResId(htmlResId: Int, args: Array<out Any>) {
        clearPlainTextRegistration()
        richHtmlResId = htmlResId
        richHtmlArgs = args
        updateRichHtmlText(formatRichHtml(htmlResId, args))
    }

    private fun clearRichTextRegistration() {
        richHtmlResId = null
        richHtmlArgs = null
    }

    private fun clearPlainTextRegistration() {
        resIdMap.remove(RemoteViewProperty.TEXT)
        propertyMap.remove(RemoteViewProperty.TEXT)
        remoteView.text = ""
    }

    private fun formatRichHtml(resId: Int, args: Array<out Any>): String {
        val template = RemoteLangUtils.getString(resId)
        return if (args.isEmpty()) template else String.format(template, *args)
    }

    private fun updateRichHtmlText(html: String) {
        val text = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()
        remoteView.text = text
        propertyMap[RemoteViewProperty.TEXT] = text
    }

    /**
     * 若登记过富文本 resId，则用当前语言模板与登记参数重建纯文本 TEXT value。
     * @return 是否发生了重建（用于决定是否需要 syncToRemote）
     */
    private fun rebuildRichHtml(): Boolean {
        val resId = richHtmlResId ?: return false
        updateRichHtmlText(formatRichHtml(resId, richHtmlArgs ?: emptyArray()))
        return true
    }
}
