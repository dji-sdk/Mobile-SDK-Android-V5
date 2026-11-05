package dji.open.pilot.psdk.protocol

import dji.open.pilot.psdk.DiagnosticEvent
import dji.open.pilot.psdk.DiagnosticKind
import dji.open.pilot.psdk.PayloadEncoding
import dji.open.pilot.psdk.PsdkDiagnostics

/** A complete message, either unfragmented or rebuilt from all of its fragments. */
internal data class AssembledMessage(
    val type: MessageType,
    val encoding: PayloadEncoding,
    val messageId: Long,
    val cmd: Int,
    val status: Int?,
    val payload: ByteArray,
)

/**
 * Reassembles fragmented messages keyed by `(generation, type, messageId)`.
 *
 * A new assembly is created on the first fragment. Metadata conflicts (cmd/type/count/status)
 * discard the assembly; a duplicate index with identical bytes is ignored, with different bytes
 * it is a conflict. Assemblies are bounded by [maxAssemblies] and expire after
 * [timeoutNanos]. [generation] isolates assemblies from different READY periods, so a
 * reconnect can never complete a message started before the disconnect.
 *
 * All methods are synchronized because receive and purge run on different coroutines.
 */
internal class FragmentAssembler(
    private val maxMessageBytes: Int,
    private val diagnostics: PsdkDiagnostics,
    private val clockNanos: () -> Long = System::nanoTime,
    private val timeoutNanos: Long = 5_000_000_000L,
    private val maxAssemblies: Int = 16,
) {
    private data class Key(val generation: Long, val type: MessageType, val messageId: Long)

    private class Partial(
        val generation: Long,
        val type: MessageType,
        val encoding: PayloadEncoding,
        val messageId: Long,
        val cmd: Int,
        val status: Int?,
        val count: Int,
        val startedAt: Long,
        val parts: Array<ByteArray?>,
    ) {
        var bytes = 0
    }

    private val assemblies = LinkedHashMap<Key, Partial>()

    /** Drops all partial assemblies; called on disconnect and close. */
    @Synchronized
    fun clear() {
        assemblies.clear()
    }

    /**
     * Adds one fragment.
     *
     * @return the assembled message once every index is present, otherwise `null` (including
     *   for ignored duplicates).
     * @throws ProtocolViolation on missing fragment metadata, metadata conflicts, differing
     *   duplicates or exceeding [maxMessageBytes]; the offending assembly is discarded.
     */
    @Synchronized
    fun accept(header: FrameHeader, payload: ByteArray, generation: Long): AssembledMessage? {
        purgeExpired()
        val key = Key(generation, header.type, header.messageId)
        val existing = assemblies[key]
        if (existing == null) {
            if (assemblies.size >= maxAssemblies) {
                val oldest = assemblies.entries.firstOrNull()
                if (oldest != null) {
                    assemblies.remove(oldest.key)
                    diagnostics.report(
                        DiagnosticEvent(
                            DiagnosticKind.FRAGMENT_EXPIRED,
                            "assembly limit reached, dropped oldest",
                            cmd = oldest.value.cmd,
                            messageId = oldest.value.messageId,
                        )
                    )
                }
            }
            val count = header.fragmentCount ?: throw ProtocolViolation("missing fragment count")
            val index = header.fragmentIndex ?: throw ProtocolViolation("missing fragment index")
            if (payload.isEmpty()) throw ProtocolViolation("empty fragment")
            if (payload.size > maxMessageBytes) throw ProtocolViolation("fragment exceeds maxMessageBytes")
            val partial = Partial(
                generation = generation,
                type = header.type,
                encoding = header.encoding,
                messageId = header.messageId,
                cmd = header.cmd,
                status = header.status,
                count = count,
                startedAt = clockNanos(),
                parts = arrayOfNulls(count),
            )
            partial.parts[index] = payload
            partial.bytes = payload.size
            assemblies[key] = partial
            return null
        }

        if (existing.cmd != header.cmd ||
            existing.encoding != header.encoding ||
            existing.type != header.type ||
            existing.count != header.fragmentCount ||
            existing.status != header.status
        ) {
            assemblies.remove(key)
            diagnostics.report(
                DiagnosticEvent(
                    DiagnosticKind.FRAGMENT_CONFLICT,
                    "fragment metadata conflict",
                    cmd = header.cmd,
                    messageId = header.messageId,
                )
            )
            throw ProtocolViolation("fragment metadata conflict")
        }
        val index = header.fragmentIndex ?: throw ProtocolViolation("missing fragment index")
        val previous = existing.parts[index]
        if (previous != null) {
            if (!previous.contentEquals(payload)) {
                assemblies.remove(key)
                diagnostics.report(
                    DiagnosticEvent(
                        DiagnosticKind.FRAGMENT_CONFLICT,
                        "duplicate fragment differs",
                        cmd = header.cmd,
                        messageId = header.messageId,
                    )
                )
                throw ProtocolViolation("duplicate fragment differs")
            }
            return null
        }
        if (existing.bytes + payload.size > maxMessageBytes) {
            assemblies.remove(key)
            throw ProtocolViolation("message exceeds maxMessageBytes=$maxMessageBytes")
        }
        existing.parts[index] = payload
        existing.bytes += payload.size
        if (existing.parts.any { it == null }) return null

        assemblies.remove(key)
        val total = ByteArray(existing.bytes)
        var offset = 0
        for (part in existing.parts) {
            val bytes = part ?: throw ProtocolViolation("missing fragment")
            bytes.copyInto(total, offset)
            offset += bytes.size
        }
        return AssembledMessage(
            type = existing.type,
            encoding = existing.encoding,
            messageId = existing.messageId,
            cmd = existing.cmd,
            status = existing.status,
            payload = total,
        )
    }

    /** Discards assemblies older than [timeoutNanos]; also called before each [accept]. */
    @Synchronized
    fun purgeExpired() {
        val now = clockNanos()
        val expired = assemblies.filterValues { now - it.startedAt > timeoutNanos }
        expired.forEach { (key, value) ->
            assemblies.remove(key)
            diagnostics.report(
                DiagnosticEvent(
                    DiagnosticKind.FRAGMENT_EXPIRED,
                    "fragment assembly expired",
                    cmd = value.cmd,
                    messageId = value.messageId,
                )
            )
        }
    }

    @Synchronized
    internal fun activeCount(): Int = assemblies.size
}
