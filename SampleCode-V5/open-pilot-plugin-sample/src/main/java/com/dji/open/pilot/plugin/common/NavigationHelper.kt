package com.dji.open.pilot.plugin.common

import android.util.Log
import com.dji.open.pilot.plugin.main.PluginApplication
import dji.open.pilot.base.ui.NavigateAction
import dji.open.pilot.client.manager.ui.NavigateManager
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError

/**
 * 统一的页面导航工具。
 *
 * 整合 MainViewModel、RemoteViewPageDemoViewModel、DrawingContext 三处
 * 散落的导航逻辑，提供统一入口。
 *
 * TODO: 后续将各处的 navigateToPilot / navigateToPluginApp 改为调用此类。
 */
object NavigationHelper {

    private const val TAG = "NavigationHelper"

    /**
     * 跳转到 Pilot 默认页面。
     */
    fun navigateToPilot() {
        NavigateManager.getInstance().navigateToPage(
            NavigateAction(NavigateAction.Value.PILOT_DEFAULT, ""),
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() { Log.i(TAG, "navigateToPilot success") }
                override fun onFailure(error: IDJIError) { Log.e(TAG, "navigateToPilot failed: $error") }
            }
        )
    }

    /**
     * 跳转到 Pilot 地图页面。
     */
    fun navigateToPilotMap() {
        NavigateManager.getInstance().navigateToPage(
            NavigateAction(NavigateAction.Value.PILOT_MAP, ""),
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() { Log.i(TAG, "navigateToPilotMap success") }
                override fun onFailure(error: IDJIError) { Log.e(TAG, "navigateToPilotMap failed: $error") }
            }
        )
    }

    /**
     * 跳转回插件 APP 当前栈顶 Activity。
     */
    fun navigateToPluginApp() {
        NavigateManager.getInstance().navigateToPage(
            NavigateAction(NavigateAction.Value.SELF_DEFINE, PluginApplication.currentActivityClassName),
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() { Log.i(TAG, "navigateToPluginApp success") }
                override fun onFailure(error: IDJIError) { Log.e(TAG, "navigateToPluginApp failed: $error") }
            }
        )
    }
}
