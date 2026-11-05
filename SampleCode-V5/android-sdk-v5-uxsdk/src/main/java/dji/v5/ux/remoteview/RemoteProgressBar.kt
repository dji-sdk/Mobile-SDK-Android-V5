package dji.v5.ux.remoteview

import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.base.ui.model.ViewType

class RemoteProgressBar(
    helper: RemoteViewHelper
) : RemoteViewProvider(helper, ViewType.PROGRESS_BAR) {

    fun setProgress(progress: Int) {
        propertyMap[RemoteViewProperty.PROGRESS] = progress.toString()
    }

    fun setMax(max: Int) {
        propertyMap[RemoteViewProperty.PROGRESS_MAX] = max.toString()
    }

    fun setProgressColor(color: Int) {
        propertyMap[RemoteViewProperty.PROGRESS_COLOR] = color.toString()
    }
}
