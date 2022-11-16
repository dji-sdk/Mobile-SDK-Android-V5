package dji.v5.ux.mapkit.gmap.map;

import dji.v5.ux.mapkit.core.maps.DJIUiSettings;
import com.google.android.gms.maps.UiSettings;

/**
 * Created by joeyang on 5/24/17.
 */
public class GUiSettings implements DJIUiSettings {

    UiSettings mUiSettings;

    public GUiSettings(UiSettings uiSettings) {
        this.mUiSettings = uiSettings;
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
        mUiSettings.setMapToolbarEnabled(enabled);
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
