package com.dji.open.pilot.plugin.common.drawing

/**
 * 进程级单例，标识当前悬浮窗内容属于哪个模块。
 *
 * 由于 [dji.v5.ux.remoteview.RemoteViewHelper.floatingBoxHelper] 是进程级单例，
 * 多个模块共享同一个悬浮窗。当模块 A 绘制悬浮窗后，模块 B 再绘制会覆盖 A 的内容。
 * 如果用户返回模块 A，A 的 ViewModel（也是进程级单例）中 `isLayoutInitialized` 仍为 true，
 * 导致不会重新绘制，悬浮窗停留在模块 B 的内容。
 *
 * 使用方式：
 * 1. 每个模块在 `initAllLayouts()` 完成后调用 [claim] 声明所有权
 * 2. 每个模块在 `drawAndNavigate()` 开头调用 [isOwnedBy] 检查是否需要重绘
 * 3. 如果不属于自己，重置模块内的 `initialized` 标记后重新调用 `initAllLayouts()`
 */
object FloatingWindowOwner {

    @Volatile
    private var currentTag: String? = null

    /** 声明悬浮窗所有权 */
    fun claim(tag: String) {
        currentTag = tag
    }

    /** 检查悬浮窗是否属于指定模块 */
    fun isOwnedBy(tag: String): Boolean = currentTag == tag
}
