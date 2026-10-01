package com.pixelody.app.data.model

/**
 * Tape Formulation characteristics defining magnetic hysteresis, saturation ceiling,
 * frequency curve, and noise floor.
 */
enum class TapeFormulation(
    val title: String,
    val subtitle: String,
    val description: String,
    val saturationCeiling: Float,
    val defaultDriveGain: Float,
    val highFreqRolloffHz: Float,
    val lowEndBumpDb: Float,
    val noiseFloorDb: Float,
    val harmonicColorHex: Long
) {
    TypeI_Ferric(
        title = "Type I Normal",
        subtitle = "Ferric (Fe2O3)",
        description = "Standard gamma-Fe2O3 magnetic formula with vintage tape hiss, punchy low-mids, and smooth early saturation.",
        saturationCeiling = 0.85f,
        defaultDriveGain = 1.25f,
        highFreqRolloffHz = 14000f,
        lowEndBumpDb = 1.8f,
        noiseFloorDb = -52f,
        harmonicColorHex = 0xFFD97706 // Warm Amber
    ),
    TypeII_Chrome(
        title = "Type II High Bias",
        subtitle = "Chrome (CrO2)",
        description = "Chromium dioxide coating featuring crisper transient treble response, higher headroom, and quieter baseline hiss.",
        saturationCeiling = 0.95f,
        defaultDriveGain = 1.10f,
        highFreqRolloffHz = 17500f,
        lowEndBumpDb = 0.9f,
        noiseFloorDb = -62f,
        harmonicColorHex = 0xFF3B82F6 // Studio Blue
    ),
    TypeIV_Metal(
        title = "Type IV Metal",
        subtitle = "Pure Metal Particle",
        description = "Pure metallic particulate suspension delivering maximum dynamic range, razor-sharp transients, and pristine highs.",
        saturationCeiling = 1.18f,
        defaultDriveGain = 1.00f,
        highFreqRolloffHz = 21000f,
        lowEndBumpDb = 0.4f,
        noiseFloorDb = -72f,
        harmonicColorHex = 0xFF10B981 // Emerald Green
    ),
    ReelToReel_15ips(
        title = "Studio Master 15 IPS",
        subtitle = "1/4\" Open Reel",
        description = "High-speed 15 inches/sec open reel transport with legendary 40Hz magnetic head-bump and silky analog glue.",
        saturationCeiling = 1.28f,
        defaultDriveGain = 0.95f,
        highFreqRolloffHz = 22500f,
        lowEndBumpDb = 2.5f,
        noiseFloorDb = -78f,
        harmonicColorHex = 0xFF8B5CF6 // Royal Violet
    ),
    ReelToReel_30ips(
        title = "Mastering Deck 30 IPS",
        subtitle = "1/2\" Mastering Reel",
        description = "Ultra-high-speed 30 inches/sec mastering tape with near-flat frequency linearity, lightning transient speed, and vanishing hiss.",
        saturationCeiling = 1.42f,
        defaultDriveGain = 0.90f,
        highFreqRolloffHz = 25000f,
        lowEndBumpDb = 0.8f,
        noiseFloorDb = -84f,
        harmonicColorHex = 0xFFF59E0B // Mastering Gold
    ),
    DegradedLoFi(
        title = "Degraded Walkman",
        subtitle = "Worn Thrift-Store Tape",
        description = "Heavily played vintage cassette with pronounced motor flutter, magnetic azimuth skew, and thick warm distortion.",
        saturationCeiling = 0.68f,
        defaultDriveGain = 1.55f,
        highFreqRolloffHz = 8500f,
        lowEndBumpDb = 3.4f,
        noiseFloorDb = -40f,
        harmonicColorHex = 0xFFEF4444 // Distorted Red
    )
}

/**
 * Noise Reduction Companding systems.
 */
enum class TapeNoiseReduction(
    val title: String,
    val badgeText: String,
    val hissAttenuationDb: Float,
    val dynamicExpansionRatio: Float
) {
    Off(
        title = "Noise Reduction Off",
        badgeText = "NR OFF",
        hissAttenuationDb = 0f,
        dynamicExpansionRatio = 1.0f
    ),
    DolbyB(
        title = "Dolby-B NR",
        badgeText = "DOLBY B",
        hissAttenuationDb = 10f,
        dynamicExpansionRatio = 1.35f
    ),
    DolbyC(
        title = "Dolby-C NR",
        badgeText = "DOLBY C",
        hissAttenuationDb = 20f,
        dynamicExpansionRatio = 1.85f
    ),
    dbx_TypeII(
        title = "dbx Type II",
        badgeText = "dbx-II",
        hissAttenuationDb = 30f,
        dynamicExpansionRatio = 2.0f
    )
}

/**
 * Visual aesthetics and shell casing themes for the animated cassette deck.
 */
