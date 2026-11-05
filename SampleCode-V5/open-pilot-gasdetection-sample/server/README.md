# 气体检测演示后端

零第三方依赖、零构建步骤的 Node Demo 服务，为《气体检测》开源 Demo 提供任务创建、下发、状态流转与检测记录接收。
服务同时提供 APP API、Admin API 与 `/` 下的静态管理台，数据保存在 `DATA_DIR`。

## 启动

```bash
cd open-pilot-gasdetection-sample/server
node server.mjs
# 或指定数据目录、监听地址与端口
DATA_DIR=/tmp/gas-demo HOST=127.0.0.1 PORT=18082 node server.mjs
# 需要让局域网内遥控器直接访问时（仅限可信网络；Admin API 无鉴权）
DATA_DIR=/tmp/gas-demo HOST=0.0.0.0 PORT=18082 node server.mjs
```

默认监听 `127.0.0.1:18082`，打开 <http://127.0.0.1:18082/> 进入管理台。
端口用 `PORT` 覆盖，监听地址用 `HOST` 覆盖；`HOST=0.0.0.0` 会把无鉴权的管理台暴露到局域网，
只建议在隔离的演示网络中使用。

全新数据目录会创建一个草稿任务 `GAS-DEMO-001`（目标点、SO2/CO2 两条通道、安全边界）。
**任务中的阈值是演示值，不代表任何法规限值。**

## 管理台

管理台与《智能巡检作业》Demo 同构：左侧导航 + 顶栏 + 视图切换，包含「任务工单」与「检测记录」两个视图，
提供任务统计、搜索、状态筛选、任务详情抽屉、任务弹窗表单（支持保存草稿与保存并下发）、
检测记录按任务分组展示、操作 Toast 提示、服务健康状态与 5 秒自动刷新。

## 测试

```bash
npm test        # 等价于 node --test
```

## APP API

所有请求必须携带 `X-RC-SN`，服务按该标识登记设备并隔离任务。

- `GET /api/v1/jobs`：该遥控器名下非草稿任务。
- `GET /api/v1/jobs/{jobId}`：任务详情（含气体配置与时间线）。
- `PUT /api/v1/jobs/{jobId}/status`：状态流转，body `{"status":"ACCEPTED"}`。
- `POST /api/v1/inspections`：上传检测记录，`recordId` 与 `jobId` 必填；按 `recordId` 幂等写入，首次返回 201，重复上传返回已有记录与 200。
- `GET /api/v1/inspections`：查询当前遥控器的检测记录，支持 `?jobId=` 过滤，返回 `{ "records": [...] }`。

状态集合：`DISPATCHED`、`ACCEPTED`、`IN_PROGRESS`、`FINALIZING`、`COMPLETED`、`CANCELLED`、`REJECTED`。
允许的迁移：

```text
DISPATCHED -> ACCEPTED | REJECTED | CANCELLED
ACCEPTED   -> IN_PROGRESS | CANCELLED
IN_PROGRESS-> FINALIZING | CANCELLED
FINALIZING -> COMPLETED | CANCELLED
终态只能保持自身
```

同一遥控器同时只能有一个 `IN_PROGRESS` / `FINALIZING` 任务。

## Admin API

- `GET /api/admin/devices`：设备与在线状态（最近 30 秒有请求视为在线）。
- `GET /api/admin/jobs`：全部任务。
- `POST /api/admin/jobs`：创建草稿。
- `PUT /api/admin/jobs/{jobId}`：编辑草稿（非草稿返回 409）。
- `DELETE /api/admin/jobs/{jobId}`：删除非执行中任务。
- `POST /api/admin/jobs/{jobId}/dispatch`：下发草稿（必须已指定目标遥控器）。
- `POST /api/admin/jobs/{jobId}/withdraw`：撤回已下发任务为草稿。
- `GET /api/admin/inspections`：检测记录只读查询，返回 `{ "records": [...] }`，可按 `?jobId=` 过滤，
  按接收时间倒序；无记录或未命中返回空数组。管理端不提供记录的写入或删除入口。

任务数据结构：

```json
{
  "title": "船舶尾气检测-001",
  "targetRcSn": "RC-A",
  "gas": {
    "target": {"latitude": 22.5, "longitude": 113.9, "altitudeM": 60},
    "channels": [
      {"channelId": "so2", "displayName": "SO2", "unit": "µg/m³", "threshold": 500}
    ],
    "safetyBoundaryM": 200,
    "requirements": "靠近船尾采样",
    "deadline": "2026-12-31T00:00:00.000Z"
  }
}
```

校验失败返回 400 并指出字段路径；状态或状态迁移非法返回 409。

## 限制

- **无鉴权**：仅用于本地演示，不要暴露到公网。
- 测量、告警与船舶关联在插件侧处理，后端接收聚合检测记录；不提供独立的实时测量流或影像文件上传接口。
- 不使用数据库；数据为单个 `state.json`，适合 Demo 规模。
