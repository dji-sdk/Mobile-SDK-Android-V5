package com.dji.sample.gasdetection.gas.wire

/**
 * 测量上行帧的预算与版本常量。
 *
 * 设计意图：单帧预算来自 `psdk-bridge` 的帧格式（7 字节帧头 + 255 字节单帧上限），
 * 集中在一个常量里，FR-2 实机核实 MTU 后只需改这里，不用在编解码各处找散落数字。
 */
object GasWireLimits {
    /** 非分片 JSON 净荷预算：255 字节单帧上限减去 7 字节帧头。 */
    const val MAX_PAYLOAD_BYTES = 248

    /** 当前帧格式版本。 */
    const val VERSION = 1

    /** 会话标识最大长度，超出直接判定编码非法而不是截断。 */
    const val MAX_SESSION_ID_LENGTH = 24
}
