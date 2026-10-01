package com.pixelody.app

import com.pixelody.app.core.playback.StemsIsolatorEngine
import com.pixelody.app.data.model.StemPreset
import com.pixelody.app.data.model.StemType
import com.pixelody.app.data.model.StemsIsolatorSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class StemsIsolatorEngineTest {

    @Test
    fun testDefaultSettingsAndTelemetry() {
        val engine = StemsIsolatorEngine()
        val settings = engine.settings.value
        val telemetry = engine.telemetry.value

        assertTrue(settings.isEnabled)
        assertEquals(StemPreset.FullMix, settings.activePreset)
        assertEquals(4, settings.channels.size)
        assertEquals(0.0f, settings.masterGainDb, 0.01f)
        assertEquals(0.0f, settings.crossfaderPosition, 0.01f)

        assertNotNull(telemetry)
        assertEquals(4, telemetry.channelTelemetry.size)
        assertEquals(0, telemetry.activeSoloCount)
        assertEquals(1.0f, telemetry.crossfaderDeckAGain, 0.05f)
        assertEquals(1.0f, telemetry.crossfaderDeckBGain, 0.05f)
    }

    @Test
    fun testPresetApplication() {
        val engine = StemsIsolatorEngine()

        // 1. Acapella Extract: Vocals solo/unmuted, others muted
        engine.applyPreset(StemPreset.AcapellaExtract)
        val acapellaChannels = engine.settings.value.channels
        assertFalse(acapellaChannels[StemType.Vocals]!!.isMuted)
        assertTrue(acapellaChannels[StemType.Drums]!!.isMuted)
        assertTrue(acapellaChannels[StemType.Bass]!!.isMuted)
        assertTrue(acapellaChannels[StemType.Instruments]!!.isMuted)

        // 2. Instrumental Karaoke: Vocals muted, others unmuted
        engine.applyPreset(StemPreset.InstrumentalKaraoke)
        val karaokeChannels = engine.settings.value.channels
        assertTrue(karaokeChannels[StemType.Vocals]!!.isMuted)
        assertFalse(karaokeChannels[StemType.Drums]!!.isMuted)
        assertFalse(karaokeChannels[StemType.Bass]!!.isMuted)
        assertFalse(karaokeChannels[StemType.Instruments]!!.isMuted)

        // 3. Drum & Bass: Rhythm section active, Vocals/Instruments muted
        engine.applyPreset(StemPreset.DrumAndBass)
        val dnbChannels = engine.settings.value.channels
        assertTrue(dnbChannels[StemType.Vocals]!!.isMuted)
        assertFalse(dnbChannels[StemType.Drums]!!.isMuted)
        assertFalse(dnbChannels[StemType.Bass]!!.isMuted)
        assertTrue(dnbChannels[StemType.Instruments]!!.isMuted)
    }

    @Test
    fun testGainControlAndMuting() {
        val engine = StemsIsolatorEngine()

        engine.setStemGain(StemType.Vocals, 4.5f)
        assertEquals(4.5f, engine.settings.value.channels[StemType.Vocals]!!.gainDb, 0.01f)

        engine.toggleMute(StemType.Drums)
        assertTrue(engine.settings.value.channels[StemType.Drums]!!.isMuted)

        engine.toggleMute(StemType.Drums)
        assertFalse(engine.settings.value.channels[StemType.Drums]!!.isMuted)
    }

    @Test
    fun testSoloMatrixRouting() {
        val engine = StemsIsolatorEngine()

        // Soloing Bass
        engine.toggleSolo(StemType.Bass)
        assertTrue(engine.settings.value.channels[StemType.Bass]!!.isSoloed)
        assertEquals(1, engine.telemetry.value.activeSoloCount)

        // Soloing Vocals in addition (multi-solo)
        engine.toggleSolo(StemType.Vocals)
        assertTrue(engine.settings.value.channels[StemType.Vocals]!!.isSoloed)
        assertEquals(2, engine.telemetry.value.activeSoloCount)

        // Unsoloing both
        engine.toggleSolo(StemType.Bass)
        engine.toggleSolo(StemType.Vocals)
        assertEquals(0, engine.telemetry.value.activeSoloCount)
    }

    @Test
    fun testDjSweepFilter() {
        val engine = StemsIsolatorEngine()

        // Set LPF (0.2)
        engine.setStemFilter(StemType.Instruments, 0.2f)
        assertEquals(0.2f, engine.settings.value.channels[StemType.Instruments]!!.filterCutoffNormalized, 0.01f)

        // Set HPF (0.8)
        engine.setStemFilter(StemType.Bass, 0.8f)
        assertEquals(0.8f, engine.settings.value.channels[StemType.Bass]!!.filterCutoffNormalized, 0.01f)
    }

    @Test
    fun testParametricEq() {
        val engine = StemsIsolatorEngine()

        engine.setStemEq(StemType.Drums, lowDb = 3.0f, midDb = -2.0f, highDb = 5.0f)
        val ch = engine.settings.value.channels[StemType.Drums]!!
        assertEquals(3.0f, ch.eqLowDb, 0.01f)
        assertEquals(-2.0f, ch.eqMidDb, 0.01f)
        assertEquals(5.0f, ch.eqHighDb, 0.01f)
    }

    @Test
    fun testCrossfaderDeckGains() {
        val engine = StemsIsolatorEngine()

        // Center (0.0) -> equal gains
        engine.setCrossfaderPosition(0.0f)
        val centerTelem = engine.telemetry.value
        assertEquals(1.0f, centerTelem.crossfaderDeckAGain, 0.05f)
        assertEquals(1.0f, centerTelem.crossfaderDeckBGain, 0.05f)

        // Deck A full left (-1.0)
        engine.setCrossfaderPosition(-1.0f)
        val leftTelem = engine.telemetry.value
        assertTrue(leftTelem.crossfaderDeckAGain > 1.3f)
        assertEquals(0.0f, leftTelem.crossfaderDeckBGain, 0.05f)

        // Deck B full right (+1.0)
        engine.setCrossfaderPosition(1.0f)
        val rightTelem = engine.telemetry.value
        assertEquals(0.0f, rightTelem.crossfaderDeckAGain, 0.05f)
        assertTrue(rightTelem.crossfaderDeckBGain > 1.3f)
    }

    @Test
    fun testStereoFrameProcessing() {
        val engine = StemsIsolatorEngine()

        val inputL = 0.6f
        val inputR = -0.4f

        var outL = 0f
        var outR = 0f
        for (i in 0 until 400) {
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
        val engine = StemsIsolatorEngine()

        val bufferSize = 512
        val leftChan = FloatArray(bufferSize) { 0.35f }
        val rightChan = FloatArray(bufferSize) { -0.35f }

        engine.processStereoBuffer(leftChan, rightChan)

        for (i in 0 until bufferSize) {
            assertFalse(leftChan[i].isNaN())
            assertFalse(rightChan[i].isNaN())
        }
    }

    @Test
    fun testBypassMode() {
        val engine = StemsIsolatorEngine()
        engine.toggleEnabled() // Disable

        assertFalse(engine.settings.value.isEnabled)
        val (outL, outR) = engine.processStereoFrame(0.85f, -0.65f)
        assertEquals(0.85f, outL, 0.001f)
        assertEquals(-0.65f, outR, 0.001f)
    }
}
