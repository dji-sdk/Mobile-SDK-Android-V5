package dji.sampleV5.moduleaircraft.models

import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import dji.sampleV5.modulecommon.models.DJIViewModel
import dji.sampleV5.modulecommon.data.DJIToastResult
import dji.sampleV5.modulecommon.util.DJIToastUtil
import dji.sdk.keyvalue.key.PayloadKey
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.common.EmptyMsg
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.sdk.keyvalue.key.KeyTools
import dji.v5.et.create
import dji.v5.et.get
import dji.v5.manager.KeyManager
import java.text.SimpleDateFormat

/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2022/2/22
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class PayLoadVM : DJIViewModel() {
    val receiveMessageLiveData = MutableLiveData<String>()


    fun sendMessageToPayLoadSdk(byteArray: ByteArray) {
        KeyManager.getInstance().performAction(
            KeyTools.createKey(PayloadKey.KeySendDataToPayload, ComponentIndexType.UP),
            byteArray,
            object : CommonCallbacks.CompletionCallbackWithParam<EmptyMsg> {
                override fun onSuccess(t: EmptyMsg?) {
                    sendToastMsg(DJIToastResult.success("Send success"))
                }

                override fun onFailure(error: IDJIError) {
                    sendToastMsg(DJIToastResult.failed(error.toString()))
                }

            })
    }

    fun receiveFromPayLoadSdk() {
        KeyManager.getInstance().listen(KeyTools.createKey(PayloadKey.KeyDataFromPayload, ComponentIndexType.UP), this) { oldValue, newValue ->
            var result = "接收时间：${getTimeNow()}"
            newValue?.let {
                var newValueString = String(newValue)
                result += "，接收内容：$newValueString"
                receiveMessageLiveData.postValue(result)
                return@listen
            }
            result += ",接收内容为空"
            receiveMessageLiveData.postValue(result)
        }
    }

    fun getProductName() {
        val productName = KeyManager.getInstance().getValue(KeyTools.createKey(PayloadKey.KeyPayloadProductName, ComponentIndexType.UP))
        if (TextUtils.isEmpty(productName)) {//如果获取到的缓存为空，则走异步获取流程
            PayloadKey.KeyPayloadProductName.create(ComponentIndexType.UP).get({
                sendToastMsg(DJIToastResult.success(it))
            }) {
                sendToastMsg(DJIToastResult.failed(it.toString()))
            }
        } else {
            sendToastMsg(DJIToastResult.success(productName))
        }
    }

    private fun getTimeNow(): String {
        val currentTime = System.currentTimeMillis()
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(currentTime)
    }

    private fun sendToastMsg(djiToastResult: DJIToastResult) {
        toastResult?.postValue(djiToastResult)
    }
}