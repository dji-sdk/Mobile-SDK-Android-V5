/**
 * @filename : DJIUnitUtil.java
 * @package : dji.pilot.publics.util
 * @date : 2015-2-10 下午3:08:56
 * @author : gashion.fang
 * <p>
 * Copyright (c) 2015, DJI All Rights Reserved.
 */

package dji.v5.ux.core.util;

import android.content.Context;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import dji.v5.utils.common.ContextUtil;

public class UnitUtils {

    //region Properties
    /**
     * meter to foot
     */
    public static final float LENGTH_METRIC2IMPERIAL = 3.2808f;
    /**
     * meter/s to
     */
    public static final float SPEED_METRIC2IMPERIAL = 2.2369f;
    /**
     * mile/h
     */
    public static final float SPEED_MS_TO_KMH = 3.6f;
    /**
     * meter to inch
     */
    public static final float LENGTH_METRIC2INCH = 39.4f;

    public static final float TEMPERATURE_K2C = 273.15f;

    /** pref values **/
    /**
     * 英制
     */
    public static final int UNIT_IMPERIAL = 0;
    /**
     * 公制千米
     */
    public static final int UNIT_METRIC_KM = 1;
    /*** 公制米 */
    public static final int UNIT_METRIC = 2;
    /**
     * 结束值，不可用
     */
    public static final int UNIT_ALL = 3;

    public static final int TEMPERATURE_FAHRENHEIT = 0; // 华氏度
    public static final int TEMPERATURE_CELSIUS = 1; // 摄氏度
    public static final int TEMPERATURE_KELVIN = 2; // 开氏度

    // 平方米
    public static final int AREA_UNIT_METER_SQUARE = 0;
    // 亩 注：英亩和亩不一样
    public static final int AREA_UNIT_ACRE = 1;
    // 公顷
    public static final int AREA_UNIT_HECTARE = 2;

    public static DecimalFormat sDecimalFormat = new DecimalFormat("###,###,###,###", new DecimalFormatSymbols(Locale.US));
    //endregion

    //region UnitType Enum
    public enum UnitType {
        METRIC("Metric", 0),
        IMPERIAL("Imperial", 1);

        private String stringValue;
        private int intValue;

        UnitType(String toString, int value) {
            stringValue = toString;
            intValue = value;
        }

        @Override
        public String toString() {
            return stringValue;
        }
    }
    //endregion

    //region APIs

    /**
     * Description : 温度转换（开氏度 - 摄氏度）
     *
     * @param kelvin
     * @return
     * @author : gashion.fang
     * @date : 2015-9-11 上午11:31:27
     */
    public static final float kelvinToCelsius(final float kelvin) {
        return (kelvin - TEMPERATURE_K2C);
    }

    /**
     * Description : 温度转换（摄氏度 - 开氏度）
     *
     * @param celsius
     * @return
     * @author : gashion.fang
     * @date : 2015-9-11 下午3:06:13
     */
    public static final float celsiusToKelvin(final float celsius) {
        return (celsius + TEMPERATURE_K2C);
    }

    /**
     * Description : 温度转换（摄氏度 - 华氏度）
     *
     * @param celsius
     * @return
     * @author : gashion.fang
     * @date : 2015-9-11 上午11:33:31
     */
    public static final float celsiusToFahrenheit(final float celsius) {
        return (celsius * 1.8f + 32);
    }

    /**
     * Description : 温度转换（华氏度 - 摄氏度）
     *
     * @param fahrenheit
     * @return
     * @author : gashion.fang
     * @date : 2015-11-27 下午12:07:26
     */
    public static final float fahrenheitToCelsius(final float fahrenheit) {
        return (fahrenheit - 32) / 1.8f;
    }

    /**
     * Description : 公制转换成英制(长度）
     *
     * @param value
     * @return
     * @author : gashion.fang
     * @date : 2014-12-20 下午6:04:52
     */
    public static float metricToImperialByLength(final float value) {
        return (value * LENGTH_METRIC2IMPERIAL);
    }

    /**
     * Description : 英制转换成公制（长度）
     *
     * @param value
     * @return
     * @author : gashion.fang
     * @date : 2015-2-10 下午3:06:22
     */
    public static float imperialToMetricByLength(final float value) {
        return (value / LENGTH_METRIC2IMPERIAL);
    }

    /**
     * Description : 公制转换成英制（速度）
     *
     * @param value
     * @return
     * @author : gashion.fang
     * @date : 2015-2-10 下午3:12:35
     */
    public static float metricToImperialBySpeed(final float value) {
        return (value * SPEED_METRIC2IMPERIAL);
    }

