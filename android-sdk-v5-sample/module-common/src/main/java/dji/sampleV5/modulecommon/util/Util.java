package dji.sampleV5.modulecommon.util;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
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

}


