package com.pixelody.app.feature.baselayer

import com.pixelody.app.data.model.*
import org.junit.Assert.*
import org.junit.Test

class ListeningCollectionsTest {
    @Test fun organizersBecomeOrdinaryPlaylistsWithoutChangingSavedSlots() {
        val originals = listOf(BaseCollection("p1", "playlists", "Playlist", listOf("a", "b")))
        val crate = Crate("c1", "Travel", listOf(CrateSlot.SingleTrack("b"), null,
            CrateSlot.Collection("playlists", "p1"), CrateSlot.PlayerView("Classic")) + List(5) { null })
        val book = CrateBook(listOf(crate))
        val visible = listeningCollections(originals, book)
        assertEquals(originals.first(), visible.first())
        assertEquals(BaseCollection("crate:c1", "playlist", "Travel", listOf("b", "a")), visible.last())
        assertEquals(crate, book.crates.single())
        assertNull(book.crates.single().slotAt(1))
        assertEquals(crateCoverKey("c1"), collectionCoverKey(visible.last().id))
    }
    @Test fun nestedOrganizersHandleCyclesAndMissingCollections() {
        val a = Crate("a", "A", listOf(CrateSlot.Collection("playlists", "crate:b"), CrateSlot.SingleTrack("t1")) + List(7) { null })
        val b = Crate("b", "B", listOf(CrateSlot.Collection("playlists", "crate:a"), CrateSlot.Collection("playlists", "gone"), CrateSlot.SingleTrack("t2")) + List(6) { null })
        val result = listeningCollections(emptyList(), CrateBook(listOf(a, b)))
        assertEquals(listOf("t2", "t1"), result.first().trackIds)
        assertEquals(listOf("t1", "t2"), result.last().trackIds)
    }
    @Test fun toolOnlyAndEmptyCollectionsStayReachableForOrganization() {
        val empty = Crate("empty", "Empty")
        val tools = Crate("tools", "Tools", listOf(CrateSlot.PlayerView("Classic")) + List(8) { null })
        val result = listeningCollections(emptyList(), CrateBook(listOf(empty, tools)))
        assertEquals(listOf("crate:empty", "crate:tools"), result.map { it.id })
        assertTrue(result.all { it.trackIds.isEmpty() })
    }
    @Test fun legacyPrefixedCollectionAddressesStillResolve() {
        val originals = listOf(BaseCollection("p1", "playlist", "Original", listOf("one")))
        val crate = Crate("c", "Saved", listOf(CrateSlot.Collection("playlist", "playlist:P1")) + List(8) { null })
        assertEquals(listOf("one"), listeningCollections(originals, CrateBook(listOf(crate))).last().trackIds)
    }
}
