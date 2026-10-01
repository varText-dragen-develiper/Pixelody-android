package com.pixelody.app

import com.pixelody.app.core.playback.SpatialAcousticChamberEngine
import com.pixelody.app.data.model.AcousticChamberPreset
import com.pixelody.app.data.model.CamelotKey
import com.pixelody.app.data.model.CamelotMode
import com.pixelody.app.data.model.HarmonicRelation
import com.pixelody.app.data.model.WallMaterialDamping
import com.pixelody.app.ui.components.BeatLoopLength
import com.pixelody.app.ui.components.HarmonicFilterMode
import com.pixelody.app.ui.components.SlipmatType
import com.pixelody.app.ui.components.TurntableHotCue
import com.pixelody.app.ui.components.computeHarmonicRelation
import com.pixelody.app.ui.components.getCompatibleCamelotKeyCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Unit tests verifying all Wave 2 innovative mechanics:
 * 1. Turntable Slipmat Felt Physics & Hot Cue Matrix
 * 2. 360° Gyro Soundstage Compass & Head Yaw ITD/ILD Telemetry
 * 3. Transient Beat-Grid Slicer & Quantized Micro-Loop Math
 * 4. Camelot Harmonic Digging Ring Lens & Modular Circle-of-Fifths Navigation
 */
class Wave2FeaturesTest {

    // =========================================================================
    // 1. Turntable Slipmat Physics & Hot Cue Matrix Tests
    // =========================================================================

    @Test
    fun testSlipmatFrictionAndTractionCoefficients() {
        // Rubber Technics: High friction / high grip
        val rubber = SlipmatType.RubberTechnics
        assertEquals(0.90f, rubber.frictionDamping, 0.001f)
        assertEquals(0.20f, rubber.motorTraction, 0.001f)
        assertEquals("RUBBER GRIP", rubber.label)

        // Butter Rug: Ultra-slick, minimal motor drag for scratching
        val butterRug = SlipmatType.ButterRugFelt
        assertEquals(0.98f, butterRug.frictionDamping, 0.001f)
        assertEquals(0.05f, butterRug.motorTraction, 0.001f)
        assertEquals("BUTTER RUG", butterRug.label)

        // Cork Audiophile: High damping acoustic decoupling
        val cork = SlipmatType.CorkAudiophile
        assertEquals(0.94f, cork.frictionDamping, 0.001f)
        assertEquals(0.12f, cork.motorTraction, 0.001f)
        assertEquals("CORK ACOUSTIC", cork.label)

        // ButterRug must have lower traction than Rubber for scratch spinning
        assertTrue(butterRug.motorTraction < rubber.motorTraction)
        // ButterRug must glide longer (higher inertia preservation) than Rubber
        assertTrue(butterRug.frictionDamping > rubber.frictionDamping)
    }

    @Test
    fun testTurntableHotCueDataModel() {
        val cue1 = TurntableHotCue(index = 1, positionMs = 15_400L, label = "DROP A")
        assertEquals(1, cue1.index)
        assertEquals(15_400L, cue1.positionMs)
        assertEquals("DROP A", cue1.label)
        assertEquals(0xFFEF4444.toLong(), cue1.colorHex)

        val cue2 = TurntableHotCue(index = 2, positionMs = 45_000L)
        assertEquals("CUE 2", cue2.label)
        assertEquals(0xFF3B82F6.toLong(), cue2.colorHex)

        val cue3 = TurntableHotCue(index = 3, positionMs = 90_200L)
        assertEquals(0xFF10B981.toLong(), cue3.colorHex)

        val cue4 = TurntableHotCue(index = 4, positionMs = 120_000L)
        assertEquals(0xFFF59E0B.toLong(), cue4.colorHex)
    }

    // =========================================================================
    // 2. 360° Gyro Soundstage Compass & Head Yaw Telemetry Tests
    // =========================================================================

