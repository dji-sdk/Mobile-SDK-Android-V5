package dji.sampleV5.modulecommon.data

import dji.sdk.keyvalue.value.product.ProductType
import dji.v5.common.register.PackageProductCategory

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/5/6
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */

data class MSDKInfo(val SDKVersion: String = DEFAULT_STR) {
    var buildVer: String = DEFAULT_STR
    var isDebug: Boolean = false
    var packageProductCategory: PackageProductCategory? = null
    var productType: ProductType = ProductType.UNKNOWN
    var networkInfo: String = DEFAULT_STR
    var countryCode: String = DEFAULT_STR
    var firmwareVer: String = DEFAULT_STR
    var isLDMLicenseLoaded: String = DEFAULT_STR
    var isLDMEnabled: String = DEFAULT_STR
}