@file:JvmName("MapboxUtil")

package dji.v5.ux.mapkit.maplibre.utils

import android.content.Context
import android.graphics.Bitmap
import dji.v5.ux.mapkit.core.camera.DJICameraUpdate
import dji.v5.ux.mapkit.core.camera.DJICameraUpdateFactory
import dji.v5.ux.mapkit.core.maps.DJIMap
import dji.v5.ux.mapkit.core.models.DJIBitmapDescriptor
import dji.v5.ux.mapkit.core.models.DJIBitmapDescriptor.Type
import dji.v5.ux.mapkit.core.models.DJICameraPosition
import dji.v5.ux.mapkit.core.models.DJILatLng
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdate
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.utils.BitmapUtils

private const val FEATURE_ID_PREFIX = "FEATURE_ID_PREFIX"
private const val MARKER_ICON_ID_PREFIX = "MARKER_ICON_ID_PREFIX"
private const val MARKER_LAYER_ID_PREFIX = "MARKER_LAYER_ID_PREFIX"
private const val MARKER_SOURCE_ID_PREFIX = "MARKER_SOURCE_ID_PREFIX"

private const val CIRCLE_SOURCE_ID_PREFIX = "CIRCLE_SOURCE_ID_PREFIX"
private const val CIRCLE_LAYER_ID_PREFIX = "CIRCLE_LAYER_ID_PREFIX"
private const val CIRCLE_BORDER_LAYER_ID_PREFIX = "CIRCLE_BORDER_LAYER_ID_PREFIX"
private const val CIRCLE_BORDER_SOURCE_ID_PREFIX = "CIRCLE_BORDER_SOURCE_ID_PREFIX"

private const val POLYGON_LAYER_ID_PREFIX = "POLYGON_LAYER_ID_PREFIX"
private const val POLYGON_SOURCE_ID_PREFIX = "POLYGON_SOURCE_ID_PREFIX"
private const val POLYGON_BORDER_LAYER_ID_PREFIX = "POLYGON_BORDER_LAYER_ID_PREFIX"
private const val POLYGON_BORDER_SOURCE_ID_PREFIX = "POLYGON_BORDER_SOURCE_ID_PREFIX"

private const val POLYLINE_LAYER_ID_PREFIX = "POLYLINE_LAYER_ID_PREFIX"
private const val POLYLINE_SOURCE_ID_PREFIX = "POLYLINE_SOURCE_ID_PREFIX"

private const val INFO_WINDOW_ID_POSTFIX = "_INFO_WINDOW"

private var markerIconIndex = 0
    get() = ++field
private var markerLayerIndex = 0
    get() = ++field
private var markerSourceIndex = 0
    get() = ++field

private var circleSourceIndex = 0
    get() = ++field
private var circleLayerIndex = 0
    get() = ++field
private var circleBorderLayerIndex = 0
    get() = ++field
private var circleBorderSourceIndex = 0
    get() = ++field

private var polygonLayerIndex = 0
    get() = ++field
private var polygonSourceIndex = 0
    get() = ++field
private var polygonBorderLayerIndex = 0
    get() = ++field
private var polygonBorderSourceIndex = 0
    get() = ++field

private var polylineLayerIndex = 0
    get() = ++field
private var polylineSourceIndex = 0
    get() = ++field

private var featureIdIndex = 0
    get() = ++field

val markerIconIdAndIncrement get() = "$MARKER_ICON_ID_PREFIX-$markerIconIndex"
val markerLayerIdAndIncrement get() = "$MARKER_LAYER_ID_PREFIX-$markerLayerIndex"
val markerSourceIdAndIncrement get() = "$MARKER_SOURCE_ID_PREFIX-$markerSourceIndex"

val circleLayerIdAndIncrement get() = "$CIRCLE_LAYER_ID_PREFIX-$circleLayerIndex"
val circleSourceIdAndIncrement get() = "$CIRCLE_SOURCE_ID_PREFIX-$circleSourceIndex"
val circleBorderLayerIdAndIncrement get() = "$CIRCLE_BORDER_LAYER_ID_PREFIX-$circleBorderLayerIndex"
val circleBorderSourceIdAndIncrement get() = "$CIRCLE_BORDER_SOURCE_ID_PREFIX-$circleBorderSourceIndex"

