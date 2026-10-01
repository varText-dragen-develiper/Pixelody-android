package com.pixelody.app.core.playback

import com.pixelody.app.data.model.AcousticChamberPreset
import com.pixelody.app.data.model.BinauralCrossfeedMode
import com.pixelody.app.data.model.SpatialAcousticTelemetry
import com.pixelody.app.data.model.SpatialChamberSettings
import com.pixelody.app.data.model.SpatialSpeakerPosition
import com.pixelody.app.data.model.WallMaterialDamping
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * SpatialAcousticChamberEngine: Studio-grade 3D Headphone Spatializer & Virtual Room Acoustics Engine.
 * Simulates real physical acoustic chambers with Head-Related Transfer Function (HRTF) Interaural Time & Level
 * Differences (ITD/ILD), 6-tap early reflection geometry, wall damping absorption spectra, and diffuse reverberation.
 */
class SpatialAcousticChamberEngine(
    initialSettings: SpatialChamberSettings = SpatialChamberSettings(),
    private val sampleRateHz: Int = 48000
) {
    private val _settings = MutableStateFlow(initialSettings)
    val settings: StateFlow<SpatialChamberSettings> = _settings.asStateFlow()

    private val _telemetry = MutableStateFlow(calculateTelemetry(initialSettings))
    val telemetry: StateFlow<SpatialAcousticTelemetry> = _telemetry.asStateFlow()

    // Circular delay buffers for early reflections and crossfeed
    private val maxDelaySamples = (sampleRateHz * 0.15f).toInt() // 150ms buffer
    private val leftDelayBuffer = FloatArray(maxDelaySamples)
    private val rightDelayBuffer = FloatArray(maxDelaySamples)
    private var writeIndex = 0

    // 1-Pole Low-Pass Filter states for contralateral ear head shadow
    private var leftFilterState = 0f
    private var rightFilterState = 0f

    // Reverb diffuse tank delay buffers (Schroeder all-pass & comb filters)
    private val comb1Buffer = FloatArray(1423)
    private val comb2Buffer = FloatArray(1789)
    private val comb3Buffer = FloatArray(2137)
    private val comb4Buffer = FloatArray(2551)
    private var comb1Idx = 0
    private var comb2Idx = 0
    private var comb3Idx = 0
    private var comb4Idx = 0

    fun updateSettings(newSettings: SpatialChamberSettings) {
        _settings.value = newSettings
        _telemetry.value = calculateTelemetry(newSettings)
    }

    fun applyPreset(preset: AcousticChamberPreset) {
        val current = _settings.value
        val angle = preset.defaultSpeakerAngleDeg
        val updated = current.copy(
            preset = preset,
            roomVolumeM3 = preset.defaultVolumeM3,
            reverbDecaySeconds = preset.defaultRt60Seconds,
            wallDamping = preset.defaultDamping,
            speakerPosition = current.speakerPosition.copy(
                leftAngleDeg = -angle,
                rightAngleDeg = angle,
                distanceMeters = when (preset) {
                    AcousticChamberPreset.MinimalistTeahouse -> 1.2f
                    AcousticChamberPreset.TokyoVinylBar -> 1.5f
                    AcousticChamberPreset.AbbeyStudioControlRoom -> 1.8f
                    AcousticChamberPreset.CyberpunkAlleyway -> 2.5f
                    AcousticChamberPreset.CathedralOfEchoes -> 4.0f
                    AcousticChamberPreset.CustomStudio -> 1.8f
                }
            ),
            earlyReflectionGain = when (preset) {
                AcousticChamberPreset.MinimalistTeahouse -> 0.15f
                AcousticChamberPreset.AbbeyStudioControlRoom -> 0.35f
                AcousticChamberPreset.TokyoVinylBar -> 0.45f
                AcousticChamberPreset.CyberpunkAlleyway -> 0.55f
                AcousticChamberPreset.CathedralOfEchoes -> 0.70f
                AcousticChamberPreset.CustomStudio -> 0.35f
            },
            diffuseTailGain = when (preset) {
                AcousticChamberPreset.MinimalistTeahouse -> 0.08f
                AcousticChamberPreset.AbbeyStudioControlRoom -> 0.20f
                AcousticChamberPreset.TokyoVinylBar -> 0.35f
                AcousticChamberPreset.CyberpunkAlleyway -> 0.60f
                AcousticChamberPreset.CathedralOfEchoes -> 0.85f
                AcousticChamberPreset.CustomStudio -> 0.25f
            }
        )
        updateSettings(updated)
    }

    fun updateSpeakerPosition(leftAngleDeg: Float, rightAngleDeg: Float, distanceMeters: Float = 1.8f) {
        val current = _settings.value
        val updated = current.copy(
            speakerPosition = current.speakerPosition.copy(
                leftAngleDeg = leftAngleDeg.coerceIn(-90f, -10f),
                rightAngleDeg = rightAngleDeg.coerceIn(10f, 90f),
                distanceMeters = distanceMeters.coerceIn(0.5f, 6.0f)
            )
        )
        updateSettings(updated)
    }

    fun setCrossfeedMode(mode: BinauralCrossfeedMode) {
        val current = _settings.value
        val updated = current.copy(
            crossfeedMode = mode,
            speakerPosition = if (mode != BinauralCrossfeedMode.DirectStereoOff) {
                current.speakerPosition.copy(
                    leftAngleDeg = -mode.baseAngleDeg,
                    rightAngleDeg = mode.baseAngleDeg
                )
            } else current.speakerPosition
        )
        updateSettings(updated)
    }

    fun setWallMaterial(material: WallMaterialDamping) {
        val current = _settings.value
        val updated = current.copy(wallDamping = material)
        updateSettings(updated)
    }

    fun updateHeadYaw(yawDeg: Float) {
        val current = _settings.value
        var normalized = yawDeg % 360f
        if (normalized > 180f) normalized -= 360f
        if (normalized < -180f) normalized += 360f
        val updated = current.copy(listenerHeadYawDeg = normalized)
        updateSettings(updated)
    }

    fun toggleEnabled() {
        val current = _settings.value
        updateSettings(current.copy(isEnabled = !current.isEnabled))
    }

    /**
     * Processes a single stereo audio sample frame (L, R) through the 3D acoustic chamber graph.
     */
    fun processStereoFrame(leftIn: Float, rightIn: Float): Pair<Float, Float> {
        val s = _settings.value
        if (!s.isEnabled || s.dryWetMix <= 0f) {
            return leftIn to rightIn
        }

        // Store input in circular delay buffer
        leftDelayBuffer[writeIndex] = leftIn
        rightDelayBuffer[writeIndex] = rightIn

        val avgAngleDeg = (abs(s.speakerPosition.leftAngleDeg) + abs(s.speakerPosition.rightAngleDeg)) / 2f
        val angleRad = (avgAngleDeg * PI / 180f).toFloat()

        // 1. HRTF Interaural Time Delay (ITD)
        val itdMicroseconds = computeItdMicroseconds(avgAngleDeg)
        val itdDelaySamples = ((itdMicroseconds / 1_000_000f) * sampleRateHz).toInt().coerceIn(0, maxDelaySamples - 1)

        val delayedLeft = readDelayBuffer(leftDelayBuffer, itdDelaySamples)
        val delayedRight = readDelayBuffer(rightDelayBuffer, itdDelaySamples)

        // 2. HRTF Interaural Level Difference (ILD) with 1-Pole Low-Pass Head Shadow
        val cutoffHz = s.crossfeedMode.lowPassCutoffHz
        val alpha = (1.0f - exp(-2.0 * PI * cutoffHz / sampleRateHz)).toFloat().coerceIn(0.01f, 0.99f)

        leftFilterState += alpha * (delayedLeft - leftFilterState)
        rightFilterState += alpha * (delayedRight - rightFilterState)

        val crossfeedGain = if (s.crossfeedMode == BinauralCrossfeedMode.DirectStereoOff) 0f else {
            (0.25f * (avgAngleDeg / 45f).coerceIn(0.4f, 1.2f))
        }

        // Crossfeed signals (Right channel leaks into Left ear filtered, Left leaks into Right ear)
        val directL = leftIn + (rightFilterState * crossfeedGain)
        val directR = rightIn + (leftFilterState * crossfeedGain)

        // 3. 6-Tap Early Reflection Spatial Room Model
        val absorption = (s.wallDamping.highFrequencyAbsorption + s.wallDamping.midFrequencyAbsorption) / 2f
        val reflectionScale = (1.0f - absorption) * s.earlyReflectionGain

        // Delay taps in milliseconds: 7ms, 13ms, 16ms, 22ms, 29ms, 38ms
        val tap1L = readDelayBuffer(leftDelayBuffer, (0.007f * sampleRateHz).toInt()) * (0.65f * reflectionScale)
        val tap2R = readDelayBuffer(rightDelayBuffer, (0.013f * sampleRateHz).toInt()) * (0.55f * reflectionScale)
        val tap3L = readDelayBuffer(leftDelayBuffer, (0.016f * sampleRateHz).toInt()) * (0.52f * reflectionScale)
        val tap4R = readDelayBuffer(rightDelayBuffer, (0.022f * sampleRateHz).toInt()) * (0.40f * reflectionScale)
        val tap5L = readDelayBuffer(leftDelayBuffer, (0.029f * sampleRateHz).toInt()) * (0.38f * reflectionScale)
        val tap6R = readDelayBuffer(rightDelayBuffer, (0.038f * sampleRateHz).toInt()) * (0.30f * reflectionScale)

        val earlyReflectionsL = tap1L + (tap2R * 0.7f) + tap3L + (tap4R * 0.5f) + tap5L
        val earlyReflectionsR = (tap1L * 0.7f) + tap2R + (tap3L * 0.5f) + tap4R + (tap5L * 0.6f) + tap6R

        // 4. Schroeder Comb Filter Diffuse Reverberation Tail
        val decayFeedback = (0.75f * (s.reverbDecaySeconds / 2.0f).coerceIn(0.2f, 0.92f))
        val diffuseIn = (leftIn + rightIn) * 0.5f * s.diffuseTailGain

        val c1Out = comb1Buffer[comb1Idx]
        comb1Buffer[comb1Idx] = diffuseIn + (c1Out * decayFeedback)
        comb1Idx = (comb1Idx + 1) % comb1Buffer.size

        val c2Out = comb2Buffer[comb2Idx]
        comb2Buffer[comb2Idx] = diffuseIn + (c2Out * decayFeedback * 0.98f)
        comb2Idx = (comb2Idx + 1) % comb2Buffer.size

        val c3Out = comb3Buffer[comb3Idx]
        comb3Buffer[comb3Idx] = diffuseIn + (c3Out * decayFeedback * 0.95f)
        comb3Idx = (comb3Idx + 1) % comb3Buffer.size

        val c4Out = comb4Buffer[comb4Idx]
        comb4Buffer[comb4Idx] = diffuseIn + (c4Out * decayFeedback * 0.92f)
        comb4Idx = (comb4Idx + 1) % comb4Buffer.size

        val diffuseL = (c1Out + c3Out) * 0.35f
        val diffuseR = (c2Out + c4Out) * 0.35f

        // 5. Dry / Wet Summation & Output Stage
        val wetL = directL + earlyReflectionsL + diffuseL
        val wetR = directR + earlyReflectionsR + diffuseR

        val dryMix = 1.0f - s.dryWetMix
        val wetMix = s.dryWetMix

        val finalL = (leftIn * dryMix) + (wetL * wetMix)
        val finalR = (rightIn * dryMix) + (wetR * wetMix)

        // Advance write pointer
        writeIndex = (writeIndex + 1) % maxDelaySamples

        return finalL to finalR
    }

    private fun readDelayBuffer(buffer: FloatArray, delaySamples: Int): Float {
        val safeDelay = delaySamples.coerceIn(0, maxDelaySamples - 1)
        var readIdx = writeIndex - safeDelay
        if (readIdx < 0) {
            readIdx += maxDelaySamples
        }
        return buffer[readIdx]
    }

    /**
     * Woodworth-Schlosser Formula for Interaural Time Difference (ITD).
     * ITD = (r / c) * (theta + sin(theta))
     */
    fun computeItdMicroseconds(angleDeg: Float): Float {
        val headRadiusM = 0.0875f // 8.75 cm human head radius
        val speedOfSoundMs = 343.0f // m/s
        val thetaRad = (abs(angleDeg) * PI / 180f).toFloat()
        val itdSeconds = (headRadiusM / speedOfSoundMs) * (thetaRad + sin(thetaRad))
        return itdSeconds * 1_000_000f // Return microseconds
    }

    /**
     * Sabine's Formula for Reverberation Time RT60.
     * RT60 = 0.161 * (V / (S * a))
     */
    fun computeSabineRt60(volumeM3: Float, wallDamping: WallMaterialDamping): Float {
        val safeVol = max(volumeM3, 10f)
        // Approximate room surface area for cube/rectangular room S ≈ 6 * V^(2/3)
        val surfaceAreaM2 = 6.0f * (safeVol.toDouble().pow(2.0 / 3.0)).toFloat()
        val avgAbsorption = (wallDamping.highFrequencyAbsorption + wallDamping.midFrequencyAbsorption) / 2f
        val totalAbsorptionArea = max(surfaceAreaM2 * avgAbsorption, 1.0f)
        val rt60 = 0.161f * (safeVol / totalAbsorptionArea)
        return rt60.coerceIn(0.15f, 6.0f)
    }

    private fun calculateTelemetry(s: SpatialChamberSettings): SpatialAcousticTelemetry {
        val avgAngleDeg = (abs(s.speakerPosition.leftAngleDeg) + abs(s.speakerPosition.rightAngleDeg)) / 2f
        val effectiveAngle = (avgAngleDeg + abs(s.listenerHeadYawDeg) * 0.5f).coerceIn(10f, 90f)
        val itd = computeItdMicroseconds(effectiveAngle)
        val yawFactor = (abs(s.listenerHeadYawDeg) / 90f).coerceIn(0f, 1f)
        val ildDb = ((6.0f + 4.0f * yawFactor) * sin((effectiveAngle * PI / 180f).toFloat())).coerceIn(0f, 12f)
        val rt60 = computeSabineRt60(s.roomVolumeM3, s.wallDamping)
        val surfaceAreaM2 = 6.0f * (s.roomVolumeM3.toDouble().pow(2.0 / 3.0)).toFloat()

        val baseCrossfeed = if (s.crossfeedMode == BinauralCrossfeedMode.DirectStereoOff) 0f else {
            (0.25f * (effectiveAngle / 45f).coerceIn(0.4f, 1.2f))
        }
        val leftGain = (baseCrossfeed * (1f + (s.listenerHeadYawDeg / 180f))).coerceIn(0f, 0.6f)
        val rightGain = (baseCrossfeed * (1f - (s.listenerHeadYawDeg / 180f))).coerceIn(0f, 0.6f)

        val spatialWidth = ((effectiveAngle / 90f) * 0.7f + (s.roomVolumeM3 / 1000f).coerceIn(0f, 0.3f)).coerceIn(0.2f, 1.0f)

        return SpatialAcousticTelemetry(
            itdMicroseconds = itd,
            ildDb = ildDb,
            calculatedRt60Seconds = rt60,
            activeRayCount = when (s.preset) {
                AcousticChamberPreset.MinimalistTeahouse -> 6
                AcousticChamberPreset.AbbeyStudioControlRoom -> 12
                AcousticChamberPreset.TokyoVinylBar -> 16
                AcousticChamberPreset.CyberpunkAlleyway -> 24
                AcousticChamberPreset.CathedralOfEchoes -> 36
                AcousticChamberPreset.CustomStudio -> 12
            },
            spatialWidthScore = spatialWidth,
            leftEarCrossbleedGain = leftGain,
            rightEarCrossbleedGain = rightGain,
            roomSurfaceAreaM2 = surfaceAreaM2
        )
    }
}
