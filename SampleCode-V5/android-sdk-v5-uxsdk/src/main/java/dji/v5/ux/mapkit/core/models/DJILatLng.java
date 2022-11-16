package dji.v5.ux.mapkit.core.models;

import dji.v5.ux.mapkit.core.utils.DJIGpsUtils;

//Doc key: DJIMap_DJILatLng
/**
 *  Represents a point which includes latitude, longitude, altitude,  and accuracy
 *  info on a map.
 */
public class DJILatLng {

   // private static final String TAG = DJILatLng.class.getSimpleName();

    private static final float ACCURACY_GPS_FINE = 16.0f;
    // private static final double ZERO_DEBOUNCE_THRESHOLD = 0.00000001d;

    public double latitude;
    public double longitude;
    public double altitude;
    public float accuracy;
    public long time;
    public long elapsedRealtimeNanos;

    //Doc key: DJIMap_DJILatLng_constructor1
    /**
     *  Creates a new `DJILatLng` object with the coordinates (latitude, longitude).
     *  
     *  @param latitude The latitude of the map point.
     *  @param longitude The longitude of the map point.
     */
    public DJILatLng(double latitude, double longitude) {
        this(latitude, longitude, 0);
    }

    //Doc key: DJIMap_DJILatLng_constructor2
    /**
     *  Creates a new `DJILatLng` object with the coordinates (latitude, longitude, altitude).
     *  
     *  @param latitude The latitude of the map point.
     *  @param longitude The longitude of the map point.
     *  @param altitude The altitude of the map point.
     */
    public DJILatLng(double latitude, double longitude, double altitude) {
        this(latitude, longitude, altitude, 0);
    }

    //Doc key: DJIMap_DJILatLng_constructor3
    /**
     *  Creates a new `DJILatLng` object with four related parameters.
     *  
     *  @param latitude The latitude of the map point.
     *  @param longitude The longitude of the map point.
     *  @param altitude The altitude of the map point.
     *  @param accuracy The accuracy of the map point.
     */
    public DJILatLng(double latitude, double longitude, double altitude, float accuracy) {
        this(latitude, longitude, altitude, accuracy, 0, 0);
    }

    //Doc key: DJIMap_DJILatLng_constructor4
    /**
     *  Creates a new `DJILatLng` object with five related parameters.
     *  
     *  @param latitude The latitude of the map point.
     *  @param longitude The longitude of the map point.
     *  @param altitude The altitude of the map point.
     *  @param accuracy The accuracy of the map point.
     *  @param time The time of the map point.
     */
    public DJILatLng(double latitude, double longitude, double altitude, float accuracy, long time) {
        this(latitude, longitude, altitude, accuracy, time, 0);
    }

