package com.pixelody.app.feature.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pixelody.app.core.playback.FlowShuffleMode
import com.pixelody.app.core.playback.QueueUndoState
import com.pixelody.app.core.playback.ShuffledTrackEntry
import com.pixelody.app.data.model.CamelotKey
import com.pixelody.app.data.model.EqualizerProfile
import com.pixelody.app.data.model.LibrarySnapshot
import com.pixelody.app.data.model.LiveState
import com.pixelody.app.data.model.Track
import com.pixelody.app.ui.components.EmptyState
import com.pixelody.app.ui.components.PixelodyTransportGlyph
import com.pixelody.app.ui.components.QueueUndoPill
import com.pixelody.app.ui.components.ScreenHeader
import com.pixelody.app.ui.components.SectionCard
import com.pixelody.app.ui.components.TrackRow
import com.pixelody.app.ui.components.TransportGlyphType
import com.pixelody.app.ui.components.performTick
import com.pixelody.app.ui.theme.LocalPixelodyThemeVariant

@Composable
internal fun QueueScreen(
    snapshot: LibrarySnapshot?,
    liveState: LiveState?,
    canRemoteControl: Boolean,
    canWriteQueue: Boolean,
    selectedTrack: Track?,
    queue: List<Track>,
    isLocalQueue: Boolean,
    isPlaying: Boolean = false,
    shuffleMode: FlowShuffleMode = FlowShuffleMode.Off,
    flowKey: CamelotKey? = null,
    flowBpm: Float? = null,
    plannedEntries: List<ShuffledTrackEntry> = emptyList(),
    onToggleShuffle: () -> Unit = {},
    onReshuffle: () -> Unit = {},
    onPlayTrack: (Track) -> Unit,
    onOpenPlayer: () -> Unit,
    equalizerProfile: EqualizerProfile,
    trackHasEqualizerOverride: Boolean,
    onCycleEqualizerPreset: () -> Unit,
    onRemotePlayback: (String) -> Unit,
    onAddRemoteQueue: (Track) -> Unit,
    undoState: QueueUndoState = QueueUndoState(),
    onUndoQueueRemoval: () -> Unit = {},
    onDismissUndo: () -> Unit = {},
    onRemoveTrack: (Track, Int) -> Unit = { _, _ -> },
    onClearQueue: () -> Unit = {},
    onReorderQueue: (List<Track>) -> Unit = {},
    onInsertBridgeTracks: (List<Track>, Int) -> Unit = { _, _ -> },
    onToggleFavorite: ((Track) -> Unit)? = null,
    onLongClickTrack: ((Track) -> Unit)? = null,
    onSaveQueueAsPlaylist: (() -> Unit)? = null,
    onClose: () -> Unit = onOpenPlayer,
    experienceMode: com.pixelody.app.data.model.AppExperienceMode = com.pixelody.app.data.model.AppExperienceMode.Essential,
    onShowDoc: ((String) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val theme = LocalPixelodyThemeVariant.current
    val plannedMap = remember(plannedEntries) { plannedEntries.associate { it.track.id to it.cue } }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 80.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScreenHeader(
                            title = "Queue",
                            subtitle = if (isLocalQueue) "Up next from this phone." else "Up next from the current host queue.",
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (queue.isNotEmpty() && onSaveQueueAsPlaylist != null) {
                                TextButton(
                                    onClick = {
                                        haptic.performTick()
                                        onSaveQueueAsPlaylist()
                                    }
                                ) {
                                    Text("+ Playlist")
                                }
                            }
                            if (queue.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        haptic.performTick()
                                        onClearQueue()
                                    }
                                ) {
                                    Text("Clear")
                                }
                            }
                            TextButton(
                                onClick = {
                                    haptic.performTick()
                                    onClose()
                                }
                            ) {
                                Text("Done")
                            }
                        }
                    }
                }
            item {
                FlowEntry(shuffleMode, flowKey, flowBpm, onToggleShuffle)
            }
            if (!isLocalQueue && snapshot != null) {
                item {
                    SectionCard(
                        title = "Host Controls",
                        subtitle = liveState?.let { "Live revision ${it.revision} / ${it.permissions.joinToString(", ").ifBlank { "read-only" }}" }
                            ?: "Waiting for live host state.",
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                enabled = canRemoteControl,
                                onClick = { onRemotePlayback("previous") }
                            ) { Text("Prev") }
                            Button(
                                enabled = canRemoteControl,
                                onClick = { onRemotePlayback("toggle") }
                            ) { Text("Play/Pause") }
                            OutlinedButton(
                                enabled = canRemoteControl,
                                onClick = { onRemotePlayback("next") }
                            ) { Text("Next") }
                        }
                        if (!canRemoteControl) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "This trusted device does not have remote playback control.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            if ((!isLocalQueue && snapshot == null) || queue.isEmpty()) {
                item { EmptyState(text = "No queue is available.") }
            } else {
                itemsIndexed(queue, key = { index, track -> "$index-${track.id}" }) { index, track ->
                    val cue = plannedMap[track.id]
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                TrackRow(
                                    track = track,
                                    selected = false,
                                    onClick = { onPlayTrack(track) },
                                    transitionCue = cue,
                                    isPlaying = isPlaying && selectedTrack?.id == track.id,
                                    isFavorite = track.favorite,
                                    onToggleFavorite = onToggleFavorite?.let { cb -> { cb(track) } },
                                    onLongClick = onLongClickTrack?.let { cb -> { cb(track) } },
                                    equalizerProfile = equalizerProfile,
                                    trackHasEqualizerOverride = trackHasEqualizerOverride,
                                    onOpenPlayer = onOpenPlayer,
                                    onOpenQueue = null,
                                    onCycleEqualizerPreset = onCycleEqualizerPreset
                                )
                            }
                            Surface(
                                onClick = {
                                    haptic.performTick()
                                    onRemoveTrack(track, index)
                                },
                                shape = theme.plate,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(start = 4.dp, end = 8.dp).size(48.dp)
                                    .semantics { contentDescription = "Remove ${track.title} from queue, position ${index + 1}" }
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    PixelodyTransportGlyph(
                                        glyph = TransportGlyphType.Close,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        size = 12.dp
                                    )
                                }
                            }
                        }
                        if (!isLocalQueue && canWriteQueue) {
                            TextButton(onClick = { onAddRemoteQueue(track) }) {
                                Text("Add to host queue")
                            }
                        }

                    }
                }
            }
        }

        if (undoState.canUndo) {
            QueueUndoPill(
                message = undoState.message,
                onUndo = onUndoQueueRemoval,
                onDismiss = onDismissUndo,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 110.dp)
                    .zIndex(10f)
            )
        }


    }
}
}

@Composable
internal fun SourceSummaryCard(
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}
