package dji.sampleV5.aircraft.models

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore

// ViewModelStore for Global Use
val globalViewModelStore = ViewModelStore()

/**
 * Create a global ViewModel in the activity
 */
@MainThread
inline fun <reified VM : ViewModel> ComponentActivity.globalViewModels(
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> {
    val factoryPromise = factoryProducer ?: {
        defaultViewModelProviderFactory
    }
    return ViewModelLazy(VM::class, { globalViewModelStore }, factoryPromise)
}

/**
 * Create a global ViewModel in the fragment
 */
@MainThread
inline fun <reified VM : ViewModel> Fragment.globalViewModels(
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> {
    val factoryPromise = factoryProducer ?: {
        defaultViewModelProviderFactory
    }
    return ViewModelLazy(VM::class, { globalViewModelStore }, factoryPromise)
}

/**
 * Create a global ViewModel in the fragment
 */
@MainThread
inline fun <reified VM : ViewModel> Application.globalViewModels(): Lazy<VM> {
    val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(this)
    return ViewModelLazy(VM::class, { globalViewModelStore }, { factory })
}

