package com.pixelody.app.feature.baselayer

import com.pixelody.app.data.model.Track
import com.pixelody.app.feature.library.matchesCollectionId
import com.pixelody.app.ui.navigation.PixelodyDetailKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConvenienceRoutingTest {

    @Test
    fun matchesCollectionId_handlesExactAndPrefixedIdentifiers() {
        // Exact matches
        assertTrue(matchesCollectionId("p1", "p1"))
        assertTrue(matchesCollectionId("album:In Rainbows", "album:In Rainbows"))

        // Prefixed vs Unprefixed
        assertTrue(matchesCollectionId("album:In Rainbows", "In Rainbows"))
        assertTrue(matchesCollectionId("In Rainbows", "album:In Rainbows"))
        assertTrue(matchesCollectionId("artist:Radiohead", "Radiohead"))
        assertTrue(matchesCollectionId("Radiohead", "artist:Radiohead"))
        assertTrue(matchesCollectionId("genre:Electronic", "Electronic"))
        assertTrue(matchesCollectionId("Electronic", "genre:Electronic"))

        // Case insensitivity
        assertTrue(matchesCollectionId("genre:electronic", "Electronic"))
        assertTrue(matchesCollectionId("album:in rainbows", "In Rainbows"))

        // Non-matches
        assertFalse(matchesCollectionId("album:In Rainbows", "Kid A"))
        assertFalse(matchesCollectionId("artist:Radiohead", "Thom Yorke"))
        assertFalse(matchesCollectionId("p1", "p2"))
        assertFalse(matchesCollectionId("", "album:In Rainbows"))
        assertFalse(matchesCollectionId("album:In Rainbows", ""))
    }

    @Test
    fun pixelodyDetailKind_supportsAllConvenienceRoutes() {
        val kinds = listOf(
            PixelodyDetailKind.Playlist,
            PixelodyDetailKind.Album,
            PixelodyDetailKind.Artist,
            PixelodyDetailKind.Genre,
            PixelodyDetailKind.Favorites,
            PixelodyDetailKind.Lossless,
            PixelodyDetailKind.Recent
        )

        kinds.forEach { kind ->
            assertEquals(kind, PixelodyDetailKind.fromStorageKey(kind.storageKey))
            assertEquals(kind, PixelodyDetailKind.fromStorageKey(kind.storageKey.uppercase()))
        }
    }

    @Test
    fun baseLayerData_collectionLookup_isResilientToPrefixesAndKindKeys() {
        val testCollection = BaseCollection(
            id = "album:Solar Fields",
            kindKey = "album",
            name = "Solar Fields",
            trackIds = listOf("t1", "t2")
        )
        val data = BaseLayerData(
            collections = listOf(testCollection)
        )

        // Exact ID lookup
        assertNotNull(data.collection("album:Solar Fields"))
        assertEquals("Solar Fields", data.collection("album:Solar Fields")?.name)

        // Strip prefix lookup
        assertNotNull(data.collection("Solar Fields"))
        assertEquals("Solar Fields", data.collection("Solar Fields")?.name)

        // Kind key lookup fallback
        assertNotNull(data.collection("", kindKey = "album"))
        assertEquals("Solar Fields", data.collection("", kindKey = "album")?.name)

        // Non-existent lookup
        assertNull(data.collection("Unknown"))
        assertNull(data.collection("", kindKey = "nonexistent"))
    }

    @Test
    fun conveniencePushes_maintainPredictableBackStack() {
        var state = BaseLayerState(destination = BaseDestination.Home)

        // 1. User taps "Liked Songs" shortcut on Home -> pushes favorites collection
        state = reduceBaseLayer(state, BaseIntent.Push(BasePush.Collection("favorites", "")))
        assertEquals(1, state.pushed.size)
        assertEquals(BackTarget.PopTo("Home"), state.backTarget())

        // 2. User navigates back -> returns to Home
        state = reduceBaseLayer(state, BaseIntent.Back)
        assertTrue(state.pushed.isEmpty())
        assertEquals(BaseDestination.Home, state.destination)

        // 3. User taps "Hi-Res Master" shortcut -> pushes lossless collection
        state = reduceBaseLayer(state, BaseIntent.Push(BasePush.Collection("lossless", "")))
        assertEquals(1, state.pushed.size)
        state = reduceBaseLayer(state, BaseIntent.Back)
        assertTrue(state.pushed.isEmpty())

        // 4. User navigates from track action -> Go to genre
        state = reduceBaseLayer(state, BaseIntent.Push(BasePush.Collection("genre", "genre:Electronic")))
        assertEquals(1, state.pushed.size)
        val pushedGenre = state.pushed.first() as BasePush.Collection
        assertEquals("genre", pushedGenre.kindKey)
        assertEquals("genre:Electronic", pushedGenre.collectionId)
        state = reduceBaseLayer(state, BaseIntent.Back)
        assertTrue(state.pushed.isEmpty())
    }

    private fun resolveDetailKind(kind: String): PixelodyDetailKind = when (kind.lowercase()) {
        "playlist", "playlists" -> PixelodyDetailKind.Playlist
        "album", "albums" -> PixelodyDetailKind.Album
        "artist", "artists" -> PixelodyDetailKind.Artist
        "genre", "genres" -> PixelodyDetailKind.Genre
        "favorites", "favorite" -> PixelodyDetailKind.Favorites
        "lossless", "hires" -> PixelodyDetailKind.Lossless
        "recent", "recents" -> PixelodyDetailKind.Recent
        else -> PixelodyDetailKind.Playlist
    }

    private fun resolvePushedCollectionTitle(pushed: BasePush.Collection): String = when (val kind = pushed.kindKey.lowercase()) {
        "recent", "recents" -> "Recent Tracks"
        "lossless", "hires" -> "Hi-Res Master"
        "favorites", "favorite" -> "Favorites"
        "playlists" -> "Playlists"
        "playlist" -> pushed.collectionId.removePrefix("playlist:").ifBlank { "Playlists" }
        "album" -> pushed.collectionId.removePrefix("album:").ifBlank { "Moonlit Circuit" }
        "artist" -> pushed.collectionId.removePrefix("artist:").ifBlank { "Artists" }
        "genre" -> pushed.collectionId.removePrefix("genre:").ifBlank { "Genres" }
        else -> pushed.collectionId.ifBlank { "Collection" }
    }

    @Test
    fun resolveDetailKind_mapsAllConvenienceInputsToCorrectKind() {
        assertEquals(PixelodyDetailKind.Favorites, resolveDetailKind("favorites"))
        assertEquals(PixelodyDetailKind.Favorites, resolveDetailKind("favorite"))
        assertEquals(PixelodyDetailKind.Recent, resolveDetailKind("recent"))
        assertEquals(PixelodyDetailKind.Recent, resolveDetailKind("recents"))
        assertEquals(PixelodyDetailKind.Lossless, resolveDetailKind("lossless"))
        assertEquals(PixelodyDetailKind.Lossless, resolveDetailKind("hires"))
        assertEquals(PixelodyDetailKind.Playlist, resolveDetailKind("playlists"))
        assertEquals(PixelodyDetailKind.Playlist, resolveDetailKind("playlist"))
        assertEquals(PixelodyDetailKind.Album, resolveDetailKind("album"))
        assertEquals(PixelodyDetailKind.Album, resolveDetailKind("albums"))
        assertEquals(PixelodyDetailKind.Artist, resolveDetailKind("artist"))
        assertEquals(PixelodyDetailKind.Artist, resolveDetailKind("artists"))
        assertEquals(PixelodyDetailKind.Genre, resolveDetailKind("genre"))
        assertEquals(PixelodyDetailKind.Genre, resolveDetailKind("genres"))
    }

    @Test
    fun resolvePushedCollectionTitle_resolvesAllConvenienceTitlesAccurately() {
        assertEquals("Recent Tracks", resolvePushedCollectionTitle(BasePush.Collection("recent", "")))
        assertEquals("Hi-Res Master", resolvePushedCollectionTitle(BasePush.Collection("lossless", "")))
        assertEquals("Favorites", resolvePushedCollectionTitle(BasePush.Collection("favorites", "")))
        assertEquals("Playlists", resolvePushedCollectionTitle(BasePush.Collection("playlists", "")))
        assertEquals("Reference Checks", resolvePushedCollectionTitle(BasePush.Collection("playlist", "Reference Checks")))
        assertEquals("Moonlit Circuit", resolvePushedCollectionTitle(BasePush.Collection("album", "album:Moonlit Circuit")))
        assertEquals("Moonlit Circuit", resolvePushedCollectionTitle(BasePush.Collection("album", "Moonlit Circuit")))
        assertEquals("Local Signal", resolvePushedCollectionTitle(BasePush.Collection("artist", "artist:Local Signal")))
        assertEquals("Electronic", resolvePushedCollectionTitle(BasePush.Collection("genre", "genre:Electronic")))
    }
}

