package dji.open.pilot.psdk.client

import dji.v5.manager.aircraft.payload.PayloadIndexType

/**
 * Process-wide, per-port message-id sequence (1..2^32-1).
 *
 * Ids are never reused within a process, including across reconnects and bridge recreation,
 * so a delayed frame from an old link cannot match a new request. When the range is exhausted
 * [next] returns `null` for that port; only a process restart resets the sequence.
 */
internal object MessageIdAllocator {
    private const val MAX_ID = 0xFFFF_FFFFL

    private class Cursor {
        var next = 1L
        var exhausted = false
    }

    private val cursors = mutableMapOf<PayloadIndexType, Cursor>()

    /** Returns the next id for [port], or `null` if the 32-bit range was exhausted. */
    @Synchronized
    fun next(port: PayloadIndexType): Long? {
        val cursor = cursors.getOrPut(port) { Cursor() }
        if (cursor.exhausted) return null
        val value = cursor.next
        if (value > MAX_ID) {
            cursor.exhausted = true
            return null
        }
        cursor.next = value + 1
        return value
    }

    @Synchronized
    internal fun resetForTest(port: PayloadIndexType) {
        cursors.remove(port)
    }

    @Synchronized
    internal fun setForTest(port: PayloadIndexType, next: Long) {
        cursors[port] = Cursor().apply {
            this.next = next
            exhausted = next > MAX_ID
        }
    }
}
