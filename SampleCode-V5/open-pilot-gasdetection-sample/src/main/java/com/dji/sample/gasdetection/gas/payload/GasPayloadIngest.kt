package com.dji.sample.gasdetection.gas.payload

import com.dji.sample.gasdetection.gas.model.GasMeasurement
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.payload.PayloadIndexType
import dji.v5.manager.aircraft.payload.data.PayloadBasicInfo
import dji.v5.manager.aircraft.payload.data.PayloadWidgetInfo
import dji.v5.manager.aircraft.payload.listener.PayloadBasicInfoListener
import dji.v5.manager.aircraft.payload.listener.PayloadDataListener
import dji.v5.manager.aircraft.payload.listener.PayloadWidgetInfoListener
import dji.v5.manager.aircraft.payload.widget.PayloadWidget
import dji.v5.manager.interfaces.IPayloadManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 气体载荷接入服务。
 *
 * 设计意图：把「SDK 载荷监听」收敛到一个可启停的服务里，对外只暴露在线快照、能力快照、
 * 测量流与诊断。业务层不直接接触 SDK 监听器；没有厂商协议时，适配器边界保证接入层仍然可运行，
 * 未匹配适配器会显式失败而不是猜格式。
 *
 * 生命周期：由接线层在插件与 Pilot 连接时调用 [start]，断开或应用销毁时调用 [stop]。
 * [stop] 幂等，只移除本服务注册的监听，不调用 `clearAll*`、不销毁 SDK 管理器，可与调试页、
 * `psdk-bridge` 的监听共存。
 *
 * 线程安全：SDK 回调可能来自不同线程；监听器表、快照与计数由内部锁保护，对外状态通过
 * `StateFlow` 发布，数据通过 `SharedFlow` 非阻塞投递。
 */
