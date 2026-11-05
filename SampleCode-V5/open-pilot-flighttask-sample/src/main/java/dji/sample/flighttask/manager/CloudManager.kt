package dji.sample.flighttask.manager

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import dji.sample.flighttask.data.Asset
import dji.sample.flighttask.data.AssetTypeDescriptor
import dji.sample.flighttask.data.BackendConnectionState
import dji.sample.flighttask.data.ChunkResult
import dji.sample.flighttask.data.CloudConfig
import dji.sample.flighttask.data.CloudHttpException
import dji.sample.flighttask.data.CloudJobStatus
import dji.sample.flighttask.data.CompleteUploadRequest
import dji.sample.flighttask.data.Job
import dji.sample.flighttask.data.JobStatus
import dji.sample.flighttask.data.JobStatusRequest
import dji.sample.flighttask.data.JobStatusUpdate
import dji.sample.flighttask.data.RequestHandle
import dji.sample.flighttask.data.ResultCallback
import dji.sample.flighttask.data.SessionInfo
import dji.sample.flighttask.data.UploadSessionRequest
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/** 单次云端请求入口。业务重试、队列和状态均由上层 Manager 管理。 */
object CloudManager {

    private val gson = Gson()
    private val client = OkHttpClient.Builder().build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val binaryMediaType = "application/octet-stream".toMediaType()
    private val mutableBackendConnectionState = MutableLiveData(BackendConnectionState.CHECKING)
    val backendConnectionState: LiveData<BackendConnectionState> = mutableBackendConnectionState
    private val healthInFlight = AtomicBoolean(false)
    private var healthExecutor: ScheduledExecutorService? = null
    private var healthUrl: HttpUrl? = null

    @Volatile
    private var config: CloudConfig? = null

    @Volatile
    private var baseUrl: HttpUrl? = null

    fun init(config: CloudConfig) {
        this.config = config
        baseUrl = config.baseUrl.trim().toHttpUrlOrNull()
    }

    fun clear() {
        config = null
        baseUrl = null
    }

    @Synchronized
    fun startHealthChecks(serverUrl: String) {
        val root = serverUrl.trim().toHttpUrlOrNull()
        healthUrl = root?.newBuilder()?.addPathSegment("health")?.build()
        healthExecutor?.shutdownNow()
        mutableBackendConnectionState.postValue(BackendConnectionState.CHECKING)
        if (healthUrl == null) {
            mutableBackendConnectionState.postValue(BackendConnectionState.UNAVAILABLE)
            return
        }
        healthExecutor = Executors.newSingleThreadScheduledExecutor().also { executor ->
            executor.scheduleWithFixedDelay(::checkHealth, 0, HEALTH_INTERVAL_SECONDS, TimeUnit.SECONDS)
        }
    }