    @Test
    fun testSpatialChamberHeadYawModulation() {
        val engine = SpatialAcousticChamberEngine()

        // Initial default: 0° yaw (facing front)
        assertEquals(0f, engine.settings.value.listenerHeadYawDeg, 0.01f)
        val initialTelemetry = engine.telemetry.value
        assertNotNull(initialTelemetry)

        // Turn listener head 45° to the right (clockwise)
        engine.updateHeadYaw(45f)
        assertEquals(45f, engine.settings.value.listenerHeadYawDeg, 0.01f)
        val yaw45Telemetry = engine.telemetry.value

        // As head turns right, left ear points forward/towards left speaker, right ear turns back.
        // ITD and crossfeed gains must dynamically adjust
        assertNotNull(yaw45Telemetry)
        assertTrue(yaw45Telemetry.itdMicroseconds > 0f)
        assertTrue(yaw45Telemetry.ildDb > 0f)

        // Turn head -90° (Stage Left: left ear directly facing front speakers)
        engine.updateHeadYaw(-90f)
        assertEquals(-90f, engine.settings.value.listenerHeadYawDeg, 0.01f)

        // Turn head 180° (Facing Rear)
        engine.updateHeadYaw(180f)
        assertEquals(180f, engine.settings.value.listenerHeadYawDeg, 0.01f)

        // Verify bounds clamping: wrapping beyond +/- 180°
        engine.updateHeadYaw(270f) // Should wrap or normalize to -90° or clamp
        assertTrue(abs(engine.settings.value.listenerHeadYawDeg) <= 180f)
    }

    @Test
    fun testWallMaterialAbsorptionMetrics() {
        // Concrete Stone: highly reflective, minimal absorption
        val concrete = WallMaterialDamping.ConcreteStone
        assertTrue(concrete.highFrequencyAbsorption in 0.01f..0.20f)
        assertTrue(concrete.midFrequencyAbsorption in 0.01f..0.20f)

        // Porous Acoustic Foam: high absorption, dry sound
        val foam = WallMaterialDamping.PorousAcousticFoam
        assertTrue(foam.highFrequencyAbsorption > 0.60f)
        assertTrue(foam.midFrequencyAbsorption > 0.40f)

        // Velvet Curtain: maximum high frequency dampening
        val velvet = WallMaterialDamping.VelvetCurtain
        assertTrue(velvet.highFrequencyAbsorption >= 0.70f)
    }

    // =========================================================================
    // 3. Transient Beat-Grid Slicer & Micro-Loop Math Tests
    // =========================================================================

    @Test
    fun testBeatLoopLengthMultipliers() {
        assertEquals(0.5f, BeatLoopLength.HalfBeat.beats, 0.001f)
        assertEquals(1.0f, BeatLoopLength.OneBeat.beats, 0.001f)
        assertEquals(2.0f, BeatLoopLength.TwoBeats.beats, 0.001f)
        assertEquals(4.0f, BeatLoopLength.FourBeats.beats, 0.001f)
        assertEquals(8.0f, BeatLoopLength.EightBeats.beats, 0.001f)
        assertEquals(16.0f, BeatLoopLength.SixteenBeats.beats, 0.001f)
        assertEquals(32.0f, BeatLoopLength.ThirtyTwoBeats.beats, 0.001f)
    }

    @Test
    fun testQuantizedBeatGridDurationCalculations() {
        // Standard House / Techno: 120 BPM -> 1 beat = 500ms
        val bpm120 = 120
        val beatDuration120Ms = (60_000f / bpm120.toFloat())
        assertEquals(500f, beatDuration120Ms, 0.01f)

        val loop4Beats120 = (beatDuration120Ms * BeatLoopLength.FourBeats.beats).toLong()
        assertEquals(2000L, loop4Beats120)

        val loopHalfBeat120 = (beatDuration120Ms * BeatLoopLength.HalfBeat.beats).toLong()
        assertEquals(250L, loopHalfBeat120)

        // Drum & Bass / Dubstep: 174 BPM -> 1 beat = ~344.83ms
        val bpm174 = 174
        val beatDuration174Ms = (60_000f / bpm174.toFloat())
        assertEquals(344.827f, beatDuration174Ms, 0.01f)

        val loop16Beats174 = (beatDuration174Ms * BeatLoopLength.SixteenBeats.beats).toLong()
        assertEquals(5517L, loop16Beats174)
    }

