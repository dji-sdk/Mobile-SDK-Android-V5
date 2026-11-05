package com.dji.sample.gasdetection.gas.wire

/**
 * 会话内序列号去重器。
 *
 * 设计意图：小数据通道可能重传，同一会话的重复序列号不应重复计入趋势与样本数；
 * 只保留最近 [window] 个序列号，窗口外的重传按新样本处理，由后续归档层用测量 ID 幂等兜底。
 * 线程安全：方法在编解码调用线程内使用，不自行加锁。
 */
class DuplicateSequenceFilter(private val window: Int = 64) {

    private val recentSequences = LinkedHashMap<String, ArrayDeque<Long>>()

    /** 记录并判断是否为重复序列号；首次出现返回 `false`，窗口内重复返回 `true`。 */
    fun isDuplicate(sessionId: String, sequence: Long): Boolean {
        val sequences = recentSequences.getOrPut(sessionId) { ArrayDeque() }
        if (sequences.contains(sequence)) return true
        sequences.addLast(sequence)
        while (sequences.size > window) {
            sequences.removeFirst()
        }
        return false
    }

    /** 清理指定会话的去重状态，会话结束时调用。 */
    fun reset(sessionId: String) {
        recentSequences.remove(sessionId)
    }
}
