package dji.v5.ux.mapkit.maplibre.map

import android.content.Context
import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import dji.v5.ux.mapkit.core.callback.MapScreenShotListener
import dji.v5.ux.mapkit.core.callback.OnMapTypeLoadedListener
import dji.v5.ux.mapkit.core.camera.DJICameraUpdate
import dji.v5.ux.mapkit.core.maps.DJIBaseMap
import dji.v5.ux.mapkit.core.maps.DJIMap
import dji.v5.ux.mapkit.core.maps.DJIMap.MapType
import dji.v5.ux.mapkit.core.models.DJICameraPosition
import dji.v5.ux.mapkit.core.models.annotations.*
import dji.v5.ux.mapkit.core.utils.DJIMapkitLog
import dji.v5.ux.mapkit.maplibre.annotations.MaplibreCircle
import dji.v5.ux.mapkit.maplibre.annotations.MaplibreMarker
import dji.v5.ux.mapkit.maplibre.annotations.MaplibrePolygon
import dji.v5.ux.mapkit.maplibre.annotations.MaplibrePolyline
import dji.v5.ux.mapkit.maplibre.utils.*
import com.mapbox.geojson.Feature
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.Layer
import dji.v5.utils.common.LogUtils
import java.util.*
import kotlin.collections.HashSet

