package com.pixelody.app

import com.pixelody.app.core.analytics.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.ZoneId

class ListeningLedgerStoreTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun store(dir: File, maxEvents: Int = 256) = ListeningLedgerStore(dir, maxEvents, zoneId = ZoneId.of("UTC"))
    private fun event(store: ListeningLedgerStore, index: Int, reason: ListeningEndReason = ListeningEndReason.NATURAL_END): ListeningEvent {
        val occurrence = store.reserveOccurrence("occurrence:$index", "entry:$index", "session", ListeningTrack("track", durationMs = 1_000), 100_000 + index.toLong())
        val accumulator = RealListeningAccumulator()
        accumulator.start(occurrence, 0, 0, true)
        accumulator.sample(1_000, 1_000, true)
        return accumulator.finish(reason, 101_000 + index.toLong())!!
    }

    @Test fun replayOccurrencesRemainDistinctAndRetryAfterPruningIsIgnored() {
        val directory = temporary.newFolder()
        val ledger = store(directory, 2)
        val first = event(ledger, 1)
        assertTrue(ledger.record(first))
        assertFalse(ledger.record(first))
        (2..5).forEach { ledger.record(event(ledger, it)) }
        val restored = store(directory, 2)
        assertFalse(restored.record(first))
        val snapshot = restored.snapshot()
        assertEquals(2, snapshot.recentEvents.size)
        assertEquals(5L, snapshot.tracks.getValue("track").heardCount)
        assertEquals(5_000L, snapshot.tracks.getValue("track").listenedWallMs)
        assertEquals(5L, snapshot.days.getValue("1970-01-01").heardCount)
    }

    @Test fun pendingCrashRecoveryUsesLastCheckpointOnceWithoutDislikeOrCompletion() {
        val directory = temporary.newFolder()
        val ledger = store(directory)
        val event = event(ledger, 1)
        ledger.checkpoint(event.checkpoint)
        val restored = store(directory)
        assertTrue(restored.recoverInterrupted(110_000))
        assertFalse(restored.recoverInterrupted(110_000))
        val recovered = restored.snapshot().recentEvents.single()
        assertEquals(ListeningEndReason.SHUTDOWN, recovered.reason)
        assertFalse(recovered.naturalCompletion)
        assertFalse(recovered.personalSkip)
        assertEquals(1_000L, recovered.checkpoint.listenedWallMs)
        assertNull(restored.snapshot().pending)
    }

    @Test fun corruptPrimaryFallsBackToLastValidBackupAndPreservesDamagedBytes() {
        val directory = temporary.newFolder()
        val ledger = store(directory)
        ledger.record(event(ledger, 1))
        // Extra checkpoint creates a backup that already includes finalized occurrence 1.
        val second = event(ledger, 2)
        ledger.checkpoint(second.checkpoint)
        File(directory, "ledger.json").writeText("{torn")
        val restored = store(directory)
        assertFalse(restored.snapshot().readOnly)
        assertTrue(restored.snapshot().diagnostics.any { it.contains("backup") })
        assertEquals(1L, restored.snapshot().tracks.getValue("track").heardCount)
        restored.recoverInterrupted(120_000)
        val damaged = directory.listFiles()!!.single { it.name.startsWith("ledger.corrupt-") }
        assertEquals("{torn", damaged.readText())
        assertFalse(store(directory).snapshot().readOnly)
    }

    @Test fun bothCorruptCopiesArePreservedReadOnly() {
        val directory = temporary.newFolder()
        File(directory, "ledger.json").writeText("broken primary")
        File(directory, "ledger.previous.json").writeText("broken backup")
        val ledger = store(directory)
        assertTrue(ledger.snapshot().readOnly)
        assertThrows(IllegalStateException::class.java) { event(ledger, 1) }
        assertEquals("broken primary", File(directory, "ledger.json").readText())
        assertEquals("broken backup", File(directory, "ledger.previous.json").readText())
    }

    @Test fun futurePrimaryRemainsReadOnlyDespiteValidOlderBackup() {
        val directory = temporary.newFolder()
        val ledger = store(directory)
        ledger.record(event(ledger, 1))
        val future = JSONObject(File(directory, "ledger.json").readText()).put("schemaVersion", 99).put("newField", "preserve me").toString()
        File(directory, "ledger.json").writeText(future)
        val restored = store(directory)
        assertTrue(restored.snapshot().readOnly)
        assertThrows(IllegalStateException::class.java) { event(restored, 2) }
        assertEquals(future, File(directory, "ledger.json").readText())
    }

    @Test fun futureBackupIsPreservedAndStopsWritesEvenWithAValidPrimary() {
        val directory = temporary.newFolder()
        val ledger = store(directory)
        ledger.record(event(ledger, 1))
        val future = JSONObject(File(directory, "ledger.previous.json").readText()).put("schemaVersion", 2).toString()
        File(directory, "ledger.previous.json").writeText(future)
        val primaryBefore = File(directory, "ledger.json").readText()
        assertTrue(store(directory).snapshot().readOnly)
        assertThrows(IllegalStateException::class.java) { event(ledger, 2) }
        assertEquals(future, File(directory, "ledger.previous.json").readText())
        assertEquals(primaryBefore, File(directory, "ledger.json").readText())
    }

    @Test fun unversionedLegacyIsRetainedRatherThanInventingMigratedHistory() {
        val directory = temporary.newFolder()
        val legacy = "{\"memories\":[{\"trackId\":\"legacy\",\"completed\":true}]}"
        File(directory, "ledger.json").writeText(legacy)
        val snapshot = store(directory).snapshot()
        assertTrue(snapshot.readOnly)
        assertTrue(snapshot.recentEvents.isEmpty())
        assertEquals(legacy, File(directory, "ledger.json").readText())
    }

    @Test fun staleCheckpointCannotReplaceCurrentOccurrenceAndZeroHeardIsNotAListen() {
        val ledger = store(temporary.newFolder())
        val first = event(ledger, 1)
        ledger.record(first)
        val second = ledger.reserveOccurrence("second", "same-entry", "s", ListeningTrack("second-track"), 200_000)
        assertFalse(ledger.checkpoint(first.checkpoint))
        assertTrue(ledger.record(ListeningEvent(ListeningCheckpoint(second), 200_000, ListeningEndReason.INFRASTRUCTURE, false)))
        assertEquals(0L, ledger.snapshot().tracks.getValue("second-track").heardCount)
        assertEquals(0L, ledger.snapshot().tracks.getValue("second-track").personalSkipCount)
    }

    @Test fun injectedRollbackAndFutureTimestampsArePreservedWithoutReorderingOrdinals() {
        val ledger = store(temporary.newFolder())
        val first = event(ledger, 1).copy(endedAtEpochMs = 5_000_000)
        ledger.record(first)
        val second = event(ledger, 2).copy(endedAtEpochMs = 1)
        ledger.record(second)
        assertEquals(listOf(5_000_000L, 1L), ledger.snapshot().recentEvents.map { it.endedAtEpochMs })
        assertEquals(5_000_000L, ledger.snapshot().tracks.getValue("track").lastHeardAtEpochMs)
    }

    @Test fun aggregateCapacityDoesNotErasePreviouslyRecordedTracks() {
        val directory = temporary.newFolder()
        val ledger = ListeningLedgerStore(directory, maxTrackAggregates = 1, zoneId = ZoneId.of("UTC"))
        ledger.record(event(ledger, 1))
        assertThrows(IllegalStateException::class.java) {
            ledger.reserveOccurrence("other", "other", "s", ListeningTrack("other"), 200_000)
        }
        assertEquals(1L, store(directory).snapshot().tracks.getValue("track").heardCount)
    }

    @Test fun structurallyCorruptCurrentSchemaFallsBackInsteadOfClaimingLegacy() {
        val directory = temporary.newFolder()
        val ledger = store(directory)
        ledger.record(event(ledger, 1))
        event(ledger, 2) // Backup now contains the first finalized outcome.
        File(directory, "ledger.json").writeText("{\"schemaVersion\":1}")
        val restored = store(directory)
        assertFalse(restored.snapshot().readOnly)
        assertEquals(1L, restored.snapshot().tracks.getValue("track").heardCount)
        assertTrue(restored.snapshot().diagnostics.any { it.contains("backup") })
    }

    @Test fun activeWriterPreservesCorruptionThatAppearedAfterLoading() {
        val directory = temporary.newFolder()
        val ledger = store(directory)
        val first = event(ledger, 1)
        File(directory, "ledger.json").writeText("{damaged while active")
        assertTrue(ledger.record(first))
        assertEquals("{damaged while active", directory.listFiles()!!.single { it.name.startsWith("ledger.corrupt-") }.readText())
        assertEquals(1L, store(directory).snapshot().tracks.getValue("track").heardCount)
    }
}
