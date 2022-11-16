package dji.v5.ux.mapkit.amap.annotations;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Polygon;
import dji.v5.ux.mapkit.amap.utils.AMapUtils;
import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.core.models.annotations.DJIPolygon;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joeyang on 6/16/17.
 */
public class APolygon implements DJIPolygon {
    private Polygon mPolygon;

    public APolygon(@NonNull Polygon mPolygon) {
        this.mPolygon = mPolygon;
    }

    @Override
    public void remove() {
        mPolygon.remove();
    }

    @Override
    public boolean isVisible() {
        return mPolygon.isVisible();
    }

    @Override
    public void setVisible(boolean visible) {
        mPolygon.setVisible(visible);
    }

    @Override
    public void setPoints(List<DJILatLng> points) {
        List<LatLng> aPoints = new ArrayList<>(points.size());
        for (DJILatLng latLng : points) {
            aPoints.add(AMapUtils.fromDJILatLng(latLng));
        }
        mPolygon.setPoints(aPoints);
    }

    @Override
    public List<DJILatLng> getPoints() {
        List<DJILatLng> djiPoints = new ArrayList<>(mPolygon.getPoints().size());
        for (LatLng latLng : mPolygon.getPoints()) {
            djiPoints.add(AMapUtils.fromLatLng(latLng));
        }
        return djiPoints;
    }

    @Override
    public void setFillColor(@ColorInt int color) {
        mPolygon.setFillColor(color);
    }

    @Override
    public int getFillColor() {
        return mPolygon.getFillColor();
    }

    @Override
    public void setStrokeColor(@ColorInt int color) {
        mPolygon.setStrokeColor(color);
    }

    @Override
    public int getStrokeColor() {
        return mPolygon.getStrokeColor();
    }

    @Override
    public void setStrokeWidth(float strokeWidth) {
        mPolygon.setStrokeWidth(strokeWidth);
    }

    @Override
    public float getStrokeWidth() {
        return mPolygon.getStrokeWidth();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        APolygon aPolygon = (APolygon) o;

        return mPolygon.equals(aPolygon.mPolygon);

    }

    @Override
    public int hashCode() {
        return mPolygon.hashCode();
    }
}
