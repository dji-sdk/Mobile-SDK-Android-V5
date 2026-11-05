package com.dji.sample.gasdetection.gas.wire

import com.dji.sample.gasdetection.gas.catalog.ChannelCatalog
import com.dji.sample.gasdetection.gas.model.FlightPhase
import com.dji.sample.gasdetection.gas.model.GasMeasurement
import com.dji.sample.gasdetection.gas.model.LocationSource

/**
 * 编码结果。
 *
 * 设计意图：超出预算必须是明确失败而不是静默分片或丢通道，调用方才能决定拆批策略。
 */
sealed class EncodeResult {
    /** 编码成功，[payload] 为单帧净荷。 */
    data class Success(val payload: ByteArray) : EncodeResult()

    /** 净荷超出单帧预算；[bytes] 是实际长度，[limit] 是预算。 */
    data class OverBudget(val bytes: Int, val limit: Int) : EncodeResult()

    /** 输入不合法（缺少通道、通道不在目录、会话标识过长等）。 */
    data class Invalid(val reason: String) : EncodeResult()
}

/**
 * 解码结果。
 *
 * 设计意图：把「解码成功但含未知通道」「重复样本」「版本不支持」「帧非法」分开，
 * 让接收侧能分别计数与提示，而不是全部当成失败。
 */
sealed class DecodeResult {
    /** 解码成功；[unknownWireIds] 为目录中不存在的数值键，数值已保留。 */
    data class Success(
        val measurement: GasMeasurement,
        val unknownWireIds: List<Int> = emptyList(),
    ) : DecodeResult()

    /** 同一会话内重复序列号，已被丢弃。 */
    data class Duplicate(val sessionId: String, val sequence: Long) : DecodeResult()

    /** 帧版本高于当前实现。 */
    data class UnsupportedVersion(val actual: Int) : DecodeResult()

    /** 帧结构非法，[reason] 说明具体原因。 */
    data class Rejected(val reason: String) : DecodeResult()
}

/**
 * 解码时由接收侧提供的本机事实。
 *
 * 设计意图：任务、载荷标识、适配器版本、阶段与位置来源都不是载荷帧内容，由插件按本地
 * 事实补全；无法确认的项（尤其 [locationSource]）保持未知，不默认猜测。
 */
data class DecodeContext(
    /** 当前通道目录，用于补全名称、单位与元数据。 */
    val catalog: ChannelCatalog,
    /** 当前任务标识；`null` 表示未关联任务。 */
    val taskId: String? = null,
    /** 载荷标识；`null` 表示未知。 */
    val payloadId: String? = null,
    /** 当前飞行阶段；帧内未携带时使用。 */
    val flightPhase: FlightPhase? = null,
    /** 适配器版本；`null` 表示未知。 */
    val adapterVersion: String? = null,
    /** 位置来源；无法确认时为 [LocationSource.UNKNOWN]。 */
    val locationSource: LocationSource = LocationSource.UNKNOWN,
    /** 接收时间（毫秒），由调用方传入真实时钟值。 */
    val receivedAtMs: Long,
)

/**
 * 测量上行帧编解码器契约。
 *
 * 设计意图：编码与解码是对称的纯同步操作，不持有 Android Context，线程安全；
 * 编码依赖目录把稳定通道标识映射为数值键，解码依赖目录把数值键还原成通道元数据。
 */
interface GasFrameCodec {
    /** 按目录把一条测量编码为单帧净荷；超预算或输入非法时返回明确失败。 */
    fun encode(measurement: GasMeasurement, catalog: ChannelCatalog): EncodeResult

    /** 解码单帧净荷，并用 [context] 补全本机字段。 */
    fun decode(payload: ByteArray, context: DecodeContext): DecodeResult
}
