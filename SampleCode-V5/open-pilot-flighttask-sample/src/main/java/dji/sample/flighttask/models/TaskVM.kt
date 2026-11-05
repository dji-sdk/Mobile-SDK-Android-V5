package dji.sample.flighttask.models

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.text.format.Formatter
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import dji.open.pilot.base.ui.NavigateAction
import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.FontStyleType
import dji.open.pilot.base.ui.model.GravityType
import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.base.ui.model.ScaleType
import dji.open.pilot.base.ui.model.ShapeDescriptor
import dji.open.pilot.base.ui.model.VisibilityType
import dji.open.pilot.base.ui.model.WidgetLayoutParam
import dji.open.pilot.client.manager.ui.NavigateManager
import dji.sample.flighttask.FlightTaskApplication
import dji.sample.flighttask.R
import dji.sample.flighttask.data.Asset
import dji.sample.flighttask.data.AssetState
import dji.sample.flighttask.data.Job
import dji.sample.flighttask.data.JobStatus
import dji.sample.flighttask.data.MediaReference
import dji.sample.flighttask.data.ResultCallback
import dji.sample.flighttask.data.UploadProgress
import dji.sample.flighttask.data.WaylineProgress
import dji.sample.flighttask.data.WaylineState
import dji.sample.flighttask.manager.AssetUploadManager
import dji.sample.flighttask.manager.CloudManager
import dji.sample.flighttask.manager.MediaManager
import dji.sample.flighttask.manager.TaskManager
import dji.sample.flighttask.manager.WaylineManager
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.utils.common.FileUtils
import dji.v5.ux.remoteview.RemoteLinearLayout
import dji.v5.ux.remoteview.RemoteProgressBar
import dji.v5.ux.remoteview.RemoteText
import dji.v5.ux.remoteview.RemoteViewHelper
import dji.v5.ux.util.RemoteViewColors
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/** Manages task data and the Pilot floating task window. */
class TaskVM : ViewModel() {

    companion object {
        private const val TAG = "TaskVM"
        private const val EMPTY_TASK_ICON = "icon_task_empty_v2.webp"
        private const val SELECTION_PREFERENCES = "flight-task-selection"
    }

    enum class State { IDLE, JOB_LIST, PREPARING, EXECUTING, SYNCING, ERROR, DONE }

    val state = MutableLiveData(State.IDLE)
    val jobs = TaskManager.jobs
    val activeJob = TaskManager.activeJob
    val syncState = TaskManager.syncState
    val taskError = TaskManager.error
    val backendConnectionState = CloudManager.backendConnectionState

    private val mutableSelectedJobId = MutableLiveData<String?>(null)
    val selectedJobId: LiveData<String?> = mutableSelectedJobId

    private val helper = RemoteViewHelper.floatingBoxHelper
    private val mainHandler = Handler(Looper.getMainLooper())
    private val isLayoutInitialized = AtomicBoolean(false)
    private val isIconUploading = AtomicBoolean(false)
    private val containerIds = mutableMapOf<State, String>()

    private var observersRegistered = false
    private var selectionPreferences: SharedPreferences? = null
    private var selectionPreferenceKey: String? = null
    private var pilotConnected = false
    private var pendingJobId: String? = null
    private var activeJobId: String? = null
    private var executionStarting = false
    private var latestJobs: List<Job> = emptyList()
    private var latestWaylineState = WaylineState.IDLE
    private var latestWaylineProgress = WaylineProgress()
    private var latestAssets: List<Asset> = emptyList()
    private var latestMediaReferences: List<MediaReference> = emptyList()
    private var latestQueue: List<Asset> = emptyList()
    private var latestUploadProgress: Map<String, UploadProgress> = emptyMap()
    private var errorMessage: String? = null

    private var jobTitleText: RemoteText? = null
    private var jobPositionText: RemoteText? = null
    private var jobDeadlineText: RemoteText? = null
    private var jobSwitchRowId: String? = null
    private var preparingJobText: RemoteText? = null
    private var preparingStatusText: RemoteText? = null
    private var executingStatusText: RemoteText? = null
    private var waypointText: RemoteText? = null
    private var capturedText: RemoteText? = null
    private var uploadedText: RemoteText? = null
    private var executingProgress: RemoteProgressBar? = null
    private var syncFileText: RemoteText? = null
    private var syncByteText: RemoteText? = null
    private var syncingProgress: RemoteProgressBar? = null
    private var syncDownloadText: RemoteText? = null
    private var errorCountText: RemoteText? = null
    private var errorTitleText: RemoteText? = null
    private var errorText: RemoteText? = null
    private var doneFileText: RemoteText? = null
    private var doneSizeText: RemoteText? = null
    private var doneTitleText: RemoteText? = null

