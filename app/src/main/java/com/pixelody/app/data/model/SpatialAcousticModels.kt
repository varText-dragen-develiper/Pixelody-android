package com.pixelody.app.data.model

/**
 * Acoustic Chamber Presets representing authentic physical acoustic environments.
 */
enum class AcousticChamberPreset(
    val title: String,
    val description: String,
    val defaultVolumeM3: Float,
    val defaultRt60Seconds: Float,
    val defaultDamping: WallMaterialDamping,
    val defaultSpeakerAngleDeg: Float,
    val badgeGlyph: String,
    val accentColorHex: Long
) {
    AbbeyStudioControlRoom(
        title = "Abbey Control Room",
        description = "World-class mastering acoustics with calibrated 60° nearfield monitors and precise early reflections.",
        defaultVolumeM3 = 120f,
        defaultRt60Seconds = 0.45f,
        defaultDamping = WallMaterialDamping.PorousAcousticFoam,
        defaultSpeakerAngleDeg = 30f,
        badgeGlyph = "CTRL",
        accentColorHex = 0xFFE5A93C // Gold
    ),
    TokyoVinylBar(
        title = "Tokyo Vinyl Bar",
        description = "Intimate wooden listening salon with warm analog acoustic resonance and cozy low-end absorption.",
        defaultVolumeM3 = 45f,
        defaultRt60Seconds = 0.65f,
        defaultDamping = WallMaterialDamping.TeakWood,
        defaultSpeakerAngleDeg = 35f,
        badgeGlyph = "VINYL",
        accentColorHex = 0xFFF59E0B // Warm Amber
    ),
    CathedralOfEchoes(
        title = "Cathedral of Echoes",
        description = "Monumental stone cathedral with soaring arched reflections, wide 90° stereo field, and majestic 3.6s tail.",
        defaultVolumeM3 = 3500f,
        defaultRt60Seconds = 3.60f,
        defaultDamping = WallMaterialDamping.ConcreteStone,
        defaultSpeakerAngleDeg = 45f,
        badgeGlyph = "CATH",
        accentColorHex = 0xFF8B5CF6 // Royal Purple
    ),
    MinimalistTeahouse(
        title = "Minimalist Teahouse",
        description = "Ultra-quiet tatami and paper screen chamber with high acoustic damping for razor-sharp vocal intimacy.",
        defaultVolumeM3 = 28f,
        defaultRt60Seconds = 0.22f,
        defaultDamping = WallMaterialDamping.VelvetCurtain,
        defaultSpeakerAngleDeg = 22.5f,
        badgeGlyph = "TEA",
        accentColorHex = 0xFF10B981 // Emerald
    ),
    CyberpunkAlleyway(
        title = "Cyberpunk Alleyway",
        description = "Atmospheric metallic and wet asphalt reflections with wide spatial stereo bounce and holographic synthetic reverb.",
        defaultVolumeM3 = 850f,
        defaultRt60Seconds = 1.80f,
        defaultDamping = WallMaterialDamping.BrushedAluminum,
        defaultSpeakerAngleDeg = 42.5f,
        badgeGlyph = "CYBER",
        accentColorHex = 0xFF38BDF8 // Sky Blue
    ),
    CustomStudio(
        title = "Custom Acoustic Rig",
        description = "Fully personalized virtual chamber with user-defined geometry, absorption, and HRTF crossfeed.",
        defaultVolumeM3 = 90f,
        defaultRt60Seconds = 0.50f,
        defaultDamping = WallMaterialDamping.TeakWood,
        defaultSpeakerAngleDeg = 30f,
        badgeGlyph = "RIG",
        accentColorHex = 0xFFEC4899 // Pink
    )
}

/**
 * Wall Material Damping determining acoustic reflection absorption spectra.
 */
