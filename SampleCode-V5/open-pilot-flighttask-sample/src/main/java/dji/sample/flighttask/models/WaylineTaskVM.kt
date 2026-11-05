package dji.sample.flighttask.models

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import dji.sample.flighttask.data.Asset
import dji.sample.flighttask.data.Job
import dji.sample.flighttask.data.JobStatus
import dji.sample.flighttask.data.ResultCallback
import dji.sample.flighttask.data.WaylineState
import dji.sample.flighttask.manager.MediaManager
import dji.sample.flighttask.manager.AssetUploadManager
import dji.sample.flighttask.manager.TaskManager
import dji.sample.flighttask.manager.WaylineManager

/**
 * 航线任务 VM（全局级，挂 globalViewModelStore）。
 *
 * 管理航线、资源和传输相关 UI，并把页面操作转交给对应 Manager；
 * 不包含 MSDK、HTTP 或文件上传实现。
 */
class WaylineTaskVM : ViewModel() {

    val waylineState = WaylineManager.state
    val waylineProgress = WaylineManager.progress
    val waylineError = WaylineManager.error
    val routeAsset = WaylineManager.routeAsset
    val preparedJobId: String? get() = WaylineManager.preparedJobId
    val localAssets = MediaManager.localAssets
    val manifestAssets = MediaManager.manifestAssets
    val thumbnailPaths = MediaManager.thumbnailPaths
    val downloadStates = MediaManager.downloadStates
    val uploadQueue = AssetUploadManager.queue
    val uploadProgress = AssetUploadManager.progress
    val uploadDetails = AssetUploadManager.details
    val mediaError = MutableLiveData<String?>(null)

    private val mainHandler = Handler(Looper.getMainLooper())
    private var initialized = false
    private var currentJob: Job? = null
    private var mediaSyncRunning = false
    private var mediaSyncAgain = false
    private var finalSyncRequested = false
    private var finalSyncFinished = false
    private var finalSyncScheduled = false
    private var finalizingFinished = false
    private var completionInFlight = false
    private var completionFinished = false
    private var mediaRetryCount = 0
    private var manualRetryRequired = false
    private var lastWaylineState = WaylineState.IDLE

    private val waylineObserver = Observer<WaylineState> { state ->
        if (state == WaylineState.FINISHED && lastWaylineState != WaylineState.FINISHED && !finalSyncRequested) {
            currentJob?.let(::beginFinalization)
        }
        lastWaylineState = state
    }

    private val jobsObserver = Observer<List<Job>> { jobs ->
        MediaManager.updateJobStatuses(jobs.associate { it.jobId to it.status })
        val job = jobs.firstOrNull { it.status == JobStatus.FINALIZING }
        if (job != null && currentJob == null) {
            // APP 冷启动恢复时等待用户从浮窗或任务分组显式继续。
            currentJob = job
            manualRetryRequired = true
        } else if (job != null && !manualRetryRequired && (!finalSyncRequested || (!finalSyncFinished && !mediaSyncRunning && !finalSyncScheduled))) {
            resumeFinalization(job)
        } else {
            jobs.firstOrNull { it.status == JobStatus.IN_PROGRESS }?.let { running ->
                if (currentJob?.jobId != running.jobId) {
                    currentJob = running
                    // 航线未结束的恢复不拉媒体，由用户重新执行航线。
                    manualRetryRequired = true
                }
            }
        }
        val current = currentJob ?: return@Observer
        if (jobs.firstOrNull { it.jobId == current.jobId }?.status in setOf(JobStatus.COMPLETED, JobStatus.CANCELLED)) {
            completionFinished = true
            MediaManager.endJob()
        }
    }

    private val uploadObserver = Observer<List<Asset>> {
        finishTaskIfReady()
    }

    private val localAssetObserver = Observer<List<Asset>> { assets ->
        val job = currentJob ?: return@Observer
        val jobAssets = assets.filter { it.jobId == job.jobId }
        if (jobAssets.isNotEmpty()) {
            AssetUploadManager.enqueue(job.jobId, jobAssets)
        }
    }

    fun onPilotConnected(context: Context) {
        if (!initialized) {
            initialized = true
            MediaManager.init(context)
            AssetUploadManager.init(context)
            WaylineManager.state.observeForever(waylineObserver)
            TaskManager.jobs.observeForever(jobsObserver)
            MediaManager.localAssets.observeForever(localAssetObserver)
            AssetUploadManager.queue.observeForever(uploadObserver)
        }
        WaylineManager.init()
        TaskManager.jobs.value?.let { jobsObserver.onChanged(it) }
        currentJob?.let { job ->
            if (job.status != JobStatus.FINALIZING) {
                MediaManager.beginJob(job.jobId) {
                    requestMediaSync(job)
                }
            }
        }
    }

