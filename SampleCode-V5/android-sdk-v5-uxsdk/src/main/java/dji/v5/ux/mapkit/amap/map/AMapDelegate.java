package dji.v5.ux.mapkit.amap.map;

import android.graphics.Bitmap;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Polygon;
import com.amap.api.maps.model.Polyline;
import dji.v5.ux.mapkit.amap.annotations.ACircle;
import dji.v5.ux.mapkit.amap.annotations.AGroupCircle;
import dji.v5.ux.mapkit.amap.annotations.AMarker;
import dji.v5.ux.mapkit.amap.annotations.APolygon;
import dji.v5.ux.mapkit.amap.annotations.APolyline;
import dji.v5.ux.mapkit.amap.map.AProjection;
import dji.v5.ux.mapkit.amap.map.AUiSettings;
import dji.v5.ux.mapkit.amap.utils.AMapUtils;
import dji.v5.ux.mapkit.core.callback.MapScreenShotListener;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by joeyang on 5/29/17.
 * 高德地图的代理类
 */
public class AMapDelegate extends DJIBaseMap implements AMap.OnMarkerClickListener,
        AMap.OnMapClickListener,
        AMap.OnInfoWindowClickListener,
        AMap.OnMarkerDragListener,
        AMap.OnMapLongClickListener, AMap.OnCameraChangeListener {

   // private static final String TAG = AMapDelegate.class.getSimpleName();

    private AMap mMap;

    private Map<Marker, AMarker> markerMap;

    public AMapDelegate(AMap map) {
        mMap = map;
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnCameraChangeListener(this);
        mMap.setOnMarkerDragListener(this);
        markerMap = new HashMap<>();
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
                .position(AMapUtils.fromDJILatLng(position))
                .anchor(markerOptions.getAnchorU(), markerOptions.getAnchorV())
                .rotateAngle(-markerOptions.getRotation())
                .zIndex(markerOptions.getZIndex())
                .visible(markerOptions.getVisible())
                .title(markerOptions.getTitle())
                .infoWindowEnable(markerOptions.isInfoWindowEnable())
                .setFlat(markerOptions.isFlat());
        if (icon != null) {
            options.icon(AMapUtils.fromDJIBitmapDescriptor(icon));
        }
        Marker marker = mMap.addMarker(options);
        AMarker aMarker = new AMarker(marker, this);
        aMarker.setPositionCache(markerOptions.getPosition());
        markerMap.put(marker, aMarker);
        return aMarker;
    }

    @Override
    public Object getMap() {
        return mMap;
    }

    @Override
    public DJICameraPosition getCameraPosition() {
        CameraPosition p = mMap.getCameraPosition();
        return AMapUtils.fromCameraPosition(p);
//        return DJICameraPosition.from(p.target.latitude, p.target.longitude,
//                p.zoom, p.tilt, p.bearing);
    }

    @Override
    public void animateCamera(DJICameraUpdate cameraUpdate) {
//        CameraUpdate update = cameraUpdate.toACameraUpdate();
//        mMap.animateCamera(update, 100, null);
        CameraUpdate update = AMapUtils.fromDJICameraUpdate(cameraUpdate);
        mMap.animateCamera(update);
    }

    @Override
    public void moveCamera(DJICameraUpdate cameraUpdate) {
//        CameraUpdate update = cameraUpdate.toACameraUpdate();
//        mMap.moveCamera(update);
        CameraUpdate update = AMapUtils.fromDJICameraUpdate(cameraUpdate);
        mMap.moveCamera(update);
    }

    @Override
    public void setInfoWindowAdapter(final InfoWindowAdapter adapter) {
        mMap.setInfoWindowAdapter(new AMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return adapter.getInfoWindow(new AMarker(marker, AMapDelegate.this));
            }

            @Override
            public View getInfoContents(Marker marker) {
                return adapter.getInfoContents(new AMarker(marker, AMapDelegate.this));
            }
        });
    }

    @NonNull
    @Override
    public DJIPolyline addPolyline(DJIPolylineOptions options) {
        Polyline polyline = mMap.addPolyline(AMapUtils.fromDJIPolylineOptions(options));
        return new APolyline(polyline, options);
    }

    @NonNull
    @Override
    public DJIPolygon addPolygon(DJIPolygonOptions options) {
        Polygon polygon = mMap.addPolygon(AMapUtils.fromDJIPolygonOptions(options));
        return new APolygon(polygon);
    }

    @Nullable
    @Override
    public DJICircle addSingleCircle(DJICircleOptions options) {
        if (options.getRadius() <= 0) {
            return null;
        }
        CircleOptions circleOptions = new CircleOptions();
        DJILatLng center = options.getCenter();
        circleOptions.center(AMapUtils.fromDJILatLng(center))
                .radius(options.getRadius())
                .strokeWidth(options.getStrokeWidth())
                .strokeColor(options.getStrokeColor())
                .fillColor(options.getFillColor());
        Circle circle = mMap.addCircle(circleOptions);
        return new ACircle(circle);
    }

    @Nullable
    @Override
    public DJICircle addMarkerCircle(DJICircleOptions options) {
        //TODO 高德安卓api未提供相应方法，暂时只在js中有该方法。
        return null;
    }

    @Nullable
    @Override
    public DJIGroupCircle addGroupCircle(DJIGroupCircleOptions options) {
        if (options.getRadius().size() != options.getCenters().size() || options.getRadius().size() == 0) {
            return null;
        }

        int size = options.getCenters().size();
        List<Circle> circles = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            CircleOptions circleOptions = new CircleOptions();
            DJILatLng center = options.getCenters().get(i);
            Double radius = options.getRadius().get(i);
            circleOptions.center(AMapUtils.fromDJILatLng(center))
                    .radius(radius)
                    .strokeWidth(options.getStrokeWidth())
                    .strokeColor(options.getStrokeColor())
                    .fillColor(options.getFillColor());
            Circle circle = mMap.addCircle(circleOptions);
            circles.add(circle);
        }
        return new AGroupCircle(circles);
    }

    @Override
    public void setMapType(int type) {
        switch (type) {
            case DJIMap.MAP_TYPE_NORMAL:
                mMap.setMapType(AMap.MAP_TYPE_NORMAL);
                break;
            case DJIMap.MAP_TYPE_SATELLITE:
                mMap.setMapType(AMap.MAP_TYPE_NIGHT);
                break;
            case DJIMap.MAP_TYPE_HYBRID:
                mMap.setMapType(AMap.MAP_TYPE_SATELLITE);
                break;
            default:
                mMap.setMapType(AMap.MAP_TYPE_NORMAL);
        }
    }

    @Override
    public void setMapType(MapType type, OnMapTypeLoadedListener listener) {
        setMapType(type);
        listener.onMapTypeLoaded();
    }

    @Override
    public void setMapType(MapType type) {
        switch (type) {
            case NORMAL:
                mMap.setMapType(AMap.MAP_TYPE_NORMAL);
                break;
            case SATELLITE:
                mMap.setMapType(AMap.MAP_TYPE_NIGHT);
                break;
            case HYBRID:
                mMap.setMapType(AMap.MAP_TYPE_SATELLITE);
                break;
            default:
                mMap.setMapType(AMap.MAP_TYPE_NORMAL);
        }
    }

    @Override
    public DJIUiSettings getUiSettings() {
        return new AUiSettings(mMap.getUiSettings());
    }

    /**********************************************************************************************/
    @Override
    public boolean onMarkerClick(Marker marker) {
        if (markerMap.containsKey(marker)) {
            onMarkerClick(markerMap.get(marker));
        }
        return true;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        DJILatLng djiLatLng = AMapUtils.fromLatLng(latLng);
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
            DJILatLng djiLatLng = AMapUtils.fromLatLng(latLng);
            djiMarker.setPosition(djiLatLng);
            onMarkerDrag(djiMarker);
        }
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        if (markerMap.containsKey(marker)) {
            DJIMarker djiMarker = markerMap.get(marker);
            LatLng latLng = marker.getPosition();
            DJILatLng djiLatLng = AMapUtils.fromLatLng(latLng);
            djiMarker.setPosition(djiLatLng);
            onMarkerDragEnd(djiMarker);
        }
    }

    @Override
    public void snapshot(final MapScreenShotListener callback) {
        mMap.getMapScreenShot(new AMap.OnMapScreenShotListener() {
            @Override
            public void onMapScreenShot(Bitmap bitmap) {
                callback.onMapScreenShot(bitmap);
            }

            @Override
            public void onMapScreenShot(Bitmap bitmap, int i) {
                //do something
            }
        });
    }

    @Override
    public DJIProjection getProjection() {
        return new AProjection(mMap.getProjection());
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

    @Override
    public void onMapLongClick(LatLng latLng) {
        DJILatLng djiLatLng = AMapUtils.fromLatLng(latLng);
        onMapLongClick(djiLatLng);
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        DJICameraPosition p = AMapUtils.fromCameraPosition(cameraPosition);
        AMapDelegate.this.onCameraChange(p);
    }

    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {
        DJICameraPosition p = AMapUtils.fromCameraPosition(cameraPosition);
        AMapDelegate.this.onCameraChangeFinish(p);
    }

}
