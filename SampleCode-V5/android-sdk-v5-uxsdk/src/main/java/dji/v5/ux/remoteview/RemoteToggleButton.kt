package dji.v5.ux.remoteview

import dji.open.pilot.base.ui.IRemoteViewOnCheckedChangeListener
import dji.open.pilot.base.ui.model.FontStyleType
import dji.open.pilot.base.ui.model.GravityType
import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.base.ui.model.ViewType

/**
 * ToggleButton 封装类，对应 [ViewType.TOGGLE_BUTTON]。
 *
 * ToggleButton 是具有 ON/OFF 两种状态的切换按钮。
 *
 * 支持属性：
 *   - 通用：TEXT、TEXT_SIZE、TEXT_COLOR、GRAVITY、FONT_STYLE_TYPE、
 *           BACKGROUND_COLOR、BACKGROUND_SHAPE、VISIBILITY、ENABLED、
 *           PADDING、ALPHA、MIN_SIZE
 *   - 专属：CHECKED（选中/取消选中状态）
 */
class RemoteToggleButton(
    helper: RemoteViewHelper
) : RemoteViewProvider(helper, ViewType.TOGGLE_BUTTON) {

    // ── 文字属性 ──────────────────────────────────────────────────────────────

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

    fun setChecked(checked: Boolean) {
        propertyMap[RemoteViewProperty.CHECKED] = checked.toString()
    }

    /**
     * 注册选中状态变化订阅器。
     * 当用户点击 ToggleButton 切换状态时触发。
     */
    fun setOnCheckChangeListener(listener: IRemoteViewOnCheckedChangeListener) {
        remoteView.onCheckChangeListener = listener
    }
}
