package com.dji.open.pilot.plugin.sampleitem.mapmanager

import android.content.Context
import android.util.Log
import java.util.UUID

/** This private Sample ledger is the only source of ownership; names never confer ownership. */
internal class ElementOwnershipStore(context: Context) {
    private val preferences = context.getSharedPreferences("map_element_sample_ownership", Context.MODE_PRIVATE)

    fun load(): Map<UUID, String> {
        val ownership = mutableMapOf<UUID, String>()
        preferences.getStringSet(OWNED_IDS, emptySet()).orEmpty().forEach { entry ->
            val fields = entry.split('|', limit = 2)
            try {
                if (fields.size == 2) ownership[UUID.fromString(fields[0])] = fields[1]
            } catch (error: IllegalArgumentException) {
                Log.e(TAG, "Invalid ownership entry: $entry", error)
            }
        }
        return ownership
    }

    fun save(ownership: Map<UUID, String>): Boolean {
        val entries = ownership.map { "${it.key}|${it.value}" }.toSet()
        // Small ledger: finish the disk write before reporting a successful creation.
        val isSaved = preferences.edit().putStringSet(OWNED_IDS, entries).commit()
        if (!isSaved) Log.e(TAG, "Unable to persist Sample ownership: $entries")
        return isSaved
    }

    private companion object {
        const val TAG = "ElementOwnershipStore"
        const val OWNED_IDS = "owned_ids"
    }
}
