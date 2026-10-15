package com.pixelody.app

import com.pixelody.app.core.analytics.*
import com.pixelody.app.core.shuffle.UniversalShuffleLearning as L
import com.pixelody.app.data.storage.PlaybackSessionCheckpoint
import com.pixelody.app.data.storage.PlaybackSessionCodec
import com.pixelody.app.data.storage.VersionedPlaybackSessionStore
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.TimeUnit

class UniversalListeningRecorderTest {
    @get:Rule val temporary = TemporaryFolder()
    private val now = 1_800_000_000_000L
    private fun sample(time: Long, position: Long = time, playing: Boolean = true, duration: Long = 10_000, personal: Boolean = true, speed: Float = 1f) =
        ListeningPlaybackSample("entry", "session", ListeningTrack("track", durationMs = duration), position, time, now + time, playing, speed, personal)
    private fun row(ledger: JSONObject) = ledger.getJSONArray("tracks").getJSONObject(0)

    @Test fun actualNaturalListeningAndSignalsSurviveProcessLifetimeWithoutUi() {
        val directory = temporary.newFolder()
        val recorder = UniversalListeningRecorder(directory)
        (0L..10).forEach { recorder.sample(sample(it * 1000)) }
        recorder.end(sample(10_000, playing = false), ListeningEndReason.NATURAL_END)
        recorder.close(null, now + 10_000); recorder.awaitIdle()
        val snapshot = recorder.snapshot()
        assertEquals(10_000, row(snapshot).getInt("heardWallMs"))
        assertEquals(3, row(snapshot).getInt("positiveUnits"))
        assertEquals(1, row(snapshot).getInt("activated"))
        assertTrue(L.importLedger(snapshot).getBoolean("ok"))
        val restored = UniversalListeningRecorder(directory)
        restored.recover(now + 20_000).get(5, TimeUnit.SECONDS)
        assertEquals(3, row(restored.snapshot()).getInt("positiveUnits"))
        val context = restored.signals(listOf("track", "unseen", "track"), now + 20_000)
        assertEquals(now + 20_000, context.getLong("nowMs"))
        assertEquals(2, context.getJSONArray("signals").length())
        assertEquals("track", context.getJSONArray("signals").getJSONObject(0).getString("trackId"))
        restored.close(null, now + 20_000); restored.awaitIdle()
    }

    @Test fun pauseBufferFocusGapAndSeekToEndCannotEarnPositiveCompletion() {
        val recorder = UniversalListeningRecorder(temporary.newFolder())
        recorder.sample(sample(0, duration = 100_000))
        recorder.sample(sample(1000, playing = false, duration = 100_000))
        recorder.sample(sample(3000, 1000, false, 100_000))
        recorder.sample(sample(4000, 1000, true, 100_000))
        recorder.discontinuity(1000, sample(4000, 99_000, true, 100_000))
        recorder.end(sample(5000, 100_000, false, 100_000), ListeningEndReason.NATURAL_END)
        recorder.close(null, now + 5000); recorder.awaitIdle()
        assertEquals(2000, row(recorder.snapshot()).getInt("heardWallMs"))
        assertEquals(0, row(recorder.snapshot()).getInt("positiveUnits"))
        assertEquals(0, row(recorder.snapshot()).getInt("negativeUnits"))
    }

    @Test fun deliberatePersonalEarlySkipIsSeparatedFromRemoteAndInfrastructureOutcomes() {
        for ((reason, personal, expected) in listOf(
            Triple(ListeningEndReason.PERSONAL_SKIP, true, 2), Triple(ListeningEndReason.PERSONAL_SKIP, false, 0),
            Triple(ListeningEndReason.INFRASTRUCTURE, true, 0), Triple(ListeningEndReason.REPLACED, true, 0)
        )) {
            val recorder = UniversalListeningRecorder(temporary.newFolder())
            (0L..8).forEach { recorder.sample(sample(it * 1000, duration = 100_000, personal = personal)) }
            recorder.end(sample(8000, duration = 100_000, personal = personal), reason)
            recorder.close(null, now + 8000); recorder.awaitIdle()
            val tracks = recorder.snapshot().getJSONArray("tracks")
            assertEquals(expected, if (tracks.length() == 0) 0 else tracks.getJSONObject(0).getInt("negativeUnits"))
        }
    }

