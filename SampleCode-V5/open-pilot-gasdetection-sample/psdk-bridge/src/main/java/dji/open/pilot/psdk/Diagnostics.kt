package dji.open.pilot.psdk

import android.util.Log

/** Internal diagnostic categories; never part of the public API. */
internal enum class DiagnosticKind {
    /** A frame was accepted by the SDK for submission. */
    TX_SUBMITTED,

    /** The SDK failed to submit a frame. */
    TX_FAILED,

    /** Reserved for successful inbound packet accounting. */
    RX_ACCEPTED,

    /** Inbound data or a callback failed and was dropped. */
    RX_REJECTED,

    /** A request reached a terminal result (detail is the result type). */
    REQUEST_COMPLETED,

    /** Reserved; timeouts currently complete as REQUEST_COMPLETED(Timeout). */
    REQUEST_TIMEOUT,

    /** Reserved; local cancellation currently uses REQUEST_COMPLETED(Cancelled). */
    REQUEST_CANCELLED,

    /** The bridge left READY and failed its pending requests. */
    DISCONNECTED,

    /** A partial fragment assembly exceeded its limit or TTL. */
    FRAGMENT_EXPIRED,

    /** Fragments of one message disagreed on metadata or bytes. */
    FRAGMENT_CONFLICT,

    /** Reserved; the current codec rejects mismatches as protocol violations. */
    ENCODING_MISMATCH,

    /** A payload could not be decoded by the codec. */
    SCHEMA_DECODE_FAILED,

    /** A listener queue overflowed and that listener was stopped. */
    LISTENER_OVERFLOW,

    /** The inbound/send queue overflowed. */
    QUEUE_OVERFLOW,

    /** A response/event arrived after its request or dedup window. */
    LATE_MESSAGE,
}

/**
 * One structured diagnostic record. [detail] is free-form and must not contain secrets;
 * [cmd], [messageId] and [bytes] are optional context.
 */
internal data class DiagnosticEvent(
    val kind: DiagnosticKind,
    val detail: String,
    val cmd: Int? = null,
    val messageId: Long? = null,
    val bytes: Int? = null,
)

internal fun interface PsdkDiagnostics {
    fun report(event: DiagnosticEvent)
}

internal object LogcatDiagnostics : PsdkDiagnostics {
    private const val TAG = "PsdkBridge"

    override fun report(event: DiagnosticEvent) {
        val line = buildString {
            append(event.kind.name)
            event.cmd?.let { append(" cmd=").append(it) }
            event.messageId?.let { append(" id=").append(it) }
            event.bytes?.let { append(" bytes=").append(it) }
            if (event.detail.isNotEmpty()) append(' ').append(event.detail)
        }
        when (event.kind) {
            DiagnosticKind.TX_FAILED,
            DiagnosticKind.RX_REJECTED,
            DiagnosticKind.FRAGMENT_CONFLICT,
            DiagnosticKind.ENCODING_MISMATCH,
            DiagnosticKind.SCHEMA_DECODE_FAILED,
            DiagnosticKind.LISTENER_OVERFLOW,
            DiagnosticKind.QUEUE_OVERFLOW,
            -> Log.w(TAG, line)
            else -> Log.d(TAG, line)
        }
    }
}
