package dji.sampleV5.aircraft.models

import androidx.lifecycle.MutableLiveData
import dji.sampleV5.aircraft.data.DeviceLockStatus
import dji.sampleV5.aircraft.data.ModifyPasswordBean
import dji.sampleV5.aircraft.data.SecurityCodeOperationResult
import dji.sampleV5.aircraft.data.DJIToastResult
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.value.flightcontroller.*
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.common.utils.CallbackUtils
import dji.v5.et.action
import dji.v5.et.create
import dji.v5.et.listen
import dji.v5.utils.common.StringUtils

/**
 * Description :机载数据安全VM
 *
 * @author: Byte.Cai
 *  date : 2022/8/10
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class SecurityCodeVM : DJIViewModel() {
    companion object {
        //用户名，报纸唯一即可
        const val USER_NAME = "SecurityCode"
    }

    private var deviceStatusList = ArrayList<DeviceLockStatus>()
    var deviceStatusListLD = MutableLiveData<ArrayList<DeviceLockStatus>>()
    var operationResultTip=MutableLiveData<String>()

    fun verifyPassword(passWord: String, index: AccessLockerDeviceType, callback: CommonCallbacks.CompletionCallback? = null) {
        FlightControllerKey.KeyAccessLockerVerifySecurityCode.create().action(getAccountInfoForVerify(passWord, index), {
            CallbackUtils.onSuccess(callback)
        }, { error: IDJIError ->
            CallbackUtils.onFailure(callback, error)
            sendToastMsg(DJIToastResult.failed(error.toString()))

        })
    }

    fun setPassword(passWord: String, index: AccessLockerDeviceType, callback: CommonCallbacks.CompletionCallback? = null) {
        FlightControllerKey.KeyAccessLockerSetSecurityCode.create().action(getAccountInfoForSet(passWord, index), {
            CallbackUtils.onSuccess(callback)
        }, { error: IDJIError ->
            CallbackUtils.onFailure(callback, error)
            sendToastMsg(DJIToastResult.failed(error.toString()))

        })
    }

    fun resetPassword(index: AccessLockerDeviceType, callback: CommonCallbacks.CompletionCallback? = null) {
        FlightControllerKey.KeyAccessLockerResetSecurityCode.create().action(getAccountInfoForReSet(index), {
            CallbackUtils.onSuccess(callback)
        }, { error: IDJIError ->
            CallbackUtils.onFailure(callback, error)
            sendToastMsg(DJIToastResult.failed(error.toString()))

        })
    }

    fun modifyPassword(bean: ModifyPasswordBean, index: AccessLockerDeviceType, callback: CommonCallbacks.CompletionCallback? = null) {
        FlightControllerKey.KeyAccessLockerModifySecurityCode.create().action(getAccountInfoForModify(bean, index), {
            CallbackUtils.onSuccess(callback)
        }, { error: IDJIError ->
            CallbackUtils.onFailure(callback, error)
            sendToastMsg(DJIToastResult.failed(error.toString()))

        })
    }

    fun addQueryAllDeviceStatesListener() {
        FlightControllerKey.KeyAccessLockerAllDeviceStatus.create().listen(this) {
            deviceStatusList.clear()
            it?.onEach { accessLockerDeviceStatus ->
                val deviceType = accessLockerDeviceStatus.deviceType
                accessLockerDeviceStatus.sdCardEncryption.apply {
                    deviceStatusList.add(DeviceLockStatus(deviceType, isFeatureSupported, isFeatureEnabled, isFeatureNeedToBeVerified))
                }

            }
            deviceStatusListLD.postValue(deviceStatusList)
        }
        FlightControllerKey.KeyAccessLockerOperationResult.create().listen(this) {
            it?.run {
                if (storageType != AccessLockerStorageType.SD_CARD) {
                    return@run
                }
                val resultTip = StringUtils.getResStr(SecurityCodeOperationResult.find(retCode).desResId)
                operationResultTip.postValue(resultTip)
            }
        }
    }

    private fun getAccountInfoForVerify(passWord: String, index: AccessLockerDeviceType): AccessLockerVerifySecurityCodeInfo {
        return AccessLockerVerifySecurityCodeInfo(USER_NAME, passWord, index, AccessLockerStorageType.SD_CARD)
    }

    private fun getAccountInfoForSet(passWord: String, index: AccessLockerDeviceType): AccessLockerSetSecurityCodeInfo {
        return AccessLockerSetSecurityCodeInfo(USER_NAME, passWord, index, AccessLockerStorageType.SD_CARD)
    }

    private fun getAccountInfoForReSet(index: AccessLockerDeviceType): AccessLockerResetSecurityCodeInfo {
        return AccessLockerResetSecurityCodeInfo(USER_NAME, index, AccessLockerStorageType.SD_CARD)
    }

    private fun getAccountInfoForModify(bean: ModifyPasswordBean, index: AccessLockerDeviceType): AccessLockerModifySecurityCodeInfo {
        return AccessLockerModifySecurityCodeInfo(USER_NAME, bean.currentPassword, bean.newPassword, index, AccessLockerStorageType.SD_CARD)
    }

    private fun sendToastMsg(djiToastResult: DJIToastResult) {
        toastResult?.postValue(djiToastResult)
    }


}