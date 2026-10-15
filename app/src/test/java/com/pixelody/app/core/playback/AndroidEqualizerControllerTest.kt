package com.pixelody.app.core.playback

import com.pixelody.app.data.model.AcousticChamberPreset
import com.pixelody.app.data.model.BinauralCrossfeedMode
import com.pixelody.app.data.model.EqualizerPreset
import com.pixelody.app.data.model.EqualizerProfile
import com.pixelody.app.data.model.MasteringPreset
import com.pixelody.app.data.model.MasteringProfile
import com.pixelody.app.data.model.SpatialChamberSettings
import com.pixelody.app.data.model.WallMaterialDamping
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AndroidEqualizerControllerTest {

    private lateinit var controller: AndroidEqualizerController

    @Before
    fun setUp() {
        controller = AndroidEqualizerController()
    }

    @Test
    fun zeroOrNegativeAudioSessionIdReturnsWaitingState() {
        val stateZero = controller.applyEffects(
            audioSessionId = 0,
            eqProfile = EqualizerProfile(),
            masteringProfile = MasteringProfile(),
            useMastering = false,
            spatialSettings = SpatialChamberSettings()
        )
        assertFalse(stateZero.active)
        assertEquals("Start playback to attach Audio Effects", stateZero.message)

        val stateNeg = controller.applyProfile(-5, EqualizerProfile())
        assertFalse(stateNeg.active)
        assertEquals("Start playback to attach Audio Effects", stateNeg.message)
    }

    @Test
    fun releaseDoesNotThrow() {
        controller.release()
        // Calling release multiple times must be safe and idempotent
        controller.release()
    }

    @Test
    fun equalizerProfileNormalizedClampsGains() {
        val rawProfile = EqualizerProfile(
            enabled = true,
            preset = EqualizerPreset.Custom,
            gainsDb = listOf(-25f, 15f, 0f, 100f, -50f, 3f) // Out of bounds & excess bands
        )
        val normalized = rawProfile.normalized()
        assertEquals(5, normalized.gainsDb.size)
        assertEquals(-12f, normalized.gainsDb[0], 0.001f) // Clamped min
        assertEquals(12f, normalized.gainsDb[1], 0.001f)  // Clamped max
        assertEquals(0f, normalized.gainsDb[2], 0.001f)
        assertEquals(12f, normalized.gainsDb[3], 0.001f)  // Clamped max
        assertEquals(-12f, normalized.gainsDb[4], 0.001f) // Clamped min
    }

    @Test
    fun masteringProfileNormalizedClampsAllRanges() {
        val rawMastering = MasteringProfile(
            enabled = true,
            preset = MasteringPreset.Custom,
            eqGainsDb = listOf(-30f, 30f, 0f, 0f, 0f),
            eqQFactors = listOf(0.1f, 10f, 1.4f, 1.4f, 1.4f),
            tubeDrive = 2.5f,
            tapeWarmth = -1.0f,
            spatialWidth = 3.5f,
            subBassBoostDb = 15f,
            limiterThresholdDb = 5.0f,
            autoGainEnabled = true
        )
        val normalized = rawMastering.normalized()
        assertEquals(5, normalized.eqGainsDb.size)
        assertEquals(-12f, normalized.eqGainsDb[0], 0.001f)
        assertEquals(12f, normalized.eqGainsDb[1], 0.001f)
        assertEquals(0.5f, normalized.eqQFactors[0], 0.001f) // Clamped min Q
        assertEquals(5.0f, normalized.eqQFactors[1], 0.001f) // Clamped max Q
        assertEquals(1.0f, normalized.tubeDrive, 0.001f)     // Clamped max Drive
        assertEquals(0.0f, normalized.tapeWarmth, 0.001f)   // Clamped min Tape
        assertEquals(2.0f, normalized.spatialWidth, 0.001f) // Clamped max Width
        assertEquals(6.0f, normalized.subBassBoostDb, 0.001f)// Clamped max SubBass
        assertEquals(0.0f, normalized.limiterThresholdDb, 0.001f) // Clamped max Limiter threshold
    }

    @Test
    fun equalizerPresetsHaveValid5BandGains() {
        EqualizerPreset.values().forEach { preset ->
            assertEquals(5, preset.gainsDb.size)
            preset.gainsDb.forEach { gain ->
                assertTrue("Gain $gain must be >= -12dB", gain >= -12f)
                assertTrue("Gain $gain must be <= +12dB", gain <= 12f)
            }
        }
    }

    @Test
    fun masteringPresetsHaveValid5BandGainsAndQFactors() {
        MasteringPreset.values().forEach { preset ->
            assertEquals(5, preset.gainsDb.size)
            assertEquals(5, preset.qFactors.size)
            preset.gainsDb.forEach { gain ->
                assertTrue("Gain $gain must be >= -12dB", gain >= -12f)
                assertTrue("Gain $gain must be <= +12dB", gain <= 12f)
            }
            preset.qFactors.forEach { q ->
                assertTrue("Q $q must be >= 0.5", q >= 0.5f)
                assertTrue("Q $q must be <= 5.0", q <= 5.0f)
            }
            assertTrue("Tube drive must be in 0..1", preset.tubeDrive in 0f..1f)
            assertTrue("Tape warmth must be in 0..1", preset.tapeWarmth in 0f..1f)
            assertTrue("Spatial width must be in 0..2", preset.spatialWidth in 0f..2f)
            assertTrue("Sub bass must be in 0..6", preset.subBassBoostDb in 0f..6f)
            assertTrue("Limiter threshold must be in -6..0", preset.limiterThresholdDb in -6f..0f)
        }
    }
}
