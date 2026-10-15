package com.pixelody.app.core.playback

import com.pixelody.app.data.model.SoundstageRoutingMatrix
import com.pixelody.app.data.model.StemAnalogRoute
import com.pixelody.app.data.model.TactileDeckMode
import com.pixelody.app.data.model.TactileSoundstageFrame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sin

/**
 * TactileSoundstageCoordinator: Architectural bridge unifying Stems, Turntable physics,
 * Cassette tape saturation, and Phosphor Oscilloscope into a synchronized performance deck.
 */
object TactileSoundstageCoordinator {

    private val _routingMatrix = MutableStateFlow(SoundstageRoutingMatrix())
    val routingMatrix: StateFlow<SoundstageRoutingMatrix> = _routingMatrix.asStateFlow()

    private val _soundstageFrame = MutableStateFlow(TactileSoundstageFrame())
    val soundstageFrame: StateFlow<TactileSoundstageFrame> = _soundstageFrame.asStateFlow()

    fun setDeckMode(mode: TactileDeckMode) {
        _routingMatrix.value = _routingMatrix.value.copy(activeDeckMode = mode)
        _soundstageFrame.value = _soundstageFrame.value.copy(activeDeckMode = mode)
    }

    fun setStemGain(stem: String, gain: Float) {
        val clamped = gain.coerceIn(0f, 1.25f)
        val current = _soundstageFrame.value
        _soundstageFrame.value = when (stem.lowercase()) {
            "vocals" -> current.copy(vocalsGain = clamped)
            "drums" -> current.copy(drumsGain = clamped)
            "bass" -> current.copy(bassGain = clamped)
            "other" -> current.copy(otherGain = clamped)
            else -> current
        }
    }

    fun setStemRoute(stem: String, route: StemAnalogRoute) {
        val current = _routingMatrix.value
        _routingMatrix.value = when (stem.lowercase()) {
            "vocals" -> current.copy(vocalRoute = route)
            "drums" -> current.copy(drumsRoute = route)
            "bass" -> current.copy(bassRoute = route)
            "other" -> current.copy(otherRoute = route)
            else -> current
        }
    }

    fun setSaturationDrive(drive: Float) {
        _routingMatrix.value = _routingMatrix.value.copy(analogSaturationDrive = drive.coerceIn(0f, 1f))
    }

    fun setVinylFrictionWear(wear: Float) {
        _routingMatrix.value = _routingMatrix.value.copy(vinylFrictionWear = wear.coerceIn(0f, 1f))
    }

    /**
     * Applies a 1-tap instant performance macro across the entire soundstage.
     */
    fun applyPerformanceMacro(macro: String) {
        when (macro.lowercase()) {
            "acapella" -> {
                _soundstageFrame.value = _soundstageFrame.value.copy(
                    vocalsGain = 1.0f,
                    drumsGain = 0.0f,
                    bassGain = 0.0f,
                    otherGain = 0.0f,
                    isMacroActive = true,
                    activeMacroName = "ACAPELLA"
                )
            }
            "instrumental" -> {
                _soundstageFrame.value = _soundstageFrame.value.copy(
                    vocalsGain = 0.0f,
                    drumsGain = 1.0f,
                    bassGain = 1.0f,
                    otherGain = 1.0f,
                    isMacroActive = true,
                    activeMacroName = "INSTRUMENTAL"
                )
            }
            "tapewarmth" -> {
                _soundstageFrame.value = _soundstageFrame.value.copy(
                    vocalsGain = 1.0f,
                    drumsGain = 1.0f,
                    bassGain = 1.0f,
                    otherGain = 1.0f,
                    isMacroActive = true,
                    activeMacroName = "TAPE WARMTH"
                )
                _routingMatrix.value = _routingMatrix.value.copy(
                    vocalRoute = StemAnalogRoute.TapeSaturation,
                    otherRoute = StemAnalogRoute.TapeSaturation,
                    analogSaturationDrive = 0.70f
                )
            }
            "lofivinyl" -> {
                _soundstageFrame.value = _soundstageFrame.value.copy(
                    vocalsGain = 0.90f,
                    drumsGain = 0.85f,
                    bassGain = 0.85f,
                    otherGain = 0.75f,
                    isMacroActive = true,
                    activeMacroName = "LO-FI VINYL"
                )
                _routingMatrix.value = _routingMatrix.value.copy(
                    vocalRoute = StemAnalogRoute.VinylWarp,
                    drumsRoute = StemAnalogRoute.VinylWarp,
                    vinylFrictionWear = 0.65f
                )
            }
            "pureclean", "reset" -> {
                _soundstageFrame.value = _soundstageFrame.value.copy(
                    vocalsGain = 1.0f,
                    drumsGain = 1.0f,
                    bassGain = 1.0f,
                    otherGain = 1.0f,
                    isMacroActive = false,
                    activeMacroName = null
                )
                _routingMatrix.value = SoundstageRoutingMatrix()
            }
        }
    }

    /**
     * Updates real-time synthesized DSP telemetry values for the active frame.
     */
    fun updateFrameTelemetry(
        turntableRpm: Float = 33.33f,
        turntableScratch: Float = 0f,
        tapeWowFlutter: Float = 1.0f,
        tapeSaturationRms: Float = 0f,
        lissajousX: Float = 0f,
        lissajousY: Float = 0f
    ) {
        _soundstageFrame.value = _soundstageFrame.value.copy(
            turntableRpm = turntableRpm,
            turntableScratchOffset = turntableScratch,
            tapeWowFlutterFactor = tapeWowFlutter,
            tapeSaturationRms = tapeSaturationRms,
            phosphorLissajousX = lissajousX,
            phosphorLissajousY = lissajousY
        )
    }
}
