package dji.open.pilot.psdk.transport

import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.payload.listener.PayloadBasicInfoListener
import dji.v5.manager.aircraft.payload.listener.PayloadDataListener
import dji.v5.manager.interfaces.IPayloadManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * SDK-backed [PayloadTransport].
 *
 * Owns exactly one basic-info listener and one data listener on the given manager.
 * Never calls init/destroy/clearAll, so other modules keep their listeners. Inbound data is
 * copied and size-checked before being handed to the receive handler; link facts come from
 * [PayloadBasicInfoListener].
 */
internal class SdkPayloadTransport(
    private val manager: IPayloadManager,
    override val maxPacketBytes: Int,
) : PayloadTransport {
    private val lock = Any()
    private val _link = MutableStateFlow(PayloadLink())
    override val link: StateFlow<PayloadLink> = _link.asStateFlow()

    private var receiveHandler: ((ByteArray) -> Unit)? = null
    private var errorHandler: ((String) -> Unit)? = null
    private var closed = false

    private val dataListener = PayloadDataListener { data ->
        val handler = synchronized(lock) { if (closed) null else receiveHandler }
        if (handler != null) {
            if (data.isEmpty() || data.size > maxPacketBytes) {
                handlerError("RX rejected: len=${data.size}, max=$maxPacketBytes")
            } else {
                handler(data.copyOf())
            }
        }
    }

    private val basicListener = PayloadBasicInfoListener { info ->
        val connected = info.isConnected
        val featureOpened = info.isFeatureOpened
        synchronized(lock) {
            if (!closed) _link.value = PayloadLink(connected = connected, featureOpened = featureOpened)
        }
    }

    init {
        require(maxPacketBytes in 16..255) { "maxPacketBytes must be 16..255" }
        try {
            manager.addPayloadBasicInfoListener(basicListener)
            manager.addPayloadDataListener(dataListener)
        } catch (error: Throwable) {
            try {
                close()
            } catch (cleanup: Throwable) {
                error.addSuppressed(cleanup)
            }
            throw error
        }
    }

    override fun setReceiveHandler(handler: (ByteArray) -> Unit) {
        synchronized(lock) { receiveHandler = handler }
    }

    override fun setErrorHandler(handler: (String) -> Unit) {
        synchronized(lock) { errorHandler = handler }
    }

    /**
     * Validates the frame locally, then bridges the SDK completion callback to a coroutine.
     * The callback resumes only while the continuation is active, so cancellation/timeout
     * never leaks a late resume.
     */
    override suspend fun send(bytes: ByteArray): SendOutcome {
        synchronized(lock) {
            when {
                closed -> return SendOutcome.Rejected("channel closed")
                bytes.isEmpty() -> return SendOutcome.Rejected("empty payload")
                bytes.size > maxPacketBytes -> return SendOutcome.Rejected("frame exceeds maxPacketBytes")
            }
        }
        return suspendCancellableCoroutine { continuation ->
            manager.sendDataToPayload(
                bytes.copyOf(),
                object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        if (continuation.isActive) continuation.resume(SendOutcome.Submitted)
                    }

                    override fun onFailure(error: IDJIError) {
                        if (continuation.isActive) continuation.resume(SendOutcome.Failed(error))
                    }
                }
            )
        }
    }

    override fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
            receiveHandler = null
            errorHandler = null
        }
        try {
            manager.removePayloadBasicInfoListener(basicListener)
        } finally {
            manager.removePayloadDataListener(dataListener)
        }
    }

    private fun handlerError(reason: String) {
        val handler = synchronized(lock) { if (closed) null else errorHandler }
        handler?.invoke(reason)
    }
}
