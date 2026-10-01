package com.pixelody.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerModelsContractTest {

    @Test
    fun equalizerPresetQuickPresetCycle() {
        val quick = EqualizerPreset.quickPresets
        assertEquals(7, quick.size)

        var current = EqualizerPreset.Flat
        for (i in 1 until quick.size) {
            current = EqualizerPreset.nextQuickPreset(current)
            assertEquals(quick[i], current)
        }
        // Wraps around to Flat
        current = EqualizerPreset.nextQuickPreset(current)
        assertEquals(EqualizerPreset.Flat, current)
    }

    @Test
    fun equalizerProfileNormalizedClampsGainRange() {
        val outOfBounds = EqualizerProfile(
            gainsDb = listOf(-20f, -12f, 0f, 12f, 25f)
        ).normalized()

        assertEquals(5, outOfBounds.gainsDb.size)
        assertEquals(EqualizerProfile.MIN_GAIN_DB, outOfBounds.gainsDb[0])
        assertEquals(-12f, outOfBounds.gainsDb[1])
        assertEquals(0f, outOfBounds.gainsDb[2])
        assertEquals(12f, outOfBounds.gainsDb[3])
        assertEquals(EqualizerProfile.MAX_GAIN_DB, outOfBounds.gainsDb[4])
    }

    @Test
    fun equalizerProfileMutations() {
        val profile = EqualizerProfile(preset = EqualizerPreset.Flat)
        val warmProfile = profile.withPreset(EqualizerPreset.Warm)

        assertEquals(EqualizerPreset.Warm, warmProfile.preset)
        assertEquals(EqualizerPreset.Warm.gainsDb, warmProfile.gainsDb)

        val customProfile = warmProfile.withBandGain(2, 4.5f)
        assertEquals(EqualizerPreset.Custom, customProfile.preset)
        assertEquals(4.5f, customProfile.gainsDb[2])
        assertEquals(EqualizerPreset.Warm.gainsDb[0], customProfile.gainsDb[0])
    }

    @Test
    fun masteringProfileMutationsAndClamping() {
        val profile = MasteringProfile(preset = MasteringPreset.AudiophileReference)
        assertEquals(MasteringPreset.AudiophileReference, profile.preset)

        // Switching preset
        val club = profile.withPreset(MasteringPreset.ClubSoundstage)
        assertEquals(MasteringPreset.ClubSoundstage, club.preset)
        assertEquals(MasteringPreset.ClubSoundstage.tubeDrive, club.tubeDrive, 0.001f)
        assertEquals(MasteringPreset.ClubSoundstage.spatialWidth, club.spatialWidth, 0.001f)
        assertEquals(MasteringPreset.ClubSoundstage.subBassBoostDb, club.subBassBoostDb, 0.001f)

        // Tube drive mutation & clamping
        val driven = club.withTubeDrive(1.5f)
        assertEquals(MasteringPreset.Custom, driven.preset)
        assertEquals(1.0f, driven.tubeDrive, 0.001f)

        val negativeDrive = club.withTubeDrive(-0.5f)
        assertEquals(0.0f, negativeDrive.tubeDrive, 0.001f)

        // Tape warmth mutation & clamping
        val warm = club.withTapeWarmth(0.75f)
        assertEquals(0.75f, warm.tapeWarmth, 0.001f)
        assertEquals(1.0f, club.withTapeWarmth(2.0f).tapeWarmth, 0.001f)
        assertEquals(0.0f, club.withTapeWarmth(-1.0f).tapeWarmth, 0.001f)

        // Spatial width clamping (0.0 .. 2.0)
        assertEquals(0.0f, club.withSpatialWidth(-0.5f).spatialWidth, 0.001f)
        assertEquals(2.0f, club.withSpatialWidth(3.0f).spatialWidth, 0.001f)
        assertEquals(1.75f, club.withSpatialWidth(1.75f).spatialWidth, 0.001f)

        // Sub bass boost clamping (0.0 .. 6.0 dB)
        assertEquals(0.0f, club.withSubBassBoost(-2.0f).subBassBoostDb, 0.001f)
        assertEquals(6.0f, club.withSubBassBoost(10.0f).subBassBoostDb, 0.001f)
        assertEquals(3.5f, club.withSubBassBoost(3.5f).subBassBoostDb, 0.001f)

        // Limiter threshold clamping (-6.0 .. 0.0 dB)
        assertEquals(-6.0f, club.withLimiterThreshold(-12.0f).limiterThresholdDb, 0.001f)
        assertEquals(0.0f, club.withLimiterThreshold(5.0f).limiterThresholdDb, 0.001f)
        assertEquals(-2.5f, club.withLimiterThreshold(-2.5f).limiterThresholdDb, 0.001f)

        // Band gain and Q factor mutations
        val tuned = club.withBandGain(0, 5.0f).withBandQ(0, 2.5f)
        assertEquals(5.0f, tuned.eqGainsDb[0], 0.001f)
        assertEquals(2.5f, tuned.eqQFactors[0], 0.001f)

        // Conversion to EqualizerProfile
        val eqProfile = tuned.toEqualizerProfile()
        assertTrue(eqProfile.enabled)
        assertEquals(EqualizerPreset.Custom, eqProfile.preset)
        assertEquals(tuned.eqGainsDb, eqProfile.gainsDb)
    }

    @Test
    fun masteringPresetsCompleteness() {
        val quick = MasteringPreset.quickPresets
        assertEquals(7, quick.size)
        assertTrue(quick.contains(MasteringPreset.AudiophileReference))
        assertTrue(quick.contains(MasteringPreset.AnalogTapeWarmth))
        assertTrue(quick.contains(MasteringPreset.TubeVibeStudio))
        assertTrue(quick.contains(MasteringPreset.ClubSoundstage))
        assertTrue(quick.contains(MasteringPreset.BinauralHorizon))
        assertTrue(quick.contains(MasteringPreset.VocalClarity))
        assertTrue(quick.contains(MasteringPreset.NightListening))

        for (preset in MasteringPreset.entries) {
            assertEquals(5, preset.gainsDb.size)
            assertEquals(5, preset.qFactors.size)
            assertTrue(preset.tubeDrive in 0f..1f)
            assertTrue(preset.tapeWarmth in 0f..1f)
            assertTrue(preset.spatialWidth in 0f..2f)
            assertTrue(preset.subBassBoostDb in 0f..6f)
            assertTrue(preset.limiterThresholdDb in -6f..0f)
        }
    }
}
