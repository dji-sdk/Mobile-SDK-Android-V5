package com.dji.sample.gasdetection.gas.catalog

/**
 * 插件气体配置。
 *
 * 设计意图：把通道目录与时效阈值放在同一份资源里，加载一次即可供目录、载荷接入
 * 与告警模块复用；合法性在解析阶段校验，不在运行期静默兜底。
 */
data class GasConfig(
    /** 通道目录。 */
    val catalog: ChannelCatalog,
    /** 数据过期阈值（毫秒），必须大于 0。 */
    val staleAfterMs: Long,
)
