package dji.v5.ux.mapkit.amap.map;

import com.amap.api.maps.UiSettings;
import dji.v5.ux.mapkit.core.maps.DJIUiSettings;

/**
 * Created by joeyang on 6/8/17.
 */
public class AUiSettings implements DJIUiSettings {

    UiSettings mUiSettings;

    public AUiSettings(UiSettings uiSettings) {
        mUiSettings = uiSettings;
    }

    @Override
    public void setZoomControlsEnabled(boolean enabled) {
        mUiSettings.setZoomControlsEnabled(enabled);
    }

    @Override
    public void setScrollGesturesEnabled(boolean enabled) {
        mUiSettings.setScrollGesturesEnabled(enabled);
    }

    @Override
    public void setCompassEnabled(boolean enabled) {
        mUiSettings.setCompassEnabled(enabled);
    }

    @Override
    public void setMapToolbarEnabled(boolean enabled) {
        //do something
    }

    @Override
    public void setMyLocationButtonEnabled(boolean enabled) {
        mUiSettings.setMyLocationButtonEnabled(enabled);
    }

    @Override
    public void setRotateGesturesEnabled(boolean enabled) {
        mUiSettings.setRotateGesturesEnabled(enabled);
    }

    @Override
    public void setTiltGesturesEnabled(boolean enabled) {
        mUiSettings.setTiltGesturesEnabled(enabled);
    }

    @Override
    public void setZoomGesturesEnabled(boolean enabled) {
        mUiSettings.setZoomGesturesEnabled(enabled);
    }
}
