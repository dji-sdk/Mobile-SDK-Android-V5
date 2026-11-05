package com.dji.open.pilot.plugin.main

/**
 * 主界面模块卡片数据模型。
 *
 * @param iconResId     模块图标 drawable 资源 ID
 * @param interfaceName AIDL 接口类名标签，如 "IRemoteViewPageManager"（数据保留，当前 UI 隐藏）
 * @param titleResId    模块名称字符串资源 ID
 * @param descResId     一句话说明字符串资源 ID
 * @param thumbResId    效果截图 drawable 资源 ID，为 0 时显示占位框（数据保留，当前 UI 隐藏）
 * @param status        Demo 实现状态（数据保留，当前 UI 隐藏）
 * @param demoClass     点击后跳转的 Activity Class，为 null 时不可点击（待开发）
 */
data class ModuleItem(
    val iconResId: Int,
    val interfaceName: String,
    val titleResId: Int,
    val descResId: Int,
    val thumbResId: Int = 0,
    val status: DemoStatus,
    val demoClass: Class<*>? = null
)

enum class DemoStatus {
    DONE,   // 已实现
    WIP,    // 开发中
    TODO    // 待开发
}
