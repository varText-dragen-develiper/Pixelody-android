package com.pixelody.app

import com.pixelody.app.core.playback.AutoDjTransitionEngine
import com.pixelody.app.data.model.DjTransitionCurve
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoDjTransitionEngineTest {

    @Test
    fun `equal power curve preserves total acoustic energy across transition`() {
        for (step in 0..10) {
            val progress = step / 10f
            val frame = AutoDjTransitionEngine.calculateEnvelopeFrame(progress, DjTransitionCurve.EqualPower)
            val totalPower = (frame.outgoingGain * frame.outgoingGain) + (frame.incomingGain * frame.incomingGain)
            assertEquals(1.0f, totalPower, 0.01f)
            assertEquals(1.0f, frame.playbackSpeedFactor, 0.001f)
        }
    }

    @Test
    fun `bass swap crossover cuts outgoing low-end at midpoint`() {
        val earlyFrame = AutoDjTransitionEngine.calculateEnvelopeFrame(0.25f, DjTransitionCurve.BassSwap)
        assertEquals(20f, earlyFrame.outgoingHighPassCutoffHz, 0.1f) // Full bass retained

        val lateFrame = AutoDjTransitionEngine.calculateEnvelopeFrame(0.75f, DjTransitionCurve.BassSwap)
        assertTrue(lateFrame.outgoingHighPassCutoffHz >= 350f) // Low frequencies cut to prevent mud
    }

    @Test
    fun `filter sweep modulates high-pass frequency exponentially`() {
        val startFrame = AutoDjTransitionEngine.calculateEnvelopeFrame(0.0f, DjTransitionCurve.FilterSweep)
        assertEquals(20f, startFrame.outgoingHighPassCutoffHz, 0.1f)

        val midFrame = AutoDjTransitionEngine.calculateEnvelopeFrame(0.5f, DjTransitionCurve.FilterSweep)
        assertTrue(midFrame.outgoingHighPassCutoffHz > 150f)

        val endFrame = AutoDjTransitionEngine.calculateEnvelopeFrame(1.0f, DjTransitionCurve.FilterSweep)
        assertEquals(3500f, endFrame.outgoingHighPassCutoffHz, 10f)
    }

    @Test
    fun `echo out generates feedback decay tail after handoff threshold`() {
        val earlyFrame = AutoDjTransitionEngine.calculateEnvelopeFrame(0.20f, DjTransitionCurve.EchoOut)
        assertEquals(1.0f, earlyFrame.outgoingGain, 0.01f)
        assertEquals(0.0f, earlyFrame.echoWetLevel, 0.01f)

        val midFrame = AutoDjTransitionEngine.calculateEnvelopeFrame(0.50f, DjTransitionCurve.EchoOut)
        assertEquals(0.0f, midFrame.outgoingGain, 0.01f)
        assertTrue(midFrame.echoWetLevel > 0.3f)
    }

    @Test
    fun `vinyl brake decelerates playback speed and pitch`() {
        val startFrame = AutoDjTransitionEngine.calculateEnvelopeFrame(0.0f, DjTransitionCurve.VinylBrake)
        assertEquals(1.0f, startFrame.playbackSpeedFactor, 0.01f)

        val decelFrame = AutoDjTransitionEngine.calculateEnvelopeFrame(0.70f, DjTransitionCurve.VinylBrake)
        assertTrue(decelFrame.playbackSpeedFactor < 0.6f)
        assertTrue(decelFrame.playbackSpeedFactor >= 0.08f)
    }

    @Test
    fun `transition cancel resets state to clean baseline`() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        try {
            var lastOutGain = 0f
            val engine = AutoDjTransitionEngine(
                scope = testScope,
                onDspUpdate = { frame -> lastOutGain = frame.outgoingGain }
            )

            engine.cancelTransition()
            val env = engine.envelope.value
            assertFalse(env.isTransitioning)
            assertEquals(1.0f, env.outgoingGain, 0.001f)
            assertEquals(0.0f, env.incomingGain, 0.001f)
            assertEquals(1.0f, env.playbackSpeedFactor, 0.001f)
            assertEquals(1.0f, lastOutGain, 0.001f)
        } finally {
            testScope.cancel()
        }
    }
}
