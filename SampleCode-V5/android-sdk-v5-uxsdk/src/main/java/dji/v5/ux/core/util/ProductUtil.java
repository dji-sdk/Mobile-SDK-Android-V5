/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dji.v5.ux.core.util;

/**
 * Utility class for product information.
 */
public final class ProductUtil {

    private ProductUtil() {
        // prevent instantiation of util class
    }

//    /**
//     * Determine whether a product is connected
//     *
//     * @return `true` if a product is connected. `false` otherwise.
//     */
//    public static boolean isProductAvailable() {
//        return DJISDKManager.getInstance() != null && DJISDKManager.getInstance().getProduct() != null;
//    }
//
//    /**
//     * Determine whether the connected product is part of the Mavic 2 series.
//     *
//     * @param model The connected product.
//     * @return `true` if the connected product is part of the Mavic 2 series. `false` if the
//     * connected product is not part of the Mavic 2 series.
//     */
//    public static boolean isMavic2SeriesProduct(Model model) {
//        return Model.MAVIC_2.equals(model)
//                || Model.MAVIC_2_PRO.equals(model)
//                || Model.MAVIC_2_ZOOM.equals(model)
//                || Model.MAVIC_2_ENTERPRISE.equals(model)
//                || Model.MAVIC_2_ENTERPRISE_DUAL.equals(model);
//    }
//
    /**
     * Determine whether the connected product has a Hasselblad camera.
     *
     * @return `true` if the connected product has a Hasselblad camera. `false` if there is
     * no product connected or if the connected product does not have a Hasselblad camera.
     */
    public static boolean isHasselbladCamera() {
//        return isProductAvailable()
//                && Model.MAVIC_2_PRO.equals(DJISDKManager.getInstance().getProduct().getModel());
        return false;
    }
//
//    /**
//     * Determine whether the connected product is a Mavic 2 Enterprise.
//     *
//     * @param model The connected product.
//     * @return `true` if the connected product is a Mavic 2 Enterprise. `false` if the connected
//     * product is not a Mavic 2 Enterprise.
//     */
//    public static boolean isMavic2Enterprise(Model model) {
//        return Model.MAVIC_2_ENTERPRISE.equals(model) || Model.MAVIC_2_ENTERPRISE_DUAL.equals(model);
//    }
//
//    /**
//     * Determine whether the connected product is a Matrice 600.
//     *
//     * @param model The connected product.
//     * @return `true` if the connected product is a Matrice 600. `false` if the connected product
//     * is not a Matrice 600.
//     */
//    public static boolean isMatrice600Series(Model model) {
//        return Model.MATRICE_600.equals(model) || Model.MATRICE_600_PRO.equals(model);
//    }
//
//    /**
//     * Determine whether the connected product is in the Matrice 200 V2 series or the Matrice 300
//     * series.
//     *
//     * @param model The connected product.
//     * @return `true` if the connected product is in the  Matrice 200 V2 or Matrice 300 series.
//     * `false` if the connected product is not in the Matrice 200 V2 or Matrice 300 series.
//     */
//    public static boolean isM200V2M300(Model model) {
//        return Model.MATRICE_200_V2.equals(model)
//                || Model.MATRICE_210_V2.equals(model)
//                || Model.MATRICE_210_RTK_V2.equals(model)
//                || Model.MATRICE_300_RTK.equals(model);
//    }
//
//    /**
//     * Determine whether the connected product supports external video input.
//     *
//     * @return `true` if the connected product supports external video input. `false` if there is
//     * no product connected or if the connected product does not support external video input.
//     */
//    public static boolean isExtPortSupportedProduct() {
//        if (isProductAvailable()) {
//            Model model = DJISDKManager.getInstance().getProduct().getModel();
//            return Model.MATRICE_600.equals(model)
//                    || Model.MATRICE_600_PRO.equals(model)
//                    || Model.A3.equals(model)
//                    || Model.N3.equals(model);
//        }
//        return false;
//    }
//
//    /**
//     * Get the Speaker object from the current product.
//     *
//     * @return The current product's speaker, or null if the current product does not have a
//     * speaker.
//     */
//    public static Speaker getSpeaker() {
//        Aircraft aircraft = (Aircraft) DJISDKManager.getInstance().getProduct();
//        if (aircraft != null && null != aircraft.getAccessoryAggregation() && null != aircraft.getAccessoryAggregation().getSpeaker()) {
//            return aircraft.getAccessoryAggregation().getSpeaker();
//        }
//        return null;
//    }
//
//    /**
//     * Determine whether the connected product supports Auto ISO.
//     *
//     * @return `true` if the connected product supports Auto ISO. `false` if there is
//     * no product connected or if the connected product does not support Auto ISO.
//     */
//    public static boolean isAutoISOSupportedProduct() {
//        if (isProductAvailable()) {
//            Model model = DJISDKManager.getInstance().getProduct().getModel();
//            return !Model.MAVIC_AIR.equals(model) && !Model.MAVIC_PRO.equals(model);
//        }
//        return false;
//    }
//
//    /**
//     * Determine whether the connected product has vision sensors.
//     *
//     * @param model The connected product.
//     * @return `true` if the connected product has vision sensors. `false` if the connected product
//     * does not have vision sensors.
//     */
//    public static boolean isVisionSupportedProduct(Model model) {
//        return !isMatrice600Series(model)
//                && !Model.MATRICE_100.equals(model)
//                && !isPhantom3Series(model)
//                && !isInspire1Series(model)
//                && !Model.MAVIC_MINI.equals(model);
//    }
//
//    /**
//     * Determine whether the connected product is connected by WiFi only without an RC.
//     *
//     * @return True if the product is connected by WiFi.
//     */
//    public static boolean isProductWiFiConnected() {
//        if (!isProductAvailable()) return false;
//
//        BaseProduct product = DJISDKManager.getInstance().getProduct();
//        if (product instanceof Aircraft) {
//            Aircraft aircraft = (Aircraft) product;
//            return (aircraft.isConnected() &&
//                    (aircraft.getRemoteController() == null
//                            || !aircraft.getRemoteController().isConnected()));
//        }
//        return false;
//    }
//
//    public static boolean isMavicAir() {
//        return DJISDKManager.getInstance() != null && DJISDKManager.getInstance().getProduct() != null
//                && Model.MAVIC_AIR.equals(DJISDKManager.getInstance().getProduct().getModel());
//    }
//
//    public static boolean isMavicPro() {
//        return DJISDKManager.getInstance() != null && DJISDKManager.getInstance().getProduct() != null
//                && Model.MAVIC_PRO.equals(DJISDKManager.getInstance().getProduct().getModel());
//    }
//
//    public static boolean isMavicMini() {
//        if (DJISDKManager.getInstance() != null && DJISDKManager.getInstance().getProduct() != null) {
//            Model model = DJISDKManager.getInstance().getProduct().getModel();
//            return Model.MAVIC_MINI.equals(model);
//        }
//        return false;
//    }
}