    private val jobsObserver = Observer<List<Job>> { value ->
        runOnMain {
            latestJobs = value.orEmpty()
            val preservedActive = activeJobId?.let { activeId ->
                latestJobs.firstOrNull {
                    it.jobId == activeId &&
                        (it.status == JobStatus.ACCEPTED || it.status == JobStatus.IN_PROGRESS || it.status == JobStatus.FINALIZING)
                }
            }
            val active = preservedActive ?: latestJobs.firstOrNull {
                it.status == JobStatus.ACCEPTED || it.status == JobStatus.IN_PROGRESS || it.status == JobStatus.FINALIZING
            }
            activeJobId = active?.jobId

            val selected = mutableSelectedJobId.value
            if (latestJobs.none { it.jobId == selected }) {
                publishSelectedJob(active?.jobId ?: pendingJobs().firstOrNull()?.jobId ?: latestJobs.firstOrNull()?.jobId)
            }

            val pending = pendingJobs()
            val selectedPending = pending.firstOrNull { it.jobId == mutableSelectedJobId.value }
            if (selectedPending != null) pendingJobId = selectedPending.jobId
            else if (pendingJobId !in pending.map { it.jobId }) pendingJobId = pending.firstOrNull()?.jobId
            val focus = active ?: latestJobs.firstOrNull { it.jobId == mutableSelectedJobId.value }
            val terminal = focus?.status == JobStatus.COMPLETED || focus?.status == JobStatus.CANCELLED
            val interrupted = focus?.status == JobStatus.IN_PROGRESS && latestWaylineState == WaylineState.IDLE && !executionStarting
            if (interrupted) errorMessage = string(R.string.fw_route_interrupted)
            val hasError = errorMessage != null || jobQueue().any { it.state == AssetState.FAILED }
            if (interrupted) setState(State.ERROR)
            else if (!terminal && state.value == State.ERROR && hasError) renderIfConnected()
            else setState(stateFor(focus, pending.isNotEmpty()))
        }
    }

    private val waylineStateObserver = Observer<WaylineState> { value ->
        runOnMain {
            latestWaylineState = value ?: WaylineState.IDLE
            val status = currentJob()?.status
            when {
                latestWaylineState == WaylineState.FAILED -> {
                    errorMessage = WaylineManager.error.value
                    setState(State.ERROR)
                }
                status == JobStatus.FINALIZING || latestWaylineState == WaylineState.FINISHED -> setState(State.SYNCING)
                status == JobStatus.IN_PROGRESS && latestWaylineState in setOf(WaylineState.EXECUTING, WaylineState.PAUSED) ->
                    setState(State.EXECUTING)
                status == JobStatus.ACCEPTED && latestWaylineState in setOf(WaylineState.PREPARING, WaylineState.READY) ->
                    setState(State.PREPARING)
                else -> renderIfConnected()
            }
        }
    }

    private val waylineProgressObserver = Observer<WaylineProgress> { value ->
        runOnMain {
            latestWaylineProgress = value ?: WaylineProgress()
            renderIfConnected()
        }
    }

    private val assetsObserver = Observer<List<Asset>> { value ->
        runOnMain {
            latestAssets = value.orEmpty()
            renderIfConnected()
        }
    }

    private val mediaReferencesObserver = Observer<List<MediaReference>> { value ->
        runOnMain {
            latestMediaReferences = value.orEmpty()
            renderIfConnected()
        }
    }

    private val queueObserver = Observer<List<Asset>> { value ->
        runOnMain {
            latestQueue = value.orEmpty()
            val failed = jobQueue().count { it.state == AssetState.FAILED }
            if (failed > 0) {
                errorMessage = null
                setState(State.ERROR)
            } else {
                renderIfConnected()
            }
        }
    }

    private val uploadProgressObserver = Observer<Map<String, UploadProgress>> { value ->
        runOnMain {
            latestUploadProgress = value.orEmpty()
            renderIfConnected()
        }
    }

    var onStartExecution: (Job, ResultCallback<Unit>) -> Unit = { _, callback ->
        callback.onFailure(IllegalStateException("Wayline flow is not connected"))
    }

