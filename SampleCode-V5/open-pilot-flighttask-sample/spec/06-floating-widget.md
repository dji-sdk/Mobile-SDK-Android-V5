# 悬浮窗 spec

## 需求来源

规划方案 3.1 及 `ui-mockup-floating-widget-flight-task.jpg`。

## SDK 能力

使用 `RemoteViewHelper.floatingBoxHelper` 和开源 RemoteView 自定义组件。图片先上传 Pilot，再设置给 RemoteImage。

## 状态

`IDLE / JOB_LIST / PREPARING / EXECUTING / SYNCING / ERROR / DONE`。

业务流：默认空闲 → 上一个/下一个选择工单 → 确认任务 → 飞行准备 → 开始执行（上传航线并启动）→ 航线进度 → 数据同步 → 失败或完成。

## 实现约束

- TaskVM 管理显示状态，RemoteView 只负责渲染。
- 所有状态容器一次性创建，后续仅切换 VISIBILITY，避免闪动。
- 标题栏仅展示“任务管理”，不显示跳转箭头；完整详情通过各状态内的“查看任务详情”进入插件 APP。
- 工单信息使用独立深色卡片，按序号、类型、要求、截止分行展示；航点、高度和速度只来自 KMZ，不在任务表单中重复维护。
- 执行态“已拍照 / 已上传”使用两个等权重列；已拍照取新媒体事件数，已上传取照片上传完成数，不用已下载文件数冒充拍照数。
- 航线进度从 KMZ 解析每条 wayline 的航点总数，SDK 的零基航点索引转换为 `1/总数` 后更新 `PROGRESS_MAX / PROGRESS` 并 `syncToRemote()`。
- UI 主状态以服务端工单状态为准：`IN_PROGRESS` 不回退准备态，`FINALIZING` 始终停留同步态，`COMPLETED/CANCELLED` 固定展示结算页，直到用户显式切换任务。
- 同步态先展示照片拉取 `已下载/已拍摄`，全部落盘后进度条切换为照片上传 `已上传/总数`。
- 同步统计只计算照片，不把已上传 KMZ 算入分母或字节数。同步页提供“重新拉取”和“稍后处理”；失败后停止无休止的回放轮询，用户可按冻结清单继续。
- “重新拉取”通过 Toast 提示已触发、成功或失败原因；当前冻结照片已不在相机列表时明确提示，无需盲目重复点击。
- 悬浮球提醒态和跨插件提醒中心本期不做。

## 当前实现

七个状态容器均已实现，并直接观察 TaskManager、WaylineManager、MediaManager 和 AssetUploadManager 的状态更新。
