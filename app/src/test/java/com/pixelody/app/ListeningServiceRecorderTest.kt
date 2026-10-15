package com.pixelody.app

import com.pixelody.app.core.analytics.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Exercises the real FIFO service helper and real durable files, without a UI owner. */
class ListeningServiceRecorderTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun sample(elapsed: Long, position: Long = elapsed, playing: Boolean = true, entry: String = "entry") =
        ListeningPlaybackSample(entry, "session", ListeningTrack("track", durationMs = 10_000), position, elapsed, 1_000_000 + elapsed, playing)
    private fun snapshot(dir: File) = ListeningLedgerStore(dir).snapshot()

    @Test fun developerResetClearsCompletedAndPendingFactsWithoutReplayingStaleOutcomes() {
        val directory = temporary.newFolder()
        val recorder = ListeningServiceRecorder(directory)
        recorder.sample(sample(0))
        recorder.sample(sample(1_000))
        recorder.end(sample(1_000), ListeningEndReason.USER_STOP)
        recorder.awaitIdle()
        val oldEvent = snapshot(directory).recentEvents.single()
        recorder.sample(sample(2_000, entry = "pending"))
        recorder.resetHistory().get(5, java.util.concurrent.TimeUnit.SECONDS)
        val cleared = snapshot(directory)
        assertNull(cleared.pending)
        assertTrue(cleared.recentEvents.isEmpty())
        assertTrue(cleared.tracks.isEmpty())
        assertTrue(cleared.days.isEmpty())
        assertFalse(ListeningLedgerStore(directory).record(oldEvent))
        val backup = org.json.JSONObject(File(directory, "ledger.previous.json").readText())
        assertEquals(0, backup.getJSONArray("recentEvents").length())
        recorder.sample(sample(3_000, entry = "fresh"))
        recorder.sample(sample(4_000, entry = "fresh"))
        recorder.close(sample(4_000, entry = "fresh"))
        recorder.awaitIdle()
        val event = snapshot(directory).recentEvents.single()
        assertEquals("fresh", event.occurrence.entryId)
        assertTrue(event.occurrence.ordinal > oldEvent.occurrence.ordinal)
        assertEquals(1_000L, event.checkpoint.listenedWallMs)
    }

    @Test fun pausedColdRestoreCreatesNoActivationOrHeardEvent() {
        val directory = temporary.newFolder()
        val recorder = ListeningServiceRecorder(directory)
        recorder.sample(sample(0, 5_000, false))
        recorder.sample(sample(3_000, 5_000, false))
        recorder.close(sample(3_000, 5_000, false))
        recorder.awaitIdle()
        assertTrue(snapshot(directory).recentEvents.isEmpty())
        assertTrue(snapshot(directory).tracks.isEmpty())
    }

    @Test fun backgroundSamplesWithoutAnyUiPersistActualListeningAcrossRestart() {
        val directory = temporary.newFolder()
        val recorder = ListeningServiceRecorder(directory)
        (0L..6L).forEach { recorder.sample(sample(it * 1_000)) }
        recorder.end(sample(6_000), ListeningEndReason.USER_STOP)
        recorder.close(null)
        recorder.awaitIdle()
        val restored = snapshot(directory)
        assertEquals(6_000L, restored.recentEvents.single().checkpoint.listenedWallMs)
        assertEquals(6_000L, restored.tracks.getValue("track").listenedWallMs)
        assertFalse(restored.recentEvents.single().personalSkip)
    }

    @Test fun seekToEndThroughServiceDoesNotManufactureCompletion() {
        val directory = temporary.newFolder()
        val recorder = ListeningServiceRecorder(directory)
        recorder.sample(sample(0))
        recorder.sample(sample(1_000))
        recorder.discontinuity(1_000, sample(1_000, 9_000))
        recorder.end(sample(2_000, 10_000, false), ListeningEndReason.NATURAL_END)
        recorder.close(null)
        recorder.awaitIdle()
        val event = snapshot(directory).recentEvents.single()
        assertEquals(2_000L, event.checkpoint.listenedWallMs)
        assertEquals(0.2, event.checkpoint.coverageRatio!!, 0.0001)
        assertFalse(event.naturalCompletion)
    }

    @Test fun genuineRepeatOfSameEntryGetsDistinctOccurrenceAndEventIds() {
        val directory = temporary.newFolder()
        val recorder = ListeningServiceRecorder(directory)
        (0L..10L).forEach { recorder.sample(sample(it * 1_000)) }
        recorder.discontinuity(10_000, sample(10_000, 0), ListeningEndReason.NATURAL_END)
        (11L..12L).forEach { recorder.sample(sample(it * 1_000, (it - 10) * 1_000)) }
        recorder.close(sample(12_000, 2_000, false))
        recorder.awaitIdle()
        val events = snapshot(directory).recentEvents
        assertEquals(2, events.size)
        assertEquals(events[0].occurrence.entryId, events[1].occurrence.entryId)
        assertNotEquals(events[0].occurrence.occurrenceId, events[1].occurrence.occurrenceId)
        assertNotEquals(events[0].occurrence.eventId, events[1].occurrence.eventId)
        assertTrue(events[0].naturalCompletion)
        assertFalse(events[1].naturalCompletion)
        assertEquals(12_000L, snapshot(directory).tracks.getValue("track").listenedWallMs)
    }

    @Test fun shutdownAndInfrastructureInterruptionNeverLearnPersonalDislike() {
        val directory = temporary.newFolder()
        val recorder = ListeningServiceRecorder(directory)
        recorder.sample(sample(0))
        recorder.end(sample(1_000), ListeningEndReason.INFRASTRUCTURE)
        recorder.sample(sample(2_000, 1_000))
        recorder.close(sample(3_000, 2_000))
        recorder.awaitIdle()
        val events = snapshot(directory).recentEvents
        assertEquals(listOf(ListeningEndReason.INFRASTRUCTURE, ListeningEndReason.SHUTDOWN), events.map { it.reason })
        assertTrue(events.none { it.personalSkip || it.naturalCompletion })
        assertEquals(0L, snapshot(directory).tracks.getValue("track").personalSkipCount)
    }

    @Test fun pendingCrashCheckpointRecoversOnceAndRemainsPausedUntilActualPlay() {
        val directory = temporary.newFolder()
        val first = ListeningLedgerStore(directory)
        val occurrence = first.reserveOccurrence("before-crash", "entry", "previous-service", ListeningTrack("track", durationMs = 10_000), 100_000)
        val accumulator = RealListeningAccumulator()
        accumulator.start(occurrence, 0, 0, true)
        accumulator.sample(1_000, 1_000, true)
        first.checkpoint(accumulator.checkpoint()!!)
        val recorder = ListeningServiceRecorder(directory)
        recorder.sample(sample(0, 1_000, false))
        recorder.sample(sample(1_000, 1_000, false))
        recorder.close(null)
        recorder.awaitIdle()
        val events = snapshot(directory).recentEvents
        assertEquals(1, events.size)
        assertEquals(ListeningEndReason.SHUTDOWN, events.single().reason)
        assertEquals(1_000L, events.single().checkpoint.listenedWallMs)
        assertNull(snapshot(directory).pending)
    }
}
