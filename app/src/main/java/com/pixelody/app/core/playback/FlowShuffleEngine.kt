package com.pixelody.app.core.playback

import com.pixelody.app.core.genre.GenreTaxonomyEngine
import com.pixelody.app.data.model.Track
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Operating modes for Pixelody's Flow Shuffle Engine.
 */
enum class FlowShuffleMode(val displayName: String, val badge: String) {
    Off("Shuffle Off", "Off"),
    SmartFlow("Flow Shuffle", "Flow"),
    AlbumPreserving("Album Shuffle", "Album"),
    PureRandom("Pure Random", "Random");

    fun next(): FlowShuffleMode = when (this) {
        Off -> SmartFlow
        SmartFlow -> AlbumPreserving
        AlbumPreserving -> PureRandom
        PureRandom -> Off
    }
}

/**
 * Explanatory reason attached to a shuffled track to provide transparent listening context.
 */
data class TransitionCue(
    val badge: String,
    val description: String
)

data class ShuffledTrackEntry(
    val track: Track,
    val cue: TransitionCue? = null
)

/**
 * Pure Kotlin implementation of the Pixelody Flow Shuffle Algorithm.
 * Balances serendipity, harmonic flow, energy trajectories, and strict anti-clumping.
 */
object FlowShuffleEngine {

    private fun normalize(text: String?): String =
        text?.lowercase(Locale.US)?.replace(Regex("[^a-z0-9]+"), " ")?.trim().orEmpty()

    private fun keyCompatibilityScore(left: Track?, right: Track): Int {
        if (left == null) return 0
        val leftKey = HarmonicKeyEngine.parseKey(left.format) ?: HarmonicKeyEngine.parseKey(left.title)
        val rightKey = HarmonicKeyEngine.parseKey(right.format) ?: HarmonicKeyEngine.parseKey(right.title)
        if (leftKey != null && rightKey != null) {
            val relation = HarmonicKeyEngine.calculateHarmonicRelation(leftKey, rightKey)
            return when (relation) {
                com.pixelody.app.data.model.HarmonicRelation.ExactMatch -> 5
                com.pixelody.app.data.model.HarmonicRelation.RelativeMajorMinor -> 4
                com.pixelody.app.data.model.HarmonicRelation.AdjacentStep -> 3
                com.pixelody.app.data.model.HarmonicRelation.DiagonalStep -> 2
                com.pixelody.app.data.model.HarmonicRelation.EnergyBoost -> 2
                com.pixelody.app.data.model.HarmonicRelation.EnergyDrop -> 1
                com.pixelody.app.data.model.HarmonicRelation.DissonantClash -> -3
            }
        }
        val leftTelemetry = HarmonicKeyEngine.estimateTrackTelemetry(left)
        val rightTelemetry = HarmonicKeyEngine.estimateTrackTelemetry(right)
        val relation = HarmonicKeyEngine.calculateHarmonicRelation(leftTelemetry.key, rightTelemetry.key)
        return when (relation) {
            com.pixelody.app.data.model.HarmonicRelation.ExactMatch -> 4
            com.pixelody.app.data.model.HarmonicRelation.RelativeMajorMinor -> 3
            com.pixelody.app.data.model.HarmonicRelation.AdjacentStep -> 2
            com.pixelody.app.data.model.HarmonicRelation.DiagonalStep -> 1
            com.pixelody.app.data.model.HarmonicRelation.EnergyBoost -> 1
            com.pixelody.app.data.model.HarmonicRelation.EnergyDrop -> 0
            com.pixelody.app.data.model.HarmonicRelation.DissonantClash -> -2
        }
    }

    private fun diversityPenalty(sequence: List<Track>, candidate: Track, windowSize: Int = 4): Pair<Int, String?> {
        val candidateArtist = normalize(candidate.artist)
        val candidateAlbum = normalize(candidate.album)
        val recent = sequence.takeLast(max(1, windowSize))

        val artistRepeat = candidateArtist.isNotBlank() && recent.any { normalize(it.artist) == candidateArtist }
        val albumRepeat = candidateAlbum.isNotBlank() && recent.any { normalize(it.album) == candidateAlbum }

        return when {
            artistRepeat -> -50 to "Artist repeat"
            albumRepeat -> -30 to "Album repeat"
            else -> 10 to "Artist Spaced"
        }
    }

