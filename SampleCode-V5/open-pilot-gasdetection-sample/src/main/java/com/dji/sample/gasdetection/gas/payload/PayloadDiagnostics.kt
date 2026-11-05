package com.dji.sample.gasdetection.gas.payload

/**
 * 单个端口的接入计数与最近错误。
 *
 * 设计意图：现场排查需要区分「没收到数据」「收到了但没适配器」「适配器解析失败」三种情况，
 * 因此分别计数；只记录长度、端口与原因，不保存完整厂商报文。
 */
data class PortDiagnostics(
    /** 收到的原始帧数。 */
    val receivedFrames: Long = 0,
    /** 成功转换出的测量条数。 */
    val decodedMeasurements: Long = 0,
    /** 被有意忽略的帧数（例如重复序列号）。 */
    val ignoredFrames: Long = 0,
    /** 解码失败帧数。 */
    val failures: Long = 0,
    /** 因未注册适配器而被拒绝的帧数。 */
    val noAdapter: Long = 0,
    /** 最近一次错误原因；没有错误时为 `null`。 */
    val lastError: String? = null,
    /** 最近一次错误时间（毫秒）。 */
    val lastErrorAtMs: Long? = null,
)

/**
 * 接入服务的整体诊断快照。
 *
 * [ports] 以端口为键；[lastCapabilityAtMs] 为最近一次能力快照时间，便于判断能力信息是否过期。
 */
data class PayloadDiagnostics(
    /** 每个端口的诊断计数。 */
    val ports: Map<PayloadPort, PortDiagnostics> = emptyMap(),
    /** 最近一次能力快照时间（毫秒）；从未成功时为 `null`。 */
    val lastCapabilityAtMs: Long? = null,
)
