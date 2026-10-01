package com.pixelody.app.ui.navigation

import com.pixelody.app.data.model.HostConnectionState

internal enum class PlaybackToggleAction {
    Pause,
    Resume,
    StartSelected,
    None
}

internal fun playbackToggleAction(
    isPlaying: Boolean,
    hasCurrentMediaItem: Boolean,
    hasSelectedTrack: Boolean
): PlaybackToggleAction = when {
    isPlaying -> PlaybackToggleAction.Pause
    hasCurrentMediaItem -> PlaybackToggleAction.Resume
    hasSelectedTrack -> PlaybackToggleAction.StartSelected
    else -> PlaybackToggleAction.None
}

internal fun playerSourceStatus(
    hasTrack: Boolean,
    isLocalTrack: Boolean,
    isPlaying: Boolean,
    connectionState: HostConnectionState
): String {
    if (!hasTrack) return "No source selected."

    if (isLocalTrack) {
        return if (isPlaying) {
            "Playing from this Android device."
        } else {
            "Ready on this Android device."
        }
    }

    return when (connectionState) {
        HostConnectionState.Connected -> if (isPlaying) {
            "Streaming from your trusted Pixelody host."
        } else {
            "Ready from your trusted Pixelody host."
        }

        HostConnectionState.Connecting,
        HostConnectionState.Reconnecting ->
            "Connecting to your trusted Pixelody host."

        HostConnectionState.Revoked,
        HostConnectionState.AuthFailed,
        HostConnectionState.PermissionDenied,
        HostConnectionState.CredentialExpired ->
            "Host access needs attention before playback."

        HostConnectionState.Offline,
        HostConnectionState.Unreachable,
        HostConnectionState.NetworkUnavailable,
        HostConnectionState.HostUnavailable ->
            "Host unavailable. Reconnect before playback."

        HostConnectionState.Disconnected ->
            "Connect to your trusted Pixelody host to play."
    }
}
