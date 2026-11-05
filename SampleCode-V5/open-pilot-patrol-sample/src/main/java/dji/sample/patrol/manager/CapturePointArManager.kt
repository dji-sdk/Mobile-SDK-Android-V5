package dji.sample.patrol.manager

import android.util.Log
import dji.sample.patrol.data.CapturePoint
import dji.sample.patrol.data.RecognitionFailureCategory
import dji.sample.patrol.data.ResultCallback
import dji.sample.patrol.data.TargetKind

/**
 * 拍照点位 AR 投射编排（design D2/D6/D7/D8/D9）。
 *
 * 设计意图：
 * - **默认开启、无开关**：连接就绪即预热图标与类别配置，拍照成功即投射；
 * - **每类别一个配置**：图标按类别区分，`configId` 按类别缓存，断连后按名称复用；
 * - **只投射本次新增点位**：不做历史回放，任务边界统一清理；
 * - **失败非阻塞**：任何 AR 失败只上报既有识别错误通道，不影响拍照、航线恢复与地图落点。
 *
 * 所有 SDK 交互经 [arPort]，本对象可在 JVM 上单测；面向用户的文字由 [labelProvider] 注入。
 */
object CapturePointArManager {

    private const val TAG = "PatrolCapArMgr"

    /** 文字字号（Pilot 渲染参数，非 Android 布局资源）。 */
    const val TEXT_SIZE = 40f

    /** 正交投影图标的固定像素大小，与地图 marker 的恒定视觉大小一致。 */
    const val ICON_SIZE = 80f

    /** 图传画面文字颜色：白色，配半透明深色底提升复杂背景下的可读性。 */
    const val TEXT_COLOR = 0xFFFFFFFF.toInt()

    /** 文字背景色：60% 不透明深色。 */
    const val TEXT_BACKGROUND_COLOR = 0x99000000.toInt()

    /** 可注入的 AR 端口（生产为 [PilotCapturePointArPort]）。 */
    @Volatile
    var arPort: CapturePointArPort? = null

    /** 类别本地图标路径（启动时从 assets 复制到私有目录）。 */
    @Volatile
    var iconLocalPaths: Map<TargetKind, String> = emptyMap()

    /** 面向用户的标识文字（类别标题 · 序号），由装配层注入资源化文案。 */
    @Volatile
    var labelProvider: (CapturePoint) -> String = { "${it.kind} · ${it.targetIndex}" }

    /** 失败回调（非阻塞，接入既有识别错误通道）。 */
    @Volatile
    var onError: ((RecognitionFailureCategory, String) -> Unit)? = null

    private val lock = Any()
    private val configIds = HashMap<TargetKind, String>()
    private val uploadedInfoIds = ArrayList<String>()

    @Volatile
    private var started = false

    /** 类别配置名：既是上传配置名，也是断连后 `queryARPointConfig` 的复用匹配键。 */
    fun configNameFor(kind: TargetKind): String = "patrol-capture-${kind.name.lowercase()}"

    /**
     * 连接就绪后预热：先按名称复用既有配置，再逐类别串行推送图标并补建缺失配置。
     *
     * 串行而非并发：文件推送由 [RemoteFilePusher] 串行处理，逐类别推进可让配置上传顺序可预测，
     * 也避免一次任务开头集中打到 AR 服务端。
     */
    fun start(iconLocalPaths: Map<TargetKind, String>) {
        this.iconLocalPaths = iconLocalPaths
        if (started) return
        val port = arPort ?: run {
            report(RecognitionFailureCategory.AR_NOT_READY, "AR port not injected")
            return
        }
        started = true
        port.queryPointConfigs(object : ResultCallback<List<ArPointConfigInfo>> {
            override fun onSuccess(data: List<ArPointConfigInfo>) {
                reuseConfigs(data)
                prepareAll(TargetKind.entries.toList())
            }

            override fun onFailure(error: Throwable) {
                // 查询失败不阻断：按「无既有配置」处理，逐类别重新上传。
                Log.w(TAG, "queryARPointConfig failed, will upload configs: ${error.message}")
                prepareAll(TargetKind.entries.toList())
            }
        })
    }

    /**
     * 投射一个拍照点位（每次拍照成功调用一次）。
     *
     * @return `true` 已发起投射；`false` 未就绪或坐标无效（已按类别上报）。
     */
    fun project(point: CapturePoint): Boolean {
        val port = arPort ?: run {
            report(RecognitionFailureCategory.AR_NOT_READY, "AR port not injected")
            return false
        }
        if (!isValid(point)) {
            report(
                RecognitionFailureCategory.AR_INVALID_LOCATION,
                "invalid capture point ${point.id} (${point.point.latitude},${point.point.longitude})",
            )
            return false
        }
        val configId = synchronized(lock) { configIds[point.kind] } ?: run {
            report(
                RecognitionFailureCategory.AR_NOT_READY,
                "config not ready for ${point.kind}, skip point ${point.id}",
            )
            return false
        }
        val text = runCatching { labelProvider(point) }.getOrElse { "${point.kind} · ${point.targetIndex}" }
        port.uploadPointInfo(
            configId = configId,
            point = point.point,
            text = text,
            textColor = TEXT_COLOR,
            callback = object : ResultCallback<String> {
                override fun onSuccess(data: String) {
                    synchronized(lock) { uploadedInfoIds += data }
                    Log.i(TAG, "project onSuccess id=$data kind=${point.kind} text=$text")
                }

                override fun onFailure(error: Throwable) {
                    report(
                        RecognitionFailureCategory.AR_UPLOAD_FAILED,
                        "uploadARPointInfo failed point=${point.id}: ${error.message}",
                    )
                }
            },
        )
        return true
    }

