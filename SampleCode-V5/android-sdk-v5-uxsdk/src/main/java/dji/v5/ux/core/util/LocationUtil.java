/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dji.v5.ux.core.util;

import android.location.Location;

import java.util.Arrays;

/**
 * Utility class for location based calculations
 */
public final class LocationUtil {

    //region Properties
    private static final float MAXIMUM_DISTANCE = 100 * 1000f;
    private static final double MINIMUM_LAT_LONG = 1E-6;
    private static final double MAXIMUM_LATITUDE = 90.0d;
    private static final double MAXIMUM_LONGITUDE = 180.0d;
    //endregion

    private LocationUtil() {
        //Util class
    }

    //region Coordinate calculation.

    /**
     * Check to ensure longitude is between 1E-6 and 180.0d
     *
     * @param longitude Value of longitude in decimal
     * @return boolean value `true` for valid longitude value, `false` otherwise
     */
    public static boolean checkLongitude(final double longitude) {
        final double absLongitude = Math.abs(longitude);
        return (MINIMUM_LAT_LONG < absLongitude && absLongitude <= MAXIMUM_LONGITUDE);
    }

    /**
     * Check to ensure latitude is between 1E-6 and 90.0d
     *
     * @param latitude Value of latitude in decimal
     * @return boolean value `true` for valid latitude value, `false` otherwise
     */
    public static boolean checkLatitude(final double latitude) {
        final double absLatitude = Math.abs(latitude);
        return (MINIMUM_LAT_LONG < absLatitude && absLatitude <= MAXIMUM_LATITUDE);
    }

    /**
     * Find the approximate distance in meters between two given GPS locations
     *
     * @param latitude1  Latitude of the first location
     * @param longitude1 Longitude of the first location
     * @param latitude2  Latitude of the second location
     * @param longitude2 Longitude of the second location
     * @return Distance between given locations in meters
     */
    public static float distanceBetween(final double latitude1,
                                        final double longitude1,
                                        final double latitude2,
                                        final double longitude2) {
        final float[] calculatedResult = new float[2];
        Arrays.fill(calculatedResult, 0.0f);
        Location.distanceBetween(latitude1, longitude1, latitude2, longitude2, calculatedResult);
        if (calculatedResult[0] <= 0 || calculatedResult[0] > MAXIMUM_DISTANCE) {
            calculatedResult[0] = 0;
        }
        return calculatedResult[0];
    }

    /**
     * Find the approximate distance and the angle between the two given GPS locations
     *
     * @param latitude1  Latitude of the first location
     * @param longitude1 Longitude of the first location
     * @param latitude2  Latitude of the second location
     * @param longitude2 Longitude of the second location
     * @return A float array containing the angle at index 0 and the distance at index 1
     */
    public static float[] calculateAngleAndDistance(final double latitude1,
                                                    final double longitude1,
                                                    final double latitude2,
                                                    final double longitude2) {
        final float[] calculatedResult = new float[2];
        Arrays.fill(calculatedResult, 0.0f);
        final float distance = distanceBetween(latitude1, longitude1, latitude2, longitude2);
        if (distance <= 0) {
            calculatedResult[0] = 0;
        } else {
            final float distance1 = distanceBetween(latitude1, longitude2, latitude2, longitude2);
            double angle = Math.toDegrees(Math.asin(distance1 / distance));
            if (latitude2 > latitude1) {
                if (longitude2 <= longitude1) {
                    angle = 180 - angle;
                }
            } else {
                if (longitude2 <= longitude1) {
                    angle = 180 + angle;
                } else {
                    angle = 360 - angle;
                }
            }

            if (Double.isNaN(angle)) {
                angle = 0.0;
            }
            calculatedResult[0] = (float) angle;
        }
        calculatedResult[1] = distance;

        return calculatedResult;
    }
    //endregion

}