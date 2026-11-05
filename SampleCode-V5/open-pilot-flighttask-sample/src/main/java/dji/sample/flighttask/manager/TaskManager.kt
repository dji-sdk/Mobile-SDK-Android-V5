package dji.sample.flighttask.manager

import android.content.Context
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import dji.sample.flighttask.data.CloudJobStatus
import dji.sample.flighttask.data.Job
import dji.sample.flighttask.data.JobEvent
import dji.sample.flighttask.data.JobStatus
import dji.sample.flighttask.data.JobStatusUpdate
import dji.sample.flighttask.data.JobsCache
import dji.sample.flighttask.data.ResultCallback
import dji.sample.flighttask.data.TaskSyncState
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/** 工单的内存数据源、云端同步入口和轻量本地缓存。 */
object TaskManager {

    private const val CACHE_FILE_PREFIX = "jobs-cache-"
    private const val POLL_INTERVAL_MS = 10_000L

    private val gson = Gson()
    private val lock = Any()

    private val mutableJobs = MutableLiveData<List<Job>>(emptyList())
    val jobs: LiveData<List<Job>> = mutableJobs

    private val mutableActiveJob = MutableLiveData<Job?>(null)
    val activeJob: LiveData<Job?> = mutableActiveJob

    private val mutableSyncState = MutableLiveData(TaskSyncState.IDLE)
    val syncState: LiveData<TaskSyncState> = mutableSyncState

    private val mutableError = MutableLiveData<String?>(null)
    val error: LiveData<String?> = mutableError

    private var appContext: Context? = null
    private var currentJobs: List<Job> = emptyList()
    private var currentActiveJob: Job? = null
    private var lastSyncTime: Long? = null
    private var mutationGeneration = 0L
    private var identityGeneration = 0L
    private var identityCacheKey: String? = null
    private var refreshSession: Long? = null
    private var scheduler: ScheduledExecutorService? = null
    private var pollingTask: ScheduledFuture<*>? = null

