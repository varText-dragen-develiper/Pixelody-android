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
