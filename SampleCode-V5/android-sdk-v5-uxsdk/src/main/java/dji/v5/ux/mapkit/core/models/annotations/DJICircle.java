package dji.v5.ux.mapkit.core.models.annotations;

import androidx.annotation.ColorInt;

import dji.v5.ux.mapkit.core.models.DJILatLng;

//Doc key: DJIMap_DJICircle
/**
 *  A circle that exists on a map object. To add a new circle to a map, see
 *  `DJICircleOptions`.
 */
public interface DJICircle {

    //Doc key: DJIMap_DJICircle_remove
    /**
     * Removes the circle from the map it is on.
     */
    void remove();

    //Doc key: DJIMap_DJICircle_setVisible
    /**
     * Changes the visibility of the circle.
     *
     * @param visible `true` if the circle is visible, `false` if it is hidden.
     */
    void setVisible(boolean visible);

    //Doc key: DJIMap_DJICircle_isVisible
    /**
     * Gets the visibility of the circle.
     *
     * @return `true` if the circle is visible, `false` if it is hidden.
     */
    boolean isVisible();

    //Doc key: DJIMap_DJICircle_setCenter
    /**
     * Sets the center of the circle.
     *
     * @param center The coordinates of the center of the circle.
     */
    void setCenter(DJILatLng center);

    //Doc key: DJIMap_DJICircle_getCenter
    /**
     * Gets the center of the circle.
     *
     * @return The coordinates of the center of the circle.
     */
    DJILatLng getCenter();

    //Doc key: DJIMap_DJICircle_setRadius
    /**
     * Sets the radius of the circle.
     *
     * @param radius The radius of the circle.
     */
    void setRadius(double radius);

    //Doc key: DJIMap_DJICircle_getRadius
    /**
     * Gets the radius of the circle.
     *
     * @return The radius of the circle.
     */
    double getRadius();

    //Doc key: DJIMap_DJICircle_setFillColor
    /**
     * Sets the fill color of the circle.
     *
     * @param color The fill color of the circle.
     */
    void setFillColor(@ColorInt int color);

    //Doc key: DJIMap_DJICircle_getFillColor
    /**
     * Gets the fill color of the circle.
     *
     * @return The fill color of the circle.
     */
    @ColorInt
    int getFillColor();

    //Doc key: DJIMap_DJICircle_setStrokeColor
    /**
     * Sets the stroke color of the circle.
     *
     * @param color The stroke color of the circle.
     */
    void setStrokeColor(@ColorInt int color);

    //Doc key: DJIMap_DJICircle_getStrokeColor
    /**
     * Gets the stroke color of the circle.
     *
     * @return The stroke color of the circle.
     */
    @ColorInt
    int getStrokeColor();

    //Doc key: DJIMap_DJICircle_setZIndex
    /**
     * Sets the zIndex of the circle.
     *
     * @param zIndex The zIndex of the circle.
     */
    void setZIndex(float zIndex);

    //Doc key: DJIMap_DJICircle_getZIndex
    /**
     * Gets the zIndex of the circle.
     *
     * @return The zIndex of the circle.
     */
    float getZIndex();

    /**
     * {@hide}
     *
     * @param center
     * @param radius
     */
    void setCircle(DJILatLng center, Double radius);


    //Doc key: DJIMap_DJICircle_setStrokeWidth
    /**
     * Sets the stroke width of the circle.
     *
     * @param strokeWidth The stroke width of the circle.
     */
    void setStrokeWidth(float strokeWidth);

    //Doc key: DJIMap_DJICircle_getStrokeWidth
    /**
     * Gets the stroke width of the circle.
     *
     * @return The stroke width of the circle.
     */
    float getStrokeWidth();

}
