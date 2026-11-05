package com.dji.sample.gasdetection.gas.catalog

/**
 * 通道目录：通道元数据的唯一权威来源。
 *
 * 设计意图：单位、显示名、量程、精度与采样周期只在目录声明一次，测量记录与帧内不重复
 * 携带；同时维护 [ChannelDescriptor.wireId] 与 [ChannelDescriptor.channelId] 的双向映射，
 * 让紧凑帧保持字节经济，而业务模型始终使用稳定字符串标识。
 *
 * 构造时校验数值键与通道标识唯一且非空，配置错误在加载阶段暴露而不是运行期静默覆盖。
 */
class ChannelCatalog(
    /** 目录版本，随配置加载，供诊断与回放对齐。 */
    val version: String,
    descriptors: List<ChannelDescriptor>,
) {
    private val ordered: List<ChannelDescriptor> = descriptors.toList()
    private val byWireId: Map<Int, ChannelDescriptor>
    private val byChannelId: Map<String, ChannelDescriptor>

    init {
        require(version.isNotBlank()) { "catalogVersion must not be blank" }
        require(ordered.isNotEmpty()) { "channel catalog must not be empty" }
        val wireIds = HashSet<Int>()
        val channelIds = HashSet<String>()
        ordered.forEach { descriptor ->
            require(descriptor.channelId.isNotBlank()) { "channelId must not be blank" }
            require(descriptor.displayName.isNotBlank()) { "displayName must not be blank for ${descriptor.channelId}" }
            require(descriptor.unit.isNotBlank()) { "unit must not be blank for ${descriptor.channelId}" }
            require(wireIds.add(descriptor.wireId)) { "duplicate wireId ${descriptor.wireId}" }
            require(channelIds.add(descriptor.channelId)) { "duplicate channelId ${descriptor.channelId}" }
        }
        byWireId = ordered.associateBy { it.wireId }
        byChannelId = ordered.associateBy { it.channelId }
    }

    /** 按帧内数值键解析；不存在时返回 [ResolvedChannel.Unknown]，不抛异常、不猜单位。 */
    fun resolve(wireId: Int): ResolvedChannel {
        val descriptor = byWireId[wireId]
        return if (descriptor != null) ResolvedChannel.Known(descriptor) else ResolvedChannel.Unknown(wireId)
    }

    /** 按稳定通道标识查找；不存在时返回 `null`。 */
    fun find(channelId: String): ChannelDescriptor? = byChannelId[channelId]

    /** 按稳定通道标识取帧内数值键；未登记时返回 `null`。 */
    fun wireIdOf(channelId: String): Int? = byChannelId[channelId]?.wireId

    /** 目录中的全部通道，保持配置声明顺序。 */
    fun descriptors(): List<ChannelDescriptor> = ordered
}
