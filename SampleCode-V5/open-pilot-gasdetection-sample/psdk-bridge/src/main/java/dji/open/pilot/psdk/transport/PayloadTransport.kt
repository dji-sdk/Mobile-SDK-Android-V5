package dji.open.pilot.psdk.transport

import dji.v5.common.error.IDJIError
import kotlinx.coroutines.flow.StateFlow

/** Payload link facts reported by the transport. */
internal data class PayloadLink(
    val connected: Boolean = false,
    val featureOpened: Boolean = false,
)

/** Result of submitting one frame to the transport. */
internal sealed interface SendOutcome {
    /** The SDK accepted the frame; it does not mean the payload executed it. */
    data object Submitted : SendOutcome

    /** The SDK reported an error. */
    data class Failed(val error: IDJIError) : SendOutcome

    /** The transport refused the frame locally (closed, empty or oversized). */
    data class Rejected(val reason: String) : SendOutcome
}

/**
 * Byte-level link to one payload port, abstracting the SDK payload manager so the protocol
 * engine can be tested without Android/SDK listeners.
 */
internal interface PayloadTransport {
    /** Maximum single-frame size negotiated for this link. */
    val maxPacketBytes: Int

    /** Connection/feature state used to gate requests. */
    val link: StateFlow<PayloadLink>

    /** Installs the receive callback. Called from the SDK thread; must stay non-blocking. */
    fun setReceiveHandler(handler: (ByteArray) -> Unit)

    /** Installs a callback for rejected inbound data with a human-readable reason. */
    fun setErrorHandler(handler: (String) -> Unit)

    /** Submits one frame, suspending until the SDK accepts or fails it. */
    suspend fun send(bytes: ByteArray): SendOutcome

    /** Removes this transport's SDK listeners; idempotent. */
    fun close()
}
