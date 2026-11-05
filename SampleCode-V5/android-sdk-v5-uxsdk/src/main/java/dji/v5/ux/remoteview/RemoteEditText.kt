package dji.v5.ux.remoteview

import dji.open.pilot.base.ui.IRemoteViewTextWatcher
import dji.open.pilot.base.ui.model.EllipsizeType
import dji.open.pilot.base.ui.model.FontStyleType
import dji.open.pilot.base.ui.model.GravityType
import dji.open.pilot.base.ui.model.InputType
import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.base.ui.model.ViewType
import dji.v5.ux.util.RemoteLangUtils

class RemoteEditText(
    helper: RemoteViewHelper
) : RemoteViewProvider(helper, ViewType.EDIT_TEXT) {

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

    fun setHint(hint: String) {
        propertyMap[RemoteViewProperty.EDIT_TEXT_HINT] = hint
    }

    /**
     * 通过资源 ID 设置输入框提示文案（hint），自动适配当前服务端语言。
     * 语言切换时，RemoteViewHelper 会自动调用 updateTextByResId() 刷新 hint 文案。
     *
     * @param resId 客户端 R.string.xxx 资源 ID
     */
    fun setHintByResId(resId: Int) {
        setResId(RemoteViewProperty.EDIT_TEXT_HINT, resId)
        propertyMap[RemoteViewProperty.EDIT_TEXT_HINT] = RemoteLangUtils.getString(resId)
    }

    fun setCursorPosition(position: Int) {
        propertyMap[RemoteViewProperty.EDIT_TEXT_CURSOR_POSITION] = position.toString()
    }

    fun setGrouping(grouping: String) {
        propertyMap[RemoteViewProperty.EDIT_TEXT_GROUPING] = grouping
    }

    fun setInputType(inputType: InputType) {
        propertyMap[RemoteViewProperty.INPUT_TYPE] = inputType.value.toString()
    }

    fun setMaxLength(maxLength: Int) {
        propertyMap[RemoteViewProperty.MAX_LENGTH] = maxLength.toString()
    }

    fun setMaxLines(maxLines: Int) {
        propertyMap[RemoteViewProperty.MAX_LINES] = maxLines.toString()
    }

    fun setEllipsize(ellipsizeType: EllipsizeType) {
        propertyMap[RemoteViewProperty.ELLIPSIZE] = ellipsizeType.value.toString()
    }

    fun setOnTextChangeListener(listener: IRemoteViewTextWatcher) {
        remoteView.onTextChangeListener = listener
    }

    fun setSingleLine(singleLine: Boolean) {
        propertyMap[RemoteViewProperty.SINGLE_LINE] = singleLine.toString()
    }

    fun setHintTextColor(color: Int) {
        propertyMap[RemoteViewProperty.HINT_TEXT_COLOR] = color.toString()
    }
}
