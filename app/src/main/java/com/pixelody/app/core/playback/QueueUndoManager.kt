package com.pixelody.app.core.playback

import com.pixelody.app.data.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DismissedQueueEntry(
    val track: Track,
    val index: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class QueueUndoState(
    val lastDismissed: DismissedQueueEntry? = null,
    val canUndo: Boolean = false,
    val message: String = ""
)

class QueueUndoManager {
    private val _undoState = MutableStateFlow(QueueUndoState())
    val undoState: StateFlow<QueueUndoState> = _undoState.asStateFlow()

    private val undoStack = mutableListOf<DismissedQueueEntry>()

    fun recordDismissal(track: Track, index: Int) {
        val entry = DismissedQueueEntry(track, index)
        undoStack.add(entry)
        _undoState.value = QueueUndoState(
            lastDismissed = entry,
            canUndo = true,
            message = "Removed \"${track.title.ifBlank { "track" }}\""
        )
    }

    fun popUndo(): DismissedQueueEntry? {
        if (undoStack.isEmpty()) return null
        val entry = undoStack.removeAt(undoStack.lastIndex)
        _undoState.value = if (undoStack.isNotEmpty()) {
            val next = undoStack.last()
            QueueUndoState(
                lastDismissed = next,
                canUndo = true,
                message = "Removed \"${next.track.title.ifBlank { "track" }}\""
            )
        } else {
            QueueUndoState(
                lastDismissed = null,
                canUndo = false,
                message = ""
            )
        }
        return entry
    }

    fun discardTracks(trackIds: Set<String>) {
        if (!undoStack.removeAll { it.track.id in trackIds }) return
        val last = undoStack.lastOrNull()
        _undoState.value = if (last == null) QueueUndoState() else QueueUndoState(
            lastDismissed = last, canUndo = true, message = "Removed \"${last.track.title.ifBlank { "track" }}\""
        )
    }

    fun clear() {
        undoStack.clear()
        _undoState.value = QueueUndoState()
    }
}
