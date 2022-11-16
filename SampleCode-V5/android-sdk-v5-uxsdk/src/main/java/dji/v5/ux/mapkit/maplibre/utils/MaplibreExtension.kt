@file:JvmName("MapboxExtension")

package dji.v5.ux.mapkit.maplibre.utils

import dji.v5.ux.mapkit.core.utils.DJIMapkitLog
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.Layer
import com.mapbox.mapboxsdk.style.sources.Source

private const val TAG = "MapboxExtension"

fun Style.removeLayerAndLog(layer: Layer) =
        if (this.removeLayer(layer)) {
            DJIMapkitLog.i(TAG, "remove layer ${layer.id} success")
            true
        } else {
            DJIMapkitLog.e(TAG, "remove layer ${layer.id} fail")
            false
        }

fun Style.removeSourceAndLog(source: Source) =
        if (sources.contains(source) && this.removeSource(source)) {
            DJIMapkitLog.i(TAG, "remove source ${source.id} success")
            true
        } else {
            DJIMapkitLog.e(TAG, "remove source ${source.id} fail")
            false
        }

fun Style.addLayerAndLog(layer: Layer) {
    this.addLayer(layer)
    DJIMapkitLog.i(TAG, "add layer ${layer.id} success")
}

fun Style.addSourceAndLog(source: Source) {
    this.addSource(source)
    DJIMapkitLog.i(TAG, "add source ${source.id} success")
}