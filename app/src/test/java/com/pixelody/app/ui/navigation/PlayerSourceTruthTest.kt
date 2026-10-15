package com.pixelody.app.ui.navigation

import com.pixelody.app.data.model.HostConnectionState
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerSourceTruthTest {
    @Test
    fun playbackTogglePausesAnActivePlayer() {
        assertEquals(
            PlaybackToggleAction.Pause,
            playbackToggleAction(isPlaying = true, hasCurrentMediaItem = true, hasSelectedTrack = true)
        )
    }

    @Test
    fun playbackToggleResumesAPreparedPlayer() {
        assertEquals(
            PlaybackToggleAction.Resume,
            playbackToggleAction(isPlaying = false, hasCurrentMediaItem = true, hasSelectedTrack = true)
        )
    }

    @Test
    fun playbackToggleRebuildsAnEmptyPlayerFromTheSelectedTrack() {
        assertEquals(
            PlaybackToggleAction.StartSelected,
            playbackToggleAction(isPlaying = false, hasCurrentMediaItem = false, hasSelectedTrack = true)
        )
    }

    @Test
    fun playbackToggleDoesNothingWithoutPlayerOrSelectionState() {
        assertEquals(
            PlaybackToggleAction.None,
            playbackToggleAction(isPlaying = false, hasCurrentMediaItem = false, hasSelectedTrack = false)
        )
    }

    @Test
    fun noTrackNeverClaimsAPlaybackSource() {
        HostConnectionState.entries.forEach { state ->
            assertEquals(
                "No source selected.",
                status(hasTrack = false, isLocalTrack = false, isPlaying = true, state = state)
            )
        }
    }

    @Test
    fun localTrackReflectsActualPlayback() {
        assertEquals(
            "Playing from this Android device.",
            status(isLocalTrack = true, isPlaying = true)
        )
        assertEquals(
            "Ready on this Android device.",
            status(isLocalTrack = true, isPlaying = false)
        )
    }

    @Test
    fun connectedRemoteTrackReflectsActualPlayback() {
        assertEquals(
            "Streaming from your trusted Pixelody host.",
            status(isPlaying = true, state = HostConnectionState.Connected)
        )
        assertEquals(
            "Ready from your trusted Pixelody host.",
            status(isPlaying = false, state = HostConnectionState.Connected)
        )
    }

    @Test
    fun connectingStatesDoNotClaimPlayback() {
        listOf(HostConnectionState.Connecting, HostConnectionState.Reconnecting).forEach { state ->
            assertEquals(
                "Connecting to your trusted Pixelody host.",
                status(isPlaying = true, state = state)
            )
        }
    }

    @Test
    fun accessFailuresGiveAccessGuidance() {
        listOf(
            HostConnectionState.Revoked,
            HostConnectionState.AuthFailed,
            HostConnectionState.PermissionDenied,
            HostConnectionState.CredentialExpired
        ).forEach { state ->
            assertEquals(
                "Host access needs attention before playback.",
                status(isPlaying = true, state = state)
            )
        }
    }

    @Test
    fun availabilityFailuresGiveReconnectGuidance() {
        listOf(
            HostConnectionState.Offline,
            HostConnectionState.Unreachable,
            HostConnectionState.NetworkUnavailable,
            HostConnectionState.HostUnavailable
        ).forEach { state ->
            assertEquals(
                "Host unavailable. Reconnect before playback.",
                status(isPlaying = true, state = state)
            )
        }
    }

    @Test
    fun disconnectedRemoteTrackRequestsConnection() {
        assertEquals(
            "Connect to your trusted Pixelody host to play.",
            status(isPlaying = true, state = HostConnectionState.Disconnected)
        )
    }

    private fun status(
        hasTrack: Boolean = true,
        isLocalTrack: Boolean = false,
        isPlaying: Boolean = false,
        state: HostConnectionState = HostConnectionState.Connected
    ): String = playerSourceStatus(
        hasTrack = hasTrack,
        isLocalTrack = isLocalTrack,
        isPlaying = isPlaying,
        connectionState = state
    )
}
