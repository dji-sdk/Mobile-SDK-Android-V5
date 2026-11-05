package com.dji.open.pilot.plugin.common

/**
 * 绘制相关的公共颜色和尺寸常量。
 *
 * 各 Drawer 中散落的重复常量（如 CUSTOM_GREEN、CORNER_DEFAULT）
 * 统一收敛到此处，避免多处定义不一致。
 *
 * TODO: 后续重构各 Drawer 文件中的局部常量，改为引用此类。
 */
object DrawingConstants {

    // ─── 圆角 ─────────────────────────────────────────────────────────────
    const val CORNER_DEFAULT = 4f
    const val CORNER_LARGE   = 24f

    // ─── 通用颜色 ─────────────────────────────────────────────────────────
    /** 卡片背景色（20% 白色透明） */
    const val COLOR_CARD_BG = 0x28FFFFFF

    /** 主色蓝（按钮/链接） */
    const val COLOR_PRIMARY_BLUE = 0xFF2696FF.toInt()

    /** 自定义绿（ButtonDrawer / EditTextDrawer 共用） */
    const val COLOR_CUSTOM_GREEN = 0xFF00C774.toInt()

    // ─── LayoutDrawer 专用 ────────────────────────────────────────────────
    const val COLOR_HEADER           = 0xFF4196FF.toInt()
    const val COLOR_CONTENT_BG       = 0xFF0659D9.toInt()
    const val COLOR_CONTENT          = 0x26FFFFFF
    const val COLOR_BUTTON           = 0xFF2696FF.toInt()

    // ─── TextViewDrawer 专用 ──────────────────────────────────────────────
    const val COLOR_BLUE   = 0xFF2196F3.toInt()
    const val COLOR_RED    = 0xFFF44336.toInt()
    const val COLOR_ORANGE = 0xFFFF9800.toInt()
}
