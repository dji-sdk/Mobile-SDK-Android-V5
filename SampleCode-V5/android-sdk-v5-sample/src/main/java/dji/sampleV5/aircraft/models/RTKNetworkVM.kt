package dji.sampleV5.aircraft.models

import androidx.lifecycle.MutableLiveData
import dji.rtk.CoordinateSystem
import dji.sdk.keyvalue.value.rtkbasestation.RTKCustomNetworkSetting
import dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource
import dji.sdk.keyvalue.value.rtkbasestation.RTKServiceState
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.rtk.RTKCenter
import dji.v5.manager.aircraft.rtk.network.INetworkServiceInfoListener
import dji.v5.utils.common.LogUtils
import dji.v5.ux.accessory.RTKUtil

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/7/23
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
class RTKNetworkVM : DJIViewModel() {
    companion object {
        const val TAG = "RTKNetworkVM"
        const val CUSTOM_RTK_SETTING_CACHE = "customRTKSettingChache"
    }

    val currentRTKState = MutableLiveData(RTKServiceState.UNKNOWN)
    val currentRTKErrorMsg = MutableLiveData("")
    val currentCustomNetworkRTKSettings = MutableLiveData<RTKCustomNetworkSetting>()
    val currentQxNetworkCoordinateSystem = MutableLiveData<CoordinateSystem>()


    private val networkServiceInfoListener: INetworkServiceInfoListener = object :
        INetworkServiceInfoListener {
        override fun onServiceStateUpdate(state: RTKServiceState?) {
            state?.let {
                currentRTKState.postValue(state)
                //启动成功，清除历史遗留的错误信息
                if (it == RTKServiceState.TRANSMITTING) {
                    currentRTKErrorMsg.postValue("")
                }
            }
        }

        override fun onErrorCodeUpdate(code: IDJIError?) {
            code?.let {
                currentRTKErrorMsg.postValue(code.toString())
            }
        }
    }

    fun getCurrentCustomNetworkRTKSettingCache(): String {
        return RTKUtil.getRtkCustomNetworkSetting().toString()
    }

    fun addNetworkRTKServiceInfoCallback() {
        RTKCenter.getInstance().customRTKManager.addNetworkRTKServiceInfoListener(
            networkServiceInfoListener
        )
        RTKCenter.getInstance().qxrtkManager.addNetworkRTKServiceInfoListener(
            networkServiceInfoListener
        )
        RTKCenter.getInstance().cmccrtkManager.addNetworkRTKServiceInfoListener(
            networkServiceInfoListener
        )
    }

    fun removeNetworkServiceInfoListener() {
        RTKCenter.getInstance().customRTKManager.removeNetworkRTKServiceInfoListener(
            networkServiceInfoListener
        )
        RTKCenter.getInstance().qxrtkManager.removeNetworkRTKServiceInfoListener(
            networkServiceInfoListener
        )
        RTKCenter.getInstance().cmccrtkManager.removeNetworkRTKServiceInfoListener(
            networkServiceInfoListener
        )
    }


    // custom network
    fun startCustomNetworkRTKService(callback: CommonCallbacks.CompletionCallback) {
        RTKCenter.getInstance().customRTKManager.startNetworkRTKService(null, callback)
    }

    fun stopCustomNetworkRTKService(callback: CommonCallbacks.CompletionCallback) {
        RTKCenter.getInstance().customRTKManager.stopNetworkRTKService(callback)
    }

    fun setCustomNetworkRTKSettings(settings: RTKCustomNetworkSetting) {
        RTKCenter.getInstance().customRTKManager.customNetworkRTKSettings = settings
        currentCustomNetworkRTKSettings.postValue(settings)
        RTKUtil.saveRtkCustomNetworkSetting(settings)
    }


    //qx network
    fun startQXNetworkRTKService(coordinateSystem: CoordinateSystem, callback: CommonCallbacks.CompletionCallback) {
        RTKCenter.getInstance().qxrtkManager.startNetworkRTKService(coordinateSystem, callback)
        RTKUtil.saveRTKCoordinateSystem(RTKReferenceStationSource.QX_NETWORK_SERVICE, coordinateSystem)
    }

    fun stopQXNetworkRTKService(callback: CommonCallbacks.CompletionCallback) {
        RTKCenter.getInstance().qxrtkManager.stopNetworkRTKService(callback)
    }

    fun getQXNetworkRTKCoordinateSystem() {
        RTKCenter.getInstance().qxrtkManager.getNetworkRTKCoordinateSystem(object :
            CommonCallbacks.CompletionCallbackWithParam<CoordinateSystem> {
            override fun onSuccess(coordinateSystem: CoordinateSystem?) {
                coordinateSystem?.let {
                    currentQxNetworkCoordinateSystem.postValue(it)
                }
            }

            override fun onFailure(error: IDJIError) {
                LogUtils.i(TAG, "getQXNetworkRTKCoordinateSystem onFailure $error")
            }
        })
    }

    //cmcc rtk
    fun startCMCCRTKService(coordinateSystem: CoordinateSystem, callback: CommonCallbacks.CompletionCallback) {
        RTKCenter.getInstance().cmccrtkManager.startNetworkRTKService(coordinateSystem, callback)
        RTKUtil.saveRTKCoordinateSystem(RTKReferenceStationSource.NTRIP_NETWORK_SERVICE, coordinateSystem)
    }

    fun stopCMCCRTKService(callback: CommonCallbacks.CompletionCallback) {
        RTKCenter.getInstance().cmccrtkManager.stopNetworkRTKService(callback)
    }
}