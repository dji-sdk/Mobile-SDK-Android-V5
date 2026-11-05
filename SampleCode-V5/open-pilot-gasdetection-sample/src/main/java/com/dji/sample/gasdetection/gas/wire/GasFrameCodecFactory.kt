package com.dji.sample.gasdetection.gas.wire

/**
 * 帧编码槽位。
 *
 * 设计意图：与 `psdk-bridge` 的 `PayloadEncoding` 语义对齐，本期只实现 JSON；
 * BINARY 保留槽位，后续需要更小净荷时再实现，不在运行期静默回落。
 */
enum class GasWireEncoding {
    JSON,
    BINARY,
}

/**
 * 编解码器创建结果。
 *
 * 设计意图：不可用的编码必须显式返回 [EncodingUnavailable]，让上层决定提示或降级策略，
 * 而不是悄悄换成另一种编码、造成两端格式不一致。
 */
sealed class GasFrameCodecResult {
    /** 创建成功。 */
    data class Available(val codec: GasFrameCodec) : GasFrameCodecResult()

    /** 该编码本期未实现。 */
    data class EncodingUnavailable(val encoding: GasWireEncoding) : GasFrameCodecResult()
}

/**
 * 帧编解码器工厂。
 *
 * 设计意图：按编码槽位创建实现，JSON 返回可用编解码器，BINARY 明确不可用。
 */
object GasFrameCodecFactory {
    /** 按 [encoding] 创建编解码器；BINARY 返回明确的不可用结果。 */
    fun create(encoding: GasWireEncoding): GasFrameCodecResult = when (encoding) {
        GasWireEncoding.JSON -> GasFrameCodecResult.Available(JsonMeasurementCodec())
        GasWireEncoding.BINARY -> GasFrameCodecResult.EncodingUnavailable(GasWireEncoding.BINARY)
    }
}
