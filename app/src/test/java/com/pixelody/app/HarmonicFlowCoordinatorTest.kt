package com.pixelody.app

import com.pixelody.app.core.playback.HarmonicFlowCoordinator
import com.pixelody.app.core.playback.HarmonicKeyEngine
import com.pixelody.app.data.model.CamelotKey
import com.pixelody.app.data.model.DjTransitionCurve
import com.pixelody.app.data.model.HarmonicEnergyMode
import com.pixelody.app.data.model.HarmonicFlowProfile
import com.pixelody.app.data.model.HarmonicRelation
import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Unified Harmonic Flow Coordinator & Bridge Solver.
 */
class HarmonicFlowCoordinatorTest {

    private fun createTrack(id: String, title: String, artist: String = "Artist A", format: String = "8A", bpm: Int = 124): Track {
        return Track(
            id = id,
            title = "$title BPM:$bpm",
            artist = artist,
            album = "Album X",
            format = format,
            streamUrl = "http://localhost/stream/$id.flac",
            lossless = true
        )
    }

    @Test
    fun testProfileSwitchingAndDefaults() {
        // Deep Listening default
        HarmonicFlowCoordinator.setProfile(HarmonicFlowProfile.DeepListening)
        assertEquals(HarmonicFlowProfile.DeepListening, HarmonicFlowCoordinator.activeProfile.value)
        assertEquals(DjTransitionCurve.EqualPower, HarmonicFlowCoordinator.customSettings.value.preferredCurve)
        assertEquals(8, HarmonicFlowCoordinator.customSettings.value.transitionDurationSeconds)

        // Club Set Flow
        HarmonicFlowCoordinator.setProfile(HarmonicFlowProfile.ClubSetFlow)
        assertEquals(HarmonicFlowProfile.ClubSetFlow, HarmonicFlowCoordinator.activeProfile.value)
        assertEquals(DjTransitionCurve.BassSwap, HarmonicFlowCoordinator.customSettings.value.preferredCurve)
        assertEquals(4, HarmonicFlowCoordinator.customSettings.value.transitionDurationSeconds)

        // Sunset Chill
        HarmonicFlowCoordinator.setProfile(HarmonicFlowProfile.SunsetChill)
        assertEquals(HarmonicFlowProfile.SunsetChill, HarmonicFlowCoordinator.activeProfile.value)
        assertEquals(DjTransitionCurve.EchoOut, HarmonicFlowCoordinator.customSettings.value.preferredCurve)
        assertEquals(6, HarmonicFlowCoordinator.customSettings.value.transitionDurationSeconds)
    }

    @Test
    fun testQueueRoadmapCalculation() {
        val t1 = createTrack("1", "Intro", format = "8A", bpm = 120) // A minor
        val t2 = createTrack("2", "Peak", format = "9A", bpm = 124)  // E minor (Adjacent +1)
        val t3 = createTrack("3", "Drop", format = "2A", bpm = 128)  // Eb minor (Clash with 9A)

        val queue = listOf(t1, t2, t3)
        val roadmap = HarmonicFlowCoordinator.calculateQueueRoadmap(queue)

        assertEquals(2, roadmap.size)

        // Slot 0: 8A -> 9A (Smooth Adjacent Step)
        val slot0 = roadmap[0]
        assertEquals("1", slot0.fromTrackId)
        assertEquals("2", slot0.toTrackId)
        assertEquals(HarmonicRelation.AdjacentStep, slot0.relation)
        assertFalse(slot0.isClash)
        assertTrue(slot0.compatibilityScore >= 0.75f)

        // Slot 1: 9A -> 2A (Dissonant Clash)
        val slot1 = roadmap[1]
        assertEquals("2", slot1.fromTrackId)
        assertEquals("3", slot1.toTrackId)
        assertEquals(HarmonicRelation.DissonantClash, slot1.relation)
        assertTrue(slot1.isClash)
        assertNotNull(slot1.suggestedBridgeKey)
    }

    @Test
    fun testHarmonicBridgeSolver() {
        val fromTrack = createTrack("A", "Track A", format = "8A")  // 8A
        val toTrack = createTrack("C", "Track C", format = "10A") // 10A (+2 from 8A)

        // Candidate pool includes a stepping stone track (9A) and an irrelevant clash (3B)
        val bridgeTrack = createTrack("B", "Stepping Stone", format = "9A")
        val clashTrack = createTrack("D", "Irrelevant", format = "3B")

        val pool = listOf(fromTrack, toTrack, bridgeTrack, clashTrack)

        val bridges = HarmonicFlowCoordinator.findHarmonicBridgeTracks(
            fromTrack = fromTrack,
            toTrack = toTrack,
            candidatePool = pool
        )

        assertEquals(1, bridges.size)
        assertEquals("B", bridges.first().id)
    }

    @Test
    fun testOptimizeQueueHarmonicFlow() {
        // Disordered queue: 8A, 2A, 9A, 10A, 8B
        val t8A = createTrack("1", "Track 8A", format = "8A")
        val t2A = createTrack("2", "Track 2A", format = "2A")
        val t9A = createTrack("3", "Track 9A", format = "9A")
        val t10A = createTrack("4", "Track 10A", format = "10A")
        val t8B = createTrack("5", "Track 8B", format = "8B")

        val disordered = listOf(t8A, t2A, t9A, t10A, t8B)

        val optimized = HarmonicFlowCoordinator.optimizeQueueHarmonicFlow(
            queue = disordered,
            anchorTrackId = "1",
            energyMode = HarmonicEnergyMode.HarmonicLock
        )

        assertEquals(5, optimized.size)
        assertEquals("1", optimized[0].id) // Anchor preserved

        // Second track should be a harmonic match to 8A (8B or 9A, not 2A)
        val secondTrackKey = HarmonicKeyEngine.estimateTrackTelemetry(optimized[1]).key.code
        assertTrue(secondTrackKey in listOf("8B", "9A", "7A"))
    }

    @Test
    fun testSeedHarmonicFlowRunway() {
        val anchor = createTrack("anchor", "Anchor Track", format = "8A")
        val library = listOf(
            anchor,
            createTrack("t1", "Track 9A", format = "9A"),
            createTrack("t2", "Track 10A", format = "10A"),
            createTrack("t3", "Track 11A", format = "11A"),
            createTrack("t4", "Track 12A", format = "12A"),
            createTrack("t5", "Track 1A", format = "1A")
        )

        val runway = HarmonicFlowCoordinator.seedHarmonicFlowRunway(
            anchorTrack = anchor,
            libraryPool = library,
            targetCount = 4,
            profile = HarmonicFlowProfile.ClubSetFlow
        )

        assertEquals(4, runway.size)
        assertEquals("anchor", runway[0].id)
    }

    @Test
    fun testTrajectoryPointsCalculation() {
        val tracks = listOf(
            createTrack("1", "T1", format = "8A", bpm = 120),
            createTrack("2", "T2", format = "9A", bpm = 124),
            createTrack("3", "T3", format = "10B", bpm = 128)
        )

        val points = HarmonicFlowCoordinator.calculateTrajectoryPoints(tracks)
        assertEquals(3, points.size)

        assertEquals("1", points[0].trackId)
        assertTrue(points[0].energyLevel in 0.1f..1.0f)
        assertTrue(points[0].normalizedWheelPosition in 0f..1f)
    }
}
