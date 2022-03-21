package dji.sampleV5.moduleaircraft.models

import androidx.lifecycle.MutableLiveData
import dji.rtk.CoordinateSystem
import dji.sampleV5.modulecommon.models.DJIViewModel
import dji.sdk.keyvalue.key.RtkBaseStationKey
import dji.sdk.keyvalue.key.RtkMobileStationKey
import dji.sdk.keyvalue.value.rtkbasestation.RTKCustomNetworkSetting
import dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource
import dji.sdk.keyvalue.value.rtkbasestation.RTKServiceState
import dji.sdk.keyvalue.value.rtkmobilestation.RTKLocation
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.sdk.keyvalue.key.KeyTools
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.rtk.RTKCenter
import dji.v5.manager.aircraft.rtk.network.INetworkServiceInfoListener
import dji.v5.utils.common.LogUtils

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/7/23
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
class RTKVM : DJIViewModel() {

    val currentRTKState = MutableLiveData(RTKServiceState.RTCM_NORMAL)
    val currentRTKErrorMsg = MutableLiveData("")
    val currentCustomNetworkRTKSettings = MutableLiveData<RTKCustomNetworkSetting>()
    val currentQxNetworkCoordinateSystem = MutableLiveData<CoordinateSystem>()
    val rtkLocation = MutableLiveData<RTKLocation>()
    val rtkSource = MutableLiveData<RTKReferenceStationSource>()

    private val networkServiceInfoListener: INetworkServiceInfoListener = object :
        INetworkServiceInfoListener {
        override fun onServiceStateUpdate(state: RTKServiceState?) {
            state?.let {
                currentRTKState.value = state
            }
        }

        override fun onErrorCodeUpdate(code: IDJIError?) {
            code?.let {
                currentRTKErrorMsg.value = code.toString()
            }
        }
    }

    override fun onCleared() {
        RTKCenter.getInstance().customRTKManager.removeNetworkRTKServiceInfoListener(
            networkServiceInfoListener
        )
        RTKCenter.getInstance().qxrtkManager.removeNetworkRTKServiceInfoListener(
            networkServiceInfoListener
        )
        KeyManager.getInstance().cancelListen(this)
    }

    fun addNetworkRTKServiceInfoCallback() {
        RTKCenter.getInstance().customRTKManager.addNetworkRTKServiceInfoListener(
            networkServiceInfoListener
        )
        RTKCenter.getInstance().qxrtkManager.addNetworkRTKServiceInfoListener(
            networkServiceInfoListener
        )
    }

    fun listenRtkLocation() {
        KeyManager.getInstance().listen(
            KeyTools.createKey(RtkMobileStationKey.KeyRTKLocation),
            this, true
        ) { _, newValue ->
            rtkLocation.value = newValue
        }
    }

    fun listenRtkSource() {
        KeyManager.getInstance().listen(
            KeyTools.createKey(RtkBaseStationKey.KeyRTKReferenceStationSource),
            this, true
        ) { _, newValue ->
            rtkSource.value = newValue
        }
    }

    // custom network
    fun startCustomNetworkRTKService(callback: CommonCallbacks.CompletionCallback) {
        RTKCenter.getInstance().customRTKManager.startNetworkRTKService(callback)
    }

    fun stopCustomNetworkRTKService(callback: CommonCallbacks.CompletionCallback) {
        RTKCenter.getInstance().customRTKManager.stopNetworkRTKService(callback)
    }

    fun setCustomNetworkRTKSettings(settings: RTKCustomNetworkSetting) {
        RTKCenter.getInstance().customRTKManager.customNetworkRTKSettings = settings
        currentCustomNetworkRTKSettings.value =
            RTKCenter.getInstance().customRTKManager.customNetworkRTKSettings
    }

    fun getCustomNetworkRTKSettings() {
        currentCustomNetworkRTKSettings.value =
            RTKCenter.getInstance().customRTKManager.customNetworkRTKSettings
    }

    //qx network
    fun startQXNetworkRTKService(callback: CommonCallbacks.CompletionCallback) {
        RTKCenter.getInstance().qxrtkManager.startNetworkRTKService(callback)
    }

    fun stopQXNetworkRTKService(callback: CommonCallbacks.CompletionCallback) {
        RTKCenter.getInstance().qxrtkManager.stopNetworkRTKService(callback)
    }

    fun setQXNetworkRTKCoordinateSystem(
        coordinateSystem: CoordinateSystem,
        callback: CommonCallbacks.CompletionCallback
    ) {
        RTKCenter.getInstance().qxrtkManager.setNetworkRTKCoordinateSystem(
            coordinateSystem,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    callback.onSuccess()
                    currentQxNetworkCoordinateSystem.value = coordinateSystem
                }

                override fun onFailure(error: IDJIError) {
                    callback.onFailure(error)
                }
            })
    }

    fun getQXNetworkRTKCoordinateSystem() {
        RTKCenter.getInstance().qxrtkManager.getNetworkRTKCoordinateSystem(object :
            CommonCallbacks.CompletionCallbackWithParam<CoordinateSystem> {
            override fun onSuccess(coordinateSystem: CoordinateSystem?) {
                coordinateSystem?.let {
                    currentQxNetworkCoordinateSystem.value = it
                }
            }

            override fun onFailure(error: IDJIError) {
                LogUtils.i(logTag, "onFailure $error")
            }
        })
    }
}