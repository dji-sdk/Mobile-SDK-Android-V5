package com.dji.sample.gasdetection.gas.task

import android.content.Context
import androidx.annotation.StringRes
import com.dji.sample.gasdetection.R

/**
 * 任务相关文案映射。
 *
 * 设计意图：阶段、预检失败原因、同步错误只在这里映射资源，入口页与浮窗共用，
 * 避免两处文案不一致或写死中文。
 */
object TaskTexts {

    @StringRes
    fun stageRes(stage: TaskStage): Int = when (stage) {
        TaskStage.IDLE -> R.string.gas_task_stage_idle
        TaskStage.NEW_ORDER -> R.string.gas_task_stage_new_order
        TaskStage.TASK_DETAIL -> R.string.gas_task_stage_detail
        TaskStage.PREP -> R.string.gas_task_stage_prep
        TaskStage.NOT_READY -> R.string.gas_task_stage_not_ready
        TaskStage.PREP_READY -> R.string.gas_task_stage_ready
        TaskStage.MANUAL -> R.string.gas_task_stage_manual
        TaskStage.FINALIZING -> R.string.gas_task_stage_finalizing
        TaskStage.FLYTO -> R.string.gas_task_stage_flyto
        TaskStage.SEARCH -> R.string.gas_task_stage_search
        TaskStage.FOLLOW -> R.string.gas_task_stage_follow
    }

    @StringRes
    fun failureRes(failure: PreflightFailure?): Int = when (failure) {
        PreflightFailure.PAYLOAD_OFFLINE -> R.string.gas_task_failure_payload_offline
        PreflightFailure.CONFIG_INVALID -> R.string.gas_task_failure_config_invalid
        PreflightFailure.AUTO_FLIGHT_UNSUPPORTED -> R.string.gas_task_failure_auto_unsupported
        PreflightFailure.AUTO_FLIGHT_UNKNOWN -> R.string.gas_task_failure_auto_unknown
        PreflightFailure.AIRCRAFT_DISCONNECTED -> R.string.gas_task_failure_aircraft_disconnected
        PreflightFailure.AIRCRAFT_NOT_FLYING -> R.string.gas_task_failure_not_flying
        PreflightFailure.GPS_WEAK -> R.string.gas_task_failure_gps_weak
        PreflightFailure.BATTERY_LOW -> R.string.gas_task_failure_battery_low
        PreflightFailure.MISSION_RUNNING -> R.string.gas_task_failure_mission_running
        PreflightFailure.AUTHORITY_OCCUPIED -> R.string.gas_task_failure_authority_occupied
        PreflightFailure.TARGET_MISSING -> R.string.gas_task_failure_target_missing
        null -> R.string.gas_task_failure_unknown
    }

    /** 后端任务状态文案；用于任务列表项，不复用阶段文案。 */
    @StringRes
    fun jobStatusRes(status: JobStatus): Int = when (status) {
        JobStatus.DISPATCHED -> R.string.gas_job_status_dispatched
        JobStatus.ACCEPTED -> R.string.gas_job_status_accepted
        JobStatus.IN_PROGRESS -> R.string.gas_job_status_in_progress
        JobStatus.FINALIZING -> R.string.gas_job_status_finalizing
        JobStatus.COMPLETED -> R.string.gas_job_status_completed
        JobStatus.CANCELLED -> R.string.gas_job_status_cancelled
        JobStatus.REJECTED -> R.string.gas_job_status_rejected
    }

    /** 同步错误转成可读文案；保留原始错误类别，不吞掉 HTTP 码。 */
    fun syncError(context: Context, error: ApiError): String = when (error) {
        is ApiError.Network -> context.getString(R.string.gas_task_error_network, error.message)
        is ApiError.Http -> context.getString(R.string.gas_task_error_http, error.code)
        is ApiError.Parse -> context.getString(R.string.gas_task_error_parse, error.message)
    }
}
