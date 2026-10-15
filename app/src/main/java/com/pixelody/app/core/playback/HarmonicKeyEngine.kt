package com.pixelody.app.core.playback

import com.pixelody.app.data.model.CamelotKey
import com.pixelody.app.data.model.CamelotMode
import com.pixelody.app.data.model.DjTransitionCurve
import com.pixelody.app.data.model.HarmonicAnalysisResult
import com.pixelody.app.data.model.HarmonicRelation
import com.pixelody.app.data.model.Track
import com.pixelody.app.data.model.TrackDjTelemetry
import java.util.Locale
import kotlin.math.abs

/**
 * HarmonicKeyEngine: Advanced Camelot Wheel harmonic mixing arithmetic,
 * musical key parser, supplied tempo metadata, and intelligent harmonic queue sorter.
 */
object HarmonicKeyEngine {

    /**
     * Parses standard musical notation, pitch classes, and Camelot codes into a [CamelotKey].
     */
    fun parseKey(rawInput: String?): CamelotKey? {
        if (rawInput.isNullOrBlank()) return null
        val clean = rawInput.trim().replace(Regex("^key\\s*:?\\s*", RegexOption.IGNORE_CASE), "")
            .replace("♯", "#").replace("♭", "b")
        Regex("^(?:K)?(1[0-2]|[1-9])([AB])$", RegexOption.IGNORE_CASE).matchEntire(clean)?.let {
            return CamelotKey.fromNumberAndMode(it.groupValues[1].toInt(),
                if (it.groupValues[2].equals("A", true)) CamelotMode.Minor else CamelotMode.Major)
        }
        val match = Regex("^([A-Ga-g])([#b]?)(?:\\s*((?i:major|maj|minor|min)|m|M))?$").matchEntire(clean) ?: return null
        val letter = match.groupValues[1].uppercase(Locale.ROOT)
        val accidental = match.groupValues[2]
        val mode = match.groupValues[3]
        val minor = mode == "m" || mode.equals("min", true) || mode.equals("minor", true)
        val pitch = ((mapOf("C" to 0, "D" to 2, "E" to 4, "F" to 5, "G" to 7, "A" to 9, "B" to 11).getValue(letter)
            + when (accidental) { "#" -> 1; "b" -> -1; else -> 0 }) + 12) % 12
        val number = if (minor) intArrayOf(5, 12, 7, 2, 9, 4, 11, 6, 1, 8, 3, 10)[pitch]
            else intArrayOf(8, 3, 10, 5, 12, 7, 2, 9, 4, 11, 6, 1)[pitch]
        return CamelotKey.fromNumberAndMode(number, if (minor) CamelotMode.Minor else CamelotMode.Major)
    }

    /**
     * Calculates the harmonic relationship and compatibility classification between two Camelot keys.
     */
    fun calculateHarmonicRelation(currentKey: CamelotKey?, nextKey: CamelotKey?): HarmonicRelation {
        if (currentKey == null || nextKey == null) return HarmonicRelation.Unknown
        if (currentKey == nextKey) {
            return HarmonicRelation.ExactMatch
        }

        // Same number, opposite mode (e.g. 8A <-> 8B, A minor <-> C major)
        if (currentKey.number == nextKey.number && currentKey.mode != nextKey.mode) {
            return HarmonicRelation.RelativeMajorMinor
        }

        // Same mode comparisons
        if (currentKey.mode == nextKey.mode) {
            val forwardDiff = (nextKey.number - currentKey.number + 12) % 12
            val backwardDiff = (currentKey.number - nextKey.number + 12) % 12

            return when {
                forwardDiff == 1 || backwardDiff == 1 -> HarmonicRelation.AdjacentStep
                forwardDiff == 2 -> HarmonicRelation.EnergyBoost
                backwardDiff == 2 -> HarmonicRelation.EnergyDrop
                else -> HarmonicRelation.DissonantClash
            }
        }

        // Opposite mode diagonal comparisons (±1 on wheel)
        val diff = (nextKey.number - currentKey.number + 12) % 12
        val revDiff = (currentKey.number - nextKey.number + 12) % 12
        if (diff == 1 || revDiff == 1) {
            return HarmonicRelation.DiagonalStep
        }

        return HarmonicRelation.DissonantClash
    }

    /**
     * Returns a list of all compatible Camelot keys and their relations relative to the provided key.
     */
    fun getCompatibleKeys(key: CamelotKey): List<Pair<CamelotKey, HarmonicRelation>> {
        return CamelotKey.entries.map { candidate ->
            candidate to calculateHarmonicRelation(key, candidate)
        }.filter { it.second != HarmonicRelation.DissonantClash }
            .sortedByDescending { it.second.score }
    }

    /**
     * Calculates pitch stretch and tempo delta percentage between Deck A and Deck B.
     */
    fun calculatePitchStretch(currentBpm: Float, nextBpm: Float): Pair<Float, Float> {
        if (currentBpm <= 0f || nextBpm <= 0f) return 0f to 0f

        // Check for half-time / double-time harmonics
        val effectiveNextBpm = when {
            abs(nextBpm * 2f - currentBpm) < abs(nextBpm - currentBpm) -> nextBpm * 2f
            abs(nextBpm / 2f - currentBpm) < abs(nextBpm - currentBpm) -> nextBpm / 2f
            else -> nextBpm
        }

        val bpmDelta = effectiveNextBpm - currentBpm
        val stretchPercent = (bpmDelta / currentBpm) * 100f
        return bpmDelta to stretchPercent
    }

