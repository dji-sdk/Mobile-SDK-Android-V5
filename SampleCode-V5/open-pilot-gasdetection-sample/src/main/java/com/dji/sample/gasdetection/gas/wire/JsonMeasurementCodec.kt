package com.dji.sample.gasdetection.gas.wire

import com.dji.sample.gasdetection.gas.catalog.ChannelCatalog
import com.dji.sample.gasdetection.gas.catalog.ResolvedChannel
import com.dji.sample.gasdetection.gas.model.ChannelValue
import com.dji.sample.gasdetection.gas.model.FlightPhase
import com.dji.sample.gasdetection.gas.model.GasMeasurement
import com.dji.sample.gasdetection.gas.model.GeoPosition
import com.dji.sample.gasdetection.gas.model.MeasurementOrigins
import com.dji.sample.gasdetection.gas.model.MeasurementQuality
import com.dji.sample.gasdetection.gas.model.ValueOrigin
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/**
 * 版本 1 紧凑 JSON 测量帧编解码器。
 *
 * 设计意图：一条测量必须落在单帧预算内，因此帧内只带可变数据（短键、目录分配的数值键、
 * 位置数组、质量码），名称/单位/量程等元数据由通道目录在解码时补全。超预算时返回
 * [EncodeResult.OverBudget]，不分片、不截断、不丢通道；解码侧对未知通道降级而不是让整条
 * 测量失败。
 *
 * 线程安全：编解码是无状态纯函数，[DuplicateSequenceFilter] 的去重状态由调用方串行使用。
 */
