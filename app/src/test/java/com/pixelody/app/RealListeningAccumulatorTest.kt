package com.pixelody.app

import com.pixelody.app.core.analytics.*
import org.junit.Assert.*
import org.junit.Test

class RealListeningAccumulatorTest {
    private fun occurrence(duration: Long? = 10_000, personal: Boolean = true) = ListeningOccurrence(
        "event-1", 1, "occurrence-1", "entry-1", "session-1",
        ListeningTrack("track-1", durationMs = duration), 100_000, personal
    )

    @Test fun pausesBuffersAndFocusLossDoNotAccrueTime() {
        val a = RealListeningAccumulator()
        a.start(occurrence(), 0, 0, true)
        a.sample(1_000, 1_000, false)
        a.sample(3_000, 1_000, false)
        a.sample(4_000, 1_000, true)
        a.sample(5_000, 2_000, true)
        assertEquals(2_000L, a.checkpoint()!!.listenedWallMs)
        assertEquals(2_000L, a.checkpoint()!!.uniqueCoverageMs)
    }

    @Test fun seekToEndDoesNotManufactureCompletion() {
        val a = RealListeningAccumulator()
        a.start(occurrence(100_000), 0, 0, true)
        a.sample(1_000, 1_000, true)
        a.discontinuity(1_000, 99_000, true)
        a.sample(2_000, 100_000, false)
        val e = a.finish(ListeningEndReason.NATURAL_END, 102_000)!!
        assertEquals(2_000L, e.checkpoint.listenedWallMs)
        assertEquals(0.02, e.checkpoint.coverageRatio!!, 0.0001)
        assertFalse(e.naturalCompletion)
        assertFalse(e.personalSkip)
    }

    @Test fun unannouncedLargePositionJumpIsConservativelyExcluded() {
        val a = RealListeningAccumulator()
        a.start(occurrence(100_000), 0, 0, true)
        a.sample(1_000, 99_000, true)
        assertEquals(0L, a.checkpoint()!!.listenedWallMs)
        assertEquals(0L, a.checkpoint()!!.uniqueCoverageMs)
    }

    @Test fun twiceSpeedCountsWallAndMediaCoverageSeparately() {
        val a = RealListeningAccumulator()
        a.start(occurrence(), 0, 0, true, 2f)
        (1..5).forEach { a.sample(it * 1_000L, it * 2_000L, true, 2f) }
        val e = a.finish(ListeningEndReason.NATURAL_END, 105_000)!!
        assertEquals(5_000L, e.checkpoint.listenedWallMs)
        assertEquals(10_000L, e.checkpoint.observedMediaMs)
        assertEquals(10_000L, e.checkpoint.uniqueCoverageMs)
        assertTrue(e.naturalCompletion)
    }

    @Test fun slowPlaybackAccruesRealWallTime() {
        val a = RealListeningAccumulator()
        a.start(occurrence(), 0, 0, true, .5f)
        a.sample(1_000, 500, true, .5f)
        assertEquals(1_000L, a.checkpoint()!!.listenedWallMs)
        assertEquals(500L, a.checkpoint()!!.uniqueCoverageMs)
    }

    @Test fun repeatedSectionAddsWallTimeWithoutDoubleCountingCoverage() {
        val a = RealListeningAccumulator()
        a.start(occurrence(), 0, 0, true)
        (1..5).forEach { a.sample(it * 1_000L, it * 1_000L, true) }
        a.discontinuity(5_000, 0, true)
        (6..10).forEach { a.sample(it * 1_000L, (it - 5) * 1_000L, true) }
        val e = a.finish(ListeningEndReason.NATURAL_END, 110_000)!!
        assertEquals(10_000L, e.checkpoint.listenedWallMs)
        assertEquals(10_000L, e.checkpoint.observedMediaMs)
        assertEquals(5_000L, e.checkpoint.uniqueCoverageMs)
        assertFalse(e.naturalCompletion)
    }

    @Test fun suspendedSamplingAndMonotonicRollbackCannotInventTime() {
        val a = RealListeningAccumulator()
        a.start(occurrence(100_000), 10_000, 0, true)
        a.sample(9_000, 1_000, true)
        a.sample(20_000, 12_000, true)
        assertEquals(0L, a.checkpoint()!!.listenedWallMs)
        a.sample(21_000, 13_000, true)
        assertEquals(1_000L, a.checkpoint()!!.listenedWallMs)
    }

    @Test fun noMediaProgressOrInvalidSpeedCannotCountAsHeard() {
        val a = RealListeningAccumulator()
        a.start(occurrence(), 0, 0, true, Float.NaN)
        a.sample(1_000, 1_000, true, 1f)
        a.sample(2_000, 1_000, true)
        assertEquals(0L, a.checkpoint()!!.listenedWallMs)
    }

    @Test fun finishIsIdempotentAndAnInfrastructureEndIsNotAPersonalSkip() {
        val a = RealListeningAccumulator()
        a.start(occurrence(), 0, 0, true)
        a.sample(1_000, 1_000, true)
        val first = a.finish(ListeningEndReason.INFRASTRUCTURE, 101_000)!!
        val duplicate = a.finish(ListeningEndReason.PERSONAL_SKIP, 102_000)!!
        assertEquals(first, duplicate)
        assertFalse(first.personalSkip)
        assertFalse(a.active)
    }

    @Test fun remoteActionsCannotBecomePersonalDislike() {
        val a = RealListeningAccumulator()
        a.start(occurrence(personal = false), 0, 0, true)
        val event = a.finish(ListeningEndReason.PERSONAL_SKIP, 100_100)!!
        assertEquals(ListeningEndReason.REMOTE, event.reason)
        assertFalse(event.personalSkip)
    }

    @Test fun unknownDurationCannotClaimPositiveCompletion() {
        val a = RealListeningAccumulator()
        a.start(occurrence(null), 0, 0, true)
        a.sample(1_000, 1_000, true)
        val e = a.finish(ListeningEndReason.NATURAL_END, 101_000)!!
        assertNull(e.checkpoint.coverageRatio)
        assertFalse(e.naturalCompletion)
    }

    @Test fun segmentLimitNeverFillsUnheardGapsOrDiscardsPriorCoverage() {
        val a = RealListeningAccumulator(maxCoverageSegments = 2)
        a.start(occurrence(), 0, 8_000, true)
        a.sample(1_000, 9_000, true)
        a.discontinuity(1_000, 4_000, true)
        a.sample(2_000, 5_000, true)
        a.discontinuity(2_000, 0, true)
        a.sample(3_000, 1_000, true)
        val cp = a.checkpoint()!!
        assertEquals(3_000L, cp.listenedWallMs)
        assertEquals(2_000L, cp.uniqueCoverageMs)
        assertTrue(cp.coverageTruncated)
        assertEquals(listOf(MediaCoverage(4_000, 5_000), MediaCoverage(8_000, 9_000)), cp.coverage)
    }
}
