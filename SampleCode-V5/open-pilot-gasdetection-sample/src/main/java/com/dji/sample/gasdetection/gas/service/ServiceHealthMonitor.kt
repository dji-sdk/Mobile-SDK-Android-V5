package com.dji.sample.gasdetection.gas.service

import android.util.Log
import com.dji.sample.gasdetection.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 演示后端服务连接状态。
 *
 * 设计意图：入口页标题栏需要区分「服务可达」与「服务不可用」，但业务接口自身的失败没有稳定的
 * 可用性信号，因此用独立的健康探测表达服务进程是否可达；状态语义与业务错误分离，不互相覆盖。
 */
enum class ServiceConnectionState {
    /** 首次探测结果尚未返回。 */
    CHECKING,

    /** 最近一次探测返回 2xx，服务可达。 */
    CONNECTED,

    /** 最近一次探测失败、超时、返回非 2xx，或服务地址非法。 */
    UNAVAILABLE,
}

/**
 * 演示后端健康探测器（进程级单例状态源）。
 *
 * 设计意图：把「周期性 GET /health」与「状态订阅」和 UI 解耦——UI 只订阅 [state]，不关心 URL、
 * 超时与调度。探测使用工程既有的 `HttpURLConnection` 风格，不引入第二个 HTTP 客户端。
 *
 * 线程契约：[start] 必须在主线程调用；探测在 IO 线程执行，[state] 可在任意线程读取。
 * 并发约束：同一时刻至多一个探测在途，慢响应不会堆积请求。
 *
 * @param baseUrl 演示后端地址，默认取构建注入的 `GASDETECTION_SERVER_URL`。
 * @param intervalMs 两次探测的间隔，默认 10 秒，与巡逻 Demo 主页保持一致。
 * @param probe 探测实现，便于用假实现驱动状态机而不依赖真实网络。
 */
class ServiceHealthMonitor(
    private val baseUrl: String = BuildConfig.GASDETECTION_SERVER_URL,
    private val intervalMs: Long = DEFAULT_INTERVAL_MS,
    private val probe: suspend (String) -> Boolean = ::probeHealth,
) {
    private val mutableState = MutableStateFlow(ServiceConnectionState.CHECKING)

    /** 当前服务连接状态，供入口页标题栏订阅。 */
    val state: StateFlow<ServiceConnectionState> = mutableState.asStateFlow()

    /** 防并发：单次探测未结束时跳过本轮，不排队。 */
    private val inFlight = AtomicBoolean(false)

    private var job: Job? = null

    /** 启动周期探测；重复调用不会产生第二个探测循环。 */
    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                checkOnce()
                delay(intervalMs)
            }
        }
    }

    /** 停止周期探测，并释放调度；进程退出前一般不调用。 */
    fun stop() {
        job?.cancel()
        job = null
    }

    /**
     * 执行一轮探测并更新状态。
     *
     * 地址非法时直接判定不可用且不发起请求；探测异常一律按不可用处理，不向上抛出，
     * 避免单次网络异常打断周期循环。
     */
    private suspend fun checkOnce() {
        if (!inFlight.compareAndSet(false, true)) {
            Log.d(TAG, "health probe skipped: previous probe still in flight")
            return
        }
        val healthUrl = resolveHealthUrl()
        if (healthUrl == null) {
            inFlight.set(false)
            mutableState.value = ServiceConnectionState.UNAVAILABLE
            Log.w(TAG, "health probe skipped: invalid service url \"$baseUrl\"")
            return
        }
        val startedAt = System.currentTimeMillis()
        val reachable = try {
            probe(healthUrl)
        } catch (error: Exception) {
            Log.w(TAG, "health probe failed: url=$healthUrl reason=${error.javaClass.simpleName}: ${error.message}")
            false
        } finally {
            inFlight.set(false)
        }
        val next = if (reachable) ServiceConnectionState.CONNECTED else ServiceConnectionState.UNAVAILABLE
        mutableState.value = next
        // 成功分支同样留痕：真机排查需要区分「探测未触发」与「探测成功但状态没变」。
        Log.d(TAG, "health probe: url=$healthUrl result=$next costMs=${System.currentTimeMillis() - startedAt}")
    }

    /** 由配置的服务地址派生健康检查地址；地址为空或不是合法 URL 时返回 `null`。 */
    private fun resolveHealthUrl(): String? {
        val base = baseUrl.trim()
        if (base.isEmpty()) return null
        val health = base.trimEnd('/') + HEALTH_PATH
        return runCatching { URL(health).toString() }.getOrNull()
    }

    private companion object {
        private const val TAG = "GasServiceHealth"
        private const val HEALTH_PATH = "/health"
        private const val DEFAULT_INTERVAL_MS = 10_000L
        private const val CONNECT_TIMEOUT_MS = 3_000
        private const val READ_TIMEOUT_MS = 5_000

        /**
         * 默认探测实现：`GET {服务地址}/health`，2xx 视为可达。
         *
         * 超时与连接错误由调用方按不可用处理；此处只负责发请求并读取状态码。
         */
        private suspend fun probeHealth(url: String): Boolean = withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    setRequestProperty("Accept", "application/json")
                }
                val code = connection.responseCode
                if (code in 200..299) connection.inputStream?.close() else connection.errorStream?.close()
                code in 200..299
            } finally {
                connection?.disconnect()
            }
        }
    }
}
