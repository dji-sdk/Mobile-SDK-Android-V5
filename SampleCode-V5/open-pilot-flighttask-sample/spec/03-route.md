# WaylineManager spec

## 需求来源

规划方案 1.2、2.2：下载工单 KMZ、校验、上传飞行器、执行并监听状态。

## SDK 能力

- `WaypointMissionManager.pushKMZFileToAircraft`：上传飞行器并用于执行。
- `pushKMZFileToRemote`：导入 Pilot 航线库，与上传飞行器语义不同。
- `getAvailableWaylineIDs`：本地解析 KMZ 航段。
- `startMission / pauseMission / resumeMission / stopMission`。
- `WaypointMissionExecuteStateListener / WaylineExecutingInfoListener`。

## 模块契约

- `prepare(job)`：下载或读取本地 KMZ，校验、解析并上传飞行器。
- `importToPilot(file)`：按需导入 Pilot。
- `start / pause / resume / resumeFrom / stop / queryBreakPoint`。
- `state / progress / error`：供 VM 观察。

## 实现约束

- 文件扩展名必须为小写 `.kmz`。
- start/stop 使用去掉扩展名的文件名。
- 命令回调只表示命令接收结果，持续状态以监听器为准。
- `WaylineExecutingInfo` 只有当前航点索引；准备 KMZ 时用 `WPMZParserManager.getWaylines()` 解析各 wayline 航点数，跨 wayline 累加后提供真实 `当前/总数` 进度。
- `release()` 只移除自身监听器，不调用 clearAll。

## 验收标准

云端/预置 KMZ 可上传飞行器，开始、暂停、恢复、停止与航点进度均可观察。
