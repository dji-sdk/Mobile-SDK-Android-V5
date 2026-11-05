# Demo Server spec

## 目标

提供可持久化、开箱即用的本地任务服务，完整演示“Web 创建并下发任务 → APP 拉取与执行 → 状态回传 → 航线及媒体归档”。服务和管理台均无第三方运行时依赖，前端不需要安装依赖或构建。

## 架构

```text
浏览器 ── Admin API ─┐
                     ├── Node HTTP Server ── DATA_DIR
遥控器 APP ─ v1 API ─┘                       ├── 状态数据
                                             ├── KMZ 航线
                                             └── 回传媒体
```

- `server/server.mjs`：HTTP 路由、APP API、Admin API 和静态文件服务。
- `server/store.mjs`：任务、设备、时间线和文件元数据的持久化存储。
- `server/web/`：原生 HTML/CSS/JS 单页管理台，由服务根路径 `/` 提供，使用相对地址调用 Admin API。
- 默认端口 `8080`；容器中使用 `HOST=0.0.0.0`、`DATA_DIR=/app/runtime`。
- Compose 默认绑定 `127.0.0.1:8080`，并用命名卷 `flight-task-demo-data` 挂载到 `/app/runtime`。
- 全新数据目录仅预置草稿 `FLIGHT-TASK-DEMO-001`（天空之城）：`generate_test.kmz`、`CLOUD_PROVIDED`、50m、航线模拟器验证；启动时将配置的 seed KMZ 复制到任务航线路径。

## APP API

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/health` | 容器及服务健康检查 |
| GET | `/api/v1/jobs` | 拉取分配给当前设备的任务快照 |
| GET | `/api/v1/jobs/{jobId}` | 任务详情 |
| PUT | `/api/v1/jobs/{jobId}/status` | 更新任务状态并追加时间线 |
| GET | `/api/v1/routes/{jobId}.kmz` | 下载任务 KMZ |
| GET | `/api/v1/asset-types` | 资源类型目录 |
| POST | `/api/v1/upload-sessions` | 创建资源分片上传会话 |
| PUT | `/api/v1/upload-sessions/{id}/parts/{index}` | 上传资源分片 |
| POST | `/api/v1/upload-sessions/{id}/complete` | 校验 MD5 并归档资源 |
| GET | `/api/v1/jobs/{id}/assets` | 查询任务已归档资源 |

## Admin API

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/admin/devices` | 查询曾连接服务的设备，供任务分配下拉框使用 |
| GET | `/api/admin/jobs` | 查询全部任务及状态、航线摘要、时间线 |
| POST | `/api/admin/jobs` | 创建草稿任务 |
| PUT | `/api/admin/jobs/{id}` | 编辑草稿任务 |
| DELETE | `/api/admin/jobs/{id}` | 删除非活动任务及其航线、媒体和未完成会话 |
| PUT | `/api/admin/jobs/{id}/route` | 上传 raw KMZ；原始文件名由 `X-File-Name` 传递 |
| POST | `/api/admin/jobs/{id}/dispatch` | 校验设备和航线后下发任务 |
| POST | `/api/admin/jobs/{id}/withdraw` | 将待接收任务撤回草稿，保留原分配以便编辑 |
| GET | `/api/admin/jobs/{id}/assets` | 查询指定任务的同步文件 |
| GET | `/api/admin/assets` | 查询全部同步文件 |
| GET | `/api/admin/assets/{assetId}/content?jobId=...` | 以复合键获取媒体原始内容，供 JPEG 预览及文件下载 |
| GET | `/api/admin/routes/{jobId}/content` | 下载指定任务的 KMZ 航线 |

JSON 接口失败时返回对应 HTTP 状态码及 `{ "error": "可读错误信息" }`。文件接口返回正确的 `Content-Type` 和下载文件名。

所有 APP API 请求必须携带 `X-RC-SN`。任务使用 `targetRcSn`、上传会话使用 `ownerRcSn`、设备记录使用 `rcSn`；三者均为单一遥控器 SN，不支持广播和旧字段别名。APP 只能访问本机任务及其航线、资源和上传会话。

