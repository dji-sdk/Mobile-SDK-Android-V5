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

import android.content.Context;
import android.location.LocationListener;
import android.location.LocationManager;

import androidx.annotation.Nullable;
import dji.v5.utils.common.LogUtils;

import static android.content.Context.LOCATION_SERVICE;

/**
 * Util class to use location manager to get the mobile device's GPS location.
 */
public class MobileGPSLocationUtil {
    //region properties

    // The minimum distance to change updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1;
    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000;

    private static final String TAG = "MobileGPSLocationUtil";
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Context activityContext;

    //endregion

    //region public methods

    /**
     * Creates a MobileGPSLocationUtil instance
     *
     * @param context  A {@link Context} object used to retrieve the location manager
     * @param listener A {@link LocationListener} whose
     *                 {@link LocationListener#onLocationChanged} method will be called for
     *                 each location update
     */
    public MobileGPSLocationUtil(@Nullable Context context, @Nullable LocationListener listener) {
        activityContext = context;
        locationListener = listener;
    }

    /**
     * Start receiving updates from the location manager
     */
    @SuppressWarnings("MissingPermission")
    public void startUpdateLocation() {
        if (activityContext == null || locationListener == null) {
            return;
        }

        try {
            locationManager = (LocationManager) activityContext.getSystemService(LOCATION_SERVICE);

            // getting GPS status
            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            // if GPS Enabled get lat/long using GPS Services
            if (isGPSEnabled) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES,
                        locationListener);
            }
        } catch (SecurityException e) {
            LogUtils.e(TAG, e.getMessage());
        }
    }

    /**
     * Stop receiving updates from the location manager
     */
    @SuppressWarnings("MissingPermission")
    public void stopUpdateLocation() {
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (SecurityException e) {
                LogUtils.e(TAG, e.getMessage());
            }
        }
    }
    //endregion
}
