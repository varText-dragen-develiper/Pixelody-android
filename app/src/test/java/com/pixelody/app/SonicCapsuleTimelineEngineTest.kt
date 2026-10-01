package com.pixelody.app

import com.pixelody.app.core.analytics.SonicCapsuleTimelineEngine
import com.pixelody.app.data.model.AudioDnaMetrics
import com.pixelody.app.data.model.NarrativeStyle
import com.pixelody.app.data.model.SonicArchetype
import com.pixelody.app.data.model.SonicCapsuleSettings
import com.pixelody.app.data.model.SonicMood
import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SonicCapsuleTimelineEngineTest {

    @Test
    fun testInitialStateAndDefaults() {
        val engine = SonicCapsuleTimelineEngine()
        val settings = engine.settings.value
        val capsule = engine.dailyCapsule.value
        val trend = engine.weeklyTrend.value

        assertTrue(settings.isAiSummaryEnabled)
        assertEquals(NarrativeStyle.DjLinerNotes, settings.narrativeStyle)
        assertTrue(settings.autoDailyCapsuleGeneration)
        assertEquals(50, settings.maxTimelineEntries)

        assertNotNull(capsule)
        assertTrue(capsule.aiNarrative.isBlank())
        assertEquals(0, capsule.streakDays)

        assertNotNull(trend)
        assertTrue(trend.dailyMinutes.isEmpty())
        assertEquals(7, trend.dayLabels.size)
        assertEquals(0, trend.activeStreakDays)
    }

    @Test
    fun testRecordTrackListenFullPlayback() {
        val engine = SonicCapsuleTimelineEngine()
        val track = Track(
            id = "track_flac_1",
            title = "Midnight Odyssey",
            artist = "Aura Matrix",
            album = "Neon Horizons",
            durationSeconds = 240,
            streamUrl = "http://localhost:8080/stream/flac1.flac",
            format = "flac",
            lossless = true
        )

        engine.recordTrackListen(
            track = track,
            listenedSeconds = 240,
            isCompleted = true,
            isBitPerfect = true
        )

        val capsule = engine.dailyCapsule.value
        assertEquals(1, capsule.memoryTimeline.size)

        val memory = capsule.memoryTimeline.first()
        assertEquals("track_flac_1", memory.trackId)
        assertEquals("Midnight Odyssey", memory.title)
        assertEquals("Aura Matrix", memory.artist)
        assertEquals("FLAC", memory.formatBadge)
        assertTrue(memory.isLossless)
        assertFalse(memory.wasSkipped)
        assertEquals(1.0f, memory.completionRatio, 0.01f)
        assertEquals(1, memory.replayCount)
    }

    @Test
    fun testRecordTrackListenPartialAndSkipped() {
        val engine = SonicCapsuleTimelineEngine()
        val track = Track(
            id = "track_skip_1",
            title = "Quick Interlude",
            artist = "Short Wave",
            album = "Transients",
            durationSeconds = 180,
            streamUrl = "http://localhost:8080/stream/skip.mp3",
            format = "mp3",
            lossless = false
        )

        engine.recordTrackListen(
            track = track,
            listenedSeconds = 20, // 20/180 = ~11% (<35%)
            isCompleted = false,
            isBitPerfect = false
        )

        val capsule = engine.dailyCapsule.value
        assertEquals(1, capsule.memoryTimeline.size)

        val memory = capsule.memoryTimeline.first()
        assertTrue(memory.wasSkipped)
        assertFalse(memory.isLossless)
        assertEquals("MP3", memory.formatBadge)
        assertTrue(memory.completionRatio < 0.35f)
    }

    @Test
    fun testRecordTrackReplayAggregation() {
        val engine = SonicCapsuleTimelineEngine()
        val track = Track(
            id = "track_repeat_1",
            title = "Loop of Eternity",
            artist = "Solar Drift",
            album = "Endless",
            durationSeconds = 200,
            streamUrl = "http://localhost:8080/stream/loop.flac",
            format = "flac",
            lossless = true
        )

        val now = System.currentTimeMillis()
        engine.recordTrackListen(track, listenedSeconds = 200, isCompleted = true, nowMs = now)
        engine.recordTrackListen(track, listenedSeconds = 200, isCompleted = true, nowMs = now + 10_000L) // 10s later

        val capsule = engine.dailyCapsule.value
        assertEquals(1, capsule.memoryTimeline.size) // aggregated as single node

        val memory = capsule.memoryTimeline.first()
        assertEquals(2, memory.replayCount)
        assertEquals(400, memory.listenedSeconds)
    }

    @Test
    fun testAudioDnaCalculation() {
        val engine = SonicCapsuleTimelineEngine()

        val track1 = Track(id = "t1", title = "Lossless 1", artist = "Artist A", durationSeconds = 300, format = "flac", lossless = true)
        val track2 = Track(id = "t2", title = "Lossless 2", artist = "Artist B", durationSeconds = 300, format = "dsd", lossless = true)
        val track3 = Track(id = "t3", title = "Lossy 1", artist = "Artist C", durationSeconds = 300, format = "aac", lossless = false)

        val now = System.currentTimeMillis()
        engine.recordTrackListen(track1, 300, true, true, nowMs = now)
        engine.recordTrackListen(track2, 300, true, true, nowMs = now + 1_000_000L)
        engine.recordTrackListen(track3, 300, true, false, nowMs = now + 2_000_000L)

        val dna = engine.dailyCapsule.value.audioDna
        assertEquals(3, dna.tracksPlayedCount)
        assertEquals(3, dna.uniqueArtistsCount)
        assertEquals(2, dna.bitPerfectPlayCount)
        assertEquals(0.666f, dna.losslessRatio, 0.02f)
        assertTrue(dna.totalListeningMinutes >= 15)
    }

    @Test
    fun testAiLinerNotesGenerationAllStyles() {
        val engine = SonicCapsuleTimelineEngine()
        val metrics = AudioDnaMetrics(
            losslessRatio = 0.90f,
            avgBpm = 126,
            dominantKey = "8A / A minor",
            energyIndex = 0.85f,
            totalListeningMinutes = 60,
            tracksPlayedCount = 15,
            uniqueArtistsCount = 10,
            bitPerfectPlayCount = 14
        )

        val poetic = engine.generateAiLinerNote(metrics, SonicArchetype.HiResAudiophile, SonicMood.LateNightDrift, NarrativeStyle.Poetic)
        assertTrue(poetic.contains("lossless aura"))
        assertTrue(poetic.contains("90%"))
        assertTrue(poetic.contains("Hi-Res Audiophile"))

        val analytical = engine.generateAiLinerNote(metrics, SonicArchetype.HiResAudiophile, SonicMood.LateNightDrift, NarrativeStyle.Analytical)
        assertTrue(analytical.contains("Session log"))
        assertTrue(analytical.contains("15 tracks"))
        assertTrue(analytical.contains("Late Night Drift"))

        val djNotes = engine.generateAiLinerNote(metrics, SonicArchetype.ElectronicExplorer, SonicMood.MiddayFocus, NarrativeStyle.DjLinerNotes)
        assertTrue(djNotes.contains("Handoff"))
        assertTrue(djNotes.contains("15 cuts"))
        assertTrue(djNotes.contains("Electronic Explorer"))
    }

    @Test
    fun testHighlightTracksExtraction() {
        val engine = SonicCapsuleTimelineEngine()

        val track1 = Track(id = "h1", title = "Hit Song", artist = "Star", durationSeconds = 200, format = "flac", lossless = true)
        val track2 = Track(id = "h2", title = "Skipped Song", artist = "Unknown", durationSeconds = 200, format = "mp3", lossless = false)
        val track3 = Track(id = "h3", title = "Repeat Song", artist = "Star", durationSeconds = 200, format = "flac", lossless = true)

        val now = System.currentTimeMillis()
        // Track 1: 1 play, 100%
        engine.recordTrackListen(track1, 200, isCompleted = true, nowMs = now)
        // Track 2: skipped
        engine.recordTrackListen(track2, 10, isCompleted = false, nowMs = now + 1_000_000L)
        // Track 3: 2 plays
        engine.recordTrackListen(track3, 200, isCompleted = true, nowMs = now + 2_000_000L)
        engine.recordTrackListen(track3, 200, isCompleted = true, nowMs = now + 2_010_000L)

        val highlights = engine.dailyCapsule.value.highlightTracks
        assertEquals(2, highlights.size) // Skipped song excluded
        assertEquals("h3", highlights[0].id) // Most repeated first
        assertEquals("h1", highlights[1].id)
    }

    @Test
    fun testTimelineTrimming() {
        val engine = SonicCapsuleTimelineEngine(
            initialSettings = SonicCapsuleSettings(maxTimelineEntries = 3)
        )

        for (i in 1..6) {
            val track = Track(id = "t_$i", title = "Track $i", artist = "Artist", durationSeconds = 200)
            engine.recordTrackListen(track, 200, true, nowMs = System.currentTimeMillis() + (i * 1_000_000L))
        }

        val timeline = engine.dailyCapsule.value.memoryTimeline
        assertEquals(3, timeline.size)
        assertEquals("t_6", timeline[0].trackId)
        assertEquals("t_5", timeline[1].trackId)
        assertEquals("t_4", timeline[2].trackId)
    }

    @Test
    fun testWeeklyTrendUpdate() {
        val engine = SonicCapsuleTimelineEngine()
        val initialWeekly = engine.weeklyTrend.value.totalWeeklyMinutes

        val track = Track(id = "t_long", title = "Long Symphony", artist = "Orchestra", durationSeconds = 3600, format = "flac")
        engine.recordTrackListen(track, 3600, true) // 60 mins

        val updatedWeekly = engine.weeklyTrend.value.totalWeeklyMinutes
        assertTrue(updatedWeekly >= initialWeekly)
        assertTrue(engine.weeklyTrend.value.dailyMinutes.last() >= 60)
    }

    @Test
    fun testClearTimeline() {
        val engine = SonicCapsuleTimelineEngine()
        val track = Track(id = "t1", title = "Song", artist = "Artist", durationSeconds = 180)
        engine.recordTrackListen(track, 180, true)

        assertEquals(1, engine.dailyCapsule.value.memoryTimeline.size)

        engine.clearTimeline()
        assertEquals(0, engine.dailyCapsule.value.memoryTimeline.size)
    }
}