    fun init(context: Context, rcSerialNumber: String) {
        stopPolling()
        appContext = context.applicationContext
        val cacheKey = MessageDigest.getInstance("SHA-256")
            .digest(rcSerialNumber.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        identityCacheKey = cacheKey
        val cached = readCache()
        synchronized(lock) {
            identityGeneration++
            refreshSession = null
            mutationGeneration = 0L
            currentJobs = cached?.jobs ?: emptyList()
            lastSyncTime = cached?.lastSyncTime
            currentActiveJob = currentJobs.firstOrNull(::isActive)
            publishJobsLocked()
        }

        if (CloudManager.isConfigured()) {
            publish(mutableSyncState, TaskSyncState.IDLE)
        } else {
            publish(mutableError, "CloudManager is not configured")
            publish(mutableSyncState, TaskSyncState.ERROR)
        }
    }

    /** 保留现有 VM 的入口。 */
    fun start(context: Context, rcSerialNumber: String) = init(context, rcSerialNumber)

    fun deactivateIdentity() {
        stopPolling()
        synchronized(lock) {
            identityGeneration++
            identityCacheKey = null
            refreshSession = null
            currentJobs = emptyList()
            currentActiveJob = null
            lastSyncTime = null
            publishJobsLocked()
        }
        publish(mutableSyncState, TaskSyncState.IDLE)
        publish(mutableError, null)
    }

    @Synchronized
    fun startPolling() {
        if (pollingTask?.isCancelled == false && pollingTask?.isDone == false) return
        if (!CloudManager.isConfigured()) {
            publish(mutableError, "CloudManager is not configured")
            publish(mutableSyncState, TaskSyncState.ERROR)
            return
        }
        val executor = Executors.newSingleThreadScheduledExecutor()
        scheduler = executor
        pollingTask = executor.scheduleWithFixedDelay(
            {
                runCatching { refresh() }.onFailure { error ->
                    publish(mutableError, error.message ?: error.javaClass.simpleName)
                    publish(mutableSyncState, TaskSyncState.ERROR)
                }
            },
            0,
            POLL_INTERVAL_MS,
            TimeUnit.MILLISECONDS,
        )
    }

    fun ensurePolling() {
        if (CloudManager.isConfigured()) startPolling()
    }

    @Synchronized
    fun stopPolling() {
        pollingTask?.cancel(true)
        pollingTask = null
        scheduler?.shutdownNow()
        scheduler = null
    }

    fun refresh() {
        if (identityCacheKey == null) return
        if (!CloudManager.isConfigured()) {
            publish(mutableError, "CloudManager is not configured")
            publish(mutableSyncState, TaskSyncState.ERROR)
            return
        }
        val requestTime = System.currentTimeMillis()
        var requestGeneration = 0L
        val requestSession = synchronized(lock) {
            if (refreshSession != null || identityCacheKey == null) return
            identityGeneration.also {
                refreshSession = it
                requestGeneration = mutationGeneration
            }
        }
        publish(mutableSyncState, TaskSyncState.SYNCING)
        publish(mutableError, null)
        CloudManager.getJobs(lastSyncTime, object : ResultCallback<List<Job>> {
            override fun onSuccess(data: List<Job>) {
                var stale = false
                synchronized(lock) {
                    if (refreshSession == requestSession) refreshSession = null
                    stale = requestSession != identityGeneration || requestGeneration != mutationGeneration
                    if (!stale) {
                        // Demo Server 返回完整快照；稳定 jobId 保证重复轮询不会产生重复项。
                        currentJobs = data.distinctBy { it.jobId }
                        lastSyncTime = requestTime
                        val activeId = currentActiveJob?.jobId
                        currentActiveJob = currentJobs.firstOrNull { it.jobId == activeId && isActive(it) }
                            ?: currentJobs.firstOrNull(::isActive)
                        publishJobsLocked()
                        persistCacheLocked()
                    }
                }
                if (stale) {
                    if (requestSession == synchronized(lock) { identityGeneration }) refresh()
                } else {
                    publish(mutableSyncState, TaskSyncState.SYNCED)
                }
            }

            override fun onFailure(error: Throwable) {
                val current = synchronized(lock) {
                    if (refreshSession == requestSession) refreshSession = null
                    requestSession == identityGeneration
                }
                if (!current) return
                publish(mutableError, error.message ?: error.javaClass.simpleName)
                publish(mutableSyncState, TaskSyncState.ERROR)
            }
        })
    }

    fun accept(jobId: String, callback: ResultCallback<Job>? = null) =
        updateStatus(jobId, CloudJobStatus.ACCEPTED, "Task accepted", callback)

    fun start(jobId: String, callback: ResultCallback<Job>? = null) =
        updateStatus(jobId, CloudJobStatus.IN_PROGRESS, "Task started", callback)

    fun complete(jobId: String, callback: ResultCallback<Job>? = null) =
        updateStatus(jobId, CloudJobStatus.COMPLETED, "Task completed", callback)

    fun finalize(jobId: String, callback: ResultCallback<Job>? = null) =
        updateStatus(jobId, CloudJobStatus.FINALIZING, "Task finalizing", callback)

    fun cancel(jobId: String, callback: ResultCallback<Job>? = null) =
        updateStatus(jobId, CloudJobStatus.CANCELLED, "Task cancelled", callback)

    fun clearActiveJob() {
        synchronized(lock) {
            currentActiveJob = null
            publish(mutableActiveJob, null)
        }
    }

    private fun updateStatus(
        jobId: String,
        status: CloudJobStatus,
        event: String,
        callback: ResultCallback<Job>?,
    ) {
        if (!CloudManager.isConfigured() || identityCacheKey == null) {
            val error = IllegalStateException("CloudManager is not configured")
            publish(mutableError, error.message)
            publish(mutableSyncState, TaskSyncState.ERROR)
            callback?.onFailure(error)
            return
        }
        val requestSession = synchronized(lock) {
            mutationGeneration++
            identityGeneration
        }

        publish(mutableSyncState, TaskSyncState.SYNCING)
        publish(mutableError, null)
        CloudManager.updateJobStatus(jobId, status, object : ResultCallback<JobStatusUpdate> {
            override fun onSuccess(data: JobStatusUpdate) {
                if (requestSession != synchronized(lock) { identityGeneration }) return
                val updated = applyStatus(jobId, status, event, data.job)
                publish(mutableSyncState, TaskSyncState.SYNCED)
                if (updated != null) callback?.onSuccess(updated)
                else callback?.onFailure(IllegalArgumentException("Unknown job: $jobId"))
                refresh()
            }

            override fun onFailure(error: Throwable) {
                if (requestSession != synchronized(lock) { identityGeneration }) return
                publish(mutableError, error.message ?: error.javaClass.simpleName)
                publish(mutableSyncState, TaskSyncState.ERROR)
                callback?.onFailure(error)
            }
        })
    }

    private fun applyStatus(jobId: String, status: CloudJobStatus, event: String, serverJob: Job?): Job? {
        return synchronized(lock) {
            val originalJob = currentJobs.firstOrNull { it.jobId == jobId }
            val localStatus = JobStatus.valueOf(status.name)
            currentJobs = currentJobs.map { job ->
                if (job.jobId != jobId) job
                else (serverJob ?: job.copy(status = localStatus)).copy(
                    timeline = (serverJob?.timeline ?: job.timeline) + JobEvent(System.currentTimeMillis(), event),
                )
            }
            currentActiveJob = when (status) {
                CloudJobStatus.ACCEPTED, CloudJobStatus.IN_PROGRESS, CloudJobStatus.FINALIZING ->
                    currentJobs.firstOrNull { it.jobId == jobId }
                CloudJobStatus.COMPLETED, CloudJobStatus.CANCELLED ->
                    if (currentActiveJob?.jobId == jobId) null else currentActiveJob
                else -> currentActiveJob
            }
            publishJobsLocked()
            persistCacheLocked()
            serverJob ?: currentJobs.firstOrNull { it.jobId == jobId } ?: originalJob
        }
    }

    private fun publishJobsLocked() {
        publish(mutableJobs, currentJobs)
        publish(mutableActiveJob, currentActiveJob)
    }

    private fun readCache(): JobsCache? {
        val file = cacheFile() ?: return null
        if (!file.isFile) return null
        return try {
            gson.fromJson(file.readText(), JobsCache::class.java)
        } catch (error: Exception) {
            publish(mutableError, "Cannot read jobs cache: ${error.message}")
            null
        }
    }

    private fun persistCache() = synchronized(lock) { persistCacheLocked() }

    private fun persistCacheLocked() {
        val file = cacheFile() ?: return
        try {
            file.writeText(gson.toJson(JobsCache(currentJobs, lastSyncTime)))
        } catch (error: Exception) {
            publish(mutableError, "Cannot write jobs cache: ${error.message}")
        }
    }

    private fun cacheFile(): File? {
        val cacheKey = identityCacheKey ?: return null
        return appContext?.filesDir?.let { File(it, "$CACHE_FILE_PREFIX$cacheKey.json") }
    }

    private fun isActive(job: Job): Boolean =
        job.status == JobStatus.ACCEPTED ||
            job.status == JobStatus.IN_PROGRESS ||
            job.status == JobStatus.FINALIZING

    private fun <T> publish(liveData: MutableLiveData<T>, value: T) {
        if (Looper.myLooper() == Looper.getMainLooper()) liveData.value = value else liveData.postValue(value)
    }
}
