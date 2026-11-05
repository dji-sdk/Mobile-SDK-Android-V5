package dji.sample.patrol.manager

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import dji.sample.patrol.data.Asset
import dji.sample.patrol.data.CapturePoint
import dji.sample.patrol.data.DetectionTarget
import dji.sample.patrol.data.GeoPoint3D
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.v5.manager.KeyManager

/** 飞行器当前经纬高探针（design D5/D9）：可 JVM fake。 */
interface GeoProbe3D {
    fun currentLocation(): GeoPoint3D?
}

/** 生产实现：同步读 `FlightControllerKey.KeyAircraftLocation3D`。 */
object KeyManagerGeoProbe3D : GeoProbe3D {

    private val locationKey = KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D)

    override fun currentLocation(): GeoPoint3D? {
        val coordinate = KeyManager.getInstance().getValue(locationKey) ?: return null
        val latitude = coordinate.latitude ?: return null
        val longitude = coordinate.longitude ?: return null
        val altitude = coordinate.altitude ?: return null
        return GeoPoint3D(latitude, longitude, altitude)
    }
}

/**
 * 拍照点位记录（design D4）：
 * - 计数口径：**一次快门 = 一条记录 = 1 组**；同一目标重复拍照各自计数，不覆盖前一条；
 * - 以飞行器当前经纬高为点位；无效坐标**不写点**、保留可重试；
 * - 一次快门的多镜头文件（如 `_W`/`_Z`/`_T`）归属同一组，组内任一照片落盘即 `complete = true`；
 * - `(targetIndex, targetType)` 去重仅用于「不再重复触发识别」，不再用于合并点位记录；
 * - 兜底关联只接受该点位时间窗内、同一工单、未被占用的照片，避免张冠李戴；
 * - 停止/离开任务清理进行中未完成记录。
 */
object PointReporter {

    private const val TAG = "PatrolPoint"

    /** 兜底关联时间窗：点位记录时间与照片拍摄时间的允许偏差（前 / 后）。 */
    private const val ATTACH_BEFORE_MS = 5_000L
    private const val ATTACH_AFTER_MS = 30_000L

    private val mutablePoints = MutableLiveData<List<CapturePoint>>(emptyList())
    val points: LiveData<List<CapturePoint>> = mutablePoints

    @Volatile
    var geoProbe: GeoProbe3D? = null

    @Volatile
    var activeJobId: String? = null

    /** 每次拍照一条记录，按时间顺序追加。 */
    private val records = mutableListOf<CapturePoint>()

    /** 已完成（组内照片已落盘）的目标键；仅用于识别去重，不参与计数。 */
    private val completedTargets = mutableSetOf<String>()

    private var nextPointId = 0L

    @Volatile
    private var assetsObserver: Observer<List<Asset>>? = null

    @Volatile
    private var assetsSource: LiveData<List<Asset>>? = null

    /** 记录一次拍照点位；坐标无效返回 `null`（不写点）。一次调用计 1 组。 */
    @Synchronized
    fun record(target: DetectionTarget, assets: List<Asset> = emptyList()): CapturePoint? {
        val location = geoProbe?.currentLocation()
        if (location == null || !isValid(location)) {
            Log.w(TAG, "point ignored: invalid aircraft location $location for ${target.dedupKey}")
            return null
        }
        val point = CapturePoint(
            point = location,
            kind = target.targetType,
            targetIndex = target.targetIndex,
            capturedAt = System.currentTimeMillis(),
            asset = assets.firstOrNull(),
            assets = assets,
            complete = assets.isNotEmpty(),
            id = ++nextPointId,
        )
        records += point
        if (point.complete) completedTargets += target.dedupKey
        publish()
        Log.i(
            TAG,
            "point recorded ${target.dedupKey} group=${point.id} assets=${assets.size} " +
                "complete=${point.complete} total=${records.size}",
        )
        return point
    }

