package com.pixelody.app.core.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ln
import kotlin.math.max

enum class SleepTimerMode(val label: String, val durationMinutes: Int?) {
    Off("Off", null),
    FifteenMinutes("15 min", 15),
    ThirtyMinutes("30 min", 30),
    FortyFiveMinutes("45 min", 45),
    SixtyMinutes("60 min", 60),
    EndOfTrack("End of Track", null)
}

data class SleepTimerState(
    val active: Boolean = false,
    val mode: SleepTimerMode = SleepTimerMode.Off,
    val remainingSeconds: Long = 0L,
    val totalSeconds: Long = 0L,
    val isFading: Boolean = false,
    val volumeScale: Float = 1.0f
) {
    val formattedRemaining: String
        get() {
            if (!active) return ""
            if (mode == SleepTimerMode.EndOfTrack) return "End of Track"
            val m = remainingSeconds / 60
            val s = remainingSeconds % 60
            return String.format("%d:%02d", m, s)
        }
}

class SleepTimerController(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    private val onTimerExpired: () -> Unit = {},
    private val onVolumeScaleChange: (Float) -> Unit = {}
) {
    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private val fadeDurationSeconds = 60L

    fun setTimer(mode: SleepTimerMode, trackRemainingSeconds: Long = 0L) {
        timerJob?.cancel()
        if (mode == SleepTimerMode.Off) {
            _state.value = SleepTimerState()
            onVolumeScaleChange(1.0f)
            return
        }

        val totalSecs = when (mode) {
            SleepTimerMode.FifteenMinutes -> 15 * 60L
            SleepTimerMode.ThirtyMinutes -> 30 * 60L
            SleepTimerMode.FortyFiveMinutes -> 45 * 60L
            SleepTimerMode.SixtyMinutes -> 60 * 60L
            SleepTimerMode.EndOfTrack -> max(1L, trackRemainingSeconds)
            SleepTimerMode.Off -> 0L
        }

        _state.value = SleepTimerState(
            active = true,
            mode = mode,
            remainingSeconds = totalSecs,
            totalSeconds = totalSecs,
            isFading = false,
            volumeScale = 1.0f
        )
        onVolumeScaleChange(1.0f)

        timerJob = scope.launch {
            var remaining = totalSecs
            while (isActive && remaining > 0) {
                delay(1000L)
                remaining--
                val isFading = remaining in 1..fadeDurationSeconds
                val volumeScale = if (isFading) {
                    calculateLogarithmicFade(remaining, fadeDurationSeconds)
                } else if (remaining <= 0) {
                    0.0f
                } else {
                    1.0f
                }

                _state.value = _state.value.copy(
                    remainingSeconds = remaining,
                    isFading = isFading,
                    volumeScale = volumeScale
                )
                onVolumeScaleChange(volumeScale)
            }

            if (isActive && remaining <= 0) {
                _state.value = _state.value.copy(
                    active = false,
                    isFading = false,
                    volumeScale = 0.0f
                )
                onVolumeScaleChange(0.0f)
                onTimerExpired()
            }
        }
    }

    fun addFiveMinutes() {
        if (!_state.value.active) return
        val current = _state.value.remainingSeconds
        val updated = current + 300L
        _state.value = _state.value.copy(
            remainingSeconds = updated,
            totalSeconds = max(_state.value.totalSeconds, updated),
            isFading = updated <= fadeDurationSeconds,
            volumeScale = if (updated > fadeDurationSeconds) 1.0f else calculateLogarithmicFade(updated, fadeDurationSeconds)
        )
    }

    fun cancel() {
        timerJob?.cancel()
        timerJob = null
        _state.value = SleepTimerState()
        onVolumeScaleChange(1.0f)
    }

    companion object {
        fun calculateLogarithmicFade(remainingSeconds: Long, fadeDurationSeconds: Long): Float {
            if (remainingSeconds <= 0) return 0f
            if (remainingSeconds >= fadeDurationSeconds) return 1f
            val ratio = remainingSeconds.toFloat() / fadeDurationSeconds.toFloat()
            // Logarithmic human auditory perception curve
            return (ln(1.0 + 9.0 * ratio) / ln(10.0)).toFloat().coerceIn(0f, 1f)
        }
    }
}
