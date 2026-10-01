package com.pixelody.app.data.model

/**
 * Display modes for the vector laser and CRT oscilloscope synth.
 */
enum class OscilloscopeDisplayMode(
    val title: String,
    val subtitle: String,
    val description: String,
    val badgeGlyph: String
) {
    Lissajous_XY(
        title = "Lissajous Phase Orbit",
        subtitle = "X=Left, Y=Right Direct",
        description = "Direct 2D phase plane plotting revealing harmonic frequency ratios and stereo channel correlation.",
        badgeGlyph = "LISS"
    ),
    CircularGoniometer(
        title = "Circular Goniometer",
        subtitle = "45° Soundstage Spread",
        description = "Rotated 45° polar stereo field displaying exact stereo width, balance, and center vs side energy.",
        badgeGlyph = "GONI"
    ),
    StereoWaveform_Dual(
        title = "Dual Trace Waveform",
        subtitle = "Time-Domain Calibrated",
        description = "Classic dual-beam oscilloscope sweep displaying Left (top) and Right (bottom) analog voltages over time.",
        badgeGlyph = "DUAL"
    ),
    VectorLaserSynth_3D(
        title = "3D Vector Laser Ribbon",
        subtitle = "Rotatable Spatial Mesh",
        description = "Interactive 3D vector ribbon projected in real-time Euler space with audio energy-driven orbital pitch and yaw.",
        badgeGlyph = "3D"
    ),
    CRT_VectorSpirals(
        title = "Logarithmic Spiral Flower",
        subtitle = "Harmonic Polar Spiral",
        description = "Complex geometric spiral transformation wrapping audio waveforms into mesmerizing sacred vector flowers.",
        badgeGlyph = "SPIR"
    ),
    AudioMatrixPolar(
        title = "Matrix Polar Radar",
        subtitle = "Radial Sweep Goniometer",
        description = "Circular radar goniometer sweeping radially with instantaneous RMS envelope modulation.",
        badgeGlyph = "RADR"
    )
}

/**
 * Authentic CRT Phosphor chemical compositions and visual aesthetics.
 */
enum class CrtPhosphorType(
    val title: String,
    val compositionName: String,
    val primaryColorHex: Long,
    val bloomGlowColorHex: Long,
    val trailColorHex: Long,
    val defaultDecayMs: Float
) {
    Green_P1(
        title = "Classic P1 Green",
        compositionName = "Zinc Silicate (Zn2SiO4:Mn)",
        primaryColorHex = 0xFF22C55E, // Bright Phosphor Green
        bloomGlowColorHex = 0xFF86EFAC,
        trailColorHex = 0xFF14532D,
        defaultDecayMs = 180f
    ),
    Amber_P3(
        title = "Vintage P3 Amber",
        compositionName = "Zinc Sulfide (ZnS:Mn)",
        primaryColorHex = 0xFFF59E0B, // Warm Amber
        bloomGlowColorHex = 0xFFFDE68A,
        trailColorHex = 0xFF78350F,
        defaultDecayMs = 240f
    ),
    Cyan_P4(
        title = "Medical P4 Radar Cyan",
        compositionName = "Zinc Sulfide & Cadmium (ZnS:Ag)",
        primaryColorHex = 0xFF06B6D4, // Electric Cyan
        bloomGlowColorHex = 0xFFA5F3FC,
        trailColorHex = 0xFF164E63,
        defaultDecayMs = 150f
    ),
    WhitePhosphor_P45(
        title = "Studio P45 Monochrome",
        compositionName = "Yttrium Oxysulfide (Y2O2S:Tb)",
        primaryColorHex = 0xFFF8FAFC, // Crisp Monochrome White
        bloomGlowColorHex = 0xFFE2E8F0,
        trailColorHex = 0xFF475569,
        defaultDecayMs = 120f
    ),
    NeonLaserRGB(
        title = "RGB Laser Vector",
        compositionName = "Multi-Diode Laser Projection",
        primaryColorHex = 0xFFEC4899, // Hot Laser Magenta
        bloomGlowColorHex = 0xFFF472B6,
        trailColorHex = 0xFF831843,
        defaultDecayMs = 320f
    ),
    CyberpunkViolet(
        title = "Cyberpunk Ultraviolet",
        compositionName = "Europium Violet Glow",
        primaryColorHex = 0xFFA855F7, // Royal Purple
        bloomGlowColorHex = 0xFFD8B4FE,
        trailColorHex = 0xFF581C87,
        defaultDecayMs = 220f
    )
}

/**
 * CRT Beam persistence duration.
 */
enum class CrtBeamPersistence(
    val title: String,
    val decayTimeConstantMs: Float,
    val trailAlphaFactor: Float
) {
    Fast_AudioTransient(title = "Transient (50ms)", decayTimeConstantMs = 50f, trailAlphaFactor = 0.35f),
    Medium_StandardCRT(title = "Standard CRT (180ms)", decayTimeConstantMs = 180f, trailAlphaFactor = 0.65f),
    Long_RadarGlow(title = "Long Glow (450ms)", decayTimeConstantMs = 450f, trailAlphaFactor = 0.85f),
    UltraLong_LaserTrace(title = "Laser Trace (1.2s)", decayTimeConstantMs = 1200f, trailAlphaFactor = 0.95f)
}

/**
 * 3D point in vector space with intensity and color metadata.
 */
data class VectorPoint3D(
    val x: Float,
    val y: Float,
    val z: Float = 0f,
    val intensity: Float = 1.0f
)

/**
 * Geometric and electron optical settings for beam rendering.
 */
data class BeamGeometrySettings(
    val beamThicknessPx: Float = 2.0f,
    val beamBrightness: Float = 1.0f,
    val bloomIntensity: Float = 0.65f,
    val scanlineIntensity: Float = 0.25f,
    val graticuleDivisions: Int = 8,
    val audioPhaseRotationDeg: Float = 0.0f,
    val zModulationDepth: Float = 0.5f,
    val eulerPitchDeg: Float = 25.0f,
    val eulerYawDeg: Float = 35.0f,
    val eulerRollDeg: Float = 0.0f
)

/**
 * User-configurable settings for the Oscilloscope & Phosphor engine.
 */
data class OscilloscopeSettings(
    val isEnabled: Boolean = true,
    val displayMode: OscilloscopeDisplayMode = OscilloscopeDisplayMode.Lissajous_XY,
    val phosphorType: CrtPhosphorType = CrtPhosphorType.Green_P1,
    val persistence: CrtBeamPersistence = CrtBeamPersistence.Medium_StandardCRT,
    val beamGeometry: BeamGeometrySettings = BeamGeometrySettings(),
    val sweepSpeed: Float = 1.0f, // 0.2f to 3.0f
    val sensitivityGain: Float = 1.25f, // 0.2f to 4.0f
    val enableGraticuleOverlay: Boolean = true,
    val enableCrtCurvature: Boolean = true,
    val enablePhosphorBloom: Boolean = true
)

/**
 * Real-time stereoscopic phase and beam deflection telemetry emitted by the engine.
 */
data class OscilloscopeTelemetry(
    val phaseCoherenceScore: Float = 0.82f, // +1.0 (Mono) to -1.0 (Anti-phase)
    val peakBeamDeflection: Float = 0.75f, // 0.0 to 1.0+
    val stereoscopicPhaseSpread: Float = 0.45f,
    val beamIntensityRms: Float = 0.65f,
    val activePointCount: Int = 512,
    val lissajousEccentricity: Float = 0.35f,
    val centerEnergyRatio: Float = 0.78f,
    val sideEnergyRatio: Float = 0.22f
)
