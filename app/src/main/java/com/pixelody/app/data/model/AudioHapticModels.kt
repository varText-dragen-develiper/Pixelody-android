package com.pixelody.app.data.model

/**
 * Operating modes for Pixelody's Audio-Haptic Resonance Engine.
 */
enum class AudioHapticMode(
    val title: String,
    val subtitle: String,
    val description: String,
    val accentColorHex: Long
) {
    SubBassRumble(
        title = "Sub-Bass Rumble",
        subtitle = "20Hz - 80Hz Low-End",
        description = "Deep tactile pulses synchronized with sub-bass drops, 808s, and heavy low-frequency resonance.",
        accentColorHex = 0xFF6366F1 // Indigo
    ),
    BeatPunch(
        title = "Beat Punch",
        subtitle = "Rhythmic Transients",
        description = "Crisp mechanical downbeat clicks and tactile punches tracking rhythmic kick drums and snare strikes.",
        accentColorHex = 0xFF38BDF8 // Sky Blue
    ),
    FullSpectrum(
        title = "Full Spectrum",
        subtitle = "Multi-Band Tactile",
        description = "Dynamic multi-tier feedback: heavy low-ticks on sub-bass, sharp clicks on snares, and micro-ticks on percussion.",
        accentColorHex = 0xFF10B981 // Emerald Green
    ),
    VinylAcoustic(
        title = "Vinyl Acoustic",
        subtitle = "Analog Texture",
        description = "Subtle mechanical tonearm drop sensations, turntable platter inertia, and analog stylus groove textures.",
        accentColorHex = 0xFFF59E0B // Amber
    ),
    Off(
        title = "Haptics Off",
        subtitle = "Silent",
        description = "Audio-haptic tactile feedback is disabled.",
        accentColorHex = 0xFF64748B // Slate
    )
}

/**
 * Supported tactile vibration primitives.
 */
enum class HapticPrimitiveType(val displayName: String, val baseDurationMs: Long) {
    LowTick("Sub-Bass Thud", 25L),
    Click("Kick/Snare Click", 20L),
    Tick("Perception Tick", 12L),
    QuickRise("Bass Swell", 45L),
    QuickFall("Bass Drop", 40L),
    Spin("Vinyl Platter Spin", 60L)
}

/**
 * Real-time event emitted when a tactile vibration pulse triggers.
 */
data class HapticPulseEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val primitive: HapticPrimitiveType = HapticPrimitiveType.LowTick,
    val amplitude: Float = 0.8f,
    val bandLabel: String = "SUB"
)

/**
 * User-configurable settings for Audio-Haptic resonance and tactile feedback.
 */
data class AudioHapticSettings(
    val isEnabled: Boolean = true,
    val mode: AudioHapticMode = AudioHapticMode.SubBassRumble,
    val intensity: Float = 0.75f,
    val subBassBoost: Float = 1.2f,
    val transientSensitivity: Float = 0.65f,
    val batterySaverThrottling: Boolean = true,
    val turntableHapticsEnabled: Boolean = true
)