    @Test fun repeatedOfferDecisionDoesNotCountAgainAndDisableResetDeletionStaySeparate() {
        val recorder = UniversalListeningRecorder(temporary.newFolder())
        recorder.offered("track", now, "decision|é")
        recorder.offered("track", now + 1, "decision|é")
        recorder.awaitIdle()
        assertEquals(1, row(recorder.snapshot()).getInt("offered"))
        recorder.offered("track", now + 1, "distinct-decision").get(5, TimeUnit.SECONDS)
        assertEquals(2, row(recorder.snapshot()).getInt("offered"))
        val producer = recorder.snapshot().getJSONArray("producers").getJSONObject(0)
        assertEquals(2, producer.getInt("through"))
        assertEquals(0, producer.getJSONArray("pending").length())
        recorder.setEnabled(false, now + 2)
        recorder.sample(sample(1000))
        recorder.awaitIdle()
        assertEquals(0, row(recorder.snapshot()).getInt("activated"))
        recorder.setEnabled(true, now + 2000)
        recorder.preference("track", 1, now + 2000)
        recorder.deleteHistory(now + 2001)
        recorder.awaitIdle()
        assertEquals(1, row(recorder.snapshot()).getInt("explicitPreference"))
        assertThrows(java.util.concurrent.ExecutionException::class.java) { recorder.exportPrivate().get(5, TimeUnit.SECONDS) }
        assertTrue(recorder.exportPrivate(true).get(5, TimeUnit.SECONDS).has("tracks"))
        recorder.resetLearning(now + 2002).get(5, TimeUnit.SECONDS)
        assertEquals(1, recorder.snapshot().getInt("learningEpoch"))
        assertTrue(recorder.snapshot().getJSONArray("seenEventIds").length() > 0)
        recorder.deleteLedger(now + 2003).get(5, TimeUnit.SECONDS)
        assertFalse(recorder.snapshot().getBoolean("enabled"))
        assertEquals(0, recorder.snapshot().getJSONArray("seenEventIds").length())
        recorder.close(null, now + 3000); recorder.awaitIdle()
    }

    @Test fun actualSpeedChangesCountWallTimeSeparatelyFromUniqueCoverage() {
        val recorder = UniversalListeningRecorder(temporary.newFolder())
        recorder.sample(sample(0, 0, duration = 30_000))
        (1L..5).forEach { recorder.sample(sample(it * 1000, it * 1000, duration = 30_000)) }
        // A speed-change callback closes the old-speed interval at the same endpoint.
        recorder.sample(sample(5000, 5000, duration = 30_000, speed = 2f))
        (6L..15).forEach { recorder.sample(sample(it * 1000, 5000 + (it - 5) * 2000, duration = 30_000, speed = 2f)) }
        recorder.end(sample(15_000, 25_000, false, 30_000, speed = 2f), ListeningEndReason.NATURAL_END)
        recorder.close(null, now + 15_000); recorder.awaitIdle()
        val ledger = recorder.snapshot()
        assertEquals(15_000, row(ledger).getInt("heardWallMs"))
        assertEquals(25_000L, L.coverageMs(ledger.getJSONArray("occurrences").getJSONObject(0)))
        assertEquals(3, row(ledger).getInt("positiveUnits"))
    }

    @Test fun privateImportPreservesInvalidInputAndStartsFreshProducerAfterValidImport() {
        val recorder = UniversalListeningRecorder(temporary.newFolder())
        recorder.offered("track", now, "before").get(5, TimeUnit.SECONDS)
        val exported = recorder.exportPrivate(true).get(5, TimeUnit.SECONDS)
        val original = recorder.snapshot().toString()
        val rejected = recorder.importPrivate(JSONObject(exported.toString()).put("policyVersion", "future-policy"), now + 1).get(5, TimeUnit.SECONDS)
        assertFalse(rejected.getBoolean("ok"))
        assertEquals(original, recorder.snapshot().toString())
        val accepted = recorder.importPrivate(exported, now + 2).get(5, TimeUnit.SECONDS)
        assertTrue(accepted.getBoolean("ok"))
        accepted.getJSONObject("ledger").put("enabled", false)
        assertTrue(recorder.snapshot().getBoolean("enabled"))
        recorder.offered("track", now + 3, "after").get(5, TimeUnit.SECONDS)
        val ledger = recorder.snapshot()
        assertEquals(2, row(ledger).getInt("offered"))
        assertEquals(2, ledger.getJSONArray("producers").length())
        assertTrue(L.importLedger(ledger).getBoolean("ok"))
        recorder.close(null, now + 3); recorder.awaitIdle()
    }

