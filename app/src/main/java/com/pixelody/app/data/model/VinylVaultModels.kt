package com.pixelody.app.data.model

/**
 * Visual perspective and rotational state of a vinyl sleeve inside a 3D flippable crate.
 */
data class VinylSleeveState(
    val track: Track,
    val sleeveIndex: Int,
    val tiltAngleDeg: Float = 0f,
    val zIndexOffset: Float = 0f,
    val isFocused: Boolean = false
)

/**
 * Sealed collectible temporal audio capsule capturing a distinct listening session with telemetry.
 */
data class TemporalSonicCapsule(
    val id: String,
    val title: String,
    val recordedAtEpochMs: Long,
    val timeOfDayLabel: String,
    val dominantKey: CamelotKey,
    val averageBpm: Float,
    val totalTracks: Int,
    val tracks: List<Track>,
    val atmosphereTag: String
)
