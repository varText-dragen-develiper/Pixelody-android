package com.pixelody.app.core.playback

import com.pixelody.app.data.model.MusicalPhraseStructure
import com.pixelody.app.data.model.PhraseZone
import com.pixelody.app.data.model.PhaseAlignmentResult
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * MusicalPhraseEngine: High-precision musical phrase structure math and downbeat-locked quantization.
 * Calculates 4/8/16/32-bar phrase boundaries, Intro/Outro structural cues, and phase lead-in offsets.
 */
object MusicalPhraseEngine {

    /**
     * Computes the complete phrase structure of a track given its BPM and duration.
     */
    fun computePhraseStructure(
        bpm: Float,
        durationMs: Long,
        phraseLengthBars: Int = 16,
        beatsPerBar: Int = 4
    ): MusicalPhraseStructure {
        val safeBpm = if (bpm > 0f) bpm else 120f
        val safeDuration = durationMs.coerceAtLeast(1000L)
        val barDurationMs = ((60_000.0 / safeBpm.toDouble()) * beatsPerBar.toDouble()).roundToLong().coerceAtLeast(500L)
        val totalBars = (safeDuration / barDurationMs).toInt().coerceAtLeast(1)

        val introBars = when {
            totalBars >= 64 -> 16
            totalBars >= 32 -> 8
            totalBars >= 16 -> 4
            else -> 2
        }

        val outroBars = when {
            totalBars >= 64 -> 16
            totalBars >= 32 -> 8
            totalBars >= 16 -> 4
            else -> 2
        }

        val introEndMs = (introBars * barDurationMs).coerceAtMost(safeDuration)
        val outroStartMs = ((totalBars - outroBars) * barDurationMs).coerceIn(introEndMs, safeDuration)

        val phraseDurationMs = barDurationMs * phraseLengthBars
        val phraseMarkers = mutableListOf<Long>()
        var marker = 0L
        while (marker < safeDuration) {
            phraseMarkers.add(marker)
            marker += phraseDurationMs
        }

        return MusicalPhraseStructure(
            bpm = safeBpm,
            barDurationMs = barDurationMs,
            beatsPerBar = beatsPerBar,
            phraseLengthBars = phraseLengthBars,
            totalBars = totalBars,
            introEndMs = introEndMs,
            outroStartMs = outroStartMs,
            phraseMarkersMs = phraseMarkers,
            phraseZone = PhraseZone.Intro
        )
    }

    /**
     * Identifies the active [PhraseZone] for a track at a specific playback position.
     */
    fun getPhraseZone(currentPositionMs: Long, structure: MusicalPhraseStructure): PhraseZone {
        if (currentPositionMs < structure.introEndMs) {
            return PhraseZone.Intro
        }
        if (currentPositionMs >= structure.outroStartMs) {
            return PhraseZone.Outro
        }

        val currentBar = (currentPositionMs / structure.barDurationMs.coerceAtLeast(1L)).toInt()
        val barInSuperphrase = currentBar % 32

        return when (barInSuperphrase) {
            in 0..7 -> PhraseZone.Drop
            in 8..15 -> PhraseZone.MainBody
            in 16..23 -> PhraseZone.Breakdown
            in 24..31 -> PhraseZone.Build
            else -> PhraseZone.MainBody
        }
    }

    /**
     * Calculates phase alignment and lead-in downbeat synchronization between Deck A and Deck B.
     */
    fun calculatePhaseAlignment(
        currentPosA: Long,
        bpmA: Float,
        bpmB: Float,
        targetPhraseLengthBars: Int = 16,
        beatsPerBar: Int = 4
    ): PhaseAlignmentResult {
        val safeBpmA = if (bpmA > 0f) bpmA else 120f
        val beatDurationA = (60_000.0 / safeBpmA.toDouble()).roundToLong().coerceAtLeast(100L)
        val barDurationA = beatDurationA * beatsPerBar
        val phraseDurationA = barDurationA * targetPhraseLengthBars

        val posInPhrase = ((currentPosA % phraseDurationA) + phraseDurationA) % phraseDurationA
        val msUntilNextPhrase = phraseDurationA - posInPhrase
        val barsUntilNextPhrase = (msUntilNextPhrase / barDurationA).toInt()

        val posInBar = ((currentPosA % barDurationA) + barDurationA) % barDurationA
        val msUntilNextBar = barDurationA - posInBar
        val beatsUntilNextBar = ((msUntilNextBar + (beatDurationA / 2)) / beatDurationA).toInt().coerceIn(0, beatsPerBar)

        val targetDownbeatMs = currentPosA + msUntilNextPhrase
        val isAligned = posInPhrase < 40L || msUntilNextPhrase < 40L

        val recommendedBars = when {
            barsUntilNextPhrase <= 4 -> 4
            barsUntilNextPhrase <= 8 -> 8
            else -> 16
        }

        return PhaseAlignmentResult(
            leadInOffsetMs = msUntilNextPhrase,
            targetDownbeatMs = targetDownbeatMs,
            barsUntilNextPhrase = barsUntilNextPhrase,
            beatsUntilNextBar = beatsUntilNextBar,
            isAligned = isAligned,
            recommendedTransitionBars = recommendedBars
        )
    }

