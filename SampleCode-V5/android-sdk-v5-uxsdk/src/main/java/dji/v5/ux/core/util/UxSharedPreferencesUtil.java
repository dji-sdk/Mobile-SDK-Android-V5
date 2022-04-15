package dji.v5.ux.core.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Set;

/**
 * @author dnld
 * @date 31/07/16
 */

public class UxSharedPreferencesUtil {

    public static final String PREF_KEY_UNIT = "pref_key_unit";

    private static volatile boolean INITIALIZED = false;
    private static String sUserId;
    private static SharedPreferences SP;

    private UxSharedPreferencesUtil() {
    }

    /**
     * 不能放在Application中初始化
     */
    public static synchronized void initialize(Context context) {
        if (context == null || INITIALIZED) {
            return;
        }
        sUserId = "ux_" + context.getPackageName();
        SP = PreferenceManager.getDefaultSharedPreferences(context);
        INITIALIZED = true;
    }

    /**
     * 未登录状态下设置一个默认的UserId，即空字符串
     */
    public static void setEmptyUserId() {
        setUserId("");
    }

    /**
     * 设置关联的UserID.
     *
     * @param userId 用户唯一ID
     */
    public static void setUserId(String userId) {
        sUserId = userId;
    }

    public static SharedPreferences.Editor getEditor() {
        return SP.edit();
    }

    /**
     * @param key
     * @return Returns if that key exists
     */
    public static boolean contains(final String key) {
        return SP.contains(key);
    }

    //region String

    /**
     * 获取String类型
     *
     * @param key      key
     * @param defValue 默认值
     * @param user     true和用户ID关联，false则和用户无关
     * @return
     */
    public static String getString(String key, String defValue, boolean user) {
        return SP.getString(user ? key + sUserId : key, defValue);
    }

    /**
     * 存储String类型
     *
     * @param key   键
     * @param value 值
     * @param user  true和用户ID关联，false则和用户无关
     * @return
     */
    public static void putString(String key, String value, boolean user) {
        getEditor().putString(user ? key + sUserId : key, value).apply();
    }

    /**
     * {@link #putString(String, String, boolean)} 是现将数据存储到内存中然后异步更新到磁盘，
     * 因此可以在主线程调用，但是缺点是如果调用的时候突然crash导致APP退出的话有可能没有
     * 将数据持久化到磁盘，而 {@link #commitString(String, String, boolean)} 则是同步的方法
     * 直接将数据存储到磁盘，但是不应该在主线程调用。
     *
     * @param key
     * @param value
     */
    @SuppressLint("ApplySharedPref")
    public static void commitString(String key, String value, boolean user) {
        getEditor().putString(user ? key + sUserId : key, value).commit();
    }
    //endregion

    //region Boolean

    public static boolean getBoolean(String key, boolean defValue, boolean user) {
        return SP.getBoolean(user ? key + sUserId : key, defValue);
    }

    public static void putBoolean(String key, boolean value, boolean user) {
        getEditor().putBoolean(user ? key + sUserId : key, value).apply();
    }

    @SuppressLint("ApplySharedPref")
    public static void commitBoolean(String key, boolean value, boolean user) {
        getEditor().putBoolean(user ? key + sUserId : key, value).commit();
    }
    //endregion

    //region Integer

    public static int getInt(String key, int defValue, boolean user) {
        return SP.getInt(user ? key + sUserId : key, defValue);
    }

    public static void putInt(String key, int value, boolean user) {
        getEditor().putInt(user ? key + sUserId : key, value).apply();
    }

    @SuppressLint("ApplySharedPref")
    public static void commitInteger(String key, int value, boolean user) {
        getEditor().putInt(user ? key + sUserId : key, value).commit();
    }
    //endregion

    //region Long

    public static long getLong(String key, long defValue, boolean user) {
        return SP.getLong(user ? key + sUserId : key, defValue);
    }

    public static void putLong(String key, long value, boolean user) {
        getEditor().putLong(user ? key + sUserId : key, value).apply();
    }

    @SuppressLint("ApplySharedPref")
    public static void commitLong(String key, long value, boolean user) {
        getEditor().putLong(user ? key + sUserId : key, value).commit();
    }
    //endregion


    //region Float

    public static float getFloat(String key, float defValue, boolean user) {
        return SP.getFloat(user ? key + sUserId : key, defValue);
    }

    public static void putFloat(String key, float value, boolean user) {
        getEditor().putFloat(user ? key + sUserId : key, value).apply();
    }

    @SuppressLint("ApplySharedPref")
    public static void commitFloat(String key, float value, boolean user) {
        getEditor().putFloat(user ? key + sUserId : key, value).commit();
    }
    //endregion

    //region Double

    /**
     * @param key
     * @return Returns the stored value of 'what'
     */
    public static double getDouble(String key, double defValue, boolean user) {
        if (!contains(key)) {
            return defValue;
        }
        return Double.longBitsToDouble(getLong(key, 0, user));
    }

    public static void putDouble(String key, double value, boolean user) {
        putLong(key, Double.doubleToLongBits(value), user);
    }

    @SuppressLint("ApplySharedPref")
    public static void commitDouble(String key, double value, boolean user) {
        commitLong(key, Double.doubleToLongBits(value), user);
    }
    //endregion

    //region StringSet

    public static Set<String> getStringSet(String key, Set<String> defValue, boolean user) {
        return SP.getStringSet(user ? key + sUserId : key, defValue);
    }

    public static void putStringSet(String key, Set<String> value, boolean user) {
        getEditor().putStringSet(user ? key + sUserId : key, value).apply();
    }

    @SuppressLint("ApplySharedPref")
    public static void commitStringSet(String key, Set<String> value, boolean user) {
        getEditor().putStringSet(user ? key + sUserId : key, value).commit();
    }
    //endregion


    //region Remove

    public static void remove(String key, boolean user) {
        getEditor().remove(user ? key + sUserId : key).apply();
    }

    public static void commitRemove(String key, boolean user) {
        getEditor().remove(user ? key + sUserId : key).commit();
    }

    public static void remove(boolean user, String... keys) {
        if (keys != null && keys.length > 0) {
            for (String key : keys) {
                remove(key, user);
            }
        }
    }

    /**
     * 会清楚所有数据，谨慎操作
     */
    public static void clear() {
        getEditor().clear().apply();
    }

    /**
     * 会清楚所有数据，谨慎操作
     */
    public static void commitClear() {
        getEditor().clear().commit();
    }
    //endregion
}
