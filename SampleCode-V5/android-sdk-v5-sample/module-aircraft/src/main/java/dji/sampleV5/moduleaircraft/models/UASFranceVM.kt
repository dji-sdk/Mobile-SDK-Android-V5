package dji.sampleV5.moduleaircraft.models

import androidx.lifecycle.MutableLiveData
import dji.sampleV5.modulecommon.models.DJIViewModel
import dji.v5.common.callback.CommonCallbacks
import dji.v5.manager.aircraft.uas.AreaStrategy
import dji.v5.manager.aircraft.uas.ElectronicIDStatus
import dji.v5.manager.aircraft.uas.UASRemoteIDManager
import dji.sampleV5.modulecommon.util.ToastUtils

/**
 * Description :法国无人机远程识别VM
 *
 * @author: Byte.Cai
 *  date : 2022/6/27
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class UASFranceVM : DJIViewModel() {
    private val uasRemoteIDManager = UASRemoteIDManager.getInstance()
    val electronicIDStatus = MutableLiveData<ElectronicIDStatus>()


    init {
        val error = uasRemoteIDManager.setUASRemoteIDAreaStrategy(AreaStrategy.FRANCE_STRATEGY)
        error?.apply {
            ToastUtils.showToast(toString())
        }
    }

    fun setElectronicIDEnabled(boolean: Boolean, callbacks: CommonCallbacks.CompletionCallback) {
        uasRemoteIDManager.setElectronicIDEnabled(boolean, callbacks)
    }

    fun addElectronicIDStatusListener() {
        uasRemoteIDManager.addElectronicIDStatusListener {
            electronicIDStatus.postValue(it)
        }
    }

    fun clearAllElectronicIDStatusListener() {
        uasRemoteIDManager.clearAllElectronicIDStatusListener()
    }
}