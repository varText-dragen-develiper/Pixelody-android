package com.pixelody.app.core.playback

import com.pixelody.app.data.model.AutoDjSettings
import com.pixelody.app.data.model.CamelotKey
import com.pixelody.app.data.model.CamelotMode
import com.pixelody.app.data.model.CollectionHarmonicTelemetry
import com.pixelody.app.data.model.DjTransitionCurve
import com.pixelody.app.data.model.EnergyContourPoint
import com.pixelody.app.data.model.EnergyContourPreset
import com.pixelody.app.data.model.HarmonicEnergyMode
import com.pixelody.app.data.model.HarmonicFlowProfile
import com.pixelody.app.data.model.HarmonicRelation
import com.pixelody.app.data.model.HarmonicTrajectoryPoint
import com.pixelody.app.data.model.QueueSlotHarmonicAffinity
import com.pixelody.app.data.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * HarmonicFlowCoordinator: Unified architectural bridge synchronizing Auto-DJ transition DSP,
 * Camelot Wheel circle-of-fifths arithmetic, and Flow Shuffle queue intelligence.
 */
object HarmonicFlowCoordinator {

    private val _activeProfile = MutableStateFlow(HarmonicFlowProfile.DeepListening)
    val activeProfile: StateFlow<HarmonicFlowProfile> = _activeProfile.asStateFlow()

    private val _customSettings = MutableStateFlow(AutoDjSettings())
    val customSettings: StateFlow<AutoDjSettings> = _customSettings.asStateFlow()

    /**
     * Activates a unified Harmonic Flow Profile and configures underlying DSP curves & energy steering.
     */
    fun setProfile(profile: HarmonicFlowProfile) {
        _activeProfile.value = profile
        if (profile != HarmonicFlowProfile.CustomManual) {
            _customSettings.value = _customSettings.value.copy(
                preferredCurve = profile.defaultCurve,
                transitionDurationSeconds = profile.defaultDurationSeconds,
                isAutoDjEnabled = true
            )
        }
    }

    /**
     * Updates manual custom settings.
     */
    fun updateCustomSettings(settings: AutoDjSettings) {
        _customSettings.value = settings
        _activeProfile.value = HarmonicFlowProfile.CustomManual
    }

    /**
     * Calculates the step-by-step harmonic roadmap and slot affinities across a queue.
     */
    fun calculateQueueRoadmap(queue: List<Track>): List<QueueSlotHarmonicAffinity> {
        if (queue.size < 2) return emptyList()

        val telemetries = queue.map { HarmonicKeyEngine.estimateTrackTelemetry(it) }

        return telemetries.zipWithNext().mapIndexed { index, (from, to) ->
            val analysis = HarmonicKeyEngine.analyzeTransition(from, to)
            val isClash = analysis.relation == HarmonicRelation.DissonantClash

            // If clashing, find an ideal intermediate bridge key (e.g. adjacent to both)
            val suggestedBridgeKey = if (isClash) {
                findIdealBridgeKey(from.key, to.key)
            } else null

            QueueSlotHarmonicAffinity(
                slotIndex = index,
                fromTrackId = from.track.id,
                toTrackId = to.track.id,
                fromKey = from.key,
                toKey = to.key,
                relation = analysis.relation,
                compatibilityScore = analysis.overallCompatibilityScore,
                bpmDelta = analysis.bpmDelta,
                pitchStretchPercent = analysis.pitchStretchPercent,
                isClash = isClash,
                recommendedCurve = analysis.recommendedCurve,
                suggestedBridgeKey = suggestedBridgeKey
            )
        }
    }

