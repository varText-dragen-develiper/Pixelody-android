package com.pixelody.app

import com.pixelody.app.core.playback.JamClockSyncEngine
import com.pixelody.app.data.model.JamClockSyncSample
import com.pixelody.app.data.model.JamSyncMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JamClockSyncEngineTest {

    @Test
    fun ntpSampleCalculatesRttAndRawOffsetCorrectly() {
        val sample = JamClockSyncSample(
            t0ClientSendMs = 1000L,
            t1ServerReceiveMs = 1050L,
            t2ServerSendMs = 1055L,
            t3ClientReceiveMs = 1115L
        )

        // RTT = (1115 - 1000) - (1055 - 1050) = 115 - 5 = 110ms
        assertEquals(110L, sample.roundTripTimeMs)

        // Raw Offset = ((1050 - 1000) + (1055 - 1115)) / 2 = (50 + (-60)) / 2 = -10 / 2 = -5ms
        assertEquals(-5L, sample.rawClockOffsetMs)
    }

    @Test
    fun clockSyncEngineAveragesAndFiltersSamples() {
        val engine = JamClockSyncEngine()
        engine.setMode(JamSyncMode.SynchronizedListener)

        // Add consistent samples with +20ms offset and 30ms RTT
        for (i in 1..5) {
            val t0 = 1000L * i
            engine.recordSample(
                t0ClientSendMs = t0,
                t1ServerReceiveMs = t0 + 35L,
                t2ServerSendMs = t0 + 37L,
                t3ClientReceiveMs = t0 + 32L
            )
        }

        // Offset should be positive around ~20ms
        assertTrue(engine.getClockOffsetMs() in 18L..22L)
        assertTrue(engine.getRoundTripTimeMs() in 28L..32L)
    }

    @Test
    fun playbackAdjustmentInSyncZoneMaintainsStandardSpeed() {
        val engine = JamClockSyncEngine(inSyncThresholdMs = 25L)
        val targetPos = 50_000L

        // Within ±25ms: speed factor should be exactly 1.0f
        val (drift10, speed10) = engine.calculatePlaybackAdjustment(
            localPlayerPositionMs = targetPos + 10L,
            targetPositionMs = targetPos
        )
        assertEquals(10L, drift10)
        assertEquals(1.0f, speed10, 0.0001f)
        assertFalse(engine.shouldHardSeek(targetPos + 10L, targetPos))

        val (driftMinus20, speedMinus20) = engine.calculatePlaybackAdjustment(
            localPlayerPositionMs = targetPos - 20L,
            targetPositionMs = targetPos
        )
        assertEquals(-20L, driftMinus20)
        assertEquals(1.0f, speedMinus20, 0.0001f)
    }

    @Test
    fun playbackAdjustmentMicroAdjustsSmoothly() {
        val engine = JamClockSyncEngine(
            inSyncThresholdMs = 25L,
            microAdjustMaxThresholdMs = 300L,
            maxSpeedAdjustmentFactor = 0.015f
        )
        val targetPos = 50_000L

        // Local player is ahead (+150ms) -> slightly slow down (< 1.0f)
        val (driftAhead, speedAhead) = engine.calculatePlaybackAdjustment(
            localPlayerPositionMs = targetPos + 150L,
            targetPositionMs = targetPos
        )
        assertEquals(150L, driftAhead)
        assertTrue(speedAhead in 0.990f..0.996f)
        assertFalse(engine.shouldHardSeek(targetPos + 150L, targetPos))

        // Local player is behind (-150ms) -> slightly speed up (> 1.0f)
        val (driftBehind, speedBehind) = engine.calculatePlaybackAdjustment(
            localPlayerPositionMs = targetPos - 150L,
            targetPositionMs = targetPos
        )
        assertEquals(-150L, driftBehind)
        assertTrue(speedBehind in 1.004f..1.010f)
        assertFalse(engine.shouldHardSeek(targetPos - 150L, targetPos))
    }

    @Test
    fun playbackAdjustmentTriggersHardSeekOnLargeDrift() {
        val engine = JamClockSyncEngine(microAdjustMaxThresholdMs = 300L)
        val targetPos = 50_000L

        assertTrue(engine.shouldHardSeek(localPlayerPositionMs = targetPos + 500L, targetPositionMs = targetPos))
        assertTrue(engine.shouldHardSeek(localPlayerPositionMs = targetPos - 400L, targetPositionMs = targetPos))
        assertFalse(engine.shouldHardSeek(localPlayerPositionMs = targetPos + 100L, targetPositionMs = targetPos))
    }

    @Test
    fun targetPositionProjectsAccuratelyOverTime() {
        val engine = JamClockSyncEngine()
        engine.recordSample(1000L, 1020L, 1020L, 1040L) // 0 offset

        val anchorPos = 10_000L
        val anchorTime = 1_000_000L

        // 3500ms elapsed
        val targetPos = engine.computeTargetPositionMs(
            anchorPositionMs = anchorPos,
            anchorTimestampMs = anchorTime,
            localNowMs = anchorTime + 3500L,
            isPlaying = true
        )
        assertEquals(13_500L, targetPos)

        // When paused, target position remains at anchor
        val pausedTargetPos = engine.computeTargetPositionMs(
            anchorPositionMs = anchorPos,
            anchorTimestampMs = anchorTime,
            localNowMs = anchorTime + 3500L,
            isPlaying = false
        )
        assertEquals(10_000L, pausedTargetPos)
    }
}