    @Test fun tenThousandTrackContextMatchesFrozenSignalsAndIsDetached() {
        val directory = temporary.newFolder()
        val ledger = L.reduce(L.createLedger(JSONObject().put("nowMs", now)), JSONObject().put("schemaVersion", 2).put("eventId", "offer")
            .put("type", "offered").put("trackId", "template"), JSONObject().put("nowMs", now))
        val template = row(ledger).toString()
        val ids = (0 until 10_000).map { "track-${it.toString().padStart(5, '0')}" }
        ledger.put("tracks", JSONArray().apply { ids.forEach { put(JSONObject(template).put("trackId", it)) } })
        // The journal may mention deleted tracks; only live occurrences require a row.
        val wrapper = JSONObject().put("bridgeVersion", 1).put("producerId", "context-producer").put("sequence", 0).put("ledger", ledger)
        assertTrue(VersionedPlaybackSessionStore(directory, "universal-learning").save(PlaybackSessionCheckpoint("universal-learning", 1, emptyList(), policyStateJson = wrapper.toString())).saved)
        val recorder = UniversalListeningRecorder(directory)
        recorder.awaitIdle(30_000)
        val measurements = mutableListOf<Long>()
        repeat(6) { round ->
            val start = System.nanoTime()
            val context = recorder.signals(ids, now + 1000)
            val elapsed = System.nanoTime() - start
            if (round > 0) measurements.add(elapsed)
            val actual = context.getJSONArray("signals")
            assertEquals(ids.size, actual.length())
            for (index in listOf(0, 1, 4999, 9999)) {
                val expected = L.trackSignals(ledger, ids[index], JSONObject().put("nowMs", now + 1000))
                val value = actual.getJSONObject(index)
                expected.keys().forEach { assertEquals("signal $index/$it", expected.get(it).toString(), value.get(it).toString()) }
            }
            actual.getJSONObject(0).put("tastePoints", 999)
        }
        assertEquals(0, recorder.signals(listOf(ids[0]), now).getJSONArray("signals").getJSONObject(0).getInt("tastePoints"))
        println("LEARNING_SIGNALS_10000_NS=" + measurements.joinToString(","))
        recorder.close(null, now + 1000); recorder.awaitIdle()
    }

    @Test fun genuineRepeatUsesNewOccurrenceAndReplayedActivationWhileCoverageDoesNotCarryOver() {
        val recorder = UniversalListeningRecorder(temporary.newFolder())
        (0L..10).forEach { recorder.sample(sample(it * 1000)) }
        recorder.discontinuity(10_000, sample(10_000, 0), ListeningEndReason.NATURAL_END)
        recorder.sample(sample(11_000, 1000))
        recorder.close(sample(12_000, 2000)); recorder.awaitIdle()
        val ledger = recorder.snapshot()
        assertEquals(2, row(ledger).getInt("activated"))
        assertEquals(1, row(ledger).getInt("replays"))
        assertEquals(3, row(ledger).getInt("positiveUnits"))
        assertEquals(2, ledger.getJSONArray("occurrences").length())
        assertNotEquals(ledger.getJSONArray("occurrences").getJSONObject(0).getString("occurrenceId"), ledger.getJSONArray("occurrences").getJSONObject(1).getString("occurrenceId"))
    }

