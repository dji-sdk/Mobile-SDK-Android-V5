package dji.sample.patrol.manager

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dji.sample.patrol.data.Asset
import dji.sample.patrol.data.AssetState
import dji.sample.patrol.data.CapturePoint
import dji.sample.patrol.data.EventEntry
import dji.sample.patrol.data.PatrolEvent
import dji.sample.patrol.data.PatrolReportBatch
import dji.sample.patrol.data.PointEntry
import dji.sample.patrol.data.UploadStage
import java.io.File
import java.security.MessageDigest

/**
 * 点位/事件上报器（M3 边飞边上传）。
 *
 * 设计意图：把 FR#4 产出的拍照点位（[CapturePoint]）与识别事件（[PatrolEvent]）按**事件时间窗**
 * 合批，序列化为 JSON 文件并构造成框架 [Asset]，交给既有上传引擎 [AssetUploadManager] 边飞边同步；
 * 收尾时用 [finalScan] 做增量补扫补传。本类不新增任何 DJI SDK 调用。
 *
 * 可测试性：时间与调度、入队出口均通过可注入端口（[TimeSource] / [Scheduler] / [AssetSink]）抽象，
 * 使批量/去重/序列化逻辑可在 JVM 上以 fake 端口单测，不加载 `dji.*`。
 *
 * 线程安全：对外方法可在主线程或回调线程调用，内部以 `synchronized` 保护批次状态。
 */
object PatrolAssetReporter {

    private const val TAG = "PatrolReport"

    /** 事件时间窗（毫秒）：窗口内到达的点位/事件合批为一个 JSON Asset。 */
    const val WINDOW_MS = 2000L

    /** 单批最大条目数，达到即立即 flush，避免窗口过长导致大文件。 */
    const val MAX_ITEMS_PER_BATCH = 50

    /** 时间源（测试可注入固定时钟）。 */
    interface TimeSource {
        fun now(): Long
    }

    /** 调度器（测试可注入同步执行；默认 postDelayed 到主线程 Handler）。 */
    fun interface Scheduler {
        fun postDelayed(delayMs: Long, task: () -> Unit)
    }

    /** 入队出口（默认转调 [AssetUploadManager.enqueue]；测试可注入 fake 收集）。 */
    fun interface AssetSink {
        fun enqueue(jobId: String, assets: List<Asset>)
    }

