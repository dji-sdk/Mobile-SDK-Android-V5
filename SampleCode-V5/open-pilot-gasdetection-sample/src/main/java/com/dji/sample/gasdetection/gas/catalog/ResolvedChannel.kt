package com.dji.sample.gasdetection.gas.catalog

/**
 * 通道解析结果。
 *
 * 设计意图：目录里没有的通道不能抛异常中断整条测量，也不能猜名称或单位；用
 * [Unknown] 显式降级并保留数值，让接收侧计入诊断。
 */
sealed class ResolvedChannel {
    /** 帧内数值键，解码与诊断都以它定位。 */
    abstract val wireId: Int

    /** 目录中存在的通道。 */
    data class Known(val descriptor: ChannelDescriptor) : ResolvedChannel() {
        override val wireId: Int get() = descriptor.wireId
    }

    /** 目录中不存在的通道，只保留数值键。 */
    data class Unknown(override val wireId: Int) : ResolvedChannel()
}
