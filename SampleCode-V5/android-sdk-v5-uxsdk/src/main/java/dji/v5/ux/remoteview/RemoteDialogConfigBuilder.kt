package dji.v5.ux.remoteview

import dji.open.pilot.base.manager.ui.config.DialogConfig
import dji.open.pilot.base.manager.ui.config.DialogElementConfig
import dji.open.pilot.base.ui.IDialogMessageListener
import dji.open.pilot.base.ui.IOpenViewOnClickListener
import dji.v5.ux.util.RemoteLangUtils

/**
 * 支持多语言的 DialogConfig 构建器，对齐 RemoteViewProvider 的封装风格。
 *
 * 通过 setXxxByResId 方法设置文案，构建时自动根据当前服务端语言解析对应文案。
 * 对话框文案在 build() 时一次性解析，不订阅后续语言变更（如需重新显示对话框，重新 build 即可）。
 *
 * 使用示例：
 * ```kotlin
 * val config = RemoteDialogConfigBuilder()
 *     .setTitleByResId(R.string.dialog_title)
 *     .setMessageByResId(R.string.dialog_message)
 *     .setStartButtonTextByResId(R.string.btn_cancel)
 *     .setEndButtonTextByResId(R.string.btn_confirm)
 *     .setOnEndButtonClick { RemoteDialogManager.getInstance().dismiss() }
 *     .build()
 * RemoteDialogManager.getInstance().showConfirmDialog(config)
 * ```
 */
class RemoteDialogConfigBuilder {

    private val builder = DialogConfig.Builder()

    // ── 文案设置（直接传字符串）──────────────────────────────

    fun setTitle(title: String): RemoteDialogConfigBuilder {
        builder.setTitle(title)
        return this
    }

    fun setMessage(message: String): RemoteDialogConfigBuilder {
        builder.setMessage(message)
        return this
    }

    fun setStartButtonText(text: String): RemoteDialogConfigBuilder {
        builder.setStartButtonText(text)
        return this
    }

    fun setEndButtonText(text: String): RemoteDialogConfigBuilder {
        builder.setEndButtonText(text)
        return this
    }

    // ── 文案设置（通过 ResId，自动适配当前语言）──────────────

    /**
     * 通过资源 ID 设置对话框标题，构建时自动解析当前服务端语言对应文案。
     */
    fun setTitleByResId(resId: Int): RemoteDialogConfigBuilder {
        builder.setTitle(RemoteLangUtils.getString(resId))
        return this
    }

    /**
     * 通过资源 ID 设置对话框消息内容，构建时自动解析当前服务端语言对应文案。
     */
    fun setMessageByResId(resId: Int): RemoteDialogConfigBuilder {
        builder.setMessage(RemoteLangUtils.getString(resId))
        return this
    }

    /**
     * 通过资源 ID 设置左侧按钮文案（通常为取消/否定操作）。
     */
    fun setStartButtonTextByResId(resId: Int): RemoteDialogConfigBuilder {
        builder.setStartButtonText(RemoteLangUtils.getString(resId))
        return this
    }

    /**
     * 通过资源 ID 设置右侧按钮文案（通常为确认/肯定操作）。
     */
    fun setEndButtonTextByResId(resId: Int): RemoteDialogConfigBuilder {
        builder.setEndButtonText(RemoteLangUtils.getString(resId))
        return this
    }

    // ── 其他配置（透传到 DialogConfig.Builder）──────────────

    fun setIconFileName(fileName: String): RemoteDialogConfigBuilder {
        builder.setIconFileName(fileName)
        return this
    }

    fun setElementConfigs(configs: List<DialogElementConfig>): RemoteDialogConfigBuilder {
        builder.setElementConfigs(configs)
        return this
    }

    fun setOnStartButtonClick(listener: IOpenViewOnClickListener): RemoteDialogConfigBuilder {
        builder.setOnStartButtonClick(listener)
        return this
    }

    fun setOnEndButtonClick(listener: IOpenViewOnClickListener): RemoteDialogConfigBuilder {
        builder.setOnEndButtonClick(listener)
        return this
    }

    fun setOnDismiss(listener: IOpenViewOnClickListener): RemoteDialogConfigBuilder {
        builder.setOnDismiss(listener)
        return this
    }

    fun setOnMessage(listener: IDialogMessageListener): RemoteDialogConfigBuilder {
        builder.setOnMessage(listener)
        return this
    }

    fun build(): DialogConfig = builder.build()
}
