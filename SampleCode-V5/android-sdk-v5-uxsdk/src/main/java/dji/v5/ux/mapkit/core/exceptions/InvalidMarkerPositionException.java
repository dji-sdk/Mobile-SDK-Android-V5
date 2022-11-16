package dji.v5.ux.mapkit.core.exceptions;

/**
 * Created by joeyang on 6/5/17.
 */

public class InvalidMarkerPositionException extends RuntimeException {

    public InvalidMarkerPositionException() {
        super("Adding an invalid Marker to a Map. "
                + "Missing the required position field. "
                + "Provide a non null DJILatLng as position to the Marker.");
    }
}
