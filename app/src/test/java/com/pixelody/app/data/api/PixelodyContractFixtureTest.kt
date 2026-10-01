package com.pixelody.app.data.api

import com.pixelody.app.data.model.HostConnectionDetails
import com.pixelody.app.data.network.toJamSessionModel
import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelodyContractFixtureTest {
    @Test
    fun sharedPairingFixtureParsesAsConnectionDetails() {
        val fixture = fixtureText("pairing-payload.json")
        val details = HostConnectionDetails.fromText(fixture)

        requireNotNull(details)
        assertEquals("042917", details.pairingCode)
        assertEquals("pairing-secret", details.pairingSecret)
        assertTrue(details.hasPairingSecret)
        assertTrue(details.baseUrls.contains("http://192.168.1.45:4822"))
        assertEquals(listOf("browse", "stream", "playback:read"), details.requestedPermissions)
    }

    @Test
    fun jamQueueFixtureKeepsFederatedSourceFields() {
        val json = JSONObject(fixtureText("jam-queue-item.json"))

        assertEquals("android-phone", json.getString("addedByDeviceId"))
        assertEquals("studio-pc", json.getString("sourceDeviceId"))
        assertEquals("studio-pc-library", json.getString("sourceLibraryId"))
        assertTrue(json.has("cacheState"))
        assertTrue(json.has("fallbackCandidates"))
    }

    @Test
    fun singleHostJamFixtureMapsRolesParticipantsPermissionsAndQueueOwnership() {
        val session = JSONObject(fixtureText("jam-session-v1.json")).put("active", true).toJamSessionModel()

        assertTrue(session.active)
        assertEquals("single-host", session.mode)
        assertTrue(!session.permissions.federatedSourcesEnabled)
        assertEquals("studio-pc", session.participants.first().deviceId)
        assertEquals("android-phone", session.participants[1].deviceId)
        assertTrue(session.participants[1].permissions.contains("suggest"))
        assertEquals("studio-pc", session.queue.first().sourceDeviceId)
        assertEquals("studio-pc-library", session.queue.first().sourceLibraryId)
        assertTrue(session.queue.first().fallbackCandidates.isEmpty())
    }

    @Test
    fun librarySnapshotFixtureDoesNotExposeDesktopPaths() {
        val fixture = fixtureText("library-snapshot.json")
        val json = JSONObject(fixture)
        val track = json.getJSONArray("tracks").getJSONObject(0)

        assertTrue(track.getString("streamUrl").startsWith("/api/v1/tracks/"))
        assertTrue(!fixture.contains("C:\\"))
        assertTrue(!fixture.contains("artworkPath"))
    }

    @Test
    fun liveStateFixtureIncludesUniversalNetworkSessionInfo() {
        val json = JSONObject(fixtureText("live-state.json"))
        val session = json.getJSONObject("networkSession")
        val playback = session.getJSONObject("playback")
        val queue = session.getJSONObject("queue")
        val revisions = json.getJSONObject("revisions")

        assertEquals("personal-library", session.getString("kind"))
        assertEquals("playing", playback.getString("state"))
        assertEquals("public-track-1", playback.getJSONObject("currentTrack").getString("id"))
        assertEquals(12.0, playback.getDouble("elapsedSeconds"), 0.01)
        assertTrue(queue.has("upcomingTrackIds"))
        assertTrue(queue.has("upcomingTracks"))
        assertEquals(1, session.getJSONObject("shuffle").getInt("version"))
        assertTrue(session.getJSONObject("shuffle").getBoolean("enabled"))
        assertTrue(!json.toString().contains("signalJournal"))
        assertEquals(3L, revisions.getLong("overall"))
        assertEquals(2L, revisions.getLong("playback"))
        assertTrue(revisions.has("jam"))
    }

    @Test
    fun revisionAwareFixturesSeparateUnchangedAndPagedPayloads() {
        val unchanged = JSONObject(fixtureText("live-unchanged.json"))
        val page = JSONObject(fixtureText("library-tracks-page.json"))

        assertTrue(unchanged.getBoolean("unchanged"))
        assertTrue(!unchanged.has("playback"))
        assertEquals(1, page.getJSONArray("tracks").length())
        assertEquals(1, page.getJSONObject("page").getInt("nextOffset"))
        assertTrue(page.getJSONObject("revisions").has("library"))
    }

    private fun fixtureText(name: String): String {
        var cursor = File(System.getProperty("user.dir") ?: ".").absoluteFile
        repeat(6) {
            val candidate = File(cursor, "../docs/api-contract-fixtures/$name").canonicalFile
            if (candidate.isFile) return candidate.readText()
            cursor = cursor.parentFile ?: cursor
        }
        throw IllegalStateException("Could not find shared contract fixture: $name")
    }
}
