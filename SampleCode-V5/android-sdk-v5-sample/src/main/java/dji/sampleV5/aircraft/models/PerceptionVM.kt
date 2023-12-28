package dji.sampleV5.aircraft.models

import androidx.lifecycle.MutableLiveData
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.key.RadarKey
import dji.v5.common.callback.CommonCallbacks
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.perception.*
import dji.v5.manager.aircraft.perception.data.ObstacleAvoidanceType
import dji.v5.manager.aircraft.perception.data.ObstacleData
import dji.v5.manager.aircraft.perception.data.PerceptionDirection
import dji.v5.manager.aircraft.perception.data.PerceptionInfo
import dji.v5.manager.aircraft.perception.radar.RadarInformation
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
    val radarInformation = MutableLiveData<RadarInformation>()
    val obstacleData = MutableLiveData<ObstacleData>()
    val obstacleDataForRadar = MutableLiveData<ObstacleData>()
    val radarConnect = MutableLiveData<Boolean>()


    fun addPerceptionInfoListener() {
        perceptionManager.addPerceptionInformationListener {
            perceptionInfo.postValue(it)
        }
        perceptionManager.addObstacleDataListener {
            obstacleData.postValue(it)
        }
        perceptionManager.radarManager.addObstacleDataListener {
            obstacleDataForRadar.postValue(it)
        }
        perceptionManager.radarManager.addRadarInformationListener {
            radarInformation.postValue(it)
        }

        KeyManager.getInstance().listen(
            KeyTools.createKey(
                RadarKey.KeyConnection
            ), this
        ) { _, newValue ->
            //雷达拔掉时会返回null值
            if (newValue == null) {
                radarConnect.postValue(false)
            } else {
                radarConnect.postValue(true)
            }
        }
    }

    override fun onCleared() {
        KeyManager.getInstance().cancelListen(this)
    }

    fun setObstacleAvoidanceEnabled(
        isEnabled: Boolean,
        direction: PerceptionDirection,
        callback: CommonCallbacks.CompletionCallback
    ) {
        perceptionManager.setObstacleAvoidanceEnabled(isEnabled, direction, callback)
    }

    fun setRadarObstacleAvoidanceEnabled(
        isEnabled: Boolean,
        direction: PerceptionDirection,
        callback: CommonCallbacks.CompletionCallback
    ) {
        perceptionManager.radarManager.setObstacleAvoidanceEnabled(isEnabled, direction, callback)
    }

    fun setObstacleAvoidanceType(
        type: ObstacleAvoidanceType,
        callback: CommonCallbacks.CompletionCallback
    ) {
        perceptionManager.setObstacleAvoidanceType(type, callback)
    }

    fun setObstacleAvoidanceWarningDistance(
        distance: Double,
        direction: PerceptionDirection,
        callback: CommonCallbacks.CompletionCallback
    ) {
        perceptionManager.setObstacleAvoidanceWarningDistance(distance, direction, callback)
    }


    fun setObstacleAvoidanceBrakingDistance(
        distance: Double,
        direction: PerceptionDirection,
        callback: CommonCallbacks.CompletionCallback
    ) {
        perceptionManager.setObstacleAvoidanceBrakingDistance(distance, direction, callback)
    }


    fun setVisionPositioningEnabled(
        isEnabled: Boolean,
        callback: CommonCallbacks.CompletionCallback
    ) {
        perceptionManager.setVisionPositioningEnabled(isEnabled, callback)
    }


    fun setPrecisionLandingEnabled(
        isEnabled: Boolean,
        callback: CommonCallbacks.CompletionCallback
    ) {
        perceptionManager.setPrecisionLandingEnabled(isEnabled, callback)
    }


}