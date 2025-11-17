package dji.sampleV5.aircraft.models

import androidx.lifecycle.MutableLiveData
import dji.sampleV5.aircraft.data.DJIToastResult
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.payload.PayloadCenter
import dji.v5.manager.aircraft.payload.PayloadIndexType
import dji.v5.manager.aircraft.payload.listener.PayloadDataListener
import dji.v5.utils.common.LogPath
import dji.v5.utils.common.LogUtils
import java.text.SimpleDateFormat

/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2022/2/22
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class PayLoadDataVM : DJIViewModel() {
    private lateinit var payloadIndexType: PayloadIndexType
    val receiveMessageLiveData = MutableLiveData<String>()
    private val payloadManagerMap = PayloadCenter.getInstance().payloadManager
    private val payloadDataListener = PayloadDataListener {
        var result = "接收时间：${getTimeNow()}"
        if (it.isNotEmpty()) {
            val newValueString = String(it)
            result += "，接收内容：$newValueString"
            LogUtils.i(LogPath.PAYLOAD,result)
            receiveMessageLiveData.postValue(result)
        } else {
            result += ",接收内容为空"
            receiveMessageLiveData.postValue(result)
        }
    }

    fun sendMessageToPayLoadSdk(byteArray: ByteArray) {
        payloadManagerMap[payloadIndexType]?.sendDataToPayload(byteArray, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                sendToastMsg(DJIToastResult.success("Send success"))

            }

            override fun onFailure(error: IDJIError) {
                sendToastMsg(DJIToastResult.failed(error.toString()))

            }

        })
    }

    fun initPayloadDataListener(payloadIndexType: PayloadIndexType) {
        this.payloadIndexType = payloadIndexType
        payloadManagerMap[payloadIndexType]?.addPayloadDataListener(payloadDataListener)
    }

    override fun onCleared() {
        super.onCleared()
        payloadManagerMap[payloadIndexType]?.removePayloadDataListener(payloadDataListener)
    }

    private fun getTimeNow(): String {
        val currentTime = System.currentTimeMillis()
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(currentTime)
    }

    private fun sendToastMsg(djiToastResult: DJIToastResult) {
        toastResult?.postValue(djiToastResult)
    }
}