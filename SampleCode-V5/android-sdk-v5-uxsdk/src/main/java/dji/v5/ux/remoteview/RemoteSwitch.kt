package dji.v5.ux.remoteview

import dji.open.pilot.base.ui.IRemoteViewOnCheckedChangeListener
import dji.open.pilot.base.ui.model.FontStyleType
import dji.open.pilot.base.ui.model.GravityType
import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.base.ui.model.ViewType

class RemoteSwitch(
    helper: RemoteViewHelper
) : RemoteViewProvider(helper, ViewType.SWITCH) {

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

    fun setOnCheckChangeListener(listener: IRemoteViewOnCheckedChangeListener) {
        remoteView.onCheckChangeListener = listener
    }

    fun setThumbColor(color: Int) {
        propertyMap[RemoteViewProperty.SWITCH_THUMB_COLOR] = color.toString()
    }

    fun setTrackColor(color: Int) {
        propertyMap[RemoteViewProperty.SWITCH_TRACK_COLOR] = color.toString()
    }
}
