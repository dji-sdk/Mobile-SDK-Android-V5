package com.dji.sample.gasdetection.gas.follow

import com.dji.sample.gasdetection.gas.recognition.TargetRect
import kotlin.math.abs

/**
 * 受限视觉跟随控制律。
 *
 * 设计意图：把「目标框偏差 → 虚拟摇杆参数」收敛成纯函数，便于在 JVM 上验证限幅、死区与无效输入，
 * 也避免把飞行控制逻辑散落在会话与界面里。输入不可用时返回 `null`，调用方必须退出跟随，
 * 绝不能把 `null` 当成「发送 0」继续跟随。
 *
 * 约定：目标矩形必须是 0–1 归一化坐标（中心点 + 宽高）。SDK 未在文档中承诺这一点，因此运行期
 * 严格校验，不满足即拒绝，避免用猜测的像素尺度驱动飞行。
 */
object FollowControlLaw {

    /**
     * 计算一个周期的控制量。
     *
     * @param rect 当前锁定目标的最新矩形；`null` 表示没有可用观测。
     * @param seenAtMs 最近一次目标观测时间；`null` 表示未知。
     * @param nowMs 当前时间。
     * @return 受限控制量；输入无效时返回 `null`。
     */
    fun compute(rect: TargetRect?, seenAtMs: Long?, nowMs: Long): FollowCommand? {
        if (rect == null || seenAtMs == null) return null
        val age = (nowMs - seenAtMs).coerceAtLeast(0L)
        if (age > FollowLimits.FRESHNESS_MS) return null
        if (!isNormalized(rect)) return null

        val yaw = yawFor(rect.centerX - 0.5)
        val pitch = pitchFor(rect.width)
        return FollowCommand(roll = 0.0, pitch = pitch, yaw = yaw, verticalThrottle = 0.0)
    }

    /** 目标矩形是否为 0–1 归一化坐标且宽高有效。 */
    fun isNormalized(rect: TargetRect): Boolean =
        rect.centerX.isFinite() &&
            rect.centerY.isFinite() &&
            rect.width.isFinite() &&
            rect.height.isFinite() &&
            rect.centerX in 0.0..1.0 &&
            rect.centerY in 0.0..1.0 &&
            rect.width > 0.0 &&
            rect.width <= 1.0 &&
            rect.height > 0.0 &&
            rect.height <= 1.0

    /** 横向误差 → 偏航角速度（带死区与限幅）。 */
    private fun yawFor(error: Double): Double {
        if (abs(error) <= FollowLimits.YAW_DEADBAND + DEADBAND_EPSILON) return 0.0
        return (error * FollowLimits.YAW_GAIN_DPS)
            .coerceIn(-FollowLimits.MAX_YAW_DPS, FollowLimits.MAX_YAW_DPS)
    }

    /** 浮点比较容差，避免 0.45 - 0.5 这类误差把死区判断顶出去。 */
    private const val DEADBAND_EPSILON = 1e-9

    /** 目标框宽度 → 前后速度（带区间与限幅）。 */
    private fun pitchFor(width: Double): Double = when {
        width < FollowLimits.WIDTH_MIN ->
            ((FollowLimits.WIDTH_MIN - width) * FollowLimits.WIDTH_GAIN)
                .coerceIn(0.0, FollowLimits.MAX_HORIZONTAL_MPS)

        width > FollowLimits.WIDTH_MAX ->
            (-(width - FollowLimits.WIDTH_MAX) * FollowLimits.WIDTH_GAIN)
                .coerceIn(-FollowLimits.MAX_HORIZONTAL_MPS, 0.0)

        else -> 0.0
    }
}
