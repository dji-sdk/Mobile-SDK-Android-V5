package dji.sample.patrol.manager

import dji.v5.common.error.IDJIError

/**
 * 返航动作或返航前置失败。
 *
 * 设计意图：与 [WaylineFailure] 同构，把飞控侧错误保留为可判定类型，让上层按 `errorCode`
 * 映射可读文案，而不是直接展示英文内部串。
 *
 * 能力契约：
 * - [errorCode] 可能带模块前缀，上层按 `isError`/包含语义匹配，禁止字符串等值比较；
 * - [sdkError] 保留完整 `IDJIError`（`errorType/errorCode/innerCode/description/hint`）；
 * - 继承 [IllegalStateException]，既有按 `Throwable` 处理的调用点行为不变。
 */
internal class ReturnHomeFailure(
    /** 对外错误码；用于上层做可读文案映射。 */
    val errorCode: String?,
    /** 原始 SDK 错误；为 null 表示失败不是由 `IDJIError` 产生。 */
    val sdkError: IDJIError?,
    message: String,
) : IllegalStateException(message)
