package com.pixelody.app.data.storage

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Durable source identities are independent of catalog sorting and membership revisions. */
class ShuffleScopeIdentityStore(context: Context) {
    private val preferences = context.getSharedPreferences("pixelody_shuffle_source_identities_v2", Context.MODE_PRIVATE)
    data class Source(val scopeId: String, val entryIds: List<String>)

    fun source(sourceKey: String?, trackIds: List<String>): Source = synchronized(monitor) {
        val envelope = JSONObject(preferences.getString("sources", "{}")!!)
        val version = envelope.opt("schemaVersion")
        require(!envelope.has("schemaVersion") || version is Number && version.toDouble() == 2.0) { "Unsupported source identities are preserved" }
        val root = if (envelope.has("schemaVersion")) envelope.getJSONObject("sources") else envelope
        val key = sourceKey ?: UUID.randomUUID().toString()
        val prior = root.optJSONObject(key)
        val scopeId = prior?.getString("scopeId") ?: UUID.randomUUID().toString()
        val existing = prior?.optJSONArray("entries") ?: JSONArray()
        val buckets = linkedMapOf<String, java.util.ArrayDeque<String>>()
        for (i in 0 until existing.length()) {
            val row = existing.getJSONObject(i)
            buckets.getOrPut(row.getString("trackId")) { java.util.ArrayDeque() }.add(row.getString("entryId"))
        }
        val records = JSONArray()
        val ids = trackIds.map { trackId ->
            val id = buckets[trackId]?.pollFirst() ?: UUID.randomUUID().toString()
            records.put(JSONObject().put("trackId", trackId).put("entryId", id))
            id
        }
        // Retain absent catalog IDs, so a temporary missing-media scan does not
        // reintroduce already consumed entitlement under a fresh source identity.
        buckets.forEach { (trackId, remaining) -> remaining.forEach { records.put(JSONObject().put("trackId", trackId).put("entryId", it)) } }
        if (sourceKey != null) {
            root.put(key, JSONObject().put("scopeId", scopeId).put("entries", records))
            check(preferences.edit().putString("sources", JSONObject().put("schemaVersion", 2).put("sources", root).toString()).commit()) { "Source identity checkpoint failed" }
        }
        Source(scopeId, ids)
    }

    private companion object { val monitor = Any() }
}
