# MediaManager spec

## 需求来源

规划方案 2.2：飞行中捕获当前工单照片，下载到 APP 本地并提供给上传模块。

## SDK 能力

- `CameraKey.KeyNewlyGeneratedMediaFile`：飞行中捕获新照片引用。
- `CameraKey.KeyCustomExpandDirectoryNameSettings`：航线执行前创建当前工单的相机目录后缀。
- `IMediaManager.pullMediaFileListFromCamera`：拉取照片列表。
- `MediaFile.pullOriginalMediaFileFromCamera(offset, listener)`：原图下载和断点恢复。
- `IMediaManager.enable/disable`：直接下载失败时，航线结束后进入回放补拉。

## 首版范围

- 主相机 `LEFT_OR_MAIN`。
- SD 卡 `SDCARD`。
- PHOTO 类型。

## 模块契约

- `beginJob(jobId, onNewMedia)`：注册新媒体监听并关联工单。
- `prepareJobFolder(jobId, callback)`：设置 `UTF8 + POSITION_END + forceCreateFolder` 的工单目录后缀。
- `downloadNewPhotos(jobId, allowPlayback, callback)`：下载尚未落盘的照片。
- `endJob / cancelDownload / deleteLocal / release`。
- `localAssets / mediaReferences / downloadStates / pendingCount`。

## 下载策略

执行顺序为 `beginJob → prepareJobFolder → 记录 startTime/startCursor → 上传并启动航线`。航线结束立即记录 `endTime` 并重置相机目录；在时间窗和索引边界内连续扫描文件列表，连续两次无新增后冻结不可变照片清单。后续下载/上传/重试只处理清单，不吸收飞机执行其他工作产生的新照片。

任务照片筛选使用 `startCursor + startTime/endTime + dirIndex`；新媒体事件用于实时触发和补充候选，文件列表中的 `MediaFile` 是最终下载对象。公开 `IMediaManager` 不支持按文件夹拉取列表，因此先拉完整列表，再在客户端过滤。

飞行中和航线结束后都不进入回放，直接拉文件列表；匹配冻结清单后先串行调用 `pullThumbnailFromCamera` 缓存缩略图，再串行调用 `pullOriginalMediaFileFromCamera` 拉原图，避免共享媒体队列互相抢占。相机忙时最多延迟重试 3 次。失败保留 pending，由用户“重新拉取”继续。原图先写 `.part`，校验大小和 MD5 后原子改名。

缩略图缓存为 `flight-assets/<jobId>/.thumbs/<dirIndex>-<fileIndex>.jpg`，使用 `(jobId, dirIndex, fileIndex)` 关联冻结清单、缩略图、本地原图与上传状态；缩略图失败不阻断原图。

执行窗口、冻结清单、新媒体引用、工单目录 `dirIndex` 与已下载 Asset 一并写入 `flight-assets/index.json`，进程重启后仍能恢复工单归属。航线完成后等待稳定扫描再冻结清单，覆盖相机最后一张照片的延迟落盘。

## 验收标准

航线启动前目录设置成功；新媒体 Key 能关联当前 `(jobId, dirIndex)`；断点下载后生成完整 Asset；重启后从本地索引恢复照片与目录归属。
