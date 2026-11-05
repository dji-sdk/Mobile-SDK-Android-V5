package com.dji.sample.gasdetection.gas.payload

/**
 * 端口到适配器的注册表。
 *
 * 设计意图：同一架飞机可能挂多个载荷，格式各不相同；注册表让调用方按端口显式声明
 * 「这个端口用哪个适配器」。没有注册的端口保持空缺，由接入服务报「未找到适配器」，
 * 不做格式自动识别。
 *
 * 线程安全：所有方法内部加锁，可在 SDK 回调线程与主线程并发调用。
 */
class PayloadAdapterRegistry {
    private val lock = Any()
    private val adapters = LinkedHashMap<PayloadPort, PayloadAdapter>()

    /** 为 [port] 注册适配器；重复注册时覆盖旧值。 */
    fun register(port: PayloadPort, adapter: PayloadAdapter) {
        synchronized(lock) { adapters[port] = adapter }
    }

    /** 移除 [port] 的适配器；不存在时无副作用。 */
    fun unregister(port: PayloadPort) {
        synchronized(lock) { adapters.remove(port) }
    }

    /** 查找 [port] 的适配器；未注册时返回 `null`。 */
    fun find(port: PayloadPort): PayloadAdapter? = synchronized(lock) { adapters[port] }

    /** 清空全部注册。 */
    fun clear() {
        synchronized(lock) { adapters.clear() }
    }

    /** 当前注册快照，按注册顺序返回，供诊断或测试使用。 */
    fun snapshot(): Map<PayloadPort, PayloadAdapter> = synchronized(lock) { LinkedHashMap(adapters) }
}
