package dji.v5.ux.mapkit.core.models.annotations;


import androidx.annotation.ColorInt;

import dji.v5.ux.mapkit.core.models.DJILatLng;

import java.util.List;

//Doc key: DJIMap_DJIPolyline
/**
 *  A polyline that exists on a map object. To add a new polyline to a map, see
 *  `DJIPolylineOptions`.
 */
public interface DJIPolyline {

    //Doc key: DJIMap_DJIPolyline_remove
    /**
     * Removes the polyline from the map it is on.
     */
    void remove();

    //Doc key: DJIMap_DJIPolyline_setWidth
    /**
     * Sets the width of the polyline.
     *
     * @param width The width of the polyline.
     */
    void setWidth(float width);

    //Doc key: DJIMap_DJIPolyline_getWidth
    /**
     * Gets the width of the polyline.
     *
     * @return The width of the polyline.
     */
    float getWidth();

    //Doc key: DJIMap_DJIPolyline_setPoints
    /**
     * Sets the points of the polyline.
     *
     * @param points The points of the polyline.
     */
    void setPoints(List<DJILatLng> points);

    //Doc key: DJIMap_DJIPolyline_getPoints
    /**
     * Gets the points of the polyline.
     *
     * @return The points of the polyline.
     */
    List<DJILatLng> getPoints();

    //Doc key: DJIMap_DJIPolyline_setColor
    /**
     * Sets the color of the polyline.
     *
     * @param color The color of the polyline.
     */
    void setColor(@ColorInt int color);

    //Doc key: DJIMap_DJIPolyline_getColor
    /**
     * gets the color of the polyline.
     *
     * @return The color of the polyline.
     */
    @ColorInt
    int getColor();

    //Doc key: DJIMap_DJIPolyline_setZIndex
    /**
     * Sets the zIndex of the polyline.
     *
     * @param zIndex The zIndex of the polyline.
     */
    void setZIndex(float zIndex);

    //Doc key: DJIMap_DJIPolyline_getZIndex
    /**
     * Gets the zIndex of the polyline.
     *
     * @return The zIndex of the polyline.
     */
    float getZIndex();
}
