package dji.open.pilot.psdk.protocol

import dji.open.pilot.psdk.PayloadEncoding

/** Message type carried in the control byte; `0` is reserved and therefore invalid. */
internal enum class MessageType(val wireValue: Int) {
    /** Plugin -> payload request. */
    REQ(1),

    /** Payload -> plugin response, the only type carrying a status field. */
    RSP(2),

    /** Payload -> plugin unsolicited event. */
    EVT(3),
    ;

    companion object {
        fun fromWire(value: Int): MessageType? = entries.firstOrNull { it.wireValue == value }
    }
}

/** Parsed header of one wire frame. [payloadOffset] points at the first payload byte. */
internal data class FrameHeader(
    val type: MessageType,
    val encoding: PayloadEncoding,
    val messageId: Long,
    val cmd: Int,
    val fragmented: Boolean,
    val fragmentIndex: Int?,
    val fragmentCount: Int?,
    val status: Int?,
    val payloadOffset: Int,
    val payloadLength: Int,
)

/** Malformed frame or message; callers translate it into a protocol failure diagnosis. */
internal class ProtocolViolation(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)

/**
 * Encodes and decodes the v1 wire format.
 *
 * ```
 * +--------+----------------+--------+------------------+------------------+---------+
 * | control| messageId (u32)| cmd u16| fragment ext (2B)| status ext (2B)  | payload |
 * |  1 B   |     little     | little | index, count     | RSP only         |  ...    |
 * +--------+----------------+--------+------------------+------------------+---------+
 * control: version[7..6] | type[5..4] | reserved[3..2] | encoding[1] | fragmented[0]
 * ```
 *
 * `messageId` is non-zero and scoped to a port; `cmd` is 1..65535. Integer fields are
 * little-endian. A fragmented message repeats the same `messageId`/`cmd` in every frame and
 * is reassembled by [FragmentAssembler]; both extension fields are optional per message type.
 */
internal object FrameCodec {
    /** control + messageId + cmd. */
    const val BASE_HEADER_BYTES = 7

    /** fragment index + fragment count. */
    const val FRAGMENT_EXTENSION_BYTES = 2

    /** RSP status (u16). */
    const val STATUS_EXTENSION_BYTES = 2
    const val VERSION = 1

    private const val VERSION_SHIFT = 6
    private const val TYPE_SHIFT = 4
    private const val ENCODING_BIT = 0x02
    private const val FRAGMENT_BIT = 0x01
    private const val RESERVED_MASK = 0x0C

    /** Header size for the given message shape; does not include the payload. */
    fun encodedLength(encoding: PayloadEncoding, type: MessageType, fragmented: Boolean): Int {
        var length = BASE_HEADER_BYTES
        if (fragmented) length += FRAGMENT_EXTENSION_BYTES
        if (type == MessageType.RSP) length += STATUS_EXTENSION_BYTES
        return length
    }

    /** Maximum payload bytes per fragment for a frame limited to [maxPacketBytes]. */
    fun fragmentCapacity(maxPacketBytes: Int, encoding: PayloadEncoding, type: MessageType): Int =
        maxPacketBytes - encodedLength(encoding, type, fragmented = true)

    /**
     * Encodes exactly one frame. Fragment fields must be supplied together and imply the
     * fragment control bit; RSP frames must carry [status] and other types must not.
     *
     * @throws IllegalArgumentException if ids, fragment metadata or the total frame size are
     *   invalid.
     */
    fun encode(
        type: MessageType,
        encoding: PayloadEncoding,
        messageId: Long,
        cmd: Int,
        payload: ByteArray,
        maxPacketBytes: Int,
        status: Int? = null,
        fragmentIndex: Int? = null,
        fragmentCount: Int? = null,
    ): ByteArray {
        require(messageId in 1..0xFFFF_FFFFL) { "messageId must be 1..4294967295" }
        require(cmd in 1..0xFFFF) { "cmd must be 1..65535" }
        require(type != MessageType.RSP || status != null) { "RSP requires status" }
        require(type == MessageType.RSP || status == null) { "only RSP carries status" }
        val fragmented = fragmentIndex != null || fragmentCount != null
        require(!fragmented || (fragmentIndex != null && fragmentCount != null)) {
            "fragment index and count must be supplied together"
        }
        if (fragmented) {
            require(fragmentCount!! in 2..255) { "fragmentCount must be 2..255" }
            require(fragmentIndex!! in 0 until fragmentCount!!) { "fragmentIndex out of range" }
            require(payload.isNotEmpty()) { "fragment payload must not be empty" }
        }

        val headerLength = encodedLength(encoding, type, fragmented)
        val total = headerLength + payload.size
        require(total <= maxPacketBytes) { "frame length $total exceeds maxPacketBytes=$maxPacketBytes" }
        require(total >= headerLength) { "invalid frame length" }

        var control = VERSION shl VERSION_SHIFT
        control = control or (type.wireValue shl TYPE_SHIFT)
        if (encoding == PayloadEncoding.JSON) control = control or ENCODING_BIT
        if (fragmented) control = control or FRAGMENT_BIT

        val out = ByteArray(total)
        out[0] = control.toByte()
        writeU32(out, 1, messageId)
        writeU16(out, 5, cmd)
        var offset = BASE_HEADER_BYTES
        if (fragmented) {
            out[offset++] = fragmentIndex!!.toByte()
            out[offset++] = fragmentCount!!.toByte()
        }
        if (type == MessageType.RSP) {
            writeU16(out, offset, status!!)
            offset += STATUS_EXTENSION_BYTES
        }
        payload.copyInto(out, offset)
        return out
    }

