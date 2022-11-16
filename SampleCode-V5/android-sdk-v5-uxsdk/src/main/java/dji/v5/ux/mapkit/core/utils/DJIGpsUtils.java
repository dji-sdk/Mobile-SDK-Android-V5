package dji.v5.ux.mapkit.core.utils;

import dji.v5.ux.mapkit.core.Mapkit;
import dji.v5.ux.mapkit.core.models.DJILatLng;

/**
 * Created by joeyang on 5/25/17.
 */

public class DJIGpsUtils {

    private static final double ZERO_DEBOUNCE_THRESHOLD = 0.00000001d;

    // public static boolean OPEN = true;
    private static final double M_PI = Math.PI;

    private static final DeltaLatLngCache sDeltaLatLngCache = new DeltaLatLngCache();

    private DJIGpsUtils(){}

    private static double transformLat(double x, double y) {
        double ret = -100.0 + 2.0*x + 3.0*y + 0.2*y*y + 0.1*x*y + 0.2*Math.sqrt(Math.abs(x));
        ret += (20.0*Math.sin(6.0*x*Math.PI) + 20.0*Math.sin(2.0*x*Math.PI)) * 2.0 / 3.0;
        ret += (20.0*Math.sin(y*Math.PI) + 40.0*Math.sin(y/3.0*Math.PI)) * 2.0 / 3.0;
        ret += (160.0*Math.sin(y/12.0*Math.PI) + 320*Math.sin(y*Math.PI/30.0)) * 2.0 / 3.0;
        return ret;
    }

    private static double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0*y + 0.1*x*x + 0.1*x*y + 0.1*Math.sqrt(Math.abs(x));
        ret += (20.0*Math.sin(6.0*x*Math.PI) + 20.0*Math.sin(2.0*x*Math.PI)) * 2.0 / 3.0;
        ret += (20.0*Math.sin(x*Math.PI) + 40.0*Math.sin(x/3.0*Math.PI)) * 2.0 / 3.0;
        ret += (150.0*Math.sin(x/12.0*Math.PI) + 300.0*Math.sin(x/30.0*Math.PI)) * 2.0 / 3.0;
        return ret;
    }

    private static DJILatLng delta(DJILatLng source) {
        double dLat;
        double dLng;
        double a = 6378245.0;
        double ee = 0.00669342162296594323;
        dLat = transformLat(source.longitude-105.0, source.latitude-35.0);
        dLng = transformLon(source.longitude-105.0, source.latitude-35.0);
        double radLat = source.latitude / 180.0 * M_PI;
        double magic = Math.sin(radLat);
        magic = 1 - ee*magic*magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * M_PI);
        dLng = (dLng * 180.0) / (a / sqrtMagic * Math.cos(radLat) * M_PI);
        return new DJILatLng(dLat, dLng);
    }

    private static DeltaLatLngCache delta(double latitude, double longitude) {
        double dLat;
        double dLng;
        double a = 6378245.0;
        double ee = 0.00669342162296594323;
        dLat = transformLat(longitude-105.0, latitude-35.0);
        dLng = transformLon(longitude-105.0, latitude-35.0);
        double radLat = latitude / 180.0 * M_PI;
        double magic = Math.sin(radLat);
        magic = 1 - ee*magic*magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * M_PI);
        dLng = (dLng * 180.0) / (a / sqrtMagic * Math.cos(radLat) * M_PI);
        sDeltaLatLngCache.latitude = dLat;
        sDeltaLatLngCache.longitude = dLng;
        return sDeltaLatLngCache;
    }

    /**
     * 在中国大陆、香港、澳门，则需要将传进来的WGS坐标转换为GCJ坐标（目前只用于高德地图和定位）
     * @param source
     * @return
     */
    public static DJILatLng wgs2gcjInChina(DJILatLng source) {
        if (!Mapkit.isInMainlandChina()
                && !Mapkit.isInHongKong()
                && !Mapkit.isInMacau()) {
            return source;
        }
        DeltaLatLngCache cache = delta(source.getLatitude(), source.getLongitude());
        double latitude = source.getLatitude() + cache.latitude;
        double longitude = source.getLongitude() + cache.longitude;
        return new DJILatLng(latitude, longitude, source.getAltitude(), source.getAccuracy(), source.getTime());
    }

    /**
     * 在中国大陆，则需要将传进来的WGS坐标转换为GCJ坐标（目前只用于谷歌、HERE地图和定位）
     * @param source
     * @return
     */
    public static DJILatLng wgs2gcjJustInMainlandChina(DJILatLng source) {
        if (!Mapkit.isInMainlandChina()) {
            return source;
        }
        DeltaLatLngCache cache = delta(source.getLatitude(), source.getLongitude());
        double latitude = source.getLatitude() + cache.latitude;
        double longitude = source.getLongitude() + cache.longitude;
        return new DJILatLng(latitude, longitude, source.getAltitude(), source.getAccuracy(), source.getTime());
    }

    /**
     * 在中国大陆、香港、澳门，则认为传进来的坐标是GCJ，需要转换为WGS坐标（目前只用于高德地图和定位）
     * @param source
     * @return
     */
    public static DJILatLng gcj2wgsInChina(DJILatLng source) {
        if (!Mapkit.isInMainlandChina()
                && !Mapkit.isInHongKong()
                && !Mapkit.isInMacau()) {
            return source;
        }
        DJILatLng latLng = delta(source);
        double latitude = source.latitude - latLng.latitude;
        double longitude = source.longitude - latLng.longitude;
        return new DJILatLng(latitude, longitude, source.getAltitude(), source.getAccuracy(), source.getTime());
    }

    /**
     * 在中国大陆，则认为传进来的坐标是GCJ，需要转换为WGS坐标（目前只用于谷歌、HERE地图和定位）
     * @param source
     * @return
     */
    public static DJILatLng gcj2wgsJustInMainlandChina(DJILatLng source) {
        if (!Mapkit.isInMainlandChina()) {
            return source;
        }
        DJILatLng latLng = delta(source);
        double latitude = source.latitude - latLng.latitude;
        double longitude = source.longitude - latLng.longitude;
        return new DJILatLng(latitude, longitude, source.getAltitude(), source.getAccuracy(), source.getTime());
    }

