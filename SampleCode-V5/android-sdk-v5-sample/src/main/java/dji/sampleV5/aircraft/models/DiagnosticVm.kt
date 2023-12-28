package dji.sampleV5.aircraft.models

import androidx.lifecycle.MutableLiveData
import dji.v5.manager.diagnostic.*

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/6/30
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
class DiagnosticVm : DJIViewModel() {

    val deviceHealthInfos = MutableLiveData(ArrayList<DJIDeviceHealthInfo>())

    val lastDeviceStatus = MutableLiveData(DJIDeviceStatus.NORMAL)
    val currentDeviceStatus = MutableLiveData(DJIDeviceStatus.NORMAL)

    private val deviceHealthInfoChangeListener = DJIDeviceHealthInfoChangeListener {
        updateDeviceHealthInfo(it as ArrayList<DJIDeviceHealthInfo>)
    }

    private val deviceStatusChangeListener = DJIDeviceStatusChangeListener { from, to ->
        updateDeviceStatus(from, to)
    }

    private fun updateDeviceHealthInfo(infos: ArrayList<DJIDeviceHealthInfo>) {
        deviceHealthInfos.value?.clear()
        deviceHealthInfos.value?.addAll(infos)
        deviceHealthInfos.postValue(deviceHealthInfos.value)
    }

    private fun updateDeviceStatus(form: DJIDeviceStatus, to: DJIDeviceStatus) {
        lastDeviceStatus.value = form
        currentDeviceStatus.value = to
    }

    override fun onCleared() {
        stopListenDeviceHealthInfoChange()
        stopListenDeviceStatusChange()
    }

    fun startListenDeviceHealthInfoChange() {
        DeviceHealthManager.getInstance()
            .addDJIDeviceHealthInfoChangeListener(deviceHealthInfoChangeListener)
    }

    fun stopListenDeviceHealthInfoChange() {
        DeviceHealthManager.getInstance()
            .removeDJIDeviceHealthInfoChangeListener(deviceHealthInfoChangeListener)
    }

    fun getCurrentDeviceHealthInfos() {
        updateDeviceHealthInfo(DeviceHealthManager.getInstance().currentDJIDeviceHealthInfos as ArrayList<DJIDeviceHealthInfo>)
    }

    fun startListenDeviceStatusChange() {
        DeviceStatusManager.getInstance()
            .addDJIDeviceStatusChangeListener(deviceStatusChangeListener)
    }

    fun stopListenDeviceStatusChange() {
        DeviceStatusManager.getInstance()
            .removeDJIDeviceStatusChangeListener(deviceStatusChangeListener)
    }

    fun getCurrentDeviceStatus() {
        currentDeviceStatus.value = DeviceStatusManager.getInstance().currentDJIDeviceStatus
    }
}