package dji.sampleV5.aircraft.models

import androidx.lifecycle.MutableLiveData
import dji.sampleV5.aircraft.R
import dji.v5.manager.aircraft.uas.AreaStrategy
import dji.v5.manager.aircraft.uas.UASRemoteIDManager
import dji.v5.manager.aircraft.uas.UASRemoteIDStatus
import dji.v5.manager.aircraft.uas.UASRemoteIDStatusListener
import dji.sampleV5.aircraft.util.ToastUtils
import dji.sdk.keyvalue.key.BatteryKey
import dji.sdk.keyvalue.key.ProductKey
import dji.sdk.keyvalue.utils.ProductUtil
import dji.v5.et.create
import dji.v5.et.get
import dji.v5.et.listen
import dji.v5.manager.KeyManager
import dji.v5.utils.common.LogPath
import dji.v5.utils.common.LogUtils
import dji.v5.utils.common.StringUtils

/**
 * Description :美国无人机远程识别VM
 *
 * @author: Byte.Cai
 *  date : 2022/8/3
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class UASUAVM : DJIViewModel() {
    val uasRemoteIDStatus = MutableLiveData<UASRemoteIDStatus>()

    private val uasRemoteIDStatusListener = UASRemoteIDStatusListener {
        uasRemoteIDStatus.postValue(it)
    }

    init {
        val error = UASRemoteIDManager.getInstance().setUASRemoteIDAreaStrategy(AreaStrategy.US_STRATEGY)
        error?.apply {
            ToastUtils.showToast(toString())
        }
    }

    override fun onCleared() {
        KeyManager.getInstance().cancelListen(this)
    }

    fun addRemoteIdStatusListener() {
        UASRemoteIDManager.getInstance().addUASRemoteIDStatusListener(uasRemoteIDStatusListener)
    }

    fun clearRemoteIdStatusListener() {
        UASRemoteIDManager.getInstance().clearUASRemoteIDStatusListener()
    }
}