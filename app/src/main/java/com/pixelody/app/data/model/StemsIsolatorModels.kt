package com.pixelody.app.data.model

/**
 * 4 Distinct Audio Stem Channels for real-time deconstruction and DJ remixing.
 */
enum class StemType(
    val title: String,
    val subtitle: String,
    val description: String,
    val colorHex: Long,
    val defaultCenterHz: Float,
    val lowFreqHz: Float,
    val highFreqHz: Float,
    val badgeGlyph: String
) {
    Vocals(
        title = "Vocals",
        subtitle = "Center Lead & Harmony",
        description = "Isolates center-channel vocal formants, lead vocals, and backing choir with mid/side extraction.",
        colorHex = 0xFF06B6D4, // Cyan
        defaultCenterHz = 2200f,
        lowFreqHz = 300f,
        highFreqHz = 4800f,
        badgeGlyph = "VOC"
    ),
    Drums(
        title = "Drums",
        subtitle = "Percussion & Transients",
        description = "Captures transient rhythmic snap, snares, hi-hats, and punchy acoustic/electronic kick attacks.",
        colorHex = 0xFFF59E0B, // Amber Gold
        defaultCenterHz = 650f,
        lowFreqHz = 180f,
        highFreqHz = 12000f,
        badgeGlyph = "DRM"
    ),
    Bass(
        title = "Bass",
        subtitle = "Sub & Low Fundaments",
        description = "Isolates sub-bass frequencies, 808s, bass guitar notes, and deep synthesizer low ends (<220Hz).",
        colorHex = 0xFF8B5CF6, // Royal Violet
        defaultCenterHz = 90f,
        lowFreqHz = 20f,
        highFreqHz = 240f,
        badgeGlyph = "BAS"
    ),
    Instruments(
        title = "Instruments",
        subtitle = "Harmonic Bed & Air",
        description = "Captures guitars, synthesizers, pianos, brass, stereo ambient reverbs, and high-frequency sparkle.",
        colorHex = 0xFF10B981, // Emerald Green
        defaultCenterHz = 3500f,
        lowFreqHz = 240f,
        highFreqHz = 22000f,
        badgeGlyph = "INST"
    )
}

/**
 * DJ Stems Extraction Presets.
 */
enum class StemPreset(
    val title: String,
    val subtitle: String,
    val description: String,
    val vocalGainDb: Float,
    val drumsGainDb: Float,
    val bassGainDb: Float,
    val instGainDb: Float,
    val vocalMute: Boolean,
    val drumsMute: Boolean,
    val bassMute: Boolean,
    val instMute: Boolean
) {
    FullMix(
        title = "Full Studio Mix",
        subtitle = "Unity Balance",
        description = "All 4 stem channels active at standard unity gain (0 dB).",
        vocalGainDb = 0f, drumsGainDb = 0f, bassGainDb = 0f, instGainDb = 0f,
        vocalMute = false, drumsMute = false, bassMute = false, instMute = false
    ),
    AcapellaExtract(
        title = "Acapella Extract",
        subtitle = "Vocals Only",
        description = "Solos and boosts vocal formants while fully muting drums, bass, and backing instruments.",
        vocalGainDb = 2.5f, drumsGainDb = -40f, bassGainDb = -40f, instGainDb = -40f,
        vocalMute = false, drumsMute = true, bassMute = true, instMute = true
    ),
    InstrumentalKaraoke(
        title = "Instrumental Karaoke",
        subtitle = "Vocals Removed",
        description = "Mutes center lead vocals while preserving full punchy drums, bass, and musical instrumentation.",
        vocalGainDb = -40f, drumsGainDb = 0.5f, bassGainDb = 0.5f, instGainDb = 0.5f,
        vocalMute = true, drumsMute = false, bassMute = false, instMute = false
    ),
    DrumAndBass(
        title = "Drum & Bass Drop",
        subtitle = "Rhythm Section Only",
        description = "Isolates the dynamic low-end engine: punchy percussion and sub-bass fundaments.",
        vocalGainDb = -40f, drumsGainDb = 1.5f, bassGainDb = 2.0f, instGainDb = -40f,
        vocalMute = true, drumsMute = false, bassMute = false, instMute = true
    ),
    VocalDuck(
        title = "Vocal Ducking",
        subtitle = "Backing Track",
        description = "Attenuates vocals by -6dB for comfortable singing or podcast voiceover accompaniment.",
        vocalGainDb = -6.0f, drumsGainDb = 0f, bassGainDb = 0f, instGainDb = 0f,
        vocalMute = false, drumsMute = false, bassMute = false, instMute = false
    ),
    BassBoostDrop(
        title = "Sub-Bass Heavy",
        subtitle = "+5dB Bass Punch",
        description = "Drives sub-bass and kick drums for club sound system dynamics.",
        vocalGainDb = -1.0f, drumsGainDb = 1.0f, bassGainDb = 5.0f, instGainDb = 0f,
        vocalMute = false, drumsMute = false, bassMute = false, instMute = false
    )
}

