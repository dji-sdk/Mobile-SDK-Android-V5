package com.dji.sample.gasdetection.gas.payload

import com.dji.sample.gasdetection.gas.model.GasMeasurement

/**
 * 适配器解码结果。
 *
 * 设计意图：把「成功产出测量」「重复样本被忽略」「数据不可解析」分开，让接入诊断能区分
 * 链路重传和设备协议不匹配，而不是把所有异常都记成失败。
 */
sealed class AdapterOutcome {
    /** 成功解码出的测量记录。 */
    data class Measurements(val items: List<GasMeasurement>) : AdapterOutcome()

    /** 样本被有意丢弃（例如同一会话重复序列号），不属于失败。 */
    data class Ignored(val reason: String) : AdapterOutcome()

    /** 数据无法解析或缺少必要条件，计入失败诊断。 */
    data class Rejected(val reason: String) : AdapterOutcome()
}

/**
 * 载荷适配器契约：把某个端口的原始字节翻译成通用测量记录。
 *
 * 设计意图：厂商协议差异只存在于适配器内部，接入服务与业务层只依赖本契约。没有匹配的
 * 适配器时由接入服务显式失败，适配器自身不得猜测格式或返回空成功。
 */
interface PayloadAdapter {
    /** 适配器稳定标识，用于诊断。 */
    val adapterId: String

    /**
     * 解码一帧来自 [port] 的原始数据。
     *
     * [payloadId] 为当前端口的载荷标识（设备未提供时为 `null`）；[receivedAtMs] 为接收时间，
     * 由接入层以真实时钟提供。实现必须同步返回结果，不抛异常；无法解码时返回
     * [AdapterOutcome.Rejected]。
     */
    fun decode(
        port: PayloadPort,
        payloadId: String?,
        bytes: ByteArray,
        receivedAtMs: Long,
    ): AdapterOutcome
}
