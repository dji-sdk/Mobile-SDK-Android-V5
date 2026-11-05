package dji.v5.ux.remoteview

import dji.open.pilot.base.ui.model.EllipsizeType
import dji.open.pilot.base.ui.model.FontStyleType
import dji.open.pilot.base.ui.model.GravityType
import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.base.ui.model.TextDecorationType
import dji.open.pilot.base.ui.model.ViewType

class RemoteText(
    helper: RemoteViewHelper
) : RemoteViewProvider(helper, ViewType.TEXT_VIEW) {

    override fun setText(text: String) {
        super.setText(text)
        propertyMap[RemoteViewProperty.TEXT] = text
    }

    fun setTextSize(size: Float) {
        propertyMap[RemoteViewProperty.TEXT_SIZE] = size.toInt().toString()
    }

    fun setTextColor(color: Int) {
        propertyMap[RemoteViewProperty.TEXT_COLOR] = color.toString()
    }

    fun setGravity(gravity: GravityType) {
        propertyMap[RemoteViewProperty.GRAVITY] = gravity.getValue().toString()
    }

    fun setFontStyle(style: FontStyleType) {
        propertyMap[RemoteViewProperty.FONT_STYLE_TYPE] = style.value.toString()
    }

    fun setVerticalScrollable(scrollable: Boolean) {
        propertyMap[RemoteViewProperty.VERTICAL_SCROLLABLE] = scrollable.toString()
    }

    fun setMaxLines(maxLines: Int) {
        propertyMap[RemoteViewProperty.MAX_LINES] = maxLines.toString()
    }

    fun setEllipsize(ellipsizeType: EllipsizeType) {
        propertyMap[RemoteViewProperty.ELLIPSIZE] = ellipsizeType.value.toString()
    }

    fun setSingleLine(singleLine: Boolean) {
        propertyMap[RemoteViewProperty.SINGLE_LINE] = singleLine.toString()
    }

    fun setLineSpacing(extra: Float, multiplier: Float) {
        propertyMap[RemoteViewProperty.LINE_SPACING] = """{"extra":$extra,"multiplier":$multiplier}"""
    }

    fun setTextDecoration(decorationType: TextDecorationType) {
        propertyMap[RemoteViewProperty.TEXT_DECORATION] = decorationType.value.toString()
    }

    fun setLetterSpacing(spacing: Float) {
        propertyMap[RemoteViewProperty.LETTER_SPACING] = spacing.toString()
    }

    /**
     * 富文本：直接传入一段 HTML 字符串跨进程下发，渲染端用 Html.fromHtml 解析。
     * 样式内联在标签中（如 <font color="#RRGGBB"><b>...</b></font>）。
     * 注意：文案已固定，不随服务端语言切换自动刷新；
     * 需要随语言切换请使用 [setRichTextByResId]。
     */
    fun setRichText(html: String) {
        applyRichHtml(html)
    }

    /**
     * 通过资源 ID 设置富文本，自动适配当前服务端语言，并支持动态样式参数。
     * strings.xml 中用 <![CDATA[...]]> 存 HTML，样式值/数值用位置占位符（如 %1$s、%2$d）。
     * 语言切换时 RemoteViewHelper 会自动回调 updateTextByResId，用新语言模板与同一批参数重建刷新。
     *
     * @param htmlResId 含 HTML 标签与占位符的文案资源 ID（文案片段应写死以保翻译语境，只占位样式值/数值）
     * @param args      String.format 的动态参数（如颜色 "#E41F29"、电量数值 15），可为空
     */
    fun setRichTextByResId(htmlResId: Int, vararg args: Any) {
        applyRichHtmlByResId(htmlResId, args)
    }
}
