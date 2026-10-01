package com.pixelody.app

import com.pixelody.app.core.playback.HarmonicGpsEngine
import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HarmonicGpsEngineTest {

    private fun createTestTrack(id: String, title: String, keyString: String, bpm: Int): Track {
        return Track(
            id = id,
            title = "$title Key:$keyString BPM:$bpm",
            artist = "Artist_$id",
            album = "Album_$id",
            durationSeconds = 180,
            streamUrl = "http://stream/$id"
        )
    }

    @Test
    fun `findHarmonicJourney returns trivial route when origin equals destination`() {
        val track = createTestTrack("t1", "Track 1", "8A", 120)
        val route = HarmonicGpsEngine.findHarmonicJourney(track, track, listOf(track))

        assertNotNull(route)
        assertEquals(0, route!!.totalHops)
        assertEquals(1, route.waypoints.size)
        assertEquals(track.id, route.originTrack.id)
        assertEquals(track.id, route.destinationTrack.id)
    }

    @Test
    fun `findHarmonicJourney connects direct harmonic neighbors in 1 hop`() {
        val trackA = createTestTrack("t1", "Track A", "8A", 120)
        val trackB = createTestTrack("t2", "Track B", "8B", 122)

        val route = HarmonicGpsEngine.findHarmonicJourney(trackA, trackB, listOf(trackA, trackB))

        assertNotNull(route)
        assertEquals(1, route!!.totalHops)
        assertEquals(2, route.waypoints.size)
        assertEquals(trackA.id, route.waypoints[0].id)
        assertEquals(trackB.id, route.waypoints[1].id)
        assertTrue("Cohesion score should be high for relative major/minor", route.averageHarmonicCohesionScore > 0.85f)
    }

    @Test
    fun `findHarmonicJourney plots multi-hop path across Camelot Wheel stepping stones`() {
        // Origin: 8A, Destination: 11A (+3 steps away)
        // Bridges available: 9A, 10A
        val track8A = createTestTrack("t8a", "Track 8A", "8A", 120)
        val track9A = createTestTrack("t9a", "Track 9A", "9A", 122)
        val track10A = createTestTrack("t10a", "Track 10A", "10A", 124)
        val track11A = createTestTrack("t11a", "Track 11A", "11A", 126)
        val clashTrack = createTestTrack("tclash", "Track 2B", "2B", 120)

        val pool = listOf(track8A, track9A, track10A, track11A, clashTrack)

        val route = HarmonicGpsEngine.findHarmonicJourney(track8A, track11A, pool, maxHops = 5)

        assertNotNull(route)
        assertTrue("Route should have between 2 and 4 hops", route!!.totalHops in 2..4)
        assertEquals(track8A.id, route.originTrack.id)
        assertEquals(track11A.id, route.destinationTrack.id)

        // Verify all intermediate transitions are harmonic
        route.steps.forEach { step ->
            assertTrue("Every transition step should be harmonic", step.relation.isHarmonic)
        }
    }
}
