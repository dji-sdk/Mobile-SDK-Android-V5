package dji.v5.ux.mapkit.core.models.annotations;

import androidx.annotation.ColorInt;
import dji.v5.ux.mapkit.core.models.BasePointCollection;
import dji.v5.ux.mapkit.core.models.DJILatLng;

import java.util.List;

//Doc key: DJIMap_DJIPolygonOptions
/**
 * Represents a set of instructions for creating a new `DJIPolygon`. This object
 * can be passed to `DJIPolygonOptions` to add a new polygon to a map. Each method
 * returns the object itself so that they can be used in a builder pattern.
 */
public class DJIPolygonOptions extends BasePointCollection {
    // private static final String TAG = "DJIPolygonOptions";

    private float mWidth;
    private float mZIndex;
    @ColorInt
    private int mStrokeColor;
    @ColorInt
    private int mFillColor;
    private boolean mVisible;

    //Doc key: DJIMap_DJIPolygonOptions_init
    /**
     * Creates a new `DJIPolygonOptions` object.
     */
    public DJIPolygonOptions() {
        super();
        mVisible = true;
        mStrokeColor = 0xff000000;
        mFillColor = 0xff000000;
    }

    //Doc key: DJIMap_DJIPolygonOptions_strokeWidth
    /**
     * Sets the stroke width the new polygon will have when added to the map.
     *
     * @param width The stroke width of the polygon in pixels.
     * @return The `DJIPolygonOptions` object.
     */
    public DJIPolygonOptions strokeWidth(float width) {
        mWidth = width;
        return this;
    }

    //Doc key: DJIMap_DJIPolygonOptions_zIndex
    /**
     * Sets the zIndex the new polygon will have when added to the map.
     *
     * @param zIndex The zIndex of the polygon.
     * @return The `DJIPolygonOptions` object.
     */
    public DJIPolygonOptions zIndex(float zIndex) {
        mZIndex = zIndex;
        return this;
    }

    //Doc key: DJIMap_DJIPolygonOptions_strokeColor
    /**
     * Sets the stroke color the new polygon will have when added to the map.
     *
     * @param color The stroke color of the polygon.
     * @return The `DJIPolygonOptions` object.
     */
    public DJIPolygonOptions strokeColor(@ColorInt int color) {
        mStrokeColor = color;
        return this;
    }

    //Doc key: DJIMap_DJIPolygonOptions_fillColor
    /**
     * Sets the fill color the new polygon will have when added to the map.
     *
     * @param color The fill color of the polygon.
     * @return The `DJIPolygonOptions` object.
     */
    public DJIPolygonOptions fillColor(@ColorInt int color) {
        mFillColor = color;
        return this;
    }

    /**
     * {@hide}
     *
     * @return
     */
    public float getStrokeWidth() {
        return mWidth;
    }

    /**
     * {@hide}
     *
     * @return
     */
    public float getZIndex() {
        return mZIndex;
    }

    /**
     * {@hide}
     *
     * @return
     */
    public @ColorInt
    int getStrokeColor() {
        return mStrokeColor;
    }

    /**
     * {@hide}
     *
     * @return
     */
    public @ColorInt
    int getFillColor() {
        return mFillColor;
    }

    /**
     * {@hide}
     *
     * @return
     */
    public boolean isVisible() {
        return mVisible;
    }

    //Doc key: DJIMap_DJIPolygonOptions_addPoint
    /**
     * Adds the given point to the polygon.
     *
     * @param point The coordinates of the point to add.
     * @return The `DJIPolygonOptions` object.
     */
    public DJIPolygonOptions add(DJILatLng point) {
        addPoint(point);
        return this;
    }

    //Doc key: DJIMap_DJIPolygonOptions_addPoints
    /**
     * Adds the given points to the polygon.
     *
     * @param points The coordinates of the points to add.
     * @return The `DJIPolygonOptions` object.
     */
    public DJIPolygonOptions add(DJILatLng... points) {
        for (DJILatLng point : points) {
            addPoint(point);
        }
        return this;
    }

    //Doc key: DJIMap_DJIPolygonOptions_addAll
    /**
     * Adds the given points to the polygon.
     *
     * @param points The coordinates of the points to add.
     * @return The `DJIPolygonOptions` object.
     */
    public DJIPolygonOptions addAll(List<DJILatLng> points) {
        for (DJILatLng point : points) {
            addPoint(point);
        }
        return this;
    }
}
