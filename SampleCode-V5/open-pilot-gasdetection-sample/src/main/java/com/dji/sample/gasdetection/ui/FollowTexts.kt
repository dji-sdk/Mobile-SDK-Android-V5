package com.dji.sample.gasdetection.ui

import android.content.Context
import androidx.annotation.StringRes
import com.dji.sample.gasdetection.R
import com.dji.sample.gasdetection.gas.follow.FollowAbortReason
import com.dji.sample.gasdetection.gas.follow.FollowBlock
import com.dji.sample.gasdetection.gas.follow.FollowPhase
import com.dji.sample.gasdetection.gas.follow.FollowState

/**
 * 跟随状态文案映射。
 *
 * 设计意图：入口页与浮窗共用同一份会话快照与文案，避免出现「入口页说已退出、浮窗还在跟随中」。
 * 领域层保持无 Android 依赖。
 */
object FollowTexts {

    /** 一行跟随状态；未开始或已复位时返回 `null`。 */
    fun statusLine(context: Context, state: FollowState): String? {
        val body = when (state.phase) {
            FollowPhase.IDLE -> return null

            FollowPhase.ARMING -> context.getString(R.string.gas_follow_status_arming)

            FollowPhase.ACTIVE -> context.getString(
                R.string.gas_follow_status_active,
                context.getString(AutoFlightTexts.authorityRes(state.authority)),
            )

            FollowPhase.RELEASING -> context.getString(R.string.gas_follow_status_releasing)

            FollowPhase.STOPPED,
            FollowPhase.ABORTED,
            -> {
                if (state.releaseConfirmed) {
                    context.getString(
                        R.string.gas_follow_status_stopped,
                        context.getString(reasonRes(state.reason)),
                    )
                } else {
                    context.getString(
                        R.string.gas_follow_status_stopped_unconfirmed,
                        context.getString(reasonRes(state.reason)),
                    )
                }
            }
        }
        return context.getString(R.string.gas_follow_label, body)
    }

    /** 退出/中止原因文案。 */
    fun reasonText(context: Context, reason: FollowAbortReason?): String =
        context.getString(reasonRes(reason))

    /** 开始跟随被拒绝的原因文案。 */
    fun blockText(context: Context, block: FollowBlock): String =
        context.getString(blockRes(block))

    @StringRes
    private fun reasonRes(reason: FollowAbortReason?): Int = when (reason) {
        FollowAbortReason.TARGET_LOST -> R.string.gas_follow_reason_target_lost
        FollowAbortReason.TARGET_STALE -> R.string.gas_follow_reason_target_stale
        FollowAbortReason.INVALID_RECT -> R.string.gas_follow_reason_invalid_rect
        FollowAbortReason.CONTROL_AUTHORITY_LOST -> R.string.gas_follow_reason_authority
        FollowAbortReason.LOW_BATTERY -> R.string.gas_follow_reason_battery
        FollowAbortReason.GPS_WEAK -> R.string.gas_follow_reason_gps
        FollowAbortReason.FC_TAKEOVER -> R.string.gas_follow_reason_fc
        FollowAbortReason.DISCONNECTED -> R.string.gas_follow_reason_disconnected
        FollowAbortReason.ENABLE_FAILED -> R.string.gas_follow_reason_enable_failed
        FollowAbortReason.AUTHORITY_TIMEOUT -> R.string.gas_follow_reason_authority_timeout
        FollowAbortReason.ADVANCED_TIMEOUT -> R.string.gas_follow_reason_advanced_timeout
        FollowAbortReason.RELEASE_TIMEOUT -> R.string.gas_follow_reason_release_timeout
        FollowAbortReason.USER_EXIT -> R.string.gas_follow_reason_user_exit
        FollowAbortReason.TASK_FINISHED -> R.string.gas_follow_reason_task_finished
        FollowAbortReason.STAGE_REJECTED -> R.string.gas_follow_reason_stage
        null -> R.string.gas_task_failure_unknown
    }

    @StringRes
    private fun blockRes(block: FollowBlock): Int = when (block) {
        FollowBlock.STAGE_NOT_READY -> R.string.gas_follow_reason_stage
        FollowBlock.NOT_LOCKED -> R.string.gas_follow_block_not_locked
        FollowBlock.TARGET_STALE -> R.string.gas_follow_reason_target_stale
        FollowBlock.INVALID_RECT -> R.string.gas_follow_reason_invalid_rect
        FollowBlock.SAFETY -> R.string.gas_follow_block_safety
        FollowBlock.AUTHORITY_OCCUPIED -> R.string.gas_follow_block_authority
        FollowBlock.ALREADY_ACTIVE -> R.string.gas_follow_block_active
    }
}
