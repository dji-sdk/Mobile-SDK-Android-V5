package dji.v5.ux.mapkit.maplibre.annotations

import android.graphics.Color
import dji.v5.ux.mapkit.core.models.DJILatLng
import dji.v5.ux.mapkit.core.models.annotations.DJICircle

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
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeta
import com.mapbox.turf.TurfTransformation
import dji.v5.ux.mapkit.core.models.annotations.DJICircleOptions

class MaplibreCircle(private val mapboxMap: MapboxMap,
                     private val options: DJICircleOptions,
                     private val onRemoveCircle: (zindex: Int, circle: MaplibreCircle) -> Boolean,
                     private val onAddCircle: (zindex: Int, polyline: MaplibreCircle) -> Unit
) : DJICircle {
    private val borderLineString = getLineString(options.center, options.radius)

    private val source by lazy {
        GeoJsonSource(circleSourceIdAndIncrement, Polygon.fromOuterInner(borderLineString))
    }
    internal val circleLayer by lazy {
        options.fillColor.let {
            val solid = Color.rgb(Color.red(it), Color.green(it), Color.blue(it))
            val alpha = Color.alpha(it) / 255f
            FillLayer(circleLayerIdAndIncrement, source.id)
                    .withProperties(
                            PropertyFactory.fillColor(solid),
                            PropertyFactory.fillOpacity(alpha)
                    )
        }
    }

    private val borderSource by lazy {
        GeoJsonSource(circleBorderSourceIdAndIncrement, borderLineString)
    }
    internal val borderLayer by lazy {
        LineLayer(circleBorderLayerIdAndIncrement, borderSource.id)
                .withProperties(
                        PropertyFactory.lineColor(options.strokeColor),
                        PropertyFactory.lineWidth(options.strokeWidth / 5f)
                )
    }

    init {
        DJIMapkitLog.i(TAG, "init")
        mapboxMap.style?.let {
            if (it.isFullyLoaded) {
                setUpCircleSource(it)
                setUpBorderSource(it)
            }
        }
    }

    override fun remove() {
        DJIMapkitLog.i(TAG, "remove ${circleLayer.id}, ${borderLayer.id}")
        mapboxMap.style?.let {
            if (!onRemoveCircle(options.zIndex.toInt(), this)) {
                DJIMapkitLog.e(TAG, "remove circle $this fail")
            }
            it.removeLayerAndLog(circleLayer)
            it.removeLayerAndLog(borderLayer)
            it.removeSourceAndLog(source)
            it.removeSourceAndLog(borderSource)
        }
    }

    override fun setVisible(visible: Boolean) {
        circleLayer.withProperties(PropertyFactory.visibility(if (visible) Property.VISIBLE else Property.NONE))
        borderLayer.withProperties(PropertyFactory.visibility(if (visible) Property.VISIBLE else Property.NONE))
    }

    override fun isVisible(): Boolean = Property.VISIBLE == circleLayer.visibility.value

    override fun setFillColor(color: Int) {
        circleLayer.withProperties(PropertyFactory.fillColor(color))
    }

    override fun getFillColor(): Int {
        return circleLayer.fillColorAsInt
    }

    override fun setStrokeColor(color: Int) {
        borderLayer.withProperties(PropertyFactory.lineColor(color))
    }

    override fun getStrokeColor(): Int {
        return borderLayer.lineColorAsInt
    }

    override fun setCircle(center: DJILatLng, radius: Double) {
        getLineString(center, radius).let {
            source.setGeoJson(it)
            borderSource.setGeoJson(it)
        }
    }

    internal fun clearCircle() {
        mapboxMap.style?.let {
            DJIMapkitLog.i(TAG, "clear circle")
            it.removeLayerAndLog(circleLayer)
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

    private fun getLineString(center: DJILatLng, radius: Double) = fromDJILatLng(center).let {
        val centerPoint = Point.fromLngLat(it.longitude, it.latitude, it.altitude)
        LineString.fromLngLats(TurfMeta.coordAll(TurfTransformation.circle(centerPoint, radius, TurfConstants.UNIT_METERS), false))
    }

    private fun setUpBorderSource(style: Style) {
        style.addSourceAndLog(borderSource)
    }

    private fun setUpCircleSource(style: Style) {
        style.addSourceAndLog(source)
    }

    override fun toString(): String {
        return "MapboxCircle { circle layer id ${circleLayer.id}, circle source id ${source.id}, " +
                "border layer id ${borderLayer.id}, border source id ${borderSource.id}}"
    }

    override fun getCenter(): DJILatLng {
        return options.center
    }

    override fun setCenter(center: DJILatLng) {
        options.center(center)
        setCircle(center, radius)
    }

    override fun getRadius(): Double {
        return options.radius
    }

    override fun setRadius(radius: Double) {
        options.radius(radius)
        setCircle(center, radius)
    }

    override fun getStrokeWidth(): Float {
        return borderLayer.lineWidth.getValue() as Float * 5f
    }

    override fun setStrokeWidth(strokeWidth: Float) {
        options.strokeWidth(strokeWidth)
        borderLayer.withProperties(PropertyFactory.lineWidth(strokeWidth / 5f))
    }

    override fun getZIndex(): Float {
        return options.zIndex
    }

    override fun setZIndex(zIndex: Float) {
        mapboxMap.style?.let {
            onRemoveCircle(options.zIndex.toInt(), this)
            options.zIndex(zIndex)
            onAddCircle(options.zIndex.toInt(), this)
        }
    }

    companion object {
        private const val TAG = "MapboxCircle"
    }
}