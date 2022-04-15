/**
 * @file_name : GpsUtils.java
 * @package_name : dji.gs.utils
 * @Data : 2014-2-19 下午4:28:25
 * @author : tony.zhang
 * <p>
 * 可以参考：http://nightfarmer.github.io/2016/12/01/GPSUtil/
 * Copyright (c) 2014, DJI All Rights Reserved.
 */

package dji.v5.ux.core.util;

import android.graphics.Path;
import android.graphics.Region;
import android.location.Location;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;

import dji.sdk.keyvalue.value.common.LocationCoordinate2D;
import dji.sdk.keyvalue.value.common.LocationCoordinate3D;
import dji.sdk.keyvalue.value.flightcontroller.AirSenseDirection;

public class GpsUtils {

    public static boolean OPEN = true;
    private static final double M_PI = Math.PI;
    private static final float[] distanceResult = new float[2];

    private static double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * Math.PI) + 40.0 * Math.sin(y / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * Math.PI) + 320 * Math.sin(y * Math.PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    private static double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * Math.PI) + 40.0 * Math.sin(x / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * Math.PI) + 300.0 * Math.sin(x / 30.0 * Math.PI)) * 2.0 / 3.0;
        return ret;
    }

    private static LocationCoordinate2D delta(LocationCoordinate2D source) {
        double dLat;
        double dLng;
        double a = 6378245.0;
        double ee = 0.00669342162296594323;
        dLat = transformLat(source.getLongitude() - 105.0, source.getLatitude() - 35.0);
        dLng = transformLon(source.getLongitude() - 105.0, source.getLatitude() - 35.0);
        double radLat = source.getLatitude() / 180.0 * M_PI;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * M_PI);
        dLng = (dLng * 180.0) / (a / sqrtMagic * Math.cos(radLat) * M_PI);
        return new LocationCoordinate2D(dLat, dLng);
    }

//    private static DJILatLng delta(DJILatLng source) {
//        double dLat;
//        double dLng;
//        double a = 6378245.0;
//        double ee = 0.00669342162296594323;
//        dLat = transformLat(source.longitude-105.0, source.latitude-35.0);
//        dLng = transformLon(source.longitude-105.0, source.latitude-35.0);
//        double radLat = source.latitude / 180.0 * M_PI;
//        double magic = Math.sin(radLat);
//        magic = 1 - ee*magic*magic;
//        double sqrtMagic = Math.sqrt(magic);
//        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * M_PI);
//        dLng = (dLng * 180.0) / (a / sqrtMagic * Math.cos(radLat) * M_PI);
//        return new DJILatLng(dLat, dLng);
//    }
//
//    public static DJILatLng wgs2gcj(DJILatLng source) {
//        if (!IsInsideChina(source)) {
//            return source;
//        }
//        DJILatLng latLng = delta(source);
//        double latitude = source.latitude + latLng.latitude;
//        double longitude = source.longitude + latLng.longitude;
//        return new DJILatLng(latitude, longitude);
//    }

    public static LocationCoordinate2D wgs2gcj(LocationCoordinate2D source) {
        if (!IsInsideChina(source)) {
            return source;
        }
        LocationCoordinate2D latLng = delta(source);
        double latitude = source.getLatitude() + latLng.getLatitude();
        double longitude = source.getLongitude() + latLng.getLongitude();
        return new LocationCoordinate2D(latitude, longitude);
    }

//    public static DJILatLng gcj2wgs(DJILatLng source) {
//        if (!IsInsideChina(source)) {
//            return source;
//        }
//        DJILatLng latLng = delta(source);
//        double latitude = source.latitude - latLng.latitude;
//        double longitude = source.longitude - latLng.longitude;
//        return new DJILatLng(latitude, longitude);
//    }

    public static LocationCoordinate2D gcj2wgs(LocationCoordinate2D source) {
        if (!IsInsideChina(source)) {
            return source;
        }
        LocationCoordinate2D latLng = delta(source);
        double latitude = source.getLatitude() - latLng.getLatitude();
        double longitude = source.getLongitude() - latLng.getLongitude();
        return new LocationCoordinate2D(latitude, longitude);
    }

    public static LocationCoordinate2D gcj2wgsMust(LocationCoordinate2D source) {
        LocationCoordinate2D latLng = delta(source);
        double latitude = source.getLatitude() - latLng.getLatitude();
        double longitude = source.getLongitude() - latLng.getLongitude();
        return new LocationCoordinate2D(latitude, longitude);
    }
