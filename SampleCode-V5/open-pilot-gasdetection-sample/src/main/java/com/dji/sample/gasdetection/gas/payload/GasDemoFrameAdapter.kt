package com.dji.sample.gasdetection.gas.payload

import com.dji.sample.gasdetection.gas.catalog.ChannelCatalog
import com.dji.sample.gasdetection.gas.model.FlightPhase
import com.dji.sample.gasdetection.gas.model.LocationSource
import com.dji.sample.gasdetection.gas.wire.DecodeContext
import com.dji.sample.gasdetection.gas.wire.DecodeResult
import com.dji.sample.gasdetection.gas.wire.JsonMeasurementCodec

/**
 * 复用 FR-1 紧凑帧的演示适配器。
 *
 * 设计意图：本仓库没有目标气体载荷的厂商协议，但需要一条能跑通「载荷数据 → 通用测量」的
 * 真实路径，供后续界面与告警联调。本适配器只解析本仓库自有演示载荷端发送的紧凑 JSON 帧，
 * **不是任何厂商协议**；接入真实设备时应另写适配器并在注册表里替换。
 *
 * 解码补全：载荷标识取设备产品名（未提供为 `null`），接收时间由接入层给出；位置与位置来源
 * 在本轮保持未知，飞行阶段按本机补全。
 */
class GasDemoFrameAdapter(
    private val catalog: ChannelCatalog,
    private val adapterVersion: String = "demo-frame-1",
) : PayloadAdapter {

    override val adapterId: String = "demo-frame-json"

    private val codec = JsonMeasurementCodec()

    override fun decode(
        port: PayloadPort,
        payloadId: String?,
        bytes: ByteArray,
        receivedAtMs: Long,
    ): AdapterOutcome {
        val context = DecodeContext(
            catalog = catalog,
            taskId = null,
            payloadId = payloadId,
            flightPhase = FlightPhase.MANUAL,
            adapterVersion = adapterVersion,
            locationSource = LocationSource.UNKNOWN,
            receivedAtMs = receivedAtMs,
        )
        return when (val result = codec.decode(bytes, context)) {
            is DecodeResult.Success -> AdapterOutcome.Measurements(listOf(result.measurement))
            is DecodeResult.Duplicate ->
                AdapterOutcome.Ignored("duplicate sequence ${result.sequence} in session ${result.sessionId}")
            is DecodeResult.UnsupportedVersion ->
                AdapterOutcome.Rejected("unsupported frame version ${result.actual}")
            is DecodeResult.Rejected -> AdapterOutcome.Rejected(result.reason)
        }
    }
}
