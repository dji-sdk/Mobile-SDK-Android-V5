package com.dji.sample.gasdetection.gas.payload

/**
 * 设备提供的一个控件条目。
 *
 * 设计意图：控件信息是设备能力描述的一部分，只记录设备真实返回的名称、类型、索引、当前值与
 * 提示文本；即使名称看起来像气体名称，也不据此生成通道或单位。
 */
data class PayloadWidgetItem(
    /** 控件名称；`null` 表示未提供。 */
    val name: String?,
    /** 控件类型枚举名；`null` 表示未提供。 */
    val type: String?,
    /** 控件索引。 */
    val index: Int,
    /** 控件当前整数值。 */
    val value: Int,
    /** 控件提示文本；`null` 表示未提供。 */
    val hint: String?,
)

/**
 * 单个端口的设备能力快照。
 *
 * 设计意图：拉取失败时保留上一次成功内容，只补充失败原因与时间，避免界面在瞬时失败时
 * 误显示「设备没有能力」。
 */
data class PayloadWidgetSnapshot(
    /** 端口标识。 */
    val port: PayloadPort,
    /** 主界面与配置界面控件条目。 */
    val items: List<PayloadWidgetItem>,
    /** 最近一次成功快照时间（毫秒）。 */
    val capturedAtMs: Long,
    /** 最近一次拉取失败原因；成功后置空。 */
    val lastError: String? = null,
    /** 最近一次拉取失败时间（毫秒）。 */
    val lastErrorAtMs: Long? = null,
)
