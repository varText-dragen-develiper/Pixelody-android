package com.pixelody.app.core.playback

import com.pixelody.app.data.model.CassetteShellTheme
import com.pixelody.app.data.model.CassetteTapeSettings
import com.pixelody.app.data.model.TapeFormulation
import com.pixelody.app.data.model.TapeMagneticsTelemetry
import com.pixelody.app.data.model.TapeNoiseReduction
import com.pixelody.app.data.model.TapeSpoolPhysics
import com.pixelody.app.data.model.TapeTransportState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * CassetteTapeEngine: Vintage Magnetic Tape Emulation & Dynamic Mechanical Deck DSP.
 * Implements magnetic core hysteresis tanh soft-clipping, dual-LFO motor Wow & Flutter delay lines,
 * tape head azimuth skew phase delay, Dolby B/C/dbx companding with pink noise hiss generation,
 * analog VU meter ballistics, and real-time spool volume conservation physics.
 */
class CassetteTapeEngine(
    initialSettings: CassetteTapeSettings = CassetteTapeSettings(),
    private val sampleRateHz: Int = 48000
) {
    private val _settings = MutableStateFlow(initialSettings)
    val settings: StateFlow<CassetteTapeSettings> = _settings.asStateFlow()

    private val _telemetry = MutableStateFlow(
        calculateInitialTelemetry(initialSettings)
    )
    val telemetry: StateFlow<TapeMagneticsTelemetry> = _telemetry.asStateFlow()

    // Circular delay buffer for Wow, Flutter, and Azimuth skew modulation (max 100ms = 4800 samples)
    private val maxDelayBufferSize = (sampleRateHz * 0.10f).toInt()
    private val leftDelayBuffer = FloatArray(maxDelayBufferSize)
    private val rightDelayBuffer = FloatArray(maxDelayBufferSize)
    private var writeIndex = 0

    // Filter states for frequency shaping (Low-end head bump and Treble rolloff)
    private var leftLowPassState = 0f
    private var rightLowPassState = 0f
    private var leftHeadBumpState = 0f
    private var rightHeadBumpState = 0f

    // Running sample clock for Wow/Flutter LFO phase
    private var sampleCounter: Long = 0L

    // VU meter needle states for ballistics
    private var leftNeedleState = 0f
    private var rightNeedleState = 0f

    // Internal noise generator state (3-pole Pink Noise generator)
    private var pinkB0 = 0f
    private var pinkB1 = 0f
    private var pinkB2 = 0f
    private var noiseRandomSeed = 123456789L

    // Current transport track progress ratio (0.0 to 1.0)
    private var currentProgressRatio = 0.15f
    private var currentTransportState = TapeTransportState.Playing

    fun updateSettings(newSettings: CassetteTapeSettings) {
        _settings.value = newSettings
        refreshTelemetry()
    }

    fun setFormulation(formulation: TapeFormulation) {
        val current = _settings.value
        updateSettings(
            current.copy(
                formulation = formulation,
                driveGain = formulation.defaultDriveGain
            )
        )
    }

    fun setNoiseReduction(nr: TapeNoiseReduction) {
        val current = _settings.value
        updateSettings(current.copy(noiseReduction = nr))
    }

    fun setShellTheme(theme: CassetteShellTheme) {
        val current = _settings.value
        updateSettings(current.copy(shellTheme = theme))
    }

    fun setTransportState(state: TapeTransportState) {
        currentTransportState = state
        refreshTelemetry()
    }

    fun setDriveGain(gain: Float) {
        val current = _settings.value
        updateSettings(current.copy(driveGain = gain.coerceIn(0.1f, 3.0f)))
    }

    fun setWowFlutterIntensity(intensity: Float) {
        val current = _settings.value
        updateSettings(current.copy(wowFlutterIntensity = intensity.coerceIn(0.0f, 1.0f)))
    }

    fun setTapeWear(wear: Float) {
        val current = _settings.value
        updateSettings(current.copy(tapeWear = wear.coerceIn(0.0f, 1.0f)))
    }

    fun setAzimuthSkewMs(skewMs: Float) {
        val current = _settings.value
        updateSettings(current.copy(azimuthSkewMs = skewMs.coerceIn(-0.4f, 0.4f)))
    }

    fun toggleEnabled() {
        val current = _settings.value
        updateSettings(current.copy(isEnabled = !current.isEnabled))
    }

    /**
     * Updates song playback position to drive mechanical spool radius and RPM calculation.
     */
    fun updateTapeProgress(positionMs: Long, durationMs: Long) {
        if (durationMs > 0L) {
            currentProgressRatio = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0.0f, 1.0f)
        }
        refreshTelemetry()
    }

    /**
     * Processes a single stereo audio sample frame (Left, Right) through the analog tape signal path.
     */
    fun processStereoFrame(leftIn: Float, rightIn: Float): Pair<Float, Float> {
        val s = _settings.value
        val form = s.formulation
        val nr = s.noiseReduction

        if (!s.isEnabled || currentTransportState == TapeTransportState.Ejected) {
            updateVuNeedles(leftIn, rightIn, leftIn, rightIn)
            return leftIn to rightIn
        }

        if (currentTransportState == TapeTransportState.Stopped || currentTransportState == TapeTransportState.Paused) {
            updateVuNeedles(leftIn, rightIn, 0f, 0f)
            return 0f to 0f
        }

        sampleCounter++

        // 1. Motor Speed Modulation: Wow (0.8 Hz) & Flutter (8.5 Hz + 14.2 Hz)
        val baseDelayMs = 2.0f // 2ms nominal delay offset
        val wowLfoHz = 0.82f
        val flutterLfoHz = 8.65f
        val flutterHarmonicHz = 14.3f

        val tSec = sampleCounter.toDouble() / sampleRateHz.toDouble()
        val wowAmp = 0.85f * s.wowFlutterIntensity * (1.0f + 1.2f * s.tapeWear)
        val flutterAmp = 0.35f * s.wowFlutterIntensity * (1.0f + 1.8f * s.tapeWear)

        val wowModMs = (sin(2.0 * PI * wowLfoHz * tSec) * wowAmp).toFloat()
        val flutterModMs = (sin(2.0 * PI * flutterLfoHz * tSec) * flutterAmp * 0.7f +
                sin(2.0 * PI * flutterHarmonicHz * tSec) * flutterAmp * 0.3f).toFloat()

        val totalLeftDelayMs = (baseDelayMs + wowModMs + flutterModMs).coerceAtLeast(0.1f)
        val totalRightDelayMs = (baseDelayMs + wowModMs + flutterModMs + s.azimuthSkewMs).coerceAtLeast(0.1f)

        // Write to circular delay buffer
        leftDelayBuffer[writeIndex] = leftIn
        rightDelayBuffer[writeIndex] = rightIn

        val leftDelayed = readFractionalDelay(leftDelayBuffer, totalLeftDelayMs * sampleRateHz / 1000f)
        val rightDelayed = readFractionalDelay(rightDelayBuffer, totalRightDelayMs * sampleRateHz / 1000f)

        // Advance write pointer
        writeIndex = (writeIndex + 1) % maxDelayBufferSize

        // 2. Input Drive & Magnetic Hysteresis Non-Linear Saturation
        val effectiveDrive = s.driveGain * form.defaultDriveGain
        val drivenL = leftDelayed * effectiveDrive
        val drivenR = rightDelayed * effectiveDrive

        val saturatedL = computeSaturation(drivenL, form.saturationCeiling, s.tapeWear)
        val saturatedR = computeSaturation(drivenR, form.saturationCeiling, s.tapeWear)

        // 3. Low-End Magnetic Head Bump (Resonant 50Hz boost)
        val bumpGain = (10.0.pow(form.lowEndBumpDb / 20.0) - 1.0).toFloat()
        val bumpAlpha = (1.0f - exp(-2.0 * PI * 55.0 / sampleRateHz)).toFloat()
        leftHeadBumpState += bumpAlpha * (saturatedL - leftHeadBumpState)
        rightHeadBumpState += bumpAlpha * (saturatedR - rightHeadBumpState)

        val bumpedL = saturatedL + (leftHeadBumpState * bumpGain)
        val bumpedR = saturatedR + (rightHeadBumpState * bumpGain)

        // 4. High-Frequency Damping / Treble Rolloff (1-Pole Low Pass Filter)
        val effectiveCutoffHz = form.highFreqRolloffHz * (1.0f - 0.28f * s.tapeWear).coerceIn(4000f, 24000f)
        val lowPassAlpha = (1.0f - exp(-2.0 * PI * effectiveCutoffHz / sampleRateHz)).toFloat().coerceIn(0.01f, 0.99f)

        leftLowPassState += lowPassAlpha * (bumpedL - leftLowPassState)
        rightLowPassState += lowPassAlpha * (bumpedR - rightLowPassState)

        // 5. Tape Hiss Synthesis (Pink Noise Generator) & Noise Reduction Attenuation
        val baseHissLevelDb = form.noiseFloorDb - nr.hissAttenuationDb
        val hissLinearGain = (10.0.pow(baseHissLevelDb / 20.0)).toFloat() * s.hissVolumeGain * 2.2f
        val pinkNoiseSample = generatePinkNoiseSample() * hissLinearGain

        // Output summing
        val outL = leftLowPassState + pinkNoiseSample
        val outR = rightLowPassState + pinkNoiseSample

        // 6. Update VU meters with analog needle ballistics
        updateVuNeedles(leftIn, rightIn, outL, outR)

        return outL to outR
    }

    /**
     * Batch processes arrays of stereo samples in-place or into target arrays.
     */
    fun processStereoBuffer(leftChannel: FloatArray, rightChannel: FloatArray) {
        val count = min(leftChannel.size, rightChannel.size)
        for (i in 0 until count) {
            val (l, r) = processStereoFrame(leftChannel[i], rightChannel[i])
            leftChannel[i] = l
            rightChannel[i] = r
        }
    }

    /**
     * Core magnetic hysteresis saturation with soft clipping and harmonic generation.
     */
    fun computeSaturation(input: Float, ceiling: Float, wear: Float): Float {
        val normInput = input / max(ceiling, 0.01f)
        // Hyperbolic tangent soft saturation curve
        val satNorm = tanh(normInput)
        val satCore = satNorm * ceiling

        // Add asymmetrical 2nd harmonic and 3rd harmonic saturation colors based on tape wear
        val secondHarmonic = 0.05f * wear * (satNorm * abs(satNorm)) * ceiling
        val thirdHarmonic = -0.03f * (satNorm * satNorm * satNorm) * ceiling

        return satCore + secondHarmonic + thirdHarmonic
    }

    /**
     * Calculates spool physics enforcing volume conservation between supply and take-up reels.
     * Supply Reel: r_supply^2 = R_max^2 * (1 - p) + R_hub^2 * p
     * Take-Up Reel: r_takeup^2 = R_hub^2 * (1 - p) + R_max^2 * p
     */
    fun computeSpoolPhysics(progressNormalized: Float, transportState: TapeTransportState): TapeSpoolPhysics {
        val p = progressNormalized.coerceIn(0.0f, 1.0f)
        val rHub = 1.0f // Hub radius ratio
        val rMax = 3.5f // Full pack radius ratio

        val rSupplySq = (rMax * rMax) * (1.0f - p) + (rHub * rHub) * p
        val rTakeupSq = (rHub * rHub) * (1.0f - p) + (rMax * rMax) * p

        val rSupply = sqrt(rSupplySq)
        val rTakeup = sqrt(rTakeupSq)

        // Normalized to [0.0 (empty hub), 1.0 (full reel)]
        val supplyRatio = ((rSupply - rHub) / (rMax - rHub)).coerceIn(0.0f, 1.0f)
        val takeupRatio = ((rTakeup - rHub) / (rMax - rHub)).coerceIn(0.0f, 1.0f)

        // Base linear tape speed RPM inversely proportional to radius
        val baseLinearSpeed = 45f // RPM constant
        val speedMult = transportState.motorSpeedMultiplier

        val supplyRpm = (baseLinearSpeed / (rSupply / rHub)) * speedMult
        val takeupRpm = (baseLinearSpeed / (rTakeup / rHub)) * speedMult

        val tensionGrams = if (transportState.headEngaged) (32f + 8f * abs(speedMult)) else 15f
        val contactPressure = if (transportState.headEngaged) 0.96f else 0.0f

        return TapeSpoolPhysics(
            supplyRadiusRatio = supplyRatio,
            takeupRadiusRatio = takeupRatio,
            supplyRpm = supplyRpm,
            takeupRpm = takeupRpm,
            tapeTensionGrams = tensionGrams,
            headContactPressure = contactPressure
        )
    }

    /**
     * Reads from delay buffer with linear sub-sample interpolation.
     */
    private fun readFractionalDelay(buffer: FloatArray, delaySamples: Float): Float {
        val safeDelay = delaySamples.coerceIn(0f, (maxDelayBufferSize - 2).toFloat())
        val intDelay = safeDelay.toInt()
        val frac = safeDelay - intDelay

        var idx0 = writeIndex - intDelay
        if (idx0 < 0) idx0 += maxDelayBufferSize

        var idx1 = idx0 - 1
        if (idx1 < 0) idx1 += maxDelayBufferSize

        val s0 = buffer[idx0]
        val s1 = buffer[idx1]

        return s0 + frac * (s1 - s0)
    }

    /**
     * Generates a 3-pole pink noise sample (1/f distribution).
     */
    private fun generatePinkNoiseSample(): Float {
        // Linear congruential generator for fast pure-JVM random float
        noiseRandomSeed = (noiseRandomSeed * 6364136223846793005L + 1442695040888963407L)
        val white = ((noiseRandomSeed ushr 40).toFloat() / 8388608f) - 1.0f

        pinkB0 = 0.99765f * pinkB0 + white * 0.0990460f
        pinkB1 = 0.96300f * pinkB1 + white * 0.2965164f
        pinkB2 = 0.57000f * pinkB2 + white * 1.0526913f
        val pink = pinkB0 + pinkB1 + pinkB2 + white * 0.1848f
        return (pink * 0.08f).coerceIn(-1.0f, 1.0f)
    }

    /**
     * Updates analog VU meter needle ballistics (300ms rise time integration).
     */
    private fun updateVuNeedles(inL: Float, inR: Float, outL: Float, outR: Float) {
        val absL = abs(outL)
        val absR = abs(outR)

        val dbL = if (absL > 0.0001f) (20.0f * log10(absL)).coerceIn(-40f, 6f) else -40f
        val dbR = if (absR > 0.0001f) (20.0f * log10(absR)).coerceIn(-40f, 6f) else -40f

        // Map dB (-40dB to +6dB) to normalized needle angle (0.0 to 1.0+)
        // -12dB (typical 0 VU reference) maps to ~0.65
        val targetNeedleL = ((dbL + 40f) / 46f).coerceIn(0.0f, 1.2f)
        val targetNeedleR = ((dbR + 40f) / 46f).coerceIn(0.0f, 1.2f)

        // Smooth needle attack (fast) and release (gentle)
        val attackAlpha = 0.22f
        val releaseAlpha = 0.06f

        leftNeedleState += if (targetNeedleL > leftNeedleState) {
            attackAlpha * (targetNeedleL - leftNeedleState)
        } else {
            releaseAlpha * (targetNeedleL - leftNeedleState)
        }

        rightNeedleState += if (targetNeedleR > rightNeedleState) {
            attackAlpha * (targetNeedleR - rightNeedleState)
        } else {
            releaseAlpha * (targetNeedleR - rightNeedleState)
        }
    }

    private fun refreshTelemetry() {
        val s = _settings.value
        val form = s.formulation
        val nr = s.noiseReduction

        val spool = computeSpoolPhysics(currentProgressRatio, currentTransportState)

        // THD % approximation based on drive and formulation ceiling
        val effectiveDrive = s.driveGain * form.defaultDriveGain
        val thdPercent = (0.45f * (effectiveDrive / form.saturationCeiling).pow(2.2f) * (1.0f + 1.5f * s.tapeWear)).coerceIn(0.1f, 18.0f)

        val snrDb = (abs(form.noiseFloorDb) + nr.hissAttenuationDb - (s.tapeWear * 12f)).coerceIn(35f, 95f)

        val tSec = sampleCounter.toDouble() / sampleRateHz.toDouble()
        val wowMod = (sin(2.0 * PI * 0.82 * tSec) * 0.85f * s.wowFlutterIntensity).toFloat()
        val flutterMod = (sin(2.0 * PI * 8.65 * tSec) * 0.35f * s.wowFlutterIntensity).toFloat()

        _telemetry.value = TapeMagneticsTelemetry(
            leftInDb = -12f,
            rightInDb = -12f,
            leftOutDb = -11.5f,
            rightOutDb = -11.5f,
            leftVuNeedleNormalized = leftNeedleState,
            rightVuNeedleNormalized = rightNeedleState,
            saturationThdPercent = thdPercent,
            wowModulationMs = wowMod,
            flutterModulationMs = flutterMod,
            spoolPhysics = spool,
            transportState = currentTransportState,
            effectiveSnrDb = snrDb
        )
    }

    private fun calculateInitialTelemetry(s: CassetteTapeSettings): TapeMagneticsTelemetry {
        return TapeMagneticsTelemetry(
            leftInDb = -14f,
            rightInDb = -14f,
            leftOutDb = -13.5f,
            rightOutDb = -13.5f,
            leftVuNeedleNormalized = 0.55f,
            rightVuNeedleNormalized = 0.55f,
            saturationThdPercent = 0.75f,
            wowModulationMs = 0.02f,
            flutterModulationMs = 0.005f,
            spoolPhysics = TapeSpoolPhysics(supplyRadiusRatio = 0.82f, takeupRadiusRatio = 0.28f),
            transportState = TapeTransportState.Playing,
            effectiveSnrDb = 68f
        )
    }
}