    var onRetrySync: () -> Unit = {
        val jobId = currentJobId()
        latestQueue.filter { it.jobId == jobId && it.state == AssetState.FAILED }
            .forEach { AssetUploadManager.retry(it.jobId, it.assetId) }
    }

    var onRetryFinalization: (Job, ResultCallback<Unit>) -> Unit = { _, callback ->
        callback.onFailure(IllegalStateException("Finalization flow is not connected"))
    }
    var onPauseFinalization: (Job) -> Unit = {}

    fun start(context: Context, rcSerialNumber: String) {
        selectionPreferences = context.getSharedPreferences(SELECTION_PREFERENCES, Context.MODE_PRIVATE)
        selectionPreferenceKey = "selected-$rcSerialNumber"
        if (mutableSelectedJobId.value == null) {
            mutableSelectedJobId.value = selectionPreferences?.getString(selectionPreferenceKey, null)
        }
        if (!observersRegistered) {
            observersRegistered = true
            TaskManager.jobs.observeForever(jobsObserver)
            WaylineManager.state.observeForever(waylineStateObserver)
            WaylineManager.progress.observeForever(waylineProgressObserver)
            MediaManager.localAssets.observeForever(assetsObserver)
            MediaManager.mediaReferences.observeForever(mediaReferencesObserver)
            AssetUploadManager.queue.observeForever(queueObserver)
            AssetUploadManager.progress.observeForever(uploadProgressObserver)
        }
        TaskManager.start(context, rcSerialNumber)
    }

    fun refreshTasks() = TaskManager.refresh()

    fun ensurePolling() = TaskManager.ensurePolling()

    fun acceptTask(jobId: String) {
        TaskManager.accept(jobId, object : ResultCallback<Job> {
            override fun onSuccess(data: Job) = runOnMain {
                activeJobId = data.jobId
                pendingJobId = data.jobId
                publishSelectedJob(data.jobId)
                errorMessage = null
                setState(State.PREPARING)
            }

            override fun onFailure(error: Throwable) = runOnMain {
                errorMessage = error.message
                setState(State.ERROR)
            }
        })
    }

    fun selectJob(jobId: String) = runOnMain {
        val job = latestJobs.firstOrNull { it.jobId == jobId } ?: return@runOnMain
        publishSelectedJob(jobId)
        if (job.status == JobStatus.COMPLETED || job.status == JobStatus.CANCELLED) {
            activeJobId = null
            setState(State.DONE)
        } else if (job.status == JobStatus.DISPATCHED) {
            pendingJobId = jobId
            val hasActiveTask = latestJobs.any {
                it.status == JobStatus.ACCEPTED || it.status == JobStatus.IN_PROGRESS || it.status == JobStatus.FINALIZING
            }
            if (!hasActiveTask && state.value != State.JOB_LIST) setState(State.JOB_LIST)
            else updateJobListViews()
        }
    }

    fun ignoreTask() {
        pendingJobId = null
        publishSelectedJob(pendingJobs().firstOrNull()?.jobId)
        setState(if (pendingJobs().isEmpty()) State.IDLE else State.JOB_LIST)
    }

    fun rejectTask(jobId: String) {
        TaskManager.cancel(jobId, object : ResultCallback<Job> {
            override fun onSuccess(data: Job) = runOnMain {
                activeJobId = null
                pendingJobId = null
                publishSelectedJob(data.jobId)
                errorMessage = null
                setState(State.DONE)
            }

            override fun onFailure(error: Throwable) = runOnMain {
                errorMessage = error.message
                setState(State.ERROR)
            }
        })
    }

    fun startTask(jobId: String) = TaskManager.start(jobId)

    fun completeTask(jobId: String) = TaskManager.complete(jobId)

    fun onPilotDisconnected() {
        pilotConnected = false
        isLayoutInitialized.set(false)
        isIconUploading.set(false)
        containerIds.clear()
        clearViewReferences()
        RemoteViewHelper.clearUploadCache()
    }

    override fun onCleared() {
        TaskManager.jobs.removeObserver(jobsObserver)
        WaylineManager.state.removeObserver(waylineStateObserver)
        WaylineManager.progress.removeObserver(waylineProgressObserver)
        MediaManager.localAssets.removeObserver(assetsObserver)
        MediaManager.mediaReferences.removeObserver(mediaReferencesObserver)
        AssetUploadManager.queue.removeObserver(queueObserver)
        AssetUploadManager.progress.removeObserver(uploadProgressObserver)
        TaskManager.stopPolling()
        super.onCleared()
    }

