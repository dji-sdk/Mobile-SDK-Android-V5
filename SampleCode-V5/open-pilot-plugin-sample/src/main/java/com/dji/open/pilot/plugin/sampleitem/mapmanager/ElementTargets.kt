package com.dji.open.pilot.plugin.sampleitem.mapmanager

import java.util.UUID

/** UUID ownership is independent of the host's delayed query snapshot. */
internal class ElementTargets<T>(private val ledger: ElementOwnershipLedger) {
    data class Remote<T>(val id: UUID, val type: String, val value: T)
    private val objects = mutableMapOf<UUID, T>()
    fun owned(): Map<UUID, String> = ledger.snapshot()
    fun resolved(): Map<UUID, T> = objects.filterKeys { it in owned() }
    fun deletionTargets(fallback: (UUID, String) -> T?): List<T> =
        owned().mapNotNull { (id, type) -> objects[id] ?: fallback(id, type) }
    fun reconcile(elements: List<Remote<T>>) {
        objects.clear()
        val ownership = owned()
        elements.filter { ownership.containsKey(it.id) }.forEach {
            objects[it.id] = it.value
        }
        ledger.refreshTypes(elements.associate { it.id to it.type })
    }
    fun missingTypes(types: List<String>): List<String> = types.filter { it !in owned().values }
    fun created(id: UUID, type: String, value: T) {
        ledger.claim(id, type)
        objects[id] = value
    }
    fun updated(id: UUID, type: String, value: T) {
        if (id !in owned()) return
        ledger.refreshTypes(mapOf(id to type))
        objects[id] = value
    }
    fun confirmDeleted(id: UUID) {
        ledger.remove(id)
        objects.remove(id)
    }
    fun deletion(ids: Set<UUID>) = Deletion(ids.intersect(owned().keys))
    class Deletion(private val ids: Set<UUID>) {
        private val missingCounts = mutableMapOf<UUID, Int>()
        private var queryCount = 0
        val canQuery: Boolean get() = queryCount < MAX_QUERIES
        fun observe(present: Set<UUID>?): Set<UUID> {
            if (!canQuery) return emptySet()
            queryCount++
            if (present == null) return emptySet()
            ids.forEach { id -> missingCounts[id] = if (id in present) 0 else (missingCounts[id] ?: 0) + 1 }
            return ids.filter { (missingCounts[it] ?: 0) >= 2 }.toSet()
        }
        private companion object { const val MAX_QUERIES = 3 }
    }
}