    private val systemTimeSource = object : TimeSource {
        override fun now(): Long = System.currentTimeMillis()
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private val mainScheduler = Scheduler { delayMs, task ->
        mainHandler.postDelayed(task, delayMs.coerceAtLeast(0L))
    }

    @Volatile
    var timeSource: TimeSource = systemTimeSource

    @Volatile
    var scheduler: Scheduler = mainScheduler

    @Volatile
    var assetSink: AssetSink = AssetSink { jobId, assets -> AssetUploadManager.enqueue(jobId, assets) }

    private val mutableStage = MutableLiveData(UploadStage.IDLE)

    /** 上传编排态：供浮窗/通知栏只读订阅。 */
    val stage: LiveData<UploadStage> = mutableStage

    /** 上传编排态的非 LiveData 快照（JVM 单测与内部判定用，避免依赖主线程）。 */
    @Volatile
    var currentStage: UploadStage = UploadStage.IDLE
        private set

    @Volatile
    private var appContext: Context? = null

    /** JSON 落盘根目录（默认应用私有 filesDir；JVM 单测可注入临时目录）。 */
    @Volatile
    var filesDir: File? = null

    /** 当前活动工单（点位/事件归属）。 */
    @Volatile
    var activeJobId: String? = null
        private set

    private val handledPointKeys = linkedSetOf<String>()
    private val handledEventIds = linkedSetOf<String>()
    private val pendingPoints = mutableListOf<CapturePoint>()
    private val pendingEvents = mutableListOf<PatrolEvent>()
    private var windowStart = 0L
    private var flushScheduled = false
    private var generation = 0L

    // 编排态输入
    @Volatile
    private var executing = false

    @Volatile
    private var finalizingJobId: String? = null

    @Volatile
    private var queueSnapshot: List<Asset> = emptyList()

    /** 连接就绪后初始化（注入 Context 与活动工单）。 */
    @Synchronized
    fun start(context: Context, jobId: String?) {
        appContext = context.applicationContext
        filesDir = context.filesDir
        resetForJob(jobId)
    }

    /** 切换/开始新工单：清空批次与去重集合。 */
    @Synchronized
    fun resetForJob(jobId: String?) {
        cancelScheduledLocked()
        pendingPoints.clear()
        pendingEvents.clear()
        windowStart = 0L
        handledPointKeys.clear()
        handledEventIds.clear()
        activeJobId = jobId
    }

    /** 停止（断连/离开任务）：清理进行中未完成批次，不删除已生成待上传文件。 */
    @Synchronized
    fun stop() {
        cancelScheduledLocked()
        val droppedPoints = pendingPoints.size
        val droppedEvents = pendingEvents.size
        pendingPoints.clear()
        pendingEvents.clear()
        windowStart = 0L
        executing = false
        finalizingJobId = null
        if (droppedPoints + droppedEvents > 0) {
            Log.i(TAG, "stop: cancelled in-flight batch points=$droppedPoints events=$droppedEvents")
        }
    }

    /**
     * 消费点位流（demo 只传照片）。
     *
     * 设计意图：点位不再序列化为 JSON 资源，只保留本地日志用于排查；照片仍由
     * `MediaManager`/`AssetUploadManager` 既有链路落盘即入队。
     */
    @Synchronized
    fun updatePoints(points: List<CapturePoint>) {
        if (points.isNotEmpty()) {
            Log.i(TAG, "points observed (local only): count=${points.size} complete=${points.count { it.complete }}")
        }
    }

    /** 记录一次识别事件（demo 只传照片）：仅写本地日志，不生成上传资源。 */
    @Synchronized
    fun recordEvent(event: PatrolEvent) {
        Log.i(TAG, "event (local only): id=${event.id} kind=${event.kind} index=${event.targetIndex} desc=${event.description}")
    }

    /**
     * 收尾补扫（demo 只传照片）：不再产生点位/事件资源，返回空列表。
     * 保留签名供既有收尾编排调用；照片补传仍由上传队列自身状态驱动。
     */
    @Synchronized
    fun finalScan(jobId: String): List<Asset> {
        Log.i(TAG, "finalScan: photo-only mode, no report assets for job=$jobId")
        return emptyList()
    }

    private fun legacyFinalScan(jobId: String): List<Asset> {
        if (activeJobId != jobId) activeJobId = jobId
        return flushLocked(jobId)
    }

    private fun addPointLocked(jobId: String, point: CapturePoint) {
        if (pendingPoints.isEmpty() && pendingEvents.isEmpty()) windowStart = timeSource.now()
        pendingPoints.add(point)
        onPendingAddedLocked(jobId)
    }

    private fun addEventLocked(jobId: String, event: PatrolEvent) {
        if (pendingPoints.isEmpty() && pendingEvents.isEmpty()) windowStart = timeSource.now()
        pendingEvents.add(event)
        onPendingAddedLocked(jobId)
    }

    private fun onPendingAddedLocked(jobId: String) {
        if (pendingPoints.size + pendingEvents.size >= MAX_ITEMS_PER_BATCH) {
            flushLocked(jobId)
            return
        }
        scheduleFlushLocked(jobId)
    }

    private fun scheduleFlushLocked(jobId: String) {
        if (flushScheduled) return
        flushScheduled = true
        val token = generation
        val elapsed = (timeSource.now() - windowStart).coerceAtLeast(0L)
        val delay = (WINDOW_MS - elapsed).coerceAtLeast(0L)
        Log.i(TAG, "schedule flush job=$jobId delayMs=$delay pending=${pendingPoints.size + pendingEvents.size}")
        scheduler.postDelayed(delay) {
            val job = synchronized(this) {
                if (token != generation) return@synchronized null
                flushScheduled = false
                activeJobId
            }
            if (job != null) flush(job)
        }
    }

    private fun cancelScheduledLocked() {
        generation++
        flushScheduled = false
    }

    /** 立即 flush 当前批次（收尾/窗口到期共用）；成功才标记已处理并出队。 */
    @Synchronized
    fun flush(jobId: String): List<Asset> = flushLocked(jobId)

    private fun flushLocked(jobId: String): List<Asset> {
        if ((pendingPoints.isEmpty() && pendingEvents.isEmpty()) || activeJobId != jobId) {
            return emptyList()
        }
        val points = pendingPoints.toList()
        val events = pendingEvents.toList()
        val start = windowStart
        val end = timeSource.now()
        val assets = buildAssets(jobId, start, end, points, events)
        if (assets.isEmpty()) {
            // 序列化/写盘失败：保留待上报数据（可重试），不产生半成品 Asset。
            Log.w(TAG, "flush failed job=$jobId points=${points.size} events=${events.size}; kept for retry")
            return emptyList()
        }
        points.forEach { handledPointKeys.add(dedupKey(it)) }
        events.forEach { handledEventIds.add(it.id) }
        pendingPoints.clear()
        pendingEvents.clear()
        windowStart = 0L
        cancelScheduledLocked()
        assets.forEach { asset ->
            Log.i(TAG, "flush produced job=$jobId asset=${asset.assetId} type=${asset.typeId} size=${asset.sizeBytes}")
        }
        assetSink.enqueue(jobId, assets)
        return assets
    }

    private fun buildAssets(
        jobId: String,
        windowStart: Long,
        windowEnd: Long,
        points: List<CapturePoint>,
        events: List<PatrolEvent>,
    ): List<Asset> {
        val baseDir = filesDir ?: return emptyList()
        val result = mutableListOf<Asset>()
        if (points.isNotEmpty()) {
            val batch = PatrolReportBatch(
                jobId = jobId,
                windowStart = windowStart,
                windowEnd = windowEnd,
                points = points.map {
                    PointEntry(
                        latitude = it.point.latitude,
                        longitude = it.point.longitude,
                        altitude = it.point.altitude,
                        kind = it.kind.name,
                        targetIndex = it.targetIndex,
                        capturedAt = it.capturedAt,
                        assetId = it.asset?.assetId,
                    )
                },
            )
            writeAsset(baseDir, jobId, "point", windowStart, batch)?.let(result::add)
        }
        if (events.isNotEmpty()) {
            val batch = PatrolReportBatch(
                jobId = jobId,
                windowStart = windowStart,
                windowEnd = windowEnd,
                events = events.map {
                    EventEntry(
                        id = it.id,
                        kind = it.kind.name,
                        targetIndex = it.targetIndex,
                        at = it.at,
                        description = it.description,
                    )
                },
            )
            writeAsset(baseDir, jobId, "event", windowStart, batch)?.let(result::add)
        }
        // 若两类都写失败，result 为空 → 调用方保留 pending 待重试。
        return result
    }

    private fun writeAsset(
        baseDir: File,
        jobId: String,
        typeId: String,
        windowStart: Long,
        batch: PatrolReportBatch,
    ): Asset? {
        val dir = File(baseDir, "patrol-events/$jobId")
        if (!dir.exists() && !dir.mkdirs()) {
            Log.w(TAG, "cannot create report dir ${dir.absolutePath}")
            return null
        }
        val fileName = "${typeId}-$windowStart.json"
        val target = File(dir, fileName)
        val tmp = File(dir, "$fileName.part")
        return try {
            tmp.writeText(buildJson(batch))
            if (tmp.length() <= 0L) {
                tmp.delete()
                return null
            }
            if (target.exists()) target.delete()
            if (!tmp.renameTo(target)) {
                tmp.delete()
                return null
            }
            val size = target.length()
            val md5 = md5(target)
            Asset(
                assetId = "patrol-$typeId-$jobId-$windowStart",
                jobId = jobId,
                typeId = typeId,
                localUri = target.absolutePath,
                sizeBytes = size,
                md5 = md5,
                state = AssetState.DETECTED,
                meta = mapOf("fileName" to fileName, "kind" to "report"),
            )
        } catch (error: Throwable) {
            Log.w(TAG, "write report $fileName failed: ${error.message}")
            runCatching { tmp.delete() }
            null
        }
    }

    /**
     * 纯 Kotlin JSON 序列化（不依赖 `org.json`：其在 JVM 单测中是 android.jar 桩，
     * 生产与测试行为需一致）。
     */
    private fun buildJson(batch: PatrolReportBatch): String {
        val sb = StringBuilder(256)
        sb.append('{')
        appendStringField(sb, "jobId", batch.jobId)
        sb.append(",\"windowStart\":").append(batch.windowStart)
        sb.append(",\"windowEnd\":").append(batch.windowEnd)
        sb.append(",\"points\":[")
        batch.points.forEachIndexed { index, entry ->
            if (index > 0) sb.append(',')
            sb.append('{')
            sb.append("\"latitude\":").append(entry.latitude)
            sb.append(",\"longitude\":").append(entry.longitude)
            sb.append(",\"altitude\":").append(entry.altitude)
            sb.append(',')
            appendStringField(sb, "kind", entry.kind)
            sb.append(",\"targetIndex\":").append(entry.targetIndex)
            sb.append(",\"capturedAt\":").append(entry.capturedAt)
            sb.append(',')
            appendStringField(sb, "assetId", entry.assetId ?: "")
            sb.append('}')
        }
        sb.append("],\"events\":[")
        batch.events.forEachIndexed { index, entry ->
            if (index > 0) sb.append(',')
            sb.append('{')
            appendStringField(sb, "id", entry.id)
            sb.append(',')
            appendStringField(sb, "kind", entry.kind)
            sb.append(",\"targetIndex\":").append(entry.targetIndex)
            sb.append(",\"at\":").append(entry.at)
            sb.append(',')
            appendStringField(sb, "description", entry.description ?: "")
            sb.append('}')
        }
        sb.append("]}")
        return sb.toString()
    }

    private fun appendStringField(sb: StringBuilder, name: String, value: String) {
        sb.append('"').append(name).append("\":\"").append(escapeJson(value)).append('"')
    }

    private fun escapeJson(value: String): String {
        val sb = StringBuilder(value.length + 8)
        value.forEach { ch ->
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (ch < ' ') sb.append("\\u%04x".format(ch.code)) else sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun md5(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun dedupKey(point: CapturePoint): String = "${point.targetIndex}/${point.kind}"

    // ---- 上传编排态 ----

    /** 航线是否处于执行中（边飞边同步）。 */
    fun onRouteExecuting(executing: Boolean) {
        this.executing = executing
        recomputeStage()
    }

    /** 当前处于收尾的工单（`JobStatus.FINALIZING`）；`null` 表示未收尾。 */
    fun onJobFinalizing(jobId: String?) {
        finalizingJobId = jobId
        recomputeStage()
    }

    /** 活动工单变化。 */
    fun onJobActive(jobId: String?) {
        if (activeJobId != jobId) activeJobId = jobId
        recomputeStage()
    }

    /** 上传队列快照变化（由观测者驱动）。 */
    fun onQueueChanged(queue: List<Asset>) {
        queueSnapshot = queue
        recomputeStage()
    }

    private fun recomputeStage() {
        val jobId = finalizingJobId ?: activeJobId
        val next = when {
            jobId == null -> UploadStage.IDLE
            else -> {
                val assets = queueSnapshot.filter { it.jobId == jobId }
                when {
                    assets.any { it.state == AssetState.FAILED } -> UploadStage.FAILED
                    finalizingJobId != null && assets.isNotEmpty() &&
                        assets.all { it.state == AssetState.UPLOADED } -> UploadStage.COMPLETED
                    finalizingJobId != null -> UploadStage.FINALIZING
                    executing -> UploadStage.LIVE
                    else -> UploadStage.IDLE
                }
            }
        }
        if (currentStage != next) {
            Log.i(TAG, "upload stage -> $next job=$jobId")
            currentStage = next
            // LiveData 仅在 Android 主线程环境同步；JVM 单测读取 currentStage。
            runCatching { mutableStage.postValue(next) }
        }
    }

    /** 测试辅助：重置到初始状态并恢复默认端口。 */
    @Synchronized
    fun resetForTest() {
        cancelScheduledLocked()
        pendingPoints.clear()
        pendingEvents.clear()
        windowStart = 0L
        handledPointKeys.clear()
        handledEventIds.clear()
        activeJobId = null
        executing = false
        finalizingJobId = null
        queueSnapshot = emptyList()
        appContext = null
        filesDir = null
        timeSource = systemTimeSource
        scheduler = mainScheduler
        assetSink = AssetSink { jobId, assets -> AssetUploadManager.enqueue(jobId, assets) }
        currentStage = UploadStage.IDLE
        runCatching { mutableStage.postValue(UploadStage.IDLE) }
    }
}
