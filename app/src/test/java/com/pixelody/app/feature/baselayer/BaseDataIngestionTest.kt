package com.pixelody.app.feature.baselayer

import com.pixelody.app.data.model.CrateStarters
import com.pixelody.app.data.model.Playlist
import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseDataIngestionTest {

    private val trackHost1 = Track(
        id = "t_host_1",
        title = "Cloud Signal",
        artist = "Apex Echo",
        album = "Skyline",
        durationSeconds = 200,
        lossless = true,
        favorite = true
    )

    private val trackHost2 = Track(
        id = "t_host_2",
        title = "Midnight Stream",
        artist = "Apex Echo",
        album = "Skyline",
        durationSeconds = 180,
        lossless = false,
        favorite = false
    )

    private val trackPhone1 = Track(
        id = "content://media/external/audio/media/10",
        title = "Voice Memo 01",
        artist = "Voice Recorder",
        album = "Recordings",
        durationSeconds = 60,
        lossless = false,
        favorite = false
    )

    private val trackPhone2 = Track(
        id = "content://media/external/audio/media/11",
        title = "High Res Master",
        artist = "Studio Band",
        album = "Session 1",
        durationSeconds = 240,
        lossless = true,
        favorite = true
    )

    private val playlist1 = Playlist(
        id = "p1",
        name = "Favorites Mix",
        trackIds = listOf("t_host_1", "content://media/external/audio/media/11"),
        artworkUrl = null
    )

    @Test
    fun buildBaseCollections_derivesPlaylistsAlbumsAndArtists() {
        val tracks = listOf(trackHost1, trackHost2, trackPhone1, trackPhone2)
        val playlists = listOf(playlist1)

        val collections = buildBaseCollections(tracks, playlists)

        // Playlists
        val playlistCollections = collections.filter { it.kindKey == BaseBrowseShape.Playlists.kindKey }
        assertEquals(1, playlistCollections.size)
        assertEquals("Favorites Mix", playlistCollections.first().name)

        // Albums: "Skyline" (2 tracks), "Recordings" (1 track), "Session 1" (1 track)
        val albumCollections = collections.filter { it.kindKey == BaseBrowseShape.Albums.kindKey }
        assertEquals(3, albumCollections.size)
        val skylineAlbum = albumCollections.first { it.name == "Skyline" }
        assertEquals(listOf("t_host_1", "t_host_2"), skylineAlbum.trackIds)

        // Artists: "Apex Echo" (2 tracks), "Studio Band" (1 track), "Voice Recorder" (1 track)
        val artistCollections = collections.filter { it.kindKey == BaseBrowseShape.Artists.kindKey }
        assertEquals(3, artistCollections.size)
        val apexArtist = artistCollections.first { it.name == "Apex Echo" }
        assertEquals(listOf("t_host_1", "t_host_2"), apexArtist.trackIds)
    }

    @Test
    fun baseLayerData_sourceOf_identifiesCorrectOrigin() {
        val data = BaseLayerData(
            tracks = listOf(trackHost1, trackPhone1),
            hostTrackIds = setOf(trackHost1.id),
            phoneTrackIds = setOf(trackPhone1.id),
            jamTrackIds = setOf("jam_1")
        )

        assertEquals(BaseSource.Host, data.sourceOf(trackHost1.id))
        assertEquals(BaseSource.Phone, data.sourceOf(trackPhone1.id))
        assertEquals(BaseSource.Jam, data.sourceOf("jam_1"))
        assertEquals(BaseSource.All, data.sourceOf("unknown_track"))
    }

    @Test
    fun baseLayerData_hostUnreachable_preservesPhonePlayability() {
        val data = BaseLayerData(
            tracks = listOf(trackHost1, trackPhone1),
            hostTrackIds = setOf(trackHost1.id),
            phoneTrackIds = setOf(trackPhone1.id),
            hostReachable = false
        )

        // Host track is unplayable when host is down
        assertFalse(data.isPlayable(trackHost1.id))
        // Phone track remains playable!
        assertTrue(data.isPlayable(trackPhone1.id))
    }

    @Test
    fun baseLayerData_lensFiltering_appliesCorrectly() {
        val data = BaseLayerData(
            tracks = listOf(trackHost1, trackHost2, trackPhone1, trackPhone2),
            hostTrackIds = setOf(trackHost1.id, trackHost2.id),
            phoneTrackIds = setOf(trackPhone1.id, trackPhone2.id),
            downloadedTrackIds = setOf(trackHost1.id),
            source = BaseSource.All,
            lens = BaseLens.Downloaded
        )

        val visible = data.visibleTracks()
        assertEquals(1, visible.size)
        assertEquals(trackHost1.id, visible.first().id)

        // Change lens to Lossless
        val losslessData = data.copy(lens = BaseLens.Lossless)
        val losslessTracks = losslessData.visibleTracks()
        assertEquals(2, losslessTracks.size)
        assertTrue(losslessTracks.any { it.id == trackHost1.id })
        assertTrue(losslessTracks.any { it.id == trackPhone2.id })

        // Change lens to Favourites
        val favData = data.copy(lens = BaseLens.Favourites)
        val favTracks = favData.visibleTracks()
        assertEquals(2, favTracks.size)
        assertTrue(favTracks.any { it.id == trackHost1.id })
        assertTrue(favTracks.any { it.id == trackPhone2.id })
    }

    @Test
    fun crateStarters_seedFromPlaylistsAndAlbums() {
        val tracks = listOf(trackHost1, trackHost2, trackPhone1, trackPhone2)
        val playlists = listOf(playlist1)
        val collections = buildBaseCollections(tracks, playlists)
        val albumIds = collections.filter { it.kindKey == BaseBrowseShape.Albums.kindKey }.map { it.id }

        val crateBook = CrateStarters.from(playlists, albumIds)
        assertFalse(crateBook.crates.isEmpty())
        assertEquals("Start here", crateBook.crates.first().name)
        assertTrue(crateBook.crates.first().occupiedCount > 0)
    }
}
