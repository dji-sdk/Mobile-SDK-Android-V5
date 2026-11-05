package com.dji.open.pilot.plugin.sampleitem.mapmanager

/** All calls into this queue must be on the scheduler's single thread. */
internal class ElementRequestQueue(private val scheduler: Scheduler) {
    interface Scheduler { fun post(delayMs: Long, action: () -> Unit) }
    internal data class Job(val key: String, val start: (Task) -> Unit, val onCancel: () -> Unit)
    private val pending = java.util.ArrayDeque<Job>()
    private var active: Task? = null

    fun enqueue(key: String, start: (Task) -> Unit, onCancel: () -> Unit) {
        if (active?.job?.key == key || pending.any { it.key == key }) return
        pending.add(Job(key, start, onCancel))
        startNext()
    }

    fun cancel() {
        val callbacks = listOfNotNull(active?.job?.onCancel) + pending.map { it.onCancel }
        active = null
        pending.clear()
        callbacks.forEach { it() }
    }

    private fun startNext() {
        if (active != null || pending.isEmpty()) return
        val task = Task(pending.removeFirst())
        active = task
        task.job.start(task)
    }

    inner class Task internal constructor(internal val job: Job) {
        private var sequence = 0L
        private var waitingToken: Long? = null
        fun request(onTimeout: () -> Unit): Long {
            check(active === this && waitingToken == null)
            val token = ++sequence
            waitingToken = token
            scheduler.post(15_000) {
                if (accept(token)) onTimeout()
            }
            return token
        }

        fun reply(token: Long, action: () -> Unit) {
            scheduler.post(0) { if (accept(token)) action() }
        }

        fun later(delayMs: Long, action: () -> Unit) {
            scheduler.post(delayMs) { if (active === this) action() }
        }

        private fun accept(token: Long): Boolean {
            if (active !== this || waitingToken != token) return false
            waitingToken = null
            return true
        }

        fun finish() {
            if (active !== this) return
            waitingToken = null
            active = null
            startNext()
        }
    }
}
