package com.dji.open.pilot.plugin.sampleitem.remoteviewpage.drawing

import com.dji.open.pilot.plugin.common.drawing.DrawingContext

import dji.v5.ux.remoteview.RemoteLinearLayout

/**
 * 组件绘制器统一接口。
 *
 * 所有 XxxDrawer object 实现此接口，ViewModel 通过注册表派发绘制任务，
 * 新增组件只需实现接口并注册，无需修改 when 分支。
 */
interface IComponentDrawer {

    /**
     * 将该组件的全部演示内容绘制到 [container] 中。
     *
     * @param ctx       共享绘制上下文，封装 RemoteViewHelper 和公共工具函数
     * @param container 目标容器，由 ViewModel 预创建并管理可见性
     */
    fun draw(ctx: DrawingContext, container: RemoteLinearLayout)
}
