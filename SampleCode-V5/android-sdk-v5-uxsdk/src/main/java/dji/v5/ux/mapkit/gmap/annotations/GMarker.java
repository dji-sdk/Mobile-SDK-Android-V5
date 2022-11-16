package dji.v5.ux.mapkit.gmap.annotations;

import dji.v5.ux.mapkit.core.models.DJIBitmapDescriptor;
import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.core.models.annotations.DJIMarker;
import dji.v5.ux.mapkit.gmap.map.GMapDelegate;
import dji.v5.ux.mapkit.gmap.utils.GoogleUtils;

import com.google.android.gms.maps.model.Marker;

/**
 * Created by joeyang on 5/29/17.
 */
public class GMarker extends DJIMarker {

    private Marker mMarker;

    private GMapDelegate gMap;

    public GMarker(Marker marker, GMapDelegate map) {
        this.mMarker = marker;
        this.gMap = map;
    }

    @Override
    public void setPosition(DJILatLng latLng) {
        setPositionCache(latLng);
        mMarker.setPosition(GoogleUtils.fromDJILatLng(latLng));
    }

    @Override
    public void setRotation(float rotate) {
        mMarker.setRotation(rotate);
    }

    @Override
    public void setIcon(DJIBitmapDescriptor icon) {
        mMarker.setIcon(GoogleUtils.fromDJIBitmapDescriptor(icon));
    }

    @Override
    public void setAnchor(float u, float v) {
        mMarker.setAnchor(u, v);
    }

    @Override
    public void setTitle(String title) {
        mMarker.setTitle(title);
    }

    @Override
    public void setVisible(boolean visible) {
        mMarker.setVisible(visible);
    }

    @Override
    public boolean isVisible() {
        return mMarker.isVisible();
    }

    @Override
    public void showInfoWindow() {
        mMarker.showInfoWindow();
    }

    @Override
    public void hideInfoWindow() {
        mMarker.hideInfoWindow();
    }

    @Override
    public boolean isInfoWindowShown() {
        return mMarker.isInfoWindowShown();
    }

    @Override
    public void remove() {
        mMarker.remove();
        gMap.onMarkerRemove(mMarker);
    }

    @Override
    public String getTitle() {
        return mMarker.getTitle();
    }

    @Override
    public void setDraggable(boolean b) {
        mMarker.setDraggable(b);
    }

    @Override
    public boolean isDraggable() {
        return mMarker.isDraggable();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GMarker gMarker = (GMarker) o;

        return mMarker != null ? mMarker.equals(gMarker.mMarker) : gMarker.mMarker == null;

    }

    @Override
    public int hashCode() {
        return mMarker != null ? mMarker.hashCode() : 0;
    }
}
