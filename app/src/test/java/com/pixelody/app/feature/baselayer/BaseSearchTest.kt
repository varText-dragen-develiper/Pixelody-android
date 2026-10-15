package com.pixelody.app.feature.baselayer

import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What counts as a match is a product decision, and the only kind of product decision
 * worth having is one somebody can disagree with by writing a failing test.
 */
class BaseSearchTest {

    private fun track(id: String, title: String, artist: String, album: String) =
        Track(id = id, title = title, artist = artist, album = album)

    private val library = BaseLayerData(
        tracks = listOf(
            track("t1", "Saline", "Held Pattern", "Lowlight"),
            track("t2", "Salt Flats", "Nakura", "Tape Room"),
            track("t3", "Copper Wire", "Held Pattern", "Lowlight"),
            track("t4", "Night Bus", "Vega Sorrel", "Long Way Round"),
            track("t5", "Held Breath", "Mordant Fields", "Iron Season")
        ),
        phoneTrackIds = setOf("t2", "t4"),
        hostTrackIds = setOf("t1", "t3", "t5"),
        downloadedTrackIds = setOf("t2")
    )

    @Test
    fun anEmptyQueryFindsNothingRatherThanEverything() {
        assertTrue(baseSearchResults(library, "").isEmpty())
        assertTrue(baseSearchResults(library, "   ").isEmpty())
    }

    @Test
    fun titleArtistAndAlbumAreAllSearchable() {
        assertEquals(listOf("t1"), baseSearchResults(library, "saline"))
        assertEquals(listOf("t4"), baseSearchResults(library, "vega"))
        assertEquals(listOf("t5"), baseSearchResults(library, "iron season"))
    }

    @Test
    fun caseAndSurroundingSpaceDoNotMatter() {
        assertEquals(listOf("t1"), baseSearchResults(library, "  SaLiNe "))
    }

    @Test
    fun everyWordHasToMatchSomewhere() {
        assertEquals(listOf("t3"), baseSearchResults(library, "copper lowlight"))
        assertTrue(baseSearchResults(library, "copper tape").isEmpty())
    }

    @Test
    fun aTitleStartingWithWhatWasTypedComesFirst() {
        // "held" matches t5's title and t1/t3's artist. The title wins.
        assertEquals("t5", baseSearchResults(library, "held").first())
        assertEquals(3, baseSearchResults(library, "held").size)
    }

    @Test
    fun theLensNarrowsSearchTheSameWayItNarrowsLibrary() {
        val phone = library.copy(source = BaseSource.Phone)
        assertEquals(listOf("t2"), baseSearchResults(phone, "sal"))

        val downloaded = library.copy(lens = BaseLens.Downloaded)
        assertEquals(listOf("t2"), baseSearchResults(downloaded, "sal"))

        assertEquals(listOf("t1", "t2"), baseSearchResults(library, "sal"))
    }

    @Test
    fun aResultYouCannotPlayIsStillAResult() {
        val down = library.copy(hostReachable = false)
        assertEquals(listOf("t1"), baseSearchResults(down, "saline"))
        assertTrue(down.visibleTracks().map { it.id }.contains("t1"))
    }

    @Test
    fun beforeAnythingIsTypedItOffersWhereYouWere() {
        val listening = library.copy(
            currentTrackId = "t3",
            lastTrackId = "t1",
            queue = listOf("t4", "t5")
        )

        assertEquals(listOf("t3", "t1", "t4", "t5"), baseSearchStartingPoints(listening))
    }

    @Test
    fun theStartingPointsObeyTheLensToo() {
        val listening = library.copy(
            currentTrackId = "t3",
            queue = listOf("t2", "t4"),
            source = BaseSource.Phone
        )

        assertEquals(listOf("t2", "t4"), baseSearchStartingPoints(listening))
    }

    @Test
    fun withNothingPlayedYetThereIsNothingToOfferAndThatIsFine() {
        assertTrue(baseSearchStartingPoints(library).isEmpty())
    }
}
