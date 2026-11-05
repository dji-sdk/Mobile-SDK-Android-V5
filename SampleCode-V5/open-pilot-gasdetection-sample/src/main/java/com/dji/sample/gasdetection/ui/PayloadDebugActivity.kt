package com.dji.sample.gasdetection.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.dji.sample.gasdetection.PsdkApplication
import com.dji.sample.gasdetection.R
import com.dji.sample.gasdetection.RawPayloadChannel
import com.dji.sample.gasdetection.databinding.ActivityPayloadDebugBinding
import dji.open.pilot.psdk.PsdkBridge
import dji.open.pilot.psdk.RequestResult
import dji.v5.manager.aircraft.payload.PayloadCenter
import dji.v5.manager.aircraft.payload.PayloadIndexType
import dji.v5.manager.aircraft.payload.listener.PayloadBasicInfoListener
import dji.v5.manager.interfaces.IPayloadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/**
 * 负载调试页：在真实载荷端口上执行协议请求或原始字节收发。
 *
 * 调试能力集中在二级页，与主页的任务、设备监测及记录传输功能分离。
 * 本页不引入新协议、会话、CRC 或重试，只复用 [RawPayloadChannel] 与 [PsdkBridge] 的既有行为。
 *
 * 生命周期：仅在页面处于前台且 Pilot 已连接时注册端口监听；离开前台、Pilot 断开或页面销毁时
 * 移除监听并取消协程。所有 UI 更新回到主线程。
 */
class PayloadDebugActivity : AppCompatActivity() {

    private companion object {
        const val TAG = "GasPayloadDebug"
        const val CMD_READ_DEVICE_INFO = 2
        const val DEFAULT_MAX_BYTES = "255"
        const val DEFAULT_PAYLOAD = "00 41 FF"
        const val MAX_LOG_LINES = 200
    }

    private lateinit var binding: ActivityPayloadDebugBinding
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val queued = AtomicInteger()
    private val dropped = AtomicInteger()
    private val lines = ArrayDeque<String>()
    private val basicListeners = mutableMapOf<IPayloadManager, PayloadBasicInfoListener>()
    private val connected = mutableSetOf<PayloadIndexType>()
    private var ports = emptyList<PayloadIndexType>()
    private var channel: RawPayloadChannel? = null
    private var bridge: PsdkBridge? = null
    private var stateJob: Job? = null
    private var eventJob: Job? = null
    private var selected: PayloadIndexType? = null
    private var generation = 0
    private var started = false

    private val app: PsdkApplication
        get() = application as PsdkApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPayloadDebugBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etMaxBytes.setText(DEFAULT_MAX_BYTES)
        binding.etPayloadInput.setText(DEFAULT_PAYLOAD)
        binding.cbProtocolMode.isChecked = true
        binding.cbHexInput.isChecked = true

        binding.btnBack.setOnClickListener { finish() }
        binding.btnApply.setOnClickListener { bind() }
        binding.btnSend.setOnClickListener { send() }

        app.sdkStatus.observe(this) { binding.tvStatus.text = it }
        app.pilotConnected.observe(this) {
            if (started) {
                if (it) attachPorts() else detachPorts()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        started = true
        Log.d(TAG, "Debug page started")
        if (app.pilotConnected.value == true) {
            attachPorts()
            if (binding.cbProtocolMode.isChecked && bridge != null) observeBridge(bridge!!)
        }
    }

    private fun attachPorts() {
        if (basicListeners.isNotEmpty()) return
        val managers = PayloadCenter.getInstance().payloadManager
        ports = managers.keys.filter { it != PayloadIndexType.EXTERNAL }.sortedBy { it.value() }
        binding.spinnerPort.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            ports.map { getString(R.string.gas_port_item, it.name, it.value()) }
        )
        managers.forEach { (index, manager) ->
            val listener = PayloadBasicInfoListener { info ->
                val available = info.isConnected
                val name = info.payloadProductName
                handler.post {
                    if (!started || basicListeners[manager] == null) return@post
                    if (available) connected.add(index) else connected.remove(index)
                    binding.tvPortInfo.text = getString(
                        R.string.gas_ports_online,
                        connected.joinToString { it.name },
                        index.name,
                        name
                    )
                    if (!available && selected == index) {
                        if (binding.cbProtocolMode.isChecked) closeProtocol() else closeChannel()
                    }
                }
            }
            basicListeners[manager] = listener
            manager.addPayloadBasicInfoListener(listener)
        }
        binding.tvPortInfo.text = getString(R.string.gas_ports_idle)
    }

    private fun bind(): Boolean {
        if (app.pilotConnected.value != true) {
            log(getString(R.string.gas_error_pilot_not_connected)); return false
        }
        val index = ports.getOrNull(binding.spinnerPort.selectedItemPosition)
        val limit = binding.etMaxBytes.text.toString().toIntOrNull()
        val protocolMode = binding.cbProtocolMode.isChecked
        val range = if (protocolMode) 16..255 else 1..255
        if (index == null || limit == null || limit !in range) {
            log(getString(R.string.gas_error_select_port_and_bytes)); return false
        }
        if (index !in connected) {
            log(getString(R.string.gas_error_port_not_online, index.name)); return false
        }
        return if (protocolMode) bindProtocol(index, limit) else bindRaw(index, limit)
    }

