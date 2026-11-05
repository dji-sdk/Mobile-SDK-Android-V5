package com.dji.open.pilot.plugin.sampleitem.mediatask

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.BaseDemoActivity
import com.dji.open.pilot.plugin.common.globalViewModels
import com.dji.open.pilot.plugin.common.widget.GridSpacingItemDecoration
import com.dji.open.pilot.plugin.databinding.ActivityMediaTaskDemoBinding
import com.dji.open.pilot.plugin.main.MainViewModel
import com.dji.open.pilot.plugin.sampleitem.mediatask.adapter.MediaFileAdapter
import dji.open.pilot.client.manager.ServerState
import dji.sdk.keyvalue.value.camera.CameraStorageLocation
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.manager.datacenter.media.MediaFile
import java.io.File

/**
 * 多媒体演示页。
 *
 * 左侧控制区（单列垂直排布，Spinner 下拉框，相机级 XMP、拉取、下载等操作）。
 * 右侧主内容区（默认展示全屏网格；点击后叠加展示详情面板）。
 *
 * 能力边界：REMOTE 模式下删除与视频播放不被支持，本页保留入口但永久禁用，不绑定任何 SDK 调用。
 */
class MediaTaskDemoActivity : BaseDemoActivity() {

    private companion object {
        const val GRID_SPAN_COUNT = 3
        const val GRID_SPACING_DP = 4

        val COMPONENT_OPTIONS = listOf(
            ComponentIndexType.LEFT_OR_MAIN,
            ComponentIndexType.RIGHT,
            ComponentIndexType.UP,
            ComponentIndexType.INDEX_3,
            ComponentIndexType.UP_TYPE_C,
            ComponentIndexType.UP_TYPE_C_EXT_ONE,
            ComponentIndexType.INDEX_6,
            ComponentIndexType.FPV,
            ComponentIndexType.INDEX_8,
            ComponentIndexType.PORT_1,
            ComponentIndexType.PORT_2,
            ComponentIndexType.PORT_3,
            ComponentIndexType.PORT_4,
            ComponentIndexType.PORT_5,
            ComponentIndexType.PORT_6,
            ComponentIndexType.PORT_7,
            ComponentIndexType.UNKNOWN
        )
        val STORAGE_OPTIONS = listOf(
            CameraStorageLocation.SDCARD,
            CameraStorageLocation.INTERNAL,
            CameraStorageLocation.UNKNOWN
        )
    }

    private lateinit var binding: ActivityMediaTaskDemoBinding

    private val viewModel: MediaTaskDemoViewModel by lazy {
        ViewModelProvider(this)[MediaTaskDemoViewModel::class.java]
    }
    private val mainViewModel: MainViewModel by globalViewModels()

    private val adapter by lazy {
        MediaFileAdapter(
            onFileClick = { fileIndex ->
                viewModel.onFileClicked(fileIndex)
                // 单选模式且有文件被选中时，显示右侧详情面板
                if (viewModel.uiState.value?.selectionMode == false) {
                    binding.layoutDetailContainer.isVisible = true
                }
            },
            onThumbnailRequired = { file, callback -> viewModel.loadThumbnail(file, callback) }
        )
    }

    /** 下载与热解析统一落在 app-specific 目录，无需申请外部存储权限。 */
    private val downloadBaseDir: File
        get() = externalCacheDir ?: cacheDir

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaTaskDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinners()
        setupGrid()
        setupActions()
        setupObservers()

        // 物理返回键：详情页打开时先回列表，列表页时才退出。
        onBackPressedDispatcher.addCallback(this) { handleBack() }

