package com.pixelody.app.data.storage

import com.pixelody.app.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class DownloadStatus {
    object Queued : DownloadStatus()
    data class InProgress(val progress: Float, val bytesDownloaded: Long = 0L, val totalBytes: Long = 0L) : DownloadStatus()
    object Completed : DownloadStatus()
    data class Failed(val reason: String) : DownloadStatus()
    object Paused : DownloadStatus()
}

data class DownloadTask(
    val track: Track,
    val isPinned: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    val status: DownloadStatus = DownloadStatus.Queued
)

data class DownloadQueueState(
    val tasks: List<DownloadTask> = emptyList(),
    val isPaused: Boolean = false,
    val activeTaskId: String? = null,
    val activeProgress: Float = 0f
) {
    val queuedTasks: List<DownloadTask> get() = tasks.filter { it.status is DownloadStatus.Queued }
    val inProgressTasks: List<DownloadTask> get() = tasks.filter { it.status is DownloadStatus.InProgress }
    val completedTasks: List<DownloadTask> get() = tasks.filter { it.status is DownloadStatus.Completed }
    val failedTasks: List<DownloadTask> get() = tasks.filter { it.status is DownloadStatus.Failed }

    val activeTask: DownloadTask? get() = tasks.firstOrNull { it.track.id == activeTaskId }
    val isIdle: Boolean get() = queuedTasks.isEmpty() && inProgressTasks.isEmpty()

    val overallProgress: Float
        get() {
            if (tasks.isEmpty()) return 1f
            val completed = completedTasks.size.toFloat()
            val inProg = if (activeTask != null) activeProgress else 0f
            return (completed + inProg) / tasks.size.toFloat()
        }
}

