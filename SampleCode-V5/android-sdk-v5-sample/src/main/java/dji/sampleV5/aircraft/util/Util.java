package dji.sampleV5.aircraft.util;


import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


import dji.v5.utils.common.ContextUtil;
import dji.v5.utils.common.LogUtils;

public class Util {

    private final static String FILED_TYPE = "TYPE";
    private final static String TAG = Util.class.getSimpleName();

    private Util(){
        //init something
    }
    /**
     * 获取map集合key的List集合
     *
     */
    public static <K, V> List<K> getMapKeyList(Map<K, V> sourceMap) {
        List<K> keyList = new ArrayList<>();
        if (sourceMap == null || sourceMap.isEmpty()) {
            return keyList;
        }
        Set<K> keySet = sourceMap.keySet();
        for (K k : keySet) {
            keyList.add(k);
        }
        return keyList;
    }


    /**
     * 获取map集合value的List集合
     *
     */
    public static <K, V> List<V> getMapValueList(Map<K, V> sourceMap) {
        List<V> valueList = new ArrayList<>();
        if (sourceMap == null || sourceMap.isEmpty()) {
            return valueList;
        }
        Collection<V> values = sourceMap.values();
        for (V v : values) {
            valueList.add(v);
        }
        return valueList;
    }


    public static int getHeight(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        Display display = getResolution(context);
        display.getMetrics(dm);
        return dm.heightPixels;
    }


    public static Display getResolution(Context context) {
        return ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    }


    /**
     * 剔除两侧空格后是否为空
     *
     * @param str
     * @return
     */
    public static boolean isBlank(String str) {
        return (str == null || str.trim().length() == 0);
    }

    public static boolean isNotBlank(String str) {
        return (str != null && str.trim().length() != 0);
    }

    public static <T> T getGenericInstance(Object obj, int i) {
        try {
            Object superClass = obj.getClass().getGenericSuperclass();
            if (superClass instanceof ParameterizedType) {
                Type type = ((ParameterizedType) superClass).getActualTypeArguments()[i];
                return ((Class<T>) type).newInstance();
            }
        } catch (InstantiationException e) {
            LogUtils.e(TAG ,e.getMessage());
        } catch (IllegalAccessException e) {
            LogUtils.e(TAG ,e.getMessage());
        }
        return null;
    }

    /**
     * 判断是否为基本类型的包装类
     *
     * @param clz
     * @return
     */
    public static boolean isWrapClass(Class<?> clz) {
        try {
            return ((Class) clz.getField(FILED_TYPE).get(null)).isPrimitive();
        } catch (Exception e) {
            return false;
        }
    }

    public static String getDateStr(Date date) {
        String result = "";
        try {
            result = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
        } catch (Exception e) {
            LogUtils.e(TAG ,e.getMessage());
        }
        return result;
    }

    public static String getString( int msg) {
        return ContextUtil.getContext().getResources().getString(msg);
    }

    /**
     * 返回自适应显示的存储大小的字符串, 数字最大保留2位，指定有效位数4
     * @param size 原始存储空间，单位B
     * @return
     */
    public static String byte2AdaptiveUnitStrDefault(long size) {
        return byte2AdaptiveUnitStr(size, 2, 4);
    }

    /**
     * 返回自适应显示的存储大小的字符串
     * @param size 原始存储空间，单位B
     * @param atMostDecimalPlaces 最大保留小数
     * @param sigFig 指定有效位数
     * @return
     */
    public static String byte2AdaptiveUnitStr(long size, int atMostDecimalPlaces, int sigFig) {
        int count = 0;
        int radix = 1024;
        double sizeDouble = size * 1.0;
        while (sizeDouble > radix) {
            sizeDouble /= radix;
            count++;
        }
        String unit = "?";
        switch (count) {
            case 3:
                unit = "G";
                break;
            case 2:
                unit = "M";
                break;
            case 1:
                unit = "K";
                break;
            case 0:
                unit = "B";
                break;
            default:
                break;
        }
        return String.format(Locale.US,"%s%s", roundToSigFigWithAtMostDecimal(sizeDouble, atMostDecimalPlaces, sigFig), unit);
    }


    /**
     * 返回最大保留小数位数的指定有效位数的数字对应字符串。
     * 当显示的有效数字位数超过最大保留小数位数时，按最大保留位数截断数字。
     * @param num 原始数字，输入不应小于1
     * @param atMostDecimalPlaces 最大保留小数位数
     * @param sigFig 有效位数
     * @return
     */
    public static String roundToSigFigWithAtMostDecimal(double num, int atMostDecimalPlaces, int sigFig) {
        if (num < 1) {
            return "0";
        }
        double roundResult = roundToSignificantFigures(num, sigFig);
        int digitAfterZero = sigFig - (int) (Math.log10(Math.abs(roundResult))) - 1;
        String format = "%." + Math.min(digitAfterZero, atMostDecimalPlaces) + "f";
        return String.format(Locale.US, format, roundResult);
    }

    /**
     * 返回指定有效位数的数字
     * @param num 原始数字
     * @param sigFig 指定有效位数
     * @return
     */
    public static double roundToSignificantFigures(double num, int sigFig) {
        if(num == 0) {
            return 0;
        }

        final double d = Math.ceil(Math.log10(num < 0 ? -num: num));
        final int power = sigFig - (int) d;

        final double magnitude = Math.pow(10, power);
        final long shifted = Math.round(num*magnitude);
        return shifted/magnitude;
    }

}


