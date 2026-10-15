package com.pixelody.app.data.model

enum class EqualizerPreset(
    val displayName: String,
    val gainsDb: List<Float>
) {
    Flat("Flat", listOf(0f, 0f, 0f, 0f, 0f)),
    Warm("Warm", listOf(2.5f, 1.2f, 0f, 0.6f, -0.6f)),
    Bass("Bass", listOf(4f, 2.4f, -0.8f, 0f, 0f)),
    Bright("Bright", listOf(-1.2f, 0f, 1.2f, 3.2f, 3.8f)),
    Vocal("Vocal", listOf(-1.6f, -0.8f, 3.2f, 2.2f, 0f)),
    Night("Night", listOf(-3.2f, -1.8f, 0.8f, -1.2f, -3f)),
    Focus("Focus", listOf(0f, -1.2f, 1.4f, 2.6f, 1.2f)),
    Custom("Custom", listOf(0f, 0f, 0f, 0f, 0f));

    companion object {
        val bandCentersHz = listOf(60, 230, 910, 3600, 14000)
        val bandLabels = listOf("60", "230", "910", "3.6K", "14K")
        val quickPresets = listOf(Flat, Warm, Bass, Bright, Vocal, Night, Focus)

        fun nextQuickPreset(current: EqualizerPreset): EqualizerPreset {
            val index = quickPresets.indexOf(current).takeIf { it >= 0 } ?: 0
            return quickPresets[(index + 1) % quickPresets.size]
        }
    }
}

data class EqualizerProfile(
    val enabled: Boolean = true,
    val preset: EqualizerPreset = EqualizerPreset.Flat,
    val gainsDb: List<Float> = preset.gainsDb
) {
    fun normalized(): EqualizerProfile {
        val padded = (gainsDb + EqualizerPreset.Flat.gainsDb)
            .take(EqualizerPreset.bandCentersHz.size)
            .map { it.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB) }
        return copy(gainsDb = padded)
    }

    fun withPreset(nextPreset: EqualizerPreset): EqualizerProfile =
        copy(enabled = true, preset = nextPreset, gainsDb = nextPreset.gainsDb).normalized()

    fun withBandGain(index: Int, gainDb: Float): EqualizerProfile {
        val next = normalized().gainsDb.toMutableList()
        if (index in next.indices) {
            next[index] = gainDb.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
        }
        return copy(enabled = true, preset = EqualizerPreset.Custom, gainsDb = next).normalized()
    }

    companion object {
        const val MIN_GAIN_DB = -12f
        const val MAX_GAIN_DB = 12f
    }
}

/**
 * Studio Mastering Preset definitions modeling real-world analog signal chains.
 */
