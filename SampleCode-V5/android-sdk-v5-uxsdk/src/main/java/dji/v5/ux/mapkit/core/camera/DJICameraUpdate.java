package dji.v5.ux.mapkit.core.camera;

import androidx.annotation.NonNull;

import dji.v5.ux.mapkit.core.maps.DJIMap;
import dji.v5.ux.mapkit.core.models.DJICameraPosition;
import dji.v5.ux.mapkit.core.models.DJILatLng;

public interface DJICameraUpdate {
    
    DJICameraPosition getCameraPosition(@NonNull DJIMap map);

    DJILatLng getTarget();

    float getZoom();

    float getTilt();

    float getBearing();
}
