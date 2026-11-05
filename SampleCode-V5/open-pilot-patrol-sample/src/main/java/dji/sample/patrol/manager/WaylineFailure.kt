package dji.sample.patrol.manager

import dji.v5.common.error.IDJIError

/**
 * 航线/飞控侧的启动或执行失败。
 *
 * 设计意图：把「失败来自航线侧」这件事变成可判定的类型，让上层能把原始错误码映射成可读文案，
 * 而不是把 `errorCode: description` 这类原始串直接展示给用户。
 *
 * 能力契约：
 * - [errorCode] 是对外错误码，可能带模块前缀，上层按后缀/包含语义匹配，不要做字符串等值比较；
 * - [sdkError] 保留完整 `IDJIError`（`errorType/errorCode/innerCode/description/hint`）供日志排查；
 * - 继承 [IllegalStateException]，因此既有按 `Throwable` 处理该错误的调用点行为不变。
 */
internal class WaylineFailure(
    /** 对外错误码；用于上层做可读文案映射。 */
    val errorCode: String?,
    /** 原始 SDK 错误；为 null 表示失败不是由 `IDJIError` 产生。 */
    val sdkError: IDJIError?,
    message: String,
) : IllegalStateException(message)
