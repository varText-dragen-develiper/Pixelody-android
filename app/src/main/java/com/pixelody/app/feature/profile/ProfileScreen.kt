package com.pixelody.app.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.data.model.HostConnectionState
import com.pixelody.app.data.model.LibrarySnapshot
import com.pixelody.app.data.model.SavedHostProfile
import com.pixelody.app.data.storage.DownloadQueueManager
import com.pixelody.app.data.storage.DownloadQueueState
import com.pixelody.app.data.storage.MobileSettingsStore
import com.pixelody.app.data.storage.OfflineMediaStore
import com.pixelody.app.feature.connection.label
import com.pixelody.app.ui.brand.LogoColorSection
import com.pixelody.app.ui.components.ScreenHeader
import com.pixelody.app.ui.components.SectionCard
import com.pixelody.app.ui.components.StatusChip
import com.pixelody.app.ui.components.performTick
import com.pixelody.app.ui.theme.PixelodyMobileTheme
import java.util.Locale

/* =========================================================================
 * Slice 4 — Profile Screen
 * Extracted from PixelodyShell.kt into com.pixelody.app.feature.profile.
 * ========================================================================= */

@Composable
internal fun ProfileScreen(
    activeTheme: PixelodyMobileTheme,
    onThemeChange: (PixelodyMobileTheme) -> Unit,
    onThemeReset: () -> Unit,
    snapshot: LibrarySnapshot?,
    savedHost: SavedHostProfile?,
    connectionState: HostConnectionState,
    localTrackCount: Int,
    cachedTrackCount: Int = 0,
    offlineMediaStore: OfflineMediaStore,
    settingsStore: MobileSettingsStore,
    downloadQueueState: DownloadQueueState,
    downloadQueueManager: DownloadQueueManager,
    onClearOfflineCache: () -> Unit = {},
    onPruneUnpinned: () -> Unit = {},
    onClearArtwork: () -> Unit = {},
    onOpenDevice: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenTechnical: () -> Unit,
    onForgetSavedHost: () -> Unit
) {
    var selectedQuotaBytes by remember { mutableStateOf(settingsStore.loadStorageQuotaBytes()) }
    var autoCacheFavs by remember { mutableStateOf(settingsStore.loadAutoCacheFavorites()) }
    var autoCacheRecent by remember { mutableStateOf(settingsStore.loadAutoCacheRecentCount() > 0) }
    var wifiOnly by remember { mutableStateOf(settingsStore.loadDownloadWifiOnly()) }
    val haptic = LocalHapticFeedback.current

    val audioBytes = remember(cachedTrackCount) { offlineMediaStore.audioCacheSizeBytes() }
    val artBytes = remember(cachedTrackCount) { offlineMediaStore.artCacheSizeBytes() }
    val totalCacheBytes = remember(cachedTrackCount) { offlineMediaStore.totalCacheSizeBytes() }
    val freeDiskBytes = remember(cachedTrackCount) { offlineMediaStore.freeDiskSpaceBytes() }

    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024L * 1024L * 1024L -> String.format(Locale.US, "%.2f GB", bytes / (1024f * 1024f * 1024f))
            bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
            bytes >= 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024f)
            else -> "$bytes B"
        }
    }

    val moduleContext = androidx.compose.ui.platform.LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            ScreenHeader(
                title = "Profile",
                subtitle = "Themes, storage quota, listening shortcuts, and music identity."
            )
        }
        item {
            Button(onClick = { moduleContext.startActivity(android.content.Intent(moduleContext, com.pixelody.app.modules.ModuleShopActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("Modules — import or manage") }
        }
        item {
            SectionCard(
                title = "Themes",
                subtitle = "${activeTheme.displayName} / saved on this device"
            ) {
                PixelodyMobileTheme.values().forEach { theme ->
                    ThemeChoiceRow(
                        theme = theme,
                        selected = activeTheme == theme,
                        onClick = { onThemeChange(theme) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedButton(
                    enabled = activeTheme != PixelodyMobileTheme.Studio,
                    onClick = onThemeReset,
                    shape = activeTheme.plate,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset To Studio")
                }
            }
        }
        item {
            LogoColorSection(settingsStore = settingsStore, activeTheme = activeTheme)
        }
        item {
            SectionCard(
                title = "Listening Profile",
                subtitle = snapshot?.host?.hostName ?: savedHost?.hostName ?: "No host connected"
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { StatusChip(text = connectionState.label) }
                    item { StatusChip(text = "${snapshot?.tracks?.size ?: 0} host") }
                    item { StatusChip(text = "$localTrackCount phone") }
                    item { StatusChip(text = "$cachedTrackCount offline") }
                }
            }
        }

        // Active Download Queue Section
        if (downloadQueueState.tasks.isNotEmpty()) {
            item {
                SectionCard(
                    title = "Download Queue",
                    subtitle = if (downloadQueueState.isPaused) "Queue Paused" else "${downloadQueueState.queuedTasks.size} queued • ${downloadQueueState.completedTasks.size} completed"
                ) {
                    val active = downloadQueueState.activeTask
                    if (active != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = active.track.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${active.track.artist} • ${active.track.format.uppercase(Locale.US)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${(downloadQueueState.activeProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { downloadQueueState.activeProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (downloadQueueState.isPaused) {
                            Button(
                                onClick = { downloadQueueManager.resumeQueue() },
                                shape = activeTheme.plate,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Resume")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { downloadQueueManager.pauseQueue() },
                                shape = activeTheme.plate,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Pause")
                            }
                        }
                        if (downloadQueueState.failedTasks.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { downloadQueueManager.retryFailed() },
                                shape = activeTheme.plate,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Retry Failed")
                            }
                        }
                        OutlinedButton(
                            onClick = { downloadQueueManager.cancelAll() },
                            shape = activeTheme.plate,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel All")
                        }
                    }
                }
            }
        }

        // Offline Storage Management Section
        item {
            SectionCard(
                title = "Offline Storage & Cache",
                subtitle = "$cachedTrackCount cached track${if (cachedTrackCount == 1) "" else "s"} • ${formatBytes(totalCacheBytes)} used"
            ) {
                // Storage Bar Breakdown
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Storage Allocation",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val totalDenom = (totalCacheBytes + freeDiskBytes).coerceAtLeast(1L).toFloat()
                    val audioWeight = (audioBytes.toFloat() / totalDenom).coerceIn(0.01f, 1f)
                    val artWeight = (artBytes.toFloat() / totalDenom).coerceIn(0.005f, 1f)
                    val freeWeight = (freeDiskBytes.toFloat() / totalDenom).coerceIn(0.01f, 1f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(audioWeight)
                                .fillMaxHeight()
                                .background(Color(0xFF10B981))
                        )
                        Box(
                            modifier = Modifier
                                .weight(artWeight)
                                .fillMaxHeight()
                                .background(Color(0xFF06B6D4))
                        )
                        Box(
                            modifier = Modifier
                                .weight(freeWeight)
                                .fillMaxHeight()
                                .background(Color(0xFF64748B))
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Audio: ${formatBytes(audioBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF10B981)
                        )
                        Text(
                            text = "Art: ${formatBytes(artBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF06B6D4)
                        )
                        Text(
                            text = "Free: ${formatBytes(freeDiskBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(10.dp))

                // Storage Quota Selector
                Text(
                    text = "Storage Quota Limit (LRU Auto-Eviction)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "When quota is reached, least recently played unpinned tracks are pruned automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                val quotaOptions = listOf(
                    "1 GB" to 1L * 1024L * 1024L * 1024L,
                    "2 GB" to 2L * 1024L * 1024L * 1024L,
                    "5 GB" to 5L * 1024L * 1024L * 1024L,
                    "10 GB" to 10L * 1024L * 1024L * 1024L,
                    "Max" to MobileSettingsStore.UNLIMITED_QUOTA_BYTES
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(quotaOptions) { (label, bytes) ->
                        val isSelected = selectedQuotaBytes == bytes
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedQuotaBytes = bytes
                                settingsStore.saveStorageQuotaBytes(bytes)
                                haptic.performTick()
                            },
                            shape = activeTheme.plate,
                            label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(10.dp))

                // Auto-Cache Policies
                Text(
                    text = "Smart Offline Sync Policies",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Auto-Cache Favorites", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Download newly favorited tracks for offline playback",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoCacheFavs,
                        onCheckedChange = { checked ->
                            autoCacheFavs = checked
                            settingsStore.saveAutoCacheFavorites(checked)
                            haptic.performTick()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Auto-Cache Last 20 Played", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Keep recently streamed tracks in local cache",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoCacheRecent,
                        onCheckedChange = { checked ->
                            autoCacheRecent = checked
                            settingsStore.saveAutoCacheRecentCount(if (checked) 20 else 0)
                            haptic.performTick()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Download on Wi-Fi Only", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Prevent cellular data usage when downloading",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = wifiOnly,
                        onCheckedChange = { checked ->
                            wifiOnly = checked
                            settingsStore.saveDownloadWifiOnly(checked)
                            haptic.performTick()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(10.dp))

                // Storage Cleanup Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onPruneUnpinned,
                        shape = activeTheme.plate,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Prune Unpinned", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onClearArtwork,
                        shape = activeTheme.plate,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear Art", maxLines = 1)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onClearOfflineCache,
                    shape = activeTheme.plate,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All Offline Downloads")
                }
            }
        }

        item {
            SectionCard(
                title = "Shortcuts",
                subtitle = "Fast routes back to listening."
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onOpenDevice, shape = activeTheme.plate, modifier = Modifier.weight(1f)) { Text("Files") }
                    OutlinedButton(onClick = onOpenQueue, shape = activeTheme.plate, modifier = Modifier.weight(1f)) { Text("Queue") }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(onClick = onOpenTechnical, shape = activeTheme.plate, modifier = Modifier.fillMaxWidth()) { Text("Connection Tools") }
            }
        }
        if (savedHost != null) {
            item {
                SectionCard(
                    title = "Saved Host",
                    subtitle = "${savedHost.hostName} / ${savedHost.baseUrl}"
                ) {
                    OutlinedButton(onClick = onForgetSavedHost, shape = activeTheme.plate, modifier = Modifier.fillMaxWidth()) {
                        Text("Forget Saved Host")
                    }
                }
            }
        }
    }
}

@Composable
internal fun ThemeChoiceRow(
    theme: PixelodyMobileTheme,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        shape = theme.plate,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) theme.accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(theme.plate)
                    .background(theme.accentColor)
                    .border(2.dp, if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), theme.plate),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = theme.badge.take(2),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(theme.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = theme.accentColor.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.45f)),
                        shape = theme.plate
                    ) {
                        Text(
                            text = theme.badge,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = theme.accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    theme.summary,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = theme.accentColor,
                    shape = theme.plate
                ) {
                    Text(
                        text = "ACTIVE",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
            }
        }
    }
}
