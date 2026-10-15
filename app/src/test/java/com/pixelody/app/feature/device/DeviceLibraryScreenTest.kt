package com.pixelody.app.feature.device

import com.pixelody.app.core.hosting.AndroidHostStatus
import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceLibraryScreenTest {

    @Test
    fun localTrackFiltering_identifiesMatchingAudioFiles() {
        val localTracks = listOf(
            Track(id = "local-1", title = "Synthwave Intro", artist = "Local Artist", album = "Phone Storage", format = "mp3"),
            Track(id = "local-2", title = "Acoustic Session", artist = "Folk Band", album = "Recordings", format = "flac"),
            Track(id = "local-3", title = "Synth Sunset", artist = "Unknown", album = "Phone Storage", format = "wav")
        )

        val query = "synth"
        val needle = query.trim().lowercase()
        val filtered = localTracks.filter { track ->
            listOf(track.title, track.artist, track.album, track.codec, track.format)
                .any { it.lowercase().contains(needle) }
        }

        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.id == "local-1" })
        assertTrue(filtered.any { it.id == "local-3" })
        assertFalse(filtered.any { it.id == "local-2" })
    }

    @Test
    fun localTrackFiltering_blankQueryReturnsAllTracks() {
        val localTracks = listOf(
            Track(id = "local-1", title = "Track 1"),
            Track(id = "local-2", title = "Track 2")
        )

        val query = "   "
        val needle = query.trim().lowercase()
        val filtered = if (needle.isBlank()) localTracks else localTracks.filter { track ->
            listOf(track.title, track.artist, track.album, track.codec, track.format)
                .any { it.lowercase().contains(needle) }
        }

        assertEquals(2, filtered.size)
    }

    @Test
    fun androidHostStatus_reflectsStateCorrectly() {
        val stopped = AndroidHostStatus()
        assertFalse(stopped.running)
        assertEquals(0, stopped.trackCount)

        val running = AndroidHostStatus(
            running = true,
            baseUrl = "http://192.168.1.50:8080",
            token = "secret12345",
            trackCount = 14
        )
        assertTrue(running.running)
        assertEquals("http://192.168.1.50:8080", running.baseUrl)
        assertEquals(14, running.trackCount)
    }
}
