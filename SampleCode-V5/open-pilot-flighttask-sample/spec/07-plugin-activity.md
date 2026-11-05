# PluginActivity spec

## 需求来源

规划方案 3.2：唯一全屏 Activity，任务/资源/传输三个 Tab，顶部常驻连接徽标。

## VM 分工

- TaskVM：任务列表、任务详情、悬浮窗状态。
- WaylineTaskVM：航线、媒体下载、资源和上传进度。
- ConnectionVM：插件连接状态。

## 页面映射

- 任务 Tab：TaskManager.jobs。
- 资源 Tab：MediaManager.localAssets。
- 传输 Tab：AssetUploadManager.queue/progress。

## 当前实现

- 任务：按状态分组的工单列表 + 任务/航线/同步详情；接受、开始执行等任务操作只在浮窗完成，APP 页面只读。
- APP 与浮窗共享 `TaskVM.selectedJobId`：APP 点击待接收任务会切换浮窗工单，浮窗上一个/下一个也会同步 APP 详情选择；执行中的任务始终优先占用浮窗。
- 资源：按任务分组的全部/照片/航线筛选 + 网格文件列表 + 缩略图/原图预览与元数据；只有 `FINALIZING` 未落盘照片显示“待拉取”，取消/未结束任务不展示占位项；上传队列状态覆盖本地 DETECTED 状态。
- `FINALIZING` 任务组提供“继续拉取并上传”，与浮窗共用恢复入口；点击照片仅切换卡片选中态和右侧详情，不重建 Grid 或改变滚动位置。
- 网络传输：照片等采集资源的上传队列 + 进度、分片、会话信息 + 暂停/恢复/重试；明确过滤 Web 下发的 KMZ。
- 三页均采用横屏列表-详情双栏，顶部连接徽标跨 Tab 常驻。
- 左上角按钮通过 NavigateManager 返回 Pilot，不走普通 Activity 返回栈；右上角分别显示 Pilot IPC 和业务服务 `/health` 状态。
- Activity 使用沉浸模式隐藏系统手势导航条，窗口重新获得焦点时再次应用。
