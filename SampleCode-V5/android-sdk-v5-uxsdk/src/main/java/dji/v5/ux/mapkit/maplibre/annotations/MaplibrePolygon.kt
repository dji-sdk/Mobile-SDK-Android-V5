package dji.v5.ux.mapkit.maplibre.annotations

import dji.v5.ux.mapkit.core.models.DJILatLng
import dji.v5.ux.mapkit.core.models.annotations.DJIPolygon
import dji.v5.ux.mapkit.core.models.annotations.DJIPolygonOptions
import dji.v5.ux.mapkit.core.utils.DJIMapkitLog
import dji.v5.ux.mapkit.maplibre.utils.*
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource

class MaplibrePolygon(private val mapboxMap: MapboxMap,
                      private val options: DJIPolygonOptions,
                      private val onRemovePolygon: (zindex: Int, polygon: MaplibrePolygon) -> Boolean
) : DJIPolygon {
    private val source by lazy {
        GeoJsonSource(polygonSourceIdAndIncrement, Polygon.fromLngLats(listOf(getPolygonLngLats(options.points))))
    }

    internal val polygonLayer by lazy {
        FillLayer(polygonLayerIdAndIncrement, source.id)
                .withProperties(
                        PropertyFactory.fillColor(options.fillColor),
                        PropertyFactory.fillOpacity(options.alpha)
                )
    }

    private val borderSource by lazy {
        GeoJsonSource(polygonBorderSourceIdAndIncrement, LineString.fromLngLats(getPolygonLngLats(options.points)))
    }

    internal val borderLayer by lazy {
        LineLayer(polygonBorderLayerIdAndIncrement, borderSource.id)
                .withProperties(
                        PropertyFactory.lineColor(options.strokeColor),
                        PropertyFactory.lineWidth(options.strokeWidth / 5f),
                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND))
    }

    init {
        DJIMapkitLog.i(TAG, "init")
        mapboxMap.style?.let {
            if (it.isFullyLoaded) {
                setUpPolygonSource(it)
                setUpBorderSource(it)
            }
        }
    }

    override fun remove() {
        DJIMapkitLog.i(TAG, "remove ${polygonLayer.id}, ${borderLayer.id}")
        mapboxMap.style?.let {
            if (!onRemovePolygon(options.zIndex.toInt(), this)) {
                DJIMapkitLog.e(TAG, "remove polygon $this fail")
            }
            it.removeLayerAndLog(polygonLayer)
            it.removeLayerAndLog(borderLayer)
            it.removeSourceAndLog(source)
            it.removeSourceAndLog(borderSource)
        }
    }

    override fun isVisible(): Boolean = Property.VISIBLE == polygonLayer.visibility.value

    override fun setVisible(visible: Boolean) {
        polygonLayer.withProperties(PropertyFactory.visibility(if (visible) Property.VISIBLE else Property.NONE))
        borderLayer.withProperties(PropertyFactory.visibility(if (visible) Property.VISIBLE else Property.NONE))
    }

    override fun getFillColor(): Int {
        return polygonLayer.fillColorAsInt
    }

    override fun setFillColor(color: Int) {
        polygonLayer.withProperties(PropertyFactory.fillColor(color))
    }

    override fun getStrokeColor(): Int {
        return borderLayer.lineColorAsInt
    }

    override fun setStrokeColor(color: Int) {
        borderLayer.withProperties(PropertyFactory.fillColor(color))
    }

    override fun getStrokeWidth(): Float {
        return borderLayer.lineWidth.getValue() as Float * 5f
    }

    override fun setStrokeWidth(strokeWidth: Float) {
        borderLayer.withProperties(PropertyFactory.lineWidth(strokeWidth / 5f))
    }

    override fun setPoints(points: MutableList<DJILatLng>?) {
        options.points = points
        source.setGeoJson(Polygon.fromLngLats(listOf(getPolygonLngLats(options.points))))
        borderSource.setGeoJson(LineString.fromLngLats(getPolygonLngLats(options.points)))
    }

    override fun getPoints(): MutableList<DJILatLng> {
        return options.points
    }

    internal fun clear() {
        DJIMapkitLog.i(TAG, "clear")
        mapboxMap.style?.let {
            it.removeLayerAndLog(polygonLayer)
            it.removeSourceAndLog(source)
            it.removeLayerAndLog(borderLayer)
            it.removeSourceAndLog(borderSource)
        }
    }

    internal fun restore() {
        DJIMapkitLog.i(TAG, "restore")
        mapboxMap.style?.let {
            it.addSourceAndLog(source)
            it.addSourceAndLog(borderSource)
        }
    }

    private fun getPolygonLngLats(points: List<DJILatLng>): List<Point> =
            if (points.first() != points.last()) {
                points.toMutableList()
                        .apply { add(points.first()) }
                        .map { fromDJILatLng(it) }
                        .map { Point.fromLngLat(it.longitude, it.latitude, it.altitude) }
                        .toList()
            } else {
                points.map { fromDJILatLng(it) }
                        .map { Point.fromLngLat(it.longitude, it.latitude, it.altitude) }
                        .toList()
            }

    private fun setUpBorderSource(style: Style) {
        style.addSourceAndLog(borderSource)
    }

    private fun setUpPolygonSource(style: Style) {
        style.addSourceAndLog(source)
    }

    companion object {
        private const val TAG = "MaplibrePolygon"
    }
}