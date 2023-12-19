package dji.sampleV5.moduleaircraft.models

import androidx.lifecycle.MutableLiveData
import dji.sampleV5.aircraft.data.DJIToastResult
import dji.sampleV5.aircraft.models.DJIViewModel
import dji.sampleV5.aircraft.util.ToastUtils

import dji.sdk.keyvalue.value.flightcontroller.RealNameRegistrationState
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.uas.AreaStrategy
import dji.v5.manager.aircraft.uas.RealNameRegistrationStatusListener
import dji.v5.manager.aircraft.uas.UASRemoteIDManager
import dji.v5.manager.aircraft.uas.UASRemoteIDStatus

/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2023/11/17
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class UASChinaVM: DJIViewModel() {
    val uomRealNameFCStatus = MutableLiveData<RealNameRegistrationState>()
    val uasRemoteIDStatus = MutableLiveData<UASRemoteIDStatus>()
    private val uasRemoteIDManager: UASRemoteIDManager = UASRemoteIDManager.getInstance()

    init {
        val error = uasRemoteIDManager.setUASRemoteIDAreaStrategy(AreaStrategy.CHINA_STRATEGY)
        error?.apply {
            ToastUtils.showToast(toString())
        }
    }

    private val listener:RealNameRegistrationStatusListener= RealNameRegistrationStatusListener {
        uomRealNameFCStatus.postValue(it.realNameRegistrationStateFromAircraft)
    }

    fun addRealNameRegistrationStatusListener() {
        uasRemoteIDManager.addRealNameRegistrationStatusListener(listener)
    }


    fun removeRealNameRegistrationStatusListener() {
        uasRemoteIDManager.removeRealNameRegistrationStatusListener(listener)
    }

    fun updateRealNameRegistrationStateFromUOM() {
        uasRemoteIDManager.updateRealNameRegistrationStateFromUOM(object : CommonCallbacks.CompletionCallback{
            override fun onSuccess() {
                toastResult?.postValue(DJIToastResult.success("updateRealNameRegistrationStateFromUOM success"))
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))

            }
        })
    }
}