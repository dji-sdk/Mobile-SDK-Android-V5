# 飞行任务管理 Demo · 总览 spec

> 需求来源：Confluence《【开源Demo】《飞行任务管理》规划方案》（pageId 825760433）
> 代码方案：Confluence《【开源Demo】《飞行任务管理》代码方案》（pageId 852932355）

## 需求 → SDK 能力总映射

| 需求 | 开放能力 | client 门面 | spec |
|---|---|---|---|
| 连接门控 / 版本徽标 | SDKManager(REMOTE_PILOT) + ServerState | SDKManagerCallback.onRemoteServerStateUpdated | 01-connection.md |
| 工单拉取 / 状态回传 | 无 SDK（REST + 本地） | — | 02-job.md |
| KMZ 导入 Pilot / 航线执行状态 | IWaypointMissionManager | RemoteWaypointMissionManager | 03-route.md |
| 新照片检测 / 下载 | IKeyManager（新媒体文件 Key）+ IMediaManager | KeyManager / MediaDataCenter.mediaManager | 04-asset-pipeline.md |
| 上传队列 / 断点续传 / 多云 | 无 SDK（纯网络） | — | 05-upload-engine.md |
| 悬浮窗八状态 / 指标区块 | IRemoteViewPageManager (FLOATING_BOX) | uxsdk RemoteViewHelper.floatingBoxHelper | 06-floating-widget.md |
| 三 Tab 页面 / 语言联动 | IRemoteServerConfigManager | RemoteServerConfigManager | 07-plugin-activity.md |

## APP 模块

| 模块 | 类型 | 职责 |
|---|---|---|
| CloudManager | 基础 Manager | 所有云端接口、鉴权和 HTTP 通信；不保存业务状态 |
| TaskManager | 业务 Manager | 工单云端交互、全部工单、当前激活工单和状态时间线 |
| TaskVM | 全局 VM | 任务列表/详情 UI、悬浮窗状态和用户操作 |
| WaylineManager | 业务 Manager | KMZ 下载/校验/上传到 Pilot 或飞行器、航线执行 |
| MediaManager | 业务 Manager | 飞行中新照片捕获、飞机照片下载、本地照片索引 |
| AssetUploadManager | 业务 Manager | 按工单管理资源上传队列、进度、暂停/恢复和重试 |
| WaylineTaskVM | 全局 VM | 航线、资源、传输 UI，以及相关操作编排 |

```text
TaskVM ─────────────→ TaskManager ───────→ CloudManager

WaylineTaskVM ──────→ WaylineManager ────→ CloudManager
       │                    │
       │                    └────────────→ MSDK 航线/媒体接口
       │
       ├────────────→ MediaManager ───────→ IMediaManager / KeyManager
       └────────────→ AssetUploadManager → CloudManager
```

## 架构约定（全模块共同遵守）

1. 对齐 MSDK Sample：使用 `data / manager / models / pages / util` 包结构，不设 Facade/Repository。
2. 不使用协程：Manager 对外使用普通方法、`LiveData` 和 `ResultCallback`，调用链直观可读。
3. Manager 不持有、不调用 VM；VM 观察 Manager 状态并调用 Manager 方法。
4. CloudManager 只负责一次云端请求怎么发；轮询、工单状态、上传队列等业务规则留在对应业务 Manager。
5. TaskVM、WaylineTaskVM 挂 `globalViewModelStore`，与 Activity 生命周期无关。
6. 连接门控统一收敛到 `ConnectionVM.onConnected {}`，其他模块不重复监听连接状态。
7. 全链路只认 Job/Asset；照片相关代码只允许出现在媒体采集实现和资源类型目录的一条数据。

## 模块 spec 模板

每个模块 spec 按以下结构编写：需求来源 / 能力推演 / SDK 接口清单 / 模块契约 / 验收标准 / 已知边界。
