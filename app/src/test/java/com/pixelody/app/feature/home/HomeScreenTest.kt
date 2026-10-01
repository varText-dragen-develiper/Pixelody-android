package com.pixelody.app.feature.home

import com.pixelody.app.data.model.DailySonicCapsule
import com.pixelody.app.data.model.HostConnectionState
import com.pixelody.app.data.model.SavedHostProfile
import com.pixelody.app.data.model.SonicArchetype
import com.pixelody.app.data.model.SonicMood
import com.pixelody.app.data.model.Track
import com.pixelody.app.feature.connection.label
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeScreenTest {

    @Test
    fun dailySonicCapsule_propertiesPreserved() {
        val sampleTracks = listOf(
            Track(
                id = "track-101",
                title = "Neon Genesis",
                artist = "Pixelody Studio",
                album = "Synthetic Horizon",
                durationSeconds = 245,
                format = "FLAC",
                lossless = true,
                streamUrl = "http://127.0.0.1:8080/audio/101.flac"
            )
        )

        val capsule = DailySonicCapsule(
            dateString = "Thu, Sep 17",
            archetype = SonicArchetype.HiResAudiophile,
            primaryMood = SonicMood.LateNightDrift,
            aiNarrative = "Deep ambient textures.",
            highlightTrackIds = listOf("track-101"),
            highlightTracks = sampleTracks,
            streakDays = 5
        )

        assertEquals("Thu, Sep 17", capsule.dateString)
        assertEquals(SonicArchetype.HiResAudiophile, capsule.archetype)
        assertEquals(SonicMood.LateNightDrift, capsule.primaryMood)
        assertEquals("Deep ambient textures.", capsule.aiNarrative)
        assertEquals(5, capsule.streakDays)
        assertEquals(1, capsule.highlightTracks.size)
        assertEquals("Neon Genesis", capsule.highlightTracks.first().title)
    }

    @Test
    fun hostConnectionState_labelsMatchExpected() {
        assertEquals("Connected", HostConnectionState.Connected.label)
        assertEquals("Connecting", HostConnectionState.Connecting.label)
        assertEquals("Not connected", HostConnectionState.Disconnected.label)
        assertEquals("Reconnecting", HostConnectionState.Reconnecting.label)
        assertEquals("Host offline", HostConnectionState.Offline.label)
    }

    @Test
    fun savedHostProfile_construction() {
        val profile = SavedHostProfile(
            hostId = "host-01",
            hostName = "Studio Workstation",
            baseUrl = "http://192.168.1.50:4040",
            baseUrls = listOf("http://192.168.1.50:4040"),
            token = "auth-sec-token-99",
            platform = "Windows",
            roles = listOf("LibraryHost"),
            savedAt = 1758000000000L,
            lastConnectedAt = 1758000000000L
        )

        assertEquals("host-01", profile.hostId)
        assertEquals("Studio Workstation", profile.hostName)
        assertEquals("http://192.168.1.50:4040", profile.baseUrl)
        assertEquals("auth-sec-token-99", profile.token)
        assertEquals("Windows", profile.platform)
        assertEquals(1758000000000L, profile.lastConnectedAt)
    }

    @Test
    fun appExperienceMode_propertiesAndToggle() {
        val essential = com.pixelody.app.data.model.AppExperienceMode.Essential
        val studio = com.pixelody.app.data.model.AppExperienceMode.Studio

        assertEquals("Essential", essential.label)
        assertEquals("⚡", essential.badge)
        assertEquals(studio, essential.toggle())

        assertEquals("Studio", studio.label)
        assertEquals("✦", studio.badge)
        assertEquals(essential, studio.toggle())
    }
}