//    public static DJILatLng wgs2gcjMust(DJILatLng source) {
//        if (!IsInsideChinaMust(source)) {
//            return source;
//        }
//        DJILatLng latLng = delta(source);
//        double latitude = source.latitude + latLng.latitude;
//        double longitude = source.longitude + latLng.longitude;
//        return new DJILatLng(latitude, longitude, source.getAltitude(), source.getAccuracy(), source.getTime());
//    }
//
//    public static DJILatLng gcj2wgsMust(DJILatLng source) {
//        if (!Mapkit.isInMainlandChina()) {
//            return source;
//        }
//        DJILatLng latLng = delta(source);
//        double latitude = source.latitude - latLng.latitude;
//        double longitude = source.longitude - latLng.longitude;
//        return new DJILatLng(latitude, longitude, source.getAltitude(), source.getAccuracy(), source.getTime());
//    }
//
//    public static DJILatLng gcj2wgs_exact(DJILatLng source) {
//        double latitude = 0;
//        double longitude = 0;
//        double initDelta = 0.01;
//        double threshold = 0.000001;
//        double dLat = initDelta, dLng = initDelta;
//        double mLat = source.latitude-dLat, mLng = source.longitude-dLng;
//        double pLat = source.latitude+dLat, pLng = source.longitude+dLng;
//
//        for (int i = 0; i < 30; i++) {
//            latitude = (mLat+pLat)/2;
//            longitude = (mLng+pLng)/2;
//            DJILatLng latLng = new DJILatLng(latitude, longitude);
//            DJILatLng tmp = wgs2gcjInChina(new DJILatLng(latitude, longitude));
//            dLat = tmp.latitude - source.latitude;
//            dLng = tmp.longitude - source.longitude;
//            if ((Math.abs(dLat) < threshold) && (Math.abs(dLng) < threshold)) {
//                return latLng;
//            }
//            if (dLat > 0) {
//                pLat = latitude;
//            } else {
//                mLat = latitude;
//            }
//            if (dLng > 0) {
//                pLng = longitude;
//            } else {
//                mLng = longitude;
//            }
//        }
//        return new DJILatLng(latitude, longitude, source.getAltitude(), source.getAccuracy(), source.getTime());
//    }

    public static double distance(double latA, double lngA, double latB, double lngB) {
        double earthR = 6371000;
        double x = Math.cos(latA*M_PI/180) * Math.cos(latB*M_PI/180) * Math.cos((lngA-lngB)*M_PI/180);
        double y = Math.sin(latA*M_PI/180) * Math.sin(latB*M_PI/180);
        double s = x + y;
        if (s > 1) {
            s = 1;
        }
        if (s < -1) {
            s = -1;
        }
        double alpha = Math.acos(s);
        double distance = alpha * earthR;
        return distance;
    }

    public static double distance(DJILatLng latLngA, DJILatLng latLngB) {
        return distance(latLngA.latitude, latLngA.longitude, latLngB.latitude, latLngB.longitude);
    }

    public static boolean isAvailable(double latitude, double longitude) {
        boolean result = Math.abs(latitude) <= 90
                && Math.abs(longitude) <= 180
                && !(isZero(latitude) && isZero(longitude));
        return result;
    }

    private static boolean isZero(double value) {
        return -ZERO_DEBOUNCE_THRESHOLD <= value && value <= ZERO_DEBOUNCE_THRESHOLD;
    }

