package dji.open.pilot.psdk

import dji.open.pilot.psdk.client.PsdkClient
import dji.open.pilot.psdk.transport.PayloadTransport
import dji.open.pilot.psdk.transport.SdkPayloadTransport
import dji.v5.manager.aircraft.payload.PayloadCenter
import dji.v5.manager.aircraft.payload.PayloadIndexType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.Closeable

/**
 * Application-level, port-scoped bridge for plugin <-> PSDK payload messages.
 *
 * One bridge owns one payload port, one [PayloadCodec] and one message-id sequence. The host
 * application keeps ownership of SDK registration and connection facts; the bridge only turns
 * them into [BridgeState] and serializes/deserializes messages. Requests submitted before
 * [BridgeStage.READY] complete with [RequestResult.NotReady] and are not sent.
 *
 * JSON is the default payload codec and requires no schema registration. Advanced
 * callers can pass a different [PayloadCodec] without changing the bridge API.
 *
 * Obtain instances through [getOrCreate]; instances are shared per port and must not be
 * closed by short-lived screens that merely stop listening.
 *
 * @property port payload port this bridge owns.
 * @property maxPacketBytes maximum size of a single wire frame, 16..255 bytes.
 */
class PsdkBridge internal constructor(
    val port: PayloadIndexType,
    val maxPacketBytes: Int,
    private val codec: PayloadCodec,
    private val client: PsdkClient,
) : AutoCloseable {
    /** Current lifecycle state; use it to show readiness in the UI. */
    val state: StateFlow<BridgeState> get() = client.state

    /** True only while [state] is [BridgeStage.READY]. */
    val isReady: Boolean get() = state.value.stage == BridgeStage.READY

    /** Wire encoding of the configured codec. */
    val encoding: PayloadEncoding get() = codec.wireEncoding

    /**
     * Sends a request and suspends until it completes (response, timeout or failure).
     *
     * Cancelling the calling coroutine only removes the local pending request; it does not
     * notify the payload.
     */
    suspend fun request(cmd: Int, params: Map<String, Any?> = emptyMap()): RequestResult =
        client.request(cmd, params)

    /**
     * Sends a request without suspending. [callback] runs on [PsdkOptions.callbackDispatcher]
     * exactly once unless the returned [RequestHandle] is cancelled first.
     */
    fun request(
        cmd: Int,
        params: Map<String, Any?> = emptyMap(),
        callback: (RequestResult) -> Unit,
    ): RequestHandle = client.request(cmd, params, callback)

    /**
     * Suspending request whose `params` are read from the public fields of a data class
     * (see `Any.toPsdkPayload`). Enums become their ordinal and Byte/Short become Int.
     */
    suspend fun request(cmd: Int, payload: Any): RequestResult = client.request(cmd, payload)

    /** Callback variant of [request] for a data-class payload. */
    fun request(
        cmd: Int,
        payload: Any,
        callback: (RequestResult) -> Unit,
    ): RequestHandle = client.request(cmd, payload, callback)

    /** Suspending request carrying a [PsdkRequest]. */
    suspend fun request(request: PsdkRequest): RequestResult = client.request(request.cmd, request.params)

    /** Callback variant of [request] carrying a [PsdkRequest]. */
    fun request(request: PsdkRequest, callback: (RequestResult) -> Unit): RequestHandle =
        client.request(request.cmd, request.params, callback)

    /**
     * Cold flow of events for one command. Collection registers a bounded listener; events
     * arriving before collection or after cancellation are dropped, never replayed.
     */
    fun observe(cmd: Int): Flow<PsdkEvent> = client.observe(cmd)

    /** Cold flow of events for every command; same delivery semantics as [observe]. */
    fun observeAll(): Flow<PsdkEvent> = client.observeAll()

    /**
     * Registers a command-scoped callback. Prefer [registerEventHandler] for long-lived
     * receivers; this is convenient for ad-hoc UI collection. Close the handle to stop.
     */
    fun addListener(cmd: Int, listener: (PsdkEvent) -> Unit): Closeable = client.addListener(cmd, listener)

    /** Registers a callback for all commands; close the handle to stop. */
    fun addListener(listener: (PsdkEvent) -> Unit): Closeable = client.addListener(listener)

    /**
     * Registers a command-scoped [PsdkEventHandler] and dispatches matching events to it on
     * the configured callback dispatcher, in arrival order.
     *
     * Only events whose `cmd` equals [PsdkEventHandler.cmd] are delivered. Events that arrive
     * before registration or after the returned handle is closed are dropped, never replayed.
     * Multiple handlers for the same command receive events independently. Arrival order is
     * preserved within each registration; ordering across registrations is not guaranteed.
     * An exception from one handler does not affect the others.
     *
     * @return handle that unregisters this registration; idempotent.
     * @throws IllegalArgumentException if [PsdkEventHandler.cmd] is outside 1..65535.
     * @throws IllegalStateException if the bridge is already closed.
     */
    fun registerEventHandler(handler: PsdkEventHandler): Closeable = client.registerEventHandler(handler)

    /** Removes every registration of [handler] by identity; returns the removed count. */
    fun unregisterEventHandler(handler: PsdkEventHandler): Int = client.unregisterEventHandler(handler)

    /**
     * Releases the bridge: fails pending requests with [RequestResult.ChannelClosed], cancels
     * timeout jobs, drops listeners/assemblies and removes this instance's SDK listeners.
     * Idempotent; the instance is unusable afterwards.
     */
    override fun close() {
        try {
            client.close()
        } finally {
            release(this)
        }
    }

    companion object {
        private data class ConfigKey(
            val maxPacketBytes: Int,
            val codec: PayloadCodec,
            val options: PsdkOptions,
            val connections: PsdkConnections,
        )

        private val registry = mutableMapOf<PayloadIndexType, PsdkBridge>()

        /**
         * Returns the cached bridge for [port], creating it on first use with the default JSON
         * codec.
         *
         * @param port payload port; [PayloadIndexType.EXTERNAL] is rejected because it is not a
         *   PSDK protocol port.
         * @param maxPacketBytes maximum single-frame size, 16..255.
         * @param connections authoritative SDK/Pilot/aircraft connection flows owned by the host.
         * @param options timeout, message size and dispatcher configuration.
         * @throws IllegalArgumentException if the arguments are invalid, the port has no
         *   payload manager, or a live bridge exists with a different configuration.
         */
        @Synchronized
        fun getOrCreate(
            port: PayloadIndexType,
            maxPacketBytes: Int,
            connections: PsdkConnections,
            options: PsdkOptions = PsdkOptions(),
        ): PsdkBridge = getOrCreate(port, maxPacketBytes, connections, options, JsonPayloadCodec(options.maxMessageBytes))

        /**
         * Same as the JSON overload but with an explicit [PayloadCodec].
         *
         * Configuration (packet size, codec, options and connection flows) is part of the
         * cache key: repeated calls with an equal configuration return the same instance,
         * while a different configuration is rejected instead of silently replacing a shared
         * channel. Close the existing bridge first to change it.
         */
        @Synchronized
        fun getOrCreate(
            port: PayloadIndexType,
            maxPacketBytes: Int,
            connections: PsdkConnections,
            options: PsdkOptions,
            codec: PayloadCodec,
        ): PsdkBridge {
            validate(port, maxPacketBytes, options)
            val key = ConfigKey(maxPacketBytes, codec, options, connections)
            registry[port]?.let { existing ->
                if (!existing.isClosed() && existing.key == key) return existing
                if (!existing.isClosed()) {
                    throw IllegalArgumentException(
                        "port $port already has a bridge with a different configuration; close it first"
                    )
                }
                registry.remove(port)
            }
            val manager = PayloadCenter.getInstance().payloadManager[port]
                ?: throw IllegalArgumentException("payload port $port is not available")
            val bridge = PsdkBridge(
                port = port,
                maxPacketBytes = maxPacketBytes,
                codec = codec,
                client = PsdkClient(
                    port = port,
                    maxPacketBytes = maxPacketBytes,
                    codec = codec,
                    connections = connections,
                    options = options,
                    transport = SdkPayloadTransport(manager, maxPacketBytes),
                    diagnostics = LogcatDiagnostics,
                ),
            ).also { it.key = key }
            registry[port] = bridge
            return bridge
        }

        /** Closes and unregisters the bridge for [port], if any; safe to call repeatedly. */
        @Synchronized
        fun closePort(port: PayloadIndexType) {
            registry.remove(port)?.close()
        }

        @Synchronized
        internal fun release(bridge: PsdkBridge) {
            if (registry[bridge.port] === bridge) registry.remove(bridge.port)
        }

        private fun validate(port: PayloadIndexType, maxPacketBytes: Int, options: PsdkOptions) {
            if (port == PayloadIndexType.EXTERNAL) {
                throw IllegalArgumentException("EXTERNAL is not a PSDK protocol port")
            }
            if (maxPacketBytes !in 16..255) {
                throw IllegalArgumentException("maxPacketBytes must be 16..255")
            }
            if (options.maxMessageBytes < maxPacketBytes) {
                throw IllegalArgumentException("maxMessageBytes must be >= maxPacketBytes")
            }
        }

        internal fun forTransport(
            port: PayloadIndexType,
            maxPacketBytes: Int,
            connections: PsdkConnections,
            options: PsdkOptions,
            codec: PayloadCodec,
            transport: PayloadTransport,
            diagnostics: PsdkDiagnostics,
        ): PsdkBridge {
            validate(port, maxPacketBytes, options)
            return PsdkBridge(
                port = port,
                maxPacketBytes = maxPacketBytes,
                codec = codec,
                client = PsdkClient(
                    port = port,
                    maxPacketBytes = maxPacketBytes,
                    codec = codec,
                    connections = connections,
                    options = options,
                    transport = transport,
                    diagnostics = diagnostics,
                ),
            ).also {
                it.key = ConfigKey(maxPacketBytes, codec, options, connections)
            }
        }
    }

    private var key: ConfigKey? = null

    private fun isClosed(): Boolean = state.value.stage == BridgeStage.CLOSED
}
