package dji.sampleV5.aircraft.models

import androidx.lifecycle.MutableLiveData
import dji.sampleV5.aircraft.data.DJIToastResult
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.aibox.IntelligentBoxAppInfo
import dji.v5.manager.aircraft.aibox.IntelligentBoxInfo
import dji.v5.manager.aircraft.aibox.IntelligentBoxInfoListener
import dji.v5.manager.aircraft.payload.PayloadCenter
import dji.v5.manager.aircraft.payload.PayloadIndexType


/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2022/12/1
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class IntelligentBoxVM : DJIViewModel() {
    private lateinit var payloadIndexType: PayloadIndexType
    private val intelligentBoxMap = PayloadCenter.getInstance().intelligentBoxManager
    val intelligentBoxInfo = MutableLiveData<IntelligentBoxInfo>()
    val intelligentBoxAppInfos = MutableLiveData<List<IntelligentBoxAppInfo>>()

    private val intelligentBoxInfoListener: IntelligentBoxInfoListener = object :
        IntelligentBoxInfoListener {

        override fun onBoxInfoUpdate(info: IntelligentBoxInfo) {
            intelligentBoxInfo.postValue(info)
        }

        override fun onBoxAppInfoUpdate(infos: List<IntelligentBoxAppInfo>) {
            intelligentBoxAppInfos.postValue(infos)
        }
    }

    fun getBoxSerialNumber() {
        intelligentBoxMap[payloadIndexType]?.getBoxSerialNumber(object :
            CommonCallbacks.CompletionCallbackWithParam<String> {
            override fun onSuccess(t: String) {
                sendToastMsg(DJIToastResult.success("getBoxSerialNumber: $t"))
            }

            override fun onFailure(error: IDJIError) {
                sendToastMsg(DJIToastResult.failed("getBoxSerialNumber,$error"))
            }
        })
    }

    fun enableApp(appID: String) {
        intelligentBoxMap[payloadIndexType]?.enableApp(appID, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                sendToastMsg(DJIToastResult.success("enableApp,success"))
            }

            override fun onFailure(error: IDJIError) {
                sendToastMsg(DJIToastResult.failed("enableApp,$error"))
            }
        })
    }

    fun disableApp(appID: String) {
        intelligentBoxMap[payloadIndexType]?.disableApp(appID, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                sendToastMsg(DJIToastResult.success("disableApp,success"))
            }

            override fun onFailure(error: IDJIError) {
                sendToastMsg(DJIToastResult.failed("disableApp,$error"))
            }
        })
    }

    fun uninstallApp(appID: String) {
        intelligentBoxMap[payloadIndexType]?.uninstallApp(appID, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                sendToastMsg(DJIToastResult.success("uninstallApp,success"))
            }

            override fun onFailure(error: IDJIError) {
                sendToastMsg(DJIToastResult.failed("uninstallApp,$error"))
            }
        })
    }

    fun initListener(payloadIndexType: PayloadIndexType) {
        this.payloadIndexType = payloadIndexType
        val iPayloadManager = intelligentBoxMap[payloadIndexType]
        iPayloadManager?.addBoxInfoListener(intelligentBoxInfoListener)
    }

    override fun onCleared() {
        super.onCleared()
        intelligentBoxMap[payloadIndexType]?.removeBoxInfoListener(intelligentBoxInfoListener)
    }

    private fun sendToastMsg(djiToastResult: DJIToastResult) {
        toastResult?.postValue(djiToastResult)
    }
}