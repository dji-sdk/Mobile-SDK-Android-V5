package dji.sample.patrol.manager

/**
 * 启动前媒体准备的失败分类。
 *
 * 设计意图：把「哪个阶段失败」变成可机器判定的稳定取值，供上层决定展示文案与是否允许重试，
 * 而不是解析 SDK 错误文本。分类与 [MediaBoundaryCapture] 的步骤一一对应。
 */
internal enum class MediaBoundaryFailureCategory {
    /** 前置条件不满足：连接、地面状态、相机可用性或拍摄状态不符合要求。 */
    PREREQUISITE,

    /** 未能进入媒体管理模式（回放）。 */
    ENTER,

    /** 照片列表拉取失败，或列表终态不是成功的 `UP_TO_DATE`。 */
    FETCH,

    /** 采集结束后未能确认退出媒体管理模式。 */
    EXIT,

    /** 起始边界持久化失败。 */
    PERSISTENCE,

    /** 被断连、任务切换或管理器销毁取消。 */
    CANCELLED,
}

/**
 * 带稳定分类的启动前媒体准备失败。
 *
 * 设计意图：原始 `IDJIError`（或本地约束错误）作为 `cause` 完整保留，
 * 上层既能用 [category] 做分支，也能在日志里拿到完整的 `errorCode + description`。
 * 清理阶段自身的失败通过 {@link Throwable#addSuppressed} 附加，不覆盖原始失败。
 */
internal class MediaBoundaryFailure(
    /** 失败发生阶段对应的稳定分类。 */
    val category: MediaBoundaryFailureCategory,
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
