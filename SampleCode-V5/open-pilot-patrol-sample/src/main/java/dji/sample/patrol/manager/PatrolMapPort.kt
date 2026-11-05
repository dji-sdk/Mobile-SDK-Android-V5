package dji.sample.patrol.manager

import android.util.Log
import dji.open.pilot.base.manager.OpenLocation
import dji.open.pilot.base.manager.map.MapDrawerType
import dji.open.pilot.base.manager.map.MapMarkerOptions
import dji.open.pilot.base.manager.map.MapPolylineOptions
import dji.open.pilot.base.manager.map.MapWidget
import dji.open.pilot.base.manager.map.MapWidgetLayer
import dji.open.pilot.client.manager.ui.IMapStateListener
import dji.open.pilot.client.manager.ui.MapWidgetManager
import dji.sample.patrol.data.GeoPoint3D
import dji.sample.patrol.data.ResultCallback
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import java.util.IdentityHashMap

/** 地图就绪订阅（工程内投影，不携带 `dji.*` 类型）。 */
fun interface MapStateListener {
    fun onMapState(ready: Boolean)
}

/**
 * 地图 SDK 集成边界（design D6/D9）：`targets` 图层与 MARKER 落点、图标预上传均可 JVM fake。
 */
interface PatrolMapPort {
    /** 建层成功后回调 `layer.id`（不使用只读默认层）。 */
    fun addLayer(name: String, callback: ResultCallback<String>)

    fun addMarker(
        layerId: String,
        point: GeoPoint3D,
        title: String,
        iconFileName: String,
        name: String,
        callback: ResultCallback<Unit>,
    )

    fun setLayerVisible(layerId: String, visible: Boolean, callback: ResultCallback<Unit>)
    fun clearLayer(layerId: String, callback: ResultCallback<Unit>)
    fun removeLayer(layerId: String, callback: ResultCallback<Unit>)
    fun addMapStateListener(listener: MapStateListener)
    fun removeMapStateListener(listener: MapStateListener)

    /** 在图层上绘制折线（route 轨迹），见 destination-flyto design D5。 */
    fun addPolyline(
        layerId: String,
        points: List<GeoPoint3D>,
        colorArgb: Int,
        widthDp: Int,
        name: String,
        callback: ResultCallback<Unit>,
    )

    /** 经 `FileTransferManager` 预上传图标文件，成功后回调远端文件名。 */
    fun pushIconFile(localPath: String, callback: ResultCallback<String>)
}

/** 生产实现：`MapWidgetManager.getInstance()` + `FileTransferManager`；`dji.*` import 仅落本文件。 */
object PilotMapPort : PatrolMapPort {

    private const val TAG = "PatrolMap"

    private val manager get() = MapWidgetManager.getInstance()
    private val stateListeners = IdentityHashMap<MapStateListener, IMapStateListener>()

    override fun addLayer(name: String, callback: ResultCallback<String>) {
        val layer = MapWidgetLayer(name)
        manager.addLayer(layer, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                Log.i(TAG, "addLayer onSuccess layerId=${layer.id}")
                callback.onSuccess(layer.id)
            }

            override fun onFailure(error: IDJIError) {
                Log.e(TAG, "addLayer onFailure ${error.errorCode()} ${error.description()}")
                callback.onFailure(IllegalStateException("${error.errorCode()}: ${error.description()}"))
            }
        })
    }

    override fun addMarker(
        layerId: String,
        point: GeoPoint3D,
        title: String,
        iconFileName: String,
        name: String,
        callback: ResultCallback<Unit>,
    ) {
        val widget = MapWidget().apply {
            drawerType = MapDrawerType.MARKER
            this.name = name
            setLayerId(layerId)
            markerOptions = MapMarkerOptions().apply {
                setPoint(OpenLocation(point.latitude, point.longitude, point.altitude))
                setTitle(title)
                if (iconFileName.isNotBlank()) {
                    setNormalIConFileName(iconFileName)
                    setClickedIConFileName(iconFileName)
                }
                setClickable(true)
            }
        }
        manager.addMapWidget(widget, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                Log.i(TAG, "addMapWidget onSuccess name=$name layer=$layerId")
                callback.onSuccess(Unit)
            }

            override fun onFailure(error: IDJIError) {
                Log.e(TAG, "addMapWidget onFailure $name ${error.errorCode()} ${error.description()}")
                callback.onFailure(IllegalStateException("${error.errorCode()}: ${error.description()}"))
            }
        })
    }

    override fun setLayerVisible(layerId: String, visible: Boolean, callback: ResultCallback<Unit>) =
        manager.setLayerVisible(layerId, visible, completion("setLayerVisible", callback))

    override fun clearLayer(layerId: String, callback: ResultCallback<Unit>) =
        manager.clearLayer(layerId, completion("clearLayer", callback))

    override fun removeLayer(layerId: String, callback: ResultCallback<Unit>) =
        manager.removeLayer(layerId, completion("removeLayer", callback))

    override fun addPolyline(
        layerId: String,
        points: List<GeoPoint3D>,
        colorArgb: Int,
        widthDp: Int,
        name: String,
        callback: ResultCallback<Unit>,
    ) {
        val widget = MapWidget().apply {
            drawerType = MapDrawerType.POLYLINE
            this.name = name
            setLayerId(layerId)
            polylineOptions = MapPolylineOptions().apply {
                setPoints(points.map { OpenLocation(it.latitude, it.longitude, it.altitude) })
                setWidth(widthDp.toFloat())
                setColor(colorArgb)
            }
        }
        manager.addMapWidget(widget, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                Log.i(TAG, "addMapWidget(polyline) onSuccess name=$name layer=$layerId points=${points.size}")
                callback.onSuccess(Unit)
            }

            override fun onFailure(error: IDJIError) {
                Log.e(TAG, "addMapWidget(polyline) onFailure $name ${error.errorCode()} ${error.description()}")
                callback.onFailure(IllegalStateException("${error.errorCode()}: ${error.description()}"))
            }
        })
    }

    override fun addMapStateListener(listener: MapStateListener) {
        val sdkListener = IMapStateListener { state ->
            Log.i(TAG, "isMapReady=${state.isMapReady}")
            listener.onMapState(state.isMapReady)
        }
        synchronized(stateListeners) { stateListeners[listener] = sdkListener }
        manager.addMapStateListener(sdkListener)
    }

    override fun removeMapStateListener(listener: MapStateListener) {
        val sdkListener = synchronized(stateListeners) { stateListeners.remove(listener) } ?: return
        manager.removeMapStateListener(sdkListener)
    }

    /** 与 AR 图标共用串行推送队列，避免两处同时上传触发 `UPLOAD_FILE_BUSY`（design D5）。 */
    override fun pushIconFile(localPath: String, callback: ResultCallback<String>) =
        RemoteFilePusher.push(localPath, callback)

    private fun completion(operation: String, callback: ResultCallback<Unit>) =
        object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                Log.i(TAG, "$operation onSuccess")
                callback.onSuccess(Unit)
            }

            override fun onFailure(error: IDJIError) {
                Log.e(TAG, "$operation onFailure ${error.errorCode()} ${error.description()}")
                callback.onFailure(IllegalStateException("${error.errorCode()}: ${error.description()}"))
            }
        }
}
