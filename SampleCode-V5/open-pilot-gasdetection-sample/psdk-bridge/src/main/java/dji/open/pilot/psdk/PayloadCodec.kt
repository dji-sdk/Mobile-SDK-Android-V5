package dji.open.pilot.psdk

import dji.open.pilot.psdk.protocol.JsonMapCodec

/** Direction of a payload so codecs can select per-command schemas when needed. */
enum class MessageKind {
    /** Outbound request parameters. */
    REQUEST,

    /** Inbound response parameters. */
    RESPONSE,

    /** Inbound unsolicited event parameters. */
    EVENT,
}

/**
 * Payload encoding extension point.
 *
 * JSON is the only built-in codec and has no schema/registration overhead. Future
 * CBOR/MessagePack/BINARY implementations only need to implement this interface,
 * without changing bridge/request/fragment/event logic.
 */
interface PayloadCodec {
    /** Wire flag stored in the frame control byte (JSON; BINARY is reserved for future codecs). */
    val wireEncoding: PayloadEncoding

    /**
     * Encodes business parameters into the message payload.
     *
     * @param kind request, response or event; codecs may select different schemas per direction.
     * @param cmd command id, for per-command schemas.
     * @throws Exception to reject the request with [RequestErrorCode.INVALID_PARAMS].
     */
    fun encode(kind: MessageKind, cmd: Int, params: Map<String, Any?>): ByteArray

    /**
     * Decodes a message payload into business parameters.
     *
     * @throws Exception to fail the request or drop the event with [ProtocolErrorCode.DECODE_ERROR].
     */
    fun decode(kind: MessageKind, cmd: Int, payload: ByteArray): Map<String, Any?>
}

/**
 * Default codec: UTF-8 JSON object with string keys; no schemas are required.
 *
 * Supports nested maps/lists (depth <= 16) and rejects non-string keys, non-finite numbers,
 * ByteArray and ULong because JSON cannot represent them portably.
 */
data class JsonPayloadCodec(private val maxMessageBytes: Int) : PayloadCodec {
    override val wireEncoding: PayloadEncoding = PayloadEncoding.JSON
    private val delegate = JsonMapCodec(maxMessageBytes)

    override fun encode(kind: MessageKind, cmd: Int, params: Map<String, Any?>): ByteArray =
        delegate.encode(params)

    override fun decode(kind: MessageKind, cmd: Int, payload: ByteArray): Map<String, Any?> =
        delegate.decode(payload)
}
