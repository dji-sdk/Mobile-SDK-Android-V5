package dji.sampleV5.aircraft.models

import android.graphics.Color
import dji.open.pilot.base.manager.ui.config.DialogButtonPosition
import dji.open.pilot.base.manager.ui.config.DialogElementConfig
import dji.open.pilot.base.manager.ui.config.DialogMessage
import dji.open.pilot.base.ui.IDialogMessageListener
import dji.open.pilot.base.ui.IOpenViewOnClickListener
import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.OperationResult
import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.base.ui.model.ViewPageType
import dji.open.pilot.base.ui.model.ViewSide
import dji.open.pilot.client.manager.ui.RemoteDialogManager
import dji.open.pilot.client.manager.ui.RemoteViewPageManager
import dji.sampleV5.aircraft.R
import dji.v5.utils.common.LogUtils
import dji.v5.ux.remoteview.RemoteDialogConfigBuilder
import dji.v5.ux.remoteview.RemoteText
import dji.v5.ux.remoteview.RemoteViewHelper

class DialogServiceVM : DJIViewModel() {
    companion object {
        val OPEN_PLUGIN_BOX_PAGE_TYPE = ViewPageType.FLOATING_BOX
        const val TAG = "DialogServiceVM"
        const val MARKER_IMAGEVIEW_M4E = "img_open_wind_imageView_m4e.webp"
        const val MARKER_AIRCRAFT_ICON = "aircraft_icon.svg"

        val markerIconList = listOf(MARKER_IMAGEVIEW_M4E, MARKER_AIRCRAFT_ICON)

        val inputHints: List<String> = listOf("设备名称")
        val inputPrefills: List<String> = listOf("Matrice 350 RTK")

        val djiAircraftModels: List<String> = listOf("Matrice 350 RTK", "Matrice 400", "Matrice M30", "Mavic 3 Enterprise", "Inspire 3", "Mavic 4D")
        val singleCheckedItems: List<Boolean> = listOf(false, false, false, false, true, false)
        val multiCheckedItems: List<Boolean> = listOf(false, true, false, false, true, false)
    }

    private lateinit var outputTextProvider: RemoteViewHelper
    private lateinit var outputTextView: RemoteText