KMZ 航线仅由 Admin API 发布。APP 上传会话拒绝 `typeId=route`；媒体查询不返回旧版本遗留的 route 上传资源，航线统一从任务的 route 接口展示和下载。

任务状态流为 `DRAFT → DISPATCHED（待接收）→ ACCEPTED（待执行）→ IN_PROGRESS → FINALIZING → COMPLETED/CANCELLED`。禁止 `IN_PROGRESS → COMPLETED` 跳过收尾；终态不接受新上传。仅 `DISPATCHED` 可由管理员撤回至 `DRAFT`；撤回保留 `targetRcSn`，草稿可改派，`ACCEPTED` 后不可改派。同一 `targetRcSn` 最多有一个 `IN_PROGRESS` 或 `FINALIZING` 任务。同状态 APP 更新幂等，不重复追加时间线。

删除是不可恢复的管理操作：`DRAFT/DISPATCHED/ACCEPTED/COMPLETED/CANCELLED` 可删除，`IN_PROGRESS/FINALIZING` 返回 409，需先取消或完成。删除同步移除任务、资源元数据、上传会话、KMZ、会话分片和已归档媒体。

## Web 管理台

- 单页包含“任务工单”和“媒体文件”两类导航，桌面优先并支持窄屏。
- 任务页提供统计、搜索、状态筛选、新增/编辑、KMZ 上传、遥控器 SN 分配、保存草稿、下发、撤回、删除非活动任务和详情抽屉。
- 详情展示任务字段、航线下载、文件同步统计和状态时间线。
- 媒体页提供搜索、类型/状态筛选、按任务折叠的文件目录、照片点击放大（灯箱）及照片/航线下载。
- 媒体页按任务折叠：每个任务是一行“文件夹”条目，展示名称（优先任务名，否则编号）、状态、照片数与容量；默认收起，点击行头才展开缩略图网格，展开状态在轮询刷新间保留。无归属文件归入“未关联任务”；存在搜索/筛选条件时自动展开命中分组。
- 请求均使用原生 `fetch`；加载、空数据和错误状态必须可见，不静默失败。
- 页面可见时每 5 秒刷新任务和设备，恢复可见时立即刷新；设备在线阈值为 30 秒。航线仅在 `routeStatus=CLOUD_PROVIDED` 时显示为已配置。
- 媒体页保留 5 秒元数据轮询，但按 `(jobId, assetId, md5/size)` 缓存 Blob URL；未变化照片不重复 GET 或闪烁，仅新增、变更、删除时更新缓存。

## 部署与连接

```bash
cd open-pilot-flighttask-sample/server
docker compose up -d --build
adb reverse tcp:8080 tcp:8080
```

- 管理台：`http://127.0.0.1:8080/`
- APP：通过 USB reverse 访问 `http://127.0.0.1:8080`。
- 局域网模式需显式将 Compose 端口改为 `8080:8080`，APP 使用电脑局域网 IP。

## 验收标准

1. `docker compose up -d --build` 后健康检查通过，管理台无需构建即可打开。
2. 首屏清楚展示加载态；Admin API 失败显示可读错误；无数据时展示引导空态。
3. Web 可创建草稿、编辑、选择遥控器 SN 与 KMZ 并下发或撤回；APP 仅能访问 `targetRcSn` 与 `X-RC-SN` 完全一致的非草稿任务。
4. APP 状态更新后，Web 刷新可看到状态和时间线变化。
5. APP 上传 JPEG 后，任务详情同步统计及媒体页均可见；媒体页按任务分组，点击 JPEG 可放大预览并下载原图。
6. 航线可从任务详情及媒体页下载，下载内容与上传 KMZ 一致。
7. `docker compose restart` 和容器重建后数据仍存在；删除命名卷 `flight-task-demo-data` 后可重置。
8. 管理台在常见桌面宽度和 320px 窄屏下可操作，表格允许横向滚动且页面不溢出崩坏。
9. `node --check web/app.js` 通过，浏览器控制台无初始化语法错误。
