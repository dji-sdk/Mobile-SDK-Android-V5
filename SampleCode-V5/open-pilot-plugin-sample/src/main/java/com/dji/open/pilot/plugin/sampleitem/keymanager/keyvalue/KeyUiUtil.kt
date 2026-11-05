package com.dji.open.pilot.plugin.sampleitem.keymanager.keyvalue

import android.content.Context
import dji.v5.utils.common.LogUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * KeyManager 迁移自 android-sdk-v5-sample 的 Util，仅保留 keyvalue 逻辑所需的无业务通用方法。
 */
object KeyUiUtil {

    private const val FILED_TYPE = "TYPE"
    private val TAG = LogUtils.getTag(this)

    @JvmStatic
    fun isBlank(str: String?): Boolean = str == null || str.trim().isEmpty()

    @JvmStatic
    fun isNotBlank(str: String?): Boolean = !isBlank(str)

    @JvmStatic
    fun isWrapClass(clz: Class<*>): Boolean {
        return try {
            (clz.getField(FILED_TYPE).get(null) as Class<*>).isPrimitive
        } catch (e: Exception) {
            false
        }
    }

    @JvmStatic
    fun <K, V> getMapKeyList(sourceMap: Map<K, V>?): List<K> {
        return sourceMap?.keys?.toList() ?: emptyList()
    }

    @JvmStatic
    fun <K, V> getMapValueList(sourceMap: Map<K, V>?): List<V> {
        return sourceMap?.values?.toList() ?: emptyList()
    }

    @JvmStatic
    fun getDateStr(date: Date): String {
        return try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            LogUtils.e(TAG, e.message)
            ""
        }
    }

    @JvmStatic
    fun getHeight(context: Context): Int = context.resources.displayMetrics.heightPixels
}
