package com.pixelody.app.ui.components

import com.pixelody.app.data.model.Track
import org.junit.Assert.*
import org.junit.Test

class SourcePlaybackAvailabilityTest {
    private val remote = Track(id = "desktop", title = "Downloaded song", streamUrl = "https://desktop/song")

    @Test fun downloadedDesktopMusicRemainsInOfflinePlayableResults() {
        assertTrue(isTrackPlayableFromSources(remote, false, emptySet(), setOf(remote.id)))
        assertFalse(isTrackPlayableFromSources(remote, false, emptySet(), emptySet()))
        assertTrue(isTrackPlayableFromSources(remote, true, emptySet(), emptySet()))
        assertTrue(isTrackPlayableFromSources(remote, false, setOf(remote.id), emptySet()))
    }

    @Test fun ownershipDoesNotOverrideMissingOrUnusableTrackMetadata() {
        assertFalse(isTrackPlayableFromSources(remote.copy(missing = true), true, emptySet(), setOf(remote.id)))
        assertFalse(isTrackPlayableFromSources(remote.copy(streamUrl = ""), true, emptySet(), setOf(remote.id)))
    }
}
