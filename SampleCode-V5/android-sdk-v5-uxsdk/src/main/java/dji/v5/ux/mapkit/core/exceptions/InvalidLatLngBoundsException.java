package dji.v5.ux.mapkit.core.exceptions;

/**
 * Created by joeyang on 6/21/17.
 */

public class InvalidLatLngBoundsException extends RuntimeException {

    public InvalidLatLngBoundsException(int latLngsListSize) {
        super("Cannot create a DJILatLngBounds from " + latLngsListSize + " items");
    }
}
