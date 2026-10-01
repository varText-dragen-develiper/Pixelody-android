package com.pixelody.app.feature.library

import com.pixelody.app.core.playback.HarmonicKeyEngine
import com.pixelody.app.data.model.Track

/**
 * Multi-criteria sorting modes for library browsing and playlist management.
 */
enum class TrackSortMode(val label: String, val badge: String) {
    Default("Default", "☰"),
    TitleAsc("Title (A-Z)", "A-Z"),
    ArtistAsc("Artist", "ARTIST"),
    HarmonicKey("Harmonic Key", "KEY"),
    DurationDesc("Duration", "TIME"),
    QualityFirst("Hi-Res First", "HI-RES");

    fun sortTracks(tracks: List<Track>): List<Track> {
        return when (this) {
            Default -> tracks
            TitleAsc -> tracks.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            ArtistAsc -> tracks.sortedWith(
                compareBy<Track, String>(String.CASE_INSENSITIVE_ORDER) { it.artist.ifBlank { "Unknown" } }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            )
            HarmonicKey -> tracks.sortedWith(
                compareBy<Track> {
                    val key = HarmonicKeyEngine.estimateTrackTelemetry(it).key
                    key.number * 2 + (if (key.mode == com.pixelody.app.data.model.CamelotMode.Minor) 0 else 1)
                }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            )
            DurationDesc -> tracks.sortedWith(
                compareByDescending<Track> { it.durationSeconds }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            )
            QualityFirst -> tracks.sortedWith(
                compareByDescending<Track> { it.lossless }
                    .thenByDescending { it.bitrate ?: 0 }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            )
        }
    }
}
