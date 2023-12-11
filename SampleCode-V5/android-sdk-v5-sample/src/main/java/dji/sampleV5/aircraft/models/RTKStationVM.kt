package dji.sampleV5.aircraft.models

import androidx.lifecycle.MutableLiveData
import dji.sampleV5.aircraft.data.DJIToastResult
import dji.sampleV5.aircraft.data.DJIRTKBaseStationConnectInfo

import dji.sdk.keyvalue.value.common.LocationCoordinate3D
import dji.sdk.keyvalue.value.rtkbasestation.RTKBaseStationResetPasswordInfo
import dji.sdk.keyvalue.value.rtkbasestation.RTKStationConnetState
import dji.sdk.keyvalue.value.rtkbasestation.RTKStationInfo

import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.rtk.RTKCenter
import dji.v5.manager.aircraft.rtk.RTKLocationInfo
import dji.v5.manager.aircraft.rtk.station.*
import dji.v5.utils.common.LogUtils
import java.util.ArrayList

/**
 * Description :基站RTK数据存储类
 *
 * @author: Byte.Cai
 *  date : 2022/3/4
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class RTKStationVM : DJIViewModel() {
    private val TAG = "StationRTKVM"
    private val rtkStationManager = RTKCenter.getInstance().rtkStationManager

    val stationListLD = MutableLiveData<List<DJIRTKBaseStationConnectInfo>>()

    val appStationConnectStatusLD = MutableLiveData<RTKStationConnetState>()

    val appStationConnectedInfoLD = MutableLiveData<ConnectedRTKStationInfo>()

    val stationPositionLD = MutableLiveData<LocationCoordinate3D?>()

    val rtkLocationLD = MutableLiveData<RTKLocationInfo>()


    private val searchStationListener =
        SearchRTKStationListener { newConnectInfoList ->
            val convertToDJIRTKBaseStationConnectInfo = convertToDJIRTKBaseStationConnectInfo(newConnectInfoList)
            stationListLD.postValue(convertToDJIRTKBaseStationConnectInfo)
        }

    private val stationConnectStatusListener =
        RTKStationConnectStatusListener { newRtkBaseStationConnectState ->
            appStationConnectStatusLD.postValue(newRtkBaseStationConnectState)
        }

    private val connectedRTKStationInfoListener =
        ConnectedRTKStationInfoListener { newValue ->
            appStationConnectedInfoLD.postValue(newValue)
        }

    fun startSearchStation() {
        LogUtils.i(TAG, "startSearchStation")
        rtkStationManager.startSearchRTKStation(object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                sendToastMsg(DJIToastResult.success("Searing rtk station..."))
            }

            override fun onFailure(error: IDJIError) {
                sendToastMsg(DJIToastResult.failed(error.toString()))

            }

        })
    }

    fun stopSearchStation() {
        rtkStationManager.stopSearchRTKStation(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                sendToastMsg(DJIToastResult.success("Stop search station  success"))

            }

            override fun onFailure(error: IDJIError) {
                sendToastMsg(DJIToastResult.failed(error.toString()))
            }

        })
    }

    fun addSearchRTKStationListener() {
        rtkStationManager.addSearchRTKStationListener(searchStationListener)
    }

    fun removeSearchRTKStationListener() {
        rtkStationManager.removeSearchRTKStationListener(searchStationListener)
    }

    fun addStationConnectStatusListener() {
        rtkStationManager.addRTKStationConnectStatusListener(stationConnectStatusListener)
    }

    fun addRTKLocationListener() {
        RTKCenter.getInstance().addRTKLocationInfoListener {
            rtkLocationLD.postValue(it)
        }
    }

    fun removeStationConnectStatusListener() {
        rtkStationManager.removeRTKStationConnectStatusListener(stationConnectStatusListener)
    }

    fun addConnectedRTKStationInfoListener() {
        rtkStationManager.addConnectedRTKStationInfoListener(connectedRTKStationInfoListener)
    }

    fun removeConnectedRTKStationInfoListener() {
        rtkStationManager.removeConnectedRTKStationInfoListener(connectedRTKStationInfoListener)
    }

    fun clearAllConnectedRTKStationInfoListener() {
        rtkStationManager.clearAllConnectedRTKStationInfoListener()
    }

    fun clearAllSearchRTKStationListener() {
        rtkStationManager.clearAllSearchRTKStationListener()
    }

    fun clearAllStationConnectStatusListener() {
        rtkStationManager.clearAllRTKStationConnectStatusListener()
    }

    fun startConnectToRTKStation(stationId: Int) {
        rtkStationManager.startConnectToRTKStation(stationId, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                sendToastMsg(DJIToastResult.success("Station connecting..."))
            }

            override fun onFailure(error: IDJIError) {
                sendToastMsg(DJIToastResult.failed(error.toString()))
            }

        })
    }

    fun loginAsAdmin(password: String, callbacks: CommonCallbacks.CompletionCallback) {
        rtkStationManager.loginRTKStation(password, callbacks)
    }

    fun setRTKStationPosition(locationCoordinate3D: LocationCoordinate3D) {
        rtkStationManager.setRTKStationReferencePosition(
            locationCoordinate3D,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    sendToastMsg(DJIToastResult.success("SetRTKStationPosition success"))
                }

                override fun onFailure(error: IDJIError) {
                    sendToastMsg(DJIToastResult.failed(error.toString()))
                }

            })
    }

    fun getRTKStationPosition() {
        rtkStationManager.getRTKStationReferencePosition(object :
            CommonCallbacks.CompletionCallbackWithParam<LocationCoordinate3D> {
            override fun onSuccess(locationCoordinate3D: LocationCoordinate3D?) {
                stationPositionLD.postValue(locationCoordinate3D)
            }

            override fun onFailure(error: IDJIError) {
                sendToastMsg(DJIToastResult.failed(error.toString()))
            }

        })
    }

    fun resetRTKStationPosition() {
        rtkStationManager.resetRTKStationReferencePosition(object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                sendToastMsg(DJIToastResult.success("Reset station position success"))

            }

            override fun onFailure(error: IDJIError) {
                sendToastMsg(DJIToastResult.failed(error.toString()))

            }


        })
    }

    fun resetStationPassword(passwordParam: RTKBaseStationResetPasswordInfo) {
        rtkStationManager.resetRTKStationPassword(passwordParam, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                sendToastMsg(DJIToastResult.success("Reset station password success"))

            }

            override fun onFailure(error: IDJIError) {
                sendToastMsg(DJIToastResult.failed(error.toString()))

            }


        })
    }

    fun setStationName(name: String) {
        rtkStationManager.setRTKStationName(name, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                sendToastMsg(DJIToastResult.success("Set station name success"))
            }

            override fun onFailure(error: IDJIError) {
                sendToastMsg(DJIToastResult.failed(error.toString()))

            }


        })
    }


    private fun convertToDJIRTKBaseStationConnectInfo(list: List<RTKStationInfo>?): ArrayList<DJIRTKBaseStationConnectInfo> {
        val djiRTKBaseStationConnectInfoList = ArrayList<DJIRTKBaseStationConnectInfo>()
        list?.let {
            for (i in list) {
                val djirtkBaseStationConnectInfo = i.toDJIRTKBaseStationConnectInfo()
                djiRTKBaseStationConnectInfoList.add(djirtkBaseStationConnectInfo)
            }
        }
        return djiRTKBaseStationConnectInfoList

    }


    private fun RTKStationInfo.toDJIRTKBaseStationConnectInfo(): DJIRTKBaseStationConnectInfo {
        val baseStationId = this.stationId
        val name = this.stationName
        val signalLevel = this.signalLevel
        return DJIRTKBaseStationConnectInfo(baseStationId, signalLevel, name)
    }

    private fun sendToastMsg(djiToastResult: DJIToastResult) {
        toastResult?.postValue(djiToastResult)
    }

}