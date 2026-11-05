package dji.sample.patrol.models

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dji.open.pilot.client.manager.ServerState
import dji.sample.patrol.MainApplication
import dji.sample.patrol.pilot.SdkRegistrationState
import dji.v5.ux.util.RemoteLangUtils
import kotlinx.coroutines.launch

/**
    * 连接模块（全局级 VM）：连接三态门控。
    *
    * 与源工程的差异：SDK 的 init/registerApp 生命周期已由 [MainApplication] 持有的
    * `SdkBootstrapper` 统一负责，本 VM 不再重复调用 `SDKManager.init/registerApp`，
    * 而是消费 `SdkBootstrapper` 暴露的 `serverState` / `registrationState` StateFlow，
    * 并把连接状态转换成框架所需的 LiveData 形态。
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

    /** CONNECTED 时依次回调，用于挂接业务初始化（MainApplication 里注册） */
    private val onConnectedBlocks = mutableListOf<() -> Unit>()
    private val onDisconnectedBlocks = mutableListOf<() -> Unit>()
    private var observersStarted = false
    private var lastServerState = ServerState.UNINITIALIZED

    fun onConnected(block: () -> Unit) {
        onConnectedBlocks.add(block)
        if (lastServerState == ServerState.CONNECTED) block()
    }

    fun onDisconnected(block: () -> Unit) {
        onDisconnectedBlocks.add(block)
    }

    /**
        * 订阅 [MainApplication] 中 [dji.sample.patrol.pilot.SdkBootstrapper] 的连接状态。
        * 这里只消费状态，不再触发 SDK 初始化。
        */
    fun initSDK(context: Context) {
        if (observersStarted) return
        observersStarted = true
        val bootstrapper = (context.applicationContext as MainApplication).sdkBootstrapper

        viewModelScope.launch {
            bootstrapper.serverState.collect { state ->
                if (state == null) return@collect
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
        }

        viewModelScope.launch {
            bootstrapper.registrationState.collect { state ->
                isRegistered.postValue(state is SdkRegistrationState.Registered)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // SDK 生命周期由 MainApplication/SdkBootstrapper 拥有，这里不再 destroy。
    }
}
