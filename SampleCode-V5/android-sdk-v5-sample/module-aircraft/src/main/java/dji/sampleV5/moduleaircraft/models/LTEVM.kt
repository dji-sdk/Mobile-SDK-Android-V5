package dji.sampleV5.moduleaircraft.models

import androidx.lifecycle.MutableLiveData
import dji.sampleV5.modulecommon.models.DJIViewModel
import dji.sdk.keyvalue.key.AirLinkKey
import dji.sdk.keyvalue.value.airlink.WlmDongleInfo
import dji.sdk.keyvalue.value.airlink.WlmDongleListInfo
import dji.sdk.keyvalue.value.airlink.WlmLinkQualityLevelInfo
import dji.v5.et.create
import dji.v5.et.listen
import dji.v5.manager.KeyManager

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/8/12
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class LTEVM : DJIViewModel() {

    val wlmLinkQualityLevel = MutableLiveData<WlmLinkQualityLevelInfo>()
    val wlmAircraftDongleListInfo = MutableLiveData<WlmDongleListInfo>()
    val wlmRcDongleListInfo = MutableLiveData<WlmDongleListInfo>()

    fun initListener() {
        AirLinkKey.KeyWlmLinkQualityLevel.create().listen(this) {
            wlmLinkQualityLevel.postValue(it)
        }
        AirLinkKey.KeyWlmAircraftDongleListInfo.create().listen(this) {
            wlmAircraftDongleListInfo.postValue(it)
        }
        AirLinkKey.KeyWlmRcDongleListInfo.create().listen(this) {
            wlmRcDongleListInfo.postValue(it)
        }
    }

    override fun onCleared() {
        KeyManager.getInstance().cancelListen(this)
    }
}