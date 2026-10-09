package com.pixelody.app.core.playback

import com.pixelody.app.core.genre.GenreTaxonomyEngine
import com.pixelody.app.data.model.Track
import com.pixelody.app.data.model.CamelotKey
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

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
 * Balances variety and known metadata. Unknown keys contribute no harmonic score.
 */
object FlowShuffleEngine {

    /** Reorder an existing queue by occurrence, retaining repeats and unresolved media items. */
    fun planUpcomingOrder(
        currentTrack: Track?, upcoming: List<Track?>, mode: FlowShuffleMode,
        random: Random = Random.Default, checkActive: () -> Unit = {}
    ): List<Int> {
        if (mode == FlowShuffleMode.Off) return upcoming.indices.toList()
        val aliases = upcoming.mapIndexedNotNull { position, track ->
            checkActive()
            track?.copy(id = "flow-occurrence-$position")
        }
        // These identities exist only inside planning. The host applies positions to
        // its original media items, so real track IDs and metadata never change.
        val positions = aliases.associate { it.id to it.id.removePrefix("flow-occurrence-").toInt() }
        val planned = planQueue(currentTrack?.copy(id = "flow-current-anchor"), aliases, mode,
            aliases.size, random, checkActive).mapNotNull { positions[it.track.id] }
        val plannedSet = planned.toSet()
        return planned + upcoming.indices.filterNot { it in plannedSet }
    }

    private fun normalize(text: String?): String =
        text?.lowercase(Locale.US)?.replace(Regex("[^\\p{L}\\p{N}]+"), " ")?.trim().orEmpty()

    private fun keyCompatibilityScore(leftKey: CamelotKey?, rightKey: CamelotKey?): Int {
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
        return 0
    }

    private fun diversityPenalty(sequence: List<Track>, candidate: Track, normalized: (String?) -> String, windowSize: Int = 4): Pair<Int, String?> {
        val candidateArtist = normalized(candidate.artist)
        val candidateAlbum = normalized(candidate.album)
        val recent = sequence.takeLast(max(1, windowSize))

        val artistRepeat = candidateArtist.isNotBlank() && recent.any { normalized(it.artist) == candidateArtist }
        val albumRepeat = candidateAlbum.isNotBlank() && recent.any { normalized(it.album) == candidateAlbum }

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
        horizon: Int = 50,
        random: Random = Random.Default,
        checkActive: () -> Unit = {}
    ): List<ShuffledTrackEntry> {
        if (horizon <= 0) return emptyList()
        val candidates = pool.filterNot { it.missing || it.streamUrl.isBlank() || it.id == currentTrack?.id }
            .distinctBy { it.id }
        if (candidates.isEmpty()) return emptyList()

        return when (mode) {
            FlowShuffleMode.Off -> {
                candidates.take(horizon).map { ShuffledTrackEntry(it) }
            }

            FlowShuffleMode.PureRandom -> {
                val shuffled = candidates.shuffled(random).take(horizon)
                shuffled.mapIndexed { index, track ->
                    val cue = if (index % 5 == 0) TransitionCue("Surprise", "Pure random draw") else null
                    ShuffledTrackEntry(track, cue)
                }
            }

            FlowShuffleMode.AlbumPreserving -> {
                val groups = groupAlbumUnits(candidates).shuffled(random).toMutableList()
                // Finish the current album before switching to a randomly chosen album.
                val currentGroup = groups.indexOfFirst { group -> currentTrack != null &&
                    currentTrack.album.isNotBlank() && normalize(group.first().album) == normalize(currentTrack.album) &&
                    normalize(group.first().artist) == normalize(currentTrack.artist) }
                if (currentGroup >= 0) groups.add(0, groups.removeAt(currentGroup))
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
                val available = candidates.shuffled(random).toMutableList()
                val sequence = mutableListOf<Track>()
                val result = mutableListOf<ShuffledTrackEntry>()
                var previous = currentTrack
                val normalizedText = mutableMapOf<String?, String>()
                val normalized: (String?) -> String = { normalizedText.getOrPut(it) { normalize(it) } }
                val keys = (listOfNotNull(currentTrack) + candidates).associate { it.id to
                    FlowBrowseFilter.knownKey(it) }
                val genreScores = mutableMapOf<Pair<String?, String>, Int>()

                var step = 0
                while (available.isNotEmpty() && result.size < horizon) {
                    checkActive()
                    step++
                    var bestCandidateIndex = 0
                    var bestScore = Int.MIN_VALUE
                    var bestCue: TransitionCue? = null

                    val history = listOfNotNull(currentTrack) + sequence.takeLast(4)

                    // Bounded randomized sampling keeps full-library planning responsive.
                    // Small queues still compare every candidate.
                    for (i in 0 until min(64, available.size)) {
                        val cand = available[i]
                        val (divPoints, divLabel) = diversityPenalty(history, cand, normalized)
                        val keyPoints = keyCompatibilityScore(keys[previous?.id], keys[cand.id])
                        val genrePoints = genreScores.getOrPut(previous?.genre to cand.genre) {
                            GenreTaxonomyEngine.genreAffinityScore(previous?.genre, cand.genre)
                        }
                        val qualityPoints = if (cand.lossless) 3 else 0
                        val randomJitter = random.nextInt(4)

                        val totalScore = divPoints + keyPoints + genrePoints + qualityPoints + randomJitter

                        if (totalScore > bestScore) {
                            bestScore = totalScore
                            bestCandidateIndex = i
                            val badge = when {
                                cand.lossless && step % 4 == 0 -> "Lossless Flow"
                                genrePoints >= 6 -> "Genre Match (${GenreTaxonomyEngine.canonicalize(cand.genre)})"
                                keyPoints > 0 -> {
                                    val key = keys[cand.id]
                                    "Harmonic Key (${key?.code})"
                                }
                                genrePoints >= 4 -> "Genre Blend (${GenreTaxonomyEngine.canonicalize(cand.genre)})"
                                divLabel != null && divPoints > 0 -> divLabel
                                else -> "Variety"
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
