package com.pixelody.app

import com.pixelody.app.core.analytics.*
import com.pixelody.app.data.model.SonicArchetype
import com.pixelody.app.data.model.SonicMood
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Instant
import java.time.ZoneId

class ListeningLedgerProjectionTest {
    @get:Rule val temporary = TemporaryFolder()
    private val zone = ZoneId.of("America/Chicago")
    private val now = Instant.parse("2026-10-11T17:00:00Z").toEpochMilli()
    private fun recorded(
        ledger: ListeningLedgerStore, id: String, end: Long,
        reason: ListeningEndReason = ListeningEndReason.NATURAL_END,
        personal: Boolean = true
    ) {
        val occurrence = ledger.reserveOccurrence(id, "same-entry", "session", ListeningTrack("track", "Title", "Artist", durationMs = 120_000, format = "FLAC", isLossless = true), end - 60_000, personal)
        val accumulator = RealListeningAccumulator()
        accumulator.start(occurrence, 0, 0, true, 2f)
        (1..60).forEach { accumulator.sample(it * 1_000L, it * 2_000L, true, 2f) }
        ledger.record(accumulator.finish(reason, end)!!)
    }

    @Test fun emptyLedgerHasNoInventedStatsMoodOrPersonality() {
        val empty = ListeningLedgerSnapshot()
        val daily = ListeningLedgerProjection.toDailyCapsule(empty, now, zone)
        assertFalse(daily.hasData)
        assertEquals(SonicMood.Unknown, daily.primaryMood)
        assertEquals(SonicArchetype.Unknown, daily.archetype)
        assertEquals("", daily.aiNarrative)
        assertFalse(ListeningLedgerProjection.toWeeklyTrend(empty, now, zone).hasData)
        assertFalse(ListeningLedgerProjection.toInsights(empty).hasData)
    }

    @Test fun coverageDrivesCompletionWhileWallTimeDrivesMinutesAndReplayIdsStayDistinct() {
        val ledger = ListeningLedgerStore(temporary.newFolder(), zoneId = zone)
        recorded(ledger, "first", now - 120_000)
        recorded(ledger, "second", now - 60_000)
        val daily = ListeningLedgerProjection.toDailyCapsule(ledger.snapshot(), now, zone)
        assertEquals(2, daily.memoryTimeline.size)
        assertEquals(2, daily.memoryTimeline.map { it.id }.distinct().size)
        assertTrue(daily.memoryTimeline.all { it.completionRatio == 1f && it.listenedSeconds == 60 && it.replayCount == 1 })
        assertEquals(2, daily.audioDna.totalListeningMinutes)
        assertEquals(0, daily.audioDna.avgBpm)
        assertEquals("", daily.audioDna.dominantKey)
        assertEquals(0, daily.audioDna.bitPerfectPlayCount)
        assertEquals(SonicMood.Unknown, daily.primaryMood)
    }

    @Test fun explicitPersonalSkipIsDistinctFromShortInfrastructureOrRemoteListen() {
        val ledger = ListeningLedgerStore(temporary.newFolder(), zoneId = zone)
        recorded(ledger, "skip", now - 180_000, ListeningEndReason.PERSONAL_SKIP)
        recorded(ledger, "infra", now - 120_000, ListeningEndReason.INFRASTRUCTURE)
        recorded(ledger, "remote", now - 60_000, ListeningEndReason.PERSONAL_SKIP, false)
        val memories = ListeningLedgerProjection.toDailyCapsule(ledger.snapshot(), now, zone).memoryTimeline
        assertEquals(listOf(false, false, true), memories.map { it.wasSkipped })
    }

    @Test fun calendarDayAndWeekUseInjectedTimeZoneWithFutureDaysEmpty() {
        val ledger = ListeningLedgerStore(temporary.newFolder(), zoneId = zone)
        recorded(ledger, "today", now - 60_000)
        recorded(ledger, "yesterday", now - 86_400_000)
        recorded(ledger, "future-week", now + 86_400_000)
        val daily = ListeningLedgerProjection.toDailyCapsule(ledger.snapshot(), now, zone)
        assertEquals(1, daily.memoryTimeline.size)
        val week = ListeningLedgerProjection.toWeeklyTrend(ledger.snapshot(), now, zone)
        assertEquals(listOf(0, 0, 0, 0, 0, 1, 1), week.dailyMinutes)
        assertEquals(2, week.totalWeeklyMinutes)
        assertEquals(2, week.activeStreakDays)
    }

    @Test fun prunedEventDetailsDoNotEraseDurableDailyOrWeeklyTotals() {
        val ledger = ListeningLedgerStore(temporary.newFolder(), maxRecentEvents = 1, zoneId = zone)
        recorded(ledger, "first", now - 120_000)
        recorded(ledger, "second", now - 60_000)
        val daily = ListeningLedgerProjection.toDailyCapsule(ledger.snapshot(), now, zone)
        assertEquals(1, daily.memoryTimeline.size)
        assertEquals(2, daily.audioDna.tracksPlayedCount)
        assertEquals(2, daily.audioDna.totalListeningMinutes)
        assertEquals(2, ListeningLedgerProjection.toWeeklyTrend(ledger.snapshot(), now, zone).totalWeeklyMinutes)
        assertEquals(0, daily.audioDna.uniqueArtistsCount) // Full-day unique count is unavailable after pruning.
    }

    @Test fun clockFutureListenWithinTodayIsRetainedButNotPresentedAsAlreadyHeard() {
        val ledger = ListeningLedgerStore(temporary.newFolder(), zoneId = zone)
        recorded(ledger, "actual", now - 60_000)
        recorded(ledger, "clock-future", now + 60_000)
        val daily = ListeningLedgerProjection.toDailyCapsule(ledger.snapshot(), now, zone)
        assertEquals(1, daily.audioDna.tracksPlayedCount)
        assertEquals(1, daily.audioDna.totalListeningMinutes)
        assertEquals(1, daily.memoryTimeline.size)
        assertEquals(1, ListeningLedgerProjection.toWeeklyTrend(ledger.snapshot(), now, zone).totalWeeklyMinutes)
        assertEquals(2L, ledger.snapshot().tracks.getValue("track").heardCount)
    }
}
