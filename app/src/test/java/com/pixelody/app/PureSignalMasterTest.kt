package com.pixelody.app

import com.pixelody.app.core.playback.PureSignalMasterEngine
import com.pixelody.app.data.model.AcousticTargetPreset
import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PureSignalMasterTest {

    private fun createTestTrack(lossless: Boolean, sampleRate: Int, bitDepth: Int? = 24): Track {
        return Track(
            id = "test_audiophile",
            title = "Master Session",
            artist = "Studio Artist",
            durationSeconds = 240,
            format = if (lossless) "FLAC" else "AAC",
            lossless = lossless,
            sampleRate = sampleRate,
            bitDepth = bitDepth,
            streamUrl = "http://stream/flac"
        )
    }

    @Test
    fun `verifyBitstreamPath recognizes 24-bit 192kHz FLAC as bit-perfect`() {
        val track = createTestTrack(lossless = true, sampleRate = 192000, bitDepth = 24)
        val verification = PureSignalMasterEngine.verifyBitstreamPath(track, "USB DAC Adapter")

        assertTrue(verification.isBitPerfect)
        assertEquals(24, verification.bitDepth)
        assertEquals(192000, verification.sampleRateHz)
        assertEquals("24-bit / 192kHz FLAC", verification.bitstreamLabel)
        assertEquals(1.0f, verification.resamplingRatio, 0.001f)
    }

    @Test
    fun `calculateLimiterGain prevents digital clipping with true-peak ceiling`() {
        // Signal below ceiling (-0.3 dB): No attenuation (Gain = 1.0)
        val safeGain = PureSignalMasterEngine.calculateLimiterGain(peakLevelDb = -1.5f, ceilingDb = -0.3f)
        assertEquals(1.0f, safeGain, 0.001f)

        // Hot signal above ceiling (+2.0 dB vs -0.3 dB): Attenuates by 2.3 dB -> gain ~0.767
        val attenuatedGain = PureSignalMasterEngine.calculateLimiterGain(peakLevelDb = 2.0f, ceilingDb = -0.3f)
        assertTrue("Limiter should attenuate signals exceeding ceiling", attenuatedGain < 1.0f)
        assertEquals(0.767f, attenuatedGain, 0.05f)
    }

    @Test
    fun `detectOptimalAcousticProfile switches presets based on output device`() {
        val losslessTrack = createTestTrack(lossless = true, sampleRate = 96000)

        assertEquals(
            AcousticTargetPreset.HiResDacDirect,
            PureSignalMasterEngine.detectOptimalAcousticProfile(losslessTrack, "Fiio USB DAC")
        )

        assertEquals(
            AcousticTargetPreset.CarAudioPunch,
            PureSignalMasterEngine.detectOptimalAcousticProfile(losslessTrack, "Car Bluetooth Audio")
        )

        assertEquals(
            AcousticTargetPreset.IemHarmonicWarmth,
            PureSignalMasterEngine.detectOptimalAcousticProfile(losslessTrack, "Sony IEM Earbuds")
        )

        assertEquals(
            AcousticTargetPreset.OpenBackAiry,
            PureSignalMasterEngine.detectOptimalAcousticProfile(losslessTrack, "Sennheiser Open-Back")
        )
    }

    @Test
    fun `calculateParametricResponse returns 10 frequency bands for target curves`() {
        val flatResponse = PureSignalMasterEngine.calculateParametricResponse(AcousticTargetPreset.StudioFlatReference)
        assertEquals(10, flatResponse.size)
        assertTrue(flatResponse.all { it == 0f })

        val carResponse = PureSignalMasterEngine.calculateParametricResponse(AcousticTargetPreset.CarAudioPunch)
        assertEquals(10, carResponse.size)
        assertTrue("Car preset should have bass boost", carResponse[0] > 3.0f)
    }
}