class DownloadQueueManager(
    private val offlineMediaStore: OfflineMediaStore,
    private val settingsStore: MobileSettingsStore,
    private val scope: CoroutineScope,
    private val hostBaseUrlProvider: () -> String?,
    private val tokenProvider: () -> String = { "" },
    private val onTrackCached: (Track) -> Unit = {}
) {
    private val _queueState = MutableStateFlow(DownloadQueueState())
    val queueState: StateFlow<DownloadQueueState> = _queueState.asStateFlow()

    private var workerJob: Job? = null

    fun enqueue(track: Track, isPinned: Boolean = false) {
        // If already cached, just update pinned status if needed
        if (offlineMediaStore.isCached(track.id)) {
            if (isPinned) {
                offlineMediaStore.setTrackPinned(track.id, true)
            }
            return
        }

        _queueState.update { state ->
            val existing = state.tasks.firstOrNull { it.track.id == track.id }
            if (existing != null) {
                // If it was failed or paused, re-queue it
                if (existing.status is DownloadStatus.Failed || existing.status is DownloadStatus.Paused) {
                    val updated = state.tasks.map {
                        if (it.track.id == track.id) it.copy(status = DownloadStatus.Queued, isPinned = isPinned || it.isPinned) else it
                    }
                    state.copy(tasks = updated)
                } else {
                    state
                }
            } else {
                state.copy(tasks = state.tasks + DownloadTask(track = track, isPinned = isPinned))
            }
        }
        ensureWorkerRunning()
    }

    fun enqueueBatch(tracks: List<Track>, isPinned: Boolean = false) {
        val nonCached = tracks.filterNot { offlineMediaStore.isCached(it.id) }
        if (nonCached.isEmpty()) {
            tracks.forEach { if (isPinned) offlineMediaStore.setTrackPinned(it.id, true) }
            return
        }

        _queueState.update { state ->
            val existingIds = state.tasks.map { it.track.id }.toSet()
            val newTasks = nonCached.filterNot { it.id in existingIds }.map {
                DownloadTask(track = it, isPinned = isPinned)
            }
            state.copy(tasks = state.tasks + newTasks)
        }
        ensureWorkerRunning()
    }

    fun pauseQueue() {
        _queueState.update { state ->
            state.copy(
                isPaused = true,
                tasks = state.tasks.map {
                    if (it.status is DownloadStatus.Queued || it.status is DownloadStatus.InProgress) {
                        it.copy(status = DownloadStatus.Paused)
                    } else it
                },
                activeTaskId = null,
                activeProgress = 0f
            )
        }
        workerJob?.cancel()
        workerJob = null
    }

    fun resumeQueue() {
        _queueState.update { state ->
            state.copy(
                isPaused = false,
                tasks = state.tasks.map {
                    if (it.status is DownloadStatus.Paused) {
                        it.copy(status = DownloadStatus.Queued)
                    } else it
                }
            )
        }
        ensureWorkerRunning()
    }

    fun cancelTask(trackId: String) {
        val currentActive = _queueState.value.activeTaskId
        _queueState.update { state ->
            state.copy(
                tasks = state.tasks.filterNot { it.track.id == trackId },
                activeTaskId = if (state.activeTaskId == trackId) null else state.activeTaskId,
                activeProgress = if (state.activeTaskId == trackId) 0f else state.activeProgress
            )
        }
        if (currentActive == trackId) {
            workerJob?.cancel()
            workerJob = null
            ensureWorkerRunning()
        }
    }

    fun cancelAll() {
        workerJob?.cancel()
        workerJob = null
        _queueState.update { DownloadQueueState() }
    }

    fun retryFailed() {
        _queueState.update { state ->
            state.copy(
                tasks = state.tasks.map {
                    if (it.status is DownloadStatus.Failed) {
                        it.copy(status = DownloadStatus.Queued)
                    } else it
                }
            )
        }
        ensureWorkerRunning()
    }

    fun clearCompleted() {
        _queueState.update { state ->
            state.copy(tasks = state.tasks.filterNot { it.status is DownloadStatus.Completed })
        }
    }

    private fun ensureWorkerRunning() {
        if (workerJob?.isActive == true) return
        if (_queueState.value.isPaused) return

        workerJob = scope.launch(Dispatchers.IO) {
            while (true) {
                val nextTask = _queueState.value.tasks.firstOrNull { it.status is DownloadStatus.Queued }
                if (nextTask == null || _queueState.value.isPaused) {
                    break
                }

                val currentHost = hostBaseUrlProvider()
                if (currentHost.isNullOrBlank()) {
                    _queueState.update { state ->
                        state.copy(
                            tasks = state.tasks.map {
                                if (it.track.id == nextTask.track.id) {
                                    it.copy(status = DownloadStatus.Failed("No paired host available"))
                                } else it
                            }
                        )
                    }
                    continue
                }

                // Check and enforce storage quota with LRU eviction before downloading
                val quotaBytes = settingsStore.loadStorageQuotaBytes()
                if (quotaBytes > 0L) {
                    val estimatedBytes = if (nextTask.track.durationSeconds > 0) {
                        nextTask.track.durationSeconds.toLong() * 44100L * 4L
                    } else {
                        30_000_000L
                    }
                    val currentSize = offlineMediaStore.totalCacheSizeBytes()
                    if (currentSize + estimatedBytes > quotaBytes) {
                        val needed = (currentSize + estimatedBytes) - quotaBytes
                        offlineMediaStore.evictOldest(needed)
                    }
                }

                // Set task to InProgress
                _queueState.update { state ->
                    state.copy(
                        activeTaskId = nextTask.track.id,
                        activeProgress = 0f,
                        tasks = state.tasks.map {
                            if (it.track.id == nextTask.track.id) {
                                it.copy(status = DownloadStatus.InProgress(0f))
                            } else it
                        }
                    )
                }

                try {
                    val cached = offlineMediaStore.cacheTrack(
                        track = nextTask.track,
                        hostBaseUrl = currentHost,
                        token = tokenProvider(),
                        isPinned = nextTask.isPinned,
                        onProgress = { progress ->
                            _queueState.update { state ->
                                if (state.activeTaskId == nextTask.track.id) {
                                    state.copy(
                                        activeProgress = progress,
                                        tasks = state.tasks.map {
                                            if (it.track.id == nextTask.track.id) {
                                                it.copy(status = DownloadStatus.InProgress(progress))
                                            } else it
                                        }
                                    )
                                } else state
                            }
                        }
                    )

                    _queueState.update { state ->
                        state.copy(
                            activeTaskId = if (state.activeTaskId == nextTask.track.id) null else state.activeTaskId,
                            activeProgress = if (state.activeTaskId == nextTask.track.id) 0f else state.activeProgress,
                            tasks = state.tasks.map {
                                if (it.track.id == nextTask.track.id) {
                                    it.copy(status = DownloadStatus.Completed)
                                } else it
                            }
                        )
                    }
                    withContext(Dispatchers.Main) {
                        onTrackCached(cached.toTrack())
                    }
                } catch (e: Exception) {
                    _queueState.update { state ->
                        state.copy(
                            activeTaskId = if (state.activeTaskId == nextTask.track.id) null else state.activeTaskId,
                            activeProgress = if (state.activeTaskId == nextTask.track.id) 0f else state.activeProgress,
                            tasks = state.tasks.map {
                                if (it.track.id == nextTask.track.id) {
                                    it.copy(status = DownloadStatus.Failed(e.message ?: "Download failed"))
                                } else it
                            }
                        )
                    }
                }
            }
        }
    }
}
