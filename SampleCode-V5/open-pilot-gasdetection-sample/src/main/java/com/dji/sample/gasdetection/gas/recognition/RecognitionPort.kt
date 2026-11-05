package com.dji.sample.gasdetection.gas.recognition

/**
 * 识别开启/停止命令的结果。
 *
 * 设计意图：命令回调只说明 SDK 是否接受了下发，不代表识别已开启；[Accepted] 之后仍必须等
 * 权威开启态（[SensingFrame.enabled]）。错误码与描述完整保留用于日志，界面只映射原因类别。
 */
sealed class RecognitionCommandResult {
    data object Accepted : RecognitionCommandResult()

    data class Failed(
        val errorCode: String?,
        val description: String?,
    ) : RecognitionCommandResult()
}

/** 识别订阅回调（对应 SDK `AutoSensingInfoListener.onAutoSensingInfoUpdate`）。 */
fun interface SensingListener {
    fun onFrame(frame: SensingFrame)
}

/**
 * 原生识别端口。
 *
 * 设计意图：把 `IIntelligentFlightManager` 的 AutoSensing 调用收敛到可替换实现后面，使会话、
 * 连续有效判定与降级逻辑能在无真机时用假端口验证。调用纪律：必须先 [addListener] 再
 * [start]；释放时用 [removeListener] 精确移除，禁止批量清理。
 */
interface RecognitionPort {
    /** 注册识别结果回调。 */
    fun addListener(listener: SensingListener)

    /** 精确移除此前注册的回调；未注册时忽略。 */
    fun removeListener(listener: SensingListener)

    /** 下发开启命令；回调只代表命令结果。 */
    fun start(onResult: (RecognitionCommandResult) -> Unit)

    /** 下发停止命令；结果只记录日志。 */
    fun stop()
}
