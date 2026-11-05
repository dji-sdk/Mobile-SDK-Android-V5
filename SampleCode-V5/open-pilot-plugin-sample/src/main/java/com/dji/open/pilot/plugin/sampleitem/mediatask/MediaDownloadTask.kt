package com.dji.open.pilot.plugin.sampleitem.mediatask

import java.io.Closeable
import java.math.BigInteger

/** Whole-file progress; null percent means the fixed metadata size is unknown. */
internal data class MediaDownloadProgress(
    val receivedBytes: Long,
    val expectedSize: Long,
    val elapsedMs: Long,
    val hasValidatedFinish: Boolean = false
) {
    val percent: Int?
        get() = when {
            expectedSize <= 0L -> null
            hasValidatedFinish -> 100
            else -> scaledBytes(100L, expectedSize).coerceAtMost(99L).toInt()
        }

    val speedBytesPerSecond: Long
        get() = scaledBytes(1_000L, elapsedMs.coerceAtLeast(1L))

    // Long multiplication can overflow for large files; keep integer precision at boundaries.
    private fun scaledBytes(scale: Long, divisor: Long): Long =
        BigInteger.valueOf(receivedBytes.coerceAtLeast(0L))
            .multiply(BigInteger.valueOf(scale))
            .divide(BigInteger.valueOf(divisor))
            .min(BigInteger.valueOf(Long.MAX_VALUE))
            .toLong()
}

/** Per-download ownership and lifecycle, independent of Android and SDK callbacks. */
internal class MediaDownloadTask(
    val session: MediaDownloadSession,
    private val expectedSize: Long,
    val startedAtMs: Long
) : Closeable {
    private enum class State { RUNNING, STOPPING, SETTLED }

    private var state = State.RUNNING
    private var receivedBytes = 0L
    private var hasSettledRunning = false
    private var hasValidatedFinish = false
    private var lastActivityAtMs = startedAtMs
    private var lastDataRenewAtMs = startedAtMs
    private var stopDeadlineMs: Long? = null

    @Synchronized
    fun isRunning(): Boolean = state == State.RUNNING

    @Synchronized
    fun progressSnapshot(nowMs: Long): MediaDownloadProgress =
        MediaDownloadProgress(receivedBytes, expectedSize, (nowMs - startedAtMs).coerceAtLeast(1L), hasValidatedFinish)

    /** Called only for onFinish, after settlement has frozen IO and before closing the session. */
    @Synchronized
    fun validateFinished(): Boolean {
        if (!hasSettledRunning) return false
        hasValidatedFinish = session.isComplete(expectedSize)
        return hasValidatedFinish
    }

    /**
     * State, IO and byte accounting share a lock: stop is a barrier, not a TOCTOU check.
     * Returns whether main should renew the idle timeout and refresh progress (at most every 2s).
     */
    @Synchronized
    fun append(bytes: ByteArray, nowMs: Long): Boolean {
        if (!isRunning()) return false
        try {
            check(bytes.size.toLong() <= Long.MAX_VALUE - receivedBytes) { "Download byte count overflow" }
            session.append(bytes)
            receivedBytes += bytes.size.toLong()
        } catch (exception: Exception) {
            state = State.STOPPING
            throw exception
        }
        lastActivityAtMs = maxOf(lastActivityAtMs, nowMs)
        if (nowMs - lastDataRenewAtMs < DATA_RENEW_INTERVAL_MS) return false
        lastDataRenewAtMs = nowMs
        return true
    }

    @Synchronized
    fun recordProgress(nowMs: Long): Boolean {
        if (!isRunning()) return false
        lastActivityAtMs = maxOf(lastActivityAtMs, nowMs)
        return true
    }

    @Synchronized
    fun idleRemainingMs(nowMs: Long): Long =
        if (isRunning()) (IDLE_TIMEOUT_MS - (nowMs - lastActivityAtMs)).coerceAtLeast(0L) else 0L

    @Synchronized
    fun beginIdleStop(nowMs: Long): Boolean =
        isRunning() && idleRemainingMs(nowMs) == 0L && beginStop()

    @Synchronized
    fun beginStop(): Boolean {
        if (!isRunning()) return false
        state = State.STOPPING
        return true
    }

    @Synchronized
    fun claimStopRequest(nowMs: Long): Boolean {
        if (state != State.STOPPING || stopDeadlineMs != null) return false
        stopDeadlineMs = nowMs + STOP_TIMEOUT_MS
        return true
    }

    @Synchronized
    fun stopRemainingMs(nowMs: Long): Long =
        stopDeadlineMs?.let { (it - nowMs).coerceAtLeast(0L) } ?: 0L

    @Synchronized
    fun settleRunning(): Boolean {
        if (!settle(State.RUNNING)) return false
        hasSettledRunning = true
        return true
    }

    @Synchronized
    fun settleStopped(): Boolean = settle(State.STOPPING)

    private fun settle(expectedState: State): Boolean {
        if (state != expectedState) return false
        state = State.SETTLED
        return true
    }

    @Synchronized
    fun isComplete(): Boolean = session.isComplete(expectedSize)

    @Synchronized
    override fun close() {
        beginStop()
        session.close()
    }

    companion object {
        const val IDLE_TIMEOUT_MS = 30_000L
        const val DATA_RENEW_INTERVAL_MS = 2_000L
        const val STOP_TIMEOUT_MS = 15_000L
    }
}