    //Doc key: DJIMap_DJILatLng_constructor5
    /**
     *  Creates a new `DJILatLng` object with five related parameters.
     *  
     *  @param latitude The latitude of the map point.
     *  @param longitude The longitude of the map point.
     *  @param altitude The altitude of the map point.
     *  @param accuracy The accuracy of the map point.
     *  @param time The time of the map point.
     *  @param elapsedRealtimeNanos The time in elapsed real-time of the map point.
     */
    public DJILatLng(double latitude, double longitude, double altitude, float accuracy, long time, long elapsedRealtimeNanos) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.accuracy = accuracy;
        this.time = time;
        this.elapsedRealtimeNanos = elapsedRealtimeNanos;
    }

    //Doc key: DJIMap_DJILatLng_constructor6
    /**
     *  Creates a new `DJILatLng` object with a `DJILatLng` parameter.
     *  
     *  @param latLng A `DJILatLng` object
     */
    public DJILatLng(DJILatLng latLng) {
        this(latLng.latitude, latLng.longitude, latLng.altitude, latLng.accuracy, latLng.time, latLng.elapsedRealtimeNanos);
    }

    //Doc key: DJIMap_DJILatLng_getLatitude
    /**
     *  Gets the latitude of the map point.
     *  
     *  @return The latitude of the map point.
     */
    public double getLatitude() {
        return latitude;
    }

    //Doc key: DJIMap_DJILatLng_setLatitude
    /**
     *  Sets the latitude of the map point.
     *  
     *  @param latitude The latitude of the map point.
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    //Doc key: DJIMap_DJILatLng_getLongitude
    /**
     *  Gets the longitude of the map point.
     *  
     *  @return The longitude of the map point.
     */
    public double getLongitude() {
        return longitude;
    }

    //Doc key: DJIMap_DJILatLng_setLongitude
    /**
     *  Sets the longitude of the map point.
     *  
     *  @param longitude The longitude of the map point.
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    //Doc key: DJIMap_DJILatLng_getAltitude
    /**
     *  Gets the altitude of the map point.
     *  
     *  @return The altitude of the map point.
     */
    public double getAltitude() {
        return altitude;
    }

    //Doc key: DJIMap_DJILatLng_getAccuracy
    /**
     *  Gets the accuracy of the map point.
     *  
     *  @return The accuracy of the map point.
     */
    public float getAccuracy() {
        return accuracy;
    }

    //Doc key: DJIMap_DJILatLng_setAccuracy
    /**
     *  Sets the accuracy of the map point.
     *  
     *  @param accuracy The accuracy of the map point.
     */
    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }

    //Doc key: DJIMap_DJILatLng_getTime
    /**
     *  Gets the time of the map point.
     *  
     *  @return The time of the map point.
     */
    public long getTime() {
        return time;
    }

    //Doc key: DJIMap_DJILatLng_setTime
    /**
     *  Sets the time of the map point.
     *  
     *  @param time The accuracy of the point.
     */
    public void setTime(long time) {
        this.time = time;
    }

    //Doc key: DJIMap_DJILatLng_getElapsedRealtimeNanos
    /**
     *  Gets the time in elapsed real-time of the map point.
     *  
     *  @return The time in elapsed real-time of the point.
     */
    public long getElapsedRealtimeNanos() {
        return elapsedRealtimeNanos;
    }

    //Doc key: DJIMap_DJILatLng_setElapsedRealtimeNanos
    /**
     *  Sets the time in elapsed real-time of the map point.
     *  
     *  @param elapsedRealtimeNanos The time in elapsed real-time of the point.
     */
    public void setElapsedRealtimeNanos(long elapsedRealtimeNanos) {
        this.elapsedRealtimeNanos = elapsedRealtimeNanos;
    }

    //Doc key: DJIMap_DJILatLng_isAvailable
    /**
     * Determines if this map point is valid. A valid map point has a latitude in the range
     * [-90,90], has a longitude in the range [-180,180], and is not (0,0).
     *
     * @return `true` if this map point is valid.
     */
    public boolean isAvailable() {
        return DJIGpsUtils.isAvailable(latitude, longitude);
    }

    /**
     * {@hide}
     * @return
     */
    public boolean isFineAccuracy() {
        return isFineAccuracy(ACCURACY_GPS_FINE);
    }

    /**
     * {@hide}
     * @param meters
     * @return
     */
    public boolean isFineAccuracy(float meters) {
        return isFineAccuracy(accuracy, meters);
    }

    /**
     * {@hide}
     * @param accuracy
     * @param meter
     * @return
     */
    public static boolean isFineAccuracy(final float accuracy, final float meter) {
        return (0 < accuracy && accuracy <= meter);
    }

    /**
     * {@hide}
     * @param string
     * @return
     */
    public static DJILatLng valueOf(String string) {
        String[] s = string.split(",");
        return s.length != 2?null:new DJILatLng(Double.valueOf(s[0]), Double.valueOf(s[1]));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DJILatLng latLng = (DJILatLng) o;

        if (Double.compare(latLng.latitude, latitude) != 0) return false;
        if (Double.compare(latLng.longitude, longitude) != 0) return false;
        if (Double.compare(latLng.altitude, altitude) != 0) return false;
        if (Float.compare(latLng.accuracy, accuracy) != 0) return false;
        return time == latLng.time;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(latitude);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(longitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(altitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (accuracy != +0.0f ? Float.floatToIntBits(accuracy) : 0);
        result = 31 * result + (int) (time ^ (time >>> 32));
        return result;
    }

    @Override
    public String toString() {
        double lat = latitude;
        double lng = longitude;
        double altitudes = this.altitude;
        float accuracys = this.accuracy;
        return new StringBuilder(60).append("lat/lng: (").append(lat).append(",").append(lng).append(")")
                .append(" altitude=").append(altitudes).append(" accuracy=").append(accuracys).toString();
    }
}