    @Test fun coldEmptyRestoreClosesDurableOpenMeasurementWithoutNewActivation() {
        val directory = temporary.newFolder()
        val ledger = L.reduce(L.createLedger(JSONObject().put("nowMs", now)), JSONObject().put("schemaVersion", 2).put("eventId", "activation")
            .put("type", "activated").put("trackId", "track").put("occurrenceId", "before-crash").put("durationMs", 10_000), JSONObject().put("nowMs", now))
        val wrapper = JSONObject().put("bridgeVersion", 1).put("producerId", "fresh-bridge").put("sequence", 0).put("ledger", ledger)
        assertTrue(VersionedPlaybackSessionStore(directory, "universal-learning").save(PlaybackSessionCheckpoint("universal-learning", 1, emptyList(), policyStateJson = wrapper.toString())).saved)
        val recorder = UniversalListeningRecorder(directory)
        recorder.recover(now + 1000).get(5, TimeUnit.SECONDS)
        assertTrue(recorder.snapshot().getJSONArray("occurrences").getJSONObject(0).getBoolean("ended"))
        assertEquals(1, row(recorder.snapshot()).getInt("activated"))
        assertEquals(0, row(recorder.snapshot()).getInt("positiveUnits"))
        recorder.close(null, now + 1000); recorder.awaitIdle()
    }

    @Test fun futureInnerBackupIsPreservedReadOnlyWithNeutralSignals() {
        val directory = temporary.newFolder()
        val recorder = UniversalListeningRecorder(directory)
        recorder.offered("track", now, "first").get(5, TimeUnit.SECONDS)
        recorder.offered("track", now + 1, "second").get(5, TimeUnit.SECONDS)
        recorder.close(null, now + 1); recorder.awaitIdle()
        val store = VersionedPlaybackSessionStore(directory, "universal-learning")
        val old = PlaybackSessionCodec.decode(store.backupFile.readText()).checkpoint
        val wrapper = JSONObject(old.policyStateJson)
        wrapper.getJSONObject("ledger").put("policyVersion", "future-policy")
        val future = PlaybackSessionCodec.encode(old.copy(policyStateJson = wrapper.toString()))
        store.backupFile.writeText(future)
        val primaryBefore = store.primaryFile.readText()
        val restored = UniversalListeningRecorder(directory)
        restored.awaitIdle()
        assertTrue(restored.readOnly())
        assertEquals(0, restored.signals(listOf("track"), now).getJSONArray("signals").getJSONObject(0).getInt("noveltyPoints"))
        assertThrows(java.util.concurrent.ExecutionException::class.java) { restored.offered("other", now + 2).get(5, TimeUnit.SECONDS) }
        restored.close(null, now + 2); restored.awaitIdle()
        assertEquals(future, store.backupFile.readText())
        assertEquals(primaryBefore, store.primaryFile.readText())
    }

    @Test fun corruptInnerPrimaryRecoversBackupWithoutRotatingCorruptionOverIt() {
        val directory = temporary.newFolder()
        val recorder = UniversalListeningRecorder(directory)
        recorder.offered("track", now, "first").get(5, TimeUnit.SECONDS)
        recorder.offered("track", now + 1, "second").get(5, TimeUnit.SECONDS)
        recorder.close(null, now + 1); recorder.awaitIdle()
        val store = VersionedPlaybackSessionStore(directory, "universal-learning")
        val old = PlaybackSessionCodec.decode(store.primaryFile.readText()).checkpoint
        val wrapper = JSONObject(old.policyStateJson).put("ledger", JSONObject().put("schemaVersion", 2).put("policyVersion", L.POLICY_VERSION))
        val damaged = PlaybackSessionCodec.encode(old.copy(policyStateJson = wrapper.toString()))
        store.primaryFile.writeText(damaged)
        val restored = UniversalListeningRecorder(directory)
        restored.offered("track", now + 2, "third").get(5, TimeUnit.SECONDS)
        assertEquals(2, row(restored.snapshot()).getInt("offered"))
        restored.close(null, now + 2); restored.awaitIdle()
        assertTrue(directory.listFiles()!!.any { it.name.startsWith("universal-learning.json.corrupt-") && it.readText() == damaged })
        assertTrue(L.importLedger(JSONObject(PlaybackSessionCodec.decode(store.primaryFile.readText()).checkpoint.policyStateJson).get("ledger")).getBoolean("ok"))
    }
}
