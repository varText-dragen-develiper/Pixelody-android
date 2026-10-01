package com.pixelody.app.data.model

/**
 * Standard audiophile audio sample rates in Hz.
 */
enum class HiResSampleRate(val sampleRateHz: Int, val displayName: String, val isHiRes: Boolean) {
    Rate_44_1kHz(44100, "44.1 kHz", false),
    Rate_48kHz(48000, "48.0 kHz", false),
    Rate_88_2kHz(88200, "88.2 kHz", true),
    Rate_96kHz(96000, "96.0 kHz", true),
    Rate_176_4kHz(176400, "176.4 kHz", true),
    Rate_192kHz(192000, "192.0 kHz", true),
    Rate_352_8kHz(352800, "352.8 kHz (DXD)", true),
    Rate_384kHz(384000, "384.0 kHz", true),
    Rate_768kHz(768000, "768.0 kHz", true);

    companion object {
        fun fromHz(hz: Int): HiResSampleRate = values().firstOrNull { it.sampleRateHz == hz } ?: Rate_44_1kHz
    }
}

/**
 * Audiophile bit depth resolution.
 */
enum class HiResBitDepth(val bitDepth: Int, val displayName: String, val isHiRes: Boolean) {
    Bit_16(16, "16-bit Integer (CD Quality)", false),
    Bit_24(24, "24-bit Studio Master", true),
    Bit_32_Int(32, "32-bit Integer", true),
    Bit_32_Float(32, "32-bit Float (Direct)", true);

    companion object {
        fun fromBits(bits: Int): HiResBitDepth = when (bits) {
            16 -> Bit_16
            24 -> Bit_24
            32 -> Bit_32_Int
            else -> if (bits > 16) Bit_24 else Bit_16
        }
    }
}

/**
 * Audio codec and container formats.
 */
enum class AudioFormatCodec(val displayName: String, val isLossless: Boolean, val isDsd: Boolean) {
    FLAC("FLAC Lossless", true, false),
    ALAC("Apple Lossless (ALAC)", true, false),
    WAV("Uncompressed PCM (WAV)", true, false),
    AIFF("Uncompressed PCM (AIFF)", true, false),
    DSD_DoP("DSD over PCM (DoP)", true, true),
    DSD_Native("Native DSD Stream", true, true),
    AAC("Advanced Audio Coding (AAC)", false, false),
    MP3("MPEG-3 Audio (MP3)", false, false),
    Unknown("Unknown Audio Stream", false, false)
}

/**
 * Connection and direct passthrough states for USB DAC hardware.
 */
enum class DacConnectionState(val title: String, val badgeColorHex: Long) {
    Disconnected("No External DAC Detected", 0xFF64748B), // Slate
    Detecting("Interrogating USB Audio Sink...", 0xFF38BDF8), // Sky Blue
    ConnectedBitPerfect("Bit-Perfect 1:1 Direct Passthrough", 0xFFE5A93C), // Lossless Gold
    ConnectedDirectAudio("Hi-Res Direct Audio (Direct PCM)", 0xFF10B981), // Emerald
    FallbackSystemMixer("System Mixer Audio (48kHz Resampled)", 0xFF94A3B8) // Muted Slate
}

/**
 * DSD (Direct Stream Digital) playback mode.
 */
enum class DsdPlaybackMode(val displayName: String, val description: String) {
    DoP("DSD over PCM (DoP)", "Encapsulates raw DSD 1-bit frames into 176.4kHz/352.8kHz PCM containers with 0x05/0xFA markers without decoding."),
    PcmConvert("DSD to PCM Multibit", "Converts 1-bit DSD bitstream to high-resolution 24-bit/88.2kHz PCM via decimation filter.")
}

/**
 * Volume attenuation strategy for connected DAC.
 */