val polygonLayerIdAndIncrement get() = "$POLYGON_LAYER_ID_PREFIX-$polygonLayerIndex"
val polygonSourceIdAndIncrement get() = "$POLYGON_SOURCE_ID_PREFIX-$polygonSourceIndex"
val polygonBorderLayerIdAndIncrement get() = "$POLYGON_BORDER_LAYER_ID_PREFIX-$polygonBorderLayerIndex"
val polygonBorderSourceIdAndIncrement get() = "$POLYGON_BORDER_SOURCE_ID_PREFIX-$polygonBorderSourceIndex"

val polylineLayerIdAndIncrement get() = "$POLYLINE_LAYER_ID_PREFIX-$polylineLayerIndex"
val polylineSourceIdAndIncrement get() = "$POLYLINE_SOURCE_ID_PREFIX-$polylineSourceIndex"

val sourceFeatureIdAndIncrement get() = "$FEATURE_ID_PREFIX-$featureIdIndex"

fun getInfoWindowIconId(markerIconId: String) = markerIconId + INFO_WINDOW_ID_POSTFIX

fun getInfoWindowLayerId(markerLayerId: String) = markerLayerId + INFO_WINDOW_ID_POSTFIX

fun fromDJIBitmapDescriptor(context: Context, descriptor: DJIBitmapDescriptor?): Bitmap? {
    return when (descriptor?.type) {
        Type.BITMAP -> descriptor.bitmap
        Type.RESOURCE_ID -> BitmapUtils.getBitmapFromDrawable(context.resources.getDrawable(descriptor.resourceId))
        else -> null
    }
}

fun fromDJILatLng(latLng: DJILatLng): LatLng = LatLng(latLng.latitude, latLng.longitude, latLng.altitude)

fun fromLatLng(latLng: LatLng): DJILatLng = DJILatLng(latLng.latitude, latLng.longitude, latLng.altitude)

fun fromCameraPosition(cameraPosition: CameraPosition): DJICameraPosition = DJICameraPosition.Builder()
        .target(fromLatLng(cameraPosition.target))
        .zoom(cameraPosition.zoom.toFloat())
        .tilt(cameraPosition.tilt.toFloat())
        .bearing(cameraPosition.bearing.toFloat())
        .build()

fun fromDJICameraUpdate(cameraUpdate: DJICameraUpdate, cameraPosition: CameraPosition): CameraUpdate {
    when (cameraUpdate) {
        is DJICameraUpdateFactory.CameraBoundsUpdate -> {
            val northeast = cameraUpdate.bounds.northeast
            val southwest = cameraUpdate.bounds.southwest
            return if (northeast == southwest) CameraUpdateFactory.newLatLng(fromDJILatLng(northeast))
            else CameraUpdateFactory.newLatLngBounds(
                    LatLngBounds.Builder().includes(listOf(fromDJILatLng(northeast), fromDJILatLng(southwest))).build(),
                    cameraUpdate.paddingLeft + cameraUpdate.padding,
                    cameraUpdate.paddingTop + cameraUpdate.padding,
                    cameraUpdate.paddingRight + cameraUpdate.padding,
                    cameraUpdate.paddingBottom + cameraUpdate.padding)
        }
        is DJICameraUpdateFactory.CameraPositionUpdate -> {
            val target = if (cameraUpdate.target.isAvailable) fromDJILatLng(cameraUpdate.target) else cameraPosition.target
            val bearing = if (cameraUpdate.bearing.isNaN()) cameraPosition.bearing else cameraUpdate.bearing.toDouble()
            val tilt = if (cameraUpdate.tilt.isNaN()) cameraPosition.tilt else cameraUpdate.tilt.toDouble()
            val zoom = if (cameraUpdate.zoom.isNaN()) cameraPosition.zoom else cameraUpdate.zoom.toDouble()
            return CameraUpdateFactory.newCameraPosition(CameraPosition.Builder().target(target).bearing(bearing).tilt(tilt).zoom(zoom).build())
        }
        else -> return CameraUpdateFactory.newLatLng(LatLng(0.0, 0.0))
    }
}

fun fromMapType(mapType: DJIMap.MapType): String {
    return when (mapType) {
        DJIMap.MapType.NORMAL -> Style.MAPBOX_STREETS
        DJIMap.MapType.HYBRID -> Style.SATELLITE_STREETS
        DJIMap.MapType.SATELLITE -> Style.SATELLITE
        else -> throw IllegalArgumentException("$mapType is not implemented")
    }
}