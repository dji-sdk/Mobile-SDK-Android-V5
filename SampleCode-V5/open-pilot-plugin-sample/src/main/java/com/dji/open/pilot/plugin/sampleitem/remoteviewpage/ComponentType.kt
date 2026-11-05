package com.dji.open.pilot.plugin.sampleitem.remoteviewpage

import com.dji.open.pilot.plugin.R

/**
 * 自定义浮层 UI 演示的组件类型枚举。
 * 枚举顺序与 ComponentMenuAdapter 中菜单分组顺序一致（基础组件 → 数据录入 → 数据展示）。
 *
 * titleRes    = 菜单主标题（组件类名）
 * descRes     = 菜单副标题（复用 comp_nav_* 功能名，多语言资源 ID）
 * bgStart / bgEnd = 图标渐变色
 * displayName = Pilot 悬浮窗顶部切换栏显示的名称（多语言资源 ID）
 */
enum class ComponentType(
    val titleRes: Int,
    val descRes: Int,
    val bgStart: Int,
    val bgEnd: Int,
    val displayNameRes: Int,
    val componentRef: String,
    val drawerRef: String
) {
    // ── 基础组件 ─────────────────────────────────────────────────────────────
    LAYOUT(
        R.string.comp_layout_title,
        R.string.comp_nav_layout,
        0xFF8B9FFF.toInt(),
        0xFF5B6AF7.toInt(),
        R.string.comp_nav_layout,
        "dji/v5/ux/remoteview/RemoteLinearLayout.kt",
        "drawing/LayoutDrawer.kt"
    ),
    TEXT_VIEW(
        R.string.comp_textview_title,
        R.string.comp_nav_text,
        0xFF6BCBA4.toInt(),
        0xFF28A856.toInt(),
        R.string.comp_nav_text,
        "dji/v5/ux/remoteview/RemoteTextView.kt",
        "drawing/TextViewDrawer.kt"
    ),
    BUTTON(
        R.string.comp_button_title,
        R.string.comp_nav_button,
        0xFFFFD54F.toInt(),
        0xFFF57F17.toInt(),
        R.string.comp_nav_button,
        "dji/v5/ux/remoteview/RemoteButton.kt",
        "drawing/ButtonDrawer.kt"
    ),

    // ── 数据录入 ─────────────────────────────────────────────────────────────
    EDIT_TEXT(
        R.string.comp_edittext_title,
        R.string.comp_nav_edittext,
        0xFFA5D6A7.toInt(),
        0xFF2E7D32.toInt(),
        R.string.comp_nav_edittext,
        "dji/v5/ux/remoteview/RemoteEditText.kt",
        "drawing/EditTextDrawer.kt"
    ),
    CHECK_BOX(
        R.string.comp_checkbox_title,
        R.string.comp_nav_checkbox,
        0xFFEF9A9A.toInt(),
        0xFFC62828.toInt(),
        R.string.comp_nav_checkbox,
        "dji/v5/ux/remoteview/RemoteCheckBox.kt",
        "drawing/CheckBoxDrawer.kt"
    ),
    SWITCH(
        R.string.comp_switch_title,
        R.string.comp_nav_switch,
        0xFFB39DDB.toInt(),
        0xFF6A1B9A.toInt(),
        R.string.comp_nav_switch,
        "dji/v5/ux/remoteview/RemoteSwitch.kt",
        "drawing/SwitchDrawer.kt"
    ),
    SPINNER(
        R.string.comp_spinner_title,
        R.string.comp_nav_spinner,
        0xFF90CAF9.toInt(),
        0xFF1565C0.toInt(),
        R.string.comp_nav_spinner,
        "dji/v5/ux/remoteview/RemoteSpinner.kt",
        "drawing/SpinnerDrawer.kt"
    ),

    // ── 数据展示 ─────────────────────────────────────────────────────────────
    IMAGE_VIEW(
        R.string.comp_imageview_title,
        R.string.comp_nav_imageview,
        0xFFFFB347.toInt(),
        0xFFE08000.toInt(),
        R.string.comp_nav_imageview,
        "dji/v5/ux/remoteview/RemoteImageView.kt",
        "drawing/ImageViewDrawer.kt"
    ),
    WEB_VIEW(
        R.string.comp_webview_title,
        R.string.comp_nav_webview,
        0xFF80DEEA.toInt(),
        0xFF0097A7.toInt(),
        R.string.comp_nav_webview,
        "dji/v5/ux/remoteview/RemoteWebView.kt",
        "drawing/WebViewDrawer.kt"
    ),
    LIST_VIEW(
        R.string.comp_listview_title,
        R.string.comp_nav_listview,
        0xFFCE93D8.toInt(),
        0xFF8E24AA.toInt(),
        R.string.comp_nav_listview,
        "dji/v5/ux/remoteview/RemoteListView.kt",
        "drawing/ListViewDrawer.kt"
    ),
    PROGRESS_BAR(
        R.string.comp_progressbar_title,
        R.string.comp_nav_progressbar,
        0xFFFF8A80.toInt(),
        0xFFE03030.toInt(),
        R.string.comp_nav_progressbar,
        "dji/v5/ux/remoteview/RemoteProgressBar.kt",
        "drawing/ProgressBarDrawer.kt"
    )
}
