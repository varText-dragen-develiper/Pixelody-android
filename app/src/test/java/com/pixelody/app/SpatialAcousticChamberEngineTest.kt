package com.pixelody.app

import com.pixelody.app.core.playback.SpatialAcousticChamberEngine
import com.pixelody.app.data.model.AcousticChamberPreset
import com.pixelody.app.data.model.BinauralCrossfeedMode
import com.pixelody.app.data.model.SpatialChamberSettings
import com.pixelody.app.data.model.WallMaterialDamping
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SpatialAcousticChamberEngineTest {

    @Test
    fun testInitialStateAndDefaults() {
        val engine = SpatialAcousticChamberEngine()
        val settings = engine.settings.value
        val telemetry = engine.telemetry.value

        assertTrue(settings.isEnabled)
        assertEquals(AcousticChamberPreset.AbbeyStudioControlRoom, settings.preset)
        assertEquals(WallMaterialDamping.PorousAcousticFoam, settings.wallDamping)
        assertEquals(BinauralCrossfeedMode.NaturalNearfield, settings.crossfeedMode)
        assertEquals(120f, settings.roomVolumeM3, 0.1f)

        assertNotNull(telemetry)
        assertTrue(telemetry.itdMicroseconds in 200f..300f)
        assertTrue(telemetry.ildDb in 2.0f..4.5f)
        assertTrue(telemetry.calculatedRt60Seconds in 0.15f..0.6f)
        assertEquals(12, telemetry.activeRayCount)
    }

    @Test
    fun testItdCalculationPhysics() {
        val engine = SpatialAcousticChamberEngine()

        val itd0 = engine.computeItdMicroseconds(0f)
        assertEquals(0f, itd0, 0.01f)

        val itd30 = engine.computeItdMicroseconds(30f)
        assertTrue("ITD at 30 deg should be ~260us, was $itd30", itd30 in 240f..280f)

        val itd45 = engine.computeItdMicroseconds(45f)
        assertTrue("ITD at 45 deg should be ~400us, was $itd45", itd45 in 380f..430f)

        val itd90 = engine.computeItdMicroseconds(90f)
        assertTrue("ITD at 90 deg should be ~655us, was $itd90", itd90 in 630f..680f)
    }

    @Test
    fun testSabineRt60Computation() {
        val engine = SpatialAcousticChamberEngine()

        // Cathedral: large volume, low absorption -> long RT60
        val cathedralRt60 = engine.computeSabineRt60(3500f, WallMaterialDamping.ConcreteStone)
        assertTrue("Cathedral RT60 should be > 2.5s, was $cathedralRt60", cathedralRt60 > 2.5f)

        // Teahouse: small volume, high absorption -> short RT60
        val teahouseRt60 = engine.computeSabineRt60(28f, WallMaterialDamping.VelvetCurtain)
        assertTrue("Teahouse RT60 should be < 0.4s, was $teahouseRt60", teahouseRt60 < 0.4f)
    }

    @Test
    fun testApplyPresets() {
        val engine = SpatialAcousticChamberEngine()

        // 1. Tokyo Vinyl Bar
        engine.applyPreset(AcousticChamberPreset.TokyoVinylBar)
        var s = engine.settings.value
        var t = engine.telemetry.value
        assertEquals(AcousticChamberPreset.TokyoVinylBar, s.preset)
        assertEquals(WallMaterialDamping.TeakWood, s.wallDamping)
        assertEquals(45f, s.roomVolumeM3, 0.1f)
        assertEquals(16, t.activeRayCount)

        // 2. Cathedral of Echoes
        engine.applyPreset(AcousticChamberPreset.CathedralOfEchoes)
        s = engine.settings.value
        t = engine.telemetry.value
        assertEquals(AcousticChamberPreset.CathedralOfEchoes, s.preset)
        assertEquals(WallMaterialDamping.ConcreteStone, s.wallDamping)
        assertEquals(3500f, s.roomVolumeM3, 0.1f)
        assertEquals(36, t.activeRayCount)
        assertTrue(t.calculatedRt60Seconds > 2.0f)

        // 3. Minimalist Teahouse
        engine.applyPreset(AcousticChamberPreset.MinimalistTeahouse)
        s = engine.settings.value
        t = engine.telemetry.value
        assertEquals(AcousticChamberPreset.MinimalistTeahouse, s.preset)
        assertEquals(6, t.activeRayCount)
        assertTrue(t.calculatedRt60Seconds < 0.5f)
    }

    @Test
    fun testUpdateSpeakerPosition() {
        val engine = SpatialAcousticChamberEngine()

        engine.updateSpeakerPosition(leftAngleDeg = -60f, rightAngleDeg = 60f, distanceMeters = 2.5f)
        val s = engine.settings.value
        val t = engine.telemetry.value

        assertEquals(-60f, s.speakerPosition.leftAngleDeg, 0.01f)
        assertEquals(60f, s.speakerPosition.rightAngleDeg, 0.01f)
        assertEquals(2.5f, s.speakerPosition.distanceMeters, 0.01f)

        assertTrue("ITD should increase with 60 deg angle, was ${t.itdMicroseconds}", t.itdMicroseconds > 450f)
        assertTrue("ILD should increase with 60 deg angle, was ${t.ildDb}", t.ildDb > 4.5f)
    }

    @Test
    fun testSetCrossfeedModes() {
        val engine = SpatialAcousticChamberEngine()

        engine.setCrossfeedMode(BinauralCrossfeedMode.WideAngleMaster)
        assertEquals(BinauralCrossfeedMode.WideAngleMaster, engine.settings.value.crossfeedMode)
        assertTrue(engine.telemetry.value.leftEarCrossbleedGain > 0.2f)

        engine.setCrossfeedMode(BinauralCrossfeedMode.DirectStereoOff)
        assertEquals(BinauralCrossfeedMode.DirectStereoOff, engine.settings.value.crossfeedMode)
        assertEquals(0f, engine.telemetry.value.leftEarCrossbleedGain, 0.001f)
    }

    @Test
    fun testProcessStereoFrameBypass() {
        val engine = SpatialAcousticChamberEngine()

        // When disabled -> exact input returned
        engine.updateSettings(engine.settings.value.copy(isEnabled = false))
        val (l1, r1) = engine.processStereoFrame(0.75f, -0.42f)
        assertEquals(0.75f, l1, 0.0001f)
        assertEquals(-0.42f, r1, 0.0001f)

        // When dryWetMix = 0.0 -> exact input returned
        engine.updateSettings(engine.settings.value.copy(isEnabled = true, dryWetMix = 0.0f))
        val (l2, r2) = engine.processStereoFrame(0.50f, 0.50f)
        assertEquals(0.50f, l2, 0.0001f)
        assertEquals(0.50f, r2, 0.0001f)
    }

    @Test
    fun testProcessStereoFrameSpatialized() {
        val engine = SpatialAcousticChamberEngine()
        engine.applyPreset(AcousticChamberPreset.AbbeyStudioControlRoom)

        // Feed isolated left channel impulse
        var maxLeftOut = 0f
        var maxRightCrossfeed = 0f

        for (i in 0..100) {
            val leftInput = if (i == 0) 1.0f else 0.0f
            val rightInput = 0.0f

            val (outL, outR) = engine.processStereoFrame(leftInput, rightInput)

            assertFalse("Output should never be NaN", outL.isNaN() || outR.isNaN())
            maxLeftOut = maxOf(maxLeftOut, abs(outL))
            maxRightCrossfeed = maxOf(maxRightCrossfeed, abs(outR))
        }

        assertTrue("Direct channel should produce signal", maxLeftOut > 0.3f)
        assertTrue("Binaural crossfeed should leak into contralateral right ear", maxRightCrossfeed > 0.01f)
    }

    @Test
    fun testWallDampingMutation() {
        val engine = SpatialAcousticChamberEngine()

        engine.setWallMaterial(WallMaterialDamping.VelvetCurtain)
        val velvetRt60 = engine.telemetry.value.calculatedRt60Seconds

        engine.setWallMaterial(WallMaterialDamping.BrushedAluminum)
        val aluminumRt60 = engine.telemetry.value.calculatedRt60Seconds

        assertTrue("Aluminum reflections should have longer RT60 than Velvet", aluminumRt60 > velvetRt60)
    }

    @Test
    fun testToggleEnabled() {
        val engine = SpatialAcousticChamberEngine()
        assertTrue(engine.settings.value.isEnabled)

        engine.toggleEnabled()
        assertFalse(engine.settings.value.isEnabled)

        engine.toggleEnabled()
        assertTrue(engine.settings.value.isEnabled)
    }

    @Test
    fun testDspTankLongTermStabilityUnderSustainedAudio() {
        val engine = SpatialAcousticChamberEngine()
        engine.applyPreset(AcousticChamberPreset.CathedralOfEchoes) // Longest RT60 & highest feedback

        // Process 2000 continuous frames with sinusoidal signal
        for (i in 0 until 2000) {
            val signal = kotlin.math.sin(i * 0.05).toFloat() * 0.8f
            val (l, r) = engine.processStereoFrame(signal, signal)

            assertFalse("Left channel must never produce NaN at frame $i", l.isNaN())
            assertFalse("Right channel must never produce NaN at frame $i", r.isNaN())
            assertFalse("Left channel must not produce Infinite", l.isInfinite())
            assertFalse("Right channel must not produce Infinite", r.isInfinite())
            assertTrue("Output signal must stay bounded (was L=$l, R=$r)", abs(l) < 5.0f && abs(r) < 5.0f)
        }
    }

    @Test
    fun testSpeakerPositionClampingLimits() {
        val engine = SpatialAcousticChamberEngine()

        // Exceed upper/lower bounds
        engine.updateSpeakerPosition(leftAngleDeg = -150f, rightAngleDeg = 150f, distanceMeters = 20f)
        var s = engine.settings.value
        assertEquals(-90f, s.speakerPosition.leftAngleDeg, 0.01f)
        assertEquals(90f, s.speakerPosition.rightAngleDeg, 0.01f)
        assertEquals(6.0f, s.speakerPosition.distanceMeters, 0.01f)

        // Below min bounds
        engine.updateSpeakerPosition(leftAngleDeg = -5f, rightAngleDeg = 5f, distanceMeters = 0.1f)
        s = engine.settings.value
        assertEquals(-10f, s.speakerPosition.leftAngleDeg, 0.01f)
        assertEquals(10f, s.speakerPosition.rightAngleDeg, 0.01f)
        assertEquals(0.5f, s.speakerPosition.distanceMeters, 0.01f)
    }

    @Test
    fun testExtremeRoomVolumeSabineCalculations() {
        val engine = SpatialAcousticChamberEngine()

        val minVolRt60 = engine.computeSabineRt60(1f, WallMaterialDamping.PorousAcousticFoam)
        assertTrue("Minimum volume RT60 should be clamped >= 0.15s", minVolRt60 >= 0.15f)

        val maxVolRt60 = engine.computeSabineRt60(100_000f, WallMaterialDamping.ConcreteStone)
        assertTrue("Maximum volume RT60 should be clamped <= 6.0s", maxVolRt60 <= 6.0f)
    }
}

