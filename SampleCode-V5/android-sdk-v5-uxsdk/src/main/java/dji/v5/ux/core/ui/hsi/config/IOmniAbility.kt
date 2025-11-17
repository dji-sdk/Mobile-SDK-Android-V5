package dji.v5.ux.core.ui.hsi.config;


import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.key.ProductKey
import dji.sdk.keyvalue.value.product.ProductType
import dji.v5.manager.KeyManager
import dji.v5.utils.dpad.DpadProductManager

interface IOmniAbility {
    /**
     * 视觉避障-上方探测距离
     */
    fun getUpDetectionCapability(): Int

    /**
     * 上方避障刹停距离设置范围
     */
    fun getUpAvoidanceDistanceRange(): Pair<Float, Float>

    /**
     * 视觉避障-下方探测距离
     */
    fun getDownDetectionCapability(): Int

    /**
     * 下方避障刹停距离设置范围
     */
    fun getDownAvoidanceDistanceRange(): Pair<Float, Float>

    /**
     * 视觉避障-水平探测距离
     */
    fun getHorizontalDetectionCapability(): Int

    /**
     * 水平避障刹停距离设置范围
     */
    fun getHorizontalAvoidanceDistanceRange(): Pair<Float, Float>

    /**
     * 视觉避障-每方向盲区大小
     */
    fun getPerceptionBlindAreaAngle(): Int

    companion object {
        /**
         * 当前未考虑遥控器连接不同飞机，未观察连接飞机的变化，当出现遥控支持不同飞机时，需要观察飞机状态变化。
         */
        fun getCurrent(): IOmniAbility {
            val type = KeyManager.getInstance().getValue(
                KeyTools.createKey(
                    ProductKey.KeyProductType), ProductType.UNKNOWN)
            return when {
                DpadProductManager.getInstance().isSmartController || type == ProductType.M300_RTK || type == ProductType.M350_RTK|| type == ProductType.DJI_MATRICE_400 -> M300OmniAbility
                DpadProductManager.getInstance().isDjiRcPlus || type == ProductType.M30_SERIES -> M30OmniAbility
                DpadProductManager.getInstance().isDjiRcPro || type == ProductType.DJI_MAVIC_3_ENTERPRISE_SERIES -> M3EOmniAbility
                else -> M300OmniAbility
            }
        }
    }
}