    /**
     * Generates a smooth visual harmonic trajectory across an album, playlist, or queue.
     */
    fun calculateTrajectoryPoints(tracks: List<Track>): List<HarmonicTrajectoryPoint> {
        return tracks.map { track ->
            val telemetry = HarmonicKeyEngine.estimateTrackTelemetry(track)
            val normalizedWheel = ((telemetry.key.number - 1) % 12) / 12f
            val baseEnergy = (telemetry.bpm - 70f) / 110f // normalize 70-180 BPM to ~0.0-1.0
            val modeBonus = if (telemetry.key.mode == CamelotMode.Major) 0.15f else 0.0f
            val energyScore = (baseEnergy + modeBonus).coerceIn(0.1f, 1.0f)

            HarmonicTrajectoryPoint(
                trackId = track.id,
                title = track.title,
                key = telemetry.key,
                bpm = telemetry.bpm,
                normalizedWheelPosition = normalizedWheel,
                energyLevel = energyScore
            )
        }
    }

    /**
     * Discovers intermediate bridge tracks from candidate pool that smoothly modulate between two distant tracks.
     */
    fun findHarmonicBridgeTracks(
        fromTrack: Track,
        toTrack: Track,
        candidatePool: List<Track>,
        maxResults: Int = 3
    ): List<Track> {
        val fromTelemetry = HarmonicKeyEngine.estimateTrackTelemetry(fromTrack)
        val toTelemetry = HarmonicKeyEngine.estimateTrackTelemetry(toTrack)

        val bridgeCandidates = candidatePool
            .filterNot { it.id == fromTrack.id || it.id == toTrack.id }
            .map { candidate ->
                val candTelemetry = HarmonicKeyEngine.estimateTrackTelemetry(candidate)
                val relA = HarmonicKeyEngine.calculateHarmonicRelation(fromTelemetry.key, candTelemetry.key)
                val relB = HarmonicKeyEngine.calculateHarmonicRelation(candTelemetry.key, toTelemetry.key)

                // Total score is the geometric mean of both transitions
                val combinedScore = (relA.score * 0.5f) + (relB.score * 0.5f)
                val isHarmonicBridge = relA.isHarmonic && relB.isHarmonic

                Triple(candidate, combinedScore, isHarmonicBridge)
            }
            .filter { it.third } // Must be harmonic on both sides
            .sortedByDescending { it.second }
            .map { it.first }
            .take(maxResults)

        return bridgeCandidates
    }

    /**
     * Mathematically optimizes the order of a queue into an optimal harmonic journey.
     */
    fun optimizeQueueHarmonicFlow(
        queue: List<Track>,
        anchorTrackId: String? = null,
        energyMode: HarmonicEnergyMode = HarmonicEnergyMode.HarmonicLock
    ): List<Track> {
        if (queue.size <= 2) return queue

        val remaining = queue.toMutableList()
        val sortedResult = mutableListOf<Track>()

        // 1. Pick Anchor Track
        val initialAnchor = if (anchorTrackId != null) {
            remaining.firstOrNull { it.id == anchorTrackId } ?: remaining.first()
        } else {
            remaining.first()
        }

        sortedResult.add(initialAnchor)
        remaining.remove(initialAnchor)

        // 2. Greedy Harmonic & Anti-Clumping Sorter
        while (remaining.isNotEmpty()) {
            val currentTrack = sortedResult.last()
            val currentTelemetry = HarmonicKeyEngine.estimateTrackTelemetry(currentTrack)

            val bestNext = remaining.maxByOrNull { candidate ->
                val candTelemetry = HarmonicKeyEngine.estimateTrackTelemetry(candidate)
                val rel = HarmonicKeyEngine.calculateHarmonicRelation(currentTelemetry.key, candTelemetry.key)

                var score = rel.score * 100f

                // Energy Mode Weighting
                when (energyMode) {
                    HarmonicEnergyMode.EnergyBoost2 -> {
                        if (rel == HarmonicRelation.EnergyBoost) score += 40f
                        if (rel == HarmonicRelation.AdjacentStep) score += 15f
                    }
                    HarmonicEnergyMode.SunsetDrift2 -> {
                        if (rel == HarmonicRelation.EnergyDrop) score += 40f
                        if (rel == HarmonicRelation.AdjacentStep) score += 15f
                    }
                    HarmonicEnergyMode.HarmonicLock -> {
                        if (rel == HarmonicRelation.ExactMatch) score += 30f
                        if (rel == HarmonicRelation.RelativeMajorMinor) score += 25f
                        if (rel == HarmonicRelation.AdjacentStep) score += 20f
                    }
                }

                // Anti-clumping penalty: discourage same artist back-to-back
                if (currentTrack.artist.isNotBlank() && candidate.artist.equals(currentTrack.artist, ignoreCase = true)) {
                    score -= 45f
                }

                // BPM proximity bonus
                val bpmDiff = abs(candTelemetry.bpm - currentTelemetry.bpm)
                score -= min(bpmDiff * 0.5f, 25f)

                score
            } ?: remaining.first()

            sortedResult.add(bestNext)
            remaining.remove(bestNext)
        }

        return sortedResult
    }

