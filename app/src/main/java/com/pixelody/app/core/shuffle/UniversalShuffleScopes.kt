package com.pixelody.app.core.shuffle

import org.json.JSONArray
import org.json.JSONObject

/** Exact scope-bank reducer from https://github.com/varText-dragen-develiper/pixelody/blob/main/src/shuffle/scopes.js, universal-shuffle-v2.1. */
object UniversalShuffleScopes {
    const val VERSION = 2
    const val MAX_SCOPES = 32
    const val MAX_EVICTION_DIAGNOSTICS = 128
    private const val MAX_SAFE_INTEGER = 9007199254740991L

    fun createBank(): JSONObject = JSONObject().put("schemaVersion", VERSION)
        .put("scopes", JSONArray()).put("evictions", JSONArray())

    fun save(bank: JSONObject, session: JSONObject, context: JSONObject = JSONObject()): JSONObject {
        UniversalShufflePolicy.validateState(session)
        if (!supported(bank)) return unsupported(bank, includeBank = true)
        val scopes = rows(bank.getJSONArray("scopes"))
        val scopeId = session.getString("scopeId")
        val prior = scopes.firstOrNull { it.getString("scopeId") == scopeId }
        if (prior == null && scopes.size >= MAX_SCOPES) {
            return JSONObject().put("status", "capacity-requires-explicit-eviction").put("bank", bank)
                .put("unfinished", JSONArray(scopes.filter { it.getJSONObject("session").getJSONArray("remaining").length() > 0 }
                    .map { it.getString("scopeId") }))
        }
        val updated = scopes.filterNot { it.getString("scopeId") == scopeId } + JSONObject()
            .put("scopeId", scopeId).put("savedAtMs", timestamp(context))
            .put("session", JSONObject(session.toString()))
        val next = JSONObject(bank.toString()).put("scopes", JSONArray(updated.sortedBy { it.getString("scopeId") }))
        return JSONObject().put("status", "saved").put("bank", next)
    }

    fun restore(bank: JSONObject, scopeId: String): JSONObject {
        if (!supported(bank)) return unsupported(bank, includeBank = false).put("state", JSONObject.NULL)
        val row = rows(bank.getJSONArray("scopes")).firstOrNull { it.getString("scopeId") == scopeId }
        if (row != null) return UniversalShufflePolicy.restore(row.getJSONObject("session"))
        val eviction = rows(bank.getJSONArray("evictions")).lastOrNull { it.getString("scopeId") == scopeId }
        return JSONObject().put("status", if (eviction == null) "missing" else "explicitly-evicted")
            .put("state", JSONObject.NULL).put("diagnostics", JSONArray(if (eviction == null) emptyList<JSONObject>() else listOf(eviction)))
    }

    fun evict(bank: JSONObject, scopeId: String, context: JSONObject = JSONObject()): JSONObject {
        if (!supported(bank)) return unsupported(bank, includeBank = true)
        val scopes = rows(bank.getJSONArray("scopes"))
        val row = scopes.firstOrNull { it.getString("scopeId") == scopeId }
            ?: return JSONObject().put("status", "missing").put("bank", bank)
        val remaining = row.getJSONObject("session").getJSONArray("remaining").length()
        val diagnostic = JSONObject().put("scopeId", scopeId).put("atMs", timestamp(context))
            .put("unfinishedEntitlements", remaining).put("completed", remaining == 0).put("reason", "explicit-scope-eviction")
        val evictions = (rows(bank.getJSONArray("evictions")) + diagnostic).takeLast(MAX_EVICTION_DIAGNOSTICS)
        val next = JSONObject(bank.toString()).put("scopes", JSONArray(scopes.filterNot { it.getString("scopeId") == scopeId }))
            .put("evictions", JSONArray(evictions))
        return JSONObject().put("status", "evicted").put("bank", next).put("diagnostic", diagnostic)
    }

    private fun supported(bank: JSONObject): Boolean = bank.opt("schemaVersion") is Number &&
        (bank.opt("schemaVersion") as Number).toDouble() == VERSION.toDouble()

    private fun unsupported(bank: JSONObject, includeBank: Boolean): JSONObject = JSONObject()
        .put("status", "unsupported").put("preserved", bank).also { if (includeBank) it.put("bank", bank) }

    private fun rows(array: JSONArray): List<JSONObject> = List(array.length()) { array.getJSONObject(it) }

    private fun timestamp(context: JSONObject): Long {
        val number = context.opt("nowMs") as? Number ?: return 0L
        val value = number.toDouble()
        return if (value.isFinite() && value >= 0.0 && value <= MAX_SAFE_INTEGER.toDouble() && value == kotlin.math.floor(value)) value.toLong() else 0L
    }
}
