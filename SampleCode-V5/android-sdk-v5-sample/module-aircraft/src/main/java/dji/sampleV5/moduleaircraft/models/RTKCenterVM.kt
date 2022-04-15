package dji.sampleV5.moduleaircraft.models

import androidx.lifecycle.MutableLiveData
import dji.sampleV5.moduleaircraft.data.DJIBaseResult
import dji.sampleV5.modulecommon.models.DJIViewModel
import dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.rtk.*
import dji.v5.utils.common.LogUtils


/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2022/3/19
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class RTKCenterVM : DJIViewModel() {
    private val TAG="RTKCenterVM"
    val setAircraftRTKModuleEnableLD = MutableLiveData<DJIBaseResult<Boolean>>()
    val getAircraftRTKModuleEnabledLD = MutableLiveData<DJIBaseResult<Boolean>>()
    val setRTKReferenceStationSourceLD = MutableLiveData<DJIBaseResult<Boolean>>()
    val rtkLocationInfoLD = MutableLiveData<DJIBaseResult<RTKLocationInfo>>()
    val rtkSystemStateLD = MutableLiveData<DJIBaseResult<RTKSystemState>>()
    val rtkSourceLD = MutableLiveData<DJIBaseResult<RTKReferenceStationSource>>()

    private val rtkLocationInfoListener = RTKLocationInfoListener {
        rtkLocationInfoLD.postValue(DJIBaseResult.success(it))
    }
    private val rtkSystemStateListener = RTKSystemStateListener {
        rtkSystemStateLD.postValue(DJIBaseResult.success(it))
    }

    fun setAircraftRTKModuleEnabled(boolean: Boolean) {
        RTKCenter.getInstance().setAircraftRTKModuleEnabled(boolean, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                setAircraftRTKModuleEnableLD.postValue(DJIBaseResult.success())
            }

            override fun onFailure(error: IDJIError) {
                setAircraftRTKModuleEnableLD.postValue(DJIBaseResult.failed(error.toString()))

            }

        })
    }

    fun getAircraftRTKModuleEnabled() {
        RTKCenter.getInstance().getAircraftRTKModuleEnabled(object :
            CommonCallbacks.CompletionCallbackWithParam<Boolean> {
            override fun onSuccess(t: Boolean?) {
                getAircraftRTKModuleEnabledLD.postValue(DJIBaseResult.success(t))
            }

            override fun onFailure(error: IDJIError) {
                getAircraftRTKModuleEnabledLD.postValue(DJIBaseResult.failed(error.toString()))
            }

        })
    }

    fun getRTKReferenceStationSource() {
        RTKCenter.getInstance().getRTKReferenceStationSource(object : CommonCallbacks.CompletionCallbackWithParam<RTKReferenceStationSource> {
            override fun onSuccess(t: RTKReferenceStationSource?) {
                LogUtils.d(TAG,"getRTKReferenceStationSource,t=$t")
                rtkSourceLD.postValue(DJIBaseResult.success(t))
            }

            override fun onFailure(error: IDJIError) {
                getAircraftRTKModuleEnabledLD.postValue(DJIBaseResult.failed(error.toString()))
            }

        })
    }

    fun setRTKReferenceStationSource(source: RTKReferenceStationSource) {
        RTKCenter.getInstance().setRTKReferenceStationSource(source, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                setRTKReferenceStationSourceLD.postValue(DJIBaseResult.success())

            }

            override fun onFailure(error: IDJIError) {
                setRTKReferenceStationSourceLD.postValue(DJIBaseResult.failed(error.toString()))
            }

        })
    }

    fun addRTKLocationInfoListener() {
        RTKCenter.getInstance().addRTKLocationInfoListener(rtkLocationInfoListener)
    }

    fun addRTKSystemStateListener() {
        RTKCenter.getInstance().addRTKSystemStateListener(rtkSystemStateListener)
    }

    fun removeRTKLocationInfoListener() {
        RTKCenter.getInstance().removeRTKLocationInfoListener(rtkLocationInfoListener)
    }

    fun removeRTKSystemStateListener() {
        RTKCenter.getInstance().removeRTKSystemStateListener(rtkSystemStateListener)
    }

    fun clearAllRTKLocationInfoListener() {
        RTKCenter.getInstance().clearAllRTKLocationInfoListener()
    }

    fun clearAllRTKSystemStateListener() {
        RTKCenter.getInstance().clearAllRTKSystemStateListener()
    }
}