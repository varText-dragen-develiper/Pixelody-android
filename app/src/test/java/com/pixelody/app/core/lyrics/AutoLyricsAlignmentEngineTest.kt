package com.pixelody.app.core.lyrics

import com.pixelody.app.core.playback.MusicalPhraseEngine
import com.pixelody.app.data.model.Track
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoLyricsAlignmentEngineTest {

    private val testTrack = Track(
        id = "test_track_1",
        title = "Neon Resonance",
        artist = "Pixelody Studio",
        album = "Analog Horizons",
        durationSeconds = 240, // 4 minutes
        format = "FLAC",
        lossless = true,
        sampleRate = 96000,
        bitDepth = 24,
        channels = 2,
        streamUrl = "file:///music/test_track_1.flac"
    )

    @Test
    fun testParseRawLyricsIntoLines() {
        val raw = """
            [00:05.00] First raw line
            Second raw line
            
            [00:15.50]Third raw line with timing
            
            Fourth line
        """.trimIndent()

        val lines = AutoLyricsAlignmentEngine.parseRawLyricsIntoLines(raw)
        assertEquals(4, lines.size)
        assertEquals("First raw line", lines[0])
        assertEquals("Second raw line", lines[1])
        assertEquals("Third raw line with timing", lines[2])
        assertEquals("Fourth line", lines[3])
    }

    @Test
    fun testAutoAlignmentWithUnsyncedLyrics() = runBlocking {
        val unsyncedText = """
            Standing on the edge of sound
            Looking at the waveform deep
            Every rhythm that we found
            Is a promise that we keep
            • Chorus •
            Catch the neon in the night
            Turn the analog signal high
            Pure frequencies burning bright
            Underneath the midnight sky
        """.trimIndent()

        val doc = AutoLyricsAlignmentEngine.processAndAlign(
            track = testTrack,
            rawText = unsyncedText,
            context = null
        )

        assertTrue(doc.hasLyrics)
        assertTrue(doc.isSynced)
        assertEquals(LyricsSource.AutoAligned, doc.source)
        assertEquals(testTrack.title, doc.title)
        assertEquals(testTrack.artist, doc.artist)

        // Must include instrumental intro + lines + outro
        assertTrue(doc.lines.size >= 9)

        // First line is Intro
        assertEquals("[Instrumental Intro]", doc.lines[0].text)
        assertEquals(0L, doc.lines[0].timestampMs)
        assertTrue(doc.lines[0].isInstrumental)

        // Subsequent lines must have monotonically increasing timestamps
        var previousTimestamp = 0L
        for (i in 1 until doc.lines.size) {
            val line = doc.lines[i]
            assertTrue(
                "Timestamp at index $i (${line.timestampMs}) should be > previous ($previousTimestamp)",
                line.timestampMs > previousTimestamp
            )
            previousTimestamp = line.timestampMs
        }

        // Final line is Outro
        val lastLine = doc.lines.last()
        assertTrue(lastLine.isInstrumental)
        assertTrue(lastLine.text.contains("Outro", ignoreCase = true))
    }

    @Test
    fun testContextualLyricsGenerationWithoutRawText() = runBlocking {
        val doc = AutoLyricsAlignmentEngine.processAndAlign(
            track = testTrack,
            rawText = null,
            context = null
        )

        assertTrue(doc.hasLyrics)
        assertTrue(doc.isSynced)
        assertEquals(LyricsSource.AutoAligned, doc.source)

        // Should contain track-specific references
        val combinedText = doc.lines.joinToString(" ") { it.text }
        assertTrue(combinedText.contains("Pixelody Studio", ignoreCase = true) || combinedText.contains("Pixelody", ignoreCase = true))
        assertTrue(combinedText.contains("Neon Resonance", ignoreCase = true))

        // Binary search for active lyric line
        val midPosMs = 60_000L
        val activeIdx = doc.activeLineIndex(midPosMs)
        assertTrue("Active line index at 60s should be valid", activeIdx >= 0 && activeIdx < doc.lines.size)
        assertTrue(doc.lines[activeIdx].timestampMs <= midPosMs)
    }

    @Test
    fun testPeakProfileExtractionAndDownbeats() {
        val bpm = 128f
        val durationMs = 180_000L
        val peaks = AutoLyricsAlignmentEngine.extractOrSynthesizePeakProfile(testTrack, durationMs, bpm, null)

        assertEquals(128, peaks.size)
        for (peak in peaks) {
            assertTrue(peak in 0.0f..1.0f)
        }
    }

    @Test
    fun testFastTempoAlignmentLocksToBars() = runBlocking {
        val fastTrack = testTrack.copy(
            title = "Fast Electro",
            durationSeconds = 120 // 2 minutes at 140 BPM
        )

        val text = """
            Line one
            Line two
            Line three
            Line four
        """.trimIndent()

        val doc = AutoLyricsAlignmentEngine.processAndAlign(
            track = fastTrack,
            rawText = text,
            context = null
        )

        assertTrue(doc.isSynced)
        assertTrue(doc.lines.isNotEmpty())
        assertTrue(doc.lines.first().timestampMs == 0L)
        assertTrue(doc.lines.last().timestampMs <= fastTrack.durationSeconds * 1000L)
    }

    @Test
    fun testDistinctSongLyricsGenerationWithoutBoilerplate() = runBlocking {
        val trackA = Track(
            id = "track_a",
            title = "Midnight Echo",
            artist = "Lunar Eclipse",
            album = "Nightfall",
            durationSeconds = 200,
            format = "FLAC"
        )

        val trackB = Track(
            id = "track_b",
            title = "Sunlight Symphony",
            artist = "Solar Flare",
            album = "Daybreak",
            durationSeconds = 180,
            format = "MP3"
        )

        val docA = AutoLyricsAlignmentEngine.processAndAlign(trackA, null, null)
        val docB = AutoLyricsAlignmentEngine.processAndAlign(trackB, null, null)

        val textA = docA.lines.joinToString("\n") { it.text }
        val textB = docB.lines.joinToString("\n") { it.text }

        // Must be distinct
        assertTrue("Track A and Track B lyrics must be distinct", textA != textB)

        // Must mention their respective titles and artists
        assertTrue(textA.contains("Midnight Echo", ignoreCase = true))
        assertTrue(textA.contains("Lunar Eclipse", ignoreCase = true))
        assertTrue(textB.contains("Sunlight Symphony", ignoreCase = true))
        assertTrue(textB.contains("Solar Flare", ignoreCase = true))

        // Must NOT contain old static boilerplate phrases
        assertFalse(textA.contains("Feel the FLAC acoustics", ignoreCase = true))
        assertFalse(textA.contains("Analog warmth floating everywhere", ignoreCase = true))
        assertFalse(textA.contains("on this sonic road", ignoreCase = true))
        assertFalse(textB.contains("Feel the FLAC acoustics", ignoreCase = true))
        assertFalse(textB.contains("on this sonic road", ignoreCase = true))
    }
}