class MaplibreMapDelegateKt(private val mapboxMap: MapboxMap,
                            private val context: Context,
                            mapView: View
) : DJIBaseMap() {
    init {
        mapboxMap.apply {
            addOnCameraMoveListener { handleCameraMove() }
            addOnMapClickListener { handleClickMap(it) }
            addOnMapLongClickListener { handleLongClickMap(it) }
        }
        mapView.setOnTouchListener { v, event -> handleTouch(v, event) }
    }

    private fun <T> HashSet<T>.addAndLog(e: T) {
        this.add(e)
        DJIMapkitLog.i(TAG, "[add] current set $this")
    }

    private fun <T> HashSet<T>.removeAndLog(e: T): Boolean {
        return if (!this.remove(e)) {
            DJIMapkitLog.e(TAG, "[remove] $e from hash set $this")
            false
        } else {
            true
        }
    }

    private fun <T> TreeSet<T>.addAndLog(e: T) {
        this.add(e)
        DJIMapkitLog.i(TAG, "[add] current tree set $this")
    }

    private fun <T> TreeSet<T>.removeAndLog(e: T): Boolean {
        return if (!this.remove(e)) {
            DJIMapkitLog.e(TAG, "[remove] $e from tree set $this")
            false
        } else {
            true
        }
    }

    private var infoWindowAdapter: DJIMap.InfoWindowAdapter? = null
    private var currentSelectedMarker: MaplibreMarker? = null

    private val markerSet = HashSet<MaplibreMarker>()
    private val circleSet = HashSet<MaplibreCircle>()
    private val polygonSet = HashSet<MaplibrePolygon>()
    private val polylineSet = HashSet<MaplibrePolyline>()
    private val sortedLayerWithZindex = TreeSet<LayerWithZindex>()

    override fun addMarker(markerOptions: DJIMarkerOptions): DJIMarker? {
        if (mapboxMap.style == null) {
            return null
        }
        return MaplibreMarker(context, mapboxMap, markerOptions) { zindex, marker ->
            val removeMarker = markerSet.removeAndLog(marker)
            val removeLayer = removeLayer(marker.markerLayer, zindex)
            removeLayer && removeMarker
        }.also {
            it.infoWindowAdapter = infoWindowAdapter
            addLayer(it.markerLayer, markerOptions.zIndex)
            markerSet.addAndLog(it)
        }
    }

    override fun addMarkerCircle(options: DJICircleOptions?): DJICircle? {
        TODO("Not yet implemented")
    }

    override fun addGroupCircle(options: DJIGroupCircleOptions?): DJIGroupCircle? {
        TODO("Not yet implemented")
    }

    override fun getMap() = mapboxMap

    override fun animateCamera(cameraUpdate: DJICameraUpdate) {
        mapboxMap.animateCamera(fromDJICameraUpdate(cameraUpdate, mapboxMap.cameraPosition))
    }

    override fun getCameraPosition(): DJICameraPosition = fromCameraPosition(mapboxMap.cameraPosition)

    override fun moveCamera(cameraUpdate: DJICameraUpdate) {
        mapboxMap.moveCamera(fromDJICameraUpdate(cameraUpdate, mapboxMap.cameraPosition))
    }

    override fun setInfoWindowAdapter(adapter: DJIMap.InfoWindowAdapter) {
        infoWindowAdapter = adapter
        markerSet.forEach { marker ->
            marker.infoWindowAdapter = adapter
        }
    }

    override fun setMapType(type: MapType) {
        setMapType(type, null)
    }

    override fun setMapType(type: MapType, listener: OnMapTypeLoadedListener?) {
        markerSet.forEach { it.clearMarker() }
        circleSet.forEach { it.clearCircle() }
        polygonSet.forEach { it.clear() }
        polylineSet.forEach { it.clear() }
        mapboxMap.setStyle(fromMapType(type)) { restoreResources(it, listener) }
    }

    private fun restoreResources(style: Style, listener: OnMapTypeLoadedListener?) {
        markerSet.forEach { it.restore() }
        circleSet.forEach { it.restore() }
        polygonSet.forEach { it.restore() }
        polylineSet.forEach { it.restore() }
        sortedLayerWithZindex.forEach {
            style.addLayerAndLog(it.layer)
        }
        listener?.onMapTypeLoaded()
    }

    override fun setMapType(type: Int) {
        when (type) {
            DJIMap.MAP_TYPE_NORMAL -> setMapType(MapType.NORMAL)
            DJIMap.MAP_TYPE_SATELLITE -> setMapType(MapType.SATELLITE)
            DJIMap.MAP_TYPE_HYBRID -> setMapType(MapType.HYBRID)
            else -> throw throw IllegalArgumentException("$type is not implemented")
        }
    }

    override fun addPolyline(options: DJIPolylineOptions) : MaplibrePolyline?  {
       if (mapboxMap.style == null) {
            return null
        }
    return MaplibrePolyline(mapboxMap, options, { zindex, polyline ->
        val removePolyline = polylineSet.removeAndLog(polyline)
        val removeLayer = removeLayer(polyline.polylineLayer, zindex)
        removePolyline && removeLayer
    }) { zindex, polyline ->
        addPolylineAtZIndex(zindex, polyline)
    }.also {
        addPolylineAtZIndex(options.zIndex.toInt(), it)
    }
    }

    private fun addPolylineAtZIndex(zindex: Int, polyline: MaplibrePolyline) {
        addLayer(polyline.polylineLayer, zindex)
        polylineSet.addAndLog(polyline)
    }

    override fun addPolygon(options: DJIPolygonOptions) = MaplibrePolygon(mapboxMap, options) { zindex, polygon ->
        val removePolygon = polygonSet.removeAndLog(polygon)
        val removeLayer = removeLayer(polygon.polygonLayer, zindex)
        val removeBorder = removeLayer(polygon.borderLayer, zindex)
        removePolygon && removeLayer && removeBorder
    }.also {
        addLayer(it.polygonLayer, options.zIndex.toInt())
        addLayer(it.borderLayer, options.zIndex.toInt())
        polygonSet.addAndLog(it)
    }

    override fun addSingleCircle(options: DJICircleOptions): DJICircle {
        return MaplibreCircle(mapboxMap, options, { zindex, circle ->
            val removeCircle = circleSet.removeAndLog(circle)
            val removeLayer = removeLayer(circle.circleLayer, zindex)
            val removeBorder = removeLayer(circle.borderLayer, zindex)
            removeCircle && removeLayer && removeBorder
        }) { zindex, circle ->
            addCircleAtZIndex(zindex, circle)
        }.also {
            addCircleAtZIndex(options.zIndex.toInt(), it)
        }
    }

    private fun addCircleAtZIndex(zindex: Int, circle: MaplibreCircle) {
        addLayer(circle.circleLayer, zindex)
        addLayer(circle.borderLayer, zindex)
        circleSet.addAndLog(circle)
    }

    override fun getUiSettings() = MUiSettings(mapboxMap.uiSettings)

    override fun snapshot(callback: MapScreenShotListener) {
        mapboxMap.snapshot { callback.onMapScreenShot(it) }
    }

    override fun getProjection() = MProjection(mapboxMap.projection)
    override fun clear() {
        DJIMapkitLog.i(TAG, "clear")
        sortedLayerWithZindex.apply {
            forEach { mapboxMap.style?.removeLayerAndLog(it.layer) }
            clear()
        }
        markerSet.apply {
            forEach { it.clearMarker() }
            clear()
        }
        circleSet.apply {
            forEach { it.clearCircle() }
            clear()
        }
        polygonSet.apply {
            forEach { it.clear() }
            clear()
        }
        polylineSet.apply {
            forEach { it.clear() }
            clear()
        }
    }

    private fun addLayer(layer: Layer, zindex: Int) {
        DJIMapkitLog.i(TAG, "[addLayer] ready to add ${layer.id}, zindex $zindex")
        mapboxMap.style?.let { style ->
            val layerWithZindex = LayerWithZindex(layer, zindex).also { sortedLayerWithZindex.addAndLog(it) }
            if (sortedLayerWithZindex.size == 1) {
                style.addLayerAndLog(layer)
            } else {
                val lower = sortedLayerWithZindex.lower(layerWithZindex)
                if (lower != null) {
                    style.addLayerAbove(layer, lower.layer.id)
                } else {
                    sortedLayerWithZindex.higher(layerWithZindex)?.let { style.addLayerBelow(layer, it.layer.id) }
                }
            }
        }
    }

    private fun removeLayer(layer: Layer, zindex: Int): Boolean {
        return sortedLayerWithZindex.removeAndLog(LayerWithZindex(layer, zindex))
    }

    private fun handleClickIcon(screenPoint: PointF): Boolean {
        for (marker: MaplibreMarker in markerSet) {
            val selectedMarkerFeatureList: List<Feature> = mapboxMap.queryRenderedFeatures(screenPoint, marker.markerLayerId)
            if (selectedMarkerFeatureList.isNotEmpty()) {
                onMarkerClick(marker)
                if (marker.isInfoWindowShown) marker.hideInfoWindow()
                else marker.showInfoWindow()
                return true
            }
        }
        return false
    }

    private fun handleClickMap(latLng: LatLng): Boolean {
        val isIconClick = handleClickIcon(mapboxMap.projection.toScreenLocation(latLng))
        if (!isIconClick) {
            onMapClick(fromLatLng(latLng))
        }
        return true
    }

    private fun handleLongClickMap(latLng: LatLng): Boolean {
        val pointF = mapboxMap.projection.toScreenLocation(latLng)
        for (marker: MaplibreMarker in markerSet) {
            val selectedMarkerFeatureList: List<Feature> = mapboxMap.queryRenderedFeatures(pointF, marker.markerLayerId)
            if (selectedMarkerFeatureList.isNotEmpty()) {
                if (marker.isDraggable) {
                    currentSelectedMarker = marker
                    onMarkerDragStart(marker)
                    return true
                }
                return false
            }
        }
        onMapLongClick(fromLatLng(latLng))
        return true
    }

    private fun handleTouch(v: View, motionEvent: MotionEvent): Boolean {
        LogUtils.e(TAG , "view is null$v")
        val action = motionEvent.action
        if (action == MotionEvent.ACTION_UP) {
            if (currentSelectedMarker != null) {
                onMarkerDragEnd(currentSelectedMarker)
            }
            currentSelectedMarker = null
        }
        return if (currentSelectedMarker != null) {
            if (action == MotionEvent.ACTION_MOVE) {
                val x = motionEvent.x
                val y = motionEvent.y
                val latLng = mapboxMap.projection.fromScreenLocation(PointF(x, y))

                currentSelectedMarker?.position = MaplibreUtils.fromLatLng(latLng)
                onMarkerDrag(currentSelectedMarker)
            }
            true
        } else {
            false
        }
    }

    private fun handleCameraMove() {
        val p = mapboxMap.cameraPosition
        val cameraPosition = fromCameraPosition(p)
        onCameraChange(cameraPosition)
    }

    /**
     * 因为 addLayer 只支持 layer，不支持 layer id，因此这里只能保存 layer
     */
    private data class LayerWithZindex(val layer: Layer, val zindex: Int) : Comparable<LayerWithZindex> {

        override fun compareTo(other: LayerWithZindex): Int {
            val zindexResult = this.zindex - other.zindex
            return if (zindexResult != 0) {
                zindexResult
            } else {
                this.layer.id.compareTo(other.layer.id)
            }
        }

        override fun toString(): String {
            return "LayerWithZindex(layer=${layer.id}, zindex=$zindex)"
        }
    }

    private companion object {
        private const val TAG = "MaplibreMapDelegateKt"
    }
}