package com.dji.sample.gasdetection.gas.catalog

import org.json.JSONArray
import org.json.JSONObject

/**
 * 气体配置解析器。
 *
 * 设计意图：配置包含通道目录与时效阈值，任何缺项或非法取值都必须在加载
 * 阶段报出具体原因；解析失败不返回半成品，也不静默套用默认值，避免现场出现
 * 「单位缺失但看起来正常」的假象。
 */
object GasConfigParser {

    /** 从 JSON 文本解析配置；格式错误抛出 [IllegalArgumentException]。 */
    fun parse(raw: String): GasConfig {
        val json = try {
            JSONObject(raw)
        } catch (error: Exception) {
            throw IllegalArgumentException("config is not a JSON object: ${error.message}", error)
        }
        return parse(json)
    }

    /** 从已解析的 JSON 对象读取配置；字段缺失或非法时抛出 [IllegalArgumentException]。 */
    fun parse(json: JSONObject): GasConfig {
        val version = json.requireText("catalogVersion")
        val channels = json.optJSONArray("channels")
            ?: throw IllegalArgumentException("channels is required")
        val descriptors = parseChannels(channels)
        val catalog = ChannelCatalog(version, descriptors)

        val staleAfterMs = json.requirePositiveLong("staleAfterMs")
        return GasConfig(catalog = catalog, staleAfterMs = staleAfterMs)
    }

    private fun parseChannels(channels: JSONArray): List<ChannelDescriptor> {
        if (channels.length() == 0) throw IllegalArgumentException("channels must not be empty")
        val result = ArrayList<ChannelDescriptor>(channels.length())
        for (index in 0 until channels.length()) {
            val item = channels.optJSONObject(index)
                ?: throw IllegalArgumentException("channels[$index] must be an object")
            val wireId = item.requirePositiveInt("wireId")
            val channelId = item.requireText("channelId")
            val displayName = item.requireText("name")
            val unit = item.requireText("unit")
            result.add(
                ChannelDescriptor(
                    wireId = wireId,
                    channelId = channelId,
                    displayName = displayName,
                    unit = unit,
                    rangeMin = item.optionalDouble("rangeMin"),
                    rangeMax = item.optionalDouble("rangeMax"),
                    accuracy = item.optionalDouble("accuracy"),
                    samplingPeriodMs = item.optionalPositiveLong("samplingPeriodMs"),
                    calibrationStatus = item.optionalText("calibrationStatus"),
                ),
            )
        }
        return result
    }

    private fun JSONObject.requireText(key: String): String {
        val value = optString(key, "")
        if (value.isBlank()) throw IllegalArgumentException("$key must be a non-blank string")
        return value
    }

    private fun JSONObject.optionalText(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val value = optString(key, "")
        return value.ifBlank { null }
    }

    private fun JSONObject.optionalDouble(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val value = optDouble(key, Double.NaN)
        if (!value.isFinite()) throw IllegalArgumentException("$key must be a finite number")
        return value
    }

    private fun JSONObject.requirePositiveLong(key: String): Long {
        if (!has(key) || isNull(key)) throw IllegalArgumentException("$key is required")
        val value = optLong(key, Long.MIN_VALUE)
        if (value <= 0L) throw IllegalArgumentException("$key must be > 0, was $value")
        return value
    }

    private fun JSONObject.optionalPositiveLong(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        val value = optLong(key, Long.MIN_VALUE)
        if (value <= 0L) throw IllegalArgumentException("$key must be > 0, was $value")
        return value
    }

    private fun JSONObject.requirePositiveInt(key: String): Int {
        if (!has(key) || isNull(key)) throw IllegalArgumentException("$key is required")
        val value = optInt(key, Int.MIN_VALUE)
        if (value <= 0) throw IllegalArgumentException("$key must be > 0, was $value")
        return value
    }
}
