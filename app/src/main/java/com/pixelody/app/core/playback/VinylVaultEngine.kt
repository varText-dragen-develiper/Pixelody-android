package com.pixelody.app.core.playback

import com.pixelody.app.data.model.CamelotKey
import com.pixelody.app.data.model.TemporalSonicCapsule
import com.pixelody.app.data.model.Track
import java.util.Calendar
import java.util.Locale

/**
 * VinylVaultEngine: Physics and archiving engine for 3D crate sleeve digging and temporal capsule memory recording.
 */
object VinylVaultEngine {

    /**
     * Calculates the 3D perspective tilt angle (degrees) for a sleeve at [index] relative to the [activeIndex].
     */
    fun calculateSleeveTiltAngle(index: Int, activeIndex: Int): Float {
        return when {
            index < activeIndex -> -28.0f // Flipped forward toward user
            index == activeIndex -> 0.0f  // Upright focused sleeve
            else -> 18.0f                 // Stacked backward in crate
        }
    }

    /**
     * Generates a sealed [TemporalSonicCapsule] from an active listening session.
     */
    fun createTemporalSonicCapsule(
        sessionTracks: List<Track>,
        customTitle: String? = null,
        timestampMs: Long = System.currentTimeMillis()
    ): TemporalSonicCapsule {
        if (sessionTracks.isEmpty()) {
            return TemporalSonicCapsule(
                id = "capsule_${timestampMs}",
                title = customTitle ?: "Empty Session",
                recordedAtEpochMs = timestampMs,
                timeOfDayLabel = "Midnight Drift",
                dominantKey = CamelotKey.K8A,
                averageBpm = 120f,
                totalTracks = 0,
                tracks = emptyList(),
                atmosphereTag = "Ambient Neutral"
            )
        }

        val telemetries = sessionTracks.map { HarmonicKeyEngine.estimateTrackTelemetry(it) }
        val avgBpm = telemetries.map { it.bpm }.average().toFloat()

        val dominantKey = telemetries.groupBy { it.key }
            .maxByOrNull { it.value.size }
            ?.key ?: telemetries.first().key

        val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
        val hour = cal.get(Calendar.HOUR_OF_DAY)

        val timeOfDay = when (hour) {
            in 5..11 -> "Morning Awakening"
            in 12..16 -> "Midday Flow"
            in 17..20 -> "Golden Hour Horizon"
            in 21..23 -> "Late Night Session"
            else -> "Deep Midnight Archive"
        }

        val atmosphere = when {
            avgBpm >= 126f -> "High-Energy Peak"
            avgBpm >= 115f -> "Groove & Movement"
            avgBpm >= 95f -> "Mellow Warmth"
            else -> "Deep Downtempo"
        }

        val title = customTitle ?: "$timeOfDay • ${sessionTracks.first().artist.ifBlank { dominantKey.code }}"

        return TemporalSonicCapsule(
            id = "capsule_${timestampMs}_${sessionTracks.size}",
            title = title,
            recordedAtEpochMs = timestampMs,
            timeOfDayLabel = timeOfDay,
            dominantKey = dominantKey,
            averageBpm = avgBpm,
            totalTracks = sessionTracks.size,
            tracks = sessionTracks,
            atmosphereTag = atmosphere
        )
    }

    /**
     * Filters tracks within a crate by search query and optional Camelot key lock.
     */
    fun filterCrateTracks(
        tracks: List<Track>,
        query: String,
        keyFilter: CamelotKey? = null
    ): List<Track> {
        val needle = query.trim().lowercase(Locale.US)
        return tracks.filter { track ->
            val matchesQuery = if (needle.isBlank()) true else {
                track.title.lowercase(Locale.US).contains(needle) ||
                        track.artist.lowercase(Locale.US).contains(needle) ||
                        track.album.lowercase(Locale.US).contains(needle)
            }

            val matchesKey = if (keyFilter == null) true else {
                val tel = HarmonicKeyEngine.estimateTrackTelemetry(track)
                tel.key == keyFilter
            }

            matchesQuery && matchesKey
        }
    }
}