    /**
     * Description : 英制转换成公制（速度）
     *
     * @param value
     * @return
     * @author : gashion.fang
     * @date : 2015-2-10 下午3:14:58
     */
    public static float imperialToMetricBySpeed(final float value) {
        return (value / SPEED_METRIC2IMPERIAL);
    }

    /**
     * Description : 将公制转换到当前所需的单位
     *
     * @param value
     * @return
     * @author : tony.zhang
     * @date : 2015-2-10 下午3:42:23
     */
    public static float getValueFromMetricByLength(float value, UnitType type) {
        float result;
        if (!isMetric(type)) {
            result = metricToImperialByLength(value);
        } else {
            result = value;
        }
        return result;
    }

    /**
     * Description : 将公制转换到当前的系统单位
     *
     * @param value
     * @return
     */
    public static float getValueFromMetricByLength(float value) {
        return getValueFromMetricByLength(value, isMetricUnits() ? UnitType.METRIC : UnitType.IMPERIAL);
    }

    /**
     * Description : 将英制转换到当前所需的单位
     *
     * @param value
     * @return
     * @author : tony.zhang
     * @date : 2015-2-10 下午3:42:26
     */
    public static float getValueFromImperialByLength(float value, UnitType type) {
        float result;
        if (isMetric(type)) {
            result = imperialToMetricByLength(value);
        } else {
            result = value;
        }
        return result;
    }

    /**
     * Description : 将当前长度单位转换为公制
     *
     * @param value
     * @return
     */
    public static float getMetricFromCurrentUnitByLength(float value) {
        float result;
        if (!isMetricUnits()) {
            result = imperialToMetricByLength(value);
        } else {
            result = value;
        }
        return result;
    }

    /**
     * Description : 将当前单位转换到公制
     *
     * @param value
     * @return
     * @author : tony.zhang
     * @date : 2015-2-10 下午4:14:31
     */
    public static float getValueFromMetricBySpeed(float value, UnitType type) {
        float result;
        if (!isMetric(type)) {
            result = metricToImperialBySpeed(value);
        } else {
            result = value;
        }
        return result;
    }

    public static float getValueFromImperialBySpeed(float value, UnitType type) {
        float result;
        if (isMetric(type)) {
            result = imperialToMetricBySpeed(value);
        } else {
            result = value;
        }
        return result;
    }

    /**
     * Description : 将当前单位转换到英制
     *
     * @param value
     * @return
     * @author : tony.zhang
     * @date : 2015-2-10 下午4:14:35
     */
    public static float getValueToImperialBySpeed(float value, UnitType type) {
        float result;
        if (isMetric(type)) {
            result = metricToImperialBySpeed(value);
        } else {
            result = value;
        }
        return result;
    }

    /**
     * Description : 长度单位
     *
     * @return
     * @author : tony.zhang
     * @date : 2015-2-10 下午3:38:07
     */
    public static String getUintStrByLength(UnitType type) {
        return isMetric(type) ? "m" : "ft";
    }

    /**
     * Description : 长度单位
     *
     * @return
     * @author : tony.zhang
     * @date : 2015-2-10 下午3:38:07
     */
    public static String getUintStr() {
        return getCurrentUnit() == UNIT_IMPERIAL ? "ft" : "m";
    }

    /**
     * Description : 速度单位
     *
     * @return
     * @author : tony.zhang
     * @date : 2015-2-10 下午3:38:11
     */
    public static String getUintStrBySpeed(UnitType type) {
        return isMetric(type) ? "m/s" : "mph";
    }

    public static boolean isMetric(UnitType type) {
        // 这里如果要取真实值 那么在限高那里要修改一下
        return type == UnitType.METRIC;
    }

    public static String getSpeedUnit() {
        int unitType = getCurrentUnit();
        if (unitType == UNIT_METRIC_KM) {
            return "km/h";
        } else if (unitType == UNIT_IMPERIAL) {
            return "mph";
        } else {
            return "m/s";
        }
    }

    /**
     * 米每秒 转 千米每小时
     */
    public static final float fromMeterPerSecondToKiloMeterPerHour(float speed) {
        return speed * SPEED_MS_TO_KMH;
    }

    /**
     * 获取当前单位类型
     */
    public static int getCurrentUnit() {
        int unit = UxSharedPreferencesUtil.getInt(UxSharedPreferencesUtil.PREF_KEY_UNIT, UNIT_ALL, false);
        Context context = ContextUtil.getContext();
        if (context != null && unit == UNIT_ALL) {
            final Locale locale = context.getResources().getConfiguration().locale;
            if (Locale.US.getCountry().equals(locale.getCountry())) {
                unit = UNIT_IMPERIAL;
            } else {
                unit = UNIT_METRIC;
            }
        }
        return unit;
    }

