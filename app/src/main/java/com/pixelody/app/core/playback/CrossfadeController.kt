package com.pixelody.app.core.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

enum class CrossfadeMode(val label: String, val durationSeconds: Int, val description: String) {
    Off("Off", 0, "No overlap between tracks"),
    Smooth3s("3s Smooth", 3, "Subtle 3-second blend for general listening"),
    EqualPower5s("5s Equal Power", 5, "Standard 5s psychoacoustic constant energy mix"),
    ClubBlend8s("8s Club", 8, "Deep 8-second DJ transition for continuous flow"),
    Extended12s("12s Extended", 12, "Long ambient morph between tracks")
}

data class CrossfadeState(
    val mode: CrossfadeMode = CrossfadeMode.Off,
    val isCrossfading: Boolean = false,
    val progress: Float = 0f,
    val outgoingGain: Float = 1.0f,
    val incomingGain: Float = 1.0f
)

/**
 * CrossfadeController: Calculates equal-power and linear gain curves for DJ transitions
 * and manages transition timers and volume fades between tracks.
 */
class CrossfadeController(
    private val scope: CoroutineScope,
    private val onGainUpdate: (outgoingGain: Float, incomingGain: Float) -> Unit = { _, _ -> }
) {
    private val _state = MutableStateFlow(CrossfadeState())
    val state: StateFlow<CrossfadeState> = _state.asStateFlow()

    private var transitionJob: Job? = null

    fun setMode(mode: CrossfadeMode) {
        transitionJob?.cancel()
        _state.value = _state.value.copy(
            mode = mode,
            isCrossfading = false,
            progress = 0f,
            outgoingGain = 1.0f,
            incomingGain = 1.0f
        )
        onGainUpdate(1.0f, 1.0f)
    }

    /**
     * Executes a crossfade transition over the configured mode duration.
     */
    fun startTransition(onComplete: () -> Unit = {}) {
        val currentMode = _state.value.mode
        if (currentMode == CrossfadeMode.Off || currentMode.durationSeconds <= 0) {
            onGainUpdate(1.0f, 1.0f)
            onComplete()
            return
        }

        transitionJob?.cancel()
        transitionJob = scope.launch {
            val totalDurationMs = currentMode.durationSeconds * 1000L
            val stepMs = 50L
            val totalSteps = (totalDurationMs / stepMs).coerceAtLeast(1L)

            _state.value = _state.value.copy(
                isCrossfading = true,
                progress = 0f
            )

            for (step in 0..totalSteps) {
                val progress = (step.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)
                val (outGain, inGain) = calculateGains(progress, currentMode)

                _state.value = _state.value.copy(
                    progress = progress,
                    outgoingGain = outGain,
                    incomingGain = inGain
                )
                onGainUpdate(outGain, inGain)

                if (step < totalSteps) {
                    delay(stepMs)
                }
            }

            _state.value = _state.value.copy(
                isCrossfading = false,
                progress = 1.0f,
                outgoingGain = 0f,
                incomingGain = 1.0f
            )
            onGainUpdate(0f, 1.0f)
            onComplete()
        }
    }

    fun cancelTransition() {
        transitionJob?.cancel()
        _state.value = _state.value.copy(
            isCrossfading = false,
            progress = 0f,
            outgoingGain = 1.0f,
            incomingGain = 1.0f
        )
        onGainUpdate(1.0f, 1.0f)
    }

    companion object {
        /**
         * Calculates equal-power gains so that (outGain^2 + inGain^2 = 1.0),
         * preventing volume dips in the middle of track transitions.
         */
        fun calculateGains(progress: Float, mode: CrossfadeMode): Pair<Float, Float> {
            val p = progress.coerceIn(0f, 1f)
            return when (mode) {
                CrossfadeMode.Off -> 1.0f to 1.0f
                CrossfadeMode.Smooth3s -> {
                    val outGain = (1f - p)
                    val inGain = p
                    outGain to inGain
                }
                CrossfadeMode.EqualPower5s,
                CrossfadeMode.ClubBlend8s,
                CrossfadeMode.Extended12s -> {
                    val angle = p * (Math.PI.toFloat() / 2f)
                    val outGain = cos(angle.toDouble()).toFloat().coerceIn(0f, 1f)
                    val inGain = sin(angle.toDouble()).toFloat().coerceIn(0f, 1f)
                    outGain to inGain
                }
            }
        }
    }
}
