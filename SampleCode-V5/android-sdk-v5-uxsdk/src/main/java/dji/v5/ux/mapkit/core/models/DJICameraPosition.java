package dji.v5.ux.mapkit.core.models;

import androidx.annotation.NonNull;

//Doc key: DJIMap_DJICameraPosition
/**
 *  The position of the camera.
 */
public class DJICameraPosition {
    public final DJILatLng target;
    public final float zoom;
    public final float tilt;
    public final float bearing;

    private DJICameraPosition() {
        target = null;
        zoom = 0F;
        tilt = 0F;
        bearing = 0F;
    }

    //Doc key: DJIMap_DJICameraPosition_constructor1
    /**
     *  Creates a new camera position with the given coordinates, zoom level, tilt, and
     *  bearing.
     *  
     *  @param target The position of the center point.
     *  @param zoom The zoom level between 0 and 20. 0 is the lowest zoom level which will show  the entire map, and 20 is the highest zoom level.
     *  @param tilt The tilt of the camera.
     *  @param bearing The orientation of the camera.
     */
    public DJICameraPosition(DJILatLng target, float zoom, float tilt, float bearing) {
        this.target = target;
        this.zoom = zoom;
        this.tilt = tilt;
        this.bearing = ((double)bearing <= 0.0d ? bearing % 360.0f + 360.0f : bearing) % 360.0f;
    }

    //Doc key: DJIMap_DJICameraPosition_constructor2
    /**
     *  Creates a new camera position with the given coordinates and zoom level.
     *  
     *  @param position The position of the center point.
     *  @param zoom The zoom level between 0 and 20. 0 is the lowest zoom level which will show  the entire map, and 20 is the highest zoom level.
     */
    public DJICameraPosition(DJILatLng position, float zoom){
        this(position, zoom, 0.0f, 0.0f);
    }

    //Doc key: DJIMap_DJICameraPosition_getPosition
    /**
     *  Gets the position of the center point of the camera.
     *  
     *  @return The position of the center point of the camera.
     */
    public DJILatLng getPosition() {
        return target;
    }

    //Doc key: DJIMap_DJICameraPosition_getZoom
    /**
     *  Gets the zoom level of the camera.
     *  
     *  @return The zoom level of the camera.
     */
    public float getZoom() {
        return zoom;
    }

    //Doc key: DJIMap_DJICameraPosition_getTilt
    /**
     *  Gets the tilt of the camera.
     *  
     *  @return The tilt of the camera.
     */
    public float getTilt() {
        return tilt;
    }

    //Doc key: DJIMap_DJICameraPosition_getBearing
    /**
     *  Gets the orientation of the camera.
     *  
     *  @return The orientation of the camera.
     */
    public float getBearing() {
        return bearing;
    }

    public static final DJICameraPosition fromLatLngZoom(DJILatLng target, float zoom) {
        return new DJICameraPosition(target, zoom, 0.0f, 0.0f);
    }

    @Override
    public int hashCode() {
        int result = target != null ? target.hashCode() : 0;
        result = 31 * result + (zoom != +0.0f ? Float.floatToIntBits(zoom) : 0);
        result = 31 * result + (tilt != +0.0f ? Float.floatToIntBits(tilt) : 0);
        result = 31 * result + (bearing != +0.0f ? Float.floatToIntBits(bearing) : 0);
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        else if (!(obj instanceof DJICameraPosition))
            return false;
        else {
            DJICameraPosition o = (DJICameraPosition) obj;
            return this.target.equals(o.target) && zoom == o.zoom && tilt == o.tilt && bearing == o.bearing;
        }
    }

    @Override
    public String toString() {
        return "latlng: " + target + " zoom: " + zoom + " tilt: " + tilt + " bearing: " + bearing;
    }

    public static Builder builder(DJICameraPosition camera) {
        return new Builder(camera);
    }

    public static final class Builder {
        private DJILatLng target;
        private float zoom;
        private float tilt;
        private float bearing;

        public Builder() {

        }

        public Builder(DJICameraPosition previous) {
            this.target = previous.target;
            this.zoom = previous.zoom;
            this.tilt = previous.tilt;
            this.bearing = previous.bearing;
        }

        public Builder target(@NonNull DJILatLng location) {
            this.target = location;
            return this;
        }

        public Builder zoom(float zoom) {
            this.zoom = zoom;
            return this;
        }

        public Builder tilt(float tilt) {
            this.tilt = tilt;
            return this;
        }

        public Builder bearing(float bearing) {
            this.bearing = bearing;
            return this;
        }

        public DJICameraPosition build() {
            return new DJICameraPosition(this.target, this.zoom, this.tilt, this.bearing);
        }
    }
}