enum class DacVolumeMode(val displayName: String, val description: String) {
    HardwareDirect("Hardware DAC Direct", "Bypasses Android software digital scaling completely. Audio bits are delivered at 0dBFS directly to the DAC's analog ladder."),
    SoftwareFloat("32-bit Float Attenuation", "High-precision 32-bit floating point digital volume attenuation with TPDF triangular dither before hardware output.")
}

/**
 * Dither algorithm for bit depth reduction.
 */
enum class DitherMode(val displayName: String) {
    None("No Dither (Truncate)"),
    Tpdf("Triangular Probability Density (TPDF)"),
    NoiseShaping("High-Pass Noise Shaping (Optimal Psychoacoustic)")
}

/**
 * Hardware specification of a detected USB Digital-to-Analog Converter.
 */
data class DacHardwareProfile(
    val deviceName: String = "Internal Audio Output",
    val manufacturer: String = "Standard System Audio",
    val usbVendorId: String = "0000",
    val usbProductId: String = "0000",
    val supportedSampleRates: List<HiResSampleRate> = listOf(HiResSampleRate.Rate_44_1kHz, HiResSampleRate.Rate_48kHz),
    val supportedBitDepths: List<HiResBitDepth> = listOf(HiResBitDepth.Bit_16, HiResBitDepth.Bit_24),
    val supportsDsdDoP: Boolean = false,
    val supportsDirectFlag: Boolean = true,
    val isExternalUsbDac: Boolean = false,
    val maxSampleRateHz: Int = 48000
)

/**
 * Audio stream metadata specification for the currently loaded/playing track.
 */
data class StreamAudioSpec(
    val codec: AudioFormatCodec = AudioFormatCodec.FLAC,
    val sampleRate: HiResSampleRate = HiResSampleRate.Rate_44_1kHz,
    val bitDepth: HiResBitDepth = HiResBitDepth.Bit_16,
    val channelCount: Int = 2,
    val bitrateKbps: Int = 1411,
    val isLossless: Boolean = true,
    val isDsd: Boolean = false,
    val dsdRateString: String? = null
)

/**
 * Complete real-time signal chain status from media source file to DAC output transducer.
 */
data class BitPerfectSignalChain(
    val sourceSpec: StreamAudioSpec = StreamAudioSpec(),
    val isDecoderLossless: Boolean = true,
    val dspBypassed: Boolean = true,
    val dacOutputRate: HiResSampleRate = HiResSampleRate.Rate_44_1kHz,
    val dacOutputDepth: HiResBitDepth = HiResBitDepth.Bit_16,
    val isExactClockMatch: Boolean = true,
    val isExactBitDepthMatch: Boolean = true,
    val isBitPerfectDirect: Boolean = false,
    val isOsResamplerBypassed: Boolean = false,
    val signalHealthScore: Float = 1.0f // 1.0 = Perfect 1:1, 0.7 = Direct Hi-Res, 0.4 = Resampled
)

/**
 * Audiophile configuration settings.
 */
data class HiResLosslessSettings(
    val isBitPerfectEnabled: Boolean = true,
    val exclusiveUsbMode: Boolean = true,
    val directPcmBypass: Boolean = true,
    val volumeControlMode: DacVolumeMode = DacVolumeMode.HardwareDirect,
    val dsdMode: DsdPlaybackMode = DsdPlaybackMode.DoP,
    val ditherMode: DitherMode = DitherMode.Tpdf,
    val showHorizonBadge: Boolean = true
)

/**
 * Real-time telemetry packet published by the USB DAC engine.
 */
data class DacTelemetryState(
    val connectionState: DacConnectionState = DacConnectionState.Disconnected,
    val hardwareProfile: DacHardwareProfile = DacHardwareProfile(),
    val sourceSpec: StreamAudioSpec = StreamAudioSpec(),
    val signalChain: BitPerfectSignalChain = BitPerfectSignalChain(),
    val clockDriftPpm: Float = 0.0f,
    val bufferUnderrunCount: Long = 0L
)
