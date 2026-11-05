package com.dji.sample.gasdetection.gas.task

import android.util.Log
import com.dji.sample.gasdetection.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** 网络层错误分类；调用方按类型给出可读文案，不清空已有任务。 */
sealed class ApiError {
    /** 连接失败、超时等传输层问题。 */
    data class Network(val message: String) : ApiError()

    /** 服务端返回非 2xx。 */
    data class Http(val code: Int, val body: String?) : ApiError()

    /** 响应体不是预期结构或字段非法。 */
    data class Parse(val message: String) : ApiError()
}

/** 请求结果。 */
sealed class ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>()
    data class Err(val error: ApiError) : ApiResult<Nothing>()
}

/**
 * 演示后端的只读任务接口与状态回写。
 *
 * 设计意图：用一个极小的 [`HttpURLConnection`] 客户端覆盖 FR-3 的三个接口，不引入 OkHttp；
 * 超时与错误分类明确，失败由调用方决定是否重试，网络层自身不做重试风暴。
 * 遥控器 SN 通过 [rcSnProvider] 延迟读取，SDK 未就绪时返回 `null`，调用方按「未就绪」处理。
 */
class JobApiClient(
    private val baseUrl: String = BuildConfig.GASDETECTION_SERVER_URL,
    private val rcSnProvider: () -> String?,
) {

    suspend fun listJobs(): ApiResult<List<GasJob>> = withContext(Dispatchers.IO) {
        when (val response = execute("GET", "/api/v1/jobs")) {
            is ApiResult.Err -> response
            is ApiResult.Ok -> parseJobs(response.value)
        }
    }

    suspend fun getJob(jobId: String): ApiResult<GasJob> = withContext(Dispatchers.IO) {
        when (val response = execute("GET", "/api/v1/jobs/$jobId")) {
            is ApiResult.Err -> response
            is ApiResult.Ok -> parseJob(response.value)
        }
    }

    suspend fun updateStatus(jobId: String, status: JobStatus): ApiResult<GasJob> =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("status", status.name).toString()
            when (val response = execute("PUT", "/api/v1/jobs/$jobId/status", body)) {
                is ApiResult.Err -> response
                is ApiResult.Ok -> parseJob(response.value)
            }
        }

    private fun execute(method: String, path: String, body: String? = null): ApiResult<String> {
        val sn = rcSnProvider()
            ?: return ApiResult.Err(ApiError.Network("遥控器 SN 未就绪"))
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("X-RC-SN", sn)
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
            }
            val code = connection.responseCode
            Log.d("GasTaskApi", "${method} ${baseUrl.trimEnd('/') + path} sn=$sn -> $code")
            if (code !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                ApiResult.Err(ApiError.Http(code, errorBody))
            } else {
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                ApiResult.Ok(text)
            }
        } catch (error: IOException) {
            ApiResult.Err(ApiError.Network(error.message ?: "网络不可达"))
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseJobs(raw: String): ApiResult<List<GasJob>> = try {
        val array = JSONObject(raw).optJSONArray("jobs") ?: JSONArray()
        val jobs = buildList {
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.let { add(parseJobObject(it)) }
            }
        }
        ApiResult.Ok(jobs)
    } catch (error: Exception) {
        ApiResult.Err(ApiError.Parse(error.message ?: "任务列表解析失败"))
    }

    private fun parseJob(raw: String): ApiResult<GasJob> = try {
        val objectValue = JSONObject(raw).optJSONObject("job")
            ?: return ApiResult.Err(ApiError.Parse("响应缺少 job 字段"))
        ApiResult.Ok(parseJobObject(objectValue))
    } catch (error: Exception) {
        ApiResult.Err(ApiError.Parse(error.message ?: "任务解析失败"))
    }

    private fun parseJobObject(value: JSONObject): GasJob {
        val status = JobStatus.parse(value.optString("status"))
            ?: throw IllegalArgumentException("未知任务状态: ${value.optString("status")}")
        val gas = value.optJSONObject("gas")
        return GasJob(
            jobId = value.getString("jobId"),
            title = value.optString("title"),
            status = status,
            target = gas?.optJSONObject("target")?.let {
                GasTarget(
                    latitude = it.getDouble("latitude"),
                    longitude = it.getDouble("longitude"),
                    altitudeM = if (it.has("altitudeM")) it.getDouble("altitudeM") else null,
                )
            },
            channels = parseChannels(gas?.optJSONArray("channels")),
            safetyBoundaryM = if (gas?.has("safetyBoundaryM") == true) {
                gas.getDouble("safetyBoundaryM")
            } else {
                null
            },
            requirements = gas?.optString("requirements")?.takeIf { it.isNotBlank() },
            deadline = gas?.optString("deadline")?.takeIf { it.isNotBlank() },
        )
    }

    private fun parseChannels(array: JSONArray?): List<JobChannel> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    JobChannel(
                        channelId = item.getString("channelId"),
                        displayName = item.optString("displayName"),
                        unit = item.optString("unit"),
                        threshold = if (item.has("threshold")) item.getDouble("threshold") else null,
                    )
                )
            }
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 3_000
        const val READ_TIMEOUT_MS = 5_000
    }
}
