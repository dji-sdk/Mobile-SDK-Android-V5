package dji.v5.ux.mapkit.gmap.annotations;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.core.models.annotations.DJIPolygon;
import dji.v5.ux.mapkit.gmap.utils.GoogleUtils;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joeyang on 6/16/17.
 */
public class GPolygon implements DJIPolygon {
    private Polygon mPolygon;

    public GPolygon(@NonNull Polygon mPolygon) {
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
        ArrayList<LatLng> gPoints = new ArrayList<>(points.size());
        for (DJILatLng latLng : points) {
            gPoints.add(GoogleUtils.fromDJILatLng(latLng));
        }
        mPolygon.setPoints(gPoints);
    }

    @Override
    public List<DJILatLng> getPoints() {
        List<DJILatLng> djiPoints = new ArrayList<>(mPolygon.getPoints().size());
        for (LatLng latLng : mPolygon.getPoints()) {
            djiPoints.add(GoogleUtils.fromLatLng(latLng));
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

        GPolygon gPolygon = (GPolygon) o;

        return mPolygon.equals(gPolygon.mPolygon);

    }

    @Override
    public int hashCode() {
        return mPolygon.hashCode();
    }
}
