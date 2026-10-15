package com.pixelody.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class VelocityMicroScrubberContractTest {

    @Test
    fun scrubRateFromVerticalOffsetDpClamping() {
        // Full speed (< 35 dp)
        assertEquals(ScrubRate.Full, ScrubRate.fromVerticalOffsetDp(0f))
        assertEquals(ScrubRate.Full, ScrubRate.fromVerticalOffsetDp(20f))
        assertEquals(ScrubRate.Full, ScrubRate.fromVerticalOffsetDp(34.9f))

        // Half speed (35 dp .. < 80 dp)
        assertEquals(ScrubRate.Half, ScrubRate.fromVerticalOffsetDp(35f))
        assertEquals(ScrubRate.Half, ScrubRate.fromVerticalOffsetDp(50f))
        assertEquals(ScrubRate.Half, ScrubRate.fromVerticalOffsetDp(79.9f))

        // Quarter speed (80 dp .. < 140 dp)
        assertEquals(ScrubRate.Quarter, ScrubRate.fromVerticalOffsetDp(80f))
        assertEquals(ScrubRate.Quarter, ScrubRate.fromVerticalOffsetDp(100f))
        assertEquals(ScrubRate.Quarter, ScrubRate.fromVerticalOffsetDp(139.9f))

        // Fine micro seek (>= 140 dp)
        assertEquals(ScrubRate.Fine, ScrubRate.fromVerticalOffsetDp(140f))
        assertEquals(ScrubRate.Fine, ScrubRate.fromVerticalOffsetDp(250f))
    }

    @Test
    fun scrubRateMultipliersAndIntervals() {
        assertEquals(1.0f, ScrubRate.Full.multiplier, 0.0001f)
        assertEquals(5000L, ScrubRate.Full.stepIntervalMs)

        assertEquals(0.5f, ScrubRate.Half.multiplier, 0.0001f)
        assertEquals(2000L, ScrubRate.Half.stepIntervalMs)

        assertEquals(0.25f, ScrubRate.Quarter.multiplier, 0.0001f)
        assertEquals(1000L, ScrubRate.Quarter.stepIntervalMs)

        assertEquals(0.1f, ScrubRate.Fine.multiplier, 0.0001f)
        assertEquals(250L, ScrubRate.Fine.stepIntervalMs)
    }

    @Test
    fun computeScrubDeltaCalculations() {
        val totalWidth = 1000f

        // Full speed
        assertEquals(0.1f, computeScrubDelta(100f, totalWidth, ScrubRate.Full), 0.0001f)
        assertEquals(-0.05f, computeScrubDelta(-50f, totalWidth, ScrubRate.Full), 0.0001f)

        // Half speed
        assertEquals(0.05f, computeScrubDelta(100f, totalWidth, ScrubRate.Half), 0.0001f)

        // Quarter speed
        assertEquals(0.025f, computeScrubDelta(100f, totalWidth, ScrubRate.Quarter), 0.0001f)

        // Fine speed
        assertEquals(0.01f, computeScrubDelta(100f, totalWidth, ScrubRate.Fine), 0.0001f)

        // Zero or negative width guard
        assertEquals(0f, computeScrubDelta(100f, 0f, ScrubRate.Full), 0.0001f)
        assertEquals(0f, computeScrubDelta(100f, -500f, ScrubRate.Full), 0.0001f)
    }
}
