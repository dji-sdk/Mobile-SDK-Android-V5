package dji.v5.ux.mapkit.core.maps;

import android.view.View;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.v5.ux.mapkit.core.callback.MapScreenShotListener;
import dji.v5.ux.mapkit.core.callback.OnCameraChangeListener;
import dji.v5.ux.mapkit.core.callback.OnMapTypeLoadedListener;
import dji.v5.ux.mapkit.core.camera.DJICameraUpdate;
import dji.v5.ux.mapkit.core.maps.DJIProjection;
import dji.v5.ux.mapkit.core.maps.DJIUiSettings;
import dji.v5.ux.mapkit.core.models.DJICameraPosition;
import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.core.models.annotations.DJICircle;
import dji.v5.ux.mapkit.core.models.annotations.DJICircleOptions;
import dji.v5.ux.mapkit.core.models.annotations.DJIGroupCircle;
import dji.v5.ux.mapkit.core.models.annotations.DJIGroupCircleOptions;
import dji.v5.ux.mapkit.core.models.annotations.DJIMarker;
import dji.v5.ux.mapkit.core.models.annotations.DJIMarkerOptions;
import dji.v5.ux.mapkit.core.models.annotations.DJIPolygon;
import dji.v5.ux.mapkit.core.models.annotations.DJIPolygonOptions;
import dji.v5.ux.mapkit.core.models.annotations.DJIPolyline;
import dji.v5.ux.mapkit.core.models.annotations.DJIPolylineOptions;

//Doc key: DJIMap
/**
 *  The public interface of map object.
 */
public interface DJIMap {
    int MAP_TYPE_NONE = 0;
    int MAP_TYPE_NORMAL = 1;
    int MAP_TYPE_SATELLITE = 2;
    int MAP_TYPE_NIGHT = 3;
    int MAP_TYPE_HYBRID = 4;

    //Doc key: DJIMap_MapType
    /**
     *  The overall representation of the map.
     */
    enum MapType {

        //Doc key: DJIMap_MapType_Normal
        /**
         *  The overall representation of the map.
         */
        NORMAL(1),

        //Doc key: DJIMap_MapType_Satellite
        /**
         *  Satellite photograph data.
         */
        SATELLITE(2),

        //Doc key: DJIMap_MapType_Hybrid
        /**
         *  Satellite photograph data and roads
         */
        HYBRID(4);

        int value;

        MapType(int value) {
            this.value = value;
        }

        private static MapType[] mValues;
        public static MapType[] getValues() {
            if (mValues == null) {
                mValues = values();
            }
            return mValues;
        }

        @NonNull
        public static MapType find(@IntRange(from = 1, to = 4) int index) {
            for (MapType mapProvider : MapType.getValues()) {
                if (mapProvider.getValue() == index) {
                    return mapProvider;
                }
            }
            return NORMAL;
        }

        public int getValue() {
            return value;
        }
    }

    //Doc key: DJIMap_addMarker
    /**
     *  Adds a marker to this map.
     *
     *  @param markerOptions Describes how to create the marker.
     *  @return The marker that was added to the map.
     */
    DJIMarker addMarker(DJIMarkerOptions markerOptions);

    //Doc key: DJIMap_getMap
    /**
     *  Gets the base Map Object. This will enable all the features available by default
     *  in each Map Provider
     *
     *  @return The DJIMap object.
     */
    Object getMap();

    //Doc key: DJIMap_animateCamera
    /**
     * Moves the camera position with default animation.
     *
     * @param cameraUpdate The change to apply to the camera.
     */
    void animateCamera(DJICameraUpdate cameraUpdate);

    //Doc key: DJIMap_setOnCameraChangeListener
    /**
     *  Sets the `OnCameraChangeListener` which can listen to camera changes.
     *
     *  @param listener The listener that is added to this map.
     */
    void setOnCameraChangeListener(final OnCameraChangeListener listener);

    //Doc key: DJIMap_removeAllOnCameraChangeListeners
    /**
     *  Removes all instances of `OnCameraChangeListener` from the map.
     */
    void removeAllOnCameraChangeListeners();

    //Doc key: DJIMap_getCameraPosition
    /**
     *  Gets the camera position.
     *
     *  @return The `DJICameraPosition` object.
     */
    DJICameraPosition getCameraPosition();

