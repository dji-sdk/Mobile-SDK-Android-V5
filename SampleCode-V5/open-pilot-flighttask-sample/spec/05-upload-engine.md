# AssetUploadManager spec

## 需求来源

规划方案 1.2、2.2：按工单上传照片及后续扩展资源，支持进度、暂停、恢复和重试。KMZ 由 Web 发布，插件仅下载并推送飞行器，不进入服务器上传队列。

## 模块契约

- `init / enqueue / pause / resume / retry / cancel / release`。
- `queue / progress` 供 VM 和页面观察。
- 只消费完整落盘且 MD5 匹配的 Asset，不依赖相机或 MediaFile。

## 上传流程

`createUploadSession → 按 partUrls 顺序上传 → completeUpload`。首版使用单线程队列，失败最多自动重试 3 次；队列保存到 `flight-assets/upload-queue.json`。进程恢复后重新申请上传会话，不复用可能过期的预签名 URL。

## 本地服务联调

本地 Demo Server 实现上传会话、分片接收、MD5 校验和文件归档；客户端不再模拟上传成功。

## 验收标准

同一工单资源顺序上传；暂停/恢复/取消有效；重启 APP 后恢复未完成队列。`FINALIZING` 重连时重新扫描媒体、补入已落盘资源并重试失败队列；照片全部拉取和上传完成后 WaylineTaskVM 才提交 `COMPLETED`。
