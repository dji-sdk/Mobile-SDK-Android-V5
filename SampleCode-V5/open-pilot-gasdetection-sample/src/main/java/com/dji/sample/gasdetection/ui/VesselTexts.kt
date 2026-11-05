package com.dji.sample.gasdetection.ui

import android.content.Context
import androidx.annotation.StringRes
import com.dji.sample.gasdetection.R
import com.dji.sample.gasdetection.gas.recognition.RecognitionFailure
import com.dji.sample.gasdetection.gas.recognition.RecognitionPhase
import com.dji.sample.gasdetection.gas.recognition.RecognitionState
import com.dji.sample.gasdetection.gas.recognition.VesselSearchBlock

/**
 * 船舶识别状态文案映射。
 *
 * 设计意图：入口页与浮窗必须展示同一份判定结果，文案只在这里映射资源，避免两处各写一套
 * 判断或出现「入口页已锁定、浮窗还在搜索」的不一致。所有文案都来自中英双语资源。
 */
object VesselTexts {

    /**
     * 一行识别状态；[RecognitionState.isSearching] 为 false 且没有被拒绝原因时返回 `null`
     * （界面据此隐藏该行）。
     */
    fun statusLine(context: Context, state: RecognitionState): String? {
        val block = state.block
        if (block != null) {
            return context.getString(R.string.gas_vessel_block, blockReason(context, block))
        }
        val body = when (state.phase) {
            RecognitionPhase.STARTING ->
                context.getString(R.string.gas_vessel_state_starting)

            RecognitionPhase.SENSING ->
                if (state.lost) {
                    context.getString(R.string.gas_vessel_state_lost)
                } else {
                    context.getString(
                        R.string.gas_vessel_state_searching,
                        state.hits,
                        state.requiredHits,
                    )
                }

            RecognitionPhase.LOCKED -> lockedText(context, state)

            RecognitionPhase.DEGRADED -> context.getString(
                R.string.gas_vessel_state_unavailable,
                failureReason(context, state.failure),
            )

            RecognitionPhase.IDLE,
            RecognitionPhase.STOPPED,
            -> return null
        }
        return context.getString(R.string.gas_vessel_label, body)
    }

    /**
     * 浮窗专用状态行：只在搜索相关阶段展示，不展示「开始搜索被拒绝」这类入口页动作反馈。
     */
    fun floatingStatus(context: Context, state: RecognitionState): String? =
        if (state.isSearching) statusLine(context, state) else null

    /** 开始搜索被拒绝的原因文案。 */
    fun blockReason(context: Context, block: VesselSearchBlock): String =
        context.getString(blockRes(block))

    /** 识别不可用原因文案。 */
    fun failureReason(context: Context, failure: RecognitionFailure?): String =
        context.getString(failureRes(failure))

    @StringRes
    private fun blockRes(block: VesselSearchBlock): Int = when (block) {
        VesselSearchBlock.NOT_ARRIVED -> R.string.gas_vessel_block_not_arrived
        VesselSearchBlock.AIRCRAFT_DISCONNECTED -> R.string.gas_vessel_block_disconnected
        VesselSearchBlock.NOT_FLYING -> R.string.gas_vessel_block_not_flying
        VesselSearchBlock.MISSION_RUNNING -> R.string.gas_vessel_block_mission_running
        VesselSearchBlock.MISSION_UNKNOWN -> R.string.gas_vessel_block_mission_unknown
        VesselSearchBlock.STAGE_NOT_READY -> R.string.gas_vessel_block_stage
    }

    @StringRes
    private fun failureRes(failure: RecognitionFailure?): Int = when (failure) {
        RecognitionFailure.START_FAILED -> R.string.gas_vessel_failure_start
        RecognitionFailure.ENABLE_TIMEOUT -> R.string.gas_vessel_failure_timeout
        RecognitionFailure.ENABLE_LOST -> R.string.gas_vessel_failure_lost
        null -> R.string.gas_vessel_failure_start
    }

    private fun lockedText(context: Context, state: RecognitionState): String {
        val vessel = state.vessel
        val confirmText = if (vessel?.humanConfirmed == true && !vessel.nameOrNumber.isNullOrBlank()) {
            context.getString(R.string.gas_vessel_confirm_done, vessel.nameOrNumber)
        } else {
            context.getString(R.string.gas_vessel_confirm_pending)
        }
        return context.getString(
            R.string.gas_vessel_state_locked,
            state.nativeIndex ?: 0,
            state.heldMs / 1000,
            confirmText,
        )
    }
}