    /**
     * 把本次拍照下载到的整组照片绑定到对应点位。
     *
     * 设计意图：一次快门可能产生多个镜头文件，必须整组绑定；只接受拍摄时间落在该点位时间窗内的
     * 照片，避免把其他组或历史照片挂到该点位。
     */
    @Synchronized
    fun attach(pointId: Long, assets: List<Asset>) {
        if (assets.isEmpty()) return
        val index = records.indexOfFirst { it.id == pointId }
        if (index < 0) {
            Log.w(TAG, "attach ignored: point $pointId not found")
            return
        }
        val point = records[index]
        val known = point.assets.associateBy { it.assetId }.toMutableMap()
        val accepted = assets.filter { asset ->
            val capturedAt = asset.meta["capturedAtMillis"]?.toLongOrNull()
            capturedAt == null || withinAttachWindow(point.capturedAt, capturedAt)
        }
        accepted.forEach { known[it.assetId] = it }
        val merged = known.values.toList()
        if (merged.isEmpty()) {
            Log.w(TAG, "attach ignored: no asset within point window, point=$pointId")
            return
        }
        records[index] = point.copy(
            asset = merged.first(),
            assets = merged,
            complete = true,
        )
        completedTargets += "${point.targetIndex}/${point.kind}"
        publish()
        Log.i(TAG, "point completed $pointId assets=${merged.map { it.assetId }}")
    }

    /** 已记录**完整**点位的同键目标不再触发。 */
    @Synchronized
    fun isRecorded(target: DetectionTarget): Boolean = target.dedupKey in completedTargets

    /**
     * 观察媒体 Asset 出现，把未完成点位补全（兜底：直接回调缺失时使用）。
     *
     * 只接受拍摄时间在该点位时间窗内、同一工单、且未被其他点位占用的照片；无拍摄时间的旧资产
     * 不参与，避免关联历史照片。
     */
    @Synchronized
    fun onAssetsUpdated(assets: List<Asset>) {
        if (records.isEmpty()) return
        val used = records.flatMap { point -> point.assets.map { it.assetId } }.toMutableSet()
        var changed = false
        records.forEachIndexed { index, point ->
            if (point.complete || point.cancelled) return@forEachIndexed
            val jobId = activeJobId
            val asset = assets.firstOrNull { candidate ->
                if (candidate.typeId != "photo") return@firstOrNull false
                if (jobId != null && candidate.jobId != jobId) return@firstOrNull false
                if (candidate.assetId in used) return@firstOrNull false
                val capturedAt = candidate.meta["capturedAtMillis"]?.toLongOrNull()
                    ?: return@firstOrNull false
                withinAttachWindow(point.capturedAt, capturedAt)
            } ?: return@forEachIndexed
            used += asset.assetId
            records[index] = point.copy(
                asset = asset,
                assets = point.assets + asset,
                complete = true,
            )
            completedTargets += "${point.targetIndex}/${point.kind}"
            changed = true
            Log.i(TAG, "point completed ${point.id} asset=${asset.assetId} (fallback)")
        }
        if (changed) publish()
    }

    /** 生产：观察 [MediaManager.localAssets]。 */
    @Synchronized
    fun observeAssets(liveData: LiveData<List<Asset>>) {
        stopObservingAssets()
        val observer = Observer<List<Asset>> { value -> onAssetsUpdated(value.orEmpty()) }
        assetsObserver = observer
        assetsSource = liveData
        liveData.observeForever(observer)
    }

    @Synchronized
    fun stopObservingAssets() {
        val observer = assetsObserver
        val source = assetsSource
        if (observer != null && source != null) source.removeObserver(observer)
        assetsObserver = null
        assetsSource = null
    }

    /** 停止/离开任务：清理进行中未完成记录，不残留半成品。 */
    @Synchronized
    fun cancelInFlight() {
        val removed = records.filter { !it.complete && !it.cancelled }
        if (removed.isEmpty()) return
        records.removeAll(removed.toSet())
        publish()
        Log.i(TAG, "cancelled in-flight points: ${removed.map { it.id }}")
    }

    @Synchronized
    fun clear() {
        records.clear()
        completedTargets.clear()
        publish()
    }

    /** 测试注入点：替换探针/工单并清空记录。 */
    fun configure(probe: GeoProbe3D?, jobId: String? = null) {
        geoProbe = probe
        activeJobId = jobId
        records.clear()
        completedTargets.clear()
        publish()
    }

    private fun withinAttachWindow(pointCapturedAt: Long, assetCapturedAt: Long): Boolean =
        assetCapturedAt >= pointCapturedAt - ATTACH_BEFORE_MS &&
            assetCapturedAt <= pointCapturedAt + ATTACH_AFTER_MS

    private fun isValid(location: GeoPoint3D): Boolean =
        location.latitude in -90.0..90.0 &&
            location.longitude in -180.0..180.0 &&
            location.altitude.isFinite() &&
            !(location.latitude == 0.0 && location.longitude == 0.0)

    private fun publish() {
        mutablePoints.postValue(records.toList())
    }
}
