package com.dji.open.pilot.plugin.sampleitem.dialogmanager

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.NavigationHelper
import com.dji.open.pilot.plugin.common.drawing.DrawingContext
import com.dji.open.pilot.plugin.common.drawing.FloatingWindowOwner
import com.dji.open.pilot.plugin.main.PluginApplication
import dji.open.pilot.base.manager.ui.config.DialogButtonPosition
import dji.open.pilot.base.manager.ui.config.DialogElementConfig
import dji.open.pilot.base.manager.ui.config.DialogMessage
import dji.open.pilot.base.ui.IDialogMessageListener
import dji.open.pilot.base.ui.IOpenViewOnClickListener
import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.OperationResult
import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.client.manager.ui.RemoteDialogManager
import dji.v5.ux.remoteview.RemoteDialogConfigBuilder
import dji.v5.ux.remoteview.RemoteViewHelper
import dji.v5.ux.util.RemoteViewColors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 对话框 Demo 调度层 ViewModel。
 *
 * 职责：
 * - 悬浮窗单页绘制：结果区 + 5 个"弹出"按钮（提示/确认/输入/单选/多选）
 * - 5 种对话框 API 调用（RemoteDialogManager.getInstance().showXxxDialog）
 * - 回调结果回写悬浮窗结果 TextView
 *
 * 对话框文案与机型列表沿用 android-sdk-v5-sample 的 DialogServiceVM。
 */
class DialogDemoViewModel : ViewModel() {

    companion object {
        private const val TAG = "DialogDemoViewModel"
        private const val OWNER_TAG = "dialog_manager"

        private const val MARKER_M4E = "img_open_wind_imageView_m4e.webp"
        private const val MARKER_AIRCRAFT = "aircraft_icon.svg"

        private val inputPrefills = listOf("Matrice 350 RTK")

        private val djiAircraftModels = listOf(
            "Matrice 350 RTK", "Matrice 400", "Matrice M30",
            "Mavic 3 Enterprise", "Inspire 3", "Mavic 4D"
        )
        private val singleCheckedItems = listOf(false, false, false, false, true, false)
        private val multiCheckedItems = listOf(false, true, false, false, true, false)
    }

    val selectedType = MutableLiveData<DialogType?>(null)

    private val initialized = AtomicBoolean(false)
    private var drawingContext: DrawingContext? = null

    /** 结果展示 TextView 的 viewId */
    private var resultTextId: String? = null

    // ── 悬浮窗绘制 ──────────────────────────────────────────────────────────

    fun initAllLayouts() {
        if (!initialized.compareAndSet(false, true)) return

        val helper = RemoteViewHelper.floatingBoxHelper
        helper.clearLayout()
        helper.setPageLayoutParamWrapContent()
        val ctx = DrawingContext(helper)
        drawingContext = ctx

        // 根容器
        val root = helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        root.addToRoot(null)

        // 结果展示卡片
        resultTextId = ctx.drawResultCard(root)

        // 操作按钮卡片（5 个弹出按钮，垂直堆叠：每个按钮独占一行）
        ctx.titledCard(root, R.string.card_title_action) { card ->
            listOf(
                R.string.dialog_btn_show_alert to ::showAlertDialog,
                R.string.dialog_btn_show_confirm to ::showConfirmDialog,
                R.string.dialog_btn_show_prompt to ::showPromptDialog,
                R.string.dialog_btn_show_single to ::showSingleChoiceDialog,
                R.string.dialog_btn_show_multi to ::showMultiChoiceDialog,
            ).forEach { (textResId, action) ->
                card.addToRemote(ctx.primaryButton(textResId, action), ctx.marginParam(2))
            }
        }

        // 返回按钮
        ctx.addBackButton(root) { NavigationHelper.navigateToPluginApp() }

        FloatingWindowOwner.claim(OWNER_TAG)
    }

