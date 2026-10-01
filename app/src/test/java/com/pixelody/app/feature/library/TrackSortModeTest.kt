package com.pixelody.app.feature.library

import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackSortModeTest {

    private val trackA = Track(id = "1", title = "Arrival", artist = "ABBA", durationSeconds = 180, lossless = false, bitrate = 320)
    private val trackB = Track(id = "2", title = "Zion", artist = "Bob Marley", durationSeconds = 300, lossless = true, bitrate = 1411)
    private val trackC = Track(id = "3", title = "Breathe", artist = "Pink Floyd", durationSeconds = 160, lossless = true, bitrate = 9216)

    @Test
    fun testSortByTitle() {
        val list = listOf(trackB, trackA, trackC)
        val sorted = TrackSortMode.TitleAsc.sortTracks(list)
        assertEquals(listOf("Arrival", "Breathe", "Zion"), sorted.map { it.title })
    }

    @Test
    fun testSortByArtist() {
        val list = listOf(trackB, trackA, trackC)
        val sorted = TrackSortMode.ArtistAsc.sortTracks(list)
        assertEquals(listOf("ABBA", "Bob Marley", "Pink Floyd"), sorted.map { it.artist })
    }

    @Test
    fun testSortByDuration() {
        val list = listOf(trackA, trackB, trackC)
        val sorted = TrackSortMode.DurationDesc.sortTracks(list)
        assertEquals(listOf("Zion", "Arrival", "Breathe"), sorted.map { it.title })
    }

    @Test
    fun testSortByQualityFirst() {
        val list = listOf(trackA, trackB, trackC)
        val sorted = TrackSortMode.QualityFirst.sortTracks(list)
        // Lossless first, sorted by bitrate descending: Pink Floyd (9216), Bob Marley (1411), ABBA (lossless=false, 320)
        assertEquals(listOf("Breathe", "Zion", "Arrival"), sorted.map { it.title })
    }
}