class GasPayloadIngest(
    private val managers: Map<PayloadIndexType, IPayloadManager>,
    private val registry: PayloadAdapterRegistry,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private data class Registration(
        val manager: IPayloadManager,
        val basic: PayloadBasicInfoListener,
        val widget: PayloadWidgetInfoListener,
        val data: PayloadDataListener,
    )

    /** 基础信息回调的纯数据投影，便于不依赖 SDK 对象做单元测试。 */
    internal data class BasicInfoValue(
        val connected: Boolean,
        val payloadType: String?,
        val productName: String?,
        val productPhase: String?,
        val serialNumber: String?,
        val firmwareVersion: String?,
        val featureOpened: Boolean,
    )

    private val lock = Any()
    private var started = false
    private var closed = false
    private val registrations = LinkedHashMap<PayloadPort, Registration>()
    private val snapshots = LinkedHashMap<PayloadPort, PayloadSnapshot>()
    private val widgetSnapshots = LinkedHashMap<PayloadPort, PayloadWidgetSnapshot>()
    private val portDiagnostics = LinkedHashMap<PayloadPort, PortDiagnostics>()

    private val stateFlow = MutableStateFlow(IngestState())
    private val payloadsFlow = MutableStateFlow<List<PayloadSnapshot>>(emptyList())
    private val capabilitiesFlow = MutableStateFlow<Map<PayloadPort, PayloadWidgetSnapshot>>(emptyMap())
    private val diagnosticsFlow = MutableStateFlow(PayloadDiagnostics())
    private val measurementFlow = MutableSharedFlow<GasMeasurement>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** 服务状态：是否监听、监听端口数、在线载荷数。 */
    val state: StateFlow<IngestState> = stateFlow.asStateFlow()

    /** 各端口的载荷在线快照，按端口值排序。 */
    val payloads: StateFlow<List<PayloadSnapshot>> = payloadsFlow.asStateFlow()

    /** 各端口的设备能力快照。 */
    val capabilities: StateFlow<Map<PayloadPort, PayloadWidgetSnapshot>> = capabilitiesFlow.asStateFlow()

    /** 接入诊断。 */
    val diagnostics: StateFlow<PayloadDiagnostics> = diagnosticsFlow.asStateFlow()

    /** 成功转换的测量流；只在服务运行时产生。 */
    val measurements: Flow<GasMeasurement> = measurementFlow.asSharedFlow()

    /**
     * 注册全部非 EXTERNAL 端口的载荷监听。
     *
     * 重复调用直接返回 [IngestStartResult.Started]；注册中途失败时回滚本次已注册的监听并返回原因。
     */
    fun start(): IngestStartResult {
        val ports = managers.keys
            .filter { it != PayloadIndexType.EXTERNAL }
            .sortedBy { it.value() }
        synchronized(lock) {
            if (started) return IngestStartResult.Started
            closed = false
            val added = mutableListOf<PayloadPort>()
            try {
                ports.forEach { index ->
                    val manager = managers.getValue(index)
                    val port = PayloadPort(index.name, index.value())
                    val basic = PayloadBasicInfoListener { info -> onBasicInfo(port, info) }
                    val widget = PayloadWidgetInfoListener { info -> onWidgetInfo(port, info) }
                    val data = PayloadDataListener { bytes -> onData(port, bytes) }
                    manager.addPayloadBasicInfoListener(basic)
                    manager.addPayloadWidgetInfoListener(widget)
                    manager.addPayloadDataListener(data)
                    registrations[port] = Registration(manager, basic, widget, data)
                    snapshots[port] = PayloadSnapshot(
                        port = port,
                        connected = false,
                        payloadType = null,
                        productName = null,
                        productPhase = null,
                        serialNumber = null,
                        firmwareVersion = null,
                        featureOpened = false,
                        updatedAtMs = clock(),
                    )
                    portDiagnostics[port] = PortDiagnostics()
                    added.add(port)
                }
                started = true
                publishLocked()
                return IngestStartResult.Started
            } catch (error: Throwable) {
                added.forEach { port -> registrations.remove(port)?.let(::removeListeners) }
                snapshots.clear()
                portDiagnostics.clear()
                publishLocked()
                return IngestStartResult.Failed(error.message ?: error.javaClass.simpleName)
            }
        }
    }

    /** 移除本服务注册的全部监听；幂等，重复调用无副作用。 */
    fun stop() {
        synchronized(lock) {
            if (!started) return
            closed = true
            registrations.values.forEach(::removeListeners)
            registrations.clear()
            snapshots.clear()
            widgetSnapshots.clear()
            started = false
            publishLocked()
        }
    }

    /**
     * 主动向所有已监听端口请求刷新设备能力。
     *
     * 成功时由 Widget 回调更新快照；失败时保留上一次成功内容，只补充失败原因与时间。
     * 服务未启动时不做任何事。
     */
    fun refreshCapability() {
        val entries = synchronized(lock) {
            if (!started) return
            registrations.toList()
        }
        entries.forEach { (port, registration) ->
            registration.manager.pullWidgetInfoFromPayload(
                object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() = Unit

                    override fun onFailure(error: IDJIError) {
                        synchronized(lock) {
                            if (!started) return
                            val previous = widgetSnapshots[port]
                                ?: PayloadWidgetSnapshot(port = port, items = emptyList(), capturedAtMs = clock())
                            widgetSnapshots[port] = previous.copy(
                                lastError = error.toString(),
                                lastErrorAtMs = clock(),
                            )
                            publishLocked()
                        }
                    }
                },
            )
        }
    }

    /** 基础信息回调：更新在线快照与在线数。 */
    private fun onBasicInfo(port: PayloadPort, info: PayloadBasicInfo) {
        applyBasicInfo(
            port = port,
            value = BasicInfoValue(
                connected = info.isConnected,
                payloadType = info.payloadType?.name,
                productName = info.payloadProductName?.takeIf { it.isNotBlank() },
                productPhase = info.payloadProductPhaseType?.name,
                serialNumber = info.serialNumber?.takeIf { it.isNotBlank() },
                firmwareVersion = info.firmwareVersion?.takeIf { it.isNotBlank() },
                featureOpened = info.isFeatureOpened,
            ),
        )
    }

    /** 更新指定端口的在线快照；供回调与单元测试使用。 */
    internal fun applyBasicInfo(port: PayloadPort, value: BasicInfoValue) {
        synchronized(lock) {
            if (!started) return
            snapshots[port] = PayloadSnapshot(
                port = port,
                connected = value.connected,
                payloadType = value.payloadType,
                productName = value.productName,
                productPhase = value.productPhase,
                serialNumber = value.serialNumber,
                firmwareVersion = value.firmwareVersion,
                featureOpened = value.featureOpened,
                updatedAtMs = clock(),
            )
            publishLocked()
        }
    }

    /** Widget 回调：保存控件快照。 */
    private fun onWidgetInfo(port: PayloadPort, info: PayloadWidgetInfo) {
        val items = buildList {
            info.mainInterfaceWidgetList?.forEach { add(it.toItem()) }
            info.configInterfaceWidgetList?.forEach { add(it.toItem()) }
        }
        applyWidgetItems(port, items)
    }

    /** 更新指定端口的控件快照；供回调与单元测试使用。 */
    internal fun applyWidgetItems(port: PayloadPort, items: List<PayloadWidgetItem>) {
        synchronized(lock) {
            if (!started) return
            widgetSnapshots[port] = PayloadWidgetSnapshot(
                port = port,
                items = items,
                capturedAtMs = clock(),
            )
            publishLocked()
        }
    }

    /** 数据回调：按端口路由到适配器，更新诊断并投递测量。 */
    private fun onData(port: PayloadPort, data: ByteArray) {
        synchronized(lock) {
            if (!started || closed) return
        }
        val receivedAt = clock()
        val adapter = registry.find(port)
        if (adapter == null) {
            recordDiagnostics(
                port = port,
                received = true,
                noAdapter = true,
                lastError = "no adapter registered for port ${port.name}",
            )
            return
        }
        val payloadId = synchronized(lock) { snapshots[port]?.productName }
        val outcome = try {
            adapter.decode(port, payloadId, data.copyOf(), receivedAt)
        } catch (error: Throwable) {
            AdapterOutcome.Rejected("adapter error: ${error.message ?: error.javaClass.simpleName}")
        }
        when (outcome) {
            is AdapterOutcome.Measurements -> {
                recordDiagnostics(port = port, received = true, decoded = outcome.items.size.toLong())
                outcome.items.forEach { measurementFlow.tryEmit(it) }
            }

            is AdapterOutcome.Ignored ->
                recordDiagnostics(port = port, received = true, ignored = true, lastError = outcome.reason)

            is AdapterOutcome.Rejected ->
                recordDiagnostics(port = port, received = true, failed = true, lastError = outcome.reason)
        }
    }

    private fun recordDiagnostics(
        port: PayloadPort,
        received: Boolean = false,
        decoded: Long = 0,
        ignored: Boolean = false,
        failed: Boolean = false,
        noAdapter: Boolean = false,
        lastError: String? = null,
    ) {
        synchronized(lock) {
            if (!started) return
            val current = portDiagnostics[port] ?: PortDiagnostics()
            portDiagnostics[port] = current.copy(
                receivedFrames = current.receivedFrames + if (received) 1 else 0,
                decodedMeasurements = current.decodedMeasurements + decoded,
                ignoredFrames = current.ignoredFrames + if (ignored) 1 else 0,
                failures = current.failures + if (failed) 1 else 0,
                noAdapter = current.noAdapter + if (noAdapter) 1 else 0,
                lastError = lastError ?: current.lastError,
                lastErrorAtMs = if (lastError != null) clock() else current.lastErrorAtMs,
            )
            publishLocked()
        }
    }

    private fun removeListeners(registration: Registration) {
        runCatching { registration.manager.removePayloadBasicInfoListener(registration.basic) }
        runCatching { registration.manager.removePayloadWidgetInfoListener(registration.widget) }
        runCatching { registration.manager.removePayloadDataListener(registration.data) }
    }

    private fun publishLocked() {
        payloadsFlow.value = snapshots.values.sortedBy { it.port.value }
        capabilitiesFlow.value = widgetSnapshots.toMap()
        diagnosticsFlow.value = PayloadDiagnostics(
            ports = portDiagnostics.toMap(),
            lastCapabilityAtMs = widgetSnapshots.values.maxOfOrNull { it.capturedAtMs },
        )
        stateFlow.value = IngestState(
            started = started,
            monitoredPorts = registrations.size,
            onlinePayloads = snapshots.values.count { it.connected },
        )
    }

    private fun PayloadWidget.toItem(): PayloadWidgetItem = PayloadWidgetItem(
        name = widgetName?.takeIf { it.isNotBlank() },
        type = widgetType?.name,
        index = widgetIndex,
        value = widgetValue,
        hint = hintMessage?.takeIf { it.isNotBlank() },
    )
}
