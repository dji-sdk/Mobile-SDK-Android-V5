package com.dji.sample.gasdetection.gas.model

/**
 * 船舶标识的获取来源。
 *
 * 设计意图：人工录入、光学识别与外部系统给出的标识可信度不同，必须显式区分，
 * 不能把未确认的识别结果当成已确认船名。
 */
enum class IdentificationSource {
    MANUAL,
    OCR,
    EXTERNAL,

    /** Pilot 原生 AI 识别锁定的对象；与人工录入、OCR 区分，避免把识别结果当成已确认船名。 */
    NATIVE_AI,
}

/**
 * 告警关联的船舶/监测对象。
 *
 * 设计意图：告警与船舶是多对一关系，本条记录保存标识来源与人工确认状态，
 * 未确认时 [humanConfirmed] 必须为 `false`，界面据此显示「待确认」。
 */
data class VesselReference(
    /** 船舶稳定标识。 */
    val vesselId: String,
    /** 关联任务标识；未关联时为 `null`。 */
    val taskId: String?,
    /** 船名或编号原文；未提供时为 `null`。 */
    val nameOrNumber: String?,
    /** 标识来源。 */
    val identificationSource: IdentificationSource,
    /** 是否已经人工确认。 */
    val humanConfirmed: Boolean = false,
    /** 取证附件标识；未关联时为 `null`。 */
    val evidenceAssetId: String? = null,
    /** 关联位置纬度；未记录时为 `null`。 */
    val latitude: Double? = null,
    /** 关联位置经度；未记录时为 `null`。 */
    val longitude: Double? = null,
    /** 关联时间（毫秒）。 */
    val timestampMs: Long,
    /** 已关联的告警事件标识列表。 */
    val alertIds: List<String> = emptyList(),
)
