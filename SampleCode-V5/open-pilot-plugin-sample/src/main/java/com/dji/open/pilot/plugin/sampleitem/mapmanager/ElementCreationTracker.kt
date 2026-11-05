package com.dji.open.pilot.plugin.sampleitem.mapmanager

import java.util.UUID

/** Process-local creation uncertainty outlives UI tasks and controllers. All access is on main. */
internal class ElementCreationTracker {
    private val pendingTypes = mutableSetOf<String>()

    fun begin(type: String): Attempt? = if (pendingTypes.add(type)) Attempt(type) else null

    fun availableTypes(types: List<String>, ownedTypes: Collection<String>): List<String> =
        types.filter { it !in pendingTypes && it !in ownedTypes }

    inner class Attempt internal constructor(private val type: String) {
        private var isSettled = false

        fun success(id: UUID, claim: (UUID, String) -> Unit): Boolean {
            if (!settle()) return false
            claim(id, type)
            return true
        }

        fun failure(): Boolean = settle()

        private fun settle(): Boolean {
            if (isSettled) return false
            isSettled = true
            pendingTypes.remove(type)
            return true
        }
    }
}
