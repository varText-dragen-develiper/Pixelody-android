package com.pixelody.app.core.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun testParseEmptyReturnsEmptyDocument() {
        val doc = LrcParser.parse("")
        assertFalse(doc.hasLyrics)
        assertFalse(doc.isSynced)
        assertEquals(0, doc.lines.size)
    }

    @Test
    fun testParseStandardSyncedLrc() {
        val lrc = """
            [ti:Analog Dreams]
            [ar:Pixelody]
            [al:Studio Tape Sessions]
            [offset:250]
            [00:05.50]Opening guitar chord
            [00:12.80]First verse begins
            [00:25.00]Pre-chorus building up
            [00:40.00]The chorus explodes in stereo
        """.trimIndent()

        val doc = LrcParser.parse(lrc)
        assertTrue(doc.hasLyrics)
        assertTrue(doc.isSynced)
        assertEquals("Analog Dreams", doc.title)
        assertEquals("Pixelody", doc.artist)
        assertEquals("Studio Tape Sessions", doc.album)
        assertEquals(250L, doc.offsetMs)
        assertEquals(4, doc.lines.size)

        assertEquals(5500L, doc.lines[0].timestampMs)
        assertEquals("Opening guitar chord", doc.lines[0].text)
        assertEquals(12800L, doc.lines[1].timestampMs)
        assertEquals("First verse begins", doc.lines[1].text)
        assertEquals(25000L, doc.lines[2].timestampMs)
        assertEquals(40000L, doc.lines[3].timestampMs)
    }

    @Test
    fun testParseMultiTimestampLines() {
        val lrc = """
            [00:10.00][00:30.00]Repeated vocal hook
        """.trimIndent()

        val doc = LrcParser.parse(lrc)
        assertTrue(doc.isSynced)
        assertEquals(2, doc.lines.size)
        assertEquals(10000L, doc.lines[0].timestampMs)
        assertEquals("Repeated vocal hook", doc.lines[0].text)
        assertEquals(30000L, doc.lines[1].timestampMs)
        assertEquals("Repeated vocal hook", doc.lines[1].text)
    }

    @Test
    fun testActiveLineIndexBinarySearch() {
        val lrc = """
            [00:10.00]Line 1
            [00:20.00]Line 2
            [00:30.00]Line 3
            [00:40.00]Line 4
        """.trimIndent()

        val doc = LrcParser.parse(lrc)

        // Before first line
        assertEquals(-1, doc.activeLineIndex(5000L))

        // Exactly at first line
        assertEquals(0, doc.activeLineIndex(10000L))

        // Between first and second line
        assertEquals(0, doc.activeLineIndex(15000L))

        // At third line
        assertEquals(2, doc.activeLineIndex(30000L))

        // Way after last line
        assertEquals(3, doc.activeLineIndex(99000L))
    }

    @Test
    fun testActiveLineWithOffsetAdjustment() {
        val lrc = """
            [offset:1000]
            [00:10.00]Line 1
            [00:20.00]Line 2
        """.trimIndent()

        val doc = LrcParser.parse(lrc)
        // With +1000ms offset, 9000ms playback position + 1000ms offset = 10000ms (Line 1 active)
        assertEquals(0, doc.activeLineIndex(9000L))
    }

    @Test
    fun testParsePlainTextStaticLyrics() {
        val text = """
            First verse without timing
            Second verse just text
            Chorus text here
        """.trimIndent()

        val doc = LrcParser.parse(text)
        assertTrue(doc.hasLyrics)
        assertFalse(doc.isSynced)
        assertEquals(3, doc.lines.size)
        assertEquals("First verse without timing", doc.lines[0].text)
        assertEquals(-1L, doc.lines[0].timestampMs)
    }

    @Test
    fun testSerializationRoundTrip() {
        val doc = LyricsDocument(
            title = "Test Track",
            artist = "Artist Name",
            album = "Album",
            offsetMs = 100L,
            isSynced = true,
            lines = listOf(
                LyricLine(timestampMs = 5000L, text = "First line"),
                LyricLine(timestampMs = 15500L, text = "Second line")
            )
        )

        val serialized = LrcParser.serialize(doc)
        val reparsed = LrcParser.parse(serialized)

        assertEquals(doc.title, reparsed.title)
        assertEquals(doc.artist, reparsed.artist)
        assertEquals(doc.album, reparsed.album)
        assertEquals(doc.offsetMs, reparsed.offsetMs)
        assertEquals(doc.lines.size, reparsed.lines.size)
        assertEquals(doc.lines[0].timestampMs, reparsed.lines[0].timestampMs)
        assertEquals(doc.lines[0].text, reparsed.lines[0].text)
        assertEquals(doc.lines[1].timestampMs, reparsed.lines[1].timestampMs)
        assertEquals(doc.lines[1].text, reparsed.lines[1].text)
    }
}