    /**
     * Groups tracks into cohesive album units for Album-Preserving Shuffle.
     */
    private fun groupAlbumUnits(tracks: List<Track>): List<List<Track>> {
        val map = linkedMapOf<String, MutableList<Track>>()
        for (track in tracks) {
            val key = if (track.album.isNotBlank()) "${normalize(track.artist)} / ${normalize(track.album)}" else "id:${track.id}"
            map.getOrPut(key) { mutableListOf() }.add(track)
        }
        return map.values.toList()
    }

    /**
     * Plans a shuffled queue from a pool of tracks according to the requested mode.
     */
    fun planQueue(
        currentTrack: Track?,
        pool: List<Track>,
        mode: FlowShuffleMode,
        horizon: Int = 50
    ): List<ShuffledTrackEntry> {
        val candidates = pool.filterNot { it.missing || it.streamUrl.isBlank() }
        if (candidates.isEmpty()) return emptyList()

        return when (mode) {
            FlowShuffleMode.Off -> {
                candidates.map { ShuffledTrackEntry(it) }
            }

            FlowShuffleMode.PureRandom -> {
                val shuffled = candidates.filterNot { it.id == currentTrack?.id }.shuffled()
                shuffled.mapIndexed { index, track ->
                    val cue = if (index % 5 == 0) TransitionCue("Surprise", "Pure random draw") else null
                    ShuffledTrackEntry(track, cue)
                }
            }

            FlowShuffleMode.AlbumPreserving -> {
                val remaining = candidates.filterNot { it.id == currentTrack?.id }
                val groups = groupAlbumUnits(remaining).shuffled()
                val result = mutableListOf<ShuffledTrackEntry>()
                groups.forEach { albumTracks ->
                    albumTracks.forEachIndexed { i, track ->
                        val cue = if (i == 0 && albumTracks.size > 1) {
                            TransitionCue("Album Run", "${track.album} (${albumTracks.size} tracks)")
                        } else null
                        result.add(ShuffledTrackEntry(track, cue))
                    }
                }
                result.take(horizon)
            }

            FlowShuffleMode.SmartFlow -> {
                val available = candidates.filterNot { it.id == currentTrack?.id }.toMutableList()
                val sequence = mutableListOf<Track>()
                val result = mutableListOf<ShuffledTrackEntry>()
                var previous = currentTrack

                var step = 0
                while (available.isNotEmpty() && result.size < horizon) {
                    step++
                    var bestCandidateIndex = 0
                    var bestScore = Int.MIN_VALUE
                    var bestCue: TransitionCue? = null

                    val history = (listOfNotNull(currentTrack) + sequence)

                    for (i in available.indices) {
                        val cand = available[i]
                        val (divPoints, divLabel) = diversityPenalty(history, cand, windowSize = min(4, available.size))
                        val keyPoints = keyCompatibilityScore(previous, cand)
                        val genrePoints = GenreTaxonomyEngine.genreAffinityScore(previous?.genre, cand.genre)
                        val qualityPoints = if (cand.lossless) 3 else 0
                        val randomJitter = (0..3).random()

                        val totalScore = divPoints + keyPoints + genrePoints + qualityPoints + randomJitter

                        if (totalScore > bestScore) {
                            bestScore = totalScore
                            bestCandidateIndex = i
                            val badge = when {
                                cand.lossless && step % 4 == 0 -> "Hi-Res Flow"
                                genrePoints >= 6 -> "Genre Match (${GenreTaxonomyEngine.canonicalize(cand.genre)})"
                                keyPoints > 0 -> {
                                    val key = HarmonicKeyEngine.parseKey(cand.format)
                                        ?: HarmonicKeyEngine.parseKey(cand.title)
                                        ?: HarmonicKeyEngine.estimateTrackTelemetry(cand).key
                                    "Harmonic Key (${key.code})"
                                }
                                genrePoints >= 4 -> "Genre Blend (${GenreTaxonomyEngine.canonicalize(cand.genre)})"
                                divLabel != null && divPoints > 0 -> divLabel
                                else -> "Energy Match"
                            }
                            bestCue = TransitionCue(badge, "Flow match after ${previous?.artist ?: "now playing"}")
                        }
                    }

                    val chosen = available.removeAt(bestCandidateIndex)
                    sequence.add(chosen)
                    result.add(ShuffledTrackEntry(chosen, bestCue))
                    previous = chosen
                }

                result
            }
        }
    }
}
