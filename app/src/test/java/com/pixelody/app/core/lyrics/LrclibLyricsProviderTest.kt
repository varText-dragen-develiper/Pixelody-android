package com.pixelody.app.core.lyrics

import com.pixelody.app.data.model.Track
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LrclibLyricsProviderTest {

    @Test
    fun testPickBestRecordPrefersSyncedLyricsAndDurationMatch() {
        val jsonArray = JSONArray("""
            [
                {
                    "id": 1,
                    "trackName": "Bohemian Rhapsody",
                    "artistName": "Queen",
                    "duration": 180.0,
                    "plainLyrics": "Is this the real life?"
                },
                {
                    "id": 2,
                    "trackName": "Bohemian Rhapsody",
                    "artistName": "Queen",
                    "duration": 355.0,
                    "syncedLyrics": "[00:00.15]Is this the real life?\n[00:07.13]Caught in a landslide"
                },
                {
                    "id": 3,
                    "trackName": "Bohemian Rhapsody (Live)",
                    "artistName": "Queen",
                    "duration": 360.0,
                    "plainLyrics": "Is this the real life?"
                }
            ]
        """.trimIndent())

        val best = LrclibLyricsProvider.pickBestRecordFromArray(
            array = jsonArray,
            targetTitle = "Bohemian Rhapsody",
            durationSeconds = 355
        )

        assertNotNull(best)
        assertEquals(2, best!!.getInt("id"))
    }

    @Test
    fun testPickBestRecordHandlesEmptyArray() {
        val jsonArray = JSONArray()
        val best = LrclibLyricsProvider.pickBestRecordFromArray(
            array = jsonArray,
            targetTitle = "Unknown Song",
            durationSeconds = 120
        )
        assertNull(best)
    }

    @Test
    fun testConvertRecordToDocumentWithSyncedLyrics() = runBlocking {
        val track = Track(
            id = "test-track-1",
            title = "Bohemian Rhapsody",
            artist = "Queen",
            durationSeconds = 355
        )
        val json = JSONObject("""
            {
                "id": 2,
                "trackName": "Bohemian Rhapsody",
                "artistName": "Queen",
                "syncedLyrics": "[00:00.15]Is this the real life?\n[00:07.13]Caught in a landslide\n[00:14.77]Open your eyes"
            }
        """.trimIndent())

        val doc = LrclibLyricsProvider.convertRecordToDocument(track, json, null)
        assertNotNull(doc)
        assertTrue(doc!!.hasLyrics)
        assertTrue(doc.isSynced)
        assertEquals(LyricsSource.LrcLib, doc.source)
        assertEquals(3, doc.lines.size)
        assertEquals("Is this the real life?", doc.lines[0].text)
        assertEquals(150L, doc.lines[0].timestampMs)
        assertEquals("Caught in a landslide", doc.lines[1].text)
        assertEquals(7130L, doc.lines[1].timestampMs)
    }

    @Test
    fun testConvertRecordToDocumentWithPlainLyricsAlignsAcoustics() = runBlocking {
        val track = Track(
            id = "test-track-2",
            title = "Acoustic Melody",
            artist = "Pixelody",
            durationSeconds = 180
        )
        val json = JSONObject("""
            {
                "id": 5,
                "trackName": "Acoustic Melody",
                "artistName": "Pixelody",
                "plainLyrics": "First line of the song\nSecond line follows right after\nThird line closes out the section"
            }
        """.trimIndent())

        val doc = LrclibLyricsProvider.convertRecordToDocument(track, json, null)
        assertNotNull(doc)
        assertTrue(doc!!.hasLyrics)
        assertTrue(doc.isSynced)
        assertEquals(LyricsSource.LrcLib, doc.source)
        assertTrue(doc.lines.any { it.text.contains("First line of the song") })
    }
}