    //Doc key: DJIMap_moveCamera
    /**
     * Moves the camera position.
     *
     * @param cameraUpdate The camera position.
     */
    void moveCamera(DJICameraUpdate cameraUpdate);

    /**
     * {@hide}
     * set a adapter {@link InfoWindowAdapter}
     * @param adapter
     */
    void setInfoWindowAdapter(InfoWindowAdapter adapter);

    //Doc key: DJIMap_setMapType
    /**
     *  Sets the map type of this map.
     *
     *  @param type The map type that the map will be set to.
     */
    void setMapType(MapType type);

    //Doc key: DJIMap_setMapType_withListener
    /**
     *  Sets the map type of this map with an `OnMapTypeLoadedListener` which can listen to the
     *  event when the map type is finished loading.
     *
     *  @param type The map type that the map will be set to.
     *  @param listener The listener that is added to this map.
     */
    void setMapType(MapType type, OnMapTypeLoadedListener listener);

    /**
     * {@hide}
     * @param type
     */
    void setMapType(int type);

    //Doc key: DJIMap_addPolyline
    /**
     * Adds a polyline.
     *
     * @param options The polyline options.
     * @return The `DJIPolyline` that was added to the map.
     */
    DJIPolyline addPolyline(DJIPolylineOptions options);

    //Doc key: DJIMap_addPolygon
    /**
     * Adds a polygon.
     *
     * @param options The polygon options.
     * @return The `DJIPolygon` that was added to the map. Returns null if the polygon is
     * invalid.
     */
    @Nullable
    DJIPolygon addPolygon(DJIPolygonOptions options);

    //Doc key: DJIMap_addSingleCircle
    /**
     * Adds a circle.
     *
     * @param options The circle options.
     * @return The `DJICircle` that was added to the map. Returns null if the circle is
     * invalid.
     */
    @Nullable
    DJICircle addSingleCircle(DJICircleOptions options);

    /**
     * {@hide}
     * Adds a markerCircle {@link DJICircle}. Only supported by Mapbox.
     * @param options {@link DJICircleOptions}
     * @return {@link DJICircle}
     */
    @Nullable
    DJICircle addMarkerCircle(DJICircleOptions options);

    /**
     * {@hide}
     * Add a group of circles. Only supported by Mapbox and AMaps.
     * @param options {@link DJIGroupCircleOptions}
     * @return {@link DJIGroupCircle}
     */
    @Nullable
    DJIGroupCircle addGroupCircle(DJIGroupCircleOptions options);

    /**
     * {@hide}
     * InfoWindow
     */
    interface InfoWindowAdapter {
        View getInfoWindow(DJIMarker marker);
        View getInfoContents(DJIMarker marker);
    }

    //Doc key: DJIMap_OnMarkerClickListenerInterface
    /**
     *  Listener on the marker click event.
     */
    interface OnMarkerClickListener {

        //Doc key: DJIMap_onMarkerClickCallback
        /**
         *  A callback indicating that a marker on the map has been clicked.
         *
         *  @param marker An object of `DJIMarker`.
         *
         *  @return A boolean that indicates whether you have consumed the event (i.e.,  you want to suppress the default behavior). If it returns `false`, then  the default behavior will occur in addition to your custom behavior.
         */
        boolean onMarkerClick(DJIMarker marker);
    }

    //Doc key: DJIMap_OnMapClickListenerInterface
    /**
     *  Listener on the map click event.
     */
    interface OnMapClickListener {

        //Doc key: DJIMap_onMapClickCallback
        /**
         *  A callback indicating that the map has been clicked.
         *
         *  @param latLng The point on the map that was clicked.
         */
        void onMapClick(DJILatLng latLng);
    }

    /**
     * {@hide}
     * Listener on InfoWindow click event
     */
    interface OnInfoWindowClickListener {
        void onInfoWindowClick(DJIMarker marker);
    }

    /**
     * {@hide}
     * Map长按事件listener
     */
    interface OnMapLongClickListener {
        void onMapLongClick(DJILatLng latLng);
    }

