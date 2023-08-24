package dji.sampleV5.modulecommon.models

import dji.v5.manager.dataprotect.DataProtectionManager

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/6/30
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
class DataProtectionVm : DJIViewModel() {

    fun agreeToProductImprovement(isAgree: Boolean) {
        DataProtectionManager.getInstance().agreeToProductImprovement(isAgree)
    }

    fun isAgreeToProductImprovement(): Boolean {
        return DataProtectionManager.getInstance().isAgreeToProductImprovement
    }
}