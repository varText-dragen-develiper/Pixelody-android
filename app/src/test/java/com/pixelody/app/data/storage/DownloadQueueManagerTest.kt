package com.pixelody.app.data.storage

import com.pixelody.app.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadQueueManagerTest {

    private fun createTrack(id: String, title: String = "Track $id"): Track {
        return Track(
            id = id,
            title = title,
            artist = "Artist $id",
            album = "Album $id",
            durationSeconds = 180,
            format = "FLAC",
            lossless = true
        )
    }

    @Test
    fun downloadQueueStateComputesProgressAndIdleAccurately() {
        val t1 = createTrack("trk_1")
        val t2 = createTrack("trk_2")
        val t3 = createTrack("trk_3")

        val stateEmpty = DownloadQueueState()
        assertTrue(stateEmpty.isIdle)
        assertEquals(1.0f, stateEmpty.overallProgress, 0.001f)
        assertNull(stateEmpty.activeTask)

        val task1 = DownloadTask(track = t1, status = DownloadStatus.Completed)
        val task2 = DownloadTask(track = t2, status = DownloadStatus.InProgress(0.6f))
        val task3 = DownloadTask(track = t3, status = DownloadStatus.Queued)

        val stateActive = DownloadQueueState(
            tasks = listOf(task1, task2, task3),
            activeTaskId = "trk_2",
            activeProgress = 0.6f
        )

        assertFalse(stateActive.isIdle)
        assertEquals(1, stateActive.completedTasks.size)
        assertEquals(1, stateActive.inProgressTasks.size)
        assertEquals(1, stateActive.queuedTasks.size)
        assertEquals("trk_2", stateActive.activeTask?.track?.id)

        // (1.0 + 0.6) / 3 = 1.6 / 3 = 0.5333f
        assertEquals(0.5333f, stateActive.overallProgress, 0.001f)
    }

    @Test
    fun downloadQueueStateFiltersFailedAndPausedTasks() {
        val t1 = createTrack("trk_1")
        val t2 = createTrack("trk_2")
        val t3 = createTrack("trk_3")

        val task1 = DownloadTask(track = t1, status = DownloadStatus.Failed("Network timeout"))
        val task2 = DownloadTask(track = t2, status = DownloadStatus.Paused)
        val task3 = DownloadTask(track = t3, status = DownloadStatus.Completed)

        val state = DownloadQueueState(
            tasks = listOf(task1, task2, task3),
            isPaused = true
        )

        assertEquals(1, state.failedTasks.size)
        assertEquals("Network timeout", (state.failedTasks[0].status as DownloadStatus.Failed).reason)
        assertEquals(1, state.completedTasks.size)
        assertTrue(state.isIdle) // Neither Queued nor InProgress
    }

    @Test
    fun downloadTaskPinnedFlagPreserved() {
        val track = createTrack("trk_pin")
        val task = DownloadTask(track = track, isPinned = true)
        assertTrue(task.isPinned)
        assertEquals("trk_pin", task.track.id)
    }

    @Test
    fun downloadQueueStatePauseAndResumeTransitions() {
        val t1 = createTrack("trk_1")
        val t2 = createTrack("trk_2")
        val t3 = createTrack("trk_3")

        val initial = DownloadQueueState(
            tasks = listOf(
                DownloadTask(track = t1, status = DownloadStatus.InProgress(0.4f)),
                DownloadTask(track = t2, status = DownloadStatus.Queued),
                DownloadTask(track = t3, status = DownloadStatus.Completed)
            ),
            activeTaskId = "trk_1",
            activeProgress = 0.4f
        )

        // Simulate pause state mapping
        val paused = initial.copy(
            isPaused = true,
            tasks = initial.tasks.map {
                if (it.status is DownloadStatus.Queued || it.status is DownloadStatus.InProgress) {
                    it.copy(status = DownloadStatus.Paused)
                } else it
            },
            activeTaskId = null,
            activeProgress = 0f
        )

        assertTrue(paused.isPaused)
        assertNull(paused.activeTaskId)
        assertEquals(0f, paused.activeProgress, 0.001f)
        assertEquals(2, paused.tasks.count { it.status is DownloadStatus.Paused })
        assertEquals(1, paused.completedTasks.size)
        assertTrue(paused.isIdle)

        // Simulate resume state mapping
        val resumed = paused.copy(
            isPaused = false,
            tasks = paused.tasks.map {
                if (it.status is DownloadStatus.Paused) {
                    it.copy(status = DownloadStatus.Queued)
                } else it
            }
        )

        assertFalse(resumed.isPaused)
        assertEquals(2, resumed.queuedTasks.size)
        assertEquals(1, resumed.completedTasks.size)
        assertFalse(resumed.isIdle)
    }

    @Test
    fun downloadQueueStateCancelAndRetryLogic() {
        val t1 = createTrack("trk_1")
        val t2 = createTrack("trk_2")
        val t3 = createTrack("trk_3")

        val state = DownloadQueueState(
            tasks = listOf(
                DownloadTask(track = t1, status = DownloadStatus.Failed("Connection refused")),
                DownloadTask(track = t2, status = DownloadStatus.Completed),
                DownloadTask(track = t3, status = DownloadStatus.Queued)
            )
        )

        // Cancel task trk_3
        val cancelled = state.copy(
            tasks = state.tasks.filterNot { it.track.id == "trk_3" }
        )
        assertEquals(2, cancelled.tasks.size)
        assertNull(cancelled.tasks.firstOrNull { it.track.id == "trk_3" })

        // Retry failed tasks
        val retried = cancelled.copy(
            tasks = cancelled.tasks.map {
                if (it.status is DownloadStatus.Failed) {
                    it.copy(status = DownloadStatus.Queued)
                } else it
            }
        )
        assertEquals(1, retried.queuedTasks.size)
        assertEquals("trk_1", retried.queuedTasks.first().track.id)
        assertEquals(0, retried.failedTasks.size)
        assertEquals(1, retried.completedTasks.size)

        // Clear completed
        val cleared = retried.copy(
            tasks = retried.tasks.filterNot { it.status is DownloadStatus.Completed }
        )
        assertEquals(1, cleared.tasks.size)
        assertEquals("trk_1", cleared.tasks.first().track.id)
    }
}
