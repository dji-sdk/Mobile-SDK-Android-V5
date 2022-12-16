package dji.v5.ux.core.ui.hsi

import androidx.annotation.MainThread
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable

import java.util.concurrent.TimeUnit

object FlashTimer {

    private val compositeDisposable = CompositeDisposable()

    private val listenerList: MutableList<Listener> = mutableListOf()

    private var started = false

    @MainThread
    fun addListener(listener: Listener?) {
        listener?.let {
            if (!listenerList.contains(it)) {
                listenerList.add(it)
            }
            if (listenerList.size > 0 && !started) {
                started = true
                startFlash()
            }
        }
    }

    @MainThread
    fun removeListener(listener: Listener?) {
        listener?.let {
            listenerList.remove(it)
            if (listenerList.isEmpty() && started) {
                started = false
                compositeDisposable.clear()
            }
        }

    }

    private fun startFlash() {
        val valueObservable = Observable.fromArray(false, true)
        val intervalObservable = Observable.interval(250, TimeUnit.MILLISECONDS)

        compositeDisposable.add(Observable.zip(valueObservable, intervalObservable) { value, _ ->
            value
        }.repeat().observeOn(AndroidSchedulers.mainThread()).subscribe { value ->
            listenerList.forEach { it.onValue(value) }
        })
    }

    fun interface Listener {
        fun onValue(show: Boolean)
    }
}