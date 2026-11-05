package dji.v5.ux.remoteview

import dji.open.pilot.base.ui.IRemoteViewOnClickListener
import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.base.ui.model.ViewType

class RemoteWebView(
    helper: RemoteViewHelper
) : RemoteViewProvider(helper, ViewType.WEB_VIEW) {

    fun setUrl(url: String) {
        propertyMap[RemoteViewProperty.WEB_VIEW_URL] = url
    }
}
