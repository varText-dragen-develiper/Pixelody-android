package com.pixelody.app.feature.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelody.app.core.hosting.AndroidHostStatus
import com.pixelody.app.data.model.SavedHostProfile
import com.pixelody.app.ui.components.PixelodyTransportGlyph
import com.pixelody.app.ui.components.ScreenHeader
import com.pixelody.app.ui.components.SectionCard
import com.pixelody.app.ui.components.TransportGlyphType
import com.pixelody.app.ui.components.performConfirm
import com.pixelody.app.ui.components.performTick
import com.pixelody.app.ui.navigation.PixelodyStateTags

/* =========================================================================
 * Create Screen
 * Extracted from PixelodyShell.kt into com.pixelody.app.feature.create.
 * ========================================================================= */

@Composable
internal fun CreateScreen(
    localTrackCount: Int,
    androidHostStatus: AndroidHostStatus,
    savedHost: SavedHostProfile?,
    isScanningDevice: Boolean,
    isScanningFolder: Boolean,
    scanNotice: String,
    onScanDevice: () -> Unit,
    onScanFolder: () -> Unit,
    onPickAudio: () -> Unit,
    onOpenPair: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenTechnical: () -> Unit,
    onStartHost: () -> Unit,
    onStopHost: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            ScreenHeader(
                title = "Create",
                subtitle = "Add music or start this phone as a portable source."
            )
        }
        item {
            SectionCard(
                title = "Add Music",
                subtitle = if (localTrackCount == 0) "Scan entire phone storage or select specific files" else "$localTrackCount phone file${if (localTrackCount == 1) "" else "s"} ready"
            ) {
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
                        Text(if (isScanningDevice) "Scanning Entire Device..." else "Scan Entire Device")
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
                            Text("Pick Folder")
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
                        Text("Pick Files")
                    }
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
        item {
            SectionCard(
                title = "Host From This Device",
                subtitle = if (androidHostStatus.running) {
                    "${androidHostStatus.trackCount} phone track${if (androidHostStatus.trackCount == 1) "" else "s"} available"
                } else {
                    "Portable hosting is opt-in."
                }
            ) {
                if (androidHostStatus.running) {
                    Text(
                        text = androidHostStatus.baseUrl.ifBlank { androidHostStatus.localBaseUrl },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onStopHost,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(PixelodyStateTags.CREATE_HOST_TOGGLE)
                    ) { Text("Stop Hosting") }
                } else {
                    Button(
                        enabled = localTrackCount > 0,
                        onClick = onStartHost,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(PixelodyStateTags.CREATE_HOST_TOGGLE)
                    ) { Text("Start Phone Host") }
                    if (androidHostStatus.lastError.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = androidHostStatus.lastError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        item {
            SectionCard(
                title = "Connect Or Personalize",
                subtitle = savedHost?.let { "Saved host: ${it.hostName}" } ?: "Join a host or pick a look."
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onOpenPair,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(PixelodyStateTags.CREATE_PAIR)
                    ) { Text("Pair") }
                    OutlinedButton(onClick = onOpenProfile, modifier = Modifier.weight(1f)) { Text("Themes") }
                }
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = onOpenTechnical, modifier = Modifier.fillMaxWidth()) { Text("Advanced Connection") }
            }
        }
    }
}
