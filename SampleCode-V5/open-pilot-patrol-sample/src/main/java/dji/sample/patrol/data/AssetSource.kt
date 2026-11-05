package dji.sample.patrol.data

/**
    * 资源采集抽象（扩展点）。
    *
    * 本期实现：CameraMediaAssetSource（照片，由 WaylineManager 管理）——
    * 新媒体文件 Key 监听 → 媒体任务下载到插件私有目录 → 构造 Asset 交给 sink。
    * 未来视频/模型/日志 = 新增实现类并注册到 WaylineManager，其余模块零改动。
    */
interface AssetSource {

    val typeId: String

    /** 开始监听：检测到新资源 → 构造 Asset（jobId = activeJob.jobId）交给 sink 入队 */
    fun start(activeJob: Job, sink: (Asset) -> Unit)

    /** 收尾同步：补扫飞行中遗漏（与已上传集合做增量 diff） */
    fun finalScan(activeJob: Job): List<Asset>

    fun stop()
}
