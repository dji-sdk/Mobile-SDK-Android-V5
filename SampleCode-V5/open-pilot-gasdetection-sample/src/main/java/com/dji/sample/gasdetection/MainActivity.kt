package com.dji.sample.gasdetection

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dji.sample.gasdetection.databinding.ActivityEntryBinding
import com.dji.sample.gasdetection.databinding.DialogVesselConfirmBinding
import com.dji.sample.gasdetection.databinding.ItemAlertRowBinding
import com.dji.sample.gasdetection.databinding.ItemCatalogChannelBinding
import com.dji.sample.gasdetection.databinding.ItemChannelReadingBinding
import com.dji.sample.gasdetection.databinding.ItemPortCardBinding
import com.dji.sample.gasdetection.databinding.ItemRecordRowBinding
import com.dji.sample.gasdetection.databinding.ItemTaskGroupBinding
import com.dji.sample.gasdetection.databinding.ItemTaskListBinding
import com.dji.sample.gasdetection.gas.alert.AlertSeverity
import com.dji.sample.gasdetection.gas.alert.AlertUiState
import com.dji.sample.gasdetection.gas.alert.isActive
import com.dji.sample.gasdetection.gas.alert.severity
import com.dji.sample.gasdetection.gas.autoflight.AutoFlightPhase
import com.dji.sample.gasdetection.gas.autoflight.AutoFlightState
import com.dji.sample.gasdetection.gas.diagnostics.DiagnosticsReportBuilder
import com.dji.sample.gasdetection.gas.follow.FollowBlock
import com.dji.sample.gasdetection.gas.follow.FollowPhase
import com.dji.sample.gasdetection.gas.follow.FollowState
import com.dji.sample.gasdetection.gas.diagnostics.DiagnosticsReportInput
import com.dji.sample.gasdetection.gas.model.AlertState
import com.dji.sample.gasdetection.gas.model.AlertType
import com.dji.sample.gasdetection.gas.model.MeasurementQuality
import com.dji.sample.gasdetection.gas.monitor.ChannelReading
import com.dji.sample.gasdetection.gas.monitor.MonitorState
import com.dji.sample.gasdetection.gas.monitor.TrendDirection
import com.dji.sample.gasdetection.gas.payload.IngestState
import com.dji.sample.gasdetection.gas.payload.PayloadDiagnostics
import com.dji.sample.gasdetection.gas.payload.PayloadPort
import com.dji.sample.gasdetection.gas.payload.PayloadSnapshot
import com.dji.sample.gasdetection.gas.record.InspectionRecord
import com.dji.sample.gasdetection.gas.record.RecordStatus
import com.dji.sample.gasdetection.gas.recognition.RecognitionPhase
import com.dji.sample.gasdetection.gas.recognition.RecognitionState
import com.dji.sample.gasdetection.gas.service.ServiceConnectionState
import com.dji.sample.gasdetection.gas.task.JobChannel
import com.dji.sample.gasdetection.gas.task.TaskGroup
import com.dji.sample.gasdetection.gas.task.TaskSelection
import com.dji.sample.gasdetection.gas.task.TaskStage
import com.dji.sample.gasdetection.gas.task.TaskSyncState
import com.dji.sample.gasdetection.gas.task.TaskTexts
import com.dji.sample.gasdetection.gas.task.TaskUiState
import com.dji.sample.gasdetection.ui.AlertTexts
import com.dji.sample.gasdetection.ui.AutoFlightTexts
import com.dji.sample.gasdetection.ui.EntryStatus
import com.dji.sample.gasdetection.ui.FollowTexts
import com.dji.sample.gasdetection.ui.EntryUiState
import com.dji.sample.gasdetection.ui.PayloadDebugActivity
import com.dji.sample.gasdetection.ui.RecordTexts
import com.dji.sample.gasdetection.ui.VesselTexts
import dji.open.pilot.base.ui.NavigateAction
import dji.open.pilot.client.manager.ui.NavigateManager
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 插件主页：任务 / 设备 / 传输三 Tab。
 *
 * 展示任务、测量、载荷快照与端口诊断；任务操作委托
 * [com.dji.sample.gasdetection.gas.task.TaskStore]，自动飞行、识别、跟随与记录操作委托
 * [PsdkApplication]，页面不直接持有 SDK 控制接口。
 *
 * 线程约束：状态收集与界面更新在主线程；诊断导出在 IO 线程写文件，主线程只做提示与分享。
 * 性能约束：测量每秒可能多次到达，通道卡按通道集合变化重建、其余情况只就地更新，避免高频
 * 重造视图；传输页仅在接入/载荷/诊断变化时重建。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEntryBinding
    private var lastStatus: EntryStatus? = null

    private var lastTask: TaskUiState? = null
    private var lastAuto = AutoFlightState()
    private var lastVessel = RecognitionState()
    private var lastFollow = FollowState()
    private var lastRecords: List<InspectionRecord> = emptyList()
    private var selectedRecordId: String? = null
    private var lastMonitor: MonitorState? = null
    private var lastIngest: IngestState? = null
    private var lastPayloads: List<PayloadSnapshot> = emptyList()
    private var lastDiagnostics = PayloadDiagnostics()

    /** 已展开详情的端口名；端口卡每次刷新都会重建，靠它保持展开态。 */
    private val expandedPorts = mutableSetOf<String>()

    /** 已渲染的通道卡；键为稳定通道标识，趋势与时序都就地刷新。 */
    private val channelBindings = linkedMapOf<String, ItemChannelReadingBinding>()
    private val channelReadings = mutableMapOf<String, ChannelReading?>()
    private var renderedChannelIds: List<String> = emptyList()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val valueFormat = DecimalFormat("0.####")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvVersion.text = getString(R.string.entry_version, BuildConfig.VERSION_NAME)
        binding.btnBackToPilot.setOnClickListener { navigateToPilot() }
        binding.btnOpenDebug.setOnClickListener {
            startActivity(Intent(this, PayloadDebugActivity::class.java))
        }
        binding.tabTasks.setOnClickListener { selectTab(Tab.TASKS) }
        binding.tabDevice.setOnClickListener { selectTab(Tab.DEVICE) }
        binding.tabTransport.setOnClickListener { selectTab(Tab.TRANSPORT) }
        binding.btnExportDiagnostics.setOnClickListener { exportDiagnostics() }
        binding.llAdvancedHeader.setOnClickListener {
            setAdvancedExpanded(!binding.llAdvanced.isVisible)
        }
        setAdvancedExpanded(false)
        selectTab(Tab.TASKS)

        observeSdkState()
        observeData()
        bindTaskActions()
    }

    private fun bindTaskActions() {
        val app = application as PsdkApplication
        binding.btnTaskAccept.setOnClickListener { app.taskStore.accept() }
        binding.btnTaskReject.setOnClickListener { app.taskStore.reject() }
        binding.btnTaskRecheck.setOnClickListener { app.taskStore.recheck() }
        binding.btnTaskManual.setOnClickListener {
            if (app.taskStore.state.value.stage == TaskStage.FLYTO) {
                app.enterManualFromAutoFlight()
            } else {
                app.taskStore.enterManual()
            }
        }
        binding.btnTaskFinish.setOnClickListener {
            val record = app.finishTask()
            if (record != null) {
                Toast.makeText(this, R.string.gas_record_generated, Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnAutoStart.setOnClickListener { confirmStartAutoFlight(app) }
        binding.btnAutoExit.setOnClickListener { app.exitAutoFlight() }
        binding.btnAutoStop.setOnClickListener { confirmEmergencyStop(app) }
        binding.btnAutoConfirm.setOnClickListener { app.confirmAutoFlightHandover() }
        binding.btnVesselSearch.setOnClickListener { startVesselSearch(app) }
        binding.btnVesselRetry.setOnClickListener { app.retryVesselSearch() }
        binding.btnVesselConfirm.setOnClickListener { showVesselConfirmDialog(app) }
        binding.btnFollowStart.setOnClickListener { confirmFollow(app) }
        binding.btnRecordRetry.setOnClickListener {
            selectedRecordId?.let { app.retryRecord(it) }
        }
    }

    /** 开始跟随的二次确认：明确限速与退出方式，确认后才开启虚拟摇杆。 */
    private fun confirmFollow(app: PsdkApplication) {
        AlertDialog.Builder(this)
            .setTitle(R.string.gas_follow_confirm_title)
            .setMessage(R.string.gas_follow_confirm_message)
            .setPositiveButton(R.string.gas_follow_start) { _, _ ->
                val block = app.startFollow()
                if (block != null) {
                    Toast.makeText(this, FollowTexts.blockText(this, block), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.gas_auto_confirm_cancel, null)
            .show()
    }

    /** 飞手在到达后显式开始搜索；被拒绝时用 Toast 补充提示，原因同时写入状态行。 */
    private fun startVesselSearch(app: PsdkApplication) {
        val block = app.startVesselSearch()
        if (block != null) {
            Toast.makeText(
                this,
                VesselTexts.blockReason(this, block),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    /** 确认当前锁定对象的船名/编号；空白内容保持弹窗并提示。 */
    private fun showVesselConfirmDialog(app: PsdkApplication) {
        val current = app.vesselRecognitionState.value.vessel ?: return
        val dialogBinding = DialogVesselConfirmBinding.inflate(layoutInflater)
        dialogBinding.etVesselName.setText(current.nameOrNumber.orEmpty())
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.gas_vessel_dialog_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.gas_vessel_dialog_save, null)
            .setNegativeButton(R.string.gas_auto_confirm_cancel, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = dialogBinding.etVesselName.text?.toString().orEmpty()
                if (app.confirmVesselName(name)) {
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, R.string.gas_vessel_name_required, Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }

    /** 启动自动飞行的二次确认：展示目标点与高度，确认后才下发命令。 */
    private fun confirmStartAutoFlight(app: PsdkApplication) {
        val target = app.taskStore.state.value.job?.target ?: return
        val height = target.altitudeM?.toInt() ?: return
        AlertDialog.Builder(this)
            .setTitle(R.string.gas_auto_confirm_title)
            .setMessage(
                getString(
                    R.string.gas_auto_confirm_message,
                    target.latitude,
                    target.longitude,
                    height,
                )
            )
            .setPositiveButton(R.string.gas_auto_confirm_start) { _, _ ->
                if (!app.startAutoFlight()) {
                    Toast.makeText(this, R.string.gas_auto_start_rejected, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.gas_auto_confirm_cancel, null)
            .show()
    }

    /** 紧急停止的二次确认：明确提示只在遥控器无法接管时使用。 */
    private fun confirmEmergencyStop(app: PsdkApplication) {
        AlertDialog.Builder(this)
            .setTitle(R.string.gas_auto_stop_confirm_title)
            .setMessage(R.string.gas_auto_stop_confirm_message)
            .setPositiveButton(R.string.gas_auto_stop_confirm_ok) { _, _ ->
                app.emergencyStopAutoFlight()
            }
            .setNegativeButton(R.string.gas_auto_confirm_cancel, null)
            .show()
    }

    /** 切换 Tab；选中态由文字颜色与 2dp 指示条共同表达，不只依赖颜色。 */
    private fun selectTab(tab: Tab) {
        val tasks = tab == Tab.TASKS
        val device = tab == Tab.DEVICE
        val transfer = tab == Tab.TRANSPORT
        binding.llTasksPage.isVisible = tasks
        binding.svDevice.isVisible = device
        binding.llTransferPage.isVisible = transfer
        binding.tvTabTasks.isSelected = tasks
        binding.tvTabDevice.isSelected = device
        binding.tvTabTransport.isSelected = transfer
        binding.indicatorTasks.isVisible = tasks
        binding.indicatorDevice.isVisible = device
        binding.indicatorTransport.isVisible = transfer
        if (transfer) renderRecords(lastRecords)
    }

    /**
     * 订阅两组状态：任务+测量驱动监测页，接入+载荷+诊断驱动传输页。
     *
     * 分成两条流是为了避免每条测量都重建传输页；离开前台时全部取消，回到前台重新收集。
     */
    private fun observeData() {
        val app = application as PsdkApplication
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(
                        app.taskStore.state,
                        app.monitorState,
                        app.autoFlightState,
                        app.vesselRecognitionState,
                        app.followState,
                    ) { task, monitor, auto, vessel, follow ->
                        TaskRenderInput(task, monitor, auto, vessel, follow)
                    }
                        .collect { input ->
                            lastTask = input.task
                            lastMonitor = input.monitor
                            lastAuto = input.auto
                            lastVessel = input.vessel
                            lastFollow = input.follow
                            renderTaskList(input.task)
                            renderTask(input.task, input.auto, input.vessel, input.follow)
                            renderChannels(input.task, input.monitor)
                            renderBatch(input.monitor)
                            renderFreshnessSummary(input.monitor)
                        }
                }
                launch {
                    combine(
                        app.gasIngestState,
                        app.gasPayloads,
                        app.gasDiagnostics,
                    ) { ingest, payloads, diagnostics -> Triple(ingest, payloads, diagnostics) }
                        .collect { (ingest, payloads, diagnostics) ->
                            lastIngest = ingest
                            lastPayloads = payloads
                            lastDiagnostics = diagnostics
                            renderTransport(ingest, payloads, diagnostics)
                        }
                }
                launch { app.alertState.collect(::renderAlerts) }
                launch {
                    app.inspectionRecords.collect { records ->
                        lastRecords = records
                        renderRecords(records)
                    }
                }
                launch {
                    while (isActive) {
                        delay(FRESHNESS_TICK_MS)
                        refreshFreshness()
                    }
                }
            }
        }
    }

    private fun renderTask(
        state: TaskUiState,
        auto: AutoFlightState,
        vessel: RecognitionState,
        follow: FollowState,
    ) {
        val stage = state.stage
        val job = state.job
        binding.llTaskDetail.isVisible = job != null
        binding.tvTaskEmpty.isVisible = job == null
        binding.tvTaskStage.text = if (job == null) {
            ""
        } else {
            getString(R.string.gas_task_stage_label, getString(TaskTexts.stageRes(stage)))
        }
        binding.tvTaskTitle.text = job?.title.orEmpty()
        binding.tvTaskTarget.isVisible = job?.target != null
        job?.target?.let {
            binding.tvTaskTarget.text = getString(R.string.gas_task_target, it.latitude, it.longitude)
        }
        binding.tvTaskBoundary.isVisible = job?.safetyBoundaryM != null
        job?.safetyBoundaryM?.let {
            binding.tvTaskBoundary.text = getString(R.string.gas_task_boundary, it)
        }
        binding.tvTaskRequirements.isVisible = job?.requirements != null
        job?.requirements?.let {
            binding.tvTaskRequirements.text = getString(R.string.gas_task_requirements, it)
        }
        binding.tvTaskChannels.isVisible = job?.channels?.isNotEmpty() == true
        job?.channels?.let { channels ->
            binding.tvTaskChannels.text = channels.joinToString("  ·  ") { channel ->
                buildString {
                    append(channel.displayName.ifBlank { channel.channelId })
                    append(' ')
                    append(channel.unit)
                    channel.threshold?.let { append(" / ").append(it) }
                }
            }
        }

        val preflight = state.preflight
        binding.tvTaskPreflight.isVisible = preflight != null
        if (preflight != null) {
            val failures = preflight.items
                .filterNot { it.passed }
                .joinToString(" · ") { getString(TaskTexts.failureRes(it.failure)) }
            binding.tvTaskPreflight.text = if (preflight.allPassed) {
                getString(R.string.gas_task_preflight_pass)
            } else {
                getString(R.string.gas_task_preflight_fail, failures)
            }
        }

        val errorText = when {
            state.sync is TaskSyncState.Failed ->
                TaskTexts.syncError(this, (state.sync as TaskSyncState.Failed).error)

            state.multipleActive -> getString(R.string.gas_task_multiple_active)
            else -> null
        }
        binding.tvTaskError.isVisible = errorText != null
        binding.tvTaskError.text = errorText.orEmpty()

        binding.btnTaskAccept.isVisible = stage == TaskStage.NEW_ORDER || stage == TaskStage.TASK_DETAIL
        // 拒绝只对已下发未接受的任务有效；接受后后端只允许取消/推进，不再接受 REJECTED。
        binding.btnTaskReject.isVisible = stage == TaskStage.NEW_ORDER || stage == TaskStage.TASK_DETAIL
        binding.btnTaskRecheck.isVisible = stage == TaskStage.PREP || stage == TaskStage.NOT_READY
        binding.btnTaskManual.isVisible = stage == TaskStage.PREP ||
            stage == TaskStage.NOT_READY || stage == TaskStage.PREP_READY ||
            stage == TaskStage.FLYTO || stage == TaskStage.SEARCH || stage == TaskStage.FOLLOW
        binding.btnTaskFinish.isVisible = stage == TaskStage.PREP_READY ||
            stage == TaskStage.MANUAL || stage == TaskStage.FLYTO ||
            stage == TaskStage.SEARCH || stage == TaskStage.FOLLOW

        renderAutoFlight(stage, auto, state.preflight?.allPassed == true)
        renderVessel(stage, auto, vessel)
        renderFollow(stage, vessel, follow)
    }

    /** 自动阶段摘要与按钮可见性；只在 FLYTO / 就绪态出现，非自动阶段隐藏。 */
    private fun renderAutoFlight(
        stage: TaskStage,
        auto: AutoFlightState,
        preflightAllPassed: Boolean,
    ) {
        val inAuto = stage == TaskStage.FLYTO || stage == TaskStage.SEARCH || stage == TaskStage.FOLLOW
        val autoState = inAuto && auto.isAutoActive
        binding.tvTaskAuto.isVisible = autoState
        if (autoState) {
            val phaseText = if (auto.phase == AutoFlightPhase.STARTING || auto.phase == AutoFlightPhase.HANDOVER) {
                getString(AutoFlightTexts.phaseRes(auto.phase))
            } else {
                getString(AutoFlightTexts.flyToStageRes(auto.flyToStage))
            }
            val authorityText = getString(AutoFlightTexts.authorityRes(auto.authority))
            binding.tvTaskAuto.text = if (auto.phase == AutoFlightPhase.HANDOVER) {
                getString(
                    R.string.gas_auto_status_with_reason,
                    phaseText,
                    authorityText,
                    getString(AutoFlightTexts.handoverRes(auto.handoverReason)),
                )
            } else {
                getString(R.string.gas_auto_status, phaseText, authorityText)
            }
        }

        val ready = stage == TaskStage.PREP_READY && preflightAllPassed
        binding.llAutoActions.isVisible = inAuto || ready
        binding.btnAutoStart.isVisible = ready
        binding.btnAutoExit.isVisible = inAuto &&
            (auto.phase == AutoFlightPhase.STARTING ||
                auto.phase == AutoFlightPhase.FLYING ||
                auto.phase == AutoFlightPhase.ARRIVED)
        binding.btnAutoStop.isVisible = inAuto &&
            (auto.phase == AutoFlightPhase.STARTING || auto.phase == AutoFlightPhase.FLYING)
        binding.btnAutoConfirm.isVisible = inAuto && auto.phase == AutoFlightPhase.HANDOVER
    }



    /** 船舶识别状态行与动作入口；与浮窗共用同一份判定结果。 */
    private fun renderVessel(
        stage: TaskStage,
        auto: AutoFlightState,
        vessel: RecognitionState,
    ) {
        val line = if (stage == TaskStage.FLYTO || stage == TaskStage.SEARCH) {
            VesselTexts.statusLine(this, vessel)
        } else {
            null
        }
        binding.tvTaskVessel.isVisible = line != null
        binding.tvTaskVessel.text = line.orEmpty()

        val searchable = stage == TaskStage.FLYTO && auto.phase == AutoFlightPhase.ARRIVED
        val retryable = stage == TaskStage.SEARCH && vessel.phase == RecognitionPhase.DEGRADED
        val confirmable = stage == TaskStage.SEARCH && vessel.phase == RecognitionPhase.LOCKED
        binding.btnVesselSearch.isVisible = searchable
        binding.btnVesselRetry.isVisible = retryable
        binding.btnVesselConfirm.isVisible = confirmable
        binding.llVesselActions.isVisible = searchable || retryable || confirmable
    }

    /** 跟随状态与「开始跟随」入口；状态行与浮窗同源。 */
    private fun renderFollow(
        stage: TaskStage,
        vessel: RecognitionState,
        follow: FollowState,
    ) {
        val line = if (stage == TaskStage.SEARCH || stage == TaskStage.FOLLOW) {
            FollowTexts.statusLine(this, follow)
        } else {
            null
        }
        binding.tvTaskFollow.isVisible = line != null
        binding.tvTaskFollow.text = line.orEmpty()

        val locked = vessel.phase == RecognitionPhase.LOCKED
        val startable = stage == TaskStage.SEARCH && locked && !follow.isBusy
        binding.btnFollowStart.isVisible = startable
    }

    /**
     * 渲染左侧任务列表。
     *
     * 分组顺序固定为进行中、待处理、已完成；空组不渲染。选中态由 item 背景 selector 表达，
     * 点击切换被任务状态机拒绝时用 Toast 说明原因。
     */
    private fun renderTaskList(state: TaskUiState) {
        binding.llTaskList.removeAllViews()
        binding.tvTaskListEmpty.isVisible = state.jobs.isEmpty()
        if (state.jobs.isEmpty()) return

        val groups = listOf(
            R.string.gas_task_group_active to TaskGroup.ACTIVE,
            R.string.gas_task_group_pending to TaskGroup.PENDING,
            R.string.gas_task_group_done to TaskGroup.DONE,
        )
        groups.forEach { (titleRes, group) ->
            val jobs = state.jobs.filter { TaskSelection.groupOf(it.status) == group }
            if (jobs.isEmpty()) return@forEach

            val header = ItemTaskGroupBinding.inflate(layoutInflater, binding.llTaskList, false)
            header.root.setText(titleRes)
            binding.llTaskList.addView(header.root)

            jobs.forEach { job ->
                val item = ItemTaskListBinding.inflate(layoutInflater, binding.llTaskList, false)
                item.root.isSelected = job.jobId == state.selectedJobId
                item.tvItemTitle.text = job.title
                item.tvItemStatus.text = getString(TaskTexts.jobStatusRes(job.status))
                item.tvItemMeta.text = job.target?.let {
                    getString(R.string.gas_task_item_meta, it.latitude, it.longitude)
                } ?: getString(R.string.gas_channel_not_provided)
                item.root.setOnClickListener {
                    val app = application as PsdkApplication
                    if (!app.taskStore.selectJob(job.jobId)) {
                        Toast.makeText(this, R.string.gas_task_select_blocked, Toast.LENGTH_SHORT).show()
                    }
                }
                binding.llTaskList.addView(item.root)
            }
        }
    }

    /** 传输页骨架：本轮只展示选中任务与固定 0 张照片，拍照入口保持禁用。 */
    /** 传输页：左栏记录列表 + 右栏选中详情；空列表时展示空态。 */
    private fun renderRecords(records: List<InspectionRecord>) {
        val sorted = records.sortedByDescending { it.endedAtMs }
        if (sorted.isEmpty()) {
            selectedRecordId = null
            binding.llRecordList.removeAllViews()
            binding.tvRecordListEmpty.isVisible = true
            binding.tvRecordDetailEmpty.isVisible = true
            binding.tvRecordTitle.text = ""
            binding.tvRecordMeta.text = ""
            binding.tvRecordStatus.text = ""
            binding.tvRecordChannels.text = ""
            binding.tvRecordError.isVisible = false
            binding.tvRecordUploadedAt.isVisible = false
            binding.btnRecordRetry.isVisible = false
            return
        }
        binding.tvRecordListEmpty.isVisible = false
        if (sorted.none { it.recordId == selectedRecordId }) {
            selectedRecordId = sorted.first().recordId
        }
        rebuildRecordList(sorted)
        renderRecordDetail(sorted.first { it.recordId == selectedRecordId })
    }

    private fun rebuildRecordList(records: List<InspectionRecord>) {
        binding.llRecordList.removeAllViews()
        records.forEach { record ->
            val item = ItemRecordRowBinding.inflate(layoutInflater, binding.llRecordList, false)
            item.tvRecordItemTitle.text = record.jobTitle.ifBlank { record.jobId }
            item.tvRecordItemStatus.text = RecordTexts.status(this, record.status)
            item.root.isSelected = record.recordId == selectedRecordId
            item.root.setOnClickListener {
                selectedRecordId = record.recordId
                renderRecords(lastRecords)
            }
            binding.llRecordList.addView(item.root)
        }
    }

    private fun renderRecordDetail(record: InspectionRecord) {
        binding.tvRecordDetailEmpty.isVisible = false
        binding.tvRecordTitle.text = record.jobTitle.ifBlank { record.jobId }
        binding.tvRecordMeta.text = getString(
            R.string.gas_record_detail_meta,
            record.totalSamples,
            record.validSamples,
            record.invalidSamples,
            record.alerts.sumOf { it.count },
        )
        binding.tvRecordStatus.text = getString(
            R.string.gas_record_status_label,
            RecordTexts.status(this, record.status),
        )
        binding.tvRecordChannels.text = record.channels.joinToString("\n") { channel ->
            getString(
                R.string.gas_record_channel_line,
                channel.displayName ?: channel.channelId,
                channel.unit ?: getString(R.string.gas_channel_not_provided),
                channel.validSamples,
                channel.min,
                channel.max,
                channel.average,
            )
        }
        val error = record.lastError
        binding.tvRecordError.isVisible = error != null
        binding.tvRecordError.text = error?.let { getString(R.string.gas_record_error, it) }.orEmpty()
        val uploadedAt = record.uploadedAtMs
        binding.tvRecordUploadedAt.isVisible = uploadedAt != null
        binding.tvRecordUploadedAt.text = uploadedAt?.let {
            getString(R.string.gas_record_uploaded_at, timeFormat.format(Date(it)))
        }.orEmpty()
        binding.btnRecordRetry.isVisible =
            record.status == RecordStatus.QUEUED || record.status == RecordStatus.FAILED
    }

    /**
     * 渲染通道卡：通道集合变化时重建，否则就地更新。
     *
     * 设计意图：测量可能每秒多次到达，重建视图会丢失滑动位置并造成闪烁；只有任务通道列表
     * 真正变化时才需要重新布局。
     */
    private fun renderChannels(task: TaskUiState, monitor: MonitorState) {
        val jobChannels = task.job?.channels.orEmpty()
        binding.tvChannelsEmpty.isVisible = jobChannels.isEmpty()
        val ids = jobChannels.map { it.channelId }
        if (ids != renderedChannelIds) {
            rebuildChannelCards(jobChannels)
            renderedChannelIds = ids
        }
        jobChannels.forEach { jobChannel ->
            val reading = monitor.readings.firstOrNull { it.channelId == jobChannel.channelId }
            channelReadings[jobChannel.channelId] = reading
            channelBindings[jobChannel.channelId]?.let { updateChannelCard(it, jobChannel, reading) }
        }
    }

    private fun rebuildChannelCards(jobChannels: List<JobChannel>) {
        binding.llChannels.removeAllViews()
        channelBindings.clear()
        channelReadings.clear()
        jobChannels.forEach { jobChannel ->
            val item = ItemChannelReadingBinding.inflate(layoutInflater, binding.llChannels, false)
            channelBindings[jobChannel.channelId] = item
            binding.llChannels.addView(item.root)
        }
    }

    private fun updateChannelCard(
        item: ItemChannelReadingBinding,
        jobChannel: JobChannel,
        reading: ChannelReading?,
    ) {
        val displayName = jobChannel.displayName.ifBlank {
            reading?.displayName ?: jobChannel.channelId
        }
        item.tvChannelName.text = displayName
        item.tvChannelValue.text = formatChannelValue(reading, jobChannel.unit)
        item.tvChannelQuality.text = getString(
            R.string.gas_channel_quality,
            getString(qualityTextRes(reading?.quality)),
        )
        renderFreshness(item, reading)
        item.tvChannelTime.text = buildString {
            append(
                getString(
                    R.string.gas_channel_source_time,
                    reading?.sourceTimestampMs?.let(::formatTime) ?: getString(R.string.gas_channel_not_provided),
                )
            )
            append('\n')
            append(
                getString(
                    R.string.gas_channel_received_time,
                    reading?.receivedTimestampMs?.let(::formatTime) ?: getString(R.string.gas_channel_not_provided),
                )
            )
        }
        val samples = reading?.samples.orEmpty()
        if (samples.size >= 2) {
            item.sparkline.isVisible = true
            item.sparkline.setPoints(samples.map { it.value.toFloat() })
            item.tvTrendLabel.text = getString(trendTextRes(reading?.trend))
        } else {
            item.sparkline.isVisible = false
            item.tvTrendLabel.text = getString(R.string.gas_channel_insufficient)
        }
    }

    private fun formatChannelValue(reading: ChannelReading?, fallbackUnit: String): String {
        val value = reading?.value ?: return getString(R.string.gas_channel_not_provided)
        val unit = reading.unit?.takeIf { it.isNotBlank() } ?: fallbackUnit
        val number = valueFormat.format(value)
        return if (unit.isBlank()) number else "$number $unit"
    }

    /** 仅刷新时效文案；过期是时间函数，不需要重建整张通道卡。 */
    private fun refreshFreshness() {
        channelBindings.forEach { (channelId, item) ->
            renderFreshness(item, channelReadings[channelId])
        }
        lastMonitor?.let {
            renderFreshnessSummary(it)
            renderDeviceLatest(it)
        }
    }

    /** 时效由接收时间与配置阈值实时计算；配置缺失时不宣称过期。 */
    private fun renderFreshness(item: ItemChannelReadingBinding, reading: ChannelReading?) {
        val receivedAt = reading?.receivedTimestampMs
        if (receivedAt == null) {
            item.tvChannelFreshness.text = getString(R.string.gas_channel_not_provided)
            item.tvChannelFreshness.setTextColor(ContextCompat.getColor(this, R.color.hint_text))
            return
        }
        val staleAfter = (application as PsdkApplication).staleAfterMs
        val stale = staleAfter != null && System.currentTimeMillis() - receivedAt > staleAfter
        if (stale) {
            item.tvChannelFreshness.text =
                getString(R.string.gas_channel_stale, formatTime(receivedAt))
            item.tvChannelFreshness.setTextColor(ContextCompat.getColor(this, R.color.status_error))
        } else {
            item.tvChannelFreshness.text = getString(R.string.gas_channel_fresh)
            item.tvChannelFreshness.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        }
    }

    private fun renderBatch(monitor: MonitorState) {
        val batch = monitor.batch
        val stateText = when {
            batch.sessionId == null -> R.string.gas_batch_none
            batch.live -> R.string.gas_batch_live
            else -> R.string.gas_batch_offline
        }
        binding.tvBatchState.setText(stateText)
        binding.tvBatchState.setTextColor(
            ContextCompat.getColor(
                this,
                when {
                    batch.sessionId == null -> R.color.hint_text
                    batch.live -> R.color.status_ok
                    else -> R.color.text_secondary
                },
            )
        )
        binding.tvBatchSession.text = batch.sessionId?.let { getString(R.string.gas_batch_session, it) }.orEmpty()
        binding.tvBatchCounts.text = getString(
            R.string.gas_batch_counts,
            batch.totalSamples,
            batch.validSamples,
            batch.invalidSamples,
        )
        binding.tvBatchUpdated.text = batch.lastReceivedAtMs
            ?.let { getString(R.string.gas_batch_updated, formatTime(it)) }
            .orEmpty()
    }

    /**
     * 渲染异常卡。
     *
     * 设计意图：列表只展示活动告警，已忽略的告警保留在存储中但不出现在界面；按钮按状态显隐，
     * 点击后不乐观更新，等待告警中心状态流回推。
     */
    private fun renderAlerts(state: AlertUiState) {
        val active = state.activeAlerts.sortedByDescending { it.timestampMs }
        binding.tvAlertCount.text = if (active.isEmpty()) {
            getString(R.string.gas_alert_section)
        } else {
            getString(R.string.gas_alert_count, active.size)
        }
        binding.tvAlertsEmpty.isVisible = active.isEmpty()
        binding.llAlerts.removeAllViews()

        val app = application as PsdkApplication
        val jobChannels = lastTask?.job?.channels.orEmpty()
        active.forEach { alert ->
            val item = ItemAlertRowBinding.inflate(layoutInflater, binding.llAlerts, false)
            val channelName = jobChannels.firstOrNull { it.channelId == alert.channelId }
                ?.displayName
                ?.takeIf { it.isNotBlank() }
                ?: alert.channelId
                ?: getString(R.string.gas_channel_not_provided)
            item.tvAlertTitle.text = getString(
                R.string.gas_alert_title,
                getString(AlertTexts.typeRes(alert.type)),
                channelName,
            )
            item.tvAlertTitle.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (alert.type.severity() == AlertSeverity.ERROR) {
                        R.color.status_error
                    } else {
                        R.color.status_wip
                    },
                )
            )
            val unit = alert.unit?.takeIf { it.isNotBlank() }
                ?: getString(R.string.gas_channel_not_provided)
            item.tvAlertObserved.text = getString(
                R.string.gas_alert_observed,
                valueFormat.format(alert.observedValue),
                unit,
            )
            val showThreshold = alert.type == AlertType.THRESHOLD_EXCEEDED && !alert.threshold.isNaN()
            item.tvAlertThreshold.isVisible = showThreshold
            if (showThreshold) {
                item.tvAlertThreshold.text = getString(
                    R.string.gas_alert_threshold,
                    valueFormat.format(alert.threshold),
                    unit,
                )
            }
            item.tvAlertMeta.text = getString(
                R.string.gas_alert_meta,
                formatTime(alert.timestampMs),
                getString(AlertTexts.stateRes(alert.state)),
            )
            item.btnAlertAck.isVisible = alert.state == AlertState.NEW
            item.btnAlertReview.isVisible =
                alert.state == AlertState.NEW || alert.state == AlertState.ACKNOWLEDGED
            item.btnAlertDismiss.isVisible = alert.state.isActive
            item.btnAlertAck.setOnClickListener { app.acknowledgeAlert(alert.alertId) }
            item.btnAlertReview.setOnClickListener { app.markAlertUnderReview(alert.alertId) }
            item.btnAlertDismiss.setOnClickListener { app.dismissAlert(alert.alertId) }
            binding.llAlerts.addView(item.root)
        }
    }

    private fun renderTransport(
        ingest: IngestState,
        payloads: List<PayloadSnapshot>,
        diagnostics: PayloadDiagnostics,
    ) {
        binding.tvLinkState.text = getString(
            if (ingest.started) R.string.gas_link_started else R.string.gas_link_stopped
        )
        binding.tvLinkState.setTextColor(
            ContextCompat.getColor(this, if (ingest.started) R.color.status_ok else R.color.text_secondary)
        )
        // 在线载荷与端口数只在摘要出现一次，逐端口明细交给端口卡
        binding.tvLinkCounts.text = getString(
            R.string.gas_payload_summary,
            ingest.onlinePayloads,
            ingest.monitoredPorts,
        )
        binding.tvLinkHint.isVisible = !ingest.started

        renderPorts(payloads, diagnostics)
        renderCatalog()
        renderDeviceLatest(lastMonitor)
    }

    /**
     * 摘要的时效行：新鲜时报距今秒数，过期改报最后更新时刻。
     *
     * 时间函数必须由 ticker 驱动（见 [refreshFreshness]），否则秒数会停在最后一次状态流发射时。
     */
    private fun renderDeviceLatest(monitor: MonitorState?) {
        val receivedAt = monitor?.let(::latestReading)?.receivedTimestampMs
        if (receivedAt == null) {
            binding.tvDeviceLatest.text = getString(R.string.gas_freshness_none)
            binding.tvDeviceLatest.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            return
        }
        val staleAfter = (application as PsdkApplication).staleAfterMs
        val ageMs = (System.currentTimeMillis() - receivedAt).coerceAtLeast(0L)
        // 阈值缺失时不得宣称过期，只报距今时间
        if (staleAfter != null && ageMs > staleAfter) {
            binding.tvDeviceLatest.text =
                getString(R.string.gas_channel_stale, formatTime(receivedAt))
            binding.tvDeviceLatest.setTextColor(ContextCompat.getColor(this, R.color.status_error))
        } else {
            binding.tvDeviceLatest.text =
                getString(R.string.gas_device_latest, (ageMs / 1000L).toInt())
            binding.tvDeviceLatest.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        }
    }

    /** 端口卡取「载荷快照 ∪ 诊断计数」的并集，避免只收到数据、未收到基础信息时漏显示。 */
    private fun renderPorts(payloads: List<PayloadSnapshot>, diagnostics: PayloadDiagnostics) {
        binding.llPorts.removeAllViews()
        val byPort = payloads.associateBy { it.port }
        val ports = (payloads.map { it.port } + diagnostics.ports.keys)
            .distinctBy { it.value }
            .sortedBy { it.value }
        binding.tvPortsEmpty.isVisible = ports.isEmpty()
        ports.forEach { port ->
            val payload = byPort[port]
            val counts = diagnostics.ports[port]
            val item = ItemPortCardBinding.inflate(layoutInflater, binding.llPorts, false)
            item.tvPortName.text = port.name
            item.tvPortState.text = getString(
                if (payload?.connected == true) R.string.gas_port_connected else R.string.gas_port_disconnected
            )
            item.tvPortState.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (payload?.connected == true) R.color.status_ok else R.color.text_secondary,
                )
            )
            item.tvPortProduct.text = getString(
                R.string.gas_port_product,
                payload?.productName ?: getString(R.string.gas_channel_not_provided),
            )
            // 最近错误提到默认可见层：这是现场最需要一眼看到的信息，无错误时整行隐藏
            val error = counts?.lastError
            item.tvPortLastError.isVisible = error != null
            item.tvPortLastError.text = if (error == null) {
                ""
            } else {
                getString(
                    R.string.gas_port_last_error,
                    error,
                    counts?.lastErrorAtMs?.let(::formatTime)
                        ?: getString(R.string.gas_channel_not_provided),
                )
            }
            item.tvPortFirmware.text = getString(
                R.string.gas_port_firmware,
                payload?.firmwareVersion ?: getString(R.string.gas_channel_not_provided),
            )
            item.tvPortFeature.text = getString(
                R.string.gas_port_feature,
                when (payload?.featureOpened) {
                    true -> getString(R.string.gas_value_yes)
                    false -> getString(R.string.gas_value_no)
                    null -> getString(R.string.gas_channel_not_provided)
                },
            )
            item.tvPortCounts.text = if (counts == null) {
                getString(R.string.gas_port_counts_unknown)
            } else {
                getString(
                    R.string.gas_port_counts,
                    counts.receivedFrames,
                    counts.decodedMeasurements,
                    counts.ignoredFrames,
                    counts.failures,
                    counts.noAdapter,
                )
            }
            // 折叠状态按端口名记忆：重建后恢复，避免用户展开后被下一次刷新收起
            applyPortExpanded(item, port.name in expandedPorts)
            item.tvPortDetailsToggle.setOnClickListener {
                val expand = port.name !in expandedPorts
                if (expand) expandedPorts.add(port.name) else expandedPorts.remove(port.name)
                applyPortExpanded(item, expand)
            }
            binding.llPorts.addView(item.root)
        }
    }

    /**
     * 展开 / 收起单个端口的诊断详情。
     *
     * 只切换可见性与入口文案；展开状态由调用方写入 [expandedPorts]，供下一次重建恢复。
     */
    private fun applyPortExpanded(item: ItemPortCardBinding, expanded: Boolean) {
        item.llPortDetails.isVisible = expanded
        item.tvPortDetailsToggle.setText(
            if (expanded) R.string.gas_device_collapse else R.string.gas_device_details
        )
    }

    /** 高级诊断折叠：默认收起，整行可点，入口文案随状态切换。 */
    private fun setAdvancedExpanded(expanded: Boolean) {
        binding.llAdvanced.isVisible = expanded
        binding.tvAdvancedState.setText(
            if (expanded) R.string.gas_device_collapse else R.string.gas_device_expand
        )
    }

    private fun renderFreshnessSummary(monitor: MonitorState) {
        val latest = latestReading(monitor)
        val threshold = (application as PsdkApplication).staleAfterMs
        binding.tvFreshnessThreshold.text = threshold
            ?.let { getString(R.string.gas_freshness_threshold, it) }
            ?: getString(R.string.gas_channel_not_provided)
        val receivedAt = latest?.receivedTimestampMs
        if (receivedAt == null) {
            binding.tvFreshnessSource.text = getString(R.string.gas_freshness_none)
            binding.tvFreshnessReceived.text = ""
            binding.tvFreshnessDelta.text = ""
            return
        }
        val sourceAt = latest.sourceTimestampMs
        binding.tvFreshnessSource.text = getString(
            R.string.gas_freshness_source,
            sourceAt?.let(::formatTime) ?: getString(R.string.gas_channel_not_provided),
        )
        binding.tvFreshnessReceived.text =
            getString(R.string.gas_freshness_received, formatTime(receivedAt))
        binding.tvFreshnessDelta.text = sourceAt
            ?.let { getString(R.string.gas_freshness_delta, receivedAt - it) }
            ?: getString(R.string.gas_channel_not_provided)
    }

    private fun renderCatalog() {
        val app = application as PsdkApplication
        val info = app.catalogInfo()
        val error = app.configError
        binding.tvCatalogUnavailable.isVisible = info == null
        binding.tvCatalogUnavailable.text = error
            ?.let { getString(R.string.gas_catalog_unavailable, it) }
            ?: getString(R.string.gas_catalog_unavailable, getString(R.string.gas_channel_not_provided))
        binding.llCatalog.isVisible = info != null
        binding.tvCatalogVersion.isVisible = info != null
        binding.tvCatalogEmpty.isVisible = info != null && info.channels.isEmpty()

        binding.llCatalog.removeAllViews()
        if (info == null) return
        binding.tvCatalogVersion.text = getString(R.string.gas_catalog_version, info.version)
        info.channels.forEach { channel ->
            val item = ItemCatalogChannelBinding.inflate(layoutInflater, binding.llCatalog, false)
            item.root.text = getString(
                R.string.gas_catalog_entry,
                channel.wireId,
                channel.channelId,
                channel.displayName,
                channel.unit,
            )
            binding.llCatalog.addView(item.root)
        }
    }

    /** 导出诊断：IO 线程写文件，主线程提示并按需调起系统分享。 */
    private fun exportDiagnostics() {
        if (lastTask == null) return
        val monitor = lastMonitor ?: return
        val ingest = lastIngest ?: return
        val app = application as PsdkApplication
        val catalog = app.catalogInfo()
        val latest = latestReading(monitor)
        val input = DiagnosticsReportInput(
            exportedAtMs = System.currentTimeMillis(),
            appVersion = BuildConfig.VERSION_NAME,
            sdkRegistered = app.sdkRegistered.value,
            pilotConnected = app.pilotConnectedState.value,
            aircraftConnected = app.aircraftConnected.value,
            ingestStarted = ingest.started,
            monitoredPorts = ingest.monitoredPorts,
            onlinePayloads = ingest.onlinePayloads,
            payloads = lastPayloads,
            diagnostics = lastDiagnostics,
            catalogVersion = catalog?.version,
            catalogChannels = catalog?.channels.orEmpty(),
            lastMeasurementSourceMs = latest?.sourceTimestampMs,
            lastMeasurementReceivedMs = latest?.receivedTimestampMs,
            configError = app.configError,
        )
        binding.btnExportDiagnostics.isEnabled = false
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = File(cacheDir, DIAGNOSTICS_DIR).apply { mkdirs() }
                    val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                    File(dir, "gas-diagnostics-$stamp.txt").apply {
                        writeText(DiagnosticsReportBuilder.build(input), Charsets.UTF_8)
                    }
                }
            }
            binding.btnExportDiagnostics.isEnabled = true
            result.onSuccess(::shareDiagnostics).onFailure { error ->
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.gas_export_failed, error.message ?: error.javaClass.simpleName),
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    private fun shareDiagnostics(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.gas_diagnostics_share_title))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(Intent.createChooser(share, getString(R.string.gas_diagnostics_share_title)))
            Toast.makeText(this, R.string.gas_export_success, Toast.LENGTH_SHORT).show()
        } catch (error: ActivityNotFoundException) {
            Log.w(TAG, "share diagnostics failed", error)
            notifyShareUnavailable(file)
        } catch (error: SecurityException) {
            Log.e(TAG, "share diagnostics denied", error)
            notifyShareUnavailable(file)
        }
    }

    private fun notifyShareUnavailable(file: File) {
        Toast.makeText(
            this,
            getString(R.string.gas_export_no_app, file.absolutePath),
            Toast.LENGTH_LONG,
        ).show()
    }

    private fun latestReading(monitor: MonitorState): ChannelReading? =
        monitor.readings
            .filter { it.receivedTimestampMs != null }
            .maxByOrNull { it.receivedTimestampMs ?: Long.MIN_VALUE }

    private fun qualityTextRes(quality: MeasurementQuality?): Int = when (quality) {
        MeasurementQuality.VALID -> R.string.gas_quality_valid
        MeasurementQuality.INVALID -> R.string.gas_quality_invalid
        MeasurementQuality.OUT_OF_RANGE -> R.string.gas_quality_out_of_range
        MeasurementQuality.SENSOR_FAULT -> R.string.gas_quality_sensor_fault
        MeasurementQuality.CALIBRATION_DUE -> R.string.gas_quality_calibration_due
        MeasurementQuality.TIME_UNCERTAIN -> R.string.gas_quality_time_uncertain
        null -> R.string.gas_channel_not_provided
    }

    private fun trendTextRes(trend: TrendDirection?): Int = when (trend) {
        TrendDirection.RISING -> R.string.gas_trend_rising
        TrendDirection.FALLING -> R.string.gas_trend_falling
        TrendDirection.STABLE -> R.string.gas_trend_stable
        TrendDirection.INSUFFICIENT, null -> R.string.gas_channel_insufficient
    }

    private fun formatTime(epochMs: Long): String = timeFormat.format(Date(epochMs))

    /**
     * 返回 Pilot 宿主当前页面。
     *
     * 独立启动（无 Pilot 宿主）时跨进程跳转无意义，给提示并停留；已连接时下发跳转命令，
     * 命令失败只记录原因并退出，不把失败呈现为成功。
     */
    private fun navigateToPilot() {
        if ((application as PsdkApplication).pilotConnectedState.value != true) {
            Toast.makeText(this, R.string.entry_pilot_not_connected, Toast.LENGTH_SHORT).show()
            return
        }
        NavigateManager.getInstance().navigateToPage(
            NavigateAction(NavigateAction.Value.PILOT_DEFAULT, ""),
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() = Unit

                override fun onFailure(error: IDJIError) {
                    Log.e(TAG, "navigateToPilot failed: $error")
                    runOnUiThread { finish() }
                }
            },
        )
    }

    /** 合并连接状态与服务健康状态，生成一次完整快照后统一渲染，避免徽章出现中间帧。 */
    private fun observeSdkState() {
        val app = application as PsdkApplication
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    app.sdkRegistered,
                    app.pilotConnectedState,
                    app.aircraftConnected,
                    app.serviceHealth.state,
                ) { sdkRegistered, pilotConnected, aircraftConnected, serviceState ->
                    EntryUiState.of(sdkRegistered, pilotConnected, aircraftConnected) to serviceState
                }.collect { (state, serviceState) -> render(state, serviceState) }
            }
        }
    }

    /**
     * 渲染标题栏两枚徽章。
     *
     * Pilot 徽章只有「已连接 / 未连接」两态；服务徽章按健康探测结果展示「检测中 / 已连接 / 不可用」。
     * 背景、文字色与圆点色统一由 [BadgeTone] 决定，保证三者语义一致。
     */
    private fun render(state: EntryUiState, serviceState: ServiceConnectionState) {
        renderTitleBadge(
            pill = binding.pillPilot,
            dot = binding.dotPilot,
            label = binding.tvPilotState,
            tone = if (state.pilotConnected) BadgeTone.READY else BadgeTone.ERROR,
            textRes = if (state.pilotConnected) {
                R.string.entry_pilot_connected
            } else {
                R.string.entry_pilot_disconnected
            },
        )

        val serviceTextRes: Int
        val serviceTone: BadgeTone
        when (serviceState) {
            ServiceConnectionState.CONNECTED -> {
                serviceTextRes = R.string.entry_service_connected
                serviceTone = BadgeTone.READY
            }

            ServiceConnectionState.UNAVAILABLE -> {
                serviceTextRes = R.string.entry_service_unavailable
                serviceTone = BadgeTone.ERROR
            }

            ServiceConnectionState.CHECKING -> {
                serviceTextRes = R.string.entry_service_checking
                serviceTone = BadgeTone.NEUTRAL
            }
        }
        renderTitleBadge(
            pill = binding.pillService,
            dot = binding.dotService,
            label = binding.tvServiceState,
            tone = serviceTone,
            textRes = serviceTextRes,
        )

        if (lastStatus != state.status) {
            lastStatus = state.status
            Log.d(
                TAG,
                "Entry state: sdkRegistered=${state.sdkRegistered}, " +
                    "pilotConnected=${state.pilotConnected}, status=${state.status}, service=$serviceState"
            )
        }
    }

    /** 单枚标题栏徽章的视觉档位：背景 + 文字/圆点共用的语义色。 */
    private enum class BadgeTone(val backgroundRes: Int, val colorRes: Int) {
        /** 已连接 / 可达。 */
        READY(R.drawable.bg_title_badge_ok, R.color.demo_top_bar_connected),

        /** 未连接 / 不可用。 */
        ERROR(R.drawable.bg_title_badge_bad, R.color.demo_top_bar_error),

        /** 检测中，尚无结论。 */
        NEUTRAL(R.drawable.bg_title_badge_checking, R.color.demo_top_bar_neutral),
    }

    /** 按档位刷新一枚徽章的背景、圆点色与文字，避免三处颜色各自漂移。 */
    private fun renderTitleBadge(pill: View, dot: View, label: TextView, tone: BadgeTone, textRes: Int) {
        val color = ContextCompat.getColor(this, tone.colorRes)
        pill.setBackgroundResource(tone.backgroundRes)
        dot.backgroundTintList = ColorStateList.valueOf(color)
        label.setText(textRes)
        label.setTextColor(color)
    }

    /** 任务页一次渲染所需的四份状态快照，避免在 collect 里拆成多层临时对象。 */
    private data class TaskRenderInput(
        val task: TaskUiState,
        val monitor: MonitorState,
        val auto: AutoFlightState,
        val vessel: RecognitionState,
        val follow: FollowState,
    )

    private enum class Tab { TASKS, DEVICE, TRANSPORT }

    private companion object {
        private const val TAG = "GasEntry"
        private const val DIAGNOSTICS_DIR = "diagnostics"
        private const val FRESHNESS_TICK_MS = 1_000L
    }
}
