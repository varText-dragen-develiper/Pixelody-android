package com.pixelody.app.core.playback

import com.pixelody.app.data.model.DjTransitionCurve
import com.pixelody.app.data.model.MultibandCrossoverProfile
import com.pixelody.app.data.model.MultibandDspFrame
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * MultibandCrossfaderEngine: Tri-band (Low/Mid/High) frequency-split crossfader DSP calculator.
 * Features downbeat-locked bass swapping, vocal midrange anti-clash ducking, and resonant high-pass sweeps.
 */
object MultibandCrossfaderEngine {

    private const val PI_HALF = (Math.PI / 2.0).toFloat()
    // -4 dB linear gain multiplier for vocal midrange ducking
    private const val VOCAL_DUCK_LINEAR_GAIN = 0.63095734f

    /**
     * Computes the 3-band DSP gains and crossover states for Deck A and Deck B at a specific transition progress.
     */
    fun calculateMultibandFrame(
        progress: Float,
        curve: DjTransitionCurve,
        profile: MultibandCrossoverProfile = MultibandCrossoverProfile()
    ): MultibandDspFrame {
        val p = progress.coerceIn(0f, 1f)

        return when (curve) {
            DjTransitionCurve.EqualPower -> {
                val angle = p * PI_HALF
                val outGain = cos(angle.toDouble()).toFloat().coerceIn(0f, 1f)
                val inGain = sin(angle.toDouble()).toFloat().coerceIn(0f, 1f)

                MultibandDspFrame(
                    progress = p,
                    deckALowGain = outGain,
                    deckAMidGain = outGain,
                    deckAHighGain = outGain,
                    deckBLowGain = inGain,
                    deckBMidGain = inGain,
                    deckBHighGain = inGain,
                    isBassSwapped = p >= 0.5f,
                    isVocalDucked = false,
                    highPassFilterHz = profile.highPassSweepStartHz,
                    echoDecayGain = 0f,
                    tempoStretchFactor = 1.0f,
                    activeCurve = curve
                )
            }

            DjTransitionCurve.BassSwap -> {
                val angle = p * PI_HALF
                val outSine = cos(angle.toDouble()).toFloat().coerceIn(0f, 1f)
                val inSine = sin(angle.toDouble()).toFloat().coerceIn(0f, 1f)

                // Instant clean Low swap at exactly the threshold (50% progress / downbeat)
                val isBassSwapped = p >= profile.bassSwapThreshold
                val outLow = if (!isBassSwapped) 1.0f else 0.0f
                val inLow = if (isBassSwapped) 1.0f else 0.0f

                // Vocal midrange ducking when both tracks' mids overlap in the core mix zone
                val isOverlapZone = p in 0.25f..0.75f
                val duckFactor = if (isOverlapZone) VOCAL_DUCK_LINEAR_GAIN else 1.0f

                val outMid = (outSine * duckFactor).coerceIn(0f, 1f)
                val inMid = inSine

                MultibandDspFrame(
                    progress = p,
                    deckALowGain = outLow,
                    deckAMidGain = outMid,
                    deckAHighGain = outSine,
                    deckBLowGain = inLow,
                    deckBMidGain = inMid,
                    deckBHighGain = inSine,
                    isBassSwapped = isBassSwapped,
                    isVocalDucked = isOverlapZone,
                    highPassFilterHz = if (isBassSwapped) profile.lowCrossoverHz else profile.highPassSweepStartHz,
                    echoDecayGain = 0f,
                    tempoStretchFactor = 1.0f,
                    activeCurve = curve
                )
            }

            DjTransitionCurve.FilterSweep -> {
                // Exponential high-pass sweep (20Hz -> 3500Hz)
                val hpCutoff = (profile.highPassSweepStartHz * (profile.highPassSweepEndHz / profile.highPassSweepStartHz).pow(p))
                    .coerceIn(profile.highPassSweepStartHz, profile.highPassSweepEndHz)

                // Low frequencies roll off sharply as high-pass filter climbs
                val outLow = (1f - p).pow(2.5f).coerceIn(0f, 1f)
                val outMid = (1f - p).pow(1.5f).coerceIn(0f, 1f)
                val outHigh = (1f - p).pow(0.8f).coerceIn(0f, 1f)

                // Incoming opens up gracefully
                val inLow = p.pow(1.5f).coerceIn(0f, 1f)
                val inMid = p.pow(1.0f).coerceIn(0f, 1f)
                val inHigh = p.pow(0.8f).coerceIn(0f, 1f)

                MultibandDspFrame(
                    progress = p,
                    deckALowGain = outLow,
                    deckAMidGain = outMid,
                    deckAHighGain = outHigh,
                    deckBLowGain = inLow,
                    deckBMidGain = inMid,
                    deckBHighGain = inHigh,
                    isBassSwapped = p >= 0.5f,
                    isVocalDucked = false,
                    highPassFilterHz = hpCutoff,
                    echoDecayGain = 0f,
                    tempoStretchFactor = 1.0f,
                    activeCurve = curve
                )
            }

            DjTransitionCurve.EchoOut -> {
                val outLow: Float
                val outMid: Float
                val outHigh: Float
                val echoWet: Float
                val inGain: Float

                if (p < 0.35f) {
                    outLow = 1.0f
                    outMid = 1.0f
                    outHigh = 1.0f
                    echoWet = 0.0f
                    inGain = (p / 0.35f) * 0.3f
                } else {
                    val decayProgress = (p - 0.35f) / 0.65f
                    outLow = 0f
                    outMid = 0f
                    outHigh = 0f
                    echoWet = (cos(decayProgress * PI_HALF.toDouble()).toFloat() * 0.85f).coerceIn(0f, 1f)
                    inGain = (0.3f + 0.7f * decayProgress).coerceIn(0f, 1f)
                }

                MultibandDspFrame(
                    progress = p,
                    deckALowGain = outLow,
                    deckAMidGain = outMid,
                    deckAHighGain = outHigh,
                    deckBLowGain = inGain,
                    deckBMidGain = inGain,
                    deckBHighGain = inGain,
                    isBassSwapped = p >= 0.35f,
                    isVocalDucked = false,
                    highPassFilterHz = profile.highPassSweepStartHz,
                    echoDecayGain = echoWet,
                    tempoStretchFactor = 1.0f,
                    activeCurve = curve
                )
            }

            DjTransitionCurve.VinylBrake -> {
                val speedFactor = if (p < 0.85f) {
                    val decelProgress = p / 0.85f
                    (1.0f - decelProgress.pow(1.7f)).coerceIn(0.08f, 1.0f)
                } else {
                    1.0f
                }

                val outGain = if (p < 0.85f) (1f - (p / 0.85f)).pow(0.6f).coerceIn(0f, 1f) else 0f
                val inGain = if (p < 0.80f) 0f else ((p - 0.80f) / 0.20f).coerceIn(0f, 1f)

                MultibandDspFrame(
                    progress = p,
                    deckALowGain = outGain,
                    deckAMidGain = outGain,
                    deckAHighGain = outGain,
                    deckBLowGain = inGain,
                    deckBMidGain = inGain,
                    deckBHighGain = inGain,
                    isBassSwapped = p >= 0.80f,
                    isVocalDucked = false,
                    highPassFilterHz = profile.highPassSweepStartHz,
                    echoDecayGain = 0f,
                    tempoStretchFactor = speedFactor,
                    activeCurve = curve
                )
            }
        }
    }
}
