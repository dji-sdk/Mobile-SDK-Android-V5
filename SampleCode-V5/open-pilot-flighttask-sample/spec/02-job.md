# TaskManager / CloudManager spec

## 需求来源

规划方案 1.2、2.2：获取云端工单，管理接受/开始/完成状态及时间线。

## 能力推演

- CloudManager 只封装一次 HTTP 请求，不保存业务状态。
- TaskManager 是工单唯一数据源，负责轮询、缓存、当前工单和状态操作。
- 客户端不内置任务数据，所有测试任务均从本地 Demo Server 拉取。

## 模块契约

- CloudManager：`getJobs / updateJobStatus / downloadWayline / getAssetTypes / createUploadSession / uploadPart / completeUpload`
- TaskManager：`init / startPolling / stopPolling / refresh / accept / start / finalize / complete / cancel`
- 所有异步结果使用 `ResultCallback`，不使用协程。
- Pilot 连接后等待 `RemoteControllerKey.KeySerialNumber` 返回非空 RC SN，再初始化云端业务；断开后停止轮询。
- 连接期间每 10 秒拉取一次完整任务快照，进入插件页和状态操作成功后立即刷新。
- 本地缓存按 RC SN 隔离，避免遥控器身份变化时短暂展示其他遥控器的任务。

## 服务端契约

- API 前缀：`/api/v1`
- 遥控器标识：请求头 `X-RC-SN`
- 鉴权：可选 `Authorization: Bearer <token>`
- 任务：`GET /jobs?since=`、`PUT /jobs/{id}/status`
- 上传：`POST /upload-sessions`、预签名分片 URL、`POST /upload-sessions/{id}/complete`
- 状态流：`DISPATCHED（待接收）→ ACCEPTED（待执行）→ IN_PROGRESS → FINALIZING → COMPLETED/CANCELLED`。
- 服务端禁止 `IN_PROGRESS → COMPLETED` 跳步；只有进入 `FINALIZING` 且照片拉取、完整落盘和上传队列全部完成后，客户端才提交 `COMPLETED`。
- `DISPATCHED` 可由用户拒绝为 `CANCELLED`；完成和拒绝都保留结算展示，切换任务才离开。

上传会话响应结构：

```json
{
  "sessionId": "upload-session-id",
  "chunkSizeBytes": 8388608,
  "partUrls": ["https://storage.example.com/presigned-part-1"],
  "uploadedChunks": []
}
```

客户端分片索引从 0 开始，`partUrls[index]` 对应同索引分片。服务端完成接口负责 MD5 校验与归档。

## 验收标准

1. 未取得 RC SN 或无服务器配置时不发起任务请求，也不回退客户端模拟数据。
2. 仅拉取分配给当前 RC SN 的非草稿任务，服务端完整快照替换本地列表。
3. 状态操作仅在云端成功后落本地；同状态重复提交由服务端幂等处理。
4. 重启 APP 后从当前 RC SN 对应的缓存恢复工单。

## 已知边界

- `android-sdk-v5-as/gradle.properties` 默认指向 `http://127.0.0.1:8080`，通过 adb reverse 连接本地 Demo Server；未配置服务地址时明确报错，不回退客户端模拟数据。
