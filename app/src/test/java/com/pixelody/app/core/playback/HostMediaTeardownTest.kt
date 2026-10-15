package com.pixelody.app.core.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HostMediaTeardownTest {

    @Test
    fun testIsRemoteHostMedia() {
        assertTrue(isRemoteHostMedia("http://192.168.1.100:8080/api/v1/tracks/1/stream"))
        assertTrue(isRemoteHostMedia("https://remote.pixelody.app/stream/audio.flac"))
        assertTrue(isRemoteHostMedia("HTTP://LOCALHOST:4000/STREAM"))
        assertTrue(isRemoteHostMedia("HTTPS://HOST:5000/AUDIO.WAV"))

        assertFalse(isRemoteHostMedia(null))
        assertFalse(isRemoteHostMedia(""))
        assertFalse(isRemoteHostMedia("   "))
        assertFalse(isRemoteHostMedia("file:///data/user/0/com.pixelody.app/cache/track1.flac"))
        assertFalse(isRemoteHostMedia("content://media/external/audio/media/42"))
        assertFalse(isRemoteHostMedia("invalid-uri-without-scheme"))
        assertFalse(isRemoteHostMedia("blob:http://localhost/1234"))
    }

    @Test
    fun testRemoteMediaIndicesInReverseOrder() {
        val uris = listOf(
            "file:///local/song1.mp3",                          // index 0: local
            "http://192.168.1.5:8080/api/v1/tracks/1/stream",  // index 1: remote
            "content://media/external/audio/2",                // index 2: local
            "https://host.local/api/v1/tracks/3/stream",       // index 3: remote
            "http://100.66.10.95:62310/stream",                // index 4: remote
            "file:///local/song4.flac"                         // index 5: local
        )

        val indices = remoteMediaIndices(uris)

        // Must be in descending order so removal does not shift preceding target indices
        assertEquals(listOf(4, 3, 1), indices)
    }

    @Test
    fun testRemoteMediaIndicesAllLocal() {
        val uris = listOf(
            "file:///local/song1.mp3",
            "content://media/external/audio/2",
            "file:///local/song3.flac"
        )
        val indices = remoteMediaIndices(uris)
        assertTrue(indices.isEmpty())
    }

    @Test
    fun testRemoteMediaIndicesAllRemote() {
        val uris = listOf(
            "http://host/1",
            "http://host/2",
            "https://host/3"
        )
        val indices = remoteMediaIndices(uris)
        assertEquals(listOf(2, 1, 0), indices)
    }

    @Test
    fun testRemoteMediaIndicesEmptyList() {
        val indices = remoteMediaIndices(emptyList())
        assertTrue(indices.isEmpty())
    }
}
