package com.pixelody.app.core.playback

import com.pixelody.app.data.model.AcousticTargetPreset
import com.pixelody.app.data.model.AudioBitstreamVerification
import com.pixelody.app.data.model.Track
import kotlin.math.pow

/**
 * Source-format information and optional processing presets.
 * Track metadata cannot verify the samples delivered by an Android output.
 */
object PureSignalMasterEngine {

    /**
     * Reports known source metadata. Output verification requires route measurements.
     */
    fun verifyBitstreamPath(
        track: Track,
        activeOutputDevice: String? = null
    ): AudioBitstreamVerification {
        return AudioBitstreamVerification(
            isBitPerfect = false,
            bitDepth = track.bitDepth?.takeIf { it > 0 } ?: 0,
            sampleRateHz = track.sampleRate.coerceAtLeast(0),
            codec = track.format.ifBlank { track.codec.ifBlank { "Unknown format" } },
            outputSink = activeOutputDevice?.takeIf { it.isNotBlank() } ?: "Output unknown"
        )
    }

    /**
     * Calculates the linear gain reduction multiplier required to keep audio below [ceilingDb].
     */
    fun calculateLimiterGain(
        peakLevelDb: Float,
        ceilingDb: Float = -0.3f
    ): Float {
        if (peakLevelDb <= ceilingDb) return 1.0f
        val attenuationDb = ceilingDb - peakLevelDb
        return (10.0.pow(attenuationDb.toDouble() / 20.0)).toFloat().coerceIn(0.1f, 1.0f)
    }

    /**
     * Detects and recommends the ideal [AcousticTargetPreset] based on track characteristics and output sink.
     */
    fun detectOptimalAcousticProfile(
        track: Track,
        outputDevice: String?
    ): AcousticTargetPreset {
        val dev = outputDevice?.lowercase() ?: ""
        return when {
            dev.contains("usb") || dev.contains("dac") -> AcousticTargetPreset.HiResDacDirect
            dev.contains("car") || dev.contains("auto") -> AcousticTargetPreset.CarAudioPunch
            dev.contains("iem") || dev.contains("earbud") -> AcousticTargetPreset.IemHarmonicWarmth
            dev.contains("open") || dev.contains("planar") -> AcousticTargetPreset.OpenBackAiry
            track.lossless -> AcousticTargetPreset.StudioFlatReference
            else -> AcousticTargetPreset.IemHarmonicWarmth
        }
    }

    /**
     * Returns the 10-band parametric EQ gain curve for a given preset.
     */
    fun calculateParametricResponse(preset: AcousticTargetPreset): List<Float> {
        return preset.bandsDb
    }
}
