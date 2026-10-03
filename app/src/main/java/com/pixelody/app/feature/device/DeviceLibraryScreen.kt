package com.pixelody.app.feature.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelody.app.core.hosting.AndroidHostStatus
import com.pixelody.app.data.model.EqualizerProfile
import com.pixelody.app.data.model.Track
import com.pixelody.app.ui.components.EmptyState
import com.pixelody.app.ui.components.PixelodyTransportGlyph
import com.pixelody.app.ui.components.ScreenHeader
import com.pixelody.app.ui.components.SectionCard
import com.pixelody.app.ui.components.TrackRow
import com.pixelody.app.ui.components.TransportGlyphType
import com.pixelody.app.ui.components.performConfirm
import com.pixelody.app.ui.components.performTick
import com.pixelody.app.ui.navigation.PixelodyStateTags

/* =========================================================================
 * Slice 4 — Device / Files Screen
 * Extracted from PixelodyShell.kt into com.pixelody.app.feature.device.
 * ========================================================================= */

@Composable
internal fun DeviceLibraryScreen(
    localTracks: List<Track>,
    selectedTrack: Track?,
    hostStatus: AndroidHostStatus,
    isScanningDevice: Boolean,
    isScanningFolder: Boolean,
    scanNotice: String,
    onScanDevice: () -> Unit,
    onScanFolder: () -> Unit,
    onPickAudio: () -> Unit,
    onClearLocalTracks: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLibrary: () -> Unit = {},
    equalizerProfile: EqualizerProfile,
    trackHasEqualizerOverride: Boolean,
    onCycleEqualizerPreset: () -> Unit,
    onStartHost: () -> Unit,
    onStopHost: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var query by remember { mutableStateOf("") }
    val filteredTracks = remember(localTracks, query) {
        val needle = query.trim().lowercase()
        if (needle.isBlank()) localTracks else localTracks.filter { track ->
            listOf(track.title, track.artist, track.album, track.codec, track.format)
                .any { it.lowercase().contains(needle) }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp)
    ) {
        item {
            ScreenHeader(
                title = "On This Phone",
                subtitle = "Choose your music. No desktop connection is needed."
            )
        }
        item {
            SectionCard(
                title = "Add your music",
                subtitle = if (localTracks.isEmpty()) "No local files added yet" else "${localTracks.size} local track${if (localTracks.size == 1) "" else "s"} ready"
            ) {
                Text("Choose a folder or specific files, or let Pixelody find audio on your phone. Access is requested only when you choose an action.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        haptic.performConfirm()
                        onScanDevice()
                    },
                    enabled = !isScanningDevice && !isScanningFolder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(PixelodyStateTags.ACQUIRE_SCAN_DEVICE)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PixelodyTransportGlyph(
                            glyph = TransportGlyphType.Search,
                            color = MaterialTheme.colorScheme.onPrimary,
                            size = 16.dp
                        )
                        Text(if (isScanningDevice) "Finding your music..." else "Find music on this phone")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            haptic.performTick()
                            onScanFolder()
                        },
                        enabled = !isScanningDevice && !isScanningFolder,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(PixelodyStateTags.ACQUIRE_PICK_FOLDER)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PixelodyTransportGlyph(
                                glyph = TransportGlyphType.PhoneDevice,
                                color = MaterialTheme.colorScheme.primary,
                                size = 14.dp
                            )
                            Text("Choose folder")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            haptic.performTick()
                            onPickAudio()
                        },
                        enabled = !isScanningDevice && !isScanningFolder,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(PixelodyStateTags.ACQUIRE_PICK_FILES)
                    ) {
                        Text("Choose files")
                    }
                }
                if (localTracks.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Open your library") }
                }
                if (scanNotice.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = scanNotice,
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        if (localTracks.isEmpty()) {
            item { EmptyState(text = "Scan a folder or choose songs to build your device library.") }
            return@LazyColumn
        }
        item {
            val haptic = LocalHapticFeedback.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        haptic.performConfirm()
                        filteredTracks.firstOrNull()?.let(onPlayTrack)
                    },
                    enabled = filteredTracks.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Play all (${filteredTracks.size})")
                }
                OutlinedButton(
                    onClick = {
                        haptic.performTick()
                        onOpenQueue()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Queue")
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search device files") },
                singleLine = true
            )
        }
        items(filteredTracks, key = { it.id }) { track ->
            TrackRow(
                track = track,
                selected = selectedTrack?.id == track.id,
                onClick = { onPlayTrack(track) },
                equalizerProfile = equalizerProfile,
                trackHasEqualizerOverride = trackHasEqualizerOverride,
                onOpenPlayer = onOpenPlayer,
                onOpenQueue = onOpenQueue,
                onCycleEqualizerPreset = onCycleEqualizerPreset
            )
        }
    }
}

@Composable
internal fun AndroidHostingCard(
    hostStatus: AndroidHostStatus,
    hasLocalTracks: Boolean,
    onStartHost: () -> Unit,
    onStopHost: () -> Unit
) {
    SectionCard(
        title = "Host this phone",
        subtitle = if (hostStatus.running) {
            "Available to trusted devices on this local network."
        } else {
            "Share selected phone files as a temporary Pixelody host."
        }
    ) {
        if (hostStatus.running) {
            Text(
                text = hostStatus.baseUrl,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${hostStatus.trackCount} hosted track${if (hostStatus.trackCount == 1) "" else "s"} / token ${hostStatus.token.take(8)}...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onStopHost,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(PixelodyStateTags.CREATE_HOST_TOGGLE)
            ) {
                Text("Stop Hosting")
            }
        } else {
            Text(
                text = "Hosting stays opt-in and only serves the local files you selected in this app session.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                enabled = hasLocalTracks,
                onClick = onStartHost,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(PixelodyStateTags.CREATE_HOST_TOGGLE)
            ) {
                Text("Start Local Host")
            }
        }
        if (hostStatus.lastError.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = hostStatus.lastError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
