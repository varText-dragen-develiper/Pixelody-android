package com.pixelody.app.data.model

/**
 * Crossover boundary frequencies and DSP parameters for 3-band frequency-split mixing.
 */
data class MultibandCrossoverProfile(
    val lowCrossoverHz: Float = 180f,
    val highCrossoverHz: Float = 2500f,
    val vocalDuckingDb: Float = -4.0f,
    val bassSwapThreshold: Float = 0.50f,
    val highPassSweepStartHz: Float = 20f,
    val highPassSweepEndHz: Float = 3500f
)

/**
 * Real-time 3-band frequency-split DSP gains and crossover states for Deck A & Deck B.
 */
data class MultibandDspFrame(
    val progress: Float = 0f,
    val deckALowGain: Float = 1.0f,
    val deckAMidGain: Float = 1.0f,
    val deckAHighGain: Float = 1.0f,
    val deckBLowGain: Float = 0.0f,
    val deckBMidGain: Float = 0.0f,
    val deckBHighGain: Float = 0.0f,
    val isBassSwapped: Boolean = false,
    val isVocalDucked: Boolean = false,
    val highPassFilterHz: Float = 20f,
    val echoDecayGain: Float = 0f,
    val tempoStretchFactor: Float = 1.0f,
    val activeCurve: DjTransitionCurve = DjTransitionCurve.EqualPower
) {
    val deckAOverallRms: Float
        get() = (deckALowGain * 0.4f + deckAMidGain * 0.4f + deckAHighGain * 0.2f).coerceIn(0f, 1f)

    val deckBOverallRms: Float
        get() = (deckBLowGain * 0.4f + deckBMidGain * 0.4f + deckBHighGain * 0.2f).coerceIn(0f, 1f)
}
