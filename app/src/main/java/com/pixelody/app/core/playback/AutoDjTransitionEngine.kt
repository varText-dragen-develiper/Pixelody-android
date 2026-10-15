package com.pixelody.app.core.playback

import com.pixelody.app.data.model.AutoDjSettings
import com.pixelody.app.data.model.DjEnvelopeFrame
import com.pixelody.app.data.model.DjTransitionCurve
import com.pixelody.app.data.model.MultibandDspFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * AutoDjTransitionEngine: High-precision real-time DSP envelope generator
 * for multi-curve DJ mixing (Equal-Power, Bass Swap, Filter Sweep, Echo Out, Vinyl Brake).
 */
class AutoDjTransitionEngine(
    private val scope: CoroutineScope,
    private val onDspUpdate: (DjEnvelopeFrame) -> Unit = {}
) {
    private val _settings = MutableStateFlow(AutoDjSettings())
    val settings: StateFlow<AutoDjSettings> = _settings.asStateFlow()

    private val _envelope = MutableStateFlow(DjEnvelopeFrame())
    val envelope: StateFlow<DjEnvelopeFrame> = _envelope.asStateFlow()

    private val _multibandEnvelope = MutableStateFlow(
        MultibandCrossfaderEngine.calculateMultibandFrame(0f, DjTransitionCurve.EqualPower)
    )
    val multibandEnvelope: StateFlow<MultibandDspFrame> = _multibandEnvelope.asStateFlow()

    private var transitionJob: Job? = null

    fun updateSettings(newSettings: AutoDjSettings) {
        _settings.value = newSettings
    }

    fun setPreferredCurve(curve: DjTransitionCurve) {
        _settings.value = _settings.value.copy(preferredCurve = curve)
    }

    fun setDuration(seconds: Int) {
        _settings.value = _settings.value.copy(transitionDurationSeconds = seconds.coerceIn(1, 30))
    }

    fun toggleAutoDj(enabled: Boolean) {
        _settings.value = _settings.value.copy(isAutoDjEnabled = enabled)
    }

    /**
     * Executes a multi-curve DJ transition over the configured or specified duration.
     */
    fun triggerTransition(
        durationSeconds: Int? = null,
        curve: DjTransitionCurve? = null,
        onHandoffPoint: () -> Unit = {},
        onComplete: () -> Unit = {}
    ) {
        val currentSettings = _settings.value
        val effectiveDuration = durationSeconds ?: currentSettings.transitionDurationSeconds
        val effectiveCurve = curve ?: currentSettings.preferredCurve

        transitionJob?.cancel()
        transitionJob = scope.launch {
            val totalDurationMs = (effectiveDuration * 1000L).coerceAtLeast(500L)
            val stepIntervalMs = 40L
            val totalSteps = (totalDurationMs / stepIntervalMs).coerceAtLeast(1L)
            var handoffTriggered = false

            _envelope.value = _envelope.value.copy(isTransitioning = true, progress = 0f)

            for (step in 0..totalSteps) {
                val progress = (step.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)
                val frame = calculateEnvelopeFrame(progress, effectiveCurve).copy(isTransitioning = true)
                val multibandFrame = MultibandCrossfaderEngine.calculateMultibandFrame(progress, effectiveCurve)

                _envelope.value = frame
                _multibandEnvelope.value = multibandFrame
                onDspUpdate(frame)

                // Handoff point typically at midpoint (50% progress) for track advancing
                if (!handoffTriggered && progress >= 0.5f) {
                    handoffTriggered = true
                    onHandoffPoint()
                }

                if (step < totalSteps) {
                    delay(stepIntervalMs)
                }
            }

            val finalFrame = DjEnvelopeFrame(
                progress = 1.0f,
                outgoingGain = 0f,
                incomingGain = 1.0f,
                outgoingHighPassCutoffHz = 20f,
                incomingLowPassCutoffHz = 20000f,
                echoWetLevel = 0f,
                playbackSpeedFactor = 1.0f,
                isTransitioning = false
            )
            _envelope.value = finalFrame
            _multibandEnvelope.value = MultibandCrossfaderEngine.calculateMultibandFrame(1.0f, effectiveCurve)
            onDspUpdate(finalFrame)

            if (!handoffTriggered) {
                onHandoffPoint()
            }
            onComplete()
        }
    }

    fun cancelTransition() {
        transitionJob?.cancel()
        val resetFrame = DjEnvelopeFrame(
            progress = 0f,
            outgoingGain = 1.0f,
            incomingGain = 0f,
            outgoingHighPassCutoffHz = 20f,
            incomingLowPassCutoffHz = 20000f,
            echoWetLevel = 0f,
            playbackSpeedFactor = 1.0f,
            isTransitioning = false
        )
        _envelope.value = resetFrame
        _multibandEnvelope.value = MultibandCrossfaderEngine.calculateMultibandFrame(0f, _settings.value.preferredCurve)
        onDspUpdate(resetFrame)
    }

    companion object {
        private const val PI_HALF = (Math.PI / 2.0).toFloat()

        /**
         * Pure mathematical envelope computation for any transition curve at a given progress (0.0 to 1.0).
         */
        fun calculateEnvelopeFrame(progress: Float, curve: DjTransitionCurve): DjEnvelopeFrame {
            val p = progress.coerceIn(0f, 1f)

            return when (curve) {
                DjTransitionCurve.EqualPower -> {
                    val angle = p * PI_HALF
                    val outGain = cos(angle.toDouble()).toFloat().coerceIn(0f, 1f)
                    val inGain = sin(angle.toDouble()).toFloat().coerceIn(0f, 1f)

                    DjEnvelopeFrame(
                        progress = p,
                        outgoingGain = outGain,
                        incomingGain = inGain,
                        outgoingHighPassCutoffHz = 20f,
                        incomingLowPassCutoffHz = 20000f,
                        echoWetLevel = 0f,
                        playbackSpeedFactor = 1.0f
                    )
                }

                DjTransitionCurve.BassSwap -> {
                    val angle = p * PI_HALF
                    val outGain = cos(angle.toDouble()).toFloat().coerceIn(0f, 1f)
                    val inGain = sin(angle.toDouble()).toFloat().coerceIn(0f, 1f)

                    // Sharp low-end crossover at 50% progress
                    val outHighPass = if (p < 0.5f) 20f else 320f + (p - 0.5f) * 600f
                    val inLowPass = 20000f

                    DjEnvelopeFrame(
                        progress = p,
                        outgoingGain = outGain,
                        incomingGain = inGain,
                        outgoingHighPassCutoffHz = outHighPass,
                        incomingLowPassCutoffHz = inLowPass,
                        echoWetLevel = 0f,
                        playbackSpeedFactor = 1.0f
                    )
                }

                DjTransitionCurve.FilterSweep -> {
                    // Exponential high-pass sweep (20Hz -> 3500Hz)
                    val outHpCutoff = (20f * (3500f / 20f).pow(p)).coerceIn(20f, 3500f)
                    val outGain = (1f - p).pow(1.2f).coerceIn(0f, 1f)

                    // Incoming track opens up from warm lows to full spectrum
                    val inLpCutoff = (600f * (20000f / 600f).pow(p)).coerceIn(600f, 20000f)
                    val inGain = p.pow(0.85f).coerceIn(0f, 1f)

                    DjEnvelopeFrame(
                        progress = p,
                        outgoingGain = outGain,
                        incomingGain = inGain,
                        outgoingHighPassCutoffHz = outHpCutoff,
                        incomingLowPassCutoffHz = inLpCutoff,
                        echoWetLevel = 0f,
                        playbackSpeedFactor = 1.0f
                    )
                }

                DjTransitionCurve.EchoOut -> {
                    val outGain: Float
                    val echoWet: Float
                    val inGain: Float

                    if (p < 0.35f) {
                        outGain = 1.0f
                        echoWet = 0.0f
                        inGain = (p / 0.35f) * 0.3f
                    } else {
                        val decayProgress = (p - 0.35f) / 0.65f
                        outGain = 0f
                        echoWet = cos(decayProgress * PI_HALF.toDouble()).toFloat() * 0.85f
                        inGain = 0.3f + (0.7f * decayProgress)
                    }

                    DjEnvelopeFrame(
                        progress = p,
                        outgoingGain = outGain,
                        incomingGain = inGain.coerceIn(0f, 1f),
                        outgoingHighPassCutoffHz = 20f,
                        incomingLowPassCutoffHz = 20000f,
                        echoWetLevel = echoWet,
                        playbackSpeedFactor = 1.0f
                    )
                }

                DjTransitionCurve.VinylBrake -> {
                    // Turntable motor stop deceleration
                    val speedFactor = if (p < 0.85f) {
                        val decelProgress = p / 0.85f
                        (1.0f - decelProgress.pow(1.7f)).coerceIn(0.08f, 1.0f)
                    } else {
                        1.0f // Reset for incoming track
                    }

                    val outGain = if (p < 0.85f) (1f - (p / 0.85f)).pow(0.6f) else 0f
                    val inGain = if (p < 0.80f) 0f else ((p - 0.80f) / 0.20f).coerceIn(0f, 1f)

                    DjEnvelopeFrame(
                        progress = p,
                        outgoingGain = outGain.coerceIn(0f, 1f),
                        incomingGain = inGain,
                        outgoingHighPassCutoffHz = 20f,
                        incomingLowPassCutoffHz = 20000f,
                        echoWetLevel = 0f,
                        playbackSpeedFactor = speedFactor
                    )
                }
            }
        }
    }
}
