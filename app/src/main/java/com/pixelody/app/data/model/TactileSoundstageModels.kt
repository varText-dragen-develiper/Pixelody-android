package com.pixelody.app.data.model

/**
 * Operating mode for the Tactile Soundstage analog performance deck.
 */
enum class TactileDeckMode(val title: String, val subtitle: String, val tag: String) {
    Stems("4-Way Stems Isolator", "Real-time Vocal, Drums, Bass, and Other separation", "STEMS"),
    Turntable("Direct-Drive Turntable", "Analog vinyl slipmat with real-time scratch and motor torque", "VINYL"),
    Cassette("Magnetic Tape Deck", "Type I/II/IV tape formulation with saturation and wow/flutter", "TAPE"),
    PhosphorLab("Dual-Trace Oscilloscope", "CRT phosphor persistence and stereo Lissajous X-Y vectors", "SCOPE"),
    MasterBlend("Unified Master Soundstage", "Simultaneous multi-engine resonance with matrix routing", "BLEND")
}

/**
 * Audio routing target for individual stems through analog DSP processors.
 */
enum class StemAnalogRoute(val displayName: String) {
    DirectClean("Direct Clean"),
    TapeSaturation("Cassette Saturation"),
    VinylWarp("Vinyl Friction & Scratches"),
    PhosphorVector("Phosphor Vector Modulation")
}

/**
 * Routing matrix connecting individual stems to analog emulation engines.
 */
data class SoundstageRoutingMatrix(
    val vocalRoute: StemAnalogRoute = StemAnalogRoute.DirectClean,
    val drumsRoute: StemAnalogRoute = StemAnalogRoute.DirectClean,
    val bassRoute: StemAnalogRoute = StemAnalogRoute.DirectClean,
    val otherRoute: StemAnalogRoute = StemAnalogRoute.DirectClean,
    val analogSaturationDrive: Float = 0.35f,
    val vinylFrictionWear: Float = 0.20f,
    val phosphorResonanceDecay: Float = 0.65f,
    val activeDeckMode: TactileDeckMode = TactileDeckMode.MasterBlend
)

/**
 * Real-time unified DSP telemetry frame across all active tactile engines.
 */
data class TactileSoundstageFrame(
    val vocalsGain: Float = 1.0f,
    val drumsGain: Float = 1.0f,
    val bassGain: Float = 1.0f,
    val otherGain: Float = 1.0f,
    val turntableRpm: Float = 33.33f,
    val turntableScratchOffset: Float = 0f,
    val tapeWowFlutterFactor: Float = 1.0f,
    val tapeSaturationRms: Float = 0f,
    val phosphorLissajousX: Float = 0f,
    val phosphorLissajousY: Float = 0f,
    val activeDeckMode: TactileDeckMode = TactileDeckMode.MasterBlend,
    val isMacroActive: Boolean = false,
    val activeMacroName: String? = null
)
