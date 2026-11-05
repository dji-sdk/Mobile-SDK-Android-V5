package com.dji.open.pilot.plugin.common

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore

/**
 * 全局 ViewModelStore，用于在 Application/Activity/Fragment 之间共享同一个 ViewModel 实例。
 */
val globalViewModelStore = ViewModelStore()

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

@MainThread
inline fun <reified VM : ViewModel> Application.globalViewModels(): Lazy<VM> {
    val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(this)
    return lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(globalViewModelStore, factory)[VM::class.java]
    }
}
