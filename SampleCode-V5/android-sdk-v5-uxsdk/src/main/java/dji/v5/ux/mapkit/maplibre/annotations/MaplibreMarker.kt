package dji.v5.ux.mapkit.maplibre.annotations

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import dji.v5.ux.mapkit.core.maps.DJIInfoWindow
import dji.v5.ux.mapkit.core.maps.DJIMap
import dji.v5.ux.mapkit.core.models.DJIBitmapDescriptor
import dji.v5.ux.mapkit.core.models.DJIBitmapDescriptorFactory
import dji.v5.ux.mapkit.core.models.DJILatLng
import dji.v5.ux.mapkit.core.models.annotations.DJIMarker
import dji.v5.ux.mapkit.core.models.annotations.DJIMarkerOptions
import dji.v5.ux.mapkit.core.utils.DJIMapkitLog
import dji.v5.ux.mapkit.maplibre.utils.*
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.Property.ICON_ANCHOR_BOTTOM
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import dji.v5.ux.R

class MaplibreMarker(private val context: Context,
                     private val mapboxMap: MapboxMap,
                     private val options: DJIMarkerOptions,
                     private val onRemoveMarker: (zindex: Int, marker: MaplibreMarker) -> Boolean
) : DJIMarker() {
    private val source: GeoJsonSource by lazy {
        DJIMapkitLog.i(TAG, "init")
        val latLng = fromDJILatLng(options.position)
        GeoJsonSource(markerSourceIdAndIncrement, Point.fromLngLat(latLng.longitude, latLng.latitude, latLng.altitude))
    }
    internal val markerLayer: SymbolLayer by lazy {
        SymbolLayer(markerLayerId, source.id)
                .withProperties(
                        PropertyFactory.iconImage(iconId),
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconIgnorePlacement(true),
                        PropertyFactory.iconRotate(options.rotation)
                )
    }
    private val infoWindowLayer: SymbolLayer by lazy {
        SymbolLayer(infoWindowLayerId, source.id)
                .withProperties(
                        PropertyFactory.iconImage(infoWindowIconId),
                        PropertyFactory.iconAnchor(ICON_ANCHOR_BOTTOM),
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconOffset(arrayOf(0f, -markerIconHeight / 2f / pixelRatio))
                )
    }
    internal val sourceFeatureId = sourceFeatureIdAndIncrement
    private val iconId = markerIconIdAndIncrement
    internal val markerLayerId = markerLayerIdAndIncrement
    private var descriptor = options.icon
    private var markerIconWidth = 0
    private var markerIconHeight = 0
    private val pixelRatio = context.resources.displayMetrics.density
    private val infoWindowIconId: String by lazy { getInfoWindowIconId(iconId) }
    private val infoWindowLayerId: String by lazy { getInfoWindowLayerId(markerLayerId) }
    internal var infoWindowAdapter: DJIMap.InfoWindowAdapter? = null
    private var infoWindow: DJIInfoWindow? = null
        set(value) {
            field = value?.apply {
                updateInfoWindow(this)
                setOnViewChangedListener { updateInfoWindow(this) }
            }
        }
    private var title: String? = options.title
    private var draggable = options.draggable

    init {
        DJIMapkitLog.i(TAG, "init")
        mapboxMap.style?.let {
            if (it.isFullyLoaded) {
                setUpSource(it)
                setUpMarkerIcon(it, descriptor)
            }
        }
    }

    override fun setPosition(latLng: DJILatLng) {
        options.position(latLng)
        fromDJILatLng(latLng).let { source.setGeoJson(Point.fromLngLat(it.longitude, it.latitude)) }
    }

    override fun setRotation(rotation: Float) {
        markerLayer.withProperties(PropertyFactory.iconRotate(rotation))
    }

    override fun setIcon(bitmap: DJIBitmapDescriptor?) {
        mapboxMap.style?.let {
            if (it.isFullyLoaded) setUpMarkerIcon(it, bitmap)
        }
    }

    override fun setAnchor(u: Float, v: Float) {
        markerLayer.withProperties(PropertyFactory.iconOffset(arrayOf(u, v)))
    }

    override fun setTitle(title: String?) {
        this.title = title
    }

    override fun getTitle(): String? {
        return title
    }

    override fun getPosition(): DJILatLng {
        return options.position
    }

    override fun setVisible(visible: Boolean) {
        if (visible) {
            markerLayer.withProperties(PropertyFactory.visibility(Property.VISIBLE))
        } else {
            markerLayer.withProperties(PropertyFactory.visibility(Property.NONE))
        }
    }

    override fun isVisible(): Boolean {
        return markerLayer.visibility.getValue() == Property.VISIBLE
    }

    override fun showInfoWindow() {
        DJIMapkitLog.i(TAG, "show info window $iconId, is shown $isInfoWindowShown, info window ${infoWindowAdapter?.getInfoWindow(this)}")
        if (isInfoWindowShown) return
        infoWindowAdapter?.getInfoWindow(this)?.let {
            infoWindow = it as DJIInfoWindow
            it.onCreate()
            createInfoWindowLayer()
        }
    }

    override fun hideInfoWindow() {
        infoWindow?.let {
            infoWindow = null
            destroyInfoWindowLayer()
            mapboxMap.style?.removeImage(infoWindowIconId)
            it.onDestroy()
        }
    }

    override fun isInfoWindowShown(): Boolean {
        return infoWindow != null
    }

    override fun remove() {
        DJIMapkitLog.i(TAG, "remove this marker")
        hideInfoWindow()
        mapboxMap.style?.let {
            if (!onRemoveMarker(options.zIndex, this)) {
                DJIMapkitLog.e(TAG, "remove marker $this fail")
            }
            it.removeImage(iconId)
            it.removeLayerAndLog(markerLayer)
            it.removeSourceAndLog(source)
        }
    }

    override fun setDraggable(b: Boolean) {
        draggable = b
    }

    override fun isDraggable(): Boolean {
        return draggable
    }

    /**
     * 地图 style 切换后，将资源加载回来
     */
    internal fun restore() {
        DJIMapkitLog.i(TAG, "restore")
        infoWindow?.let {
            DJIMapkitLog.i(TAG, "restore info window $infoWindowLayerId")
            updateInfoWindow(it)
            createInfoWindowLayer()
        }
        mapboxMap.style?.let {
            setUpMarkerIcon(it, descriptor)
            it.addSourceAndLog(source)
        }
    }

    /**
     * 地图 style 切换前，将资源 remove 掉
     */
    internal fun clearMarker() {
        infoWindow?.let {
            DJIMapkitLog.i(TAG, "clear info window $infoWindowLayerId")
            destroyInfoWindowLayer()
            mapboxMap.style?.removeImage(infoWindowIconId)
        }
        mapboxMap.style?.let {
            DJIMapkitLog.i(TAG, "clear marker")
            it.removeLayerAndLog(markerLayer)
            it.removeSourceAndLog(source)
            it.removeImage(iconId)
        }
    }

    private fun createInfoWindowLayer() {
        mapboxMap.style?.addLayerAndLog(infoWindowLayer)
    }

    private fun destroyInfoWindowLayer() {
        mapboxMap.style?.removeLayerAndLog(infoWindowLayer)
    }

    private fun generateInfoWindowBitmap(infoWindow: View): Bitmap {
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED).let { infoWindow.measure(it, it) }
        infoWindow.run {
            layout(0, 0, measuredWidth, measuredHeight)
            val bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
                    .apply { eraseColor(Color.TRANSPARENT) }
            draw(Canvas(bitmap))
            return bitmap
        }
    }

    private fun setUpMarkerIcon(style: Style, icon: DJIBitmapDescriptor?) {
        descriptor = icon
        if (descriptor == null) {
            descriptor = DJIBitmapDescriptorFactory.fromResource(R.drawable.uxsdk_ic_bird)
        }
        fromDJIBitmapDescriptor(context, descriptor)?.let { bitmap ->
            markerIconWidth = bitmap.width
            markerIconHeight = bitmap.height
            style.removeImage(iconId)
            style.addImage(iconId, bitmap)
        }
    }

    private fun setUpSource(style: Style) {
        style.addSourceAndLog(source)
    }

    private fun updateInfoWindow(infoWindow: DJIInfoWindow?) {
        infoWindow?.let {
            val bitmap = generateInfoWindowBitmap(it as View)
            mapboxMap.style?.run {
                if (isFullyLoaded) {
                    removeImage(infoWindowIconId)
                    addImage(infoWindowIconId, bitmap)
                }
            }
        }
    }

    override fun toString(): String {
        var result = "MaplibreMarker { iconid = $iconId, markerLayerId = ${markerLayer.id}, " +
                "markerSourceId = ${source.id}}"
        if (isInfoWindowShown) {
            result += ", info window layer id = ${infoWindowLayer.id}"
        }
        return result
    }

    companion object {
        private const val TAG = "MaplibreMarker"
    }
}