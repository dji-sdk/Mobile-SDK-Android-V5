package dji.v5.ux.mapkit.core.models.annotations;

import dji.v5.ux.mapkit.core.models.DJIBitmapDescriptor;
import dji.v5.ux.mapkit.core.models.DJILatLng;

//Doc key: DJIMap_DJIMarker
/**
 *  A marker that exists on a map object. To add a new marker to a map, see
 *  `DJIMarkerOptions`.
 */
public abstract class DJIMarker {

    protected DJILatLng positionCache; // 缓存的WGS坐标
    private float rotationCache;
    private Object object;

    //Doc key: DJIMap_DJIMarker_setPosition
    /**
     *  Sets the coordinates of the marker.
     *  
     *  @param latLng The new coordinates.
     */
    public abstract void setPosition(DJILatLng latLng);

    /**
     * {@hide}
     * Sets marker geographic coordinate cache
     * @param latLng
     */
    public void setPositionCache(DJILatLng latLng) {
        positionCache = latLng;
    }

    /**
     * {@hide}
     * @param rotation
     */
    public void setRotationCache(float rotation) {
        rotationCache = rotation;
    }

    //Doc key: DJIMap_DJIMarker_setRotation
    /**
     *  Changes the rotation of the marker.
     *  
     *  @param rotation The new rotation.
     */
    public abstract void setRotation(float rotation);

    /**
     * {@hide}
     * @return
     */
    public float getRotation() {
        return rotationCache;
    }

    //Doc key: DJIMap_DJIMarker_setIcon
    /**
     *  Changes the icon of the marker.
     *  
     *  @param bitmap The new icon.
     */
    public abstract void setIcon(DJIBitmapDescriptor bitmap);

    //Doc key: DJIMap_DJIMarker_setAnchor
    /**
     * Sets the anchor point of the marker image.
     *
     * @param u Horizontal distance, normalized to [0, 1], of the anchor from the left edge.
     * @param v Vertical distance, normalized to [0, 1], of the anchor from the top edge.
     */
    public abstract void setAnchor(float u, float v);

    //Doc key: DJIMap_DJIMarker_getPosition
    /**
     *  Gets the position of the marker.
     *  
     *  @return The position of the marker.
     */
    public DJILatLng getPosition() {
        return positionCache;
    }

    //Doc key: DJIMap_DJIMarker_setTitle
    /**
     *  Changes the title of the marker.
     *  
     *  @param title The new title.
     */
    public abstract void setTitle(String title);

    //Doc key: DJIMap_DJIMarker_getTitle
    /**
     *  Gets the title of the marker.
     *  
     *  @return The title of the marker.
     */
    public abstract String getTitle();

    //Doc key: DJIMap_DJIMarker_setVisible
    /**
     *  Changes the visibility of the marker.
     *  
     *  @param visible `true` if the marker is visible, `false` if it is hidden.
     */
    public abstract void setVisible(boolean visible);

    //Doc key: DJIMap_DJIMarker_isVisible
    /**
     *  Gets the visibility of the marker.
     *  
     *  @return `true` if the marker is visible, `false` if it is hidden.
     */
    public abstract boolean isVisible();

    /**
     * {@hide}
     * Shows InfoWindow
     */
    public abstract void showInfoWindow();

    /**
     * {@hide}
     * Hides InfoWindow
     */
    public abstract void hideInfoWindow();

    /**
     * {@hide}
     * Is InfoWindow showing
     * @return
     */
    public abstract boolean isInfoWindowShown();

    //Doc key: DJIMap_DJIMarker_remove
    /**
     *  Removes the marker from the map it is on.
     */
    public abstract void remove();

    //Doc key: DJIMap_DJIMarker_setTag
    /**
     *  Sets the tag which is an object associated with the marker.
     *  
     *  @param o An object associated with the marker.
     */
    public void setTag(Object o) {
        object = o;
    }

    //Doc key: DJIMap_DJIMarker_getTag
    /**
     *  Gets the tag.
     *  
     *  @return An object associated with the marker.
     */
    public Object getTag() {
        return object;
    }

    //Doc key: DJIMap_DJIMarker_setDraggable
    /**
     * Sets the draggability of the marker.
     *
     * @param draggable The draggability of the marker.
     */
    public abstract void setDraggable(boolean draggable);

    //Doc key: DJIMap_DJIMarker_isDraggable
    /**
     * Gets the draggability of the marker.
     *
     * @return The draggability of the marker.
     */
    public abstract boolean isDraggable();

}
