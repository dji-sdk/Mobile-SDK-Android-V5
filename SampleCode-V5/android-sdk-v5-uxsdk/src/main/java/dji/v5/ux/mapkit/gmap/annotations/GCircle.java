package dji.v5.ux.mapkit.gmap.annotations;


import androidx.annotation.ColorInt;


import dji.v5.ux.mapkit.core.models.annotations.DJICircle;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;

import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.gmap.utils.GoogleUtils;

/**
 * Created by joeyang on 6/12/17.
 */
public class GCircle implements DJICircle {
    private Circle mCircle;

    public GCircle(Circle circle) {
        mCircle = circle;
    }

    @Override
    public void remove() {
        mCircle.remove();
    }

    @Override
    public void setVisible(boolean visible) {
        mCircle.setVisible(visible);
    }

    @Override
    public boolean isVisible() {
        return mCircle.isVisible();
    }

    @Override
    public void setCenter(DJILatLng center) {
        mCircle.setCenter(GoogleUtils.fromDJILatLng(center));
    }

    @Override
    public DJILatLng getCenter() {
        LatLng center = mCircle.getCenter();
        return GoogleUtils.fromLatLng(center);
    }

    @Override
    public void setRadius(double radius) {
        mCircle.setRadius(radius);
    }

    @Override
    public double getRadius() {
        return mCircle.getRadius();
    }

    @Override
    public void setFillColor(@ColorInt int color) {
        mCircle.setFillColor(color);
    }

    @Override
    public int getFillColor() {
        return mCircle.getFillColor();
    }

    @Override
    public void setStrokeColor(@ColorInt int color) {
        mCircle.setStrokeColor(color);
    }

    @Override
    public int getStrokeColor() {
        return mCircle.getStrokeColor();
    }

    @Override
    public void setZIndex(float zIndex) {
        mCircle.setZIndex(zIndex);
    }

    @Override
    public float getZIndex() {
        return mCircle.getZIndex();
    }

    @Override
    public void setCircle(DJILatLng center, Double radius) {
        mCircle.setCenter(GoogleUtils.fromDJILatLng(center));
        mCircle.setRadius(radius);
    }

    @Override
    public void setStrokeWidth(float strokeWidth) {
        mCircle.setStrokeWidth(strokeWidth);
    }

    @Override
    public float getStrokeWidth() {
        return mCircle.getStrokeWidth();
    }
}
