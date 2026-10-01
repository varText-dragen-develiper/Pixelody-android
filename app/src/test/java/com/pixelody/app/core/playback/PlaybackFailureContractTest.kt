package com.pixelody.app.core.playback

import androidx.media3.common.PlaybackException
import com.pixelody.app.data.model.HostConnectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackFailureContractTest {
    @Test
    fun verticalSliceFormatsHaveExplicitMediaTypes() {
        assertEquals("audio/flac", mediaMimeType("FLAC"))
        assertEquals("audio/wav", mediaMimeType("wav"))
        assertEquals("audio/mpeg", mediaMimeType("MP3"))
        assertEquals("audio/mpeg", mediaMimeType("MPEG"))
        assertEquals("audio/mpeg", mediaMimeType("audio/mpeg"))
        assertEquals("audio/mp4", mediaMimeType("m4a"))
        assertEquals("audio/mp4", mediaMimeType("MP4"))
        assertEquals("audio/aac", mediaMimeType("aac"))
        assertEquals("audio/ogg", mediaMimeType("ogg"))
        assertEquals("audio/aiff", mediaMimeType("aiff"))
        assertEquals("audio/alac", mediaMimeType("alac"))
        assertEquals("audio/x-dsd", mediaMimeType("dsf"))
    }

    @Test
    fun revokedMediaAccessClearsCredentialAndIsVisible() {
        val failure = classifyPlaybackFailure(401, "auth_revoked", PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS, "bad status")

        assertTrue(failure.clearCredential)
        assertEquals(HostConnectionState.Revoked, failure.connectionState)
        assertTrue(failure.message.contains("revoked", ignoreCase = true))
    }

    @Test
    fun missingAndNetworkMediaHaveActionableDiagnostics() {
        val missing = classifyPlaybackFailure(404, null, PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS, "bad status")
        val network = classifyPlaybackFailure(null, null, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, "network")

        assertFalse(missing.clearCredential)
        assertTrue(missing.message.contains("missing", ignoreCase = true))
        assertTrue(network.message.contains("Wi-Fi"))
    }

    @Test
    fun staleMediaOriginsAreRebasedAfterHostAddressRecovery() {
        val recovered = rebaseRemoteMediaUrl(
            "http://192.168.1.10:47813/api/v1/tracks/example/stream",
            "http://192.168.1.10:47814"
        )

        assertEquals("http://192.168.1.10:47814/api/v1/tracks/example/stream", recovered)
        assertEquals("http://192.168.1.10:47814/api/v1/tracks/example/stream", rebaseRemoteMediaUrl("/api/v1/tracks/example/stream", "http://192.168.1.10:47814"))
        assertEquals("http://192.168.1.10:47814/api/v1/tracks/example/stream", rebaseRemoteMediaUrl("api/v1/tracks/example/stream", "http://192.168.1.10:47814/"))
        assertEquals("content://media/external/audio/1", rebaseRemoteMediaUrl("content://media/external/audio/1", "http://192.168.1.10:47814"))
        assertEquals("file:///storage/emulated/0/Music/track.mp3", rebaseRemoteMediaUrl("file:///storage/emulated/0/Music/track.mp3", "http://192.168.1.10:47814"))
    }
}
