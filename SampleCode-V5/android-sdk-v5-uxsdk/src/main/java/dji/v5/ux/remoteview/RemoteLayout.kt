package dji.v5.ux.remoteview

import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.GravityType
import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.base.ui.model.ViewType
import dji.open.pilot.base.ui.model.WidgetLayoutParam

/**
 * 布局容器基类，封装子 View 的添加和自动属性同步逻辑
 */
abstract class RemoteLayout(
    helper: RemoteViewHelper,
    viewType: ViewType
) : RemoteViewProvider(helper, viewType) {

    /**
     * 设置布局内子 View 的整体排列对齐方式
     * @param gravity 对齐方式，如 GravityType.CENTER_HORIZONTAL、GravityType.START、GravityType.END
     */
    fun setGravity(gravity: GravityType) {
        propertyMap[RemoteViewProperty.GRAVITY] = gravity.value.toString()
    }

    /**
     * 添加当前布局到根布局，并同步自身初始属性
     * @param params 布局参数（可选）
     */
    fun addToRoot(params: WidgetLayoutParam? = null) {
        helper.addViewToParent(remoteView, RemoteView.PARENT_ID, params)
        syncToRemote()
    }

    /**
     * 添加子 View 到当前布局，并自动同步子 View 的初始属性
     * @param child 子 View
     */
    fun addToRemote(child: RemoteViewProvider) {
        addToRemote(child, null)
    }

    /**
     * 添加子 View 到当前布局，并自动同步子 View 的初始属性
     * @param child 子 View
     * @param params 布局参数（可选）
     */
    fun addToRemote(child: RemoteViewProvider, params: WidgetLayoutParam?) {
        helper.addViewToParent(child.remoteView, remoteView.id, params)
        child.syncToRemote()
    }
}