    fun drawAndNavigate() {
        if (!FloatingWindowOwner.isOwnedBy(OWNER_TAG)) {
            initialized.set(false)
        }
        initAllLayouts()
        NavigationHelper.navigateToPilot()
    }

    // ── 对话框调用 ──────────────────────────────────────────────────────────

    private fun showAlertDialog() {
        val config = RemoteDialogConfigBuilder()
            .setTitleByResId(R.string.dialog_alert_title)
            .setMessageByResId(R.string.dialog_alert_message)
            .setIconFileName(MARKER_M4E)
            .setOnDismiss(object : IOpenViewOnClickListener {
                override fun onClick() {
                    pushResult("[Alert Dialog]\n\n[Dismiss]")
                }
            })
            .build()

        handleResult(RemoteDialogManager.getInstance().showAlertDialog(config))
    }

    private fun showConfirmDialog() {
        val config = RemoteDialogConfigBuilder()
            .setTitleByResId(R.string.dialog_confirm_title)
            .setMessageByResId(R.string.dialog_confirm_message)
            .setIconFileName(MARKER_M4E)
            .setStartButtonTextByResId(R.string.dialog_confirm_btn_no)
            .setEndButtonTextByResId(R.string.dialog_confirm_btn_yes)
            .setOnStartButtonClick(object : IOpenViewOnClickListener {
                override fun onClick() {
                    pushResult("[Confirm Dialog]\n\n[Button] StartButton\n\n[Result] Not a Matrice 4!")
                }
            })
            .setOnEndButtonClick(object : IOpenViewOnClickListener {
                override fun onClick() {
                    pushResult("[Confirm Dialog]\n\n[Button] EndButton\n\n[Result] It is a Matrice 4!")
                }
            })
            .setOnDismiss(object : IOpenViewOnClickListener {
                override fun onClick() {
                    Log.d(TAG, "[Dismiss OnClick] confirm dismissed")
                }
            })
            .build()

        handleResult(RemoteDialogManager.getInstance().showConfirmDialog(config))
    }

    private fun showPromptDialog() {
        val inputHints = listOf(str(R.string.dialog_prompt_hint))
        val elementConfigs = formInputElementConfigs(inputHints, inputPrefills)
        val config = RemoteDialogConfigBuilder()
            .setTitleByResId(R.string.dialog_prompt_title)
            .setMessageByResId(R.string.dialog_prompt_message)
            .setIconFileName(MARKER_AIRCRAFT)
            .setStartButtonTextByResId(R.string.dialog_btn_cancel)
            .setEndButtonTextByResId(R.string.dialog_btn_confirm)
            .setElementConfigs(elementConfigs)
            .setOnMessage(object : IDialogMessageListener {
                override fun onMessage(message: DialogMessage) {
                    val inputResult = message.inputTexts?.firstOrNull() ?: ""
                    pushResult("[Prompt Dialog]\n\n[Button] ${message.buttonType}\n\n[Input] $inputResult")
                }
            })
            .setOnDismiss(object : IOpenViewOnClickListener {
                override fun onClick() {
                    Log.d(TAG, "[Dismiss OnClick] prompt dismissed")
                }
            })
            .build()

        handleResult(RemoteDialogManager.getInstance().showPromptDialog(config))
    }

    private fun showSingleChoiceDialog() {
        val elementConfigs = formChoiceElementConfigs(djiAircraftModels, singleCheckedItems)
        val config = RemoteDialogConfigBuilder()
            .setTitleByResId(R.string.dialog_single_title)
            .setMessageByResId(R.string.dialog_single_message)
            .setIconFileName(MARKER_AIRCRAFT)
            .setElementConfigs(elementConfigs)
            .setOnMessage(object : IDialogMessageListener {
                override fun onMessage(message: DialogMessage) {
                    if (!message.selectedIndexes.isNullOrEmpty()) {
                        val index = message.selectedIndexes[0]
                        pushResult("[Single Choice]\n\n[Result]\nFavorite: ${djiAircraftModels[index]}")
                    }
                }
            })
            .setOnDismiss(object : IOpenViewOnClickListener {
                override fun onClick() {
                    Log.d(TAG, "[Dismiss OnClick] single choice dismissed")
                }
            })
            .build()

        handleResult(RemoteDialogManager.getInstance().showSingleChoiceDialog(config))
    }

