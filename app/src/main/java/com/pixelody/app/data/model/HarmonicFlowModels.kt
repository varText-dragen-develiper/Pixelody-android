package com.pixelody.app.data.model

/**
 * Unified Harmonic Flow Profiles synthesizing Auto-DJ, Flow Shuffle, and Camelot arithmetic into 1-tap listening modes.
 */
enum class HarmonicFlowProfile(
    val title: String,
    val subtitle: String,
    val tag: String,
    val energyMode: HarmonicEnergyMode,
    val defaultCurve: DjTransitionCurve,
    val defaultDurationSeconds: Int,
    val colorHex: Long
) {
    ClubSetFlow(
        title = "Club / Set Flow",
        subtitle = "Energy lifts and bass-swap transitions for a more energetic set",
        tag = "CLUB SET",
        energyMode = HarmonicEnergyMode.EnergyBoost2,
        defaultCurve = DjTransitionCurve.BassSwap,
        defaultDurationSeconds = 4,
        colorHex = 0xFFF59E0B // Warm Amber
    ),
    DeepListening(
        title = "Deep Listening",
        subtitle = "Closer key matches with equal-power fades",
        tag = "DEEP LISTEN",
        energyMode = HarmonicEnergyMode.HarmonicLock,
        defaultCurve = DjTransitionCurve.EqualPower,
        defaultDurationSeconds = 8,
        colorHex = 0xFF10B981 // Emerald Green
    ),
    SunsetChill(
        title = "Sunset Chill",
        subtitle = "Lower-energy key changes with echo or vinyl-style endings",
        tag = "SUNSET CHILL",
        energyMode = HarmonicEnergyMode.SunsetDrift2,
        defaultCurve = DjTransitionCurve.EchoOut,
        defaultDurationSeconds = 6,
        colorHex = 0xFF38BDF8 // Sky Blue
    ),
    CustomManual(
        title = "Custom Manual",
        subtitle = "DJ controls and adjustable processing curves",
        tag = "CUSTOM",
        energyMode = HarmonicEnergyMode.HarmonicLock,
        defaultCurve = DjTransitionCurve.EqualPower,
        defaultDurationSeconds = 4,
        colorHex = 0xFFA78BFA // Purple
    )
}

/**
 * Detailed analysis of harmonic compatibility and acoustic connection between two adjacent queue slots.
 */
data class QueueSlotHarmonicAffinity(
    val slotIndex: Int,
    val fromTrackId: String,
    val toTrackId: String,
    val fromKey: CamelotKey?,
    val toKey: CamelotKey?,
    val relation: HarmonicRelation,
    val compatibilityScore: Float,
    val bpmDelta: Float?,
    val pitchStretchPercent: Float?,
    val isClash: Boolean,
    val recommendedCurve: DjTransitionCurve,
    val suggestedBridgeKey: CamelotKey? = null
)

/**
 * Single data point along an album, playlist, or queue harmonic trajectory arc.
 */
data class HarmonicTrajectoryPoint(
    val trackId: String,
    val title: String,
    val key: CamelotKey?,
    val bpm: Float?,
    val normalizedWheelPosition: Float?, // Unavailable when key metadata is absent
    val energyLevel: Float?, // Weak tempo heuristic, not measured energy
    val energyProvenance: String = "unknown"
)

/**
 * Energy Contour Presets for interactive queue trajectory sculpting.
 */
enum class EnergyContourPreset(
    val title: String,
    val subtitle: String,
    val tag: String,
    val targetEnergyFunction: (progress: Float) -> Float
) {
    RampUp(
        title = "Ramp Up / Lift",
        subtitle = "Build from a quieter opening toward a peak",
        tag = "RAMP UP",
        targetEnergyFunction = { progress -> (0.25f + 0.70f * progress).coerceIn(0.1f, 1.0f) }
    ),
    PeakWave(
        title = "Peak Wave",
        subtitle = "Build toward a peak at 60 percent, then wind down",
        tag = "PEAK WAVE",
        targetEnergyFunction = { progress ->
            (0.35f + 0.60f * kotlin.math.sin(progress * Math.PI.toFloat())).coerceIn(0.1f, 1.0f)
        }
    ),
    SunsetDrift(
        title = "Sunset Drift",
        subtitle = "Gradually reduce the energy",
        tag = "SUNSET DRIFT",
        targetEnergyFunction = { progress -> (0.90f - 0.65f * progress).coerceIn(0.1f, 1.0f) }
    ),
    HarmonicPlateau(
        title = "Plateau Lock",
        subtitle = "Keep energy steady while favoring related keys",
        tag = "PLATEAU",
        targetEnergyFunction = { 0.65f }
    ),
    CustomSculpt(
        title = "Custom Sculpt",
        subtitle = "Draw the energy curve yourself",
        tag = "CUSTOM",
        targetEnergyFunction = { 0.5f }
    )
}

/**
 * Control point along an interactive energy sculpting curve.
 */
data class EnergyContourPoint(
    val progress: Float, // 0.0 to 1.0
    val targetEnergy: Float // 0.0 to 1.0
)

/**
 * High-level harmonic telemetry and energy summary across a playlist, album, or crate.
 */
data class CollectionHarmonicTelemetry(
    val keyRange: String,
    val dominantKey: CamelotKey?,
    val averageBpm: Float?,
    val harmonicCohesionPercent: Int?,
    val trajectoryPoints: List<HarmonicTrajectoryPoint>
)

