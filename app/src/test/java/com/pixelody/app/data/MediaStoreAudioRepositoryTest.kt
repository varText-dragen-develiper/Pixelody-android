package com.pixelody.app.data

import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaStoreAudioRepositoryTest {

    @Test
    fun testFilterLosslessTracks() {
        val tracks = listOf(
            Track(
                id = "content://media/1",
                title = "Flac Master 24bit",
                artist = "Audiophile Artist",
                album = "Reference Album",
                durationSeconds = 240,
                format = "FLAC",
                codec = "FLAC",
                lossless = true,
                sampleRate = 96000,
                bitDepth = 24,
                bitrate = 2800,
                channels = 2,
                replayGainDb = null,
                artworkUrl = null,
                streamUrl = "content://media/1",
                favorite = false,
                missing = false
            ),
            Track(
                id = "content://media/2",
                title = "Mp3 Stream",
                artist = "Web Artist",
                album = "Lossy Album",
                durationSeconds = 180,
                format = "MP3",
                codec = "mp3",
                lossless = false,
                sampleRate = 44100,
                bitDepth = null,
                bitrate = 320,
                channels = 2,
                replayGainDb = null,
                artworkUrl = null,
                streamUrl = "content://media/2",
                favorite = false,
                missing = false
            ),
            Track(
                id = "content://media/3",
                title = "Wav Session 16bit",
                artist = "Studio Band",
                album = "Live Studio",
                durationSeconds = 300,
                format = "WAV",
                codec = "pcm_s16le",
                lossless = true,
                sampleRate = 48000,
                bitDepth = 16,
                bitrate = 1536,
                channels = 2,
                replayGainDb = null,
                artworkUrl = null,
                streamUrl = "content://media/3",
                favorite = true,
                missing = false
            )
        )

        val lossless = tracks.filter { it.lossless }
        assertEquals(2, lossless.size)
        assertTrue(lossless.all { it.format in setOf("FLAC", "WAV") })
    }

    @Test
    fun testTrackCamelotEstimation() {
        val track = Track(
            id = "content://media/10",
            title = "Harmonic Sunrise",
            artist = "Solaris",
            album = "Equinox",
            durationSeconds = 210,
            format = "FLAC",
            codec = "flac",
            lossless = true,
            sampleRate = 88200,
            bitDepth = 24,
            bitrate = 2600,
            channels = 2,
            replayGainDb = null,
            artworkUrl = null,
            streamUrl = "content://media/10",
            favorite = false,
            missing = false
        )

        val telemetry = com.pixelody.app.core.playback.HarmonicKeyEngine.estimateTrackTelemetry(track)
        assertTrue("Estimated BPM should be in normal range", telemetry.bpm >= 60f && telemetry.bpm <= 200f)
        assertTrue("Detected key confidence should be high", telemetry.detectedKeyConfidence > 0.8f)
    }
}
