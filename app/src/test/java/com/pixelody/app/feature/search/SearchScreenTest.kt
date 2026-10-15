package com.pixelody.app.feature.search

import com.pixelody.app.data.model.Track
import com.pixelody.app.ui.components.SourceScope
import com.pixelody.app.ui.components.filterTracksBySource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchScreenTest {

    @Test
    fun searchFiltering_identifiesMatchingTracks() {
        val tracks = listOf(
            Track(id = "1", title = "Hyperdrive", artist = "Neon Knight", durationSeconds = 180),
            Track(id = "2", title = "Midnight Sun", artist = "Solaris", durationSeconds = 200),
            Track(id = "3", title = "Neon Horizon", artist = "RetroWave", durationSeconds = 220)
        )

        val needle = "neon"
        val filtered = tracks.filter { track ->
            listOf(track.title, track.artist, track.album, track.codec, track.format)
                .any { it.lowercase().contains(needle) }
        }

        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.id == "1" })
        assertTrue(filtered.any { it.id == "3" })
    }

    @Test
    fun searchFiltering_handlesSourceScope() {
        val allTracks = listOf(
            Track(id = "phone-1", title = "Local Song", streamUrl = "content://local/1"),
            Track(id = "host-1", title = "Host Song", streamUrl = "http://host/1")
        )
        val localIds = setOf("phone-1")
        val jamIds = emptySet<String>()

        val phoneScoped = filterTracksBySource(allTracks, SourceScope.LocalPhone, localIds, jamIds)
        assertEquals(1, phoneScoped.size)
        assertEquals("phone-1", phoneScoped.first().id)

        val hostScoped = filterTracksBySource(allTracks, SourceScope.DesktopHost, localIds, jamIds)
        assertEquals(1, hostScoped.size)
        assertEquals("host-1", hostScoped.first().id)
    }

    @Test
    fun searchScreen_stateTagsAreRegisteredAndFormatted() {
        val expectedTags = listOf(
            com.pixelody.app.ui.navigation.PixelodyStateTags.SEARCH_QUERY_FIELD,
            com.pixelody.app.ui.navigation.PixelodyStateTags.SEARCH_INTENT_FILTER
        )

        for (tag in expectedTags) {
            assertTrue("Tag $tag must be present in PixelodyStateTags.all", tag in com.pixelody.app.ui.navigation.PixelodyStateTags.all)
            assertTrue("Tag $tag must start with state:search", tag.startsWith("state:search"))
        }
    }
}
