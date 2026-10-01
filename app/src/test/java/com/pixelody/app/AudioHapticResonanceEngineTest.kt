package com.pixelody.app

import com.pixelody.app.core.playback.AudioHapticResonanceEngine
import com.pixelody.app.data.model.AudioHapticMode
import com.pixelody.app.data.model.AudioHapticSettings
import com.pixelody.app.data.model.HapticPrimitiveType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioHapticResonanceEngineTest {

    @Test
    fun testAudioHapticEngineInitialState() {
        val engine = AudioHapticResonanceEngine()
        val settings = engine.settings.value

        assertTrue(settings.isEnabled)
        assertEquals(AudioHapticMode.SubBassRumble, settings.mode)
        assertEquals(0.75f, settings.intensity, 0.001f)
        assertEquals(1.2f, settings.subBassBoost, 0.001f)
        assertTrue(settings.turntableHapticsEnabled)
        assertNull(engine.latestPulse.value)
    }

    @Test
    fun testSettingsMutationsAndCoercion() {
        val engine = AudioHapticResonanceEngine()

        engine.setMode(AudioHapticMode.BeatPunch)
        assertEquals(AudioHapticMode.BeatPunch, engine.settings.value.mode)

        engine.setIntensity(1.5f) // Should coerce to 1.0f
        assertEquals(1.0f, engine.settings.value.intensity, 0.001f)

        engine.setIntensity(-0.2f) // Should coerce to 0.0f
        assertEquals(0.0f, engine.settings.value.intensity, 0.001f)

        engine.setSubBassBoost(3.0f) // Should coerce to 2.0f
        assertEquals(2.0f, engine.settings.value.subBassBoost, 0.001f)

        engine.toggleEnabled(false)
        assertEquals(false, engine.settings.value.isEnabled)
    }

    @Test
    fun testProcessAudioFrameWhenDisabledOrNotPlaying() {
        val engine = AudioHapticResonanceEngine()
        engine.toggleEnabled(false)

        engine.processAudioFrame(
            lowBandEnergy = 0.9f,
            midBandEnergy = 0.8f,
            highBandEnergy = 0.7f,
            isPlaying = true,
            nowMs = 1000L
        )
        assertNull(engine.latestPulse.value)

        engine.toggleEnabled(true)
        engine.processAudioFrame(
            lowBandEnergy = 0.9f,
            midBandEnergy = 0.8f,
            highBandEnergy = 0.7f,
            isPlaying = false,
            nowMs = 1100L
        )
        assertNull(engine.latestPulse.value)

        engine.setMode(AudioHapticMode.Off)
        engine.processAudioFrame(
            lowBandEnergy = 0.9f,
            midBandEnergy = 0.8f,
            highBandEnergy = 0.7f,
            isPlaying = true,
            nowMs = 1200L
        )
        assertNull(engine.latestPulse.value)
    }

    @Test
    fun testSubBassRumbleModeTriggering() {
        val engine = AudioHapticResonanceEngine(
            initialSettings = AudioHapticSettings(
                mode = AudioHapticMode.SubBassRumble,
                intensity = 1.0f,
                subBassBoost = 1.2f,
                transientSensitivity = 0.75f
            )
        )

        // Baseline frame
        engine.processAudioFrame(0.1f, 0.1f, 0.1f, isPlaying = true, nowMs = 1000L)

        // Bass onset transient frame > 80ms later
        engine.processAudioFrame(0.8f, 0.2f, 0.1f, isPlaying = true, nowMs = 1100L)

        val pulse = engine.latestPulse.value
        assertNotNull(pulse)
        assertEquals(HapticPrimitiveType.LowTick, pulse?.primitive)
        assertEquals("SUB", pulse?.bandLabel)
        assertEquals(1100L, pulse?.timestamp)
        assertTrue((pulse?.amplitude ?: 0f) > 0.5f)
    }

    @Test
    fun testBeatPunchModeTriggering() {
        val engine = AudioHapticResonanceEngine(
            initialSettings = AudioHapticSettings(
                mode = AudioHapticMode.BeatPunch,
                intensity = 0.9f,
                transientSensitivity = 0.8f
            )
        )

        // Baseline
        engine.processAudioFrame(0.1f, 0.1f, 0.1f, isPlaying = true, nowMs = 2000L)

        // Mid punch kick transient
        engine.processAudioFrame(0.2f, 0.85f, 0.1f, isPlaying = true, nowMs = 2100L)

        val kickPulse = engine.latestPulse.value
        assertNotNull(kickPulse)
        assertEquals(HapticPrimitiveType.Click, kickPulse?.primitive)
        assertEquals("KICK", kickPulse?.bandLabel)

        // Followed by low bass punch
        engine.processAudioFrame(0.85f, 0.2f, 0.1f, isPlaying = true, nowMs = 2200L)
        val bassPulse = engine.latestPulse.value
        assertNotNull(bassPulse)
        assertEquals(HapticPrimitiveType.LowTick, bassPulse?.primitive)
        assertEquals("PUNCH", bassPulse?.bandLabel)
    }

    @Test
    fun testFullSpectrumModeTriggering() {
        val engine = AudioHapticResonanceEngine(
            initialSettings = AudioHapticSettings(
                mode = AudioHapticMode.FullSpectrum,
                intensity = 1.0f,
                transientSensitivity = 0.8f
            )
        )

        // High hat transient
        engine.processAudioFrame(0.1f, 0.1f, 0.8f, isPlaying = true, nowMs = 3000L)
        val hatPulse = engine.latestPulse.value
        assertNotNull(hatPulse)
        assertEquals(HapticPrimitiveType.Tick, hatPulse?.primitive)
        assertEquals("HAT", hatPulse?.bandLabel)
    }

    @Test
    fun testRefractoryCooldownSuppression() {
        val engine = AudioHapticResonanceEngine(
            initialSettings = AudioHapticSettings(
                mode = AudioHapticMode.SubBassRumble,
                intensity = 1.0f
            )
        )

        // Baseline
        engine.processAudioFrame(0.1f, 0.1f, 0.1f, isPlaying = true, nowMs = 3900L)

        // First strong pulse
        engine.processAudioFrame(0.8f, 0.1f, 0.1f, isPlaying = true, nowMs = 4000L)
        val pulse1 = engine.latestPulse.value
        assertNotNull(pulse1)
        assertEquals(4000L, pulse1?.timestamp)

        // Immediate subsequent frame within refractory window (30ms < 80ms)
        engine.processAudioFrame(0.9f, 0.1f, 0.1f, isPlaying = true, nowMs = 4030L)
        // Timestamp must still be 4000L (suppressed)
        assertEquals(4000L, engine.latestPulse.value?.timestamp)

        // Trough frame to create low onset baseline
        engine.processAudioFrame(0.1f, 0.1f, 0.1f, isPlaying = true, nowMs = 4090L)

        // Frame after refractory period (100ms > 80ms) with strong onset (0.95 - 0.1 = 0.85 > 0.08)
        engine.processAudioFrame(0.95f, 0.1f, 0.1f, isPlaying = true, nowMs = 4100L)
        assertEquals(4100L, engine.latestPulse.value?.timestamp)
    }

    @Test
    fun testTurntableHapticFeedback() {
        val engine = AudioHapticResonanceEngine(
            initialSettings = AudioHapticSettings(turntableHapticsEnabled = true)
        )

        engine.pulseTurntableNeedleDrop()
        val needlePulse = engine.latestPulse.value
        assertNotNull(needlePulse)
        assertEquals(HapticPrimitiveType.QuickFall, needlePulse?.primitive)
        assertEquals("NEEDLE", needlePulse?.bandLabel)

        engine.pulseTurntableBrake()
        val brakePulse = engine.latestPulse.value
        assertNotNull(brakePulse)
        assertEquals(HapticPrimitiveType.Spin, brakePulse?.primitive)
        assertEquals("BRAKE", brakePulse?.bandLabel)

        // When turntable haptics disabled
        engine.updateSettings(engine.settings.value.copy(turntableHapticsEnabled = false))
        engine.testPulse(HapticPrimitiveType.Click) // Set last pulse to Click
        assertEquals(HapticPrimitiveType.Click, engine.latestPulse.value?.primitive)

        engine.pulseTurntableNeedleDrop()
        // Must remain Click
        assertEquals(HapticPrimitiveType.Click, engine.latestPulse.value?.primitive)
    }

    @Test
    fun testManualTestPulse() {
        val engine = AudioHapticResonanceEngine(
            initialSettings = AudioHapticSettings(intensity = 0.8f)
        )

        engine.testPulse(HapticPrimitiveType.QuickRise, intensity = 0.5f)
        val pulse = engine.latestPulse.value
        assertNotNull(pulse)
        assertEquals(HapticPrimitiveType.QuickRise, pulse?.primitive)
        assertEquals("TEST", pulse?.bandLabel)
        assertEquals(0.4f, pulse?.amplitude ?: 0f, 0.01f) // 0.5 * 0.8 = 0.4
    }
}
