package com.pixelody.app.data.model

/**
 * End-to-end bitstream verification metadata across the Android audio pipeline.
 */
data class AudioBitstreamVerification(
    val isBitPerfect: Boolean = true,
    val bitDepth: Int = 24,
    val sampleRateHz: Int = 192000,
    val codec: String = "FLAC",
    val resamplingRatio: Float = 1.0f, // 1.0 = native direct bit-perfect path
    val outputSink: String = "USB DAC Direct",
    val dynamicRangeDb: Float = 118.5f,
    val thdPlusNPercent: Float = 0.0003f
) {
    val bitstreamLabel: String
        get() = "$bitDepth-bit / ${sampleRateHz / 1000}kHz $codec"
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
        subtitle = "Zero-coloration unadulterated linear frequency response",
        tag = "FLAT",
        bandsDb = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        limiterCeilingDb = -0.1f,
        stereoWidthFactor = 1.0f,
        colorHex = 0xFF4ADE80 // Green
    ),
    IemHarmonicWarmth(
        title = "IEM Harmonic Warmth",
        subtitle = "Harmon-curve target tuned for in-ear monitors & sealed drivers",
        tag = "IEM WARM",
        bandsDb = listOf(2.5f, 2.0f, 1.0f, 0.5f, 0f, 0f, 1.0f, 1.5f, 2.0f, 2.5f),
        limiterCeilingDb = -0.3f,
        stereoWidthFactor = 1.1f,
        colorHex = 0xFFF59E0B // Amber
    ),
    OpenBackAiry(
        title = "Open-Back Soundstage",
        subtitle = "Expanded treble sparkle and dimensional stereo spread for planar headphones",
        tag = "OPEN AIR",
        bandsDb = listOf(0.5f, 0.5f, 0f, 0f, 0.5f, 0.5f, 1.5f, 2.5f, 3.0f, 3.5f),
        limiterCeilingDb = -0.2f,
        stereoWidthFactor = 1.25f,
        colorHex = 0xFF38BDF8 // Cyan
    ),
    CarAudioPunch(
        title = "Car Acoustic Punch",
        subtitle = "Road noise compensation with dynamic low-end punch and vocal clarity",
        tag = "CAR PUNCH",
        bandsDb = listOf(4.0f, 3.5f, 2.0f, 0.5f, 0f, 1.0f, 1.5f, 2.0f, 2.5f, 2.0f),
        limiterCeilingDb = -0.5f,
        stereoWidthFactor = 1.15f,
        colorHex = 0xFFEC4899 // Pink
    ),
    HiResDacDirect(
        title = "Bit-Perfect USB Passthrough",
        subtitle = "Direct integer stream bypass routing directly to external DAC",
        tag = "BIT-PERFECT",
        bandsDb = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        limiterCeilingDb = 0.0f,
        stereoWidthFactor = 1.0f,
        colorHex = 0xFFA78BFA // Purple
    )
}
