package com.pixelody.app

import com.pixelody.app.core.shuffle.UniversalShufflePolicy as Policy
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import java.math.BigInteger

class UniversalShufflePolicyParityTest {
    private fun fixture(name: String): File {
        System.getProperty("pixelody.shuffle.fixtures")?.let { return File(it, name) }
        return generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .map { File(it, "docs/api-contract-fixtures/shuffle-universal-v2/$name") }.first { it.isFile }
    }
    private fun sha(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
    private fun exact(label: String, expected: Any?, actual: Any?) {
        val left = Policy.canonicalJson(expected); val right = Policy.canonicalJson(actual)
        if (left != right) {
            val index = left.indices.firstOrNull { it >= right.length || left[it] != right[it] } ?: minOf(left.length, right.length)
            fail("$label diverged at canonical char $index. Expected ${left.substring(maxOf(0, index - 100), minOf(left.length, index + 180))}; actual ${right.substring(maxOf(0, index - 100), minOf(right.length, index + 180))}")
        }
    }
    @Test fun everySharedGoldenStateDecisionAndCanonicalHashMatchesTheProductionKotlinReducer() {
        val root = JSONObject(fixture("traces.json").readText())
        assertEquals(Policy.POLICY_VERSION, root.getString("policyVersion"))
        val traces = root.getJSONArray("traces")
        var stepsChecked = 0
        for (index in 0 until traces.length()) {
            val trace = traces.getJSONObject(index); val name = trace.getString("name")
            val input = trace.getJSONObject("input"); val inputBefore = Policy.canonicalJson(input)
            var state = Policy.createSession(input)
            exact("$name initial", trace.getJSONObject("initialState"), state)
            assertEquals(inputBefore, Policy.canonicalJson(input))
            val steps = trace.getJSONArray("steps")
            for (stepIndex in 0 until steps.length()) {
                val step = steps.getJSONObject(stepIndex)
                val before = Policy.canonicalJson(state)
                val actual = Policy.reduce(state, step.getJSONObject("event"), step.getJSONObject("context"))
                assertEquals("$name step $stepIndex mutated its input", before, Policy.canonicalJson(state))
                state = actual
                exact("$name step $stepIndex", step.getJSONObject("expectedState"), state)
                exact("$name decision $stepIndex", step.opt("expectedDecision"), state.optJSONObject("pending")?.optJSONObject("receipt"))
                assertEquals("$name SHA step $stepIndex", step.getString("canonicalStateSha256"), sha(Policy.canonicalJson(state)))
                assertTrue(Policy.validateState(state))
                stepsChecked++
            }
        }
        assertEquals(9, traces.length())
        println("UniversalShufflePolicy parity: ${traces.length()} shared traces, $stepsChecked full state/decision/SHA steps")
    }

    private fun input(mode: String = "balanced", count: Int = 5): JSONObject = JSONObject()
        .put("sessionId", "session-🎵").put("scopeId", "scope|一").put("authorityId", "local")
        .put("seed", "portable-🎵-seed").put("mode", mode)
        .put("entries", JSONArray().apply { repeat(count) { put(JSONObject().put("entryId", "entry-$it").put("trackId", "track-$it")) } })

    @Test fun unsignedRngAndEveryBoundUseExactIntegerMultiplication() {
        assertEquals(3593680108L, Policy.hashSeed("portable-🎵-seed"))
        var rng = Policy.createRng("integer-mapping")
        for (bound in listOf(1L, 3L, 1000L, 1000000L, 128000000L)) repeat(100) {
            val next = Policy.draw(rng); val bounded = Policy.bounded(rng, bound)
            val expected = BigInteger.valueOf(next.getLong("value")).multiply(BigInteger.valueOf(bound)).shiftRight(32).toLong()
            assertEquals(expected, bounded.getLong("value")); exact("rng after bounded draw", next.getJSONObject("rng"), bounded.getJSONObject("rng"))
            rng = bounded.getJSONObject("rng")
        }
    }
    @Test fun allModesPreserveEveryEntitlementIncludingDeliberateTrackDuplicatesAndStop() {
        for (mode in Policy.MODES) {
            val raw = input(mode, 4)
            raw.getJSONArray("entries").getJSONObject(1).put("trackId", "track-0")
            var state = Policy.createSession(raw)
            repeat(4) { iteration ->
                val staged = Policy.stage(state, JSONObject().put("attemptId", "try-$iteration"))
                assertEquals(4 - iteration, staged.getJSONArray("remaining").length())
                state = Policy.commit(staged, staged.getJSONObject("pending"))
            }
            assertEquals(4, state.getJSONArray("history").length())
            assertEquals(0, state.getJSONArray("remaining").length())
            assertTrue(Policy.stage(state, JSONObject().put("attemptId", "stop")).isNull("pending"))
        }
    }
    @Test fun projectionsAreCancelableOneSelectionBatchesAndCannotMutateCommittedState() {
        val state = Policy.createSession(input("flow", 80)); val before = Policy.canonicalJson(state)
        val first = Policy.advanceProjection(Policy.createProjection(state), JSONObject().put("nowMs", 1000))
        assertEquals(1, first.getJSONArray("decisions").length())
        val stopped = Policy.advanceProjection(first, JSONObject().put("cancelled", true))
        assertTrue(stopped.getBoolean("done")); assertEquals(1, stopped.getJSONArray("decisions").length())
        Policy.project(state, JSONObject().put("horizon", 32))
        assertEquals(before, Policy.canonicalJson(state))
    }
    @Test fun futureAndCorruptSnapshotsRemainPreservedAndPendingRestoreStaysPaused() {
        val staged = Policy.stage(Policy.createSession(input()), JSONObject().put("attemptId", "not-confirmed"))
        val restored = Policy.restore(staged)
        assertEquals("restored", restored.getString("status")); assertTrue(restored.getJSONObject("state").getBoolean("paused"))
        assertTrue(restored.getJSONObject("state").isNull("pending"))
        exact("restored entitlement", staged.getJSONArray("remaining"), restored.getJSONObject("state").getJSONArray("remaining"))
        val future = JSONObject(staged.toString()).put("schemaVersion", 99)
        assertEquals("unsupported", Policy.restore(future).getString("status")); exact("preserved future", future, Policy.restore(future).get("preserved"))
        val corrupt = JSONObject(staged.toString()).put("remaining", JSONArray().put("missing"))
        assertEquals("corrupt", Policy.restore(corrupt).getString("status")); exact("preserved corrupt", corrupt, Policy.restore(corrupt).get("preserved"))
    }
    @Test fun commandsAreIdempotentAuthorityBoundAndProducerGapsFail() {
        var state = Policy.createSession(input())
        val event = JSONObject().put("type", "repeat-change").put("repeat", "all").put("eventId", "command-1").put("producerId", "controller").put("sequence", 1)
        state = Policy.reduce(state, event)
        assertSame(state, Policy.reduce(state, event))
        try { Policy.reduce(state, JSONObject(event.toString()).put("sequence", 3)); fail("Gap accepted") } catch (error: IllegalArgumentException) { assertTrue(error.message!!.contains("gap")) }
        val staged = Policy.stage(state, JSONObject().put("attemptId", "old-authority"))
        val changed = Policy.reduce(staged, JSONObject().put("type", "authority-change").put("authorityId", "other").put("authorityEpoch", 1))
        assertSame(changed, Policy.commit(changed, staged.getJSONObject("pending")))
    }
    @Test fun identitiesWhitespaceKeysAndNullZeroFeaturesMatchPortableRules() {
        assertEquals("é 東京 a", Policy.textIdentity("\uFEFFe\u0301\u2003東京\u00a0A\n"))
        assertEquals(1, Policy.parseKey("D♭ minor")!!.getInt("pitchClass"))
        assertEquals("major", Policy.parseKey("CM")!!.getString("mode")); assertEquals("minor", Policy.parseKey("Cm")!!.getString("mode"))
        assertEquals(9, Policy.parseKey("8A")!!.getInt("pitchClass")); assertNull(Policy.parseKey("fake"))
        val zero = JSONObject().put("entryId", "zero").put("trackId", "track").put("features", JSONObject().put("energy", JSONObject().put("value", 0).put("provenance", "measured")))
        assertEquals(0, Policy.normalizeEntry(zero).getJSONObject("features").getJSONObject("energy").getInt("value"))
        val unknown = JSONObject(zero.toString()); unknown.getJSONObject("features").getJSONObject("energy").put("value", JSONObject.NULL)
        assertTrue(Policy.normalizeEntry(unknown).getJSONObject("features").getJSONObject("energy").isNull("value"))
        try { Policy.normalizeEntry(JSONObject().put("entryId", "\ud800").put("trackId", "a")); fail("Invalid UTF16 accepted") } catch (_: IllegalArgumentException) { }
        assertEquals(Policy.hashSeed("a,,b"), Policy.hashSeed(JSONArray().put("a").put(JSONObject.NULL).put("b")))
        assertEquals(Policy.hashSeed("[object Object]"), Policy.hashSeed(JSONObject().put("private", "ignored by String(object)")))
    }
    @Test fun legacyMigrationAndPublicProjectionPreserveTraversalAndPrivateBoundary() {
        val raw = input("off", 3)
        val legacy = JSONObject().put("plan", JSONObject().put("style", "standard").put("order", JSONArray(listOf("track-0", "track-1", "track-0", "removed"))).put("cursor", 2))
        val migrated = Policy.migrateLegacy(legacy, raw)
        assertEquals(4, migrated.getJSONArray("history").length()); assertTrue(migrated.getJSONArray("history").getJSONObject(3).getBoolean("tombstone"))
        val staged = Policy.stage(Policy.createSession(raw), JSONObject().put("attemptId", "first"))
        val state = Policy.commit(staged, staged.getJSONObject("pending"))
        val mappers = listOf("authorityId", "trackId", "entryId", "occurrenceId", "decisionId").associateWith { { value: String -> sha(value).take(24) as String? } }
        val public = Policy.publicSnapshot(state, mappers, JSONObject().put("nowMs", 100000))
        val serialized = Policy.canonicalJson(public)
        assertFalse(serialized.contains("features")); assertFalse(serialized.contains("scope|")); assertFalse(serialized.contains("session-"))
        assertEquals(Policy.project(state).getJSONArray("entryIds").length(), public.getJSONArray("future").length())
    }

    @Test fun malformedFutureReservationsAndNonArrayPoolsAreNotSilentlyAccepted() {
        try { Policy.createSession(input().put("entries", "invalid")); fail("Nonarray pool accepted") } catch (_: IllegalArgumentException) { }
        val state = Policy.createSession(input())
        for (field in listOf("pending", "albumLock")) {
            val corrupt = JSONObject(state.toString()).put(field, true)
            assertEquals("corrupt", Policy.restore(corrupt).getString("status"))
            exact("primitive invalid $field retained", corrupt, Policy.restore(corrupt).get("preserved"))
        }
    }
}
