package com.pixelody.app.feature.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomePlaybackActionTest {
    @Test fun existingSessionUsesTransportEvenWhenLibraryIsEmpty() {
        assertEquals(HomePlaybackAction.Toggle, homePlaybackAction(true, true, true))
        assertEquals(HomePlaybackAction.Toggle, homePlaybackAction(false, true, true))
        assertEquals(HomePlaybackAction.Toggle, homePlaybackAction(false, true, false))
        assertEquals("Pause", homePlaybackLabel(true, true))
        assertEquals("Resume", homePlaybackLabel(false, true))
    }
    @Test fun freshSessionStartsOnlyWhenMusicIsAvailable() {
        assertEquals(HomePlaybackAction.Start, homePlaybackAction(false, false, true))
        assertEquals(HomePlaybackAction.Unavailable, homePlaybackAction(false, false, false))
        assertEquals("Play", homePlaybackLabel(false, false))
    }
}
