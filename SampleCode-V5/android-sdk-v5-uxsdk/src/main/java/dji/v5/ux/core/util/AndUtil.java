package dji.v5.ux.core.util;
/*
 * Copyright (c) 2014, DJI All Rights Reserved.
 */

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.provider.Settings;
import android.text.Html;
import android.text.TextUtils;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Size;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import androidx.annotation.ArrayRes;
import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import dji.v5.utils.common.LogUtils;

/**
 * <p>Created by luca on 2016/12/19.</p>
 */

public class AndUtil {
    // 香港跟台北可以用GoogleMap
    public static final Set<String> CHINA_TIME_ZONE = new HashSet<String>();
    public static final int MIX_PIXEL = 60;
    private static final String DEF_TYPE = "string";
    private static final String UTF_8 = "UTF-8";
    private static final String TAG = LogUtils.getTag("AndUtil");

    static {
        CHINA_TIME_ZONE.add("Asia/Chongqing");  // 重庆
        CHINA_TIME_ZONE.add("Asia/Shanghai");   // 上海
        CHINA_TIME_ZONE.add("Asia/Urumqi");     // 哈尔滨
        // CHINA_TIME_ZONE.add("Asia/Macao");      // 澳门    // 主要是判断是否为大陆
    }

    public static Size getLandScreenSize(Context context) {
        if (context == null) {
            return new Size(0, 0);
        }

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) {
            return new Size(0, 0);
        }

        int screenWidth;
        int screenHeight;

        if (Build.VERSION.SDK_INT < 17) {
            DisplayMetrics metrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(metrics);
            screenWidth = metrics.widthPixels;
            screenHeight = metrics.heightPixels;

            if (screenWidth < screenHeight) {
                int tmp = screenWidth;
                screenWidth = screenHeight;
                screenHeight = tmp;
            }
        } else {
            Display display = windowManager.getDefaultDisplay();
            Point outSize = new Point();
            display.getRealSize(outSize);
            screenWidth = outSize.x > outSize.y ? outSize.x : outSize.y;
            screenHeight = outSize.x > outSize.y ? outSize.y : outSize.x;
        }

