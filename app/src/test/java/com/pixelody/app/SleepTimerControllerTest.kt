package com.pixelody.app

import com.pixelody.app.core.playback.SleepTimerController
import com.pixelody.app.core.playback.SleepTimerMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepTimerControllerTest {

    @Test
    fun `logarithmic fade scales smoothly to zero`() {
        val full = SleepTimerController.calculateLogarithmicFade(60L, 60L)
        val mid = SleepTimerController.calculateLogarithmicFade(30L, 60L)
        val zero = SleepTimerController.calculateLogarithmicFade(0L, 60L)

        assertEquals(1.0f, full, 0.001f)
        assertTrue(mid in 0.5f..0.85f)
        assertEquals(0.0f, zero, 0.001f)
    }

    @Test
    fun `set timer starts active countdown with correct remaining seconds`() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val controller = SleepTimerController(
                scope = testScope
            )

            controller.setTimer(SleepTimerMode.FifteenMinutes)
            val state = controller.state.value

            assertTrue(state.active)
            assertEquals(15 * 60L, state.remainingSeconds)
            assertEquals(SleepTimerMode.FifteenMinutes, state.mode)
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun `add 5 minutes increases remaining time by 300 seconds`() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val controller = SleepTimerController(
                scope = testScope
            )

            controller.setTimer(SleepTimerMode.FifteenMinutes)
            controller.addFiveMinutes()

            val state = controller.state.value
            assertTrue(state.remainingSeconds >= 15 * 60L + 295L)
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun `cancel timer resets state and restores volume scale`() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        try {
            var lastVolume = 0.5f
            val controller = SleepTimerController(
                scope = testScope,
                onVolumeScaleChange = { lastVolume = it }
            )

            controller.setTimer(SleepTimerMode.ThirtyMinutes)
            assertTrue(controller.state.value.active)

            controller.cancel()
            assertFalse(controller.state.value.active)
            assertEquals(1.0f, lastVolume, 0.001f)
        } finally {
            testScope.cancel()
        }
    }
}
