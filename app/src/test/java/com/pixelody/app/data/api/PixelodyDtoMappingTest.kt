package com.pixelody.app.data.api

import com.pixelody.app.data.fixtures.FakePixelodyHost
import com.pixelody.app.data.model.HostConnectionState
import com.pixelody.app.data.model.RepeatMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelodyDtoMappingTest {
    @Test
    fun optionalAlbumAndMusicalTagsMapWithoutDefaults() {
        val fakeHost = FakePixelodyHost()
        val snapshot = fakeHost.librarySnapshot()
        val original = snapshot.tracks.first().copy(albumArtist = "Various Artists", discNumber = 2,
            trackNumber = 4, albumId = "album-1", recordingId = "recording-1", musicalKey = "C♭ major", bpm = 128.25f)
        val mapped = snapshot.copy(tracks = listOf(original)).toModel(fakeHost.baseUrl).tracks.single()
        assertEquals(original.albumArtist, mapped.albumArtist); assertEquals(original.discNumber, mapped.discNumber)
        assertEquals(original.trackNumber, mapped.trackNumber); assertEquals(original.albumId, mapped.albumId)
        assertEquals(original.recordingId, mapped.recordingId); assertEquals(original.musicalKey, mapped.musicalKey)
        assertEquals(original.bpm, mapped.bpm)
        val legacy = snapshot.toModel(fakeHost.baseUrl).tracks.first()
        assertEquals(null, legacy.albumArtist); assertEquals(null, legacy.discNumber); assertEquals(null, legacy.trackNumber)
        assertEquals(null, legacy.musicalKey); assertEquals(null, legacy.bpm)
    }

    @Test
    fun fixtureSnapshotMapsToAppModelWithoutDesktopPaths() {
        val fakeHost = FakePixelodyHost()
        val snapshot = fakeHost.librarySnapshot().toModel(fakeHost.baseUrl)

        assertEquals("Pixelody on Studio PC", snapshot.host.hostName)
        assertEquals(HostConnectionState.Connected, snapshot.host.connectionState)
        assertEquals(RepeatMode.Off, snapshot.queue.repeatMode)
        assertEquals(3, snapshot.tracks.size)

        val firstTrack = snapshot.tracks.first()
        assertTrue(firstTrack.lossless)
        assertTrue(firstTrack.streamUrl.startsWith(fakeHost.baseUrl))
        assertFalse(firstTrack.streamUrl.contains("C:\\"))
        assertFalse(firstTrack.streamUrl.contains("path"))
    }
}
