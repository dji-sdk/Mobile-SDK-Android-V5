package dji.v5.ux.mapkit.maplibre.annotations

import androidx.annotation.ColorInt
import dji.v5.ux.mapkit.core.models.DJILatLng
import dji.v5.ux.mapkit.core.models.annotations.DJIPolyline
import dji.v5.ux.mapkit.core.models.annotations.DJIPolylineOptions
import dji.v5.ux.mapkit.core.utils.DJIMapkitLog
import dji.v5.ux.mapkit.maplibre.utils.*
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource

class MaplibrePolyline(private val mapboxMap: MapboxMap,
                       val options: DJIPolylineOptions,
                       private val onRemovePolyline: (zindex: Int, polyline: MaplibrePolyline) -> Boolean,
                       private val onAddPolyline: (zindex: Int, polyline: MaplibrePolyline) -> Unit
) : DJIPolyline {
    private val source by lazy {
        options.points.map { fromDJILatLng(it) }
                .map { Point.fromLngLat(it.longitude, it.latitude, it.altitude) }
                .let { GeoJsonSource(polylineSourceIdAndIncrement, LineString.fromLngLats(it)) }
    }

    internal val polylineLayer by lazy {
        val propertyArray = arrayOf(PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.lineWidth(options.width / 5f),
                PropertyFactory.lineColor(options.color))

        LineLayer(polylineLayerIdAndIncrement, source.id)
                .withProperties(*propertyArray)
    }

    init {
        DJIMapkitLog.i(TAG, "init")
        mapboxMap.style?.let {
            if (it.isFullyLoaded) {
                setUpPolylineSource(it)
            }
        }
    }

    override fun remove() {
        DJIMapkitLog.i(TAG, "remove")
        mapboxMap.style?.let { style ->
            if (!onRemovePolyline(options.zIndex.toInt(), this)) {
                DJIMapkitLog.e(TAG, "remove polyline $this fail")
            }
            style.removeLayerAndLog(polylineLayer)
            style.removeSourceAndLog(source)
        }
    }

    override fun setWidth(width: Float) {
        polylineLayer.withProperties(PropertyFactory.lineWidth(width / 5f))
    }

    override fun getWidth(): Float {
        return polylineLayer.lineWidth.getValue() as Float * 5f
    }

    override fun setPoints(points: List<DJILatLng>) {
        options.points = points
        points.map { fromDJILatLng(it) }
                .map { Point.fromLngLat(it.longitude, it.latitude, it.altitude) }
                .let { source.setGeoJson(LineString.fromLngLats(it)) }
    }

    override fun getPoints(): MutableList<DJILatLng> {
        return options.points
    }

    override fun setColor(@ColorInt color: Int) {
        polylineLayer.withProperties(PropertyFactory.lineColor(color))
    }

    @ColorInt
    override fun getColor(): Int {
        return polylineLayer.lineColorAsInt
    }

    internal fun clear() {
        DJIMapkitLog.i(TAG, "clear")
        mapboxMap.style?.let { style ->
            style.removeLayerAndLog(polylineLayer)
            style.removeSourceAndLog(source)
        }
    }

    internal fun restore() {
        DJIMapkitLog.i(TAG, "restore")
        mapboxMap.style?.addSourceAndLog(source)
    }

    private fun setUpPolylineSource(style: Style) {
        style.addSourceAndLog(source)
    }

    override fun getZIndex(): Float {
        return options.zIndex
    }

    override fun setZIndex(zIndex: Float) {
        mapboxMap.style?.let {
            onRemovePolyline(options.zIndex.toInt(), this)
            options.zIndex(zIndex)
            onAddPolyline(options.zIndex.toInt(), this)
        }
    }

    companion object {
        private const val TAG = "MaplibrePolyline"
    }
}