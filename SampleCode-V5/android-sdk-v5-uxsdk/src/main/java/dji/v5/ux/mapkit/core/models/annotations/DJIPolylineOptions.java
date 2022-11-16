package dji.v5.ux.mapkit.core.models.annotations;

import androidx.annotation.ColorInt;
import dji.v5.ux.mapkit.core.models.BasePointCollection;
import dji.v5.ux.mapkit.core.models.DJIBitmapDescriptor;
import dji.v5.ux.mapkit.core.models.DJILatLng;

import java.util.List;

//Doc key: DJIMap_DJIPolylineOptions
/**
 * Represents a set of instructions for creating a new `DJIPolyline`. This object
 * can be passed to `DJIPolylineOptions` to add a new polyline to a map. Each method
 * returns the object itself so that they can be used in a builder pattern.
 */
public class DJIPolylineOptions extends BasePointCollection {
    // private static final String TAG = "DJIPolylineOptions";

    private static final float DASH_LENGTH = 3F;

    private float mWidth;
    private float mZIndex;
    @ColorInt
    private int mColor;
    private boolean mGeodesic;
    private boolean mVisible;
    private DJIBitmapDescriptor mBitmapDescriptor;
    private boolean mEnableTexture;
    private float mDashLength;
    private boolean mDashed;

    //Doc key: DJIMap_DJIPolylineOptions_init
    /**
     * Creates a new `DJIPolylineOptions` object.
     */
    public DJIPolylineOptions() {
        super();
        mVisible = true;
        mColor = 0xff000000;
        mDashLength = DASH_LENGTH;
    }

    //Doc key: DJIMap_DJIPolylineOptions_width
    /**
     * Sets the width the new polyline will have when added to the map.
     *
     * @param width The width of the polyline in pixels.
     * @return The `DJIPolylineOptions` object.
     */
    public DJIPolylineOptions width(float width) {
        mWidth = width;
        return this;
    }

    //Doc key: DJIMap_DJIPolylineOptions_zIndex
    /**
     * Sets the zIndex the new polyline will have when added to the map.
     *
     * @param zIndex The zIndex of the polyline.
     * @return The `DJIPolylineOptions` object.
     */
    public DJIPolylineOptions zIndex(float zIndex) {
        mZIndex = zIndex;
        return this;
    }

    //Doc key: DJIMap_DJIPolylineOptions_color
    /**
     * Sets the color the new polyline will have when added to the map.
     *
     * @param color The color of the polyline.
     * @return The `DJIPolylineOptions` object.
     */
    public DJIPolylineOptions color(@ColorInt int color) {
        mColor = color;
        return this;
    }

    //Doc key: DJIMap_DJIPolylineOptions_geodesic
    /**
     * Indicates whether the polyline will be drawn as geodesic when added to the map. Supported
     * by AMaps and Google Maps.
     *
     * @param geodesic Whether the polyline will be drawn as geodesic.
     * @return The `DJIPolylineOptions` object.
     */
    public DJIPolylineOptions geodesic(boolean geodesic) {
        mGeodesic = geodesic;
        return this;
    }

    //Doc key: DJIMap_DJIPolylineOptions_visible
    /**
     * Sets the visibility the new polyline will have when added to the map.
     *
     * @param visible The visibility of the polyline.
     * @return The `DJIPolylineOptions` object.
     */
    public DJIPolylineOptions visible(boolean visible) {
        mVisible = visible;
        return this;
    }

    /**
     * {@hide}
     * @param enableTexture
     * @return
     */
    public DJIPolylineOptions setUseTexture(boolean enableTexture) {
        mEnableTexture = enableTexture;
        return this;
    }

    /**
     * {@hide}
     * @param bitmapDescriptor
     * @return
     */
    public DJIPolylineOptions setCustomTexture(DJIBitmapDescriptor bitmapDescriptor) {
        mBitmapDescriptor = bitmapDescriptor;
        return this;
    }

    //Doc key: DJIMap_DJIPolylineOptions_dashed
    /**
     * Indicates whether the new polyline will be dashed when added to the map.
     *
     * @param dashed Whether the polyline is dashed.
     * @return The `DJIPolylineOptions` object.
     */
    public DJIPolylineOptions setDashed(boolean dashed) {
        mDashed = dashed;
        return this;
    }

    //Doc key: DJIMap_DJIPolylineOptions_dashLength
    /**
     * Sets the dash length in pixels the new polyline will have when added to the map. Not
     * supported by AMaps.
     *
     * @param length The dash length in pixels of the polyline.
     * @return The `DJIPolylineOptions` object.
     */
    public DJIPolylineOptions setDashLength(float length) {
        mDashLength = length;
        return this;
    }

    /**
     * {@hide}
     *
     * @return
     */
    public float getWidth() {
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
    @ColorInt
    public int getColor() {
        return mColor;
    }

    /**
     * {@hide}
     *
     * @return
     */
    public boolean isGeodesic() {
        return mGeodesic;
    }

    /**
     * {@hide}
     *
     * @return
     */
    public boolean isVisible() {
        return mVisible;
    }

    /**
     * {@hide}
     *
     * @return
     */
    public boolean isEnableTexture() {
        return mEnableTexture;
    }

    /**
     * {@hide}
     *
     * @return
     */
    public boolean isDashed() { return mDashed; }

    /**
     * {@hide}
     *
     * @return
     */
    public float getDashLength() { return mDashLength; }

    /**
     * {@hide}
     *
     * @return
     */
    public DJIBitmapDescriptor getBitmapDescriptor() {
        return mBitmapDescriptor;
    }

    //Doc key: DJIMap_DJIPolylineOptions_addPoint
    /**
     * Adds the given point to the polyline.
     *
     * @param point The coordinates of the point to add.
     * @return The `DJIPolylineOptions` object.
     */
    public DJIPolylineOptions add(DJILatLng point) {
        addPoint(point);
        return this;
    }

    //Doc key: DJIMap_DJIPolylineOptions_addPoints
    /**
     * Adds the given points to the polyline.
     *
     * @param points The coordinates of the points to add.
     * @return The `DJIPolylineOptions` object.
     */
    public DJIPolylineOptions add(DJILatLng... points) {
        for (DJILatLng point : points) {
            addPoint(point);
        }
        return this;
    }

    //Doc key: DJIMap_DJIPolylineOptions_addAll
    /**
     * Adds the given points to the polyline.
     *
     * @param points The coordinates of the points to add.
     * @return The `DJIPolylineOptions` object.
     */
    public DJIPolylineOptions addAll(List<DJILatLng> points) {
        setPoints(points);
        return this;
    }
}