        return new Size(screenWidth, screenHeight);
    }

    public static int getLandScreenWidth(Context context) {
        return getLandScreenSize(context).getWidth();
    }

    public static int getLandScreenHeight(Context context) {
        return getLandScreenSize(context).getHeight();
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f) - 15;
    }

    public static boolean isLeftGravity(View view) {
        if (view != null && view.getParent() instanceof FrameLayout) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
            return (params.gravity & Gravity.LEFT) == Gravity.LEFT || (params.gravity & Gravity.START) == Gravity.START;
        }
        return false;
    }

    public static boolean isRightGravity(View view) {
        if (view != null && view.getParent() instanceof FrameLayout) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
            return (params.gravity & Gravity.RIGHT) == Gravity.RIGHT || (params.gravity & Gravity.END) == Gravity.END;
        }
        return false;
    }

    public static void hideSoftInput(Activity activity) {
        if (activity == null || activity.getCurrentFocus() == null) {
            return;
        }
        InputMethodManager m = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (m.isActive()) {
            m.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }

    public static void hideSoftInput(View view) {
        if (view == null) {
            return;
        }
        InputMethodManager m = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (m.isActive()) {
            m.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static String getResString(Context context, String idName) {
        if (context != null && !TextUtils.isEmpty(idName)) {
            int resId = context.getResources().getIdentifier(idName, DEF_TYPE, context.getPackageName());
            return context.getString(resId);
        }
        return "";
    }

    @SuppressLint("ResourceType")
    public static String getResString(Context context, @StringRes int resId) {
        if (context != null && resId > 0) {
            return context.getString(resId);
        }
        return "";
    }

    @SuppressLint("ResourceType")
    public static CharSequence getHtmlResString(Context context, @StringRes int resId) {
        if (context != null && resId > 0) {
            String text = context.getString(resId).replace("\n", "<br />");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY);
            } else {
                return Html.fromHtml(text);
            }
        }
        return "";
    }

    @SuppressLint("ResourceType")
    public static String getResString(Context context, @StringRes int resId, Object... formatArgs) {
        if (context != null && resId > 0) {
            return context.getString(resId, formatArgs);
        }
        return "";
    }

    public static String getResString(Context context, String idName, Object... formatArgs) {
        if (context != null && !TextUtils.isEmpty(idName)) {
            int resId = context.getResources().getIdentifier(idName, DEF_TYPE, context.getPackageName());
            return context.getString(resId, formatArgs);
        }
        return "";
    }

    public static String getFormatResString(Context context, @StringRes int resId, Object... formatArgs) {
        return getFormatResString(context, Locale.US, resId, formatArgs);
    }

    @SuppressLint("ResourceType")
    public static String getFormatResString(Context context, Locale locale, @StringRes int resId, Object... formatArgs) {
        if (context != null && resId > 0) {
            return String.format(locale, context.getResources().getString(resId), formatArgs);
        }
        return "";
    }

    public static String getFormatResString(Context context, String idName, Object... formatArgs) {
        return getFormatResString(context, Locale.US, idName, formatArgs);
    }

    public static String getFormatResString(Context context, Locale locale, String idName, Object... formatArgs) {
        if (context != null && !TextUtils.isEmpty(idName)) {
            int resId = context.getResources().getIdentifier(idName, DEF_TYPE, context.getPackageName());
            return String.format(locale, context.getResources().getString(resId), formatArgs);
        }
        return "";
    }

    @SuppressLint("ResourceType")
    public static String[] getResStringArray(Context context, @ArrayRes int resId) {
        if (context != null && resId > 0 && context.getResources() != null) {
            return context.getResources().getStringArray(resId);
        }
        return new String[0];
    }

    @SuppressLint("ResourceType")
    public static int getResColor(Context context, @ColorRes int resId) {
        if (context != null && resId > 0) {
            return context.getResources().getColor(resId);
        }
        return 0;
    }

    @SuppressLint("ResourceType")
    public static Drawable getResDrawable(Context context, @DrawableRes int resId) {
        if (context != null && resId > 0) {
            return context.getResources().getDrawable(resId);
        }
        return null;
    }

    @SuppressLint("ResourceType")
    public static float getDimension(Context context, @DimenRes int resId) {
        if (context != null && resId > 0) {
            return context.getResources().getDimension(resId);
        }
        return 0f;
    }

    /**
     * 获取时区
     *
     * @return 时区名： 如"Asia/Chongqing", "Asia/Shanghai"
     */
    public static String getTimeZone() {
        TimeZone tz = TimeZone.getDefault();
        return tz != null ? tz.getID() : null;
    }

    public static boolean isChinaTimeZone() {
        String timezone = getTimeZone();
        return timezone != null && CHINA_TIME_ZONE.contains(timezone);
    }

    /**
     * 获取进程号{@link android.os.Process}对应的进程名
     *
     * @param pid 进程号
     * @return 进程名
     */
    public static String getProcessName(int pid) {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/" + pid + "/cmdline"))) {
            String processName = reader.readLine();
            if (!TextUtils.isEmpty(processName)) {
                processName = processName.trim();
            }
            return processName;
        } catch (FileNotFoundException e) {
            LogUtils.e(TAG, e.getMessage());
        } catch (IOException e) {
            LogUtils.e(TAG, e.getMessage());
        }
        return null;
    }

    /**
     * 获取CPU核数
     *
     * @return CPU核数
     */
    public static int getCPUCores() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
            // Gingerbread doesn't support giving a single application access to both cores, but a
            // handful of devices (Atrix 4G and Droid X2 for example) were released with a dual-core
            // chipset and Gingerbread; that can let an app in the background run without impacting
            // the foreground application. But for our purposes, it makes them single core.
            return 1;  //上面的意思就是2.3以前不支持多核,有些特殊的设备有双核...不考虑,就当单核!!
        }
        int cores;
        try {
            cores = new File("/sys/devices/system/cpu/").listFiles(CPU_FILTER).length;
        } catch (SecurityException | NullPointerException e) {
            cores = 2;   //这个常量得自己约定
        }
        return cores;
    }

    private static final FileFilter CPU_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            String path = pathname.getName();
            //regex is slow, so checking char by char.
            if (path.startsWith("cpu")) {
                for (int i = 3; i < path.length(); i++) {
                    if (path.charAt(i) < '0' || path.charAt(i) > '9') {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    };

    public static float getItemHeight(Context context) {
        TypedValue value = new TypedValue();
        DisplayMetrics metrics = new DisplayMetrics();
        context.getTheme().resolveAttribute(android.R.attr.listPreferredItemHeight, value, true);
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            windowManager.getDefaultDisplay().getMetrics(metrics);
        }
        return TypedValue.complexToDimension(value.data, metrics);
    }

    /**
     * 尝试把context转换成Activity
     *
     * @param context
     * @return
     */
    public static Activity getActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    /**
     * 判断是否平板设备
     *
     * @return true:平板,false:手机
     */
    public static boolean isTabletDevice(Context context) {
        return context != null
                && (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    /**
     * 判断两个Float数字是否相等
     *
     * @param num1
     * @param num2
     * @return
     */
    public static boolean isFloatEquals(float num1, float num2) {
        BigDecimal a = BigDecimal.valueOf(num1);
        BigDecimal b = BigDecimal.valueOf(num2);
        return a.equals(b);
    }

    /**
     * 判断两个Double数字是否相等
     *
     * @param num1
     * @param num2
     * @return
     */
    public static boolean isDoubleEquals(double num1, double num2) {
        BigDecimal a = BigDecimal.valueOf(num1);
        BigDecimal b = BigDecimal.valueOf(num2);
        return a.equals(b);
    }

//    /**
//     * 返回一个点是否在一个多边形区域内
//     *
//     * @param points 多边形坐标点列表
//     * @param point  待判断点
//     * @return true 多边形包含这个点,false 多边形未包含这个点。
//     */
//    public static boolean isPolygonContainsPoint(List<DJILatLng> points, DJILatLng point) {
//        int nCross = 0;
//        for (int i = 0; i < points.size(); i++) {
//            DJILatLng p1 = points.get(i);
//            DJILatLng p2 = points.get((i + 1) % points.size());
//            // 取多边形任意一个边,做点point的水平延长线,求解与当前边的交点个数
//            // p1p2是水平线段,要么没有交点,要么有无限个交点
//            if (p1.getLongitude() == p2.getLongitude()) {
//                continue;
//            }
//            // point 在p1p2 底部 --> 无交点
//            if (point.getLongitude() < Math.min(p1.getLongitude(), p2.getLongitude())) {
//                continue;
//            }
//            // point 在p1p2 顶部 --> 无交点
//            if (point.getLongitude() >= Math.max(p1.getLongitude(), p2.getLongitude())) {
//                continue;
//            }
//            // 求解 point点水平线与当前p1p2边的交点的 X 坐标
//            double x = (point.getLongitude() - p1.getLongitude()) * (p2.getLatitude() - p1.getLatitude()) / (p2.getLongitude() - p1.getLongitude()) + p1.getLatitude();
//            // 当x=point.x时,说明point在p1p2线段上
//            if (x > point.getLatitude()) {
//                nCross++; // 只统计单边交点
//            }
//        }
//        // 单边交点为偶数，点在多边形之外 ---
//        return (nCross % 2 == 1);
//    }

    public static boolean isEquals(Point p1, Point p2) {
        int disx = p1.x - p2.x;
        int disy = p1.y - p2.y;
        return Math.abs(disx) < MIX_PIXEL && Math.abs(disy) < MIX_PIXEL;
    }

    /**
     * 获取application中指定的meta-data 调用方法时key就是com.dji.sdk.API_KEY
     *
     * @return 如果没有获取成功(没有对应值 ， 或者异常)，则返回值为空
     */
    public static String getApiKeyData(Context ctx) {
        if (ctx == null) {
            return null;
        }
        String resultData = null;
        try {
            PackageManager packageManager = ctx.getPackageManager();
            if (packageManager != null) {
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(ctx.getPackageName(), PackageManager.GET_META_DATA);
                if (applicationInfo != null) {
                    if (applicationInfo.metaData != null) {
                        resultData = applicationInfo.metaData.getString("com.dji.sdk.API_KEY");
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            LogUtils.e(TAG,e.getMessage());
        }

        return resultData;
    }

    public static boolean isGPSModeString(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        } else if (str.equalsIgnoreCase("P-GPS")
                || str.equalsIgnoreCase("OPTI")
                || str.equalsIgnoreCase("GPS")
                || str.equalsIgnoreCase("F-GPS")
                || str.equalsIgnoreCase("Position")
                || str.equalsIgnoreCase("RTK")) {
            return true;
        }
        return false;
    }

    public static List<Object> filterRepeatList(List<Object> list) {
        if (list == null) {
            return list;
        }
        Set<Object> set = new HashSet<>();
        List<Object> newList = new ArrayList<>();
        for (Iterator<Object> iter = list.iterator(); iter.hasNext(); ) {
            Object element = iter.next();
            if (set.add(element)) {
                newList.add(element);
            }
        }
        return newList;
    }

    public static float byteToMB(float b) {
        return b / 1024 / 1024;
    }

    public static float byteToKB(float b) {
        return b / 1024;
    }

    public static String encodeBase64(final String content) {
        try {
            return new String(Base64.encode(content.getBytes(UTF_8), Base64.NO_WRAP), UTF_8);
        } catch (Exception e) {
            return content;
        }
    }

    public static String decodeBase64(final String content) {
        try {
            return new String(Base64.decode(content.getBytes(UTF_8), Base64.NO_WRAP), UTF_8);
        } catch (Exception e) {
            return content;
        }
    }

    public static String getCrystalProductName() {
        return getProp("ro.product.model", "Unknown");
    }

    public static String getCrystalVersionCodeOfLocalSystem() {
        return getProp("persist.system.version", "Unknown");
    }

    private static String getProp(String key, String defaultValue) {
        String value = defaultValue;
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class, String.class);
            value = (String) (get.invoke(c, key, defaultValue));
        } catch (Exception e) {
        }
        return value;
    }

    /**
     * @param context
     * @return
     * @Description : 判断系统是否开启声音效果（例如触摸声音）
     */
    public static boolean querySoundEffectsEnabled(final Context context) {
        boolean enabled = false;
        try {
            enabled = Settings.System.getInt(context.getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED) != 0;
        } catch (Settings.SettingNotFoundException e) {

        }
        return enabled;
    }
}
