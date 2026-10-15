package com.pixelody.app.data.storage

import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalMediaScannerTest {

    @Test
    fun isLocalTrack_identifiesLocalSchemes() {
        val contentTrack = Track(
            id = "content://media/external/audio/media/42",
            title = "Local Audio",
            artist = "Artist",
            album = "Album",
            durationSeconds = 120,
            streamUrl = "content://media/external/audio/media/42"
        )
        val fileTrack = Track(
            id = "file:///sdcard/Music/sample.flac",
            title = "FLAC File",
            artist = "Artist",
            album = "Album",
            durationSeconds = 180,
            streamUrl = "file:///sdcard/Music/sample.flac"
        )
        val remoteTrack = Track(
            id = "t1",
            title = "Remote Audio",
            artist = "Artist",
            album = "Album",
            durationSeconds = 200,
            streamUrl = "http://192.168.1.50:8080/api/v1/tracks/t1/stream"
        )

        assertTrue(isLocalTrack(contentTrack))
        assertTrue(isLocalTrack(fileTrack))
        assertFalse(isLocalTrack(remoteTrack))
        assertFalse(isLocalTrack(null))
    }

    @Test
    fun isLocalTrackId_identifiesSchemes() {
        assertTrue(isLocalTrackId("content://media/external/audio/media/100"))
        assertTrue(isLocalTrackId("file:///storage/emulated/0/Music/track.mp3"))
        assertFalse(isLocalTrackId("t1"))
        assertFalse(isLocalTrackId("track-999"))
        assertFalse(isLocalTrackId("http://host:8080"))
    }

    @Test
    fun localTechnicalMetadata_defaults() {
        val metadata = LocalTechnicalMetadata()
        assertEquals("", metadata.codec)
        assertEquals(0, metadata.sampleRate)
        assertEquals(null, metadata.bitDepth)
        assertEquals(null, metadata.bitrate)
        assertEquals(0, metadata.channels)
        assertEquals(null, metadata.embeddedArtwork)
    }
}
