package dji.sample.flighttask.util

import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore

/**
 * 全局 ViewModelStore：与 MSDK Sample 的 GlobalVM 机制一致。
 * 悬浮窗/上传/采集这类跨页面、与 Activity 生命周期无关的 VM 挂在这里，进程存活期间常驻。
 */
val globalViewModelStore = ViewModelStore()

/** 无宿主场景（Application 内）直接获取全局 VM */
@MainThread
inline fun <reified VM : ViewModel> globalViewModel(): VM {
    return ViewModelProvider(globalViewModelStore, ViewModelProvider.NewInstanceFactory())[VM::class.java]
}

@MainThread
inline fun <reified VM : ViewModel> ComponentActivity.globalViewModels(
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> {
    return lazy(LazyThreadSafetyMode.NONE) {
        val factory = factoryProducer?.invoke() ?: defaultViewModelProviderFactory
        ViewModelProvider(globalViewModelStore, factory)[VM::class.java]
    }
}

@MainThread
inline fun <reified VM : ViewModel> Fragment.globalViewModels(
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> {
    return lazy(LazyThreadSafetyMode.NONE) {
        val factory = factoryProducer?.invoke() ?: defaultViewModelProviderFactory
        ViewModelProvider(globalViewModelStore, factory)[VM::class.java]
    }
}
