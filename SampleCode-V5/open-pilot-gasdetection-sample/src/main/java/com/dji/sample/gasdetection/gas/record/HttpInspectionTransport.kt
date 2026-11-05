package com.dji.sample.gasdetection.gas.record

import android.util.Log
import com.dji.sample.gasdetection.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 检测记录的 HTTP 传输实现。
 *
 * 设计意图：沿用工程既有的 `HttpURLConnection` 风格（不引入 OkHttp），把状态码与异常统一收敛为
 * [InspectionUploadResult]；上传使用 `POST /api/v1/inspections`，完成回写按
 * `IN_PROGRESS → FINALIZING → COMPLETED` 迁移，兼容演示后端的迁移限制。
 */
class HttpInspectionTransport(
    private val baseUrl: String = BuildConfig.GASDETECTION_SERVER_URL,
    private val rcSnProvider: () -> String?,
) : InspectionTransport {

    override fun upload(record: InspectionRecord, callback: (InspectionUploadResult) -> Unit) {
        callback(post("/api/v1/inspections", InspectionJson.payload(record)))
    }

    override fun completeJob(jobId: String, callback: (InspectionUploadResult) -> Unit) {
        // 后端只允许 ACCEPTED/IN_PROGRESS → IN_PROGRESS → FINALIZING → COMPLETED。
        // 前两步失败（例如已处于目标状态）忽略，最终以 COMPLETED 的结果为准。
        put("/api/v1/jobs/$jobId/status", """{"status":"IN_PROGRESS"}""")
        put("/api/v1/jobs/$jobId/status", """{"status":"FINALIZING"}""")
        callback(put("/api/v1/jobs/$jobId/status", """{"status":"COMPLETED"}"""))
    }

    private fun post(path: String, body: String): InspectionUploadResult = execute("POST", path, body)

    private fun put(path: String, body: String): InspectionUploadResult = execute("PUT", path, body)

    private fun execute(method: String, path: String, body: String): InspectionUploadResult {
        val sn = rcSnProvider()
            ?: return InspectionUploadResult.Failure("遥控器 SN 未就绪")
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("X-RC-SN", sn)
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                doOutput = true
                outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            if (code in 200..299) {
                connection.inputStream?.close()
                InspectionUploadResult.Success
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                InspectionUploadResult.Failure("HTTP $code: ${error.take(200)}")
            }
        } catch (error: Exception) {
            Log.e(TAG, "request failed $method $path: ${error.message}")
            InspectionUploadResult.Failure(error.message ?: error.javaClass.simpleName)
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 3_000
        private const val READ_TIMEOUT_MS = 5_000
        private const val TAG = "GasInspectionRecord"
    }
}