    private val telemetryCache = java.util.concurrent.ConcurrentHashMap<Track, TrackDjTelemetry>()

    fun clearCache() {
        telemetryCache.clear()
    }

    /** Reads supplied tags or explicitly labeled metadata. It performs no audio analysis. */
    fun estimateTrackTelemetry(track: Track): TrackDjTelemetry = telemetryCache.getOrPut(track) {
        val text = "${track.title} ${track.album} ${track.format}"
        val labeledKey = Regex("""\bKey\s*:?\s*((?:K)?(?:1[0-2]|[1-9])[AB]\b|[A-G][#b♯♭]?(?:\s*(?:major|minor|maj|min)\b|m\b|M\b)?)(?![\p{L}\p{N}])""", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.get(1)?.let(::parseKey)
        val taggedKey = parseKey(track.musicalKey)
        val key = taggedKey ?: parseKey(track.format) ?: parseKey(track.title) ?: labeledKey
        val taggedBpm = track.bpm?.takeIf { it.isFinite() && it > 0f }
        val labeledBpm = Regex("""\bBPM\s*:?\s*(\d{1,3}(?:\.\d+)?)(?![\d.])""", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.get(1)?.toFloatOrNull()?.takeIf { it.isFinite() && it > 0f }
        TrackDjTelemetry(track = track, key = key, bpm = taggedBpm ?: labeledBpm,
            detectedKeyConfidence = null,
            keyProvenance = if (taggedKey != null) "tagged" else if (key != null) "derived" else "unknown",
            bpmProvenance = if (taggedBpm != null) "tagged" else if (labeledBpm != null) "derived" else "unknown")
    }

    /**
     * Conducts a full harmonic and tempo analysis between Deck A (Outgoing) and Deck B (Incoming).
     */
    fun analyzeTransition(deckA: TrackDjTelemetry, deckB: TrackDjTelemetry): HarmonicAnalysisResult {
        val relation = calculateHarmonicRelation(deckA.key, deckB.key)
        val tempo = if (deckA.bpm != null && deckB.bpm != null) calculatePitchStretch(deckA.bpm, deckB.bpm) else null
        val bpmDelta = tempo?.first
        val pitchStretch = tempo?.second

        // BPM compatibility factor (penalize tempo stretch > ±5%)
        val tempoScore = pitchStretch?.let { (1.0f - (abs(it) / 10f)).coerceIn(0.2f, 1.0f) } ?: 0f
        val overallScore = (relation.score * 0.7f) + (tempoScore * 0.3f)

        val recommendedCurve = when {
            relation == HarmonicRelation.DissonantClash -> {
                if (pitchStretch != null && abs(pitchStretch) > 4f) DjTransitionCurve.VinylBrake else DjTransitionCurve.FilterSweep
            }
            relation == HarmonicRelation.ExactMatch || relation == HarmonicRelation.RelativeMajorMinor -> {
                DjTransitionCurve.EqualPower
            }
            relation == HarmonicRelation.AdjacentStep -> {
                DjTransitionCurve.BassSwap
            }
            relation == HarmonicRelation.EnergyBoost -> {
                DjTransitionCurve.FilterSweep
            }
            else -> DjTransitionCurve.EqualPower
        }

        return HarmonicAnalysisResult(
            currentKey = deckA.key,
            nextKey = deckB.key,
            relation = relation,
            currentBpm = deckA.bpm,
            nextBpm = deckB.bpm,
            bpmDelta = bpmDelta,
            pitchStretchPercent = pitchStretch,
            recommendedCurve = recommendedCurve,
            overallCompatibilityScore = overallScore
        )
    }

    /**
     * Reorders an upcoming queue of tracks using a greedy Camelot Wheel nearest-neighbor
     * algorithm to minimize harmonic clash and ensure seamless musical flow.
     */
    fun harmonicSortQueue(currentTrack: Track?, upcomingTracks: List<Track>): List<Track> {
        if (upcomingTracks.size <= 1) return upcomingTracks

        val remaining = upcomingTracks.toMutableList()
        val sorted = mutableListOf<Track>()

        var pivotTelemetry = currentTrack?.let { estimateTrackTelemetry(it) }
            ?: estimateTrackTelemetry(remaining.removeAt(0).also { sorted.add(it) })

        while (remaining.isNotEmpty()) {
            // Find track in remaining pool with highest harmonic + tempo compatibility to pivot
            var bestIdx = 0
            var bestScore = -1f

            for (i in remaining.indices) {
                val candidateTelemetry = estimateTrackTelemetry(remaining[i])
                val analysis = analyzeTransition(pivotTelemetry, candidateTelemetry)
                if (analysis.overallCompatibilityScore > bestScore) {
                    bestScore = analysis.overallCompatibilityScore
                    bestIdx = i
                }
            }

            val nextTrack = remaining.removeAt(bestIdx)
            sorted.add(nextTrack)
            pivotTelemetry = estimateTrackTelemetry(nextTrack)
        }

        return sorted
    }
}
