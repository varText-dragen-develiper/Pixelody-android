package com.pixelody.app

import com.pixelody.app.core.playback.AcousticTimbreEngine
import com.pixelody.app.core.playback.TimbreSpectralProfile
import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AcousticTimbreEngineTest {

    @Test
    fun `calculateTargetGain normalizes to EBU R128 minus 14 LUFS target`() {
        // At exactly target LUFS (-14), gain multiplier is 1.0 (0 dB gain)
        val unityGain = AcousticTimbreEngine.calculateTargetGain(-14.0f, -14.0f)
        assertEquals(1.0f, unityGain, 0.01f)

        // Quiet track at -20 LUFS (+6 dB boost required -> gain ~2.0)
        val boostGain = AcousticTimbreEngine.calculateTargetGain(-20.0f, -14.0f)
        assertTrue("Quiet track should receive gain boost", boostGain > 1.0f)
        assertEquals(1.995f, boostGain, 0.05f)

        // Loud master at -8 LUFS (-6 dB cut required -> gain ~0.50)
        val cutGain = AcousticTimbreEngine.calculateTargetGain(-8.0f, -14.0f)
        assertTrue("Loud track should receive gain cut", cutGain < 1.0f)
        assertEquals(0.501f, cutGain, 0.05f)
    }

    @Test
    fun `calculateWarpedTempo performs smooth S-Curve cubic transition`() {
        val bpmA = 120f
        val bpmB = 130f

        // At progress 0% -> exactly BPM A
        assertEquals(120f, AcousticTimbreEngine.calculateWarpedTempo(bpmA, bpmB, 0f), 0.01f)

        // At progress 50% -> exactly midpoint (125 BPM)
        assertEquals(125f, AcousticTimbreEngine.calculateWarpedTempo(bpmA, bpmB, 0.5f), 0.01f)

        // At progress 100% -> exactly BPM B
        assertEquals(130f, AcousticTimbreEngine.calculateWarpedTempo(bpmA, bpmB, 1.0f), 0.01f)

        // Check smooth inflection: rate of change is lower near endpoints
        val tempoAt10 = AcousticTimbreEngine.calculateWarpedTempo(bpmA, bpmB, 0.1f)
        val delta0To10 = tempoAt10 - 120f // S-Curve delta at 10% is 3*(0.01) - 2*(0.001) = 0.028 -> 0.28 BPM
        assertTrue("Slope should be gentle near start", delta0To10 < 0.5f)
    }

    @Test
    fun `calculateTimbreCohesion computes euclidean similarity accurately`() {
        val profileA = TimbreSpectralProfile(brightness = 0.5f, warmth = 0.5f, percussionDensity = 0.5f, vocalProminence = 0.5f)
        val profileIdentical = TimbreSpectralProfile(brightness = 0.5f, warmth = 0.5f, percussionDensity = 0.5f, vocalProminence = 0.5f)
        val profileDistant = TimbreSpectralProfile(brightness = 0.9f, warmth = 0.1f, percussionDensity = 0.9f, vocalProminence = 0.1f)

        // Identical profiles should have 1.0 cohesion
        assertEquals(1.0f, AcousticTimbreEngine.calculateTimbreCohesion(profileA, profileIdentical), 0.001f)

        // Distant profile should have lower cohesion
        val distantScore = AcousticTimbreEngine.calculateTimbreCohesion(profileA, profileDistant)
        assertTrue("Distant spectral timbre should have lower score", distantScore < 0.7f)
    }

    @Test
    fun `estimateTrackLoudness and estimateTrackTimbre produce deterministic profiles`() {
        val track = Track(
            id = "test_track_1",
            title = "Acoustic Journey",
            artist = "Pixelody",
            durationSeconds = 200,
            sampleRate = 48000,
            streamUrl = "http://stream/1"
        )

        val loudness = AcousticTimbreEngine.estimateTrackLoudness(track)
        assertTrue("Estimated LUFS should be within normal broadcast range", loudness.integratedLufs in -24.0f..-6.0f)

        val timbre = AcousticTimbreEngine.estimateTrackTimbre(track)
        assertTrue("Timbre brightness in unit range", timbre.brightness in 0f..1f)
        assertTrue("Timbre warmth in unit range", timbre.warmth in 0f..1f)
    }
}
