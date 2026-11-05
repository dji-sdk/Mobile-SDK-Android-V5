package dji.sample.patrol.pilot

import android.app.Application
import android.util.Log
import dji.sample.patrol.PluginActivity
import dji.open.pilot.base.manager.PluginAPPConfig
import dji.open.pilot.base.ui.NavigateAction
import dji.open.pilot.client.manager.ServerState
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent
import dji.v5.common.register.InitSettings
import dji.v5.common.register.SDKLinKMode
import dji.v5.manager.KeyManager
import dji.v5.manager.SDKManager
import dji.v5.manager.interfaces.SDKManagerCallback
import dji.v5.network.DJINetworkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
    * 负责 DJI MSDK 初始化、注册以及与 Pilot/飞行器的连接状态维护。
    *
    * 这是本插件模板中唯一需要的"初始化 + 建立连接"逻辑，所有状态通过
    * [StateFlow] 暴露给 UI 层直接观察，不再需要额外的依赖注入框架或分层接口。
    */
class SdkBootstrapper(private val application: Application) {

    private val keyListenerInstalled = AtomicBoolean(false)

    private val _registrationState = MutableStateFlow<SdkRegistrationState>(SdkRegistrationState.Unregistered)
    val registrationState: StateFlow<SdkRegistrationState> = _registrationState.asStateFlow()

    private val _productConnected = MutableStateFlow(false)
    val productConnected: StateFlow<Boolean> = _productConnected.asStateFlow()

    private val _serverState = MutableStateFlow<ServerState?>(null)
    val serverState: StateFlow<ServerState?> = _serverState.asStateFlow()

    /**
        * 启动 DJI MSDK 初始化。
        *
        * 注意：`PluginAPPConfig` / `NavigateAction` 等 `dji.open.pilot.*` 类只在
        * Pilot 宿主进程里才真实存在（编译期是 `compileOnly` 的 stub jar）。当本 App
        * 被独立启动（不是通过 Pilot 加载）时，引用它们会抛出 [NoClassDefFoundError]。
        * 这里仅对插件配置构造做兜底：如果检测到不在 Pilot 宿主环境中运行，则退化为
        * 不带插件配置的普通 MSDK 初始化。PluginActivity 仍直接依赖 Pilot compileOnly 类，
        * 因此完整运行时验证必须在 Pilot 宿主环境中进行。
        */
    fun start() {
        val settings = buildInitSettings()

        try {
            SDKManager.getInstance().init(application, settings, object : SDKManagerCallback {
                override fun onRegisterSuccess() {
                    Log.i(TAG, "SDK registered successfully")
                    _registrationState.value = SdkRegistrationState.Registered
                }

                override fun onRegisterFailure(error: IDJIError) {
                    Log.e(TAG, "SDK registration failed: $error")
                    _registrationState.value = SdkRegistrationState.Failed(error.toString())
                }

                override fun onProductDisconnect(productId: Int) {
                    Log.d(TAG, "Product disconnected: $productId")
                    _productConnected.value = false
                }

                override fun onProductConnect(productId: Int) {
                    Log.d(TAG, "Product connected: $productId")
                    _productConnected.value = true
                }

                override fun onProductChanged(productId: Int) {
                    Log.d(TAG, "Product changed: $productId")
                }

                override fun onInitProcess(event: DJISDKInitEvent, totalProcess: Int) {
                    Log.d(TAG, "Init process: ${event.name}")
                    if (event == DJISDKInitEvent.INITIALIZE_COMPLETE) {
                        SDKManager.getInstance().registerApp()
                    }
                }

                @Deprecated("Deprecated in API")
                override fun onDatabaseDownloadProgress(current: Long, total: Long) = Unit

                override fun onRemoteServerStateUpdated(serverState: ServerState) {
                    Log.d(TAG, "Pilot server state: ${serverState.name}")
                    _serverState.value = serverState
                    if (serverState == ServerState.CONNECTED) {
                        installProductConnectionListener()
                    }
                }

                override fun onRemoteServerCompatibleError(error: IDJIError) {
                    Log.e(TAG, "SDK not compatible with Pilot server: $error")
                }
            })

            DJINetworkManager.getInstance().addNetworkStatusListener { isAvailable ->
                if (isAvailable && !SDKManager.getInstance().isRegistered) {
                    SDKManager.getInstance().registerApp()
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "SDKManager init failed", e)
            _registrationState.value = SdkRegistrationState.Failed(e.message ?: "SDK init failed")
        }
    }

    /**
        * 构造 [InitSettings]；仅当运行在 Pilot 宿主进程中（`dji.open.pilot.*` 类可用）
        * 时才携带插件首页跳转配置。缺少相关类时会捕获 [NoClassDefFoundError]，退化为
        * 不带插件配置的初始化设置；这不构成整个 App 的独立运行兼容承诺。
        */
    private fun buildInitSettings(): InitSettings = try {
        val appConfig = PluginAPPConfig().apply {
            // 用户在 Pilot 插件列表点击插件卡片时跳回的页面：SELF_DEFINE + 目标 Activity 全类名。
            // PluginAPPConfig 的 setTitle / setDescription / setIconResFileName 已废弃；
            // 插件名称、描述、图标由所选平台的 Manifest/资源集成提供。
            homePageOnClickAction = NavigateAction(
                NavigateAction.Value.SELF_DEFINE,
                PluginActivity::class.java.name
            )
        }
        InitSettings(SDKLinKMode.REMOTE_PILOT).also { it.pluginAppConfig = appConfig }
    } catch (e: Throwable) {
        Log.w(TAG, "Not running inside DJI Pilot host, skip plugin app config", e)
        InitSettings(SDKLinKMode.REMOTE_PILOT)
    }

    private fun installProductConnectionListener() {
        if (!keyListenerInstalled.compareAndSet(false, true)) return
        runCatching {
            val key = KeyTools.createKey(FlightControllerKey.KeyConnection)
            KeyManager.getInstance().listen(
                key,
                this,
                object : CommonCallbacks.KeyListener<Boolean> {
                    override fun onValueChange(oldValue: Boolean?, newValue: Boolean?) {
                        val actual = newValue ?: false
                        Log.i(TAG, "KeyConnection changed: $oldValue -> $newValue")
                        _productConnected.value = actual
                    }
                }
            )
            val current = KeyManager.getInstance().getValue(key) ?: false
            Log.i(TAG, "KeyConnection initial value: $current")
            _productConnected.value = current
        }.onFailure {
            Log.e(TAG, "installProductConnectionListener failed", it)
            keyListenerInstalled.set(false)
        }
    }

    companion object {
        private const val TAG = "SdkBootstrapper"
    }
}
