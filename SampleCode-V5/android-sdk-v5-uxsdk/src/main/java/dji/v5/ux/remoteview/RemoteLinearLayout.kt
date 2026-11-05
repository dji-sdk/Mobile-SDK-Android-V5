package dji.v5.ux.remoteview

import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.ViewSide
import dji.open.pilot.base.ui.model.ViewType

class RemoteLinearLayout(
    helper: RemoteViewHelper,
    viewType: ViewType
) : RemoteLayout(helper, viewType) {

    fun setConstraintsOnRoot(viewDirection: ViewSide, targetDirection: ViewSide, margin: Int = 0) {
        helper.setConstraints(RemoteView.PARENT_ID, remoteView.id, RemoteView.PARENT_ID, viewDirection, targetDirection, margin)
    }
}
