package dji.sample.flighttask.models

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dji.open.pilot.base.manager.PluginAPPConfig
import dji.open.pilot.base.ui.NavigateAction
import dji.open.pilot.client.manager.ServerState
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent
import dji.v5.common.register.InitSettings
import dji.v5.common.register.SDKLinKMode
import dji.v5.manager.SDKManager
import dji.v5.manager.interfaces.SDKManagerCallback
import dji.v5.ux.util.RemoteLangUtils

/**
 * 连接模块（全局级 VM）：MSDK 初始化（REMOTE_PILOT）+ 连接三态门控。
 *
 * 门控约定：所有业务初始化（悬浮窗渲染、上传引擎、采集监听）统一收敛到 CONNECTED 分支触发；
 * DISCONNECT 时暂停。其余 VM 不各自监听连接状态，只订阅本 VM 的 serverState。
 */
class ConnectionVM : ViewModel() {

    companion object {
        private const val TAG = "ConnectionVM"
    }

    val serverState = MutableLiveData(ServerState.UNINITIALIZED)

    /** true = MSDK 注册成功 */
    val isRegistered = MutableLiveData(false)

    /** CONNECTED 时依次回调，用于挂接业务初始化（FlightTaskApplication 里注册） */
    private val onConnectedBlocks = mutableListOf<() -> Unit>()
    private val onDisconnectedBlocks = mutableListOf<() -> Unit>()
    private var sdkInitialized = false
    private var lastServerState = ServerState.UNINITIALIZED

    fun onConnected(block: () -> Unit) {
        onConnectedBlocks.add(block)
        if (lastServerState == ServerState.CONNECTED) block()
    }

    fun onDisconnected(block: () -> Unit) {
        onDisconnectedBlocks.add(block)
    }

    fun initSDK(context: Context) {
        if (sdkInitialized) return
        sdkInitialized = true
        val appConfig = PluginAPPConfig().apply {
            title = "Flight Task Management"
            homePageOnClickAction = NavigateAction(
                NavigateAction.Value.SELF_DEFINE,
                "dji.sample.flighttask.PluginActivity"
            )
        }

        val settings = InitSettings(SDKLinKMode.REMOTE_PILOT).also {
            it.pluginAppConfig = appConfig
        }

        SDKManager.getInstance().init(context, settings, object : SDKManagerCallback {
            override fun onInitProcess(event: DJISDKInitEvent, totalProcess: Int) {
                Log.i(TAG, "onInitProcess: $event $totalProcess")
                if (event == DJISDKInitEvent.INITIALIZE_COMPLETE) {
                    SDKManager.getInstance().registerApp()
                }
            }

            override fun onRemoteServerStateUpdated(state: ServerState) {
                Log.i(TAG, "ServerState changed: $state")
                val previousState = lastServerState
                lastServerState = state
                serverState.postValue(state)
                if (state == ServerState.CONNECTED && previousState != ServerState.CONNECTED) {
                    RemoteLangUtils.init(context.applicationContext)
                    onConnectedBlocks.forEach { it() }
                } else if (state == ServerState.DISCONNECT && previousState != ServerState.DISCONNECT) {
                    onDisconnectedBlocks.forEach { it() }
                }
            }

            override fun onRemoteServerCompatibleError(error: IDJIError) {
                Log.e(TAG, "Version compatible error: $error")
            }

            override fun onRegisterSuccess() {
                isRegistered.postValue(true)
            }

            override fun onRegisterFailure(error: IDJIError) {
                Log.e(TAG, "onRegisterFailure: $error")
                isRegistered.postValue(false)
            }

            override fun onProductConnect(productId: Int) {}
            override fun onProductDisconnect(productId: Int) {}
            override fun onProductChanged(productId: Int) {}

            @Deprecated("Deprecated in SDKManagerCallback")
            override fun onDatabaseDownloadProgress(current: Long, total: Long) {}
        })
    }

    override fun onCleared() {
        super.onCleared()
        SDKManager.getInstance().destroy()
    }
}