    //Doc Key: DJIMap_OnMarkerDragListenerInterface
    /**
     *  Listener on the marker drag event.
     */
    interface OnMarkerDragListener {
        //Doc Key: DJIMap_onMarkerDragStartCallback
        /**
         *  A callback indicating that a marker drag has started.
         *
         *  @param marker The marker that is being dragged.
         */
        void onMarkerDragStart(DJIMarker marker);

        //Doc Key: DJIMap_onMarkerDragCallback
        /**
         *  A callback indicating that a marker drag is in progress.
         *
         *  @param marker The marker that is being dragged.
         */
        void onMarkerDrag(DJIMarker marker);

        //Doc Key: DJIMap_onMarkerDragEndCallback
        /**
         *  A callback indicating that a marker drag has ended.
         *
         *  @param marker The marker that is being dragged.
         */
        void onMarkerDragEnd(DJIMarker marker);
    }

    //Doc key: DJIMap_getUiSettings
    /**
     * Gets the UI Settings.
     *
     * @return The `DJIUiSettings` object.
     */
    DJIUiSettings getUiSettings();

    /**
     * {@hide}
     * 移除已设置的{@link OnCameraChangeListener}
     * @param listener
     */
    void removeOnCameraChangeListener(final OnCameraChangeListener listener);

    //Doc key: DJIMap_removeOnMarkerClickListener
    /**
     *  Removes the `OnMarkerClickListenerInterface` from the map.
     *
     *  @param listener The listener to remove.
     */
    void removeOnMarkerClickListener(OnMarkerClickListener listener);

    /**
     * {@hide}
     * 移除已经设置的{@linkplain OnMarkerClickListener}
     */
    void removeAllOnMarkerClickListener();

    /**
     * {@hide}
     * 设置InfoWindow的点击事件
     * @param listener
     */
    void setOnInfoWindowClickListener(OnInfoWindowClickListener listener);

    //Doc Key: DJIMap_setOnMarkerClickListener
    /**
     *  Sets the `OnMarkerClickListenerInterface` which can listen to click events on the
     *  map's markers.
     *
     *  @param listener The listener that is added to this map.
     */
    void setOnMarkerClickListener(OnMarkerClickListener listener);

    //Doc Key: DJIMap_setOnMapClickListener
    /**
     *  Sets the `OnMapClickListenerInterface`.
     *
     *  @param listener The listener that is added to this map.
     */
    void setOnMapClickListener(OnMapClickListener listener);

    //Doc key: DJIMap_removeOnMapClickListener
    /**
     *  Removes the `OnMapClickListenerInterface` from the map.
     *
     *  @param listener The listener to remove.
     */
    void removeOnMapClickListener(OnMapClickListener listener);

    /**
     * {@hide}
     * screenshot
     * 设置Map的长按事件
     * @param listener
     */
    void setOnMapLongClickListener(OnMapLongClickListener listener);

    /**
     * {@hide}
     * 移除所有点击事件
     */
    void removeAllOnMapClickListener();

    /**
     * {@hide}
     * 移除单个长按事件
     */
    void removeOnMapLongClickListener(OnMapLongClickListener listener);

    /**
     * {@hide}
     * 移除所有长按事件
     */
    void removeAllOnMapLongClickListener();

    //Doc Key: DJIMap_setOnMarkerDragListener
    /**
     *  Sets the map's `OnMarkerDragListenerInterface`.
     *
     *  @param listener The listener that is added to this map.
     */
    void setOnMarkerDragListener(OnMarkerDragListener listener);

    //Doc Key: DJIMap_removeOnMarkerDragListener
    /**
     *  Removes the `OnMarkerDragListenerInterface` from the map.
     *
     *  @param listener The listener to remove.
     */
    void removeOnMarkerDragListener(OnMarkerDragListener listener);

    //Doc Key: DJIMap_removeAllOnMarkerDragListener
    /**
     *  Removes all instances of `OnMarkerDragListenerInterface` from the map.
     */
    void removeAllOnMarkerDragListener();

    /**
     * {@hide}
     * screenshot
     * @param callback
     */
    void snapshot(MapScreenShotListener callback);

    /**
     * {@hide}
     * Gets projection
     * @return
     */
    DJIProjection getProjection();

    /**
     * {@hide}
     * Releases the resources on map
     */
    void clear();
}