    // =========================================================================
    // 4. Camelot Harmonic Digging Ring Lens & Compatibility Math Tests
    // =========================================================================

    @Test
    fun testCamelotStrictAdjacentCompatibility() {
        // 8A (A minor) base
        val key8A = CamelotKey.K8A
        val strictAdjacent = getCompatibleCamelotKeyCodes(key8A, HarmonicFilterMode.StrictAdjacent)

        // Should include: 8A (exact), 8B (relative major: C major), 9A (E minor: +1 fifth), 7A (D minor: -1 fifth)
        assertEquals(setOf("8A", "8B", "9A", "7A"), strictAdjacent)
    }

    @Test
    fun testCamelotEnergyBoostAndSunsetDrift() {
        val key8A = CamelotKey.K8A

        // Energy Boost (+2 steps up the wheel for energy lift)
        val energyBoost = getCompatibleCamelotKeyCodes(key8A, HarmonicFilterMode.EnergyBoost)
        assertEquals(setOf("8A", "9A", "10A"), energyBoost)

        // Sunset Drift (-2 steps down the wheel for melodic cooldown)
        val sunsetDrift = getCompatibleCamelotKeyCodes(key8A, HarmonicFilterMode.SunsetDrift)
        assertEquals(setOf("8A", "7A", "6A"), sunsetDrift)
    }

    @Test
    fun testCamelotAllCompatibleSet() {
        val key8A = CamelotKey.K8A
        val allCompatible = getCompatibleCamelotKeyCodes(key8A, HarmonicFilterMode.AllCompatible)

        assertEquals(setOf("8A", "8B", "9A", "7A", "10A", "6A"), allCompatible)
    }

    @Test
    fun testCamelotClockModularWrapAround() {
        // 12A (E minor) -> +1 should be 1A (B minor), -1 should be 11A (A minor)
        val key12A = CamelotKey.K12A
        val strict12A = getCompatibleCamelotKeyCodes(key12A, HarmonicFilterMode.StrictAdjacent)
        assertEquals(setOf("12A", "12B", "1A", "11A"), strict12A)

        val energy12A = getCompatibleCamelotKeyCodes(key12A, HarmonicFilterMode.EnergyBoost)
        assertEquals(setOf("12A", "1A", "2A"), energy12A)

        // 1A (B minor) -> -1 should be 12A (E minor), -2 should be 11A (A minor)
        val key1A = CamelotKey.K1A
        val sunset1A = getCompatibleCamelotKeyCodes(key1A, HarmonicFilterMode.SunsetDrift)
        assertEquals(setOf("1A", "12A", "11A"), sunset1A)
    }

    @Test
    fun testComputeHarmonicRelationClassifications() {
        val base8A = CamelotKey.K8A

        // Exact match
        assertEquals(HarmonicRelation.ExactMatch, computeHarmonicRelation(base8A, "8A"))

        // Relative Major
        assertEquals(HarmonicRelation.RelativeMajorMinor, computeHarmonicRelation(base8A, "8B"))

        // Adjacent +1 / -1
        assertEquals(HarmonicRelation.AdjacentStep, computeHarmonicRelation(base8A, "9A"))
        assertEquals(HarmonicRelation.AdjacentStep, computeHarmonicRelation(base8A, "7A"))

        // Energy Boost (+2)
        assertEquals(HarmonicRelation.EnergyBoost, computeHarmonicRelation(base8A, "10A"))

        // Energy Drop / Sunset (-2)
        assertEquals(HarmonicRelation.EnergyDrop, computeHarmonicRelation(base8A, "6A"))

        // Harmonic Clash (distant key, e.g. 2A or 2B)
        assertEquals(HarmonicRelation.DissonantClash, computeHarmonicRelation(base8A, "2A"))
        assertEquals(HarmonicRelation.DissonantClash, computeHarmonicRelation(base8A, "3B"))
    }
}
