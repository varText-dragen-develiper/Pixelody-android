package com.pixelody.app

import com.pixelody.app.core.shuffle.UniversalShufflePolicy
import com.pixelody.app.core.shuffle.UniversalShuffleScopes
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class UniversalShuffleScopesTest {
    private fun fixture(): JSONObject = JSONObject(javaClass.getResourceAsStream("/universal-shuffle-scope-vectors.json")!!
        .bufferedReader(Charsets.UTF_8).use { it.readText() })
    private fun session(scope: String): JSONObject = UniversalShufflePolicy.createSession(JSONObject()
        .put("sessionId", "session-$scope").put("scopeId", scope).put("authorityId", "local")
        .put("seed", "scope-tests").put("mode", "balanced")
        .put("entries", JSONArray().put(JSONObject().put("entryId", "e1").put("trackId", "same"))
            .put(JSONObject().put("entryId", "e2").put("trackId", "same"))))

    @Test fun scopeReducersMatchJavaScriptGoldenEnvelopes() {
        val fixture = fixture()
        assertEquals(UniversalShufflePolicy.POLICY_VERSION, fixture.getString("policyVersion"))
        val vectors = fixture.getJSONArray("vectors")
        for (index in 0 until vectors.length()) {
            val vector = vectors.getJSONObject(index)
            val bank = vector.getJSONObject("bank")
            val before = UniversalShufflePolicy.canonicalJson(bank)
            val actual = when (vector.getString("operation")) {
                "save" -> UniversalShuffleScopes.save(bank, vector.getJSONObject("session"), vector.getJSONObject("context"))
                "restore" -> UniversalShuffleScopes.restore(bank, vector.getString("scopeId"))
                "evict" -> UniversalShuffleScopes.evict(bank, vector.getString("scopeId"), vector.optJSONObject("context") ?: JSONObject())
                else -> error("Unknown fixture operation")
            }
            assertEquals("scope vector $index", UniversalShufflePolicy.canonicalJson(vector.getJSONObject("expected")), UniversalShufflePolicy.canonicalJson(actual))
            assertEquals("Input bank mutated $index", before, UniversalShufflePolicy.canonicalJson(bank))
        }
    }

    @Test fun fullBankRequiresExplicitEvictionAndRetainsUnfinishedEntitlements() {
        var bank = UniversalShuffleScopes.createBank()
        repeat(32) { index -> bank = UniversalShuffleScopes.save(bank, session("scope-$index")).getJSONObject("bank") }
        val original = UniversalShufflePolicy.canonicalJson(bank)
        val rejected = UniversalShuffleScopes.save(bank, session("new-scope"))
        assertEquals("capacity-requires-explicit-eviction", rejected.getString("status"))
        assertEquals(32, rejected.getJSONArray("unfinished").length())
        assertEquals(original, UniversalShufflePolicy.canonicalJson(rejected.getJSONObject("bank")))
        val evicted = UniversalShuffleScopes.evict(bank, "scope-3", JSONObject().put("nowMs", 900L))
        assertEquals(2, evicted.getJSONObject("diagnostic").getInt("unfinishedEntitlements"))
        assertFalse(evicted.getJSONObject("diagnostic").getBoolean("completed"))
        bank = UniversalShuffleScopes.save(evicted.getJSONObject("bank"), session("new-scope")).getJSONObject("bank")
        assertEquals(32, bank.getJSONArray("scopes").length())
        assertEquals("explicitly-evicted", UniversalShuffleScopes.restore(bank, "scope-3").getString("status"))
    }

    @Test fun overwritesAreAllowedAtCapacityAndSnapshotsAreIndependent() {
        var bank = UniversalShuffleScopes.createBank()
        repeat(32) { bank = UniversalShuffleScopes.save(bank, session("scope-$it")).getJSONObject("bank") }
        val state = session("scope-3")
        bank = UniversalShuffleScopes.save(bank, state, JSONObject().put("nowMs", 10L)).getJSONObject("bank")
        state.getJSONArray("remaining").remove(0)
        assertEquals(2, UniversalShuffleScopes.restore(bank, "scope-3").getJSONObject("state").getJSONArray("remaining").length())
        assertEquals(32, bank.getJSONArray("scopes").length())
        val ids = (0 until 32).map { bank.getJSONArray("scopes").getJSONObject(it).getString("scopeId") }
        assertEquals(ids.sorted(), ids)
    }

    @Test fun evictionDiagnosticsAreBoundedAndLatestEvictionIsReturned() {
        var bank = UniversalShuffleScopes.createBank()
        repeat(150) { index ->
            bank = UniversalShuffleScopes.save(bank, session("scope")).getJSONObject("bank")
            bank = UniversalShuffleScopes.evict(bank, "scope", JSONObject().put("nowMs", index)).getJSONObject("bank")
        }
        assertEquals(128, bank.getJSONArray("evictions").length())
        val diagnostic = UniversalShuffleScopes.restore(bank, "scope").getJSONArray("diagnostics").getJSONObject(0)
        assertEquals(149, diagnostic.getInt("atMs")); assertEquals(2, diagnostic.getInt("unfinishedEntitlements"))
    }

    @Test fun invalidClockContextUsesZeroAndUnknownBankIsNeverReplaced() {
        listOf<Any>(-1, 0.5, "123", true, 9007199254740992.0, JSONObject.NULL).forEach { value ->
            val saved = UniversalShuffleScopes.save(UniversalShuffleScopes.createBank(), session("scope"), JSONObject().put("nowMs", value))
            assertEquals(0L, saved.getJSONObject("bank").getJSONArray("scopes").getJSONObject(0).getLong("savedAtMs"))
        }
        val future = JSONObject().put("schemaVersion", 3).put("futureField", "retained")
        assertEquals("retained", UniversalShuffleScopes.save(future, session("scope")).getJSONObject("preserved").getString("futureField"))
        assertEquals("retained", UniversalShuffleScopes.restore(future, "scope").getJSONObject("preserved").getString("futureField"))
    }
}