    /**
     * Parses and validates the header of one frame.
     *
     * @param expectedEncoding the bridge's configured encoding; frames using the other flag
     *   are rejected so mixed codecs cannot be decoded silently.
     * @throws ProtocolViolation for short/oversized frames, unsupported version/type/reserved
     *   bits, zero ids, mismatched encoding or malformed extensions.
     */
    fun decodeHeader(bytes: ByteArray, maxPacketBytes: Int, expectedEncoding: PayloadEncoding): FrameHeader {
        if (bytes.size < BASE_HEADER_BYTES) throw ProtocolViolation("frame shorter than $BASE_HEADER_BYTES bytes")
        if (bytes.size > maxPacketBytes) throw ProtocolViolation("frame ${bytes.size} exceeds maxPacketBytes=$maxPacketBytes")
        val control = bytes[0].toInt() and 0xFF
        val version = control ushr VERSION_SHIFT
        if (version != VERSION) throw ProtocolViolation("unsupported version $version")
        if (control and RESERVED_MASK != 0) throw ProtocolViolation("reserved control bits set")
        val type = MessageType.fromWire((control ushr TYPE_SHIFT) and 0x03)
            ?: throw ProtocolViolation("unsupported message type")
        val encoding = if (control and ENCODING_BIT != 0) PayloadEncoding.JSON else PayloadEncoding.BINARY
        if (encoding != expectedEncoding) throw ProtocolViolation("encoding mismatch: frame=$encoding expected=$expectedEncoding")
        val fragmented = control and FRAGMENT_BIT != 0
        val messageId = readU32(bytes, 1)
        if (messageId == 0L) throw ProtocolViolation("messageId must be non-zero")
        val cmd = readU16(bytes, 5)
        if (cmd == 0) throw ProtocolViolation("cmd must be 1..65535")

        var offset = BASE_HEADER_BYTES
        var fragmentIndex: Int? = null
        var fragmentCount: Int? = null
        if (fragmented) {
            if (bytes.size < offset + FRAGMENT_EXTENSION_BYTES) throw ProtocolViolation("missing fragment extension")
            fragmentIndex = bytes[offset++].toInt() and 0xFF
            fragmentCount = bytes[offset++].toInt() and 0xFF
            if (fragmentCount !in 2..255) throw ProtocolViolation("fragmentCount must be 2..255")
            if (fragmentIndex >= fragmentCount) throw ProtocolViolation("fragmentIndex out of range")
        }
        var status: Int? = null
        if (type == MessageType.RSP) {
            if (bytes.size < offset + STATUS_EXTENSION_BYTES) throw ProtocolViolation("missing status extension")
            status = readU16(bytes, offset)
            offset += STATUS_EXTENSION_BYTES
        }
        if (fragmented && bytes.size == offset) throw ProtocolViolation("empty fragment payload")
        return FrameHeader(
            type = type,
            encoding = encoding,
            messageId = messageId,
            cmd = cmd,
            fragmented = fragmented,
            fragmentIndex = fragmentIndex,
            fragmentCount = fragmentCount,
            status = status,
            payloadOffset = offset,
            payloadLength = bytes.size - offset,
        )
    }

    /**
     * Encodes a message into one frame, or into ordered fragments when the payload does not
     * fit [maxPacketBytes].
     *
     * All fragments share [messageId], [cmd] and (for RSP) [status]; each carries its index and
     * the total count. The number of fragments must be 2..255.
     *
     * @throws ProtocolViolation if the payload cannot be fragmented within protocol limits.
     */
    fun split(
        type: MessageType,
        encoding: PayloadEncoding,
        messageId: Long,
        cmd: Int,
        payload: ByteArray,
        maxPacketBytes: Int,
        status: Int? = null,
    ): List<ByteArray> {
        val nonFragmentedLength = encodedLength(encoding, type, fragmented = false) + payload.size
        if (nonFragmentedLength <= maxPacketBytes) {
            return listOf(encode(type, encoding, messageId, cmd, payload, maxPacketBytes, status))
        }
        val capacity = fragmentCapacity(maxPacketBytes, encoding, type)
        if (capacity <= 0) throw ProtocolViolation("maxPacketBytes leaves no fragment payload")
        val count = (payload.size + capacity - 1) / capacity
        if (count !in 2..255) throw ProtocolViolation("fragment count $count outside 2..255")
        return (0 until count).map { index ->
            val from = index * capacity
            val to = minOf(payload.size, from + capacity)
            val part = payload.copyOfRange(from, to)
            encode(
                type = type,
                encoding = encoding,
                messageId = messageId,
                cmd = cmd,
                payload = part,
                maxPacketBytes = maxPacketBytes,
                status = status,
                fragmentIndex = index,
                fragmentCount = count,
            )
        }
    }

    private fun writeU16(target: ByteArray, offset: Int, value: Int) {
        target[offset] = (value and 0xFF).toByte()
        target[offset + 1] = ((value ushr 8) and 0xFF).toByte()
    }

    private fun writeU32(target: ByteArray, offset: Int, value: Long) {
        target[offset] = (value and 0xFF).toByte()
        target[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        target[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        target[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }

    private fun readU16(source: ByteArray, offset: Int): Int =
        (source[offset].toInt() and 0xFF) or ((source[offset + 1].toInt() and 0xFF) shl 8)

    private fun readU32(source: ByteArray, offset: Int): Long =
        (source[offset].toLong() and 0xFF) or
            ((source[offset + 1].toLong() and 0xFF) shl 8) or
            ((source[offset + 2].toLong() and 0xFF) shl 16) or
            ((source[offset + 3].toLong() and 0xFF) shl 24)
}