    /**
     * 任务边界清理：删除本次已投射的远端点位，保留类别配置供下一轮复用。
     *
     * 单个删除失败只上报不中断；链路已断时由 [reset] 承担本地清空。
     */
    fun clear() {
        val port = arPort
        val ids = synchronized(lock) {
            val copy = uploadedInfoIds.toList()
            uploadedInfoIds.clear()
            copy
        }
        if (ids.isEmpty()) return
        if (port == null) {
            Log.w(TAG, "clear skipped: AR port not injected, local ids cleared (${ids.size})")
            return
        }
        ids.forEach { id ->
            port.deletePointInfo(id, object : ResultCallback<Unit> {
                override fun onSuccess(data: Unit) {
                    Log.i(TAG, "delete point onSuccess id=$id")
                }

                override fun onFailure(error: Throwable) {
                    report(
                        RecognitionFailureCategory.AR_CLEANUP_FAILED,
                        "deleteARPointInfo failed id=$id: ${error.message}",
                    )
                }
            })
        }
    }

    /**
     * 断连清理：只清本地引用（链路已断，无法删除远端对象），重连后重新预热并查询复用。
     */
    fun reset() {
        synchronized(lock) {
            uploadedInfoIds.clear()
            configIds.clear()
        }
        started = false
        Log.i(TAG, "reset done")
    }

    private fun reuseConfigs(configs: List<ArPointConfigInfo>) {
        configs.forEach { config ->
            val kind = TargetKind.entries.firstOrNull { configNameFor(it) == config.name } ?: return@forEach
            synchronized(lock) { configIds[kind] = config.id }
            Log.i(TAG, "reuse AR config kind=$kind configId=${config.id}")
        }
    }

    private fun prepareAll(kinds: List<TargetKind>) {
        val kind = kinds.firstOrNull() ?: return
        prepareKind(kind) { prepareAll(kinds.drop(1)) }
    }

    private fun prepareKind(kind: TargetKind, done: () -> Unit) {
        val port = arPort ?: return done()
        val alreadyReady = synchronized(lock) { configIds[kind] != null }
        if (alreadyReady) {
            // 配置已复用；图标仍需按同名覆盖推送一次，保证远端图标与当前 APK 版本一致。
            pushIcon(port, kind) { done() }
            return
        }
        pushIcon(port, kind) { remoteName ->
            if (remoteName == null) {
                done()
                return@pushIcon
            }
            port.uploadPointConfig(
                configName = configNameFor(kind),
                iconFileName = remoteName,
                textSize = TEXT_SIZE,
                iconSize = ICON_SIZE,
                textBackgroundColor = TEXT_BACKGROUND_COLOR,
                callback = object : ResultCallback<String> {
                    override fun onSuccess(data: String) {
                        if (data.isBlank()) {
                            report(RecognitionFailureCategory.AR_UPLOAD_FAILED, "empty configId for $kind")
                        } else {
                            synchronized(lock) { configIds[kind] = data }
                            Log.i(TAG, "config ready kind=$kind configId=$data icon=$remoteName")
                        }
                        done()
                    }

                    override fun onFailure(error: Throwable) {
                        report(
                            RecognitionFailureCategory.AR_UPLOAD_FAILED,
                            "uploadARPointConfig failed kind=$kind: ${error.message}",
                        )
                        done()
                    }
                },
            )
        }
    }

    /** 推送类别图标；成功回调远端文件名，失败回调 null（已上报）。 */
    private fun pushIcon(port: CapturePointArPort, kind: TargetKind, done: (String?) -> Unit) {
        val localPath = iconLocalPaths[kind]
        if (localPath.isNullOrBlank()) {
            report(RecognitionFailureCategory.AR_ICON_UNAVAILABLE, "missing local icon for $kind")
            done(null)
            return
        }
        port.pushIconFile(localPath, object : ResultCallback<String> {
            override fun onSuccess(data: String) {
                Log.i(TAG, "icon pushed kind=$kind remote=$data")
                done(data)
            }

            override fun onFailure(error: Throwable) {
                report(
                    RecognitionFailureCategory.AR_ICON_UNAVAILABLE,
                    "push icon failed kind=$kind: ${error.message}",
                )
                done(null)
            }
        })
    }

    private fun isValid(point: CapturePoint): Boolean = with(point.point) {
        latitude in -90.0..90.0 &&
            longitude in -180.0..180.0 &&
            altitude.isFinite() &&
            !(latitude == 0.0 && longitude == 0.0)
    }

    private fun report(category: RecognitionFailureCategory, message: String) {
        Log.w(TAG, "${category.id}: $message")
        runCatching { onError?.invoke(category, message) }
    }

    /** 测试辅助：重置到初始状态。 */
    fun resetForTest() {
        arPort = null
        iconLocalPaths = emptyMap()
        labelProvider = { "${it.kind} · ${it.targetIndex}" }
        onError = null
        synchronized(lock) {
            configIds.clear()
            uploadedInfoIds.clear()
        }
        started = false
    }

    /** 测试辅助：直接注入类别配置就绪态（跳过预热）。 */
    fun setConfigReadyForTest(kind: TargetKind, configId: String) {
        synchronized(lock) { configIds[kind] = configId }
    }

    /** 测试辅助：读取本次待清理的点位 id 数量。 */
    fun uploadedCountForTest(): Int = synchronized(lock) { uploadedInfoIds.size }
}
