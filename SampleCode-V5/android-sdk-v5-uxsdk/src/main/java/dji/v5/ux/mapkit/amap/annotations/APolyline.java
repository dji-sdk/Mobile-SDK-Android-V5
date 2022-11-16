package dji.v5.ux.mapkit.amap.annotations;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Polyline;
import dji.v5.ux.mapkit.amap.utils.AMapUtils;
import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.core.models.annotations.DJIPolyline;
import dji.v5.ux.mapkit.core.models.annotations.DJIPolylineOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joeyang on 6/11/17.
 */
public class APolyline implements DJIPolyline {

    private Polyline mPolyline;
    private DJIPolylineOptions mDJIPolylineOptions;

    public APolyline(@NonNull Polyline polyline) {
        this.mPolyline = polyline;
    }

    public APolyline(Polyline polyline, DJIPolylineOptions options) {
        mPolyline = polyline;
        mDJIPolylineOptions = options;
    }

    @Override
    public void remove() {
        mPolyline.remove();
    }

    @Override
    public void setWidth(float width) {
        mPolyline.setWidth(width);
    }

    @Override
    public float getWidth() {
        return mPolyline.getWidth();
    }

    @Override
    public void setPoints(List<DJILatLng> points) {
        List<LatLng> aPoints = new ArrayList<>(points.size());
        for (DJILatLng latLng : points) {
            aPoints.add(AMapUtils.fromDJILatLng(latLng));
        }
        mPolyline.setPoints(aPoints);
    }

    @Override
    public List<DJILatLng> getPoints() {
        List<DJILatLng> djiPoints = new ArrayList<>(mPolyline.getPoints().size());
        for (LatLng latLng : mPolyline.getPoints()) {
            djiPoints.add(AMapUtils.fromLatLng(latLng));
        }
        return djiPoints;
    }

    @Override
    public void setZIndex(float zIndex) {
        mPolyline.setZIndex(zIndex);
    }

    @Override
    public float getZIndex() {
        return mPolyline.getZIndex();
    }

    @Override
    public void setColor(@ColorInt int color) {
        mPolyline.setColor(color);
    }

    @ColorInt
    @Override
    public int getColor() {
        return mPolyline.getColor();
    }

    public DJIPolylineOptions getOptions() {
        return mDJIPolylineOptions;
    }

    public void setOptions(DJIPolylineOptions options) {
        mDJIPolylineOptions = options;
        mPolyline.setOptions(AMapUtils.fromDJIPolylineOptions(options));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        APolyline aPolyline = (APolyline) o;

        return mPolyline.equals(aPolyline.mPolyline);

    }

    @Override
    public int hashCode() {
        return mPolyline.hashCode();
    }
}