    private fun checkHealth() {
        val url = healthUrl ?: return
        if (!healthInFlight.compareAndSet(false, true)) return
        client.newCall(Request.Builder().url(url).get().build()).enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) {
                healthInFlight.set(false)
                mutableBackendConnectionState.postValue(BackendConnectionState.UNAVAILABLE)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    mutableBackendConnectionState.postValue(
                        if (it.isSuccessful) BackendConnectionState.CONNECTED else BackendConnectionState.UNAVAILABLE,
                    )
                }
                healthInFlight.set(false)
            }
        })
    }

    fun isConfigured(): Boolean {
        val current = config
        return baseUrl != null && current != null && current.rcSerialNumber.isNotBlank()
    }

    fun getJobs(since: Long?, callback: ResultCallback<List<Job>>): RequestHandle {
        val url = apiUrl("jobs", callback = callback) ?: return failedHandle()
        val requestUrl = url.newBuilder().apply {
            if (since != null) addQueryParameter("since", since.toString())
        }.build()
        return enqueue(apiRequest(requestUrl).get().build(), callback) { response ->
            val text = response.body?.string().orEmpty()
            val root = JsonParser.parseString(text)
            if (root.isJsonObject && root.asJsonObject.has("jobs")) {
                gson.fromJson(root.asJsonObject.get("jobs"), object : TypeToken<List<Job>>() {}.type)
            } else {
                gson.fromJson(text, object : TypeToken<List<Job>>() {}.type)
            }
        }
    }

    fun updateJobStatus(
        jobId: String,
        status: JobStatus,
        callback: ResultCallback<Job>,
    ): RequestHandle {
        return updateJobStatus(jobId, CloudJobStatus.valueOf(status.name), object : ResultCallback<JobStatusUpdate> {
            override fun onSuccess(data: JobStatusUpdate) {
                val job = data.job
                if (job != null) callback.onSuccess(job)
                else callback.onFailure(IOException("Job status response did not contain a job"))
            }

            override fun onFailure(error: Throwable) = callback.onFailure(error)
        })
    }

    fun updateJobStatus(
        jobId: String,
        status: CloudJobStatus,
        callback: ResultCallback<JobStatusUpdate>,
    ): RequestHandle {
        val url = apiUrl("jobs", jobId, "status", callback = callback) ?: return failedHandle()
        val body = gson.toJson(JobStatusRequest(status)).toRequestBody(jsonMediaType)
        return enqueue(apiRequest(url).put(body).build(), callback) { response ->
            val text = response.body?.string().orEmpty()
            if (text.isBlank()) return@enqueue JobStatusUpdate(null)

            val root = gson.fromJson(text, JsonObject::class.java)
            val job = when {
                root == null -> null
                root.has("job") && !root.get("job").isJsonNull -> gson.fromJson(root.get("job"), Job::class.java)
                root.has("jobId") && status != CloudJobStatus.CANCELLED -> gson.fromJson(root, Job::class.java)
                else -> null
            }
            JobStatusUpdate(job)
        }
    }

    fun downloadWayline(
        url: String,
        targetFile: File,
        onProgress: (Int) -> Unit,
        callback: ResultCallback<File>,
    ): RequestHandle {
        val resolvedUrl = resolveUrl(url, callback) ?: return failedHandle()
        val request = apiRequest(resolvedUrl, includeCloudHeaders = isSameServer(resolvedUrl)).get().build()
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.part")
        return enqueue(request, callback) { response ->
            val body = response.body ?: throw IOException("Wayline response body is empty")
            targetFile.parentFile?.mkdirs()
            tempFile.delete()
            val total = body.contentLength()
            var written = 0L
            onProgress(0)
            try {
                body.byteStream().use { input ->
                    tempFile.outputStream().buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            written += count
                            if (total > 0) onProgress(((written * 100 / total).coerceAtMost(100)).toInt())
                        }
                    }
                }
                if (targetFile.exists() && !targetFile.delete()) {
                    throw IOException("Cannot replace ${targetFile.absolutePath}")
                }
                if (!tempFile.renameTo(targetFile)) {
                    throw IOException("Cannot move wayline to ${targetFile.absolutePath}")
                }
                onProgress(100)
                targetFile
            } catch (error: Throwable) {
                tempFile.delete()
                throw error
            }
        }
    }

    fun getAssetTypes(callback: ResultCallback<List<AssetTypeDescriptor>>): RequestHandle {
        val url = apiUrl("asset-types", callback = callback) ?: return failedHandle()
        return enqueue(apiRequest(url).get().build(), callback) { response ->
            val text = response.body?.string().orEmpty()
            val root = JsonParser.parseString(text)
            if (root.isJsonObject && root.asJsonObject.has("assetTypes")) {
                gson.fromJson(root.asJsonObject.get("assetTypes"), object : TypeToken<List<AssetTypeDescriptor>>() {}.type)
            } else {
                gson.fromJson(text, object : TypeToken<List<AssetTypeDescriptor>>() {}.type)
            }
        }
    }

    fun createUploadSession(asset: Asset, callback: ResultCallback<SessionInfo>): RequestHandle {
        val url = apiUrl("upload-sessions", callback = callback) ?: return failedHandle()
        val body = gson.toJson(UploadSessionRequest(asset)).toRequestBody(jsonMediaType)
        return enqueue(apiRequest(url).post(body).build(), callback) { response ->
            gson.fromJson(response.body?.charStream(), SessionInfo::class.java)
        }
    }

    fun uploadPart(
        uploadUrl: String,
        data: ByteArray,
        callback: ResultCallback<ChunkResult>,
    ): RequestHandle {
        val url = resolveUrl(uploadUrl, callback) ?: return failedHandle()
        val body = data.toRequestBody(binaryMediaType)
        val request = apiRequest(url, includeCloudHeaders = isSameServer(url)).put(body).build()
        return enqueue(request, callback) { response ->
            val text = response.body?.string().orEmpty()
            if (text.isNotBlank()) {
                gson.fromJson(text, ChunkResult::class.java)
            } else {
                val index = url.queryParameter("partNumber")?.toIntOrNull()
                    ?: url.queryParameter("part")?.toIntOrNull()
                    ?: 0
                ChunkResult(index = index, success = true, message = response.header("ETag"))
            }
        }
    }

    fun completeUpload(
        sessionId: String,
        parts: List<ChunkResult>,
        callback: ResultCallback<Asset>,
    ): RequestHandle {
        val url = apiUrl("upload-sessions", sessionId, "complete", callback = callback) ?: return failedHandle()
        val body = gson.toJson(CompleteUploadRequest(parts)).toRequestBody(jsonMediaType)
        return enqueue(apiRequest(url).post(body).build(), callback) { response ->
            gson.fromJson(response.body?.charStream(), Asset::class.java)
        }
    }

    private fun apiRequest(url: HttpUrl, includeCloudHeaders: Boolean = true): Request.Builder {
        val builder = Request.Builder().url(url).header("Accept", "application/json")
        val current = config
        if (includeCloudHeaders && current != null) {
            builder.header("X-RC-SN", current.rcSerialNumber)
            if (current.accessToken.isNotBlank()) {
                builder.header("Authorization", "Bearer ${current.accessToken}")
            }
        }
        return builder
    }

    private fun <T> apiUrl(vararg segments: String, callback: ResultCallback<T>): HttpUrl? {
        val root = baseUrl
        if (root == null || !isConfigured()) {
            dispatchFailure(callback, IllegalStateException("CloudManager is not configured"))
            return null
        }
        return root.newBuilder().apply {
            addPathSegments("api/v1")
            segments.forEach(::addPathSegment)
        }.build()
    }

    private fun <T> resolveUrl(url: String, callback: ResultCallback<T>): HttpUrl? {
        val resolved = url.toHttpUrlOrNull() ?: baseUrl?.resolve(url)
        if (resolved == null) dispatchFailure(callback, IllegalArgumentException("Invalid URL: $url"))
        return resolved
    }

    private fun isSameServer(url: HttpUrl): Boolean {
        val root = baseUrl ?: return false
        return root.scheme == url.scheme && root.host == url.host && root.port == url.port
    }

    private fun <T> enqueue(
        request: Request,
        callback: ResultCallback<T>,
        parse: (Response) -> T,
    ): RequestHandle {
        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) = callback.onFailure(error)

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    try {
                        if (!response.isSuccessful) {
                            throw CloudHttpException(response.code, response.body?.string().orEmpty())
                        }
                        callback.onSuccess(parse(response))
                    } catch (error: Throwable) {
                        callback.onFailure(error)
                    }
                }
            }
        })
        return CallRequestHandle(call)
    }

    private fun <T> dispatchFailure(callback: ResultCallback<T>, error: Throwable) {
        client.dispatcher.executorService.execute { callback.onFailure(error) }
    }

    private fun failedHandle(): RequestHandle = EmptyRequestHandle()

    private const val HEALTH_INTERVAL_SECONDS = 10L

    private class CallRequestHandle(private val call: Call) : RequestHandle {
        override fun cancel() = call.cancel()
        override val isCanceled: Boolean get() = call.isCanceled()
    }

    private class EmptyRequestHandle : RequestHandle {
        private val canceled = AtomicBoolean(false)
        override fun cancel() { canceled.set(true) }
        override val isCanceled: Boolean get() = canceled.get()
    }
}
