package com.dji.open.pilot.plugin.main

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.dji.open.pilot.plugin.main.MainViewModel
import com.dji.open.pilot.plugin.common.globalViewModels

class PluginApplication : Application() {

    companion object {
        /** Application 实例，供需要 Context 的工具类使用（如 Toast） */
        lateinit var instance: PluginApplication
            private set

        /** 当前栈顶 Activity 的全限定类名，供跳回插件时动态使用。只存类名字符串，不持有 Activity 引用。 */
        var currentActivityClassName: String = "com.dji.open.pilot.plugin.main.MainActivity"
            private set
    }

    private val mainViewModel: MainViewModel by globalViewModels()

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        com.cySdkyc.clx.Helper.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        mainViewModel.initSDK(this)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                // 只存类名字符串，绝不持有 Activity 引用，避免内存泄漏
                currentActivityClassName = activity.javaClass.name
            }
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
