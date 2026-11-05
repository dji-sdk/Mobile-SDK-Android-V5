# psdk-bridge

可复用的 Pilot–PSDK 通信库（Android library）：业务只关心命令和业务数据，不需要自己持有 SDK
listener、分配请求编号、处理分片/超时或写编解码器。

本库提供 Android 侧通信逻辑。载荷侧 PSDK 应用需由设备开发者按通信协议实现，本次发布不包含设备端 C 库或程序。

本库通过 MSDK 提供的载荷接口通信。

- Gradle：`:open-pilot-gasdetection-sample:psdk-bridge`
- 包名：`dji.open.pilot.psdk`
- 默认载荷编码：**JSON**（无需 schema、无需注册）

## 1. 接入

```kotlin
implementation project(':open-pilot-gasdetection-sample:psdk-bridge')
```

应用自己完成 SDK 初始化/注册，并把权威连接状态交给 bridge：

```kotlin
val connections = PsdkConnections(
    sdkRegistered = sdkRegisteredFlow,   // StateFlow<Boolean>
    pilotConnected = pilotConnectedFlow,
    aircraftConnected = aircraftConnectedFlow,
)
```

`PsdkBridge` 只在 SDK 注册、Pilot 连接、飞行器连接、负载 `isConnected` 且 `isFeatureOpened` 后进入
`READY`；READY 前请求返回 `RequestResult.NotReady(reason)`。

## 2. 定义命令和业务数据

命令编号必须和负载端约定一致：

```kotlin
object PayloadCommands {
    const val SET_SAMPLING_CONFIG = 1
    const val READ_DEVICE_INFO = 2
    const val TELEMETRY = 3
}

data class SamplingConfig(
    val intervalMs: Int = 1000,
    val enabled: Boolean = false,
)

data class DeviceInfo(
    val serialNumber: String,
    val firmwareVersion: String,
    val temperature: Float,
)
```

## 3. 创建 bridge

一个工程固定一个 port 和一种编码。`getOrCreate` 内部先查端口缓存：已存在就返回，不存在才创建。
**推荐在业务模块真正需要收发时再创建**（例如页面/ViewModel 初始化或首次点击「开始监听」），
不要在 `Application.onCreate` 提前创建：

```kotlin
val bridge = PsdkBridge.getOrCreate(
    port = PayloadIndexType.PORT_1,   // 必传，EXTERNAL 拒绝
    maxPacketBytes = 128,             // 必传，整帧上限 16..255
    connections = connections,
)
```

创建的前提是 SDK 已注册、`PayloadCenter` 中该 port 的 manager 已存在，否则抛 `IllegalArgumentException`；
创建后由 bridge 内部跟踪连接状态，未 `READY` 时请求返回 `RequestResult.NotReady`，不需要等就绪再创建。
同一参数重复调用会拿到同一实例；配置不一致会拒绝，避免静默替换共享通道。
配置包括编码、包长、超时、消息上限、两种 dispatcher 和三个连接 StateFlow；重复获取时应复用同一组状态源。

创建时机只影响「能收到多早的事件」：**bridge 不缓存创建前的事件**（见「5. 事件监听」）。

## 4. 发请求

```kotlin
lifecycleScope.launch {
    when (val result = bridge.request(
        PayloadCommands.SET_SAMPLING_CONFIG,
        SamplingConfig(intervalMs = 2000, enabled = true),
    )) {
        is RequestResult.Response -> {
            val code = result.message.code       // 设备 status，0 成功
            val params = result.message.params   // Map<String, Any?>
        }
        is RequestResult.InvalidRequest -> Unit   // 未发送：cmd/参数/编码错误
        is RequestResult.NotReady -> Unit         // SDK/Pilot/飞行器/负载/功能不可用
        is RequestResult.SdkFailure -> Unit       // 保留 IDJIError
        is RequestResult.ProtocolFailure -> Unit  // 编解码失败或 SDK 调用异常
        RequestResult.Timeout -> Unit
        RequestResult.Disconnected -> Unit
        RequestResult.ChannelClosed -> Unit
        RequestResult.Cancelled -> Unit
    }
}
```

也可以直接传 `Map`：

```kotlin
bridge.request(
    PayloadCommands.SET_SAMPLING_CONFIG,
    mapOf("intervalMs" to 2000, "enabled" to true),
)
```

读取设备信息：

```kotlin
val result = bridge.request(PayloadCommands.READ_DEVICE_INFO)
if (result is RequestResult.Response) {
    val info = DeviceInfo(
        serialNumber = result.message.params["serialNumber"] as? String ?: "",
        firmwareVersion = result.message.params["firmwareVersion"] as? String ?: "",
        temperature = (result.message.params["temperature"] as? Number)?.toFloat() ?: 0f,
    )
}
```

### 超时与回调

```kotlin
val bridge = PsdkBridge.getOrCreate(
    port = PayloadIndexType.PORT_1,
    maxPacketBytes = 128,
    connections = connections,
    options = PsdkOptions(
        requestTimeoutMillis = 3000,
        maxMessageBytes = 1024,
    ),
)

val handle = bridge.request(
    PayloadCommands.SET_SAMPLING_CONFIG,
    SamplingConfig(enabled = true),
) { result -> /* Main dispatcher */ }

handle.cancel() // 只取消本地等待，不向设备发送取消
```

