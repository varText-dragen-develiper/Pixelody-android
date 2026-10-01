package com.pixelody.app.core.playback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.pixelody.app.data.model.AudioHapticMode
import com.pixelody.app.data.model.AudioHapticSettings
import com.pixelody.app.data.model.HapticPrimitiveType
import com.pixelody.app.data.model.HapticPulseEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/**
 * AudioHapticResonanceEngine: Advanced audio-haptic transducer and tactile synthesis engine.
 * Converts real-time sub-bass transients, rhythmic kick downbeats, and mechanical turntable
 * textures into hardware vibration compositions using Android's VibratorManager and VibrationEffect.
 */
class AudioHapticResonanceEngine(
    private val context: Context? = null,
    initialSettings: AudioHapticSettings = AudioHapticSettings()
) {
    private val _settings = MutableStateFlow(initialSettings)
    val settings: StateFlow<AudioHapticSettings> = _settings.asStateFlow()

    private val _latestPulse = MutableStateFlow<HapticPulseEvent?>(null)
    val latestPulse: StateFlow<HapticPulseEvent?> = _latestPulse.asStateFlow()

    private var vibrator: Vibrator? = null
    private var lastPulseTimestampMs: Long = 0L
    private var previousLowEnergy: Float = 0f
    private var previousMidEnergy: Float = 0f

    init {
        initVibrator()
    }

    private fun initVibrator() {
        val ctx = context ?: return
        vibrator = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }.getOrNull()
    }

    fun updateSettings(newSettings: AudioHapticSettings) {
        _settings.value = newSettings
    }

    fun setMode(mode: AudioHapticMode) {
        _settings.value = _settings.value.copy(mode = mode)
    }

    fun setIntensity(intensity: Float) {
        _settings.value = _settings.value.copy(intensity = intensity.coerceIn(0f, 1f))
    }

    fun setSubBassBoost(boost: Float) {
        _settings.value = _settings.value.copy(subBassBoost = boost.coerceIn(0.5f, 2.0f))
    }

    fun toggleEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(isEnabled = enabled)
    }

    /**
     * Evaluates real-time low, mid, and high frequency audio band energies
     * and triggers tactile resonance pulses when transient thresholds are exceeded.
     */
    fun processAudioFrame(
        lowBandEnergy: Float,
        midBandEnergy: Float,
        highBandEnergy: Float,
        isPlaying: Boolean,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val currentSettings = _settings.value
        if (!currentSettings.isEnabled || currentSettings.mode == AudioHapticMode.Off || !isPlaying) {
            return
        }

        // Refractory window (65ms minimum interval between pulses to avoid motor buzzing fatigue)
        val refractoryMs = if (currentSettings.mode == AudioHapticMode.SubBassRumble) 80L else 65L
        if (nowMs - lastPulseTimestampMs < refractoryMs) {
            previousLowEnergy = lowBandEnergy
            previousMidEnergy = midBandEnergy
            return
        }

        val lowOnset = lowBandEnergy - previousLowEnergy
        val midOnset = midBandEnergy - previousMidEnergy

        val sensitivity = currentSettings.transientSensitivity
        val subThreshold = (0.35f * (1.1f - sensitivity)).coerceAtLeast(0.10f)
        val kickThreshold = (0.40f * (1.1f - sensitivity)).coerceAtLeast(0.12f)

        when (currentSettings.mode) {
            AudioHapticMode.SubBassRumble -> {
                val effectiveLow = lowBandEnergy * currentSettings.subBassBoost
                if (effectiveLow > subThreshold && lowOnset > 0.08f) {
                    val rawAmp = effectiveLow.coerceIn(0.2f, 1.0f)
                    triggerPulse(HapticPrimitiveType.LowTick, rawAmp, "SUB", nowMs)
                }
            }

            AudioHapticMode.BeatPunch -> {
                if (midBandEnergy > kickThreshold && midOnset > 0.12f) {
                    val rawAmp = midBandEnergy.coerceIn(0.3f, 1.0f)
                    triggerPulse(HapticPrimitiveType.Click, rawAmp, "KICK", nowMs)
                } else if (lowBandEnergy > subThreshold && lowOnset > 0.15f) {
                    val rawAmp = lowBandEnergy.coerceIn(0.25f, 1.0f)
                    triggerPulse(HapticPrimitiveType.LowTick, rawAmp, "PUNCH", nowMs)
                }
            }

            AudioHapticMode.FullSpectrum -> {
                when {
                    lowBandEnergy * currentSettings.subBassBoost > subThreshold && lowOnset > 0.12f -> {
                        val rawAmp = lowBandEnergy.coerceIn(0.3f, 1.0f)
                        triggerPulse(HapticPrimitiveType.LowTick, rawAmp, "SUB", nowMs)
                    }
                    midBandEnergy > kickThreshold && midOnset > 0.15f -> {
                        val rawAmp = (midBandEnergy * 0.85f).coerceIn(0.2f, 0.9f)
                        triggerPulse(HapticPrimitiveType.Click, rawAmp, "SNARE", nowMs)
                    }
                    highBandEnergy > 0.55f -> {
                        val rawAmp = (highBandEnergy * 0.6f).coerceIn(0.15f, 0.7f)
                        triggerPulse(HapticPrimitiveType.Tick, rawAmp, "HAT", nowMs)
                    }
                }
            }

            AudioHapticMode.VinylAcoustic -> {
                if (lowBandEnergy > subThreshold && lowOnset > 0.10f) {
                    val rawAmp = (lowBandEnergy * 0.7f).coerceIn(0.15f, 0.8f)
                    triggerPulse(HapticPrimitiveType.LowTick, rawAmp, "GROOVE", nowMs)
                }
            }

            AudioHapticMode.Off -> Unit
        }

        previousLowEnergy = lowBandEnergy
        previousMidEnergy = midBandEnergy
    }

    /**
     * Executes a tactile vibration primitive and emits a telemetry event for the UI visualizer.
     */
    fun triggerPulse(
        primitive: HapticPrimitiveType,
        amplitude: Float,
        bandLabel: String = "TACTILE",
        timestampMs: Long = System.currentTimeMillis()
    ) {
        lastPulseTimestampMs = timestampMs
        val scaledAmplitude = (amplitude * _settings.value.intensity).coerceIn(0f, 1f)
        val event = HapticPulseEvent(
            timestamp = timestampMs,
            primitive = primitive,
            amplitude = scaledAmplitude,
            bandLabel = bandLabel
        )
        _latestPulse.value = event

        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ Composition Primitives
                val composition = VibrationEffect.startComposition()
                val primitiveId = when (primitive) {
                    HapticPrimitiveType.LowTick -> VibrationEffect.Composition.PRIMITIVE_LOW_TICK
                    HapticPrimitiveType.Click -> VibrationEffect.Composition.PRIMITIVE_CLICK
                    HapticPrimitiveType.Tick -> VibrationEffect.Composition.PRIMITIVE_TICK
                    HapticPrimitiveType.QuickRise -> VibrationEffect.Composition.PRIMITIVE_QUICK_RISE
                    HapticPrimitiveType.QuickFall -> VibrationEffect.Composition.PRIMITIVE_QUICK_FALL
                    HapticPrimitiveType.Spin -> VibrationEffect.Composition.PRIMITIVE_SPIN
                }

                if (v.areAllPrimitivesSupported(primitiveId)) {
                    composition.addPrimitive(primitiveId, scaledAmplitude)
                    v.vibrate(composition.compose())
                    return
                }
            }

            // Fallback for API 26+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val durationMs = primitive.baseDurationMs
                val ampInt = (scaledAmplitude * 255).roundToInt().coerceIn(1, 255)
                v.vibrate(VibrationEffect.createOneShot(durationMs, ampInt))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(primitive.baseDurationMs)
            }
        }
    }

    /**
     * Triggers a manual preview test pulse of the given primitive.
     */
    fun testPulse(primitive: HapticPrimitiveType, intensity: Float? = null) {
        val effectiveIntensity = intensity ?: 1.0f
        triggerPulse(
            primitive = primitive,
            amplitude = effectiveIntensity.coerceIn(0.2f, 1.0f),
            bandLabel = "TEST",
            timestampMs = System.currentTimeMillis()
        )
    }

    /**
     * Tactile feedback for turntable tonearm needle landing on vinyl groove.
     */
    fun pulseTurntableNeedleDrop() {
        if (!_settings.value.turntableHapticsEnabled) return
        triggerPulse(HapticPrimitiveType.QuickFall, 0.75f, "NEEDLE")
    }

    /**
     * Tactile feedback for turntable platter brake/stop.
     */
    fun pulseTurntableBrake() {
        if (!_settings.value.turntableHapticsEnabled) return
        triggerPulse(HapticPrimitiveType.Spin, 0.65f, "BRAKE")
    }
}
