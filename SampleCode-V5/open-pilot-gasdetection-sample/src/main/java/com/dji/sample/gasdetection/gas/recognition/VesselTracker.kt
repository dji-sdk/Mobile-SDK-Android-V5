package com.dji.sample.gasdetection.gas.recognition

/**
 * 船舶连续有效判定。
 *
 * 设计意图：把「什么样的原生目标算锁定了」从控制器与 SDK 回调里剥离成纯逻辑——只接受
 * [TargetKind.BOAT]，同一 `targetIndex` 在新鲜窗口内连续有效达到 [requiredHits] 次才锁定；
 * 任一断档都复位连续性，锁定后超过新鲜窗口没有更新即失锁。这样可以在 JVM 上覆盖全部
 * 时序分支，不依赖真机或时钟推进。
 *
 * 线程约束：所有方法在主线程调用，内部不加锁。
 */
class VesselTracker(
    /** 新鲜窗口（毫秒）：相邻两次更新的最大允许间隔，也是锁定后的失锁阈值。 */
    private val lockHoldMs: Long = DEFAULT_LOCK_HOLD_MS,
    /** 达到锁定的连续有效次数。 */
    val requiredHits: Int = REQUIRED_HITS,
) {

    /** 单帧处理或节拍检查的结果。 */
    data class TrackerUpdate(
        /** 未锁定时当前候选索引；无候选为 `null`。 */
        val candidateIndex: Int? = null,
        /** 未锁定时当前候选的连续有效次数。 */
        val hits: Int = 0,
        /** 本次调用刚刚锁定的索引；仅新锁定时非空。 */
        val newlyLockedIndex: Int? = null,
        /** 本次锁定的起始时间（首次有效命中），用于计算连续时长。 */
        val newlyLockedAtMs: Long? = null,
        /** 当前锁定索引；未锁定为 `null`。 */
        val lockedIndex: Int? = null,
        /** 本次调用判定失锁的索引；仅失锁时非空。 */
        val lostIndex: Int? = null,
    )

    private data class Hits(
        /** 连续有效次数。 */
        val consecutive: Int,
        /** 最近一次命中的时间。 */
        val lastSeenMs: Long,
        /** 本轮连续有效的首次命中时间。 */
        val firstSeenMs: Long,
    )

    private val candidates = LinkedHashMap<Int, Hits>()
    private var lockedIndex: Int? = null
    private var lockedAtMs: Long = 0L
    private var lockedLastSeenMs: Long = 0L

    init {
        require(lockHoldMs > 0) { "lockHoldMs must be > 0" }
        require(requiredHits > 0) { "requiredHits must be > 0" }
    }

    /** 当前锁定索引；未锁定为 `null`。 */
    fun lockedIndex(): Int? = lockedIndex

    /** 连续有效时长；未锁定时为 0。 */
    fun heldMs(nowMs: Long): Long = if (lockedIndex == null) 0L else (nowMs - lockedAtMs).coerceAtLeast(0L)

    /**
     * 处理一帧识别结果。
     *
     * @param targets 已投影的目标列表；非 BOAT 目标在这里直接忽略。
     * @param nowMs 当前时间（毫秒），由调用方注入以便测试。
     */
    fun onFrame(targets: List<SensingTargetInfo>, nowMs: Long): TrackerUpdate {
        val currentLock = lockedIndex
        if (currentLock != null) {
            val stillVisible = targets.any { it.kind == TargetKind.BOAT && it.index == currentLock }
            if (stillVisible) lockedLastSeenMs = nowMs
            // 可见性消失不立即失锁：由 tick() 按新鲜窗口统一判定，避免单帧抖动导致来回锁定。
            return TrackerUpdate(lockedIndex = currentLock)
        }

        pruneStale(nowMs)

        var best: Pair<Int, Hits>? = null
        var newlyLocked: Pair<Int, Hits>? = null
        targets.forEach { target ->
            if (target.kind != TargetKind.BOAT) return@forEach
            val previous = candidates[target.index]
            val next = if (previous != null && nowMs - previous.lastSeenMs <= lockHoldMs) {
                previous.copy(consecutive = previous.consecutive + 1, lastSeenMs = nowMs)
            } else {
                Hits(consecutive = 1, lastSeenMs = nowMs, firstSeenMs = nowMs)
            }
            candidates[target.index] = next
            if (newlyLocked == null && next.consecutive >= requiredHits) {
                newlyLocked = target.index to next
            }
            val bestSoFar = best
            if (bestSoFar == null || next.consecutive > bestSoFar.second.consecutive) {
                best = target.index to next
            }
        }

        val lock = newlyLocked
        if (lock != null) {
            lockedIndex = lock.first
            lockedAtMs = lock.second.firstSeenMs
            lockedLastSeenMs = nowMs
            candidates.clear()
            return TrackerUpdate(
                hits = lock.second.consecutive,
                newlyLockedIndex = lock.first,
                newlyLockedAtMs = lock.second.firstSeenMs,
                lockedIndex = lock.first,
            )
        }

        return TrackerUpdate(
            candidateIndex = best?.first,
            hits = best?.second?.consecutive ?: 0,
        )
    }

    /**
     * 周期检查新鲜度：锁定目标超过新鲜窗口未更新即失锁，未锁定的候选断档后清空。
     */
    fun tick(nowMs: Long): TrackerUpdate {
        val currentLock = lockedIndex
        if (currentLock != null) {
            if (nowMs - lockedLastSeenMs > lockHoldMs) {
                lockedIndex = null
                lockedAtMs = 0L
                lockedLastSeenMs = 0L
                candidates.clear()
                return TrackerUpdate(lostIndex = currentLock)
            }
            return TrackerUpdate(lockedIndex = currentLock)
        }
        pruneStale(nowMs)
        val best = candidates.maxByOrNull { it.value.consecutive }
        return TrackerUpdate(
            candidateIndex = best?.key,
            hits = best?.value?.consecutive ?: 0,
        )
    }

    /** 清空全部状态（退出搜索或断连）。 */
    fun reset() {
        candidates.clear()
        lockedIndex = null
        lockedAtMs = 0L
        lockedLastSeenMs = 0L
    }

    private fun pruneStale(nowMs: Long) {
        candidates.entries.removeAll { entry -> nowMs - entry.value.lastSeenMs > lockHoldMs }
    }

    companion object {
        /** 默认新鲜窗口：AI 识别帧间隔通常远小于该值，2 秒可容忍短暂抖动。 */
        const val DEFAULT_LOCK_HOLD_MS = 2_000L

        /** 默认连续有效次数。 */
        const val REQUIRED_HITS = 2
    }
}