    /**
     * Quantizes a raw playback timestamp to the nearest 16-bar phrase boundary.
     */
    fun quantizeToNearestPhraseBoundary(
        currentPosMs: Long,
        bpm: Float,
        phraseBars: Int = 16,
        beatsPerBar: Int = 4
    ): Long {
        val safeBpm = if (bpm > 0f) bpm else 120f
        val barDurationMs = ((60_000.0 / safeBpm.toDouble()) * beatsPerBar.toDouble()).roundToLong().coerceAtLeast(500L)
        val phraseDurationMs = barDurationMs * phraseBars

        val remainder = currentPosMs % phraseDurationMs
        return if (remainder < phraseDurationMs / 2) {
            currentPosMs - remainder
        } else {
            currentPosMs + (phraseDurationMs - remainder)
        }
    }

    /**
     * Quantizes a raw playback timestamp to the nearest single bar downbeat.
     */
    fun quantizeToNearestBar(
        currentPosMs: Long,
        bpm: Float,
        beatsPerBar: Int = 4
    ): Long {
        val safeBpm = if (bpm > 0f) bpm else 120f
        val barDurationMs = ((60_000.0 / safeBpm.toDouble()) * beatsPerBar.toDouble()).roundToLong().coerceAtLeast(500L)
        val remainder = currentPosMs % barDurationMs
        return if (remainder < barDurationMs / 2) {
            currentPosMs - remainder
        } else {
            currentPosMs + (barDurationMs - remainder)
        }
    }

    /**
     * Returns the 0.0f..1.0f progress through the active musical phrase.
     */
    fun calculatePhraseProgress(
        currentPosMs: Long,
        bpm: Float,
        phraseBars: Int = 16,
        beatsPerBar: Int = 4
    ): Float {
        val safeBpm = if (bpm > 0f) bpm else 120f
        val barDurationMs = ((60_000.0 / safeBpm.toDouble()) * beatsPerBar.toDouble()).roundToLong().coerceAtLeast(500L)
        val phraseDurationMs = barDurationMs * phraseBars
        val posInPhrase = ((currentPosMs % phraseDurationMs) + phraseDurationMs) % phraseDurationMs
        return (posInPhrase.toFloat() / phraseDurationMs.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Returns the 1-indexed bar number within the current phrase (1..phraseBars).
     */
    fun calculateBarInPhrase(
        currentPosMs: Long,
        bpm: Float,
        phraseBars: Int = 16,
        beatsPerBar: Int = 4
    ): Int {
        val safeBpm = if (bpm > 0f) bpm else 120f
        val barDurationMs = ((60_000.0 / safeBpm.toDouble()) * beatsPerBar.toDouble()).roundToLong().coerceAtLeast(500L)
        val phraseDurationMs = barDurationMs * phraseBars
        val posInPhrase = ((currentPosMs % phraseDurationMs) + phraseDurationMs) % phraseDurationMs
        val barIdx = (posInPhrase / barDurationMs).toInt()
        return (barIdx % phraseBars) + 1
    }

    /**
     * Returns the 1-indexed beat number within the current bar (1..beatsPerBar).
     */
    fun calculateBeatInBar(
        currentPosMs: Long,
        bpm: Float,
        beatsPerBar: Int = 4
    ): Int {
        val safeBpm = if (bpm > 0f) bpm else 120f
        val beatDurationMs = (60_000.0 / safeBpm.toDouble()).roundToLong().coerceAtLeast(100L)
        val barDurationMs = beatDurationMs * beatsPerBar
        val posInBar = ((currentPosMs % barDurationMs) + barDurationMs) % barDurationMs
        val beatIdx = (posInBar / beatDurationMs).toInt()
        return (beatIdx % beatsPerBar) + 1
    }
}
