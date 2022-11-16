package dji.v5.ux.mapkit.core.models.annotations;

import android.graphics.PointF;

import androidx.annotation.FloatRange;

import dji.v5.ux.mapkit.core.models.DJIBitmapDescriptor;
import dji.v5.ux.mapkit.core.models.DJILatLng;

//Doc key: DJIMap_DJIMarkerOptions
/**
 * Represents a set of instructions for creating a new `DJIMarker`. This object
 * can be passed to `DJIMarkerOptions` to add a new marker to a map. Each method
 * returns the object itself so that they can be used in a builder pattern.
 */
public class DJIMarkerOptions {

    private boolean mDraggable;
    private DJILatLng mPosition;
    private PointF mAnchor;
    private DJIBitmapDescriptor mIcon;
    private float mRotation;
    private int mZIndex;
    private boolean isCustomAnchor;
    private boolean mVisible;
    private String mTitle;
    private boolean mFlat;
    private boolean mInfoWindowEnable;

    //Doc key: DJIMap_DJIMarkerOptions_init
    /**
     * Creates a new `DJIMarkerOptions` object.
     */
    public DJIMarkerOptions() {
        mAnchor = new PointF(0.5f, 0.5f);
        mVisible = true;
        mInfoWindowEnable = true;
        mTitle = "";
    }

    //Doc key: DJIMap_DJIMarkerOptions_draggable
    /**
     * Sets the draggability the new marker will have when added to the map.
     *
     * @param draggable The draggability of the marker.
     * @return The `DJIMarkerOptions` object.
     */
    public DJIMarkerOptions draggable(boolean draggable) {
        mDraggable = draggable;
        return this;
    }

    //Doc key: DJIMap_DJIMarkerOptions_position
    /**
     * Sets the position the new marker will have when added to the map.
     *
     * @param position The coordinates of the marker.
     * @return The `DJIMarkerOptions` object.
     */
    public DJIMarkerOptions position(DJILatLng position) {
        mPosition = position;
        return this;
    }

    //Doc key: DJIMap_DJIMarkerOptions_anchor
    /**
     * Sets the anchor point the marker image will have when added to the map. The default is
     * (0.5, 0.5).
     *
     * @param u Horizontal distance, normalized to [0, 1], of the anchor from the left edge.
     * @param v Vertical distance, normalized to [0, 1], of the anchor from the top edge.
     * @return The `DJIMarkerOptions` object.
     */
    public DJIMarkerOptions anchor(@FloatRange(from = 0, to = 1) float u,
                                   @FloatRange(from = 0, to = 1) float v) {
        isCustomAnchor = true;
        mAnchor = new PointF(u, v);
        return this;
    }

    //Doc key: DJIMap_DJIMarkerOptions_icon
    /**
     * Sets the icon the new marker will have when added to the map.
     *
     * @param bitmap The icon of the marker.
     * @return The `DJIMarkerOptions` object.
     */
    public DJIMarkerOptions icon(DJIBitmapDescriptor bitmap) {
        mIcon = bitmap;
        return this;
    }

    //Doc key: DJIMap_DJIMarkerOptions_rotation
    /**
     * Sets the rotation the new marker will have when added to the map.
     *
     * @param rotation The rotation of the marker.
     * @return The `DJIMarkerOptions` object.
     */
    public DJIMarkerOptions rotation(float rotation) {
        mRotation = rotation;
        return this;
    }

    //Doc key: DJIMap_DJIMarkerOptions_zIndex
    /**
     * Sets the zIndex the new marker will have when added to the map.
     *
     * @param zIndex The zIndex of the marker.
     * @return The `DJIMarkerOptions` object.
     */
    public DJIMarkerOptions zIndex(int zIndex) {
        mZIndex = zIndex;
        return this;
    }

    //Doc key: DJIMap_DJIMarkerOptions_visible
    /**
     * Sets the visibility the new marker will have when added to the map.
     *
     * @param visible The visibility of the marker.
     * @return The `DJIMarkerOptions` object.
     */
    public DJIMarkerOptions visible(boolean visible) {
        mVisible = visible;
        return this;
    }

    //Doc key: DJIMap_DJIMarkerOptions_title
    /**
     * Sets the title the new marker will have when added to the map.
     *
     * @param title The title of the marker.
     * @return The `DJIMarkerOptions` object.
     */
    public DJIMarkerOptions title(String title) {
        mTitle = title;
        return this;
    }

    //Doc key: DJIMap_DJIMarkerOptions_flat
    /**
     * Sets whether the marker should be flat against the map when added to the map. Only supported
     * by AMaps and Google Maps.
     *
     * @param flat `true` if the marker should be flat against the map, `false` if the marker
     *             should be a billboard facing the camera.
     * @return The `DJIMarkerOptions` object.
     */
    public DJIMarkerOptions flat(boolean flat) {
        mFlat = flat;
        return this;
    }

    //Doc key: DJIMap_DJIMarkerOptions_infoWindowEnable
    /**
     * Sets whether the info window is enabled for the new marker when added to the map. Only
     * supported by AMaps.
     *
     * @param infoWindowEnable Whether the info window is enabled for the marker.
     * @return The `DJIMarkerOptions` object.
     */
    public DJIMarkerOptions setInfoWindowEnable(boolean infoWindowEnable) {
        this.mInfoWindowEnable = infoWindowEnable;
        return this;
    }

    /**
     * {@hide}
     *
     * @return
     */
    public boolean getDraggable() {
        return mDraggable;
    }

    /**
     * {@hide}
     *
     * @return
     */
    public DJILatLng getPosition() {
        return mPosition;
    }

    /**
     * {@hide}
     *
     * @return
     */
    @FloatRange(from = 0, to = 1)
    public float getAnchorU() {
        return mAnchor.x;
    }

    /**
     * {@hide}
     *
     * @return
     */
    @FloatRange(from = 0, to = 1)
    public float getAnchorV() {
        return mAnchor.y;
    }

    /**
     * {@hide}
     *
     * @return
     */
    public DJIBitmapDescriptor getIcon() {
        return mIcon;
    }

    /**
     * {@hide}
     *
     * @return
     */
    public float getRotation() {
        return mRotation;
    }

    /**
     * {@hide}
     *
     * @return
     */
    public int getZIndex() {
        return mZIndex;
    }

    /**
     * {@hide}
     *
     * @return
     */
    public boolean isCustomAnchor() {
        return isCustomAnchor;
    }

    /**
     * {@hide}
     *
     * @return
     */
    public boolean getVisible() {
        return mVisible;
    }

    /**
     * {@hide}
     *
     * @return
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * {@hide}
     *
     * @return
     */
    public boolean isFlat() {
        return mFlat;
    }

    /**
     * {@hide}
     *
     * @return
     */
    public boolean isInfoWindowEnable() {
        return mInfoWindowEnable;
    }
}
