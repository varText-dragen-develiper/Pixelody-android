package com.pixelody.app

import com.pixelody.app.core.playback.ShuffleSourceKeys
import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShuffleSourceKeysTest {
    private fun track(id: String, artist: String, album: String) = Track(id = id, title = "Title $id", artist = artist, album = album, genre = "",
        durationSeconds = 180, format = "FLAC", codec = "FLAC", lossless = true, sampleRate = 44100, bitDepth = 16, bitrate = null, channels = 2,
        replayGainDb = null, artworkUrl = null, streamUrl = "http://localhost:41023/audio/$id", favorite = false, missing = false)
    private val library = listOf(track("1", "Mitski", "Puberty 2"), track("2", "Mitski", "Puberty 2"), track("3", "Mitski", "Laurel Hell"), track("4", "Japanese Breakfast", "Jubilee"))

    @Test fun playAndShuffleStayInsideTheListTheyStartedFrom() {
        assertEquals("local-library", ShuffleSourceKeys.keyFor(library.reversed(), library, emptyList()))
        assertEquals("playlist:road", ShuffleSourceKeys.keyFor(library.take(2), library, listOf("road" to listOf("2", "1"))))
        assertEquals("album:album:mitski:puberty 2", ShuffleSourceKeys.keyFor(library.take(2), library, emptyList()))
        assertEquals("artist:Mitski", ShuffleSourceKeys.keyFor(library.take(3), library, emptyList()))
    }

    @Test fun aPartialSelectionGetsAOneOffScope() {
        assertNull(ShuffleSourceKeys.keyFor(listOf(library[0], library[3]), library, emptyList()))
        assertNull(ShuffleSourceKeys.keyFor(listOf(library[0]), library, emptyList()))
        assertNull(ShuffleSourceKeys.keyFor(emptyList(), library, emptyList()))
    }
}
