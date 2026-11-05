package com.dji.sample.gasdetection

import android.app.Application
import dji.open.pilot.base.manager.PluginAPPConfig
import dji.open.pilot.base.ui.NavigateAction
import dji.open.pilot.client.manager.ServerState
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent
import dji.v5.common.register.InitSettings
import dji.v5.common.register.SDKLinKMode
import dji.v5.manager.SDKManager
import dji.v5.manager.interfaces.SDKManagerCallback

/**
 * MSDK 注册入口，单独成类。
 *
 * 对外发布版以加固 AAR 形式依赖 MSDK，加固类要等 `attachBaseContext` 里的 secneo Helper
 * 解壳后才可用。若 Application 类自身出现加固包中的类型（匿名实现 SDK 回调会产生
 * `check-cast` 到该接口），ART 会在 Application 实例化阶段就解析它，早于解壳，直接抛
 * `NoClassDefFoundError`。因此这里把所有 MSDK 类型收敛在本类内，宿主只需实现不涉及
 * SDK 类型的 [Host] 回调。
 */
internal object SdkBootstrap {

    /** 注册过程回调；参数全部使用基本类型，避免宿主类引用 MSDK 类型。 */
    internal interface Host {
        fun onInitProgress(eventName: String, totalProcess: Int)
        fun onRegisterSucceeded()
        fun onRegisterFailed(reason: String)
        fun onProductConnected(productId: Int)
        fun onProductDisconnected(productId: Int)
        fun onProductChanged(productId: Int)
        fun onPilotStateChanged(connected: Boolean, stateName: String)
        fun onPilotIncompatible(reason: String)
    }

    fun register(application: Application, homeActivityClassName: String, host: Host) {
        // PluginAPPConfig 的 setTitle / setDescription / setIconResFileName 已废弃：
        // 插件名称、描述与图标由契约 dji_open_pilot_plugin.xml 与固定字符串资源提供，
        // 这里只声明「Pilot 插件卡片点击后跳回的页面」。
        val config = PluginAPPConfig().apply {
            homePageOnClickAction = NavigateAction(
                NavigateAction.Value.SELF_DEFINE,
                homeActivityClassName
            )
        }
        val settings = InitSettings(SDKLinKMode.REMOTE_PILOT).apply { pluginAppConfig = config }
        SDKManager.getInstance().init(application, settings, object : SDKManagerCallback {
            override fun onInitProcess(event: DJISDKInitEvent, totalProcess: Int) {
                host.onInitProgress(event.name, totalProcess)
                if (event == DJISDKInitEvent.INITIALIZE_COMPLETE) SDKManager.getInstance().registerApp()
            }

            override fun onRegisterSuccess() {
                host.onRegisterSucceeded()
            }

            override fun onRegisterFailure(error: IDJIError) {
                host.onRegisterFailed(error.toString())
            }

            override fun onProductConnect(productId: Int) {
                host.onProductConnected(productId)
            }

            override fun onProductDisconnect(productId: Int) {
                host.onProductDisconnected(productId)
            }

            override fun onProductChanged(productId: Int) {
                host.onProductChanged(productId)
            }

            override fun onRemoteServerStateUpdated(serverState: ServerState) {
                host.onPilotStateChanged(serverState == ServerState.CONNECTED, serverState.name)
            }

            override fun onRemoteServerCompatibleError(error: IDJIError) {
                host.onPilotIncompatible(error.toString())
            }

            @Deprecated("SDK callback")
            override fun onDatabaseDownloadProgress(current: Long, total: Long) {}
        })
    }
}
