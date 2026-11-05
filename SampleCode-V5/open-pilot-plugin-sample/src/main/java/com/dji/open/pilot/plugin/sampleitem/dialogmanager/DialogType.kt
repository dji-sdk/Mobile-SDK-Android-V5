package com.dji.open.pilot.plugin.sampleitem.dialogmanager

import com.dji.open.pilot.plugin.R

/**
 * 对话框演示模块的类型枚举。
 * 每个类型对应 Demo 页左侧菜单的一项，以及悬浮窗中的一个"弹出"按钮。
 *
 * titleRes     = 菜单主标题
 * descRes      = 菜单副标题（中文描述）
 * displayName  = 展示名称
 * interfaceRef = 该用例对应的开源接口类相对路径（源码位置提示，统一展示在"查看演示效果按钮"处）
 * sampleRef    = 本 Demo 实际调用该用例的源码相对路径
 * previewResId = 右侧效果预览图 drawable 资源 ID（0 表示暂无预览图，界面保持原样留空）
 */
enum class DialogType(
    val titleRes: Int,
    val descRes: Int,
    val displayName: String,
    val interfaceRef: String,
    val sampleRef: String,
    val previewResId: Int
) {
    ALERT(
        R.string.dialog_type_alert_title,
        R.string.dialog_type_alert_desc,
        "提示框",
        "dji/open/pilot/client/interfaces/IRemoteDialogManager.java",
        "sampleitem/dialogmanager/DialogDemoViewModel.kt",
        R.drawable.preview_dialog_alert
    ),
    CONFIRM(
        R.string.dialog_type_confirm_title,
        R.string.dialog_type_confirm_desc,
        "确认框",
        "dji/open/pilot/client/interfaces/IRemoteDialogManager.java",
        "sampleitem/dialogmanager/DialogDemoViewModel.kt",
        R.drawable.preview_dialog_confirm
    ),
    PROMPT(
        R.string.dialog_type_prompt_title,
        R.string.dialog_type_prompt_desc,
        "输入框",
        "dji/open/pilot/client/interfaces/IRemoteDialogManager.java",
        "sampleitem/dialogmanager/DialogDemoViewModel.kt",
        R.drawable.preview_dialog_prompt
    ),
    SINGLE_CHOICE(
        R.string.dialog_type_single_title,
        R.string.dialog_type_single_desc,
        "单选框",
        "dji/open/pilot/client/interfaces/IRemoteDialogManager.java",
        "sampleitem/dialogmanager/DialogDemoViewModel.kt",
        R.drawable.preview_dialog_single
    ),
    MULTI_CHOICE(
        R.string.dialog_type_multi_title,
        R.string.dialog_type_multi_desc,
        "多选框",
        "dji/open/pilot/client/interfaces/IRemoteDialogManager.java",
        "sampleitem/dialogmanager/DialogDemoViewModel.kt",
        R.drawable.preview_dialog_multi
    );
}
