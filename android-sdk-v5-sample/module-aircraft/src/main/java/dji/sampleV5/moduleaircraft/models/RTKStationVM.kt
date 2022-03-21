package dji.sampleV5.moduleaircraft.models

import androidx.lifecycle.MutableLiveData
import dji.sampleV5.modulecommon.models.DJIViewModel
import dji.sampleV5.moduleaircraft.data.DJIBaseResult
import dji.sampleV5.moduleaircraft.data.DJIRTKBaseStationConnectInfo
import dji.sdk.keyvalue.value.common.LocationCoordinate3D
import dji.sdk.keyvalue.value.rtkbasestation.RTKBaseStationResetPasswordInfo
import dji.sdk.keyvalue.value.rtkbasestation.RTKStationConnetState
import dji.sdk.keyvalue.value.rtkbasestation.RTKStationInfo

import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.rtk.RTKCenter
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
    val startSearchStationLD = MutableLiveData<DJIBaseResult<String>>()
    val stopSearchStationLD = MutableLiveData<DJIBaseResult<String>>()

    val stationListLD = MutableLiveData<DJIBaseResult<List<DJIRTKBaseStationConnectInfo>>>()

    val connectRTKStationLD = MutableLiveData<DJIBaseResult<Boolean>>()

    val appStationConnectStatusLD = MutableLiveData<DJIBaseResult<RTKStationConnetState>>()

    val appStationConnectedInfoLD = MutableLiveData<DJIBaseResult<ConnectedRTKStationInfo>>()


    val loginLD = MutableLiveData<DJIBaseResult<Boolean>>()

    val setStationPositionLD = MutableLiveData<DJIBaseResult<Boolean>>()
    val getStationPositionLD = MutableLiveData<DJIBaseResult<LocationCoordinate3D>>()

    val resetStationPositionLD = MutableLiveData<DJIBaseResult<Boolean>>()

    val resetStationPasswordLD = MutableLiveData<DJIBaseResult<Boolean>>()

    val setStationNameLD = MutableLiveData<DJIBaseResult<Boolean>>()

    private val searchStationListener =
        SearchRTKStationListener { newConnectInfoList ->
            val convertToDJIRTKBaseStationConnectInfo =
                convertToDJIRTKBaseStationConnectInfo(newConnectInfoList)
            stationListLD.postValue(
                DJIBaseResult.success(
                    convertToDJIRTKBaseStationConnectInfo
                )
            )

        }

    private val stationConnectStatusListener =
        RTKStationConnectStatusListener { newRtkBaseStationConnectState ->
            appStationConnectStatusLD.postValue(
                DJIBaseResult.success(
                    newRtkBaseStationConnectState
                )
            )
        }

    private val connectedRTKStationInfoListener =
        ConnectedRTKStationInfoListener { newValue ->
            appStationConnectedInfoLD.postValue(
                DJIBaseResult.success(newValue)
            )
        }


    fun startSearchStation() {
        LogUtils.d(TAG, "startSearchStation")
        rtkStationManager.startSearchRTKStation(object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                startSearchStationLD.postValue(DJIBaseResult.success())
            }

            override fun onFailure(error: IDJIError) {
                startSearchStationLD.postValue(DJIBaseResult.failed(error.toString()))

            }

        })
    }

    fun stopSearchStation() {
        rtkStationManager.stopSearchRTKStation(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                stopSearchStationLD.postValue(DJIBaseResult.success())

            }

            override fun onFailure(error: IDJIError) {
                stopSearchStationLD.postValue(DJIBaseResult.failed(error.toString()))
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
                connectRTKStationLD.postValue(DJIBaseResult.success())
            }

            override fun onFailure(error: IDJIError) {
                connectRTKStationLD.postValue(DJIBaseResult.failed(error.toString()))

            }

        })
    }

    fun loginAsAdmin(password: String) {
        rtkStationManager.loginRTKStation(password, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                loginLD.postValue(DJIBaseResult.success())

            }

            override fun onFailure(error: IDJIError) {
                loginLD.postValue(DJIBaseResult.failed(error.toString()))

            }


        })
    }

    fun setRTKStationPosition(locationCoordinate3D: LocationCoordinate3D) {
        rtkStationManager.setRTKStationReferencePosition(
            locationCoordinate3D,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    setStationPositionLD.postValue(DJIBaseResult.success())

                }

                override fun onFailure(error: IDJIError) {
                    setStationPositionLD.postValue(DJIBaseResult.failed(error.toString()))
                }

            });
    }

    fun getRTKStationPosition() {
        rtkStationManager.getRTKStationReferencePosition(object :
            CommonCallbacks.CompletionCallbackWithParam<LocationCoordinate3D> {
            override fun onSuccess(locationCoordinate3D: LocationCoordinate3D?) {
                getStationPositionLD.postValue(DJIBaseResult.success(locationCoordinate3D))

            }

            override fun onFailure(error: IDJIError) {
                getStationPositionLD.postValue(DJIBaseResult.failed(error.toString()))
            }

        });
    }

    fun resetRTKStationPosition() {
        rtkStationManager.resetRTKStationReferencingPosition(object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                resetStationPositionLD.postValue(DJIBaseResult.success())

            }

            override fun onFailure(error: IDJIError) {
                resetStationPositionLD.postValue(DJIBaseResult.failed(error.toString()))

            }


        })
    }

    fun resetStationPassword(passwordParam: RTKBaseStationResetPasswordInfo) {
        rtkStationManager.resetRTKStationPassword(passwordParam, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                resetStationPasswordLD.postValue(DJIBaseResult.success())

            }

            override fun onFailure(error: IDJIError) {
                resetStationPasswordLD.postValue(DJIBaseResult.failed(error.toString()))

            }


        })
    }

    fun setStationName(name: String) {
        rtkStationManager.setRTKStationName(name, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                setStationNameLD.postValue(DJIBaseResult.success())
            }

            override fun onFailure(error: IDJIError) {
                setStationNameLD.postValue(DJIBaseResult.failed(error.toString()))

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


}