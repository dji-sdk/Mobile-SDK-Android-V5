package dji.sampleV5.modulecommon.models

import android.content.Context
import androidx.annotation.NonNull
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dji.sampleV5.modulecommon.R
import dji.sampleV5.modulecommon.data.DJIToastResult
import dji.sdk.keyvalue.key.RemoteControllerKey
import dji.sdk.keyvalue.value.remotecontroller.PairingState
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent
import dji.v5.et.action
import dji.v5.et.create
import dji.v5.et.get
import dji.v5.manager.SDKManager
import dji.v5.manager.interfaces.SDKManagerCallback
import dji.v5.utils.common.ContextUtil
import dji.v5.utils.common.StringUtils

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/2/14
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class BaseMainActivityVm : DJIViewModel() {

    val registerState = MutableLiveData<String>()
    val sdkNews = MutableLiveData<SDKNews>()

    init {
        updateNews()
    }

    fun registerApp(context: Context, @NonNull callback: SDKManagerCallback) {
        SDKManager.getInstance().init(context, object : SDKManagerCallback {
            override fun onRegisterSuccess() {
                callback.onRegisterSuccess()
                registerState.postValue(StringUtils.getResStr(ContextUtil.getContext(), R.string.registered))
            }

            override fun onRegisterFailure(error: IDJIError?) {
                callback.onRegisterFailure(error)
                registerState.postValue(StringUtils.getResStr(ContextUtil.getContext(), R.string.unregistered))
            }

            override fun onProductDisconnect(product: Int) {
                callback.onProductDisconnect(product)
            }

            override fun onProductConnect(product: Int) {
                callback.onProductConnect(product)
            }

            override fun onProductChanged(product: Int) {
                callback.onProductChanged(product)
            }

            override fun onInitProcess(event: DJISDKInitEvent?, totalProcess: Int) {
                callback.onInitProcess(event, totalProcess)
                if (event == DJISDKInitEvent.INITIALIZE_COMPLETE) {
                    SDKManager.getInstance().registerApp()
                }
            }

            override fun onDatabaseDownloadProgress(current: Long, total: Long) {
                callback.onDatabaseDownloadProgress(current, total)
            }
        })
    }

    fun releaseSDKCallback() {
        SDKManager.getInstance().destroy()
    }

    fun updateNews() {
        sdkNews.postValue(SDKNews(R.string.news_title, R.string.news_description, StringUtils.getResStr(R.string.news_date)))
    }

    fun doPairing(callback: ((String) -> Unit)? = null) {
        if (!SDKManager.getInstance().isRegistered) {
            return
        }
        RemoteControllerKey.KeyPairingStatus.create().get({
            if (it == PairingState.PAIRING) {
                RemoteControllerKey.KeyStopPairing.create().action()
                callback?.invoke(StringUtils.getResStr(R.string.stop_pairing))
            } else {
                RemoteControllerKey.KeyRequestPairing.create().action()
                callback?.invoke(StringUtils.getResStr(R.string.start_pairing))
            }
        }) {
            callback?.invoke(it.toString())
        }
    }

    data class SDKNews(
        var title: Int,
        var description: Int,
        var date: String,
    )
}