class JsonMeasurementCodec(
    private val duplicateFilter: DuplicateSequenceFilter = DuplicateSequenceFilter(),
) : GasFrameCodec {

    override fun encode(measurement: GasMeasurement, catalog: ChannelCatalog): EncodeResult {
        if (measurement.sessionId.isBlank() || measurement.sessionId.length > GasWireLimits.MAX_SESSION_ID_LENGTH) {
            return EncodeResult.Invalid(
                "sessionId must be 1..${GasWireLimits.MAX_SESSION_ID_LENGTH} characters, " +
                    "was ${measurement.sessionId.length}",
            )
        }
        if (measurement.channels.isEmpty()) return EncodeResult.Invalid("measurement has no channels")
        if (measurement.position.valid &&
            (measurement.position.latitude == null || measurement.position.longitude == null)
        ) {
            return EncodeResult.Invalid("valid position requires latitude and longitude")
        }

        val channelArray = JSONArray()
        measurement.channels.forEach { channel ->
            val wireId = catalog.wireIdOf(channel.channelId)
                ?: return EncodeResult.Invalid("channel ${channel.channelId} is not in catalog")
            channelArray.put(
                JSONArray().apply {
                    put(wireId)
                    put(roundToFourDecimals(channel.value))
                    put(channel.quality.code)
                },
            )
        }

        val json = JSONObject().apply {
            put("v", GasWireLimits.VERSION)
            put("s", measurement.sessionId)
            put("n", measurement.sequence)
            put("t", measurement.sourceTimestampMs)
            put("p", positionArray(measurement.position))
            put("g", channelArray)
            put("f", measurement.flightPhase.code)
            put("a", if (measurement.automaticMode) 1 else 0)
        }
        measurement.targetId?.takeIf { it.isNotBlank() }?.let { json.put("o", it) }
        measurement.targetConfidence?.let { json.put("c", roundToFourDecimals(it)) }

        val payload = json.toString().toByteArray(Charsets.UTF_8)
        return if (payload.size > GasWireLimits.MAX_PAYLOAD_BYTES) {
            EncodeResult.OverBudget(payload.size, GasWireLimits.MAX_PAYLOAD_BYTES)
        } else {
            EncodeResult.Success(payload)
        }
    }

    override fun decode(payload: ByteArray, context: DecodeContext): DecodeResult {
        if (payload.isEmpty()) return DecodeResult.Rejected("empty payload")
        val json = try {
            JSONObject(decodeUtf8(payload))
        } catch (error: Exception) {
            return DecodeResult.Rejected("invalid JSON: ${error.message}")
        }

        if (!json.has("v")) return DecodeResult.Rejected("version missing")
        val version = json.optInt("v", -1)
        if (version != GasWireLimits.VERSION) return DecodeResult.UnsupportedVersion(version)

        val sessionId = json.optString("s", "")
        if (sessionId.isBlank()) return DecodeResult.Rejected("sessionId missing")
        if (!json.has("n")) return DecodeResult.Rejected("sequence missing")
        val sequence = json.optLong("n", Long.MIN_VALUE)
        if (!json.has("t")) return DecodeResult.Rejected("sourceTimestamp missing")
        val sourceTimestampMs = json.optLong("t", Long.MIN_VALUE)
        val rawChannels = json.optJSONArray("g")
            ?: return DecodeResult.Rejected("channels missing")
        if (rawChannels.length() == 0) return DecodeResult.Rejected("channels empty")

        val position = parsePosition(json.optJSONArray("p"))
            ?: return DecodeResult.Rejected("position array invalid")

        val phase: FlightPhase
        val phaseOrigin: ValueOrigin
        if (json.has("f")) {
            val code = json.optInt("f", Int.MIN_VALUE)
            phase = FlightPhase.fromCode(code)
                ?: return DecodeResult.Rejected("unknown flight phase code $code")
            phaseOrigin = ValueOrigin.DEVICE
        } else {
            phase = context.flightPhase ?: FlightPhase.MANUAL
            phaseOrigin = ValueOrigin.LOCAL
        }

        val unknownWireIds = mutableListOf<Int>()
        val channels = ArrayList<ChannelValue>(rawChannels.length())
        for (index in 0 until rawChannels.length()) {
            val entry = rawChannels.optJSONArray(index)
                ?: return DecodeResult.Rejected("channel[$index] must be an array")
            if (entry.length() < 3) return DecodeResult.Rejected("channel[$index] must have 3 fields")
            val wireId = entry.optInt(0, Int.MIN_VALUE)
            if (wireId == Int.MIN_VALUE) return DecodeResult.Rejected("channel[$index] wireId missing")
            val value = entry.optDouble(1, Double.NaN)
            if (!value.isFinite()) return DecodeResult.Rejected("channel[$index] value invalid")
            val qualityCode = entry.optInt(2, Int.MIN_VALUE)
            val quality = MeasurementQuality.fromCode(qualityCode)
                ?: return DecodeResult.Rejected("channel[$index] unknown quality code $qualityCode")

            channels.add(
                when (val resolved = context.catalog.resolve(wireId)) {
                    is ResolvedChannel.Known -> {
                        val descriptor = resolved.descriptor
                        ChannelValue(
                            channelId = descriptor.channelId,
                            value = value,
                            unit = descriptor.unit,
                            quality = quality,
                            displayName = descriptor.displayName,
                            rangeMin = descriptor.rangeMin,
                            rangeMax = descriptor.rangeMax,
                            accuracy = descriptor.accuracy,
                            samplingPeriodMs = descriptor.samplingPeriodMs,
                            calibrationStatus = descriptor.calibrationStatus,
                        )
                    }

                    is ResolvedChannel.Unknown -> {
                        unknownWireIds.add(wireId)
                        ChannelValue(
                            channelId = "wire:$wireId",
                            value = value,
                            unit = null,
                            quality = quality,
                        )
                    }
                },
            )
        }

        val normalizedChannels = if (sourceTimestampMs > context.receivedAtMs) {
            channels.map { channel ->
                if (channel.quality == MeasurementQuality.VALID) {
                    channel.copy(quality = MeasurementQuality.TIME_UNCERTAIN)
                } else {
                    channel
                }
            }
        } else {
            channels
        }

        val measurement = GasMeasurement(
            measurementId = "$sessionId#$sequence",
            sessionId = sessionId,
            sequence = sequence,
            payloadId = context.payloadId,
            sourceTimestampMs = sourceTimestampMs,
            receivedTimestampMs = context.receivedAtMs,
            channels = normalizedChannels,
            position = position,
            locationSource = context.locationSource,
            taskId = context.taskId,
            flightPhase = phase,
            automaticMode = json.optInt("a", 0) == 1,
            targetId = json.optString("o", "").ifBlank { null },
            targetConfidence = if (json.has("c") && !json.isNull("c")) json.optDouble("c") else null,
            adapterVersion = context.adapterVersion,
            origins = MeasurementOrigins(
                taskId = ValueOrigin.LOCAL,
                payloadId = ValueOrigin.LOCAL,
                flightPhase = phaseOrigin,
                receivedTime = ValueOrigin.LOCAL,
            ),
        )

        if (duplicateFilter.isDuplicate(sessionId, sequence)) {
            return DecodeResult.Duplicate(sessionId, sequence)
        }
        return DecodeResult.Success(measurement, unknownWireIds)
    }

    private fun positionArray(position: GeoPosition): JSONArray {
        val array = JSONArray()
        if (position.valid) {
            array.put(position.latitude ?: JSONObject.NULL)
            array.put(position.longitude ?: JSONObject.NULL)
            array.put(position.altitudeM ?: JSONObject.NULL)
            array.put(position.horizontalAccuracyM ?: JSONObject.NULL)
            array.put(1)
        } else {
            array.put(JSONObject.NULL)
            array.put(JSONObject.NULL)
            array.put(JSONObject.NULL)
            array.put(JSONObject.NULL)
            array.put(0)
        }
        return array
    }

    private fun parsePosition(array: JSONArray?): GeoPosition? {
        if (array == null) return GeoPosition.UNKNOWN
        if (array.length() < 5) return null
        val valid = array.optInt(4, 0) == 1
        if (!valid) return GeoPosition.UNKNOWN
        val latitude = array.optionalDouble(0) ?: return null
        val longitude = array.optionalDouble(1) ?: return null
        return GeoPosition(
            latitude = latitude,
            longitude = longitude,
            altitudeM = array.optionalDouble(2),
            horizontalAccuracyM = array.optionalDouble(3),
            valid = true,
        )
    }

    private fun JSONArray.optionalDouble(index: Int): Double? {
        if (index >= length() || isNull(index)) return null
        val value = optDouble(index, Double.NaN)
        return if (value.isFinite()) value else null
    }

    private fun decodeUtf8(bytes: ByteArray): String = Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes))
        .toString()

    private fun roundToFourDecimals(value: Double): Double =
        BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).toDouble()
}
