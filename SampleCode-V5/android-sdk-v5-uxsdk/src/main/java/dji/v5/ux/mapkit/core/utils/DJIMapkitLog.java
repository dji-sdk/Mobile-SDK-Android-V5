package dji.v5.ux.mapkit.core.utils;

import android.util.Log;

import dji.v5.utils.common.LogPath;
import dji.v5.utils.common.LogUtils;


/**
 * Created by joeyang on 10/18/17.
 */

public final class DJIMapkitLog {

    private DJIMapkitLog() {
    }

    private static final String TAG = "DJIMapKit";

    public static void i(String log) {
        Log.i(TAG, log);
    }

    public static void i(String tag, String message) {
        Log.i(tag, message);
    }

    public static void d(String log) {
        LogUtils.d(LogPath.FLY_SAFE, log);
    }

    public static void d(String tag, String message) {
        LogUtils.d(LogPath.FLY_SAFE, message);
    }

    public static void e(String log) {
        Log.e(TAG, log);
    }

    public static void e(String tag, String message) {
        Log.e(tag, message);
    }

    public static String getCurrentStack() {
        StringBuilder builder = new StringBuilder();
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        boolean isFirst = true;
        for (StackTraceElement st : stackTrace) {
            if (isFirst) {
                isFirst = false;
                continue;
            }
            builder.append("    " + st.toString() + "\n");
        }

        return builder.toString();
    }
}
