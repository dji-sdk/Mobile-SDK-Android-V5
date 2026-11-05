package com.dji.sample.gasdetection.gas.catalog

/**
 * 传输页展示用的通道目录摘要。
 *
 * 设计意图：界面只需要目录版本与通道映射，不直接持有可变配置对象；字段按目录声明顺序
 * 输出，便于与设备字段逐条核对。
 *
 * @property version 目录版本。
 * @property channels 目录中的通道描述。
 */
data class CatalogInfo(
    val version: String,
    val channels: List<ChannelDescriptor>,
)
