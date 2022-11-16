package dji.v5.ux.mapkit.amap.annotations;

import com.amap.api.maps.model.Marker;

import dji.v5.ux.mapkit.amap.map.AMapDelegate;
import dji.v5.ux.mapkit.amap.utils.AMapUtils;
import dji.v5.ux.mapkit.core.models.DJIBitmapDescriptor;
import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.core.models.annotations.DJIMarker;

/**
 * Created by joeyang on 5/30/17.
 */
public class AMarker extends DJIMarker {

    private Marker mMarker;

    private AMapDelegate aMap;

    public AMarker(Marker marker, AMapDelegate aMap) {
        this.mMarker = marker;
        this.aMap = aMap;
    }

    @Override
    public void setPosition(DJILatLng latLng) {
        setPositionCache(latLng);
        mMarker.setPosition(AMapUtils.fromDJILatLng(latLng));
    }

    /**
     * 设置水平旋转角度
     * @param rotate 旋转角度，顺时针为正；高德地图以逆时针为正，所以实现中要取负
     */
    @Override
    public void setRotation(float rotate) {
        setRotationCache(-rotate);
        mMarker.setRotateAngle(-rotate);
    }

    @Override
    public void setIcon(DJIBitmapDescriptor icon) {
        mMarker.setIcon(AMapUtils.fromDJIBitmapDescriptor(icon));
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
    public String getTitle() {
        return mMarker.getTitle();
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
        aMap.onMarkerRemove(mMarker);
    }

    @Override
    public void setDraggable(boolean b) {
        mMarker.setDraggable(b);
    }

    @Override
    public boolean isDraggable() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AMarker aMarker = (AMarker) o;

        return mMarker != null ? mMarker.equals(aMarker.mMarker) : aMarker.mMarker == null;

    }

    @Override
    public int hashCode() {
        return mMarker != null ? mMarker.hashCode() : 0;
    }
}
