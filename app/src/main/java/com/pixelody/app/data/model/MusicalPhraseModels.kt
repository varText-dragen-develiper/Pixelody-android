package com.pixelody.app.data.model

/**
 * Structural zones within a musical track for phrase-aligned mixing.
 */
enum class PhraseZone(val displayName: String) {
    Intro("Intro (Bars 1-16)"),
    Build("Build-Up"),
    Drop("Drop / Climax"),
    MainBody("Main Progression"),
    Breakdown("Breakdown"),
    Outro("Outro (Fade Zone)")
}

/**
 * High-precision musical phrase structure computed from BPM and track duration.
 */
data class MusicalPhraseStructure(
    val bpm: Float,
    val barDurationMs: Long,
    val beatsPerBar: Int = 4,
    val phraseLengthBars: Int = 16,
    val totalBars: Int,
    val introEndMs: Long,
    val outroStartMs: Long,
    val phraseMarkersMs: List<Long>,
    val phraseZone: PhraseZone = PhraseZone.MainBody
)

/**
 * Phase alignment result for Deck A -> Deck B downbeat synchronization.
 */
data class PhaseAlignmentResult(
    val leadInOffsetMs: Long,
    val targetDownbeatMs: Long,
    val barsUntilNextPhrase: Int,
    val beatsUntilNextBar: Int,
    val isAligned: Boolean,
    val recommendedTransitionBars: Int
)
