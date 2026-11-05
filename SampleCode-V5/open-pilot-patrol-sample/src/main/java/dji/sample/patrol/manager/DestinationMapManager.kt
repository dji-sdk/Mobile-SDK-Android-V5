package dji.sample.patrol.manager

import android.util.Log
import dji.sample.patrol.data.DestinationFailureCategory
import dji.sample.patrol.data.DestinationPoint
import dji.sample.patrol.data.GeoPoint3D
import dji.sample.patrol.data.ResultCallback

/**
 * 目的地地图标注管理器（destination / route 图层）。
 *
 * 设计意图：把「标注目的地点位」「绘制航线轨迹」与地图 SDK 解耦——复用 `#4` 的 [PatrolMapPort]
 * （`PilotMapPort` 为生产实现）与图层/就绪门控纪律，仅新增独立 `destination`/`route` 图层，
 * 不触碰 `#4` 的 `targets` 图层。可 JVM fake 单测。
 *
 * 线程安全：所有方法在主线程调用。
 */
object DestinationMapManager {

    private const val TAG = "PatrolDestMap"
    private const val LAYER_DESTINATION = "destination"
    private const val LAYER_ROUTE = "route"
    private const val ROUTE_COLOR_ARGB = 0xFF2696FF.toInt()
    private const val ROUTE_WIDTH_DP = 4

    /** 可注入的地图端口（生产为 [PilotMapPort]）。 */
    @Volatile
    var mapPort: PatrolMapPort? = null

    /** 地图状态/落点失败回调（`map-error`，非阻塞）。 */
    @Volatile
    var onError: ((DestinationFailureCategory, String) -> Unit)? = null

    /** 预上传后的图标文件名（空表示无图标，按标题-only 落点）。 */
    @Volatile
    var iconFileName: String? = null

    private var destinationLayerId: String? = null
    private var routeLayerId: String? = null
    private var mapReady = false
    private var registered = false
    private var pending: DestinationPoint? = null
    private var pendingTitle = ""

    private val stateListener = MapStateListener { ready ->
        mapReady = ready
        Log.i(TAG, "map ready=$ready")
        if (ready) pending?.let { performMark(it, pendingTitle) }
    }

    /** 连接就绪后注册地图状态订阅。 */
    fun start() {
        if (registered) return
        val port = mapPort ?: return
        registered = true
        port.addMapStateListener(stateListener)
    }

    /** 离开任务/断连清理：隐藏并清空自建图层、精确移除订阅。 */
    fun cleanup() {
        val port = mapPort
        if (registered && port != null) {
            port.removeMapStateListener(stateListener)
        }
        registered = false
        destinationLayerId?.let { layerId -> port?.clearLayer(layerId, unitCallback({}, {})) }
        routeLayerId?.let { layerId -> port?.clearLayer(layerId, unitCallback({}, {})) }
        destinationLayerId = null
        routeLayerId = null
        pending = null
        mapReady = false
        Log.i(TAG, "cleanup done")
    }

    /** 上传目的地图标（供 `MARKER` 按文件名引用）；失败不影响后续标题-only 落点。 */
    fun uploadIcon(localPath: String) {
        val port = mapPort ?: return
        port.pushIconFile(localPath, stringCallback(
            onSuccess = { remoteName ->
                iconFileName = remoteName
                Log.i(TAG, "icon uploaded $remoteName")
            },
        ))
    }

    /**
     * 标注目的地点位。
     *
     * @return `true` 已受理（未就绪时暂存待地图就绪重试）；`false` 坐标无效。
     */
    fun mark(point: DestinationPoint, title: String): Boolean {
        if (!point.isValid) {
            report(DestinationFailureCategory.MAP_ERROR, "invalid destination coordinate")
            return false
        }
        if (mapPort == null) {
            report(DestinationFailureCategory.MAP_ERROR, "map port not injected")
            return false
        }
        pending = point
        pendingTitle = title
        if (!mapReady) {
            Log.i(TAG, "destination deferred until map ready")
            return true
        }
        performMark(point, title)
        return true
    }

    /** 绘制航线轨迹（route 图层，POLYLINE）；少于 2 点或未就绪时不绘制。 */
    fun drawRoute(points: List<GeoPoint3D>) {
        if (points.size < 2) return
        val port = mapPort ?: return
        if (!mapReady) {
            Log.i(TAG, "route deferred until map ready")
            return
        }
        ensureRouteLayer { layerId ->
            port.addPolyline(layerId, points, ROUTE_COLOR_ARGB, ROUTE_WIDTH_DP, LAYER_ROUTE, unitCallback(
                onSuccess = { Log.i(TAG, "route drawn points=${points.size}") },
                onFailure = { report(DestinationFailureCategory.MAP_ERROR, "addPolyline failed: ${it.message}") },
            ))
        }
    }

    private fun performMark(point: DestinationPoint, title: String) {
        val port = mapPort ?: return
        ensureDestinationLayer { layerId ->
            port.addMarker(
                layerId,
                GeoPoint3D(point.latitude, point.longitude, point.altitude),
                title,
                iconFileName.orEmpty(),
                LAYER_DESTINATION,
                unitCallback(
                    onSuccess = {
                        if (pending == point) pending = null
                        Log.i(TAG, "destination marked layer=$layerId")
                    },
                    onFailure = { report(DestinationFailureCategory.MAP_ERROR, "addMarker failed: ${it.message}") },
                ),
            )
        }
    }

    private fun ensureDestinationLayer(onReady: (String) -> Unit) {
        val existing = destinationLayerId
        if (existing != null) {
            onReady(existing)
            return
        }
        val port = mapPort ?: return
        port.addLayer(LAYER_DESTINATION, stringCallback(
            onSuccess = { layerId ->
                destinationLayerId = layerId
                Log.i(TAG, "destination layer ready id=$layerId")
                onReady(layerId)
            },
        ))
    }

    private fun ensureRouteLayer(onReady: (String) -> Unit) {
        val existing = routeLayerId
        if (existing != null) {
            onReady(existing)
            return
        }
        val port = mapPort ?: return
        port.addLayer(LAYER_ROUTE, stringCallback(
            onSuccess = { layerId ->
                routeLayerId = layerId
                Log.i(TAG, "route layer ready id=$layerId")
                onReady(layerId)
            },
        ))
    }

    private fun report(category: DestinationFailureCategory, message: String) {
        Log.w(TAG, "${category.id}: $message")
        onError?.invoke(category, message)
    }

    private fun unitCallback(
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit,
    ) = object : ResultCallback<Unit> {
        override fun onSuccess(data: Unit) = onSuccess()
        override fun onFailure(error: Throwable) = onFailure(error)
    }

    private fun stringCallback(
        onSuccess: (String) -> Unit,
        onFailure: (Throwable) -> Unit = {},
    ) = object : ResultCallback<String> {
        override fun onSuccess(data: String) = onSuccess(data)
        override fun onFailure(error: Throwable) = onFailure(error)
    }

    /** 测试辅助：重置到初始状态。 */
    fun resetForTest() {
        mapPort = null
        onError = null
        iconFileName = null
        destinationLayerId = null
        routeLayerId = null
        mapReady = false
        registered = false
        pending = null
        pendingTitle = ""
    }

    /** 测试辅助：直接注入地图就绪态（避免依赖端口回调）。 */
    fun setMapReadyForTest(ready: Boolean) {
        mapReady = ready
    }
}