enum class MasteringPreset(
    val displayName: String,
    val description: String,
    val gainsDb: List<Float>,
    val qFactors: List<Float>,
    val tubeDrive: Float,
    val tapeWarmth: Float,
    val spatialWidth: Float,
    val subBassBoostDb: Float,
    val limiterThresholdDb: Float
) {
    AudiophileReference(
        displayName = "Reference",
        description = "Transparent high-fidelity transfer with linear phase response",
        gainsDb = listOf(0f, 0f, 0f, 0f, 0f),
        qFactors = listOf(1.0f, 1.4f, 1.4f, 1.4f, 1.0f),
        tubeDrive = 0.0f,
        tapeWarmth = 0.0f,
        spatialWidth = 1.0f,
        subBassBoostDb = 0.0f,
        limiterThresholdDb = -0.5f
    ),
    AnalogTapeWarmth(
        displayName = "Tape Warmth",
        description = "Subtle 2nd-order harmonics with soft high-frequency tape rolloff",
        gainsDb = listOf(2.0f, 1.0f, 0.0f, 0.5f, -1.0f),
        qFactors = listOf(0.8f, 1.2f, 1.4f, 1.2f, 0.8f),
        tubeDrive = 0.25f,
        tapeWarmth = 0.45f,
        spatialWidth = 1.15f,
        subBassBoostDb = 1.5f,
        limiterThresholdDb = -0.8f
    ),
    TubeVibeStudio(
        displayName = "Tube Vibe",
        description = "Rich triode valve saturation with harmonic density & presence",
        gainsDb = listOf(2.5f, 1.5f, -0.5f, 1.8f, 1.0f),
        qFactors = listOf(1.0f, 1.4f, 1.5f, 1.4f, 1.1f),
        tubeDrive = 0.55f,
        tapeWarmth = 0.30f,
        spatialWidth = 1.25f,
        subBassBoostDb = 2.0f,
        limiterThresholdDb = -1.0f
    ),
    ClubSoundstage(
        displayName = "Club Stage",
        description = "Sub-harmonic bass synthesis with widened stereo field immersion",
        gainsDb = listOf(4.5f, 2.0f, -1.0f, 1.5f, 3.0f),
        qFactors = listOf(1.2f, 1.5f, 1.4f, 1.3f, 1.2f),
        tubeDrive = 0.35f,
        tapeWarmth = 0.20f,
        spatialWidth = 1.45f,
        subBassBoostDb = 4.0f,
        limiterThresholdDb = -1.2f
    ),
    BinauralHorizon(
        displayName = "Binaural 3D",
        description = "Psychoacoustic HRTF crossfeed with 200% expanded stereo horizon",
        gainsDb = listOf(0.5f, -0.5f, 1.0f, 2.0f, 3.5f),
        qFactors = listOf(1.0f, 1.3f, 1.4f, 1.4f, 1.0f),
        tubeDrive = 0.15f,
        tapeWarmth = 0.10f,
        spatialWidth = 1.85f,
        subBassBoostDb = 1.0f,
        limiterThresholdDb = -0.6f
    ),
    VocalClarity(
        displayName = "Vocal Clarity",
        description = "Mid-forward presence curve with transparent sibilance control",
        gainsDb = listOf(-1.5f, -0.8f, 3.5f, 2.8f, 0.5f),
        qFactors = listOf(0.9f, 1.2f, 1.8f, 1.6f, 1.0f),
        tubeDrive = 0.20f,
        tapeWarmth = 0.15f,
        spatialWidth = 1.10f,
        subBassBoostDb = 0.0f,
        limiterThresholdDb = -0.5f
    ),
    NightListening(
        displayName = "Night Studio",
        description = "Soft-knee dynamic containment for fatigue-free late-night listening",
        gainsDb = listOf(-3.0f, -1.5f, 1.0f, -1.0f, -2.5f),
        qFactors = listOf(0.8f, 1.0f, 1.2f, 1.0f, 0.8f),
        tubeDrive = 0.10f,
        tapeWarmth = 0.20f,
        spatialWidth = 0.95f,
        subBassBoostDb = 0.0f,
        limiterThresholdDb = -3.0f
    ),
    Custom(
        displayName = "Custom",
        description = "Custom calibrated studio mastering parameters",
        gainsDb = listOf(0f, 0f, 0f, 0f, 0f),
        qFactors = listOf(1.0f, 1.4f, 1.4f, 1.4f, 1.0f),
        tubeDrive = 0.0f,
        tapeWarmth = 0.0f,
        spatialWidth = 1.0f,
        subBassBoostDb = 0.0f,
        limiterThresholdDb = -0.5f
    );

    companion object {
        val quickPresets = listOf(
            AudiophileReference,
            AnalogTapeWarmth,
            TubeVibeStudio,
            ClubSoundstage,
            BinauralHorizon,
            VocalClarity,
            NightListening
        )
    }
}

/**
 * Full studio mastering profile combining 5-band parametric EQ (with resonant Q),
 * analog tube/tape saturation drive, stereo horizon width, sub-bass synthesis, and peak limiter.
 */
