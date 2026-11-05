package com.dji.sample.gasdetection.gas.payload

/**
 * 接入服务的可观察状态。
 *
 * 设计意图：界面与诊断只需要知道服务是否在监听、监听了几个端口、当前有几个载荷在线，
 * 不需要读取内部监听器表。
 */
data class IngestState(
    /** 服务是否处于监听状态。 */
    val started: Boolean = false,
    /** 已注册监听的端口数（不含 EXTERNAL）。 */
    val monitoredPorts: Int = 0,
    /** 当前在线载荷数。 */
    val onlinePayloads: Int = 0,
)

/**
 * 启动结果。
 *
 * 设计意图：监听注册失败必须显式返回原因，让接线层记录问题而不是静默处于「未启动」。
 */
sealed class IngestStartResult {
    /** 已开始监听（或此前已启动）。 */
    object Started : IngestStartResult()

    /** 启动失败，[reason] 为具体原因；已注册的监听会被回滚。 */
    data class Failed(val reason: String) : IngestStartResult()
}