    /**
     * Seeds a complete continuous harmonic runway from any single selected anchor track.
     */
    fun seedHarmonicFlowRunway(
        anchorTrack: Track,
        libraryPool: List<Track>,
        targetCount: Int = 15,
        profile: HarmonicFlowProfile = HarmonicFlowProfile.DeepListening
    ): List<Track> {
        val pool = libraryPool.filterNot { it.missing || it.streamUrl.isBlank() }
        if (pool.isEmpty()) return listOf(anchorTrack)

        val selected = mutableListOf(anchorTrack)
        val available = pool.filterNot { it.id == anchorTrack.id }.toMutableList()

        while (selected.size < targetCount && available.isNotEmpty()) {
            val current = selected.last()
            val currentTel = HarmonicKeyEngine.estimateTrackTelemetry(current)

            val nextBest = available.maxByOrNull { cand ->
                val candTel = HarmonicKeyEngine.estimateTrackTelemetry(cand)
                val rel = HarmonicKeyEngine.calculateHarmonicRelation(currentTel.key, candTel.key)

                var score = rel.score * 100f
                when (profile.energyMode) {
                    HarmonicEnergyMode.EnergyBoost2 -> if (rel == HarmonicRelation.EnergyBoost) score += 50f
                    HarmonicEnergyMode.SunsetDrift2 -> if (rel == HarmonicRelation.EnergyDrop) score += 50f
                    HarmonicEnergyMode.HarmonicLock -> if (rel.isHarmonic) score += 25f
                }

                // Diversity penalty
                val recentArtists = selected.takeLast(3).map { it.artist.lowercase(Locale.US) }
                if (cand.artist.lowercase(Locale.US) in recentArtists) {
                    score -= 50f
                }

                score
            } ?: break

            selected.add(nextBest)
            available.remove(nextBest)
        }

        return selected
    }

