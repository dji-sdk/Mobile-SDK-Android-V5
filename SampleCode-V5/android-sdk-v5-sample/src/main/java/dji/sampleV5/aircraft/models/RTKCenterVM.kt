package dji.sampleV5.aircraft.models

import androidx.lifecycle.MutableLiveData
import dji.sampleV5.aircraft.data.DJIToastResult
import dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.rtk.*


/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2022/3/19
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class RTKCenterVM : DJIViewModel() {
    val aircraftRTKModuleEnabledLD = MutableLiveData<Boolean?>()
    val rtkLocationInfoLD = MutableLiveData<RTKLocationInfo>()
    val rtkSystemStateLD = MutableLiveData<RTKSystemState>()
    val rtkAccuracyMaintainLD = MutableLiveData<Boolean?>()

    private val rtkLocationInfoListener = RTKLocationInfoListener {
        rtkLocationInfoLD.postValue(it)
    }
    private val rtkSystemStateListener = RTKSystemStateListener {
        rtkSystemStateLD.postValue(it)
    }

    fun setAircraftRTKModuleEnabled(boolean: Boolean) {
        RTKCenter.getInstance().setAircraftRTKModuleEnabled(boolean, object :CommonCallbacks.CompletionCallback{
            override fun onSuccess() {
                //结果以添加的listener结果为准
            }

            override fun onFailure(error: IDJIError) {
                aircraftRTKModuleEnabledLD.postValue(false)
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }

        })
    }


    fun setRTKReferenceStationSource(source: RTKReferenceStationSource) {
        RTKCenter.getInstance().setRTKReferenceStationSource(source, null)
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


    fun setRTKMaintainAccuracyEnabled(enable: Boolean) {
        RTKCenter.getInstance().setRTKMaintainAccuracyEnabled(enable, null)
    }

}