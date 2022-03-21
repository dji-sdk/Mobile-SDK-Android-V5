package dji.sampleV5.modulecommon.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dji.v5.manager.diagnostic.DJIDeviceHealthInfo
import dji.v5.utils.common.LogUtils

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/7/5
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class DJIViewModel : ViewModel() {

    val logTag = LogUtils.getTag(this)

}