    /**
     * Mathematically sculpts the queue to match a target energy contour curve while preserving harmonic affinity.
     */
    fun sculptQueueToTargetCurve(
        queue: List<Track>,
        preset: EnergyContourPreset,
        customPoints: List<Float>? = null,
        anchorTrackId: String? = null
    ): List<Track> {
        if (queue.size <= 2) return queue

        val remaining = queue.toMutableList()
        val sortedResult = mutableListOf<Track>()

        // 1. Keep anchor track at index 0
        val initialAnchor = if (anchorTrackId != null) {
            remaining.firstOrNull { it.id == anchorTrackId } ?: remaining.first()
        } else {
            remaining.first()
        }
        sortedResult.add(initialAnchor)
        remaining.remove(initialAnchor)

        val totalSlots = queue.size

        // 2. Iteratively select next track matching slot target energy & maximizing harmonic affinity
        while (remaining.isNotEmpty()) {
            val currentTrack = sortedResult.last()
            val currentTelemetry = HarmonicKeyEngine.estimateTrackTelemetry(currentTrack)
            val currentSlotIndex = sortedResult.size
            val progress = currentSlotIndex.toFloat() / (totalSlots - 1).coerceAtLeast(1)

            val targetEnergy = if (preset == EnergyContourPreset.CustomSculpt && customPoints != null && customPoints.isNotEmpty()) {
                val idx = (progress * (customPoints.size - 1)).toInt().coerceIn(0, customPoints.size - 1)
                customPoints[idx]
            } else {
                preset.targetEnergyFunction(progress)
            }

            val bestNext = remaining.minByOrNull { candidate ->
                val candTelemetry = HarmonicKeyEngine.estimateTrackTelemetry(candidate)
                val candEnergy = calculateTrajectoryPoints(listOf(candidate)).first().energyLevel

                // Energy deviation penalty
                val energyDiff = abs(candEnergy - targetEnergy)
                val energyPenalty = energyDiff * 60f

                // Harmonic relation score (higher is better, so invert for minBy)
                val rel = HarmonicKeyEngine.calculateHarmonicRelation(currentTelemetry.key, candTelemetry.key)
                val harmonicScore = rel.score * 100f

                // Anti-clumping penalty
                val artistClumpPenalty = if (currentTrack.artist.isNotBlank() &&
                    candidate.artist.equals(currentTrack.artist, ignoreCase = true)
                ) 35f else 0f

                // Overall cost: minimize energy error, maximize harmonic score
                energyPenalty - harmonicScore + artistClumpPenalty
            } ?: remaining.first()

            sortedResult.add(bestNext)
            remaining.remove(bestNext)
        }

        return sortedResult
    }

    /**
     * Calculates comprehensive collection-wide harmonic telemetry for albums, playlists, and crates.
     */
    fun calculateCollectionTelemetry(tracks: List<Track>): CollectionHarmonicTelemetry {
        if (tracks.isEmpty()) {
            return CollectionHarmonicTelemetry(
                keyRange = "N/A",
                dominantKey = CamelotKey.K8A,
                averageBpm = 120f,
                harmonicCohesionPercent = 100,
                trajectoryPoints = emptyList()
            )
        }

        val trajectory = calculateTrajectoryPoints(tracks)
        val telemetries = tracks.map { HarmonicKeyEngine.estimateTrackTelemetry(it) }
        val avgBpm = telemetries.map { it.bpm }.average().toFloat()

        // Dominant key (mode of keys)
        val dominantKey = telemetries.groupBy { it.key }
            .maxByOrNull { it.value.size }
            ?.key ?: telemetries.first().key

        // Key span range
        val keyNumbers = telemetries.map { it.key.number }.distinct().sorted()
        val keyRange = if (keyNumbers.size == 1) {
            "${dominantKey.code}"
        } else {
            "${keyNumbers.first()}${dominantKey.mode.code} - ${keyNumbers.last()}${dominantKey.mode.code}"
        }

        // Harmonic cohesion score across adjacent pairs
        val roadmap = calculateQueueRoadmap(tracks)
        val cohesionScore = if (roadmap.isNotEmpty()) {
            (roadmap.map { it.compatibilityScore }.average() * 100f).toInt().coerceIn(10, 100)
        } else 100

        return CollectionHarmonicTelemetry(
            keyRange = keyRange,
            dominantKey = dominantKey,
            averageBpm = avgBpm,
            harmonicCohesionPercent = cohesionScore,
            trajectoryPoints = trajectory
        )
    }

    private fun findIdealBridgeKey(keyA: CamelotKey, keyB: CamelotKey): CamelotKey {
        // Find intermediate key on Camelot wheel between keyA and keyB
        val diff = (keyB.number - keyA.number + 12) % 12
        val step = if (diff in 1..6) keyA.number + (diff / 2) else keyA.number - ((12 - diff) / 2)
        return CamelotKey.fromNumberAndMode(step, keyA.mode)
    }
}
