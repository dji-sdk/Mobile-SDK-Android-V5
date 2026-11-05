# 飞行任务管理演示后端

零前端依赖、零构建步骤的《飞行任务管理》演示服务。Node 服务同时提供 APP API、Admin API 和静态管理台（源码位于 `web/`，访问路径为 `/`），任务、航线及回传文件保存在 `DATA_DIR`。

全新数据目录会创建且仅创建草稿任务 `FLIGHT-TASK-DEMO-001`（天空之城），并将 `data/FLIGHT-TASK-DEMO-001.kmz` 复制为其运行时航线 `generate_test.kmz`。

## Docker 部署

在本目录运行：

```bash
docker compose up -d --build
docker compose ps
curl http://127.0.0.1:8080/health
```

镜像使用 Node 24 Alpine，容器监听 `0.0.0.0:8080`。Compose 默认仅发布到电脑的 `127.0.0.1:8080`，不会向局域网暴露服务。

浏览器打开：

```text
http://127.0.0.1:8080/
```

## 连接遥控器

### USB（推荐）

用 USB 连接遥控器，确认 `adb devices` 能看到设备，再执行：

```bash
adb reverse tcp:8080 tcp:8080
adb reverse --list
```

APP 服务地址保持 `http://127.0.0.1:8080`。`adb reverse` 在遥控器重启、USB 重连后可能失效，失效时重新执行即可。

### 局域网备选

电脑和遥控器处于同一局域网时，可将 `docker-compose.yml` 端口改为 `8080:8080`，重新运行 `docker compose up -d`；然后在 Android 工程根目录的 `gradle.properties` 将 `FLIGHT_TASK_SERVER_URL` 改为电脑局域网 IP（例如 `http://192.168.1.20:8080`）并重新编译 APP。同时确认系统防火墙允许 TCP 8080；不要将 Demo 服务直接暴露到公网。

## Web 下发任务

1. 打开管理台，进入“任务工单”，点击“新增任务”。
2. 填写任务名称、截止时间和作业要求，从“遥控器 SN”下拉框选择已连接过服务的遥控器；在线状态按最近 30 秒请求计算。
3. 选择 `.kmz` 航线文件。
4. 点击“保存草稿”可稍后继续编辑；点击“保存并下发”会保存任务、上传航线并下发。
5. 已有草稿可在表格中点击“下发”；待接收任务可撤回为草稿并重新分配，APP 接收后不可改派。
6. APP 拉取并执行任务后，在管理台刷新任务；回传照片和航线可在“媒体文件”中按任务分组查看，点击照片放大预览，并可下载原图。
7. 非执行中任务可从列表永久删除；任务、KMZ、回传媒体及未完成上传会话会一并清理。执行中或同步中的任务需先取消或完成。

## 数据保留与重置

Compose 使用命名卷 `flight-task-demo-data` 挂载到容器 `/app/runtime`。执行 `docker compose restart`、升级镜像或重建容器不会丢失数据，也避免 Linux 宿主机 bind mount 权限问题。

重置全部 Demo 数据：

```bash
docker compose down
docker volume rm flight-task-demo-data
docker compose up -d
```

该操作会删除任务、设备登记、航线和回传媒体，执行前请确认不需要保留。

## Admin API

所有 APP API 请求必须携带 `X-RC-SN`。任务、上传会话和设备分别以 `targetRcSn`、`ownerRcSn`、`rcSn` 标识唯一遥控器，不支持广播或旧字段别名。同一遥控器最多有一个 `IN_PROGRESS`/`FINALIZING` 任务。

- `GET /api/admin/devices`：管理台设备列表。
- `GET|POST /api/admin/jobs`、`PUT /api/admin/jobs/{id}`：查询、新建和编辑草稿任务。
- `DELETE /api/admin/jobs/{id}`：永久删除非执行中任务及其航线、媒体和上传会话。
- `PUT /api/admin/jobs/{id}/route`：上传原始 KMZ，文件名放在 `X-File-Name`。
- `POST /api/admin/jobs/{id}/dispatch`：下发草稿任务。
- `POST /api/admin/jobs/{id}/withdraw`：将 `DISPATCHED` 任务撤回为草稿并保留原 `targetRcSn`。
- `GET /api/admin/jobs/{id}/assets`、`GET /api/admin/assets`：查询任务文件或全部文件。
- `GET /api/admin/assets/{assetId}/content?jobId=...`：预览或下载文件（复合键避免跨任务同名冲突）。
- `GET /api/admin/routes/{jobId}/content`：下载任务航线。
