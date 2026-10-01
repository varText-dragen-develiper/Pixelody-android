package com.pixelody.app

import com.pixelody.app.core.playback.CrossfadeController
import com.pixelody.app.core.playback.CrossfadeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class CrossfadeControllerTest {

    @Test
    fun `equal power crossfade preserves total energy`() {
        // For equal-power crossfade, outGain^2 + inGain^2 = 1.0 (total power is constant)
        for (step in 0..10) {
            val progress = step / 10f
            val (outGain, inGain) = CrossfadeController.calculateGains(progress, CrossfadeMode.EqualPower5s)
            val totalPower = (outGain * outGain) + (inGain * inGain)
            assertEquals(1.0f, totalPower, 0.01f)
        }
    }

    @Test
    fun `equal power boundary gains are exact`() {
        val (startOut, startIn) = CrossfadeController.calculateGains(0f, CrossfadeMode.EqualPower5s)
        assertEquals(1.0f, startOut, 0.001f)
        assertEquals(0.0f, startIn, 0.001f)

        val (endOut, endIn) = CrossfadeController.calculateGains(1f, CrossfadeMode.EqualPower5s)
        assertEquals(0.0f, endOut, 0.001f)
        assertEquals(1.0f, endIn, 0.001f)

        val (midOut, midIn) = CrossfadeController.calculateGains(0.5f, CrossfadeMode.EqualPower5s)
        val expectedMid = (sqrt(2.0) / 2.0).toFloat()
        assertEquals(expectedMid, midOut, 0.01f)
        assertEquals(expectedMid, midIn, 0.01f)
    }

    @Test
    fun `mode change updates state and resets crossfading`() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val controller = CrossfadeController(scope = testScope)
            assertEquals(CrossfadeMode.Off, controller.state.value.mode)

            controller.setMode(CrossfadeMode.ClubBlend8s)
            val state = controller.state.value
            assertEquals(CrossfadeMode.ClubBlend8s, state.mode)
            assertFalse(state.isCrossfading)
            assertEquals(1.0f, state.outgoingGain, 0.001f)
            assertEquals(1.0f, state.incomingGain, 0.001f)
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun `cancel transition restores full gains and stops crossfading`() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        try {
            var updatedOut = 0f
            var updatedIn = 0f
            val controller = CrossfadeController(
                scope = testScope,
                onGainUpdate = { outG, inG ->
                    updatedOut = outG
                    updatedIn = inG
                }
            )

            controller.setMode(CrossfadeMode.Extended12s)
            controller.cancelTransition()

            val state = controller.state.value
            assertFalse(state.isCrossfading)
            assertEquals(1.0f, state.outgoingGain, 0.001f)
            assertEquals(1.0f, state.incomingGain, 0.001f)
            assertEquals(1.0f, updatedOut, 0.001f)
            assertEquals(1.0f, updatedIn, 0.001f)
        } finally {
            testScope.cancel()
        }
    }
}
