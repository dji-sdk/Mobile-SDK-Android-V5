package dji.v5.ux.remoteview

import dji.open.pilot.base.ui.IRemoteViewOnItemSelectedListener
import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.FontStyleType
import dji.open.pilot.base.ui.model.GravityType
import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.base.ui.model.ViewType
import dji.v5.ux.util.RemoteLangUtils

class RemoteSpinner(
    helper: RemoteViewHelper
) : RemoteViewProvider(helper, ViewType.SPINNER) {

    /** 存储通过 setItemsByResIds 设置的资源 ID 列表（Spinner items 为列表，单独处理） */
    private var itemResIds: List<Int>? = null

    /**
     * 设置下拉框选项列表
     * @param items 选项文本列表
     */
    fun setItems(items: List<String>) {
        propertyMap[RemoteViewProperty.SPINNER_ITEMS] = items.joinToString(RemoteView.ITEMS_SEPARATOR)
    }

    /**
     * 通过资源 ID 列表设置下拉框选项，自动适配当前服务端语言。
     * 语言切换时，RemoteViewHelper 会自动调用 updateTextByResId() 刷新所有选项文案。
     *
     * @param resIds 客户端 R.string.xxx 资源 ID 列表
     */
    fun setItemsByResIds(resIds: List<Int>) {
        itemResIds = resIds
        setItems(resIds.map { RemoteLangUtils.getString(it) })
    }

    /**
     * 覆写 updateTextByResId，特殊处理 itemResIds 列表场景。
     */
    override fun updateTextByResId() {
        itemResIds?.let { resIds ->
            setItems(resIds.map { RemoteLangUtils.getString(it) })
            syncToRemote()
        }
        super.updateTextByResId()
    }

    /**
     * 设置当前选中项索引
     * @param index 选中项索引（从0开始）
     */
    fun setSelectedIndex(index: Int) {
        propertyMap[RemoteViewProperty.SPINNER_SELECTED_INDEX] = index.toString()
    }

    /**
     * 设置下拉弹窗背景颜色（ARGB 整型）。
     * 仅影响弹出的 dropdown 列表区域背景，不影响 spinner 头部。
     */
    fun setDropdownBackgroundColor(color: Int) {
        propertyMap[RemoteViewProperty.SPINNER_DROPDOWN_BG_COLOR] = color.toString()
    }

    /**
     * 设置下拉弹窗中选项文字的颜色（ARGB 整型）。
     */
    fun setDropdownTextColor(color: Int) {
        propertyMap[RemoteViewProperty.SPINNER_DROPDOWN_TEXT_COLOR] = color.toString()
    }

    /**
     * 设置下拉弹窗选中项文字的颜色（ARGB 整型）。
     */
    fun setSelectedTextColor(color: Int) {
        propertyMap[RemoteViewProperty.SPINNER_SELECTED_TEXT_COLOR] = color.toString()
    }

    /**
     * 设置 spinner 头部（已选中项）显示文字的大小（sp）。
     * 注意：当前服务端尚未支持对 Spinner 头部应用此属性，暂设为 private。
     */
    private fun setTextSize(size: Float) {
        propertyMap[RemoteViewProperty.TEXT_SIZE] = size.toInt().toString()
    }

    /**
     * 设置 spinner 头部（已选中项）显示文字的颜色（ARGB 整型）。
     * 注意：当前服务端尚未支持对 Spinner 头部应用此属性，暂设为 private。
     */
    private fun setTextColor(color: Int) {
        propertyMap[RemoteViewProperty.TEXT_COLOR] = color.toString()
    }

    /**
     * 设置 spinner 右侧图标的颜色（ARGB 整型）。
     */
    fun setSpinnerIconColor(color: Int) {
        propertyMap[RemoteViewProperty.SPINNER_ICON_COLOR] = color.toString()
    }

    /**
     * 设置 spinner 头部文字的对齐方式。
     * 注意：当前服务端尚未支持对 Spinner 头部应用此属性，暂设为 private。
     */
    fun setGravity(gravity: GravityType) {
        propertyMap[RemoteViewProperty.GRAVITY] = gravity.value.toString()
    }

    /**
     * 设置 spinner 头部文字的字体样式（加粗 / 斜体 / 常规）。
     * 注意：当前服务端尚未支持对 Spinner 头部应用此属性，暂设为 private。
     */
    private fun setFontStyle(style: FontStyleType) {
        propertyMap[RemoteViewProperty.FONT_STYLE_TYPE] = style.value.toString()
    }

    /**
     * 设置选项选中事件订阅器
     * @param listener 选项选中订阅器
     */
    fun setOnItemSelectedListener(listener: IRemoteViewOnItemSelectedListener) {
        remoteView.onItemSelectedListener = listener
    }
}