    fun initSampleRemoteView() {
        val helper = RemoteViewHelper.obtain(OPEN_PLUGIN_BOX_PAGE_TYPE)
        outputTextProvider = helper
        helper.clearLayout()
        helper.setPageLayoutParam(430)

        val viewWidth = 140
        val textHeight = 15

        val container = helper.createConstraintLayout().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        container.addToRoot(null)

        val textOutput = helper.createText().apply {
            setTextByResId(R.string.dialog_sample_output_label)
            setWidth(viewWidth)
            setHeight(textHeight)
        }
        container.addToRemote(textOutput)

        val outputText = helper.createText().apply {
            setTextByResId(R.string.dialog_sample_output_placeholder)
            setWidth(viewWidth)
            setHeight(160)
        }
        container.addToRemote(outputText)

        val buttonTypeText = helper.createText().apply {
            setTextByResId(R.string.dialog_sample_type_label)
            setWidth(viewWidth)
            setHeight(textHeight)
        }
        container.addToRemote(buttonTypeText)

        val alertBtn = helper.createButton().apply {
            setTextByResId(R.string.dialog_sample_btn_alert)
            setWidth(viewWidth)
            setHeight(30)
            setBackground(Color.GRAY)
        }
        container.addToRemote(alertBtn)

        val confirmBtn = helper.createButton().apply {
            setTextByResId(R.string.dialog_sample_btn_confirm)
            setWidth(viewWidth)
            setHeight(30)
            setBackground(Color.GRAY)
        }
        container.addToRemote(confirmBtn)

        val promptBtn = helper.createButton().apply {
            setTextByResId(R.string.dialog_sample_btn_prompt)
            setWidth(viewWidth)
            setHeight(30)
            setBackground(Color.GRAY)
        }
        container.addToRemote(promptBtn)

        val singleBtn = helper.createButton().apply {
            setTextByResId(R.string.dialog_sample_btn_single)
            setWidth(viewWidth)
            setHeight(30)
            setBackground(Color.GRAY)
        }
        container.addToRemote(singleBtn)

        val multiBtn = helper.createButton().apply {
            setTextByResId(R.string.dialog_sample_btn_multi)
            setWidth(viewWidth)
            setHeight(30)
            setBackground(Color.GRAY)
        }
        container.addToRemote(multiBtn)

        // 约束布局
        helper.setConstraints(container.getId(), textOutput.getId(), RemoteView.PARENT_ID, ViewSide.TOP, ViewSide.TOP, 4)
        helper.setConstraints(container.getId(), textOutput.getId(), RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 0)
        helper.setConstraints(container.getId(), textOutput.getId(), RemoteView.PARENT_ID, ViewSide.END, ViewSide.END, 0)

        helper.setConstraints(container.getId(), outputText.getId(), RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 0)
        helper.setConstraints(container.getId(), outputText.getId(), RemoteView.PARENT_ID, ViewSide.END, ViewSide.END, 0)
        helper.setConstraints(container.getId(), outputText.getId(), textOutput.getId(), ViewSide.TOP, ViewSide.BOTTOM, 4)

        helper.setConstraints(container.getId(), buttonTypeText.getId(), RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 0)
        helper.setConstraints(container.getId(), buttonTypeText.getId(), RemoteView.PARENT_ID, ViewSide.END, ViewSide.END, 0)
        helper.setConstraints(container.getId(), buttonTypeText.getId(), outputText.getId(), ViewSide.TOP, ViewSide.BOTTOM, 8)

        helper.setConstraints(container.getId(), alertBtn.getId(), RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 5)
        helper.setConstraints(container.getId(), alertBtn.getId(), RemoteView.PARENT_ID, ViewSide.END, ViewSide.END, 5)
        helper.setConstraints(container.getId(), alertBtn.getId(), buttonTypeText.getId(), ViewSide.TOP, ViewSide.BOTTOM, 4)

        helper.setConstraints(container.getId(), confirmBtn.getId(), RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 5)
        helper.setConstraints(container.getId(), confirmBtn.getId(), RemoteView.PARENT_ID, ViewSide.END, ViewSide.END, 5)
        helper.setConstraints(container.getId(), confirmBtn.getId(), alertBtn.getId(), ViewSide.TOP, ViewSide.BOTTOM, 4)

        helper.setConstraints(container.getId(), promptBtn.getId(), RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 5)
        helper.setConstraints(container.getId(), promptBtn.getId(), RemoteView.PARENT_ID, ViewSide.END, ViewSide.END, 5)
        helper.setConstraints(container.getId(), promptBtn.getId(), confirmBtn.getId(), ViewSide.TOP, ViewSide.BOTTOM, 4)

        helper.setConstraints(container.getId(), singleBtn.getId(), RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 5)
        helper.setConstraints(container.getId(), singleBtn.getId(), RemoteView.PARENT_ID, ViewSide.END, ViewSide.END, 5)
        helper.setConstraints(container.getId(), singleBtn.getId(), promptBtn.getId(), ViewSide.TOP, ViewSide.BOTTOM, 4)

        helper.setConstraints(container.getId(), multiBtn.getId(), RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 5)
        helper.setConstraints(container.getId(), multiBtn.getId(), RemoteView.PARENT_ID, ViewSide.END, ViewSide.END, 5)
        helper.setConstraints(container.getId(), multiBtn.getId(), singleBtn.getId(), ViewSide.TOP, ViewSide.BOTTOM, 4)

        // 保存 outputText 引用供 pushOnClickResult 使用
        outputTextView = outputText

        alertBtn.setOnClickListener { showAlertDialog() }
        confirmBtn.setOnClickListener { showConfirmDialog() }
        promptBtn.setOnClickListener { showPromptDialog() }
        singleBtn.setOnClickListener { showSingleChoiceDialog() }
        multiBtn.setOnClickListener { showMultiChoiceDialog() }
    }

    private fun showAlertDialog() {
        val config = RemoteDialogConfigBuilder()
            .setTitleByResId(R.string.dialog_alert_title)
            .setMessageByResId(R.string.dialog_alert_message)
            .setIconFileName(MARKER_IMAGEVIEW_M4E)
            .setOnDismiss(object : IOpenViewOnClickListener {
                override fun onClick() {
                    LogUtils.d(TAG, "[Dismiss OnClick]提示框回调成功")
                    pushOnClickResult("[Dismiss OnClick] Alert dismissed")
                }
            })
            .build()

        val operationResult = RemoteDialogManager.getInstance().showAlertDialog(config)
        LogUtils.d(TAG, "operationResult: $operationResult")
        if (operationResult == OperationResult.DIALOG_ALREADY_EXISTS) {
            pushOnClickResult("[Error] Dialog already exists")
        }
    }

