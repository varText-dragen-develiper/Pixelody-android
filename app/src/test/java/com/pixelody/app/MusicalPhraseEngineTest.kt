package com.pixelody.app

import com.pixelody.app.core.playback.MusicalPhraseEngine
import com.pixelody.app.data.model.PhraseZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicalPhraseEngineTest {

    @Test
    fun `computePhraseStructure calculates correct bar duration and markers for 120 BPM`() {
        val bpm = 120f
        val durationMs = 180_000L // 3 minutes
        val structure = MusicalPhraseEngine.computePhraseStructure(bpm, durationMs, phraseLengthBars = 16)

        // At 120 BPM, 1 beat = 500ms, 1 bar (4 beats) = 2000ms
        assertEquals(2000L, structure.barDurationMs)
        assertEquals(90, structure.totalBars)
        assertEquals(16 * 2000L, structure.introEndMs) // 32000ms
        assertEquals(74 * 2000L, structure.outroStartMs) // 148000ms
        assertTrue(structure.phraseMarkersMs.isNotEmpty())
        assertEquals(0L, structure.phraseMarkersMs[0])
        assertEquals(32000L, structure.phraseMarkersMs[1]) // 16 bars * 2000ms
    }

    @Test
    fun `getPhraseZone correctly returns Intro, Outro, Drop, Breakdown, and Build`() {
        val bpm = 120f
        val durationMs = 240_000L // 4 minutes = 120 bars
        val structure = MusicalPhraseEngine.computePhraseStructure(bpm, durationMs, phraseLengthBars = 16)

        // Intro: 0 to 32,000ms (bars 0-15)
        assertEquals(PhraseZone.Intro, MusicalPhraseEngine.getPhraseZone(10_000L, structure))

        // Outro: bars 104-120 (>= 208,000ms)
        assertEquals(PhraseZone.Outro, MusicalPhraseEngine.getPhraseZone(220_000L, structure))

        // Main body superphrase modulation (superphrase = 32 bars = 64,000ms)
        // Bar 16 -> superphrase bar 16 -> Breakdown
        val breakdownMs = 16 * 2000L + 500L // Bar 16
        assertEquals(PhraseZone.Breakdown, MusicalPhraseEngine.getPhraseZone(breakdownMs, structure))

        // Bar 25 -> superphrase bar 25 -> Build
        val buildMs = 25 * 2000L + 500L
        assertEquals(PhraseZone.Build, MusicalPhraseEngine.getPhraseZone(buildMs, structure))

        // Bar 32 -> superphrase bar 0 -> Drop
        val dropMs = 32 * 2000L + 500L
        assertEquals(PhraseZone.Drop, MusicalPhraseEngine.getPhraseZone(dropMs, structure))
    }

    @Test
    fun `calculatePhaseAlignment returns accurate downbeat offsets and recommendation`() {
        val bpmA = 120f // 2000ms per bar, 32000ms per 16-bar phrase
        val bpmB = 124f

        // Playback at 28,000ms (14 bars in -> 4000ms / 2 bars until next phrase downbeat)
        val alignment = MusicalPhraseEngine.calculatePhaseAlignment(
            currentPosA = 28_000L,
            bpmA = bpmA,
            bpmB = bpmB,
            targetPhraseLengthBars = 16
        )

        assertEquals(4000L, alignment.leadInOffsetMs)
        assertEquals(32_000L, alignment.targetDownbeatMs)
        assertEquals(2, alignment.barsUntilNextPhrase)
        assertFalse(alignment.isAligned)

        // Exact downbeat test (32,000ms is exactly on phrase boundary)
        val alignedCheck = MusicalPhraseEngine.calculatePhaseAlignment(
            currentPosA = 32_000L,
            bpmA = bpmA,
            bpmB = bpmB,
            targetPhraseLengthBars = 16
        )
        assertTrue(alignedCheck.isAligned)
    }

    @Test
    fun `quantizeToNearestPhraseBoundary snaps to closest 16-bar boundary`() {
        val bpm = 120f // 32,000ms per 16-bar phrase

        // 31,000ms -> snaps up to 32,000ms
        assertEquals(32_000L, MusicalPhraseEngine.quantizeToNearestPhraseBoundary(31_000L, bpm, 16))

        // 33,000ms -> snaps down to 32,000ms
        assertEquals(32_000L, MusicalPhraseEngine.quantizeToNearestPhraseBoundary(33_000L, bpm, 16))
    }

    @Test
    fun `calculateBarInPhrase and calculateBeatInBar return 1-indexed integers`() {
        val bpm = 120f // 2000ms per bar, 500ms per beat

        // At 0ms -> Bar 1, Beat 1
        assertEquals(1, MusicalPhraseEngine.calculateBarInPhrase(0L, bpm, 16))
        assertEquals(1, MusicalPhraseEngine.calculateBeatInBar(0L, bpm))

        // At 2500ms -> Bar 2 (2000-4000ms), Beat 2 (500ms in)
        assertEquals(2, MusicalPhraseEngine.calculateBarInPhrase(2500L, bpm, 16))
        assertEquals(2, MusicalPhraseEngine.calculateBeatInBar(2500L, bpm))

        // Progress at midpoint of 16-bar phrase (16,000ms / 32,000ms = 0.5f)
        val progress = MusicalPhraseEngine.calculatePhraseProgress(16_000L, bpm, 16)
        assertEquals(0.5f, progress, 0.01f)
    }
}
