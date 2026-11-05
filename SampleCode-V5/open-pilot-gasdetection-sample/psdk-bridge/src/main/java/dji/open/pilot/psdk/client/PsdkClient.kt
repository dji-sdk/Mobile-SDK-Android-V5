package dji.open.pilot.psdk.client

import dji.open.pilot.psdk.BridgeStage
import dji.open.pilot.psdk.MessageKind
import dji.open.pilot.psdk.PayloadCodec
import dji.open.pilot.psdk.BridgeState
import dji.open.pilot.psdk.DiagnosticEvent
import dji.open.pilot.psdk.DiagnosticKind
import dji.open.pilot.psdk.NotReadyReason
import dji.open.pilot.psdk.ProtocolErrorCode
import dji.open.pilot.psdk.PsdkConnections
import dji.open.pilot.psdk.PsdkDiagnostics
import dji.open.pilot.psdk.PsdkEvent
import dji.open.pilot.psdk.PsdkEventHandler
import dji.open.pilot.psdk.PsdkOptions
import dji.open.pilot.psdk.PsdkRequest
import dji.open.pilot.psdk.PsdkResponse
import dji.open.pilot.psdk.RequestErrorCode
import dji.open.pilot.psdk.RequestHandle
import dji.open.pilot.psdk.RequestResult
import dji.open.pilot.psdk.protocol.AssembledMessage
import dji.open.pilot.psdk.protocol.FrameCodec
import dji.open.pilot.psdk.protocol.FragmentAssembler
import dji.open.pilot.psdk.protocol.MessageType
import dji.open.pilot.psdk.protocol.ProtocolViolation
import dji.open.pilot.psdk.protocol.toPsdkPayload
import dji.open.pilot.psdk.transport.PayloadTransport
import dji.open.pilot.psdk.transport.SendOutcome
import dji.v5.manager.aircraft.payload.PayloadIndexType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Protocol engine behind [dji.open.pilot.psdk.PsdkBridge]: request correlation, timeouts,
 * fragmentation, receive dispatch and connection gating.
 *
 * Threading model:
 * - a worker scope runs state observation, receive, send and fragment-purge jobs;
 * - a delivery scope runs request callbacks, listeners and handlers on
 *   [PsdkOptions.callbackDispatcher];
 * - `pendingLock` guards `pendingById` and the READY/generation invariant used by both
 *   request submission and state changes.
 *
 * Frames are accepted only while READY. Leaving READY increments [generation], clears partial
 * assemblies/dedup state and fails pending requests with [RequestResult.Disconnected], so
 * frames from a previous connection can never satisfy new requests.
 */
