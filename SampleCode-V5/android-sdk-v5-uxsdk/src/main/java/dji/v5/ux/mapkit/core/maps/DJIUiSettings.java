package dji.v5.ux.mapkit.core.maps;

//Doc key: DJIMap_DJIUiSettings
/**
 * Settings for the user interface of a DJIMap.
 */
public interface DJIUiSettings {

    //Doc key: DJIMap_DJIUiSettings_setZoomControlsEnabled
    /**
     * Enables or disables the zoom controls. Not supported by HERE Maps or Mapbox.
     *
     * @param enabled `true` to enable the zoom controls; `false` to disable the zoom controls.
     */
    void setZoomControlsEnabled(boolean enabled);

    //Doc key: DJIMap_DJIUiSettings_setCompassEnabled
    /**
     * Enables or disables the compass. Not supported by HERE Maps.
     *
     * @param enabled `true` to enable the compass; `false` to disable the compass.
     */
    void setCompassEnabled(boolean enabled);

    /**
     * {@hide}
     */
    void setMapToolbarEnabled(boolean enabled);

    /**
     * {@hide}
     */
    void setMyLocationButtonEnabled(boolean enabled);

    //Doc key: DJIMap_DJIUiSettings_setRotateGesturesEnabled
    /**
     * Enables or disables rotate gestures.
     *
     * @param enabled `true` to enable rotate gestures; `false` to disable rotate gestures
     */
    void setRotateGesturesEnabled(boolean enabled);

    //Doc key: DJIMap_DJIUiSettings_setTiltGesturesEnabled
    /**
     * Enables or disables tilt gestures.
     *
     * @param enabled `true` to enable tilt gestures; `false` to disable tilt gestures
     */
    void setTiltGesturesEnabled(boolean enabled);

    //Doc key: DJIMap_DJIUiSettings_setZoomGesturesEnabled
    /**
     * Enables or disables zoom gestures.
     *
     * @param enabled `true` to enable zoom gestures; `false` to disable zoom gestures
     */
    void setZoomGesturesEnabled(boolean enabled);

    //Doc key: DJIMap_DJIUiSettings_setScrollGesturesEnabled
    /**
     * Enables or disables scroll gestures.
     *
     * @param enabled `true` to enable scroll gestures; `false` to disable scroll gestures
     */
    void setScrollGesturesEnabled(boolean enabled);
}
