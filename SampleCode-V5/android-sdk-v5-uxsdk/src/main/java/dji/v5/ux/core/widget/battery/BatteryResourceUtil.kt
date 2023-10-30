package dji.v5.ux.core.widget.battery

import dji.sdk.keyvalue.key.BatteryKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.battery.IndustryBatteryType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.manager.KeyManager
import dji.v5.utils.common.AndUtil
import dji.v5.ux.R

object BatteryResourceUtil {


    /**
     * 获取电池的标题
     * 如果是一个电池的飞机，返回`电池`
     * 如果是两个电池的飞机，返回的是`左电池`或者`右电池`
     * 否则返回电池1、电池2、电池3
     */
    fun getBatteryTitle(index: Int): String {
        val batteryOverview = KeyManager.getInstance().getValue(
            KeyTools.createKey(BatteryKey.KeyBatteryOverviews, ComponentIndexType.AGGREGATION))
        return when (batteryOverview?.size) {
            1 -> AndUtil.getResString(R.string.uxsdk_setting_ui_general_battery)
            2 -> if (index == 0) {
                AndUtil.getResString(R.string.uxsdk_fpv_top_bar_battery_left_battery)
            } else {
                AndUtil.getResString(R.string.uxsdk_fpv_top_bar_battery_right_battery)
            }

            else -> AndUtil.getResString(R.string.uxsdk_setting_ui_general_battery)
                .toString() + " " + (index + 1)

        }
    }


    fun IndustryBatteryType.productName(): String {
        return when (this) {
            IndustryBatteryType.TB60 -> "TB60"
            IndustryBatteryType.TB65 -> "TB65"
            else -> ""
        }
    }
}