package com.dji.sample.gasdetection.gas.payload

/**
 * 一个载荷端口的轻量标识。
 *
 * 设计意图：接入层内部的注册表、快照与诊断都按端口索引，但不希望把 SDK 的端口枚举
 * 扩散到适配器和测试里。[name] 与 [value] 都来自 SDK 端口枚举，只做稳定标识，不做业务推断。
 */
data class PayloadPort(
    /** 端口枚举名，例如 `LEFT_OR_MAIN`。 */
    val name: String,
    /** SDK 端口枚举值，用于排序与诊断。 */
    val value: Int,
)
