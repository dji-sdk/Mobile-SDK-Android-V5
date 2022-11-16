package dji.v5.ux.mapkit.core.models.annotations;

import androidx.annotation.ColorInt;
import dji.v5.ux.mapkit.core.models.DJILatLng;

//Doc key: DJIMap_DJICircleOptions
/**
 * Represents a set of instructions for creating a new `DJICircle`. This object
 * can be passed to `DJICircleOptions` to add a new circle to a map. Each method
 * returns the object itself so that they can be used in a builder pattern.
 */
public class DJICircleOptions {
    private DJILatLng center;
    private double radius;
    private float strokeWidth;
    @ColorInt
    private int strokeColor;
    @ColorInt
    private int fillColor;
    private float zIndex;

    //Doc key: DJIMap_DJICircleOptions_center
    /**
     * Sets the center the new circle will have when added to the map.
     *
     * @param center The coordinates of the center of the circle.
     * @return The `DJICircleOptions` object.
     */
    public DJICircleOptions center(DJILatLng center) {
        this.center = center;
        return this;
    }

    //Doc key: DJIMap_DJICircleOptions_radius
    /**
     * Sets the radius the new circle will have when added to the map.
     *
     * @param radius The radius of the circle in meters.
     * @return The `DJICircleOptions` object.
     */
    public DJICircleOptions radius(double radius) {
        this.radius = radius;
        return this;
    }

    //Doc key: DJIMap_DJICircleOptions_zIndex
    /**
     * Sets the zIndex the new circle will have when added to the map.
     *
     * @param zIndex The zIndex of the circle.
     * @return The `DJICircleOptions` object.
     */
    public DJICircleOptions zIndex(float zIndex) {
        this.zIndex = zIndex;
        return this;
    }

    //Doc key: DJIMap_DJICircleOptions_strokeWidth
    /**
     * Sets the stroke width the new circle will have when added to the map.
     *
     * @param width The stroke width of the circle in pixels.
     * @return The `DJICircleOptions` object.
     */
    public DJICircleOptions strokeWidth(float width) {
        this.strokeWidth = width;
        return this;
    }

    //Doc key: DJIMap_DJICircleOptions_strokeColor
    /**
     * Sets the stroke color the new circle will have when added to the map.
     *
     * @param color The stroke color of the circle.
     * @return The `DJICircleOptions` object.
     */
    public DJICircleOptions strokeColor(@ColorInt int color) {
        this.strokeColor = color;
        return this;
    }

    //Doc key: DJIMap_DJICircleOptions_fillColor
    /**
     * Sets the fill color the new circle will have when added to the map.
     *
     * @param color The fill color of the circle.
     * @return The `DJICircleOptions` object.
     */
    public DJICircleOptions fillColor(@ColorInt int color) {
        this.fillColor = color;
        return this;
    }

    /**
     * {@hide}
     *
     * @return
     */
    public DJILatLng getCenter() {
        return center;
    }

    /**
     * {@hide}
     *
     * @return
     */
    public double getRadius() {
        return radius;
    }

    /**
     * {@hide}
     *
     * @return
     */
    public float getStrokeWidth() {
        return strokeWidth;
    }

    /**
     * {@hide}
     *
     * @return
     */
    @ColorInt
    public int getStrokeColor() {
        return strokeColor;
    }

    /**
     * {@hide}
     *
     * @return
     */
    @ColorInt
    public int getFillColor() {
        return fillColor;
    }

    /**
     * {@hide}
     *
     * @return
     */
    public float getZIndex() {
        return zIndex;
    }
}
