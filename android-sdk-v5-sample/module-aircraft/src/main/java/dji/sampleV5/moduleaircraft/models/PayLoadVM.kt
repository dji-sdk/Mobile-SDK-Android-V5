package dji.sampleV5.moduleaircraft.models

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dji.sampleV5.modulecommon.models.DJIViewModel
import dji.sampleV5.moduleaircraft.data.DJIBaseResult
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
    private val sendMessageLiveData = MutableLiveData<DJIBaseResult<String>>()
    private val receiveMessageLiveData = MutableLiveData<DJIBaseResult<String>>()
    private val productNameLiveData = MutableLiveData<DJIBaseResult<String>>()

    fun getSendMessageLiveData(): LiveData<DJIBaseResult<String>> {
        return sendMessageLiveData
    }

    fun getReceiveMessageLiveData(): LiveData<DJIBaseResult<String>> {
        return receiveMessageLiveData
    }

    fun getProductNameLiveData(): LiveData<DJIBaseResult<String>> {
        return productNameLiveData
    }

    fun sendMessageToPayLoadSdk(byteArray: ByteArray) {
        KeyManager.getInstance().performAction(
            KeyTools.createKey(PayloadKey.KeySendDataToPayload, ComponentIndexType.UP),
            byteArray,
            object : CommonCallbacks.CompletionCallbackWithParam<EmptyMsg> {
                override fun onSuccess(t: EmptyMsg?) {
                    sendMessageLiveData.postValue(DJIBaseResult.success(t.toString()))
                }

                override fun onFailure(error: IDJIError) {
                    sendMessageLiveData.postValue(DJIBaseResult.failed(error.toString()))
                }

            })
    }

    fun receiveFromPayLoadSdk() {
        KeyManager.getInstance().listen(
            KeyTools.createKey(PayloadKey.KeyDataFromPayload, ComponentIndexType.UP), this
        ) { oldValue, newValue ->
            var result = "接收时间：${getTimeNow()}"
            newValue?.let {
                var newValueString = String(newValue)
                result += "，接收内容：$newValueString"
                receiveMessageLiveData.postValue(DJIBaseResult.success(result))
                return@listen
            }
            result += ",接收内容为空"
            receiveMessageLiveData.postValue(DJIBaseResult.success(result))
        }
    }

    fun getProductName() {
        val productName =
            KeyManager.getInstance().getValue(
                KeyTools.createKey(
                    PayloadKey.KeyPayloadProductName,
                    ComponentIndexType.UP
                )
            )
        if (TextUtils.isEmpty(productName)) {//如果获取到的缓存为空，则走异步获取流程
            PayloadKey.KeyPayloadProductName.create(ComponentIndexType.UP).get({
                productNameLiveData.postValue(DJIBaseResult.success(it))
            }) {
                productNameLiveData.postValue(DJIBaseResult.failed(it.toString()))
            }
        } else {
            productNameLiveData.postValue(DJIBaseResult.success(productName))
        }
    }

    private fun getTimeNow(): String {
        val currentTime = System.currentTimeMillis()
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(currentTime)
    }
}