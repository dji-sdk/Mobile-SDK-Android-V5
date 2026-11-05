package dji.sample.patrol.manager

import android.util.Log
import dji.sample.patrol.data.CapturePoint
import dji.sample.patrol.data.RecognitionFailureCategory
import dji.sample.patrol.data.ResultCallback
import dji.sample.patrol.data.TargetKind

/**
 * 地图 `targets` 图层落点（design D6）：
 * - `addLayer` 成功后保存 `layer.id` 为 `layerId`（不使用只读默认层）；
 * - 仅当 `mapState.isMapReady == true` 才批量落点，未就绪保留点位待重试；
 * - 图标文件名字符串先按 `TargetKind` 串行预上传，未完成时保留点位；
 * - `addLayer`/`addMapWidget` 失败记 `anchor-error`、保留可重试、**不阻塞航线恢复**；
 * - 清理成对（隐藏/清空 + 精确 `removeMapStateListener`），清理失败不抛未捕获异常。
 *
 * 端口/图标/文案均可注入，使落点编排可脱离真机在 JVM 上单测。
 */
object PatrolMapManager {

    private const val TAG = "PatrolMap"
    private const val LAYER_NAME = "targets"

    @Volatile
    var mapPort: PatrolMapPort? = null

    /** 各类别本地图标路径；缺省时以标题落点（不引用未推送文件名）。 */
    @Volatile
    var iconLocalPaths: Map<TargetKind, String> = emptyMap()

    @Volatile
    var titleProvider: (TargetKind) -> String = { it.name }

    @Volatile
    var onError: (RecognitionFailureCategory, String) -> Unit = { _, _ -> }

    @Volatile
    private var layerId: String? = null

    @Volatile
    private var mapReady = false

    @Volatile
    private var stateListener: MapStateListener? = null

    private val pending = mutableListOf<CapturePoint>()
    private val uploadedIcons = mutableMapOf<TargetKind, String>()
    private val uploadingKinds = mutableSetOf<TargetKind>()

    /** 生产入口：订阅地图状态并建层。 */
    fun start() {
        val port = mapPort ?: return
        if (stateListener == null) {
            val listener = MapStateListener { ready -> onMapState(ready) }
            stateListener = listener
            port.addMapStateListener(listener)
        }
        ensureLayer()
    }

    fun onMapState(ready: Boolean) {
        mapReady = ready
        Log.i(TAG, "onMapState ready=$ready")
        if (ready) {
            // 图层通常在连接时就已建好，ensureLayer() 会直接返回；这里必须补一次 flush，
            // 否则「地图未就绪时排队的点位」在回到地图页后永远不会落点。
            ensureLayer()
            flush()
        }
    }

    /** 落点：入队并在就绪时批量落点；失败不抛出、不阻塞调用方。 */
    fun drop(point: CapturePoint) {
        pending += point
        if (layerId == null) ensureLayer()
        flush()
    }

    /** 离开任务/停止：隐藏并清空整层，精确移除地图状态监听，不残留。 */
    fun cleanup() {
        val port = mapPort
        val id = layerId
        if (port != null) {
            if (id != null) {
                val ignore = object : ResultCallback<Unit> {
                    override fun onSuccess(data: Unit) = Unit
                    override fun onFailure(error: Throwable) {
                        Log.w(TAG, "cleanup layer op failed: ${error.message}")
                    }
                }
                runCatching {
                    port.setLayerVisible(id, false, ignore)
                    port.clearLayer(id, ignore)
                }.onFailure { Log.w(TAG, "cleanup failed", it) }
            }
            stateListener?.let { runCatching { port.removeMapStateListener(it) } }
        }
        stateListener = null
        layerId = null
        mapReady = false
        pending.clear()
        uploadedIcons.clear()
        uploadingKinds.clear()
    }

    /** 测试注入点：替换端口/图标/文案/错误回调并复位。 */
    fun configure(
        port: PatrolMapPort?,
        icons: Map<TargetKind, String> = emptyMap(),
        titles: (TargetKind) -> String = { it.name },
        error: (RecognitionFailureCategory, String) -> Unit = { _, _ -> },
    ) {
        cleanup()
        mapPort = port
        iconLocalPaths = icons
        titleProvider = titles
        onError = error
        layerId = null
        mapReady = false
    }

    private fun ensureLayer() {
        if (layerId != null) return
        val port = mapPort ?: return
        port.addLayer(LAYER_NAME, object : ResultCallback<String> {
            override fun onSuccess(data: String) {
                layerId = data
                Log.i(TAG, "targets layer ready id=$data")
                flush()
            }

            override fun onFailure(error: Throwable) {
                report(RecognitionFailureCategory.ANCHOR_ERROR, "addLayer failed: ${error.message}")
            }
        })
    }

    private fun flush() {
        val port = mapPort ?: return
        val layer = layerId ?: return
        if (!mapReady) return
        val iterator = pending.iterator()
        while (iterator.hasNext()) {
            val point = iterator.next()
            val icon = ensureIcon(port, point.kind) ?: return
            iterator.remove()
            port.addMarker(
                layerId = layer,
                point = point.point,
                title = titleProvider(point.kind),
                iconFileName = icon,
                name = "targets/${point.kind}/${point.targetIndex}",
                callback = object : ResultCallback<Unit> {
                    override fun onSuccess(data: Unit) {
                        Log.i(TAG, "marker dropped kind=${point.kind} index=${point.targetIndex}")
                    }
                    override fun onFailure(error: Throwable) =
                        report(RecognitionFailureCategory.ANCHOR_ERROR, "addMapWidget failed: ${error.message}")
                },
            )
        }
    }

    /**
     * 返回该类别可用的图标文件名；未配置本地图标时返回空串（以标题落点）。
     * 上传进行中或失败时返回 `null`，保留点位待重试。
     */
    private fun ensureIcon(port: PatrolMapPort, kind: TargetKind): String? {
        uploadedIcons[kind]?.let { return it }
        val localPath = iconLocalPaths[kind] ?: run {
            uploadedIcons[kind] = ""
            return ""
        }
        if (!uploadingKinds.add(kind)) return null
        port.pushIconFile(localPath, object : ResultCallback<String> {
            override fun onSuccess(data: String) {
                uploadedIcons[kind] = data
                uploadingKinds.remove(kind)
                Log.i(TAG, "icon uploaded kind=$kind name=$data")
                flush()
            }

            override fun onFailure(error: Throwable) {
                uploadingKinds.remove(kind)
                // 不无限等待图标：降级为空图标名，由地图适配层渲染文字标签，保证点位可见。
                uploadedIcons[kind] = ""
                report(RecognitionFailureCategory.ANCHOR_ERROR, "icon upload failed, falling back to text label: ${error.message}")
                flush()
            }
        })
        return null
    }

    private fun report(category: RecognitionFailureCategory, message: String) {
        Log.w(TAG, "${category.id}: $message")
        runCatching { onError(category, message) }
    }
}