    public static boolean isMetricUnits() {
        int unit = getCurrentUnit();
        return unit == UNIT_METRIC || unit == UNIT_METRIC_KM;
    }

    /**
     * 根据单位转换传入的距离值
     *
     * @param length
     * @return
     */
    public static float transformLength(float length) {
        if (UNIT_IMPERIAL == getCurrentUnit()) {
            length = metricToImperialByLength(length);
        } else if (UNIT_METRIC_KM == getCurrentUnit()) {
            length /= 1000.0f;
        }
        return length;
    }

    /**
     * 仅转换公制英制，公制用m，英制ft
     *
     * @param length
     * @return
     */
    public static float transformLength2(float length) {
        if (UNIT_IMPERIAL == getCurrentUnit()) {
            length = metricToImperialByLength(length);
        }
        return length;
    }

    public static String transformLengthByFormat(float length, int unit) {
        if (UNIT_IMPERIAL == unit) {
            return sDecimalFormat.format(metricToImperialByLength(length));
        } else if (UNIT_METRIC_KM == unit) {
            return sDecimalFormat.format(length / 1000.0f);
        } else {
            return sDecimalFormat.format(length);
        }
    }

    /**
     * 仅转换公制英制，公制用m，英制ft
     *
     * @param length
     * @param unit
     * @return
     */
    public static String transformLengthByFormat2(float length, int unit) {
        if (UNIT_IMPERIAL == unit) {
            return sDecimalFormat.format(metricToImperialByLength(length));
        } else {
            return sDecimalFormat.format(length);
        }
    }

    public static String transformLengthByFormat(float length) {
        if (UNIT_IMPERIAL == getCurrentUnit()) {
            length = metricToImperialByLength(length);
        } else if (UNIT_METRIC_KM == getCurrentUnit()) {
            length /= 1000.0f;
        }
        return sDecimalFormat.format(length);
    }

    public static float transFormSpeedIntoDifferentUnit(float speed) {
        int unit = getCurrentUnit();
        if (UNIT_IMPERIAL == unit) {
            speed = metricToImperialBySpeed(speed);
        } else if (UNIT_METRIC_KM == unit) {
            speed = fromMeterPerSecondToKiloMeterPerHour(speed);
        }
        return speed;
    }

    public static float transFormSpeedIntoDifferentUnit(float speed, int unit) {
        if (UNIT_IMPERIAL == unit) {
            speed = metricToImperialBySpeed(speed);
        } else if (UNIT_METRIC_KM == unit) {
            speed = fromMeterPerSecondToKiloMeterPerHour(speed);
        }
        return speed;
    }

    public static String formatBitRate(float value) {
        return formatBitRate(value, true);
    }

    public static String formatBitRate(float value, boolean isBinary) {
        float K = isBinary ? 1024 : 1000;
        float M = K * K;
        float G = M * M;
        value = value < 0 ? 0 : value;
        if (value < M) {
            return String.format(Locale.US, "%.1f", value / K) + " Kbps";
        } else if (value < G) {
            return String.format(Locale.US, "%.1f", value / M) + " Mbps";
        } else {
            return String.format(Locale.US, "%.1f", value / G) + " Gbps";
        }
    }

    /**
     * 平方米 转 亩， 根据语言转成英亩或亩
     */
    public static float squareMeter2Acre(Context context, final float value) {
        if (context.getResources().getConfiguration().locale.getLanguage().equals(Locale.CHINESE.getLanguage())) {
            final float multiplyFactor = 0.0015f;
            return value * multiplyFactor;
        } else {
            final float division = 4047f;
            return value / division;
        }
    }

    /**
     * 平方米 转 公顷， 根据语言转成英亩或亩
     */
    public static float squareMeter2Hectare(final float value) {
        final float multiplyFactor = 0.0001f;
        return value * multiplyFactor;
    }

    /**
     * 避免四舍五入之后出现 -0.0的情况
     *
     * @param origin
     * @param fractionDigit 有效小数点位数
     * @return
     */
    public static double roundNotNegZero(double origin, int fractionDigit) {

        if (origin >= 0) {
            return origin;
        }

        double scaleValue = origin;
        for (int i = 0; i < fractionDigit; i++) {
            scaleValue = scaleValue * 10;
        }
        boolean isZero = Math.round(scaleValue) == 0;
        if (isZero) {
            origin = Math.abs(origin);
        }
        return origin;
    }
    //endregion
}
