package com.pixelody.app.feature.library

import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryScreenTest {

    @Test
    fun collectionShortcut_constructsProperly() {
        val tracks = listOf(
            Track(
                id = "track-1",
                title = "Song A",
                artist = "Artist A",
                album = "Album A",
                durationSeconds = 210,
                format = "FLAC",
                lossless = true,
                streamUrl = "http://host/stream/1"
            ),
            Track(
                id = "track-2",
                title = "Song B",
                artist = "Artist A",
                album = "Album A",
                durationSeconds = 240,
                format = "FLAC",
                lossless = true,
                streamUrl = "http://host/stream/2"
            )
        )

        val shortcut = CollectionShortcut(
            id = "album:Album A",
            title = "Album A",
            subtitle = "2 tracks",
            artworkUrl = "http://host/art/albumA.png",
            tracks = tracks
        )

        assertEquals("album:Album A", shortcut.id)
        assertEquals("Album A", shortcut.title)
        assertEquals("2 tracks", shortcut.subtitle)
        assertEquals("http://host/art/albumA.png", shortcut.artworkUrl)
        assertEquals(2, shortcut.tracks.size)
        assertTrue(shortcut.tracks.all { it.lossless })
    }

    @Test
    fun collectionShortcut_handlesEmptyTrackList() {
        val shortcut = CollectionShortcut(
            id = "playlist:empty",
            title = "Empty Playlist",
            subtitle = "0 tracks",
            artworkUrl = null,
            tracks = emptyList()
        )

        assertEquals("playlist:empty", shortcut.id)
        assertEquals(0, shortcut.tracks.size)
    }
}
