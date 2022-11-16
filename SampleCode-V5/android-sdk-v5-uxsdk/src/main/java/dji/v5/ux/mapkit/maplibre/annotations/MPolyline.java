package dji.v5.ux.mapkit.maplibre.annotations;

import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.core.models.annotations.DJIPolyline;
import dji.v5.ux.mapkit.core.models.annotations.DJIPolylineOptions;
import dji.v5.ux.mapkit.maplibre.utils.MaplibreUtils;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joeyang on 11/2/17.
 * Mapbox的Polyline代理类
 */
public class MPolyline implements DJIPolyline {

    Polyline polyline;
    DJIPolylineOptions mDJIPolylineOptions;
    MapboxMap mapboxMap;

    public MPolyline(Polyline polyline, DJIPolylineOptions options, MapboxMap mapboxMap) {
        this.polyline = polyline;
        this.mDJIPolylineOptions = options;
        this.mapboxMap = mapboxMap;
    }

    @Override
    public void remove() {
        mapboxMap.removePolyline(polyline);
    }

    @Override
    public void setWidth(float width) {
        polyline.setWidth(width / 5f);
    }

    @Override
    public float getWidth() {
        return polyline.getWidth();
    }

    @Override
    public void setPoints(List<DJILatLng> points) {
        List<LatLng> mPoints = new ArrayList<>(points.size());
        for (DJILatLng latLng : points) {
            mPoints.add(MaplibreUtils.fromDJILatLng(latLng));
        }
        polyline.setPoints(mPoints);
    }

    @Override
    public List<DJILatLng> getPoints() {
        List<DJILatLng> djiPoints = new ArrayList<>(polyline.getPoints().size());
        for (LatLng latLng : polyline.getPoints()) {
            djiPoints.add(MaplibreUtils.fromLatLng(latLng));
        }
        return djiPoints;
    }

    @Override
    public void setColor(int color) {
        polyline.setColor(color);
    }

    @Override
    public int getColor() {
        return polyline.getColor();
    }

    @Override
    public void setZIndex(float zIndex) {
        //  11/2/17 Mapbox没有这个，空实现
    }

    @Override
    public float getZIndex() {
        return 0;
    }

    public DJIPolylineOptions getOptions() {
        return mDJIPolylineOptions;
    }

    /**
     * 通过 polylineOptions 来更新 polyline
     * @param options
     */
    public void setOptions(DJIPolylineOptions options) {
        List<DJILatLng> latLngs = options.getPoints();
        List<LatLng> points = new ArrayList<>(latLngs.size());
        for (DJILatLng latLng : latLngs) {
            points.add(MaplibreUtils.fromDJILatLng(latLng));
        }
        polyline.setColor(options.getColor());
        polyline.setWidth(options.getWidth() / 5f);
        polyline.setPoints(points);
    }
}
