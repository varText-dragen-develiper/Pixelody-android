package com.pixelody.app

import com.pixelody.app.core.playback.CassetteTapeEngine
import com.pixelody.app.data.model.CassetteShellTheme
import com.pixelody.app.data.model.CassetteTapeSettings
import com.pixelody.app.data.model.TapeFormulation
import com.pixelody.app.data.model.TapeNoiseReduction
import com.pixelody.app.data.model.TapeTransportState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CassetteTapeEngineTest {

    @Test
    fun testDefaultSettingsAndTelemetry() {
        val engine = CassetteTapeEngine()
        val settings = engine.settings.value
        val telemetry = engine.telemetry.value

        assertTrue(settings.isEnabled)
        assertEquals(TapeFormulation.TypeII_Chrome, settings.formulation)
        assertEquals(TapeNoiseReduction.DolbyB, settings.noiseReduction)
        assertEquals(CassetteShellTheme.SmokedAcrylic, settings.shellTheme)

        assertNotNull(telemetry)
        assertTrue(telemetry.effectiveSnrDb > 50f)
        assertTrue(telemetry.spoolPhysics.supplyRadiusRatio in 0.0f..1.0f)
        assertTrue(telemetry.spoolPhysics.takeupRadiusRatio in 0.0f..1.0f)
    }

    @Test
    fun testFormulationSwitching() {
        val engine = CassetteTapeEngine()

        TapeFormulation.values().forEach { formulation ->
            engine.setFormulation(formulation)
            assertEquals(formulation, engine.settings.value.formulation)
            assertEquals(formulation.defaultDriveGain, engine.settings.value.driveGain, 0.01f)
            assertTrue(formulation.saturationCeiling > 0.5f)
            assertTrue(formulation.highFreqRolloffHz in 5000f..30000f)
            assertTrue(formulation.noiseFloorDb < -30f)
        }
    }

    @Test
    fun testMagneticSaturationSoftClipping() {
        val engine = CassetteTapeEngine()
        engine.setFormulation(TapeFormulation.TypeI_Ferric)
        engine.setDriveGain(2.5f)

        // Low input signal (linear region)
        val lowSat = engine.computeSaturation(0.1f, 0.85f, 0.0f)
        assertTrue(lowSat > 0.05f && lowSat <= 0.12f)

        // Extreme input signal (saturation compression)
        val extremeSat = engine.computeSaturation(5.0f, 0.85f, 0.0f)
        // tanh(5 / 0.85) * 0.85 ≈ 0.85
        assertTrue(extremeSat <= 0.95f)
        assertTrue(extremeSat > 0.75f)
    }

    @Test
    fun testSpoolPhysicsVolumeConservation() {
        val engine = CassetteTapeEngine()

        // Beginning of tape (p = 0.0) -> Supply full, Takeup empty
        val startSpool = engine.computeSpoolPhysics(0.0f, TapeTransportState.Playing)
        assertTrue(startSpool.supplyRadiusRatio > 0.90f)
        assertTrue(startSpool.takeupRadiusRatio < 0.10f)
        assertTrue(startSpool.takeupRpm > startSpool.supplyRpm) // Empty spool spins faster to maintain linear speed

        // Middle of tape (p = 0.5) -> Both spools approximately equal
        val midSpool = engine.computeSpoolPhysics(0.5f, TapeTransportState.Playing)
        assertEquals(midSpool.supplyRadiusRatio, midSpool.takeupRadiusRatio, 0.05f)
        assertEquals(midSpool.supplyRpm, midSpool.takeupRpm, 2.0f)

        // End of tape (p = 1.0) -> Supply empty, Takeup full
        val endSpool = engine.computeSpoolPhysics(1.0f, TapeTransportState.Playing)
        assertTrue(endSpool.supplyRadiusRatio < 0.10f)
        assertTrue(endSpool.takeupRadiusRatio > 0.90f)
        assertTrue(endSpool.supplyRpm > endSpool.takeupRpm)
    }

    @Test
    fun testTransportStateTransitions() {
        val engine = CassetteTapeEngine()

        // Fast Forwarding: High RPM, heads disengaged
        engine.setTransportState(TapeTransportState.FastForwarding)
        val ffSpool = engine.computeSpoolPhysics(0.5f, TapeTransportState.FastForwarding)
        assertTrue(ffSpool.supplyRpm > 60f)
        assertEquals(0.0f, ffSpool.headContactPressure, 0.01f)

        // Rewinding: Negative RPM multiplier
        engine.setTransportState(TapeTransportState.Rewinding)
        val rwdSpool = engine.computeSpoolPhysics(0.5f, TapeTransportState.Rewinding)
        assertTrue(rwdSpool.supplyRpm < -60f)

        // Stopped: 0 RPM
        engine.setTransportState(TapeTransportState.Stopped)
        val stopSpool = engine.computeSpoolPhysics(0.5f, TapeTransportState.Stopped)
        assertEquals(0.0f, stopSpool.supplyRpm, 0.01f)
        assertEquals(0.0f, stopSpool.takeupRpm, 0.01f)
    }

    @Test
    fun testNoiseReductionAndSnrScaling() {
        val engine = CassetteTapeEngine()
        engine.setFormulation(TapeFormulation.TypeII_Chrome)

        engine.setNoiseReduction(TapeNoiseReduction.Off)
        val snrOff = engine.telemetry.value.effectiveSnrDb

        engine.setNoiseReduction(TapeNoiseReduction.DolbyB)
        val snrDolbyB = engine.telemetry.value.effectiveSnrDb
        assertTrue(snrDolbyB > snrOff)

        engine.setNoiseReduction(TapeNoiseReduction.DolbyC)
        val snrDolbyC = engine.telemetry.value.effectiveSnrDb
        assertTrue(snrDolbyC > snrDolbyB)

        engine.setNoiseReduction(TapeNoiseReduction.dbx_TypeII)
        val snrDbx = engine.telemetry.value.effectiveSnrDb
        assertTrue(snrDbx > snrDolbyC)
    }

    @Test
    fun testStereoFrameProcessing() {
        val engine = CassetteTapeEngine()
        engine.setTransportState(TapeTransportState.Playing)

        val inputL = 0.5f
        val inputR = -0.5f

        // Warm up engine delay lines
        var outL = 0f
        var outR = 0f
        for (i in 0 until 500) {
            val (l, r) = engine.processStereoFrame(inputL, inputR)
            outL = l
            outR = r
        }

        assertFalse(outL.isNaN())
        assertFalse(outR.isNaN())
        assertFalse(outL.isInfinite())
        assertFalse(outR.isInfinite())
        assertTrue(abs(outL) > 0.01f)
        assertTrue(abs(outR) > 0.01f)
    }

    @Test
    fun testStereoBufferProcessing() {
        val engine = CassetteTapeEngine()
        engine.setTransportState(TapeTransportState.Playing)

        val bufferSize = 1024
        val leftChan = FloatArray(bufferSize) { 0.4f }
        val rightChan = FloatArray(bufferSize) { -0.4f }

        engine.processStereoBuffer(leftChan, rightChan)

        for (i in 0 until bufferSize) {
            assertFalse(leftChan[i].isNaN())
            assertFalse(rightChan[i].isNaN())
        }
    }

    @Test
    fun testTapeBypassMode() {
        val engine = CassetteTapeEngine()
        engine.toggleEnabled() // Disable

        assertFalse(engine.settings.value.isEnabled)
        val (outL, outR) = engine.processStereoFrame(0.75f, -0.75f)
        assertEquals(0.75f, outL, 0.001f)
        assertEquals(-0.75f, outR, 0.001f)
    }

    @Test
    fun testTapeProgressUpdatesSpoolTelemetry() {
        val engine = CassetteTapeEngine()

        engine.updateTapeProgress(positionMs = 15000L, durationMs = 180000L) // ~8% progress
        val earlySupply = engine.telemetry.value.spoolPhysics.supplyRadiusRatio

        engine.updateTapeProgress(positionMs = 160000L, durationMs = 180000L) // ~88% progress
        val lateSupply = engine.telemetry.value.spoolPhysics.supplyRadiusRatio

        assertTrue(earlySupply > lateSupply)
    }
}
