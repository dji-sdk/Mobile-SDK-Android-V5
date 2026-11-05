package com.dji.sample.gasdetection.gas.recognition

import android.util.Log
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.intelligent.AutoSensingInfo
import dji.v5.manager.intelligent.AutoSensingInfoListener
import dji.v5.manager.intelligent.AutoSensingTarget
import dji.v5.manager.intelligent.AutoSensingTargetType
import dji.v5.manager.intelligent.IntelligentFlightManager
import dji.v5.manager.interfaces.IIntelligentFlightManager
import java.util.IdentityHashMap

/**
 * 原生识别的 SDK 适配层。
 *
 * 设计意图：本文件是本任务唯一允许 import `dji.v5.manager.intelligent.*` 的地方。它把
 * `AutoSensingInfo` 投影成工程内模型，维护业务 listener 与 SDK listener 的一一配对，
 * 保证释放时能精确移除；命令回调只映射为 [RecognitionCommandResult]，不判断业务开启态。
 */
class SdkRecognitionPort(
    private val manager: IIntelligentFlightManager = IntelligentFlightManager.getInstance(),
) : RecognitionPort {

    private val registered = IdentityHashMap<SensingListener, AutoSensingInfoListener>()

    override fun addListener(listener: SensingListener) {
        val sdkListener = object : AutoSensingInfoListener {
            override fun onAutoSensingInfoUpdate(info: AutoSensingInfo) {
                listener.onFrame(info.toFrame())
            }
        }
        synchronized(registered) { registered[listener] = sdkListener }
        manager.addAutoSensingInfoListener(sdkListener)
        Log.i(TAG, "autoSensing listener registered")
    }

    override fun removeListener(listener: SensingListener) {
        val sdkListener = synchronized(registered) { registered.remove(listener) }
        if (sdkListener == null) {
            Log.w(TAG, "autoSensing listener remove ignored: not registered")
            return
        }
        manager.removeAutoSensingInfoListener(sdkListener)
        Log.i(TAG, "autoSensing listener removed")
    }

    override fun start(onResult: (RecognitionCommandResult) -> Unit) {
        manager.startAutoSensing(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                Log.i(TAG, "startAutoSensing accepted")
                onResult(RecognitionCommandResult.Accepted)
            }

            override fun onFailure(error: IDJIError) {
                Log.e(TAG, "startAutoSensing failed: $error")
                onResult(
                    RecognitionCommandResult.Failed(
                        errorCode = error.errorCode(),
                        description = error.description(),
                    )
                )
            }
        })
    }

    override fun stop() {
        manager.stopAutoSensing(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                Log.i(TAG, "stopAutoSensing accepted")
            }

            override fun onFailure(error: IDJIError) {
                Log.e(TAG, "stopAutoSensing failed: $error")
            }
        })
    }

    private fun AutoSensingInfo.toFrame(): SensingFrame = SensingFrame(
        enabled = isAutoSensingEnable,
        targets = targets.orEmpty().map { it.toInfo() },
    )

    private fun AutoSensingTarget.toInfo(): SensingTargetInfo = SensingTargetInfo(
        index = targetIndex,
        kind = targetType.toTargetKind(),
        label = label,
        rect = rect?.let { TargetRect(it.x, it.y, it.width, it.height) },
    )

    private fun AutoSensingTargetType?.toTargetKind(): TargetKind = when (this) {
        AutoSensingTargetType.PERSON -> TargetKind.PERSON
        AutoSensingTargetType.CAR -> TargetKind.CAR
        AutoSensingTargetType.BOAT -> TargetKind.BOAT
        AutoSensingTargetType.CUSTOM -> TargetKind.CUSTOM
        else -> TargetKind.UNKNOWN
    }

    private companion object {
        const val TAG = "GasVesselRecognition"
    }
}