//    private static class Rectangle
//    {
//        public double West;
//        public double North;
//        public double East;
//        public double South;
//        public Rectangle(double latitude1, double longitude1, double latitude2, double longitude2)
//        {
//            this.West = Math.min(longitude1, longitude2);
//            this.North = Math.max(latitude1, latitude2);
//            this.East = Math.max(longitude1, longitude2);
//            this.South = Math.min(latitude1, latitude2);
//        }
//    }

//    private static Rectangle[] region = new Rectangle[]
//            {
//                    new Rectangle(49.220400, 079.446200, 42.889900, 096.330000),
//                    new Rectangle(54.141500, 109.687200, 39.374200, 135.000200),
//                    new Rectangle(42.889900, 073.124600, 29.529700, 124.143255),
//                    new Rectangle(29.529700, 082.968400, 26.718600, 097.035200),
//                    new Rectangle(29.529700, 097.025300, 20.414096, 124.367395),
//                    new Rectangle(20.414096, 107.975793, 17.871542, 111.744104),
//            };
//    private static Rectangle[] exclude = new Rectangle[]
//            {
//                    new Rectangle(25.398623, 119.921265, 21.785006, 122.497559),
//                    new Rectangle(22.284000, 101.865200, 20.098800, 106.665000),
//                    new Rectangle(21.542200, 106.452500, 20.487800, 108.051000),
//                    new Rectangle(55.817500, 109.032300, 50.325700, 119.127000),
//                    new Rectangle(55.817500, 127.456800, 49.557400, 137.022700),
//                    new Rectangle(44.892200, 131.266200, 42.569200, 137.022700),
//            };

//    public static boolean IsInsideChina(DJILatLng pos)
//    {
//        if(!OPEN) {
//            return false;
//        }
//        for (int i = 0; i < region.length; i++)
//        {
//            if (InRectangle(region[i], pos))
//            {
//                for (int j = 0; j < exclude.length; j++)
//                {
//                    if (InRectangle(exclude[j], pos))
//                    {
//                        return false;
//                    }
//                }
//                return true;
//            }
//        }
//        return false;
//    }

//    public static double getBearing(double lat1, double long1, double lat2, double long2) {
//        return (bearing(lat2, long2, lat1, long1) + 180.0) % 360;
//    }

//    private static double bearing(double lat1, double long1, double lat2, double long2) {
//        double degToRad = Math.PI / 180.0;
//        double phi1 = lat1 * degToRad;
//        double phi2 = lat2 * degToRad;
//        double lam1 = long1 * degToRad;
//        double lam2 = long2 * degToRad;
//
//        return Math.atan2(Math.sin(lam2 - lam1) * Math.cos(phi2),
//                Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(lam2 - lam1)
//        ) * 180 / Math.PI;
//    }
//
//
//
//    public static boolean IsInsideChinaMust(DJILatLng pos)
//    {
//        for (int i = 0; i < region.length; i++)
//        {
//            if (InRectangle(region[i], pos))
//            {
//                for (int j = 0; j < exclude.length; j++)
//                {
//                    if (InRectangle(exclude[j], pos))
//                    {
//                        return false;
//                    }
//                }
//                return true;
//            }
//        }
//        return false;
//    }

//    private static boolean InRectangle(Rectangle rect, DJILatLng pos)
//    {
//        return rect.West <= pos.longitude && rect.East >= pos.longitude && rect.North >= pos.latitude && rect.South <= pos.latitude;
//    }

    private static class DeltaLatLngCache {
        private double latitude;
        private double longitude;
    }
}
