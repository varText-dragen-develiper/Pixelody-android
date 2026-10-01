package com.pixelody.app.data.model

import androidx.compose.ui.graphics.Color

/**
 * Camelot mode representing Minor (A) and Major (B) tonalities.
 */
enum class CamelotMode(val code: String, val displayName: String) {
    Minor("A", "Minor"),
    Major("B", "Major")
}

/**
 * High-precision Camelot Key representation on the 12-hour harmonic circle.
 */
enum class CamelotKey(
    val number: Int,
    val mode: CamelotMode,
    val musicalKey: String,
    val enharmonic: String = "",
    val wheelAngleDeg: Float,
    val harmonicColorHex: Long
) {
    // Inner Ring: Minor Tonality (A)
    K1A(1, CamelotMode.Minor, "A♭ minor", "G♯ minor", 30f, 0xFF00D5D8),
    K2A(2, CamelotMode.Minor, "E♭ minor", "D♯ minor", 60f, 0xFF0284C7),
    K3A(3, CamelotMode.Minor, "B♭ minor", "A♯ minor", 90f, 0xFF2563EB),
    K4A(4, CamelotMode.Minor, "F minor", "", 120f, 0xFF6366F1),
    K5A(5, CamelotMode.Minor, "C minor", "", 150f, 0xFF8B5CF6),
    K6A(6, CamelotMode.Minor, "G minor", "", 180f, 0xFFD946EF),
    K7A(7, CamelotMode.Minor, "D minor", "", 210f, 0xFFF43F5E),
    K8A(8, CamelotMode.Minor, "A minor", "", 240f, 0xFFEA580C),
    K9A(9, CamelotMode.Minor, "E minor", "", 270f, 0xFFF59E0B),
    K10A(10, CamelotMode.Minor, "B minor", "", 300f, 0xFF84CC16),
    K11A(11, CamelotMode.Minor, "F♯ minor", "G♭ minor", 330f, 0xFF10B981),
    K12A(12, CamelotMode.Minor, "D♭ minor", "C♯ minor", 0f, 0xFF06B6D4),

    // Outer Ring: Major Tonality (B)
    K1B(1, CamelotMode.Major, "B major", "C♭ major", 30f, 0xFF22D3EE),
    K2B(2, CamelotMode.Major, "F♯ major", "G♭ major", 60f, 0xFF38BDF8),
    K3B(3, CamelotMode.Major, "D♭ major", "C♯ major", 90f, 0xFF60A5FA),
    K4B(4, CamelotMode.Major, "A♭ major", "G♯ major", 120f, 0xFF818CF8),
    K5B(5, CamelotMode.Major, "E♭ major", "D♯ major", 150f, 0xFFA78BFA),
    K6B(6, CamelotMode.Major, "B♭ major", "A♯ major", 180f, 0xFFE879F9),
    K7B(7, CamelotMode.Major, "F major", "", 210f, 0xFFFB7185),
    K8B(8, CamelotMode.Major, "C major", "", 240f, 0xFFFB923C),
    K9B(9, CamelotMode.Major, "G major", "", 270f, 0xFFFBBF24),
    K10B(10, CamelotMode.Major, "D major", "", 300f, 0xFFA3E635),
    K11B(11, CamelotMode.Major, "A major", "", 330f, 0xFF34D399),
    K12B(12, CamelotMode.Major, "E major", "", 0f, 0xFF22D3EE);

    val code: String
        get() = "$number${mode.code}"

    val fullTitle: String
        get() = "$code • $musicalKey"

    val harmonicColor: Color
        get() = Color(harmonicColorHex)

    companion object {
        fun fromCode(code: String): CamelotKey? {
            val clean = code.trim().uppercase()
            return entries.firstOrNull { it.code.equals(clean, ignoreCase = true) }
        }

        fun fromNumberAndMode(number: Int, mode: CamelotMode): CamelotKey {
            val normalizedNumber = ((number - 1) % 12 + 12) % 12 + 1
            return entries.first { it.number == normalizedNumber && it.mode == mode }
        }
    }
}

/**
 * Classification of musical harmonic relationships between two Camelot keys.
 */
enum class HarmonicRelation(
    val title: String,
    val score: Float,
    val advice: String,
    val badgeColorHex: Long
) {
    ExactMatch(
        title = "Exact Match",
        score = 1.0f,
        advice = "Perfect harmonic cohesion with identical root scale and chords.",
        badgeColorHex = 0xFF4ADE80 // Green
    ),
    RelativeMajorMinor(
        title = "Relative Key",
        score = 0.95f,
        advice = "Emotional modal shift sharing all 7 diatonic notes.",
        badgeColorHex = 0xFF38BDF8 // Sky Blue
    ),
    AdjacentStep(
        title = "Adjacent Step (±1)",
        score = 0.90f,
        advice = "Natural 5th interval harmonic progression (Dominant/Subdominant).",
        badgeColorHex = 0xFFA78BFA // Purple
    ),
    EnergyBoost(
        title = "Energy Boost (+2)",
        score = 0.75f,
        advice = "Euphoric two-step key modulation lifting overall energy.",
        badgeColorHex = 0xFFF59E0B // Amber
    ),
    EnergyDrop(
        title = "Energy Drop (-2)",
        score = 0.70f,
        advice = "Moody two-step downward harmonic pivot.",
        badgeColorHex = 0xFFFB923C // Orange
    ),
    DiagonalStep(
        title = "Diagonal Pivot",
        score = 0.65f,
        advice = "Warm diagonal modal modulation across major/minor boundaries.",
        badgeColorHex = 0xFFEC4899 // Pink
    ),
    DissonantClash(
        title = "Harmonic Clash",
        score = 0.25f,
        advice = "High harmonic dissonance. Recommended to use Bass Swap, Filter Sweep, or Vinyl Brake.",
        badgeColorHex = 0xFFEF4444 // Red
    );

    val isHarmonic: Boolean get() = this != DissonantClash
}