data class MasteringProfile(
    val enabled: Boolean = true,
    val preset: MasteringPreset = MasteringPreset.AudiophileReference,
    val eqGainsDb: List<Float> = preset.gainsDb,
    val eqQFactors: List<Float> = preset.qFactors,
    val tubeDrive: Float = preset.tubeDrive, // 0.0 .. 1.0
    val tapeWarmth: Float = preset.tapeWarmth, // 0.0 .. 1.0
    val spatialWidth: Float = preset.spatialWidth, // 0.0 (mono) .. 1.0 (stereo) .. 2.0 (super-wide)
    val subBassBoostDb: Float = preset.subBassBoostDb, // 0.0 .. 6.0 dB
    val limiterThresholdDb: Float = preset.limiterThresholdDb, // -6.0 .. 0.0 dB
    val autoGainEnabled: Boolean = true
) {
    fun normalized(): MasteringProfile {
        val paddedGains = (eqGainsDb + MasteringPreset.AudiophileReference.gainsDb)
            .take(EqualizerPreset.bandCentersHz.size)
            .map { it.coerceIn(EqualizerProfile.MIN_GAIN_DB, EqualizerProfile.MAX_GAIN_DB) }

        val paddedQs = (eqQFactors + MasteringPreset.AudiophileReference.qFactors)
            .take(EqualizerPreset.bandCentersHz.size)
            .map { it.coerceIn(MIN_Q, MAX_Q) }

        return copy(
            eqGainsDb = paddedGains,
            eqQFactors = paddedQs,
            tubeDrive = tubeDrive.coerceIn(0.0f, 1.0f),
            tapeWarmth = tapeWarmth.coerceIn(0.0f, 1.0f),
            spatialWidth = spatialWidth.coerceIn(MIN_SPATIAL_WIDTH, MAX_SPATIAL_WIDTH),
            subBassBoostDb = subBassBoostDb.coerceIn(0.0f, MAX_SUB_BASS_DB),
            limiterThresholdDb = limiterThresholdDb.coerceIn(MIN_LIMITER_THRESHOLD_DB, 0.0f)
        )
    }

    fun withPreset(nextPreset: MasteringPreset): MasteringProfile =
        copy(
            enabled = true,
            preset = nextPreset,
            eqGainsDb = nextPreset.gainsDb,
            eqQFactors = nextPreset.qFactors,
            tubeDrive = nextPreset.tubeDrive,
            tapeWarmth = nextPreset.tapeWarmth,
            spatialWidth = nextPreset.spatialWidth,
            subBassBoostDb = nextPreset.subBassBoostDb,
            limiterThresholdDb = nextPreset.limiterThresholdDb
        ).normalized()

    fun withBandGain(index: Int, gainDb: Float): MasteringProfile {
        val next = normalized().eqGainsDb.toMutableList()
        if (index in next.indices) {
            next[index] = gainDb.coerceIn(EqualizerProfile.MIN_GAIN_DB, EqualizerProfile.MAX_GAIN_DB)
        }
        return copy(enabled = true, preset = MasteringPreset.Custom, eqGainsDb = next).normalized()
    }

    fun withBandQ(index: Int, q: Float): MasteringProfile {
        val next = normalized().eqQFactors.toMutableList()
        if (index in next.indices) {
            next[index] = q.coerceIn(MIN_Q, MAX_Q)
        }
        return copy(enabled = true, preset = MasteringPreset.Custom, eqQFactors = next).normalized()
    }

    fun withTubeDrive(drive: Float): MasteringProfile =
        copy(enabled = true, preset = MasteringPreset.Custom, tubeDrive = drive.coerceIn(0f, 1f)).normalized()

    fun withTapeWarmth(warmth: Float): MasteringProfile =
        copy(enabled = true, preset = MasteringPreset.Custom, tapeWarmth = warmth.coerceIn(0f, 1f)).normalized()

    fun withSpatialWidth(width: Float): MasteringProfile =
        copy(enabled = true, preset = MasteringPreset.Custom, spatialWidth = width.coerceIn(MIN_SPATIAL_WIDTH, MAX_SPATIAL_WIDTH)).normalized()

    fun withSubBassBoost(boostDb: Float): MasteringProfile =
        copy(enabled = true, preset = MasteringPreset.Custom, subBassBoostDb = boostDb.coerceIn(0f, MAX_SUB_BASS_DB)).normalized()

    fun withLimiterThreshold(thresholdDb: Float): MasteringProfile =
        copy(enabled = true, preset = MasteringPreset.Custom, limiterThresholdDb = thresholdDb.coerceIn(MIN_LIMITER_THRESHOLD_DB, 0f)).normalized()

    fun toEqualizerProfile(): EqualizerProfile = EqualizerProfile(
        enabled = enabled,
        preset = EqualizerPreset.Custom,
        gainsDb = eqGainsDb
    )

    companion object {
        const val MIN_Q = 0.5f
        const val MAX_Q = 5.0f
        const val MIN_SPATIAL_WIDTH = 0.0f
        const val MAX_SPATIAL_WIDTH = 2.0f
        const val MAX_SUB_BASS_DB = 6.0f
        const val MIN_LIMITER_THRESHOLD_DB = -6.0f
    }
}
