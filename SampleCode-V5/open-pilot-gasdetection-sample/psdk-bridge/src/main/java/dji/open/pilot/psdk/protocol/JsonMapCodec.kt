package dji.open.pilot.psdk.protocol

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/**
 * Strict UTF-8 JSON object <-> `Map<String, Any?>` codec.
 *
 * Enforces [maxMessageBytes] and a max nesting depth of 16, rejects trailing data, non-object
 * roots, non-string keys, non-finite numbers, ByteArray and ULong. `null` maps to
 * [JSONObject.NULL] and back. This keeps the wire representation portable across payload
 * SDKs and languages.
 */
internal class JsonMapCodec(private val maxMessageBytes: Int) {
    private val maxDepth = 16

    fun encode(params: Map<String, Any?>): ByteArray {
        val json = JSONObject()
        params.forEach { (key, value) -> json.put(key, toJson(value, 0)) }
        val bytes = json.toString().toByteArray(Charsets.UTF_8)
        if (bytes.size > maxMessageBytes) throw ProtocolViolation("JSON message ${bytes.size} exceeds maxMessageBytes=$maxMessageBytes")
        return bytes
    }

    fun decode(bytes: ByteArray): Map<String, Any?> {
        if (bytes.isEmpty()) throw ProtocolViolation("empty JSON payload")
        if (bytes.size > maxMessageBytes) throw ProtocolViolation("JSON message exceeds maxMessageBytes=$maxMessageBytes")
        val text = try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (error: Exception) {
            throw ProtocolViolation("invalid UTF-8 JSON", error)
        }
        val tokens = JSONTokener(text)
        val value = tokens.nextValue()
        if (tokens.nextClean() != '\u0000') throw ProtocolViolation("trailing data after JSON object")
        val json = value as? JSONObject ?: throw ProtocolViolation("JSON payload must be an object")
        return fromObject(json, 0)
    }

    private fun toJson(value: Any?, depth: Int): Any {
        if (depth > maxDepth) throw ProtocolViolation("JSON nesting exceeds $maxDepth")
        return when (value) {
            null -> JSONObject.NULL
            is String, is Boolean, is Byte, is Short, is Int, is Long -> value
            is Float -> if (value.isFinite()) value else throw ProtocolViolation("non-finite float")
            is Double -> if (value.isFinite()) value else throw ProtocolViolation("non-finite double")
            is Map<*, *> -> JSONObject().also { json ->
                value.forEach { (key, item) ->
                    val name = key as? String ?: throw ProtocolViolation("JSON map keys must be strings")
                    json.put(name, toJson(item, depth + 1))
                }
            }
            is List<*> -> JSONArray().also { array -> value.forEach { array.put(toJson(it, depth + 1)) } }
            is ByteArray -> throw ProtocolViolation("ByteArray is not supported by JSON encoding")
            is ULong -> throw ProtocolViolation("ULong is not supported by JSON encoding")
            else -> throw ProtocolViolation("unsupported JSON value type ${value.javaClass.simpleName}")
        }
    }

    private fun fromObject(json: JSONObject, depth: Int): Map<String, Any?> {
        if (depth > maxDepth) throw ProtocolViolation("JSON nesting exceeds $maxDepth")
        val result = linkedMapOf<String, Any?>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            result[key] = fromJson(json.get(key), depth + 1)
        }
        return result
    }

    private fun fromJson(value: Any, depth: Int): Any? {
        if (depth > maxDepth) throw ProtocolViolation("JSON nesting exceeds $maxDepth")
        return when {
            value === JSONObject.NULL -> null
            value is JSONObject -> fromObject(value, depth)
            value is JSONArray -> (0 until value.length()).map { fromJson(value.get(it), depth + 1) }
            value is Double && !value.isFinite() -> throw ProtocolViolation("non-finite JSON number")
            else -> value
        }
    }
}
