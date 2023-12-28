package dji.sampleV5.aircraft.models

import androidx.lifecycle.MutableLiveData
import dji.sampleV5.aircraft.R
import dji.sdk.keyvalue.key.RemoteControllerKey
import dji.sdk.keyvalue.value.remotecontroller.PairingState
import dji.v5.et.action
import dji.v5.et.create
import dji.v5.et.get
import dji.v5.manager.SDKManager
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

    val sdkNews = MutableLiveData<SDKNews>()

    init {
        updateNews()
    }

    private fun updateNews() {
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