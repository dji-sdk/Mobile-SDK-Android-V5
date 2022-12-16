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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import dji.v5.utils.common.ContextUtil;
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
    private final List<LocationListener> locationListeners = new ArrayList<>();

    private final LocationListener locationManagerListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            for (LocationListener listener : locationListeners) {
                listener.onLocationChanged(location);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            for (LocationListener listener : locationListeners) {
                listener.onStatusChanged(provider, status, extras);
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
            for (LocationListener listener : locationListeners) {
                listener.onProviderEnabled(provider);
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            for (LocationListener listener : locationListeners) {
                listener.onProviderDisabled(provider);
            }
        }
    };

    private static class LazyHolder {
        private static final MobileGPSLocationUtil INSTANCE = new MobileGPSLocationUtil();
    }

    public static MobileGPSLocationUtil getInstance() {
        return MobileGPSLocationUtil.LazyHolder.INSTANCE;
    }

    private MobileGPSLocationUtil() {
    }

    public void addLocationListener(LocationListener listener) {
        if (listener != null) {
            locationListeners.add(listener);
        }
    }

    public void removeLocationListener(LocationListener listener) {
        locationListeners.remove(listener);
    }

    public void clearAllLocationListener() {
        locationListeners.clear();
    }

    /**
     * Start receiving updates from the location manager
     */
    @SuppressWarnings("MissingPermission")
    public void startUpdateLocation() {
        Context context = ContextUtil.getContext();
        if (context == null) {
            return;
        }
        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
            if (locationManager == null){
                return;
            }

            // getting GPS status
            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            // if GPS Enabled get lat/long using GPS Services
            if (isGPSEnabled) {
                locationManager.removeUpdates(locationManagerListener);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES,
                        locationManagerListener);
            }
        } catch (SecurityException e) {
            LogUtils.e(TAG, e.getMessage());
        }
    }
}
