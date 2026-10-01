package com.pixelody.app

import com.pixelody.app.core.playback.VinylVaultEngine
import com.pixelody.app.data.model.CamelotKey
import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VinylVaultEngineTest {

    private fun createTestTrack(id: String, title: String, artist: String, keyString: String, bpm: Int): Track {
        return Track(
            id = id,
            title = "$title Key:$keyString BPM:$bpm",
            artist = artist,
            durationSeconds = 200,
            streamUrl = "http://stream/$id"
        )
    }

    @Test
    fun `calculateSleeveTiltAngle assigns realistic forward, upright, and backward 3D angles`() {
        val activeIndex = 5

        // Previous sleeve (flipped forward toward user)
        val prevAngle = VinylVaultEngine.calculateSleeveTiltAngle(index = 3, activeIndex = activeIndex)
        assertEquals(-28.0f, prevAngle, 0.01f)

        // Current sleeve (upright focused)
        val currentAngle = VinylVaultEngine.calculateSleeveTiltAngle(index = 5, activeIndex = activeIndex)
        assertEquals(0.0f, currentAngle, 0.01f)

        // Next sleeve (stacked backward in crate)
        val nextAngle = VinylVaultEngine.calculateSleeveTiltAngle(index = 8, activeIndex = activeIndex)
        assertEquals(18.0f, nextAngle, 0.01f)
    }

    @Test
    fun `createTemporalSonicCapsule summarizes mood, key, and atmosphere from session`() {
        val tracks = listOf(
            createTestTrack("1", "Night Drive", "Kavinsky", "8A", 128),
            createTestTrack("2", "Synth Wave", "Gunship", "8A", 130),
            createTestTrack("3", "Outrun", "Lazerhawk", "9A", 126)
        )

        val capsule = VinylVaultEngine.createTemporalSonicCapsule(
            sessionTracks = tracks,
            customTitle = "Synthwave Night",
            timestampMs = 1700000000000L
        )

        assertEquals("Synthwave Night", capsule.title)
        assertEquals(3, capsule.totalTracks)
        assertEquals(CamelotKey.K8A, capsule.dominantKey)
        assertEquals(128.0f, capsule.averageBpm, 0.1f)
        assertEquals("High-Energy Peak", capsule.atmosphereTag)
    }

    @Test
    fun `filterCrateTracks filters accurately by text query and Camelot key`() {
        val tracks = listOf(
            createTestTrack("1", "Midnight City", "M83", "8A", 120),
            createTestTrack("2", "Starlight", "Muse", "8B", 122),
            createTestTrack("3", "Solaris", "Solar Fields", "11A", 110)
        )

        val textFiltered = VinylVaultEngine.filterCrateTracks(tracks, query = "Muse")
        assertEquals(1, textFiltered.size)
        assertEquals("2", textFiltered.first().id)

        val keyFiltered = VinylVaultEngine.filterCrateTracks(tracks, query = "", keyFilter = CamelotKey.K8A)
        assertEquals(1, keyFiltered.size)
        assertEquals("1", keyFiltered.first().id)
    }
}
