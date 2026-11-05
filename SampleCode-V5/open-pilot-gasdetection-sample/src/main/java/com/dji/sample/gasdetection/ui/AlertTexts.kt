package com.dji.sample.gasdetection.ui

import androidx.annotation.StringRes
import com.dji.sample.gasdetection.R
import com.dji.sample.gasdetection.gas.model.AlertState
import com.dji.sample.gasdetection.gas.model.AlertType

/**
 * 告警类型与状态的文案映射。
 *
 * 设计意图：插件页面与浮窗共用同一份映射，避免两处对同一类型出现不同叫法；文案全部来自
 * 中英双语资源，设备原文的通道名与单位不在这里翻译。
 */
object AlertTexts {

    @StringRes
    fun typeRes(type: AlertType): Int = when (type) {
        AlertType.THRESHOLD_EXCEEDED -> R.string.gas_alert_type_threshold
        AlertType.SENSOR_FAULT -> R.string.gas_alert_type_sensor_fault
        AlertType.DATA_STALE -> R.string.gas_alert_type_stale
        AlertType.DATA_QUALITY -> R.string.gas_alert_type_quality
    }

    @StringRes
    fun stateRes(state: AlertState): Int = when (state) {
        AlertState.NEW -> R.string.gas_alert_state_new
        AlertState.ACKNOWLEDGED -> R.string.gas_alert_state_acknowledged
        AlertState.UNDER_REVIEW -> R.string.gas_alert_state_review
        AlertState.DISMISSED -> R.string.gas_alert_state_dismissed
        AlertState.CLOSED -> R.string.gas_alert_state_closed
    }
}
