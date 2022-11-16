package dji.v5.ux.mapkit.core.maps;

import android.graphics.Point;

import dji.v5.ux.mapkit.core.models.DJILatLng;

/**
 * Created by joeyang on 6/12/17.
 * 地图当前的坐标映射
 */
public interface DJIProjection {

    /**
     * 从屏幕点映射到地理坐标点
     * @param point 屏幕点
     * @return 地理坐标点
     */
    DJILatLng fromScreenLocation(Point point);

    /**
     * 从地理坐标点映射到屏幕点
     * @param location 地理坐标点
     * @return 屏幕点
     */
    Point toScreenLocation(DJILatLng location);
}
