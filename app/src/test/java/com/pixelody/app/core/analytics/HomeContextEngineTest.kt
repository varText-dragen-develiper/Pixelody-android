package com.pixelody.app.core.analytics

import com.pixelody.app.core.playback.AudioDeviceRoute
import com.pixelody.app.data.model.DailySonicCapsule
import com.pixelody.app.data.model.SonicArchetype
import com.pixelody.app.data.model.SonicMood
import com.pixelody.app.data.model.Track
import com.pixelody.app.data.storage.CircadianPhase
import com.pixelody.app.data.storage.FakeSharedPreferences
import com.pixelody.app.data.storage.HabitActionType
import com.pixelody.app.data.storage.LocalHabitStore
import com.pixelody.app.feature.home.HomeOperationalMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar

class HomeContextEngineTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var habitStore: LocalHabitStore
    private lateinit var engine: HomeContextEngine

    private val sampleTrack = Track(
        id = "track-1",
        title = "The Secret Room",
        artist = "Chalmer-Ju",
        album = "Traitor of the Sun",
        durationSeconds = 240,
        format = "FLAC",
        codec = "FLAC",
        lossless = true,
        sampleRate = 96000,
        bitDepth = 24,
        bitrate = 2400,
        channels = 2,
        replayGainDb = null,
        artworkUrl = null,
        streamUrl = "http://localhost:8080/stream/1"
    )

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        habitStore = LocalHabitStore(fakePrefs)
        engine = HomeContextEngine(habitStore)
    }

    @Test
    fun evaluate_coldMorningLaunch_returnsHorizonCueModeAndMorningTheme() {
        val morningCal = GregorianCalendar(2026, Calendar.SEPTEMBER, 18, 8, 30) // 8:30 AM
        val state = engine.evaluate(
            isPlaying = false,
            currentTrack = null,
            nextTrack = null,
            tracks = listOf(sampleTrack),
            favoritesCount = 3,
            queueCount = 0,
            calendar = morningCal
        )

        assertEquals(HomeOperationalMode.HorizonCue, state.operationalMode)
        assertEquals(CircadianPhase.Morning, state.circadianPhase)
        assertTrue(state.greetingTitle.contains("Morning"))
        assertEquals(false, state.showActiveGrooveConsole)
    }

    @Test
    fun evaluate_activePlayback_returnsActiveGrooveModeWithConsole() {
        val state = engine.evaluate(
            isPlaying = true,
            currentTrack = sampleTrack,
            nextTrack = sampleTrack.copy(id = "track-2", title = "Next Horizon"),
            tracks = listOf(sampleTrack),
            favoritesCount = 1,
            queueCount = 1
        )

        assertEquals(HomeOperationalMode.ActiveGroove, state.operationalMode)
        assertEquals(true, state.showActiveGrooveConsole)
        assertEquals("The Secret Room", state.heroTitle)
        assertNotNull(state.nextTrack)
        assertNotNull(state.harmonicNextKey)
    }

    @Test
    fun evaluate_usbDacConnected_returnsHardwareAnchoredMode() {
        val state = engine.evaluate(
            isPlaying = false,
            currentTrack = sampleTrack,
            nextTrack = null,
            tracks = listOf(sampleTrack),
            favoritesCount = 1,
            queueCount = 0,
            audioRoute = AudioDeviceRoute.Wired
        )

        assertEquals(HomeOperationalMode.HardwareAnchored, state.operationalMode)
        assertEquals(true, state.showHardwareBanner)
        assertTrue(state.hardwareAnchor?.isUsbDac == true)
        assertTrue(state.hardwareAnchor?.title?.contains("DAC") == true)
    }

    @Test
    fun evaluate_offlineState_returnsHardwareAnchorWithLocalTracks() {
        val state = engine.evaluate(
            isPlaying = false,
            currentTrack = null,
            nextTrack = null,
            tracks = listOf(sampleTrack),
            favoritesCount = 1,
            queueCount = 0,
            isOffline = true
        )

        assertEquals(HomeOperationalMode.HardwareAnchored, state.operationalMode)
        assertEquals(true, state.showHardwareBanner)
        assertTrue(state.hardwareAnchor?.isOffline == true)
    }

    @Test
    fun evaluate_quickPaths_ranksQueueHigherWhenPlaying() {
        val idleState = engine.evaluate(
            isPlaying = false,
            currentTrack = null,
            nextTrack = null,
            tracks = listOf(sampleTrack),
            favoritesCount = 1,
            queueCount = 3
        )

        val playingState = engine.evaluate(
            isPlaying = true,
            currentTrack = sampleTrack,
            nextTrack = null,
            tracks = listOf(sampleTrack),
            favoritesCount = 1,
            queueCount = 3
        )

        val idleQueuePath = idleState.quickPaths.find { it.actionType == HabitActionType.OpenQueue }
        val playingQueuePath = playingState.quickPaths.find { it.actionType == HabitActionType.OpenQueue }

        if (idleQueuePath != null && playingQueuePath != null) {
            assertTrue(playingQueuePath.weight > idleQueuePath.weight)
        }
    }
}
