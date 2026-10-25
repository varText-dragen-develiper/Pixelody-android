package com.pixelody.app.data.model

/**
 * Known source metadata. Zero means unknown for integer fields; NaN means
 * unmeasured for float fields. isBitPerfect is false until actually verified.
 */
data class AudioBitstreamVerification(
    val isBitPerfect: Boolean = false,
    val bitDepth: Int = 0,
    val sampleRateHz: Int = 0,
    val codec: String = "Unknown format",
    val resamplingRatio: Float = Float.NaN,
    val outputSink: String = "Output unknown",
    val dynamicRangeDb: Float = Float.NaN,
    val thdPlusNPercent: Float = Float.NaN
) {
    val bitstreamLabel: String
        get() = listOfNotNull(
            bitDepth.takeIf { it > 0 }?.let { "$it-bit" },
            sampleRateHz.takeIf { it > 0 }?.let { "${it / 1000.0}kHz" }
        ).joinToString(" / ").let { if (it.isBlank()) codec else "$it $codec" }
}

/**
 * High-precision 10-band acoustic target mastering profile.
 */
enum class AcousticTargetPreset(
    val title: String,
    val subtitle: String,
    val tag: String,
    val bandsDb: List<Float>, // 32Hz, 64Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz
    val limiterCeilingDb: Float = -0.3f,
    val stereoWidthFactor: Float = 1.0f,
    val colorHex: Long = 0xFF4ADE80
) {
    StudioFlatReference(
        title = "Studio Flat Reference",
        subtitle = "Flat tuning profile",
        tag = "FLAT",
        bandsDb = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        limiterCeilingDb = -0.1f,
        stereoWidthFactor = 1.0f,
        colorHex = 0xFF4ADE80 // Green
    ),
    IemHarmonicWarmth(
        title = "IEM Harmonic Warmth",
        subtitle = "A target profile for in-ear and sealed headphones",
        tag = "IEM WARM",
        bandsDb = listOf(2.5f, 2.0f, 1.0f, 0.5f, 0f, 0f, 1.0f, 1.5f, 2.0f, 2.5f),
        limiterCeilingDb = -0.3f,
        stereoWidthFactor = 1.1f,
        colorHex = 0xFFF59E0B // Amber
    ),
    OpenBackAiry(
        title = "Open-Back Soundstage",
        subtitle = "A brighter high end and wider stereo image for planar headphones",
        tag = "OPEN AIR",
        bandsDb = listOf(0.5f, 0.5f, 0f, 0f, 0.5f, 0.5f, 1.5f, 2.5f, 3.0f, 3.5f),
        limiterCeilingDb = -0.2f,
        stereoWidthFactor = 1.25f,
        colorHex = 0xFF38BDF8 // Cyan
    ),
    CarAudioPunch(
        title = "Car Acoustic Punch",
        subtitle = "Bass and vocal emphasis for a noisier listening environment",
        tag = "CAR PUNCH",
        bandsDb = listOf(4.0f, 3.5f, 2.0f, 0.5f, 0f, 1.0f, 1.5f, 2.0f, 2.5f, 2.0f),
        limiterCeilingDb = -0.5f,
        stereoWidthFactor = 1.15f,
        colorHex = 0xFFEC4899 // Pink
    ),
    HiResDacDirect(
        title = "Bit-Perfect USB Passthrough",
        subtitle = "Request a direct DAC route where supported; actual output needs verification",
        tag = "BIT-PERFECT",
        bandsDb = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        limiterCeilingDb = 0.0f,
        stereoWidthFactor = 1.0f,
        colorHex = 0xFFA78BFA // Purple
    )
}
