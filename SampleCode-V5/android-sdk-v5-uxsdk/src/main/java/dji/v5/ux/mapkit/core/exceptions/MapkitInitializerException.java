package dji.v5.ux.mapkit.core.exceptions;

import dji.v5.ux.mapkit.core.Mapkit;

/**
 * Mapkit的createMapView异常
 * Created by joeyang on 1/5/18.
 */

public class MapkitInitializerException extends IllegalStateException {

    public MapkitInitializerException(@Mapkit.MapProviderConstant int provider) {
        super("Error initializing map for provider type " + provider);
    }

    public MapkitInitializerException(String s) {
        super(s);
    }
}
