package com.pixelody.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LiveSyncContractTest {
    @Test
    fun playingPositionInterpolatesFromAnchorAndClampsToDuration() {
        val playback = NetworkSessionPlayback(
            state = "playing",
            currentTrackId = "track",
            currentTrack = null,
            playing = true,
            elapsedSeconds = 12.0,
            durationSeconds = 15.0,
            positionUpdatedAt = "2026-07-02T00:00:01Z",
            estimatedStartedAt = "2026-07-01T23:59:49Z"
        )

        assertEquals(14.0, playback.estimatedElapsedSeconds(1782950403000L), 0.001)
        assertEquals(15.0, playback.estimatedElapsedSeconds(1782950410000L), 0.001)
    }

    @Test
    fun pausedPositionDoesNotAdvance() {
        val playback = NetworkSessionPlayback(
            state = "paused",
            currentTrackId = "track",
            currentTrack = null,
            playing = false,
            elapsedSeconds = 12.0,
            durationSeconds = 15.0,
            positionUpdatedAt = "2026-07-02T00:00:01Z",
            estimatedStartedAt = ""
        )

        assertEquals(12.0, playback.estimatedElapsedSeconds(1782950410000L), 0.001)
    }
}
