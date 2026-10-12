package com.pixelody.app.feature.baselayer

import com.pixelody.app.data.model.*
import org.junit.Assert.*
import org.junit.Test

class DisplayedSourceTruthTest {
    private fun track(id: String) = Track(id = id, title = id)
    @Test fun localFilesAreNotInventedDesktopFiles() {
        val data = BaseLayerData(tracks = listOf(track("phone"), track("desktop"), track("both")),
            phoneTrackIds = setOf("phone", "both"), hostTrackIds = setOf("desktop", "both"))
        assertEquals(listOf("desktop", "both"), data.desktopTracks().map { it.id })
        assertEquals(listOf("phone", "both"), data.copy(source = BaseSource.Phone).visibleTracks().map { it.id })
        assertEquals(listOf("desktop", "both"), data.copy(source = BaseSource.Host).visibleTracks().map { it.id })
    }

    @Test fun localOrDownloadedCopiesRemainPlayableWhenHostAndJamAreOffline() {
        val data = BaseLayerData(tracks = listOf(track("phone"), track("remote"), track("cache")),
            phoneTrackIds = setOf("phone"), hostTrackIds = setOf("phone", "remote", "cache"),
            jamTrackIds = setOf("remote"), downloadedTrackIds = setOf("cache"), hostReachable = false)
        assertTrue(data.isPlayable("phone"))
        assertTrue(data.isPlayable("cache"))
        assertFalse(data.isPlayable("remote"))
    }

    @Test fun desktopIdentityComesFromTheSavedPairing() {
        val saved = SavedHostProfile(hostId = "owner-id", hostName = "My actual desktop", baseUrl = "http://192.0.2.1",
            baseUrls = listOf("http://192.0.2.1"), token = "test", platform = "Windows", roles = listOf("libraryHost"), savedAt = 1, lastConnectedAt = 2)
        val profile = displayedHostProfile(saved, HostConnectionState.Offline)
        assertEquals(saved.hostName, profile.hostName)
        assertEquals("Windows", profile.platform)
        assertEquals(HostConnectionState.Offline, profile.connectionState)
        val phone = displayedHostProfile(null, HostConnectionState.Disconnected)
        assertEquals("Phone library", phone.hostName)
        assertEquals("", phone.baseUrl)
    }
}
