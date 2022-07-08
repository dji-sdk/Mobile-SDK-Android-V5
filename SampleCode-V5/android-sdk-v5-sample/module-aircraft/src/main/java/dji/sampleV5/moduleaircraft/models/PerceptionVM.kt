package dji.sampleV5.moduleaircraft.models

import androidx.lifecycle.MutableLiveData
import dji.sampleV5.modulecommon.models.DJIViewModel
import dji.v5.common.callback.CommonCallbacks
import dji.v5.manager.aircraft.perception.*
import dji.v5.utils.common.LogUtils

/**
 * Description :感知模块的ViewModel
 *
 * @author: Byte.Cai
 *  date : 2022/6/7
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class PerceptionVM : DJIViewModel() {
    private val TAG = LogUtils.getTag("PerceptionVM")
    private val perceptionManager = PerceptionManager.getInstance()
    val perceptionInfo = MutableLiveData<PerceptionInfo>()


    fun addPerceptionInfoListener() {
        perceptionManager.addPerceptionInformationListener {
            perceptionInfo.postValue(it)
        }
    }


    fun setOverallObstacleAvoidanceEnabled(isEnabled: Boolean, callback: CommonCallbacks.CompletionCallback) {
        perceptionManager.setOverallObstacleAvoidanceEnabled(isEnabled, callback)
    }


    fun setObstacleAvoidanceEnabled(isEnabled: Boolean, direction: PerceptionDirection, callback: CommonCallbacks.CompletionCallback) {
        perceptionManager.setObstacleAvoidanceEnabled(isEnabled, direction, callback)
    }


    fun setObstacleAvoidanceType(type: ObstacleAvoidanceType, callback: CommonCallbacks.CompletionCallback) {
        perceptionManager.setObstacleAvoidanceType(type, callback)
    }

    fun setObstacleAvoidanceWarningDistance(distance: Double, direction: PerceptionDirection, callback: CommonCallbacks.CompletionCallback) {
        perceptionManager.setObstacleAvoidanceWarningDistance(distance, direction, callback)
    }


    fun setObstacleAvoidanceBrakingDistance(distance: Double, direction: PerceptionDirection, callback: CommonCallbacks.CompletionCallback) {
        perceptionManager.setObstacleAvoidanceBrakingDistance(distance, direction, callback)
    }


    fun setVisionPositioningEnabled(isEnabled: Boolean, callback: CommonCallbacks.CompletionCallback) {
        perceptionManager.setVisionPositioningEnabled(isEnabled, callback)
    }


    fun setPrecisionLandingEnabled(isEnabled: Boolean, callback: CommonCallbacks.CompletionCallback) {
        perceptionManager.setPrecisionLandingEnabled(isEnabled, callback)
    }


}