    private fun bindProtocol(index: PayloadIndexType, limit: Int): Boolean {
        closeChannel()
        if (bridge?.port == index && bridge?.maxPacketBytes == limit && bridge?.isReady == true) {
            selected = index
            observeBridge(bridge!!)
            return true
        }
        closeProtocol()
        return try {
            val opened = app.openBridge(index, limit)
            bridge = opened
            selected = index
            observeBridge(opened)
            log(getString(R.string.gas_protocol_listening, index.name, limit))
            true
        } catch (error: IllegalArgumentException) {
            log(getString(R.string.gas_protocol_error, error.message ?: "invalid configuration"))
            false
        }
    }

    private fun bindRaw(index: PayloadIndexType, limit: Int): Boolean {
        closeProtocol()
        if (selected == index && channel?.maxBytes == limit) return true
        closeChannel()
        val manager = PayloadCenter.getInstance().payloadManager[index] ?: return false
        selected = index
        val token = generation
        channel = RawPayloadChannel(manager, limit,
            { bytes ->
                if (queued.incrementAndGet() > 32) {
                    queued.decrementAndGet(); dropped.incrementAndGet()
                } else handler.post {
                    queued.decrementAndGet()
                    if (started && generation == token) {
                        val hex = RawPayloadChannel.hex(bytes)
                        log("RX len=${bytes.size} hex=$hex dropped=${dropped.get()}")
                    }
                }
            }, { error -> handler.post { if (generation == token) log(error) } })
        log(getString(R.string.gas_listening, index.name, limit))
        return true
    }

    private fun observeBridge(value: PsdkBridge) {
        stateJob?.cancel()
        eventJob?.cancel()
        stateJob = scope.launch {
            value.state.collect { state ->
                log(getString(R.string.gas_protocol_state, state.stage.name, state.reason?.name ?: "-"))
            }
        }
        eventJob = scope.launch {
            value.observeAll().collect { event ->
                log(getString(R.string.gas_protocol_event, event.cmd, event.params.toString()))
            }
        }
    }

    private fun send() {
        if (!bind()) return
        if (binding.cbProtocolMode.isChecked) {
            val active = bridge ?: return
            scope.launch {
                val result = active.request(CMD_READ_DEVICE_INFO)
                log(getString(R.string.gas_protocol_result, describe(result)))
            }
            return
        }
        val bytes = try {
            if (binding.cbHexInput.isChecked) RawPayloadChannel.parseHex(binding.etPayloadInput.text.toString())
            else binding.etPayloadInput.text.toString().toByteArray(Charsets.UTF_8)
        } catch (error: IllegalArgumentException) {
            log(error.message ?: "Invalid Hex"); return
        }
        val token = generation
        channel?.send(bytes,
            {
                handler.post {
                    if (generation != token) return@post
                    val hex = RawPayloadChannel.hex(bytes)
                    log("SDK submitted len=${bytes.size} hex=$hex; delivery unconfirmed")
                }
            },
            { error -> handler.post { if (generation == token) log("TX failed: $error") } })
    }

    private fun describe(result: RequestResult): String = when (result) {
        is RequestResult.Response -> "Response code=${result.message.code} params=${result.message.params}"
        is RequestResult.SdkFailure -> "SdkFailure ${result.error}"
        is RequestResult.InvalidRequest -> "InvalidRequest ${result.code} ${result.detail ?: ""}"
        is RequestResult.NotReady -> "NotReady ${result.reason}"
        is RequestResult.ProtocolFailure -> "ProtocolFailure ${result.code} ${result.detail ?: ""}"
        RequestResult.Timeout -> "Timeout"
        RequestResult.Disconnected -> "Disconnected"
        RequestResult.ChannelClosed -> "ChannelClosed"
        RequestResult.Cancelled -> "Cancelled"
    }

    private fun log(message: String) {
        if (lines.size == MAX_LOG_LINES) lines.removeFirst()
        lines.addLast(message)
        binding.tvLog.text = lines.joinToString("\n")
        binding.scrollLog.post { binding.scrollLog.fullScroll(View.FOCUS_DOWN) }
    }

    private fun closeChannel() {
        generation++
        channel?.close()
        channel = null
        selected = null
    }

    private fun closeProtocol() {
        stateJob?.cancel()
        eventJob?.cancel()
        stateJob = null
        eventJob = null
        bridge = null
        selected = null
        app.closeBridge()
    }

    private fun detachPorts() {
        stateJob?.cancel()
        eventJob?.cancel()
        stateJob = null
        eventJob = null
        channel?.close()
        channel = null
        if (!binding.cbProtocolMode.isChecked) selected = null
        basicListeners.forEach { (manager, listener) -> manager.removePayloadBasicInfoListener(listener) }
        basicListeners.clear()
        connected.clear()
    }

    override fun onStop() {
        started = false
        detachPorts()
        Log.d(TAG, "Debug page stopped")
        super.onStop()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
