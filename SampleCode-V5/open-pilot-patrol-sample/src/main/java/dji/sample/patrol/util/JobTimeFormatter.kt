package dji.sample.patrol.util

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * 任务详情区的时间格式化工具（纯函数）。
 *
 * 设计意图：把「服务端时间串 / 事件时间戳 → 用户可读文案」的转换从 Fragment 中抽出来，使其不依赖
 * Android 框架与 Looper，可直接用 JVM 单测覆盖（含跨时区与异常输入）；同时集中处理服务端契约的
 * 形态兼容与降级，避免把 ISO-8601 原串或 epoch 数字暴露给用户。
 *
 * 约束：`minSdk 24` 不可用 `java.time`（需 API 26 或 core library desugaring），因此使用
 * `SimpleDateFormat`，与工程既有 `PointFragment` 的写法一致。`SimpleDateFormat` 非线程安全，
 * 本类不共享实例，每次调用新建。
 */
object JobTimeFormatter {

    /** 详情区「截止时间」的展示格式：`2027-10-09 14:19`。 */
    private const val DISPLAY_DATE_TIME = "yyyy-MM-dd HH:mm"

    /** 时间线事件的展示格式：`10-09 14:19`（含日期，跨天事件仍可区分）。 */
    private const val DISPLAY_EVENT_TIME = "MM-dd HH:mm"

    /** 服务端时间串按 UTC 解析（`server/seed.mjs` 下发 `Z` 结尾的 ISO-8601）。 */
    private const val SERVER_TIME_ZONE = "UTC"

    /**
     * 服务端时间串的可接受形态，按顺序尝试。
     *
     * 前两项是 design D4 约定的 ISO-8601 形态（`X` 时区模式）；后两项是不依赖 `X` 的字面量 `Z`
     * 形态，用于兼容个别 API 级别对 `X` 支持缺失的情况——服务端固定的 `…Z` 串在两种形态下都能
     * 正确解析（均按 UTC 解释），因此这是纯鲁棒性兜底，不改变约定语义。
     */
    private val SERVER_PATTERNS = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
    )

    /**
     * 把服务端下发的时间串转成目标时区的可读格式。
     *
     * @param raw 服务端时间串（ISO-8601，例如 `2027-10-09T06:19:00.000Z`）；可空。
     * @param zone 目标时区；生产传 `TimeZone.getDefault()`，测试注入固定时区以保证确定性。
     * @return 可读时间；`raw` 为空、纯空白或所有形态都无法解析时返回 `null`，由调用方落占位文案，
     *   绝不把原始串或 epoch 数字当作兜底展示。
     */
    fun formatDeadline(raw: String?, zone: TimeZone): String? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null
        val parsed = parseServerTime(text) ?: return null
        return SimpleDateFormat(DISPLAY_DATE_TIME, Locale.getDefault())
            .apply { timeZone = zone }
            .format(parsed)
    }

    /**
     * 把时间线事件的时间戳转成目标时区的可读格式。
     *
     * @param epochMillis 事件发生时间（epoch 毫秒，来自 `JobEvent.timestamp`）。
     * @param zone 目标时区。
     * @return 形如 `10-09 14:19` 的可读时间。
     */
    fun formatEventTime(epochMillis: Long, zone: TimeZone): String =
        SimpleDateFormat(DISPLAY_EVENT_TIME, Locale.getDefault())
            .apply { timeZone = zone }
            .format(java.util.Date(epochMillis))

    /**
     * 依次按 [SERVER_PATTERNS] 解析；全部失败返回 `null`（不猜测时区、不做宽松解析）。
     *
     * `isLenient = false` 保证 `2027-13-45T…` 之类的非法值被拒绝而不是被滚成别的日期。
     */
    private fun parseServerTime(text: String): java.util.Date? {
        for (pattern in SERVER_PATTERNS) {
            try {
                return SimpleDateFormat(pattern, Locale.US)
                    .apply {
                        timeZone = TimeZone.getTimeZone(SERVER_TIME_ZONE)
                        isLenient = false
                    }
                    .parse(text)
            } catch (e: java.text.ParseException) {
                // 该形态不匹配，继续尝试下一种。
            } catch (e: IllegalArgumentException) {
                // 该 API 级别不支持该 pattern 字母（例如 `X`），跳到不依赖它的兜底形态。
            }
        }
        return null
    }
}
