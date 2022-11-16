package dji.v5.ux.mapkit.maplibre.map

import android.content.Context
import dji.v5.ux.mapkit.core.Mapkit
import dji.v5.ux.mapkit.core.maps.DJIMap
import dji.v5.ux.mapkit.core.maps.DJIMapView
import dji.v5.ux.mapkit.core.maps.DJIMapViewInternal
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMapOptions
import com.mapbox.mapboxsdk.maps.Style

class MaplibreMapView @JvmOverloads constructor(
        context: Context,
        options: MapboxMapOptions = MapboxMapOptions.createFromAttributes(context)
) : MapView(context, options), DJIMapViewInternal {

    override fun getDJIMapAsync(callback: DJIMapView.OnDJIMapReadyCallback?) {
        getMapAsync { mapboxMap ->
            var styleLoaded = false
            // 按照官方文档中描述 设置style完成后回调 ，但是如果立即addMarker addPolylne等操作是mapbox获取的style是null 所以不会成功的，需要延时回调onDJIMapReady 或者addMarker addPolylne 时返回null
            mapboxMap.setStyle(getMapboxStyle()) {
                styleLoaded = true
            }

            var mapLoadedOnce = false
            val finishLoadingMapListener = OnDidFinishLoadingMapListener {
                mapLoadedOnce = true
                callback?.onDJIMapReady(MaplibreMapDelegateKt(mapboxMap, context, this))
            }
            val startLoadingMapListener = OnWillStartLoadingMapListener {
                if (mapLoadedOnce && styleLoaded) {
                    removeOnDidFinishLoadingMapListener(finishLoadingMapListener)
                }
            }
            addOnWillStartLoadingMapListener(startLoadingMapListener)
            addOnDidFinishLoadingMapListener(finishLoadingMapListener)
        }
    }

    private fun getMapboxStyle(): String {
        return when (Mapkit.getMapType()) {
            DJIMap.MAP_TYPE_NORMAL -> Style.MAPBOX_STREETS
            DJIMap.MAP_TYPE_HYBRID -> Style.SATELLITE_STREETS
            DJIMap.MAP_TYPE_SATELLITE -> Style.SATELLITE
            else -> Style.MAPBOX_STREETS
        }
    }
}