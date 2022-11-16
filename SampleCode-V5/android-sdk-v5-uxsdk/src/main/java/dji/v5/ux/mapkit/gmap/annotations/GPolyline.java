package dji.v5.ux.mapkit.gmap.annotations;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.core.models.annotations.DJIPolyline;
import dji.v5.ux.mapkit.core.models.annotations.DJIPolylineOptions;
import dji.v5.ux.mapkit.gmap.utils.GoogleUtils;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by joeyang on 6/11/17.
 */
public class GPolyline implements DJIPolyline {
    private Polyline mPolyline;

    public GPolyline(@NonNull Polyline polyline) {
        this.mPolyline = polyline;
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
        ArrayList<LatLng> gPoints = new ArrayList<>(points.size());
        for (DJILatLng latLng : points) {
            gPoints.add(GoogleUtils.fromDJILatLng(latLng));
        }
        mPolyline.setPoints(gPoints);
    }

    @Override
    public List<DJILatLng> getPoints() {
        List<DJILatLng> djiPoints = new ArrayList<>(mPolyline.getPoints().size());
        for (LatLng latLng : mPolyline.getPoints()) {
            djiPoints.add(GoogleUtils.fromLatLng(latLng));
        }
        return djiPoints;
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
        //  6/11/17  事实上目前并没有用到，主要是用于在DJIMapManager.drawAirPoints的增量画中
        DJIPolylineOptions options = new DJIPolylineOptions();
        return options;
    }

    @Override
    public void setZIndex(float zIndex) {
        mPolyline.setZIndex(zIndex);
    }

    @Override
    public float getZIndex() {
        return mPolyline.getZIndex();
    }

    //  11/6/17 这里setOptions只改变了points，事实上，可能其他的也改变了这里没有实现
    public void setOptions(DJIPolylineOptions options) {
        // Google Map 没有 setOptions 方法
        List<DJILatLng> latLngs = options.getPoints();
        List<LatLng> points = new ArrayList<>(latLngs.size());
        for (DJILatLng latLng : latLngs) {
            points.add(GoogleUtils.fromDJILatLng(latLng));
        }
        mPolyline.setPoints(points);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GPolyline gPolyline = (GPolyline) o;

        return mPolyline.equals(gPolyline.mPolyline);

    }

    @Override
    public int hashCode() {
        return mPolyline.hashCode();
    }
}