        // 受限能力入口：可见但永久禁用，并明确说明原因。
        binding.btnDeleteDisabled.alpha = 0.5f
        binding.btnVideoDisabled.alpha = 0.5f
    }

    override fun onDestroy() {
        viewModel.releasePageResources()
        super.onDestroy()
    }

    private fun setupSpinners() {
        val compAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            COMPONENT_OPTIONS.map { it.name }
        )
        compAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.btnComponent.adapter = compAdapter
        binding.btnComponent.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.selectComponent(COMPONENT_OPTIONS[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val storageAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            STORAGE_OPTIONS.map { it.name }
        )
        storageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.btnStorage.adapter = storageAdapter
        binding.btnStorage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.selectStorage(STORAGE_OPTIONS[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupGrid() {
        binding.rvMediaFiles.layoutManager = GridLayoutManager(this, GRID_SPAN_COUNT)
        binding.rvMediaFiles.adapter = adapter
        binding.rvMediaFiles.addItemDecoration(
            GridSpacingItemDecoration(
                GRID_SPAN_COUNT,
                (GRID_SPACING_DP * resources.displayMetrics.density).toInt(),
                true
            )
        )
    }

    private fun setupActions() {
        binding.tvBack.setOnClickListener { handleBack() }

        binding.btnEnterPlayback.setOnClickListener { viewModel.enterPlayback() }
        binding.btnExitPlayback.setOnClickListener { viewModel.exitPlayback() }

        binding.btnPullFiles.setOnClickListener {
            viewModel.pullMediaFiles(
                binding.etMediaIndex.text?.toString().orEmpty(),
                binding.etMediaCount.text?.toString().orEmpty()
            )
        }
        binding.btnStopPull.setOnClickListener { viewModel.stopPullMediaFiles() }

        binding.btnSelectMode.setOnClickListener {
            viewModel.setSelectionMode(viewModel.uiState.value?.selectionMode != true)
        }
        binding.btnDownloadSelected.setOnClickListener {
            viewModel.downloadSelectedFiles(downloadBaseDir)
        }

        binding.btnSetXmp.setOnClickListener { showXmpInputDialog() }
        binding.btnGetXmp.setOnClickListener { viewModel.getCameraXmpCustomInfo() }
        binding.btnTakePhoto.setOnClickListener { viewModel.takePhoto() }
        binding.btnFormatStorage.setOnClickListener { confirmFormatStorage() }

        binding.btnPullPreview.setOnClickListener { viewModel.pullPreview() }
        binding.btnDownloadFile.setOnClickListener { viewModel.downloadSelectedFile(downloadBaseDir) }
        binding.btnCancelDownload.setOnClickListener { viewModel.cancelDownload() }

        binding.btnPullXmp.setOnClickListener { viewModel.pullXmpFileData() }
        binding.btnPullXmpCustom.setOnClickListener { viewModel.pullXmpCustomInfo() }
        binding.btnParseThermal.setOnClickListener { viewModel.parseThermal(downloadBaseDir) }
    }

    private fun isDetailVisible(): Boolean = binding.layoutDetailContainer.isVisible

    /** 关闭详情面板并清空选中，回到列表。 */
    private fun closeDetail() {
        binding.layoutDetailContainer.isVisible = false
        viewModel.clearSelection()
    }

    /** 统一返回逻辑：详情页打开时先回列表，否则退出页面。 */
    private fun handleBack() {
        if (isDetailVisible()) {
            closeDetail()
        } else {
            finish()
        }
    }

    private fun setupObservers() {
        mainViewModel.serverState.observe(this) { state ->
            viewModel.onServerStateChanged(state == ServerState.CONNECTED)
        }
        viewModel.uiState.observe(this) { state -> render(state) }
        viewModel.toast.observe(this) { message ->
            if (message == null) {
                return@observe
            }
            viewModel.consumeToast()
            Toast.makeText(
                this,
                getString(message.resId, *message.args.toTypedArray()),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ---------------------------------------------------------------- 渲染

    private fun render(state: MediaTaskUiState) {
        binding.tvServerState.text = getString(
            R.string.media_server_state,
            getString(
                if (state.isServerConnected) {
                    R.string.media_server_connected
                } else {
                    R.string.media_server_disconnected
                }
            )
        )
        val compIndex = COMPONENT_OPTIONS.indexOf(state.component)
        if (compIndex >= 0 && binding.btnComponent.selectedItemPosition != compIndex) {
            binding.btnComponent.setSelection(compIndex)
        }
        val storageIndex = STORAGE_OPTIONS.indexOf(state.storage)
        if (storageIndex >= 0 && binding.btnStorage.selectedItemPosition != storageIndex) {
            binding.btnStorage.setSelection(storageIndex)
        }
        binding.tvPlaybackState.text = getString(
            R.string.media_playback_label,
            getString(playbackStateRes(state.playbackState))
        )
        binding.btnSelectMode.text = getString(
            if (state.selectionMode) R.string.media_select_mode_off else R.string.media_select_mode_on
        )
        binding.tvCameraXmp.text = state.cameraXmpCustomInfo
            ?.let { getString(R.string.media_xmp_current, it) }
            .orEmpty()
        binding.tvOperationStatus.text = if (state.selectionMode) {
            getString(R.string.media_selected_count, state.selectedFileIndices.size)
        } else {
            ""
        }

        adapter.submitList(state.files)
        adapter.submitSelection(state.selectionMode, state.selectedFileIndices)
        renderContentState(state)
        renderDetail(state)
        renderEnablement(state)
    }

    private fun playbackStateRes(playbackState: PlaybackUiState): Int = when (playbackState) {
        PlaybackUiState.UNKNOWN -> R.string.media_playback_unknown
        PlaybackUiState.ENTERING -> R.string.media_playback_entering
        PlaybackUiState.ENTERED -> R.string.media_playback_entered_state
        PlaybackUiState.EXITING -> R.string.media_playback_exiting
        PlaybackUiState.EXITED -> R.string.media_playback_exited_state
        PlaybackUiState.FAILED -> R.string.media_playback_failed_state
    }

    private fun renderContentState(state: MediaTaskUiState) {
        val hasContent = state.contentState == ContentUiState.CONTENT
        binding.rvMediaFiles.isVisible = hasContent
        binding.tvEmptyHint.isVisible = !hasContent
        binding.tvEmptyHint.text = when (state.contentState) {
            ContentUiState.LOADING -> getString(R.string.media_loading)
            ContentUiState.EMPTY -> getString(R.string.media_empty)
            ContentUiState.ERROR -> getString(R.string.media_list_failed)
            else -> getString(R.string.media_select_hint)
        }
    }

    private fun renderDetail(state: MediaTaskUiState) {
        val file = state.selectedFile
        binding.tvFileMetadata.text = if (file == null) {
            getString(R.string.media_select_hint)
        } else {
            val metadata = getString(
                R.string.media_metadata,
                file.fileName,
                file.fileIndex,
                file.fileType.name,
                formatFileSize(file.fileSize),
                file.date?.toDisplayDate().orEmpty().ifEmpty { getString(R.string.media_unknown) }
            )
            // 需求3：本地 media-task 目录已存在同名文件时，追加下载位置。
            val localFile = localFileOf(file)
            if (localFile != null) {
                metadata + "\n" + getString(R.string.media_download_location, localFile.absolutePath)
            } else {
                metadata
            }
        }

        binding.progressPreview.isVisible = state.previewState == DetailUiState.LOADING
        binding.ivPreview.isVisible = state.previewBitmap != null
        state.previewBitmap?.let { binding.ivPreview.setImageBitmap(it) }
        binding.tvPreviewState.text = when {
            state.previewIsThumbnail && state.previewState != DetailUiState.LOADING -> ""
            state.previewState == DetailUiState.LOADING -> getString(R.string.media_loading)
            state.previewState == DetailUiState.ERROR -> getString(R.string.media_load_failed)
            else -> ""
        }

        binding.progressDownload.isIndeterminate =
            state.downloadState == DownloadUiState.RUNNING && state.isDownloadIndeterminate
        binding.progressDownload.progress = state.downloadProgress
        binding.tvDownloadProgress.text = when (state.downloadState) {
            DownloadUiState.RUNNING -> if (state.isDownloadIndeterminate) {
                getString(
                    R.string.media_download_received_speed,
                    formatFileSize(state.downloadReceivedBytes),
                    formatFileSize(state.downloadSpeedBytesPerSecond)
                )
            } else {
                getString(
                    R.string.media_download_progress_speed,
                    state.downloadProgress,
                    formatFileSize(state.downloadSpeedBytesPerSecond)
                )
            }
            DownloadUiState.SUCCEEDED -> getString(R.string.media_download_completed)
            DownloadUiState.FAILED -> getString(R.string.media_download_failed)
            DownloadUiState.CANCELLED -> getString(R.string.media_download_cancelled)
            DownloadUiState.IDLE -> ""
        }

        binding.tvXmpResult.text = when (state.xmpFileState) {
            DetailUiState.LOADING -> getString(R.string.media_loading)
            DetailUiState.CONTENT -> state.xmpFileData.orEmpty()
            DetailUiState.ERROR -> getString(R.string.media_load_failed)
            DetailUiState.IDLE -> ""
        }
        binding.tvXmpCustomResult.text = when (state.xmpCustomState) {
            DetailUiState.LOADING -> getString(R.string.media_loading)
            DetailUiState.CONTENT -> state.xmpCustomInfo.orEmpty()
            DetailUiState.ERROR -> getString(R.string.media_load_failed)
            DetailUiState.IDLE -> ""
        }

        binding.tvThermalResult.text = when (state.thermalState) {
            DetailUiState.LOADING -> getString(R.string.media_loading)
            DetailUiState.CONTENT -> state.thermalResult?.let { info ->
                getString(
                    R.string.media_thermal_result,
                    info.maxTemperature,
                    info.minTemperature,
                    info.emissivity,
                    info.ambientTemperature,
                    info.captureTime
                )
            }.orEmpty()
            DetailUiState.ERROR -> getString(R.string.media_load_failed)
            DetailUiState.IDLE -> ""
        }
    }

    private fun renderEnablement(state: MediaTaskUiState) {
        val canOperate = state.isServerConnected && !state.operationInProgress
        val listLoading = state.contentState == ContentUiState.LOADING
        val downloadRunning = state.downloadState == DownloadUiState.RUNNING
        val hasFile = state.selectedFileIndex != null

        setEnabled(binding.btnComponent, canOperate)
        setEnabled(binding.btnStorage, canOperate)
        setEnabled(binding.btnEnterPlayback, canOperate && state.playbackState != PlaybackUiState.ENTERING)
        setEnabled(binding.btnExitPlayback, canOperate && state.playbackState != PlaybackUiState.EXITING)
        setEnabled(binding.btnPullFiles, canOperate && !listLoading)
        setEnabled(binding.btnStopPull, state.isServerConnected && listLoading)
        setEnabled(binding.btnSelectMode, canOperate)
        setEnabled(binding.btnDownloadSelected, canOperate && state.selectedFileIndices.isNotEmpty() && !downloadRunning)
        setEnabled(binding.btnSetXmp, canOperate)
        setEnabled(binding.btnGetXmp, canOperate)
        setEnabled(binding.btnTakePhoto, canOperate)
        setEnabled(binding.btnFormatStorage, canOperate)

        setEnabled(binding.btnPullPreview, canOperate && hasFile)
        setEnabled(binding.btnDownloadFile, canOperate && hasFile && !downloadRunning)
        setEnabled(binding.btnCancelDownload, state.isServerConnected && downloadRunning)
        setEnabled(binding.btnPullXmp, canOperate && hasFile)
        setEnabled(binding.btnPullXmpCustom, canOperate && hasFile)
        setEnabled(binding.btnParseThermal, canOperate && hasFile)
    }

    private fun setEnabled(view: View, enabled: Boolean) {
        view.isEnabled = enabled
        view.alpha = if (enabled) 1f else 0.5f
    }

    /**
     * 返回本地已下载的同名文件（不存在则为 null）。
     *
     * 目录与文件名裁剪逻辑必须与 [MediaDownloadSession.open] 保持一致，保证路径匹配。
     */
    private fun localFileOf(file: MediaFile): File? {
        val safeName = File(file.fileName ?: "").name
        if (safeName.isBlank()) {
            return null
        }
        return File(File(downloadBaseDir, "media-task"), safeName).takeIf { it.isFile }
    }

    // ---------------------------------------------------------------- 弹窗

    private fun showSingleChoice(options: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit) {
        AlertDialog.Builder(this)
            .setSingleChoiceItems(options.toTypedArray(), selectedIndex) { dialog, which ->
                onSelected(which)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun confirmFormatStorage() {
        AlertDialog.Builder(this)
            .setTitle(R.string.media_format_confirm_title)
            .setMessage(R.string.media_format_confirm_message)
            .setPositiveButton(android.R.string.ok) { _, _ -> viewModel.confirmFormatStorage() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showXmpInputDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = getString(R.string.media_xmp_input_hint)
        }
        val density = resources.displayMetrics.density
        val container = FrameLayout(this).apply {
            val horizontal = (16 * density).toInt()
            setPadding(horizontal, (8 * density).toInt(), horizontal, 0)
            addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.media_xmp_input_title)
            .setView(container)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val text = input.text?.toString().orEmpty()
                if (text.isNotBlank()) {
                    viewModel.setCameraXmpCustomInfo(text)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
