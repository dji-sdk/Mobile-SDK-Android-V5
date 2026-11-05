package com.dji.sample.gasdetection.gas.diagnostics

import com.dji.sample.gasdetection.gas.catalog.ChannelDescriptor
import com.dji.sample.gasdetection.gas.payload.PayloadDiagnostics
import com.dji.sample.gasdetection.gas.payload.PayloadSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 诊断日志导出的输入快照。
 *
 * 设计意图：导出内容只包含可复核的连接、接入、端口计数与目录事实，不携带任何原始载荷
 * 字节或完整任务正文；所有字段由调用方从既有状态流取值，构建器不做业务补全。
 */
data class DiagnosticsReportInput(
    val exportedAtMs: Long,
    val appVersion: String,
    val sdkRegistered: Boolean,
    val pilotConnected: Boolean,
    val aircraftConnected: Boolean,
    val ingestStarted: Boolean,
    val monitoredPorts: Int,
    val onlinePayloads: Int,
    val payloads: List<PayloadSnapshot>,
    val diagnostics: PayloadDiagnostics,
    val catalogVersion: String?,
    val catalogChannels: List<ChannelDescriptor>,
    val lastMeasurementSourceMs: Long?,
    val lastMeasurementReceivedMs: Long?,
    val configError: String?,
)

/**
 * 诊断日志文本构建器。
 *
 * 设计意图：把导出格式固定在纯 Kotlin 层，便于 JVM 单测校验字段完整性与「不含原始报文」的
 * 约束；文件写入与系统分享由界面层负责。
 */
object DiagnosticsReportBuilder {

    private const val NOT_PROVIDED = "-"
    private const val HEADER = "gas-diagnostics-v1"

    /**
     * 生成可读的诊断文本。
     *
     * 输出为 `key=value` 行，未知值统一写 `-`；端口与目录按输入顺序逐行展开，便于 diff。
     */
    fun build(input: DiagnosticsReportInput): String {
        val lines = mutableListOf<String>()
        lines += HEADER
        lines += "exportedAt=${formatTime(input.exportedAtMs)}"
        lines += "appVersion=${input.appVersion}"
        lines += "sdkRegistered=${input.sdkRegistered}"
        lines += "pilotConnected=${input.pilotConnected}"
        lines += "aircraftConnected=${input.aircraftConnected}"
        lines += "ingestStarted=${input.ingestStarted}"
        lines += "monitoredPorts=${input.monitoredPorts}"
        lines += "onlinePayloads=${input.onlinePayloads}"
        lines += "configError=${input.configError ?: NOT_PROVIDED}"
        lines += "catalogVersion=${input.catalogVersion ?: NOT_PROVIDED}"
        lines += "catalogChannelCount=${input.catalogChannels.size}"
        input.catalogChannels.forEach { channel ->
            lines += "catalogChannel wireId=${channel.wireId} channelId=${channel.channelId} " +
                "displayName=${channel.displayName} unit=${channel.unit}"
        }

        lines += "payloadCount=${input.payloads.size}"
        input.payloads.forEach { payload ->
            lines += buildString {
                append("payload port=").append(payload.port.name)
                append(" portValue=").append(payload.port.value)
                append(" connected=").append(payload.connected)
                append(" type=").append(payload.payloadType ?: NOT_PROVIDED)
                append(" product=").append(payload.productName ?: NOT_PROVIDED)
                append(" phase=").append(payload.productPhase ?: NOT_PROVIDED)
                append(" serial=").append(payload.serialNumber ?: NOT_PROVIDED)
                append(" firmware=").append(payload.firmwareVersion ?: NOT_PROVIDED)
                append(" featureOpened=").append(payload.featureOpened)
                append(" updatedAt=").append(formatTime(payload.updatedAtMs))
            }
        }

        val portCounts = input.diagnostics.ports
        lines += "portDiagnosticsCount=${portCounts.size}"
        portCounts.forEach { (port, counts) ->
            lines += buildString {
                append("portDiagnostics port=").append(port.name)
                append(" received=").append(counts.receivedFrames)
                append(" decoded=").append(counts.decodedMeasurements)
                append(" ignored=").append(counts.ignoredFrames)
                append(" failures=").append(counts.failures)
                append(" noAdapter=").append(counts.noAdapter)
                append(" lastError=").append(counts.lastError ?: NOT_PROVIDED)
                append(" lastErrorAt=").append(counts.lastErrorAtMs?.let(::formatTime) ?: NOT_PROVIDED)
            }
        }

        lines += "lastMeasurementSourceAt=" +
            (input.lastMeasurementSourceMs?.let(::formatTime) ?: NOT_PROVIDED)
        lines += "lastMeasurementReceivedAt=" +
            (input.lastMeasurementReceivedMs?.let(::formatTime) ?: NOT_PROVIDED)
        if (input.lastMeasurementSourceMs != null && input.lastMeasurementReceivedMs != null) {
            lines += "lastMeasurementDeltaMs=" +
                (input.lastMeasurementReceivedMs - input.lastMeasurementSourceMs)
        } else {
            lines += "lastMeasurementDeltaMs=$NOT_PROVIDED"
        }
        return lines.joinToString(separator = "\n", postfix = "\n")
    }

    private fun formatTime(epochMs: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(epochMs))
}