/**
 * Supported transition curves for Auto-DJ mixing.
 */
enum class DjTransitionCurve(
    val title: String,
    val subtitle: String,
    val description: String,
    val iconGlyphName: String
) {
    EqualPower(
        title = "Equal Power",
        subtitle = "Smooth Sinusoidal",
        description = "Constant psychoacoustic RMS energy fade preventing volume dips.",
        iconGlyphName = "WaveformBars"
    ),
    BassSwap(
        title = "Bass Swap",
        subtitle = "Low-End Crossover",
        description = "Instant low-frequency cutoff switch at midpoint to prevent muddy phase clash.",
        iconGlyphName = "Sliders"
    ),
    FilterSweep(
        title = "Filter Sweep",
        subtitle = "High-Pass Resonant",
        description = "Sweeps outgoing track low/mid body away into shimmering highs before dropping incoming track.",
        iconGlyphName = "Sparkle"
    ),
    EchoOut(
        title = "Echo Out",
        subtitle = "Delay Feedback Tail",
        description = "Cuts outgoing deck with simulated rhythmic echo delay tail as incoming track drops.",
        iconGlyphName = "Broadcast"
    ),
    VinylBrake(
        title = "Vinyl Brake",
        subtitle = "Turntable Decel Bend",
        description = "Simulates analog turntable motor stop with pitch bend down into instant incoming drop.",
        iconGlyphName = "VinylDisc"
    )
}

/**
 * Settings and preferences for Auto-DJ.
 */
data class AutoDjSettings(
    val isAutoDjEnabled: Boolean = true,
    val preferredCurve: DjTransitionCurve = DjTransitionCurve.EqualPower,
    val transitionDurationSeconds: Int = 4,
    val autoTriggerBeforeEndSeconds: Int = 8,
    val pitchSyncEnabled: Boolean = true,
    val harmonicSortEnabled: Boolean = true
)

/**
 * Real-time DSP parameters calculated during an active DJ transition frame.
 */
data class DjEnvelopeFrame(
    val progress: Float = 0f,
    val outgoingGain: Float = 1.0f,
    val incomingGain: Float = 0f,
    val outgoingHighPassCutoffHz: Float = 20f,
    val incomingLowPassCutoffHz: Float = 20000f,
    val echoWetLevel: Float = 0f,
    val playbackSpeedFactor: Float = 1.0f,
    val isTransitioning: Boolean = false
)

/**
 * Telemetry details for a DJ deck (Current Outgoing vs Upcoming Incoming).
 */
data class TrackDjTelemetry(
    val track: Track,
    val bpm: Float,
    val key: CamelotKey,
    val detectedKeyConfidence: Float = 0.95f
)

/**
 * Complete Harmonic Analysis result between two tracks.
 */
data class HarmonicAnalysisResult(
    val currentKey: CamelotKey,
    val nextKey: CamelotKey,
    val relation: HarmonicRelation,
    val currentBpm: Float,
    val nextBpm: Float,
    val bpmDelta: Float,
    val pitchStretchPercent: Float,
    val recommendedCurve: DjTransitionCurve,
    val overallCompatibilityScore: Float
)

/**
 * Interactive Harmonic Energy Steering mode for the Auto-DJ console.
 */
enum class HarmonicEnergyMode(
    val title: String,
    val subtitle: String,
    val deltaCamelot: Int,
    val tag: String,
    val colorHex: Long
) {
    HarmonicLock(
        title = "Harmonic Lock",
        subtitle = "Strict ±1 step adjacent matching",
        deltaCamelot = 0,
        tag = "HARMONIC LOCK",
        colorHex = 0xFF4ADE80
    ),
    EnergyBoost2(
        title = "Energy Boost (+2)",
        subtitle = "+2 Camelot modulation to raise dancefloor energy",
        deltaCamelot = 2,
        tag = "ENERGY BOOST +2",
        colorHex = 0xFFE5A93C
    ),
    SunsetDrift2(
        title = "Sunset Drift (-2)",
        subtitle = "-2 Camelot downshift for mellow harmonic unwinding",
        deltaCamelot = -2,
        tag = "SUNSET DRIFT -2",
        colorHex = 0xFF38BDF8
    )
}

