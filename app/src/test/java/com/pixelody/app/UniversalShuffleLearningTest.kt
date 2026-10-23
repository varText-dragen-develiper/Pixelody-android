package com.pixelody.app

import com.pixelody.app.core.shuffle.UniversalShuffleLearning as L
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class UniversalShuffleLearningTest {
    private fun fixtures(name: String): File {
        val relative = "docs/api-contract-fixtures/shuffle-universal-v2/$name"
        return listOf(File(relative), File("../$relative"), File("../../$relative"), File("C:/dev/music101-shuffle-universal-20261011/$relative"))
            .firstOrNull { it.isFile } ?: error("Missing shared fixture: $name")
    }
    private fun canonical(value: Any?): String = when (value) {
        null, JSONObject.NULL -> "null"
        is JSONObject -> value.keys().asSequence().sorted().joinToString(",", "{", "}") { JSONObject.quote(it) + ":" + canonical(value.get(it)) }
        is JSONArray -> (0 until value.length()).joinToString(",", "[", "]") { canonical(value.get(it)) }
        is Number -> JSONObject.numberToString(value)
        is Boolean -> value.toString()
        else -> JSONObject.quote(value.toString())
    }
    private val now = 1_800_000_000_000L
    private var sequence = 0
    private fun event(type: String, fields: JSONObject = JSONObject()) = JSONObject(fields.toString()).put("schemaVersion", 2).put("eventId", "event-${++sequence}").put("type", type)
    private fun reduce(ledger: JSONObject, type: String, fields: JSONObject = JSONObject(), clock: Long = now) = L.reduce(ledger, event(type, fields), JSONObject().put("nowMs", clock))
    private fun active(ledger: JSONObject = L.createLedger(JSONObject().put("nowMs", now)), occurrence: String = "occurrence") = reduce(ledger, "activated", JSONObject().put("trackId", "track").put("occurrenceId", occurrence).put("durationMs", 180_000))

    @Test fun allFrozenLearningTracesMatchEveryNormalizedEventStateAndSignals() {
        val traces = JSONObject(fixtures("learning-traces.json").readText()).getJSONArray("vectors")
        var count = 0
        for (i in 0 until traces.length()) {
            val vector = traces.getJSONObject(i)
            var ledger = JSONObject(vector.getJSONObject("initial").toString())
            val events = vector.getJSONArray("events")
            val contexts = vector.optJSONArray("contexts")
            for (j in 0 until events.length()) {
                val context = contexts?.getJSONObject(j) ?: JSONObject().put("nowMs", vector.getLong("nowMs"))
                val effective = JSONObject().put("nowMs", maxOf(context.getLong("nowMs"), ledger.getLong("clockFloorMs")))
                assertEquals("${vector.getString("name")}: normalized $j", canonical(vector.getJSONArray("normalizedEvents").get(j)), canonical(L.normalizeEvent(events.getJSONObject(j), effective)))
                val old = canonical(ledger)
                val oldReference = ledger
                ledger = L.reduce(ledger, events.getJSONObject(j), context)
                assertEquals("Input mutation at ${vector.getString("name")}:$j", old, canonical(oldReference))
                assertEquals("${vector.getString("name")}: state $j", canonical(vector.getJSONArray("states").get(j)), canonical(ledger))
                assertTrue(L.importLedger(ledger).getBoolean("ok"))
                count++
            }
            val expected = vector.getJSONObject("signals")
            if (expected.has("tastePoints")) {
                val track = events.getJSONObject(0).getString("trackId")
                assertEquals(vector.getString("name"), canonical(expected), canonical(L.trackSignals(ledger, track, JSONObject().put("nowMs", vector.getLong("nowMs")))))
            } else {
                expected.keys().forEach { track -> assertEquals("${vector.getString("name")}: signal $track", canonical(expected.get(track)), canonical(L.trackSignals(ledger, track, JSONObject().put("nowMs", vector.getLong("nowMs"))))) }
            }
        }
        assertEquals(36, count)
    }

    @Test fun configAndVersionsMatchFrozenMachineAndLearningContracts() {
        val machine = JSONObject(fixtures("machine-contract.json").readText())
        val learning = JSONObject(fixtures("learning-contract.json").readText())
        // Session marks never reach the ledger, so it keeps the v2.1 learning policy.
        assertEquals(machine.getString("learningPolicyVersion"), L.POLICY_VERSION)
        assertEquals(learning.getInt("schemaVersion"), L.VERSION)
        assertEquals(canonical(machine.getJSONObject("learningConstants")), canonical(JSONObject(L.CONFIG)))
        assertEquals(canonical(learning.getJSONObject("config")), canonical(JSONObject(L.CONFIG)))
    }

    @Test fun numericStringsLoneSurrogatesMalformedBooleansAndFutureEventsRejectWithoutConsumption() {
        val ledger = L.createLedger(JSONObject().put("nowMs", now))
        val bad = listOf(
            event("offered", JSONObject().put("trackId", "track").put("timestampMs", "123")),
            event("offered", JSONObject().put("trackId", "\uD800")),
            event("offered", JSONObject().put("trackId", "track").put("personal", 1)),
            event("offered", JSONObject().put("trackId", "track").put("timestampMs", now + 300_001)),
            event("activated", JSONObject().put("trackId", "track").put("occurrenceId", "x".repeat(16385)))
        )
        bad.forEach { assertSame(ledger, L.reduce(ledger, it, JSONObject().put("nowMs", now))) }
        val occurrence = JSONObject().put("trackId", "track").put("occurrenceId", "[\"" + "\\u0000".repeat(1024) + "\",1]")
        assertEquals(1, reduce(ledger, "activated", occurrence).getJSONArray("tracks").length())
    }

    @Test fun disabledAndRemoteActionsDoNotAlterEvidenceOrEnableControls() {
        val active = active()
        assertSame(active, L.setEnabled(active, false, JSONObject().put("eventId", "remote").put("source", "remote"), JSONObject().put("nowMs", now)))
        val disabled = L.setEnabled(active, false, JSONObject().put("eventId", "disable"), JSONObject().put("nowMs", now))
        assertTrue(disabled.getJSONArray("occurrences").getJSONObject(0).getBoolean("ended"))
        assertEquals(0, L.trackSignals(disabled, "track").getInt("exposure"))
        val reset = L.resetLearning(disabled, JSONObject().put("eventId", "reset"), JSONObject().put("nowMs", now))
        assertEquals(1, reset.getInt("learningEpoch"))
        assertTrue(reset.getJSONArray("seenEventIds").length() > 0)
        val deleted = L.deleteLedger(reset)
        assertFalse(deleted.getBoolean("enabled"))
        assertEquals(2, deleted.getInt("learningEpoch"))
        assertEquals(0, deleted.getJSONArray("seenEventIds").length())
    }

    @Test fun importRejectsUnexpectedFieldsBadWatermarksAndLenientJsonWithoutOverwritingInput() {
        val ledger = active()
        val unexpected = L.exportLedger(ledger, JSONObject().put("includePrivate", true)).put("token", "private")
        assertEquals("invalid-ledger", L.importLedger(unexpected).getString("reason"))
        val badProducer = L.exportLedger(ledger, JSONObject().put("includePrivate", true)).put("producers", JSONArray().put(JSONObject().put("producerId", "p").put("through", 0).put("pending", JSONArray().put(1))))
        assertFalse(L.importLedger(badProducer).getBoolean("ok"))
        listOf("{", "{'schemaVersion':2}", "{schemaVersion:2}", "{\"a\":1,}", "[1,]", "true false", "01").forEach {
            assertEquals(it, "invalid-json", L.importLedger(it).getString("reason"))
            assertEquals(it, L.importLedger(it).getString("preserved"))
        }
        val future = JSONObject().put("schemaVersion", 99).put("future", JSONArray().put("keep"))
        assertEquals(canonical(future), canonical(L.importLedger(future).get("preserved")))
        assertThrows(IllegalStateException::class.java) { L.exportLedger(ledger) }
        val duplicate = "{\"schemaVersion\":99,\"schemaVersion\":2,\"policyVersion\":\"${L.POLICY_VERSION}\"}"
        assertEquals("invalid-ledger", L.importLedger(duplicate).getString("reason"))
        assertEquals(2, L.importLedger(duplicate).getJSONObject("preserved").getInt("schemaVersion"))
        assertEquals("invalid-ledger", L.importLedger("{\"schemaVersion\":2,\"policyVersion\":\"${L.POLICY_VERSION}\",\"revision\":1e400}").getString("reason"))
    }

    @Test fun producerWatermarkSurvivesJournalAndEventIdPruning() {
        var ledger = L.createLedger(JSONObject().put("nowMs", now))
        val first = event("offered", JSONObject().put("trackId", "track").put("producerId", "producer").put("sequence", 1))
        ledger = L.reduce(ledger, first, JSONObject().put("nowMs", now))
        for (i in 2..4098) ledger = reduce(ledger, "offered", JSONObject().put("trackId", "track").put("producerId", "producer").put("sequence", i))
        assertEquals(512, ledger.getJSONArray("journal").length())
        assertEquals(4096, ledger.getJSONArray("seenEventIds").length())
        assertSame(ledger, L.reduce(ledger, first, JSONObject().put("nowMs", now)))
        assertEquals(4098, ledger.getJSONArray("tracks").getJSONObject(0).getInt("offered"))
        assertEquals(4098, ledger.getJSONArray("producers").getJSONObject(0).getInt("through"))
        assertTrue(L.importLedger(ledger).getBoolean("ok"))
    }
}
