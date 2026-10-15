package com.pixelody.app

import com.pixelody.app.core.playback.OscilloscopePhosphorEngine
import com.pixelody.app.data.model.CrtBeamPersistence
import com.pixelody.app.data.model.CrtPhosphorType
import com.pixelody.app.data.model.OscilloscopeDisplayMode
import com.pixelody.app.data.model.OscilloscopeSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class OscilloscopePhosphorEngineTest {

    private lateinit var engine: OscilloscopePhosphorEngine

    @Before
    fun setUp() {
        engine = OscilloscopePhosphorEngine()
    }

    @Test
    fun testDefaultSettingsAndTelemetry() {
        val settings = engine.settings.value
        assertTrue(settings.isEnabled)
        assertEquals(OscilloscopeDisplayMode.Lissajous_XY, settings.displayMode)
        assertEquals(CrtPhosphorType.Green_P1, settings.phosphorType)
        assertEquals(CrtBeamPersistence.Medium_StandardCRT, settings.persistence)
        assertEquals(1.25f, settings.sensitivityGain, 0.01f)

        val telemetry = engine.telemetry.value
        assertNotNull(telemetry)
        assertTrue(telemetry.phaseCoherenceScore in -1.0f..1.0f)
        assertTrue(telemetry.activePointCount > 0)
    }

    @Test
    fun testSetDisplayModes() {
        OscilloscopeDisplayMode.entries.forEach { mode ->
            engine.setDisplayMode(mode)
            assertEquals(mode, engine.settings.value.displayMode)
        }
    }

    @Test
    fun testSetPhosphorTypes() {
        CrtPhosphorType.entries.forEach { phosphor ->
            engine.setPhosphorType(phosphor)
            assertEquals(phosphor, engine.settings.value.phosphorType)
            assertTrue(phosphor.defaultDecayMs > 0f)
        }
    }

    @Test
    fun testSetPersistenceAndSensitivity() {
        engine.setPersistence(CrtBeamPersistence.UltraLong_LaserTrace)
        assertEquals(CrtBeamPersistence.UltraLong_LaserTrace, engine.settings.value.persistence)

        engine.setSensitivityGain(2.5f)
        assertEquals(2.5f, engine.settings.value.sensitivityGain, 0.01f)

        // Test clamping
        engine.setSensitivityGain(10.0f)
        assertEquals(4.0f, engine.settings.value.sensitivityGain, 0.01f)

        engine.setSensitivityGain(0.01f)
        assertEquals(0.1f, engine.settings.value.sensitivityGain, 0.01f)
    }

    @Test
    fun testStereoFrameProcessing() {
        val (leftOut, rightOut) = engine.processStereoFrame(0.5f, -0.5f)
        assertEquals(0.5f, leftOut, 0.001f)
        assertEquals(-0.5f, rightOut, 0.001f)
    }

    @Test
    fun testStereoBufferProcessing() {
        val left = FloatArray(256) { sin(it * 0.1f) }
        val right = FloatArray(256) { sin(it * 0.1f + 0.5f) }
        engine.processStereoBuffer(left, right)

        val points = engine.generateVectorPoints(400f, 400f, 128)
        assertEquals(128, points.size)
    }

    @Test
    fun testStereoPhaseCorrelationMono() {
        // Pure mono signal (L == R)
        val n = 512
        val left = FloatArray(n) { sin(it * 2.0 * PI / 32.0).toFloat() }
        val right = FloatArray(n) { sin(it * 2.0 * PI / 32.0).toFloat() }
        engine.processStereoBuffer(left, right)

        val correlation = engine.computeStereoPhaseCorrelation()
        assertTrue("Mono correlation should be ~1.0, was $correlation", correlation > 0.95f)
    }

    @Test
    fun testStereoPhaseCorrelationAntiPhase() {
        // Anti-phase signal (L == -R)
        val n = 512
        val left = FloatArray(n) { sin(it * 2.0 * PI / 32.0).toFloat() }
        val right = FloatArray(n) { -sin(it * 2.0 * PI / 32.0).toFloat() }
        engine.processStereoBuffer(left, right)

        val correlation = engine.computeStereoPhaseCorrelation()
        assertTrue("Anti-phase correlation should be ~ -1.0, was $correlation", correlation < -0.95f)
    }

    @Test
    fun testStereoPhaseCorrelationStereoWide() {
        // Orthogonal 90° quadrature signal (sin vs cos)
        val n = 512
        val left = FloatArray(n) { sin(it * 2.0 * PI / 32.0).toFloat() }
        val right = FloatArray(n) { kotlin.math.cos(it * 2.0 * PI / 32.0).toFloat() }
        engine.processStereoBuffer(left, right)

        val correlation = engine.computeStereoPhaseCorrelation()
        assertTrue("Quadrature phase correlation should be near 0.0, was $correlation", kotlin.math.abs(correlation) < 0.2f)
    }

    @Test
    fun testVectorPointsGenerationAllModes() {
        val left = FloatArray(256) { 0.4f * sin(it * 0.2f) }
        val right = FloatArray(256) { 0.4f * kotlin.math.cos(it * 0.2f) }
        engine.processStereoBuffer(left, right)

        val w = 600f
        val h = 600f

        OscilloscopeDisplayMode.entries.forEach { mode ->
            engine.setDisplayMode(mode)
            val points = engine.generateVectorPoints(w, h, 64)
            assertTrue("Mode $mode should generate vector points", points.isNotEmpty())
            points.forEach { pt ->
                assertTrue("X coordinate should be finite", !pt.x.isNaN() && !pt.x.isInfinite())
                assertTrue("Y coordinate should be finite", !pt.y.isNaN() && !pt.y.isInfinite())
                assertTrue("Intensity should be between 0 and 1", pt.intensity in 0.0f..1.0f)
            }
        }
    }

    @Test
    fun test3DEulerRotationAndPhaseRotation() {
        engine.update3DRotation(pitchDeg = 45f, yawDeg = 120f)
        assertEquals(45f, engine.settings.value.beamGeometry.eulerPitchDeg, 0.01f)
        assertEquals(120f, engine.settings.value.beamGeometry.eulerYawDeg, 0.01f)

        // Test pitch clamping
        engine.update3DRotation(pitchDeg = 150f, yawDeg = 400f)
        assertEquals(90f, engine.settings.value.beamGeometry.eulerPitchDeg, 0.01f)
        assertEquals(40f, engine.settings.value.beamGeometry.eulerYawDeg, 0.01f)

        engine.setPhaseRotationDeg(450f)
        assertEquals(90f, engine.settings.value.beamGeometry.audioPhaseRotationDeg, 0.01f)
    }

    @Test
    fun testToggleEnabled() {
        val initial = engine.settings.value.isEnabled
        engine.toggleEnabled()
        assertEquals(!initial, engine.settings.value.isEnabled)
        engine.toggleEnabled()
        assertEquals(initial, engine.settings.value.isEnabled)
    }
}