//
//    public static DJILatLng wgs2gcjMust(DJILatLng source) {
//        DJILatLng latLng = delta(source);
//        double latitude = source.latitude + latLng.latitude;
//        double longitude = source.longitude + latLng.longitude;
//        return new DJILatLng(latitude, longitude);
//    }

    public static LocationCoordinate2D wgs2gcjMust(LocationCoordinate2D source) {
        LocationCoordinate2D latLng = delta(source);
        double latitude = source.getLatitude() + latLng.getLatitude();
        double longitude = source.getLongitude() + latLng.getLongitude();
        return new LocationCoordinate2D(latitude, longitude);
    }

    public static LocationCoordinate3D wgs2gcjMust(LocationCoordinate3D source) {
        LocationCoordinate2D latLng = delta(new LocationCoordinate2D(source.getLatitude(), source.getLongitude()));
        double latitude = source.getLatitude() + latLng.getLatitude();
        double longitude = source.getLongitude() + latLng.getLongitude();
        return new LocationCoordinate3D(latitude, longitude, source.getAltitude());
    }

//    public static DJILatLng gcj2wgsMust(DJILatLng source) {
//        DJILatLng latLng = delta(source);
//        double latitude = source.latitude - latLng.latitude;
//        double longitude = source.longitude - latLng.longitude;
//        return new DJILatLng(latitude, longitude, source.altitude, source.accuracy);
//    }

    public static LocationCoordinate3D gcj2wgsMust(LocationCoordinate3D source) {
        LocationCoordinate2D latLng = delta(new LocationCoordinate2D(source.getLatitude(), source.getLongitude()));
        double latitude = source.getLatitude() - latLng.getLatitude();
        double longitude = source.getLongitude() - latLng.getLongitude();
        return new LocationCoordinate3D(latitude, longitude, source.getAltitude());
    }
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
//            DJILatLng tmp = wgs2gcj(new DJILatLng(latitude, longitude));
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
//        return new DJILatLng(latitude, longitude);
//    }

    public static double distance(double latA, double lngA, double latB, double lngB) {
        double earthR = 6371000;
        double x = Math.cos(latA * M_PI / 180) * Math.cos(latB * M_PI / 180) * Math.cos((lngA - lngB) * M_PI / 180);
        double y = Math.sin(latA * M_PI / 180) * Math.sin(latB * M_PI / 180);
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

//    public static float distanceBetween(DJILatLng p1, DJILatLng p2) {
//        return distanceBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude);
//    }

    /**
     * 计算两个3维坐标点的相对距离
     *
     * @param coordinate1 坐标点1
     * @param coordinate2 坐标点2
     * @return 相对距离
     */
    public static double distance3D(LocationCoordinate3D coordinate1, LocationCoordinate3D coordinate2) {
        float distance2D = distanceBetween(coordinate1.getLatitude(), coordinate1.getLongitude(),
                coordinate2.getLatitude(), coordinate2.getLongitude());
        double height = Math.abs(coordinate1.getAltitude() - coordinate2.getAltitude());
        return Math.sqrt(distance2D * distance2D + height * height);
    }

    /**
     * 将十进制经纬度转化成度分秒
     *
     * @param value    十进制坐标值
     * @param accuracy 秒精度
     * @return 度分秒
     */
    public static double[] transformGpsDMS(double value, int accuracy) {
        //转化成度分秒
        double[] gpsDMSValue = new double[3];
        gpsDMSValue[0] = (int) value;
        value = Math.abs(value - gpsDMSValue[0]) * 60;
        gpsDMSValue[1] = (int) (value);
        gpsDMSValue[2] = (float) ((value - (int) gpsDMSValue[1]) * 60);
        //四舍五入
        gpsDMSValue[2] = BigDecimal.valueOf(gpsDMSValue[2]).setScale(accuracy, RoundingMode.HALF_UP).doubleValue();
        //进位
        if (gpsDMSValue[2] == 60.0) {
            gpsDMSValue[1] += 1;
            gpsDMSValue[2] = 0;
        }
        if (gpsDMSValue[1] == 60.0) {
            gpsDMSValue[0] += gpsDMSValue[0] >= 0 ? 1 : -1;
            gpsDMSValue[1] = 0;
        }
        return gpsDMSValue;
    }

    /**
     * 将十进制经纬度转化成度分
     *
     * @param value    十进制坐标值
     * @param accuracy 分精度
     * @return 度分
     */
    public static double[] transformGpsDM(double value, int accuracy) {
        //转化成度分秒
        double[] gpsDMValue = new double[2];
        gpsDMValue[0] = (int) value;
        value = Math.abs(value - gpsDMValue[0]) * 60;

        //四舍五入
        gpsDMValue[1] = BigDecimal.valueOf(value).setScale(accuracy, RoundingMode.HALF_UP).doubleValue();

        //进位
        if (gpsDMValue[1] == 60.0) {
            gpsDMValue[0] += gpsDMValue[0] >= 0 ? 1 : -1;
            gpsDMValue[1] = 0;
        }
        return gpsDMValue;
    }

    /**
     * 将十进制经纬度转化成度分秒,默认秒精确为3
     *
     * @param value
     * @return
     */
    public static double[] transformGpsDMS(double value) {
        return transformGpsDMS(value, 3);
    }

    /**
     * 将GPS度分秒转化成十进制经纬度
     *
     * @param degree int 经度[-180,180] 纬度[-90,90]
     * @param minute int 范围: [0,60)
     * @param second float 范围: [0,60)
     * @return
     */
    public static double getGpsValue(double degree, double minute, double second, int accuracy) {
        return BigDecimal.valueOf((degree >= 0 ? 1 : -1) * (Math.abs(degree) + minute / 60 + second / 3600))
                .setScale(accuracy, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * 将GPS度分秒转化成十进制经纬度，默认精度为9位
     *
     * @param degree
     * @param minute
     * @param second
     * @return
     */
    public static double getGpsValue(double degree, double minute, double second) {
        return getGpsValue(degree, minute, second, 9);
    }

    /**
     * Description : 计算两点（经纬度）的距离
     *
     * @param latitude1
     * @param longitude1
     * @param latitude2
     * @param longitude2
     * @return
     * @author : gashion.fang
     * @date : 2015-2-9 上午10:14:35
     */
    public static float distanceBetween(double latitude1, double longitude1, double latitude2, double longitude2) {
        float[] res = new float[2];
        Location.distanceBetween(latitude1, longitude1, latitude2, longitude2, res);
        if (res[0] <= 0) {
            res[0] = 0;
        }
        return res[0];
    }

    private static class Rectangle {
        public double West;
        public double North;
        public double East;
        public double South;

        public Rectangle(double latitude1, double longitude1, double latitude2, double longitude2) {
            this.West = Math.min(longitude1, longitude2);
            this.North = Math.max(latitude1, latitude2);
            this.East = Math.max(longitude1, longitude2);
            this.South = Math.min(latitude1, latitude2);
        }
    }

    private static Rectangle[] region = new Rectangle[]
            {
                    new Rectangle(49.220400, 079.446200, 42.889900, 096.330000),
                    new Rectangle(54.141500, 109.687200, 39.374200, 135.000200),
                    new Rectangle(42.889900, 073.124600, 29.529700, 124.143255),
                    new Rectangle(29.529700, 082.968400, 26.718600, 097.035200),
                    new Rectangle(29.529700, 097.025300, 20.414096, 124.367395),
                    new Rectangle(20.414096, 107.975793, 17.871542, 111.744104),
            };
    private static Rectangle[] exclude = new Rectangle[]
            {
                    new Rectangle(25.398623, 119.921265, 21.785006, 122.497559),
                    new Rectangle(22.284000, 101.865200, 20.098800, 106.665000),
                    new Rectangle(21.542200, 106.452500, 20.487800, 108.051000),
                    new Rectangle(55.817500, 109.032300, 50.325700, 119.127000),
                    new Rectangle(55.817500, 127.456800, 49.557400, 137.022700),
                    new Rectangle(44.892200, 131.266200, 42.569200, 137.022700),
            };

//    public static boolean IsInsideChina(DJILatLng pos)
//    {
//    	if(!OPEN) return false;
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

    public static boolean IsInsideChina(LocationCoordinate2D pos) {
        if (!OPEN) return false;
        for (int i = 0; i < region.length; i++) {
            if (InRectangle(region[i], pos)) {
                for (int j = 0; j < exclude.length; j++) {
                    if (InRectangle(exclude[j], pos)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

//    private static boolean InRectangle(Rectangle rect, DJILatLng pos)
//    {
//        return rect.West <= pos.longitude && rect.East >= pos.longitude && rect.North >= pos.latitude && rect.South <= pos.latitude;
//    }

    private static boolean InRectangle(Rectangle rect, LocationCoordinate2D pos) {
        return rect.West <= pos.getLongitude() && rect.East >= pos.getLongitude() && rect.North >= pos.getLatitude() && rect.South <= pos.getLatitude();
    }

    public static boolean isInAmerican(double lat, double lng) {
        //先大致判断一下
        if (lat > 25 && lat < 49 && lng > -130 && lng < -70) {//美国本土
            return true;
        } else if (lat > 60 && lat < 70 && lng > -170 && lng < -140) {//阿拉斯加
            return true;
        } else if (lat > 19 && lat < 23 && lng > -180 && lng < -150) {//夏威夷群岛
            return true;
        }
        return false;
    }

//    private static List<CountryBorder.CountryItem> mlist = null;
//
//    public static boolean isInJapanOffline(double lat, double lon){
//        return isInCountryOffline("JPN", lat, lon);
//    }
//
//    public static boolean isInChinaOffline(double lat, double lon){
//        return isInCountryOffline("CHN", lat, lon);
//    }
//
//    public static boolean isInAmeriaOffline(double lat, double lon){
//        return isInCountryOffline("USA", lat, lon);
//    }
//
//    public static boolean isInCountryOffline(String countryId, double lat, double lon){
//        ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> mBorders = null;
//        for (CountryBorder.CountryItem item : mlist) {
//            if (item.id.equals(countryId)) {
//                mBorders = item.geometry.coordinates;
//            }
//        }
//
//        boolean result = false;
//        for(ArrayList<ArrayList<ArrayList<Double>>> item : mBorders) {
//            ArrayList<ArrayList<Double>> borders = item.get(0);
//            if (inPolygon(lat, lon, borders)) {
//                return true;
//            }
//        }
//        return false;
//    }

    private static boolean inPolygon(double lat, double lon, ArrayList<ArrayList<Double>> polygonLocations) {
        if (polygonLocations.size() <= 3) {
            return false;
        }
        boolean result = false;
        int j = polygonLocations.size() - 1;
        for (int i = 0; i < polygonLocations.size(); j = i++) {

            if (Math.abs(polygonLocations.get(j).get(1) - polygonLocations.get(i).get(1)) < 1e-6) {
                continue;
            }
            boolean result1 = (polygonLocations.get(i).get(1) > lat) != (polygonLocations.get(j).get(1) > lat);

            double lng = (polygonLocations.get(j).get(0) - polygonLocations.get(i).get(0))
                    * (lat - polygonLocations.get(j).get(1))
                    / (polygonLocations.get(j).get(1)
                    - polygonLocations.get(i).get(1))
                    + polygonLocations.get(i).get(0);

            if (result1 && (lon < lng)) {
                result = !result;
            }
        }
        return result;
    }

    /**
     * Description : 检查纬度是否正确
     *
     * @author : gashion.fang
     * @date : 2014-12-20 下午3:05:42
     * @param latitude
     * @return
     */
    public static boolean checkLatitude(final double latitude) {

        final double absLatitude = Math.abs(latitude);
        return (1E-6 < absLatitude && absLatitude <= 90.0d);
    }

    /**
     * Description : 检查经度是否正确
     *
     * @author : gashion.fang
     * @date : 2014-12-20 下午3:07:20
     * @param longitude
     * @return
     */
    public static boolean checkLongitude(final double longitude) {
        final double absLongitude = Math.abs(longitude);
        return (1E-6 < absLongitude && absLongitude <= 180.0d);
    }

    /**
     * Description : 计算两点距离
     *
     * @author : gashion.fang
     * @date : 2015-2-27 下午12:10:45
     * @param latitue1
     * @param longtitue1
     * @param latitue2
     * @param longtitue2
     * @return
     */
    public static float distanceBetweenNoMax(final double latitue1, final double longtitue1, final double latitue2,
                                             final double longtitue2) {
        Arrays.fill(distanceResult, 0.0f);
        Location.distanceBetween(latitue1, longtitue1, latitue2, longtitue2, distanceResult);
        if (distanceResult[0] <= 0) {
            distanceResult[0] = 0;
        }
        return distanceResult[0];
    }

    public static LocationCoordinate2D locationFrom(LocationCoordinate2D source, float distance, AirSenseDirection direction) {
        // 转换成弧度
        double latitue = Math.toRadians(source.getLatitude());
        double longtitue = Math.toRadians(source.getLongitude());
        double d = Math.toRadians(distance / (Math.cos(latitue) * 111000.0f));

        double dir = Math.PI / 2;
        switch (direction) {
            case NORTH:
                dir = Math.PI / 2;
                break;
            case NORTH_EAST:
                dir = Math.PI / 4;
                break;
            case EAST:
                dir = 0;
                break;
            case SOUTH_EAST:
                dir = -(Math.PI / 4);
                break;
            case SOUTH:
                dir = -(Math.PI / 2);
                break;
            case SOUTH_WEST:
                dir = -((3.0f / 4.0f) * Math.PI);
                break;
            case WEST:
                dir = Math.PI;
                break;
            case NORTH_WEST:
                dir = (3.0f / 4.0f) * Math.PI;
                break;
            default:
                break;
        }

        double newLatitue = d * Math.sin(dir) + latitue;
        double newLongitue = d * Math.cos(dir) + longtitue;
        return new LocationCoordinate2D(Math.toDegrees(newLatitue), Math.toDegrees(newLongitue));
    }

    public static float getDistance(LocationCoordinate2D p1, LocationCoordinate2D p2) {
        float[] results = new float[1];
        Location.distanceBetween(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude(), results);
        return results[0];
    }

    public static float[] getAllDistance(LocationCoordinate2D p1, LocationCoordinate2D p2) {
        float[] results = new float[3];
        Location.distanceBetween(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude(), results);
        return results;
    }

    /**
     * 判断经纬度是否在中国
     *
     * @param lat 纬度
     * @param lon 经度
     * @return boolean TRUE 不在中国  ； FALSE 在中国
     */
    public static boolean outOfChina(double lat, double lon) {
        // taiwai 返回TRUE
        if ((lat > 21.7569) && (lat < 25.942)) {
            if ((lon > 119.3) && (lon < 124.58)) {
                return true;
            }
        }


        //        if (lon < 72.004 || lon > 137.8347)
        //            return true;
        //        if (lat < 0.8293 || lat > 55.8271)
        //            return true;
        //        return false;

        return checkPoint((int) (lat), (int) (lon));
    }

    private static boolean checkPoint(int lat, int lon) {

        Path path = new Path();

        path.moveTo(D(48, 58, 42.64), D(87, 5, 59.19));

        path.lineTo(D(46, 43, 33.27), D(85, 25, 26.56));
        path.lineTo(D(47, 00, 18.85), D(83, 13, 32.25));
        path.lineTo(D(44, 51, 45.02), D(79, 52, 21.83));
        path.lineTo(D(42, 06, 38.75), D(80, 16, 38.32));
        path.lineTo(D(40, 26, 33.02), D(74, 52, 43.66));
        path.lineTo(D(38, 46, 42.06), D(73, 45, 47.54));
        path.lineTo(D(35, 40, 53.53), D(77, 17, 50.18));
        path.lineTo(D(35, 18, 19.61), D(80, 25, 3.57));
        path.lineTo(D(33, 47, 49.88), D(79, 4, 29.33));
        path.lineTo(D(31, 30, 12.49), D(78, 27, 15.10));
        path.lineTo(D(29, 56, 32.62), D(81, 15, 44.46));
        path.lineTo(D(27, 11, 56.65), D(89, 01, 43.60));
        path.lineTo(D(28, 12, 41.71), D(97, 27, 43.41));
        path.lineTo(D(25, 53, 39.08), D(98, 48, 8.70));
        path.lineTo(D(24, 39, 34.05), D(97, 28, 21.20));
        path.lineTo(D(23, 45, 59.97), D(97, 39, 54.56));
        path.lineTo(D(21, 06, 32.01), D(101, 21, 31.23));
        path.lineTo(D(22, 39, 01.59), D(103, 26, 4.0));
        path.lineTo(D(20, 16, 53.73), D(107, 49, 47.06));
        path.lineTo(D(15, 47, 06.87), D(108, 54, 19.46));
        path.lineTo(D(14, 53, 53.70), D(114, 38, 35.08));
        path.lineTo(D(21, 04, 46.90), D(121, 47, 25.12));
        path.lineTo(D(30, 04, 39.55), D(125, 46, 41.78));
        path.lineTo(D(39, 29, 30.38), D(123, 11, 52.45));
        path.lineTo(D(42, 46, 30.75), D(131, 10, 38.60));
        path.lineTo(D(48, 15, 27.43), D(134, 46, 49.54));
        path.lineTo(D(49, 29, 41.88), D(127, 50, 21.50));
        path.lineTo(D(53, 06, 13.32), D(125, 9, 54.19));
        path.lineTo(D(52, 52, 56.58), D(119, 52, 42.41));
        path.lineTo(D(48, 05, 29.20), D(115, 17, 35.36));
        path.lineTo(D(46, 32, 0.41), D(119, 44, 29.70));
        path.lineTo(D(44, 36, 38.94), D(111, 41, 41.13));
        path.lineTo(D(42, 13, 8.78), D(107, 12, 24.35));
        path.lineTo(D(42, 43, 53.70), D(96, 26, 11.34));
        path.lineTo(D(44, 44, 42.52), D(93, 47, 30.02));
        path.lineTo(D(45, 18, 42.86), D(90, 47, 27.32));
        path.lineTo(D(47, 45, 6.43), D(90, 10, 4.97));

        path.close();

        Region mRegion = new Region();
        Region clip1 = new Region();
        clip1.set(1 * TIMES, 73 * TIMES, 55 * TIMES, 138 * TIMES);

        boolean flag = mRegion.setPath(path, clip1);
        //Log.d("checkPoint", "setPath flag="+flag);

        boolean Result = mRegion.contains(lat, lon);

        if (Result) {
            return false;
        } else {
            return true;
        }
    }

    private static int TIMES = 1;

    private static float D(double degree, double minute, double second) {
        return (float) ((degree + (minute + (second / 60.0)) / 60.0) * TIMES);
    }

    /**
     * Description : 检查飞行器星数是否可用
     *
     * @param gpsNum
     * @return
     * @author : gashion.fang
     * @date : 2014-12-20 下午3:24:45
     */
    public static boolean checkGpsNumValid(final int gpsNum) {
        // TODO 暂时不支持P3
        // if (DJIProductManager.getInstance().getType() == ProductType.litchiC) {
        //     return (gpsNum >= 6 && gpsNum < 50);
        // } else {
        return (gpsNum >= 8 && gpsNum < 50);
        // }
    }

    // /**
    //  * Description : 用于设置Home点时的GPS有效性判断
    //  *
    //  * @return
    //  * @author : gashion.fang
    //  * @date : 2015-11-5 下午5:15:56
    //  */
    // public static boolean checkGpsValid() {
    //     boolean ret = false;
    //     final DataOsdGetPushCommon common = DataOsdGetPushCommon.getInstance();
    //     if (common.isGetted()) {
    //         if (common.getFlycVersion() < 6) {
    //             ret = checkGpsNumValid(common.getGpsNum());
    //         } else {
    //             ret = common.getGpsLevel() >= 3;
    //         }
    //     }
    //     return ret;
    // }

    /**
     * Description : 判断当前飞机GPS点是否可用
     *
     * @param flycVersion
     * @param gpsNum
     * @param gpsLevel
     * @return
     * @author : gashion.fang
     * @date : 2016-4-28 下午4:56:52
     */
    public static boolean checkGpsValid(final int flycVersion, final int gpsNum, final int gpsLevel) {
        if (flycVersion < 6) {
            return checkGpsNumValid(gpsNum);
        } else {
            return gpsLevel >= 3;
        }
    }

    /**
     * Description : 获取飞行器星数等级
     *
     * @param gpsNum
     * @return
     * @author : gashion.fang
     * @date : 2014-12-20 下午3:50:34
     */
    public static int getGpsLevel(final int gpsNum) {
        int level = 0;
        if (0 == gpsNum || gpsNum >= 50) {
            level = 0;
        } else if (gpsNum <= 7) {
            level = 1;
        } else if (gpsNum > 10) {
            level = 5;
        } else {
            level = gpsNum - 6;
        }
        return level;
    }

//    public static List<DJILatLng> pointConvert(List<com.dji.flysafe.mapkit.core.core.models.DJILatLng> polygonPoints) {
//
//        if(polygonPoints == null){
//            return new ArrayList<>();
//        }
//        List<DJILatLng> points = new ArrayList<>();
//        for (com.dji.flysafe.mapkit.core.core.models.DJILatLng point : polygonPoints) {
//            points.add(new DJILatLng(point.latitude, point.longitude));
//
//        }
//        return points;
//    }
//
//    public static DJILatLng convertDJILatLng(com.dji.flysafe.mapkit.core.core.models.DJILatLng latLng) {
//        DJILatLng djiLatLng = new DJILatLng(latLng.latitude,latLng.longitude,latLng.altitude);
//        return djiLatLng;
//    }

//    public static Location convertToLocation(DJILatLng latLng) {
//        Location location = new Location("DJILocationManager");
//        location.setLatitude(latLng.getLatitude());
//        location.setLongitude(latLng.getLongitude());
//        location.setAltitude(latLng.getAltitude());
//        location.setAccuracy(latLng.getAccuracy());
//        location.setTime(latLng.getTime());
//        return location;
//    }
//    public static Location convertToLocation(RcGPSInfo gpsInfo) {
//        Location location = new Location("DJILocationManager");
//        location.setLatitude(gpsInfo.getLocation().getLatitude());
//        location.setLongitude(gpsInfo.getLocation().getLongitude());
//        location.setAccuracy(gpsInfo.getAccuracy().floatValue());
//        Calendar calendar = Calendar.getInstance();
//        calendar.set(gpsInfo.getTime().getYear(),gpsInfo.getTime().getMonth(),gpsInfo.getTime().getDay(),gpsInfo.getTime().getHour(),gpsInfo.getTime().getMinute(),gpsInfo.getTime().getSecond());
//        location.setTime(calendar.getTime().getTime());
//        return location;
//    }

//    public static Location getCenter(List<Location> points) {
//        if (points == null || points.size() == 0) {
//            Location location = new Location("Location");
//            location.setLatitude(0);
//            location.setLongitude(0);
//            return location;
//        }
//
//        double minLat = points.get(0).getLatitude();
//        double minLng = points.get(0).getLongitude();
//        double maxLat = points.get(0).getLatitude();
//        double maxLng = points.get(0).getLongitude();
//        for (int i = 0; i != points.size(); ++i) {
//            double curLat = points.get(i).getLatitude();
//            double curLng = points.get(i).getLongitude();
//            if (curLat > maxLat) {
//                maxLat = curLat;
//            }
//            if (curLat < minLat) {
//                minLat = curLat;
//            }
//            if (curLng > maxLng) {
//                maxLng = curLng;
//            }
//            if (curLng < minLng) {
//                minLng = curLng;
//            }
//
//        }
//
//        return GpsUtils.convertToLocation(new DJILatLng((maxLat + minLat) / 2, (maxLng + minLng) / 2));
//
//    }

    /**
     * 由椭球高转换成海拔高
     * @param wgs84altitude 椭球高
     * @param latitude 经度
     * @param longitude 纬度
     * @return egm96Altitude 海拔高
     */
    public static double egm96Altitude(double wgs84altitude, double latitude, double longitude) {
        return wgs84altitude - getGeoidOffset(latitude, longitude);
    }

    public static float egm96AltitudeByUnit(double wgs84altitude, double latitude, double longitude) {
        return UnitUtils.getValueFromMetricByLength((float) egm96Altitude(wgs84altitude, latitude, longitude));
    }

    public static double wgs84Altitude(double egm96Altitude, double latitude, double longitude) {
        return egm96Altitude + getGeoidOffset(latitude, longitude);
    }

    public static float wgs84AltitudeByUnit(double egm96Altitude, double latitude, double longitude) {
        return UnitUtils.getValueFromMetricByLength((float) wgs84Altitude(egm96Altitude, latitude, longitude));
    }

    public static double getGeoidOffset(double latitude, double longitude) {
        return Double.isNaN(latitude) || Double.isNaN(longitude) ? 0 : GeoidManager.getInstance().geoidhEgm96(latitude, longitude);
    }
}

