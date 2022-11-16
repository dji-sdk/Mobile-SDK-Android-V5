package dji.v5.ux.mapkit.gmap.map;

import android.graphics.Bitmap;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.v5.ux.mapkit.core.callback.MapScreenShotListener;
import dji.v5.ux.mapkit.core.callback.OnCameraChangeListener;
import dji.v5.ux.mapkit.core.callback.OnMapTypeLoadedListener;
import dji.v5.ux.mapkit.core.camera.DJICameraUpdate;
import dji.v5.ux.mapkit.core.maps.DJIBaseMap;
import dji.v5.ux.mapkit.core.maps.DJIMap;
import dji.v5.ux.mapkit.core.maps.DJIProjection;
import dji.v5.ux.mapkit.core.maps.DJIUiSettings;
import dji.v5.ux.mapkit.core.models.DJIBitmapDescriptor;
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
import dji.v5.ux.mapkit.gmap.annotations.GCircle;
import dji.v5.ux.mapkit.gmap.annotations.GMarker;
import dji.v5.ux.mapkit.gmap.annotations.GPolygon;
import dji.v5.ux.mapkit.gmap.annotations.GPolyline;
import dji.v5.ux.mapkit.gmap.utils.GoogleUtils;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;

import java.util.HashMap;

/**
 * Created by joeyang on 5/27/17.
 * GoogleMap的代理类
 */
