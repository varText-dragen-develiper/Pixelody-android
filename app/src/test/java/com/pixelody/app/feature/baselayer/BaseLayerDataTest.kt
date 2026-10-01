package com.pixelody.app.feature.baselayer

import com.pixelody.app.data.model.Playlist
import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseLayerDataTest {

    private fun track(
        id: String,
        album: String = "Lowlight",
        artist: String = "Held Pattern",
        favorite: Boolean = false,
        lossless: Boolean = false,
        missing: Boolean = false
    ) = Track(
        id = id,
        title = "Track $id",
        artist = artist,
        album = album,
        lossless = lossless,
        favorite = favorite,
        missing = missing
    )

    private val library = BaseLayerData(
        tracks = listOf(
            track("t1", favorite = true, lossless = true),
            track("t2", album = "Tape Room", artist = "Nakura"),
            track("t3", album = "Iron Season", artist = "Mordant Fields", lossless = true),
            track("t4", album = "Tape Room", artist = "Nakura", favorite = true)
        ),
        phoneTrackIds = setOf("t2", "t4"),
        hostTrackIds = setOf("t1", "t3"),
        downloadedTrackIds = setOf("t2")
    )

    @Test
    fun aSourceIsWhereATrackCameFromNotAFieldOnIt() {
        assertEquals(BaseSource.Host, library.sourceOf("t1"))
        assertEquals(BaseSource.Phone, library.sourceOf("t2"))
        assertEquals(BaseSource.All, library.sourceOf("unknown"))
    }

    @Test
    fun theSameLensServesEveryScreenThatUsesIt() {
        assertEquals(4, library.visibleTracks().size)

        val favourites = library.copy(lens = BaseLens.Favourites)
        assertEquals(listOf("t1", "t4"), favourites.visibleTracks().map { it.id })

        val phone = library.copy(source = BaseSource.Phone)
        assertEquals(listOf("t2", "t4"), phone.visibleTracks().map { it.id })

        val both = library.copy(source = BaseSource.Phone, lens = BaseLens.Favourites)
        assertEquals(listOf("t4"), both.visibleTracks().map { it.id })
    }

    @Test
    fun aLensThatMatchesNothingIsEmptyRatherThanEverything() {
        val none = library.copy(source = BaseSource.Jam, lens = BaseLens.Lossless)
        assertTrue(none.visibleTracks().isEmpty())
    }

    @Test
    fun aHostTrackWithTheHostGoneKeepsItsRowAndStopsBeingPlayable() {
        val down = library.copy(hostReachable = false)

        assertTrue("still listed", down.visibleTracks().map { it.id }.contains("t1"))
        assertFalse("not playable", down.isPlayable("t1"))
        assertTrue("phone music unaffected", down.isPlayable("t2"))
        assertEquals(listOf("t2", "t4"), down.playableTrackIds(listOf("t1", "t2", "t3", "t4")))
    }

    @Test
    fun aMissingFileIsNeverPlayableEvenWithTheHostUp() {
        val withMissing = library.copy(tracks = library.tracks + track("t9", missing = true))
        assertFalse(withMissing.isPlayable("t9"))
        assertFalse(withMissing.isPlayable("nothing-by-that-name"))
    }

    @Test
    fun theLensLabelNamesBothAxesOnlyWhenBothAreNarrowed() {
        assertEquals("Everything", library.lensLabel)
        assertEquals("Favourites", library.copy(lens = BaseLens.Favourites).lensLabel)
        assertEquals(
            "Downloaded · Phone",
            library.copy(lens = BaseLens.Downloaded, source = BaseSource.Phone).lensLabel
        )
    }

    @Test
    fun albumsAndArtistsAreDerivedOnceSoNothingInventsItsOwnVersion() {
        val playlists = listOf(
            Playlist(id = "p1", name = "Metal", trackIds = listOf("t3"), artworkUrl = null)
        )
        val built = buildBaseCollections(library.tracks, playlists)

        assertEquals(listOf("Metal"), built.filter { it.kindKey == "playlist" }.map { it.name })
        assertEquals(
            listOf("Iron Season", "Lowlight", "Tape Room"),
            built.filter { it.kindKey == "album" }.map { it.name }
        )
        assertEquals(
            listOf("Held Pattern", "Mordant Fields", "Nakura"),
            built.filter { it.kindKey == "artist" }.map { it.name }
        )

        val tapeRoom = built.first { it.name == "Tape Room" }
        assertEquals(listOf("t2", "t4"), tapeRoom.trackIds)
    }

    @Test
    fun aTrackWithNoAlbumOrArtistDoesNotProduceANamelessCollection() {
        val anonymous = listOf(track("t5", album = "", artist = ""))
        assertTrue(buildBaseCollections(anonymous, emptyList()).isEmpty())
    }

    /**
     * The starter crate shipped with its album slot resolving to "Gone", because
     * `CrateStarters` was handed album names where collection ids were wanted. The unit
     * test agreed with the bug — it asserted the wrong value — so only the device
     * caught it. This is the test that would have: not "is the slot what I said", but
     * "does the slot point at something that is actually there".
     */
    @Test
    fun everySlotInTheStarterCratePointsAtSomethingThatExists() {
        val data = sampleBaseLayerData()
        val starter = data.crates.crates.single()

        assertTrue("the starter crate should not be empty", starter.occupiedCount > 0)
        starter.slots.filterNotNull().forEach { slot ->
            val face = data.resolveCrateSlot(slot)
            assertNotNull(slot.toString(), face)
            assertTrue(slot.toString(), face!!.available)
        }
    }

    @Test
    fun everyBrowseShapeHasACollectionKindExceptTracks() {
        val kinds = buildBaseCollections(
            library.tracks,
            listOf(Playlist(id = "p1", name = "Metal", trackIds = listOf("t3"), artworkUrl = null))
        ).map { it.kindKey }.distinct()

        assertTrue(kinds.contains(BaseBrowseShape.Playlists.kindKey))
        assertTrue(kinds.contains(BaseBrowseShape.Albums.kindKey))
        assertTrue(kinds.contains(BaseBrowseShape.Artists.kindKey))
        assertFalse(kinds.contains(BaseBrowseShape.Tracks.kindKey))
    }
}
