package com.dji.open.pilot.plugin.sampleitem.mapmanager

import java.util.UUID

/** Main-thread delta writes always read the latest shared ledger, never a controller's old snapshot. */
internal class ElementOwnershipLedger(
    private val read: () -> Map<UUID, String>,
    private val write: (Map<UUID, String>) -> Unit
) {
    fun snapshot(): Map<UUID, String> = read().toMap()
    fun claim(id: UUID, type: String) {
        val current = snapshot().toMutableMap()
        if (current[id] == type) return
        current[id] = type
        write(current.toMap())
    }

    fun remove(id: UUID) {
        val current = snapshot().toMutableMap()
        if (current.remove(id) != null) write(current.toMap())
    }

    fun refreshTypes(types: Map<UUID, String>) {
        val current = snapshot()
        val refreshed = current.mapValues { (id, type) -> types[id] ?: type }
        if (refreshed != current) write(refreshed)
    }
}
