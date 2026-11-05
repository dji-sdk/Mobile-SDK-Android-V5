package com.dji.sample.gasdetection.gas.remote

import android.util.Log
import dji.v5.common.error.IDJIError
import dji.v5.manager.intelligent.IntelligentFlightInfo
import dji.v5.manager.intelligent.IntelligentFlightInfoListener
import dji.v5.manager.intelligent.MissionType
import dji.v5.manager.intelligent.IntelligentFlightManager
import dji.v5.manager.interfaces.IIntelligentFlightManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 自动作业能力结论。未收到飞机回调前保持 [Unknown]，不默认视为支持。 */
sealed class AutoFlightCapability {
    data object Unknown : AutoFlightCapability()
    data object Supported : AutoFlightCapability()
    data object Unsupported : AutoFlightCapability()
}

/**
 * 只读探测机型是否支持 `FLY_TO`。
 *
 * 设计意图：FR-4 只做能力预检，不下发任何飞行命令；能力结论以 `IntelligentFlightInfo`
 * 回调为唯一权威，注册与移除成对出现，断开时清空结论，避免缓存上一次机型的能力。
 */
class AutoFlightCapabilityProbe(
    private val manager: IIntelligentFlightManager = IntelligentFlightManager.getInstance(),
) {

    private val mutableState = MutableStateFlow<AutoFlightCapability>(AutoFlightCapability.Unknown)
    val state: StateFlow<AutoFlightCapability> = mutableState.asStateFlow()

    /** 最近一次回调的智能任务占用；`null` 表示尚未收到回调。 */
    private val mutableRunningMission = MutableStateFlow<MissionType?>(null)
    val runningMission: StateFlow<MissionType?> = mutableRunningMission.asStateFlow()

    private var listener: IntelligentFlightInfoListener? = null

    /** 注册能力监听；重复调用幂等。 */
    fun start() {
        if (listener != null) return
        val value = object : IntelligentFlightInfoListener {
            override fun onIntelligentFlightInfoUpdate(info: IntelligentFlightInfo) {
                val supported = info.supportedMissions.orEmpty()
                    .contains(MissionType.FLY_TO)
                mutableState.value = if (supported) {
                    AutoFlightCapability.Supported
                } else {
                    AutoFlightCapability.Unsupported
                }
                mutableRunningMission.value = info.runningMission
            }

            override fun onIntelligentFlightErrorUpdate(error: IDJIError) {
                // 错误不改变能力结论，但必须完整留痕：全局智能错误只从这条回调抛出。
                Log.e(TAG, "intelligent flight error: $error")
            }
        }
        listener = value
        manager.addIntelligentFlightInfoListener(value)
    }

    /** 移除监听并清空结论。 */
    fun stop() {
        listener?.let(manager::removeIntelligentFlightInfoListener)
        listener = null
        mutableState.value = AutoFlightCapability.Unknown
        mutableRunningMission.value = null
    }

    private companion object {
        const val TAG = "GasVesselRecognition"
    }
}
