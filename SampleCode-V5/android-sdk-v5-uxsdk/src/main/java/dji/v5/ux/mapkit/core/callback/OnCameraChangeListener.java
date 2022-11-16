package dji.v5.ux.mapkit.core.callback;

import dji.v5.ux.mapkit.core.models.DJICameraPosition;

// Doc key: DJIMap_onCameraChangeListenerInterface
/**
 *  Listener on the camera change event.
 */
public interface OnCameraChangeListener {

    // Doc key: DJIMap_onCameraChangeCallback
    /**
     *  A callback indicating that the camera position has changed.
     *  
     *  @param cameraPosition The new camera position.
     */
    void onCameraChange(DJICameraPosition cameraPosition);
}
