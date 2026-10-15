package com.pixelody.app

import com.pixelody.app.data.model.Track
import com.pixelody.app.core.playback.QueueUndoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueUndoManagerTest {

    private val sampleTrack = Track(
        id = "track_1",
        title = "Moonlit Circuit",
        artist = "Local Signal",
        album = "Night Horizon",
        durationSeconds = 180,
        format = "FLAC",
        codec = "FLAC",
        lossless = true,
        sampleRate = 96000,
        bitDepth = 24,
        bitrate = 2800,
        channels = 2,
        replayGainDb = null,
        artworkUrl = null,
        streamUrl = "http://localhost/track_1",
        favorite = true,
        missing = false
    )

    @Test
    fun `recording dismissal enables undo state and preserves track and index`() {
        val manager = QueueUndoManager()
        assertFalse(manager.undoState.value.canUndo)

        manager.recordDismissal(sampleTrack, 3)
        val state = manager.undoState.value

        assertTrue(state.canUndo)
        assertEquals("Removed \"Moonlit Circuit\"", state.message)
        assertEquals(3, state.lastDismissed?.index)
        assertEquals("track_1", state.lastDismissed?.track?.id)
    }

    @Test
    fun `host revocation discards remote undo entries while preserving local ones`() {
        val manager = QueueUndoManager()
        val local = sampleTrack.copy(id = "local", streamUrl = "content://media/external/audio/1")
        manager.recordDismissal(sampleTrack, 0)
        manager.recordDismissal(local, 1)
        manager.recordDismissal(sampleTrack, 2)
        manager.discardTracks(setOf(sampleTrack.id))
        assertEquals("local", manager.undoState.value.lastDismissed?.track?.id)
        assertEquals("local", manager.popUndo()?.track?.id)
        assertNull(manager.popUndo())
    }

    @Test
    fun `popUndo restores dismissed entry and clears state when empty`() {
        val manager = QueueUndoManager()
        manager.recordDismissal(sampleTrack, 2)

        val restored = manager.popUndo()
        assertNotNull(restored)
        assertEquals(2, restored?.index)
        assertEquals("track_1", restored?.track?.id)

        assertFalse(manager.undoState.value.canUndo)
        assertNull(manager.undoState.value.lastDismissed)
    }
}