public class GMapDelegate extends DJIBaseMap implements DJIMap, GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerDragListener,
        GoogleMap.OnInfoWindowClickListener {
   // private static final String TAG = GMapDelegate.class.getSimpleName();

    private GoogleMap mMap;

    private HashMap<Marker, GMarker> markerMap;

    public GMapDelegate(GoogleMap map) {
        mMap = map;
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnMarkerDragListener(this);
        mMap.setOnInfoWindowClickListener(this);

        markerMap = new HashMap<>();
        mMap.setOnCameraMoveListener(() -> {
            CameraPosition p = GMapDelegate.this.mMap.getCameraPosition();
            DJICameraPosition cameraPosition = GoogleUtils.fromCameraPosition(p);
            onCameraChange(cameraPosition);
        });
    }

    @NonNull
    @Override
    public DJIMarker addMarker(DJIMarkerOptions markerOptions) {
        DJILatLng latLng = markerOptions.getPosition();
        if (latLng == null) {
            throw new IllegalArgumentException("DJIMarkerOptions parameter must have position set");
        }

        MarkerOptions options = new MarkerOptions();
        DJILatLng position = markerOptions.getPosition();
        DJIBitmapDescriptor icon = markerOptions.getIcon();

        options.draggable(markerOptions.getDraggable())
                .position(GoogleUtils.fromDJILatLng(position))
                .anchor(markerOptions.getAnchorU(), markerOptions.getAnchorV())
                .rotation(markerOptions.getRotation())
                .zIndex(markerOptions.getZIndex())
                .visible(markerOptions.getVisible())
                .title(markerOptions.getTitle())
                .flat(markerOptions.isFlat());
        if (icon != null) {
            options.icon(GoogleUtils.fromDJIBitmapDescriptor(icon));
        }
        Marker marker = mMap.addMarker(options);
        GMarker gMarker = new GMarker(marker, this);
        gMarker.setPositionCache(markerOptions.getPosition());
        markerMap.put(marker, gMarker);
        return gMarker;
    }

    @Override
    public Object getMap() {
        return mMap;
    }

    @Override
    public DJICameraPosition getCameraPosition() {
        CameraPosition p = mMap.getCameraPosition();
        return GoogleUtils.fromCameraPosition(p);
    }

    @Override
    public void animateCamera(DJICameraUpdate cameraUpdate) {
        CameraUpdate update = GoogleUtils.fromDJICameraUpdate(cameraUpdate);
        mMap.animateCamera(update);
    }

    @Override
    public void moveCamera(@NonNull DJICameraUpdate cameraUpdate) {
        CameraUpdate update = GoogleUtils.fromDJICameraUpdate(cameraUpdate);
        mMap.moveCamera(update);
    }

    @Override
    public void setOnCameraChangeListener(final OnCameraChangeListener listener) {
        if (onCameraChangeListeners.contains(listener)) {
            return;
        }
        onCameraChangeListeners.add(listener);
        mMap.setOnCameraMoveListener(() -> {
            CameraPosition p = mMap.getCameraPosition();
            DJICameraPosition cameraPosition = GoogleUtils.fromCameraPosition(p);
//                DJICameraPosition cameraPosition = DJICameraPosition.from(p.target.latitude,
//                        p.target.longitude, p.zoom, p.tilt, p.bearing);
            for (OnCameraChangeListener listener1 : onCameraChangeListeners) {
                listener1.onCameraChange(cameraPosition);
            }
        });
    }

    @Override
    public void removeAllOnCameraChangeListeners() {
        mMap.setOnCameraMoveListener(null);
        onCameraChangeListeners.clear();
    }

    @Override
    public void setInfoWindowAdapter(final InfoWindowAdapter adapter) {
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return adapter.getInfoWindow(markerMap.get(marker));
            }

            @Override
            public View getInfoContents(Marker marker) {
                return adapter.getInfoContents(markerMap.get(marker));
            }
        });
    }

    @NonNull
    @Override
    public DJIPolyline addPolyline(DJIPolylineOptions options) {
        Polyline polyline = mMap.addPolyline(GoogleUtils.fromDJIPolylineOptions(options));
        return new GPolyline(polyline);
    }

    @NonNull
    @Override
    public DJIPolygon addPolygon(DJIPolygonOptions options) {
        Polygon polygon = mMap.addPolygon(GoogleUtils.fromDJIPolygonOptions(options));
        return new GPolygon(polygon);
    }

    @Override
    public DJICircle addMarkerCircle(DJICircleOptions options) {
        return null;
    }

    @Override
    public DJIGroupCircle addGroupCircle(DJIGroupCircleOptions options) {
        return null;
    }

    @Nullable
    @Override
    public DJICircle addSingleCircle(DJICircleOptions options) {
        if (options.getRadius() <= 0) {
            return null;
        }
        CircleOptions circleOptions = new CircleOptions();
        DJILatLng center = options.getCenter();
        circleOptions.center(GoogleUtils.fromDJILatLng(center))
                .radius(options.getRadius())
                .strokeWidth(options.getStrokeWidth())
                .strokeColor(options.getStrokeColor())
                .fillColor(options.getFillColor());
        Circle circle = mMap.addCircle(circleOptions);
        return new GCircle(circle);
    }

    @Override
    public void setMapType(MapType type, OnMapTypeLoadedListener listener) {
        setMapType(type);
        listener.onMapTypeLoaded();
    }

    @Override
    public void setMapType(int type) {
        switch (type) {
            case DJIMap.MAP_TYPE_NORMAL:
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                break;
            case DJIMap.MAP_TYPE_SATELLITE:
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                break;
            case DJIMap.MAP_TYPE_HYBRID:
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                break;
            default:
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }
    }

    @Override
    public void setMapType(MapType type) {
        switch (type) {
            case NORMAL:
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                break;
            case SATELLITE:
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                break;
            case HYBRID:
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                break;
            default:
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }
    }

    @Override
    public DJIUiSettings getUiSettings() {
        return new GUiSettings(mMap.getUiSettings());
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        boolean result = true;
        // 将Marker封装成DJIMarker，调用DJIMap.OnMarkerClickListener.onMarkerClick(DJIMarker)
        if (markerMap.containsKey(marker)) {
            onMarkerClick(markerMap.get(marker));
        }
        return result;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        DJILatLng djiLatLng = GoogleUtils.fromLatLng(latLng);
        onMapClick(djiLatLng);
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        if (markerMap.containsKey(marker)) {
            onInfoWindowClick(markerMap.get(marker));
        }
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        if (markerMap.containsKey(marker)) {
            onMarkerDragStart(markerMap.get(marker));
        }
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        if (markerMap.containsKey(marker)) {
            DJIMarker djiMarker = markerMap.get(marker);
            LatLng latLng = marker.getPosition();
            DJILatLng djiLatLng = GoogleUtils.fromLatLng(latLng);
            djiMarker.setPosition(djiLatLng);
            onMarkerDrag(djiMarker);
        }
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        if (markerMap.containsKey(marker)) {
            DJIMarker djiMarker = markerMap.get(marker);
            LatLng latLng = marker.getPosition();
            DJILatLng djiLatLng = GoogleUtils.fromLatLng(latLng);
            djiMarker.setPosition(djiLatLng);
            onMarkerDragEnd(djiMarker);
        }
    }

    @Override
    public void snapshot(final MapScreenShotListener callback) {
        mMap.snapshot(bitmap -> callback.onMapScreenShot(bitmap));
    }

    @Override
    public DJIProjection getProjection() {
        return new GProjection(mMap.getProjection());
    }

    @Override
    public void clear() {
        mMap.clear();
    }

    public void onMarkerRemove(Marker marker) {
        if (markerMap.containsKey(marker)) {
            markerMap.remove(marker);
        }
    }

    // :拖动和长按还没实现，需要继续完成

}
