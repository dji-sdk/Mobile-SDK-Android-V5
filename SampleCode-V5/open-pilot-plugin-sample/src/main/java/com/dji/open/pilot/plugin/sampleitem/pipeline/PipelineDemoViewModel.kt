package com.dji.open.pilot.plugin.sampleitem.pipeline

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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
import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService

/**
 * MOP 演示 ViewModel（迁移自 android-sdk-v5-sample 的 MopVM）。
 *
 * 收发区与上传下载区合并到同一个 ViewModel，但两条管道各自持有独立引用，
 * 避免样例中两个 Fragment 各自持有 MopVM 的行为差异：
 * - [interfacePipeline]：收发区，connect 后启动 [readData]，用于收发文本
 * - [downloadPipeline]：上传下载区，仅用于文件传输，不启动读循环
 */
class PipelineDemoViewModel : ViewModel() {

    private val TAG = "PipelineDemoVM"

    val receiveMessageLiveData = MutableLiveData<String>()
    val pipelineMapLivData = MutableLiveData<Map<Int, Pipeline>>()
    val toastMessage = MutableLiveData<String?>()

    private val data = ByteArray(19004)
    private val executorService: ExecutorService = DJIExecutor.getExecutorFor(DJIExecutor.Purpose.URGENT)

    private var interfacePipeline: Pipeline? = null
    private var interfaceParam: Param? = null

    @Volatile
    private var interfaceStop = false

    private var downloadPipeline: Pipeline? = null
    private var downloadParam: Param? = null

    private var listenerInited = false

    fun initListener() {
        if (listenerInited) {
            return
        }
        listenerInited = true
        PipelineManager.getInstance().addPipelineConnectionListener {
            LogUtils.i(TAG, it.values)
            pipelineMapLivData.postValue(it)
        }
    }

    fun connect(
        index: ComponentIndexType,
        id: Int,
        deviceType: PipelineDeviceType,
        transmissionControlType: TransmissionControlType,
        isUseForDown: Boolean = false
    ) {
        executorService.execute {
            val error = PipelineManager.getInstance()
                .connectPipeline(index, id, deviceType, transmissionControlType)
            if (error == null) {
                val param = Param(index, id, transmissionControlType, deviceType)
                val connected = PipelineManager.getInstance().pipelines[id]
                if (isUseForDown) {
                    downloadPipeline = connected
                    downloadParam = param
                } else {
                    interfacePipeline = connected
                    interfaceParam = param
                    interfaceStop = false
                    readData()
                }
                toastMessage.postValue("Connect Success")
            } else {
                toastMessage.postValue("Connect Fail:$error")
            }
        }
    }

    fun readData() {
        if (interfaceStop) {
            return
        }
        val pipeline = interfacePipeline ?: return
        var time = "Start time：${getTimeNow()}"
        val result = pipeline.readData(data) ?: DataResult()
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
            LogUtils.e(TAG, "can not read anything")
        } else {
            LogUtils.e(TAG, "mop error，result=$result")
            if (!interfaceStop && !result.error.errorCode().equals(DJIPipeLineError.TIMEOUT)) {
                stopInterface()
                return
            }
        }
        if (!interfaceStop) {
            readData()
        }
    }

    fun sendData(byteArray: ByteArray) {
        executorService.submit {
            val pipeline = interfacePipeline
            if (pipeline == null) {
                toastMessage.postValue("MOP is not connected, please trigger connection first and try again")
                return@submit
            }
            val result = pipeline.writeData(byteArray)
            if (result?.error != null) {
                toastMessage.postValue("sendData error:${result.error}")
            } else {
                toastMessage.postValue("sendData success, length is:${result?.length}")
            }
        }
    }

    private fun disconnectInterface() {
        val param = interfaceParam ?: return
        executorService.execute {
            LogUtils.d(TAG, "start to disconnect interface pipeline")
            val error = PipelineManager.getInstance()
                .disconnectPipeline(param.index, param.id, param.deviceType, param.transmissionControlType)
            AndroidSchedulers.mainThread().scheduleDirect {
                if (error == null) {
                    interfacePipeline = null
                    interfaceParam = null
                    toastMessage.postValue("disconnectPipeline success")
                    LogUtils.d(TAG, "disconnectInterface: disconnect successful")
                } else {
                    interfaceStop = false
                    toastMessage.postValue("disconnectPipeline fail:$error")
                    LogUtils.d(TAG, "disconnectInterface: Error disconnecting: $error")
                }
            }
        }
    }

    private fun disconnectDownload() {
        val param = downloadParam ?: return
        executorService.execute {
            LogUtils.d(TAG, "start to disconnect download pipeline")
            val error = PipelineManager.getInstance()
                .disconnectPipeline(param.index, param.id, param.deviceType, param.transmissionControlType)
            AndroidSchedulers.mainThread().scheduleDirect {
                if (error == null) {
                    downloadPipeline = null
                    downloadParam = null
                    toastMessage.postValue("disconnectPipeline success")
                    LogUtils.d(TAG, "disconnectDownload: disconnect successful")
                } else {
                    toastMessage.postValue("disconnectPipeline fail:$error")
                    LogUtils.d(TAG, "disconnectDownload: Error disconnecting: $error")
                }
            }
        }
    }

    /** 断开收发区管道 */
    fun stopInterface() {
        if (!interfaceStop) {
            interfaceStop = true
            disconnectInterface()
        }
    }

    /** 断开上传下载区管道 */
    fun stopDownload() {
        disconnectDownload()
    }

    /** 断开两条管道（供 Activity onDestroy 调用） */
    fun stopMop() {
        stopInterface()
        stopDownload()
    }

    fun consumeToast() {
        toastMessage.value = null
    }

    private fun getTimeNow(): String {
        val currentTime = System.currentTimeMillis()
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(currentTime)
    }

    data class Param(
        var index: ComponentIndexType,
        var id: Int,
        var transmissionControlType: TransmissionControlType,
        var deviceType: PipelineDeviceType
    )
}