enum class CassetteShellTheme(
    val title: String,
    val bodyColorHex: Long,
    val labelColorHex: Long,
    val spoolHubColorHex: Long,
    val accentColorHex: Long,
    val windowBorderColorHex: Long
) {
    SmokedAcrylic(
        title = "Smoked Acrylic",
        bodyColorHex = 0xFF1E293B,
        labelColorHex = 0xFF334155,
        spoolHubColorHex = 0xFFF1F5F9,
        accentColorHex = 0xFF38BDF8,
        windowBorderColorHex = 0xFF64748B
    ),
    VintageCream(
        title = "Vintage Cream 1982",
        bodyColorHex = 0xFFFDFBF7,
        labelColorHex = 0xFFE2E8F0,
        spoolHubColorHex = 0xFFD97706,
        accentColorHex = 0xFFB45309,
        windowBorderColorHex = 0xFFCBD5E1
    ),
    NeonVaporwave(
        title = "Neon Vaporwave",
        bodyColorHex = 0xFF18182E,
        labelColorHex = 0xFF2D1B4E,
        spoolHubColorHex = 0xFFEC4899,
        accentColorHex = 0xFF06B6D4,
        windowBorderColorHex = 0xFFA855F7
    ),
    StudioMasterReel(
        title = "Studio Master Deck",
        bodyColorHex = 0xFF0F172A,
        labelColorHex = 0xFF1E293B,
        spoolHubColorHex = 0xFFF59E0B,
        accentColorHex = 0xFF10B981,
        windowBorderColorHex = 0xFF475569
    ),
    ObsidianBlack(
        title = "Obsidian Stealth",
        bodyColorHex = 0xFF090A0F,
        labelColorHex = 0xFF181920,
        spoolHubColorHex = 0xFF94A3B8,
        accentColorHex = 0xFFE11D48,
        windowBorderColorHex = 0xFF27272A
    )
}

/**
 * Physical transport state for the mechanical tape deck.
 */
enum class TapeTransportState(
    val title: String,
    val motorSpeedMultiplier: Float,
    val headEngaged: Boolean
) {
    Stopped(title = "STOP", motorSpeedMultiplier = 0f, headEngaged = false),
    Playing(title = "PLAY", motorSpeedMultiplier = 1f, headEngaged = true),
    FastForwarding(title = "FFWD", motorSpeedMultiplier = 4.5f, headEngaged = false),
    Rewinding(title = "RWD", motorSpeedMultiplier = -4.5f, headEngaged = false),
    Paused(title = "PAUSE", motorSpeedMultiplier = 0f, headEngaged = true),
    Ejected(title = "EJECT", motorSpeedMultiplier = 0f, headEngaged = false)
}

/**
 * Real-time dynamic spool physics modeling conservation of tape volume.
 * As supply reel unspools, its radius decreases while take-up spool radius increases.
 */
data class TapeSpoolPhysics(
    val supplyRadiusRatio: Float = 0.85f, // 0.0 (empty hub) to 1.0 (full tape pack)
    val takeupRadiusRatio: Float = 0.25f, // 0.0 (empty hub) to 1.0 (full tape pack)
    val supplyRpm: Float = 45f,
    val takeupRpm: Float = 115f,
    val tapeTensionGrams: Float = 35f,
    val headContactPressure: Float = 0.95f
)

/**
 * User-configurable settings for the Cassette & Magnetics engine.
 */
data class CassetteTapeSettings(
    val isEnabled: Boolean = true,
    val formulation: TapeFormulation = TapeFormulation.TypeII_Chrome,
    val noiseReduction: TapeNoiseReduction = TapeNoiseReduction.DolbyB,
    val shellTheme: CassetteShellTheme = CassetteShellTheme.SmokedAcrylic,
    val driveGain: Float = 1.10f, // 0.2f to 3.0f
    val wowFlutterIntensity: Float = 0.35f, // 0.0f to 1.0f
    val azimuthSkewMs: Float = 0.06f, // -0.4ms to +0.4ms
    val tapeWear: Float = 0.12f, // 0.0f to 1.0f
    val hissVolumeGain: Float = 0.30f, // 0.0f to 1.0f
    val enableStereoWidener: Boolean = true,
    val autoAzimuthCorrection: Boolean = false
)

/**
 * Real-time computed magnetic telemetry emitted by the Cassette DSP engine.
 */
data class TapeMagneticsTelemetry(
    val leftInDb: Float = -12f,
    val rightInDb: Float = -12f,
    val leftOutDb: Float = -11.5f,
    val rightOutDb: Float = -11.5f,
    val leftVuNeedleNormalized: Float = 0.55f, // 0.0 to 1.0+ with overshoot ballistics
    val rightVuNeedleNormalized: Float = 0.55f,
    val saturationThdPercent: Float = 0.85f,
    val wowModulationMs: Float = 0.02f,
    val flutterModulationMs: Float = 0.005f,
    val spoolPhysics: TapeSpoolPhysics = TapeSpoolPhysics(),
    val transportState: TapeTransportState = TapeTransportState.Playing,
    val effectiveSnrDb: Float = 68f
)
