package dji.v5.ux.core.ui.hsi.fpv;

import M300FpvParams
import M30FpvParams
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.key.ProductKey
import dji.sdk.keyvalue.value.product.ProductType
import dji.v5.manager.KeyManager
import dji.v5.utils.dpad.DpadProductManager

/**
 * FPV 能力参数
 */
interface IFPVParams {
    /**
     * 镜头焦距，以像素为单位
     */
    fun getFocusX(): Float

    /**
     * 镜头焦距，以像素为单位
     */
    fun getFocusY(): Float

    /**
     * 视频原始宽度的1/2
     */
    fun getCenterX(): Float

    /**
     * 视频原始高度的1/2
     */
    fun getCenterY(): Float


    companion object {
        /**
         * 当前未考虑遥控器连接不同飞机，未观察连接飞机的变化，当出现遥控支持不同飞机时，需要观察飞机状态变化。
         */
        fun getCurrent(): IFPVParams {
            val type = KeyManager.getInstance().getValue(
                KeyTools.createKey(
                    ProductKey.KeyProductType), ProductType.UNKNOWN)
            if (type == ProductType.M300_RTK || type == ProductType.M350_RTK || type == ProductType.DJI_MATRICE_400) {
                return M300FpvParams
            } else if (type == ProductType.M30_SERIES) {
                return M30FpvParams
            }
            if (DpadProductManager.getInstance().isSmartController) {
                return M300FpvParams
            } else if (DpadProductManager.getInstance().isDjiRcPlus) {
                return M30FpvParams
            }
            return M300FpvParams
        }
    }
}