    private fun showConfirmDialog() {
        val config = RemoteDialogConfigBuilder()
            .setTitleByResId(R.string.dialog_confirm_title)
            .setMessageByResId(R.string.dialog_confirm_message)
            .setIconFileName(MARKER_IMAGEVIEW_M4E)
            .setStartButtonTextByResId(R.string.dialog_confirm_btn_no)
            .setEndButtonTextByResId(R.string.dialog_confirm_btn_yes)
            .setOnStartButtonClick(object : IOpenViewOnClickListener {
                override fun onClick() {
                    LogUtils.d(TAG, "[StartButton OnClick]确认框回调成功")
                    val messageTemp = """
                        [Confirm Dialog]
                        
                        [Button] StartButton
                        
                        [Result] Not a Matrice 4!
                        """.trimIndent()
                    pushOnClickResult(messageTemp)
                }
            })
            .setOnEndButtonClick(object : IOpenViewOnClickListener {
                override fun onClick() {
                    LogUtils.d(TAG, "[EndButton OnClick]确认框回调成功")
                    val messageTemp = """
                        [Confirm Dialog]
                        
                        [Button] EndButton
                        
                        [Result] It is a Matrice 4!
                        """.trimIndent()
                    pushOnClickResult(messageTemp)
                }
            })
            .setOnDismiss(object : IOpenViewOnClickListener {
                override fun onClick() {
                    LogUtils.d(TAG, "[Dismiss OnClick]确认框回调成功")
                }
            })
            .build()

        val operationResult = RemoteDialogManager.getInstance().showConfirmDialog(config)
        LogUtils.d(TAG, "operationResult: $operationResult")
        if (operationResult == OperationResult.DIALOG_ALREADY_EXISTS) {
            pushOnClickResult("[Error] Dialog already exists")
        }
    }

    private fun showPromptDialog() {
        val elementConfigs = formDialogElementConfigForInput(inputHints, inputPrefills)
        val config = RemoteDialogConfigBuilder()
            .setTitleByResId(R.string.dialog_prompt_title)
            .setMessageByResId(R.string.dialog_prompt_message)
            .setIconFileName(MARKER_AIRCRAFT_ICON)
            .setStartButtonText("StartButton")
            .setEndButtonText("EndButton")
            .setElementConfigs(elementConfigs)
            .setOnStartButtonClick(object : IOpenViewOnClickListener {
                override fun onClick() {
                    LogUtils.d(TAG, "[StartButton OnClick]输入框回调成功")
                }
            })
            .setOnEndButtonClick(object : IOpenViewOnClickListener {
                override fun onClick() {
                    LogUtils.d(TAG, "[EndButton OnClick]输入框回调成功")
                }
            })
            .setOnMessage(object : IDialogMessageListener {
                override fun onMessage(message: DialogMessage) {
                    LogUtils.d(TAG, "[onMessage Return]输入框：$message")
                    val inputResult = message.inputTexts?.firstOrNull() ?: ""
                    val messageTemp = """
                        [Prompt Dialog]
                        
                        [Button] ${message.buttonType}
                        
                        [Input] $inputResult
                        """.trimIndent()
                    pushOnClickResult(messageTemp)
                }
            })
            .setOnDismiss(object : IOpenViewOnClickListener {
                override fun onClick() {
                    LogUtils.d(TAG, "[Dismiss OnClick]输入框回调成功")
                }
            })
            .build()

        val operationResult = RemoteDialogManager.getInstance().showPromptDialog(config)
        LogUtils.d(TAG, "operationResult: $operationResult")
        if (operationResult == OperationResult.DIALOG_ALREADY_EXISTS) {
            pushOnClickResult("[Error] Dialog already exists")
        }
    }