internal class PsdkClient(
    val port: PayloadIndexType,
    val maxPacketBytes: Int,
    private val codec: PayloadCodec,
    private val connections: PsdkConnections,
    val options: PsdkOptions,
    private val transport: PayloadTransport,
    private val diagnostics: PsdkDiagnostics,
) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + options.workerDispatcher)
    private val deliveryScope = CoroutineScope(SupervisorJob() + options.callbackDispatcher)

    private val closed = AtomicBoolean(false)
    private val _state = MutableStateFlow(computeState())
    val state: StateFlow<BridgeState> = _state.asStateFlow()

    private val pendingLock = Any()
    private val pendingById = mutableMapOf<Long, Pending>()

    /** Outbound frames waiting for the single send job; keeps SDK writes serialized. */
    private val sendChannel = Channel<SendTask>(SEND_QUEUE_CAPACITY)

    private val assembler = FragmentAssembler(options.maxMessageBytes, diagnostics)
    /** messageId dedup window so a retried/duplicated EVT is not delivered twice. */
    private val recentEvents = RecentEventCache(maxEntries = 256, ttlNanos = 5_000_000_000L)

    private val listenerLock = Any()
    private val listeners = linkedSetOf<EventRegistration>()

    private val handlerLock = Any()
    private val handlers = linkedSetOf<HandlerRegistration>()

    @Volatile private var generation = 0L

    private class ReceivedPacket(val bytes: ByteArray, val generation: Long)
    private val inbox = Channel<ReceivedPacket>(INBOX_CAPACITY)
    private val receiveJob: Job
    private val stateJob: Job
    private val sendJob: Job
    private val purgeJob: Job

    init {
        try {
            transport.setReceiveHandler { bytes -> onBytes(bytes) }
            transport.setErrorHandler { reason ->
                diagnostics.report(DiagnosticEvent(DiagnosticKind.RX_REJECTED, reason))
            }
            stateJob = scope.launch {
                combine(
                    connections.sdkRegistered,
                    connections.pilotConnected,
                    connections.aircraftConnected,
                    transport.link,
                ) { sdk, pilot, aircraft, link ->
                    computeState(sdk, pilot, aircraft, link.connected, link.featureOpened)
                }.collect { next -> applyState(next) }
            }
            receiveJob = scope.launch {
                for (packet in inbox) {
                    try {
                        synchronized(pendingLock) {
                            if (!closed.get() && _state.value.stage == BridgeStage.READY && packet.generation == generation) {
                                handleFrame(packet.bytes)
                            }
                        }
                    } catch (error: ProtocolViolation) {
                        diagnostics.report(DiagnosticEvent(DiagnosticKind.RX_REJECTED, error.message ?: "protocol violation"))
                    } catch (error: Exception) {
                        diagnostics.report(DiagnosticEvent(DiagnosticKind.RX_REJECTED, error.message ?: "receive failure"))
                    }
                }
            }
            sendJob = scope.launch {
                for (task in sendChannel) {
                    sendTask(task)
                }
            }
            purgeJob = scope.launch {
                while (isActive) {
                    delay(1000)
                    assembler.purgeExpired()
                }
            }
        } catch (error: Throwable) {
            scope.cancel()
            deliveryScope.cancel()
            sendChannel.cancel()
            inbox.cancel()
            try {
                transport.close()
            } catch (cleanup: Throwable) {
                error.addSuppressed(cleanup)
            }
            throw error
        }
    }

    /** Callback request whose params are reflected from [payload]'s fields. */
    fun request(cmd: Int, payload: Any, callback: (RequestResult) -> Unit): RequestHandle =
        request(cmd, payload.toPsdkPayload(), callback)

    /**
     * Callback request. The returned handle cancels the local wait and delivers
     * [RequestResult.Cancelled]; the payload is not notified.
     */
    fun request(cmd: Int, params: Map<String, Any?>, callback: (RequestResult) -> Unit): RequestHandle {
        val pending = submit(cmd, params, callback)
        return RequestHandle { pending?.let { cancelPending(it, deliverCancelled = true) } }
    }

    /** Suspending request whose params are reflected from [payload]'s fields. */
    suspend fun request(cmd: Int, payload: Any): RequestResult = request(cmd, payload.toPsdkPayload())

    /**
     * Suspending request. Coroutine cancellation removes the pending entry and rethrows;
     * no [RequestResult.Cancelled] is produced in that case.
     */
    suspend fun request(cmd: Int, params: Map<String, Any?>): RequestResult {
        val deferred = CompletableDeferred<RequestResult>()
        val pending = submit(cmd, params) { deferred.complete(it) }
        if (pending == null) return deferred.await()
        return try {
            deferred.await()
        } catch (error: CancellationException) {
            removePending(pending)
            throw error
        }
    }

    fun observe(cmd: Int): Flow<PsdkEvent> = observeInternal(cmd)

    fun observeAll(): Flow<PsdkEvent> = observeInternal(null)

    fun addListener(cmd: Int, listener: (PsdkEvent) -> Unit): Closeable = addListenerInternal(cmd, listener)

    fun addListener(listener: (PsdkEvent) -> Unit): Closeable = addListenerInternal(null, listener)

    fun registerEventHandler(handler: PsdkEventHandler): Closeable {
        val cmd = handler.cmd
        require(cmd in 1..65535) { "PsdkEventHandler.cmd must be 1..65535: ${handler.javaClass.name}" }
        val registration = HandlerRegistration(cmd, handler)
        synchronized(handlerLock) {
            check(!closed.get()) { "bridge is closed" }
            handlers.add(registration)
        }
        try {
            registration.bind(
                addListenerInternal(cmd, listener = { event ->
                    if (registration.active) handler.onEvent(event)
                }, onStopped = { registration.close() }),
            )
        } catch (error: Throwable) {
            registration.close()
            throw error
        }
        return registration
    }

    fun unregisterEventHandler(handler: PsdkEventHandler): Int {
        val matched = synchronized(handlerLock) { handlers.filter { it.handler === handler } }
        matched.forEach { it.close() }
        return matched.size
    }

    private inner class HandlerRegistration(
        val cmd: Int,
        val handler: PsdkEventHandler,
    ) : Closeable {
        @Volatile
        var active = true
            private set

        private var listener: Closeable? = null

        @Synchronized
        fun bind(listener: Closeable) {
            if (active) {
                this.listener = listener
            } else {
                listener.close()
            }
        }

        @Synchronized
        override fun close() {
            if (!active) return
            active = false
            synchronized(handlerLock) { handlers.remove(this) }
            listener?.close()
            listener = null
        }
    }

    /**
     * Validates, encodes, fragments and enqueues one request, registering it for correlation.
     *
     * The deadline starts here, so it covers queueing and per-frame sends as well as the wait
     * for a response. Returns `null` when the request completed immediately (not ready,
     * invalid, capacity exhausted or closed); [callback] is still invoked exactly once.
     * Otherwise the pending entry is registered and its timeout job started atomically with
     * the READY/generation check, then the frames are offered to [sendChannel].
     */
    private fun submit(
        cmd: Int,
        params: Map<String, Any?>,
        callback: (RequestResult) -> Unit,
    ): Pending? {
        if (closed.get()) {
            deliver(callback, RequestResult.ChannelClosed)
            return null
        }
        val requestGeneration = generation
        val current = _state.value
        if (current.stage != BridgeStage.READY) {
            deliver(callback, RequestResult.NotReady(current.reason ?: NotReadyReason.SDK_NOT_REGISTERED))
            return null
        }
        if (cmd !in 1..65535) {
            deliver(callback, RequestResult.InvalidRequest(RequestErrorCode.INVALID_CMD, "cmd must be 1..65535"))
            return null
        }
        val payload = try {
            encodePayload(cmd, MessageType.REQ, params)
        } catch (error: Exception) {
            deliver(callback, RequestResult.InvalidRequest(RequestErrorCode.INVALID_PARAMS, error.message))
            return null
        }
        if (payload.size > options.maxMessageBytes) {
            deliver(callback, RequestResult.InvalidRequest(RequestErrorCode.CAPACITY_EXCEEDED, "payload exceeds maxMessageBytes"))
            return null
        }
        val id = MessageIdAllocator.next(port)
        if (id == null) {
            deliver(callback, RequestResult.InvalidRequest(RequestErrorCode.ID_EXHAUSTED))
            return null
        }
        val encodedFrames = try {
            FrameCodec.split(
                type = MessageType.REQ,
                encoding = codec.wireEncoding,
                messageId = id,
                cmd = cmd,
                payload = payload,
                maxPacketBytes = maxPacketBytes,
            )
        } catch (error: ProtocolViolation) {
            deliver(callback, RequestResult.InvalidRequest(RequestErrorCode.CAPACITY_EXCEEDED, error.message))
            return null
        }
        val pending = Pending(id = id, cmd = cmd, callback = callback, deadlineAt = System.nanoTime() + options.requestTimeoutMillis * 1_000_000L)
        synchronized(pendingLock) {
            if (closed.get()) {
                deliver(callback, RequestResult.ChannelClosed)
                return null
            }
            if (generation != requestGeneration || _state.value.stage != BridgeStage.READY || computeState().stage != BridgeStage.READY) {
                deliver(callback, RequestResult.Disconnected)
                return null
            }
            if (pendingById.size >= PENDING_CAPACITY) {
                deliver(callback, RequestResult.InvalidRequest(RequestErrorCode.CAPACITY_EXCEEDED, "too many pending requests"))
                return null
            }
            pendingById[id] = pending
            pending.timeoutJob = scope.launch {
                delay(options.requestTimeoutMillis)
                complete(pending, RequestResult.Timeout)
            }
        }
        if (!sendChannel.trySend(SendTask(pending, encodedFrames)).isSuccess) {
            if (removePending(pending)) {
                val result = if (closed.get()) RequestResult.ChannelClosed
                else RequestResult.InvalidRequest(RequestErrorCode.CAPACITY_EXCEEDED, "send queue is full")
                deliver(callback, result)
            }
            return null
        }
        return pending
    }

    /**
     * Sends a task's frames in order, stopping as soon as the request is no longer pending or
     * its deadline has passed. Each frame is bounded by the remaining request budget; any
     * submitted frame keeps the request alive until its response or timeout.
     */
    private suspend fun sendTask(task: SendTask) {
        for (frame in task.frames) {
            if (!isActive(task.pending)) return
            val remaining = task.pending.deadlineAt - System.nanoTime()
            if (remaining <= 0L) {
                complete(task.pending, RequestResult.Timeout)
                return
            }
            val outcome = try {
                withTimeout(remaining / 1_000_000L + 1L) { transport.send(frame) }
            } catch (error: TimeoutCancellationException) {
                complete(task.pending, RequestResult.Timeout)
                return
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                complete(task.pending, RequestResult.ProtocolFailure(ProtocolErrorCode.SDK_CALL_EXCEPTION, error.message))
                return
            }
            when (outcome) {
                is SendOutcome.Submitted -> diagnostics.report(
                    DiagnosticEvent(DiagnosticKind.TX_SUBMITTED, "", cmd = task.pending.cmd, messageId = task.pending.id, bytes = frame.size)
                )
                is SendOutcome.Failed -> {
                    diagnostics.report(
                        DiagnosticEvent(DiagnosticKind.TX_FAILED, outcome.error.toString(), cmd = task.pending.cmd, messageId = task.pending.id)
                    )
                    complete(task.pending, RequestResult.SdkFailure(outcome.error))
                    return
                }
                is SendOutcome.Rejected -> {
                    val result = if (closed.get()) RequestResult.ChannelClosed
                    else RequestResult.ProtocolFailure(ProtocolErrorCode.SDK_CALL_EXCEPTION, outcome.reason)
                    complete(task.pending, result)
                    return
                }
            }
        }
    }

    /**
     * Receive ingress, called from the SDK thread. Drops data unless READY, then hands it to
     * the receive job through a bounded inbox. On overflow the packet is dropped and the
     * assembler is cleared so a later fragment cannot complete a corrupted message.
     */
    private fun onBytes(bytes: ByteArray) {
        val packet = synchronized(pendingLock) {
            if (closed.get() || _state.value.stage != BridgeStage.READY) return
            ReceivedPacket(bytes, generation)
        }
        if (!inbox.trySend(packet).isSuccess) {
            diagnostics.report(DiagnosticEvent(DiagnosticKind.QUEUE_OVERFLOW, "receive queue full, packet dropped", bytes = bytes.size))
            assembler.clear()
        }
    }

    /** Parses one frame and either assembles it or dispatches the complete message. */
    private fun handleFrame(bytes: ByteArray) {
        val header = FrameCodec.decodeHeader(bytes, maxPacketBytes, codec.wireEncoding)
        val payload = bytes.copyOfRange(header.payloadOffset, bytes.size)
        if (header.fragmented) {
            val assembled = assembler.accept(header, payload, generation) ?: return
            process(assembled)
        } else {
            process(
                AssembledMessage(
                    type = header.type,
                    encoding = header.encoding,
                    messageId = header.messageId,
                    cmd = header.cmd,
                    status = header.status,
                    payload = payload,
                )
            )
        }
    }

    /**
     * Routes a complete inbound message. Inbound REQ is not part of the plugin-side contract
     * (the plugin is always the requester) and is only diagnosed.
     */
    private fun process(message: AssembledMessage) {
        when (message.type) {
            MessageType.RSP -> processResponse(message)
            MessageType.EVT -> processEvent(message)
            MessageType.REQ -> diagnostics.report(
                DiagnosticEvent(DiagnosticKind.RX_REJECTED, "inbound REQ is not handled by the plugin", cmd = message.cmd, messageId = message.messageId)
            )
        }
    }

    /**
     * Correlates a response with its pending request. Late/unknown ids only produce a
     * diagnosis; a decode failure completes that request with [ProtocolErrorCode.DECODE_ERROR]
     * without affecting other requests.
     */
    private fun processResponse(message: AssembledMessage) {
        val pending = synchronized(pendingLock) { pendingById[message.messageId] }
        if (pending == null || pending.cmd != message.cmd) {
            diagnostics.report(
                DiagnosticEvent(DiagnosticKind.LATE_MESSAGE, "unmatched response", cmd = message.cmd, messageId = message.messageId)
            )
            return
        }
        val params = try {
            decodePayload(message.cmd, MessageType.RSP, message.payload)
        } catch (error: Exception) {
            diagnostics.report(
                DiagnosticEvent(
                    DiagnosticKind.SCHEMA_DECODE_FAILED,
                    "response decode failed: ${error.message}",
                    cmd = message.cmd,
                    messageId = message.messageId,
                )
            )
            complete(pending, RequestResult.ProtocolFailure(ProtocolErrorCode.DECODE_ERROR, error.message))
            return
        }
        val code = message.status ?: throw ProtocolViolation("RSP missing status")
        complete(pending, RequestResult.Response(PsdkResponse(code = code, params = params)))
    }

    /**
     * Decodes an event, applies the messageId dedup window, then fans it out to listeners.
     * Decode failures and duplicates are dropped without affecting the bridge.
     */
    private fun processEvent(message: AssembledMessage) {
        val params = try {
            decodePayload(message.cmd, MessageType.EVT, message.payload)
        } catch (error: Exception) {
            diagnostics.report(
                DiagnosticEvent(
                    DiagnosticKind.SCHEMA_DECODE_FAILED,
                    "event decode failed: ${error.message}",
                    cmd = message.cmd,
                    messageId = message.messageId,
                )
            )
            return
        }
        if (!recentEvents.accept(message.type, message.messageId)) {
            diagnostics.report(
                DiagnosticEvent(DiagnosticKind.LATE_MESSAGE, "duplicate event ignored", cmd = message.cmd, messageId = message.messageId)
            )
            return
        }
        offer(PsdkEvent(cmd = message.cmd, params = params))
    }

    private fun encodePayload(cmd: Int, type: MessageType, params: Map<String, Any?>): ByteArray =
        codec.encode(type.toKind(), cmd, params)

    private fun decodePayload(cmd: Int, type: MessageType, payload: ByteArray): Map<String, Any?> =
        codec.decode(type.toKind(), cmd, payload)

    private fun MessageType.toKind(): MessageKind = when (this) {
        MessageType.REQ -> MessageKind.REQUEST
        MessageType.RSP -> MessageKind.RESPONSE
        MessageType.EVT -> MessageKind.EVENT
    }

    /**
     * Completes a request at most once: the pending entry is removed under [pendingLock] and
     * only the caller that removed it delivers the result. Stale timeout/send jobs therefore
     * become no-ops.
     */
    private fun complete(pending: Pending, result: RequestResult) {
        val removed = synchronized(pendingLock) {
            if (pendingById[pending.id] !== pending) false else {
                pendingById.remove(pending.id)
                true
            }
        }
        if (!removed) return
        pending.timeoutJob?.cancel()
        diagnostics.report(
            DiagnosticEvent(
                DiagnosticKind.REQUEST_COMPLETED,
                result.javaClass.simpleName,
                cmd = pending.cmd,
                messageId = pending.id,
            )
        )
        deliver(pending.callback, result)
    }

    private fun removePending(pending: Pending): Boolean {
        val removed = synchronized(pendingLock) {
            if (pendingById[pending.id] !== pending) false else {
                pendingById.remove(pending.id)
                true
            }
        }
        if (removed) pending.timeoutJob?.cancel()
        return removed
    }

    private fun cancelPending(pending: Pending, deliverCancelled: Boolean) {
        val removed = synchronized(pendingLock) {
            if (pendingById[pending.id] !== pending) false else {
                pendingById.remove(pending.id)
                true
            }
        }
        if (!removed) return
        pending.timeoutJob?.cancel()
        if (deliverCancelled) deliver(pending.callback, RequestResult.Cancelled)
    }

    private fun isActive(pending: Pending): Boolean = synchronized(pendingLock) { pendingById[pending.id] === pending }

    /** Removes every pending request and completes it with [result] (disconnect/close). */
    private fun failAllPending(result: RequestResult) {
        val snapshot = synchronized(pendingLock) {
            val values = pendingById.values.toList()
            pendingById.clear()
            values
        }
        snapshot.forEach {
            it.timeoutJob?.cancel()
            deliver(it.callback, result)
        }
    }

    /**
     * Invokes a request callback on the delivery scope. ATOMIC start guarantees the final
     * result is delivered even if the bridge is closed concurrently; callback exceptions are
     * diagnosed, never propagated.
     */
    private fun deliver(callback: (RequestResult) -> Unit, result: RequestResult) {
        deliveryScope.launch(start = CoroutineStart.ATOMIC) {
            try {
                callback(result)
            } catch (error: Exception) {
                diagnostics.report(DiagnosticEvent(DiagnosticKind.RX_REJECTED, "callback failed: ${error.message}"))
            }
        }
    }

    /** Cold flow view over an [EventRegistration]; unregisters when collection ends. */
    private fun observeInternal(cmd: Int?): Flow<PsdkEvent> = flow {
        val registration = register(cmd)
        try {
            for (event in registration.channel) {
                if (!registration.active || closed.get()) break
                if (event.generation == generation && _state.value.stage == BridgeStage.READY) emit(event.event)
            }
        } finally {
            unregister(registration)
        }
    }

    /**
     * Drains one registration's bounded queue on the callback dispatcher and invokes
     * [listener] in arrival order. Events from an older generation or outside READY are
     * skipped. Queue overflow ends the registration (reported as LISTENER_OVERFLOW) without
     * affecting other listeners. [onStopped] runs after the drain job ends for any reason.
     */
    private fun addListenerInternal(cmd: Int?, listener: (PsdkEvent) -> Unit, onStopped: () -> Unit = {}): Closeable {
        val registration = register(cmd)
        val job = scope.launch(options.callbackDispatcher) {
            try {
                for (event in registration.channel) {
                    if (!registration.active || closed.get()) break
                    if (event.generation != generation || _state.value.stage != BridgeStage.READY) continue
                    try {
                        listener(event.event)
                    } catch (error: Exception) {
                        diagnostics.report(DiagnosticEvent(DiagnosticKind.RX_REJECTED, "listener failed: ${error.message}"))
                    }
                }
            } catch (error: Exception) {
                diagnostics.report(DiagnosticEvent(DiagnosticKind.LISTENER_OVERFLOW, "listener stopped: ${error.message}"))
            } finally {
                unregister(registration)
                onStopped()
            }
        }
        return Closeable {
            unregister(registration)
            job.cancel()
        }
    }

    /** Adds a registration; throws if the bridge is already closed. */
    private fun register(cmd: Int?): EventRegistration {
        val registration = EventRegistration(cmd)
        synchronized(listenerLock) {
            check(!closed.get()) { "bridge is closed" }
            listeners.add(registration)
        }
        return registration
    }

    /** Deactivates and removes a registration; idempotent. */
    private fun unregister(registration: EventRegistration) {
        registration.active = false
        synchronized(listenerLock) { listeners.remove(registration) }
        registration.channel.cancel()
    }

    /**
     * Fans an event out to matching registrations without blocking the receive job. Each
     * registration has its own queue; a full queue removes just that registration.
     */
    private fun offer(event: PsdkEvent) {
        val snapshot = synchronized(listenerLock) { listeners.toList() }
        snapshot.forEach { registration ->
            if (registration.cmd == null || registration.cmd == event.cmd) {
                if (!registration.channel.trySend(QueuedEvent(event, generation)).isSuccess) {
                    synchronized(listenerLock) { listeners.remove(registration) }
                    registration.channel.close(ListenerOverflowException(registration.cmd))
                    diagnostics.report(
                        DiagnosticEvent(DiagnosticKind.LISTENER_OVERFLOW, "listener queue full", cmd = event.cmd)
                    )
                }
            }
        }
    }

    /**
     * Publishes a new state. Leaving READY invalidates the current [generation] and clears
     * assembly/dedup state under [pendingLock], so no frame from the old link can complete a
     * request issued on the new one. Pending requests are failed with
     * [RequestResult.Disconnected] after the lock is released.
     */
    private fun applyState(next: BridgeState) {
        val disconnected = synchronized(pendingLock) {
            if (closed.get()) return
            val previous = _state.value
            _state.value = next
            if (previous.stage == BridgeStage.READY && next.stage != BridgeStage.READY) {
                generation++
                assembler.clear()
                recentEvents.clear()
                pendingById.values.toList().also { pendingById.clear() }
            } else emptyList()
        }
        disconnected.forEach {
            it.timeoutJob?.cancel()
            deliver(it.callback, RequestResult.Disconnected)
        }
        if (disconnected.isNotEmpty()) {
            diagnostics.report(
                DiagnosticEvent(DiagnosticKind.DISCONNECTED, "bridge left READY: ${next.reason}")
            )
        }
    }

    private fun computeState(): BridgeState = computeState(
        connections.sdkRegistered.value,
        connections.pilotConnected.value,
        connections.aircraftConnected.value,
        transport.link.value.connected,
        transport.link.value.featureOpened,
    )

    /**
     * READY requires all of: SDK registered, Pilot connected, aircraft connected, payload
     * connected and the payload's PSDK feature opened. The first failing condition decides
     * the stage/reason reported to callers.
     */
    private fun computeState(
        sdk: Boolean,
        pilot: Boolean,
        aircraft: Boolean,
        payloadConnected: Boolean,
        featureOpened: Boolean,
    ): BridgeState = when {
        closed.get() -> BridgeState(BridgeStage.CLOSED)
        !sdk -> BridgeState(BridgeStage.STOPPED, NotReadyReason.SDK_NOT_REGISTERED)
        !pilot -> BridgeState(BridgeStage.DISCONNECTED, NotReadyReason.PILOT_DISCONNECTED)
        !aircraft -> BridgeState(BridgeStage.DISCONNECTED, NotReadyReason.AIRCRAFT_DISCONNECTED)
        !payloadConnected -> BridgeState(BridgeStage.DISCONNECTED, NotReadyReason.PAYLOAD_DISCONNECTED)
        !featureOpened -> BridgeState(BridgeStage.DISCONNECTED, NotReadyReason.FEATURE_UNAVAILABLE)
        else -> BridgeState(BridgeStage.READY)
    }

    /**
     * Idempotent shutdown: marks CLOSED, stops jobs/channels, fails pending requests, closes
     * handlers/listeners and finally removes this transport's SDK listeners.
     */
    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        synchronized(pendingLock) {
            _state.value = BridgeState(BridgeStage.CLOSED)
            generation++
            assembler.clear()
            recentEvents.clear()
        }
        stateJob.cancel()
        receiveJob.cancel()
        sendJob.cancel()
        purgeJob.cancel()
        sendChannel.cancel()
        inbox.cancel()
        failAllPending(RequestResult.ChannelClosed)
        val handlerSnapshot = synchronized(handlerLock) {
            handlers.toList().also { handlers.clear() }
        }
        handlerSnapshot.forEach { it.close() }
        val snapshot = synchronized(listenerLock) {
            listeners.toList().also { listeners.clear() }
        }
        snapshot.forEach {
            it.active = false
            it.channel.cancel()
        }
        try {
            transport.close()
        } finally {
            scope.cancel()
            deliveryScope.cancel()
        }
    }

    /**
     * One in-flight request. [deadlineAt] is a [System.nanoTime] value taken at submission;
     * [timeoutJob] is cancelled whenever the request is completed or removed.
     */
    private class Pending(
        val id: Long,
        val cmd: Int,
        val callback: (RequestResult) -> Unit,
        val deadlineAt: Long,
    ) {
        var timeoutJob: Job? = null
    }

    /** A pending request plus all frames the send job must submit, in order. */
    private class SendTask(val pending: Pending, val frames: List<ByteArray>)

    /** Event stamped with the generation observed at receive time. */
    private class QueuedEvent(val event: PsdkEvent, val generation: Long)

    /** A listener/handler registration; [cmd] is `null` for an all-command listener. */
    private class EventRegistration(val cmd: Int?) {
        @Volatile var active = true
        val channel = Channel<QueuedEvent>(LISTENER_QUEUE_CAPACITY)
    }

    internal class ListenerOverflowException(cmd: Int?) : RuntimeException("listener overflow for cmd=$cmd")

    /**
     * Small LRU + TTL window of recently seen `(type, messageId)` pairs. It only suppresses
     * duplicate EVTs for a short period and never stores business payloads.
     */
    private class RecentEventCache(private val maxEntries: Int, private val ttlNanos: Long) {
        private val entries = object : LinkedHashMap<Pair<MessageType, Long>, Long>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<MessageType, Long>, Long>?): Boolean =
                size > maxEntries
        }

        @Synchronized
        fun accept(type: MessageType, messageId: Long): Boolean {
            val now = System.nanoTime()
            val key = type to messageId
            val previous = entries[key]
            if (previous != null && now - previous <= ttlNanos) return false
            entries[key] = now
            return true
        }

        @Synchronized
        fun clear() = entries.clear()
    }

    private companion object {
        /** Inbound frames buffered before the receive job; overflow drops and clears assemblies. */
        const val INBOX_CAPACITY = 128

        /** Maximum concurrent in-flight requests per bridge. */
        const val PENDING_CAPACITY = 64

        /** Outbound tasks buffered before the send job; full queue rejects the request. */
        const val SEND_QUEUE_CAPACITY = 64

        /** Per-registration event buffer; a full queue ends that registration. */
        const val LISTENER_QUEUE_CAPACITY = 64
    }
}
