package dji.sampleV5.aircraft.models

import androidx.lifecycle.MutableLiveData
import dji.sampleV5.aircraft.data.DJIToastResult
import dji.sdk.keyvalue.value.airlink.WlmDongleInfo
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.lte.*

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/8/12
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class LTEVM : DJIViewModel() {

    val lteAuthenticationInfo = MutableLiveData<LTEAuthenticationInfo>()
    val lteLinkInfo = MutableLiveData<LTELinkInfo>()
    val acWlmDongleInfo = MutableLiveData<MutableList<WlmDongleInfo>>()
    val rcWlmDongleInfo = MutableLiveData<MutableList<WlmDongleInfo>>()

    private val lteAuthenticationInfoListener = LTEAuthenticationInfoListener { info -> lteAuthenticationInfo.postValue(info) }

    private val lteLinkInfoListener = LTELinkInfoListener { info: LTELinkInfo -> lteLinkInfo.postValue(info) }

    private val lteDongleInfoListener = object :
        LTEDongleInfoListener {
        override fun onLTEAircraftDongleInfoUpdate(aircraftDongleInfos: MutableList<WlmDongleInfo>) {
            acWlmDongleInfo.postValue(aircraftDongleInfos)
        }

        override fun onLTERemoteControllerDongleInfoUpdate(remoteControllerDongleInfos: MutableList<WlmDongleInfo>) {
            rcWlmDongleInfo.postValue(remoteControllerDongleInfos)
        }
    }

    fun initListener() {
        LTEManager.getInstance().addLTEAuthenticationInfoListener(lteAuthenticationInfoListener)
        LTEManager.getInstance().addLTELinkInfoListener(lteLinkInfoListener)
        LTEManager.getInstance().addLTEDongleInfoListener(lteDongleInfoListener)
    }

    override fun onCleared() {
        KeyManager.getInstance().cancelListen(this)
        LTEManager.getInstance().removeLTEAuthenticationInfoListener(lteAuthenticationInfoListener)
        LTEManager.getInstance().removeLTELinkInfoListener(lteLinkInfoListener)
        LTEManager.getInstance().removeLTEDongleInfoListener(lteDongleInfoListener)
    }

    fun updateLTEAuthenticationInfo() {
        LTEManager.getInstance().updateLTEAuthenticationInfo(object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                toastResult?.postValue(DJIToastResult.success())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }

    fun getLTEAuthenticationVerificationCode(phoneAreaCode: String, phoneNumber: String) {
        LTEManager.getInstance().getLTEAuthenticationVerificationCode(phoneAreaCode, phoneNumber, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                toastResult?.postValue(DJIToastResult.success())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }

    fun startLTEAuthentication(phoneAreaCode: String, phoneNumber: String, verificationCode: String) {
        LTEManager.getInstance().startLTEAuthentication(phoneAreaCode, phoneNumber, verificationCode, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                toastResult?.postValue(DJIToastResult.success())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }

    fun setLTEEnhancedTransmissionType(lteLinkType: LTELinkType) {
        LTEManager.getInstance().setLTEEnhancedTransmissionType(lteLinkType, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                toastResult?.postValue(DJIToastResult.success())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }

    fun getLTEEnhancedTransmissionType() {
        LTEManager.getInstance().getLTEEnhancedTransmissionType(object :
            CommonCallbacks.CompletionCallbackWithParam<LTELinkType> {
            override fun onSuccess(lteLinkType: LTELinkType) {
                toastResult?.postValue(DJIToastResult.success(lteLinkType.name))
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }

    fun setLTEAcPrivatizationServerMsg(serverInfo: LTEPrivatizationServerInfo) {
        LTEManager.getInstance().setLTEAircraftPrivatizationServerInfo(serverInfo, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                toastResult?.postValue(DJIToastResult.success())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed("$error"))
            }
        })
    }

    fun setLTERcPrivatizationServerMsg(serverInfo: LTEPrivatizationServerInfo) {
        LTEManager.getInstance().setLTERemoteControllerPrivatizationServerInfo(serverInfo, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                toastResult?.postValue(DJIToastResult.success())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed("$error"))
            }
        })
    }

    fun clearLTEAcPrivatizationServerMsg() {
        LTEManager.getInstance().clearLTEAircraftPrivatizationServer(object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                toastResult?.postValue(DJIToastResult.success())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }

    fun clearLTERcPrivatizationServerMsg() {
        LTEManager.getInstance().clearLTERemoteControllerPrivatizationServer(object :
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