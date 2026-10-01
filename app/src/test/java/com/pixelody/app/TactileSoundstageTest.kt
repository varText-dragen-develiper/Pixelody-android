package com.pixelody.app

import com.pixelody.app.core.playback.TactileSoundstageCoordinator
import com.pixelody.app.data.model.StemAnalogRoute
import com.pixelody.app.data.model.TactileDeckMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TactileSoundstageTest {

    @Before
    fun setUp() {
        TactileSoundstageCoordinator.applyPerformanceMacro("reset")
        TactileSoundstageCoordinator.setDeckMode(TactileDeckMode.MasterBlend)
    }

    @Test
    fun `setDeckMode updates both routing matrix and soundstage frame`() {
        TactileSoundstageCoordinator.setDeckMode(TactileDeckMode.Turntable)

        assertEquals(TactileDeckMode.Turntable, TactileSoundstageCoordinator.routingMatrix.value.activeDeckMode)
        assertEquals(TactileDeckMode.Turntable, TactileSoundstageCoordinator.soundstageFrame.value.activeDeckMode)
    }

    @Test
    fun `setStemGain clamps values and isolates targets accurately`() {
        TactileSoundstageCoordinator.setStemGain("vocals", 1.20f)
        TactileSoundstageCoordinator.setStemGain("drums", 0.50f)
        TactileSoundstageCoordinator.setStemGain("bass", -0.10f)

        val frame = TactileSoundstageCoordinator.soundstageFrame.value
        assertEquals(1.20f, frame.vocalsGain, 0.01f)
        assertEquals(0.50f, frame.drumsGain, 0.01f)
        assertEquals(0.0f, frame.bassGain, 0.01f)
    }

    @Test
    fun `setStemRoute updates matrix without modifying other stem routes`() {
        TactileSoundstageCoordinator.setStemRoute("vocals", StemAnalogRoute.TapeSaturation)
        TactileSoundstageCoordinator.setStemRoute("drums", StemAnalogRoute.VinylWarp)

        val matrix = TactileSoundstageCoordinator.routingMatrix.value
        assertEquals(StemAnalogRoute.TapeSaturation, matrix.vocalRoute)
        assertEquals(StemAnalogRoute.VinylWarp, matrix.drumsRoute)
        assertEquals(StemAnalogRoute.DirectClean, matrix.bassRoute)
        assertEquals(StemAnalogRoute.DirectClean, matrix.otherRoute)
    }

    @Test
    fun `applyPerformanceMacro applies Acapella, Instrumental, and Tape Warmth presets`() {
        // 1. Acapella
        TactileSoundstageCoordinator.applyPerformanceMacro("acapella")
        var frame = TactileSoundstageCoordinator.soundstageFrame.value
        assertEquals(1.0f, frame.vocalsGain, 0.01f)
        assertEquals(0.0f, frame.drumsGain, 0.01f)
        assertEquals(0.0f, frame.bassGain, 0.01f)
        assertTrue(frame.isMacroActive)
        assertEquals("ACAPELLA", frame.activeMacroName)

        // 2. Instrumental
        TactileSoundstageCoordinator.applyPerformanceMacro("instrumental")
        frame = TactileSoundstageCoordinator.soundstageFrame.value
        assertEquals(0.0f, frame.vocalsGain, 0.01f)
        assertEquals(1.0f, frame.drumsGain, 0.01f)
        assertEquals(1.0f, frame.bassGain, 0.01f)
        assertEquals(1.0f, frame.otherGain, 0.01f)

        // 3. Tape Warmth
        TactileSoundstageCoordinator.applyPerformanceMacro("tapewarmth")
        val matrix = TactileSoundstageCoordinator.routingMatrix.value
        assertEquals(StemAnalogRoute.TapeSaturation, matrix.vocalRoute)
        assertEquals(0.70f, matrix.analogSaturationDrive, 0.01f)

        // 4. Reset
        TactileSoundstageCoordinator.applyPerformanceMacro("reset")
        frame = TactileSoundstageCoordinator.soundstageFrame.value
        assertEquals(1.0f, frame.vocalsGain, 0.01f)
        assertEquals(1.0f, frame.drumsGain, 0.01f)
        assertFalse(frame.isMacroActive)
    }

    @Test
    fun `updateFrameTelemetry propagates real-time analog parameters`() {
        TactileSoundstageCoordinator.updateFrameTelemetry(
            turntableRpm = 45.0f,
            turntableScratch = 0.35f,
            tapeWowFlutter = 0.98f,
            tapeSaturationRms = 0.42f,
            lissajousX = 0.85f,
            lissajousY = -0.40f
        )

        val frame = TactileSoundstageCoordinator.soundstageFrame.value
        assertEquals(45.0f, frame.turntableRpm, 0.01f)
        assertEquals(0.35f, frame.turntableScratchOffset, 0.01f)
        assertEquals(0.98f, frame.tapeWowFlutterFactor, 0.01f)
        assertEquals(0.42f, frame.tapeSaturationRms, 0.01f)
        assertEquals(0.85f, frame.phosphorLissajousX, 0.01f)
        assertEquals(-0.40f, frame.phosphorLissajousY, 0.01f)
    }
}
