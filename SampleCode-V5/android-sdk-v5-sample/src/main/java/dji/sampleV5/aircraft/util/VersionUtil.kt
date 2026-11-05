@file:JvmName("VersionUtil")
package dji.sampleV5.aircraft.util

import dji.v5.utils.common.LogUtils


/**
 * <p>Description:</p>
 *
 * @author create at 2022/10/5 by joseph.ye for dji-pilot
 * @version v1.0
 */
fun compareVer(oldVer: String?, newVer: String?): Boolean {
    if (oldVer.isNullOrEmpty()) {
        return true
    }
    if (newVer.isNullOrEmpty()) {
        return false
    }

    val newVerList = newVer.split(".")
    val oldVerList = oldVer.split(".")

    val minLen = newVerList.size.coerceAtMost(oldVerList.size)
    for (index in 0 until minLen) {
        try {
            val newValue = newVerList[index].toInt()
            val oldValue = oldVerList[index].toInt()
            if (newValue > oldValue) {
                return true
            }
            if (oldValue > newValue) {
                return false
            }
        } catch (e: Exception) {
            LogUtils.e("VersionUtil", "compareVer err: ${e.message}")
        }
    }
    return newVerList.size > minLen
}