package com.dji.open.pilot.plugin.common

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

/**
 * 所有演示模块 Activity 的公共基类。
 *
 * 统一处理沉浸式全屏：隐藏系统状态栏与底部导航栏，避免 Activity 底部露出系统导航栏黑边。
 * 新增演示模块的 Activity 应继承本类，无需各自重复设置 systemUiVisibility。
 */
open class BaseDemoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyImmersiveFullScreen()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // IMMERSIVE_STICKY 下，失焦（如弹窗、下拉系统栏）后重新获得焦点时需重设，防止导航栏残留
        if (hasFocus) {
            applyImmersiveFullScreen()
        }
    }

    private fun applyImmersiveFullScreen() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }
}
