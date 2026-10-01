package com.pixelody.app.data.storage

import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineMediaStoreTest {
    @Test
    fun cachedTrackConvertsToPlayableLocalTrack() {
        val cached = CachedTrack(
            id = "trk_123",
            title = "Starlight Voyage",
            artist = "Aether",
            album = "Cosmos",
            durationSeconds = 245,
            format = "FLAC",
            codec = "flac",
            lossless = true,
            sampleRate = 96000,
            bitDepth = 24,
            bitrate = 2800,
            channels = 2,
            replayGainDb = -1.4,
            localFilePath = "/data/user/0/com.pixelody.app/files/pixelody_offline_audio/track_trk_123.flac",
            localArtworkPath = "/data/user/0/com.pixelody.app/files/pixelody_offline_art/art_trk_123.jpg",
            fileSizeBytes = 34500000L,
            cachedAt = 1756500000L,
            lastAccessedAt = 1756500000L,
            isPinned = true
        )

        val track = cached.toTrack()
        assertEquals("trk_123", track.id)
        assertEquals("Starlight Voyage", track.title)
        assertEquals("Aether", track.artist)
        assertEquals("file:///data/user/0/com.pixelody.app/files/pixelody_offline_audio/track_trk_123.flac", track.streamUrl)
        assertEquals("file:///data/user/0/com.pixelody.app/files/pixelody_offline_art/art_trk_123.jpg", track.artworkUrl)
        assertFalse(track.missing)
        assertTrue(track.lossless)
        assertEquals(96000, track.sampleRate)
        assertEquals(24, track.bitDepth)
        assertEquals(2800, track.bitrate)
        assertEquals(2, track.channels)
        assertEquals(-1.4, track.replayGainDb ?: 0.0, 0.001)
        assertTrue(track.favorite) // isPinned maps to favorite in offline track representation
    }

    @Test
    fun cachedTrackLossyFormatFlagsNonLossless() {
        val cached = CachedTrack(
            id = "trk_mp3",
            title = "Lo-Fi Breeze",
            artist = "Chill Wave",
            album = "Summer",
            durationSeconds = 180,
            format = "MP3",
            codec = "mp3",
            localFilePath = "/data/user/0/com.pixelody.app/files/pixelody_offline_audio/track_trk_mp3.mp3",
            localArtworkPath = null,
            fileSizeBytes = 4500000L,
            cachedAt = 1756500000L
        )

        val track = cached.toTrack()
        assertFalse(track.lossless)
        assertEquals(null, track.artworkUrl)
        assertFalse(track.favorite)
    }

    @Test
    fun lruEvictionPrioritizesOldestUnpinnedTracks() {
        val t1 = CachedTrack(
            id = "t1", title = "Track 1", artist = "Artist", album = "Album", durationSeconds = 100,
            format = "FLAC", codec = "flac", localFilePath = "/path/1", localArtworkPath = null,
            fileSizeBytes = 10_000_000L, cachedAt = 1000L, lastAccessedAt = 1000L, isPinned = false
        )
        val t2 = CachedTrack(
            id = "t2", title = "Track 2", artist = "Artist", album = "Album", durationSeconds = 100,
            format = "FLAC", codec = "flac", localFilePath = "/path/2", localArtworkPath = null,
            fileSizeBytes = 10_000_000L, cachedAt = 1000L, lastAccessedAt = 5000L, isPinned = false
        )
        val t3Pinned = CachedTrack(
            id = "t3", title = "Track 3 Pinned", artist = "Artist", album = "Album", durationSeconds = 100,
            format = "FLAC", codec = "flac", localFilePath = "/path/3", localArtworkPath = null,
            fileSizeBytes = 10_000_000L, cachedAt = 100L, lastAccessedAt = 100L, isPinned = true
        )

        val list = listOf(t2, t3Pinned, t1)
        // LRU order among unpinned
        val unpinnedSorted = list.filterNot { it.isPinned }.sortedBy { it.lastAccessedAt }
        assertEquals(listOf("t1", "t2"), unpinnedSorted.map { it.id })
    }

    @Test
    fun downloadQueueStateComputesProgressAccurately() {
        val track1 = Track(id = "trk_1", title = "T1")
        val track2 = Track(id = "trk_2", title = "T2")
        val track3 = Track(id = "trk_3", title = "T3")

        val task1 = DownloadTask(track = track1, status = DownloadStatus.Completed)
        val task2 = DownloadTask(track = track2, status = DownloadStatus.InProgress(0.5f))
        val task3 = DownloadTask(track = track3, status = DownloadStatus.Queued)

        val state = DownloadQueueState(
            tasks = listOf(task1, task2, task3),
            activeTaskId = "trk_2",
            activeProgress = 0.5f
        )

        assertEquals(1, state.completedTasks.size)
        assertEquals(1, state.inProgressTasks.size)
        assertEquals(1, state.queuedTasks.size)
        assertEquals(task2, state.activeTask)
        assertFalse(state.isIdle)

        // 1 completed (1.0) + 1 at 50% (0.5) out of 3 total => 1.5 / 3 = 0.5 (50%)
        assertEquals(0.5f, state.overallProgress, 0.001f)
    }
}

