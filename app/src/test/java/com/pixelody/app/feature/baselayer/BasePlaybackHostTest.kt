package com.pixelody.app.feature.baselayer

import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BasePlaybackHostTest {

    private fun sampleTrack(
        id: String,
        title: String = "Test Track $id",
        artist: String = "Artist $id",
        album: String = "Album $id"
    ) = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationSeconds = 180
    )

    @Test
    fun listeningSlotStateRendersResumeWhenLastTrackIsPresentWithoutActivePlayingTrack() {
        val track = sampleTrack("t1", title = "Saline", artist = "Held Pattern", album = "Lowlight")
        val data = BaseLayerData(
            tracks = listOf(track),
            currentTrackId = null,
            lastTrackId = "t1",
            isPlaying = false
        )

        val slot = data.listeningSlotState()
        assertTrue(slot is ListeningSlotState.Resume)
        val resumeSlot = slot as ListeningSlotState.Resume
        assertEquals("Saline", resumeSlot.title)
        assertEquals("Paused earlier · Held Pattern", resumeSlot.subtitle)
        assertEquals("L", resumeSlot.badge)
    }

    @Test
    fun listeningSlotStateRendersOccupiedWhenCurrentTrackIsActive() {
        val track = sampleTrack("t2", title = "Lo-Fi Meditation", artist = "Nakura", album = "Tape Room")
        val data = BaseLayerData(
            tracks = listOf(track),
            currentTrackId = "t2",
            lastTrackId = "t2",
            isPlaying = true
        )

        val slot = data.listeningSlotState()
        assertTrue(slot is ListeningSlotState.Occupied)
        val occupiedSlot = slot as ListeningSlotState.Occupied
        assertEquals("Lo-Fi Meditation", occupiedSlot.title)
        assertEquals("Nakura", occupiedSlot.subtitle)
        assertEquals("TR", occupiedSlot.badge)
        assertTrue(occupiedSlot.isPlaying)
    }

    @Test
    fun listeningSlotStateRendersSilentWhenNoCurrentOrLastTrackExists() {
        val data = BaseLayerData(
            tracks = emptyList(),
            currentTrackId = null,
            lastTrackId = null
        )

        val slot = data.listeningSlotState()
        assertEquals(ListeningSlotState.Silent, slot)
    }

    @Test
    fun queueProgressionDropsFirstTrackOnNext() {
        val track1 = sampleTrack("t1")
        val track2 = sampleTrack("t2")
        val track3 = sampleTrack("t3")
        var data = BaseLayerData(
            tracks = listOf(track1, track2, track3),
            currentTrackId = "t1",
            queue = listOf("t2", "t3")
        )

        // Advance to next
        val nextId = data.queue.first()
        data = data.copy(
            currentTrackId = nextId,
            lastTrackId = nextId,
            queue = data.queue.drop(1)
        )

        assertEquals("t2", data.currentTrackId)
        assertEquals(listOf("t3"), data.queue)
    }
}
