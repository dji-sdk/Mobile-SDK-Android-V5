package dji.sampleV5.aircraft.models

import androidx.lifecycle.MutableLiveData
import dji.sampleV5.aircraft.data.DJIToastResult
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.uas.AreaStrategy
import dji.v5.manager.aircraft.uas.UARegistrationNumberStatus
import dji.v5.manager.aircraft.uas.UASRemoteIDManager
import dji.v5.manager.aircraft.uas.UASRemoteIDStatus
import dji.sampleV5.aircraft.util.ToastUtils

/**
 * Description :日本无人机远程识别VM
 *
 * @author: Byte.Cai
 *  date : 2022/6/27
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class UASJapanVM : DJIViewModel() {
    private val instance = UASRemoteIDManager.getInstance()
    val uaRegNumberStatus = MutableLiveData<UARegistrationNumberStatus>()
    val uasRemoteIDStatus = MutableLiveData<UASRemoteIDStatus>()
    val uaRegistrationNumber = MutableLiveData<String?>()

    init {
        val error = instance.setUASRemoteIDAreaStrategy(AreaStrategy.JAPAN_STRATEGY)
        error?.apply {
            ToastUtils.showToast(toString())
        }
    }

    fun setUARegistrationNumber(number: String) {

        instance.setUARegistrationNumber(number,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    toastResult?.postValue(DJIToastResult.success("setUARegistrationNumber success"))

                }

                override fun onFailure(error: IDJIError) {
                    toastResult?.postValue(DJIToastResult.failed(error.toString()))

                }

            })
    }

    fun getUARegistrationNumber() {
        instance.getUARegistrationNumber(object : CommonCallbacks.CompletionCallbackWithParam<String> {
            override fun onSuccess(t: String?) {
                toastResult?.postValue(DJIToastResult.success("getUARegistrationNumber success"))
                uaRegistrationNumber.postValue(t)
            }

            override fun onFailure(error: IDJIError) {
                uaRegistrationNumber.postValue(error.toString())
            }

        })
    }

    fun addUASRemoteIDStatusListener() {
        instance.addUASRemoteIDStatusListener {
            uasRemoteIDStatus.postValue(it)
        }
    }

    fun addUARegistrationNumberStatusListener() {
        instance.addUARegistrationNumberStatusListener {
            uaRegNumberStatus.postValue(it)
        }
    }

    fun clearAllListener() {
        instance.clearUASRemoteIDStatusListener()
        instance.clearAllUARegistrationNumberStatusListener()
    }
}