    fun render(target: State = state.value ?: State.IDLE) {
        pilotConnected = true
        if (!isLayoutInitialized.get()) {
            uploadIconAndInit()
            return
        }
        updateDynamicViews()
        switchVisibility(target)
    }

    private fun uploadIconAndInit() {
        if (!isIconUploading.compareAndSet(false, true)) return
        val context = FlightTaskApplication.instance
        val iconFile = File(context.getExternalFilesDir(null), EMPTY_TASK_ICON)
        FileUtils.copyAssetsFileIfNeed(context, "plugin/$EMPTY_TASK_ICON", iconFile)
        helper.pushImageFile(iconFile.path, object : CommonCallbacks.CompletionCallbackWithProgress<Double> {
            override fun onProgressUpdate(progress: Double) = Unit

            override fun onSuccess() = runOnMain {
                isIconUploading.set(false)
                if (!pilotConnected) return@runOnMain
                initLayout()
                updateDynamicViews()
                switchVisibility(state.value ?: State.IDLE)
            }

            override fun onFailure(error: IDJIError) {
                isIconUploading.set(false)
                Log.e(TAG, "upload empty task icon failed: $error")
            }
        })
    }

    /** Creates every state container once. Later renders only update properties and visibility. */
    private fun initLayout() {
        if (!isLayoutInitialized.compareAndSet(false, true)) return
        helper.clearLayout()
        helper.setPageLayoutParamWrapContent()

        val root = helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        root.addToRoot()
        addHeader(root)
        addIdleContainer(root)
        addJobListContainer(root)
        addPreparingContainer(root)
        addExecutingContainer(root)
        addSyncingContainer(root)
        addErrorContainer(root)
        addDoneContainer(root)
    }

