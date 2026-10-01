package com.pixelody.app.ui.components

import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TriSourcePivotBarContractTest {

    @Test
    fun sourceScopeFromIdParsing() {
        assertEquals(SourceScope.All, SourceScope.fromId("all"))
        assertEquals(SourceScope.LocalPhone, SourceScope.fromId("phone"))
        assertEquals(SourceScope.DesktopHost, SourceScope.fromId("host"))
        assertEquals(SourceScope.JamMesh, SourceScope.fromId("jam"))

        // Fallbacks
        assertEquals(SourceScope.All, SourceScope.fromId(null))
        assertEquals(SourceScope.All, SourceScope.fromId(""))
        assertEquals(SourceScope.All, SourceScope.fromId("unknown_scope"))
    }

    @Test
    fun sourceScopeGlyphTypeMappings() {
        assertEquals(TransportGlyphType.OmniSource, SourceScope.All.toGlyphType())
        assertEquals(TransportGlyphType.PhoneDevice, SourceScope.LocalPhone.toGlyphType())
        assertEquals(TransportGlyphType.DesktopHost, SourceScope.DesktopHost.toGlyphType())
        assertEquals(TransportGlyphType.MeshNetwork, SourceScope.JamMesh.toGlyphType())
    }

    @Test
    fun filterTracksBySourceSeparatesPhoneHostAndJam() {
        val phoneTrack1 = Track(id = "p-1", title = "Phone Track 1", artist = "A", streamUrl = "content://media/audio/1")
        val phoneTrack2 = Track(id = "p-2", title = "Phone Track 2", artist = "B", streamUrl = "file:///storage/emulated/0/Music/2.mp3")
        val phoneTrack3 = Track(id = "p-3", title = "Phone Track 3", artist = "C", streamUrl = "http://127.0.0.1:4822/stream/3") // explicitly in local set
        val hostTrack1 = Track(id = "h-1", title = "Host Track 1", artist = "D", streamUrl = "http://192.168.1.100:4822/api/v1/tracks/h-1/stream")
        val hostTrack2 = Track(id = "h-2", title = "Host Track 2", artist = "E", streamUrl = "http://192.168.1.100:4822/api/v1/tracks/h-2/stream")
        val jamTrack = Track(id = "j-1", title = "Jam Collab", artist = "F", streamUrl = "http://192.168.1.100:4822/api/v1/tracks/j-1/stream")

        val allTracks = listOf(phoneTrack1, phoneTrack2, phoneTrack3, hostTrack1, hostTrack2, jamTrack)
        val localIds = setOf("p-1", "p-2", "p-3")
        val jamIds = setOf("j-1")

        // Scope: All
        val allResult = filterTracksBySource(allTracks, SourceScope.All, localIds, jamIds)
        assertEquals(6, allResult.size)

        // Scope: LocalPhone
        val phoneResult = filterTracksBySource(allTracks, SourceScope.LocalPhone, localIds, jamIds)
        assertEquals(3, phoneResult.size)
        assertTrue(phoneResult.contains(phoneTrack1))
        assertTrue(phoneResult.contains(phoneTrack2))
        assertTrue(phoneResult.contains(phoneTrack3))

        // Scope: DesktopHost (excludes localIds and content:// / file://)
        val hostResult = filterTracksBySource(allTracks, SourceScope.DesktopHost, localIds, jamIds)
        assertEquals(3, hostResult.size)
        assertTrue(hostResult.contains(hostTrack1))
        assertTrue(hostResult.contains(hostTrack2))
        assertTrue(hostResult.contains(jamTrack))

        // Scope: JamMesh
        val jamResult = filterTracksBySource(allTracks, SourceScope.JamMesh, localIds, jamIds)
        assertEquals(1, jamResult.size)
        assertEquals(jamTrack, jamResult.first())
    }
}
