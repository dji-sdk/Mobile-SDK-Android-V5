package dji.v5.ux.remoteview

import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.base.ui.model.ScaleType
import dji.open.pilot.base.ui.model.ViewType

class RemoteImage(
    helper: RemoteViewHelper
) : RemoteViewProvider(helper, ViewType.IMAGE_VIEW) {

    override fun setImageFileName(imageFileName: String) {
        super.setImageFileName(imageFileName)
        propertyMap[RemoteViewProperty.IMAGE] = imageFileName
    }

    fun setScaleType(scaleType: ScaleType) {
        propertyMap[RemoteViewProperty.SCALE_TYPE] = scaleType.value.toString()
    }

    fun setImageTint(color: Int) {
        propertyMap[RemoteViewProperty.IMAGE_TINT] = color.toString()
    }
}