enum class WallMaterialDamping(
    val title: String,
    val highFrequencyAbsorption: Float,
    val midFrequencyAbsorption: Float,
    val acousticWarmthFactor: Float
) {
    TeakWood(
        title = "Teak Hardwood",
        highFrequencyAbsorption = 0.35f,
        midFrequencyAbsorption = 0.20f,
        acousticWarmthFactor = 1.25f
    ),
    VelvetCurtain(
        title = "Heavy Velvet",
        highFrequencyAbsorption = 0.78f,
        midFrequencyAbsorption = 0.45f,
        acousticWarmthFactor = 0.90f
    ),
    BrushedAluminum(
        title = "Brushed Aluminum",
        highFrequencyAbsorption = 0.12f,
        midFrequencyAbsorption = 0.08f,
        acousticWarmthFactor = 0.70f
    ),
    ConcreteStone(
        title = "Concrete & Stone",
        highFrequencyAbsorption = 0.15f,
        midFrequencyAbsorption = 0.10f,
        acousticWarmthFactor = 0.80f
    ),
    PorousAcousticFoam(
        title = "Studio Acoustic Foam",
        highFrequencyAbsorption = 0.88f,
        midFrequencyAbsorption = 0.70f,
        acousticWarmthFactor = 1.05f
    )
}

/**
 * Binaural Crossfeed mode determining Interaural Time & Level Difference matrix.
 */
enum class BinauralCrossfeedMode(
    val title: String,
    val description: String,
    val baseAngleDeg: Float,
    val baseItdMicroseconds: Float,
    val lowPassCutoffHz: Float
) {
    NaturalNearfield(
        title = "Natural 60° Nearfield",
        description = "Standard equilateral studio monitor triangle with natural 250μs acoustic bleed for zero ear fatigue.",
        baseAngleDeg = 30f,
        baseItdMicroseconds = 250f,
        lowPassCutoffHz = 700f
    ),
    WideAngleMaster(
        title = "Wide 90° Soundstage",
        description = "Expansive acoustic arc with deeper 650μs ITD delay and prominent head-shadow attenuation.",
        baseAngleDeg = 45f,
        baseItdMicroseconds = 650f,
        lowPassCutoffHz = 600f
    ),
    IntimateHeadstage(
        title = "Intimate 45° Focus",
        description = "Focused center imaging with gentle 120μs crossfeed for vocal tracks and acoustic jazz recordings.",
        baseAngleDeg = 22.5f,
        baseItdMicroseconds = 120f,
        lowPassCutoffHz = 850f
    ),
    DirectStereoOff(
        title = "Direct Stereo (Bypassed)",
        description = "Standard isolated L/R headphone output with zero acoustic crossfeed.",
        baseAngleDeg = 90f,
        baseItdMicroseconds = 0f,
        lowPassCutoffHz = 20000f
    )
}

/**
 * Spatial Virtual Speaker Coordinates in the 3D room.
 */
data class SpatialSpeakerPosition(
    val leftAngleDeg: Float = -30f,
    val rightAngleDeg: Float = 30f,
    val distanceMeters: Float = 1.8f,
    val elevationDeg: Float = 0f
)

/**
 * Complete user settings for the Spatial Acoustic Chamber.
 */
data class SpatialChamberSettings(
    val isEnabled: Boolean = true,
    val preset: AcousticChamberPreset = AcousticChamberPreset.AbbeyStudioControlRoom,
    val crossfeedMode: BinauralCrossfeedMode = BinauralCrossfeedMode.NaturalNearfield,
    val speakerPosition: SpatialSpeakerPosition = SpatialSpeakerPosition(),
    val listenerHeadYawDeg: Float = 0f,
    val roomVolumeM3: Float = 120f,
    val wallDamping: WallMaterialDamping = WallMaterialDamping.PorousAcousticFoam,
    val reverbDecaySeconds: Float = 0.45f,
    val earlyReflectionGain: Float = 0.35f,
    val diffuseTailGain: Float = 0.25f,
    val dryWetMix: Float = 0.75f,
    val headphoneProfileCompensation: Boolean = true
)

/**
 * Real-time computed acoustic physics telemetry emitted by the Spatial Chamber DSP.
 */
data class SpatialAcousticTelemetry(
    val itdMicroseconds: Float = 250f,
    val ildDb: Float = 3.2f,
    val calculatedRt60Seconds: Float = 0.45f,
    val activeRayCount: Int = 12,
    val spatialWidthScore: Float = 0.78f,
    val leftEarCrossbleedGain: Float = 0.22f,
    val rightEarCrossbleedGain: Float = 0.22f,
    val roomSurfaceAreaM2: Float = 145f
)