    private fun showMultiChoiceDialog() {
        val elementConfigs = formChoiceElementConfigs(djiAircraftModels, multiCheckedItems)
        val config = RemoteDialogConfigBuilder()
            .setTitleByResId(R.string.dialog_multi_title)
            .setMessageByResId(R.string.dialog_multi_message)
            .setIconFileName(MARKER_AIRCRAFT)
            .setStartButtonTextByResId(R.string.dialog_btn_cancel)
            .setEndButtonTextByResId(R.string.dialog_btn_confirm)
            .setElementConfigs(elementConfigs)
            .setOnStartButtonClick(object : IOpenViewOnClickListener {
                override fun onClick() {
                    Log.d(TAG, "[StartButton OnClick] multi choice")
                }
            })
            .setOnEndButtonClick(object : IOpenViewOnClickListener {
                override fun onClick() {
                    Log.d(TAG, "[EndButton OnClick] multi choice")
                }
            })
            .setOnMessage(object : IDialogMessageListener {
                override fun onMessage(message: DialogMessage) {
                    val result = if (message.buttonType == DialogButtonPosition.END) {
                        val selectionResult = message.selectedIndexes
                            ?.map { index -> djiAircraftModels[index] }
                            ?.joinToString(separator = ",  ")
                            ?: ""
                        "[Multi Choice]\n\n[Button] ${message.buttonType}\n\n[Result] $selectionResult"
                    } else {
                        "[Multi Choice]\n\n[Button] ${message.buttonType}\n\n[Result] Cancelled"
                    }
                    pushResult(result)
                }
            })
            .setOnDismiss(object : IOpenViewOnClickListener {
                override fun onClick() {
                    Log.d(TAG, "[Dismiss OnClick] multi choice dismissed")
                }
            })
            .build()

        handleResult(RemoteDialogManager.getInstance().showMultiChoiceDialog(config))
    }

    // ── 工具 ──────────────────────────────────────────────────────────────

    private fun formInputElementConfigs(
        hints: List<String>, prefills: List<String>
    ): List<DialogElementConfig> {
        if (hints.isEmpty() && prefills.isEmpty()) return emptyList()
        val size = minOf(hints.size, prefills.size).takeIf { it > 0 } ?: return emptyList()
        return List(size) { index ->
            DialogElementConfig(hints.getOrNull(index) ?: "", prefills.getOrNull(index) ?: "", "", false)
        }
    }

    private fun formChoiceElementConfigs(
        items: List<String>, isSelected: List<Boolean>
    ): List<DialogElementConfig> {
        if (items.isEmpty() && isSelected.isEmpty()) return emptyList()
        val size = minOf(items.size, isSelected.size).takeIf { it > 0 } ?: return emptyList()
        return List(size) { index ->
            DialogElementConfig("", "", items.getOrNull(index) ?: "", isSelected.getOrNull(index) ?: false)
        }
    }

    private fun handleResult(result: OperationResult?) {
        Log.d(TAG, "operationResult: $result")
        if (result == OperationResult.DIALOG_ALREADY_EXISTS) {
            pushResult("[Error] Dialog already exists")
        }
    }

    private fun pushResult(message: String) {
        val id = resultTextId ?: return
        drawingContext?.helper?.updateView(id, RemoteViewProperty.TEXT, message)
    }

    private fun str(@StringRes resId: Int, vararg args: Any): String {
        return PluginApplication.instance?.getString(resId, *args) ?: ""
    }
}
