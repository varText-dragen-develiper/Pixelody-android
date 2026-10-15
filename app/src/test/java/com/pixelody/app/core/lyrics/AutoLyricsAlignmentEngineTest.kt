package com.pixelody.app.core.lyrics

import com.pixelody.app.data.model.Track
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AutoLyricsAlignmentEngineTest {
    private val track = Track(id = "lyrics", title = "Neon Resonance", artist = "Pixelody Studio",
        album = "Analog Horizons", durationSeconds = 240, format = "FLAC")

    @Test fun parseRawLyricsPreservesWordsAndStripsExistingTimestampLabels() {
        assertEquals(listOf("First line", "Second line"), AutoLyricsAlignmentEngine.parseRawLyricsIntoLines(
            "[00:05.00] First line\n\n[00:15.500]Second line"))
    }

    @Test fun unknownTempoPreservesTextAndMetadataWithoutInventingTiming() = runBlocking {
        val result = AutoLyricsAlignmentEngine.processAndAlign(track, "First line\nSecond line")
        assertTrue(result.hasLyrics)
        assertFalse(result.isSynced)
        assertEquals(LyricsSource.None, result.source)
        assertEquals(listOf("First line", "Second line"), result.lines.map { it.text })
        assertTrue(result.lines.all { it.timestampMs < 0L })
        assertEquals(track.title, result.title)
        assertEquals(track.artist, result.artist)
        assertEquals(track.album, result.album)
    }

    @Test fun knownTaggedTempoCanProvideExplicitHeuristicAlignment() = runBlocking {
        val result = AutoLyricsAlignmentEngine.processAndAlign(track.copy(bpm = 120f), "First line\nSecond line")
        assertTrue(result.isSynced)
        assertEquals(LyricsSource.AutoAligned, result.source)
        assertTrue(result.lines.any { it.text == "First line" })
        assertTrue(result.lines.zipWithNext().all { (a, b) -> b.timestampMs > a.timestampMs })
        assertTrue(result.lines.all { it.timestampMs < track.durationSeconds * 1000L })
    }

    @Test fun missingLyricsDoNotBecomeInventedSongWordsOrAcousticMarkers() = runBlocking {
        listOf(track, track.copy(bpm = 140f, musicalKey = "8A")).forEach {
            val result = AutoLyricsAlignmentEngine.processAndAlign(it)
            assertFalse(result.hasLyrics)
            assertFalse(result.isSynced)
            assertEquals(it.title, result.title)
            assertEquals(LyricsSource.None, result.source)
        }
    }

    @Test fun unavailableDecodedAudioDoesNotBecomeHashDerivedEnergyPeaks() {
        assertTrue(AutoLyricsAlignmentEngine.extractOrSynthesizePeakProfile(track, 240_000, 120f, null).isEmpty())
    }

    @Test fun unknownDurationPreservesTextEvenWithTaggedTempo() = runBlocking {
        val result = AutoLyricsAlignmentEngine.processAndAlign(track.copy(durationSeconds = 0, bpm = 120f), "Keep these words")
        assertFalse(result.isSynced)
        assertEquals(listOf("Keep these words"), result.lines.map { it.text })
    }
}