    fun loadWayline(job: Job, callback: ResultCallback<Unit>? = null) {
        if (!initialized) {
            callback?.onFailure(IllegalStateException("Pilot is not connected"))
            return
        }
        currentJob = job
        finalSyncRequested = false
        finalSyncFinished = false
        finalSyncScheduled = false
        finalizingFinished = false
        completionInFlight = false
        completionFinished = false
        mediaRetryCount = 0
        manualRetryRequired = false
        lastWaylineState = WaylineState.IDLE
        mediaError.value = null
        if (job.status == JobStatus.IN_PROGRESS) {
            MediaManager.restartJob(job.jobId) { prepareWaylineJob(job, callback) }
        } else {
            prepareWaylineJob(job, callback)
        }
    }

    private fun prepareWaylineJob(job: Job, callback: ResultCallback<Unit>?) {
        MediaManager.beginJob(job.jobId) {
            requestMediaSync(job)
        }
        MediaManager.prepareJobFolder(job.jobId, object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) {
                WaylineManager.prepare(job, object : ResultCallback<Unit> {
                    override fun onSuccess(data: Unit) {
                        callback?.onSuccess(Unit)
                    }

                    override fun onFailure(error: Throwable) {
                        callback?.onFailure(error)
                    }
                })
            }

            override fun onFailure(error: Throwable) {
                callback?.onFailure(error)
            }
        })
    }

    fun startMission(callback: ResultCallback<Unit>? = null) {
        val job = currentJob
        if (job == null) {
            callback?.onFailure(IllegalStateException("Load a task before starting the mission"))
            return
        }
        TaskManager.start(job.jobId, object : ResultCallback<Job> {
            override fun onSuccess(data: Job) {
                WaylineManager.start(callback = callback)
            }
            override fun onFailure(error: Throwable) {
                callback?.onFailure(error)
            }
        })
    }

    fun pauseMission() {
        WaylineManager.pause()
    }

    fun resumeMission() {
        WaylineManager.resume()
    }

    fun stopMission() {
        WaylineManager.stop()
    }

    fun downloadPhotos(allowPlayback: Boolean = false) {
        currentJob?.let { requestMediaSync(it, finalAttempt = allowPlayback) }
    }

    fun uploadAssets(jobId: String, assets: List<Asset>) {
        AssetUploadManager.enqueue(jobId, assets)
    }

    fun pauseUpload(jobId: String) {
        AssetUploadManager.pause(jobId)
    }

    fun resumeUpload(jobId: String) {
        AssetUploadManager.resume(jobId)
    }

    fun retryUpload(jobId: String, assetId: String) {
        AssetUploadManager.retry(jobId, assetId)
    }

    fun retryFinalization(job: Job, callback: ResultCallback<Unit>) {
        if (mediaSyncRunning || finalSyncScheduled) {
            callback.onFailure(IllegalStateException("Photo sync is already running"))
            return
        }
        manualRetryRequired = false
        mediaSyncRunning = false
        mediaSyncAgain = false
        finalSyncScheduled = false
        MediaManager.cancelDownload()
        AssetUploadManager.resume(job.jobId)
        resumeFinalization(job, callback)
    }

    fun pauseFinalization(job: Job) {
        manualRetryRequired = true
        MediaManager.cancelDownload()
        mediaSyncRunning = false
        mediaSyncAgain = false
        finalSyncScheduled = false
        AssetUploadManager.pause(job.jobId)
        mediaError.postValue("Sync paused")
    }

    fun onPilotDisconnected() {
        MediaManager.cancelDownload()
        MediaManager.endJob()
        WaylineManager.release()
        mediaSyncRunning = false
        mediaSyncAgain = false
        finalSyncScheduled = false
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun requestMediaSync(
        job: Job,
        finalAttempt: Boolean = false,
        attemptCallback: ResultCallback<Unit>? = null,
    ) {
        if (mediaSyncRunning) {
            mediaSyncAgain = true
            if (finalAttempt) finalSyncRequested = true
            return
        }
        mediaSyncRunning = true
        // 文件列表和原图拉取均不进入回放，保持边飞边拍边拉能力。
        MediaManager.downloadNewPhotos(job.jobId, allowPlayback = false, object : ResultCallback<List<Asset>> {
            override fun onSuccess(data: List<Asset>) {
                mediaRetryCount = 0
                if (data.isEmpty()) {
                    onMediaSyncFinished(job, finalAttempt)
                    attemptCallback?.onSuccess(Unit)
                } else {
                    AssetUploadManager.enqueue(job.jobId, data) {
                        mainHandler.post {
                            onMediaSyncFinished(job, finalAttempt)
                            attemptCallback?.onSuccess(Unit)
                        }
                    }
                }
            }

            override fun onFailure(error: Throwable) {
                // 飞行中的直拉失败保留 pending，航线结束后按冻结清单补拉。
                mediaSyncRunning = false
                if (finalAttempt) {
                    mediaError.postValue(error.message ?: "Photo download failed")
                    finalSyncFinished = false
                    manualRetryRequired = true
                    attemptCallback?.onFailure(error)
                } else if (mediaSyncAgain) {
                    mediaSyncAgain = false
                    requestMediaSync(job, finalAttempt = finalSyncRequested)
                } else if (mediaRetryCount < MAX_IN_FLIGHT_MEDIA_RETRIES) {
                    mediaRetryCount++
                    mainHandler.postDelayed({
                        if (currentJob?.jobId == job.jobId && !finalSyncRequested) {
                            requestMediaSync(job)
                        }
                    }, IN_FLIGHT_MEDIA_RETRY_DELAY_MS)
                }
            }
        })
    }

    private fun onMediaSyncFinished(job: Job, finalAttempt: Boolean) {
        mediaSyncRunning = false
        if (finalAttempt) finalSyncFinished = true
        if (mediaSyncAgain) {
            mediaSyncAgain = false
            requestMediaSync(job, finalAttempt = finalSyncRequested)
        } else {
            finishTaskIfReady()
        }
    }

    private fun finishTaskIfReady() {
        val job = currentJob ?: return
        if (!finalSyncFinished || !finalizingFinished || mediaSyncRunning || completionInFlight || completionFinished) return
        val jobAssets = AssetUploadManager.queue.value.orEmpty().filter { it.jobId == job.jobId }
        if (jobAssets.any { it.state != dji.sample.flighttask.data.AssetState.UPLOADED }) return
        completionInFlight = true
        TaskManager.complete(job.jobId, object : ResultCallback<Job> {
            override fun onSuccess(data: Job) {
                completionInFlight = false
                completionFinished = true
            }

            override fun onFailure(error: Throwable) {
                completionInFlight = false
                mediaError.postValue(error.message ?: "Task completion failed")
                mainHandler.postDelayed(::finishTaskIfReady, PIPELINE_RETRY_DELAY_MS)
            }
        })
    }

    private fun beginFinalization(job: Job) {
        MediaManager.markJobFinished(job.jobId)
        finalSyncRequested = true
        TaskManager.finalize(job.jobId, object : ResultCallback<Job> {
            override fun onSuccess(data: Job) {
                currentJob = data
                finalizingFinished = true
                scheduleFinalSync(data, FINAL_MEDIA_SETTLE_MS)
            }

            override fun onFailure(error: Throwable) {
                finalSyncRequested = false
                mediaError.postValue(error.message ?: "Cannot enter finalizing state")
                mainHandler.postDelayed({
                    if (currentJob?.jobId == job.jobId && !finalSyncRequested) beginFinalization(job)
                }, PIPELINE_RETRY_DELAY_MS)
            }
        })
    }

    private fun resumeFinalization(job: Job, callback: ResultCallback<Unit>? = null) {
        currentJob = job
        finalSyncRequested = true
        finalizingFinished = true
        completionFinished = false
        manualRetryRequired = false
        val local = MediaManager.localAssets.value.orEmpty().filter { it.jobId == job.jobId }
        if (local.isNotEmpty()) AssetUploadManager.enqueue(job.jobId, local)
        AssetUploadManager.retry(job.jobId)
        scheduleFinalSync(job, 0, callback)
    }

    private fun scheduleFinalSync(job: Job, delayMillis: Long, callback: ResultCallback<Unit>? = null) {
        if (mediaSyncRunning || finalSyncScheduled) return
        finalSyncScheduled = true
        mainHandler.postDelayed({
            finalSyncScheduled = false
            if (currentJob?.jobId != job.jobId) {
                return@postDelayed
            }
            finalSyncFinished = false
            MediaManager.endJob()
            MediaManager.freezeJobManifest(job.jobId, object : ResultCallback<Unit> {
                override fun onSuccess(data: Unit) {
                    requestMediaSync(job, finalAttempt = true, attemptCallback = callback)
                }

                override fun onFailure(error: Throwable) {
                    mediaError.postValue(error.message ?: "Cannot freeze task media list")
                    manualRetryRequired = true
                    callback?.onFailure(error)
                }
            })
        }, delayMillis)
    }

    override fun onCleared() {
        super.onCleared()
        WaylineManager.state.removeObserver(waylineObserver)
        TaskManager.jobs.removeObserver(jobsObserver)
        MediaManager.localAssets.removeObserver(localAssetObserver)
        AssetUploadManager.queue.removeObserver(uploadObserver)
        MediaManager.release()
        AssetUploadManager.release()
        WaylineManager.release()
    }

    companion object {
        private const val FINAL_MEDIA_SETTLE_MS = 1_200L
        private const val IN_FLIGHT_MEDIA_RETRY_DELAY_MS = 1_500L
        private const val MAX_IN_FLIGHT_MEDIA_RETRIES = 3
        private const val PIPELINE_RETRY_DELAY_MS = 3_000L
    }
}
