package com.pixelody.app.feature.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeStartStateTest {
    @Test fun freshLibraryOffersSetup() {
        assertEquals(HomeStartState.Empty, homeStartState(0, 0, false, false))
    }
    @Test fun cachedMusicStaysUsableDuringBackgroundWork() {
        assertEquals(HomeStartState.Ready, homeStartState(4, 4, true, true))
    }
    @Test fun loadingIsNotAnEmptyLibrary() {
        assertEquals(HomeStartState.Loading, homeStartState(0, 0, true, false))
    }
    @Test fun connectingIsNotAFailedOrEmptyLibrary() {
        assertEquals(HomeStartState.Connecting, homeStartState(0, 0, false, true))
    }
    @Test fun emptySourceOffersExistingMusicInsteadOfRepeatedImport() {
        assertEquals(HomeStartState.OtherSource, homeStartState(0, 4, false, false))
    }
}
