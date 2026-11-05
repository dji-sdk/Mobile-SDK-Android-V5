package com.dji.sample.gasdetection.gas.record

import com.dji.sample.gasdetection.gas.model.AlertType
import org.json.JSONArray
import org.json.JSONObject

/**
 * 检测记录的 JSON 编解码。
 *
 * 设计意图：队列文件与上传请求共用同一份编码，避免「本地能存、远端解析不了」的字段漂移。
 * 解析失败返回 `null` 或空列表，由调用方决定是保留原文件还是跳过损坏项。
 */
object InspectionJson {

    private const val VERSION = 1

    /** 把记录列表编码成队列文件内容。 */
    fun encodeQueue(records: List<InspectionRecord>): String {
        val array = JSONArray()
        records.forEach { array.put(toJson(it)) }
        return JSONObject()
            .put("version", VERSION)
            .put("queue", array)
            .toString()
    }

    /** 解析队列文件；版本不符或结构损坏时返回空列表。 */
    fun decodeQueue(raw: String?): List<InspectionRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val root = JSONObject(raw)
            if (root.optInt("version", 0) != VERSION) return emptyList()
            val array = root.optJSONArray("queue") ?: return emptyList()
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    fromJson(item)?.let { add(it) }
                }
            }
        }.getOrDefault(emptyList())
    }

    /** 单条记录的上传载荷。 */
    fun payload(record: InspectionRecord): String = toJson(record).toString()

    fun toJson(record: InspectionRecord): JSONObject = JSONObject().apply {
        put("recordId", record.recordId)
        put("jobId", record.jobId)
        put("jobTitle", record.jobTitle)
        put("startedAtMs", record.startedAtMs ?: JSONObject.NULL)
        put("endedAtMs", record.endedAtMs)
        put("endReason", record.endReason.name)
        put("totalSamples", record.totalSamples)
        put("validSamples", record.validSamples)
        put("invalidSamples", record.invalidSamples)
        put("channels", JSONArray().apply {
            record.channels.forEach { channel ->
                put(JSONObject().apply {
                    put("channelId", channel.channelId)
                    put("displayName", channel.displayName ?: "")
                    put("unit", channel.unit ?: "")
                    put("validSamples", channel.validSamples)
                    put("min", channel.min)
                    put("max", channel.max)
                    put("average", channel.average)
                })
            }
        })
        put("alerts", JSONArray().apply {
            record.alerts.forEach { summary ->
                put(JSONObject().put("type", summary.type.name).put("count", summary.count))
            }
        })
        put("alertIds", JSONArray(record.alertIds))
        put("vesselId", record.vesselId ?: "")
        put("vesselName", record.vesselName ?: "")
        put("vesselConfirmed", record.vesselConfirmed)
        put("status", record.status.name)
        put("retryCount", record.retryCount)
        put("lastError", record.lastError ?: "")
        put("completionPending", record.completionPending)
        put("uploadedAtMs", record.uploadedAtMs ?: JSONObject.NULL)
    }

    fun fromJson(json: JSONObject): InspectionRecord? = runCatching {
        val channels = mutableListOf<ChannelSummary>()
        json.optJSONArray("channels")?.let { array ->
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                channels += ChannelSummary(
                    channelId = item.getString("channelId"),
                    displayName = item.optString("displayName").ifBlank { null },
                    unit = item.optString("unit").ifBlank { null },
                    validSamples = item.optInt("validSamples"),
                    min = item.optDouble("min", 0.0),
                    max = item.optDouble("max", 0.0),
                    average = item.optDouble("average", 0.0),
                )
            }
        }
        val alerts = mutableListOf<AlertSummary>()
        json.optJSONArray("alerts")?.let { array ->
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val type = AlertType.entries.firstOrNull { it.name == item.optString("type") } ?: continue
                alerts += AlertSummary(type, item.optInt("count"))
            }
        }
        val alertIds = mutableListOf<String>()
        json.optJSONArray("alertIds")?.let { array ->
            for (index in 0 until array.length()) alertIds += array.getString(index)
        }
        InspectionRecord(
            recordId = json.getString("recordId"),
            jobId = json.getString("jobId"),
            jobTitle = json.optString("jobTitle"),
            startedAtMs = if (json.isNull("startedAtMs")) null else json.optLong("startedAtMs"),
            endedAtMs = json.optLong("endedAtMs"),
            endReason = RecordEndReason.entries.firstOrNull { it.name == json.optString("endReason") }
                ?: RecordEndReason.PILOT_FINISH,
            totalSamples = json.optInt("totalSamples"),
            validSamples = json.optInt("validSamples"),
            invalidSamples = json.optInt("invalidSamples"),
            channels = channels,
            alerts = alerts,
            alertIds = alertIds,
            vesselId = json.optString("vesselId").ifBlank { null },
            vesselName = json.optString("vesselName").ifBlank { null },
            vesselConfirmed = json.optBoolean("vesselConfirmed"),
            status = RecordStatus.entries.firstOrNull { it.name == json.optString("status") }
                ?: RecordStatus.QUEUED,
            retryCount = json.optInt("retryCount"),
            lastError = json.optString("lastError").ifBlank { null },
            completionPending = json.optBoolean("completionPending"),
            uploadedAtMs = if (json.isNull("uploadedAtMs")) null else json.optLong("uploadedAtMs"),
        )
    }.getOrNull()
}
