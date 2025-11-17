package dji.sampleV5.aircraft.models

import androidx.lifecycle.MutableLiveData
import dji.sampleV5.aircraft.util.ToastUtils
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.mop.PipelineDeviceType
import dji.sdk.keyvalue.value.mop.TransmissionControlType
import dji.v5.common.error.DJIPipeLineError
import dji.v5.manager.mop.DataResult

import dji.v5.manager.mop.Pipeline
import dji.v5.manager.mop.PipelineManager
import dji.v5.utils.common.DJIExecutor
import dji.v5.utils.common.LogUtils
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService

/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2023/1/31
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class MopVM : DJIViewModel() {

    private var isStop = false
    val receiveMessageLiveData = MutableLiveData<String>()
    val pipelineMapLivData = MutableLiveData<Map<Int, Pipeline>>()
    private val data = ByteArray(19004)
    private val executorService: ExecutorService = DJIExecutor.getExecutorFor(DJIExecutor.Purpose.URGENT)
    private var mReadDataDisposable: Disposable? = null
    private var pipeline: Pipeline? = null
    private var currentConnectParam: Param? = null


    fun initListener() {
        PipelineManager.getInstance().addPipelineConnectionListener {
            LogUtils.i(logTag, it.values)
            pipelineMapLivData.postValue(it)
        }
    }

    fun connect(index: ComponentIndexType, id: Int, deviceType: PipelineDeviceType, transmissionControlType: TransmissionControlType, isUseForDown: Boolean = false) {
        executorService.execute {
            val error = PipelineManager.getInstance()
                .connectPipeline(index, id, deviceType, transmissionControlType)
            if (error == null) {
                currentConnectParam = Param(index, id, deviceType = deviceType, transmissionControlType = transmissionControlType)
                isStop = false
                pipeline = PipelineManager.getInstance().pipelines[id]
                ToastUtils.showToast("Connect Success")
                if (!isUseForDown) {
                    readData()
                }
            } else {
                ToastUtils.showToast("Connect Fail:$error")
            }
        }
    }

    fun readData() {
        var time = "Start time：${getTimeNow()}"
        val result = pipeline?.readData(data) ?: DataResult()
        val len = result.length
        time += "，Receive time：${getTimeNow()}，“len:${len}"
        if (len > 0) {
            if (data.isNotEmpty()) {
                val newValueString = String(data)
                time += "，Receive content：$newValueString"
                receiveMessageLiveData.postValue(time)
            } else {
                time += ",Receive content is empty"
                receiveMessageLiveData.postValue(time)
            }
        } else if (len == 0) {
            LogUtils.e(logTag, "can not read anything")
        } else {
            LogUtils.e(logTag, "mop error，result=$result")
            // len < 0 && result.error != MOP_ERROR_CODE_TIMEOUT 的一般情况错误都走断开逻辑。
            if (!isStop && !result.error.errorCode().equals(DJIPipeLineError.TIMEOUT)) {
                stopMop()
            }
        }
        if (!isStop) {
            readData()
        }
    }

    fun sendData(byteArray: ByteArray) {
        executorService.submit {
            if (pipeline == null) {
                ToastUtils.showToast("MOP is not connected, please trigger connection first and try again")
                return@submit
            }

            val result = pipeline?.writeData(byteArray)
            if (result?.error != null) {
                ToastUtils.showToast("sendData error:${result.error}")
            } else {
                ToastUtils.showToast("sendData success, length is:${result?.length}")
            }
        }
    }

    private fun disconnectMop() {
        executorService.execute {
            pipeline?.let {
                currentConnectParam?.let {
                    isStop = true
                    LogUtils.d("start to disconnectMop")
                    val error = PipelineManager.getInstance()
                        .disconnectPipeline(it.index, it.id, it.deviceType, it.transmissionControlType)
                    AndroidSchedulers.mainThread().scheduleDirect {
                        if (error == null) {
                            ToastUtils.showToast("disconnectPipeline success")
                            LogUtils.d("disconnectMop: disconnect successful")
                        } else {
                            isStop = false
                            ToastUtils.showToast("disconnectPipeline fail:$error")
                            LogUtils.d("disconnectMop: Error disconnecting: $error")
                        }
                    }
                }


            }
        }
    }

    private fun stopReadDataTimer() {
        LogUtils.d("Stopping read data timer start")

        mReadDataDisposable?.let {
            mReadDataDisposable?.dispose()
            mReadDataDisposable == null
        }
        LogUtils.d("Stopping read data timer：After Disposing interval disposable: $mReadDataDisposable")
    }


    fun stopMop() {
        if (!isStop) {
            stopReadDataTimer()
            disconnectMop()
        }
    }


    private fun getTimeNow(): String {
        val currentTime = System.currentTimeMillis()
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(currentTime)
    }

    data class Param(var index: ComponentIndexType, var id: Int, var transmissionControlType: TransmissionControlType, var deviceType: PipelineDeviceType)


}