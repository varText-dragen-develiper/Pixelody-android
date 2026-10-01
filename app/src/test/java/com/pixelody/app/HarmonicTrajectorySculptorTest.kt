package com.pixelody.app

import com.pixelody.app.core.playback.HarmonicFlowCoordinator
import com.pixelody.app.core.playback.HarmonicKeyEngine
import com.pixelody.app.data.model.CamelotKey
import com.pixelody.app.data.model.CamelotMode
import com.pixelody.app.data.model.EnergyContourPreset
import com.pixelody.app.data.model.HarmonicEnergyMode
import com.pixelody.app.data.model.HarmonicFlowProfile
import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HarmonicTrajectorySculptorTest {

    private fun createTestTrack(id: String, title: String, artist: String, keyFormat: String, bpm: Float): Track {
        return Track(
            id = id,
            title = "$title BPM:${bpm.toInt()}",
            artist = artist,
            album = "Test Album",
            format = keyFormat,
            streamUrl = "http://localhost:8080/stream/$id",
            durationSeconds = 240,
            sampleRate = 44100
        )
    }

    @Test
    fun testCamelotKeyChromaticColors() {
        assertEquals(24, CamelotKey.entries.size)
        CamelotKey.entries.forEach { key ->
            assertTrue(key.harmonicColorHex != 0L)
            assertNotNull(key.harmonicColor)
        }

        // Test specific known colors
        assertEquals(0xFF00D5D8L, CamelotKey.K1A.harmonicColorHex) // 1A Teal
        assertEquals(0xFF22D3EEL, CamelotKey.K1B.harmonicColorHex) // 1B Cyan
        assertEquals(0xFFEA580CL, CamelotKey.K8A.harmonicColorHex) // 8A Orange
        assertEquals(0xFF10B981L, CamelotKey.K11A.harmonicColorHex) // 11A Emerald
    }

    @Test
    fun testSculptQueueRampUp() {
        val tracks = listOf(
            createTestTrack("1", "Mellow Intro", "Artist A", "8A", 80f),
            createTestTrack("2", "Peak Banger", "Artist B", "9A", 140f),
            createTestTrack("3", "Mid Groove", "Artist C", "8B", 110f),
            createTestTrack("4", "Hyper Build", "Artist D", "10A", 130f)
        )

        val sculpted = HarmonicFlowCoordinator.sculptQueueToTargetCurve(
            queue = tracks,
            preset = EnergyContourPreset.RampUp,
            anchorTrackId = "1"
        )

        assertEquals(4, sculpted.size)
        assertEquals("1", sculpted.first().id) // Preserves anchor at start

        val trajectory = HarmonicFlowCoordinator.calculateTrajectoryPoints(sculpted)
        // Energy of earlier tracks should generally be lower than later tracks
        assertTrue(trajectory.first().energyLevel <= trajectory.last().energyLevel + 0.15f)
    }

    @Test
    fun testSculptQueueSunsetDrift() {
        val tracks = listOf(
            createTestTrack("1", "High Energy Start", "Artist A", "8A", 135f),
            createTestTrack("2", "Ambient Cool", "Artist B", "7A", 85f),
            createTestTrack("3", "Mid Pulse", "Artist C", "8B", 115f),
            createTestTrack("4", "Deep Drift", "Artist D", "6A", 75f)
        )

        val sculpted = HarmonicFlowCoordinator.sculptQueueToTargetCurve(
            queue = tracks,
            preset = EnergyContourPreset.SunsetDrift,
            anchorTrackId = "1"
        )

        assertEquals(4, sculpted.size)
        assertEquals("1", sculpted.first().id)
    }

    @Test
    fun testSculptQueuePeakWave() {
        val tracks = listOf(
            createTestTrack("1", "Track 1", "Artist A", "8A", 100f),
            createTestTrack("2", "Track 2", "Artist B", "9A", 140f),
            createTestTrack("3", "Track 3", "Artist C", "9B", 120f),
            createTestTrack("4", "Track 4", "Artist D", "8A", 95f),
            createTestTrack("5", "Track 5", "Artist E", "10A", 135f)
        )

        val sculpted = HarmonicFlowCoordinator.sculptQueueToTargetCurve(
            queue = tracks,
            preset = EnergyContourPreset.PeakWave,
            anchorTrackId = "1"
        )

        assertEquals(5, sculpted.size)
        assertEquals("1", sculpted.first().id)
    }

    @Test
    fun testCalculateCollectionTelemetry() {
        val collection = listOf(
            createTestTrack("1", "Track 1", "Artist A", "8A", 120f),
            createTestTrack("2", "Track 2", "Artist B", "8A", 122f),
            createTestTrack("3", "Track 3", "Artist C", "9A", 124f),
            createTestTrack("4", "Track 4", "Artist D", "10A", 126f)
        )

        val telemetry = HarmonicFlowCoordinator.calculateCollectionTelemetry(collection)

        assertEquals(CamelotKey.K8A, telemetry.dominantKey)
        assertTrue(telemetry.averageBpm in 120f..126f)
        assertTrue(telemetry.harmonicCohesionPercent >= 70)
        assertEquals(4, telemetry.trajectoryPoints.size)
        assertTrue(telemetry.keyRange.contains("8A"))
    }

    @Test
    fun testEmptyCollectionTelemetry() {
        val emptyTelemetry = HarmonicFlowCoordinator.calculateCollectionTelemetry(emptyList())
        assertEquals("N/A", emptyTelemetry.keyRange)
        assertEquals(100, emptyTelemetry.harmonicCohesionPercent)
        assertTrue(emptyTelemetry.trajectoryPoints.isEmpty())
    }
}
