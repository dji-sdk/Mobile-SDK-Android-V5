package com.dji.open.pilot.plugin.sampleitem.filetransfer

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dji.open.pilot.base.manager.file.FileTransferDestination
import dji.open.pilot.client.manager.IServerStateListener
import dji.open.pilot.client.manager.RemoteLinkManager
import dji.open.pilot.client.manager.ServerState
import dji.open.pilot.client.manager.file.FileTransferManager
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError

class FileTransferDemoViewModel : ViewModel() {

    private val mainHandler = Handler(Looper.getMainLooper())
    // 操作开始、断连及 callback 终态均在主线程按 token 收敛。
    private var operationSequence = 0L
    private var activeOperationToken: Long? = null
    private var isServerConnected = false
    private var isCleared = false

    val selectedDestination = MutableLiveData(FileTransferDestination.PLUGIN_PRIVATE)
    val toastMessage = MutableLiveData<String?>()
    val isOperating = MutableLiveData(false)

    private val serverStateListener = object : IServerStateListener {
        override fun onStateChanged(status: ServerState) {
            onMain {
                if (isCleared) return@onMain
                isServerConnected = status == ServerState.CONNECTED
                if (!isServerConnected) {
                    activeOperationToken?.let { token ->
                        finishOperation(token) {
                            toastMessage.value = "Pilot service disconnected"
                        }
                    }
                }
            }
        }

        override fun onVersionCompatibleError(error: IDJIError) {
            onStateChanged(ServerState.INCOMPATIBLE)
        }
    }

    init {
        onMain { RemoteLinkManager.getInstance().addServerStateListener(serverStateListener) }
    }

    fun selectDestination(destination: FileTransferDestination) = onMain {
        selectedDestination.value = destination
    }

    fun upload(filePath: String, fileName: String) = onMain {
        val token = beginOperation() ?: return@onMain
        val destination = selectedDestination.value ?: FileTransferDestination.PLUGIN_PRIVATE
        toastMessage.value = "Uploading $fileName"
        FileTransferManager.getInstance().pushFileToRemoteDest(
            filePath,
            destination,
            object : CommonCallbacks.CompletionCallbackWithProgress<Double> {
                override fun onProgressUpdate(progress: Double) = Unit

                override fun onSuccess() {
                    finishOperation(token) {
                        toastMessage.value = "Upload succeeded: $fileName -> ${destination.name}"
                    }
                }

                override fun onFailure(error: IDJIError) {
                    finishOperation(token) {
                        toastMessage.value = "Upload failed: ${error.errorCode()} ${error.description()}"
                    }
                }
            }
        )
    }

    private fun beginOperation(): Long? {
        if (isCleared || activeOperationToken != null || isOperating.value == true) return null
        if (!isServerConnected) {
            toastMessage.value = "Pilot service disconnected"
            return null
        }
        val token = ++operationSequence
        activeOperationToken = token
        isOperating.value = true
        return token
    }

    private fun finishOperation(token: Long, onTerminal: () -> Unit) = onMain {
        if (activeOperationToken != token) return@onMain
        activeOperationToken = null
        onTerminal()
        // 最后解除 busy，避免同步 LiveData observer 开始下一操作后仍写入旧终态。
        isOperating.value = false
    }

    private fun onMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post { action() }
        }
    }

    override fun onCleared() {
        onMain {
            isCleared = true
            activeOperationToken = null
            isOperating.value = false
            RemoteLinkManager.getInstance().removeServerStateListener(serverStateListener)
        }
        super.onCleared()
    }

    fun consumeToast() = onMain {
        toastMessage.value = null
    }
}
