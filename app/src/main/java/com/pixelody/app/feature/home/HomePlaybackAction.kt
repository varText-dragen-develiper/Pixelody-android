package com.pixelody.app.feature.home

internal enum class HomePlaybackAction { Toggle, Start, Unavailable }

// A current session resumes at its existing position and retains its queue.
internal fun homePlaybackAction(isPlaying: Boolean, hasCurrentTrack: Boolean, hasPlayableTracks: Boolean): HomePlaybackAction = when {
    isPlaying || hasCurrentTrack -> HomePlaybackAction.Toggle
    hasPlayableTracks -> HomePlaybackAction.Start
    else -> HomePlaybackAction.Unavailable
}

internal fun homePlaybackLabel(isPlaying: Boolean, hasCurrentTrack: Boolean): String = when {
    isPlaying -> "Pause"
    hasCurrentTrack -> "Resume"
    else -> "Play"
}