    private fun showSingleChoiceDialog() {
        val elementConfigs = formDialogElementConfigForChoice(djiAircraftModels, singleCheckedItems)
        val config = RemoteDialogConfigBuilder()
            .setTitleByResId(R.string.dialog_single_title)
            .setMessageByResId(R.string.dialog_single_message)
            .setIconFileName(MARKER_AIRCRAFT_ICON)
            .setElementConfigs(elementConfigs)
            .setOnMessage(object : IDialogMessageListener {
                override fun onMessage(message: DialogMessage) {
                    LogUtils.d(TAG, "[框体]单选框：$message")
                    if (!message.selectedIndexes.isNullOrEmpty()) {
                        val index = message.selectedIndexes[0]
                        pushOnClickResult("[Single Choice]\n\n[Result]\nFavorite: ${djiAircraftModels[index]}")
                    }
                }
            })
            .setOnDismiss(object : IOpenViewOnClickListener {
                override fun onClick() {
                    LogUtils.d(TAG, "[Dismiss OnClick]单选框回调成功")
                }
            })
            .build()

        val operationResult = RemoteDialogManager.getInstance().showSingleChoiceDialog(config)
        LogUtils.d(TAG, "operationResult: $operationResult")
        if (operationResult == OperationResult.DIALOG_ALREADY_EXISTS) {
            pushOnClickResult("[Error] Dialog already exists")
        }
    }

    private fun showMultiChoiceDialog() {
        val elementConfigs = formDialogElementConfigForChoice(djiAircraftModels, multiCheckedItems)
        val config = RemoteDialogConfigBuilder()
            .setTitleByResId(R.string.dialog_multi_title)
            .setMessageByResId(R.string.dialog_multi_message)
            .setIconFileName(MARKER_AIRCRAFT_ICON)
            .setStartButtonTextByResId(R.string.dialog_btn_cancel)
            .setEndButtonTextByResId(R.string.dialog_btn_confirm)
            .setElementConfigs(elementConfigs)
            .setOnStartButtonClick(object : IOpenViewOnClickListener {
                override fun onClick() {
                    LogUtils.d(TAG, "[StartButton OnClick]多选框回调成功")
                }
            })
            .setOnEndButtonClick(object : IOpenViewOnClickListener {
                override fun onClick() {
                    LogUtils.d(TAG, "[EndButton OnClick]多选框回调成功")
                }
            })
            .setOnMessage(object : IDialogMessageListener {
                override fun onMessage(message: DialogMessage) {
                    LogUtils.d(TAG, "[onMessage]多选框：$message")
                    val messageTemp = if (message.buttonType == DialogButtonPosition.END) {
                        val selectionResult = message.selectedIndexes
                            ?.map { index -> djiAircraftModels[index] }
                            ?.joinToString(separator = ",  ")
                            ?: ""
                        """
                        [Multi Choice]
                        
                        [Button] ${message.buttonType}
                        
                        [Result] $selectionResult
                        """.trimIndent()
                    } else {
                        """
                        [Multi Choice]
                        
                        [Button] ${message.buttonType}
                        
                        [Result] Cancelled
                        """.trimIndent()
                    }
                    pushOnClickResult(messageTemp)
                }
            })
            .setOnDismiss(object : IOpenViewOnClickListener {
                override fun onClick() {
                    LogUtils.d(TAG, "[Dismiss OnClick]多选框回调成功")
                }
            })
            .build()

        val operationResult = RemoteDialogManager.getInstance().showMultiChoiceDialog(config)
        LogUtils.d(TAG, "operationResult: $operationResult")
        if (operationResult == OperationResult.DIALOG_ALREADY_EXISTS) {
            pushOnClickResult("[Error] Dialog already exists")
        }
    }

    private fun formDialogElementConfigForInput(inputHints: List<String>, inputPrefills: List<String>): List<DialogElementConfig> {
        if (inputHints.isEmpty() && inputPrefills.isEmpty()) return emptyList()
        val size = minOf(inputHints.size, inputPrefills.size).takeIf { it > 0 } ?: return emptyList()
        return List(size) { index ->
            DialogElementConfig(inputHints.getOrNull(index) ?: "", inputPrefills.getOrNull(index) ?: "", "", false)
        }
    }

    private fun formDialogElementConfigForChoice(selectionItems: List<String>, isSelected: List<Boolean>): List<DialogElementConfig> {
        if (selectionItems.isEmpty() && isSelected.isEmpty()) return emptyList()
        val size = minOf(selectionItems.size, isSelected.size).takeIf { it > 0 } ?: return emptyList()
        return List(size) { index ->
            DialogElementConfig("", "", selectionItems.getOrNull(index) ?: "", isSelected.getOrNull(index) ?: false)
        }
    }

    private fun pushOnClickResult(message: String) {
        RemoteViewPageManager.getInstance().updateView(OPEN_PLUGIN_BOX_PAGE_TYPE, outputTextView.getId(), RemoteViewProperty.TEXT, message)
    }
}