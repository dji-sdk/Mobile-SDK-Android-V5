package com.dji.sample.gasdetection

import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.payload.listener.PayloadDataListener
import dji.v5.manager.interfaces.IPayloadManager

/** Raw bytes only. Owner must initialize SDK/PayloadCenter and select the real port.
 * Callbacks run on SDK threads and must be short. close removes only our listener.
 * Success is SDK submission, never a business acknowledgement.
 */
class RawPayloadChannel(
    private val manager: IPayloadManager,
    val maxBytes: Int = 255,
    private val onReceive: (ByteArray) -> Unit,
    private val onReceiveError: (String) -> Unit = {}
) : AutoCloseable {
    private val lock = Any()
    private var closed = false
    private val listener = PayloadDataListener { bytes ->
        synchronized(lock) {
            if (!closed) {
                if (bytes.isEmpty() || bytes.size > maxBytes) {
                    onReceiveError("RX rejected: len=${bytes.size}, max=$maxBytes")
                } else onReceive(bytes.copyOf())
            }
        }
    }

    init {
        require(maxBytes in 1..255) { "maxBytes must be 1..255" }
        manager.addPayloadDataListener(listener)
    }

    fun send(bytes: ByteArray, onSubmitted: () -> Unit, onError: (String) -> Unit) {
        sendTyped(bytes, onSubmitted, { onError(it.toString()) }, onError)
    }

    /** JSON 通道使用类型化 SDK 错误，避免丢失错误对象；原有 send 保持兼容。 */
    internal fun sendTyped(
        bytes: ByteArray,
        onSubmitted: () -> Unit,
        onSdkError: (IDJIError) -> Unit,
        onRejected: (String) -> Unit
    ) {
        synchronized(lock) {
            when {
                closed -> { onRejected("Channel closed"); return }
                bytes.isEmpty() -> { onRejected("Cannot send empty data"); return }
                bytes.size > maxBytes -> { onRejected("TX rejected: len=${bytes.size}, max=$maxBytes"); return }
            }
            manager.sendDataToPayload(bytes.copyOf(), object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() = synchronized(lock) { if (!closed) onSubmitted() }
                override fun onFailure(error: IDJIError) = synchronized(lock) { if (!closed) onSdkError(error) }
            })
        }
    }

    override fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
        }
        // Avoid holding our lock while SDK removal may wait for an in-flight callback.
        manager.removePayloadDataListener(listener)
    }

    companion object {
        fun parseHex(input: String): ByteArray {
            fun isHexDigit(c: Char) = c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F'
            val groups = input.trim().split(Regex("\\s+"))
            require(groups.all { it.isNotEmpty() && it.length % 2 == 0 && it.all { c -> isHexDigit(c) } }) {
                "Hex requires complete byte pairs, e.g. 00 41 FF"
            }
            val text = groups.joinToString("")
            require(text.length <= 510) { "Hex exceeds 255 bytes" }
            return ByteArray(text.length / 2) { text.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        }

        fun hex(bytes: ByteArray): String {
            val digits = "0123456789ABCDEF"
            return buildString(bytes.size * 2) {
                bytes.forEach { append(digits[(it.toInt() and 255) ushr 4]); append(digits[it.toInt() and 15]) }
            }
        }
    }
}
