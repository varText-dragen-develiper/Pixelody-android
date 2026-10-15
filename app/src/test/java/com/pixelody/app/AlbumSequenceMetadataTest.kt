package com.pixelody.app

import com.pixelody.app.core.playback.*
import com.pixelody.app.data.model.Track
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class AlbumSequenceMetadataTest {
    private fun track(id: String, performer: String, disc: Int? = null, number: Int? = null) = Track(id, id,
        artist = performer, album = "Compilation", albumArtist = "Various Artists", discNumber = disc,
        trackNumber = number, streamUrl = "content://test/$id")

    @Test fun compilationOrdersDiscsAndTracksAndRetainsDuplicateOccurrences() {
        val a = track("a", "One", 1, 1); val b = track("b", "Two", 1, 2); val c = track("c", "Three", 2, 1)
        val unit = AlbumSequenceMetadata.units(listOf(c, b, a, b)).single()
        assertEquals(listOf(a, b, b, c), unit.tracks); assertEquals("disc-track-tags", unit.orderSource)
    }

    @Test fun incompleteTagsPreserveExactSourceOrder() {
        val source = listOf(track("c", "C", 2, 1), track("unknown", "U"), track("a", "A", 1, 1))
        val unit = AlbumSequenceMetadata.units(source).single()
        assertEquals(source, unit.tracks); assertEquals("source-order", unit.orderSource)
    }

    @Test fun albumIdsAvoidDelimiterCollisionsAndNfcGroupsEquivalentText() {
        val a = track("a", "x").copy(albumArtist = "a / b", album = "c")
        val b = track("b", "y").copy(albumArtist = "a", album = "b / c")
        assertEquals(2, AlbumSequenceMetadata.units(listOf(a, b)).size)
        assertEquals(1, AlbumSequenceMetadata.units(listOf(a.copy(albumId = "same"), b.copy(albumId = "same"))).size)
        assertEquals(1, AlbumSequenceMetadata.units(listOf(a.copy(album = "Café"), a.copy(album = "Cafe\u0301"))).size)
    }

    @Test fun occurrenceReplanKeepsExplicitCurrentIdReplaysAndCanonicalAlbumOrder() {
        val a = track("a", "One", 1, 1); val b = track("b", "Two", 1, 2)
        val plan = FlowShuffleEngine.planQueue(a, listOf(b, a, b), FlowShuffleMode.AlbumPreserving,
            horizon = 3, random = Random(4), preserveOccurrences = true)
        assertEquals(listOf(a, b, b), plan.map { it.track })
        assertTrue(plan.first().cue!!.description.contains("disc-track-tags"))
    }

    @Test fun sourcePositionParsingDoesNotInventMissingNumbers() {
        assertEquals(3, AlbumSequenceMetadata.parsePosition("03/12")); assertNull(AlbumSequenceMetadata.parsePosition("0"))
        assertNull(AlbumSequenceMetadata.parsePosition(null)); assertNull(AlbumSequenceMetadata.parsePosition("one"))
    }
}
