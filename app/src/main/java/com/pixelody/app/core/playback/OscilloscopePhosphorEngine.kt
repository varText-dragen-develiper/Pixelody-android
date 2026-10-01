package com.pixelody.app.core.playback

import com.pixelody.app.data.model.CrtBeamPersistence
import com.pixelody.app.data.model.CrtPhosphorType
import com.pixelody.app.data.model.OscilloscopeDisplayMode
import com.pixelody.app.data.model.OscilloscopeSettings
import com.pixelody.app.data.model.OscilloscopeTelemetry
import com.pixelody.app.data.model.VectorPoint3D
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * OscilloscopePhosphorEngine: Real-Time Vector Laser & CRT Phosphor DSP Engine.
 * Transforms incoming stereo audio streams into vector coordinate trajectories for Lissajous X-Y plotting,
 * 45° circular goniometers, 3D rotatable Euler vector wireframes, logarithmic spiral flowers,
 * and calibrated dual-trace time sweeps with real-time stereoscopic phase coherence analysis.
 */
class OscilloscopePhosphorEngine(
    initialSettings: OscilloscopeSettings = OscilloscopeSettings(),
    private val sampleRateHz: Int = 48000
) {
    private val _settings = MutableStateFlow(initialSettings)
    val settings: StateFlow<OscilloscopeSettings> = _settings.asStateFlow()

    private val _telemetry = MutableStateFlow(calculateInitialTelemetry(initialSettings))
    val telemetry: StateFlow<OscilloscopeTelemetry> = _telemetry.asStateFlow()

    // Circular audio sample history ring buffers for vector generation (1024 samples)
    private val bufferSize = 1024
    private val leftHistory = FloatArray(bufferSize)
    private val rightHistory = FloatArray(bufferSize)
    private var writeIndex = 0

    // Running sample counter
    private var sampleCounter = 0L

    fun updateSettings(newSettings: OscilloscopeSettings) {
        _settings.value = newSettings
        refreshTelemetry()
    }

    fun setDisplayMode(mode: OscilloscopeDisplayMode) {
        val current = _settings.value
        updateSettings(current.copy(displayMode = mode))
    }

    fun setPhosphorType(phosphor: CrtPhosphorType) {
        val current = _settings.value
        updateSettings(current.copy(phosphorType = phosphor))
    }

    fun setPersistence(persistence: CrtBeamPersistence) {
        val current = _settings.value
        updateSettings(current.copy(persistence = persistence))
    }

    fun setSensitivityGain(gain: Float) {
        val current = _settings.value
        updateSettings(current.copy(sensitivityGain = gain.coerceIn(0.1f, 4.0f)))
    }

    fun update3DRotation(pitchDeg: Float, yawDeg: Float) {
        val current = _settings.value
        val geo = current.beamGeometry
        updateSettings(
            current.copy(
                beamGeometry = geo.copy(
                    eulerPitchDeg = pitchDeg.coerceIn(-90f, 90f),
                    eulerYawDeg = yawDeg % 360f
                )
            )
        )
    }

    fun setPhaseRotationDeg(rotDeg: Float) {
        val current = _settings.value
        val geo = current.beamGeometry
        updateSettings(
            current.copy(
                beamGeometry = geo.copy(audioPhaseRotationDeg = rotDeg % 360f)
            )
        )
    }

    fun toggleEnabled() {
        val current = _settings.value
        updateSettings(current.copy(isEnabled = !current.isEnabled))
    }

    /**
     * Ingests a single audio frame and updates the circular history buffer.
     */
    fun processStereoFrame(leftIn: Float, rightIn: Float): Pair<Float, Float> {
        leftHistory[writeIndex] = leftIn
        rightHistory[writeIndex] = rightIn
        writeIndex = (writeIndex + 1) % bufferSize
        sampleCounter++

        if (sampleCounter % 512L == 0L) {
            refreshTelemetry()
        }

        return leftIn to rightIn
    }

    /**
     * Batch processes an array of stereo samples into history buffer.
     */
    fun processStereoBuffer(leftChannel: FloatArray, rightChannel: FloatArray) {
        val count = min(leftChannel.size, rightChannel.size)
        for (i in 0 until count) {
            processStereoFrame(leftChannel[i], rightChannel[i])
        }
    }

    /**
     * Generates a list of 2D/3D screen space vector points for the active display mode.
     */
    fun generateVectorPoints(width: Float, height: Float, pointCount: Int = 384): List<VectorPoint3D> {
        val s = _settings.value
        val points = ArrayList<VectorPoint3D>(pointCount)
        val gain = s.sensitivityGain
        val geo = s.beamGeometry

        val centerX = width / 2f
        val centerY = height / 2f
        val maxRadius = min(width, height) * 0.44f

        val step = max(1, bufferSize / pointCount)
        val radOffset = (geo.audioPhaseRotationDeg * PI / 180.0).toFloat()

        when (s.displayMode) {
            OscilloscopeDisplayMode.Lissajous_XY -> {
                for (i in 0 until pointCount) {
                    val bufIdx = (writeIndex - (i * step) + bufferSize) % bufferSize
                    val rawL = leftHistory[bufIdx] * gain
                    val rawR = rightHistory[bufIdx] * gain

                    // Phase plane rotation
                    val rotL = (rawL * cos(radOffset) - rawR * sin(radOffset))
                    val rotR = (rawL * sin(radOffset) + rawR * cos(radOffset))

                    val screenX = centerX + (rotL * maxRadius).coerceIn(-maxRadius, maxRadius)
                    val screenY = centerY - (rotR * maxRadius).coerceIn(-maxRadius, maxRadius)
                    val intensity = (1.0f - (i.toFloat() / pointCount.toFloat()) * (1.0f - s.persistence.trailAlphaFactor)).coerceIn(0.1f, 1.0f)

                    points.add(VectorPoint3D(screenX, screenY, 0f, intensity))
                }
            }

            OscilloscopeDisplayMode.CircularGoniometer -> {
                val sqrt2Inv = 0.70710678f
                for (i in 0 until pointCount) {
                    val bufIdx = (writeIndex - (i * step) + bufferSize) % bufferSize
                    val l = leftHistory[bufIdx] * gain
                    val r = rightHistory[bufIdx] * gain

                    // 45° Goniometer transform (X = Side / L-R, Y = Mid / L+R)
                    val side = (l - r) * sqrt2Inv
                    val mid = (l + r) * sqrt2Inv

                    val screenX = centerX + (side * maxRadius).coerceIn(-maxRadius, maxRadius)
                    val screenY = centerY - (mid * maxRadius).coerceIn(-maxRadius, maxRadius)
                    val intensity = (1.0f - (i.toFloat() / pointCount.toFloat()) * (1.0f - s.persistence.trailAlphaFactor)).coerceIn(0.1f, 1.0f)

                    points.add(VectorPoint3D(screenX, screenY, 0f, intensity))
                }
            }

            OscilloscopeDisplayMode.StereoWaveform_Dual -> {
                // Top half: Left Channel, Bottom half: Right Channel
                for (i in 0 until pointCount) {
                    val bufIdx = (writeIndex - (i * step) + bufferSize) % bufferSize
                    val frac = (i.toFloat() / pointCount.toFloat())
                    val screenX = width * frac

                    val l = leftHistory[bufIdx] * gain
                    val r = rightHistory[bufIdx] * gain

                    val topY = (height * 0.28f) - (l * height * 0.18f).coerceIn(-height * 0.22f, height * 0.22f)
                    val botY = (height * 0.72f) - (r * height * 0.18f).coerceIn(-height * 0.22f, height * 0.22f)

                    points.add(VectorPoint3D(screenX, topY, 0f, 1.0f))
                    points.add(VectorPoint3D(screenX, botY, 0f, 0.85f))
                }
            }

            OscilloscopeDisplayMode.VectorLaserSynth_3D -> {
                val pitchRad = (geo.eulerPitchDeg * PI / 180.0).toFloat()
                val yawRad = (geo.eulerYawDeg * PI / 180.0).toFloat()

                val cosP = cos(pitchRad)
                val sinP = sin(pitchRad)
                val cosY = cos(yawRad)
                val sinY = sin(yawRad)

                for (i in 0 until pointCount) {
                    val bufIdx = (writeIndex - (i * step) + bufferSize) % bufferSize
                    val frac = (i.toFloat() / pointCount.toFloat())
                    val rawL = leftHistory[bufIdx] * gain
                    val rawR = rightHistory[bufIdx] * gain

                    // 3D coordinates in normalized space [-1, 1]
                    val x0 = rawL * 0.85f
                    val y0 = rawR * 0.85f
                    val z0 = (sin(frac * 2.0 * PI).toFloat() * 0.4f * geo.zModulationDepth)

                    // 3D Euler rotation (Yaw around Y, Pitch around X)
                    val x1 = (x0 * cosY + z0 * sinY)
                    val z1 = (-x0 * sinY + z0 * cosY)
                    val y1 = (y0 * cosP - z1 * sinP)
                    val z2 = (y0 * sinP + z1 * cosP)

                    // Perspective projection
                    val dist = 2.2f
                    val fov = 1.0f / (dist + z2 * 0.5f)

                    val screenX = centerX + (x1 * maxRadius * fov * 1.8f)
                    val screenY = centerY - (y1 * maxRadius * fov * 1.8f)
                    val intensity = (0.35f + fov * 0.65f).coerceIn(0.1f, 1.0f)

                    points.add(VectorPoint3D(screenX, screenY, z2, intensity))
                }
            }

            OscilloscopeDisplayMode.CRT_VectorSpirals -> {
                for (i in 0 until pointCount) {
                    val bufIdx = (writeIndex - (i * step) + bufferSize) % bufferSize
                    val frac = i.toFloat() / pointCount.toFloat()
                    val l = leftHistory[bufIdx] * gain
                    val r = rightHistory[bufIdx] * gain

                    val radius = (frac.toDouble().pow(0.55) * maxRadius * (0.25f + 0.75f * abs(l))).toFloat()
                    val theta = (frac * 6.0 * PI + (r * PI * 0.5f) + radOffset).toFloat()

                    val screenX = centerX + (radius * cos(theta))
                    val screenY = centerY + (radius * sin(theta))
                    val intensity = (1.0f - frac * 0.5f).coerceIn(0.2f, 1.0f)

                    points.add(VectorPoint3D(screenX, screenY, 0f, intensity))
                }
            }

            OscilloscopeDisplayMode.AudioMatrixPolar -> {
                for (i in 0 until pointCount) {
                    val bufIdx = (writeIndex - (i * step) + bufferSize) % bufferSize
                    val theta = (i.toFloat() / pointCount.toFloat()) * (2.0 * PI).toFloat() + radOffset
                    val l = leftHistory[bufIdx] * gain
                    val r = rightHistory[bufIdx] * gain

                    val envelope = (sqrt(l * l + r * r) * maxRadius).coerceIn(4f, maxRadius)
                    val screenX = centerX + (envelope * cos(theta))
                    val screenY = centerY + (envelope * sin(theta))

                    points.add(VectorPoint3D(screenX, screenY, 0f, 1.0f))
                }
            }
        }

        return points
    }

    /**
     * Calculates real-time phase correlation, center/side energy ratios, and beam deflection.
     */
    fun computeStereoPhaseCorrelation(): Float {
        var dotSum = 0.0
        var sumSqL = 0.0
        var sumSqR = 0.0

        val window = min(bufferSize, 512)
        for (i in 0 until window) {
            val bufIdx = (writeIndex - i + bufferSize) % bufferSize
            val l = leftHistory[bufIdx].toDouble()
            val r = rightHistory[bufIdx].toDouble()

            dotSum += l * r
            sumSqL += l * l
            sumSqR += r * r
        }

        val denominator = sqrt(sumSqL * sumSqR)
        return if (denominator > 0.00001) {
            (dotSum / denominator).toFloat().coerceIn(-1.0f, 1.0f)
        } else 1.0f
    }

    private fun refreshTelemetry() {
        val s = _settings.value
        val corr = computeStereoPhaseCorrelation()

        var sumSqL = 0f
        var sumSqR = 0f
        var peak = 0f
        var midEnergy = 0f
        var sideEnergy = 0f

        for (i in 0 until bufferSize step 2) {
            val l = leftHistory[i]
            val r = rightHistory[i]
            sumSqL += l * l
            sumSqR += r * r

            val maxSample = max(abs(l), abs(r))
            if (maxSample > peak) peak = maxSample

            val mid = (l + r) * 0.707f
            val side = (l - r) * 0.707f
            midEnergy += mid * mid
            sideEnergy += side * side
        }

        val totalEnergy = max(midEnergy + sideEnergy, 0.0001f)
        val centerRatio = midEnergy / totalEnergy
        val sideRatio = sideEnergy / totalEnergy

        val rms = sqrt((sumSqL + sumSqR) / bufferSize.toFloat())
        val spread = (1.0f - abs(corr)).coerceIn(0.0f, 1.0f)

        _telemetry.value = OscilloscopeTelemetry(
            phaseCoherenceScore = corr,
            peakBeamDeflection = (peak * s.sensitivityGain).coerceIn(0.0f, 2.0f),
            stereoscopicPhaseSpread = spread,
            beamIntensityRms = (rms * s.sensitivityGain).coerceIn(0.0f, 1.5f),
            activePointCount = 384,
            lissajousEccentricity = abs(centerRatio - sideRatio),
            centerEnergyRatio = centerRatio,
            sideEnergyRatio = sideRatio
        )
    }

    private fun calculateInitialTelemetry(s: OscilloscopeSettings): OscilloscopeTelemetry {
        return OscilloscopeTelemetry(
            phaseCoherenceScore = 0.88f,
            peakBeamDeflection = 0.65f,
            stereoscopicPhaseSpread = 0.25f,
            beamIntensityRms = 0.45f,
            activePointCount = 384,
            lissajousEccentricity = 0.55f,
            centerEnergyRatio = 0.82f,
            sideEnergyRatio = 0.18f
        )
    }
}
