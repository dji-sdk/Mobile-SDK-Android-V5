package dji.v5.ux.mapkit.core.models.annotations;

import androidx.annotation.ColorInt;

import dji.v5.ux.mapkit.core.models.DJILatLng;

import java.util.List;

//Doc key: DJIMap_DJIPolygon
/**
 *  A polygon that exists on a map object. To add a new polygon to a map, see
 *  `DJIPolygonOptions`.
 */
public interface DJIPolygon {

    //Doc key: DJIMap_DJIPolygon_remove
    /**
     * Removes the polygon from the map it is on.
     */
    void remove();

    //Doc key: DJIMap_DJIPolygon_isVisible
    /**
     * Gets the visibility of the polygon.
     *
     * @return `true` if the polygon is visible, `false` if it is hidden.
     */
    boolean isVisible();

    //Doc key: DJIMap_DJIPolygon_setVisible
    /**
     * Changes the visibility of the polygon.
     *
     * @param visible `true` if the polygon is visible, `false` if it is hidden.
     */
    void setVisible(boolean visible);

    //Doc key: DJIMap_DJIPolygon_setPoints
    /**
     * Sets the points of the polygon.
     *
     * @param points The points of the polygon.
     */
    void setPoints(List<DJILatLng> points);

    //Doc key: DJIMap_DJIPolygon_getPoints
    /**
     * Gets the points of the polygon.
     *
     * @return The points of the polygon.
     */
    List<DJILatLng> getPoints();

    //Doc key: DJIMap_DJIPolygon_setFillColor
    /**
     * Sets the fill color of the polygon.
     *
     * @param color The fill color of the polygon.
     */
    void setFillColor(@ColorInt int color);

    //Doc key: DJIMap_DJIPolygon_getFillColor
    /**
     * Gets the fill color of the polygon.
     *
     * @return The fill color of the polygon.
     */
    @ColorInt
    int getFillColor();

    //Doc key: DJIMap_DJIPolygon_setStrokeColor
    /**
     * Sets the stroke color of the polygon.
     *
     * @param color The stroke color of the polygon.
     */
    void setStrokeColor(@ColorInt int color);

    //Doc key: DJIMap_DJIPolygon_getStrokeColor
    /**
     * Gets the stroke color of the polygon.
     *
     * @return The fill color of the polygon.
     */
    @ColorInt
    int getStrokeColor();

    //Doc key: DJIMap_DJIPolygon_setStrokeWidth
    /**
     * Sets the stroke width of the polygon.
     *
     * @param strokeWidth The stroke width of the polygon.
     */
    void setStrokeWidth(float strokeWidth);

    //Doc key: DJIMap_DJIPolygon_getStrokeWidth
    /**
     * Gets the stroke width of the polygon.
     *
     * @return The stroke width of the polygon.
     */
    float getStrokeWidth();



}
