package dji.v5.ux.mapkit.maplibre.map;

import dji.v5.ux.mapkit.core.maps.DJIUiSettings;
import com.mapbox.mapboxsdk.maps.UiSettings;

/**
 * Created by joeyang on 11/3/17.
 */
public class MUiSettings implements DJIUiSettings {

    UiSettings uiSettings;

    public MUiSettings(UiSettings uiSettings) {
        this.uiSettings = uiSettings;
    }

    @Override
    public void setZoomControlsEnabled(boolean enabled) {
        // no zoom controls available on Mapbox since 7.0.0
    }

    @Override
    public void setCompassEnabled(boolean enabled) {
        uiSettings.setCompassEnabled(enabled);
    }

    @Override
    public void setMapToolbarEnabled(boolean enabled) {
       //do something
    }

    @Override
    public void setMyLocationButtonEnabled(boolean enabled) {
        //do something
    }

    @Override
    public void setRotateGesturesEnabled(boolean enabled) {
        uiSettings.setRotateGesturesEnabled(enabled);
    }

    @Override
    public void setTiltGesturesEnabled(boolean enabled) {
        uiSettings.setTiltGesturesEnabled(enabled);
    }

    @Override
    public void setZoomGesturesEnabled(boolean enabled) {
        uiSettings.setZoomGesturesEnabled(enabled);
    }

    @Override
    public void setScrollGesturesEnabled(boolean enabled) {
        uiSettings.setScrollGesturesEnabled(enabled);
    }
}
