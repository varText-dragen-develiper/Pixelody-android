package com.pixelody.app

import com.pixelody.app.core.analytics.SessionCapsuleEngine
import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionCapsuleEngineTest {
    @Test fun resetRemovesOldListensAndStartsANewSession() {
        val engine = SessionCapsuleEngine()
        engine.recordTrackPlay(flacTrack)
        engine.updateSessionTime(50)
        engine.resetSession()
        assertEquals(0, engine.insights.value.tracksPlayed)
        assertEquals(0, engine.insights.value.sessionMinutes)
        engine.recordTrackPlay(mp3Track)
        assertEquals(1, engine.insights.value.tracksPlayed)
        assertEquals(0f, engine.insights.value.losslessRatio)
    }


    private val flacTrack = Track(
        id = "flac_1",
        title = "Track A",
        artist = "Artist A",
        album = "Album A",
        durationSeconds = 200,
        format = "FLAC",
        codec = "FLAC",
        lossless = true,
        sampleRate = 96000,
        bitDepth = 24,
        bitrate = 2800,
        channels = 2,
        replayGainDb = null,
        artworkUrl = null,
        streamUrl = "http://localhost/flac_1",
        favorite = false,
        missing = false
    )

    private val mp3Track = Track(
        id = "mp3_1",
        title = "Track B",
        artist = "Artist B",
        album = "Album B",
        durationSeconds = 180,
        format = "MP3",
        codec = "MP3",
        lossless = false,
        sampleRate = 44100,
        bitDepth = 16,
        bitrate = 320,
        channels = 2,
        replayGainDb = null,
        artworkUrl = null,
        streamUrl = "http://localhost/mp3_1",
        favorite = false,
        missing = false
    )

    @Test
    fun `recording track plays updates insights metrics and format dominance`() {
        val engine = SessionCapsuleEngine()
        engine.recordTrackPlay(flacTrack)
        engine.recordTrackPlay(flacTrack)
        engine.recordTrackPlay(mp3Track)

        val insights = engine.insights.value
        assertEquals(3, insights.tracksPlayed)
        assertEquals(2f / 3f, insights.losslessRatio, 0.01f)
        assertEquals("FLAC", insights.dominantFormat)
        assertTrue(insights.timeOfDayMood.isNotBlank())
    }
}
