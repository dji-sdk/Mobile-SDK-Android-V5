package dji.sampleV5.moduleaircraft.models

import androidx.lifecycle.MutableLiveData
import dji.sampleV5.modulecommon.data.DJIToastResult
import dji.sampleV5.modulecommon.models.DJIViewModel
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.et.create
import dji.v5.et.get
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.flysafe.FlySafeNotificationListener
import dji.v5.manager.aircraft.flysafe.FlyZoneManager
import dji.v5.manager.aircraft.flysafe.info.*

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/8/12
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class FlySafeVM : DJIViewModel() {

    val flySafeWarningInformation = MutableLiveData<FlySafeWarningInformation>()
    val flySafeSeriousWarningInformation = MutableLiveData<FlySafeSeriousWarningInformation>()
    val flySafeReturnToHomeInformation = MutableLiveData<FlySafeReturnToHomeInformation>()
    val flySafeTipInformation = MutableLiveData<FlySafeTipInformation>()
    val flyZoneInformation = MutableLiveData<MutableList<FlyZoneInformation>>()
    val serverFlyZoneLicenseInfo = MutableLiveData<MutableList<FlyZoneLicenseInfo>>()
    val aircraftFlyZoneLicenseInfo = MutableLiveData<MutableList<FlyZoneLicenseInfo>>()

    private val flySafeNotificationListener = object : FlySafeNotificationListener {

        override fun onWarningNotificationUpdate(info: FlySafeWarningInformation) {
            flySafeWarningInformation.postValue(info)
        }

        override fun onSeriousWarningNotificationUpdate(info: FlySafeSeriousWarningInformation) {
            flySafeSeriousWarningInformation.postValue(info)
        }

        override fun onReturnToHomeNotificationUpdate(info: FlySafeReturnToHomeInformation) {
            flySafeReturnToHomeInformation.postValue(info)
        }

        override fun onTipNotificationUpdate(info: FlySafeTipInformation) {
            flySafeTipInformation.postValue(info)
        }

        override fun onSurroundingFlyZonesUpdate(infos: MutableList<FlyZoneInformation>) {
            flyZoneInformation.postValue(infos)
        }
    }

    fun initListener() {
        FlyZoneManager.getInstance().addFlySafeNotificationListener(flySafeNotificationListener)
    }

    override fun onCleared() {
        KeyManager.getInstance().cancelListen(this)
        FlyZoneManager.getInstance().removeFlySafeNotificationListener(flySafeNotificationListener)
    }

    fun getAircraftLocation() = FlightControllerKey.KeyAircraftLocation.create().get(LocationCoordinate2D(0.0, 0.0))

    fun getFlyZonesInSurroundingArea(location: LocationCoordinate2D) {
        FlyZoneManager.getInstance().getFlyZonesInSurroundingArea(location, object :
            CommonCallbacks.CompletionCallbackWithParam<MutableList<FlyZoneInformation>> {

            override fun onSuccess(infos: MutableList<FlyZoneInformation>?) {
                toastResult?.postValue(DJIToastResult.success())
                flyZoneInformation.postValue(infos ?: arrayListOf())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }

    fun downloadFlyZoneLicensesFromServer() {
        FlyZoneManager.getInstance().downloadFlyZoneLicensesFromServer(object :
            CommonCallbacks.CompletionCallbackWithParam<MutableList<FlyZoneLicenseInfo>> {

            override fun onSuccess(infos: MutableList<FlyZoneLicenseInfo>?) {
                toastResult?.postValue(DJIToastResult.success())
                serverFlyZoneLicenseInfo.postValue(infos ?: arrayListOf())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }

    fun pushFlyZoneLicensesToAircraft() {
        FlyZoneManager.getInstance().pushFlyZoneLicensesToAircraft(object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                pullFlyZoneLicensesFromAircraft()
                toastResult?.postValue(DJIToastResult.success())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }

    fun pullFlyZoneLicensesFromAircraft() {
        FlyZoneManager.getInstance().pullFlyZoneLicensesFromAircraft(object :
            CommonCallbacks.CompletionCallbackWithParam<MutableList<FlyZoneLicenseInfo>> {

            override fun onSuccess(infos: MutableList<FlyZoneLicenseInfo>?) {
                toastResult?.postValue(DJIToastResult.success())
                aircraftFlyZoneLicenseInfo.postValue(infos ?: arrayListOf())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }

    fun deleteFlyZoneLicensesFromAircraft() {
        FlyZoneManager.getInstance().deleteFlyZoneLicensesFromAircraft(object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                toastResult?.postValue(DJIToastResult.success())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }

    fun setFlyZoneLicensesEnabled(info: FlyZoneLicenseInfo, isEnable: Boolean) {
        FlyZoneManager.getInstance().setFlyZoneLicensesEnabled(info, isEnable, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                pullFlyZoneLicensesFromAircraft()
                toastResult?.postValue(DJIToastResult.success())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }

    fun unlockAuthorizationFlyZone(flyZoneID: Int) {
        FlyZoneManager.getInstance().unlockAuthorizationFlyZone(flyZoneID, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                toastResult?.postValue(DJIToastResult.success())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }

    fun unlockAllEnhancedWarningFlyZone() {
        FlyZoneManager.getInstance().unlockAllEnhancedWarningFlyZone(object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                toastResult?.postValue(DJIToastResult.success())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }
}