超时从登记请求开始计算，覆盖排队、分片提交和等待响应；SDK 提交成功不代表设备已执行。

## 5. 事件监听

推荐按命令实现 `PsdkEventHandler` 并注册，由 bridge 按 `cmd` 分发，业务侧不需要自己 collect 和过滤：

```kotlin
class TelemetryHandler(
    private val onTelemetry: (progress: Int, state: Int) -> Unit,
) : PsdkEventHandler {
    override val cmd: Int = PayloadCommands.TELEMETRY

    override fun onEvent(event: PsdkEvent) {
        val params = event.params // Map<String, Any?>
        val progress = (params["progress"] as? Number)?.toInt() ?: return
        val state = (params["state"] as? Number)?.toInt() ?: return
        onTelemetry(progress, state)
    }
}

val handler = TelemetryHandler { progress, state -> /* 更新 UI */ }
val registration = bridge.registerEventHandler(handler)
registration.close()                       // 注销本次注册
bridge.unregisterEventHandler(handler)     // 或按对象身份注销该 handler 的全部注册
```

- 只有 `event.cmd == handler.cmd` 的事件才会回调；同一命令可注册多个 handler，各自独立接收。每个注册保持事件到达顺序，不保证不同 handler 之间的全局调用顺序。
- 回调在 `PsdkOptions.callbackDispatcher`（默认主线程）上按到达顺序执行；单个 handler 抛异常只记诊断，不影响其他 handler。
- 每个注册有独立的有界队列（64 条）；队列满时终止该注册并报告 `ListenerOverflow`，不影响其他 handler 或请求响应。回调请保持简短，耗时操作自行切后台。
- **不缓存、不回放**：注册前、注销后到达的事件直接丢弃，不会补发；没有任何 handler/listener 时事件同样丢弃。
  仅 `state` 是保留最新值的 `StateFlow`；内部事件缓存只用于 messageId 去重，不保存业务数据。
- 持久接收请用 `registerEventHandler`；`observe(cmd)`/`observeAll()` 与 `addListener` 仍保留，适合页面展示类的临时收集：

```kotlin
val job = lifecycleScope.launch {
    bridge.observe(PayloadCommands.TELEMETRY).collect { event -> /* ... */ }
}
val subscription = bridge.addListener(PayloadCommands.TELEMETRY) { event -> /* ... */ }
subscription.close()
```

- 框架不内置远端订阅；需要开关设备上报时，先注册本地 handler，再用业务自己的命令发 `request`。
- 长任务用 payload 中的 `taskId` 关联请求和事件，框架不维护业务任务状态。

## 6. 编码扩展

JSON 是默认编码。后续需要其他编码时，实现 `PayloadCodec` 并在首次创建 bridge 时传入：

```kotlin
interface PayloadCodec {
    val wireEncoding: PayloadEncoding
    fun encode(kind: MessageKind, cmd: Int, params: Map<String, Any?>): ByteArray
    fun decode(kind: MessageKind, cmd: Int, payload: ByteArray): Map<String, Any?>
}
```

```kotlin
val bridge = PsdkBridge.getOrCreate(
    port = PayloadIndexType.PORT_1,
    maxPacketBytes = 128,
    connections = connections,
    options = PsdkOptions(maxMessageBytes = 1024),
    codec = MyCborPayloadCodec(),
)
```

bridge、请求关联、分片、超时和事件分发都不需要改。当前版本只内置 JSON。

## 7. 资源与生命周期

- `bridge` 是应用级资源：页面退出只取消自己的监听，不要关闭其他业务仍在使用的 bridge。
- `close()` 幂等：结束等待、关闭 Flow/callback、清理重组与监听、移除本实例 SDK listener。
- 请求编号按 port 在进程内递增，重连/实例重建不重置；进程重启重新计数。
- 原始字节调试通道如果也要用同一端口，必须先 `close()` 对应 bridge，避免同端口双所有者。

## 8. 设备端对接约定

设备端需配合 Android 侧实现以下通信契约：

- 命令编号 `cmd` 与业务参数结构保持一致，默认业务编码为 JSON。
- 支持 `REQ` 请求、`RSP` 响应及按业务需要上报的 `EVT` 事件；响应使用对应请求的 `messageId` 与 `cmd`，并携带 `status`。
- 使用 v1 帧格式：基础帧头为 `control + messageId(u32) + cmd(u16)`，共 7 字节；整数按小端编码，分片及响应状态使用相应扩展字段。
- 双方约定单包上限，设备端处理相同的分片信息和消息关联规则。

帧头、字段范围和分片校验以 Android 源码 [`FrameCodec.kt`](src/main/java/dji/open/pilot/psdk/protocol/FrameCodec.kt) 为准，JSON 业务体格式见 [`JsonMapCodec.kt`](src/main/java/dji/open/pilot/psdk/protocol/JsonMapCodec.kt)。

气体测量上行使用独立的原始紧凑 JSON 解码链路，不套本库帧头；接入要求见[气体检测 README](../README.md#气体测量上行)。