/**
 * Real-time channel strip state for a single audio stem.
 */
data class StemChannelState(
    val stemType: StemType,
    val gainDb: Float = 0.0f, // -40dB to +6dB
    val isMuted: Boolean = false,
    val isSoloed: Boolean = false,
    val pan: Float = 0.0f, // -1.0 (Left) to +1.0 (Right)
    val eqHighDb: Float = 0.0f, // -12dB to +12dB
    val eqMidDb: Float = 0.0f,
    val eqLowDb: Float = 0.0f,
    val filterCutoffNormalized: Float = 0.5f, // 0.0 (Full LPF) <-> 0.5 (Off) <-> 1.0 (Full HPF)
    val resonanceQ: Float = 0.707f
)

/**
 * Master user-configurable settings for the Stems Isolator engine.
 */
data class StemsIsolatorSettings(
    val isEnabled: Boolean = true,
    val channels: Map<StemType, StemChannelState> = createDefaultChannelMap(),
    val masterGainDb: Float = 0.0f,
    val crossfaderPosition: Float = 0.0f, // -1.0 (Deck A: Drums/Bass) <-> 0.0 (Center) <-> +1.0 (Deck B: Vocals/Inst)
    val activePreset: StemPreset = StemPreset.FullMix,
    val enableTransientEnhancer: Boolean = true,
    val enableMidSideDemixing: Boolean = true
) {
    companion object {
        fun createDefaultChannelMap(): Map<StemType, StemChannelState> {
            return StemType.values().associateWith { type ->
                StemChannelState(stemType = type)
            }
        }
    }
}

/**
 * Live audio physics telemetry for a single stem channel.
 */
data class StemChannelTelemetry(
    val stemType: StemType,
    val rmsDb: Float = -24f,
    val peakDb: Float = -18f,
    val meterNormalized: Float = 0.45f, // 0.0 to 1.0 for LED ladder meters
    val transientActivity: Float = 0.35f,
    val spectralCentroidHz: Float = 1200f
)

/**
 * Aggregated telemetry stream emitted by the Stems Isolator engine.
 */
data class StemsIsolatorTelemetry(
    val channelTelemetry: Map<StemType, StemChannelTelemetry> = createDefaultTelemetryMap(),
    val masterRmsDb: Float = -14f,
    val masterPeakDb: Float = -10f,
    val masterMeterNormalized: Float = 0.55f,
    val activeSoloCount: Int = 0,
    val crossfaderDeckAGain: Float = 1.0f,
    val crossfaderDeckBGain: Float = 1.0f
) {
    companion object {
        fun createDefaultTelemetryMap(): Map<StemType, StemChannelTelemetry> {
            return StemType.values().associateWith { type ->
                StemChannelTelemetry(
                    stemType = type,
                    rmsDb = -20f,
                    peakDb = -14f,
                    meterNormalized = 0.50f,
                    transientActivity = 0.40f,
                    spectralCentroidHz = type.defaultCenterHz
                )
            }
        }
    }
}