    private fun addHeader(root: RemoteLinearLayout) {
        val header = helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(42)
            setGravity(GravityType.CENTER_VERTICAL)
        }
        root.addToRemote(header)
        header.addToRemote(createText(R.string.fw_title, bold = true).apply {
            setWidth(0)
            setHeight(RemoteView.MATCH_PARENT)
            setGravity(GravityType.CENTER)
        }, WidgetLayoutParam().apply { weight = 1f })
        root.addToRemote(helper.createText().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(1)
            setBackgroundShape(ShapeDescriptor.Builder().fillColor(RemoteViewColors.LIGHT_GREY).build())
        }, helper.margin(left = 10, right = 10))
    }

    private fun addIdleContainer(root: RemoteLinearLayout) {
        val container = createContainer(root, State.IDLE, 112).apply { setGravity(GravityType.CENTER) }
        container.addToRemote(helper.createImage().apply {
            setWidth(28)
            setHeight(28)
            setImageFileName(EMPTY_TASK_ICON)
            setScaleType(ScaleType.FIT_CENTER)
        }, WidgetLayoutParam().apply { layoutGravity = GravityType.CENTER_HORIZONTAL })
        container.addToRemote(createText(R.string.fw_idle).apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(28)
            setGravity(GravityType.CENTER)
        }, helper.margin(top = 5))
    }

    private fun addJobListContainer(root: RemoteLinearLayout) {
        val container = createContainer(root, State.JOB_LIST)
        jobPositionText = addDynamicText(container).apply {
            setGravity(GravityType.CENTER)
            syncToRemote()
        }
        val taskCard = helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setPadding(8, 8, 8, 8)
            setBackgroundShape(
                ShapeDescriptor.Builder()
                    .fillColor(RemoteViewColors.BLACK_60)
                    .cornerRadius(6f)
                    .build()
            )
        }
        container.addToRemote(taskCard, contentMargin())
        jobTitleText = addDynamicText(taskCard, bold = true)
        jobDeadlineText = addDynamicText(taskCard)
        val switchRow = addButtonRow(container)
        jobSwitchRowId = switchRow.getId()
        switchRow.addToRemote(createButton(R.string.fw_btn_previous, width = 0) { selectPendingJob(-1) }, weightedMargin())
        switchRow.addToRemote(createButton(R.string.fw_btn_next, width = 0) { selectPendingJob(1) }, weightedMargin())
        val actionRow = addButtonRow(container)
        actionRow.addToRemote(createButton(R.string.fw_btn_confirm, width = 0) {
            selectedPendingJob()?.let { acceptTask(it.jobId) }
        }, weightedMargin())
        actionRow.addToRemote(createButton(R.string.fw_btn_reject, filled = false, width = 0) {
            selectedPendingJob()?.let { rejectTask(it.jobId) }
        }, weightedMargin())
        container.addToRemote(createButton(R.string.fw_btn_task_detail, filled = false) { navigateToPluginApp() }, contentMargin())
    }

    private fun addPreparingContainer(root: RemoteLinearLayout) {
        val container = createContainer(root, State.PREPARING)
        container.addToRemote(createText(R.string.fw_preparing, bold = true), contentMargin())
        preparingJobText = addDynamicText(container)
        container.addToRemote(createText(R.string.fw_start_hint), contentMargin())
        preparingStatusText = addDynamicText(container)
        container.addToRemote(createButton(R.string.fw_btn_start_execution) { startExecution() }, contentMargin())
        container.addToRemote(createButton(R.string.fw_btn_task_detail, filled = false) { navigateToPluginApp() }, contentMargin())
    }

    private fun addExecutingContainer(root: RemoteLinearLayout) {
        val container = createContainer(root, State.EXECUTING)
        container.addToRemote(createText(R.string.fw_executing, bold = true), contentMargin())
        executingStatusText = addDynamicText(container)
        waypointText = addDynamicText(container)
        executingProgress = helper.createProgressBar().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(8)
            setProgressColor(RemoteViewColors.BLUE_PRIMARY)
        }.also { container.addToRemote(it, contentMargin()) }
        val photoRow = helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }.also { container.addToRemote(it, contentMargin()) }
        capturedText = helper.createText().apply {
            setWidth(0)
            setHeight(RemoteView.WRAP_CONTENT)
            setGravity(GravityType.CENTER)
            setTextSize(13f)
        }.also { photoRow.addToRemote(it, weightedMargin()) }
        uploadedText = helper.createText().apply {
            setWidth(0)
            setHeight(RemoteView.WRAP_CONTENT)
            setGravity(GravityType.CENTER)
            setTextSize(13f)
        }.also { photoRow.addToRemote(it, weightedMargin()) }
        container.addToRemote(createButton(R.string.fw_btn_task_detail, filled = false) { navigateToPluginApp() }, contentMargin())
    }

    private fun addSyncingContainer(root: RemoteLinearLayout) {
        val container = createContainer(root, State.SYNCING)
        container.addToRemote(createText(R.string.fw_syncing, bold = true), contentMargin())
        syncingProgress = helper.createProgressBar().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(8)
            setProgressColor(RemoteViewColors.BLUE_PRIMARY)
        }.also { container.addToRemote(it, contentMargin()) }
        syncDownloadText = addDynamicText(container)
        syncFileText = addDynamicText(container)
        syncByteText = addDynamicText(container)
        val actionRow = addButtonRow(container)
        actionRow.addToRemote(createButton(R.string.fw_btn_retry_photos, width = 0) {
            currentJob()?.let(::retryFinalizationWithFeedback)
        }, weightedMargin())
        actionRow.addToRemote(createButton(R.string.fw_btn_pause_sync, filled = false, width = 0) {
            currentJob()?.let { job ->
                onPauseFinalization(job)
                showToast(R.string.fw_sync_paused_toast)
            }
        }, weightedMargin())
        container.addToRemote(createButton(R.string.fw_btn_task_detail, filled = false) { navigateToPluginApp() }, contentMargin())
    }

    private fun addErrorContainer(root: RemoteLinearLayout) {
        val container = createContainer(root, State.ERROR)
        errorTitleText = addDynamicText(container, bold = true)
        errorCountText = addDynamicText(container)
        errorText = addDynamicText(container)
        container.addToRemote(createButton(R.string.fw_btn_retry) {
            errorMessage = null
            val job = currentJob()
            if (job?.status == JobStatus.FINALIZING) {
                setState(State.SYNCING)
                onRetrySync()
                retryFinalizationWithFeedback(job)
            } else if (currentUploadProgress().failed > 0) {
                setState(State.SYNCING)
                onRetrySync()
            } else {
                setState(State.PREPARING)
                startExecution()
            }
        }, contentMargin())
        container.addToRemote(createButton(R.string.fw_btn_task_detail, filled = false) { navigateToPluginApp() }, contentMargin())
    }

    private fun addDoneContainer(root: RemoteLinearLayout) {
        val container = createContainer(root, State.DONE)
        doneTitleText = addDynamicText(container, bold = true)
        doneFileText = addDynamicText(container)
        doneSizeText = addDynamicText(container)
        container.addToRemote(createButton(R.string.fw_btn_back_jobs) {
            activeJobId = null
            pendingJobId = pendingJobs().firstOrNull()?.jobId
            publishSelectedJob(pendingJobId)
            setState(if (pendingJobId == null) State.IDLE else State.JOB_LIST)
        }, contentMargin())
    }

    private fun createContainer(root: RemoteLinearLayout, state: State, height: Int = RemoteView.WRAP_CONTENT): RemoteLinearLayout {
        return helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(height)
            setPadding(0, 8, 0, 8)
            setVisibility(VisibilityType.GONE)
            root.addToRemote(this)
            containerIds[state] = getId()
        }
    }

    private fun createText(resId: Int, bold: Boolean = false): RemoteText = helper.createText().apply {
        setTextByResId(resId)
        setWidth(RemoteView.MATCH_PARENT)
        setHeight(RemoteView.WRAP_CONTENT)
        setTextSize(13f)
        if (bold) setFontStyle(FontStyleType.BOLD)
    }

    private fun addDynamicText(parent: RemoteLinearLayout, bold: Boolean = false): RemoteText {
        return helper.createText().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setTextSize(13f)
            if (bold) setFontStyle(FontStyleType.BOLD)
            parent.addToRemote(this, contentMargin())
        }
    }

    private fun addButtonRow(parent: RemoteLinearLayout): RemoteLinearLayout {
        return helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            parent.addToRemote(this, helper.margin(top = 4, left = 6, right = 6))
        }
    }

    private fun createButton(
        resId: Int,
        filled: Boolean = true,
        width: Int = RemoteView.MATCH_PARENT,
        onClick: () -> Unit,
    ) = helper.createButton().apply {
        setTextByResId(resId)
        setWidth(width)
        setHeight(32)
        setTextSize(12f)
        if (filled) {
            setTextColor(RemoteViewColors.WHITE)
            setBackgroundShape(ShapeDescriptor.Builder().fillColor(RemoteViewColors.BLUE_PRIMARY).cornerRadius(4f).build())
        } else {
            setBackgroundShape(ShapeDescriptor.Builder().fillColor(RemoteViewColors.NEAR_WHITE_20).cornerRadius(4f).build())
        }
        setOnClickListener { runOnMain(onClick) }
    }

    private fun contentMargin() = helper.margin(top = 3, bottom = 3, left = 10, right = 10)

    private fun weightedMargin() = WidgetLayoutParam().apply {
        weight = 1f
        marginLeft = 4
        marginRight = 4
    }

    private fun selectPendingJob(offset: Int) {
        val pending = pendingJobs()
        if (pending.isEmpty()) return
        val current = pending.indexOfFirst { it.jobId == pendingJobId }.let { if (it < 0) 0 else it }
        pendingJobId = pending[(current + offset + pending.size) % pending.size].jobId
        publishSelectedJob(pendingJobId)
        updateJobListViews()
    }

    private fun startExecution() {
        if (executionStarting) return
        val job = currentJob() ?: return
        executionStarting = true
        updatePreparingViews()
        onStartExecution(job, object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) = runOnMain {
                executionStarting = false
                errorMessage = null
                setState(State.EXECUTING)
            }

            override fun onFailure(error: Throwable) = runOnMain {
                executionStarting = false
                errorMessage = error.message
                setState(State.ERROR)
            }
        })
    }

    private fun setState(target: State) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { setState(target) }
            return
        }
        if (state.value != target) {
            Log.i(TAG, "state: ${state.value} -> $target")
            state.value = target
        }
        renderIfConnected()
    }

    private fun renderIfConnected() {
        if (pilotConnected) render(state.value ?: State.IDLE)
    }

    private fun updateDynamicViews() {
        updateJobListViews()
        updatePreparingViews()
        updateExecutingViews()
        updateSyncViews()
        updateErrorViews()
        updateDoneViews()
    }

    private fun updateJobListViews() {
        val job = selectedPendingJob() ?: return
        val pending = pendingJobs()
        val index = pending.indexOfFirst { it.jobId == job.jobId }.coerceAtLeast(0)
        setRemoteText(jobPositionText, string(R.string.fw_job_position, index + 1, pending.size))
        setRemoteText(jobTitleText, job.title)
        setRemoteText(jobDeadlineText, string(R.string.fw_job_deadline, job.extras["deadline"].orEmpty()))
        jobSwitchRowId?.let { id ->
            helper.updateView(
                id,
                RemoteViewProperty.VISIBILITY,
                (if (pending.size > 1) VisibilityType.VISIBLE else VisibilityType.GONE).value.toString(),
            )
        }
    }

    private fun updatePreparingViews() {
        val job = currentJob() ?: return
        setRemoteText(preparingJobText, job.title)
        setRemoteText(
            preparingStatusText,
            string(if (executionStarting || latestWaylineState == WaylineState.PREPARING) R.string.fw_uploading_wayline else R.string.fw_ready_to_start),
        )
    }

    private fun updateExecutingViews() {
        val progress = latestWaylineProgress
        val statusRes = if (latestWaylineState == WaylineState.PAUSED) R.string.fw_wayline_paused else R.string.fw_wayline_executing
        setRemoteText(executingStatusText, string(R.string.fw_route_status_value, string(statusRes)))
        setRemoteText(waypointText, string(R.string.fw_current_waypoint, progress.currentWaypoint, progress.totalWaypoints))
        executingProgress?.apply {
            val max = if (progress.totalWaypoints > 0) progress.totalWaypoints else 100
            setMax(max)
            setProgress(progress.currentWaypoint.coerceIn(0, max))
            syncToRemote()
        }
        val jobId = executionJobId()
        val photos = latestMediaReferences.count { it.jobId == jobId }
        val uploaded = latestQueue.count { it.jobId == jobId && it.typeId == "photo" && it.state == AssetState.UPLOADED }
        setRemoteText(capturedText, string(R.string.fw_photos_captured, photos))
        setRemoteText(uploadedText, string(R.string.fw_photos_uploaded, uploaded))
    }

    private fun updateSyncViews() {
        val jobId = executionJobId()
        val references = latestMediaReferences.count { it.jobId == jobId }
        val downloaded = latestAssets.count { it.jobId == jobId && it.typeId == "photo" }
        val photoQueue = latestQueue.filter { it.jobId == jobId && it.typeId == "photo" }
        val uploaded = photoQueue.count { it.state == AssetState.UPLOADED }
        val downloading = references > downloaded
        val max = if (downloading) references.coerceAtLeast(1) else photoQueue.size.coerceAtLeast(1)
        val value = if (downloading) downloaded else uploaded.coerceAtMost(max)
        syncingProgress?.apply {
            setMax(max)
            setProgress(value.coerceIn(0, max))
            syncToRemote()
        }
        setRemoteText(syncDownloadText, string(R.string.fw_download_progress, downloaded, references))
        val totalBytes = latestMediaReferences.filter { it.jobId == jobId }.sumOf { it.fileSize }
        val uploadedBytes = photoQueue.filter { it.state == AssetState.UPLOADED }.sumOf { it.sizeBytes }
        setRemoteText(syncFileText, string(R.string.fw_file_progress, uploaded, references))
        setRemoteText(syncByteText, string(R.string.fw_byte_progress, formatBytes(uploadedBytes), formatBytes(totalBytes)))
    }

    private fun updateErrorViews() {
        val progress = currentUploadProgress()
        setRemoteText(
            errorTitleText,
            string(if (progress.failed > 0) R.string.fw_failed else R.string.fw_execution_failed),
        )
        setRemoteText(errorCountText, string(R.string.fw_failed_count, progress.failed))
        setRemoteText(errorText, errorMessage ?: string(R.string.fw_upload_failed_message))
    }

    private fun updateDoneViews() {
        val jobId = executionJobId()
        val uploadedPhotos = latestQueue.filter {
            it.jobId == jobId && it.typeId == "photo" && it.state == AssetState.UPLOADED
        }
        setRemoteText(doneTitleText, string(if (currentJob()?.status == JobStatus.CANCELLED) R.string.fw_cancelled else R.string.fw_done))
        setRemoteText(doneFileText, string(R.string.fw_done_files, uploadedPhotos.size))
        setRemoteText(doneSizeText, string(R.string.fw_done_size, formatBytes(uploadedPhotos.sumOf { it.sizeBytes })))
    }

    private fun switchVisibility(target: State) {
        containerIds.forEach { (containerState, id) ->
            helper.updateView(
                id,
                RemoteViewProperty.VISIBILITY,
                (if (containerState == target) VisibilityType.VISIBLE else VisibilityType.GONE).value.toString(),
            )
        }
    }

    private fun selectedPendingJob(): Job? = pendingJobs().firstOrNull { it.jobId == pendingJobId }
        ?: pendingJobs().firstOrNull()

    private fun pendingJobs(): List<Job> = latestJobs.filter { it.status == JobStatus.DISPATCHED }

    private fun currentJobId(): String? {
        val selected = latestJobs.firstOrNull { it.jobId == mutableSelectedJobId.value }
        if (selected?.status == JobStatus.COMPLETED || selected?.status == JobStatus.CANCELLED) return selected.jobId
        return activeJobId ?: pendingJobId ?: selected?.jobId
    }

    private fun executionJobId(): String? = WaylineManager.preparedJobId ?: currentJobId()

    private fun currentJob(): Job? = currentJobId()?.let { id -> latestJobs.firstOrNull { it.jobId == id } }

    private fun jobQueue(): List<Asset> {
        val jobId = executionJobId()
        return latestQueue.filter { it.jobId == jobId }
    }

    private fun currentUploadProgress(): UploadProgress {
        val jobId = executionJobId().orEmpty()
        return latestUploadProgress[jobId] ?: UploadProgress(jobId, 0, 0, 0)
    }

    private fun setRemoteText(view: RemoteText?, text: String) {
        view?.apply {
            setText(text)
            syncToRemote()
        }
    }

    private fun retryFinalizationWithFeedback(job: Job) {
        showToast(R.string.fw_retry_started_toast)
        onRetryFinalization(job, object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) = runOnMain {
                showToast(R.string.fw_retry_success_toast)
            }

            override fun onFailure(error: Throwable) = runOnMain {
                val reason = if (error.message?.contains("Pending photos", ignoreCase = true) == true ||
                    error.message?.contains("still pending", ignoreCase = true) == true
                ) {
                    string(R.string.fw_photos_not_found_toast)
                } else {
                    error.message ?: error.javaClass.simpleName
                }
                Toast.makeText(
                    FlightTaskApplication.instance,
                    string(R.string.fw_retry_failed_toast, reason),
                    Toast.LENGTH_LONG,
                ).show()
            }
        })
    }

    private fun showToast(resId: Int) {
        Toast.makeText(FlightTaskApplication.instance, resId, Toast.LENGTH_SHORT).show()
    }

    private fun string(resId: Int, vararg args: Any): String =
        FlightTaskApplication.instance.getString(resId, *args)

    private fun publishSelectedJob(jobId: String?) {
        if (mutableSelectedJobId.value != jobId) mutableSelectedJobId.value = jobId
        val key = selectionPreferenceKey ?: return
        selectionPreferences?.edit()?.apply {
            if (jobId == null) remove(key) else putString(key, jobId)
        }?.apply()
    }

    private fun stateFor(job: Job?, hasPending: Boolean): State = when (job?.status) {
        JobStatus.DISPATCHED -> State.JOB_LIST
        JobStatus.ACCEPTED -> State.PREPARING
        JobStatus.IN_PROGRESS -> if (latestWaylineState == WaylineState.FINISHED) State.SYNCING else State.EXECUTING
        JobStatus.FINALIZING -> State.SYNCING
        JobStatus.COMPLETED, JobStatus.CANCELLED -> State.DONE
        null -> if (hasPending) State.JOB_LIST else State.IDLE
    }

    private fun formatBytes(bytes: Long): String =
        Formatter.formatShortFileSize(FlightTaskApplication.instance, bytes)

    private fun clearViewReferences() {
        jobTitleText = null
        jobPositionText = null
        jobDeadlineText = null
        jobSwitchRowId = null
        preparingJobText = null
        preparingStatusText = null
        executingStatusText = null
        waypointText = null
        capturedText = null
        uploadedText = null
        executingProgress = null
        syncDownloadText = null
        syncFileText = null
        syncByteText = null
        syncingProgress = null
        errorCountText = null
        errorTitleText = null
        errorText = null
        doneFileText = null
        doneSizeText = null
        doneTitleText = null
    }

    private fun runOnMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action() else mainHandler.post(action)
    }

    private fun navigateToPluginApp() {
        NavigateManager.getInstance().navigateToPage(
            NavigateAction(NavigateAction.Value.SELF_DEFINE, "dji.sample.flighttask.PluginActivity"),
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() = Unit
                override fun onFailure(error: IDJIError) {
                    Log.e(TAG, "navigateToPluginApp failed: $error")
                }
            },
        )
    }
}
