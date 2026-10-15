package com.pixelody.app

import com.pixelody.app.core.playback.MultibandCrossfaderEngine
import com.pixelody.app.data.model.DjTransitionCurve
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MultibandCrossfaderEngineTest {

    @Test
    fun `EqualPower produces smooth sinusoidal gains without bass swap or vocal ducking`() {
        val startFrame = MultibandCrossfaderEngine.calculateMultibandFrame(0f, DjTransitionCurve.EqualPower)
        assertEquals(1.0f, startFrame.deckALowGain, 0.01f)
        assertEquals(0.0f, startFrame.deckBLowGain, 0.01f)
        assertFalse(startFrame.isVocalDucked)

        val midFrame = MultibandCrossfaderEngine.calculateMultibandFrame(0.5f, DjTransitionCurve.EqualPower)
        assertEquals(0.707f, midFrame.deckALowGain, 0.01f)
        assertEquals(0.707f, midFrame.deckBLowGain, 0.01f)

        val endFrame = MultibandCrossfaderEngine.calculateMultibandFrame(1.0f, DjTransitionCurve.EqualPower)
        assertEquals(0.0f, endFrame.deckALowGain, 0.01f)
        assertEquals(1.0f, endFrame.deckBLowGain, 0.01f)
    }

    @Test
    fun `BassSwap performs instant low-end swap at exactly 50 percent threshold`() {
        // Before 50% threshold: Deck A low is full, Deck B low is zero
        val beforeSwap = MultibandCrossfaderEngine.calculateMultibandFrame(0.40f, DjTransitionCurve.BassSwap)
        assertEquals(1.0f, beforeSwap.deckALowGain, 0.001f)
        assertEquals(0.0f, beforeSwap.deckBLowGain, 0.001f)
        assertFalse(beforeSwap.isBassSwapped)

        // At/After 50% threshold: Deck A low is cut to 0, Deck B low is full
        val afterSwap = MultibandCrossfaderEngine.calculateMultibandFrame(0.55f, DjTransitionCurve.BassSwap)
        assertEquals(0.0f, afterSwap.deckALowGain, 0.001f)
        assertEquals(1.0f, afterSwap.deckBLowGain, 0.001f)
        assertTrue(afterSwap.isBassSwapped)
    }

    @Test
    fun `BassSwap applies vocal midrange ducking during mid overlap zone`() {
        // At 50% progress, overlap is active, Deck A mid should be ducked by -4dB (~0.631 multiplier)
        val midFrame = MultibandCrossfaderEngine.calculateMultibandFrame(0.50f, DjTransitionCurve.BassSwap)
        assertTrue(midFrame.isVocalDucked)
        // 0.707 * 0.631 = ~0.446
        assertEquals(0.446f, midFrame.deckAMidGain, 0.03f)

        // At 10% progress (outside overlap zone), no ducking
        val earlyFrame = MultibandCrossfaderEngine.calculateMultibandFrame(0.10f, DjTransitionCurve.BassSwap)
        assertFalse(earlyFrame.isVocalDucked)
    }

    @Test
    fun `FilterSweep sweeps high-pass cutoff exponentially`() {
        val startFrame = MultibandCrossfaderEngine.calculateMultibandFrame(0f, DjTransitionCurve.FilterSweep)
        assertEquals(20f, startFrame.highPassFilterHz, 1f)

        val midFrame = MultibandCrossfaderEngine.calculateMultibandFrame(0.5f, DjTransitionCurve.FilterSweep)
        assertTrue("High-pass cutoff should sweep up at midpoint", midFrame.highPassFilterHz > 200f)

        val endFrame = MultibandCrossfaderEngine.calculateMultibandFrame(1.0f, DjTransitionCurve.FilterSweep)
        assertEquals(3500f, endFrame.highPassFilterHz, 1f)
    }

    @Test
    fun `EchoOut activates echo delay tail wet level after 35 percent progress`() {
        val beforeEcho = MultibandCrossfaderEngine.calculateMultibandFrame(0.20f, DjTransitionCurve.EchoOut)
        assertEquals(0f, beforeEcho.echoDecayGain, 0.01f)
        assertEquals(1.0f, beforeEcho.deckALowGain, 0.01f)

        val afterEcho = MultibandCrossfaderEngine.calculateMultibandFrame(0.50f, DjTransitionCurve.EchoOut)
        assertTrue("Echo wet level should be active", afterEcho.echoDecayGain > 0f)
        assertEquals(0.0f, afterEcho.deckALowGain, 0.001f)
    }

    @Test
    fun `VinylBrake decelerates playback speed factor before 85 percent progress`() {
        val brakeFrame = MultibandCrossfaderEngine.calculateMultibandFrame(0.60f, DjTransitionCurve.VinylBrake)
        assertTrue("Playback speed should decrease during vinyl brake", brakeFrame.tempoStretchFactor < 1.0f)
    }
}
