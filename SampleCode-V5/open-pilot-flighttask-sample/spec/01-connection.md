# connection 模块 spec

> 需求来源：规划方案 3.2.1（连接状态管理）、3.2.2（多语言联动）
> 可拷贝单元：`models/ConnectionVM.kt`（全局级）

## 能力推演

- 插件与 Pilot 跨进程连接是一切开放能力的前提 → MSDK 以 `SDKLinKMode.REMOTE_PILOT` 初始化，
  连接状态经 `SDKManagerCallback.onRemoteServerStateUpdated(ServerState)` 回调。
- 需求要求「业务调用门控」：全部业务初始化收敛到 CONNECTED 后触发 → ConnectionVM 提供
  `onConnected(block)` 注册点，Application 组装时挂接（悬浮窗渲染、上传引擎、采集监听）。
- `INCOMPATIBLE` 是提示性状态、不断链（接口照常可调但功能不保障），只驱动顶部徽标展示，不做拦截。

## SDK 接口清单

| 接口 | 用途 |
|---|---|
| `SDKManager.getInstance().init(context, InitSettings(REMOTE_PILOT), callback)` | 初始化，pluginAppConfig 带插件标题与入口 Activity |
| `SDKManager.getInstance().registerApp()` | INITIALIZE_COMPLETE 后注册 |
| `callback.onRemoteServerStateUpdated(state)` | 四态：UNINITIALIZED / CONNECTED / DISCONNECT / INCOMPATIBLE |
| `RemoteLangUtils.init(context)` | CONNECTED 后初始化 uxsdk 多语言（悬浮窗 ResId 文案依赖） |

## 模块契约

- `serverState: LiveData<ServerState>` —— 顶部徽标/其他模块订阅
- `isRegistered: LiveData<Boolean>`
- `onConnected(block)` —— 门控注册点；已连接时立即执行

## 验收标准

1. 真机装 Pilot + 插件：Pilot 插件列表出现「Flight Task Management」
2. 打开插件，顶部徽标随 Pilot 启停在「已连接/未连接」间切换
3. logcat 过滤 `ConnectionVM`：可见 ServerState changed 日志

## 已知边界

- INCOMPATIBLE 不断链，防崩溃靠契约序列化设计，与本模块无关
- 语言联动的悬浮窗部分由 uxsdk RemoteViewHelper 内建 ResId 机制承载，本模块只负责 RemoteLangUtils.init
