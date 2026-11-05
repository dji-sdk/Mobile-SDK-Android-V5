package com.dji.sample.gasdetection.ui

import androidx.annotation.StringRes
import com.dji.sample.gasdetection.R
import com.dji.sample.gasdetection.gas.autoflight.AutoFlightPhase
import com.dji.sample.gasdetection.gas.autoflight.FlightAuthority
import com.dji.sample.gasdetection.gas.autoflight.FlyToStage
import com.dji.sample.gasdetection.gas.autoflight.HandoverReason

/**
 * 自动飞行文案映射。
 *
 * 设计意图：阶段、FlyTo 状态、控制权与接管原因只在这里映射资源，入口页与浮窗共用同一套
 * 判定，避免两处出现不一致的描述；领域层保持无 Android 依赖。
 */
object AutoFlightTexts {

    @StringRes
    fun phaseRes(phase: AutoFlightPhase): Int = when (phase) {
        AutoFlightPhase.IDLE -> R.string.gas_task_stage_idle
        AutoFlightPhase.STARTING -> R.string.gas_auto_phase_starting
        AutoFlightPhase.FLYING -> R.string.gas_auto_phase_flying
        AutoFlightPhase.ARRIVED -> R.string.gas_auto_phase_arrived
        AutoFlightPhase.HANDOVER -> R.string.gas_auto_phase_handover
    }

    @StringRes
    fun flyToStageRes(stage: FlyToStage?): Int = when (stage) {
        FlyToStage.PREPARE -> R.string.gas_auto_flyto_prepare
        FlyToStage.LAUNCH_PRE -> R.string.gas_auto_flyto_launch_pre
        FlyToStage.LAUNCH -> R.string.gas_auto_flyto_launch
        FlyToStage.MARCHON -> R.string.gas_auto_flyto_marchon
        FlyToStage.FINISHED -> R.string.gas_auto_flyto_finished
        FlyToStage.INTERRUPTED -> R.string.gas_auto_flyto_interrupted
        null, FlyToStage.UNKNOWN -> R.string.gas_auto_flyto_unknown
    }

    @StringRes
    fun authorityRes(authority: FlightAuthority): Int = when (authority) {
        FlightAuthority.RC -> R.string.gas_auto_authority_rc
        FlightAuthority.MSDK -> R.string.gas_auto_authority_msdk
        FlightAuthority.OSDK -> R.string.gas_auto_authority_osdk
        FlightAuthority.AIRPORT -> R.string.gas_auto_authority_airport
        FlightAuthority.AUTO_TEST -> R.string.gas_auto_authority_auto_test
        FlightAuthority.UNKNOWN -> R.string.gas_auto_authority_unknown
    }

    @StringRes
    fun handoverRes(reason: HandoverReason?): Int = when (reason) {
        HandoverReason.USER_EXIT -> R.string.gas_auto_handover_user_exit
        HandoverReason.TASK_INTERRUPTED -> R.string.gas_auto_handover_interrupted
        HandoverReason.START_NOT_CONFIRMED -> R.string.gas_auto_handover_not_confirmed
        HandoverReason.STATE_NOT_ADVANCED -> R.string.gas_auto_handover_not_advanced
        HandoverReason.DISCONNECTED -> R.string.gas_auto_handover_disconnected
        HandoverReason.BATTERY_LOW -> R.string.gas_auto_handover_battery
        HandoverReason.GPS_LOST -> R.string.gas_auto_handover_gps
        HandoverReason.FC_TAKEOVER -> R.string.gas_auto_handover_fc
        HandoverReason.STOP_FAILED -> R.string.gas_auto_handover_stop_failed
        null -> R.string.gas_task_failure_unknown
    }
}
