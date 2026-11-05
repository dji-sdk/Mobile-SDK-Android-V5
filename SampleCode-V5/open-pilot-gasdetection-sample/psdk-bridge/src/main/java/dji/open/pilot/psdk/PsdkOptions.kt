package dji.open.pilot.psdk

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow

/**
 * Tunables for a [PsdkBridge] instance. Values are fixed at creation time and are part
 * of the bridge's identity when [PsdkBridge.getOrCreate] is called again for the same port.
 *
 * @property requestTimeoutMillis total budget from submission to response, covering queueing,
 *   fragment submission and the wait for a response. Must be > 0.
 * @property maxMessageBytes upper bound for one fully assembled message (after fragment
 *   reassembly). Must be 1..65535 and at least the bridge's `maxPacketBytes`.
 * @property callbackDispatcher dispatcher for request callbacks, event handlers and listeners;
 *   defaults to the main thread.
 * @property workerDispatcher dispatcher for internal receive/send/state/timeout coroutines.
 */
data class PsdkOptions(
    val requestTimeoutMillis: Long = 3000,
    val maxMessageBytes: Int = 1024,
    val callbackDispatcher: CoroutineDispatcher = Dispatchers.Main,
    val workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    init {
        require(requestTimeoutMillis > 0) { "requestTimeoutMillis must be > 0" }
        require(maxMessageBytes in 1..65535) { "maxMessageBytes must be 1..65535" }
    }
}

/**
 * Connection facts owned by the host application.
 *
 * The bridge never initializes or registers the SDK itself; it only consumes these
 * authoritative state flows together with the payload basic-info listener. All three flows
 * must be observed from the host's real SDK state; the bridge combines them with the payload
 * manager's own connection/feature state to decide [BridgeStage.READY].
 *
 * @property sdkRegistered whether the SDK has completed registration.
 * @property pilotConnected whether Pilot is connected.
 * @property aircraftConnected whether the aircraft is connected.
 */
data class PsdkConnections(
    val sdkRegistered: StateFlow<Boolean>,
    val pilotConnected: StateFlow<Boolean>,
    val aircraftConnected: StateFlow<Boolean>,
)
