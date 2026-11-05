package dji.open.pilot.psdk

import dji.v5.common.error.IDJIError

/** Wire payload encoding selected once when a bridge is created. */
enum class PayloadEncoding {
    /** UTF-8 JSON object payload (built-in, no schema registration). */
    JSON,

    /** Binary payload; reserved for custom [PayloadCodec] implementations. */
    BINARY,
}

/**
 * Logical request passed to [PsdkBridge.request].
 *
 * @property cmd command id agreed with the payload; 1..65535.
 * @property params flat string-keyed business parameters, encoded by the configured codec.
 */
data class PsdkRequest(
    val cmd: Int,
    val params: Map<String, Any?> = emptyMap(),
)

/**
 * Decoded response of a request.
 *
 * @property code payload-defined status; `0` means success.
 * @property params decoded response parameters.
 */
data class PsdkResponse(
    val code: Int,
    val params: Map<String, Any?> = emptyMap(),
) {
    /** True when the payload reported success (`code == 0`). */
    val isSuccess: Boolean get() = code == 0
}

/**
 * Unsolicited event pushed by the payload.
 *
 * @property cmd command id agreed with the payload.
 * @property params decoded event parameters.
 */
data class PsdkEvent(
    val cmd: Int,
    val params: Map<String, Any?>,
)

/** Why a request was rejected locally before anything was sent to the payload. */
enum class RequestErrorCode {
    /** [PsdkRequest.cmd] is outside 1..65535. */
    INVALID_CMD,

    /** The payload could not be encoded with the configured codec. */
    INVALID_PARAMS,

    /** Encoded payload or pending/send queue exceeded its capacity. */
    CAPACITY_EXCEEDED,

    /** All 32-bit message ids for this port have been consumed in this process. */
    ID_EXHAUSTED,

    /** Reserved; encoding failures currently surface as [INVALID_PARAMS]. */
    ENCODE_ERROR,
}

/** Why the protocol layer considered a received or sent message invalid. */
enum class ProtocolErrorCode {
    /** The payload bytes could not be decoded. */
    DECODE_ERROR,

    /** Reserved for codecs that validate the frame encoding against their own. */
    ENCODING_MISMATCH,

    /** The underlying SDK call threw or rejected the frame. */
    SDK_CALL_EXCEPTION,
}

/** Reason the bridge is not [BridgeStage.READY]. */
enum class NotReadyReason {
    /** The host application has not registered the SDK yet. */
    SDK_NOT_REGISTERED,

    /** Pilot is not connected. */
    PILOT_DISCONNECTED,

    /** The aircraft is not connected. */
    AIRCRAFT_DISCONNECTED,

    /** The payload manager reports the payload as disconnected. */
    PAYLOAD_DISCONNECTED,

    /** The payload is connected but its PSDK feature is not opened. */
    FEATURE_UNAVAILABLE,
}

/**
 * Terminal result of a request.
 *
 * Every request completes exactly once unless [RequestHandle.cancel] stops the local wait.
 * Only [Response] means the payload answered; all other branches are local or transport failures.
 */
sealed interface RequestResult {
    /** The payload answered; inspect [PsdkResponse.code] for the business status. */
    data class Response(val message: PsdkResponse) : RequestResult

    /** The SDK reported an error while submitting a frame; the original error is preserved. */
    data class SdkFailure(val error: IDJIError) : RequestResult

    /** The request was rejected locally and never sent. */
    data class InvalidRequest(val code: RequestErrorCode, val detail: String? = null) : RequestResult

    /** The bridge was not ready when the request was submitted. */
    data class NotReady(val reason: NotReadyReason) : RequestResult

    /** Encoding/decoding failed, or the SDK call threw. */
    data class ProtocolFailure(val code: ProtocolErrorCode, val detail: String? = null) : RequestResult

    /** No response arrived before `requestTimeoutMillis`. */
    data object Timeout : RequestResult

    /** The link left READY while the request was pending. */
    data object Disconnected : RequestResult

    /** The bridge was closed while the request was pending. */
    data object ChannelClosed : RequestResult

    /** [RequestHandle.cancel] was called; no cancellation is sent to the payload. */
    data object Cancelled : RequestResult
}

/** Lifecycle stage of a bridge, derived from SDK/Pilot/aircraft/payload connectivity. */
enum class BridgeStage {
    /** SDK not registered. */
    STOPPED,

    /** SDK registered but at least one link is unavailable. */
    DISCONNECTED,

    /** All links are up; requests may be sent and events received. */
    READY,

    /** [PsdkBridge.close] has run; the instance is terminal. */
    CLOSED,
}

/**
 * Snapshot of a bridge lifecycle state.
 *
 * @property stage current stage.
 * @property reason why the bridge is not ready; `null` when [BridgeStage.READY].
 */
data class BridgeState(
    val stage: BridgeStage,
    val reason: NotReadyReason? = null,
)

/** Handle for a callback-style request, used to stop waiting locally. */
fun interface RequestHandle {
    /** Cancels the local wait; idempotent. Does not notify the payload. */
    fun cancel()
}

/**
 * Command-scoped event handler registered through [PsdkBridge.registerEventHandler].
 *
 * The bridge matches incoming events by [cmd] and calls [onEvent] on the configured
 * callback dispatcher, preserving arrival order for this handler. Register one handler
 * per command per business concern instead of collecting a shared event stream.
 */
interface PsdkEventHandler {
    /** Command this handler subscribes to ([PsdkEvent.cmd]); read once at registration. */
    val cmd: Int

    /** Called for each event whose [PsdkEvent.cmd] equals [cmd]. Keep it short. */
    fun onEvent(event: PsdkEvent)
}
